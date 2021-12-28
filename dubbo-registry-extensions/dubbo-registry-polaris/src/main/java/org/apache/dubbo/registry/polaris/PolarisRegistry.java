package org.apache.dubbo.registry.polaris;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.support.FailbackRegistry;

/**
 * 接口级应用注册, do nothing
 * @author karimli
 */
public class PolarisRegistry extends FailbackRegistry {

    /**
     * whether the service provider actively registered to Polaris
     */
    public PolarisRegistry(URL url) {
        super(url);
    }

    /**
     * interface level register
     * @param url
     */
    @Override
    public void doRegister(URL url) {
    }

    @Override
    public void doUnregister(URL url) {
    }

    @Override
    public void doSubscribe(URL url, NotifyListener listener) {
        System.out.println("subscribe: " + url);
    }

    @Override
    public void doUnsubscribe(URL url, NotifyListener listener) {

    }

    @Override
    public boolean isAvailable() {
        return false;
    }
}
