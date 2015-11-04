package org.blueskiron.goldilocks.membership;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.RuntimeErrorException;

import org.blueskiron.goldilocks.api.Cluster;
import org.blueskiron.goldilocks.api.Configuration;
import org.blueskiron.goldilocks.api.Member;
import org.blueskiron.goldilocks.api.Raft;
import org.blueskiron.goldilocks.api.messages.MembershipMessage;
import org.blueskiron.goldilocks.api.messages.RaftMessage;
import org.blueskiron.goldilocks.api.statemachine.StateMachine;
import org.blueskiron.goldilocks.membership.messages.Join;
import org.blueskiron.goldilocks.membership.messages.Leave;
import org.blueskiron.goldilocks.membership.messages.Rejoin;
import org.blueskiron.goldilocks.membership.net.AeronConnector;
import org.blueskiron.goldilocks.membership.net.Connector;
import org.blueskiron.goldilocks.membership.net.NamedPoolThreadFactory;
import org.blueskiron.goldilocks.membership.net.RxBus;
import org.blueskiron.goldilocks.membership.net.sbe.EncodersAndDecoders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * @author jzachar
 */
public class ClusterImpl implements Cluster, GroupMembershipAware {

  private static final Logger LOG = LoggerFactory.getLogger(ClusterImpl.class);
  private final String id;
  private final Configuration configuration;
  private final AtomicReference<LocalMember> localMember = new AtomicReference<>();
  private final Set<Member> remoteMembers = new HashSet<>();
  private final RxBus bus;
  private final Connector connector;
  private final ScheduledExecutorService executor;
  private final AtomicReference<Boolean> isStarted = new AtomicReference<Boolean>(false);

  public ClusterImpl(String id, Configuration configuration) {
    if (configuration == null) {
      throw new RuntimeErrorException(null, "Cannot proceed without a valid Configuration");
    }
    executor =
        new ScheduledThreadPoolExecutor(configuration.withClusterWorkers(), new NamedPoolThreadFactory("Cluster-Worker", true));
    this.id = id;
    this.configuration = configuration;
    bus = new RxBus();
    configuration.withRemoteMembers().forEach(rm -> remoteMembers.add(new RemoteMember(rm)));
    connector = new AeronConnector(bus, configuration, executor);
    localMember.set(new LocalMember(configuration.withLocalBinding()));
  }

  @Override
  public void addStateMachine(StateMachine<?, ?> sm, Set<String> replicaIds) {
    String smId = sm.getId();
    LOG.info("Adding StateMachine={} to Cluster={}", smId, this);
    String serializedStateMachine = EncodersAndDecoders.mapper.writeValueAsString(sm);
    Join join =
        new Join(sm.getId(), sm.getClass().getName(), serializedStateMachine, replicaIds,
            Sets.newHashSet(getLocalMemberId()));
    getRemoteMembersView(replicaIds)
    .forEach(remote -> connector.send(remote.getId(), join));
  }

  @Override
  public void removeStatemachine(String stateMachineId) {
    bus.unsubscribe(stateMachineId);
    Optional<Raft> resource = localMember().getResource(stateMachineId);
    resource.ifPresent(r -> r.stop());
    localMember().removeResource(stateMachineId);

  }

  @Override
  public void start() {
    while (isStarted.compareAndSet(false, true)) {
      LOG.info("Starting up cluster on {}", localMember);
      connector.update(configuration);
      connector.open();
      localMember().startAllResources();
      bus.subscribeToMembership(getId(), this);
    }
  }

  @Override
  public void stop() {
    while (isStarted.compareAndSet(true, false)) {
      LOG.info("Stopping cluster on {}", localMember);
      bus.unsubscribeFromAll();
      localMember.get().stopAllResources();
      connector.close();
    }
  }
  
  @Override
  public void onMembershipMessage(MembershipMessage event) {
    // JOIN
    if (event instanceof Join) {
      Join join = (Join) event;
      if (!localMember().getResource(join.getStateMachineId()).isPresent()) {
        on(join);
      }
    } else if (event instanceof Rejoin) {
      on((Rejoin) event);
    } else if (event instanceof Leave) {
      on((Leave) event);
    }
  }
  
