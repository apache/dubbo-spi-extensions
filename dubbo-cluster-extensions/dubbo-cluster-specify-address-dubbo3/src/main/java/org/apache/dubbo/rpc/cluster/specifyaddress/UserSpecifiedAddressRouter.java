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
package org.apache.dubbo.rpc.cluster.specifyaddress;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.Holder;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.router.RouterSnapshotNode;
import org.apache.dubbo.rpc.cluster.router.state.AbstractStateRouter;
import org.apache.dubbo.rpc.cluster.router.state.BitList;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class UserSpecifiedAddressRouter<T> extends AbstractStateRouter<T> {
    private final static Logger logger = LoggerFactory.getLogger(UserSpecifiedAddressRouter.class);
    // protected for ut purpose
    protected static int EXPIRE_TIME = 10 * 60 * 1000;
    private final static String USER_SPECIFIED_SERVICE_ADDRESS_BUILDER_KEY = "userSpecifiedServiceAddressBuilder";

    private volatile BitList<Invoker<T>> invokers = BitList.emptyList();
    private volatile Map<String, Invoker<T>> ip2Invoker;
    private volatile Map<String, Invoker<T>> address2Invoker;

    private final Lock cacheLock = new ReentrantLock();
    private final Map<URL, InvokerCache<T>> newInvokerCache = new LinkedHashMap<>(16, 0.75f, true);

    private final UserSpecifiedServiceAddressBuilder userSpecifiedServiceAddressBuilder;

    private final Protocol protocol;
    private final ScheduledExecutorService scheduledExecutorService;
    private final AtomicBoolean launchRemovalTask = new AtomicBoolean(false);
    private volatile ScheduledFuture<?> removalFuture;

    public UserSpecifiedAddressRouter(URL referenceUrl) {
        super(referenceUrl);
        this.scheduledExecutorService = referenceUrl.getScopeModel().getDefaultExtension(ExecutorRepository.class).nextScheduledExecutor();
        this.protocol = referenceUrl.getOrDefaultFrameworkModel().getExtensionLoader(Protocol.class).getAdaptiveExtension();
        this.userSpecifiedServiceAddressBuilder = referenceUrl.getScopeModel().getExtensionLoader(UserSpecifiedServiceAddressBuilder.class)
            .getExtension(referenceUrl.getParameter(USER_SPECIFIED_SERVICE_ADDRESS_BUILDER_KEY, DefaultUserSpecifiedServiceAddressBuilder.NAME));
    }

    @Override
    public void notify(BitList<Invoker<T>> invokers) {
        this.invokers = invokers;
        // do not build cache until first Specify Invoke happened
        if (ip2Invoker != null) {
            ip2Invoker = processIp(invokers);
            address2Invoker = processAddress(invokers);
        }
    }

    @Override
    protected BitList<Invoker<T>> doRoute(BitList<Invoker<T>> invokers, URL url, Invocation invocation,
                                          boolean needToPrintMessage, Holder<RouterSnapshotNode<T>> nodeHolder,
                                          Holder<String> messageHolder) throws RpcException {
        Address address = UserSpecifiedAddressUtil.getAddress();

        // 1. check if set address in ThreadLocal
        if (address == null) {
            if (needToPrintMessage) {
                messageHolder.set("No address specified, continue.");
            }
            return continueRoute(invokers, url, invocation, needToPrintMessage, nodeHolder);
        }

        BitList<Invoker<T>> result = new BitList<>(invokers, true);

        // 2. check if set address url
        if (address.getUrlAddress() != null) {
            Invoker<T> invoker = getInvokerByURL(address, invocation);
            result.add(invoker);
            if (needToPrintMessage) {
                messageHolder.set("URL Address has been set. URL Address: " + address.getUrlAddress());
            }
            return result;
        }

        // 3. check if set ip and port
        if (StringUtils.isNotEmpty(address.getIp())) {
            Invoker<T> invoker = getInvokerByIp(address, invocation);
            if (invoker != null) {
                result.add(invoker);
                if (needToPrintMessage) {
                    messageHolder.set("Target Ip has been set and address can be found in directory. Target Ip: " + address.getIp() + " Port: " + address.getPort());
                }
                return result;
            } // target ip is not contains in directory

            if (address.isNeedToCreate()) {
                invoker = createInvoker(address, invocation);
                result.add(invoker);
                if (needToPrintMessage) {
                    messageHolder.set("Target Ip has been set and address cannot be found in directory, build new one. Target Ip: " + address.getIp() + " Port: " + address.getPort());
                }
                return result;
            }
        }

        if (needToPrintMessage) {
            messageHolder.set("Target Address has not been set.");
        }
        return continueRoute(invokers, url, invocation, needToPrintMessage, nodeHolder);
    }

    @Override
    protected boolean supportContinueRoute() {
        return true;
    }

    private Invoker<T> getInvokerByURL(Address address, Invocation invocation) {
        tryLoadSpecifiedMap();

        // try to find in directory
        URL urlAddress = address.getUrlAddress();
        String targetAddress = urlAddress.getHost() + ":" + urlAddress.getPort();
        Invoker<T> invoker = address2Invoker.get(targetAddress);
        if (invoker != null) {
            AtomicBoolean match = new AtomicBoolean(true);
            if (StringUtils.isNotEmpty(urlAddress.getProtocol())) {
                match.set(invoker.getUrl().getProtocol().equals(urlAddress.getProtocol()));
            }
            if (match.get()) {
                urlAddress.getParameters().forEach((k, v) -> {
                    if (match.get()) {
                        match.set(v.equals(invoker.getUrl().getParameter(k)));
                    }
                });
            }
            if (match.get()) {
                return invoker;
            }
        }

        // create new one
        URL url = userSpecifiedServiceAddressBuilder.rebuildAddress(invokers, address, invocation, getUrl());
        return getOrBuildInvokerCache(url);
    }

    private Invoker<T> getOrBuildInvokerCache(URL url) {
        logger.info("Unable to find a proper invoker from directory. Try to create new invoker. New URL: " + url);

        InvokerCache<T> cache;
        cacheLock.lock();
        try {
            cache = newInvokerCache.get(url);
        } finally {
            cacheLock.unlock();
        }
        if (cache == null) {
            Invoker<T> invoker = refer(url);
            cacheLock.lock();
            try {
                cache = newInvokerCache.get(url);
                if (cache == null) {
                    cache = new InvokerCache<>(invoker);
                    newInvokerCache.put(url, cache);
                    if (launchRemovalTask.compareAndSet(false, true)) {
                        removalFuture = scheduledExecutorService.scheduleAtFixedRate(new RemovalTask(), EXPIRE_TIME / 2, EXPIRE_TIME / 2, TimeUnit.MILLISECONDS);
                    }
                } else {
                    invoker.destroy();
                }
            } finally {
                cacheLock.unlock();
            }
        }
        return cache.getInvoker();
    }

    public Invoker<T> getInvokerByIp(Address address, Invocation invocation) {
        tryLoadSpecifiedMap();

        String ip = address.getIp();
        int port = address.getPort();

        Invoker<T> targetInvoker;
        if (port != 0) {
            targetInvoker = address2Invoker.get(ip + ":" + port);
            if (targetInvoker != null) {
                return targetInvoker;
            }
        } else {
            targetInvoker = ip2Invoker.get(ip);
            if (targetInvoker != null) {
                return targetInvoker;
            }
        }

        if (!address.isNeedToCreate()) {
            throwException(invocation, address);
        }

        return null;
    }

    public Invoker<T> createInvoker(Address address, Invocation invocation) {
        return getOrBuildInvokerCache(userSpecifiedServiceAddressBuilder.buildAddress(invokers, address, invocation, getUrl()));
    }

    private Invoker<T> refer(URL url) {
        return (Invoker<T>) protocol.refer(getUrl().getServiceModel().getServiceInterfaceClass(), url);
    }

    private void throwException(Invocation invocation, Address address) {
        throw new RpcException("user specified server address : [" + address + "] is not a valid provider for service: ["
            + getUrl().getServiceKey() + "]");
    }

    private Map<String, Invoker<T>> processIp(List<Invoker<T>> invokerList) {
        Map<String, Invoker<T>> ip2Invoker = new HashMap<>();
        for (Invoker<T> invoker : invokerList) {
            ip2Invoker.put(invoker.getUrl().getHost(), invoker);
        }
        return Collections.unmodifiableMap(ip2Invoker);
    }

    private Map<String, Invoker<T>> processAddress(List<Invoker<T>> addresses) {
        Map<String, Invoker<T>> address2Invoker = new HashMap<>();
        for (Invoker<T> invoker : addresses) {
            address2Invoker.put(invoker.getUrl().getHost() + ":" + invoker.getUrl().getPort(), invoker);
        }
        return Collections.unmodifiableMap(address2Invoker);
    }

    // For ut only
    @Deprecated
    protected Map<String, Invoker<T>> getIp2Invoker() {
        return ip2Invoker;
    }

    // For ut only
    @Deprecated
    protected Map<String, Invoker<T>> getAddress2Invoker() {
        return address2Invoker;
    }

    // For ut only
    @Deprecated
    protected BitList<Invoker<T>> getInvokers() {
        return invokers;
    }

    // For ut only
    @Deprecated
    protected Map<URL, InvokerCache<T>> getNewInvokerCache() {
        return newInvokerCache;
    }

    private void tryLoadSpecifiedMap() {
        if (ip2Invoker != null) {
            return;
        }
        synchronized (this) {
            if (ip2Invoker != null) {
                return;
            }
            BitList<Invoker<T>> invokers = this.invokers;
            if (CollectionUtils.isEmpty(invokers)) {
                address2Invoker = Collections.unmodifiableMap(new HashMap<>());
                ip2Invoker = Collections.unmodifiableMap(new HashMap<>());
                return;
            }
            address2Invoker = processAddress(invokers);
            ip2Invoker = processIp(invokers);
        }
    }

    @Override
    public void stop() {
        if (removalFuture != null) {
            removalFuture.cancel(false);
        }
    }

    private class RemovalTask implements Runnable {
        @Override
        public void run() {
            cacheLock.lock();
            try {
                if (newInvokerCache.size() > 0) {
                    Iterator<Map.Entry<URL, InvokerCache<T>>> iterator = newInvokerCache.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<URL, InvokerCache<T>> entry = iterator.next();
                        if (System.currentTimeMillis() - entry.getValue().getLastAccess() > EXPIRE_TIME) {
                            iterator.remove();
                            entry.getValue().getInvoker().destroy();
                        } else {
                            break;
                        }
                    }
                }
            } finally {
                cacheLock.unlock();
            }
        }
    }
}
