package org.blueskiron.goldilocks.membership;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.blueskiron.goldilocks.api.Member;
import org.blueskiron.goldilocks.api.messages.AppendEntriesRequest;
import org.blueskiron.goldilocks.api.messages.VoteRequest;
import org.blueskiron.goldilocks.api.messages.VoteResponse;
import org.blueskiron.goldilocks.api.states.State;
import org.blueskiron.goldilocks.membership.messages.VoteResponseImpl;

/**
 * @author jzachar
 */
public class Starter implements State {
  private final String fqid;

  public Starter(String fqid) {
    this.fqid = fqid;
  }

  @Override
  public void begin() {

  }

  @Override
  public void end() {

  }

  @Override
  public int term() {
    return -1;
  }

  @Override
  public Optional<String> votedFor() {
    return Optional.empty();
  }

  @Override
  public CompletableFuture<Member> leaderPromise() {
    return new CompletableFuture<Member>();
  }

  @Override
  public VoteResponse onVoteRequest(VoteRequest request) {
    return VoteResponseImpl.granted(request, fqid, committedLogIndex());
  }

  @Override
  public void onAppendEntriesRequest(AppendEntriesRequest request) {
  }

  @Override
  public long committedLogIndex() {
    return 0;
  }

  @Override
  public String toString() {
    return String.format("%s[%s][%d]", fqid, "STARTER", term());
  }

}
