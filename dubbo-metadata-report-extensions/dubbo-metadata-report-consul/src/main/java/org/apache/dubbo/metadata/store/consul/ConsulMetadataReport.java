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

package org.apache.dubbo.metadata.store.consul;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.config.configcenter.ConfigChangeType;
import org.apache.dubbo.common.config.configcenter.ConfigChangedEvent;
import org.apache.dubbo.common.config.configcenter.ConfigItem;
import org.apache.dubbo.common.config.configcenter.ConfigurationListener;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.constants.LoggerCodeConstants;
import org.apache.dubbo.common.utils.JsonUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.configcenter.consul.ConsulConstants;
import org.apache.dubbo.metadata.MappingChangedEvent;
import org.apache.dubbo.metadata.MappingListener;
import org.apache.dubbo.metadata.MetadataInfo;
import org.apache.dubbo.metadata.ServiceNameMapping;
import org.apache.dubbo.metadata.report.identifier.BaseMetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.KeyTypeEnum;
import org.apache.dubbo.metadata.report.identifier.MetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.ServiceMetadataIdentifier;
import org.apache.dubbo.metadata.report.identifier.SubscriberMetadataIdentifier;
import org.apache.dubbo.metadata.report.support.AbstractMetadataReport;
import org.apache.dubbo.rpc.Constants;
import org.apache.dubbo.rpc.RpcException;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.cache.ConsulCache;
import com.orbitz.consul.cache.KVCache;
import com.orbitz.consul.model.kv.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;


/**
 * metadata report impl for consul
 */
public class ConsulMetadataReport extends AbstractMetadataReport {

    private final Map<String, ConsulListener> watchListenerMap = new ConcurrentHashMap<>();

    private final Map<String, MappingDataListener> casListenerMap = new ConcurrentHashMap<>();

    private ConsulClient client;

    private KeyValueClient kvClient;

    private final int watchTimeout;

    /**
     * The ACL token
     */
    private final String token;

    private final String root;

    public ConsulMetadataReport(URL url) {
        super(url);

        token = url.getParameter(Constants.TOKEN_KEY, (String) null);
        root = url.getGroup(DEFAULT_ROOT);
        String host = url.getHost();
        int port = ConsulConstants.INVALID_PORT != url.getPort() ? url.getPort() : ConsulConstants.DEFAULT_PORT;
        client = new ConsulClient(host, port);

        Consul.Builder builder = Consul.builder().withHostAndPort(HostAndPort.fromParts(host, port));
        if(StringUtils.isNotEmpty(token)){
            builder.withAclToken(token);
        }

        this.kvClient = builder.build().keyValueClient();
        this.watchTimeout = url.getParameter(ConsulConstants.WATCH_TIMEOUT, ConsulConstants.DEFAULT_WATCH_TIMEOUT);
    }

    @Override
    public void publishAppMetadata(SubscriberMetadataIdentifier identifier, MetadataInfo metadataInfo) {
        if(metadataInfo.getContent() != null){
            this.storeMetadata(identifier, metadataInfo.getContent());
        }
    }

    @Override
    public void unPublishAppMetadata(SubscriberMetadataIdentifier identifier, MetadataInfo metadataInfo) {
        this.deleteMetadata(identifier);
    }

    @Override
    public MetadataInfo getAppMetadata(SubscriberMetadataIdentifier identifier, Map<String, String> instanceMetadata) {
        String content = new String(Base64.getDecoder().decode(getMetadata(identifier)));
        return JsonUtils.toJavaObject(content, MetadataInfo.class);
    }

    @Override
    public ConfigItem getConfigItem(String serviceKey, String group) {
        String key = buildMappingKey(group, serviceKey);
        String content = getConfig(key);
        return new ConfigItem(content, content);
    }

    @Override
    public boolean registerServiceAppMapping(String serviceInterface, String defaultMappingGroup,
                                             String newConfigContent, Object ticket) {
        try{
            if(null != ticket && !(ticket instanceof String)){
                throw new IllegalArgumentException("redis publishConfigCas requires stat type ticket");
            }

            String key = buildMappingKey(defaultMappingGroup, serviceInterface);
            Response<Boolean> response;

            if(token == null){
                response = client.setKVValue(key, newConfigContent);
            }else{
                response = client.setKVValue(key, newConfigContent, token, null);
            }

            return response != null && response.getValue();
        }catch(Exception e){
            logger.warn(LoggerCodeConstants.TRANSPORT_FAILED_RESPONSE, "", "", "consul publishConfigCas failed.", e);
            return false;
        }
    }

