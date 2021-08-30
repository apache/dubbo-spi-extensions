package org.apache.dubbo.mock.handler;

import com.google.gson.Gson;

/**
 * @author chenglu
 * @date 2021-08-30 19:46
 */
public class JsonTypeHandler implements TypeHandler<Object> {

    private Gson gson;

    public JsonTypeHandler() {
        gson = new Gson();
    }

    @Override
    public boolean isMatch(ResultContext resultContext) {
        return true;
    }

    @Override
    public Object handleResult(ResultContext resultContext) {
        return gson.fromJson(resultContext.getData(), resultContext.getTargetType());
    }
}
