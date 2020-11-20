package org.apache.dubbo.apidocs.core.beans;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * api module cache item.
 *
 * @author klw(213539 @ qq.com)
 * @date 2020/11/20 15:21
 */
@Getter
@Setter
public class ModuleCacheItem {

    private String moduleDocName;

    private String moduleClassName;

    private String moduleVersion;

    private List<ApiCacheItem> moduleApiList;

}
