package org.apache.dubbo.mock.handler;

import java.util.Objects;

/**
 * @author chenglu
 * @date 2021-08-30 19:23
 */
public class FloatTypeHandler implements TypeHandler<Float> {

    @Override
    public boolean isMatch(ResultContext resultContext) {
        return Objects.equals(Float.class, resultContext.getTargetType())
            || Objects.equals(float.class, resultContext.getTargetType());
    }

    @Override
    public Float handleResult(ResultContext resultContext) {
        return Float.parseFloat(resultContext.getData());
    }
}
