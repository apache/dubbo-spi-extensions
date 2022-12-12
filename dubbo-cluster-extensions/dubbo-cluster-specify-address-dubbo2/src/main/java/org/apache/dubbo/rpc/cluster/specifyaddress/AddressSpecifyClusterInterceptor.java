package org.apache.dubbo.rpc.cluster.specifyaddress;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.cluster.interceptor.ClusterInterceptor;
import org.apache.dubbo.rpc.cluster.support.AbstractClusterInvoker;

/**
 * The SPECIFY ADDRESS field is handed over to the attachment by the thread
 */
@Activate(group = CommonConstants.CONSUMER)
public class AddressSpecifyClusterInterceptor implements ClusterInterceptor {

    @Override
    public void before(AbstractClusterInvoker<?> clusterInvoker, Invocation invocation) {
        Address current = UserSpecifiedAddressUtil.current();
        if (current != null) {
            invocation.setAttachment(Address.name, current);
            UserSpecifiedAddressUtil.removeAddress();
        }
    }


    @Override
    public void after(AbstractClusterInvoker<?> clusterInvoker, Invocation invocation) {

    }
}
