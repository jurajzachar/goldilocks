package org.blueskiron.goldilocks.api.statemachine;


/**
 * @author jzachar
 */
public class LogEntry implements Sequenced {

    private final int term;
    private final long index;
    private final Command<?> command;

    public LogEntry(int term, long index, Command<?> command) {
        this.term = term;
        this.index = index;
        this.command = command;
    }

    public int getTerm() {
        return term;
    }

    @Override
    public long getIndex() {
        return index;
    }

    @Override
    public Command<?> getEntry() {
        return command;
    }
    
    @Override
    public String toString() {
        return String.format("LogEntry={term=%d, index=%d, command=%s}", term, index, command);
    }
    
}
