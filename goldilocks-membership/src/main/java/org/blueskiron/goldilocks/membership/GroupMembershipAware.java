package org.blueskiron.goldilocks.membership;

import org.blueskiron.goldilocks.api.messages.MembershipMessage;
import org.blueskiron.goldilocks.api.messages.RaftMessage;
import org.blueskiron.goldilocks.api.statemachine.StateMachine;

public interface GroupMembershipAware {

  /**
   * Raft protocol-specific invocation on incoming RaftMessage that cannot be
   * attributed to any registered {@link StateMachine}
   * 
   * @param event
   *          RaftMessage
   */
  public void onRaftMessage(RaftMessage event);

  /**
   * Membership-specific invocation on incoming MembershipMessage that cannot be
   * attributed to any registered {@link StateMachine}
   * 
   * @param event
   *          RaftMessage
   */
  public void onMembershipMessage(MembershipMessage event);
}
