package org.apache.dubbo.rpc.cluster.specifyaddress;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.filter.ClusterFilter;

/**
 * The SPECIFY ADDRESS field is handed over to the attachment by the thread
 */
@Activate(group = {"consumer"})
public class AddressSpecifyClusterFilter implements ClusterFilter {


    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {

        Address current = UserSpecifiedAddressUtil.current();
        if (current != null) {
            invocation.setAttachment(Address.name, current);
            UserSpecifiedAddressUtil.removeAddress();
        }
        return invoker.invoke(invocation);
    }


}
