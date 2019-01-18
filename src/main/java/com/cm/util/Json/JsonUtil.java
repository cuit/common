package com.cm.util.Json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author shichao.xia
 * @date 2019/1/18 下午2:08
 */
public class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        // 允许注释
        MAPPER.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        // 时间转换
        SimpleModule module = new SimpleModule("DateTimeModule", Version.unknownVersion());
        module.addSerializer(Date.class, new DateJsonSerializer());
        module.addDeserializer(Date.class, new DateJsonDeserializer());
        MAPPER.registerModule(module);
    }

    public static String of(Object object) {
        if (Objects.nonNull(object)) {
            try {
                return MAPPER.writeValueAsString(object);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public static <T> T toBean(String json, Class<T> clazz) {
        if (StringUtils.isNotBlank(json) && Objects.nonNull(clazz)) {
            try {
                return MAPPER.readValue(json, clazz);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public static <T> T toBean(String json, TypeReference<T> reference) {
        if (StringUtils.isNotBlank(json) && Objects.nonNull(reference)) {
            try {
                return (T) MAPPER.readValue(json, reference);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    /**
     * 转换的实体 必须有无参构造器
     *
     * @param json  待转换的json字符串
     * @param clazz 待转换的实体类
     * @param <T>   泛型
     * @return 集合
     */
    public static <T> List<T> toList(String json, Class<T> clazz) {
        if (StringUtils.isNotBlank(json) && Objects.nonNull(clazz)) {
            try {
                JavaType javaType = MAPPER.getTypeFactory().constructParametricType(ArrayList.class, clazz);
                return MAPPER.readValue(json, javaType);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    public static <K, V> Map<K, V> ofMap(String json, Class<K> keyClass, Class<V> valueClass) {
        if (StringUtils.isNotBlank(json) && Objects.nonNull(keyClass) && Objects.nonNull(valueClass)) {
            try {
                JavaType javaType = MAPPER.getTypeFactory().constructParametricType(HashMap.class, keyClass, valueClass);
                return MAPPER.readValue(json, javaType);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

}
