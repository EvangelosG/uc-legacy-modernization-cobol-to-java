package com.carddemo.common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * Replaces CSUTLDTC.cbl, CODATECN.cpy, and CSDAT01Y.cpy date utilities.
 * Handles date validation, conversion, and formatting using java.time.
 * Supports the CEEDAYS epoch (Jan 1, 1601) Lillian date conversion.
 */
public final class DateUtil {

    private static final LocalDate LILLIAN_EPOCH = LocalDate.of(1601, 1, 1);
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter COBOL_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    private DateUtil() {
    }

    /**
     * Converts a Lillian day number (days since Jan 1, 1601) to a LocalDate.
     * Replaces CEEDAYS API output interpretation from CSUTLDTC.
     */
    public static LocalDate fromLillianDay(long lillianDay) {
        return LILLIAN_EPOCH.plusDays(lillianDay - 1);
    }

    /**
     * Converts a LocalDate to a Lillian day number (days since Jan 1, 1601).
     * Replaces the input to CEEDAYS API from CSUTLDTC.
     */
    public static long toLillianDay(LocalDate date) {
        return ChronoUnit.DAYS.between(LILLIAN_EPOCH, date) + 1;
    }

    /**
     * Validates a date string. Replaces CSUTLDTC date validation logic.
     * Returns true if the date is valid in the given format.
     */
    public static boolean isValidDate(String dateStr, String format) {
        try {
            parseDate(dateStr, format);
            return true;
        } catch (DateTimeParseException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Parses a date from a string using the specified format.
     * Supports COBOL-style format strings: YYYYMMDD, YYYY-MM-DD.
     */
    public static LocalDate parseDate(String dateStr, String format) {
        if (dateStr == null || dateStr.isBlank()) {
            throw new IllegalArgumentException("Date string must not be blank");
        }
        DateTimeFormatter formatter = resolveFormatter(format);
        return LocalDate.parse(dateStr.trim(), formatter);
    }

    /**
     * Parses a date using YYYY-MM-DD format (COBOL X(10) date fields).
     */
    public static LocalDate parseDate(String dateStr) {
        return parseDate(dateStr, "YYYY-MM-DD");
    }

    /**
     * Formats a date to a string using the specified format.
     */
    public static String formatDate(LocalDate date, String format) {
        if (date == null) {
            return "";
        }
        DateTimeFormatter formatter = resolveFormatter(format);
        return date.format(formatter);
    }

    /**
     * Converts between date formats (replaces CODATECN.cpy logic).
     * Input type "1" = YYYYMMDD, "2" = YYYY-MM-DD.
     * Output type "1" = YYYY-MM-DD, "2" = YYYYMMDD.
     */
    public static String convertDateFormat(String inputDate, String inputType, String outputType) {
        DateTimeFormatter inFmt = "1".equals(inputType) ? YYYYMMDD : YYYY_MM_DD;
        DateTimeFormatter outFmt = "1".equals(outputType) ? YYYY_MM_DD : YYYYMMDD;
        LocalDate date = LocalDate.parse(inputDate.trim(), inFmt);
        return date.format(outFmt);
    }

    /**
     * Parses a COBOL timestamp string (X(26): YYYY-MM-DD HH:MM:SS.MMMMMM).
     */
    public static LocalDateTime parseTimestamp(String timestampStr) {
        if (timestampStr == null || timestampStr.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(timestampStr.trim(), COBOL_TIMESTAMP);
    }

    /**
     * Formats a LocalDateTime to COBOL timestamp format.
     */
    public static String formatTimestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(COBOL_TIMESTAMP);
    }

    private static DateTimeFormatter resolveFormatter(String format) {
        if (format == null) {
            return YYYY_MM_DD;
        }
        return switch (format.toUpperCase().replace(" ", "")) {
            case "YYYYMMDD" -> YYYYMMDD;
            case "YYYY-MM-DD" -> YYYY_MM_DD;
            default -> DateTimeFormatter.ofPattern(format);
        };
    }
}
