/*
 *
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements.  See the NOTICE file distributed with
 *   this work for additional information regarding copyright ownership.
 *   The ASF licenses this file to You under the Apache License, Version 2.0
 *   (the "License"); you may not use this file except in compliance with
 *   the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.apache.dubbo.test.serialization.protobuf;

import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;

import java.util.concurrent.CompletableFuture;

public class DemoServiceImpl implements DemoService {

    @Override
    public Empty testVoid(Empty request) {
        return Empty.getDefaultInstance();
    }

    @Override
    public CompletableFuture<Empty> testVoidAsync(Empty request) {
        return CompletableFuture.completedFuture(testVoid(request));
    }

    @Override
    public StringValue testString(StringValue request) {
        return StringValue.of("Hello " + request.getValue());
    }

    @Override
    public CompletableFuture<StringValue> testStringAsync(StringValue request) {
        return CompletableFuture.completedFuture(testString(request));
    }

    @Override
    public Int32Value testBase(Int32Value request) {
        return Int32Value.of(100 + request.getValue());
    }

    @Override
    public CompletableFuture<Int32Value> testBaseAsync(Int32Value request) {
        return CompletableFuture.completedFuture(testBase(request));
    }

    @Override
    public BigPerson testObject(BigPerson request) {
        return request;
    }

    @Override
    public CompletableFuture<BigPerson> testObjectAsync(BigPerson request) {
        return CompletableFuture.completedFuture(testObject(request));
    }
}
