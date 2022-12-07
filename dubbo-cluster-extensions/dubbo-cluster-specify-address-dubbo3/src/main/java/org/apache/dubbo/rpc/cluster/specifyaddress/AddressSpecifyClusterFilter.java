package org.apache.dubbo.rpc.cluster.specifyaddress;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.*;
import org.apache.dubbo.rpc.cluster.common.SpecifyAddress;
import org.apache.dubbo.rpc.cluster.filter.ClusterFilter;

/**
 * The SPECIFY ADDRESS field is handed over to the attachment by the thread
 */
@Activate(group = {"consumer"})
public class AddressSpecifyClusterFilter implements ClusterFilter {


    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {

        SpecifyAddress<URL> current = UserSpecifiedAddressUtil.current();
        if (current != null) {
            invocation.setAttachment(SpecifyAddress.name, current);
            UserSpecifiedAddressUtil.removeAddress();
        }
        return invoker.invoke(invocation);
    }


}
