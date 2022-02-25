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
package org.apache.dubbo.rpc.cluster.specifyaddress;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.url.component.DubboServiceAddressURL;
import org.apache.dubbo.common.url.component.PathURLAddress;
import org.apache.dubbo.common.url.component.URLParam;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.model.ApplicationModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;

public class DefaultUserSpecifiedServiceAddressBuilder implements UserSpecifiedServiceAddressBuilder {
    public final static String NAME = "default";

    private final ExtensionLoader<Protocol> protocolExtensionLoader;

    public DefaultUserSpecifiedServiceAddressBuilder(ApplicationModel applicationModel) {
        this.protocolExtensionLoader = applicationModel.getExtensionLoader(Protocol.class);
    }

    @Override
    public <T> URL buildAddress(List<Invoker<T>> invokers, Address address, Invocation invocation, URL consumerUrl) {
        if (!invokers.isEmpty()) {
            URL template = invokers.iterator().next().getUrl();
            template = template.setHost(address.getIp());
            if (address.getPort() != 0) {
                template = template.setPort(address.getPort());
            }
            return template;
        } else {
            String ip = address.getIp();
            int port = address.getPort();
            String protocol = consumerUrl.getParameter(PROTOCOL_KEY, DUBBO);
            if (port == 0) {
                port = protocolExtensionLoader.getExtension(protocol).getDefaultPort();
            }
            return new DubboServiceAddressURL(
                new PathURLAddress(protocol, null, null, consumerUrl.getPath(), ip, port),
                URLParam.parse(""), consumerUrl, null);
        }
    }

    @Override
    public <T> URL rebuildAddress(List<Invoker<T>> invokers, Address address, Invocation invocation, URL consumerUrl) {
        URL url = address.getUrlAddress();
        Map<String, String> parameters = new HashMap<>(url.getParameters());
        parameters.put(VERSION_KEY, consumerUrl.getVersion());
        parameters.put(GROUP_KEY, consumerUrl.getGroup());
        String protocol = StringUtils.isEmpty(url.getProtocol()) ? consumerUrl.getParameter(PROTOCOL_KEY, DUBBO) : url.getProtocol();
        return new DubboServiceAddressURL(
            new PathURLAddress(protocol, null, null, consumerUrl.getPath(), url.getHost(), url.getPort()),
            URLParam.parse(parameters), consumerUrl, null);
    }
}
