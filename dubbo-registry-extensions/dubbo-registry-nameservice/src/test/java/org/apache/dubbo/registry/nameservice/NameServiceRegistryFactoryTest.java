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

package org.apache.dubbo.registry.nameservice;

import org.apache.dubbo.registry.Registry;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.annotation.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(value = {NameServiceRegistryFactory.class})
public class NameServiceRegistryFactoryTest {

    @Mock
    private NameServiceRegistry registry;

    @Test
    public void createRegistryTest() throws Exception {

        PowerMockito.whenNew(NameServiceRegistry.class).withAnyArguments().thenReturn(registry);
        NameServiceRegistryFactory registryFactory = new NameServiceRegistryFactory();
        Registry registry = registryFactory.createRegistry(null);
        Assert.assertEquals(registry, this.registry);
    }
}
