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

package org.apache.dubbo.gateway.common;

public interface OmnipotentCommonConstants {

    //save origin group when service is omn
    String ORIGIN_GROUP_KEY = "originGroup";

    String ORIGIN_GENERIC_PARAMETER_TYPES = "originGenericParameterTypes";

    String ORIGIN_PARAMETER_TYPES_DESC = "originParameterTypesDesc";

    String $INVOKE_OMN = "$invokeOmn";

    String ORIGIN_PATH_KEY = "originPath";

    String ORIGIN_METHOD_KEY = "originMethod";

    String ORIGIN_VERSION_KEY = "originVersion";

    String SPECIFY_ADDRESS = "specifyAddress";
    String GATEWAY_MODE = "gatewayMode";

}
