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
package org.apache.dubbo.apidocs.examples.params;

import org.apache.dubbo.apidocs.annotations.ResponseProperty;

/**
 * Internal class test, response bean.
 *
 * @date 2021/2/2 16:24
 */
public class InnerClassResponseBean<T> {

    @ResponseProperty("tResp")
    private T tResp;

    @ResponseProperty("innerRespBean1")
    private InnerRespBean1 innerRespBean1;

    @ResponseProperty("innerRespBean2")
    private InnerRespBean2 innerRespBean2;

    @ResponseProperty("innerRespBean3")
    private InnerRespBean3 innerRespBean3;

    class InnerRespBean1 {
        @ResponseProperty("InnerRespBean1#String1")
        private String string1;

        public String getString1() {
            return string1;
        }

        public void setString1(String string1) {
            this.string1 = string1;
        }
    }

    public class InnerRespBean2 {
        @ResponseProperty("InnerRespBean2#String2")
        private String string2;

        public String getString2() {
            return string2;
        }

        public void setString2(String string2) {
            this.string2 = string2;
        }
    }

    private class InnerRespBean3 {
        @ResponseProperty("InnerRespBean3#String3")
        private String string3;

        public String getString3() {
            return string3;
        }

        public void setString3(String string3) {
            this.string3 = string3;
        }
    }

    public T gettResp() {
        return tResp;
    }

    public void settResp(T tResp) {
        this.tResp = tResp;
    }

    public InnerRespBean1 getInnerRespBean1() {
        return innerRespBean1;
    }

    public void setInnerRespBean1(InnerRespBean1 innerRespBean1) {
        this.innerRespBean1 = innerRespBean1;
    }

    public InnerRespBean2 getInnerRespBean2() {
        return innerRespBean2;
    }

    public void setInnerRespBean2(InnerRespBean2 innerRespBean2) {
        this.innerRespBean2 = innerRespBean2;
    }

    public InnerRespBean3 getInnerRespBean3() {
        return innerRespBean3;
    }

    public void setInnerRespBean3(InnerRespBean3 innerRespBean3) {
        this.innerRespBean3 = innerRespBean3;
    }
}
