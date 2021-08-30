package org.apache.dubbo.mock.handler;

import java.util.Objects;

/**
 * @author chenglu
 * @date 2021-08-30 19:19
 */
public class BooleanTypeHandler implements TypeHandler<Boolean> {

    @Override
    public boolean isMatch(ResultContext resultContext) {
        return Objects.equals(Boolean.class, resultContext.getTargetType())
            || Objects.equals(boolean.class, resultContext.getTargetType());
    }

    @Override
    public Boolean handleResult(ResultContext resultContext) {
        return Boolean.getBoolean(resultContext.getData());
    }
}
