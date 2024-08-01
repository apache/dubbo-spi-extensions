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

package org.apache.dubbo.common.serialize.fury;

import org.apache.dubbo.common.serialize.fury.dubbo.FuryObjectInput;
import org.apache.dubbo.common.serialize.model.person.FullAddress;
import org.apache.fury.Fury;
import org.apache.fury.config.Language;
import org.apache.fury.memory.MemoryBuffer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;


public class FuryObjectInputTest {
    private FuryObjectInput furyObjectInput;
    private static final Fury fury = Fury.builder().withLanguage(Language.JAVA).build();
    private static final MemoryBuffer buffer = fury.getBuffer();
    @Test
    public void testWrongClassInput(){
        this.furyObjectInput = new FuryObjectInput(fury,buffer,new ByteArrayInputStream("{animal: 'cat'}".getBytes()));
        assert (furyObjectInput.readObject(FullAddress.class) == null);
    }
    @Test
    public void testEmptyByteArrayForEmptyInput() throws IOException {
        this.furyObjectInput = new FuryObjectInput(fury,buffer,new ByteArrayInputStream(new byte[]{'a'}));
        assert (furyObjectInput.readByte() == 97);
    }
}
