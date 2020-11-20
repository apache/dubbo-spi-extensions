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
package org.apache.dubbo.apidocs.controller;

import org.apache.dubbo.apidocs.controller.vo.ApiInfoRequest;
import org.apache.dubbo.apidocs.controller.vo.CallDubboServiceRequest;
import org.apache.dubbo.apidocs.controller.vo.CallDubboServiceRequestInterfaceParam;
import org.apache.dubbo.apidocs.editor.CustomDateEditor;
import org.apache.dubbo.apidocs.editor.CustomLocalDateEditor;
import org.apache.dubbo.apidocs.editor.CustomLocalDateTimeEditor;
import org.apache.dubbo.apidocs.utils.DubboGenericUtil;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SimplePropertyPreFilter;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * dubbo doc ui server api.
 * @author klw(213539@qq.com)
 * 2020/11/14 20:23
 */
@Api(tags = {"alita-restful-API--demoAPI"})
@RestController
@Slf4j
@RequestMapping("/api")
public class DubboApiDocsController {

    private static final SimplePropertyPreFilter CLASS_NAME_PRE_FILTER = new SimplePropertyPreFilter(HashMap.class);
    static {
        // Remove the "class" attribute from the returned result
        CLASS_NAME_PRE_FILTER.getExcludes().add("class");
    }

    /**
     * retries for dubbo provider.
     */
    @Value("${dubbo.consumer.retries:2}")
    private int retries;

    /**
     * timeout.
     */
    @Value("${dubbo.consumer.timeout:1000}")
    private int timeout;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
        binder.registerCustomEditor(Date.class, new CustomDateEditor());
        binder.registerCustomEditor(LocalDate.class, new CustomLocalDateEditor());
        binder.registerCustomEditor(LocalDateTime.class, new CustomLocalDateTimeEditor());
    }

    /**
     * Set timeout and retries for {@link DubboGenericUtil}.
     * 2020/11/14 20:26
     */
    @PostConstruct
    public void setRetriesAndTimeout(){
        DubboGenericUtil.setRetriesAndTimeout(retries, timeout);
    }

    @ApiOperation(value = "request dubbo api", notes = "request dubbo api", httpMethod = "POST", produces = "application/json")
    @PostMapping("/requestDubbo")
    public Mono<String> callDubboService(CallDubboServiceRequest dubboCfg, @RequestBody List<CallDubboServiceRequestInterfaceParam> methodParams){
        String[] paramTypes = null;
        Object[] paramValues = null;
        if(CollectionUtils.isNotEmpty(methodParams)){
            paramTypes = new String[methodParams.size()];
            paramValues = new Object[methodParams.size()];
            for(int i = 0; i < methodParams.size(); i++){
                CallDubboServiceRequestInterfaceParam param = methodParams.get(i);
                paramTypes[i] = param.getParamType();
                Object paramValue = param.getParamValue();
                if(isBaseType(param.getParamType()) && null != paramValue){
                    if(paramValue instanceof Map){
                        Map<?, ?> tempMap = (Map<?, ?>) paramValue;
                        if(!tempMap.isEmpty()) {
                            this.emptyString2Null(tempMap);
                            paramValues[i] = tempMap.values().stream().findFirst().orElse(null);
                        }
                    } else {
                        paramValues[i] = emptyString2Null(paramValue);
                    }
                } else {
                    this.emptyString2Null(paramValue);
                    paramValues[i] = paramValue;
                }
            }
        }
        if (null == paramTypes) {
            paramTypes = new String[0];
        }
        if (null == paramValues) {
            paramValues = new Object[0];
        }
        CompletableFuture<Object> future = DubboGenericUtil.invoke(dubboCfg.getRegistryCenterUrl(), dubboCfg.getInterfaceClassName(),
                dubboCfg.getMethodName(), dubboCfg.isAsync(), paramTypes, paramValues);
        return Mono.fromFuture(future).map( o -> JSON.toJSONString(o, CLASS_NAME_PRE_FILTER));
    }

    private Object emptyString2Null(Object paramValue){
        if(null != paramValue) {
            if (paramValue instanceof String && StringUtils.isBlank((String) paramValue)) {
                return null;
            } else if (paramValue instanceof Map) {
                Map<String, Object> tempMap = (Map<String, Object>) paramValue;
                tempMap.forEach((k, v) -> {
                    if (v != null && v instanceof String && StringUtils.isBlank((String) v)) {
                        tempMap.put(k, null);
                    } else {
                        this.emptyString2Null(v);
                    }
                });
            }
        }
        return paramValue;
    }

    @ApiOperation(value = "Get basic information of all modules, excluding API parameter information", notes = "Get basic information of all modules, excluding API parameter information", httpMethod = "GET", produces = "application/json")
    @GetMapping("/apiModuleList")
    public Mono<String> apiModuleList(ApiInfoRequest apiInfoRequest){
        CallDubboServiceRequest req = new CallDubboServiceRequest();
        req.setRegistryCenterUrl("dubbo://" + apiInfoRequest.getDubboIp() + ":" + apiInfoRequest.getDubboPort());
        req.setInterfaceClassName("org.apache.dubbo.apidocs.core.providers.IDubboDocProvider");
        req.setMethodName("apiModuleList");
        req.setAsync(false);
        return callDubboService(req, null);
    }

    @ApiOperation(value = "Get the parameter information of the specified API", notes = "Get the parameter information of the specified API", httpMethod = "GET", produces = "application/json")
    @GetMapping("/apiParamsResp")
    public Mono<String> apiParamsResp(ApiInfoRequest apiInfoRequest){
        CallDubboServiceRequest req = new CallDubboServiceRequest();
        req.setRegistryCenterUrl("dubbo://" + apiInfoRequest.getDubboIp() + ":" + apiInfoRequest.getDubboPort());
        req.setInterfaceClassName("org.apache.dubbo.apidocs.core.providers.IDubboDocProvider");
        req.setMethodName("apiParamsResponseInfo");
        req.setAsync(false);

        List<CallDubboServiceRequestInterfaceParam> methodParams = new ArrayList<>(1);
        CallDubboServiceRequestInterfaceParam param = new CallDubboServiceRequestInterfaceParam();
        param.setParamType(String.class.getName());
        param.setParamValue(apiInfoRequest.getApiName());
        methodParams.add(param);
        return callDubboService(req, methodParams);
    }

    private static boolean isBaseType(String typeStr) {
        if ("java.lang.Integer".equals(typeStr) ||
                "java.lang.Byte".equals(typeStr) ||
                "java.lang.Long".equals(typeStr) ||
                "java.lang.Double".equals(typeStr) ||
                "java.lang.Float".equals(typeStr) ||
                "java.lang.Character".equals(typeStr) ||
                "java.lang.Short".equals(typeStr) ||
                "java.lang.Boolean".equals(typeStr) ||
                "java.lang.String".equals(typeStr)) {
            return true;
        }
        return false;
    }
}
