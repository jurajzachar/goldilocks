package org.blueskiron.goldilocks.api.messages;

/**
 * @author jzachar
 */
public interface VoteResponse extends RaftMessage {

  /**
   * @param request
   * @return
   */
  public boolean belongsToRequest(VoteRequest request);
  /**
   * @return
   */
  public boolean isGranted();
  
  /**
   * @return
   */
  public String getVoterMemberId();
  
  /**
   * @return
   */
  public String getFullyQualifiedOriginatorId();
}
