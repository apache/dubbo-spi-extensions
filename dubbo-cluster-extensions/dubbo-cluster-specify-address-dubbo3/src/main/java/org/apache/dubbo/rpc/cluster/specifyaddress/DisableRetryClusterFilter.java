package org.apache.dubbo.rpc.cluster.specifyaddress;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.cluster.filter.ClusterFilter;

import static org.apache.dubbo.common.constants.CommonConstants.RETRIES_KEY;

/**
 * Set the number of retries to 0 to disable retries
 */
@Activate(group = {"consumer"})
public class DisableRetryClusterFilter implements ClusterFilter {
    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {

        if (currentThreadDisableRetry()) {
            RpcContext rpcContext = RpcContext.getContext();
            rpcContext.setAttachment(RETRIES_KEY, 0);
        }
        return invoker.invoke(invocation);
    }

    private boolean currentThreadDisableRetry() {
        Address current = UserSpecifiedAddressUtil.current();
        return current != null && current.isDisableRetry();
    }
}
