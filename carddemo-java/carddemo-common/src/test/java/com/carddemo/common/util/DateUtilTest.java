package com.carddemo.common.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilTest {

    @Test
    void testLillianDayConversion() {
        // Jan 1, 1601 = Lillian day 1
        LocalDate epoch = LocalDate.of(1601, 1, 1);
        assertEquals(1, DateUtil.toLillianDay(epoch));
        assertEquals(epoch, DateUtil.fromLillianDay(1));
    }

    @Test
    void testLillianDayRoundTrip() {
        LocalDate today = LocalDate.of(2024, 6, 15);
        long lillian = DateUtil.toLillianDay(today);
        assertEquals(today, DateUtil.fromLillianDay(lillian));
    }

    @Test
    void testParseDateYYYYMMDD() {
        LocalDate date = DateUtil.parseDate("20240615", "YYYYMMDD");
        assertEquals(LocalDate.of(2024, 6, 15), date);
    }

    @Test
    void testParseDateYYYY_MM_DD() {
        LocalDate date = DateUtil.parseDate("2024-06-15", "YYYY-MM-DD");
        assertEquals(LocalDate.of(2024, 6, 15), date);
    }

    @Test
    void testParseDateDefault() {
        LocalDate date = DateUtil.parseDate("2024-06-15");
        assertEquals(LocalDate.of(2024, 6, 15), date);
    }

    @Test
    void testFormatDate() {
        LocalDate date = LocalDate.of(2024, 6, 15);
        assertEquals("2024-06-15", DateUtil.formatDate(date, "YYYY-MM-DD"));
        assertEquals("20240615", DateUtil.formatDate(date, "YYYYMMDD"));
    }

    @Test
    void testFormatDateNull() {
        assertEquals("", DateUtil.formatDate(null, "YYYY-MM-DD"));
    }

    @Test
    void testConvertDateFormat() {
        // YYYYMMDD (type 1) → YYYY-MM-DD (output type 1)
        assertEquals("2024-06-15", DateUtil.convertDateFormat("20240615", "1", "1"));
        // YYYY-MM-DD (type 2) → YYYYMMDD (output type 2)
        assertEquals("20240615", DateUtil.convertDateFormat("2024-06-15", "2", "2"));
    }

    @Test
    void testIsValidDate() {
        assertTrue(DateUtil.isValidDate("2024-06-15", "YYYY-MM-DD"));
        assertTrue(DateUtil.isValidDate("20240615", "YYYYMMDD"));
        assertFalse(DateUtil.isValidDate("2024-13-01", "YYYY-MM-DD"));
        assertFalse(DateUtil.isValidDate("invalid", "YYYY-MM-DD"));
        assertFalse(DateUtil.isValidDate("", "YYYY-MM-DD"));
    }

    @Test
    void testParseTimestamp() {
        LocalDateTime dt = DateUtil.parseTimestamp("2022-06-10 19:27:53.000000");
        assertNotNull(dt);
        assertEquals(2022, dt.getYear());
        assertEquals(6, dt.getMonthValue());
        assertEquals(10, dt.getDayOfMonth());
        assertEquals(19, dt.getHour());
        assertEquals(27, dt.getMinute());
        assertEquals(53, dt.getSecond());
    }

    @Test
    void testParseTimestampNull() {
        assertNull(DateUtil.parseTimestamp(null));
        assertNull(DateUtil.parseTimestamp(""));
        assertNull(DateUtil.parseTimestamp("   "));
    }

    @Test
    void testFormatTimestamp() {
        LocalDateTime dt = LocalDateTime.of(2022, 6, 10, 19, 27, 53);
        String formatted = DateUtil.formatTimestamp(dt);
        assertEquals("2022-06-10 19:27:53.000000", formatted);
    }

    @Test
    void testFormatTimestampNull() {
        assertEquals("", DateUtil.formatTimestamp(null));
    }
}
