package org.blueskiron.goldilocks.membership;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.blueskiron.goldilocks.api.messages.AppendEntriesRequest;
import org.blueskiron.goldilocks.api.messages.AppendEntriesResponse;
import org.blueskiron.goldilocks.api.messages.VoteRequest;
import org.blueskiron.goldilocks.api.messages.VoteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.functions.Action1;

final class Reactor implements Action1<Object> {

  private static final Logger LOG = LoggerFactory.getLogger(Reactor.class);
  private final Map<String, VoteResponse> collectedVotes = new ConcurrentHashMap<>();
  private final Map<String, AppendEntriesResponse> collectedLeaseResponses =
      new ConcurrentHashMap<>();
  //wired after constructor call
  private  Rafter raft;
  
  //implicit constructor used for wiring
  public Reactor(){
    
  }
  void wire(Rafter raft) {
    this.raft = raft;
  }

  @Override
  public void call(Object event) {
    if (LOG.isTraceEnabled()) {
      LOG.trace("{} reacting on event={}", raft.getFullyQualifiedId(), event);
    }
    if (event instanceof VoteResponse) {
      VoteResponse response = (VoteResponse) event;
      raft.on(response);
      collectedVotes.put(response.getFullyQualifiedOriginatorId(), response);
    } else if (event instanceof VoteRequest) {
      VoteRequest request = (VoteRequest) event;
      raft.on(request);
    } else if (event instanceof AppendEntriesResponse) {
      AppendEntriesResponse response = (AppendEntriesResponse) event;
      if(response.isLeaseResponse()){
       collectedLeaseResponses.put(response.getFullyQualifiedOriginatorId(), response);
      } else {
        raft.on(response);
      }
    } else if (event instanceof AppendEntriesRequest) {
      // cluster.executorService().submit(() -> local.on((AppendEntriesRequest) event));
      raft.on((AppendEntriesRequest) event);
    } else {
      LOG.error("Ignoring unsupported message type: {}", event);
    }
  }

  void resetCollectedVotes() {
    collectedVotes.clear();
  }

  void resetCollectedLeaseResponses() {
    collectedVotes.clear();
  }

  Map<String, VoteResponse> getCollectedVotes() {
    return collectedVotes;
  }

  Map<String, AppendEntriesResponse> getCollectedLeaseResponses() {
    return collectedLeaseResponses;
  }
}
