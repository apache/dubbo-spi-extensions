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

import io.seata.common.util.StringUtils;
import io.seata.core.constants.DubboConstants;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;

/**
 * The type Transaction propagation provider filter.
 */
@Activate(group = DubboConstants.PROVIDER)
public class SeataTransactionPropagationProviderFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeataTransactionPropagationProviderFilter.class);

    private static final String LOWER_KEY_XID = RootContext.KEY_XID.toLowerCase();

    private static final String LOWER_KEY_BRANCH_TYPE = RootContext.KEY_BRANCH_TYPE.toLowerCase();

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String rpcXid = invocation.getAttachment(RootContext.KEY_XID);
        if (rpcXid == null) {
            rpcXid = invocation.getAttachment(LOWER_KEY_XID);
        }
        String rpcBranchType = invocation.getAttachment(RootContext.KEY_BRANCH_TYPE);
        if (rpcBranchType == null) {
            rpcBranchType = invocation.getAttachment(LOWER_KEY_BRANCH_TYPE);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Server side xid in RpcContext[" + rpcXid + "]");
        }
        boolean bind = false;

        if (rpcXid != null) {
            RootContext.bind(rpcXid);
            if (StringUtils.equals(BranchType.TCC.name(), rpcBranchType)) {
                RootContext.bindBranchType(BranchType.TCC);
            }
            bind = true;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("bind xid [%s] branchType [%s] to RootContext", rpcXid, rpcBranchType));
            }
        }
        try {
            return invoker.invoke(invocation);
        } finally {
            if (bind) {
                BranchType previousBranchType = RootContext.getBranchType();
                String unbindXid = RootContext.unbind();
                if (BranchType.TCC == previousBranchType) {
                    RootContext.unbindBranchType();
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(String.format("unbind xid [%s] branchType [%s] from RootContext", unbindXid, previousBranchType));
                }
                if (!rpcXid.equalsIgnoreCase(unbindXid)) {
                    LOGGER.warn(String.format("xid in change during RPC from %s to %s,branchType from %s to %s", rpcXid, unbindXid,
                        rpcBranchType != null ? rpcBranchType : "AT", previousBranchType));
                    if (unbindXid != null) {
                        RootContext.bind(unbindXid);
                        LOGGER.warn(String.format("bind xid [%s] back to RootContext", unbindXid));
                        if (BranchType.TCC == previousBranchType) {
                            RootContext.bindBranchType(BranchType.TCC);
                            LOGGER.warn(String.format("bind branchType [%s] back to RootContext", previousBranchType));
                        }
                    }
                }
            }
        }
    }
}
