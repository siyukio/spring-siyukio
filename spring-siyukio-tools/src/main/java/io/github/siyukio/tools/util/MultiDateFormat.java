package io.github.siyukio.tools.util;

import io.github.siyukio.tools.collection.ConcurrentCache;

import java.text.*;
import java.util.*;

/**
 * Converts between Strings and Dates in various formats, with thread safety.
 * Supports the formats: yyyy-MM-dd HH:mm:ss, yyyy-MM-dd HH:mm, yyyy-MM-dd, yyyy-MM-ddTHH:mm:ss
 *
 * @author Buddy
 */
public class MultiDateFormat extends DateFormat {

    private final ConcurrentCache<Long, FeignDateFormat> feignDateFormatMap = new ConcurrentCache<>(99);
    private final Locale locale = Locale.getDefault(Locale.Category.FORMAT);

    public MultiDateFormat() {
        this.calendar = Calendar.getInstance(locale);
        //
        this.numberFormat = NumberFormat.getIntegerInstance(locale);
    }

    private FeignDateFormat getFeignDateFormat() {
        Long id = Thread.currentThread().threadId();
        FeignDateFormat feignDateFormat = this.feignDateFormatMap.get(id);
        if (feignDateFormat == null) {
            feignDateFormat = new FeignDateFormat(this.locale);
            this.feignDateFormatMap.put(id, feignDateFormat);
        }
        return feignDateFormat;
    }

    @Override
    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
        FeignDateFormat feignDateFormat = this.getFeignDateFormat();
        return feignDateFormat.format(date, toAppendTo, new FeignFieldPosition());
    }

    @Override
    public Date parse(String source, ParsePosition pos) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Date parse(String source) throws ParseException {
        FeignDateFormat feignDateFormat = this.getFeignDateFormat();
        return feignDateFormat.parse(source);
    }

    private final static class FeignDateFormat extends DateFormat {

        private final SimpleDateFormat defaultDateFormat;
        private final List<SimpleDateFormat> otherDateFormatList = new ArrayList<>();

        public FeignDateFormat(Locale locale) {
            this.calendar = Calendar.getInstance(locale);
            this.numberFormat = NumberFormat.getIntegerInstance(locale);
            defaultDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale);
            defaultDateFormat.setCalendar(this.calendar);
            defaultDateFormat.setNumberFormat(numberFormat);

            this.otherDateFormatList.add(defaultDateFormat);
            //
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", locale);
            this.otherDateFormatList.add(simpleDateFormat);
            simpleDateFormat.setCalendar(this.calendar);
            simpleDateFormat.setNumberFormat(numberFormat);
            //
            simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", locale);
            this.otherDateFormatList.add(simpleDateFormat);
            simpleDateFormat.setCalendar(this.calendar);
            simpleDateFormat.setNumberFormat(numberFormat);
        }

        @Override
        public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
            StringBuffer result = this.defaultDateFormat.format(date, toAppendTo, fieldPosition);
            String str = result.toString();
            if (str.contains("00:00:00")) {
                str = str.substring(0, str.indexOf(" "));
            }
            result = new StringBuffer();
            result.append(str);
            return result;
        }

        @Override
        public Date parse(String source) throws ParseException {
            source = source.replace("T", " ");
            Date date = null;
            ParsePosition pos;
            for (SimpleDateFormat simpleDateFormat : this.otherDateFormatList) {
                pos = new ParsePosition(0);
                date = simpleDateFormat.parse(source, pos);
                if (date != null) {
                    break;
                }
            }
            if (date == null) {
                try {
                    long time = Long.parseLong(source);
                    date = new Date(time);
                } catch (NumberFormatException ignored) {
                }
            }
            return date;
        }

        @Override
        public Date parse(String source, ParsePosition pos) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private final static class FeignFieldPosition extends FieldPosition {

        public FeignFieldPosition() {
            super(0);
        }

    }

}
