package org.blueskiron.goldilocks.membership;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.blueskiron.goldilocks.api.Configuration;
import org.blueskiron.goldilocks.api.Member;
import org.blueskiron.goldilocks.api.messages.AppendEntriesRequest;
import org.blueskiron.goldilocks.api.messages.VoteRequest;
import org.blueskiron.goldilocks.api.messages.VoteResponse;
import org.blueskiron.goldilocks.api.states.Candidate;
import org.blueskiron.goldilocks.api.states.State;
import org.blueskiron.goldilocks.leader.election.Election;
import org.blueskiron.goldilocks.leader.election.LeaderTimeout;
import org.blueskiron.goldilocks.membership.messages.VoteRequestImpl;
import org.blueskiron.goldilocks.membership.messages.VoteResponseImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jzachar
 */
class CandidateImpl implements Candidate {

  private static final Logger LOG = LoggerFactory.getLogger(CandidateImpl.class);
  private final Rafter raft;
  private final Configuration config;
  private final VoteRequest voteRequest;
  private final CompletableFuture<Member> leaderPromise = new CompletableFuture<>();
  private Election election;

  private Optional<String> votedFor = Optional.empty();
  private final int term;

  CandidateImpl(Rafter raft, int term, Configuration config) {
    this.raft = raft;
    this.term = term;
    this.config = config;
    long votingTimeout = config.withVotingTimeout();
    voteRequest = new VoteRequestImpl(raft.getFullyQualifiedId(), term, raft.committedLogIndex());
    election = new Election(this, raft, votingTimeout, raft.executorService());
  }

  @Override
  public void begin() {
    votedFor = Optional.of(raft.localMember().getId());
    broadcastVoteRequest();
    election.start();
  }

  @Override
  public void end() {
    election.stop();
  }

  @Override
  public VoteResponse onVoteRequest(VoteRequest voteRequest) {
    String localFqId = raft.getFullyQualifiedId();
    VoteResponse response = VoteResponseImpl.rejected(voteRequest, localFqId, term, raft.committedLogIndex());
    // check if self voting
    if (this.voteRequest.equals(voteRequest)) {
      response =  VoteResponseImpl.granted(voteRequest, localFqId, raft.committedLogIndex());
    } else {
      long candidateLogIndex = voteRequest.getCommittedLogIndex();
      String candidateId = voteRequest.getMemberId();
      int candidateTerm = voteRequest.getTerm();
      if (!votedFor.isPresent() && (candidateTerm > term || candidateLogIndex >= raft.committedLogIndex())) {
        votedFor = Optional.of(candidateId);
        response =  VoteResponseImpl.granted(voteRequest, localFqId, raft.committedLogIndex());
      }
    }
    return response;
  }

  @Override
  public void onAppendEntriesRequest(AppendEntriesRequest request) {
    // another leader has already been elected...
    int leaderTerm = request.getTerm();
    String leaderId = request.getMemberId();
    if (leaderTerm >= term) {
      votedFor = Optional.empty();
    }
    Member leader = raft.findMember(leaderId);
    raft.becomeFollower(leaderTerm, CompletableFuture.completedFuture(leader), votedFor);
    // TODO: what goes in this message?
    if (!request.isHeartbeat()) {
      raft.on(request);
    }
  }

  @Override
  public CompletableFuture<Member> leaderPromise() {
    return leaderPromise;
  }

  @Override
  public Optional<String> votedFor() {
    return Optional.of(raft.getFullyQualifiedId());
  }

  @Override
  public void becomeLeader(int term) {
    LOG.debug("{} is trying to become Leader for term [{}]", raft.getFullyQualifiedId(), term);
    raft.becomeLeader(term);
  }

  // when a split vote occurs candidate holds a new election for the next term
  // this call is scheduled for a newly randomized timeout
  @Override
  public void repeatElection(int term) {
    LOG.debug("Repeating election for {} ...", raft.getFullyQualifiedId());
    votedFor = Optional.empty();
    long timeout = LeaderTimeout.generateTimeout(config);
    raft.executorService().schedule(() -> raft.becomeCandidate(term), timeout, MILLISECONDS);
  }

  private void broadcastVoteRequest() {
    raft.broadcastVoteRequest(voteRequest);
  }

  @Override
  public VoteRequest voteRequest() {
    return voteRequest;
  }

  @Override
  public long committedLogIndex() {
    return raft.committedLogIndex();
  }

  @Override
  public int term() {
    return term;
  }

  @Override
  public String toString() {
    return String.format("%s[%s][%d]", raft.getFullyQualifiedId(), State.TAGS.CANDIDATE, term);
  }
}
