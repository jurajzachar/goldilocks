package test.org.blueskiron.goldilocks.leader.election;

import org.blueskiron.goldilocks.api.messages.VoteRequest;
import org.blueskiron.goldilocks.api.messages.VoteResponse;

public class MockVoteResponse implements VoteResponse {
  
  private final String originatorId;
  private final VoteRequest vrq;
  private final boolean granted;
  
  public MockVoteResponse(VoteRequest vrq, String originatorId, boolean granted){
    this.originatorId = originatorId;
    this.vrq = vrq;
    this.granted = granted;
  }
  
  @Override
  public String getFullyQualifiedId() {
    return vrq.getFullyQualifiedId();
  }

  @Override
  public String getMemberId() {
    // TODO Auto-generated method stub
    return vrq.getMemberId();
  }

  @Override
  public String getStateMachineId() {
    return vrq.getStateMachineId();
  }

  @Override
  public int getTerm() {
    return vrq.getTerm();
  }

  @Override
  public long getCommittedLogIndex() {
    return vrq.getCommittedLogIndex();
  }

  @Override
  public boolean belongsToRequest(VoteRequest request) {
    return request.equals(vrq);
  }

  @Override
  public boolean isGranted() {
    return granted;
  }

  @Override
  public String getVoterMemberId() {
    return originatorId;
  }

  @Override
  public String getFullyQualifiedOriginatorId() {
    return vrq.getStateMachineId() + "@" + originatorId;
  }

}
