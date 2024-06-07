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
package org.apache.dubbo.registry.etcd;

public class EtcdCompatibleRegistryFactory extends EtcdServiceDiscoveryFactory {

    // The extension name of dubbo-registry-etcd is etcd3 and user should config the URL as 'etcd3://localhost:2379'.
    // But the extension name of dubbo-metadata-report-etcd and dubbo-configcenter-etcd are etcd
    // and user should config the URL as 'etcd://localhost:2379'.
    // To avoid confusion for users when configuring URLs in registry, rename the dubbo-registry-etcd extension name
    // from etcd3 to etcd, and use extend class to compatible the old version of dubbo-registry-etcd.
    // It can unify the extension name and avoid confusion for users and compatible the old version

}
