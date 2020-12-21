package org.apache.dubbo.apidocs.core.beans;

import java.util.List;

/**
 * api module cache item.
 */
public class ModuleCacheItem {

    private String moduleDocName;

    private String moduleClassName;

    private String moduleVersion;

    private List<ApiCacheItem> moduleApiList;

    public String getModuleDocName() {
        return moduleDocName;
    }

    public void setModuleDocName(String moduleDocName) {
        this.moduleDocName = moduleDocName;
    }

    public String getModuleClassName() {
        return moduleClassName;
    }

    public void setModuleClassName(String moduleClassName) {
        this.moduleClassName = moduleClassName;
    }

    public String getModuleVersion() {
        return moduleVersion;
    }

    public void setModuleVersion(String moduleVersion) {
        this.moduleVersion = moduleVersion;
    }

    public List<ApiCacheItem> getModuleApiList() {
        return moduleApiList;
    }

    public void setModuleApiList(List<ApiCacheItem> moduleApiList) {
        this.moduleApiList = moduleApiList;
    }
}
