package org.apache.dubbo.mock.handler;

/**
 * @author chenglu
 * @date 2021-08-30 19:25
 */
public class UnknownTypeHandler implements TypeHandler<Object> {

    @Override
    public boolean isMatch(ResultContext resultContext) {
        return false;
    }

    @Override
    public Object handleResult(ResultContext resultContext) {
        return null;
    }
}
