package org.apache.dubbo.gateway.provider;

import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.dubbo.DecodeableRpcInvocation;
import org.apache.dubbo.rpc.protocol.dubbo.ExceptionProcessor;

import java.io.InputStream;

public class ServiceNotFoundExceptionProcessor implements ExceptionProcessor {

    public FrameworkModel frameworkModel;

    public ServiceNotFoundExceptionProcessor(FrameworkModel frameworkModel) {
        this.frameworkModel = frameworkModel;
    }

    @Override
    public DecodeableRpcInvocation getRetryDecodeableRpcInvocation(Channel channel, Request req, InputStream is, byte proto) {
        return new RetryDecodeableRpcInvocation(frameworkModel, channel, req, is, proto);
    }
}
