package org.blueskiron.goldilocks.membership;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.blueskiron.goldilocks.api.Member;
import org.blueskiron.goldilocks.api.Membership;
import org.blueskiron.goldilocks.api.Raft;
import org.blueskiron.goldilocks.api.messages.AppendEntriesRequest;
import org.blueskiron.goldilocks.api.messages.AppendEntriesResponse;
import org.blueskiron.goldilocks.api.messages.VoteRequest;
import org.blueskiron.goldilocks.api.messages.VoteResponse;
import org.blueskiron.goldilocks.api.statemachine.Command;
import org.blueskiron.goldilocks.api.statemachine.StateMachine;
import org.blueskiron.goldilocks.api.states.Follower;
import org.blueskiron.goldilocks.api.states.Leader;
import org.blueskiron.goldilocks.api.states.State;
import org.blueskiron.goldilocks.log.replication.InMemoryLog;
import org.blueskiron.goldilocks.membership.messages.AppendEntriesResponseImpl;
import org.blueskiron.goldilocks.membership.messages.Join;
import org.blueskiron.goldilocks.membership.messages.VoteResponseImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rafter class backs a StateMachine and maintains internal variables of the Raft protocol, such as
 * committed term, log index and replicated log entries.
 * @author jzachar
 */
final class Rafter implements Raft {

  private static final Logger LOG = LoggerFactory.getLogger(Rafter.class);
  // TODO configure me
  private final InMemoryLog memlog = new InMemoryLog(1024);
  private final AtomicReference<State> state = new AtomicReference<State>();
  private final AtomicInteger committedTerm = new AtomicInteger();
  private final AtomicLong committedLogIndex = new AtomicLong();
  private final AtomicReference<String> votedFor = new AtomicReference<>();
  private final ClusterImpl cluster;
  private final Reactor reactor;
  private final StateMachine<?, ?> stateMachine;
  private final Join join;
  private final AtomicReference<Membership> membership = new AtomicReference<>();

  public Rafter(ClusterImpl cluster, Reactor reactor, StateMachine<?, ?> stateMachine, Join join) {
    this.stateMachine = stateMachine;
    this.cluster = cluster;
    this.reactor = reactor;
    this.join = join;
    state.set(new Starter(getFullyQualifiedId()));
    Set<Member> group = cluster.getRemoteMembersView(join.getMemberIds());
    LOG.debug("Formed group of remote replicas: {}", group);
    final Membership membership =
        GroupMembership.createLeaderless(cluster.localMember(), currentState(), group);
    commitMembership(membership);
  }

  @Override
  public void start() {
    LOG.debug("Starting up resource={}", getFullyQualifiedId());
    becomeFollower(0, currentLeaderPromise(), Optional.empty());
  }

  @Override
  public void stop() {
    LOG.debug("Stopping resource={}", getFullyQualifiedId());
    final State state = currentState();
    state.end();
    while (changeState(state, new Starter(getFullyQualifiedId())))
      ;
    commitStateAttributes();
  }

  @SuppressWarnings("unchecked")
  @Override
  public <S extends StateMachine<?, ?>, C extends Command<S>> StateMachine<S, C> stateMachine() {
    return (StateMachine<S, C>) stateMachine;
  }

  @Override
  public Membership membership() {
    return membership.get();
  }

  @Override
  public CompletableFuture<Set<AppendEntriesResponse>> collectLeaseResponses(int term,
      AppendEntriesRequest appendEntriesRequest) {
    return CompletableFuture.supplyAsync(() -> {
      Set<AppendEntriesResponse> responses =
          new HashSet<>(reactor.getCollectedLeaseResponses().values());
      reactor.resetCollectedLeaseResponses();
      return responses;
    });
  }

  @Override
  public CompletableFuture<Set<VoteResponse>> collectVotes(int term, VoteRequest voteRequest) {
    return CompletableFuture.supplyAsync(() -> {
      Set<VoteResponse> collectedVotes = new HashSet<>(reactor.getCollectedVotes().values());
      reactor.resetCollectedVotes();
      return collectedVotes;
    });
  }

  /**
   * @return the join
   */
  public Join getJoin() {
    return join;
  }

  private boolean changeState(State currentState, State desiredState) {
    return state.compareAndSet(currentState, desiredState);
  }

  private void commitStateAttributes() {
    final State currentState = currentState();
    committedTerm.set(currentState.term());
    if (currentState.votedFor().isPresent()) {
      votedFor.set(currentState.votedFor().get());
    }
  }

  private void commitMembership(Membership desiredMembership) {
    membership.set(desiredMembership);
  }

  void become(State desiredState, int term) {
    State currentState = currentState();
    LOG.debug("Trying to transition from {} to {}", currentState, desiredState);
    while (currentState().canTransitionTo(desiredState)) {
      if (changeState(currentState, desiredState)) {
        commitStateAttributes();
        currentState.end();
        desiredState.begin();
      }
    }
    LOG.debug("Transitioned to state {}", currentState());
  }

  void becomeLeader(int term) {
    final LeaderImpl leader =
        new LeaderImpl(this, term, cluster.executorService(), cluster.getConfiguration());
    become(leader, term);
  }

