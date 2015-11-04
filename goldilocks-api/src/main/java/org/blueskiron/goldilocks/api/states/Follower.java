package org.blueskiron.goldilocks.api.states;

/**
 * @author jzachar
 */
public interface Follower extends State {

    /**
     * @param term
     */
    public void becomeCandidate(int term);
}
