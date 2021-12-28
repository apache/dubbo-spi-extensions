package org.apache.dubbo.registry.polaris;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.client.AbstractServiceDiscoveryFactory;
import org.apache.dubbo.registry.client.ServiceDiscovery;

/**
 *
 * @author karimli
 */
public class PolarisServiceDiscoveryFactory extends AbstractServiceDiscoveryFactory {

    @Override
    protected ServiceDiscovery createDiscovery(URL registryURL) {
        return new PolarisServiceDiscovery();
    }
}
