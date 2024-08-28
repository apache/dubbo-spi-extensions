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
package org.apache.dubbo.metadata.store.etcd;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigItem;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.metadata.MappingChangedEvent;
import org.apache.dubbo.metadata.MappingListener;
import org.apache.dubbo.metadata.report.identifier.BaseMetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.KeyTypeEnum;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.ServiceMetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;
import org.apache.dubbo.metadata.report.support.AbstractMetadataReport;
import org.apache.dubbo.remoting.etcd.jetcd.JEtcdClient;

import com.google.protobuf.ByteString;
import io.etcd.jetcd.api.Event;
import io.etcd.jetcd.api.WatchCancelRequest;
import io.etcd.jetcd.api.WatchCreateRequest;
import io.etcd.jetcd.api.WatchGrpc;
import io.etcd.jetcd.api.WatchRequest;
import io.etcd.jetcd.api.WatchResponse;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_SEPARATOR;
import static org.apache.dubbo.metadata.ServiceNameMapping.DEFAULT_MAPPING_GROUP;
import static org.apache.dubbo.metadata.ServiceNameMapping.getAppNames;

/**
 * Report Metadata to Etcd
 */
public class EtcdMetadataReport extends AbstractMetadataReport {

    private final String root;

    /**
     * The etcd client
     */
    private final JEtcdClient etcdClient;

    private final ConcurrentHashMap<MappingListener, EtcdWatcher> mappingDataListenerMap = new ConcurrentHashMap<>();


    public EtcdMetadataReport(URL url) {
        super(url);
        if (url.isAnyHost()) {
            throw new IllegalStateException("registry address == null");
        }
        String group = url.getParameter(GROUP_KEY, DEFAULT_ROOT);
        if (!group.startsWith(PATH_SEPARATOR)) {
            group = PATH_SEPARATOR + group;
        }
        this.root = group;
        etcdClient = new JEtcdClient(url);
    }

    @Override
    protected void doStoreProviderMetadata(MetadataIdentifier providerMetadataIdentifier, String serviceDefinitions) {
        storeMetadata(providerMetadataIdentifier, serviceDefinitions);
    }

    @Override
    protected void doStoreConsumerMetadata(MetadataIdentifier consumerMetadataIdentifier, String value) {
        storeMetadata(consumerMetadataIdentifier, value);
    }

    @Override
    protected void doSaveMetadata(ServiceMetadataIdentifier serviceMetadataIdentifier, URL url) {
        String key = getNodeKey(serviceMetadataIdentifier);
        if (!etcdClient.put(key, URL.encode(url.toFullString()))) {
            logger.error("Failed to put " + serviceMetadataIdentifier + " to etcd, value: " + url);
        }
    }

    @Override
    protected void doRemoveMetadata(ServiceMetadataIdentifier serviceMetadataIdentifier) {
        etcdClient.delete(getNodeKey(serviceMetadataIdentifier));
    }

    @Override
    protected List<String> doGetExportedURLs(ServiceMetadataIdentifier metadataIdentifier) {
        String content = etcdClient.getKVValue(getNodeKey(metadataIdentifier));
        if (StringUtils.isEmpty(content)) {
            return Collections.emptyList();
        }
        return new ArrayList<String>(Arrays.asList(URL.decode(content)));
    }

    @Override
    protected void doSaveSubscriberData(SubscriberMetadataIdentifier subscriberMetadataIdentifier, String urlListStr) {
        String key = getNodeKey(subscriberMetadataIdentifier);
        if (!etcdClient.put(key, urlListStr)) {
            logger.error("Failed to put " + subscriberMetadataIdentifier + " to etcd, value: " + urlListStr);
        }
    }

    @Override
    protected String doGetSubscribedURLs(SubscriberMetadataIdentifier subscriberMetadataIdentifier) {
        return etcdClient.getKVValue(getNodeKey(subscriberMetadataIdentifier));
    }

    @Override
    public String getServiceDefinition(MetadataIdentifier metadataIdentifier) {
        return etcdClient.getKVValue(getNodeKey(metadataIdentifier));
    }

    private void storeMetadata(MetadataIdentifier identifier, String v) {
        String key = getNodeKey(identifier);
        if (!etcdClient.put(key, v)) {
            logger.error("Failed to put " + identifier + " to etcd, value: " + v);
        }
    }

