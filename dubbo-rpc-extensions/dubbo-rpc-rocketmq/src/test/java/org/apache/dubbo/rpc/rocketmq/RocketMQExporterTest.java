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

package org.apache.dubbo.rpc.rocketmq;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class RocketMQExporterTest {

    RocketMQExporter<Object> rocketMQExporter;

    Map<String, Exporter<?>> exporterMap = new HashMap<>();

    @Before
    public void before() {
        String urlString =
            "nameservice://localhost:9876/org.apache.dubbo.registry.RegistryService?application=rocketmq-provider&dubbo=2.0.2&interface=org.apache.dubbo.registry.RegistryService&pid=8990&release=3.0.7&route=false";
        URL url = URLBuilder.valueOf(urlString);

        Invoker<Object> invocation = Mockito.mock(Invoker.class);
        Mockito.when(invocation.getInterface()).thenReturn(Object.class);
        Mockito.when(invocation.getUrl()).thenReturn(url);
        rocketMQExporter = new RocketMQExporter<Object>(invocation, url, exporterMap);
        Assert.assertEquals("providers_org-apache-dubbo-registry-RegistryService_3176292646", rocketMQExporter.getKey());
        Assert.assertEquals(invocation, rocketMQExporter.getInvoker());
        Assert.assertEquals(exporterMap, ReflectUtils.getFieldValue(rocketMQExporter, "exporterMap"));

        url = URLBuilder.from(url).addParameter("groupModel", "topic").addParameter(CommonConstants.VERSION_KEY, "1.0.0")
            .addParameter(CommonConstants.GROUP_KEY, "laohu").build();
        rocketMQExporter = new RocketMQExporter<Object>(invocation, url, exporterMap);
        Assert.assertEquals("providers_org-apache-dubbo-registry-RegistryService_1-0-0_laohu_1765322527", rocketMQExporter.getKey());
    }

    @Test
    public void afterUnExportTest() {
        Assert.assertFalse(exporterMap.isEmpty());
        rocketMQExporter.afterUnExport();

    }
}
