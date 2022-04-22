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
import org.apache.dubbo.apidocs.examples.api.IQuickStartDemo;
import org.apache.dubbo.apidocs.examples.params.DemoParamBean4;
import org.apache.dubbo.apidocs.examples.params.InnerClassRequestBean;
import org.apache.dubbo.apidocs.examples.params.InnerClassResponseBean;
import org.apache.dubbo.apidocs.examples.params.QuickStartRequestBase;
import org.apache.dubbo.apidocs.examples.params.QuickStartRequestBean;
import org.apache.dubbo.apidocs.examples.params.QuickStartRequestBean2;
import org.apache.dubbo.apidocs.examples.params.QuickStartRespBean;
import org.apache.dubbo.config.annotation.DubboService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * quick start demo implement.
 *
 * @date 2020/12/23 17:21
 */
@DubboService(version = "${demo.apiversion.quickstart}", group = "demoGroup")
@ApiModule(value = "quick start demo", apiInterface = IQuickStartDemo.class)
public class QuickStartDemoImpl implements IQuickStartDemo {

    @ApiDoc(value = "quick start demo", version = "v0.1", description = "this api is a quick start demo", responseClassDescription = "A quick star response bean")
    @Override
    public QuickStartRespBean quickStart(@RequestParam(value = "strParamxxx", required = true) List<DemoParamBean4> strParam, QuickStartRequestBean beanParam) {
        return new QuickStartRespBean(200, "hello " + beanParam.getName() + ", " + beanParam.toString());
    }

    @ApiDoc(value = "quick start demo, request use generic.", version = "v0.1", description = "quick start demo, request use generic.", responseClassDescription = "A quick star response bean")
    @Override
    public QuickStartRespBean quickStart2(Map<String, DemoParamBean4> beanList, QuickStartRequestBase<QuickStartRequestBean, InnerClassRequestBean<DemoParamBean4>> beanParam) {
        return new QuickStartRespBean(200, "【" + beanParam.getMethod() + "】hello " + beanParam.getBody3() + ", " + beanParam.toString());
    }

    @ApiDoc(value = "multiple generic demo", version = "v0.1", description = "multiple generic demo.", responseClassDescription = "A quick star response bean")
    @Override
    public QuickStartRespBean quickStart3(QuickStartRequestBean2 beanParam) {
        return new QuickStartRespBean(200, "quickStart3, multiple generic demo");
    }

    @ApiDoc(value = "response use multiple generic bean", description = "response use multiple generic bean, but not set generic.", responseClassDescription = "A quick star response bean")
    @Override
    public QuickStartRequestBase quickStart4(BigDecimal number, QuickStartRequestBean2 beanParam) {
        QuickStartRequestBase response = new QuickStartRequestBase();
        response.setBody(Arrays.asList("body2-1", "body2-2"));
        response.setBody3("body3");
        response.setMethod("test");
        return response;
    }

    @ApiDoc(value = "internal class test", description = "internal class test.", responseClassDescription = "Internal class test, response bean.")
    @Override
    public InnerClassResponseBean<List<String>> quickStart5(InnerClassRequestBean<List<String>> testBean) {
        InnerClassResponseBean<List<String>> responseBean = new InnerClassResponseBean<>();
        List<String> respT = new ArrayList<>(2);
        respT.add("respT string1");
        respT.add("respT string2");
        responseBean.settResp(respT);

        return responseBean;
    }
}
