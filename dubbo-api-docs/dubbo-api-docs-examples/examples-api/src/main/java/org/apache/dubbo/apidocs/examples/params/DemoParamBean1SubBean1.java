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

/**
 * Attribute bean in DemoParamBean1.
 * @author klw(213539@qq.com)
 * 2020/10/30 9:49
 */
@Getter
@Setter
public class DemoParamBean1SubBean1 {

    @RequestParam("Sub Name")
    private String subName;

    @RequestParam("Sub Age")
    private Integer subAge;

    private TestEnum testEnum;

    // Circular reference for test
//    @RequestParam("====bean")
//    private DemoParamBean1 bean;

}
