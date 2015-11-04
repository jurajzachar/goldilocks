package org.blueskiron.goldilocks.membership.net;

import java.nio.ByteBuffer;

import org.blueskiron.goldilocks.membership.net.sbe.EncodersAndDecoders;

import uk.co.real_logic.aeron.Aeron;
import uk.co.real_logic.aeron.Publication;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

public class PublisherWithBuffer {
  private final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(AeronConnector.DEFAULT_BUFFER_SIZE);
  private final UnsafeBuffer direct = new UnsafeBuffer(byteBuffer);
  private final String binding;
  private final String channel;
  private final int streamId;
  private final Aeron aeron;
  private Publication publication;
  private final EncodersAndDecoders helper = new EncodersAndDecoders();

  public PublisherWithBuffer(String binding, int streamId, Aeron aeron) {
    this.binding = binding;
    this.streamId = streamId;
    this.channel = "udp://" + binding;
    this.aeron = aeron;
  }

  private boolean offer(int offset, int encodedLength) {
    boolean success = false;
    long res = publication.offer(direct, offset, 8 + encodedLength);
    if (res < 0L) {
      if (res == Publication.BACK_PRESSURED) {
        AeronConnector.LOG.warn(" Offer failed due to back pressure");
      } else if (res == Publication.NOT_CONNECTED) {
        AeronConnector.LOG
            .error("Offer failed because publisher {} is not yet connected to subscriber", channel);
      } else {
        AeronConnector.LOG.error("Offer failed due to unknown reason");
      }
    } else {
      success = true;
      byteBuffer.clear();
    }
    return success;
  }

  // wrappers
  protected void offer(Object obj, int retries) {
    int length = helper.encode(obj, direct);
    long period = 3;
    while (!offer(0, length) && retries > 0) {
      try {
        AeronConnector.LOG.error("Sleeping...");
        Thread.sleep(period);
      } catch (InterruptedException e) {
        AeronConnector.LOG.error("Failed while waiting out back pressure...", e);
      }
      retries--;
      period +=10;
    }
  }

  protected void addPublication() {
    publication = aeron.addPublication(channel, streamId);
  }

  /**
   * @return the publication
   */
  protected Publication getPublication() {
    return publication;
  }

  /**
   * @return the binding
   */
  public String getBinding() {
    return binding;
  }
}
