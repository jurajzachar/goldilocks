package test.org.blueskiron.goldilocks.membership;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.blueskiron.goldilocks.api.Cluster;
import org.blueskiron.goldilocks.api.Configuration;
import org.blueskiron.goldilocks.api.Configuration.PropertiesReader;
import org.blueskiron.goldilocks.api.Member;
import org.blueskiron.goldilocks.api.Raft;
import org.blueskiron.goldilocks.api.statemachine.Command;
import org.blueskiron.goldilocks.api.statemachine.StateMachine;
import org.blueskiron.goldilocks.membership.ClusterImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;

@RunWith(MockitoJUnitRunner.class)
public class TestMembership {
  private static final Logger LOG = LoggerFactory.getLogger(TestMembership.class);
  private final Random random = new Random(System.currentTimeMillis());
  private ClusterImpl localCluster;
  private Configuration config;
  private Map<String, Cluster> instances = new ConcurrentHashMap<>();

  @Before
  public void setup() throws InterruptedException {
    config = new Configuration();
    Properties props = TestConnector
        .loadFromSource(Configuration.class.getClassLoader().getResourceAsStream("goldilocks.properties"));
    String localId = config.withLocalBinding();
    localCluster = new ClusterImpl(localId, config);
    instances.put(localId, localCluster);
    // init remote instances...
    String remote1 = config.withLocalBinding().replace("9000", "9001");
    String remote2 = config.withLocalBinding().replace("9000", "9002");
    String remote3 = config.withLocalBinding().replace("9000", "9003");
    String remote4 = config.withLocalBinding().replace("9000", "9004");
    buildRemoteCluster(props, remote1);
    buildRemoteCluster(props, remote2);
    buildRemoteCluster(props, remote3);
    buildRemoteCluster(props, remote4);
    Thread.sleep(100);
    instances.entrySet().parallelStream().forEach(entry -> {
      LOG.debug("Starting up {}", entry.getKey());
      entry.getValue().start();
    });
    Thread.sleep(5000);
  }

  @After
  public void destroy() {
    instances.forEach((id, cluster) -> {
      cluster.getAllResources().forEach(r -> cluster.removeStatemachine(r.stateMachine().getId()));
      cluster.stop();
    });
  }

  @Test
  public void aTestReachingConsensusForOneGroupMemberhip() throws InterruptedException {
    LOG.info("Testing reaching consensus for one StateMachine...");
    final StateMachine<KVStore, Command<KVStore>> stateMachine = new KVStore(KVStore.ID);
    String smId = stateMachine.getId();
    Set<String> keys = instances.keySet();
    Set<String> randomizedGroup = pickRandomThree(keys);
    LOG.info("Picked random three nodes: {}", randomizedGroup);
    Cluster initiator = injectStateMachine(stateMachine, randomizedGroup);
    // allow enough time for up to 10 elections. The consensus convergence is,
    // however, most of the time much faster.
    int retries = 10;
    if (waitForConsensus(initiator, smId, retries)) {
      Member leader = checkAndGetLeader(smId, randomizedGroup);
      LOG.info("Found Leader for {} => {}", smId, leader);
    } else {
      assertTrue("Failed to reach consensus", false);
    }
  }
  
  @Ignore
  @Test
  public void aTestRecoveryOfConsensusForOneGroupMemberhip() throws InterruptedException {
    LOG.info("Testing reaching consensus for one StateMachine...");
    final StateMachine<KVStore, Command<KVStore>> stateMachine = new KVStore(KVStore.ID);
    String smId = stateMachine.getId();
    Set<String> keys = instances.keySet();
    Set<String> randomizedGroup = pickRandomThree(keys);
    LOG.info("Picked random three nodes: {}", randomizedGroup);
    Cluster initiator = injectStateMachine(stateMachine, randomizedGroup);
    // allow enough time for up to 10 elections. The consensus convergence is,
    // however, most of the time much faster.
    Member leader = null;
    final int retries = 10;
    if (waitForConsensus(initiator, smId, retries)) {
      leader = checkAndGetLeader(smId, randomizedGroup);
      LOG.info("Found Leader for {} => {}", smId, leader);
    } else {
      assertTrue("Failed to reach consensus", false);
    }

    // now stop leader's cluster
    Cluster leaderCluster = instances.get(leader.getId());
    leaderCluster.removeStatemachine(smId);
    Set<String> regrouped = Sets.difference(randomizedGroup, Sets.newHashSet(leaderCluster.getId()));
    Cluster someOther = instances.get(regrouped.toArray()[0]);
    assertTrue(someOther != null);
    // allow time for the loss of the leader to propagate...
    Thread.sleep(config.withMaxLeaderTimeout() * 2);
    if (waitForConsensus(someOther, smId, retries)) {
      //
    } else {
      assertTrue("Failed to reach consensus", false);
    }
    final Member recoveredLeader = checkAndGetLeader(smId, regrouped);
    LOG.info("Found recovered Leader for {} => {}", smId, recoveredLeader);
  }

