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


import org.apache.dubbo.metadata.extension.rest.api.AbstractNoAnnotatedParameterProcessor;
import org.apache.dubbo.metadata.extension.rest.api.RestMethodMetadata;
import org.apache.dubbo.metadata.extension.rest.api.jaxrs.JAXRSServiceRestMetadataResolver;
import org.apache.dubbo.metadata.extension.rest.api.media.MediaType;
import org.apache.dubbo.metadata.extension.rest.api.tag.BodyTag;
import org.apache.dubbo.metadata.extension.rest.api.tag.ParamTag;

public class ParamNoAnnotatedProcessor extends AbstractNoAnnotatedParameterProcessor {
    @Override
    public MediaType consumerContentType() {
        return MediaType.ALL_VALUE;
    }

    @Override
    public String defaultAnnotationClassName(RestMethodMetadata restMethodMetadata) {

        if (JAXRSServiceRestMetadataResolver.class.equals(restMethodMetadata.getCodeStyle())) {
            return BodyTag.class.getName();
        }

        return ParamTag.class.getName();
    }
}
