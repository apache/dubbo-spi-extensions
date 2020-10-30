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
package org.apache.dubbo.apidocs.core;

import org.apache.dubbo.apidocs.core.beans.HtmlTypeEnum;
import org.apache.dubbo.apidocs.core.beans.ParamBean;
import org.apache.dubbo.apidocs.core.providers.DubboDocProviderImpl;
import org.apache.dubbo.apidocs.core.providers.IDubboDocProvider;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.config.annotation.Service;
import org.apache.dubbo.apidocs.annotations.*;
import org.apache.dubbo.apidocs.utils.ClassTypeUtil;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scan and process dubbo doc annotations.
 *
 * @author klw(213539 @ qq.com)
 * 2020/10/29 17:55
 */
@Slf4j
@Import({DubboDocProviderImpl.class})
public class DubboApiDocsAnnotationScanner implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ApplicationConfig application;

    @Autowired
    private RegistryConfig registry;

    @Autowired
    private ProtocolConfig protocol;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        // Register dubbo doc provider
        IDubboDocProvider dubboDocProvider = applicationContext.getBean(IDubboDocProvider.class);
        exportDubboService(IDubboDocProvider.class, dubboDocProvider, false);

        log.info("================= Dubbo API Docs--Start scanning and processing doc annotations ================");

        Map<String, Object> apiModules = applicationContext.getBeansWithAnnotation(ApiModule.class);
        apiModules.forEach((key, apiModuleTemp) -> {
            Class<?> apiModuleClass;
            if (AopUtils.isAopProxy(apiModuleTemp)) {
                apiModuleClass = AopUtils.getTargetClass(apiModuleTemp);
            } else {
                apiModuleClass = apiModuleTemp.getClass();
            }
            ApiModule moduleAnn = apiModuleClass.getAnnotation(ApiModule.class);
            if (!apiModuleClass.isAnnotationPresent(Service.class) && !apiModuleClass.isAnnotationPresent(DubboService.class)) {
                log.warn(
                        "【Warning】{} @ApiModule annotation is used, but it is not a dubbo provider (without {} annotation)",
                        apiModuleClass.getName(), Service.class.getName() + " or " + DubboService.class.getName());
                return;
            }
            boolean async;
            if (apiModuleClass.isAnnotationPresent(Service.class)) {
                Service dubboService = apiModuleClass.getAnnotation(Service.class);
                async = dubboService.async();
            } else {
                DubboService dubboService = apiModuleClass.getAnnotation(DubboService.class);
                async = dubboService.async();
            }
            Map<String, Object> moduleCacheItem = new HashMap<>(4);
            DubboApiDocsCache.addApiModule(moduleAnn.apiInterface().getCanonicalName(), moduleCacheItem);
            //module name
            moduleCacheItem.put("moduleChName", moduleAnn.value());
            //interface name containing package path
            moduleCacheItem.put("moduleClassName", moduleAnn.apiInterface().getCanonicalName());
            //module version
            moduleCacheItem.put("moduleVersion", moduleAnn.version());

            Method[] apiModuleMethods = apiModuleClass.getMethods();
            // API basic information list in module cache
            List<Map<String, Object>> moduleApiList = new ArrayList<>(apiModuleMethods.length);
            moduleCacheItem.put("moduleApiList", moduleApiList);
            for (Method method : apiModuleMethods) {
                if (method.isAnnotationPresent(ApiDoc.class)) {
                    processApiDocAnnotation(method, moduleApiList, moduleAnn, async, moduleCacheItem);
                }
            }
        });
        log.info("================= Dubbo API Docs-- doc annotations scanning and processing completed ================");
    }

    private void processApiDocAnnotation(Method method, List<Map<String, Object>> moduleApiList, ApiModule moduleAnn,
                                         boolean async, Map<String, Object> moduleCacheItem) {
        ApiDoc dubboApi = method.getAnnotation(ApiDoc.class);

        // API basic information in API list in module
        Map<String, Object> apiListItem = new HashMap<>(4);
        moduleApiList.add(apiListItem);
        //API method name
        apiListItem.put("apiName", method.getName());
        //API name
        apiListItem.put("apiChName", dubboApi.value());
        // API description
        apiListItem.put("description", dubboApi.description());
        //API version
        apiListItem.put("apiVersion", dubboApi.version());
        //Description of API return data
        apiListItem.put("apiRespDec", dubboApi.responseClassDescription());

        // Interface parameters and response information
        Map<String, Object> apiParamsAndResp = new HashMap<>(2);
        DubboApiDocsCache.addApiParamsAndResp(
                moduleAnn.apiInterface().getCanonicalName() + "." + method.getName(), apiParamsAndResp);

        Class<?>[] argsClass = method.getParameterTypes();
        Annotation[][] argsAnns = method.getParameterAnnotations();
        Parameter[] parameters = method.getParameters();
        List<Map<String, Object>> paramList = new ArrayList<>(argsClass.length);
        apiParamsAndResp.put("async", async);
        apiParamsAndResp.put("apiName", method.getName());
        apiParamsAndResp.put("apiChName", dubboApi.value());
        apiParamsAndResp.put("apiVersion", dubboApi.version());
        apiParamsAndResp.put("apiRespDec", dubboApi.responseClassDescription());
        apiParamsAndResp.put("apiModelClass", moduleCacheItem.get("moduleClassName"));
        apiParamsAndResp.put("params", paramList);
        apiParamsAndResp.put("response", ClassTypeUtil.calss2Json(method.getGenericReturnType(), method.getReturnType()));
        for (int i = 0; i < argsClass.length; i++) {
            Class<?> argClass = argsClass[i];
            Annotation[] argAnns = argsAnns[i];
            Map<String, Object> prarmListItem = new HashMap<>(2);
            paramList.add(prarmListItem);
            prarmListItem.put("prarmType", argClass.getCanonicalName());
            prarmListItem.put("prarmIndex", i);
            RequestParam requestParam = null;
            // Handling @RequestParam annotations on parameters
            for (Annotation ann : argAnns) {
                if (ann instanceof RequestParam) {
                    requestParam = (RequestParam) ann;
                }
            }
            ParamBean paramBean = this.processHtmlType(argClass, requestParam, null);
            if (paramBean == null) {
                // Not a basic type, handling properties in method parameters
                List<ParamBean> apiParamsList = processField(argClass);
                if (apiParamsList != null && !apiParamsList.isEmpty()) {
                    prarmListItem.put("prarmInfo", apiParamsList);
                }
            } else {
                // Is the basic type
                Parameter methodParameter = parameters[i];
                prarmListItem.put("name", methodParameter.getName());
                prarmListItem.put("htmlType", paramBean.getHtmlType().name());
                prarmListItem.put("allowableValues", paramBean.getAllowableValues());
                if (requestParam != null) {

                    // Handling requestparam annotations on parameters
                    prarmListItem.put("nameCh", requestParam.value());
                    prarmListItem.put("description", requestParam.description());
                    prarmListItem.put("example", requestParam.example());
                    prarmListItem.put("defaultValue", requestParam.defaultValue());
                    prarmListItem.put("required", requestParam.required());
                } else {
                    prarmListItem.put("required", false);
                }
            }
        }
    }

    /**
     * For the attributes in the method parameters, only one layer is processed.
     * The deeper layer is directly converted to JSON, and the deeper layer is up to 5 layers
     */
    private List<ParamBean> processField(Class<?> argClass) {

        List<ParamBean> apiParamsList = new ArrayList(16);
        // get all fields
        List<Field> allFields = ClassTypeUtil.getAllFields(null, argClass);
        for (Field field : allFields) {
            ParamBean paramBean = new ParamBean();
            paramBean.setName(field.getName());
            paramBean.setJavaType(field.getType().getCanonicalName());
            RequestParam requestParam = null;
            if (field.isAnnotationPresent(RequestParam.class)) {
                // Handling @RequestParam annotations on properties
                requestParam = field.getAnnotation(RequestParam.class);
                paramBean.setNameCh(requestParam.value());
                paramBean.setRequired(requestParam.required());
                paramBean.setDescription(requestParam.description());
                paramBean.setExample(requestParam.example());
                paramBean.setDefaultValue(requestParam.defaultValue());
            } else {
                paramBean.setRequired(false);
            }

            if (this.processHtmlType(field.getType(), requestParam, paramBean) == null) {
                // Not a basic type, handle as JSON
                Object objResult = ClassTypeUtil.initClassTypeWithDefaultValue(
                        field.getGenericType(), field.getType(), 0);
                if (!ClassTypeUtil.isBaseType(objResult)) {
                    paramBean.setHtmlType(HtmlTypeEnum.TEXT_AREA);
                    paramBean.setSubParamsJson(JSON.toJSONString(objResult, ClassTypeUtil.FAST_JSON_FEATURES));
                }
            }
            apiParamsList.add(paramBean);
        }
        return apiParamsList;
    }

    /**
     * Determine what HTML form elements to use.
     * 2020/10/29 18:24
     *
     * @param classType  classType
     * @param annotation annotation
     * @param prarm      prarm
     * @return org.apache.dubbo.apidocs.core.beans.ParamBean
     */
    private ParamBean processHtmlType(Class<?> classType, RequestParam annotation, ParamBean prarm) {
        if (prarm == null) {
            prarm = new ParamBean();
        }
        if (annotation != null) {
            prarm.setAllowableValues(annotation.allowableValues());
        }
        // Is there any allowed values
        boolean hasAllowableValues = (prarm.getAllowableValues() != null && prarm.getAllowableValues().length > 0);
        // Processed or not
        boolean processed = false;
        if (Integer.class.isAssignableFrom(classType)) {
            prarm.setHtmlType(HtmlTypeEnum.NUMBER_INTEGER);
            processed = true;
        } else if (Byte.class.isAssignableFrom(classType)) {
            prarm.setHtmlType(HtmlTypeEnum.TEXT_BYTE);
            processed = true;
        } else if (Long.class.isAssignableFrom(classType)) {
            prarm.setHtmlType(HtmlTypeEnum.NUMBER_INTEGER);
            processed = true;
        } else if (Double.class.isAssignableFrom(classType)) {
            prarm.setHtmlType(HtmlTypeEnum.NUMBER_DECIMAL);
            processed = true;
        } else if (Float.class.isAssignableFrom(classType)) {
            prarm.setHtmlType(HtmlTypeEnum.NUMBER_DECIMAL);
            processed = true;
        } else if (String.class.isAssignableFrom(classType)) {
            prarm.setHtmlType(HtmlTypeEnum.TEXT);
            processed = true;
        } else if (Character.class.isAssignableFrom(classType)) {
            prarm.setHtmlType(HtmlTypeEnum.TEXT_CHAR);
            processed = true;
        } else if (Short.class.isAssignableFrom(classType)) {
            prarm.setHtmlType(HtmlTypeEnum.NUMBER_INTEGER);
            processed = true;
        }
        if (processed) {
            // Processed, time to return
            if (hasAllowableValues) {
                // Allowed values has value, change to select
                prarm.setHtmlType(HtmlTypeEnum.SELECT);
            }
            return prarm;
        }

        // haven't dealt with it. Go on
        if (Boolean.class.isAssignableFrom(classType)) {
            prarm.setHtmlType(HtmlTypeEnum.SELECT);
            // Boolean can only be true / false. No matter what the previous allowed value is, it is forced to replace
            prarm.setAllowableValues(new String[]{"true", "false"});
            processed = true;
        } else if (Enum.class.isAssignableFrom(classType)) {
            // process enum
            prarm.setHtmlType(HtmlTypeEnum.SELECT);
            if (!hasAllowableValues) {
                // If there is no optional value, it is taken from the enumeration.
                //TODO If there is an optional value, it is necessary
                // to check whether the optional value matches the enumeration. It is add it later
                Object[] enumConstants = classType.getEnumConstants();
                String[] enumAllowableValues = new String[enumConstants.length];
                try {
                    Method getNameMethod = classType.getMethod("name");
                    for (int i = 0; i < enumConstants.length; i++) {
                        Object obj = enumConstants[i];
                        enumAllowableValues[i] = (String) getNameMethod.invoke(obj);
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    log.error("", e);
                }
                prarm.setAllowableValues(enumAllowableValues);
            }
            processed = true;
        }
        if (processed) {
            return prarm;
        }
        return null;
    }

    /**
     * export dubbo service for dubbo doc
     */
    private <I, T> void exportDubboService(Class<I> serviceClass, T serviceImplInstance, boolean async) {
        ServiceConfig<T> service = new ServiceConfig<>();
        service.setApplication(application);
        service.setRegistry(registry);
        service.setProtocol(protocol);
        service.setInterface(serviceClass);
        service.setRef(serviceImplInstance);
        service.setAsync(async);
//        service.setVersion("1.0.0");
        service.export();
    }

}
