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
import org.apache.dubbo.rpc.protocol.PermittedSerializationKeeper;
import org.apache.dubbo.rpc.protocol.dubbo.DecodeableRpcInvocation;
import org.apache.dubbo.rpc.protocol.dubbo.DubboCodec;
import org.apache.dubbo.rpc.support.RpcUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.apache.dubbo.common.BaseServiceMetadata.keyWithoutGroup;
import static org.apache.dubbo.common.constants.CommonConstants.DEFAULT_VERSION;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_VERSION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHOD_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.gateway.provider.OmnipotentCommonConstants.$INVOKE_OMN;
import static org.apache.dubbo.gateway.provider.OmnipotentCommonConstants.ORIGIN_GENERIC_PARAMETER_TYPES;
import static org.apache.dubbo.gateway.provider.OmnipotentCommonConstants.ORIGIN_GROUP_KEY;
import static org.apache.dubbo.gateway.provider.OmnipotentCommonConstants.ORIGIN_PARAMETER_TYPES_DESC;
import static org.apache.dubbo.rpc.Constants.SERIALIZATION_ID_KEY;
import static org.apache.dubbo.rpc.Constants.SERIALIZATION_SECURITY_CHECK_KEY;

public class SnfDecodeableRpcInvocation extends DecodeableRpcInvocation {

    private static final String DEFAULT_OMNIPOTENT_SERVICE = OmnipotentService.class.getName();


    private static final boolean CHECK_SERIALIZATION = Boolean.parseBoolean(System.getProperty(SERIALIZATION_SECURITY_CHECK_KEY, "true"));

    public SnfDecodeableRpcInvocation(FrameworkModel frameworkModel, Channel channel, Request request, InputStream is, byte id) {
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

        try {
            Object[] args = DubboCodec.EMPTY_OBJECT_ARRAY;
            Class<?>[] pts = DubboCodec.EMPTY_CLASS_ARRAY;
            if (desc.length() > 0) {
                pts = drawPts(path, version, desc, pts);
                if (pts == DubboCodec.EMPTY_CLASS_ARRAY) {
                    // Service not found
                    pts = ReflectUtils.desc2classArray(desc);
                }
                args = drawArgs(in, pts);
            }
            setParameterTypes(pts);
            setAttachment(ORIGIN_GENERIC_PARAMETER_TYPES, pts);

            Map<String, Object> map = in.readAttachments();
            Class<?>[] retryPts = null;
            if (CollectionUtils.isNotEmptyMap(map)) {
                if (map.containsKey(ORIGIN_PARAMETER_TYPES_DESC)) {
                    String originParameterTypesDesc = map.get(ORIGIN_PARAMETER_TYPES_DESC).toString();
                    retryPts = drawPts(path, version, originParameterTypesDesc, DubboCodec.EMPTY_CLASS_ARRAY);
                    boolean snf = (retryPts == DubboCodec.EMPTY_CLASS_ARRAY) && !RpcUtils.isGenericCall(originParameterTypesDesc, getMethodName()) && !RpcUtils.isEcho(originParameterTypesDesc, getMethodName());
                    if (snf) {
                        setAttachment(OmnipotentCommonConstants.ORIGIN_PATH_KEY, getAttachment(PATH_KEY));
                        // Replace serviceName in req with omn
                        setAttachment(PATH_KEY, DEFAULT_OMNIPOTENT_SERVICE);
                        setAttachment(INTERFACE_KEY, DEFAULT_OMNIPOTENT_SERVICE);

                        // version
                        setAttachment(OmnipotentCommonConstants.ORIGIN_VERSION_KEY, getAttachment(VERSION_KEY));
                        setAttachment(VERSION_KEY, DEFAULT_VERSION);

                        // method
                        setAttachment(OmnipotentCommonConstants.ORIGIN_METHOD_KEY, getMethodName());
                        setAttachment(METHOD_KEY, $INVOKE_OMN);
                        setMethodName($INVOKE_OMN);
                        setParameterTypes(new Class<?>[]{Invocation.class});

                        // Omn needs to use the default path, version and group,
                        // and the original value starts with origin to save the variable
                        map.remove(PATH_KEY);
                        map.remove(VERSION_KEY);
                        if (map.containsKey(GROUP_KEY)) {
                            map.put(ORIGIN_GROUP_KEY, map.get(GROUP_KEY));
                            map.remove(GROUP_KEY);
                        }
                        retryPts = (Class<?>[]) getObjectAttachments().get(ORIGIN_GENERIC_PARAMETER_TYPES);
                    }
                }

                addObjectAttachments(map);
            }

            boolean isConvert = false;
            for (Class<?> clazz : pts) {
                if (clazz == JavaBeanDescriptor.class) {
                    isConvert = true;
                    break;
                }
            }
            if (isConvert) {
                setParameterTypes(retryPts);
                pts = retryPts;
                Object[] newArgs = new Object[args.length];
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof JavaBeanDescriptor) {
                        newArgs[i] = JavaBeanSerializeUtil.deserialize((JavaBeanDescriptor) args[i]);
                    }
                }
                args = newArgs;
            }
            decodeArgument(channel, pts, args);
        } catch (ClassNotFoundException e) {
            throw new IOException(StringUtils.toString("Read invocation data failed.", e));
        } finally {
            Thread.currentThread().setContextClassLoader(originClassLoader);
            if (in instanceof Cleanable) {
                ((Cleanable) in).cleanup();
            }
        }
        return this;
    }

}
