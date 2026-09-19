package com.quickloan.app;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateHelper {

    private static final String INDIAN_FORMAT = "dd-MM-yyyy";
    private static final String ISO_FORMAT = "yyyy-MM-dd";

    public static String getTodayDate() {
        return new SimpleDateFormat(INDIAN_FORMAT, Locale.getDefault()).format(new Date());
    }

    public static String formatToIndianDate(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return "-";
        }
        rawDate = rawDate.trim();

        if (rawDate.matches("\\d{2}-\\d{2}-\\d{4}")) {
            return rawDate;
        }

        try {
            Date date = new SimpleDateFormat(ISO_FORMAT, Locale.getDefault()).parse(rawDate);
            if (date != null) {
                return new SimpleDateFormat(INDIAN_FORMAT, Locale.getDefault()).format(date);
            }
        } catch (ParseException ignored) {}

        return rawDate;
    }

    /**
     * Calculates days elapsed from loan start date to today.
     * Day 1 is the loan start date itself.
     */
    public static int getDaysElapsed(String startDateStr) {
        if (startDateStr == null || startDateStr.trim().isEmpty()) return 1;
        try {
            SimpleDateFormat sdf;
            if (startDateStr.contains("-")) {
                String[] parts = startDateStr.split("-");
                if (parts[0].length() == 4) {
                    sdf = new SimpleDateFormat(ISO_FORMAT, Locale.getDefault());
                } else {
                    sdf = new SimpleDateFormat(INDIAN_FORMAT, Locale.getDefault());
                }
            } else {
                return 1;
            }

            Date start = sdf.parse(startDateStr);
            Date now = new Date();

            SimpleDateFormat zeroTime = new SimpleDateFormat(ISO_FORMAT, Locale.getDefault());
            Date startZero = zeroTime.parse(zeroTime.format(start));
            Date nowZero = zeroTime.parse(zeroTime.format(now));

            if (startZero != null && nowZero != null) {
                long diffMillis = nowZero.getTime() - startZero.getTime();
                int days = (int) (diffMillis / (1000 * 60 * 60 * 24)) + 1;
                return Math.max(1, days);
            }
        } catch (Exception ignored) {}
        return 1;
    }

    /**
     * Calculates cumulative due. Unpaid EMIs accumulate each elapsed day:
     * Accumulated Due = (Days Elapsed * Daily EMI) - Total Paid
     * Capped at remaining balance.
     */
    public static double calculateAccumulatedDue(String startDate, double dailyEmi, double totalAmount, double paidAmount) {
        int days = getDaysElapsed(startDate);
        double expectedCumulativeDue = days * dailyEmi;
        double remainingBalance = Math.max(0.0, totalAmount - paidAmount);
        double overdue = Math.max(0.0, expectedCumulativeDue - paidAmount);
        return Math.min(overdue, remainingBalance);
    }
}
