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

package org.apache.dubbo.mock.handler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

/**
 * handle the {@link Date} type result.
 */
public class DateTypeHandler implements TypeHandler<Date> {

    @Override
    public boolean isMatch(ResultContext resultContext) {
        return Objects.equals(Date.class, resultContext.getTargetType());
    }

    @Override
    public Date handleResult(ResultContext resultContext) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return dateFormat.parse(resultContext.getData());
        } catch (Exception e) {
            return new Date(Long.parseLong(resultContext.getData()));
        }
    }
}
