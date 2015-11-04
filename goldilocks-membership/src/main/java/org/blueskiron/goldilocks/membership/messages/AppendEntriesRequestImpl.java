package org.blueskiron.goldilocks.membership.messages;

import java.util.List;
import java.util.Optional;

import org.blueskiron.goldilocks.api.messages.AppendEntriesRequest;
import org.blueskiron.goldilocks.api.statemachine.LogEntry;

public class AppendEntriesRequestImpl extends AbstractRaftMessage implements AppendEntriesRequest {
  
  private static final String JSON_STR =
      "AERQ{\"fqId\":\"%s\", \"term\":\"%d\", \"commitIndex\":\"%d\", \"entries\":\"%d\", \"leaseRequest\":\"%s\"}";
  private final boolean isLeaseRequest;
  private Optional<List<LogEntry>> entries;
  
  public AppendEntriesRequestImpl(String compositeId, 
      int term, 
      long commitIndex,
      Optional<List<LogEntry>> entries,
      boolean isLeaseRequest) {
    super(compositeId, term, commitIndex);
    this.isLeaseRequest = isLeaseRequest;
    this.entries = entries;
  }
  
  @Override
  public boolean isLeaseRequest() {
    return isLeaseRequest;
  }

  @Override
  public boolean isHeartbeat() {
    return (entries == null || !entries.isPresent());
  }

  @Override
  public Optional<List<LogEntry>> entries() {
    return entries;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((entries == null) ? 0 : entries.hashCode());
    result = prime * result + (isLeaseRequest ? 1231 : 1237);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!super.equals(obj))
      return false;
    if (!(obj instanceof AppendEntriesRequestImpl))
      return false;
    AppendEntriesRequestImpl other = (AppendEntriesRequestImpl) obj;
    if (entries == null) {
      if (other.entries != null)
        return false;
    } else if (!entries.equals(other.entries))
      return false;
    if (isLeaseRequest != other.isLeaseRequest)
      return false;
    return true;
  }
  
  @Override
  public String toString(){
    return String.format(JSON_STR, getFullyQualifiedId(), getTerm(), getCommittedLogIndex(), (isHeartbeat()? 0 : entries.get().size()), isLeaseRequest);
  }
}
