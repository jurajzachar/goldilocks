package org.blueskiron.goldilocks.membership.messages;

import org.blueskiron.goldilocks.api.messages.AppendEntriesRequest;
import org.blueskiron.goldilocks.api.messages.AppendEntriesResponse;

public class AppendEntriesResponseImpl extends AbstractRaftMessage implements AppendEntriesResponse {
  
  private static final String JSON_STR =
      "AERP{\"fqId\":\"%s\", \"originId\":\"%s\", \"term\":\"%d\", \"commitIndex\":\"%d\", \"leaseResponse\":\"%s\"}";
  private final boolean isLeaseResponse;
  private final String originatorCompositeId;

  public AppendEntriesResponseImpl(
      String destinationCompositeId, 
      String originatorCompositeId,
      int term, 
      long commitIndex, 
      boolean isLeaseResponse) {
    super(destinationCompositeId, term, commitIndex);
    this.originatorCompositeId = originatorCompositeId;
    this.isLeaseResponse = isLeaseResponse;
  }

  @Override
  public boolean isSuccess(AppendEntriesRequest request) {
    return getFullyQualifiedId().equals(request.getFullyQualifiedId()) &&
        getTerm() == request.getTerm() &&
        getCommittedLogIndex() == request.getCommittedLogIndex() &&
        isLeaseResponse == request.isLeaseRequest();
  }

  @Override
  public boolean isLeaseResponse() {
    return isLeaseResponse;
  }
  
  @Override
  public String getFullyQualifiedOriginatorId() {
    return originatorCompositeId;
  }
  
  /**
   * @return the originatorCompositeId
   */
  @Override
  public String getRespondentsMemberId() {
   return getMemberIdFromCompositeId(originatorCompositeId);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + (isLeaseResponse ? 1231 : 1237);
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
    if (!(obj instanceof AppendEntriesResponseImpl))
      return false;
    AppendEntriesResponseImpl other = (AppendEntriesResponseImpl) obj;
    if (isLeaseResponse != other.isLeaseResponse)
      return false;
    if (originatorCompositeId == null) {
      if (other.originatorCompositeId != null)
        return false;
    } else if (!originatorCompositeId.equals(other.originatorCompositeId))
      return false;
    return true;
  }
  
  @Override
  public String toString(){
    return String.format(JSON_STR, getFullyQualifiedId(), getRespondentsMemberId(), getTerm(), getCommittedLogIndex(), isLeaseResponse);
  }

}
