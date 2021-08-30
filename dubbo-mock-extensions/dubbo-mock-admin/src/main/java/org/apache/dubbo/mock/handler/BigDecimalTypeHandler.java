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
