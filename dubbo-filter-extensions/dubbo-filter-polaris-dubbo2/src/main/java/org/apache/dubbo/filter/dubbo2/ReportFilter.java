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

package org.apache.dubbo.filter.dubbo2;


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

@Activate(group = CommonConstants.CONSUMER)
public class ReportFilter extends PolarisOperatorDelegate implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportFilter.class);

    public ReportFilter() {
        LOGGER.info("[POLARIS] init polaris reporter");
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        long startTimeMilli = System.currentTimeMillis();
        Result result = null;
        Throwable exception = null;
        RpcException rpcException = null;
        try {
            result = invoker.invoke(invocation);
        } catch (Throwable e) {
            exception = e;
        }
        if (null != result && result.hasException()) {
            exception = result.getException();
        }
        if (exception instanceof RpcException) {
            rpcException = (RpcException) exception;
        }
        PolarisOperator polarisOperator = getPolarisOperator();
        if (null == polarisOperator) {
            return result;
        }
        RetStatus retStatus = RetStatus.RetSuccess;
        int code = 0;
        if (null != exception) {
            retStatus = RetStatus.RetFail;
            if (null != rpcException) {
                code = rpcException.getCode();
                if (code == RpcException.LIMIT_EXCEEDED_EXCEPTION) {
                    // 限流异常不进行熔断
                    retStatus = RetStatus.RetSuccess;
                }
            } else {
                code = -1;
            }
        }
        URL url = invoker.getUrl();
        long delay = System.currentTimeMillis() - startTimeMilli;
        polarisOperator.reportInvokeResult(url.getServiceInterface(), invocation.getMethodName(), url.getHost(),
                url.getPort(), delay, retStatus, code);
        if (null != rpcException) {
            throw rpcException;
        } else if (null != exception) {
            throw new RpcException(exception);
        }
        return result;
    }
}
