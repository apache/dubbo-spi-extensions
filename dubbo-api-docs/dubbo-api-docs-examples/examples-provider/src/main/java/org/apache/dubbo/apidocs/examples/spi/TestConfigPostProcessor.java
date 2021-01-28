package org.apache.dubbo.apidocs.examples.spi;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.config.ConfigPostProcessor;
import org.apache.dubbo.config.ServiceConfig;

/**
 * .
 *
 * @date 2021/1/12 16:51
 */
@Activate
public class TestConfigPostProcessor implements ConfigPostProcessor {


    @Override
    public void postProcessServiceConfig(ServiceConfig serviceConfig) {
//        ((ServiceBean)serviceConfig).getService()
//        ((ServiceBean)serviceConfig).applicationContext.getBean(((ServiceBean) serviceConfig).getInterfaceClass());
        serviceConfig.getRef();  // 拿实例
        System.out.println("====postProcessServiceConfig");
    }

}
