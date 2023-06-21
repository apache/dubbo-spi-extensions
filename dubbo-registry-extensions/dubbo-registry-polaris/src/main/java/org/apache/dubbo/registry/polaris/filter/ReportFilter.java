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

package org.apache.dubbo.registry.polaris.filter;


import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.common.registry.PolarisOperator;
import com.tencent.polaris.common.registry.PolarisOperatorDelegate;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;
import com.tencent.polaris.common.exception.PolarisBlockException;
import org.apache.dubbo.rpc.RpcContext;

@Activate(group = CommonConstants.CONSUMER, order = Integer.MIN_VALUE)
public class ReportFilter extends PolarisOperatorDelegate implements Filter, Filter.Listener {

    private static final String LABEL_START_TIME = "reporter_filter_start_time";

    private static final String LABEL_REMOTE_HOST = "reporter_remote_host_store";

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportFilter.class);

    public ReportFilter() {
        LOGGER.info("[POLARIS] init polaris reporter");
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        invocation.put(LABEL_START_TIME, System.currentTimeMillis());
        invocation.put(LABEL_REMOTE_HOST, RpcContext.getContext().getRemoteHost());
        return invoker.invoke(invocation);
    }

    @Override
    public void onResponse(Result appResponse, Invoker<?> invoker, Invocation invocation) {
        PolarisOperator polarisOperator = getPolarisOperator();
        if (null == polarisOperator) {
            return;
        }
        String callerIp = (String) invocation.get(LABEL_REMOTE_HOST);
        Long startTimeMilli = (Long) invocation.get(LABEL_START_TIME);
        RetStatus retStatus = RetStatus.RetSuccess;
        int code = 0;
        if (appResponse.hasException()) {
            retStatus = RetStatus.RetFail;
            code = -1;
        }
        URL url = invoker.getUrl();
        long delay = System.currentTimeMillis() - startTimeMilli;
        polarisOperator.reportInvokeResult(url.getServiceInterface(), invocation.getMethodName(), url.getHost(),
            url.getPort(), callerIp, delay, retStatus, code);
    }

    @Override
    public void onError(Throwable t, Invoker<?> invoker, Invocation invocation) {
        PolarisOperator polarisOperator = getPolarisOperator();
        if (null == polarisOperator) {
            return;
        }
        String callerIp = (String) invocation.get(LABEL_REMOTE_HOST);
        Long startTimeMilli = (Long) invocation.get(LABEL_START_TIME);
        RetStatus retStatus = RetStatus.RetFail;
        int code = -1;
        if (t instanceof RpcException) {
            RpcException rpcException = (RpcException) t;
            code = rpcException.getCode();
            if (isFlowControl(rpcException)) {
                retStatus = RetStatus.RetFlowControl;
            }
            if (rpcException.isTimeout()) {
                retStatus = RetStatus.RetTimeout;
            }
            if (rpcException.getCause() instanceof CallAbortedException) {
                retStatus = RetStatus.RetReject;
            }
        }
        URL url = invoker.getUrl();
        long delay = System.currentTimeMillis() - startTimeMilli;
        polarisOperator.reportInvokeResult(url.getServiceInterface(), invocation.getMethodName(), url.getHost(),
            url.getPort(), callerIp, delay, retStatus, code);
    }

    private boolean isFlowControl(RpcException rpcException) {
        boolean a = StringUtils.isNotBlank(rpcException.getMessage()) && rpcException.getMessage()
            .contains(PolarisBlockException.PREFIX);
        boolean b = rpcException.isLimitExceed();
        return a || b;
    }
}
