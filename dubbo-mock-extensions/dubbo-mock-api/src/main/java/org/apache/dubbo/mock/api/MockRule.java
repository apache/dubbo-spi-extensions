package org.apache.dubbo.mock.api;

import java.util.HashSet;
import java.util.Set;

/**
 * @author chenglu
 * @date 2021-08-24 11:19
 */
public class MockRule {

    private volatile boolean enableMock = false;

    private volatile Set<String> enabledMockRules = new HashSet<>();

    public boolean isEnableMock() {
        return enableMock;
    }

    public void setEnableMock(boolean enableMock) {
        this.enableMock = enableMock;
    }

    public Set<String> getEnabledMockRules() {
        return enabledMockRules;
    }

    public void setEnabledMockRules(Set<String> enabledMockRules) {
        this.enabledMockRules = enabledMockRules;
    }

    @Override
    public String toString() {
        return "MockRule{" +
            "enableMock=" + enableMock +
            ", enabledMockRules=" + enabledMockRules +
            '}';
    }
}
