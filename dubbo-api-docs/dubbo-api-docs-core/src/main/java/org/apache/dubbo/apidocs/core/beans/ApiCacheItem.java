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

    private String description;

    private String apiRespDec;

    private String apiModelClass;

    private List<ApiParamsCacheItem> params;

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
}
