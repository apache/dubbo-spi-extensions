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

package org.apache.dubbo.common.serialize.fory.dubbo;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.serialize.ObjectOutput;
import org.apache.dubbo.common.serialize.Serialization;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.apache.fory.Fory;
import org.apache.fory.collection.Tuple2;
import org.apache.fory.memory.MemoryBuffer;
import org.apache.fory.util.LoaderBinding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
/**
 * Fory serialization framework integration with dubbo.
 *
 * @author chaokunyang
 */
public abstract class BaseForySerialization implements Serialization {
  protected abstract Tuple2<LoaderBinding, MemoryBuffer> getFory();

  public ObjectOutput serialize(URL url, OutputStream output) throws IOException {
    Tuple2<LoaderBinding, MemoryBuffer> tuple2 = getFory();
    tuple2.f0.setClassLoader(Thread.currentThread().getContextClassLoader());
    Fory fory = tuple2.f0.get();
    ForyCheckerListener checkerListener = getCheckerListener(url);
    fory.getClassResolver().setClassChecker(checkerListener.getChecker());
    fory.getClassResolver().setSerializerFactory(checkerListener);
    return new ForyObjectOutput(fory, tuple2.f1, output);
  }

  public ObjectInput deserialize(URL url, InputStream input) throws IOException {
    Tuple2<LoaderBinding, MemoryBuffer> tuple2 = getFory();
    tuple2.f0.setClassLoader(Thread.currentThread().getContextClassLoader());
    Fory fory = tuple2.f0.get();
    ForyCheckerListener checkerListener = getCheckerListener(url);
    fory.getClassResolver().setClassChecker(checkerListener.getChecker());
    return new ForyObjectInput(fory, tuple2.f1, input);
  }

  private static ForyCheckerListener getCheckerListener(URL url) {
    return Optional.ofNullable(url)
        .map(URL::getOrDefaultFrameworkModel)
        .orElseGet(FrameworkModel::defaultModel)
        .getBeanFactory()
        .getBean(ForyCheckerListener.class);
  }
}
