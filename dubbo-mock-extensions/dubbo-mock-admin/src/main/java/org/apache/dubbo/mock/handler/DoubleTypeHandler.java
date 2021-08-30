package org.apache.dubbo.mock.handler;

import java.util.Objects;

/**
 * @author chenglu
 * @date 2021-08-30 19:21
 */
public class DoubleTypeHandler implements TypeHandler<Double> {

    @Override
    public boolean isMatch(ResultContext resultContext) {
        return Objects.equals(Double.class, resultContext.getTargetType())
            || Objects.equals(double.class, resultContext.getTargetType());
    }

    @Override
    public Double handleResult(ResultContext resultContext) {
        return Double.parseDouble(resultContext.getData());
    }
}
