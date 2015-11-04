package org.blueskiron.goldilocks.membership.net;

import org.blueskiron.goldilocks.api.Configuration;

/**
 * @author jzachar
 */
public interface Connector {

  /**
     * 
     */
  public void open();

  /**
     * 
     */
  public void close();

  /**
   * @param configuration
   */
  public void update(Configuration configuration);

  /**
   * @param binding
   * @param message
   */
  public void send(String binding, Object message);

}
