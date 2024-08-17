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
package org.apache.dubbo.common.serialize.jdk;

import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.java.JavaObjectOutput;
import org.apache.dubbo.common.serialize.java.JavaSerialization;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

/**
 * {@link JavaSerialization} Unit Test
 */
public class JdkSerializationTest {

    private JavaSerialization JavaSerialization;

    @BeforeEach
    public void setUp() {
        this.JavaSerialization = new JavaSerialization();
    }

    @Test
    public void testContentTypeId() {
        MatcherAssert.assertThat(JavaSerialization.getContentTypeId(), is((byte) 3));
    }

    @Test
    public void testContentType() {
        MatcherAssert.assertThat(JavaSerialization.getContentType(), is("x-application/java"));
    }

    @Test
    public void testObjectOutput() throws IOException {
        ObjectOutput objectOutput = JavaSerialization.serialize(null, mock(OutputStream.class));
        assertThat(objectOutput, Matchers.instanceOf(JavaObjectOutput.class));
    }

}
