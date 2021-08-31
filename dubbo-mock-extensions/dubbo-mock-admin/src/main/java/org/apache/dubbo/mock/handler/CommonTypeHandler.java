package org.apache.dubbo.mock.handler;

import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author chenglu
 * @date 2021-08-30 19:25
 */
public class CommonTypeHandler implements TypeHandler<Object> {

    private static final Logger logger = LoggerFactory.getLogger(CommonTypeHandler.class);

    private List<TypeHandler> typeHandlers;

    private UnknownTypeHandler unknownTypeHandler;

    private JsonTypeHandler jsonTypeHandler;

    public CommonTypeHandler() {
        unknownTypeHandler = new UnknownTypeHandler();
        jsonTypeHandler = new JsonTypeHandler();
        typeHandlers = Arrays.asList(new StringTypeHandler(), new IntegerTypeHandler(), new LongTypeHandler(),
            new BigDecimalTypeHandler(), new ProtobufTypeHandler(), new DateTypeHandler(), new BooleanTypeHandler(),
            new ByteTypeHandler(), new DoubleTypeHandler(), new FloatTypeHandler(), new BigIntegerTypeHandler());
    }

    @Override
    public boolean isMatch(ResultContext resultContext) {
        return true;
    }

    @Override
    public Object handleResult(ResultContext resultContext) {
        if (Objects.isNull(resultContext.getData()) || Objects.isNull(resultContext.getTargetType())) {
            return null;
        }
        try {
            Optional<TypeHandler> typeHandler = typeHandlers.stream()
                .filter(th -> th.isMatch(resultContext))
                .findFirst();
            if (typeHandler.isPresent()) {
                return typeHandler.get().handleResult(resultContext);
            }
            return jsonTypeHandler.handleResult(resultContext);
        } catch (Exception e) {
            logger.warn("[Dubbo Mock] handle the common result failed, will use unknown type handler.", e);
            return unknownTypeHandler.handleResult(resultContext);
        }
    }
}
