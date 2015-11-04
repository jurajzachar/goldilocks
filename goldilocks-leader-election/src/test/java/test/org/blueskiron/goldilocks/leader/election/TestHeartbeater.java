package test.org.blueskiron.goldilocks.leader.election;

import org.blueskiron.goldilocks.leader.election.Heartbeater;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TestHeartbeater extends AbstractTest {

    private long heartbeatPeriod;
    private Heartbeater heartbeater;
    
    @Before
    public void setup(){
        super.setup();
        heartbeatPeriod = 100L; //100 millis
        heartbeater = new Heartbeater(getLeader(), getExecutor(), heartbeatPeriod);
    }
    
    @Test
    public void testHeartBeater() throws InterruptedException{
        int expectedHeartbeats = 10;
        LOG.debug("Expecting to capture at least {} heartbeats", expectedHeartbeats);
        heartbeater.start();
        long wakeMeupAfter = expectedHeartbeats * heartbeatPeriod;
        LOG.debug("Sleeping for {}...", wakeMeupAfter);
        Thread.sleep(wakeMeupAfter);
        heartbeater.stop();
        Mockito.verify(getLeader(), Mockito.atLeast(expectedHeartbeats)).broadcastHeartbeat(getTerm(), false);
    }
    
}
