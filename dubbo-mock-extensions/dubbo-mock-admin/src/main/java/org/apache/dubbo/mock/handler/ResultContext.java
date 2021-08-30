package org.apache.dubbo.mock.handler;

/**
 * @author chenglu
 * @date 2021-08-30 19:23
 */
public class ResultContext {

    private Class<?> targetType;

    private String data;

    private ResultContext(Builder builder) {
        this.targetType = builder.targetType;
        this.data = builder.data;
    }

    public static Builder newResultContext() {
        return new Builder();
    }

    public Class<?> getTargetType() {
        return targetType;
    }

    public void setTargetType(Class<?> targetType) {
        this.targetType = targetType;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public static final class Builder {
        private Class<?> targetType;
        private String data;

        private Builder() {
        }

        public ResultContext build() {
            return new ResultContext(this);
        }

        public Builder targetType(Class<?> targetType) {
            this.targetType = targetType;
            return this;
        }

        public Builder data(String data) {
            this.data = data;
            return this;
        }
    }
}
