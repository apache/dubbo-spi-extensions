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
package org.apache.dubbo.common.serialize.fury.dubbo;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.apache.fury.Fury;
import org.apache.fury.collection.Tuple2;
import org.apache.fury.memory.MemoryBuffer;
import org.apache.fury.util.LoaderBinding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

/**
 * Fury serialization framework integration with dubbo.
 */
public abstract class BaseFurySerialization implements Serialization {
  protected abstract Tuple2<LoaderBinding, MemoryBuffer> getFury();

  public ObjectOutput serialize(URL url, OutputStream output) throws IOException {
    Tuple2<LoaderBinding, MemoryBuffer> tuple2 = getFury();
    tuple2.f0.setClassLoader(Thread.currentThread().getContextClassLoader());
    Fury fury = tuple2.f0.get();
    FuryCheckerListener checkerListener = getCheckerListener(url);
    fury.getClassResolver().setClassChecker(checkerListener.getChecker());
    fury.getClassResolver().setSerializerFactory(checkerListener);
    return new FuryObjectOutput(fury, tuple2.f1, output);
  }

  public ObjectInput deserialize(URL url, InputStream input) throws IOException {
    Tuple2<LoaderBinding, MemoryBuffer> tuple2 = getFury();
    tuple2.f0.setClassLoader(Thread.currentThread().getContextClassLoader());
    Fury fury = tuple2.f0.get();
    FuryCheckerListener checkerListener = getCheckerListener(url);
    fury.getClassResolver().setClassChecker(checkerListener.getChecker());
    return new FuryObjectInput(fury, tuple2.f1, input);
  }

  private static FuryCheckerListener getCheckerListener(URL url) {
    return Optional.ofNullable(url)
        .map(URL::getOrDefaultFrameworkModel)
        .orElseGet(FrameworkModel::defaultModel)
        .getBeanFactory()
        .getBean(FuryCheckerListener.class);
  }
}
