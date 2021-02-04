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

import org.apache.dubbo.apidocs.annotations.RequestParam;

/**
 * Internal class test, request bean.
 *
 * @date 2021/2/2 16:24
 */
public class InnerClassRequestBean<T> {

    @RequestParam("tReq")
    private T tReq;

    @RequestParam("innerReqBean1")
    private InnerReqBean1 innerReqBean1;

    @RequestParam("innerReqBean2")
    private InnerReqBean2 innerReqBean2;

    @RequestParam("innerReqBean3")
    private InnerReqBean3 innerReqBean3;

    class InnerReqBean1 {
        @RequestParam("InnerReqBean1#string1")
        private String string1;

        public String getString1() {
            return string1;
        }

        public void setString1(String string1) {
            this.string1 = string1;
        }
    }

    public class InnerReqBean2 {
        @RequestParam("InnerReqBean2#string2")
        private String string2;

        public String getString1() {
            return string2;
        }

        public void setString2(String string2) {
            this.string2 = string2;
        }
    }

    private class InnerReqBean3 {
        @RequestParam("InnerReqBean3#string3")
        private String string3;

        @RequestParam("InnerReqBean3#string4")
        private String string4;

        public String getString3() {
            return string3;
        }

        public void setString3(String string3) {
            this.string3 = string3;
        }

        public String getString4() {
            return string4;
        }

        public void setString4(String string4) {
            this.string4 = string4;
        }
    }

    public T gettReq() {
        return tReq;
    }

    public void settReq(T tReq) {
        this.tReq = tReq;
    }

    public InnerReqBean1 getInnerReqBean1() {
        return innerReqBean1;
    }

    public void setInnerReqBean1(InnerReqBean1 innerReqBean1) {
        this.innerReqBean1 = innerReqBean1;
    }

    public InnerReqBean2 getInnerReqBean2() {
        return innerReqBean2;
    }

    public void setInnerReqBean2(InnerReqBean2 innerReqBean2) {
        this.innerReqBean2 = innerReqBean2;
    }

    public InnerReqBean3 getInnerReqBean3() {
        return innerReqBean3;
    }

    public void setInnerReqBean3(InnerReqBean3 innerReqBean3) {
        this.innerReqBean3 = innerReqBean3;
    }

}
