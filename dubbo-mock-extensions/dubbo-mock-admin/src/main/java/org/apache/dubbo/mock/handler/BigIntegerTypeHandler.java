package org.apache.dubbo.mock.handler;

import java.math.BigInteger;
import java.util.Objects;

/**
 * @author chenglu
 * @date 2021-08-30 19:28
 */
public class BigIntegerTypeHandler implements TypeHandler<BigInteger> {

    @Override
    public boolean isMatch(ResultContext resultContext) {
        return Objects.equals(BigInteger.class, resultContext.getTargetType());
    }

    @Override
    public BigInteger handleResult(ResultContext resultContext) {
        return new BigInteger(resultContext.getData());
    }
}
