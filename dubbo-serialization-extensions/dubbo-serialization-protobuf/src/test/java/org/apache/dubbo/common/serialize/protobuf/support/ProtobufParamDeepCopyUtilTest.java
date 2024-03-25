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
package org.apache.dubbo.common.serialize.protobuf.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.serialize.protobuf.support.model.GooglePB;
import org.apache.dubbo.rpc.protocol.injvm.DefaultParamDeepCopyUtil;
import org.apache.dubbo.rpc.protocol.injvm.ParamDeepCopyUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.protobuf.InvalidProtocolBufferException;

public class ProtobufParamDeepCopyUtilTest {
    private ParamDeepCopyUtil paramDeepCopyUtil;

    @BeforeEach
    public void setUp() {
        URL url = mockURL();
        this.paramDeepCopyUtil = url.getOrDefaultFrameworkModel().getExtensionLoader(ParamDeepCopyUtil.class)
            .getExtension(url.getParameter(CommonConstants.INJVM_COPY_UTIL_KEY, DefaultParamDeepCopyUtil.NAME));
    }

    @Test
    public void testProtobufDeepCopy() throws InvalidProtocolBufferException {
        ProtobufUtils.marshaller(GooglePB.PBRequestType.getDefaultInstance());
        GooglePB.PhoneNumber phoneNumber = GooglePB.PhoneNumber.newBuilder()
            .setNumber("134123781291").build();
        List<GooglePB.PhoneNumber> phoneNumberList = new ArrayList<>();
        phoneNumberList.add(phoneNumber);
        Map<String, GooglePB.PhoneNumber> phoneNumberMap = new HashMap<>();
        phoneNumberMap.put("someUser", phoneNumber);
        GooglePB.PBRequestType request = GooglePB.PBRequestType.newBuilder()
            .setAge(15).setCash(10).setMoney(16.0).setNum(100L)
            .addAllPhone(phoneNumberList).putAllDoubleMap(phoneNumberMap).build();
        GooglePB.PBRequestType copyResult = paramDeepCopyUtil.copy(mockURL(), request, GooglePB.PBRequestType.class);
        String jsonString = ProtobufUtils.serializeJson(request);
        String jsonString2 = ProtobufUtils.serializeJson(copyResult);
        assertEquals(jsonString, jsonString2);
        assertNotEquals(System.identityHashCode(request), System.identityHashCode(copyResult));
        List<GooglePB.PhoneNumber> copyPhoneList = copyResult.getPhoneList();
        Map<String, GooglePB.PhoneNumber> copyDoubleMap = copyResult.getDoubleMap();
        assertNotEquals(System.identityHashCode(phoneNumberList.get(0)), System.identityHashCode(copyPhoneList.get(0)));
        assertNotEquals(System.identityHashCode(phoneNumberMap.get("someUser")), System.identityHashCode(copyDoubleMap.get("someUser")));
    }

    URL mockURL() {
        URL url = new URL("dubbo", "localhost", 20880);
        return url;
    }
}
