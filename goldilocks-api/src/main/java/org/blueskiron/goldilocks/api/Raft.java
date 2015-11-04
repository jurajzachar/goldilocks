package org.blueskiron.goldilocks.api;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.blueskiron.goldilocks.api.messages.AppendEntriesRequest;
import org.blueskiron.goldilocks.api.messages.AppendEntriesResponse;
import org.blueskiron.goldilocks.api.messages.VoteRequest;
import org.blueskiron.goldilocks.api.messages.VoteResponse;
import org.blueskiron.goldilocks.api.statemachine.Command;
import org.blueskiron.goldilocks.api.statemachine.StateMachine;

/**
 * @author jurajzachar
 *
 */
public interface Raft {

  /**
   * @return
   */
  public <S extends StateMachine<?, ?>, D extends Command<S>> StateMachine<S, D> stateMachine();

  /**
   * @return
   */
  public Membership membership();

  /* Cluster operational calls */
  /**
   * 
   */
  public void start();

  /**
   * 
   */
  public void stop();

  /* Election calls */
  /**
   * @param term
   * @param voteRequest
   * @return
   */
  public CompletableFuture<Set<VoteResponse>> collectVotes(int term, VoteRequest voteRequest);

  /* Leader's lease watch */
  /**
   * @param term
   * @param appendEntriesRequest
   * @return
   */
  public CompletableFuture<Set<AppendEntriesResponse>> collectLeaseResponses(int term,
      AppendEntriesRequest appendEntriesRequest);
}
