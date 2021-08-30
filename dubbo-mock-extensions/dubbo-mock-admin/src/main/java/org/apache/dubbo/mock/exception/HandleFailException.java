package org.apache.dubbo.mock.exception;

/**
 * @author chenglu
 * @date 2021-08-30 17:21
 */
public class HandleFailException extends RuntimeException {

    public HandleFailException() {
        super();
    }

    public HandleFailException(String msg) {
        super(msg);
    }

    public HandleFailException(Throwable throwable) {
        super(throwable);
    }
}
