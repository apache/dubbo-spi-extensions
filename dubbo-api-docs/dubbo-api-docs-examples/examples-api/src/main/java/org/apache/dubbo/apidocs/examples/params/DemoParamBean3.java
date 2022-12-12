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
import org.apache.dubbo.apidocs.annotations.ResponseProperty;

/**
 * DemoParamBean3.
 */
public class DemoParamBean3 {

    @RequestParam("a string")
    @ResponseProperty("result a string")
    private String string;

    @RequestParam("a DemoParamBean4")
    @ResponseProperty("result a bean4")
    private DemoParamBean4 bean4;

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    public DemoParamBean4 getBean4() {
        return bean4;
    }

    public void setBean4(DemoParamBean4 bean4) {
        this.bean4 = bean4;
    }
}
