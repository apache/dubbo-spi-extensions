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
package org.apache.dubbo.gateway.consumer.test;

import org.apache.dubbo.common.beanutil.JavaBeanDescriptor;
import org.apache.dubbo.common.beanutil.JavaBeanSerializeUtil;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.gateway.consumer.filter.OmnSerFilter;
import org.apache.dubbo.rpc.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.apache.dubbo.gateway.common.OmnipotentCommonConstants.SPECIFY_ADDRESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

public class OmnSerFilterTest {

    @InjectMocks
    private OmnSerFilter omnSerFilter;

    @Mock
    private Invoker<Invoker<?>> invoker;

    @Mock
    private Invocation invocation;

    @Mock
    private Result result;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testInvokeWithSpecifyAddress() throws RpcException {
        // Set up
        when(invocation.get(SPECIFY_ADDRESS)).thenReturn("127.0.0.1");
        when(invoker.invoke(any(Invocation.class))).thenReturn(result);

        // Invoke
        Result actual = omnSerFilter.invoke(invoker, invocation);

        // Assert
        assertNotNull(actual);
        // Additional assertions can be added based on expected behavior
    }

    @Test
    public void testInvokeWithoutSpecifyAddress() throws RpcException {
        // Set up
        when(invocation.get(SPECIFY_ADDRESS)).thenReturn(null);
        when(invoker.invoke(any(Invocation.class))).thenReturn(result);

        // Invoke
        Result actual = omnSerFilter.invoke(invoker, invocation);

        // Assert
        assertNotNull(actual);
        // Additional assertions can be added based on expected behavior
    }

    @Test
    public void testOnResponseWithPrimitives() {
        // Set up
        Object primitives = Integer.valueOf(10);
        when(result.getValue()).thenReturn(primitives);

        // Invoke
        omnSerFilter.onResponse(result, invoker, invocation);

        // Assert
        assertEquals(primitives, result.getValue());
        // Additional assertions can be added based on expected behavior for primitive types
    }

    @Test
    public void testOnResponseWithPojo() {
        // Set up
        JavaBeanDescriptor pojo = new JavaBeanDescriptor();
        when(result.getValue()).thenReturn(pojo);

        // Invoke
        omnSerFilter.onResponse(result, invoker, invocation);

        // Assert
        assertSame(pojo, result.getValue());
        // Additional assertions can be added based on expected behavior for pojo types
    }

    @Test
    public void testOnResponseWithCollection() {
        // Set up
        Collection<JavaBeanDescriptor> collection = new ArrayList<>();
        JavaBeanDescriptor descriptor = new JavaBeanDescriptor("org.apache.dubbo.gateway.consumer.test.Person",1);
        descriptor.setProperty("name", "org.apache.dubbo.gateway.consumer.test.Person");
        collection.add(descriptor);
        when(result.getValue()).thenReturn(collection);

        // Invoke
        omnSerFilter.onResponse(result, invoker, invocation);

        // Assert
        // Ensure the collection is unchanged
        assertEquals(1, ((Collection<?>) result.getValue()).size());
        assertEquals(Person.class.toString(), ((Collection<?>) result.getValue()).iterator().next().toString());
    }

    @Test
    public void testOnResponseWithMap() {
        // Set up
        Map map = new HashMap<>();
        JavaBeanDescriptor descriptor = new JavaBeanDescriptor("org.apache.dubbo.gateway.consumer.test.Person", 1);
        map.put("org.apache.dubbo.gateway.consumer.test.Person", descriptor);
        when(result.getValue()).thenReturn(map);
        descriptor.setProperty("name", "org.apache.dubbo.gateway.consumer.test.Person");

        // Invoke
        omnSerFilter.onResponse(result, invoker, invocation);

        // Assert
        // Additional assertions can be added based on expected behavior for map types
        assertEquals(1, ((Map<?,?>) result.getValue()).size());
        assertEquals(Person.class.toString(), (((Map<?,?>) result.getValue()).get("org.apache.dubbo.gateway.consumer.test.Person").toString()));

    }
}
