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

import java.math.BigDecimal;
import java.util.Objects;

/**
 * @author chenglu
 * @date 2021-08-30 19:44
 */
public class BigDecimalTypeHandler implements TypeHandler<BigDecimal> {

    @Override
    public boolean isMatch(ResultContext resultContext) {
        return Objects.equals(BigDecimal.class, resultContext.getTargetType());
    }

    @Override
    public BigDecimal handleResult(ResultContext resultContext) {
        return new BigDecimal(resultContext.getData());
    }
}
