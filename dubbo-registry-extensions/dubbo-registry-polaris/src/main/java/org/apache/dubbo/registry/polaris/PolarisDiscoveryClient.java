package org.apache.dubbo.registry.polaris;

import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.core.ProviderAPI;
import com.tencent.polaris.api.exception.PolarisException;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.rpc.*;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.polaris.util.JobResult;
import org.apache.dubbo.registry.polaris.util.ScheduledWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static org.apache.dubbo.registry.client.metadata.ServiceInstanceMetadataUtils.*;

/**
 * Polaris API 代理和 listWatch 增强
 * @author karimli
 */
public class PolarisDiscoveryClient {

    private static final Logger logger = LoggerFactory.getLogger(PolarisDiscoveryClient.class);

    public static final int MAX_HEARTBEAT_MILLIS_TTL = 1000 * 10;
    public static final int MIN_HEARTBEAT_MILLIS_TTL = 1000 * 3;

    private int ttl = MAX_HEARTBEAT_MILLIS_TTL;

    private final URL registryUrl;
    private final PolarisConfig polarisConfig;

    private ProviderAPI providerAPI;
    private ConsumerAPI consumerAPI;

    private ScheduledWorker heartbeatWorker;

    private static final ConcurrentHashMap<String, ScheduledWorker> SERVICE_WATCHER = new ConcurrentHashMap<>();

    private final String[] metadataKeySet = new String[] {
        ENDPOINTS, INSTANCE_REVISION_UPDATED_KEY,
        METADATA_SERVICE_URL_PARAMS_PROPERTY_NAME, METADATA_SERVICE_URLS_PROPERTY_NAME, EXPORTED_SERVICES_REVISION_PROPERTY_NAME,
        METADATA_STORAGE_TYPE_PROPERTY_NAME, METADATA_CLUSTER_PROPERTY_NAME
    };

    final AtomicBoolean running = new AtomicBoolean(true);

    public PolarisDiscoveryClient(URL registryUrl) {
        this.registryUrl = registryUrl;
        this.polarisConfig = PolarisConfig.builder().withURL(registryUrl).build();
        this.consumerAPI = DiscoveryAPIFactory.createConsumerAPIByContext(polarisConfig.getSdkContext());
        if (polarisConfig.isEnabledRegister()) {
            this.providerAPI = DiscoveryAPIFactory.createProviderAPIByContext(polarisConfig.getSdkContext());
        }
        if (polarisConfig.isEnabledHealthyCheck()) {
            this.ttl = Math.max(1, Math.min(polarisConfig.getHeartbeatTTL() , MAX_HEARTBEAT_MILLIS_TTL));
            this.heartbeatWorker = new ScheduledWorker("polaris-heartbeat-worker", 1);
            this.heartbeatWorker.setWaitForTasksToCompleteOnStop(false);
        }
    }

    /**
     * 向北极星进行服务注册
     * @param serviceInstance
     */
    public void register(ServiceInstance serviceInstance) {
        if (!polarisConfig.isEnabledRegister() || !running.get()) {
            return;
        }
        InstanceRegisterRequest registerRequest = enrichPolarisRequest(new InstanceRegisterRequest(), serviceInstance);
        registerRequest.setMetadata(serviceInstance.getMetadata());
//        String revision = serviceInstance.getMetadata(EXPORTED_SERVICES_REVISION_PROPERTY_NAME);
//        if (StringUtils.isNotBlank(revision)) {
//            // 不能超过20个字符
//            registerRequest.setVersion(revision);
//        }
        if (polarisConfig.isEnabledHealthyCheck()) {
            // 这里设置实际没啥用, 控制台显示该 5s 还是 5s
            registerRequest.setTtl(ttl);
        }
        // 实例权重设置, 一般来说只有传统主机部署 性能不一致的情况才会使用, 先不考虑
//        registerRequest.getMetadata().put("weight", 100);

        try {
            providerAPI.register(registerRequest);
            if (logger.isInfoEnabled()) {
                logger.info("polaris registry - finished service register, namespace = {}, serviceInstance = {}, registryUrl = {} ",
                    polarisConfig.getNamespace(),
                    serviceInstance,
                    // 仅打印host, 避免Token泄漏
                    desensitizeRegistryUrl(registryUrl));
            }
            if (polarisConfig.isEnabledHealthyCheck() && heartbeatWorker != null) {
                startHeartbeat(serviceInstance);
            }
        } catch (PolarisException e) {
            logger.error(String.format("register service failed, request: %s", registerRequest), e);
        }

    }

