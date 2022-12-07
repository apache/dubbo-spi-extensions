package org.apache.dubbo.rpc.cluster.specifyaddress;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.cluster.common.SpecifyAddress;
import org.apache.dubbo.rpc.cluster.interceptor.ClusterInterceptor;
import org.apache.dubbo.rpc.cluster.support.AbstractClusterInvoker;

/**
 * The SPECIFY ADDRESS field is handed over to the attachment by the thread
 */
@Activate(group = CommonConstants.CONSUMER)
public class AddressSpecifyClusterInterceptor implements ClusterInterceptor {

    @Override
    public void before(AbstractClusterInvoker<?> clusterInvoker, Invocation invocation) {
        SpecifyAddress<URL> current = UserSpecifiedAddressUtil.current();
        if (current != null) {
            invocation.setAttachment(SpecifyAddress.name, current);
            UserSpecifiedAddressUtil.removeAddress();
        }
    }


    @Override
    public void after(AbstractClusterInvoker<?> clusterInvoker, Invocation invocation) {

    }
}
