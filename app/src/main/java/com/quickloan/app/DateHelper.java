package com.quickloan.app;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateHelper {

    private static final String INDIAN_FORMAT = "dd-MM-yyyy";
    private static final String ISO_FORMAT = "yyyy-MM-dd";

    /** Returns current date formatted as DD-MM-YYYY (e.g., 18-09-2026) */
    public static String getTodayDate() {
        return new SimpleDateFormat(INDIAN_FORMAT, Locale.getDefault()).format(new Date());
    }

    /** Converts any date string (ISO or timestamp) to DD-MM-YYYY for display */
    public static String formatToIndianDate(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return "-";
        }
        rawDate = rawDate.trim();

        // Already in DD-MM-YYYY
        if (rawDate.matches("\\d{2}-\\d{2}-\\d{4}")) {
            return rawDate;
        }

        // Convert YYYY-MM-DD to DD-MM-YYYY
        try {
            Date date = new SimpleDateFormat(ISO_FORMAT, Locale.getDefault()).parse(rawDate);
            if (date != null) {
                return new SimpleDateFormat(INDIAN_FORMAT, Locale.getDefault()).format(date);
            }
        } catch (ParseException ignored) {}

        return rawDate;
    }
}
