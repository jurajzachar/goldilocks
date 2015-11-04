package org.blueskiron.goldilocks.membership.messages;

import java.util.Set;

public class Leave extends AbstractAcknowledgedMessage {

  private static final String JSON_STR =
      "LEAVE{\"stateMachineId\":\"%s\", \"memberId\":\"%s\", \"acks\":\"%s\"}";
  private final String originatorId;
  private transient long absentSince;

  public Leave(String stateMachineId, String originatorId, Set<String> acknowledgements) {
    super(stateMachineId, acknowledgements);
    this.originatorId = originatorId;
  }

  /**
   * @return the originatorId
   */
  public String getOriginatorId() {
    return originatorId;
  }

  /**
   * @return the absentSince
   */
  public long getAbsentSince() {
    return absentSince;
  }

  /**
   * @param absentSince the absentSince to set
   */
  public void setAbsentSince(long absentSince) {
    this.absentSince = absentSince;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result + ((getAcknowledgements() == null) ? 0 : getAcknowledgements().hashCode());
    result = prime * result + ((originatorId == null) ? 0 : originatorId.hashCode());
    result = prime * result + ((getStateMachineId() == null) ? 0 : getStateMachineId().hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof Leave))
      return false;
    Leave other = (Leave) obj;
    if (getAcknowledgements() == null) {
      if (other.getAcknowledgements() != null)
        return false;
    } else if (!getAcknowledgements().equals(other.getAcknowledgements()))
      return false;
    if (originatorId == null) {
      if (other.originatorId != null)
        return false;
    } else if (!originatorId.equals(other.originatorId))
      return false;
    if (getStateMachineId() == null) {
      if (other.getStateMachineId() != null)
        return false;
    } else if (!getStateMachineId().equals(other.getStateMachineId()))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return String.format(JSON_STR, getStateMachineId(), originatorId, getAcknowledgements());
  }
}
