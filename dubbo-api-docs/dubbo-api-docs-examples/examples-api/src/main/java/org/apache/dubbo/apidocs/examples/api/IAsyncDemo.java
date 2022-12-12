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
package org.apache.dubbo.apidocs.examples.api;

import org.apache.dubbo.apidocs.examples.params.DemoParamBean1;
import org.apache.dubbo.apidocs.examples.params.DemoParamBean2;
import org.apache.dubbo.apidocs.examples.params.DemoParamBean3;
import org.apache.dubbo.apidocs.examples.params.DemoParamBean4;
import org.apache.dubbo.apidocs.examples.responses.DemoRespBean1;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * asynchronous demo.
 */
public interface IAsyncDemo {

    /**
     * request and response parameters are beans.
     *
     * @param param1
     * @param param2
     * @return java.util.concurrent.CompletableFuture<org.apache.dubbo.apidocs.examples.responses.DemoRespBean1>
     */
    CompletableFuture<DemoRespBean1> demoApi1(DemoParamBean1 param1, DemoParamBean2 param2);

    /**
     * Map without generics
     */
    CompletableFuture<Map> demoApi6();

    /**
     * Map generic with Object
     */
    CompletableFuture<Map<Object, Object>> demoApi7();

    /**
     * List without generics
     */
    CompletableFuture<List> demoApi10();

    /**
     * List generic with Object
     */
    CompletableFuture<List<Object>> demoApi9();

    /**
     * Object
     */
    CompletableFuture<Object> demoApi8();

    /**
     * Integer
     */
    CompletableFuture<Integer> demoApi11();


    /**
     * many generics
     *
     * @return java.util.concurrent.CompletableFuture<java.util.List < java.util.List < java.lang.String>>>
     * @param:
     */
    CompletableFuture<List<List<String>>> demoApi12();

    /**
     * Simple test.
     *
     * @param param1
     * @param param2
     * @return java.util.concurrent.CompletableFuture<org.apache.dubbo.apidocs.examples.params.DemoParamBean3>
     */
    CompletableFuture<DemoParamBean3> demoApi13(DemoParamBean3 param1, DemoParamBean4 param2);

}
