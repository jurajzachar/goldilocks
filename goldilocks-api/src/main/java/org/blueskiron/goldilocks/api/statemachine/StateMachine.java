package org.blueskiron.goldilocks.api.statemachine;

import java.io.Serializable;
import java.util.function.BiFunction;

/**
 * @author jzachar
 */
public interface StateMachine<S, C extends Command<S>> extends Serializable {

    /**
     * @return unique identifier of this StateMachine.
     */
    public String getId();
    
    /**
     * lastAppliedIndex is called when deciding which WriteCommands can be replayed during startups.
     * lastAppiliedIndex greater than 0 means this StateMachine has been initialized.
     * @return last applied index
     */
    public Long lastAppliedIndex();

    /**
     * Called when consensus has been reached on a Command. Along with the Command an index is
     * provided to allow persistent StateMachines to save atomically both the Command's updates and
     * the index.
     * @param S - command's parameter
     */
    public BiFunction<Long, C, Boolean> applyFunction();

}
