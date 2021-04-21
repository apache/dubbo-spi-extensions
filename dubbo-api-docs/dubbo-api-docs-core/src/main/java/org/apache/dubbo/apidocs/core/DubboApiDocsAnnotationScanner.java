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

import org.apache.dubbo.apidocs.core.beans.ApiCacheItem;
import org.apache.dubbo.apidocs.core.beans.ApiParamsCacheItem;
import org.apache.dubbo.apidocs.core.beans.ModuleCacheItem;
import org.apache.dubbo.apidocs.core.beans.HtmlTypeEnum;
import org.apache.dubbo.apidocs.core.beans.ParamBean;
import org.apache.dubbo.apidocs.core.providers.DubboDocProviderImpl;
import org.apache.dubbo.apidocs.core.providers.IDubboDocProvider;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.ProviderConfig;
import org.apache.dubbo.config.annotation.DubboService;
import org.apache.dubbo.config.annotation.Service;
import org.apache.dubbo.apidocs.annotations.ApiModule;
import org.apache.dubbo.apidocs.annotations.ApiDoc;
import org.apache.dubbo.apidocs.annotations.RequestParam;
import org.apache.dubbo.apidocs.utils.ClassTypeUtil;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Import;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.dubbo.apidocs.core.Constants.DOT;
import static org.apache.dubbo.apidocs.core.Constants.METHOD_PARAM_INDEX_BOUNDARY_LEFT;
import static org.apache.dubbo.apidocs.core.Constants.METHOD_PARAM_INDEX_BOUNDARY_RIGHT;
import static org.apache.dubbo.apidocs.core.Constants.METHOD_PARAMETER_SEPARATOR;
import static org.apache.dubbo.apidocs.core.Constants.SKIP_FIELD_SERIALVERSIONUID;
import static org.apache.dubbo.apidocs.core.Constants.SKIP_FIELD_THIS$0;
import static org.apache.dubbo.apidocs.core.Constants.ALLOWABLE_BOOLEAN_TRUE;
import static org.apache.dubbo.apidocs.core.Constants.ALLOWABLE_BOOLEAN_FALSE;
import static org.apache.dubbo.apidocs.core.Constants.METHOD_NAME_NAME;

/**
 * Scan and process dubbo doc annotations.
 */
