package org.blueskiron.goldilocks.api.messages;

import java.util.List;
import java.util.Optional;

import org.blueskiron.goldilocks.api.statemachine.LogEntry;

/**
 * @author jurajzachar
 */
public interface AppendEntriesRequest extends RaftMessage {

  /**
   * @return
   */
  public boolean isLeaseRequest();

  /**
   * @return
   */
  public boolean isHeartbeat();

  /**
   * @return
   */
  public Optional<List<LogEntry>> entries();

}
