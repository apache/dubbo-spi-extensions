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
package org.apache.dubbo.tag.subnets.config;

import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.config.ConfigPostProcessor;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ScopeModelAware;
import org.apache.dubbo.tag.subnets.utils.SubnetUtil;

import static org.apache.dubbo.common.utils.NetUtils.getLocalHost;


@Activate
public class SubnetTagConfigPostProcessor implements ConfigPostProcessor, ScopeModelAware {
    private ApplicationModel applicationModel;

    @Override
    public void setApplicationModel(ApplicationModel applicationModel) {
        this.applicationModel = applicationModel;
    }

    @Override
    public void postProcessServiceConfig(ServiceConfig serviceConfig) {
        if (StringUtils.isNotBlank(serviceConfig.getTag())) {
            return;
        }
        if (SubnetUtil.isEmpty()) {
            String content = ConfigurationUtils.getCachedDynamicProperty(applicationModel.getDefaultModule(), SubnetUtil.TAG_SUBNETS_KEY, null);
            SubnetUtil.init(content);
        }
        if (SubnetUtil.isEmpty()) {
            return;
        }
        String providerHost = serviceConfig.getProvider().getHost();
        if (StringUtils.isBlank(providerHost)) {
            providerHost = getLocalHost();
        }
        String tagLevel = SubnetUtil.getTagLevelByHost(providerHost);
        serviceConfig.setTag(tagLevel);
    }


}
