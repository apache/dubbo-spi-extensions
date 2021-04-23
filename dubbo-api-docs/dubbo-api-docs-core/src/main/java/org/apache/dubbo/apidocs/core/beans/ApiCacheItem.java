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
package org.apache.dubbo.apidocs.core.beans;

import java.util.List;

/**
 * api cache item.
 */
public class ApiCacheItem {

    private Boolean async;

    private String apiName;

    private String apiDocName;

    private String apiVersion;

    private String apiGroup;

    private String description;

    private String apiRespDec;

    private String apiModelClass;

    private List<ApiParamsCacheItem> params;

    private String paramsDesc;

    private String response;

    private String methodParamInfo;

    public Boolean getAsync() {
        return async;
    }

    public void setAsync(Boolean async) {
        this.async = async;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getApiDocName() {
        return apiDocName;
    }

    public void setApiDocName(String apiDocName) {
        this.apiDocName = apiDocName;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getApiRespDec() {
        return apiRespDec;
    }

    public void setApiRespDec(String apiRespDec) {
        this.apiRespDec = apiRespDec;
    }

    public String getApiModelClass() {
        return apiModelClass;
    }

    public void setApiModelClass(String apiModelClass) {
        this.apiModelClass = apiModelClass;
    }

    public List<ApiParamsCacheItem> getParams() {
        return params;
    }

    public void setParams(List<ApiParamsCacheItem> params) {
        this.params = params;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getMethodParamInfo() {
        return methodParamInfo;
    }

    public void setMethodParamInfo(String methodParamInfo) {
        this.methodParamInfo = methodParamInfo;
    }

    public String getApiGroup() {
        return apiGroup;
    }

    public void setApiGroup(String apiGroup) {
        this.apiGroup = apiGroup;
    }

    public String getParamsDesc() {
        return paramsDesc;
    }

    public void setParamsDesc(String paramsDesc) {
        this.paramsDesc = paramsDesc;
    }
}
