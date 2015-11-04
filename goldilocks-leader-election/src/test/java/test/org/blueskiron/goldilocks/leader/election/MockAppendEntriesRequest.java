package test.org.blueskiron.goldilocks.leader.election;

import java.util.List;
import java.util.Optional;

import org.blueskiron.goldilocks.api.messages.AppendEntriesRequest;
import org.blueskiron.goldilocks.api.statemachine.LogEntry;

public class MockAppendEntriesRequest implements AppendEntriesRequest {
  
  private final String smId;
  private final String memberId;
  private final int term;
  
  public MockAppendEntriesRequest(String smId, String memberId, int term) {
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

  @Override
  public boolean isLeaseRequest() {
    return true;
  }

  @Override
  public boolean isHeartbeat() {
    return false;
  }

  @Override
  public Optional<List<LogEntry>> entries() {
    return null;
  }

}
