package org.apache.dubbo.registry.polaris.util;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * 提供能够在整点触发, 运行的周期Timer:
 * @author karimli
 */
public class CyclicTimer {

	private AtomicBoolean running;
	private long nextEpochSeconds;
	private long intervalSeconds;

	public CyclicTimer(int seconds) {
		this(seconds, 0);
	}

	/**
	 *
	 * @param seconds
	 * @param offset
	 */
	public CyclicTimer(int seconds, int offset) {
		this.running = new AtomicBoolean();
		this.intervalSeconds = seconds;
		long t = Instant.now().getEpochSecond();
		long r = (t - offset) % intervalSeconds;
		this.nextEpochSeconds = r == 0 ? t : (t - r) + intervalSeconds;
	}

	public long getIntervalSeconds() {
		return intervalSeconds;
	}

	public long onTime(Instant time) {
		if (time.getEpochSecond() >= nextEpochSeconds) {
			return nextEpochSeconds;
		} else {
			return -1;
		}
	}

	public Instant getTime() {
		return Instant.ofEpochSecond(nextEpochSeconds);
	}

	public void execute(long t, Function<Instant, Boolean> func) {
		if (running.compareAndSet(false, true)) {
			try {
				Instant now = Instant.ofEpochSecond(t);
				if (func.apply(now)) {
					next();
				}
			} catch (Exception ex) {
				throw new RuntimeException("execute failed:", ex);
			} finally {
				running.set(false);
			}
		}
	}


	public void next() {
		nextEpochSeconds = nextEpochSeconds + intervalSeconds;
	}
}
