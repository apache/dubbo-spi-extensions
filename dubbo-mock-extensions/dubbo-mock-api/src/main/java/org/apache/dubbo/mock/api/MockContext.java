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

import java.io.Serializable;
import java.util.Arrays;

/**
 * The Mock Context will put the data send to MockService Provider.
 */
public class MockContext implements Serializable {

    /**
     * service name.
     */
    private String serviceName;

    /**
     * method name.
     */
    private String methodName;

    /**
     * method params of consumer
     */
    private Object[] arguments;

    public MockContext() {
    }

    private MockContext(Builder builder) {
        this.serviceName = builder.serviceName;
        this.methodName = builder.methodName;
        this.arguments = builder.arguments;
    }

    public static Builder newMockContext() {
        return new Builder();
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public void setArguments(Object[] arguments) {
        this.arguments = arguments;
    }

    @Override
    public String toString() {
        return "MockContext{" +
            "serviceName='" + serviceName + '\'' +
            ", methodName='" + methodName + '\'' +
            ", arguments=" + Arrays.toString(arguments) +
            '}';
    }

    public static final class Builder {
        private String serviceName;
        private String methodName;
        private Object[] arguments;

        private Builder() {
        }

        public MockContext build() {
            return new MockContext(this);
        }

        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder methodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public Builder arguments(Object[] arguments) {
            this.arguments = arguments;
            return this;
        }
    }
}
