package org.blueskiron.goldilocks.membership.messages;

import org.blueskiron.goldilocks.api.messages.VoteRequest;
import org.blueskiron.goldilocks.api.messages.VoteResponse;

public class VoteResponseImpl extends AbstractRaftMessage implements VoteResponse {

  private static final String JSON_STR =
      "VRP{\"fqId\":\"%s\", \"originId\":\"%s\", \"term\":\"%d\", \"commitIndex\":\"%d\", \"granted\":\"%s\"}";
  private final boolean isGranted;
  private final String originatorCompositeId;

  public VoteResponseImpl(String destinationCompositeId, String originatorCompositeId, int term,
      long commitIndex, boolean isGranted) {
    super(destinationCompositeId, term, commitIndex);
    this.isGranted = isGranted;
    this.originatorCompositeId = originatorCompositeId;
  }

  /**
   * Constructor for VoteResponse that grants VoteRequest
   * @param request
   * @param voterId
   * @param commitIndex
   * @return
   */
  public static VoteResponse granted(VoteRequest request, String voterFqId, long commitIndex) {
    return new VoteResponseImpl(request.getFullyQualifiedId(), voterFqId, request.getTerm(),
        commitIndex, true);
  }

  /**
   * Constructor for VoteResponse that rejects VoteRequest
   * @param higherTerm
   * @param requestorFqId
   * @param votingMemberId
   * @param commitIndex
   * @return
   */
  public static VoteResponse rejected(VoteRequest request, String voterFqId, int higherTerm,
      long commitIndex) {
    return new VoteResponseImpl(request.getFullyQualifiedId(), voterFqId, higherTerm, commitIndex,
        false);
  }

  @Override
  public boolean belongsToRequest(VoteRequest request) {
    return request.getFullyQualifiedId().equals(getFullyQualifiedId()) &&
        request.getTerm() == getTerm();
  }

  @Override
  public boolean isGranted() {
    return isGranted;
  }
  
  @Override
  public String getFullyQualifiedOriginatorId() {
    return originatorCompositeId;
  }
  
  @Override
  public String getVoterMemberId() {
    return getMemberIdFromCompositeId(originatorCompositeId);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + (isGranted ? 1231 : 1237);
    result =
        prime * result + ((originatorCompositeId == null) ? 0 : originatorCompositeId.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (!(obj instanceof VoteResponseImpl))
      return false;
    VoteResponseImpl other = (VoteResponseImpl) obj;
    if (isGranted != other.isGranted)
      return false;
    if (originatorCompositeId == null) {
      if (other.originatorCompositeId != null)
        return false;
    } else if (!originatorCompositeId.equals(other.originatorCompositeId))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return String.format(JSON_STR, getFullyQualifiedId(), getFullyQualifiedOriginatorId(), getTerm(),
        getCommittedLogIndex(), isGranted);
  }

}
