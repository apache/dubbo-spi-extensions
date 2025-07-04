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

import org.apache.dubbo.common.utils.AllowClassNotifyListener;
import org.apache.dubbo.common.utils.SerializeCheckStatus;
import org.apache.dubbo.common.utils.SerializeSecurityManager;
import org.apache.dubbo.rpc.model.FrameworkModel;

import org.apache.fory.Fory;
import org.apache.fory.exception.InsecureException;
import org.apache.fory.resolver.AllowListChecker;
import org.apache.fory.serializer.Serializer;
import org.apache.fory.serializer.SerializerFactory;

import java.io.Serializable;
import java.util.Set;

@SuppressWarnings("rawtypes")
public class ForyCheckerListener implements AllowClassNotifyListener, SerializerFactory {
    private final AllowListChecker checker;
    private volatile boolean checkSerializable;

  public ForyCheckerListener(FrameworkModel frameworkModel) {
    checker = new AllowListChecker();
    SerializeSecurityManager securityManager = frameworkModel.getBeanFactory().getOrRegisterBean(SerializeSecurityManager.class);
    securityManager.registerListener(this);
  }

  @Override
  public void notifyPrefix(Set<String> allowedList, Set<String> disAllowedList) {
    for (String prefix : allowedList) {
      checker.allowClass(prefix + "*");
    }
    for (String prefix : disAllowedList) {
      checker.disallowClass(prefix + "*");
    }
  }

  @Override
  public void notifyCheckStatus(SerializeCheckStatus status) {
    switch (status) {
      case DISABLE:
        checker.setCheckLevel(AllowListChecker.CheckLevel.DISABLE);
        return;
      case WARN:
        checker.setCheckLevel(AllowListChecker.CheckLevel.WARN);
        return;
      case STRICT:
        checker.setCheckLevel(AllowListChecker.CheckLevel.STRICT);
        return;
      default:
        throw new UnsupportedOperationException("Unsupported check level " + status);
    }
  }

  @Override
  public void notifyCheckSerializable(boolean checkSerializable) {
    this.checkSerializable = checkSerializable;
  }

  public AllowListChecker getChecker() {
    return checker;
  }

  public boolean isCheckSerializable() {
    return checkSerializable;
  }

  @Override
  public Serializer createSerializer(Fory fory, Class<?> cls) {
    if (checkSerializable && !Serializable.class.isAssignableFrom(cls)) {
      throw new InsecureException(String.format("%s is not Serializable", cls));
    }
    return null;
  }

}
