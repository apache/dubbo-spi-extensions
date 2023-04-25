package org.apache.dubbo.gateway.consumer.config;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.config.ConfigPostProcessor;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.rpc.Constants;

@Activate
public class InjvmConfigPostProcessor implements ConfigPostProcessor {

    @Override
    public void postProcessReferConfig(ReferenceConfig referenceConfig) {
        referenceConfig.setScope(Constants.SCOPE_REMOTE);
    }
}
