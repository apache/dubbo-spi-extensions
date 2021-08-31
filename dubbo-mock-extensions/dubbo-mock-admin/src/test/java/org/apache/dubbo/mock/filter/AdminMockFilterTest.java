package org.apache.dubbo.mock.filter;

import org.apache.dubbo.mock.api.GlobalMockRule;
import org.apache.dubbo.rpc.AsyncRpcResult;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

/**
 * {@link AdminMockFilter} unit tests.
 *
 * @author chenglu
 * @date 2021-08-31 12:31
 */
@RunWith(MockitoJUnitRunner.class)
public class AdminMockFilterTest {

    @InjectMocks
    private AdminMockFilter adminMockFilter;

    @Mock
    private GlobalMockRule globalMockRule;

    @Mock
    private Invoker invoker;

    @Mock
    private Invocation invocation;

    @Test(expected = RpcException.class)
    public void testInvoke() {
        Result result = new AsyncRpcResult(null, invocation);
        Mockito.when(globalMockRule.getEnableMock()).thenReturn(false);
        Mockito.when(invoker.invoke(Mockito.any())).thenReturn(result);
        Object res1 = adminMockFilter.invoke(invoker, invocation);
        Assert.assertEquals(result, res1);

        Mockito.when(globalMockRule.getEnableMock()).thenReturn(true);
        Result res2 = adminMockFilter.invoke(invoker, invocation);
        Assert.assertEquals(result, res2);

        Mockito.when(globalMockRule.getEnabledMockRules()).thenReturn(Collections.singleton("A#B"));
        Mockito.when(invocation.getTargetServiceUniqueName()).thenReturn("A");
        Mockito.when(invocation.getMethodName()).thenReturn("B");
        adminMockFilter.invoke(invoker, invocation);
    }
}