    private String desensitizeRegistryUrl(URL registryUrl) {
        if (registryUrl == null) {
            return null;
        }
        String url = registryUrl.toFullString();
        int tokenIdx = url.indexOf(Constants.TOKEN);
        if (tokenIdx == -1) {
            return url;
        }
        return mosaicString(url, tokenIdx + Constants.TOKEN.length() + 1, 32);
    }

    private String mosaicString(String s, int startChars, int leftChars) {
        if (StringUtils.isEmpty(s)) {
            return s;
        }
        if (s.length() >= startChars) {
            return s.substring(0, startChars) + mosaicString(s.substring(startChars), leftChars);
        } else {
            return StringUtils.repeat("*", s.length());
        }
    }

    private String mosaicString(String s, int leftChars) {
        if (StringUtils.isEmpty(s)) {
            return s;
        }
        int n = s.length() - leftChars;
        if (n > 0) {
            return StringUtils.repeat("*", leftChars) + s.substring(leftChars);
        } else {
            return StringUtils.repeat("*", s.length());
        }
    }

    /**
     * 心跳保持
     * @param serviceInstance
     */
    private void startHeartbeat(ServiceInstance serviceInstance) {
        String name = "polaris-heartbeat_" + serviceInstance.getServiceName();
        final InstanceHeartbeatRequest heartbeatRequest = enrichPolarisRequest(new InstanceHeartbeatRequest(), serviceInstance);
        heartbeatWorker.addScheduledJob(name, 0, (long) ttl, () -> {
            try {
                providerAPI.heartbeat(heartbeatRequest);
            } catch (Exception e) {
                logger.error("polaris heartbeat report failed: {}", serviceInstance);
            }
            return JobResult.IDLE;
        });
    }

    public void deRegister(ServiceInstance serviceInstance) {
        if (!running.get()) {
            return;
        }
        checkArgument(serviceInstance != null, "serviceInstance cannot be null");
        providerAPI.deRegister(enrichPolarisRequest(new InstanceDeregisterRequest(), serviceInstance));
        if (logger.isInfoEnabled()) {
            logger.info("polaris registry - finished service deregister, namespace = {}, serviceInstance = {}, registryUrl = {} ",
                polarisConfig.getNamespace(),
                serviceInstance,
                // 仅打印host, 避免Token泄漏
                desensitizeRegistryUrl(registryUrl));
        }
    }

    private <T extends CommonProviderBaseEntity> T enrichPolarisRequest(T request, ServiceInstance serviceInstance) {
        checkArgument(request != null, "request can not be null");
        request.setNamespace(polarisConfig.getNamespace());
        request.setToken(polarisConfig.getToken());

        if (serviceInstance != null) {
            request.setService(serviceInstance.getServiceName());
            request.setHost(serviceInstance.getHost());
            request.setPort(serviceInstance.getPort());
        }
        return request;
    }

    public List<ServiceInstance> getFilteredInstances(String serviceName, boolean healthyOnly) {
        return this.getFilteredInstances(serviceName, healthyOnly, null);
    }

