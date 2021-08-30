package org.apache.dubbo.mock.handler;

import java.util.Objects;

/**
 * @author chenglu
 * @date 2021-08-30 19:43
 */
public class StringTypeHandler implements TypeHandler<String> {

    @Override
    public boolean isMatch(ResultContext resultContext) {
        return Objects.equals(String.class, resultContext.getTargetType());
    }

    @Override
    public String handleResult(ResultContext resultContext) {
        return resultContext.getData();
    }
}
