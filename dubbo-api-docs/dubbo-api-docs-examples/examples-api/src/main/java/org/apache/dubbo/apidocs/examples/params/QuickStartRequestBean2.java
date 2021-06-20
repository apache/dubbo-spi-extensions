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

import java.math.BigDecimal;

/**
 * quick start demo request parameter bean.
 */
public class QuickStartRequestBean2 implements java.io.Serializable {

    private static final long serialVersionUID = -7214413446084107294L;

    @RequestParam(value = "You name", required = true, description = "please enter your full name", example = "Zhang San")
    private String name;

    @RequestParam(value = "multiple generic")
    private QuickStartRequestBase<QuickStartRequestBean, DemoParamBean4> requestBase;

    @RequestParam(value = "BigDecimal number")
    private BigDecimal bigDecimalNumber;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public QuickStartRequestBase<QuickStartRequestBean, DemoParamBean4> getRequestBase() {
        return requestBase;
    }

    public void setRequestBase(QuickStartRequestBase<QuickStartRequestBean, DemoParamBean4> requestBase) {
        this.requestBase = requestBase;
    }

    public BigDecimal getBigDecimalNumber() {
        return bigDecimalNumber;
    }

    public void setBigDecimalNumber(BigDecimal bigDecimalNumber) {
        this.bigDecimalNumber = bigDecimalNumber;
    }
}
