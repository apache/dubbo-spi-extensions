package org.apache.dubbo.registry.polaris;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.support.AbstractRegistryFactory;


/**
 * @author karimli
 */
public class PolarisRegistryFactory extends AbstractRegistryFactory {

    public PolarisRegistryFactory() {
    }

    /**
     * TODO registryUrl配置与polaris的配置只能选一个生效, 北极星支持使用默认配置, 但是 dubbo 这边也需要指定 protocol 以及是否开启服务注册
     * @param registryUrl
     * @return
     */
    @Override
    protected Registry createRegistry(URL registryUrl) {
        return new PolarisRegistry(registryUrl);
    }
}
