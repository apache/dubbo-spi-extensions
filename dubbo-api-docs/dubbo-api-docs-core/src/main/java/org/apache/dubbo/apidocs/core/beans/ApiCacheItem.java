package org.apache.dubbo.apidocs.core.beans;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * api cache item.
 */
@Getter
@Setter
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

}
