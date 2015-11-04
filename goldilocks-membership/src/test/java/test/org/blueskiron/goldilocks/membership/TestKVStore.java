package test.org.blueskiron.goldilocks.membership;

import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.LongAdder;

import org.blueskiron.goldilocks.api.statemachine.Command;
import org.blueskiron.goldilocks.api.statemachine.StateMachine;
import org.junit.Before;
import org.junit.Test;

public class TestKVStore {
    
    static final String STATE_MACHINE_NAME = "TestKVStore";
    private final StateMachine<KVStore, Command<KVStore>> sm = new KVStore();
    private final LongAdder la = new LongAdder();
    private final List<Command<KVStore>> commands = new LinkedList<Command<KVStore>>();
   
    @Before
    public void setup() {
        commands.add(new KVStore.InitialCommand(STATE_MACHINE_NAME));
        int size = 10;
        while (size > 0) {
            commands.add(generateCommand(STATE_MACHINE_NAME));
            size--;
        }
    }

    @Test
    public void test() {
        Command<KVStore> initial = commands.remove(0);
        sm.applyFunction().apply(0L, initial);
        commands.forEach(c -> {
            final long index = la.longValue();
            sm.applyFunction().apply(index, c);
            la.increment();
        });
        KVStore kvsm = (KVStore) sm;
        kvsm.getKvMap().forEach((k, v) -> System.out.println(k + " --> " + v));
        assertTrue(kvsm.getKvMap().size() == 10);
    }

    private static Command<KVStore> generateCommand(String smName) {
        return new Command<KVStore>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getStateMachineId() {
                return smName;
            }

            @Override
            public Class<KVStore> getStateMachineType() {
                return KVStore.class;
            }

            @Override
            public Object[] getFunctionParameters() {
                return new Object[]{UUID.randomUUID().toString()};
            }
        };
    }

    /**
     * @return the sm
     */
    public StateMachine<KVStore, Command<KVStore>> getStateMachine() {
      return sm;
    }

    /**
     * @return the commands
     */
    public List<Command<KVStore>> getCommands() {
      return commands;
    }
}
