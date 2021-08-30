package org.apache.dubbo.mock.handler;

import java.util.Objects;

/**
 * @author chenglu
 * @date 2021-08-30 19:24
 */
public class ByteTypeHandler implements TypeHandler<Byte> {

    @Override
    public boolean isMatch(ResultContext resultContext) {
        return Objects.equals(Byte.class, resultContext.getTargetType())
            || Objects.equals(byte.class, resultContext.getTargetType());
    }

    @Override
    public Byte handleResult(ResultContext resultContext) {
        return Byte.valueOf(resultContext.getData());
    }
}
