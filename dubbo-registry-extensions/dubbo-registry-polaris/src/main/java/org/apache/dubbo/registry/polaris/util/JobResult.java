package org.apache.dubbo.registry.polaris.util;

/**
 * Scheduled Job 或其他运行状态
 * @author karimli
 */
public enum JobResult {
	/**
	 * 有数据仍然在执行, 可以继续发起
	 */
	CONTINUABLE,
	/**
	 * 没有可执行的数据
	 */
	IDLE,
	/**
	 * 加锁失败
	 */
	LOCK_FAILED,
	/**
	 * 异常
	 */
	EXCEPTION,
	;
}
