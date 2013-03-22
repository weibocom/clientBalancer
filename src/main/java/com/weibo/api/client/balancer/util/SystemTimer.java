package com.weibo.api.client.balancer.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 由于 System.currentTimeMillis 需要从用户态到内核态切换，在 memcache 每秒上万请求中大量使用会造成性能损耗。
 * 因此将系统时间 cache 10ms, 在不需要10ms以下精度之处可以使用此方法
 * 
 * 此代码从互联网获得
 * 
 * @author fishermen copy from commons
 * @author tim
 *
 */
public class SystemTimer {
	private final static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private static final long tickUnit = Long.parseLong(System.getProperty("commons.systimer.tick", "10"));
	private static volatile long time = System.currentTimeMillis();

	private static class TimerTicker implements Runnable {
		public void run() {
			time = System.currentTimeMillis();
		}
	}

	public static long currentTimeMillis() {
		return time;
	}

	static {
		executor.scheduleAtFixedRate(new TimerTicker(), tickUnit, tickUnit, TimeUnit.MILLISECONDS);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				executor.shutdown();
			}
		});
	}
}