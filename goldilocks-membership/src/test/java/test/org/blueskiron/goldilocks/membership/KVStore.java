package test.org.blueskiron.goldilocks.membership;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiFunction;

import org.blueskiron.goldilocks.api.statemachine.Command;
import org.blueskiron.goldilocks.api.statemachine.StateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jzachar
 */
public class KVStore implements StateMachine<KVStore, Command<KVStore>> {

  private static final long serialVersionUID = -1256339426190052574L;
  public static final String ID = "KVStore(Test)";
  private static final Logger LOG = LoggerFactory.getLogger(KVStore.class);
  private String id;

  private final SortedMap<Long, Object> kvMap = new TreeMap<>();

  // implicit constructor
  public KVStore() {
    this.id = ID;
  }

  public KVStore(String id) {
    this.id = id;
  }

  public void initialize(Object... params) {
    // expect one parameter only and that should be the string id
    if (params == null || params.length == 0) {
      id = "unnamed-kvstore";
    } else {
      id = params[0].toString();
    }
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public Long lastAppliedIndex() {
    return kvMap.isEmpty() ? 0L : kvMap.lastKey();
  }

  public SortedMap<Long, Object> getKvMap() {
    return new TreeMap<Long, Object>(kvMap);
  }

  @Override
  public BiFunction<Long, Command<KVStore>, Boolean> applyFunction() {
    return new BiFunction<Long, Command<KVStore>, Boolean>() {
      @Override
      public Boolean apply(Long i, Command<KVStore> c) {
        boolean success = false;
        if (c instanceof KVStore.InitialCommand) {
          initialize(c.getFunctionParameters());
          success = true;
        } else {
          try {
            if (id.equals(c.getStateMachineId())) {
              Object param = c.getFunctionParameters()[0]; // we only expect one
                                                           // argument
              kvMap.put(i, param);
              success = true;
            } else {
              LOG.warn("Could not match given Command={} against this StateMachine={}",
                  c.getStateMachineId(), id);
            }

          } catch (Exception e) {
            LOG.error("Eeeek! -->{}", e);
          }
        }
        return success;
      }
    };
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + ((kvMap == null) ? 0 : kvMap.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof KVStore))
      return false;
    KVStore other = (KVStore) obj;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    if (kvMap == null) {
      if (other.kvMap != null)
        return false;
    } else if (!kvMap.equals(other.kvMap))
      return false;
    return true;
  }

  public static class InitialCommand implements Command<KVStore> {

    private static final long serialVersionUID = 1497988550308260756L;
    private final String name;

    public InitialCommand(String name) {
      this.name = name;
    }

    @Override
    public String getStateMachineId() {
      return name;
    }

    @Override
    public Class<KVStore> getStateMachineType() {
      return KVStore.class;
    }

    @Override
    public Object[] getFunctionParameters() {
      return new Object[]{name};
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (!(obj instanceof InitialCommand))
        return false;
      InitialCommand other = (InitialCommand) obj;
      if (name == null) {
        if (other.name != null)
          return false;
      } else if (!name.equals(other.name))
        return false;
      return true;
    }

  }
}
