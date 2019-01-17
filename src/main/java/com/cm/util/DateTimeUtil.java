package com.cm.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author shichao.xia
 * @date 2019/1/17 下午4:20
 */
public class DateTimeUtil {

    private static final String YYYY_MM_DD = "yyyy/MM/dd";

    private static final ThreadLocal<SimpleDateFormat> LOCAL = ThreadLocal.withInitial(SimpleDateFormat::new);

    private static SimpleDateFormat getDateFormat(String pattern) {
        SimpleDateFormat simpleDateFormat = LOCAL.get();
        simpleDateFormat.applyPattern(pattern);
        return simpleDateFormat;
    }

    public static String parseYYYYMMDD(Date date) {
        try {
            return getDateFormat(YYYY_MM_DD).format(date);
        } catch (Exception e) {
            return null;
        }
    }

}
