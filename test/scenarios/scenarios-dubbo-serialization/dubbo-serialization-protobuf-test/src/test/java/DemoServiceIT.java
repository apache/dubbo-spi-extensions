/*
 *
 *   Licensed to the Apache Software Foundation (ASF) under one or more
 *   contributor license agreements.  See the NOTICE file distributed with
 *   this work for additional information regarding copyright ownership.
 *   The ASF licenses this file to You under the Apache License, Version 2.0
 *   (the "License"); you may not use this file except in compliance with
 *   the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

import org.apache.dubbo.test.serialization.protobuf.BigPerson;
import org.apache.dubbo.test.serialization.protobuf.DemoService;
import org.apache.dubbo.test.serialization.protobuf.FullAddress;
import org.apache.dubbo.test.serialization.protobuf.PersonInfo;
import org.apache.dubbo.test.serialization.protobuf.Phone;

import com.google.protobuf.Empty;
import com.google.protobuf.Int32Value;
import com.google.protobuf.StringValue;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Random;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath*:spring/dubbo-demo-consumer.xml")
public class DemoServiceIT {
    @Autowired
    @Qualifier("demoService")
    private DemoService service;


    @Test
    public void testVoid() throws Exception {
        service.testVoid(Empty.getDefaultInstance());
    }

    @Test
    public void testString() throws Exception {
        Assert.assertTrue(service.testString(StringValue.of("world")).getValue().endsWith("world"));
    }

    @Test
    public void testBase() throws Exception {
        Random random = new Random(10000);
        int num = random.nextInt();
        Assert.assertEquals(100 + num, service.testBase(Int32Value.of(num)).getValue());
    }

    @Test
    public void testObject() throws Exception {
        Phone phone1 = Phone.newBuilder().setCountry("86").setArea("0571").setNumber("87654321")
            .setExtensionNumber("001").build();
        Phone phone2 = Phone.newBuilder().setCountry("86").setArea("0571").setNumber("87654321")
            .setExtensionNumber("002").build();

        PersonInfo pi = PersonInfo.newBuilder().addPhones(phone1).addPhones(phone2)
            .setFax(Phone.newBuilder().setCountry("86").setArea("0571").setNumber("87654321"))
            .setFullAddress(FullAddress.newBuilder().setCountryId("CN").setCountryName("CN").setProvinceName("zj")
                .setCityId("3480").setCityName("3480").setStreetAddress("wensanlu").setZipCode("315000")
            )
            .setMobileNo("13584652131").setMale(true).setDepartment("b2b")
            .setHomepageUrl("www.capcom.com").setJobTitle("qa").setName("superman")
            .build();

        BigPerson person = BigPerson.newBuilder()
            .setPersonId("superman111").setLoginName("superman")
            .setStatus(BigPerson.PersonStatus.ENABLED)
            .setEmail("sm@1.com").setPenName("pname")
            .setInfoProfile(pi)
            .build();

        Assert.assertEquals(service.testObject(person), person);
    }

}

