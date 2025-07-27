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

import org.apache.fury.Fury;
import org.apache.fury.collection.Tuple2;
import org.apache.fury.config.CompatibleMode;
import org.apache.fury.memory.MemoryBuffer;
import org.apache.fury.memory.MemoryUtils;
import org.apache.fury.util.LoaderBinding;

/**
 * Fury serialization for dubbo. This integration support type forward/backward compatibility.
 */
public class FuryCompatibleSerialization extends BaseFurySerialization {
  public static final byte FURY_SERIALIZATION_ID = 29;
  private static final ThreadLocal<Tuple2<LoaderBinding, MemoryBuffer>> furyFactory =
      ThreadLocal.withInitial(
          () -> {
            LoaderBinding binding =
                new LoaderBinding(
                    classLoader ->
                        Fury.builder()
                            .withRefTracking(true)
                            .withStringCompressed(true)
                            .requireClassRegistration(false)
                            .withCompatibleMode(CompatibleMode.COMPATIBLE)
                            .withClassLoader(classLoader)
                            .build());
            MemoryBuffer buffer = MemoryUtils.buffer(32);
            return Tuple2.of(binding, buffer);
          });

  public byte getContentTypeId() {
    return FURY_SERIALIZATION_ID;
  }

  public String getContentType() {
    return "fury/compatible";
  }

  @Override
  protected Tuple2<LoaderBinding, MemoryBuffer> getFury() {
    return furyFactory.get();
  }
}
