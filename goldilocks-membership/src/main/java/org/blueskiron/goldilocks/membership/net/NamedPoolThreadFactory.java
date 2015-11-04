package org.blueskiron.goldilocks.membership.net;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Inspired by
 * https://github.com/twitter/util/blob/master/util-core/src/main/scala
 * /com/twitter/concurrent/NamedPoolThreadFactory.scala
 * 
 * @author jzachar
 *
 */
public class NamedPoolThreadFactory implements ThreadFactory {

	private final String factoryName;
	private final boolean makeDaemons;
	private final AtomicInteger threadNumber = new AtomicInteger(1);
	private final ThreadGroup threadGroup;

	public NamedPoolThreadFactory(String factoryName) {
		this.factoryName = factoryName;
		this.makeDaemons = false;
		this.threadGroup = new ThreadGroup(Thread.currentThread()
				.getThreadGroup(), factoryName);
	}

	public NamedPoolThreadFactory(String factoryName, boolean makeDaemons) {
		this.factoryName = factoryName;
		this.makeDaemons = makeDaemons;
		this.threadGroup = new ThreadGroup(Thread.currentThread()
				.getThreadGroup(), factoryName);
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(threadGroup, r, factoryName + "-"
				+ threadNumber.getAndIncrement());
		t.setDaemon(makeDaemons);
		if (t.getPriority() != Thread.NORM_PRIORITY) {
			t.setPriority(Thread.NORM_PRIORITY);
		}
		return t;
	}

}
