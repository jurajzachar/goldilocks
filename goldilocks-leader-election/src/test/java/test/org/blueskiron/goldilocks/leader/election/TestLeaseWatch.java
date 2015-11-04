package test.org.blueskiron.goldilocks.leader.election;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.blueskiron.goldilocks.api.messages.AppendEntriesRequest;
import org.blueskiron.goldilocks.api.messages.AppendEntriesResponse;
import org.blueskiron.goldilocks.leader.election.LeaseWatch;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class TestLeaseWatch extends AbstractTest {

  private long heartbeatPeriod;
  private LeaseWatch watch;

  @Before
  public void setup() {
    super.setup();
    heartbeatPeriod = 100L; // 100 millis
    watch = new LeaseWatch(getRaft(), getLeader(), getExecutor(), heartbeatPeriod);
  }

  @Test
  public void testLeaseWatch() throws InterruptedException {
    LOG.debug("Testing capture of AppendEntries lease responses");
    //generate mock responses
    Set<AppendEntriesResponse> responses = new HashSet<>();
    AppendEntriesRequest aerq = getLeader().currentLeaseRequest();
    responses.add(new MockAppendEntriesResponse(BINDING + 1, SM_BINDING, aerq));
    responses.add(new MockAppendEntriesResponse(BINDING + 2, SM_BINDING, aerq));
    LOG.debug("membership --> {}", getRaft().membership().members());
    LOG.debug("responses --> {}", responses);
    Mockito.doReturn(CompletableFuture.completedFuture(responses)).when(getRaft())
            .collectLeaseResponses(getTerm(), getLeaseRequest());
    int heartbeatResponseFactor = 10;
    watch.start();
    long wakeMeupAfter = heartbeatResponseFactor * heartbeatPeriod + 100L;
    LOG.debug("Sleeping for {}...", wakeMeupAfter);
    Thread.sleep(wakeMeupAfter);
    watch.stop();
    Mockito.verify(getLeader(), Mockito.never()).stepDown(getTerm(), Optional.empty());
  }

}
