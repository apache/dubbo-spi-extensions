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
 * DemoParamBean4.
 */
public class DemoParamBean4 {

    @RequestParam("a string")
    @ResponseProperty("result a string")
    private String bean4Str;

    public String getBean4Str() {
        return bean4Str;
    }

    public void setBean4Str(String bean4Str) {
        this.bean4Str = bean4Str;
    }
}
