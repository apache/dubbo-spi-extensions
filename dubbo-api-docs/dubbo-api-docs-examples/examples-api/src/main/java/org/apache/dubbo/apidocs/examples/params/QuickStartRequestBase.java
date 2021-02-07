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
 * QuickStartRequestBase.
 *
 * @date 2021/1/26 15:24
 */
public class QuickStartRequestBase<E, T> implements java.io.Serializable {

    private static final long serialVersionUID = 373497393757790262L;

    @RequestParam(value = "Request method", required = true)
    private String method;

    private List<List<T>> body;

    private E body3;

    private List<T> body4;

    private Map<String, T> body5;

    private T[] body6;

    private QuickStartRequestBean body2;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public QuickStartRequestBean getBody2() {
        return body2;
    }

    public void setBody2(QuickStartRequestBean body2) {
        this.body2 = body2;
    }

    public E getBody3() {
        return body3;
    }

    public void setBody3(E body3) {
        this.body3 = body3;
    }

    public List<List<T>> getBody() {
        return body;
    }

    public void setBody(List<List<T>> body) {
        this.body = body;
    }

    public List<T> getBody4() {
        return body4;
    }

    public void setBody4(List<T> body4) {
        this.body4 = body4;
    }

    public Map<String, T> getBody5() {
        return body5;
    }

    public void setBody5(Map<String, T> body5) {
        this.body5 = body5;
    }

    public T[] getBody6() {
        return body6;
    }

    public void setBody6(T[] body6) {
        this.body6 = body6;
    }
}
