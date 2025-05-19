package com.siyukio.tools.util;

import lombok.extern.slf4j.Slf4j;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;

/**
 * Converts between Strings and Dates in various formats
 * <p>
 * Author: Buddy
 */
@Slf4j
public abstract class DateUtils {

    public static final DateFormat DEFAULT_DATE_FORMAT = new MultiDateFormat();

    public static String format(Date date) {
        return DEFAULT_DATE_FORMAT.format(date);
    }

    public static String format(Date date, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(date);
    }

    public static String format(Duration duration) {
        Date date = new Date(System.currentTimeMillis() + duration.toMillis());
        return format(date);
    }

    public static Date parse(String date) {
        try {
            return DEFAULT_DATE_FORMAT.parse(date);
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Date parse(String date, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        try {
            return sdf.parse(date);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

}
