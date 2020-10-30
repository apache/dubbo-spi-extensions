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

import org.apache.dubbo.apidocs.utils.ClassTypeUtil;

import com.alibaba.fastjson.JSON;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * dubbo doc cache.
 * @author klw(213539@qq.com)
 * 2020/10/29 17:40
 */
public class DubboApiDocsCache {

    /**
     * module cache.
     */
    private static Map<String, Map<String, Object>> apiModulesCache = new ConcurrentHashMap<>(16);
    /**
     * module cache.
     */
    private static Map<String, String> apiModulesStrCache = new ConcurrentHashMap<>(16);

    /**
     * API details cache in module.
     */
    private static Map<String, Map<String, Object>> apiParamsAndRespCache = new ConcurrentHashMap<>(16);
    /**
     * API details cache in module.
     */
    private static Map<String, String> apiParamsAndRespStrCache = new ConcurrentHashMap<>(16);

    private static String allApiModuleInfo = null;

    public static void addApiModule(String key, Map<String, Object> moduleCacheItem){
        apiModulesCache.put(key, moduleCacheItem);
    }

    public static void addApiParamsAndResp(String key, Map<String, Object> apiParamsAndResp){
        apiParamsAndRespCache.put(key, apiParamsAndResp);
    }

    public static Map<String, Object> getApiModule(String key){
        return apiModulesCache.get(key);
    }

    public static String getApiModuleStr(String key){
        String result = apiModulesStrCache.get(key);
        if(result == null){
            Map<String, Object> temp = apiModulesCache.get(key);
            if(temp != null) {
                result = JSON.toJSONString(temp, ClassTypeUtil.FAST_JSON_FEATURES);
                apiModulesStrCache.put(key, result);
            }
        }
        return result;
    }

    public static Map<String, Object> getApiParamsAndResp(String key){
        return apiParamsAndRespCache.get(key);
    }

    public static String getApiParamsAndRespStr(String key){
        String result = apiParamsAndRespStrCache.get(key);
        if(result == null){
            Map<String, Object> temp = apiParamsAndRespCache.get(key);
            if(temp != null) {
                result = JSON.toJSONString(temp, ClassTypeUtil.FAST_JSON_FEATURES);
                apiParamsAndRespStrCache.put(key, result);
            }
        }
        return result;
    }

    public static String getAllApiModuleInfo(){
        if(allApiModuleInfo == null){
            List<Map<String, Object>> tempList = new ArrayList<>(apiModulesCache.size());
            apiModulesCache.forEach((k, v) -> {
                tempList.add(v);
            });
            allApiModuleInfo = JSON.toJSONString(tempList, ClassTypeUtil.FAST_JSON_FEATURES);
        }
        return allApiModuleInfo;
    }

}
