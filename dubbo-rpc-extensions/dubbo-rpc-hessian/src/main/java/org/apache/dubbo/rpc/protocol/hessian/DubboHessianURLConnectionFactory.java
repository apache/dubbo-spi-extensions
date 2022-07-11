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

package org.apache.dubbo.rpc.protocol.hessian;

import org.apache.dubbo.remoting.Constants;

import com.caucho.hessian.client.HessianConnection;
import com.caucho.hessian.client.HessianURLConnectionFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

public class DubboHessianURLConnectionFactory extends HessianURLConnectionFactory {

    @Override
    public HessianConnection open(URL url) throws IOException {
        HessianConnection connection = super.open(url);
        Map<String, String> attachments = HessianProtocolClientFilter.getAttachments();
        if (attachments != null) {
            attachments.forEach((k, v) -> connection.addHeader(Constants.DEFAULT_EXCHANGER + k, v));
        }

        return connection;
    }
}
