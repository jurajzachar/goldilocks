package test.org.blueskiron.goldilocks.membership;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.LongAdder;

import org.blueskiron.goldilocks.api.Configuration;
import org.blueskiron.goldilocks.api.Configuration.PropertiesReader;
import org.blueskiron.goldilocks.api.messages.AppendEntriesRequest;
import org.blueskiron.goldilocks.api.messages.AppendEntriesResponse;
import org.blueskiron.goldilocks.api.messages.VoteRequest;
import org.blueskiron.goldilocks.api.messages.VoteResponse;
import org.blueskiron.goldilocks.membership.ClusterImpl;
import org.blueskiron.goldilocks.membership.net.AeronConnector;
import org.blueskiron.goldilocks.membership.net.Connector;
import org.blueskiron.goldilocks.membership.net.RxBus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.functions.Action1;

public class TestConnector {

  private static final Logger LOG = LoggerFactory.getLogger(TestConnector.class);
  private Connector connA;
  private Configuration confA;
  private Connector connB;
  private Configuration confB;
  private final RxBus bus = new RxBus();
  private final String smId = TestKVStore.STATE_MACHINE_NAME;
  private final ExecutorService executorA = new ScheduledThreadPoolExecutor(2);
  private final ExecutorService executorB = new ScheduledThreadPoolExecutor(2);

  @Before
  public void setup() throws InterruptedException {
    // config related
    confA = new Configuration();
    Properties props =
        loadFromSource(Configuration.class.getClassLoader().getResourceAsStream(
            "goldilocks.properties"));
    LOG.info("ConnectorA config");
    debugProps(props);
    String remoteB = confA.withLocalBinding().replace("9000", "9001");
    String orig = props.getProperty(Configuration.LISTEN_ADDRESS);
    props.put(Configuration.LISTEN_ADDRESS, remoteB);
    String previousMembers = props.getProperty(Configuration.REMOTE_MEMBERS);
    String updatedMembers = previousMembers.replace(remoteB, orig);
    props.put(Configuration.REMOTE_MEMBERS, updatedMembers);
    confB = new Configuration(new PropertiesReader(props));
    LOG.info("ConnectorB config");
    debugProps(props);

    // connector init
    connA = new AeronConnector(bus, confA, executorA);
    connB = new AeronConnector(bus, confB, executorB);
    connA.open();
    connB.open();
  }
  
  @After
  public void destroy() throws InterruptedException {
    connA.close();
    connB.close();
    //Give Media Manager's threads a chance to hang up 
    Thread.sleep(5000);
  }
  
  @Test
  public void test() throws InterruptedException {
    final TestEncodersAndDecoders otherTest = new TestEncodersAndDecoders();
    final LongAdder counter = new LongAdder();
    // Test Raft subscription
    bus.subscribeToRaft(smId, new Action1<Object>() {
      @Override
      public void call(Object event) {
        counter.increment();
        //LOG.debug("Observed: {}", event);
        if (event instanceof AppendEntriesRequest) {
          assertTrue(event.equals(otherTest.appendRequest));
        } else if (event instanceof AppendEntriesResponse) {
          assertTrue(event.equals(otherTest.appendResponse));
        } else if (event instanceof VoteRequest) {
          assertTrue(event.equals(otherTest.voteRequest));
        } else if (event instanceof VoteResponse) {
          assertTrue(event.equals(otherTest.voteResponse));
        } else {
          assertTrue(false);
        }
      }
    });
    
    // Test Membership subscription
    ClusterImpl cluster = Mockito.mock(ClusterImpl.class);
    String clusterId = "testCluster";
    Mockito.doReturn(clusterId).when(cluster).getId();
    bus.subscribeToMembership(clusterId, cluster);

    Thread.sleep(1000);
    int feed = 1000;
    long start = System.currentTimeMillis();
    while (feed > 0) {
      connA.send(confB.withLocalBinding(), otherTest.appendRequest);
      connB.send(confA.withLocalBinding(), otherTest.appendResponse);
      connA.send(confB.withLocalBinding(), otherTest.voteRequest);
      connB.send(confA.withLocalBinding(), otherTest.voteResponse);
// TODO: mock GroupMembershipAware behaviour...      
//      connA.send(confB.withLocalBinding(), otherTest.leave);
//      connB.send(confA.withLocalBinding(), otherTest.rejoin);
//      connA.send(confB.withLocalBinding(), otherTest.join);
      feed--;
    }
    long end = System.currentTimeMillis() - start;
    Thread.sleep(2000);
    LOG.debug(counter.longValue() + " messages were exchanged in " + end + " millis");
  }

  static Properties loadFromSource(InputStream input) {
    Properties props = new Properties();
    try {
      props.load(input);

    } catch (IOException ex) {
      ex.printStackTrace();
    } finally {
      if (input != null) {
        try {
          input.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return props;
  }

  static void debugProps(Properties props) {
    LOG.debug("Cluster={} properties:", props.get(Configuration.LISTEN_ADDRESS));
    for (Entry<Object, Object> entry : props.entrySet()) {
      LOG.debug("{}={}", entry.getKey(), entry.getValue());
    }
  }
}
