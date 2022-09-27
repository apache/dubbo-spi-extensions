package org.apache.dubbo.rpc.rocketmq;

import java.util.Map;

import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.protocol.AbstractExporter;

public class RocketMQExporter<T> extends AbstractExporter<T> {

	private final String key;

	private final Map<String, Exporter<?>> exporterMap;

	public RocketMQExporter( Invoker<T> invoker,String key,  Map<String, Exporter<?>> exporterMap) {
		super(invoker);
		this.key = key;
		this.exporterMap = exporterMap;
		this.exporterMap.put(key, this);
	}

	public void afterUnExport() {
		exporterMap.remove(key, this);
	}

}
