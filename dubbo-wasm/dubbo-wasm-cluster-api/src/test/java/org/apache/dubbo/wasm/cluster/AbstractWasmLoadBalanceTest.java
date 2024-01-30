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

package org.apache.dubbo.wasm.cluster;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * see dubbo-wasm/dubbo-wasm-cluster-api/src/test/rust-load-balance/README.md
 */
public class AbstractWasmLoadBalanceTest {

    @Test
    void test() {
        MyInvoker myInvoker = new MyInvoker();
        final List<Invoker<Object>> invokers = new ArrayList<>();
        invokers.add(new MyInvoker());
        invokers.add(myInvoker);

        final RustLoadBalance balance = new RustLoadBalance();
        final Invoker<Object> selected = balance.doSelect(invokers, null, null);
        assertEquals(myInvoker, selected);
    }

    static class RustLoadBalance extends AbstractWasmLoadBalance {
    }

    static class MyInvoker implements Invoker<Object> {
        @Override
        public URL getUrl() {
            return null;
        }

        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public void destroy() {
        }

        @Override
        public Class<Object> getInterface() {
            return null;
        }

        @Override
        public Result invoke(Invocation invocation) throws RpcException {
            return null;
        }
    }
}