@Import({DubboDocProviderImpl.class})
public class DubboApiDocsAnnotationScanner implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(DubboApiDocsAnnotationScanner.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ApplicationConfig application;

    @Autowired
    private RegistryConfig registry;

    @Autowired
    private ProtocolConfig protocol;

    @Autowired(required = false)
    private ProviderConfig providerConfig;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        // Register dubbo doc provider
        IDubboDocProvider dubboDocProvider = applicationContext.getBean(IDubboDocProvider.class);
        exportDubboService(IDubboDocProvider.class, dubboDocProvider, false);

        LOG.info("================= Dubbo API Docs--Start scanning and processing doc annotations ================");

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
                LOG.warn("【Warning】" + apiModuleClass.getName() + " @ApiModule annotation is used, " +
                        "but it is not a dubbo provider (without " + Service.class.getName() + " or " +
                        DubboService.class.getName() + " annotation)");
                return;
            }
            boolean async;
            String apiVersion;
            String apiGroup;
            if (apiModuleClass.isAnnotationPresent(Service.class)) {
                Service dubboService = apiModuleClass.getAnnotation(Service.class);
                async = dubboService.async();
                apiVersion = dubboService.version();
                apiGroup = dubboService.group();
            } else {
                DubboService dubboService = apiModuleClass.getAnnotation(DubboService.class);
                async = dubboService.async();
                apiVersion = dubboService.version();
                apiGroup = dubboService.group();
            }


            // API version&group safe guard!
            apiVersion = getApiVersionIfAbsent(apiVersion);
            apiGroup = getApiGroupIfAbsent(apiGroup);

            apiVersion = applicationContext.getEnvironment().resolvePlaceholders(apiVersion);
            apiGroup = applicationContext.getEnvironment().resolvePlaceholders(apiGroup);

            ModuleCacheItem moduleCacheItem = new ModuleCacheItem();
            DubboApiDocsCache.addApiModule(moduleAnn.apiInterface().getCanonicalName(), moduleCacheItem);
            //module name
            moduleCacheItem.setModuleDocName(moduleAnn.value());
            //interface name containing package path
            moduleCacheItem.setModuleClassName(moduleAnn.apiInterface().getCanonicalName());
            //module version
            moduleCacheItem.setModuleVersion(apiVersion);
            //module group
            moduleCacheItem.setModuleGroup(apiGroup);

            Method[] apiModuleMethods = apiModuleClass.getMethods();
            // API basic information list in module cache
            List<ApiCacheItem> moduleApiList = new ArrayList<>(apiModuleMethods.length);
            moduleCacheItem.setModuleApiList(moduleApiList);
            for (Method method : apiModuleMethods) {
                if (method.isAnnotationPresent(ApiDoc.class)) {
                    processApiDocAnnotation(method, moduleApiList, moduleAnn, async, moduleCacheItem, apiVersion, apiGroup);
                }
            }
        });
        LOG.info("================= Dubbo API Docs-- doc annotations scanning and processing completed ================");
    }

    /**
     * get provider config(default) api version if param apiVersion is blank
     * @param apiVersion api version
     * @return api version is apiVersion when it isn`t blank, or return provider config(default) version
     */
    private String getApiVersionIfAbsent(String apiVersion) {
        if (StringUtils.isBlank(apiVersion)) {
            if (providerConfig != null) {
                apiVersion = providerConfig.getVersion();
            }

            if (StringUtils.isBlank(apiVersion)) {
                apiVersion = "";
            }
        }
        return apiVersion;
    }

    /**
     * get provider config(default) api group if param apiGroup is blank
     * @param apiGroup api version
     * @return api group is apiGroup when it isn`t blank, or return provider config(default) group
     */
    private String getApiGroupIfAbsent(String apiGroup) {
        if (StringUtils.isBlank(apiGroup)) {
            if (providerConfig != null) {
                apiGroup = providerConfig.getGroup();
            }

            if (StringUtils.isBlank(apiGroup)) {
                apiGroup = "";
            }
        }
        return apiGroup;
    }

    private void processApiDocAnnotation(Method method, List<ApiCacheItem> moduleApiList, ApiModule moduleAnn,
                                         boolean async, ModuleCacheItem moduleCacheItem, String apiVersion, String apiGroup) {

        ApiDoc dubboApi = method.getAnnotation(ApiDoc.class);

        // API basic information in API list in module
        ApiCacheItem apiListItem = new ApiCacheItem();
        moduleApiList.add(apiListItem);
        //API method name
        apiListItem.setApiName(method.getName());
        //API name
        apiListItem.setApiDocName(dubboApi.value());
        // API description
        apiListItem.setDescription(dubboApi.description());
        //API version
        apiListItem.setApiVersion(apiVersion);
        //API group
        apiListItem.setApiGroup(apiGroup);
        //Description of API return data
        apiListItem.setApiRespDec(dubboApi.responseClassDescription());

        // API details in cache, contain interface parameters and response information
        ApiCacheItem apiParamsAndResp = new ApiCacheItem();
        DubboApiDocsCache.addApiParamsAndResp(
                moduleAnn.apiInterface().getCanonicalName() + DOT + method.getName(), apiParamsAndResp);

        Class<?>[] argsClass = method.getParameterTypes();
        Annotation[][] argsAnns = method.getParameterAnnotations();
        Parameter[] parameters = method.getParameters();
        Type[] parametersTypes = method.getGenericParameterTypes();
        List<ApiParamsCacheItem> paramList = new ArrayList<>(argsClass.length);
        apiParamsAndResp.setAsync(async);
        apiParamsAndResp.setApiName(method.getName());
        apiParamsAndResp.setApiDocName(dubboApi.value());
        apiParamsAndResp.setApiVersion(apiVersion);
        apiParamsAndResp.setApiGroup(apiGroup);
        apiParamsAndResp.setApiRespDec(dubboApi.responseClassDescription());
        apiParamsAndResp.setDescription(dubboApi.description());
        apiParamsAndResp.setApiModelClass(moduleCacheItem.getModuleClassName());
        apiParamsAndResp.setParams(paramList);
        apiParamsAndResp.setResponse(ClassTypeUtil.calss2Json(method.getGenericReturnType(), method.getReturnType()));
        StringBuilder methodParamInfoSb = new StringBuilder();
        for (int i = 0; i < argsClass.length; i++) {
            Class<?> argClass = argsClass[i];
            Type parameterType = parametersTypes[i];
            methodParamInfoSb.append(METHOD_PARAM_INDEX_BOUNDARY_LEFT).append(i)
                    .append(METHOD_PARAM_INDEX_BOUNDARY_RIGHT).append(argClass.getCanonicalName());
            if (i + 1 < argsClass.length) {
                methodParamInfoSb.append(METHOD_PARAMETER_SEPARATOR);
            }
            Annotation[] argAnns = argsAnns[i];
            ApiParamsCacheItem paramListItem = new ApiParamsCacheItem();
            paramList.add(paramListItem);
            paramListItem.setParamType(argClass.getCanonicalName());
            paramListItem.setParamIndex(i);
            RequestParam requestParam = null;
            // Handling @RequestParam annotations on parameters
            for (Annotation ann : argAnns) {
                if (ann instanceof RequestParam) {
                    requestParam = (RequestParam) ann;
                }
            }
            ParamBean paramBean = this.processHtmlType(argClass, requestParam, null);
            Parameter methodParameter = parameters[i];
            if (paramBean == null) {
                // Not a basic type, handling properties in method parameters
                List<ParamBean> apiParamsList = processField(argClass, parameterType, methodParameter);
                if (apiParamsList != null && !apiParamsList.isEmpty()) {
                    paramListItem.setParamInfo(apiParamsList);
                }
            } else {
                // Is the basic type
                paramListItem.setName(methodParameter.getName());
                paramListItem.setHtmlType(paramBean.getHtmlType().name());
                paramListItem.setAllowableValues(paramBean.getAllowableValues());
                if (requestParam != null) {
                    // Handling requestparam annotations on parameters
                    paramListItem.setDocName(requestParam.value());
                    paramListItem.setDescription(requestParam.description());
                    paramListItem.setExample(requestParam.example());
                    paramListItem.setDefaultValue(requestParam.defaultValue());
                    paramListItem.setRequired(requestParam.required());
                } else {
                    paramListItem.setRequired(false);
                }

                if (HtmlTypeEnum.TEXT_AREA.name().equals(paramListItem.getHtmlType())) {
                    List<ParamBean> apiParamsList = processField(argClass, parameterType, methodParameter);
                    paramListItem.setAllowableValues(apiParamsList.get(0).getAllowableValues());
                    paramListItem.setSubParamsJson(apiParamsList.get(0).getSubParamsJson());
                }
            }
        }
        apiParamsAndResp.setMethodParamInfo(methodParamInfoSb.toString());
    }

    /**
     * For the attributes in the method parameters, only one layer is processed.
     * The deeper layer is directly converted to JSON, and the deeper layer is up to 5 layers
     */
    private List<ParamBean> processField(Class<?> argClass, Type parameterType, Parameter parameter) {
        Map<String, String> genericTypeAndNamesMap;
        if (parameterType instanceof ParameterizedTypeImpl) {
            ParameterizedTypeImpl parameterTypeImpl = (ParameterizedTypeImpl) parameterType;
            TypeVariable<? extends Class<?>>[] typeVariables = parameterTypeImpl.getRawType().getTypeParameters();
            Type[] actualTypeArguments = parameterTypeImpl.getActualTypeArguments();
            genericTypeAndNamesMap = new HashMap<>(typeVariables.length);
            for (int i = 0; i < typeVariables.length; i++) {
                genericTypeAndNamesMap.put(typeVariables[i].getTypeName(), actualTypeArguments[i].getTypeName());
            }
        } else {
            genericTypeAndNamesMap = Collections.EMPTY_MAP;
        }

        List<ParamBean> apiParamsList = new ArrayList<>(16);
        // get all fields
        List<Field> allFields = ClassTypeUtil.getAllFields(null, argClass);
        if (allFields.size() > 0) {
            for (Field field : allFields) {
                if (SKIP_FIELD_SERIALVERSIONUID.equals(field.getName()) || SKIP_FIELD_THIS$0.equals(field.getName())) {
                    continue;
                }
                ParamBean paramBean = new ParamBean();
                paramBean.setName(field.getName());
                String genericTypeName = genericTypeAndNamesMap.get(field.getGenericType().getTypeName());
                Class<?> genericType = null;
                if (StringUtils.isBlank(genericTypeName)) {
                    paramBean.setJavaType(field.getType().getCanonicalName());
                } else {
                    paramBean.setJavaType(genericTypeName);
                    genericType = ClassTypeUtil.makeClass(genericTypeName);
                }
                RequestParam requestParam = null;
                if (field.isAnnotationPresent(RequestParam.class)) {
                    // Handling @RequestParam annotations on properties
                    requestParam = field.getAnnotation(RequestParam.class);
                    paramBean.setDocName(requestParam.value());
                    paramBean.setRequired(requestParam.required());
                    paramBean.setDescription(requestParam.description());
                    paramBean.setExample(requestParam.example());
                    paramBean.setDefaultValue(requestParam.defaultValue());
                } else {
                    paramBean.setRequired(false);
                }

                ParamBean tempParamBean = this.processHtmlType(null == genericType ?
                        field.getType() : genericType, requestParam, paramBean);
                if (tempParamBean == null || HtmlTypeEnum.TEXT_AREA.equals(tempParamBean.getHtmlType())) {
                    Object objResult;
                    if (null == genericType) {
                        objResult = ClassTypeUtil.initClassTypeWithDefaultValue(
                                field.getGenericType(), field.getType(), 0, genericTypeAndNamesMap);
                    } else {
                        objResult = ClassTypeUtil.initClassTypeWithDefaultValue(
                                ClassTypeUtil.makeParameterizedType(genericTypeName), genericType, 0,
                                true, genericTypeAndNamesMap);
                    }
                    if (!ClassTypeUtil.isBaseType(objResult)) {
                        paramBean.setHtmlType(HtmlTypeEnum.TEXT_AREA);
                        paramBean.setSubParamsJson(JSON.toJSONString(objResult, ClassTypeUtil.FAST_JSON_FEATURES));
                    }
                }
                apiParamsList.add(paramBean);
            }
        } else {
            ParamBean paramBean = new ParamBean();
            paramBean.setName(parameter.getName());
            paramBean.setJavaType(argClass.getCanonicalName());
            RequestParam requestParam = null;
            if (parameter.isAnnotationPresent(RequestParam.class)) {
                // Handling @RequestParam annotations on properties
                requestParam = parameter.getAnnotation(RequestParam.class);
                paramBean.setDocName(requestParam.value());
                paramBean.setRequired(requestParam.required());
                paramBean.setDescription(requestParam.description());
                paramBean.setExample(requestParam.example());
                paramBean.setDefaultValue(requestParam.defaultValue());
            } else {
                paramBean.setRequired(false);
            }

            Object objResult = ClassTypeUtil.initClassTypeWithDefaultValue(
                    parameterType, argClass, 0, genericTypeAndNamesMap);
            if (!ClassTypeUtil.isBaseType(objResult)) {
                paramBean.setHtmlType(HtmlTypeEnum.TEXT_AREA);
                paramBean.setSubParamsJson(JSON.toJSONString(objResult, ClassTypeUtil.FAST_JSON_FEATURES));
            }
            apiParamsList.add(paramBean);
        }
        return apiParamsList;
    }

    /**
     * Determine what HTML form elements to use.
     *
     * @param classType  classType
     * @param annotation annotation
     * @param param      param
     * @return org.apache.dubbo.apidocs.core.beans.ParamBean
     */
    private ParamBean processHtmlType(Class<?> classType, RequestParam annotation, ParamBean param) {
        if (param == null) {
            param = new ParamBean();
        }
        if (annotation != null) {
            param.setAllowableValues(annotation.allowableValues());
        }
        // Is there any allowed values
        boolean hasAllowableValues = (param.getAllowableValues() != null && param.getAllowableValues().length > 0);
        // Processed or not
        boolean processed = false;
        if (Integer.class.isAssignableFrom(classType) || int.class.isAssignableFrom(classType)) {
            param.setHtmlType(HtmlTypeEnum.NUMBER_INTEGER);
            processed = true;
        } else if (Byte.class.isAssignableFrom(classType) || byte.class.isAssignableFrom(classType)) {
            param.setHtmlType(HtmlTypeEnum.TEXT_BYTE);
            processed = true;
        } else if (Long.class.isAssignableFrom(classType) || long.class.isAssignableFrom(classType) ||
                BigDecimal.class.isAssignableFrom(classType) || BigInteger.class.isAssignableFrom(classType)) {
            param.setHtmlType(HtmlTypeEnum.NUMBER_INTEGER);
            processed = true;
        } else if (Double.class.isAssignableFrom(classType) || double.class.isAssignableFrom(classType)) {
            param.setHtmlType(HtmlTypeEnum.NUMBER_DECIMAL);
            processed = true;
        } else if (Float.class.isAssignableFrom(classType) || float.class.isAssignableFrom(classType)) {
            param.setHtmlType(HtmlTypeEnum.NUMBER_DECIMAL);
            processed = true;
        } else if (String.class.isAssignableFrom(classType)) {
            param.setHtmlType(HtmlTypeEnum.TEXT);
            processed = true;
        } else if (Character.class.isAssignableFrom(classType) || char.class.isAssignableFrom(classType)) {
            param.setHtmlType(HtmlTypeEnum.TEXT_CHAR);
            processed = true;
        } else if (Short.class.isAssignableFrom(classType) || short.class.isAssignableFrom(classType)) {
            param.setHtmlType(HtmlTypeEnum.NUMBER_INTEGER);
            processed = true;
        } else if (Date.class.isAssignableFrom(classType) || LocalDateTime.class.isAssignableFrom(classType)) {
            param.setHtmlType(HtmlTypeEnum.DATETIME_SELECTOR);
            processed = true;
        } else if (LocalDate.class.isAssignableFrom(classType)) {
            param.setHtmlType(HtmlTypeEnum.DATE_SELECTOR);
            processed = true;
        } else if (classType.isArray() || Collection.class.isAssignableFrom(classType) ||
                Map.class.isAssignableFrom(classType)) {
            param.setHtmlType(HtmlTypeEnum.TEXT_AREA);
            processed = true;
        }

        if (processed) {
            // Processed, time to return
            if (hasAllowableValues) {
                // Allowed values has value, change to select
                param.setHtmlType(HtmlTypeEnum.SELECT);
            }
            return param;
        }

        // haven't dealt with it. Go on
        if (Boolean.class.isAssignableFrom(classType) || boolean.class.isAssignableFrom(classType)) {
            param.setHtmlType(HtmlTypeEnum.SELECT);
            // Boolean can only be true / false. No matter what the previous allowed value is, it is forced to replace
            param.setAllowableValues(new String[]{ALLOWABLE_BOOLEAN_TRUE, ALLOWABLE_BOOLEAN_FALSE});
            processed = true;
        } else if (Enum.class.isAssignableFrom(classType)) {
            // process enum
            param.setHtmlType(HtmlTypeEnum.SELECT);

            Object[] enumConstants = classType.getEnumConstants();
            String[] enumValues = new String[enumConstants.length];
            try {
                Method getNameMethod = classType.getMethod(METHOD_NAME_NAME);
                for (int i = 0; i < enumConstants.length; i++) {
                    Object obj = enumConstants[i];
                    enumValues[i] = (String) getNameMethod.invoke(obj);
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                LOG.error(e.getMessage(), e);
            }

            if (!hasAllowableValues) {
                // If there is no optional value, it is taken from the enumeration.
                param.setAllowableValues(enumValues);
            } else {
                // If there has allowable values, it is necessary to check whether the allowable values matches the enumeration.
                boolean checkSuccess = true;
                String[] allowableValues = param.getAllowableValues();
                for (String allowableValue : allowableValues) {
                    for (String enumValue : enumValues) {
                        if (!StringUtils.equals(enumValue, allowableValue)) {
                            checkSuccess = false;
                        }
                    }
                }
                if (!checkSuccess) {
                    LOG.error("The allowed value in the @RequestParam annotation does not match the " +
                            "annotated enumeration " + classType.getCanonicalName() + ", please check!");
                }
            }
            processed = true;
        }
        if (processed) {
            return param;
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
        service.setGroup("apiDocsGroup");
        service.setVersion("v1");
        service.export();
    }

}
