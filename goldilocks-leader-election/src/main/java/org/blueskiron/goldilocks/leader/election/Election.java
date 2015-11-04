package org.blueskiron.goldilocks.leader.election;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.blueskiron.goldilocks.api.Member;
import org.blueskiron.goldilocks.api.Membership;
import org.blueskiron.goldilocks.api.Raft;
import org.blueskiron.goldilocks.api.messages.VoteRequest;
import org.blueskiron.goldilocks.api.states.Candidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jzachar
 */
public final class Election extends AbstractScheduledWork {
  private static final Logger LOG = LoggerFactory.getLogger(Election.class);
  private final ScheduledExecutorService executor;
  private final Candidate candidate;
  private final long votingTimeout;
  private final Runnable electionTask;

  public Election(Candidate candidate, Raft raft, long votingTimeout, ScheduledExecutorService executorService) {
    this.candidate = candidate;
    this.executor = executorService;
    this.votingTimeout = votingTimeout;
    final int term = candidate.term();
    electionTask = () -> raft.collectVotes(term, candidate.voteRequest()).thenAccept(votes -> {
      // candidate giving himself a vote
      VoteRequest voteRequest = candidate.voteRequest();
      votes.add(candidate.onVoteRequest(voteRequest));
      LOG.debug("Votes: {}", votes);
      Set<Member> members = votes.stream().filter(vote -> vote.belongsToRequest(voteRequest) && vote.isGranted())
          .map(vote -> raft.membership().getMember(vote.getVoterMemberId()))
          // TODO: ternary operator is very cumbersome
          // here. However,
          // Optional streams are going to be supported in
          // JDK 1.9
          .flatMap(opt -> opt.isPresent() ? Stream.of(opt.get()) : Stream.empty())
          .collect(Collectors.toCollection(HashSet<Member>::new));
      Membership membership = raft.membership();
      if (membership.hasMajority(members)) {
        LOG.info("Electing {} with {} votes as the new Leader for {}", candidate, members.size(),
            raft.stateMachine().getId());
        candidate.becomeLeader(term);
      } else {
        LOG.info(
            "{} does not have enough votes to become a Leader of {}. " + "(received {} but needed at least {} votes)",
            candidate, raft.stateMachine(), members.size(), membership.quorum());
        candidate.repeatElection(term + 1);
      }
    });
  }

  @Override
  public void start() {
    LOG.info("Starting a new {}", this);
    super.init(executor.schedule(electionTask, votingTimeout, MILLISECONDS));
  }

  @Override
  public String toString() {
    return String.format("Election[%s]", candidate);
  }
}
