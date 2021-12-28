package test.apache.dubbo.registry.polaris;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.Page;
import org.apache.dubbo.registry.client.DefaultServiceInstance;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.event.ServiceInstancesChangedEvent;
import org.apache.dubbo.registry.client.event.listener.ServiceInstancesChangedListener;
import org.apache.dubbo.registry.polaris.PolarisServiceDiscovery;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.collections.Sets;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class PolarisServiceDiscoveryTest {

    private static final String registryUrl = "polaris://polaris.com?enableRegister=true&namespace=default&enableHealthyCheck=true&ttl=3";

    private String polarisTestServiceName = "dubbo-demo-service-new2";

    private static PolarisServiceDiscovery discovery;

    List<ServiceInstance> serviceCache = new ArrayList<>();

    @BeforeAll
    public static void init() throws Exception {
        discovery = new PolarisServiceDiscovery();
        discovery.initialize(URL.valueOf(registryUrl));
    }

    @AfterAll
    public static void destroy() throws Exception {
        discovery.destroy();
    }

    @AfterEach
    public void close() {
        serviceCache.forEach(inst -> discovery.unregister(inst));
    }

    public void registerServiceInstance(ServiceInstance serviceInstance) {
        discovery.register(serviceInstance);
        serviceCache.add(serviceInstance);
    }

    public void deRegisterServiceInstance(ServiceInstance serviceInstance) {
        discovery.unregister(serviceInstance);
        serviceCache.remove(serviceInstance);
    }

    @Test
    public void testServiceRegistration() throws Exception {
        ServiceInstance serviceInstance = createServiceInstance(polarisTestServiceName, "127.0.0.1", NetUtils.getAvailablePort());

        registerServiceInstance(serviceInstance);
        Thread.sleep(10000);
        List<ServiceInstance> instanceList = discovery.getInstances(polarisTestServiceName);

        assertTrue(instanceList.contains(serviceInstance));
        assertEquals(asList(serviceInstance), instanceList);

        deRegisterServiceInstance(serviceInstance);
        Thread.sleep(10000);
        instanceList = discovery.getInstances(polarisTestServiceName);

        assertTrue(instanceList.isEmpty());
    }

    @Test
    public void testGetInstances() throws Exception {
        List<ServiceInstance> instances = asList(
                createServiceInstance(polarisTestServiceName, "127.0.0.1", 8080),
                createServiceInstance(polarisTestServiceName, "127.0.0.1", 8081),
                createServiceInstance(polarisTestServiceName, "127.0.0.1", 8082)
        );

        instances.forEach(this::registerServiceInstance);
        Thread.sleep(5000);

        List<ServiceInstance> serviceInstances = new LinkedList<>();
        CountDownLatch latch = new CountDownLatch(1);
        discovery.addServiceInstancesChangedListener(
                new ServiceInstancesChangedListener(Sets.newSet(polarisTestServiceName), discovery) {
                    @Override
                    public void onEvent(ServiceInstancesChangedEvent event) {
                        serviceInstances.addAll(event.getServiceInstances());
                        latch.countDown();
                    }
                });

        registerServiceInstance(createServiceInstance(polarisTestServiceName, "127.0.0.1", 8082));
        // not support
//        discovery.update(createServiceInstance(polarisTestServiceName, "127.0.0.1", 8082));

        latch.await();

        assertFalse(serviceInstances.isEmpty());

        // offset starts 0
        int offset = 0;
        // requestSize > total elements
        int requestSize = 5;

        Page<ServiceInstance> page = discovery.getInstances(polarisTestServiceName, offset, requestSize);
        for (ServiceInstance instance : page.getData()) {
            assertTrue(instances.contains(instance));
        }

        // TODO requestSize < total elements, 暂不支持分页
    }

    public ServiceInstance createServiceInstance(String serviceName, String ip, int port) {
        return new DefaultServiceInstance(serviceName, ip, port);
    }

}
