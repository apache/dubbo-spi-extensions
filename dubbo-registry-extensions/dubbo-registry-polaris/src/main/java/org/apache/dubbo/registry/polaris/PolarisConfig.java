package org.apache.dubbo.registry.polaris;

import com.google.common.base.Preconditions;
import com.tencent.polaris.api.config.Configuration;
import com.tencent.polaris.client.api.SDKContext;
import com.tencent.polaris.factory.ConfigAPIFactory;
import org.apache.dubbo.common.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.apache.dubbo.registry.polaris.Constants.*;

/**
 * @author karimli
 */
public class PolarisConfig {

    private static final Logger logger = LoggerFactory.getLogger(PolarisConfig.class);

    public static final String ENCODED_AND_MARK = "%26";
    public static final String DEFAULT_NAMESPACE = "default";

    //-- global --//
    private String namespace;

    //-- callee -- //
    /**
     * 开启主动注册, 这里指不通过 mTKE-CRD 代理注册行为
     */
    private boolean enabledRegister;
    /**
     * 开启就近访问
     */
    private boolean enabledNearbyInvoke;
    /**
     * 开启健康检测
     */
    private boolean enabledHealthyCheck;
    /**
     * 以上操作所需Token
     */
    private String token;
    /**
     * 心跳上报周期, MaxValue: 10s
     */
    private int heartbeatTTL;

    //-- polaris context --//
    private SDKContext sdkContext;
    private final AtomicBoolean ctxInit = new AtomicBoolean(false);
    private final CountDownLatch ready = new CountDownLatch(1);

    public PolarisConfig() {}

    public String getNamespace() {
        return this.namespace;
    }

    public boolean isEnabledRegister() {
        return enabledRegister;
    }

    public boolean isEnabledNearbyInvoke() {
        return enabledNearbyInvoke;
    }

    public boolean isEnabledHealthyCheck() {
        return enabledHealthyCheck;
    }

    public String getToken() {
        return token;
    }

    public int getHeartbeatTTL() {
        return heartbeatTTL;
    }

    public SDKContext getSdkContext() {
        if (ctxInit.compareAndSet(false, true)) {
            try {
                // load classpath:polaris.yml
                Configuration configuration = ConfigAPIFactory.defaultConfig();
                sdkContext = SDKContext.initContextByConfig(configuration);
                if (logger.isInfoEnabled()) {
                    logger.info("Polaris SDKContext initialized with context");
                }
            } catch (RuntimeException ex) {
                ctxInit.set(false);
                throw ex;
            } finally {
                ready.countDown();
            }
        }

        try {
            ready.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        return sdkContext;
    }


    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private static final String REQIRED_FIELD_NOT_FOUT_TEMPLATE = "polaris config cannot be created with null or empty %s";

        private final PolarisConfig product = new PolarisConfig();

        public Builder withNamespace(String namespace) {
            product.namespace = namespace;
            return this;
        }

        public Builder withEnabledRegister(boolean enable) {
            product.enabledRegister = enable;
            return this;
        }

        public Builder withNearbyInvoke(boolean enable) {
            product.enabledNearbyInvoke = enable;
            return this;
        }

        public Builder withToken(String token) {
            product.token = token;
            return this;
        }

        public Builder withHealthyCheck(boolean enable) {
            product.enabledHealthyCheck = enable;
            return this;
        }

        public Builder withTTL(int ttl) {
            product.heartbeatTTL = ttl;
            return this;
        }

        public Builder withURL(URL registryUrl) {
            checkArgument(registryUrl != null, "registryUrl can not be null or empty");
            String urlStr = registryUrl.toFullString();
            boolean encoded = urlStr.contains("?") && urlStr.contains(ENCODED_AND_MARK);
            if (encoded) {
                registryUrl = URL.valueOf(URL.decode(urlStr));
            }
            return this.withNamespace(registryUrl.getParameter(NAMESPACE, PolarisConfig.DEFAULT_NAMESPACE))
                    .withNearbyInvoke(registryUrl.getParameter(ENABLE_NEARBY_INVOKE, false))
                    .withHealthyCheck(registryUrl.getParameter(ENABLE_HEALTHY_CHECK, false))
                    .withTTL(registryUrl.getParameter(TTL, Integer.MAX_VALUE))
                    .withEnabledRegister(registryUrl.getParameter(ENABLE_REGISTER, false))
                    .withToken(registryUrl.getParameter(TOKEN));
        }

        private String getNullFieldMessage(String field) {
            return format(REQIRED_FIELD_NOT_FOUT_TEMPLATE, field);
        }

        private void validateIsNotEmpty(String value, String fieldId) {
            Preconditions.checkState(value != null, getNullFieldMessage(fieldId));
        }

        public PolarisConfig build() {
            return this.product;
        }

    }

}
