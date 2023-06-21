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

package org.apache.dubbo.registry.polaris;

import com.tencent.polaris.api.pojo.Instance;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.utils.NetUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class PolarisRegistryTest {

    private static PolarisRegistry polarisRegistry;

    @BeforeAll
    public static void setup() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("namespace", "dubbo-java-test");
        parameters.put("persist_enable", "false");
        URL url = new URL("polaris", "127.0.0.1", 8091, parameters);
        polarisRegistry = new PolarisRegistry(url);
    }

    @AfterAll
    public static void teardown() {
        if (null != polarisRegistry) {
            polarisRegistry.destroy();
        }
    }

    @Test
    public void testSubscribe() {
        int count = 10;
        AtomicBoolean notified = new AtomicBoolean(false);
        String svcName = "polaris-registry-test-service-subscribe";
        URL consumerUrl = URL.valueOf("consumer://0.0.0.0/" + svcName);
        NotifyListener listener = new NotifyListener() {
            @Override
            public void notify(List<URL> urls) {
                notified.set(true);
            }
        };
        polarisRegistry.subscribe(consumerUrl, listener);
        String host = NetUtils.getLocalHost();
        List<URL> serviceUrls = buildInstanceUrls(svcName, host, 11300, count);
        try {
            for (URL serviceUrl : serviceUrls) {
                polarisRegistry.doRegister(serviceUrl);
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            polarisRegistry.unsubscribe(consumerUrl, listener);
            Assertions.assertTrue(notified.get());
        } finally {
            for (URL serviceUrl : serviceUrls) {
                polarisRegistry.doUnregister(serviceUrl);
            }
        }
    }

    @Test
    public void testRegister() {
        String svcName = "polaris-registry-test-service-register";
        int count = 10;
        String host = NetUtils.getLocalHost();
        List<URL> serviceUrls = buildInstanceUrls(svcName, host,11300, count);
        for (URL serviceUrl : serviceUrls) {
            polarisRegistry.doRegister(serviceUrl);
        }
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            Instance[] availableInstances = polarisRegistry.getPolarisOperator().getAvailableInstances(svcName, true);
            Assertions.assertEquals(count, countInstanceByHost(host, availableInstances));
        } finally {
            for (URL serviceUrl : serviceUrls) {
                polarisRegistry.doUnregister(serviceUrl);
            }
        }
    }

    private static List<URL> buildInstanceUrls(String service, String host, int startPort, int count) {
        List<URL> serviceUrls = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            URL serviceUrl = URL.valueOf("dubbo://" + host + ":" +
                Integer.toString(startPort + i) + "/" + service + "?methods=test1,test2");
            serviceUrls.add(serviceUrl);
        }
        return serviceUrls;
    }

    private static int countInstanceByHost(String host, Instance[] instances) {
        int count = 0;
        for (Instance instance : instances) {
            if (StringUtils.isEquals(host, instance.getHost())) {
                count++;
            }
        }
        return count;
    }

}
