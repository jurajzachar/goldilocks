package org.blueskiron.goldilocks.membership.messages;

import org.blueskiron.goldilocks.api.messages.RaftMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractRaftMessage implements RaftMessage {

  static final Logger LOG = LoggerFactory.getLogger(RaftMessage.class);

  protected static final String COMPOSITE_ID_FORMAT = "%s@%s";
  protected static final String ERR_MSG =
      "Expected memberId of the form 'StateMachineId@hostname:port' but instead got: {}";
  private final String stateMachineId;
  private final String memberId;
  private final int term;
  private long commitIndex;

  AbstractRaftMessage(String compositeId, int term, long commitIndex) {
    String[] idTokens = compositeId.split("@");
    String smId = "n/a";
    String memId = "n/a";
    try {
      smId = idTokens[0];
      memId = idTokens[1];
    } catch (Exception e) {
      LOG.error(ERR_MSG, compositeId);
      LOG.error("Thrown error:", e);
    }
    this.stateMachineId = smId;
    this.memberId = memId;
    this.term = term;
    this.commitIndex = commitIndex;
  }
  
  protected String getMemberIdFromCompositeId(String compositeId){
    String[] idTokens = compositeId.split("@");
    String memId = "n/a";
    try {
      memId = idTokens[1];
    } catch (Exception e) {
      LOG.error(ERR_MSG, compositeId);
      LOG.error("Thrown error:", e);
    }
    return memId;
  }
  
  /**
   * @return composite id consisting of form COMPOSITE_ID_FORMAT
   */
  @Override
  public String getFullyQualifiedId() {
    return String.format(COMPOSITE_ID_FORMAT, stateMachineId, memberId);
  }

  /**
   * @return the memberId
   */
  @Override
  public String getMemberId() {
    return memberId;
  }

  @Override
  public String getStateMachineId() {
    return stateMachineId;
  }

  /**
   * @return the term
   */
  @Override
  public int getTerm() {
    return term;
  }

  /**
   * @return the commitIndex
   */
  @Override
  public long getCommittedLogIndex() {
    return commitIndex;
  }
  
  public void setNextCommitedLogIndex(long index){
    commitIndex = index;
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (commitIndex ^ (commitIndex >>> 32));
    result = prime * result + ((memberId == null) ? 0 : memberId.hashCode());
    result = prime * result + ((stateMachineId == null) ? 0 : stateMachineId.hashCode());
    result = prime * result + term;
    return result;
  }
  
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof AbstractRaftMessage))
      return false;
    AbstractRaftMessage other = (AbstractRaftMessage) obj;
    if (commitIndex != other.commitIndex)
      return false;
    if (memberId == null) {
      if (other.memberId != null)
        return false;
    } else if (!memberId.equals(other.memberId))
      return false;
    if (stateMachineId == null) {
      if (other.stateMachineId != null)
        return false;
    } else if (!stateMachineId.equals(other.stateMachineId))
      return false;
    if (term != other.term)
      return false;
    return true;
  }
}
