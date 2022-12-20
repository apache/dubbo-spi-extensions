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
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.threadpool.manager.ExecutorRepository;
import org.apache.dubbo.common.utils.ClassUtils;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.router.AbstractRouter;
import org.apache.dubbo.rpc.cluster.specifyaddress.common.InvokerCache;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.dubbo.common.constants.CommonConstants.DUBBO;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.MONITOR_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PROTOCOL_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;


public class UserSpecifiedAddressRouter<T> extends AbstractRouter {
    private final static Logger logger = LoggerFactory.getLogger(UserSpecifiedAddressRouter.class);
    // protected for ut purpose
    protected static int EXPIRE_TIME = 10 * 60 * 1000;

    private volatile List<Invoker<T>> invokers = Collections.emptyList();
    private volatile Map<String, Invoker<T>> ip2Invoker;
    private volatile Map<String, Invoker<T>> address2Invoker;
    private final Protocol protocol;

    private final Lock cacheLock = new ReentrantLock();

    private final ScheduledExecutorService scheduledExecutorService;
    private final AtomicBoolean launchRemovalTask = new AtomicBoolean(false);


    private final Map<URL, InvokerCache<Invoker<T>>> newInvokerCache = new LinkedHashMap<>(16, 0.75f, true);

    public UserSpecifiedAddressRouter(URL referenceUrl) {
        super(referenceUrl);
        this.protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
        this.scheduledExecutorService = ExtensionLoader.getExtensionLoader(ExecutorRepository.class).getDefaultExtension().nextScheduledExecutor();
    }

    @Override
    public <T> void notify(List<Invoker<T>> invokers) {
        this.invokers = (List) invokers;
        // do not build cache until first Specify Invoke happened
        if (ip2Invoker != null) {
            ip2Invoker = processIp((List) invokers);
            address2Invoker = processAddress((List) invokers);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<Invoker<T>> route(List<Invoker<T>> invokers, URL url, Invocation invocation) throws RpcException {

        Object addressObj = invocation.get(Address.name);

        // 1. check if set address in ThreadLocal
        if (addressObj == null) {
            return invokers;
        }

        Address address = (Address) addressObj;

        List<Invoker<T>> result = new LinkedList<>();

        // 2. check if set address url
        if (address.getUrlAddress() != null) {
            Invoker<?> invoker = getInvokerByURL(address);
            result.add((Invoker) invoker);
            return result;
        }

        // 3. check if set ip and port
        if (StringUtils.isNotEmpty(address.getIp())) {
            Invoker<?> invoker = getInvokerByIp(address);
            result.add((Invoker) invoker);
            return result;
        }

        return invokers;
    }

    private Invoker<?> getInvokerByURL(Address address) {
        tryLoadSpecifiedMap();

        // try to find in directory
        URL urlAddress = address.getUrlAddress();
        String targetAddress = urlAddress.getHost() + ":" + urlAddress.getPort();
        Invoker<?> invoker = address2Invoker.get(targetAddress);
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

        URL newUrl = rebuildAddress(address, getUrl());
        return getOrBuildInvokerCache(newUrl);
    }

    public Invoker<?> getInvokerByIp(Address address) {
        tryLoadSpecifiedMap();

        String ip = address.getIp();
        int port = address.getPort();

        Invoker<?> targetInvoker;
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
            throwException(address);
        }

        URL newUrl = buildAddress(invokers, address, getUrl());
        return getOrBuildInvokerCache(newUrl);
    }


    private void throwException(Address address) {
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
    protected List<Invoker<T>> getInvokers() {
        return invokers;
    }

    private void tryLoadSpecifiedMap() {
        if (ip2Invoker != null) {
            return;
        }
        synchronized (this) {
            if (ip2Invoker != null) {
                return;
            }
            List<Invoker<T>> invokers = this.invokers;
            if (CollectionUtils.isEmpty(invokers)) {
                address2Invoker = Collections.unmodifiableMap(new HashMap<>());
                ip2Invoker = Collections.unmodifiableMap(new HashMap<>());
                return;
            }
            address2Invoker = processAddress(invokers);
            ip2Invoker = processIp(invokers);
        }
    }


    public URL buildAddress(List<Invoker<T>> invokers, Address address, URL consumerUrl) {
        if (!invokers.isEmpty()) {
            URL template = invokers.iterator().next().getUrl();
            template = template.setHost(address.getIp());
            if (address.getPort() != 0) {
                template = template.setPort(address.getPort());
            }
            return template;
        } else {
            String ip = address.getIp();
            int port = address.getPort();
            if (port == 0) {
                port = ExtensionLoader.getExtensionLoader(Protocol.class).getDefaultExtension().getDefaultPort();
            }
            return copyConsumerUrl(consumerUrl, ip, port, new HashMap<>());
        }
    }

    private URL copyConsumerUrl(URL url, String ip, int port, Map<String, String> parameters) {
        String protocol = url.getParameter(PROTOCOL_KEY, DUBBO);
        return URLBuilder.from(url)
                .setHost(ip)
                .setPort(port)
                .setProtocol(protocol)
                .setPath(url.getPath())
                .clearParameters()
                .addParameters(parameters)
                .removeParameter(MONITOR_KEY)
                .build();
    }

    public URL rebuildAddress(Address address, URL consumerUrl) {
        URL url = address.getUrlAddress();
        Map<String, String> parameters = new HashMap<>(url.getParameters());
        parameters.put(VERSION_KEY, consumerUrl.getParameter(VERSION_KEY, "0.0.0"));
        parameters.put(GROUP_KEY, consumerUrl.getParameter(GROUP_KEY));
        parameters.putAll(consumerUrl.getParameters());
        return copyConsumerUrl(consumerUrl, url.getHost(), url.getPort(),parameters);
    }

    private Invoker<T> getOrBuildInvokerCache(URL url) {
        logger.info("Unable to find a proper invoker from directory. Try to create new invoker. New URL: " + url);

        InvokerCache<Invoker<T>> cache;
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
                        scheduledExecutorService.scheduleAtFixedRate(new RemovalTask(), EXPIRE_TIME / 2, EXPIRE_TIME / 2, TimeUnit.MILLISECONDS);
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

    private Invoker<T> refer(URL url) {

        try {
            Class interfaceClass = Class.forName(getUrl().getServiceInterface(), true, ClassUtils.getClassLoader());
            return this.protocol.refer(interfaceClass, url);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    private class RemovalTask implements Runnable {
        @Override
        public void run() {
            cacheLock.lock();
            try {
                if (newInvokerCache.size() > 0) {
                    Iterator<Map.Entry<URL, InvokerCache<Invoker<T>>>> iterator = newInvokerCache.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<URL, InvokerCache<Invoker<T>>> entry = iterator.next();
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
