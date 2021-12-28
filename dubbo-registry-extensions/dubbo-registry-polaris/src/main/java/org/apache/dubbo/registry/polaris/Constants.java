package org.apache.dubbo.registry.polaris;

public interface Constants {

    /**
     * 是否开启 Provider 侧主动服务注册
     */
    String ENABLE_REGISTER = "enableRegister";

    // polaris namespace
    String NAMESPACE = "namespace";

    // polaris service token, only effective when enableRegister=true
    String TOKEN = "token";

    // 是否开启就近访问, 需要支持cmdb host
    String ENABLE_NEARBY_INVOKE = "enableNearbyInvoke";

    // 是否开启健康检查
    String ENABLE_HEALTHY_CHECK = "enableHealthyCheck";

    // 健康检查上报周期 (ms)
    String TTL = "ttl";
}
