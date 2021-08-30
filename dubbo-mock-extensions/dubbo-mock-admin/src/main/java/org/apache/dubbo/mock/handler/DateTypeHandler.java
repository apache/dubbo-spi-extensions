package org.apache.dubbo.mock.handler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

/**
 * @author chenglu
 * @date 2021-08-30 19:11
 */
public class DateTypeHandler implements TypeHandler<Date> {

    @Override
    public boolean isMatch(ResultContext resultContext) {
        return Objects.equals(Date.class, resultContext.getTargetType());
    }

    @Override
    public Date handleResult(ResultContext resultContext) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return dateFormat.parse(resultContext.getData());
        } catch (Exception e) {
            return new Date(Long.parseLong(resultContext.getData()));
        }
    }
}