    @Override
    public Set<String> getServiceAppMapping(String serviceKey, MappingListener listener, URL url) {
        String group = ServiceNameMapping.DEFAULT_MAPPING_GROUP;

        if(casListenerMap.get(buildListenerKey(serviceKey, group)) == null){
            addCasServiceMappingListener(serviceKey, group, listener);
        }

        String key = buildMappingKey(group, serviceKey);
        String content = getConfig(key);
        return ServiceNameMapping.getAppNames(content);
    }

    @Override
    public Set<String> getServiceAppMapping(String serviceKey, URL url) {
        String group = url.getGroup(ServiceNameMapping.DEFAULT_MAPPING_GROUP);
        String key = buildMappingKey(group, serviceKey);
        String content = getConfig(key);
        return ServiceNameMapping.getAppNames(content);
    }

    @Override
    public void removeServiceAppMappingListener(String serviceKey, MappingListener listener) {
        String group = ServiceNameMapping.DEFAULT_MAPPING_GROUP;

        MappingDataListener mappingDataListener = casListenerMap.get(buildListenerKey(serviceKey, group));
        if(mappingDataListener != null){
            removeCasServiceMappingListener(serviceKey, group, listener);
        }
    }

    @Override
    public String getServiceDefinition(MetadataIdentifier metadataIdentifier) {
        return getMetadata(metadataIdentifier);
    }

    @Override
    protected void doStoreProviderMetadata(MetadataIdentifier providerMetadataIdentifier, String serviceDefinitions) {
        this.storeMetadata(providerMetadataIdentifier, serviceDefinitions);
    }

    @Override
    protected void doStoreConsumerMetadata(MetadataIdentifier consumerMetadataIdentifier, String value) {
        this.storeMetadata(consumerMetadataIdentifier, value);
    }

    @Override
    protected void doSaveMetadata(ServiceMetadataIdentifier serviceMetadataIdentifier, URL url) {
        this.storeMetadata(serviceMetadataIdentifier, URL.encode(url.toFullString()));
    }

    @Override
    protected void doRemoveMetadata(ServiceMetadataIdentifier serviceMetadataIdentifier) {
        this.deleteMetadata(serviceMetadataIdentifier);
    }

    @Override
    protected List<String> doGetExportedURLs(ServiceMetadataIdentifier metadataIdentifier) {
        //todo encode and decode
        String content = getMetadata(metadataIdentifier);
        if (StringUtils.isEmpty(content)) {
            return Collections.emptyList();
        }
        return Arrays.asList(URL.decode(content));
    }

    @Override
    protected void doSaveSubscriberData(SubscriberMetadataIdentifier subscriberMetadataIdentifier, String urlListStr) {
        this.storeMetadata(subscriberMetadataIdentifier, urlListStr);
    }

    @Override
    protected String doGetSubscribedURLs(SubscriberMetadataIdentifier subscriberMetadataIdentifier) {
        return getMetadata(subscriberMetadataIdentifier);
    }

    private String getConfig(String key) {
        Response<GetValue> response;

        if(token == null){
            response = client.getKVValue(key);
        }else{
            response = client.getKVValue(key, token);
        }

        if(response == null || response.getValue() == null){
            return null;
        }

        Base64.Decoder decoder = Base64.getDecoder();
        return new String(decoder.decode(response.getValue().getValue()));
    }

