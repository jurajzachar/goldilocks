package org.blueskiron.goldilocks.api.messages;

/**
 * @author jurajzachar
 */
public interface AppendEntriesResponse extends RaftMessage {

  /**
   * @param request
   * @return
   */
  public boolean isSuccess(AppendEntriesRequest request); 

  /**
   * @return
   */
  public boolean isLeaseResponse();
  
  /**
   * @return
   */
  public String getRespondentsMemberId();

  /**
   * @return
   */
  public String getFullyQualifiedOriginatorId();
  
}
