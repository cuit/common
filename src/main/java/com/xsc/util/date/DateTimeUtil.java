package com.xsc.util.date;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

/**
 * @author shichao.xia
 * @date 2019/1/17 下午4:20
 */
public class DateTimeUtil {

    private static final String YYYY_MM_DD = "yyyy/MM/dd";

    private static final String DD_MM_YYYY = "dd/MM/yyyy";

    private static final String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

    private static final String YYYY_MM_DD_1 = "yyyy:MM:dd";

    private static final String DD_MM_YYYY_1 = "dd:MM:yyyy";

    private static final ThreadLocal<SimpleDateFormat> LOCAL = ThreadLocal.withInitial(SimpleDateFormat::new);

    private static SimpleDateFormat getDateFormat(String pattern) {
        SimpleDateFormat simpleDateFormat = LOCAL.get();
        simpleDateFormat.applyPattern(pattern);
        return simpleDateFormat;
    }

    public static String parse4Y2M2D(Date date) {
        try {
            return getDateFormat(YYYY_MM_DD).format(date);
        } catch (Exception e) {
            return null;
        }
    }

    public static String parse2D2M4Y(Date date) {
        try {
            return getDateFormat(DD_MM_YYYY).format(date);
        } catch (Exception e) {
            return null;
        }
    }

    public static String parse4Y2M2D1(Date date) {
        try {
            return getDateFormat(YYYY_MM_DD_1).format(date);
        } catch (Exception e) {
            return null;
        }
    }

    public static String parse2D2M4Y1(Date date) {
        try {
            return getDateFormat(DD_MM_YYYY_1).format(date);
        } catch (Exception e) {
            return null;
        }
    }

    public static String parse4Y2M2D2H2M2S(Date date) {
        try {
            return getDateFormat(YYYY_MM_DD_HH_MM_SS).format(date);
        } catch (Exception e) {
            return null;
        }
    }

    public static Date get4Y2M2D2H2M2S(String date) {
        try {
            return getDateFormat(YYYY_MM_DD_HH_MM_SS).parse(date);
        } catch (Exception e) {
            return null;
        }
    }

    public static Date get4Y2M2D(String date) {
        try {
            return getDateFormat(YYYY_MM_DD).parse(date);
        } catch (ParseException e) {
            return null;
        }
    }

    public static Date get4Y2M2D1(String date) {
        try {
            return getDateFormat(YYYY_MM_DD_1).parse(date);
        } catch (ParseException e) {
            return null;
        }
    }

    public static Date get2D2M4Y(String date) {
        try {
            return getDateFormat(DD_MM_YYYY).parse(date);
        } catch (ParseException e) {
            return null;
        }
    }

    public static Date get2D2M4Y1(String date) {
        try {
            return getDateFormat(DD_MM_YYYY_1).parse(date);
        } catch (ParseException e) {
            return null;
        }
    }

    public static Date getNextDay(Date date) {
        return getNextNDay(date, 1);
    }

    public static Date getNextNDay(Date date, int day) {
        if (Objects.nonNull(date)) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, day);
            return calendar.getTime();
        }
        return null;
    }

    /**
     * 获取传入时间所在月份的第一天的相关信息
     */
    public static Calendar getFirstDayOfMonth(Date date) {
        if (Objects.nonNull(date)) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            return calendar;
        }
        return null;
    }

}