  @Override
  public void onRaftMessage(RaftMessage event) {
    String unknownSmId = event.getStateMachineId();
    String localId = getLocalMemberId();
    if (!localMember().getResource(unknownSmId).isPresent()) {
      LOG.debug("{} must REJOIN", localId);
      // Rejoin rejoin = new Rejoin(localId, unknownSmId, Sets.newHashSet(localId));
      // broadcast Rejoin to all
      // configuration.withRemoteMembers().forEach(remote -> connector.send(remote, rejoin));
    } else {
      LOG.info("StateMachine {} already under group membership. Doing nothing.", unknownSmId);
    }
  }
  
  @Override
  public void update(Configuration configuration) {
    // TODO Auto-generated method stub
    //dynamic configuration updates are currently not supported.
  }
  
  @Override
  public Set<Raft> getAllResources() {
    Set<Raft> resources = new HashSet<>();
    resources.addAll(localMember().getAllResources());
    return resources;
  }
  
  @Override
  public Optional<Raft> getResource(String stateMachineId){
    return localMember().getResource(stateMachineId);
  }
  
  @Override
  public Configuration getConfiguration() {
    return configuration;
  }
  
  @Override
  public String getId() {
    return id;
  }
  
  public boolean isStarted() {
    return isStarted.get();
  }

  private StateMachine<?, ?> instantiateStateMachine(Join join) {
    LOG.info("Trying to instantiate incoming StateMachine={}", join.getStateMachineId());
    StateMachine<?, ?> sm = null;
    try {
      Class<?> klazz = Class.forName(join.getStateMachineClassName());
      sm =
          (StateMachine<?, ?>) EncodersAndDecoders.mapper.readValue(
              join.getSerializedStateMachine(), klazz);
    } catch (ClassNotFoundException e) {
      LOG.error("Could not instantiate incoming StateMachine, error thrown: ", e);
    }
    return sm;
  }

  private void addStateMachine(StateMachine<?, ?> sm, Join join) {
    String smId = sm.getId();
    LOG.info("Adding StateMachine={} to cluster={}", smId, this);
    Reactor reactor = new Reactor();
    Rafter raft = new Rafter(this, reactor, sm, join);
    reactor.wire(raft);
    localMember().addResource(raft);
    bus.subscribeToRaft(smId, reactor);
    if (isStarted.get()) {
      raft.start();
    }
  }

  private void on(Join join) {
    String joinSmId = join.getStateMachineId();
    if (join.isFullyAcknowledged(join.getMemberIds())) {
      StateMachine<?, ?> sm = instantiateStateMachine(join);
      if (sm != null && joinSmId.equals(sm.getId())) {
        addStateMachine(sm, join);
        //broadcast join to all for the last time
        getRemoteMembersView(join.getMemberIds()).forEach(member -> connector.send(member.getId(), join));
      } else {
        LOG.error("Failed to join StateMachine {} from the Join message", sm,
            join.getStateMachineId());
      }
    } else {
      join.tag(getLocalMemberId());
      Set<String> remaining = Sets.difference(join.getMemberIds(), join.getAcknowledgements());
      if(remaining.isEmpty()){
        on(join);
      }else{
        getRemoteMembersView(remaining)
        .forEach(member -> connector.send(member.getId(), join));  
      }
    }
  }

  private void on(Rejoin rejoin) {
    // TODO
  }

  private void on(Leave leave) {
    // TODO
  }

  void broadcast(Object obj, Set<Member> destinations) {
    destinations.forEach(remote -> connector.send(remote.getId(), obj));
  }

  // Shared resources
  ScheduledExecutorService executorService() {
    return executor;
  }

  LocalMember localMember() {
    return localMember.get();
  }

  Connector connector() {
    return connector;
  }
  
//  Set<Member> getMembersByIds(Set<String> ids){
//    return Sets.filter(remoteMembers, member -> ids.contains(member.getId()));
//  }
  
  Set<Member> getRemoteMembersView(Set<String> ids) {
    Set<String> keys = Sets.difference(ids, Sets.newHashSet(getLocalMemberId()));
    return Sets.filter(remoteMembers, member -> keys.contains(member.getId()));
  }

  private String getLocalMemberId() {
    return localMember().getId();
  }

  @Override
  public String toString() {
    return String.format("%s@[%s]", id, getLocalMemberId());
  }

}
