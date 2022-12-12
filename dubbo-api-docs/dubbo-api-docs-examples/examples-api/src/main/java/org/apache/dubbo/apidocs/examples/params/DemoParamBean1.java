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

import java.util.List;
import java.util.Map;

/**
 * demo request bean.
 */
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Boolean getMan() {
        return man;
    }

    public void setMan(Boolean man) {
        this.man = man;
    }

    public List<DemoParamBean1SubBean1> getSubBean() {
        return subBean;
    }

    public void setSubBean(List<DemoParamBean1SubBean1> subBean) {
        this.subBean = subBean;
    }

    public Map<String, DemoParamBean1SubBean1> getSubBean2() {
        return subBean2;
    }

    public void setSubBean2(Map<String, DemoParamBean1SubBean1> subBean2) {
        this.subBean2 = subBean2;
    }

    public String[] getStrArray() {
        return strArray;
    }

    public void setStrArray(String[] strArray) {
        this.strArray = strArray;
    }

    public DemoParamBean1SubBean1[] getStrArray2() {
        return strArray2;
    }

    public void setStrArray2(DemoParamBean1SubBean1[] strArray2) {
        this.strArray2 = strArray2;
    }

    public TestEnum getTestEnum() {
        return testEnum;
    }

    public void setTestEnum(TestEnum testEnum) {
        this.testEnum = testEnum;
    }

    public DemoParamBean1SubBean1 getSubBean3() {
        return subBean3;
    }

    public void setSubBean3(DemoParamBean1SubBean1 subBean3) {
        this.subBean3 = subBean3;
    }

    public Map getMap1() {
        return map1;
    }

    public void setMap1(Map map1) {
        this.map1 = map1;
    }

    public Map<Object, Object> getMap2() {
        return map2;
    }

    public void setMap2(Map<Object, Object> map2) {
        this.map2 = map2;
    }

    public List getList1() {
        return list1;
    }

    public void setList1(List list1) {
        this.list1 = list1;
    }

    public List<Object> getList2() {
        return list2;
    }

    public void setList2(List<Object> list2) {
        this.list2 = list2;
    }

    public Object getObj1() {
        return obj1;
    }

    public void setObj1(Object obj1) {
        this.obj1 = obj1;
    }

}
