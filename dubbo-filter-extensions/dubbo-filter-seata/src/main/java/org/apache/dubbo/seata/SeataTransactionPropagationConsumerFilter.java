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
package org.apache.dubbo.seata;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import io.seata.core.constants.DubboConstants;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;

/**
 * The type Transaction propagation consumer filter.
 */
@Activate(group = DubboConstants.CONSUMER)
public class SeataTransactionPropagationConsumerFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeataTransactionPropagationConsumerFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String xid = RootContext.getXID();
        BranchType branchType = RootContext.getBranchType();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Client side xid in RootContext[%s]", xid));
        }
        if (xid != null) {
            invocation.setAttachment(RootContext.KEY_XID, xid);
            if (branchType != null) {
                invocation.setAttachment(RootContext.KEY_BRANCH_TYPE, branchType.name());
            }
        }
        return invoker.invoke(invocation);
    }
}
