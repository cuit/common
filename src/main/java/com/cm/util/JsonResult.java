package com.cm.util;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

/**
 * @author shichao.xia
 * @date 2019/1/16 下午5:28
 */
public class JsonResult<T> {

    private int status;

    private String msg;

    private T data;

    @JsonCreator
    protected JsonResult(@JsonProperty("status") int status, @JsonProperty("msg") String msg, @JsonProperty("data") T data) {
        this.status = status;
        this.msg = msg;
        this.data = data;
    }

    public static <T> JsonResult<T> create(int status, String msg, T data) {
        return new JsonResult<T>(status, msg, data);
    }

    public static <T> JsonResult<T> success(int status, String msg) {
        return create(status, msg, null);
    }

    public static <T> JsonResult<T> success(int status, String msg, T data) {
        return create(status, msg, data);
    }

    public static <T> JsonResult<T> success(String msg, T data) {
        return create(0, msg, data);
    }

    public static <T> JsonResult<T> success(T data) {
        return create(0, null, data);
    }

    public static <T> JsonResult<T> error(int status, String msg) {
        return create(status, msg, null);
    }

    public int getStatus() {
        return status;
    }

    public String getMsg() {
        return msg;
    }

    public T getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o, true);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(17, 31, this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }
}
