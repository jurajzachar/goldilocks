package org.blueskiron.goldilocks.membership.messages;

import java.util.Set;

public class Rejoin extends AbstractAcknowledgedMessage {

  private static final String JSON_STR =
      "REJOIN{\"memberId\":\"%s\", \"stateMachineId\":\"%s\", \"acks\":\"%s\"}";
  private final String memberId;

  public Rejoin(String memberId, String stateMachineId, Set<String> acknowledgements) {
    super(stateMachineId, acknowledgements);
    this.memberId = memberId;
  }

  /**
   * @return the memberId
   */
  public String getMemberId() {
    return memberId;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((getAcknowledgements() == null) ? 0 : getAcknowledgements().hashCode());
    result = prime * result + ((memberId == null) ? 0 : memberId.hashCode());
    result = prime * result + ((getStateMachineId() == null) ? 0 : getStateMachineId().hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof Rejoin))
      return false;
    Rejoin other = (Rejoin) obj;
    if (getAcknowledgements() == null) {
      if (other.getAcknowledgements() != null)
        return false;
    } else if (!getAcknowledgements().equals(other.getAcknowledgements()))
      return false;
    if (memberId == null) {
      if (other.memberId != null)
        return false;
    } else if (!memberId.equals(other.memberId))
      return false;
    if (getStateMachineId() == null) {
      if (other.getStateMachineId() != null)
        return false;
    } else if (!getStateMachineId().equals(other.getStateMachineId()))
      return false;
    return true;
  }

  @Override
  public String toString(){
    return String.format(JSON_STR, memberId, getStateMachineId(), getAcknowledgements());
  }
}
