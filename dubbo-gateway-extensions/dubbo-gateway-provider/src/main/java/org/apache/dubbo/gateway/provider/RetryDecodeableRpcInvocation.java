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

package org.apache.dubbo.gateway.provider;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.beanutil.JavaBeanDescriptor;
import org.apache.dubbo.common.beanutil.JavaBeanSerializeUtil;
import org.apache.dubbo.common.serialize.Cleanable;
import org.apache.dubbo.common.serialize.ObjectInput;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.ReflectUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.remoting.Channel;
import org.apache.dubbo.remoting.exchange.Request;
import org.apache.dubbo.remoting.transport.CodecSupport;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.model.FrameworkModel;
import org.apache.dubbo.rpc.protocol.dubbo.DecodeableRpcInvocation;
import org.apache.dubbo.rpc.protocol.dubbo.DubboCodec;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.common.BaseServiceMetadata.keyWithoutGroup;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_VERSION;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_VERSION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHOD_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.rpc.Constants.SERIALIZATION_ID_KEY;
import static org.apache.dubbo.rpc.Constants.SERIALIZATION_SECURITY_CHECK_KEY;

/**
 * Support for retrying {@link DecodeableRpcInvocation} when there is an exception handling extension
 */
public class RetryDecodeableRpcInvocation extends DecodeableRpcInvocation {

    private static final String DEFAULT_OMNIPOTENT_SERVICE = OmnipotentService.class.getName();

    public RetryDecodeableRpcInvocation(FrameworkModel frameworkModel, Channel channel, Request request, InputStream is, byte id) {
        super(frameworkModel, channel, request, is, id);
    }


    @Override
    public Object decode(Channel channel, InputStream input) throws IOException {
        ObjectInput in = CodecSupport.getSerialization(serializationType)
            .deserialize(channel.getUrl(), input);
        this.put(SERIALIZATION_ID_KEY, serializationType);

        String dubboVersion = in.readUTF();
        request.setVersion(dubboVersion);
        setAttachment(DUBBO_VERSION_KEY, dubboVersion);

        String path = in.readUTF();
        setAttachment(PATH_KEY, path);
        String version = in.readUTF();
        setAttachment(VERSION_KEY, version);

        setMethodName(in.readUTF());

        String desc = in.readUTF();
        setParameterTypesDesc(desc);

        ClassLoader originClassLoader = Thread.currentThread().getContextClassLoader();
        boolean existServiceKey = true;
        try {
            if (Boolean.parseBoolean(System.getProperty(SERIALIZATION_SECURITY_CHECK_KEY, "true"))) {
                try {
                    CodecSupport.checkSerialization(frameworkModel.getServiceRepository(), path, version, serializationType);
                } catch (IOException e) {
                    List<URL> urls = frameworkModel.getServiceRepository().lookupRegisteredProviderUrlsWithoutGroup(keyWithoutGroup(path, version));
                    if (CollectionUtils.isEmpty(urls)) {
                        existServiceKey = false;
                        //update to OmnipotentService when service not found
                        saveSceneAndUpdate();
                        path = getAttachment(PATH_KEY);
                        version = getAttachment(VERSION_KEY);

                    }
                }
            }
            Object[] args = DubboCodec.EMPTY_OBJECT_ARRAY;
            Class<?>[] pts = DubboCodec.EMPTY_CLASS_ARRAY;
            if (desc.length() > 0) {
                pts = drawPts(path, version, desc, pts);
                if (pts == DubboCodec.EMPTY_CLASS_ARRAY) {
                    pts = ReflectUtils.desc2classArray(desc);
                }
                args = drawArgs(in, pts);
            }

            if (getParameterTypes() == null) {
                setParameterTypes(pts);
            }
            if (!existServiceKey) {
                setAttachment(OmnipotentCommonConstants.ORIGIN_GENERIC_PARAMETER_TYPES, pts);
            }
            Map<String, Object> map = in.readAttachments();
            if (CollectionUtils.isNotEmptyMap(map)) {
                if (existServiceKey) {
                    // If the ServiceKey exists, use the original type
                    if (map.containsKey(OmnipotentCommonConstants.ORIGIN_PARAMETER_TYPES)) {
                        String originParameterTypesDesc = map.get(OmnipotentCommonConstants.ORIGIN_PARAMETER_TYPES).toString();
                        try {
                            pts = ReflectUtils.desc2classArray(originParameterTypesDesc);
                            setParameterTypes(pts);
                            for (int i = 0; i < args.length; i++) {
                                // In gateway mode, consumer has used JavaBeanDescriptor as parameter
                                if (args[i] instanceof JavaBeanDescriptor) {
                                    args[i] = JavaBeanSerializeUtil.deserialize((JavaBeanDescriptor) args[i]);
                                }
                            }
                        } catch (ClassNotFoundException e) {
                            throw new IOException(StringUtils.toString("Read invocation data failed, client " +
                                "must set originParameterType in gateway-mode, and can be recognized by the server.", e));
                        }
                    }
                } else {
                    // Omn needs to use the default path, version and group,
                    // and the original value starts with origin to save the variable
                    map.remove(PATH_KEY);
                    map.remove(VERSION_KEY);
                    if (map.containsKey(GROUP_KEY)) {
                        map.put(OmnipotentCommonConstants.ORIGIN_GROUP_KEY, map.get(GROUP_KEY));
                        map.remove(GROUP_KEY);
                    }
                }
                addObjectAttachments(map);
            }

            //decode argument ,may be callback
            decodeArgument(channel, pts, args);
        } catch (ClassNotFoundException e) {
            throw new IOException(StringUtils.toString("Read invocation data failed.", e));
        } finally {
            if ((in instanceof Cleanable)) {
                ((Cleanable) in).cleanup();
            }
            Thread.currentThread().setContextClassLoader(originClassLoader);
        }
        return this;
    }

    private void saveSceneAndUpdate() {
        setAttachment(OmnipotentCommonConstants.ORIGIN_PATH_KEY, getAttachment(PATH_KEY));
        // Replace serviceName in req with omn
        setAttachment(PATH_KEY, DEFAULT_OMNIPOTENT_SERVICE);
        setAttachment(INTERFACE_KEY, DEFAULT_OMNIPOTENT_SERVICE);

        // version
        setAttachment(OmnipotentCommonConstants.ORIGIN_VERSION_KEY, getAttachment(VERSION_KEY));
        setAttachment(VERSION_KEY, DEFAULT_VERSION);

        // method
        setAttachment(OmnipotentCommonConstants.ORIGIN_METHOD_KEY, getMethodName());
        setAttachment(METHOD_KEY, OmnipotentCommonConstants.$INVOKE_OMN);
        setMethodName(OmnipotentCommonConstants.$INVOKE_OMN);
        setParameterTypes(new Class<?>[]{Invocation.class});
    }

}
