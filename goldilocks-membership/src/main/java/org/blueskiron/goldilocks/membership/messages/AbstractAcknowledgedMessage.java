package org.blueskiron.goldilocks.membership.messages;

import java.util.Set;

import org.blueskiron.goldilocks.api.messages.MembershipMessage;

import com.google.common.collect.Sets;

public abstract class AbstractAcknowledgedMessage implements MembershipMessage {

  private final String stateMachineId;
  private final Set<String> acknowledgements;

  AbstractAcknowledgedMessage(String stateMachineId, Set<String> tokens) {
    this.stateMachineId =  stateMachineId;
    this.acknowledgements = tokens;
  }

  public void tag(String token) {
    acknowledgements.add(token);
  }

  public boolean isFullyAcknowledged(Set<String> memberIds) {
    return Sets.difference(memberIds, acknowledgements).isEmpty();
  }

  /**
   * @return the stateMachineId
   */
  public String getStateMachineId() {
    return stateMachineId;
  }
  
  /**
   * @return the tokens
   */
  @Override
  public Set<String> getAcknowledgements() {
    return acknowledgements;
  }

}
