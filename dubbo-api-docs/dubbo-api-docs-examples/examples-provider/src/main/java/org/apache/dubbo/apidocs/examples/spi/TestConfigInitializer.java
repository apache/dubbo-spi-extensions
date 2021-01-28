package org.apache.dubbo.apidocs.examples.spi;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.config.ConfigInitializer;
import org.apache.dubbo.config.ServiceConfig;

/**
 * .
 *
 * @date 2021/1/12 17:09
 */
@Activate
public class TestConfigInitializer implements ConfigInitializer {

    @Override
    public void initServiceConfig(ServiceConfig serviceConfig) {
        System.out.println("====initServiceConfig");
    }

}