    private void storeMetadata(BaseMetadataIdentifier identifier, String v) {
        try{
            if(token == null){
                client.setKVValue(identifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), v);
            }else{
                client.setKVValue(identifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), v, token, null);
            }
        }catch(Throwable t){
            logger.error("Failed to put " + identifier + " to consul " + v + ", cause: " + t.getMessage(), t);
            throw new RpcException("Failed to put " + identifier + " to consul " + v + ", cause: " + t.getMessage(), t);
        }
    }

    private void deleteMetadata(BaseMetadataIdentifier identifier) {
        try{
            if(token == null){
                client.deleteKVValue(identifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY));
            }else{
                client.deleteKVValue(identifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), token);
            }
        }catch(Throwable t){
            logger.error("Failed to delete " + identifier + " from consul , cause: " + t.getMessage(), t);
            throw new RpcException("Failed to delete " + identifier + " from consul , cause: " + t.getMessage(), t);
        }
    }

    private String getMetadata(BaseMetadataIdentifier identifier) {
        try{
            Response<GetValue> response;

            if(token == null){
                response = client.getKVValue(identifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY));
            }else{
                response = client.getKVValue(identifier.getUniqueKey(KeyTypeEnum.UNIQUE_KEY), token);
            }

            //FIXME CHECK
            if(response != null && response.getValue() != null){
                //todo check decode value and value diff
                return response.getValue().getValue();
            }
            return null;
        }catch(Throwable t){
            logger.error("Failed to get " + identifier + " from consul , cause: " + t.getMessage(), t);
            throw new RpcException("Failed to get " + identifier + " from consul , cause: " + t.getMessage(), t);
        }
    }

    private void addCasServiceMappingListener(String serviceKey, String group, MappingListener listener) {
        String listenerKey = buildListenerKey(ServiceNameMapping.DEFAULT_MAPPING_GROUP, serviceKey);
        MappingDataListener mappingDataListener = casListenerMap.computeIfAbsent(listenerKey,
            (k)->new MappingDataListener(serviceKey));

        mappingDataListener.addListener(listener);

        ConsulListener consulListener = watchListenerMap.computeIfAbsent(listenerKey,
            (k)->new ConsulListener(serviceKey, group));
        consulListener.addListener(mappingDataListener);
    }

    private void removeCasServiceMappingListener(String serviceKey, String group, MappingListener listener) {
        String listenerKey = buildListenerKey(ServiceNameMapping.DEFAULT_MAPPING_GROUP, serviceKey);
        MappingDataListener mappingDataListener = casListenerMap.get(listenerKey);

        if(mappingDataListener != null){
            mappingDataListener.removeListener(listener);
            if(mappingDataListener.isEmpty()){
                ConsulListener consulListener = watchListenerMap.get(listenerKey);

                if(consulListener != null){
                    consulListener.removeListener(mappingDataListener);
                    watchListenerMap.remove(listenerKey);
                }

                casListenerMap.remove(listenerKey, mappingDataListener);
            }
        }
    }

    private String buildMappingKey(String group, String serviceKey) {
        StringJoiner joiner = new StringJoiner(CommonConstants.GROUP_CHAR_SEPARATOR);

        joiner.add(root).add(group).add(serviceKey);

        return joiner.toString();
    }

    private String buildListenerKey(String group, String serviceKey) {
        StringJoiner joiner = new StringJoiner(CommonConstants.PROPERTIES_CHAR_SEPARATOR);

        joiner.add(root).add(group).add(serviceKey);

        return joiner.toString();
    }

    class ConsulListener implements ConsulCache.Listener<String, Value> {

        private KVCache kvCache;

        private final Set<ConfigurationListener> listeners = new LinkedHashSet<>();

        private final String normalizedKey;

        private final String group;

        public ConsulListener(String normalizedKey, String group) {
            this.normalizedKey = normalizedKey;
            this.group = group;
            this.initKVCache();
        }

        private void initKVCache() {
            this.kvCache = KVCache.newCache(ConsulMetadataReport.this.kvClient, normalizedKey,
                ConsulMetadataReport.this.watchTimeout);
            this.kvCache.addListener(this);
            this.kvCache.start();
        }

        public void notify(Map<String, Value> newValues) {
            Optional<Value> newValue = newValues.values().stream().filter((value)->value.getKey().equals(normalizedKey))
                .findAny();
            newValue.ifPresent((value)->{
                Optional<String> decodedValue = newValue.get().getValueAsString();
                decodedValue.ifPresent((v)->{
                    this.listeners.forEach((l)->{
                        ConfigChangedEvent event = new ConfigChangedEvent(normalizedKey, group, v,
                            ConfigChangeType.MODIFIED);
                        l.process(event);
                    });
                });
            });
        }

        private void addListener(ConfigurationListener listener) {
            this.listeners.add(listener);
        }

        private void removeListener(ConfigurationListener listener) {
            this.listeners.remove(listener);
        }

    }

    static class MappingDataListener implements ConfigurationListener {

        private String serviceKey;

        private Set<MappingListener> listeners;

        public MappingDataListener(String serviceKey) {
            this.serviceKey = serviceKey;
            this.listeners = new HashSet<>();
        }

        public void addListener(MappingListener listener) {
            this.listeners.add(listener);
        }

        public void removeListener(MappingListener listener) {
            this.listeners.remove(listener);
        }

        public boolean isEmpty() {
            return listeners.isEmpty();
        }

        @Override
        public void process(ConfigChangedEvent event) {
            if(ConfigChangeType.DELETED == event.getChangeType()){
                return;
            }

            if(!serviceKey.equals(event.getKey()) || !serviceKey.equals(event.getGroup())){
                return;
            }

            Set<String> apps = ServiceNameMapping.getAppNames(event.getContent());

            MappingChangedEvent mappingChangedEvent = new MappingChangedEvent(serviceKey, apps);

            listeners.forEach((listener)->listener.onEvent(mappingChangedEvent));
        }

    }

}
