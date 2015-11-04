package org.blueskiron.goldilocks.membership;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.blueskiron.goldilocks.api.Member;
import org.blueskiron.goldilocks.api.messages.AppendEntriesRequest;
import org.blueskiron.goldilocks.api.messages.VoteRequest;
import org.blueskiron.goldilocks.api.messages.VoteResponse;
import org.blueskiron.goldilocks.api.states.Follower;
import org.blueskiron.goldilocks.api.states.State;
import org.blueskiron.goldilocks.leader.election.LeaderTimeout;
import org.blueskiron.goldilocks.membership.messages.AppendEntriesResponseImpl;
import org.blueskiron.goldilocks.membership.messages.VoteResponseImpl;

/**
 * @author jzachar
 */
public class FollowerImpl implements Follower {

  private final Rafter raft;
  private final LeaderTimeout leaderTimeout;
  private final CompletableFuture<Member> leaderPromise;
  private final int term;
  
  //mutable
  private Optional<String> votedFor = Optional.empty();

  public FollowerImpl(Rafter raft, ClusterImpl cluster, CompletableFuture<Member> leaderPromise, int term,
      Optional<String> votedFor) {
    this.raft = raft;
    this.term = term;
    this.leaderPromise = leaderPromise;
    this.votedFor = votedFor;
    this.leaderTimeout = new LeaderTimeout(cluster.getConfiguration(), this, cluster.executorService());
  }

  @Override
  public void begin() {
    leaderTimeout.start();
    if(leaderPromise.isDone()){
      raft.onCompletedLeaderPromise(leaderPromise, term);
    }
  }

  @Override
  public void end() {
    leaderTimeout.stop();
  }

  @Override
  public CompletableFuture<Member> leaderPromise() {
    return leaderPromise;
  }

  @Override
  public int term() {
    return term;
  }

  @Override
  public void becomeCandidate(int term) {
    if (term >= term()) {
      // loss of leader...
      raft.onLeaderless();
      raft.becomeCandidate(term);
    }
  }

  @Override
  public VoteResponse onVoteRequest(VoteRequest voteRequest) {
    long candidateLogIndex = voteRequest.getCommittedLogIndex();
    String localFqId = raft.getFullyQualifiedId();
    String candidateId = voteRequest.getMemberId();
    int candidateTerm = voteRequest.getTerm();
    VoteResponse response = VoteResponseImpl.rejected(voteRequest, localFqId, term(), committedLogIndex());
    if (candidateTerm > term()) {
      leaderTimeout.reset();
      raft.becomeFollower(candidateTerm, new CompletableFuture<Member>(), Optional.empty());
      response = VoteResponseImpl.granted(voteRequest, localFqId, committedLogIndex());
    } else if (!votedFor.isPresent() && candidateLogIndex >= raft.committedLogIndex()) {
      leaderTimeout.reset();
      response = VoteResponseImpl.granted(voteRequest, localFqId, committedLogIndex());
      votedFor = Optional.of(candidateId);
    }
    raft.onLeaderless();
    return response;
  }
  
  private void handleLeaderShip(String leaderId, int leaderTerm){
    if(!leaderPromise.isDone()){
      Member leader = raft.findMember(leaderId);
      leaderPromise.complete(leader);
      raft.onCompletedLeaderPromise(leaderPromise, leaderTerm);
    } else {
      // check if there is a new leader...
      leaderPromise.thenAccept(currentLeader -> {
        if (!currentLeader.getId().equals(leaderId)) {
          Member newLeader = raft.findMember(leaderId);
          leaderPromise.complete(newLeader);
          raft.onCompletedLeaderPromise(leaderPromise, leaderTerm);
        }
      });
    }
  }
  
  @Override
  public void onAppendEntriesRequest(AppendEntriesRequest request) {
    int leaderTerm = request.getTerm();
    String leaderId = request.getMemberId();
    leaderTimeout.reset();
    handleLeaderShip(leaderId, leaderTerm);
    
    // always reply to valid lease requests
    if (request.isLeaseRequest()) {
      raft.on(new AppendEntriesResponseImpl(request.getFullyQualifiedId(), raft.getFullyQualifiedId(), term,
          raft.committedLogIndex(), request.isLeaseRequest()));
    }
    // TO-DO: update log indices and reply to leader...
    if (!request.isHeartbeat()) {
      // TODO ...
      // cluster.on(response);
    }
  }

  @Override
  public long committedLogIndex() {
    return raft.committedLogIndex();
  }

  @Override
  public Optional<String> votedFor() {
    return votedFor;
  }

  @Override
  public String toString() {
    return String.format("%s[%s][%d]", raft.getFullyQualifiedId(), State.TAGS.FOLLOWER, term());
  }
}
