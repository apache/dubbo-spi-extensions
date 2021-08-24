/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
