package test.org.blueskiron.goldilocks.leader.election;

import org.blueskiron.goldilocks.api.messages.AppendEntriesRequest;
import org.blueskiron.goldilocks.api.messages.AppendEntriesResponse;

public class MockAppendEntriesResponse implements AppendEntriesResponse {

  private final AppendEntriesRequest aerq;
  private final String memberId;
  private final String smId;

  public MockAppendEntriesResponse(String memberId, String smId, AppendEntriesRequest aerq) {
    this.aerq = aerq;
    this.memberId = memberId;
    this.smId = smId;
  }

  @Override
  public String getFullyQualifiedId() {
    return aerq.getFullyQualifiedId();
  }

  @Override
  public String getMemberId() {
    return aerq.getMemberId();
  }

  @Override
  public String getStateMachineId() {
    return smId;
  }

  @Override
  public int getTerm() {
    return aerq.getTerm();
  }

  @Override
  public long getCommittedLogIndex() {
    return 0;
  }

  @Override
  public boolean isSuccess(AppendEntriesRequest request) {
    return request.equals(aerq);
  }

  @Override
  public boolean isLeaseResponse() {
    return aerq.isLeaseRequest();
  }

  @Override
  public String getRespondentsMemberId() {
    return memberId;
  }

  @Override
  public String getFullyQualifiedOriginatorId() {
    return smId + "@" + memberId;
  }

}
