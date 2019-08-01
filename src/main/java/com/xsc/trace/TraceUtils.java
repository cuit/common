package com.xsc.trace;

import java.util.UUID;

/**
 * @author xia
 * @date 2019/7/29 17:13
 */
public class TraceUtils {

    public static String buildTraceId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
}
