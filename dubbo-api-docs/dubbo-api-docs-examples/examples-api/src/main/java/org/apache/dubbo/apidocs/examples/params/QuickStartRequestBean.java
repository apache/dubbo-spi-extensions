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

/**
 * quick start demo request parameter bean.
 */
public class QuickStartRequestBean implements java.io.Serializable {

    private static final long serialVersionUID = -7214413446084107294L;

    @RequestParam(value = "You name", required = true, description = "please enter your full name", example = "Zhang San")
    private String name;

    @RequestParam(value = "You age", defaultValue = "18")
    private int age;

    @RequestParam("Are you a main?")
    private boolean man;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean getMan() {
        return man;
    }

    public void setMan(boolean man) {
        this.man = man;
    }

    @Override
    public String toString() {
        return "QuickStartRequestBean{" +
            "name='" + name + '\'' +
            ", age=" + age +
            ", man=" + man +
            '}';
    }
}
