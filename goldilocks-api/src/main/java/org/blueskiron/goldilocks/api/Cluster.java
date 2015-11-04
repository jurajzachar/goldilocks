package org.blueskiron.goldilocks.api;

import java.util.Optional;
import java.util.Set;

import org.blueskiron.goldilocks.api.statemachine.StateMachine;

/**
 * @author jurajzachar
 */
public interface Cluster {

  /**
   * Getter for Cluster Id
   * 
   * @return Cluster Id represented by a String
   */
  public String getId();

  /**
   * Getter for Cluster Configuration
   * 
   * @return Cluster configuration
   */
  public Configuration getConfiguration();

  /**
   * Getter for registered StateMachines and their corresponding Memberships via
   * {@link Raft interface}
   * 
   * @return a set of StateMachines
   */
  public Set<Raft> getAllResources();

  /**
   * @param stateMachineId
   *          Unique stateMachineId
   * @return Raft instance that manages registered StateMachine
   */
  public Optional<Raft> getResource(String stateMachineId);

  /**
   * Add a {@link StateMachine} under the management of this Cluster
   * 
   * @param sm
   *          StateMachine
   * @param remoteReplicaIds
   *          a set of remote {@link Member} instances that are expected to form
   *          a membership for the StateMachine
   */
  public void addStateMachine(StateMachine<?, ?> sm, Set<String> remoteReplicaIds);

  /**
   * Remove registered {@link StateMachine} from the management of this Cluster
   * 
   * @param id
   *          StateMachine Id
   */
  public void removeStatemachine(String id);

  /**
   * Start cluster and all its registered {@link StateMachine} instances
   */
  public void start();

  /**
   * Stop cluster and all its registered {@link StateMachine} instances
   */
  public void stop();

  /**
   * Update Cluster configuration. (currently not implemented)
   * 
   * @param configuration
   */
  public void update(Configuration configuration);

}
