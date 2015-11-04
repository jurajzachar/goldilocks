package org.blueskiron.goldilocks.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author jurajzachar
 */
public class Configuration {

  public static final String CONF_PREFIX = "goldilocks";
  public static final String MAX_LEADER_TIMEOUT = CONF_PREFIX + ".leader.max-timeout";
  public static final String LOWER_LEADER_TIMEOUT_THRESHOLD = CONF_PREFIX + ".leader.lower-threshold-timeout";
  public static final String UPPER_LEADER_TIMEOUT_THRESHOLD = CONF_PREFIX + ".leader.upper-threshold-timeout";
  public static final String VOTING_TIMEOUT = CONF_PREFIX + ".election.voting-timeout";
  public static final String CLUSTER_WORKERS = CONF_PREFIX + ".cluster.workers";
  public static final String WRITE_TIMEOUT = CONF_PREFIX + ".write-timeout";
  public static final String APPEND_ENTRIES_PERIOD = CONF_PREFIX + ".append-entries.period";
  public static final String APPEND_ENTRIES_WORKERS = CONF_PREFIX + ".append-entries.workers";
  public static final String LISTEN_ADDRESS = CONF_PREFIX + ".listen-address";
  public static final String REMOTE_MEMBERS = CONF_PREFIX + ".remote-members";

  private final PropertiesReader reader;

  /**
   * unit test only
   */
  public Configuration() {
    reader = new PropertiesReader(
        Configuration.class.getClassLoader().getResourceAsStream(CONF_PREFIX + ".properties"));
  }

  public Configuration(PropertiesReader reader) {
    this.reader = reader;
  }

  public void reload() {
    reader.clear();
    reader.load();
  }

  public long withMaxLeaderTimeout() {
    return Long.valueOf(reader.read(MAX_LEADER_TIMEOUT).toString());
  }

  public double withLowerLeaderTimeoutThreshold() {
    return Double.valueOf(reader.read(LOWER_LEADER_TIMEOUT_THRESHOLD).toString());
  }

  public double withUpperLeaderTimeoutThreshold() {
    return Double.valueOf(reader.read(UPPER_LEADER_TIMEOUT_THRESHOLD).toString());
  }

  public long withVotingTimeout() {
    return Long.valueOf(reader.read(VOTING_TIMEOUT).toString());
  }

  public int withClusterWorkers() {
    return Integer.valueOf(reader.read(CLUSTER_WORKERS).toString());
  }

  public long withWriteTimeout() {
    return Long.valueOf(reader.read(WRITE_TIMEOUT).toString());
  }

  public long withAppendEntriesPeriod() {
    return Long.valueOf(reader.read(APPEND_ENTRIES_PERIOD).toString());
  }

  public int withAppendEntriesWorkers() {
    return Integer.valueOf(reader.read(APPEND_ENTRIES_WORKERS).toString());
  }

  public String withLocalBinding() {
    return reader.read(LISTEN_ADDRESS).toString();
  }

  // comma separated list of 'hostname:port' entries
  public List<String> withRemoteMembers() {
    List<String> members = new ArrayList<>();
    String[] entries = reader.read(REMOTE_MEMBERS).toString().split(",");
    for (int i = 0; i < entries.length; i++) {
      members.add(entries[i].trim());
    }
    return members;
  }

  public static class PropertiesReader {
    private final Properties props;
    private InputStream input;

    public PropertiesReader(Properties props) {
      this.props = props;
    }

    public PropertiesReader(InputStream inputStream) {
      this.input = inputStream;
      props = new Properties();
      load();
    }

    private void clear() {
      props.clear();
    }

    private void load() {
      try {
        props.load(input);

      } catch (IOException ex) {
        ex.printStackTrace();
      } finally {
        if (input != null) {
          try {
            input.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((props == null) ? 0 : props.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (!(obj instanceof PropertiesReader))
        return false;
      PropertiesReader other = (PropertiesReader) obj;
      if (props == null) {
        if (other.props != null)
          return false;
      } else if (!props.equals(other.props))
        return false;
      return true;
    }

    Object read(String key) {
      return props.get(key);
    }
  }
}
