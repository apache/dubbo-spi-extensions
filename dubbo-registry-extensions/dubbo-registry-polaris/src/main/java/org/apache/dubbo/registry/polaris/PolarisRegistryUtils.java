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

import com.tencent.polaris.common.registry.BaseBootConfigHandler;
import com.tencent.polaris.common.registry.PolarisOperator;
import com.tencent.polaris.common.registry.PolarisOperators;
import org.apache.dubbo.common.URL;

public class PolarisRegistryUtils {

    public static PolarisOperator getOrCreatePolarisOperator(URL registryURL) {
        synchronized (PolarisOperators.INSTANCE) {
            String host = registryURL.getHost();
            int port = registryURL.getPort();
            PolarisOperator existsOperator = PolarisOperators.INSTANCE.getPolarisOperator(host, port);
            if (null != existsOperator) {
                return existsOperator;
            } else {
                PolarisOperator polarisOperator = new PolarisOperator(
                    host, port, registryURL.getParameters(), new BaseBootConfigHandler());
                PolarisOperators.INSTANCE.addPolarisOperator(polarisOperator);
                return polarisOperator;
            }
        }
    }

    public static void removePolarisOperator(URL registryURL) {
        synchronized (PolarisOperators.INSTANCE) {
            String host = registryURL.getHost();
            int port = registryURL.getPort();
            PolarisOperators.INSTANCE.deletePolarisOperator(host, port);
        }
    }

}
