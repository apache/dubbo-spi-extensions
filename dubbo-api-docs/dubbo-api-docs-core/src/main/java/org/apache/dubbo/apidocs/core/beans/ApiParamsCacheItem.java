package org.apache.dubbo.apidocs.core.beans;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * api params cache item.
 *
 * @author klw(213539 @ qq.com)
 * @date 2020/11/20 16:46
 */
@Getter
@Setter
public class ApiParamsCacheItem {

    private String name;

    private String docName;

    private String htmlType;

    private String[] allowableValues;

    private String paramType;

    private Integer paramIndex;

    private List<ParamBean> paramInfo;

    private String description;

    private String example;

    private String defaultValue;

    private Boolean required;

}
