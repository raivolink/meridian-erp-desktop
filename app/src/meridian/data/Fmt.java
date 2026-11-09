package meridian.data;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lenient input parsing and output formatting, the same way as the Meridian web app. */
public final class Fmt {
    private static final DateTimeFormatter US_DATE = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final Pattern AMOUNT = Pattern.compile("-?\\d+(\\.\\d{1,2})?");
    private static final Pattern DATE_US = Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{2}|\\d{4})");
    private static final Pattern DATE_ISO = Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})");
    private static final Pattern DATE_DE = Pattern.compile("(\\d{1,2})\\.(\\d{1,2})\\.(\\d{4})");

    private Fmt() {
    }

    /** "1,234.56" / "1234.56" / "$99" -> number; "" -> null; garbage -> NaN. */
    public static Double parseAmount(String s) {
        String v = (s == null ? "" : s).replaceAll("[,\\s]", "").replaceFirst("^\\$", "");
        if (v.isEmpty()) {
            return null;
        }
        return AMOUNT.matcher(v).matches() ? Double.valueOf(v) : Double.NaN;
    }

    /** Accepts MM/DD/YYYY, M/D/YY, YYYY-MM-DD and DD.MM.YYYY; null when invalid. */
    public static LocalDate parseDate(String s) {
        String v = s == null ? "" : s.trim();
        int y;
        int m;
        int d;
        Matcher r;
        if ((r = DATE_US.matcher(v)).matches()) {
            m = Integer.parseInt(r.group(1));
            d = Integer.parseInt(r.group(2));
            y = Integer.parseInt(r.group(3));
        } else if ((r = DATE_ISO.matcher(v)).matches()) {
            y = Integer.parseInt(r.group(1));
            m = Integer.parseInt(r.group(2));
            d = Integer.parseInt(r.group(3));
        } else if ((r = DATE_DE.matcher(v)).matches()) {
            d = Integer.parseInt(r.group(1));
            m = Integer.parseInt(r.group(2));
            y = Integer.parseInt(r.group(3));
        } else {
            return null;
        }
        if (y < 100) {
            y += 2000;
        }
        try {
            return LocalDate.of(y, m, d);
        } catch (DateTimeException e) {
            return null;
        }
    }

    /** 1234.5 -> "1,234.50" */
    public static String amount(double n) {
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(n);
    }

    /** MM/DD/YYYY, or "" for null. */
    public static String date(LocalDate date) {
        return date == null ? "" : date.format(US_DATE);
    }

    public static double round2(double n) {
        return Math.round(n * 100) / 100.0;
    }

    public static String up(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }

    public static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
