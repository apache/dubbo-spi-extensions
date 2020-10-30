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
package org.apache.dubbo.apidocs.core.beans;

/**
 * html type enum.
 * @author klw(213539@qq.com)
 * 2020/10/29 16:56
 */
public enum HtmlTypeEnum {

    /**
     * Textbox.
     */
    TEXT,

    /**
     * Textbox, This type will be converted to byte before calling dubbo API.
     */
    TEXT_BYTE,

    /**
     * Textbox, will be limited to one character. This type will be converted to char before calling dubbo API.
     */
    TEXT_CHAR,

    /**
     * Numeric input box, integer.
     */
    NUMBER_INTEGER,

    /**
     * Numeric input box, decimal.
     */
    NUMBER_DECIMAL,

    /**
     * Drop down selection box.
     */
    SELECT,

    /**
     * Text area, which is generally used to show the JSON string of the Java Bean contained in the parameter.
     */
    TEXT_AREA,
    ;

}
