package org.blueskiron.goldilocks.membership.net;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.blueskiron.goldilocks.api.Configuration;
import org.blueskiron.goldilocks.membership.net.sbe.EncodersAndDecoders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import uk.co.real_logic.aeron.Aeron;
import uk.co.real_logic.aeron.Subscription;
import uk.co.real_logic.aeron.driver.MediaDriver;
import uk.co.real_logic.aeron.driver.ThreadingMode;
import uk.co.real_logic.aeron.exceptions.DriverTimeoutException;
import uk.co.real_logic.aeron.logbuffer.FragmentHandler;
import uk.co.real_logic.aeron.logbuffer.Header;
import uk.co.real_logic.agrona.DirectBuffer;
import uk.co.real_logic.agrona.LangUtil;
import uk.co.real_logic.agrona.concurrent.BusySpinIdleStrategy;
import uk.co.real_logic.agrona.concurrent.IdleStrategy;
import uk.co.real_logic.agrona.concurrent.NoOpIdleStrategy;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

public class AeronConnector implements Connector {
  public static final int DEFAULT_BUFFER_SIZE = 4096;
  protected static final Logger LOG = LoggerFactory.getLogger(AeronConnector.class);
  private static final IdleStrategy OFFER_IDLE_STRATEGY = new BusySpinIdleStrategy();
  private static final int STREAM_ID = 1;
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final RxBus bus;
  private final ExecutorService executor;
  // sending via dedicated Publishers
  private final Map<String, PublisherWithBuffer> publishers = new ConcurrentHashMap<>();
  private final EncodersAndDecoders helper = new EncodersAndDecoders();

  // mutable
  private Configuration configuration;
  private Subscription subscription;
  private MediaDriver mediaDriver;
  private Aeron aeron;

  public AeronConnector(RxBus bus, Configuration configuration, ExecutorService executor) {
    this.bus = bus;
    this.configuration = configuration;
    this.executor = executor;
  }

  private void subscription() {
    String channel = "udp://" + configuration.withLocalBinding();
    // local subscriber
    subscription = aeron.addSubscription(channel, STREAM_ID);
    LOG.info("Local subscription added: {}-{}", channel, STREAM_ID);
  }

  private void publishers() {
    for (String remote : configuration.withRemoteMembers()) {
      PublisherWithBuffer publisher = new PublisherWithBuffer(remote, STREAM_ID, aeron);
      publisher.addPublication();
      publishers.put(remote, publisher);
    }
  }

  @Override
  public void open() {
    if (running.compareAndSet(false, true)) {

      final MediaDriver.Context driverCtx = new MediaDriver.Context();

      // low-latency embedded driver
      driverCtx.threadingMode(ThreadingMode.SHARED)
          .conductorIdleStrategy(OFFER_IDLE_STRATEGY)
          .receiverIdleStrategy(new NoOpIdleStrategy())
          .senderIdleStrategy(new NoOpIdleStrategy());
      driverCtx.dirsDeleteOnStart(true);
      //tmp partition is too small on my computer. Each Aeron instance consumes roughly 500MB!
      String embeddedDirName = "/virt/aeron/aeron_" + configuration.withLocalBinding();
      driverCtx.dirName(embeddedDirName);
      LOG.info("Launching Media Driver at {}", configuration.withLocalBinding());
      try {
        mediaDriver = MediaDriver.launch(driverCtx);
        Aeron.Context aeronCtx = new Aeron.Context();
        aeronCtx.dirName(embeddedDirName);
        aeronCtx.errorHandler(AeronConnector::silentInactivityErrorHandler);
        aeron = Aeron.connect(aeronCtx);
        subscription();
        publishers();
        executor.execute(() -> subscriberLoop(this::onFragment, 10, running).accept(subscription));
      } finally {
        while (!Sets.newHashSet(configuration.withRemoteMembers()).containsAll(publishers.keySet()))
          ;
        LOG.info("AeronConnector open at {}", configuration.withLocalBinding());
        LOG.debug("Publishers: {}", publishers.keySet());
      }
    }
  }

  private void onFragment(DirectBuffer buffer, int offset, int length, Header header) {
    Object obj = helper.decode(new UnsafeBuffer(buffer), offset);
    if (obj != null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("{} received: {}", configuration.withLocalBinding(), obj);
      }
      bus.publish(obj);
    } else {
      LOG.error("Corrupt communication detected! Message is null");
    }
  }

  @Override
  public void close() {
    if (running.compareAndSet(true, false)) {
      publishers.values().forEach((pub) -> pub.getPublication().close());
      mediaDriver.close();
    }
  }

  @Override
  public void update(Configuration configuration) {
    // TODO Auto-generated method stub

  }

  @Override
  public void send(String binding, Object message) {
    if (publishers.containsKey(binding.trim())) {
      publishers.get(binding).offer(message, 10);
    } else {
      LOG.error("Remote binding on {} not known={}", configuration.withLocalBinding(), binding);
    }
  }

  private static void silentInactivityErrorHandler(Throwable t) {
    if (t instanceof DriverTimeoutException) {
      // ignore inactivity of the Media Driver
    } else {
      LOG.error("Driver error: {}", t);
    }
  }

  /**
   * Return a reusable, parameterised event loop that calls a default idler when
   * no messages are received
   * 
   * @param fragmentHandler
   *          to be called back for each message.
   * @param limit
   *          passed to {@link Subscription#poll(FragmentHandler, int)}
   * @param running
   *          indication for loop
   * @return loop function
   */
  public static Consumer<Subscription> subscriberLoop(final FragmentHandler fragmentHandler, final int limit,
      final AtomicBoolean running) {
    return subscriberLoop(fragmentHandler, limit, running, OFFER_IDLE_STRATEGY);
  }

  /**
   * Return a reusable, parameterized event loop that calls and idler when no
   * messages are received
   * 
   * @param fragmentHandler
   *          to be called back for each message.
   * @param limit
   *          passed to {@link Subscription#poll(FragmentHandler, int)}
   * @param running
   *          indication for loop
   * @param idleStrategy
   *          to use for loop
   * @return loop function
   */
  public static Consumer<Subscription> subscriberLoop(final FragmentHandler fragmentHandler, final int limit,
      final AtomicBoolean running, final IdleStrategy idleStrategy) {
    return (subscription) -> {
      try {
        while (running.get()) {
          final int fragmentsRead = subscription.poll(fragmentHandler, limit);
          idleStrategy.idle(fragmentsRead);
        }
      } catch (final Exception ex) {
        LangUtil.rethrowUnchecked(ex);
      }
    };
  }
}
