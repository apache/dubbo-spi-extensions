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
import org.apache.dubbo.apidocs.examples.api.IAsyncDemo;
import org.apache.dubbo.apidocs.examples.params.DemoParamBean1;
import org.apache.dubbo.apidocs.examples.params.DemoParamBean2;
import org.apache.dubbo.apidocs.examples.params.DemoParamBean3;
import org.apache.dubbo.apidocs.examples.params.DemoParamBean4;
import org.apache.dubbo.apidocs.examples.responses.DemoRespBean1;
import org.apache.dubbo.config.annotation.DubboService;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Asynchronous demo implementation.
 */
@DubboService(async = true)
@ApiModule(value = "Asynchronous demo", apiInterface = IAsyncDemo.class)
public class AsyncDemoImpl implements IAsyncDemo {

    public static final ScheduledExecutorService EXECUTOR = new ScheduledThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors() * 40 * 3,
        new BasicThreadFactory.Builder().namingPattern("dubbo-async-executor-pool-%d").daemon(true).build());

    @ApiDoc("request and response parameters are beans")
    @Override
    public CompletableFuture<DemoRespBean1> demoApi1(DemoParamBean1 param1, DemoParamBean2 param2) {
        DemoRespBean1 result = new DemoRespBean1();
        result.setCode("123456789");
        result.setMessage("called demoApi1 msg1");
        result.setMessage2("called demoApi1 msg2");
        return CompletableFuture.supplyAsync(() -> result, EXECUTOR);
    }

    @Override
    @ApiDoc(value = "Map without generics", responseClassDescription = "Map without generics")
    public CompletableFuture<Map> demoApi6() {
        return null;
    }

    @Override
    @ApiDoc(value = "Map generic with Object", responseClassDescription = "Map generic with Object")
    public CompletableFuture<Map<Object, Object>> demoApi7() {
        return null;
    }

    @Override
    @ApiDoc(value = "List without generics", responseClassDescription = "List without generics")
    public CompletableFuture<List> demoApi10() {
        return null;
    }

    @Override
    @ApiDoc(value = "List generic with Object", responseClassDescription = "List generic with Object")
    public CompletableFuture<List<Object>> demoApi9() {
        return null;
    }

    @Override
    @ApiDoc(value = "Object", responseClassDescription = "Object")
    public CompletableFuture<Object> demoApi8() {
        return null;
    }

    @Override
    @ApiDoc(value = "Integer", responseClassDescription = "Integer")
    public CompletableFuture<Integer> demoApi11() {
        return null;
    }

    @Override
    @ApiDoc(value = "many generics", responseClassDescription = "many generics")
    public CompletableFuture<List<List<String>>> demoApi12() {
        return null;
    }

    @Override
    @ApiDoc(value = "Simple test", responseClassDescription = "Simple test")
    public CompletableFuture<DemoParamBean3> demoApi13(DemoParamBean3 param1, DemoParamBean4 param2) {
        DemoParamBean3 result = new DemoParamBean3();
        result.setString("demoApi13 result");
        return CompletableFuture.supplyAsync(() -> result, EXECUTOR);
    }
}
