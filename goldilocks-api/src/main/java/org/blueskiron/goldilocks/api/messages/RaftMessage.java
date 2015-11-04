package org.blueskiron.goldilocks.api.messages;


public interface RaftMessage {
  
  /**
   * @return composite id consisting of form "stateMachineId\@memberId:port"
   */
  public String getFullyQualifiedId();
  
  /**
   * @return memberId origin of this RaftMessage
   */
  public String getMemberId();
  
  /**
   * @return stateMachineId to which this RaftMessage belongs to
   */
  public String getStateMachineId();
  
  /**
   * @return term epoch of this RaftMessage
   */
  public int getTerm();

  /**
   * @return commitIndex last committed LogEntry index 
   */
  public long getCommittedLogIndex();

}
