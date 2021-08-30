package org.apache.dubbo.mock.handler;

/**
 * @author chenglu
 * @date 2021-08-30 19:19
 */
public interface TypeHandler<T> {

    boolean isMatch(ResultContext resultContext);

    T handleResult(ResultContext resultContext);
}
