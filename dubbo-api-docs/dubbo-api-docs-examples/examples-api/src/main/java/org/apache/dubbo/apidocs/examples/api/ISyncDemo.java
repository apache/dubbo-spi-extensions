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
import org.apache.dubbo.apidocs.examples.responses.BaseResponse;
import org.apache.dubbo.apidocs.examples.responses.DemoRespBean1;

import java.util.List;
import java.util.Map;

/**
 * synchronization demo.
 */
public interface ISyncDemo {

    /**
     * request and response parameters are beans.
     *
     * @param param1
     * @param param2
     * @return org.apache.dubbo.apidocs.examples.responses.DemoRespBean1
     */
    DemoRespBean1 demoApi1(DemoParamBean1 param1, DemoParamBean2 param2);

    /**
     * request and response parameters are Strings
     *
     * @return java.lang.String
     * @param: param1
     * @param: param2
     */
    String demoApi2(String param1, String param2);

    /**
     * Without Dubbo doc annotation, no document will be generated
     *
     * @return java.lang.String
     * @param: param1
     */
    String demoApi3(String param1);

    /**
     * Nonparametric method with Dubbo doc annotation
     *
     * @return java.lang.String
     * @param:
     */
    String demoApi4();

    /**
     * Use generics in response
     */
    BaseResponse<DemoRespBean1> demoApi5();

    /**
     * Map without generics
     */
    Map demoApi6();

    /**
     * Map generic with Object
     */
    Map<Object, Object> demoApi7();

    /**
     * List without generics
     */
    List demoApi10();

    /**
     * List generic with Object
     */
    List<Object> demoApi9();

    /**
     * Object
     */
    Object demoApi8();

    /**
     * Simple test.
     *
     * @param param1
     * @param param2
     * @return org.apache.dubbo.apidocs.examples.params.DemoParamBean3
     */
    DemoParamBean3 demoApi13(DemoParamBean3 param1, DemoParamBean4 param2);

}
