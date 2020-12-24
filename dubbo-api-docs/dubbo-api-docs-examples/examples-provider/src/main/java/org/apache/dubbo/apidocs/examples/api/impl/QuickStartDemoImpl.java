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
import org.apache.dubbo.apidocs.examples.params.QuickStartRequestBean;
import org.apache.dubbo.apidocs.examples.params.QuickStartRespBean;
import org.apache.dubbo.config.annotation.DubboService;

/**
 * quick start demo implement.
 *
 * @author klw(213539 @ qq.com)
 * @date 2020/12/23 17:21
 */
@DubboService
@ApiModule(value = "quick start demo", apiInterface = IQuickStartDemo.class, version = "v0.1")
public class QuickStartDemoImpl implements IQuickStartDemo {

    @ApiDoc(value = "quick start demo", version = "v0.1", description = "this api is a quick start demo", responseClassDescription="A quick star response bean")
    @Override
    public QuickStartRespBean quickStart(@RequestParam(value = "strParam", required = true) String strParam, QuickStartRequestBean beanParam) {
        return new QuickStartRespBean(200, "hello " + beanParam.getName() + ", " + beanParam.toString());
    }
}
