package org.apache.dubbo.registry.polaris.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static org.apache.dubbo.registry.polaris.util.JobResult.CONTINUABLE;

/**
 * 周期性运行Worker, 在启动中执行{@link ScheduledWorker#addScheduledJob}方法初始化
 * @author karimli
 */
public final class ScheduledWorker {
	private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledWorker.class);
	private static final int DEFAULT_LOOP_INTERVAL = 5;

    /**
     * waiting tasks when shutdown, default no
     */
    private boolean waitForTasksToCompleteOnStop = false;
	private ScheduledThreadPoolExecutor executor;

	public ScheduledWorker(String name, int threadCount) {
		Assert.isTrue(threadCount > 0, "'threadCount' must be 1 or higher");
		ThreadFactory threadFactory = new ThreadFactoryBuilder()
			.setNameFormat("scheduled-" + name + "-%d")
			.setDaemon(true)
			.build();
		this.executor = new ScheduledThreadPoolExecutor(threadCount, threadFactory);
	}

	public void addScheduledJob(String jobName, long delay, long intervalMs, Callable<JobResult> func) {
		final AtomicBoolean running = new AtomicBoolean(false);
		// disallow concurrent
		// TODO job.runtime > intervalMs时, 期间调度的任务会忽略, 对于注册中心来说可以防止缓存并发更新滚雪球问题
		executor.scheduleWithFixedDelay(() -> {
			if (!running.compareAndSet(false, true)) {
				return;
			}
			try {
				while (true) {
					if (func.call() != CONTINUABLE) {
						break;
					}
				}
			} catch (Exception ex) {
				LOGGER.error("scheduledJob failed : " + jobName, ex);
			}
			running.set(false);
		}, delay, intervalMs, TimeUnit.MILLISECONDS);
	}

	public void addScheduledJob(String jobName, CyclicTimer timer, Function<Instant, Boolean> func) {
		String s = jobName + "_t" + timer.getIntervalSeconds();
		executor.scheduleWithFixedDelay(() -> {
			long t = timer.onTime(Instant.now());
			if (t < 0) {
				return;
			}
			try {
				timer.execute(t, func);
			} catch (Exception ex) {
				LOGGER.error("scheduledJob failed: " + s , ex);
			}
		}, DEFAULT_LOOP_INTERVAL, DEFAULT_LOOP_INTERVAL, TimeUnit.MILLISECONDS);
	}

	public void stop() {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Stopping SchedulerWorker");
		}
		if (this.executor != null) {
			if (this.waitForTasksToCompleteOnStop) {
				this.executor.shutdown();
			}
			else {
				for (Runnable remainingTask : this.executor.shutdownNow()) {
					cancelRemainingTask(remainingTask);
				}
			}
		}
	}

	private void cancelRemainingTask(Runnable task) {
		if (task instanceof Future) {
			((Future<?>)task).cancel(true);
		}
	}

	public void setWaitForTasksToCompleteOnStop(boolean waitForTasksToCompleteOnStop) {
		this.waitForTasksToCompleteOnStop = waitForTasksToCompleteOnStop;
	}

}
