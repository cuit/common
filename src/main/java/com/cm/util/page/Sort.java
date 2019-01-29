package com.cm.util.page;

import java.util.Locale;

/**
 * @author nobody
 * @date 2019/1/29 18:08
 */
public enum Sort {

    // 降序
    DESC,
    // 升序
    ASC;

    public static Sort fromString(String sort) {
        try {
            return Sort.valueOf(sort.toUpperCase(Locale.US));
        } catch (Exception e) {
            throw new IllegalArgumentException(String.format("Invalid value '%s' for sort given! Has to be 'desc' or 'asc'.", sort), e);
        }
    }

    public static Sort fromStringOrNull(String sort) {
        try {
            return fromString(sort);
        } catch (Exception e) {
            return null;
        }
    }
}
