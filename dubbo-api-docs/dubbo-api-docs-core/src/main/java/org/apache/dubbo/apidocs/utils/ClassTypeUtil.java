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
package org.apache.dubbo.apidocs.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import org.apache.dubbo.apidocs.annotations.*;

/**
 * Java class tool class, special for Dubbo doc.
 *
 * @author klw(213539 @ qq.com)
 * 2020/10/29 18:08
 */
@Slf4j
public class ClassTypeUtil {

    /**
     * fastjson features
     */
    public static SerializerFeature[] FAST_JSON_FEATURES = {
            //Whether to output the field with null value. The default value is false.
            SerializerFeature.WriteMapNullValue,
            //If the list field is null, the output is [], not null
            SerializerFeature.WriteNullListAsEmpty,
            //If the character type field is null, the output is' ', not null
            SerializerFeature.WriteNullStringAsEmpty,
            //If the Boolean field is null, the output is false instead of null
            SerializerFeature.WriteNullBooleanAsFalse,
            // Null number output 0
            SerializerFeature.WriteNullNumberAsZero,
            //Eliminate the problem of circular reference to the same object.
            // The default value is false (it may enter a dead cycle if not configured)
            SerializerFeature.DisableCircularReferenceDetect,
            // Use. Name() to handle enumeration
            SerializerFeature.WriteEnumUsingName
    };

    private static final int PROCESS_COUNT_MAX = 10;

    private static final String GENERIC_START_SYMBOL = "<";

    public static String calss2Json(Type genericType, Class<?> classType) {
        Object obj = initClassTypeWithDefaultValue(genericType, classType, 0);
        return JSON.toJSONString(obj, FAST_JSON_FEATURES);
    }

    /**
     * Instantiate class and its fields.
     * 2020/10/29 18:08
     *
     * @param genericType  genericType
     * @param classType    classType
     * @param processCount processCount
     * @return java.lang.Object
     */
    public static Object initClassTypeWithDefaultValue(Type genericType, Class<?> classType, int processCount) {
        if (processCount >= PROCESS_COUNT_MAX) {
            log.warn("The depth of bean has exceeded 10 layers, the deeper layer will be ignored! " +
                    "Please modify the parameter structure or check whether there is circular reference in bean!");
            return null;
        }
        processCount++;

        Object initResult = initClassTypeWithDefaultValueNoProceeField(genericType, classType, processCount);
        if (null != initResult) {
            return initResult;
        }

        Map<String, Object> result = new HashMap<>(16);
        // get all fields
        List<Field> allFields = getAllFields(null, classType);
        for (Field field2 : allFields) {
            if ("serialVersionUID".equals(field2.getName())) {
                continue;
            }
            if (String.class.isAssignableFrom(field2.getType())) {
                if (field2.isAnnotationPresent(RequestParam.class)) {
                    RequestParam requestParam = field2.getAnnotation(RequestParam.class);
                    result.put(field2.getName(), requestParam.value());
                } else if (field2.isAnnotationPresent(ResponseProperty.class)) {
                    ResponseProperty responseProperty = field2.getAnnotation(ResponseProperty.class);
                    StringBuilder strValue = new StringBuilder(responseProperty.value());
                    if (StringUtils.isNotBlank(responseProperty.example())) {
                        strValue.append("【example: ").append(responseProperty.example()).append("】");
                    }
                    result.put(field2.getName(), strValue.toString());
                } else {
                    // It's string, but there's no annotation
                    result.put(field2.getName(), initClassTypeWithDefaultValue(field2.getGenericType(), field2.getType(), processCount));
                }
            } else {
                // Check if the type of the property is generic
                if ("T".equals(field2.getGenericType().getTypeName())) {
                    // The type of the attribute is generic. Find the generic from the definition of
                    // the class in which the attribute is located
                    ParameterizedType pt = (ParameterizedType) genericType;
                    Type[] actualTypeArguments = pt.getActualTypeArguments();
                    if (actualTypeArguments.length > 0) {
                        if (actualTypeArguments.length == 1) {
                            result.put(field2.getName(), initClassTypeWithDefaultValue(
                                    makeParameterizedType(actualTypeArguments[0].getTypeName()),
                                    makeClass(pt.getActualTypeArguments()[0].getTypeName()), processCount));
                        } else {
                            log.warn("{}#{} generics are not supported temporarily. " +
                                    "This property will be ignored", classType.getName(), field2.getName());
                        }
                    } else {
                        result.put(field2.getName(), initClassTypeWithDefaultValue(field2.getGenericType(), field2.getType(), processCount));
                    }
                } else {
                    // Not generic
                    result.put(field2.getName(), initClassTypeWithDefaultValue(field2.getGenericType(), field2.getType(), processCount));
                }
            }
        }
        return result;
    }

