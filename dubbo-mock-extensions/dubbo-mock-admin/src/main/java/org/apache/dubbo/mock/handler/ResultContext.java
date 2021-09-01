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

package org.apache.dubbo.mock.handler;

/**
 * @author chenglu
 * @date 2021-08-30 19:23
 */
public class ResultContext {

    private String serviceName;

    private String methodName;

    private Class<?> targetType;

    private String data;

    private ResultContext(Builder builder) {
        this.targetType = builder.targetType;
        this.data = builder.data;
        this.serviceName = builder.serviceName;
        this.methodName = builder.methodName;
    }

    public static Builder newResultContext() {
        return new Builder();
    }

    public Class<?> getTargetType() {
        return targetType;
    }

    public void setTargetType(Class<?> targetType) {
        this.targetType = targetType;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
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

    @Override
    public String toString() {
        return "ResultContext{" +
            "serviceName='" + serviceName + '\'' +
            ", methodName='" + methodName + '\'' +
            ", targetType=" + targetType +
            ", data='" + data + '\'' +
            '}';
    }

    public static final class Builder {
        private Class<?> targetType;
        private String data;
        private String serviceName;
        private String methodName;

        private Builder() {
        }

        public ResultContext build() {
            return new ResultContext(this);
        }

        public Builder targetType(Class<?> targetType) {
            this.targetType = targetType;
            return this;
        }

        public Builder data(String data) {
            this.data = data;
            return this;
        }

        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder methodName(String methodName) {
            this.methodName = methodName;
            return this;
        }
    }
}
