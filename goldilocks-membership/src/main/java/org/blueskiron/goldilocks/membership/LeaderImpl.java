package org.blueskiron.goldilocks.membership;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import org.blueskiron.goldilocks.api.Configuration;
import org.blueskiron.goldilocks.api.Member;
import org.blueskiron.goldilocks.api.messages.AppendEntriesRequest;
import org.blueskiron.goldilocks.api.messages.AppendEntriesResponse;
import org.blueskiron.goldilocks.api.messages.VoteRequest;
import org.blueskiron.goldilocks.api.messages.VoteResponse;
import org.blueskiron.goldilocks.api.states.Leader;
import org.blueskiron.goldilocks.api.states.State;
import org.blueskiron.goldilocks.leader.election.Heartbeater;
import org.blueskiron.goldilocks.leader.election.LeaseWatch;
import org.blueskiron.goldilocks.membership.messages.AppendEntriesRequestImpl;
import org.blueskiron.goldilocks.membership.messages.VoteResponseImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jzachar
 */
public class LeaderImpl implements Leader {

  private static final Logger LOG = LoggerFactory.getLogger(LeaderImpl.class);
  private final Rafter raft;
  private final CompletableFuture<Member> leaderPromise;
  private final Heartbeater heartbeater;
  private final LeaseWatch leaseWatch;
  private final int leaderTerm;

  private AppendEntriesRequest heartbeatMessage;
  private AppendEntriesRequest leaseRequest;

  private Optional<String> votedFor = Optional.empty();

  public LeaderImpl(Rafter raft, int leaderTerm, ScheduledExecutorService executor, Configuration config) {
    this.raft = raft;
    this.leaderTerm = leaderTerm;
    this.heartbeatMessage =
        new AppendEntriesRequestImpl(raft.getFullyQualifiedId(), leaderTerm, raft.committedLogIndex(),
            Optional.empty(), false);
    this.leaseRequest =
        new AppendEntriesRequestImpl(raft.getFullyQualifiedId(), leaderTerm, raft.committedLogIndex(),
            Optional.empty(), true);
    this.heartbeater = new Heartbeater(this, executor, config.withAppendEntriesPeriod());
    this.leaseWatch = new LeaseWatch(raft, this, executor, config.withAppendEntriesPeriod());
    leaderPromise = CompletableFuture.completedFuture(raft.localMember());
  }

  @Override
  public void begin() {
    broadcastHeartbeat(term(), false);
    heartbeater.start();
    leaseWatch.start();
    raft.onCompletedLeaderPromise(leaderPromise, leaderTerm);
  }

  @Override
  public void end() {
    LOG.debug("Ending leader's term {}", this);
    heartbeater.stop();
    leaseWatch.stop();
    raft.onLeaderless();
  }

  @Override
  public VoteResponse onVoteRequest(VoteRequest voteRequest) {
    VoteResponse response =
        VoteResponseImpl.rejected(voteRequest, raft.getFullyQualifiedId(), term(),
            committedLogIndex());
    if (!votedFor.isPresent() && voteRequest.getCommittedLogIndex() >= committedLogIndex()) {
      heartbeater.stop();
      leaseWatch.stop();
      response =
          VoteResponseImpl.granted(voteRequest, raft.getFullyQualifiedId(), committedLogIndex());
    }
    return response;
  }

  @Override
  public void onAppendEntriesRequest(AppendEntriesRequest request) {
    // TODO: what do we do here?
    // step down
    if (request.getTerm() >= term() && request.getCommittedLogIndex() >= committedLogIndex()) {
      String leaderId = request.getMemberId();
      Member leader = raft.findMember(leaderId);
      stepDown(request.getTerm(), Optional.of(leader));
    }
  }

  @Override
  public void stepDown(int term, Optional<Member> discoveredLeader) {
    //stop these eagerly, don't wait on raft to do so
    heartbeater.stop();
    leaseWatch.stop();
    LOG.info("Leader {} is stepping down...", this);
    raft.becomeFollower(term, new CompletableFuture<Member>(), Optional.empty());
  }

  @Override
  public CompletableFuture<Member> leaderPromise() {
    return leaderPromise;
  }

  @Override
  public int term() {
    return leaderTerm;
  }

  @Override
  public void onAppendEntriesResponse(AppendEntriesResponse response) {
    int followerTerm = response.getTerm();
    if (followerTerm > term()) {
      stepDown(followerTerm, Optional.empty());
    } else {
      // TODO...
    }
  }

  @Override
  public Optional<String> votedFor() {
    return votedFor;
  }

  @Override
  public void broadcastHeartbeat(int term, boolean isLeaseRequest) {
    if (!(term() == term)) {
      LOG.error("Heartbeat term={} does not match internal term={}", term, term());
    } else if (isLeaseRequest) {
      raft.broadcastHeartbeat(leaseRequest);
    } else {
      raft.broadcastHeartbeat(heartbeatMessage);
    }

  }

  @Override
  public AppendEntriesRequest currentLeaseRequest() {
    return leaseRequest;
  }

  @Override
  public long committedLogIndex() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String toString() {
    return String.format("%s[%s][%d]", raft.getFullyQualifiedId(), State.TAGS.LEADER, term());
  }

}
