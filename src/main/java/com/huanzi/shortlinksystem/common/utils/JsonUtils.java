package com.huanzi.shortlinksystem.common.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.huanzi.shortlinksystem.common.exception.BizException;
import com.huanzi.shortlinksystem.common.result.ResultCode;

public final class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private JsonUtils() {
    }

    public static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BizException(ResultCode.SYSTEM_ERROR.getCode(), "json serialize error");
        }
    }

    public static <T> T parseObject(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException exception) {
            throw new BizException(ResultCode.SYSTEM_ERROR.getCode(), "json parse error");
        }
    }
}
