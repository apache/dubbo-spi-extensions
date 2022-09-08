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

import java.net.InetSocketAddress;

import org.apache.dubbo.common.utils.NetUtils;

public interface RocketMQProtocolConstant {


    static final String CONSUMER_CROUP_NAME = "dubbo-roucketmq-consumer-group";

    static final String PRODUCER_CROUP_NAME = "dubbo-roucketmq-producer-group";

    static final String DUBBO_DEFAULT_PROTOCOL_TOPIC = "dubbo_default_protocol_topic";

    static final String SEND_ADDRESS = "send_address";

    static final String URL_STRING = "url_string";

    static final InetSocketAddress LOCAL_ADDRESS = InetSocketAddress.createUnresolved(NetUtils.getLocalHost(), 9876);
}
