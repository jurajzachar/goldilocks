package org.blueskiron.goldilocks.api.states;

import org.blueskiron.goldilocks.api.messages.VoteRequest;

/**
 * @author jzachar
 */
public interface Candidate extends State {

  public VoteRequest voteRequest();

  public void becomeLeader(int term);

  public void repeatElection(int term);

  @Override
  default boolean canTransitionTo(State to) {
    boolean permitted = false;
    if ((to instanceof Leader) || (to instanceof Follower)) {
      permitted = to.term() >= term();
    } else {
      permitted = to.term() > term();
    }
    return permitted;
  }
}
