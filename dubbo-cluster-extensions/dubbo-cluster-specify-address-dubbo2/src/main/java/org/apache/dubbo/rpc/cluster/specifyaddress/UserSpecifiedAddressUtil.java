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

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.threadlocal.InternalThreadLocal;
import org.apache.dubbo.rpc.cluster.common.SpecifyAddress;

public class UserSpecifiedAddressUtil {
    private final static InternalThreadLocal<SpecifyAddress<URL>> ADDRESS = new InternalThreadLocal<>();

    /**
     * Set specified address to next invoke
     * use setSpecifyAddress(SpecifyAddress<URL> specifyAddress) replace
     *
     * @param address specified address
     */
    @Deprecated
    public static void setAddress(Address address) {
        SpecifyAddress<URL> specifyAddress = new SpecifyAddress<>();
        specifyAddress.setIp(address.getIp());
        specifyAddress.setPort(address.getPort());
        specifyAddress.setUrlAddress(address.getUrlAddress());
        specifyAddress.setNeedToCreate(address.isNeedToCreate());
        ADDRESS.set(specifyAddress);
    }

    public static void setSpecifyAddress(SpecifyAddress<URL> specifyAddress) {
        ADDRESS.set(specifyAddress);
    }

    public static SpecifyAddress<URL> current() {
        return ADDRESS.get();
    }

    public static void removeAddress() {
        ADDRESS.remove();
    }
}
