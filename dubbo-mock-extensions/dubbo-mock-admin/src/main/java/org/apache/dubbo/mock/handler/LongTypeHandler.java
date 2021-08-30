package org.apache.dubbo.mock.handler;

import java.util.Objects;

/**
 * @author chenglu
 * @date 2021-08-30 19:22
 */
public class LongTypeHandler implements TypeHandler<Long> {

    @Override
    public boolean isMatch(ResultContext resultContext) {
        return Objects.equals(Long.class, resultContext.getTargetType())
            || Objects.equals(long.class, resultContext.getTargetType());
    }

    @Override
    public Long handleResult(ResultContext resultContext) {
        return Long.parseLong(resultContext.getData());
    }
}
