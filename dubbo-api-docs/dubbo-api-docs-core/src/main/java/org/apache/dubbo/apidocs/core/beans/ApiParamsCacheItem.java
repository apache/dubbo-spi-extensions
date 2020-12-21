package org.apache.dubbo.apidocs.core.beans;

import java.util.List;

/**
 * api params cache item.
 */
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDocName() {
        return docName;
    }

    public void setDocName(String docName) {
        this.docName = docName;
    }

    public String getHtmlType() {
        return htmlType;
    }

    public void setHtmlType(String htmlType) {
        this.htmlType = htmlType;
    }

    public String[] getAllowableValues() {
        return allowableValues;
    }

    public void setAllowableValues(String[] allowableValues) {
        this.allowableValues = allowableValues;
    }

    public String getParamType() {
        return paramType;
    }

    public void setParamType(String paramType) {
        this.paramType = paramType;
    }

    public Integer getParamIndex() {
        return paramIndex;
    }

    public void setParamIndex(Integer paramIndex) {
        this.paramIndex = paramIndex;
    }

    public List<ParamBean> getParamInfo() {
        return paramInfo;
    }

    public void setParamInfo(List<ParamBean> paramInfo) {
        this.paramInfo = paramInfo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExample() {
        return example;
    }

    public void setExample(String example) {
        this.example = example;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }
}
