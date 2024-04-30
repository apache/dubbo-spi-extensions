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
package org.apache.dubbo.metadata.extension.rest.api.springmvc;

import org.apache.dubbo.metadata.extension.rest.api.AbstractAnnotatedMethodParameterProcessor;
import org.apache.dubbo.metadata.extension.rest.api.AnnotatedMethodParameterProcessor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;

import static org.apache.dubbo.metadata.extension.rest.api.RestMetadataConstants.SPRING_MVC.REQUEST_BODY_ANNOTATION_CLASS_NAME;

/**
 * The {@link AnnotatedMethodParameterProcessor} implementation for Spring Web MVC's @RequestBody
 */
public class RequestBodyParameterProcessor extends AbstractAnnotatedMethodParameterProcessor {

    @Override
    public String getAnnotationName() {
        return REQUEST_BODY_ANNOTATION_CLASS_NAME;
    }

    @Override
    protected String getAnnotationValue(Annotation annotation, Parameter parameter, int parameterIndex) {
        return null;
    }

    @Override
    protected String getDefaultValue(Annotation annotation, Parameter parameter, int parameterIndex) {
        return null;
    }
}
