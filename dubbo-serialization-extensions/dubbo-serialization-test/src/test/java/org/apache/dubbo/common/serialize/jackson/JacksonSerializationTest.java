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
package org.apache.dubbo.common.serialize.jackson;

import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

/**
 * {@link JacksonSerialization} Unit Test
 *
 * @author aofall
 */
public class JacksonSerializationTest {

    private JacksonSerialization jacksonSerialization;

    @BeforeEach
    public void setUp() {
        this.jacksonSerialization = new JacksonSerialization();
    }

    @Test
    public void testContentTypeId() {
        MatcherAssert.assertThat(jacksonSerialization.getContentTypeId(), is((byte) 18));
    }

    @Test
    public void testContentType() {
        MatcherAssert.assertThat(jacksonSerialization.getContentType(), is("application/json"));
    }

    @Test
    public void testObjectOutput() throws IOException {
        ObjectOutput objectOutput = jacksonSerialization.serialize(null, mock(OutputStream.class));
        assertThat(objectOutput, Matchers.instanceOf(JacksonObjectOutput.class));
    }

    @Test
    public void testObjectInput() throws IOException {
        ObjectInput objectInput = jacksonSerialization.deserialize(null, mock(InputStream.class));
        assertThat(objectInput, Matchers.instanceOf(JacksonObjectInput.class));
    }

}
