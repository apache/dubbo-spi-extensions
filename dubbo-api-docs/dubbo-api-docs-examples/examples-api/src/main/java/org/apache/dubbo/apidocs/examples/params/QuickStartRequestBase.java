package org.apache.dubbo.apidocs.examples.params;

import org.apache.dubbo.apidocs.annotations.RequestParam;

/**
 * QuickStartRequestBase.
 *
 * @date 2021/1/26 15:24
 */
public class QuickStartRequestBase<E, T> {

    @RequestParam(value = "Request method", required = true)
    private String method;

    private T body;

    private E body3;

    private QuickStartRequestBean body2;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public T getBody() {
        return body;
    }

    public void setBody(T body) {
        this.body = body;
    }

    public QuickStartRequestBean getBody2() {
        return body2;
    }

    public void setBody2(QuickStartRequestBean body2) {
        this.body2 = body2;
    }

    public E getBody3() {
        return body3;
    }

    public void setBody3(E body3) {
        this.body3 = body3;
    }

}
