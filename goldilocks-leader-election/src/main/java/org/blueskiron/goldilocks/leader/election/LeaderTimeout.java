package org.blueskiron.goldilocks.leader.election;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;

import org.blueskiron.goldilocks.api.Configuration;
import org.blueskiron.goldilocks.api.states.Follower;

/**
 * @author jzachar
 */
public final class LeaderTimeout extends AbstractScheduledWork {
  private static final Random random = new Random();
  private final long timeout;
  private final ScheduledExecutorService scheduler;
  private final Follower follower;
  private final int nextTerm;
  private final Runnable transitionToCandidate;

  public LeaderTimeout(Configuration configuration, Follower follower, ScheduledExecutorService scheduler) {
    this.follower = follower;
    this.scheduler = scheduler;
    this.timeout = generateTimeout(configuration);
    nextTerm = follower.term() + 1;
    transitionToCandidate = () -> {
      LOG.info("Leader timed out on heart beat period after {} millis. Promoting {} to Candidate[{}] state", timeout,
          follower, nextTerm);
      follower.becomeCandidate(nextTerm);
    };
  }

  @Override
  public void start() {
    super.init(scheduler.schedule(transitionToCandidate, timeout, MILLISECONDS));
  }

  public static long generateTimeout(Configuration configuration) {
    long timeout = 300L;
    long appendEntriesPeriod = configuration.withAppendEntriesPeriod();
    long maxLeaderTimeout = configuration.withMaxLeaderTimeout();
    long lowerThreshold = (long) (appendEntriesPeriod * (1 + configuration.withLowerLeaderTimeoutThreshold()));
    long upperThreshold = (long) (maxLeaderTimeout * configuration.withUpperLeaderTimeoutThreshold());
    long randomChunk = (long) (maxLeaderTimeout - (random.nextDouble() * maxLeaderTimeout));
    if (randomChunk < appendEntriesPeriod) {
      randomChunk = appendEntriesPeriod + randomChunk;
    }

    if (randomChunk < lowerThreshold) {
      timeout = lowerThreshold;
    } else if (randomChunk > upperThreshold) {
      timeout = maxLeaderTimeout;
    } else {
      timeout = randomChunk;
    }
    return timeout;
  }

  @Override
  public String toString() {
    return String.format("LeaderTimeout[%s@%d]", follower, timeout);
  }
}
