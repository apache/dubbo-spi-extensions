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
package org.apache.dubbo.apidocs.examples.params;

import org.apache.dubbo.apidocs.annotations.RequestParam;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * demo request bean.
 * @author klw(213539@qq.com)
 * 2020/10/30 9:58
 */
@Getter
@Setter
public class DemoParamBean1 {

    @RequestParam(value = "Name", description = "说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试" +
            "说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测" +
            "说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测" +
            "说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测" +
            "说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测" +
            "试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试说明测试")
    private String name;

    @RequestParam(value = "Age", description = "test description test description test description test description" +
            " test description test description")
    private Integer age;

    private Boolean man;

    @RequestParam("====subBean")
    private List<DemoParamBean1SubBean1> subBean;

    @RequestParam("Map")
    private Map<String, DemoParamBean1SubBean1> subBean2;

    @RequestParam("Array")
    private String[] strArray;

    @RequestParam("Array 2")
    private DemoParamBean1SubBean1[] strArray2;

    @RequestParam("Enumeration for test")
    private TestEnum testEnum;

    private DemoParamBean1SubBean1 subBean3;

    @RequestParam("Map without generics")
    private Map map1;

    @RequestParam("Map generic with Object")
    private Map<Object, Object> map2;

    @RequestParam("List without generics")
    private List list1;

    @RequestParam("List generic with Object")
    private List<Object> list2;

    @RequestParam("Object")
    private Object obj1;

}
