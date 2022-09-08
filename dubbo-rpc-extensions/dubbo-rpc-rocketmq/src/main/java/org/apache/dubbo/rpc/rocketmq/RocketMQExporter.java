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
package org.apache.dubbo.rpc.rocketmq;

import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.DEFAULT_CATEGORY;

import java.util.Map;
import java.util.Objects;
import java.util.zip.CRC32;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.protocol.AbstractExporter;

public class RocketMQExporter<T> extends AbstractExporter<T> {

    private static final String DEFAULT_PARAM_VALUE = "";

    private static final String NAME_SEPARATOR = "_";

    private final String key;

    private final Map<String, Exporter<?>> exporterMap;

    public RocketMQExporter(Invoker<T> invoker, URL url, Map<String, Exporter<?>> exporterMap) {
        super(invoker);
        this.key = toValue(url);
        this.exporterMap = exporterMap;
        this.exporterMap.put(key, this);
    }

    public void afterUnExport() {
        exporterMap.remove(key, this);
    }

    public String getKey() {
        return this.key;
    }

    private String toValue(URL url) {
        String serviceInterface = url.getParameter(INTERFACE_KEY);
        String version = url.getParameter(VERSION_KEY, DEFAULT_PARAM_VALUE);
        String group = url.getParameter(GROUP_KEY, DEFAULT_PARAM_VALUE);

        String value = null;
        if (Objects.equals(url.getParameter("groupModel"), "topic")) {
            value = DEFAULT_CATEGORY + NAME_SEPARATOR + serviceInterface + NAME_SEPARATOR + version + NAME_SEPARATOR + group;
        } else {
            value = DEFAULT_CATEGORY + NAME_SEPARATOR + serviceInterface;
        }
        CRC32 crc32 = new CRC32();
        crc32.update(value.getBytes());
        value = value.replace(".", "-") + NAME_SEPARATOR + Long.toString(crc32.getValue());
        return value;
    }

}
