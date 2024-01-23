package org.apache.dubbo.rpc.cluster.router.xds;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.registry.xds.resource.XdsCluster;
import org.apache.dubbo.registry.xds.resource.XdsClusterWeight;
import org.apache.dubbo.registry.xds.resource.XdsEndpoint;
import org.apache.dubbo.registry.xds.resource.XdsRoute;
import org.apache.dubbo.registry.xds.resource.XdsVirtualHost;
import org.apache.dubbo.registry.xds.util.PilotExchanger;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.governance.GovernanceRuleRepository;
import org.apache.dubbo.rpc.cluster.router.RouterSnapshotNode;
import org.apache.dubbo.rpc.cluster.router.state.AbstractStateRouter;
import org.apache.dubbo.rpc.cluster.router.state.BitList;
import org.apache.dubbo.rpc.model.ModuleModel;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class XdsRouterDemo<T> extends AbstractStateRouter<T> {
    private volatile boolean force = false;
    private volatile URL url;
    protected ModuleModel moduleModel;

    private PilotExchanger pilotExchanger = PilotExchanger.getInstance();

    public XdsRouterDemo(URL url) {
        super(url);
        this.moduleModel = url.getOrDefaultModuleModel();
        this.url = url;
    }

    @Override
    protected BitList<Invoker<T>> doRoute(BitList<Invoker<T>> invokers, URL url, Invocation invocation, boolean needToPrintMessage, Holder<RouterSnapshotNode<T>> routerSnapshotNodeHolder, Holder<String> messageHolder) throws RpcException {
        // 1. 匹配 cluster
        String matchCluster = matchCluster(invocation);

        // 2. 匹配 endpoint
        BitList<Invoker<T>> invokerList = matchInvoker(matchCluster, invokers);

        // 3. 设置负载均衡策略
        XdsCluster<T> xdsCluster = pilotExchanger.getXdsClusterMap().get(matchCluster);
        String lbPolicy = xdsCluster.getLbPolicy();
        invokerList.get(0).getUrl().putAttribute("loadbalance", lbPolicy);

        return invokerList;
    }

    private String matchCluster(Invocation invocation) {
        String cluster = null;
        String serviceName = invocation.getInvoker().getUrl().getParameter("providedBy");
        XdsVirtualHost xdsVirtualHost = pilotExchanger.getXdsVirtualHostMap().get(serviceName);

        // match route
        for (XdsRoute xdsRoute : xdsVirtualHost.getRoutes()) {
            // 判断路径是否匹配
            String path = "/" + invocation.getInvoker().getUrl().getPath() + "/" + RpcUtils.getMethodName(invocation);
            if (xdsRoute.getRouteMatch().isMatch(path)) {
                cluster = xdsRoute.getRouteAction().getCluster();

                // 如果是权重cluster，则进行权重分配
                if (cluster == null) {
                    cluster = computeWeightCluster(xdsRoute.getRouteAction().getClusterWeights());
                }
            }
        }

        return cluster;
    }

    private String computeWeightCluster(List<XdsClusterWeight> weightedClusters) {
        int totalWeight = Math.max(weightedClusters.stream().mapToInt(XdsClusterWeight::getWeight).sum(), 1);

        int target = ThreadLocalRandom.current().nextInt(1, totalWeight + 1);
        for (XdsClusterWeight xdsClusterWeight : weightedClusters) {
            int weight = xdsClusterWeight.getWeight();
            target -= weight;
            if (target <= 0) {
                return xdsClusterWeight.getName();
            }
        }
        return null;
    }

    private BitList<Invoker<T>> matchInvoker(String clusterName, BitList<Invoker<T>> invokers) {

        XdsCluster<T> xdsCluster = pilotExchanger.getXdsClusterMap().get(clusterName);

        List<XdsEndpoint> endpoints = xdsCluster.getXdsEndpoints();
        List<Invoker<T>> filterInvokers = invokers.stream()
            .filter(inv -> {
                String host = inv.getUrl().getHost();
                int port = inv.getUrl().getPort();
                Optional<XdsEndpoint> any = endpoints.stream()
                    .filter(end -> host.equals(end.getAddress()) && port == end.getPortValue())
                    .findAny();
                return any.isPresent();
            })
            .collect(Collectors.toList());

        BitList<Invoker<T>> finalInvokers = new BitList<>(filterInvokers);
        xdsCluster.setInvokers(finalInvokers);
        return finalInvokers;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public boolean isRuntime() {
        return false;
    }

    @Override
    public boolean isForce() {
        return false;
    }
}
