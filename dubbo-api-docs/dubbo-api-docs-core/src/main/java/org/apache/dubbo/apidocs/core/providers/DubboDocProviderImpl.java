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
package org.apache.dubbo.apidocs.core.providers;

import org.apache.dubbo.apidocs.core.DubboApiDocsCache;
import org.apache.dubbo.config.annotation.DubboService;

import lombok.extern.slf4j.Slf4j;

/**
 * The api implementation of Dubbo doc.
 * @author klw(213539@qq.com)
 * 2020/10/29 17:38
 */
@Slf4j
@DubboService
public class DubboDocProviderImpl implements IDubboDocProvider {

    @Override
    public String apiModuleList() {
        return DubboApiDocsCache.getAllApiModuleInfo();
    }

    @Override
    public String apiModuleInfo(String apiInterfaceClassName) {
        return DubboApiDocsCache.getApiModuleStr(apiInterfaceClassName);
    }

    @Override
    public String apiParamsResponseInfo(String apiInterfaceClassNameMethodName) {
        return DubboApiDocsCache.getApiParamsAndRespStr(apiInterfaceClassNameMethodName);
    }
}
