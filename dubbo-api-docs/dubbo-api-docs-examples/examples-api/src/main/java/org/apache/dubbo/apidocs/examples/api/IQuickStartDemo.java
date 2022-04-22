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

import org.apache.dubbo.apidocs.examples.params.DemoParamBean4;
import org.apache.dubbo.apidocs.examples.params.InnerClassRequestBean;
import org.apache.dubbo.apidocs.examples.params.InnerClassResponseBean;
import org.apache.dubbo.apidocs.examples.params.QuickStartRequestBase;
import org.apache.dubbo.apidocs.examples.params.QuickStartRequestBean;
import org.apache.dubbo.apidocs.examples.params.QuickStartRequestBean2;
import org.apache.dubbo.apidocs.examples.params.QuickStartRespBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * quick start demo.
 */
public interface IQuickStartDemo {

    /**
     * quick start demo.
     *
     * @param strParam
     * @param beanParam
     * @return org.apache.dubbo.apidocs.examples.params.QuickStartRespBean
     */
    QuickStartRespBean quickStart(List<DemoParamBean4> strParam, QuickStartRequestBean beanParam);

    /**
     * quick start demo2, request use generic.
     *
     * @param beanList
     * @param beanParam
     * @return org.apache.dubbo.apidocs.examples.params.QuickStartRespBean
     */
    QuickStartRespBean quickStart2(Map<String, DemoParamBean4> beanList, QuickStartRequestBase<QuickStartRequestBean, InnerClassRequestBean<DemoParamBean4>> beanParam);

    /**
     * quick start demo3, request use multiple generic.
     *
     * @return org.apache.dubbo.apidocs.examples.params.QuickStartRespBean
     */
    QuickStartRespBean quickStart3(QuickStartRequestBean2 beanParam);

    /**
     * quick start demo4, response use multiple generic bean, but not set generic.
     *
     * @param beanParam
     * @return org.apache.dubbo.apidocs.examples.params.QuickStartRequestBase
     */
    QuickStartRequestBase quickStart4(BigDecimal number, QuickStartRequestBean2 beanParam);

    /**
     * internal class test.
     *
     * @param testBean
     * @return org.apache.dubbo.apidocs.examples.params.InnerClassResponseBean
     */
    InnerClassResponseBean<List<String>> quickStart5(InnerClassRequestBean<List<String>> testBean);
}