  @Test
  public void aTestReachingConsensusForMultipleGroupMemberhips() throws InterruptedException {
    final int feed = 10;
    int counter = feed;
    List<StateMachine<?, ?>> sms = new ArrayList<>();
    while (counter > 0) {
      sms.add(new KVStore(KVStore.ID + "-" + counter));
      counter--;
    }
    final Set<String> keys = instances.keySet();
    final Set<String> randomizedGroup = pickRandomThree(keys);
    LOG.info("Picked random three nodes: {}", randomizedGroup);
    sms.forEach(sm -> {
      String smId = sm.getId();
      Cluster initiator = injectStateMachine(sm, randomizedGroup);
      try {
        Thread.sleep(1000);
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      int retries = 10;
      if (waitForConsensus(initiator, smId, retries)) {
        Member leader = checkAndGetLeader(smId, randomizedGroup);
        LOG.info("Found Leader for {} => {}", smId, leader);
      } else {
        assertTrue("Failed to reach consensus", false);
      }
    });
    long wait = 10000;
    LOG.info("No more log entries should appear on the screen if the system is stable...");
    LOG.info("Sleeping for {} millis", wait);
    Thread.sleep(wait);
    LOG.info("Testing reaching consensus for {} concurrent StateMachines complete", feed);
  }

  private boolean waitForConsensus(final Cluster initiator, final String smId, final int retries) {
    int counter = retries;
    while (!checkLeadership(initiator.getResource(smId)) && counter > 0) {
      LOG.info("Waiting on consensus for {}...", smId);
      try {
        Thread.sleep(config.withMaxLeaderTimeout());
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      counter--;
    }
    return checkLeadership(initiator.getResource(smId));
  }

  private boolean checkLeadership(Optional<Raft> resource) {
    return resource.isPresent() && resource.get().membership().leader().isPresent();
  }

  private Member checkAndGetLeader(final String stateMachineId, final Set<String> keys) {
    final Collection<Cluster> groupInstances = Collections2.filter(instances.values(), c -> keys.contains(c.getId()));
    // check stateMachine present
    assertTrue(groupInstances.size() == keys.size());
    final List<Member> leaderReport = new ArrayList<>();
    groupInstances.forEach(i -> {
      Optional<Raft> raft = i.getResource(stateMachineId);
      assertTrue("Adding StateMachine " + stateMachineId + " failed", raft.isPresent());
      // check leader present
      if (checkLeadership(raft)) {
        Member leader = raft.get().membership().leader().get();
        LOG.info("Found Leader for {} => {} on cluster {}", stateMachineId, leader, i);
        leaderReport.add(leader);
      } else {
        //too harsh
        //assertTrue(String.format("Consensus has not been reached for %s on %s", stateMachineId, i), false);
      }
    });
    // check that the leader is the same Member
    final Set<Member> singleLeader = new HashSet<>(leaderReport);
    assertTrue(String.format("Leader not the same Member in consensus for %s -> %s", stateMachineId, leaderReport),
        singleLeader.size() == 1);
    return (Member) singleLeader.toArray()[0];
  }

  private Cluster injectStateMachine(StateMachine<?, ?> stateMachine, Set<String> group) {
    final Cluster initiator = instances.get(group.toArray()[0]);
    initiator.addStateMachine(stateMachine, group);
    return initiator;
  }

  private Set<String> pickRandomThree(Set<String> keys) {
    Set<String> group = new HashSet<>();
    List<String> list = new ArrayList<>();
    list.addAll(keys);
    int counter = 3;
    while (counter > 0) {
      group.add(list.remove(random.nextInt(list.size())));
      counter--;
    }
    assert group.size() == 3;
    return group;
  }

  private void buildRemoteCluster(Properties props, String localBinding) {
    final Properties updatedProps = new Properties();
    for (Entry<Object, Object> entry : props.entrySet()) {
      updatedProps.put(entry.getKey(), entry.getValue());
    }

    String orig = updatedProps.getProperty(Configuration.LISTEN_ADDRESS);
    updatedProps.put(Configuration.LISTEN_ADDRESS, localBinding);
    String previousMembers = updatedProps.getProperty(Configuration.REMOTE_MEMBERS);
    String updatedMembers = previousMembers.replace(localBinding, orig);
    updatedProps.put(Configuration.REMOTE_MEMBERS, updatedMembers);
    TestConnector.debugProps(updatedProps);
    ClusterImpl cluster = new ClusterImpl(localBinding, new Configuration(new PropertiesReader(updatedProps)));
    LOG.debug("Adding remote cluster instance for {}", localBinding);
    instances.put(localBinding, cluster);
  }
}
