package ph.pakete.helpers;

import android.text.format.DateUtils;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.util.Date;

public class RelativeDateHelper {

    public static String abbrevRelativeTimeFromDate(Date date) {
        PeriodFormatterBuilder formatterBuilder = new PeriodFormatterBuilder();
        Date now = new Date();
        Period period = new Period(date.getTime(), now.getTime());
        if(period.getYears() != 0) {
            formatterBuilder.appendYears().appendSuffix("y");
        } else if(period.getMonths() != 0) {
            formatterBuilder.appendMonths().appendSuffix("mo");
        } else if(period.getWeeks() != 0) {
            formatterBuilder.appendWeeks().appendSuffix("w");
        } else if(period.getDays() != 0) {
            formatterBuilder.appendDays().appendSuffix("d");
        } else if(period.getHours() != 0) {
            formatterBuilder.appendHours().appendSuffix("h");
        } else if(period.getMinutes() != 0) {
            formatterBuilder.appendMinutes().appendSuffix("m");
        } else if(period.getSeconds() != 0) {
            formatterBuilder.appendSeconds().appendSuffix("s");
        }
        PeriodFormatter formatter = formatterBuilder.printZeroNever().toFormatter();
        return formatter.print(period);
    }

    public static String relativeTimeFromDate(Date date) {
        return DateUtils.getRelativeTimeSpanString(date.getTime(), new Date().getTime(),DateUtils.MINUTE_IN_MILLIS, 0).toString();
    }
}
