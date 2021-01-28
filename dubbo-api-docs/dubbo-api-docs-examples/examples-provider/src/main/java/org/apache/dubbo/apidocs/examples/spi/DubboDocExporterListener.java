package org.apache.dubbo.apidocs.examples.spi;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.ExporterListener;
import org.apache.dubbo.rpc.RpcException;

/**
 * .
 *
 * @date 2020/10/29 10:50
 */
@Activate
public class DubboDocExporterListener implements ExporterListener {
    @Override
    public void exported(Exporter<?> exporter) throws RpcException {
        System.out.println("=============exported=============");
    }

    @Override
    public void unexported(Exporter<?> exporter) {
        System.out.println("=============unexported=============");
    }
}
