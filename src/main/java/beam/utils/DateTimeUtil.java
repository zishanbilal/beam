package beam.utils;

import org.joda.time.DateTime;
import org.joda.time.Seconds;

import java.time.LocalDate;

/**
 * @author ahmar.nadeem
 */
public final class DateTimeUtil {

    /**
     * A util function to parse a local date string to java local date.
     *
     * @param localDateString
     * @return
     */
    public static LocalDate convertStringToLocalDate(String localDateString) {

        return LocalDate.parse(localDateString);
    }

    /**
     * A util function to calculate time difference from the base.
     * <p>If the currentTime is provided, it checks the difference in seconds from the base.</p>
     * <p>In case the parameter is null, then the function checks the difference from base using current system time</p>
     *
     * @param currentTime
     * @return
     */
    public static int calculateTimeFromBase(DateTime currentTime) {

        //TODO: temporarily hardcoding the base date because couldn't call the scala function to get base date.
        String BASE_DATE = "2016-10-17T00:00:00-07:00";

        DateTime baseTime = DateTime.parse(BASE_DATE);
        DateTime timeToCompare = currentTime != null ? currentTime : DateTime.now();

        return Seconds.secondsBetween(baseTime, timeToCompare).getSeconds();
    }
}
