package org.blueskiron.goldilocks.api.states;

import java.util.Optional;

import org.blueskiron.goldilocks.api.Member;
import org.blueskiron.goldilocks.api.messages.AppendEntriesRequest;
import org.blueskiron.goldilocks.api.messages.AppendEntriesResponse;

/**
 * @author jzachar
 *
 */
public interface Leader extends State {

    /**
     * @param term
     * @param isLeaseRequest
     */
    public void broadcastHeartbeat(int term, boolean isLeaseRequest);

    /**
     * @param response
     */
    public void onAppendEntriesResponse(AppendEntriesResponse response);
    /**
     * @param term
     * @param discoveredLeader
     */
    void stepDown(int term, Optional<Member> discoveredLeader);

    @Override
    default boolean canTransitionTo(State to) {
        boolean permitted = false;
        if (to instanceof Follower && to.term() >= term()) {
            permitted = true;
        }
        return permitted;
    }

    /**
     * @return
     */
    public AppendEntriesRequest currentLeaseRequest();

}
