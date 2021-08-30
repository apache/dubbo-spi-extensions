package org.apache.dubbo.mock.handler;

import java.util.Objects;

/**
 * @author chenglu
 * @date 2021-08-30 19:27
 */
public class IntegerTypeHandler implements TypeHandler<Integer> {

    @Override
    public boolean isMatch(ResultContext resultContext) {
        Class<?> targetType = resultContext.getTargetType();
        return Objects.equals(targetType, Integer.class) || Objects.equals(targetType, int.class);
    }

    @Override
    public Integer handleResult(ResultContext resultContext) {
        return Integer.parseInt(resultContext.getData());
    }
}
