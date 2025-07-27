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

import org.apache.fory.Fory;
import org.apache.fory.collection.Tuple2;
import org.apache.fory.memory.MemoryBuffer;
import org.apache.fory.memory.MemoryUtils;
import org.apache.fory.util.LoaderBinding;

/**
 * Fory serialization for dubbo. This integration doesn't allow type inconsistency between
 * serialization and deserialization peer.
 */
public class ForySerialization extends BaseForySerialization {
  public static final byte FORY_SERIALIZATION_ID = 28;
  private static final ThreadLocal<Tuple2<LoaderBinding, MemoryBuffer>> foryFactory =
      ThreadLocal.withInitial(
          () -> {
            LoaderBinding binding =
                new LoaderBinding(
                    classLoader ->
                        Fory.builder()
                            .withRefTracking(true)
                            .withStringCompressed(true)
                            .requireClassRegistration(false)
                            .withClassLoader(classLoader)
                            .build());
            MemoryBuffer buffer = MemoryUtils.buffer(32);
            return Tuple2.of(binding, buffer);
          });

  public byte getContentTypeId() {
    return FORY_SERIALIZATION_ID;
  }

  public String getContentType() {
    return "fory/consistent";
  }

  @Override
  protected Tuple2<LoaderBinding, MemoryBuffer> getFory() {
    return foryFactory.get();
  }
}
