package org.apache.dubbo.service.discovery.springcloud.test;

import org.apache.dubbo.common.ProtocolServiceKey;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.registry.client.ServiceInstance;
import org.apache.dubbo.registry.client.metadata.ServiceInstanceNotificationCustomizer;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class SpringCloudServiceInstanceNotificationCustomizer implements ServiceInstanceNotificationCustomizer {

    @Override
    public void customize(List<ServiceInstance> serviceInstance) {
        if (serviceInstance.isEmpty()) {
            return;
        }

        if (!serviceInstance.stream()
            .allMatch(instance -> "SPRING_CLOUD".equals(instance.getMetadata("preserved.register.source")))) {
            return;
        }

        for (ServiceInstance instance : serviceInstance) {
            MetadataInfo.ServiceInfo serviceInfo =
                new MetadataInfo.ServiceInfo("*", "*", "*", "rest", instance.getPort(), "*", new HashMap<>());
            String revision = "SPRING_CLOUD-" + instance.getServiceName() + "-" + instance.getAddress() + "-"
                + instance.getPort();
            MetadataInfo metadataInfo =
                new MetadataInfo(
                    instance.getServiceName(),
                    revision,
                    new ConcurrentHashMap<>(Collections.singletonMap("*", serviceInfo))) {
                    @Override
                    public List<ServiceInfo> getMatchedServiceInfos(ProtocolServiceKey consumerProtocolServiceKey) {
                        getServices()
                            .putIfAbsent(
                                consumerProtocolServiceKey.getServiceKeyString(),
                                new MetadataInfo.ServiceInfo(
                                    consumerProtocolServiceKey.getInterfaceName(),
                                    consumerProtocolServiceKey.getGroup(),
                                    consumerProtocolServiceKey.getVersion(),
                                    consumerProtocolServiceKey.getProtocol(),
                                    instance.getPort(),
                                    consumerProtocolServiceKey.getInterfaceName(),
                                    new HashMap<>()));
                        return super.getMatchedServiceInfos(consumerProtocolServiceKey);
                    }
                };

            instance.setServiceMetadata(metadataInfo);
        }
    }
}
