package com.cm.util.json;

import com.cm.util.date.DateTimeUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Date;

/**
 * @author shichao.xia
 * @date 2019/1/18 下午2:18
 */
public class DateJsonDeserializer extends JsonDeserializer<Date> {
    @Override
    public Date deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        String date = jsonParser.getText();
        if (StringUtils.isNotBlank(date)) {
            return DateTimeUtil.get4Y2M2D2H2M2S(date);
        }
        return null;
    }
}
