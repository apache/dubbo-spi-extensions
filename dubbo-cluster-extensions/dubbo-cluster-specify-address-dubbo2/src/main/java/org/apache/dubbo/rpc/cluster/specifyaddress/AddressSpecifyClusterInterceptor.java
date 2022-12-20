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
package org.apache.dubbo.rpc.cluster.specifyaddress;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.registry.integration.DynamicDirectory;
import org.apache.dubbo.registry.integration.RegistryDirectory;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.cluster.Directory;
import org.apache.dubbo.rpc.cluster.interceptor.ClusterInterceptor;
import org.apache.dubbo.rpc.cluster.support.AbstractClusterInvoker;

import java.lang.reflect.Field;

/**
 * The SPECIFY ADDRESS field is handed over to the attachment by the thread
 */
@Activate(group = CommonConstants.CONSUMER)
public class AddressSpecifyClusterInterceptor implements ClusterInterceptor {

    @Override
    public void before(AbstractClusterInvoker<?> clusterInvoker, Invocation invocation) {
        Address current = UserSpecifiedAddressUtil.getAddress();
        if (current != null) {
            invocation.put(Address.name, current);
        }

        try {
            Field nodeInvoker = clusterInvoker.getClass().getDeclaredField("clusterInvoker");
            nodeInvoker.setAccessible(true);
            Object clusterObj = nodeInvoker.get(clusterInvoker);
            if (clusterObj instanceof AbstractClusterInvoker) {
                // Disable the existence check of the local cluster service
                Directory<?> directory = ((AbstractClusterInvoker<?>)clusterObj).getDirectory();
                if (directory instanceof RegistryDirectory) {
                    RegistryDirectory<?> rd = (RegistryDirectory<?>) directory;

                    Field forbiddenField = DynamicDirectory.class.getDeclaredField("forbidden");
                    forbiddenField.setAccessible(true);
                    forbiddenField.set(rd, false);
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException ignore) {
        }
    }


    @Override
    public void after(AbstractClusterInvoker<?> clusterInvoker, Invocation invocation) {

    }
}
