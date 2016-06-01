package ph.pakete.helpers;

import android.text.format.DateUtils;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormatterBuilder;

import java.util.Date;

public class RelativeDateHelper {

    public static String abbrevRelativeTimeFromDate(Date date) {
        Date now = new Date();
        long difference = Math.abs(date.getTime() - now.getTime());
        Period period = new Period(date.getTime(), now.getTime());
        PeriodFormatterBuilder formatterBuilder = new PeriodFormatterBuilder();
        if (difference > DateUtils.YEAR_IN_MILLIS) {
            formatterBuilder.appendYears().appendSuffix("y");
        } else if (difference > DateUtils.DAY_IN_MILLIS * 30) {
            formatterBuilder.appendMonths().appendSuffix("m");
        } else if (difference > DateUtils.WEEK_IN_MILLIS) {
            formatterBuilder.appendWeeks().appendSuffix("w");
        } else if (difference > DateUtils.DAY_IN_MILLIS) {
            formatterBuilder.appendDays().appendSuffix("d");
        } else if (difference > DateUtils.HOUR_IN_MILLIS) {
            formatterBuilder.appendHours().appendSuffix("h");
        } else if (difference > DateUtils.MINUTE_IN_MILLIS) {
            formatterBuilder.appendMinutes().appendSuffix("m");
        } else if (difference > DateUtils.SECOND_IN_MILLIS) {
            formatterBuilder.appendSeconds().appendSuffix("s");
        }

        return formatterBuilder.toFormatter().print(period);
    }

    public static String relativeTimeFromDate(Date date) {
        return DateUtils.getRelativeTimeSpanString(date.getTime(), new Date().getTime(),DateUtils.MINUTE_IN_MILLIS, 0).toString();
    }
}
