package org.blueskiron.goldilocks.api.statemachine;

import java.io.Serializable;

/**
 * @author jzachar
 * @param <T> Type of the StateMachine
 */
public interface Command<T> extends Serializable {

    /**
     * @return
     */
    public String getStateMachineId();

    /**
     * @return
     */
    public Class<T> getStateMachineType();

    /**
     * @return
     */
    public Object[] getFunctionParameters();
}
