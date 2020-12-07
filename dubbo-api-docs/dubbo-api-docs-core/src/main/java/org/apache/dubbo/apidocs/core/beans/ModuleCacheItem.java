package org.apache.dubbo.apidocs.core.beans;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * api module cache item.
 */
@Getter
@Setter
public class ModuleCacheItem {

    private String moduleDocName;

    private String moduleClassName;

    private String moduleVersion;

    private List<ApiCacheItem> moduleApiList;

}
