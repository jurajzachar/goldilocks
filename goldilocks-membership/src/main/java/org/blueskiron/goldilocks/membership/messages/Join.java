package org.blueskiron.goldilocks.membership.messages;

import java.util.Set;

public class Join extends AbstractAcknowledgedMessage {

  private static final String JSON_STR =
      "JOIN{\"smdId\":\"%s\", \"members\":\"%s\", \"acks\":\"%s\", \"class\":\"%s\"}";
  private final String stateMachineClassName;
  private final String serializedStateMachine;
  private final Set<String> memberIds;
  private transient long memberSince;

  public Join(String stateMachineId, String stateMachineClassName, String serializedStateMachine,
      Set<String> memberIds, Set<String> acknowledgements) {
    super(stateMachineId, acknowledgements);
    this.stateMachineClassName = stateMachineClassName;
    this.serializedStateMachine = serializedStateMachine;
    this.memberIds = memberIds;
  }

  /**
   * @return the memberSince
   */
  public long getMemberSince() {
    return memberSince;
  }

  /**
   * @param memberSince the memberSince to set
   */
  public void setMemberSince(long memberSince) {
    this.memberSince = memberSince;
  }

  /**
   * @return the stateMachineClassName
   */
  public String getStateMachineClassName() {
    return stateMachineClassName;
  }

  /**
   * @return the serializedStateMachine
   */
  public String getSerializedStateMachine() {
    return serializedStateMachine;
  }

  /**
   * @return the memberIds
   */
  public Set<String> getMemberIds() {
    return memberIds;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((getAcknowledgements() == null) ? 0 : getAcknowledgements().hashCode());
    result = prime * result + ((memberIds == null) ? 0 : memberIds.hashCode());
    result =
        prime * result + ((serializedStateMachine == null) ? 0 : serializedStateMachine.hashCode());
    result =
        prime * result + ((stateMachineClassName == null) ? 0 : stateMachineClassName.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof Join))
      return false;
    Join other = (Join) obj;
    if (getAcknowledgements() == null) {
      if (other.getAcknowledgements() != null)
        return false;
    } else if (!getAcknowledgements().equals(other.getAcknowledgements()))
      return false;
    if (memberIds == null) {
      if (other.memberIds != null)
        return false;
    } else if (!memberIds.equals(other.memberIds))
      return false;
    if (serializedStateMachine == null) {
      if (other.serializedStateMachine != null)
        return false;
    } else if (!serializedStateMachine.equals(other.serializedStateMachine))
      return false;
    if (stateMachineClassName == null) {
      if (other.stateMachineClassName != null)
        return false;
    } else if (!stateMachineClassName.equals(other.stateMachineClassName))
      return false;
    return true;
  }
  
  @Override
  public String toString(){
    return String.format(JSON_STR, getStateMachineId(), memberIds, getAcknowledgements(), stateMachineClassName);
  }
}