    String getNodeKey(BaseMetadataIdentifier identifier) {
        return toRootDir() + identifier.getUniqueKey(KeyTypeEnum.PATH);
    }

    String toRootDir() {
        if (root.equals(PATH_SEPARATOR)) {
            return root;
        }
        return root + PATH_SEPARATOR;
    }

    @Override
    public ConfigItem getConfigItem(String key, String group) {
        String content = etcdClient.getKVValue(buildPathKey(group, key));
        return new ConfigItem(content, content);
    }

    private String buildPathKey(String group, String serviceKey) {
        return toRootDir() + group + PATH_SEPARATOR + serviceKey;
    }

    @Override
    public Set<String> getServiceAppMapping(String serviceKey, MappingListener listener, URL url) {
        addListener(serviceKey, listener);
        return this.getServiceAppMapping(serviceKey, url);
    }

    public void addListener(String serviceKey, MappingListener listener) {
        if (mappingDataListenerMap.get(listener) == null) {
            String path = buildPathKey(DEFAULT_MAPPING_GROUP, serviceKey);
            EtcdWatcher watcher = new EtcdWatcher(path, serviceKey, listener);
            mappingDataListenerMap.put(listener, watcher);
            watcher.watch();
        }
    }

    @Override
    public Set<String> getServiceAppMapping(String serviceKey, URL url) {
        return getAppNames(etcdClient.getKVValue(buildPathKey(DEFAULT_MAPPING_GROUP, serviceKey)));
    }

    @Override
    public void removeServiceAppMappingListener(String serviceKey, MappingListener listener) {
        EtcdWatcher etcdWatcher = mappingDataListenerMap.get(listener);
        if (etcdWatcher != null) {
            etcdWatcher.cancelWatch();
        }
    }

    @Override
    public boolean registerServiceAppMapping(String key, String group, String newConfigContent, Object ticket) {
        try {
            if (Objects.nonNull(ticket) && !(ticket instanceof String)) {
                throw new IllegalArgumentException("etcd publishConfigCas requires string type ticket");
            }
            String pathKey = buildPathKey(group, key);
            return etcdClient.putCas(pathKey, (String) ticket, newConfigContent);
        } catch (Exception e) {
            return false;
        }
    }


    public class EtcdWatcher implements StreamObserver<WatchResponse> {

        private MappingListener listener;

        protected WatchGrpc.WatchStub watchStub;

        private StreamObserver<WatchRequest> observer;

        protected long watchId;

        private ManagedChannel channel;

        private final String path;

        private final String serviceKey;

        public EtcdWatcher(String path, String serviceKey, MappingListener listener) {
            this.path = path;
            this.listener = listener;
            this.serviceKey = serviceKey;
            this.channel = etcdClient.getChannel();
        }

        @Override
        public void onNext(WatchResponse watchResponse) {
            this.watchId = watchResponse.getWatchId();
            for (Event etcdEvent : watchResponse.getEventsList()) {
                String value = etcdEvent.getKv().getValue().toString(UTF_8);
                Set<String> apps = getAppNames(value);
                MappingChangedEvent mappingChangedEvent = new MappingChangedEvent(path, apps);
                listener.onEvent(mappingChangedEvent);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            // ignore
        }

        @Override
        public void onCompleted() {
            // ignore
        }

        public long getWatchId() {
            return watchId;
        }

        private void watch() {
            watchStub = WatchGrpc.newStub(channel);
            observer = watchStub.watch(this);
            WatchCreateRequest.Builder builder = WatchCreateRequest.newBuilder()
                .setKey(ByteString.copyFromUtf8(path))
                .setProgressNotify(true);
            WatchRequest req = WatchRequest.newBuilder().setCreateRequest(builder).build();
            observer.onNext(req);
        }

        private void cancelWatch() {
            WatchCancelRequest watchCancelRequest =
                WatchCancelRequest.newBuilder().setWatchId(getWatchId()).build();
            WatchRequest cancelRequest = WatchRequest.newBuilder()
                .setCancelRequest(watchCancelRequest).build();
            observer.onNext(cancelRequest);
        }
    }

}
