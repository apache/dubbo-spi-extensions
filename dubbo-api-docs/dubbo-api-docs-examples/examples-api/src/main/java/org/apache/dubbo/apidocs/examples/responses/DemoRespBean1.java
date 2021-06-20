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
package org.apache.dubbo.apidocs.examples.responses;

import org.apache.dubbo.apidocs.annotations.ResponseProperty;

import java.util.List;
import java.util.Map;

/**
 * demo response bean 1.
 */
public class DemoRespBean1 {

    @ResponseProperty("Response code")
    private String code;

    @ResponseProperty("Response message")
    private String message;

    @ResponseProperty(value = "Response message 2", example = "This is response message 2")
    private String message2;

    @ResponseProperty("Map without generics")
    private Map map1;

    @ResponseProperty("Map generic with Object")
    private Map<Object, Object> map2;

    @ResponseProperty("List without generics")
    private List list1;

    @ResponseProperty("List generic with Object")
    private List<Object> list2;

    @ResponseProperty("Object")
    private Object obj1;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage2() {
        return message2;
    }

    public void setMessage2(String message2) {
        this.message2 = message2;
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
