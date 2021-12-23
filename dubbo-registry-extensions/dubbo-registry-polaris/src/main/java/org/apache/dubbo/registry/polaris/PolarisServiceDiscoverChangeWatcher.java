package org.apache.dubbo.registry.polaris;

import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.polaris.util.JobResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The polaris instances change watcher
 * @author karimli
 */
@NotThreadSafe
public class PolarisServiceDiscoverChangeWatcher implements Callable<JobResult> {
    private static final Logger logger = LoggerFactory.getLogger(PolarisServiceDiscoverChangeWatcher.class);

    private final PolarisDiscoveryClient polarisDiscoveryClient;
    private final String serviceName;
    private final ServiceInstancesChangedListener listener;

    private Map<Integer, ServiceInstance> cache = new HashMap<>();

    public PolarisServiceDiscoverChangeWatcher(PolarisDiscoveryClient discoveryClient, String serviceName, ServiceInstancesChangedListener listener) {
        this.polarisDiscoveryClient = discoveryClient;
        this.serviceName = serviceName;
        this.listener = listener;
    }

    @Override
    public JobResult call() throws Exception {
        List<ServiceInstance> newest = polarisDiscoveryClient.getFilteredInstances(serviceName, true);
        if (instanceChanged(newest)) {
            listener.onEvent(new ServiceInstancesChangedEvent(serviceName, newest));
            if (logger.isInfoEnabled()) {
                logger.info("Polaris service instance changed, serviceName: {}, instances: {}", serviceName, newest.size());
            }
        }
        return JobResult.IDLE;
    }

    // 一趟遍历, 状态检测 + 缓存更新 O(n)
    public boolean instanceChanged(List<ServiceInstance> instances) {
        int newestSize = instances.size();
        int oldSize = cache.size();

        AtomicBoolean changed = new AtomicBoolean(newestSize != oldSize);
        final Map<Integer, ServiceInstance> newCache = new HashMap<>();

        instances.forEach(inst -> {
            // hash冲突就多刷一次, dont care
            int hash = inst.hashCode();
            if (!changed.get()) {
                if (!cache.containsKey(hash) && inst.equals(cache.get(hash))) {
                    changed.set(true);
                }
            }
            newCache.putIfAbsent(hash, inst);
        });
        cache = newCache;

        return changed.get();
    }

    // bloom filter固定空间开销, 对于小规模场景比较占内存
//    private BloomFilter<ServiceInstance> bloomFilter = BloomFilter.create()
//    private class ServiceInstanceFunnel implements Funnel<ServiceInstance> {
//        @Override
//        public void funnel(ServiceInstance from, PrimitiveSink into) {
//            into.putUnencodedChars(from.getId())
//                    .putUnencodedChars(from.getAddress())
//                    .putInt(from.getPort());
//        }
//    }

}
