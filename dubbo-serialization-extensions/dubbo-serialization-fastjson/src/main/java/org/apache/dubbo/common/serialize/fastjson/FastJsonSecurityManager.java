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
package org.apache.dubbo.common.serialize.fastjson;

import org.apache.dubbo.common.utils.AllowClassNotifyListener;
import org.apache.dubbo.common.utils.SerializeCheckStatus;

import java.util.Set;

/**
 * FastJsonSecurityManager
 */
public class FastJsonSecurityManager implements AllowClassNotifyListener {


    @Override
    public void notifyPrefix(Set<String> allowedList, Set<String> disAllowedList) {

    }

    @Override
    public void notifyCheckStatus(SerializeCheckStatus status) {

    }

    @Override
    public void notifyCheckSerializable(boolean checkSerializable) {

    }

    public static class Handler  {

    }
}