    /**
     * 从北极星本地缓存读取实例列表
     * @param serviceName 查询服务标识, Important!: 必须要提前手动注册
     * @param healthyOnly 健康节点过滤
     * @param localInstance 本地服务实例对象, 开启就近访问时需要
     */
    public List<ServiceInstance> getFilteredInstances(String serviceName, boolean healthyOnly, ServiceInstance localInstance) {
        if (!running.get()) {
            logger.warn("polaris client has been stopped!");
            return Collections.EMPTY_LIST;
        }
        GetInstancesRequest request = new GetInstancesRequest();
        request.setNamespace(polarisConfig.getNamespace());
        request.setService(serviceName);

        if (healthyOnly) {
            request.setIncludeUnhealthy(false);
            request.setIncludeCircuitBreak(false);
        }
        if (polarisConfig.isEnabledNearbyInvoke() && localInstance != null) {
            ServiceInfo sourceInfo = new ServiceInfo();
            sourceInfo.setNamespace(polarisConfig.getNamespace());
            sourceInfo.setService(localInstance.getServiceName());
            sourceInfo.setMetadata(localInstance.getMetadata());
            request.setServiceInfo(sourceInfo);
        }

        List<ServiceInstance> availableInstances = new ArrayList<>();
        try {
            InstancesResponse response = consumerAPI.getInstances(request);
            Stream.of(response.getInstances())
                .filter(inst -> !healthyOnly || this.isHealthy(inst))
                .forEach(inst -> {
//                        DefaultServiceInstance serviceInstance = new DefaultServiceInstance(inst.getService(), inst.getHost(), inst.getPort());
                    DefaultServiceInstance serviceInstance = new DefaultServiceInstance(inst.getService(), inst.getHost(), inst.getPort());
                    serviceInstance.setHealthy(inst.isHealthy() || inst.isIsolated());
                    serviceInstance.setMetadata(purMeta(inst.getMetadata()));
                    availableInstances.add(serviceInstance);
                });
        } catch (PolarisException e) {
            logger.error("polaris registry, failed to load instances from registry, " +
                "namespace = " + polarisConfig.getNamespace() +
                ", serviceName = " + serviceName +
                ", registryUrl = " + desensitizeRegistryUrl(registryUrl), e);
        }
        return availableInstances;
    }

    /**
     * 清理北极星对于未设置 metadata 的实例返回一些默认值得行为, 只处理我们主动设置的 metadata
     */
    private Map<String, String> purMeta(Map<String, String> metadata) {
        Map<String, String> rpcMeta = new HashMap<>();
        if (CollectionUtils.isEmptyMap(metadata)) {
            return rpcMeta;
        }
        for (String key : metadataKeySet) {
            String value = metadata.get(key);
            if (StringUtils.isNotEmpty(value)) {
                rpcMeta.put(key, metadata.get(key));
            }
        }
        return rpcMeta;
    }

    private boolean isHealthy(Instance instance) {
        return instance.isHealthy() || instance.isIsolated();
    }

    public void destroy() {
        running.set(false);
        if (!polarisConfig.isEnabledRegister()) {
            return;
        }
        if (consumerAPI != null) {
            consumerAPI.destroy();
        }
        if (providerAPI != null) {
            providerAPI.destroy();
        }
    }

    public void registerWatcher(ServiceInstancesChangedListener listener) {
        listener.getServiceNames().forEach(serviceName -> registerServiceWatcher(serviceName, listener));
    }

    public void removeWatcher(ServiceInstancesChangedListener listener) {
        listener.getServiceNames().forEach(serviceName -> {
            // remove watcher & stop
            ScheduledWorker scheduledWorker = SERVICE_WATCHER.remove(serviceName);
            if (scheduledWorker != null) {
                scheduledWorker.stop();
            }
        });
    }

    private void registerServiceWatcher(String serviceName, ServiceInstancesChangedListener listener) {
        ScheduledWorker worker = SERVICE_WATCHER.computeIfAbsent(
            serviceName, (name) -> new ScheduledWorker("polaris-service-watcher_" + name, 1));
        // scheduler 100ms 读取一次北极星 API K: servicename V: Watcher
        long watcher_intervalMs = 200;
        worker.addScheduledJob(serviceName, watcher_intervalMs, watcher_intervalMs,
            new PolarisServiceDiscoverChangeWatcher(this, serviceName, listener));
    }

    public PolarisConfig getPolarisConfig() {
        return this.polarisConfig;
    }

}
