package org.apache.dubbo.common.serialize.fastjson;

import org.apache.dubbo.common.utils.AllowClassNotifyListener;
import org.apache.dubbo.common.utils.SerializeCheckStatus;

import java.util.Set;

public class FastJsonSecurityManager implements AllowClassNotifyListener {


    @Override
    public void notifyPrefix(Set<String> allowedList, Set<String> disAllowedList) {

    }

    @Override
    public void notifyCheckStatus(SerializeCheckStatus status) {

    }

    @Override
    public void notifyCheckSerializable(boolean checkSerializable) {

    }

    public static class Handler  {

    }
}