  void becomeCandidate(int term) {
    LOG.debug("Next term: {}", term);
    final CandidateImpl candidate = new CandidateImpl(this, term, cluster.getConfiguration());
    become(candidate, term);
  }

  void becomeFollower(int term, CompletableFuture<Member> leaderPromise, Optional<String> votedFor) {
    final Follower follower = new FollowerImpl(this, cluster, leaderPromise, term, votedFor);
    become(follower, term);
  }

  void on(AppendEntriesRequest request) {
    int term = currentState().term();
    int requestTerm = request.getTerm();
    if (requestTerm < term) {
      AppendEntriesResponse response =
          new AppendEntriesResponseImpl(request.getFullyQualifiedId(), getFullyQualifiedId(), term,
              committedLogIndex(), request.isLeaseRequest());
      cluster.connector().send(request.getMemberId(), response);
    } else if (requestTerm > term) {
      becomeFollower(requestTerm, new CompletableFuture<Member>(), Optional.empty());
    } else {
      currentState().onAppendEntriesRequest(request);
    }
  }

  void on(AppendEntriesResponse response) {
    if (currentState() instanceof Leader) {
      Leader leader = (Leader) currentState();
      leader.onAppendEntriesResponse(response);
      // don not send to itself
    } else if (response != null && !localMember().getId().equals(response.getMemberId())) {
      cluster.connector().send(response.getMemberId(), response);
    }
  }

  void on(VoteResponse response) {
    int responseTerm = response.getTerm();
    if (responseTerm > currentState().term()) {
      becomeFollower(responseTerm, new CompletableFuture<Member>(), Optional.empty());
    }
  }

  void on(VoteRequest request) {
    VoteResponse response = null;
    if (request.getTerm() < currentState().term()) {
      LOG.debug("Voting down old candidate={}", request);
      response =
          VoteResponseImpl.rejected(request, request.getFullyQualifiedId(), currentState().term(),
              committedLogIndex());
    } else {
      response = currentState().onVoteRequest(request);
    }
    // don not send to itself
    if (response != null && !localMember().getId().equals(request.getMemberId())) {
      cluster.connector().send(request.getMemberId(), response);
    }
  }

  void onCompletedLeaderPromise(CompletableFuture<Member> leaderPromise, int term) {
    leaderPromise.thenAccept(member -> {
      LOG.info("For {}@{}[{}] LeaderPromise completed with {}[{}]", stateMachine.getId(),
          localMember().getId(), committedTerm.get(), member.getId(), term);
      final Optional<Member> searchedLeader = membership.get().getMember(member.getId());
      searchedLeader.ifPresent(knownMember -> {
        boolean isSteppingDown = false;
        if (knownMember instanceof RemoteMember) {
          // ((RemoteMember) knownMember).updateTrackedState(State.TAGS.LEADER);
        } else if (!(currentState() instanceof Leader)) {
          isSteppingDown = true;
        }
        if (!isSteppingDown) {
          final Membership membership =
              GroupMembership.createWithConsensus(membership(), knownMember, term, currentState());
          commitMembership(membership);
        }
      });
    });
  }

  void onLeaderless() {
    final Membership membership =
        GroupMembership.createLeaderless(cluster.localMember(), currentState(), membership()
            .remoteMembers());
    commitMembership(membership);
  }

  Member findMember(String memberId) {
    return membership
        .get()
        .getMember(memberId)
        .orElseThrow(
            () -> new RuntimeException("Member '" + memberId + "' not found in the " +
                "current cluster configuration! Exiting."));
  }

  CompletableFuture<Member> currentLeaderPromise() {
    return currentState().leaderPromise();
  }

  State currentState() {
    return state.get();
  }

  String getStateMachineId() {
    return stateMachine.getId();
  }

  LocalMember localMember() {
    return cluster.localMember();
  }

  Membership getMembership() {
    return membership.get();
  }

  int committedTerm() {
    return committedTerm.get();
  }

  long committedLogIndex() {
    return committedLogIndex.get();
  }

  int previousLogTerm() {
    return memlog.getLatestTerm();
  }

  void broadcastHeartbeat(AppendEntriesRequest request) {
    cluster.broadcast(request, membership.get().remoteMembers());
  }

  void broadcastVoteRequest(VoteRequest request) {
    cluster.broadcast(request, membership.get().remoteMembers());
  }

  ScheduledExecutorService executorService() {
    return cluster.executorService();
  }

  String getFullyQualifiedId() {
    return stateMachine.getId() + "@" + localMember().getId();
  }

  Class<?> getStateMachineClass() {
    return stateMachine.getClass();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((cluster == null) ? 0 : cluster.hashCode());
    result = prime * result + ((stateMachine == null) ? 0 : stateMachine.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Rafter other = (Rafter) obj;
    if (cluster == null) {
      if (other.cluster != null)
        return false;
    } else if (!cluster.equals(other.cluster))
      return false;
    if (stateMachine == null) {
      if (other.stateMachine != null)
        return false;
    } else if (!stateMachine.equals(other.stateMachine))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return currentState().toString();
  }
}
