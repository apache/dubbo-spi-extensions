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
package org.apache.dubbo.apidocs.examples.api.impl;

import org.apache.dubbo.apidocs.annotations.ApiDoc;
import org.apache.dubbo.apidocs.annotations.ApiModule;
import org.apache.dubbo.apidocs.annotations.RequestParam;
import org.apache.dubbo.apidocs.examples.api.ISyncDemo;
import org.apache.dubbo.apidocs.examples.params.DemoParamBean1;
import org.apache.dubbo.apidocs.examples.params.DemoParamBean2;
import org.apache.dubbo.apidocs.examples.params.DemoParamBean3;
import org.apache.dubbo.apidocs.examples.params.DemoParamBean4;
import org.apache.dubbo.apidocs.examples.responses.BaseResponse;
import org.apache.dubbo.apidocs.examples.responses.DemoRespBean1;
import org.apache.dubbo.config.annotation.DubboService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Synchronous demo implementation.
 */
@DubboService
@ApiModule(value = "Synchronous demo", apiInterface = ISyncDemo.class)
public class SyncDemoImpl implements ISyncDemo {

    private static final Logger log = LoggerFactory.getLogger(SyncDemoImpl.class);

    @ApiDoc("request and response parameters are beans")
    @Override
    public DemoRespBean1 demoApi1(DemoParamBean1 param1, DemoParamBean2 param2) {
        log.info("called demoApi1");
        DemoRespBean1 result = new DemoRespBean1();
        result.setCode("123456789");
        result.setMessage("called demoApi1 msg1");
        result.setMessage2("called demoApi1 msg2");
        return result;
    }

    @ApiDoc(value = "request and response parameters are Strings", responseClassDescription = "A string")
    @Override
    public String demoApi2(@RequestParam(value = "Parameter 1", required = true) String param1, String param2) {
        log.info(" called demoApi2");
        return "demoApi2";
    }

    @Override
    public String demoApi3(String param1) {
        return null;
    }

    @ApiDoc(value = "Nonparametric method with Dubbo doc annotation", responseClassDescription = "A string")
    @Override
    public String demoApi4() {
        return "asdfasdfsdafds";
    }

    @ApiDoc(value = " Use generics in response", responseClassDescription = " Use generics in response")
    @Override
    public BaseResponse<DemoRespBean1> demoApi5() {
        BaseResponse<DemoRespBean1> response = new BaseResponse<>();
        DemoRespBean1 responseData = new DemoRespBean1();
        responseData.setCode("2222");
        responseData.setMessage("msg1");
        responseData.setMessage2("msg2");
        response.setData(responseData);
        response.setCode("1111");
        response.setMessage("msg");
        return response;
    }

    @Override
    @ApiDoc(value = "Map without generics", responseClassDescription = "Map without generics")
    public Map demoApi6() {
        return null;
    }

    @Override
    @ApiDoc(value = "Map generic with Object", responseClassDescription = "Map generic with Object")
    public Map<Object, Object> demoApi7() {
        return null;
    }

    @Override
    @ApiDoc(value = "List without generics", responseClassDescription = "List without generics")
    public List demoApi10() {
        return null;
    }

    @Override
    @ApiDoc(value = "List generic with Object", responseClassDescription = "List generic with Object")
    public List<Object> demoApi9() {
        return null;
    }

    @Override
    @ApiDoc(value = "Object", responseClassDescription = "Object")
    public Object demoApi8() {
        return null;
    }

    @Override
    @ApiDoc(value = "Simple test", responseClassDescription = "Simple test")
    public DemoParamBean3 demoApi13(DemoParamBean3 param1, DemoParamBean4 param2) {
        DemoParamBean3 result = new DemoParamBean3();
        result.setString("demoApi13 result");
        return result;
    }
}
