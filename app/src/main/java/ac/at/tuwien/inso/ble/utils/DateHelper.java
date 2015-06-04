package ac.at.tuwien.inso.ble.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by manu on 04.06.2015.
 */
public class DateHelper {

    private static SimpleDateFormat FORMAT = new SimpleDateFormat("yyMMddHHmmss");

    public static String toString(Date date) {
        return FORMAT.format(date);
    }

    public static Date parse(String s) {
        try {
            return FORMAT.parse(s);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
