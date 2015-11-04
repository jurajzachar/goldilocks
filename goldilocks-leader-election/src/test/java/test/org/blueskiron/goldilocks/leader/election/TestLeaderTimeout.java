package test.org.blueskiron.goldilocks.leader.election;

import org.blueskiron.goldilocks.api.Configuration;
import org.blueskiron.goldilocks.leader.election.LeaderTimeout;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestLeaderTimeout extends AbstractTest {

  private LeaderTimeout leaderTimeout;
  private Configuration config;

  @Before
  public void setup() {
    super.setup();
    // read config
    config = new Configuration();
    leaderTimeout = new LeaderTimeout(config, getFollower(), getExecutor());
  }

  @After
  public void destroy() {
    leaderTimeout = null;
  }

  @Test
  public void testLeaderTimeoutTrigger() {
    leaderTimeout.start();
    LOG.info("Testing LeaderTimeout will trigger promotion to CANDIDATE state.");
    sleep(config.withMaxLeaderTimeout() + 100);
    Mockito.verify(getFollower(), Mockito.times(1)).becomeCandidate(getTerm());
    leaderTimeout.stop();
    // assertTrue("LeaderTimeout failed to stop.", !leaderTimeout.isScheduled());
  }

  //
  // @Test
  public void testLeaderTimeoutReset() {
    leaderTimeout.start();
    LOG.info("Testing LeaderTimeout will reset its internal timer.");
    LOG.debug("Reseting LeaderTimeout after " + config.withAppendEntriesPeriod() + " millis.");
    // sleep for the duration of the heartbeat period
    sleep(config.withAppendEntriesPeriod());
    leaderTimeout.reset();
    Mockito.verify(getFollower(), Mockito.times(0)).becomeCandidate(getTerm());
    // now expect a full leaderTimeout period
    sleep((long) (config.withMaxLeaderTimeout() * (config.withUpperLeaderTimeoutThreshold())));
    Mockito.verify(getFollower(), Mockito.times(1)).becomeCandidate(getTerm());
    leaderTimeout.stop();
  }

}
