package org.blueskiron.goldilocks.api.states;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.blueskiron.goldilocks.api.Member;
import org.blueskiron.goldilocks.api.messages.AppendEntriesRequest;
import org.blueskiron.goldilocks.api.messages.VoteRequest;
import org.blueskiron.goldilocks.api.messages.VoteResponse;

/**
 * @author jzachar
 */
public interface State {

    public static enum TAGS {
        FOLLOWER,
        CANDIDATE,
        LEADER
    };

    /**
     * 
     */
    void begin();

    /**
     * 
     */
    void end();

    /**
     * @return
     */
    int term();
    
    /**
     * @return
     */
    long committedLogIndex();
    
    /**
     * @return
     */
    Optional<String> votedFor();

    /**
     * @param request
     * @return
     */
    VoteResponse onVoteRequest(VoteRequest request);

    /**
     * @param request
     */
    void onAppendEntriesRequest(AppendEntriesRequest request);

    CompletableFuture<Member> leaderPromise();

    default boolean canTransitionTo(State to) {
        return to.term() > term();
    }
}
