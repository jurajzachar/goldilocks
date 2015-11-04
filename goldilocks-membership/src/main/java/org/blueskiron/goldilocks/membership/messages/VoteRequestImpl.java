package org.blueskiron.goldilocks.membership.messages;

import org.blueskiron.goldilocks.api.messages.VoteRequest;

public class VoteRequestImpl extends AbstractRaftMessage implements VoteRequest {
  
  private static final String JSON_STR = "VRQ{\"fqId\":\"%s\", \"term\":\"%d\", \"commitIndex\":\"%d\"}";
  public VoteRequestImpl(String compositeId, int term, long commitIndex) {
    super(compositeId, term, commitIndex);
  }
  
  @Override
  public String toString(){
    return String.format(JSON_STR, getFullyQualifiedId(), getTerm(), getCommittedLogIndex());
  }
}
