package org.blueskiron.goldilocks.leader.election;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple synchronization class for handling scheduling of various tasks.
 * @author jzachar
 */
abstract class AbstractScheduledWork {

  protected static final Logger LOG = LoggerFactory.getLogger(AbstractScheduledWork.class);
  private final AtomicReference<List<ScheduledFuture<?>>> scheduledWork = new AtomicReference<>();
  private final Semaphore semaphore = new Semaphore(1);

  protected void init(ScheduledFuture<?>... tasks) {
    if (semaphore.tryAcquire()) {
      scheduledWork.set(Arrays.asList(tasks));
      semaphore.release();
    }
  }

  public abstract void start();

  public void stop() {
    if (semaphore.tryAcquire()) {
      while (!isDone()) {
        scheduledWork.get().forEach(work -> work.cancel(true));
      }
      semaphore.release();
    }
  }

  public void reset() {
    stop();
    start();
  }

  public boolean isDone() {
    boolean isDone = true;
    if (scheduledWork.get() != null) {
      isDone = false;
      for (ScheduledFuture<?> s : scheduledWork.get()) {
        isDone = s.isDone();
        if (!isDone) {
          break;
        }
      }
    }
    return isDone;
  }
}
