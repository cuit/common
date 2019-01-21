package com.cm.util;

import java.util.Collection;

/**
 * @author shichao.xia
 * @date 2019/1/21 上午10:16
 */
public class XAssert {

    private static final String NULL = "对象为空";

    private static final String IS_TRUE = "不符合预期结果";

    public static <T> T notNull(T t) {
        if (t == null) {
            throw new RuntimeException(NULL);
        }
        return t;
    }

    public static <T> T notNull(T t, String errMsg) {
        if (t == null) {
            throw new RuntimeException(errMsg);
        }
        return t;
    }

    public static <T> T notNull(T t, String template, Object... params) {
        if (t == null) {
            throw new RuntimeException(String.format(template, params));
        }
        return t;
    }

    public static void isTrue(boolean expression) {
        if (!expression) {
            throw new RuntimeException(IS_TRUE);
        }
    }

    public static void isTrue(boolean expression, String errMsg) {
        if (!expression) {
            throw new RuntimeException(errMsg);
        }
    }

    public static void isTrue(boolean expression, String template, Object... params) {
        if (!expression) {
            throw new RuntimeException(String.format(template, params));
        }
    }

    public static void notEmpty(Object[] obj) {
        if (obj == null || obj.length == 0) {
            throw new RuntimeException(NULL);
        }
    }

    public static void notEmpty(Object[] obj, String errMsg) {
        if (obj == null || obj.length == 0) {
            throw new RuntimeException(errMsg);
        }
    }

    public static void notEmpty(Object[] obj, String template, Object[] params) {
        if (obj == null || obj.length == 0) {
            throw new RuntimeException(String.format(template, params));
        }
    }

    public static void notEmpty(Collection collection) {
        if (collection == null || collection.size() == 0) {
            throw new RuntimeException(NULL);
        }
    }

    public static void notEmpty(Collection collection, String errMsg) {
        if (collection == null || collection.size() == 0) {
            throw new RuntimeException(errMsg);
        }
    }

    public static void notEmpty(Collection collection, String template, Object[] params) {
        if (collection == null || collection.size() == 0) {
            throw new RuntimeException(String.format(template, params));
        }
    }
}
