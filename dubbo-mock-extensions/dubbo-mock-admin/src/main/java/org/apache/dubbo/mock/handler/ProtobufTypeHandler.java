package org.apache.dubbo.mock.handler;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.mock.exception.HandleFailException;

import com.google.protobuf.Message;
import com.googlecode.protobuf.format.JsonFormat;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * @author chenglu
 * @date 2021-08-30 19:53
 */
public class ProtobufTypeHandler implements TypeHandler<Object> {

    private static final Logger logger = LoggerFactory.getLogger(ProtobufTypeHandler.class);

    @Override
    public boolean isMatch(ResultContext resultContext) {
        Class<?> superType = resultContext.getTargetType().getSuperclass();
        if (Objects.isNull(superType)) {
            return false;
        }
        String superTypeName = superType.getName();
        return Objects.equals(superTypeName, "com.google.protobuf.GeneratedMessageV3")
            || Objects.equals(superTypeName, "com.google.protobuf.GeneratedMessage");
    }

    @Override
    public Object handleResult(ResultContext resultContext) {
        try {
            Method buildMethod = resultContext.getTargetType().getMethod("newBuilder");
            Message.Builder message = (Message.Builder) buildMethod.invoke(null);
            JsonFormat.merge(resultContext.getData(), message);
            return message.build();
        } catch (Exception e) {
            logger.warn("[Dubbo Mock] handle protobuf object failed", e);
            throw new HandleFailException(e);
        }
    }
}