    private static Object initClassTypeWithDefaultValueNoProceeField(Type genericType, Class<?> classType, int processCount) {
        if (Integer.class.isAssignableFrom(classType)) {
            return 0;
        } else if (Byte.class.isAssignableFrom(classType)) {
            return (byte) 0;
        } else if (Long.class.isAssignableFrom(classType)) {
            return 0L;
        } else if (Double.class.isAssignableFrom(classType)) {
            return 0.0D;
        } else if (Float.class.isAssignableFrom(classType)) {
            return 0.0F;
        } else if (String.class.isAssignableFrom(classType)) {
            return "";
        } else if (Character.class.isAssignableFrom(classType)) {
            return 'c';
        } else if (Short.class.isAssignableFrom(classType)) {
            return (short) 0;
        } else if (Boolean.class.isAssignableFrom(classType)) {
            return false;
        } else if (Date.class.isAssignableFrom(classType)) {
            return "【" + Date.class.getName() + "】yyyy-MM-dd HH:mm:ss";
        } else if (LocalDate.class.isAssignableFrom(classType)) {
            return "【" + LocalDate.class.getName() + "】yyyy-MM-dd";
        } else if (LocalDateTime.class.isAssignableFrom(classType)) {
            return "【" + LocalDateTime.class.getName() + "】yyyy-MM-dd HH:mm:ss";
        } else if (Enum.class.isAssignableFrom(classType)) {
            Object[] enumConstants = classType.getEnumConstants();
            StringBuilder sb = new StringBuilder("|");
            try {
                Method getName = classType.getMethod("name");
                for (Object obj : enumConstants) {
                    sb.append(getName.invoke(obj)).append("|");
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                log.error("", e);
            }
            return sb.toString();
        } else if (classType.isArray()) {
            Class<?> arrType = classType.getComponentType();
            Object obj = initClassTypeWithDefaultValue(null, arrType, processCount);
            return new Object[]{obj};
        } else if (Collection.class.isAssignableFrom(classType)) {
            List<Object> list = new ArrayList<>(1);
            if (genericType == null) {
                list.add(new Object());
                return list;
            }
            Object obj;
            if (genericType instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) genericType;
                String subTypeName = pt.getActualTypeArguments()[0].getTypeName();
                obj = initClassTypeWithDefaultValue(makeParameterizedType(subTypeName), makeClass(subTypeName), processCount);
                list.add(obj);
            }
            return list;
        } else if (Map.class.isAssignableFrom(classType)) {
            Map<String, Object> map = new HashMap<>(1);
            if (genericType == null) {
                map.put("", new Object());
                return map;
            }
            if (genericType instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) genericType;
                String subTypeName = pt.getActualTypeArguments()[1].getTypeName();
                Object objValue = initClassTypeWithDefaultValue(makeParameterizedType(subTypeName), makeClass(subTypeName), processCount);
                map.put("", objValue);
            }
            return map;
        } else if (CompletableFuture.class.isAssignableFrom(classType)) {
            // process CompletableFuture
            if (genericType == null) {
                return new Object();
            }
            ParameterizedType pt = (ParameterizedType) genericType;
            String typeName = pt.getActualTypeArguments()[0].getTypeName();
            return initClassTypeWithDefaultValue(makeParameterizedType(typeName), makeClass(typeName), processCount);
        }
        return null;
    }

    /**
     * Check if it is a basic data type.
     * 2020/10/29 18:09
     *
     * @param o
     * @return boolean
     */
    public static boolean isBaseType(Object o) {
        if (o instanceof Integer ||
                o instanceof Byte ||
                o instanceof Long ||
                o instanceof Double ||
                o instanceof Float ||
                o instanceof Character ||
                o instanceof Short ||
                o instanceof Boolean ||
                o instanceof String) {
            return true;
        }
        return false;
    }

    /**
     * Get all fields in the class.
     * 2020/10/29 18:10
     *
     * @param fieldList fieldList
     * @param classz    classz
     * @return java.util.List<java.lang.reflect.Field>
     */
    public static List<Field> getAllFields(List<Field> fieldList, Class<?> classz) {
        if (classz == null) {
            return fieldList;
        }
        if (fieldList == null) {
            fieldList = new ArrayList<>(Arrays.asList(classz.getDeclaredFields()));
        } else {
            fieldList.addAll(Arrays.asList(classz.getDeclaredFields()));
        }
        return getAllFields(fieldList, classz.getSuperclass());
    }

    public static ParameterizedType makeParameterizedType(String typeName) {
        if (typeName.indexOf(GENERIC_START_SYMBOL) == -1) {
            return null;
        }
        try {
            Class<?> typeClass;
            typeClass = Class.forName(typeName.substring(0, typeName.indexOf("<")));
            String subTypeNames = typeName.substring((typeName.indexOf("<") + 1), (typeName.length() - 1));
            String[] subTypeNamesArray = subTypeNames.split(",");
            Type[] subTypes = makeSubClass(subTypeNamesArray);
            return ParameterizedTypeImpl.make(typeClass, subTypes, null);
        } catch (ClassNotFoundException e) {
            log.warn("Exception getting generics in completabilefuture", e);
            return null;
        }
    }

    public static Class<?> makeClass(String className) {
        className = className.trim();
        try {
            if (className.indexOf(GENERIC_START_SYMBOL) == -1) {
                // CompletableFuture 中的类没有泛型
                return Class.forName(className);
            } else {
                return Class.forName(className.substring(0, className.indexOf("<")));
            }
        } catch (ClassNotFoundException e) {
            log.warn("Exception getting generics in completabilefuture", e);
            return null;
        }
    }

    private static Type[] makeSubClass(String... classNames) {
        Type[] types;
        if (classNames != null) {
            types = new Type[classNames.length];
            for (int i = 0; i < classNames.length; i++) {
                String className = classNames[i];
                types[i] = new SimpleTypeImpl(className);
            }
        } else {
            types = new Type[0];
        }
        return types;
    }

}
