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

import com.tencent.polaris.api.plugin.circuitbreaker.ResourceStat;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.InstanceResource;
import com.tencent.polaris.api.plugin.circuitbreaker.entity.Resource;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.circuitbreak.api.InvokeHandler;
import com.tencent.polaris.circuitbreak.api.pojo.InvokeContext;
import com.tencent.polaris.circuitbreak.api.pojo.ResultToErrorCode;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;
import com.tencent.polaris.common.exception.PolarisBlockException;
import com.tencent.polaris.common.registry.PolarisOperator;
import com.tencent.polaris.common.registry.PolarisOperatorDelegate;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

@Activate(group = CommonConstants.CONSUMER)
public class CircuitBreakerFilter extends PolarisOperatorDelegate implements Filter, ResultToErrorCode {

    private static final Logger LOGGER = LoggerFactory.getLogger(CircuitBreakerFilter.class);

    private final CallAbortCallback callback;

    public CircuitBreakerFilter() {
        ServiceLoader<CallAbortCallback> loader = ServiceLoader.load(CallAbortCallback.class);
        CallAbortCallback instance = loader.iterator().next();
        if (Objects.nonNull(instance)) {
            this.callback = instance;
        } else {
            this.callback = new DefaultCallAbortCallback();
        }

        LOGGER.info("[POLARIS] init polaris circuitbreaker");
    }

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        PolarisOperator polarisOperator = getPolarisOperator();
        if (null == polarisOperator) {
            return invoker.invoke(invocation);
        }

        CircuitBreakAPI circuitBreakAPI = getPolarisOperator().getCircuitBreakAPI();
        InvokeContext.RequestContext context = new InvokeContext.RequestContext(createCalleeService(invoker),
            invocation.getMethodName());
        context.setResultToErrorCode(this);
        InvokeHandler handler = circuitBreakAPI.makeInvokeHandler(context);
        try {
            long startTimeMilli = System.currentTimeMillis();
            InvokeContext.ResponseContext responseContext = new InvokeContext.ResponseContext();
            responseContext.setDurationUnit(TimeUnit.MILLISECONDS);
            Result result = null;
            RpcException exception = null;
            handler.acquirePermission();
            try {
                result = invoker.invoke(invocation);
                responseContext.setDuration(System.currentTimeMillis() - startTimeMilli);
                if (result.hasException()) {
                    responseContext.setError(result.getException());
                    handler.onError(responseContext);
                } else {
                    responseContext.setResult(result);
                    handler.onSuccess(responseContext);
                }
            } catch (RpcException e) {
                exception = e;
                responseContext.setError(e);
                responseContext.setDuration(System.currentTimeMillis() - startTimeMilli);
                handler.onError(responseContext);
            }
            ResourceStat resourceStat = createInstanceResourceStat(invoker, invocation, responseContext,
                responseContext.getDuration());
            circuitBreakAPI.report(resourceStat);
            if (result != null) {
                return result;
            }
            throw exception;
        } catch (CallAbortedException abortedException) {
            return callback.handle(invoker, invocation, abortedException);
        }
    }

    private ResourceStat createInstanceResourceStat(Invoker<?> invoker, Invocation invocation,
                                                    InvokeContext.ResponseContext context, long delay) {
        URL url = invoker.getUrl();
        Throwable exception = context.getError();
        RetStatus retStatus = RetStatus.RetSuccess;
        int code = 0;
        if (null != exception) {
            retStatus = RetStatus.RetFail;
            if (exception instanceof RpcException) {
                RpcException rpcException = (RpcException) exception;
                code = rpcException.getCode();
                if (StringUtils.isNotBlank(rpcException.getMessage()) && rpcException.getMessage()
                    .contains(PolarisBlockException.PREFIX)) {
                    // 限流异常不进行熔断
                    retStatus = RetStatus.RetFlowControl;
                }
                if (rpcException.isTimeout()) {
                    retStatus = RetStatus.RetTimeout;
                }
            } else {
                code = -1;
            }
        }

        ServiceKey calleeServiceKey = createCalleeService(invoker);
        Resource resource = new InstanceResource(
            calleeServiceKey,
            url.getHost(),
            url.getPort(),
            new ServiceKey()
        );
        return new ResourceStat(resource, code, delay, retStatus);
    }

    private ServiceKey createCalleeService(Invoker<?> invoker) {
        URL url = invoker.getUrl();
        return new ServiceKey(getPolarisOperator().getPolarisConfig().getNamespace(), url.getServiceInterface());
    }

    @Override
    public int onSuccess(Object value) {
        return 0;
    }

    @Override
    public int onError(Throwable throwable) {
        int code = 0;
        if (throwable instanceof RpcException) {
            RpcException rpcException = (RpcException) throwable;
            code = rpcException.getCode();
        } else {
            code = -1;
        }
        return code;
    }

    private static final class DefaultCallAbortCallback implements CallAbortCallback {

        @Override
        public Result handle(Invoker<?> invoker, Invocation invocation, CallAbortedException ex) {
            return AsyncRpcResult.newDefaultAsyncResult(ex, invocation);
        }
    }
}
