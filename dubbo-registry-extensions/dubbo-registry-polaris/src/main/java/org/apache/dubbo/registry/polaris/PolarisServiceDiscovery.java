package org.apache.dubbo.registry.polaris;

import com.google.gson.GsonBuilder;
import com.tencent.polaris.api.exception.PolarisException;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.ConcurrentHashSet;
import org.apache.dubbo.common.utils.DefaultPage;
import org.apache.dubbo.common.utils.Page;
import org.apache.dubbo.registry.client.AbstractServiceDiscovery;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Application.Lv Service Discovery
 * @author karimli@tencent.com
 */
public class PolarisServiceDiscovery extends AbstractServiceDiscovery {
    private static final Logger logger = LoggerFactory.getLogger(PolarisServiceDiscovery.class);

    private PolarisDiscoveryClient polarisClient;

    private URL registryUrl;
    private final Set<String> services = new ConcurrentHashSet<>();

    public void doInitialize(URL registryURL) {
        this.registryUrl = registryURL;
        polarisClient = new PolarisDiscoveryClient(registryURL);
    }

//    private <T, R> R executeInPolaris(Function<T, R> function) {
//        if (polarisClient != null) {
//        }
////        function.apply();
//    }

    /**
     * App级别注册发现
     * @param serviceInstance
     */
    @Override
    public void doRegister(ServiceInstance serviceInstance) {
        polarisClient.register(serviceInstance);
        services.add(serviceInstance.getServiceName());
    }

    @Override
    public void doUpdate(ServiceInstance serviceInstance) {
        // TODO 需要确定是什么调用场景
        logger.info("doUpdate() service instance: " + new GsonBuilder().create().toJson(serviceInstance));
    }

    @Override
    public Page<ServiceInstance> getInstances(String serviceName, int offset, int pageSize, boolean healthyOnly)
            throws NullPointerException, IllegalArgumentException, UnsupportedOperationException {
        List<ServiceInstance> availableInstances = polarisClient.getFilteredInstances(serviceName, healthyOnly, getLocalInstance());
        return new DefaultPage<>(offset, pageSize, availableInstances, availableInstances.size());
    }

    @Override
    public void doDestroy() throws Exception {
        if (polarisClient != null) {
            polarisClient.destroy();
        }
    }

    public void doUnregister(ServiceInstance serviceInstance) {
        logger.info("doUnregister service instance: " + new GsonBuilder().create().toJson(serviceInstance));
        try {
            polarisClient.deRegister(serviceInstance);
            services.remove(serviceInstance.getServiceName());
        } catch (PolarisException e) {
            logger.error("register failed: " + serviceInstance);
        }
    }

    @Override
    public Set<String> getServices() {
        return Collections.unmodifiableSet(services);
    }

    @Override
    public URL getUrl() {
        return registryUrl;
    }

    /**
     * TODO 实时性问题
     */
    @Override
    public void addServiceInstancesChangedListener(ServiceInstancesChangedListener listener)
            throws NullPointerException, IllegalArgumentException {
        polarisClient.registerWatcher(listener);
    }

    @Override
    public void removeServiceInstancesChangedListener(ServiceInstancesChangedListener listener) throws IllegalArgumentException {
        polarisClient.removeWatcher(listener);
    }

}
