package org.blueskiron.goldilocks.leader.election;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.blueskiron.goldilocks.api.Member;
import org.blueskiron.goldilocks.api.Membership;
import org.blueskiron.goldilocks.api.Raft;
import org.blueskiron.goldilocks.api.messages.AppendEntriesRequest;
import org.blueskiron.goldilocks.api.states.Leader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LeaseWatch extends AbstractScheduledWork {

  private static final Logger LOG = LoggerFactory.getLogger(LeaseWatch.class);
  // TODO: make me configurable
  private final static int LEASE_MULTIPLIER = 10;
  private final Leader leader;
  private final long leasePeriod;
  private ScheduledExecutorService scheduler;
  private final Runnable leaseWatchTask;

  public LeaseWatch(Raft raft, Leader leader, ScheduledExecutorService scheduler, long heartbeatPeriod) {
    this.leader = leader;
    this.scheduler = scheduler;
    this.leasePeriod = LEASE_MULTIPLIER * heartbeatPeriod;
    leaseWatchTask = () -> {
      LOG.debug("Checking lease for {}", leader);
      final AppendEntriesRequest leaseRequest = leader.currentLeaseRequest();
      final Membership membership = raft.membership();
      raft.collectLeaseResponses(leader.term(), leaseRequest).thenAccept(leaseResponses -> {
        Set<Member> members = leaseResponses.stream()
            .filter(res -> res.isSuccess(leaseRequest) && res.isLeaseResponse())
            .map(res -> membership.getMember(res.getRespondentsMemberId()))
            .flatMap(opt -> opt.isPresent() ? Stream.of(opt.get()) : Stream.empty())
            .collect(Collectors.toCollection(HashSet<Member>::new));

        // this means we are most likely partitioned off and therefore lost the consensus.
        // valid lease is quorum minus one (leader can never grant lease to itself)
        final int validLease = membership.quorum() -1 ;
        if (members.size() < validLease) {
          LOG.info(
              "{} failed to extend its lease for term[{}] with heartbeat. Got {} but needed at least {} responses. "
                  + "Assuming partitioned leader!",
              leader, leader.term(), members.size(), validLease);
          scheduler.execute(() -> leader.stepDown(leader.term(), Optional.empty()));
        } else {
          LOG.debug("Current {} has extended it's lease with {} responses.", leader, members.size());
        }
      });
    };
  }

  public void start() {
    LOG.info("Starting up {}", this);
    super.init(scheduler.scheduleAtFixedRate(leaseWatchTask, leasePeriod, leasePeriod, TimeUnit.MILLISECONDS));
  }
  
  @Override
  public String toString() {
    return String.format("LeaseWatch[%s@%d]", leader, leasePeriod);
  }
}
