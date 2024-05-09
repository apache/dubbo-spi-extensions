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

import io.fury.Fury;
import io.fury.exception.InsecureException;
import io.fury.resolver.AllowListChecker;
import io.fury.serializer.Serializer;
import io.fury.serializer.SerializerFactory;
import java.io.Serializable;
import org.apache.dubbo.rpc.model.FrameworkModel;

@SuppressWarnings("rawtypes")
public class FuryCheckerListener implements SerializerFactory {

  private final AllowListChecker checker;
  private final boolean checkSerializable;

  public FuryCheckerListener(FrameworkModel frameworkModel) {
    checker = new AllowListChecker();
    // serializable check from dubbo 3.1
    checker.setCheckLevel(AllowListChecker.CheckLevel.DISABLE);
    checkSerializable = false;
  }

  public AllowListChecker getChecker() {
    return checker;
  }

  @Override
  public Serializer createSerializer(Fury fury, Class<?> cls) {
    if (checkSerializable && !Serializable.class.isAssignableFrom(cls)) {
      throw new InsecureException(String.format("%s is not Serializable", cls));
    }
    return null;
  }
}
