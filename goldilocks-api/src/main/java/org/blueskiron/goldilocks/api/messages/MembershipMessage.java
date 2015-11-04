package org.blueskiron.goldilocks.api.messages;

import java.util.Set;

public interface MembershipMessage {

  /**
   * @return
   */
  public String getStateMachineId();

  /**
   * @return
   */
  public Set<String> getAcknowledgements();

}
