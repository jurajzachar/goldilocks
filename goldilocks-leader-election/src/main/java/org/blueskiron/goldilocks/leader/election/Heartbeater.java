package org.blueskiron.goldilocks.leader.election;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.blueskiron.goldilocks.api.states.Leader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jzachar
 */
public class Heartbeater extends AbstractScheduledWork {

  private static final Logger LOG = LoggerFactory.getLogger(Heartbeater.class);
  // TODO: make this configurable
  private final static int LEASE_MULTIPLIER = 10;
  private final long heartbeatPeriod;
  private ScheduledExecutorService scheduler;
  private final Runnable heartbeatHandle;
  private final Runnable leaseHandle;
  private final String id; //debug purposes

  public Heartbeater(Leader leader, ScheduledExecutorService scheduler, long heartbeatPeriod) {
    this.heartbeatPeriod = heartbeatPeriod;
    this.scheduler = scheduler;
    this.id = leader.toString();
    heartbeatHandle = () -> leader.broadcastHeartbeat(leader.term(), false);
    leaseHandle = () -> leader.broadcastHeartbeat(leader.term(), true);
  }

  public void start() {
    super.init( scheduler.scheduleAtFixedRate(heartbeatHandle, 0,
            heartbeatPeriod, TimeUnit.MILLISECONDS),
            scheduler.scheduleAtFixedRate(leaseHandle, 0,
                heartbeatPeriod * LEASE_MULTIPLIER, TimeUnit.MILLISECONDS));
    LOG.info("Starting up {}", this);
  }
  
  @Override
  public String toString() {
    return String.format("Heartbeater[%s]", id);
  }
}
