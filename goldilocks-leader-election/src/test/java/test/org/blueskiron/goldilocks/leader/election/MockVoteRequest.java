package test.org.blueskiron.goldilocks.leader.election;

import org.blueskiron.goldilocks.api.messages.VoteRequest;

public class MockVoteRequest implements VoteRequest {

  private final String smId;
  private final String memberId;
  private final int term;
  
  public MockVoteRequest(String smId, String memberId, int term) {
    this.smId = smId;  
    this.memberId = memberId;
    this.term = term;
  }
  
  @Override
  public String getFullyQualifiedId() {
    return smId + "@" + memberId;
  }

  @Override
  public String getMemberId() {
    // TODO Auto-generated method stub
    return memberId;
  }

  @Override
  public String getStateMachineId() {
    return smId;
  }

  @Override
  public int getTerm() {
    return term;
  }

  @Override
  public long getCommittedLogIndex() {
    // TODO Auto-generated method stub
    return 0;
  }

}
