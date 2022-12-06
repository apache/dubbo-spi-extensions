package org.apache.dubbo.rpc.cluster.specifyaddress;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.cluster.interceptor.ClusterInterceptor;
import org.apache.dubbo.rpc.cluster.support.AbstractClusterInvoker;

import static org.apache.dubbo.common.constants.CommonConstants.RETRIES_KEY;

/**
 * Set the number of retries to 0 to disable retries
 */
@Activate(group = CommonConstants.CONSUMER)
public class DisableRetryClusterInterceptor implements ClusterInterceptor {

    public static final String NAME = "disableRetry";

    @Override
    public void before(AbstractClusterInvoker<?> clusterInvoker, Invocation invocation) {
        if (currentThreadDisableRetry()) {
            RpcContext rpcContext = RpcContext.getContext();
            rpcContext.setAttachment(RETRIES_KEY, 0);
        }
    }

    private boolean currentThreadDisableRetry() {
        Address current = UserSpecifiedAddressUtil.current();
        return current != null && current.isDisableRetry();
    }

    @Override
    public void after(AbstractClusterInvoker<?> clusterInvoker, Invocation invocation) {

    }
}
