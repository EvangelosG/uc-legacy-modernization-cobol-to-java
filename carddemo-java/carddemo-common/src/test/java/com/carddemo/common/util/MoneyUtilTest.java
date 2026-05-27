package com.carddemo.common.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyUtilTest {

    @Test
    void testToMoneyFromBigDecimal() {
        BigDecimal result = MoneyUtil.toMoney(new BigDecimal("19.999"));
        assertEquals(new BigDecimal("20.00"), result);
    }

    @Test
    void testToMoneyFromString() {
        assertEquals(new BigDecimal("100.50"), MoneyUtil.toMoney("100.50"));
        assertEquals(new BigDecimal("100.00"), MoneyUtil.toMoney("100"));
    }

    @Test
    void testToMoneyFromLong() {
        assertEquals(new BigDecimal("100.00"), MoneyUtil.toMoney(100L));
    }

    @Test
    void testAdd() {
        BigDecimal a = MoneyUtil.toMoney("100.50");
        BigDecimal b = MoneyUtil.toMoney("50.25");
        assertEquals(new BigDecimal("150.75"), MoneyUtil.add(a, b));
    }

    @Test
    void testSubtract() {
        BigDecimal a = MoneyUtil.toMoney("100.50");
        BigDecimal b = MoneyUtil.toMoney("50.25");
        assertEquals(new BigDecimal("50.25"), MoneyUtil.subtract(a, b));
    }

    @Test
    void testMultiply() {
        BigDecimal a = MoneyUtil.toMoney("100.00");
        BigDecimal b = new BigDecimal("0.15");
        assertEquals(new BigDecimal("15.00"), MoneyUtil.multiply(a, b));
    }

    @Test
    void testDivide() {
        BigDecimal a = MoneyUtil.toMoney("100.00");
        BigDecimal b = new BigDecimal("3");
        assertEquals(new BigDecimal("33.33"), MoneyUtil.divide(a, b));
    }

    @Test
    void testDivideRoundsHalfUp() {
        BigDecimal a = MoneyUtil.toMoney("100.00");
        BigDecimal b = new BigDecimal("6");
        // 100/6 = 16.6666... → rounds to 16.67 (HALF_UP)
        assertEquals(new BigDecimal("16.67"), MoneyUtil.divide(a, b));
    }

    @Test
    void testComputeDailyInterest() {
        BigDecimal principal = MoneyUtil.toMoney("10000.00");
        BigDecimal rate = new BigDecimal("15.00");
        BigDecimal interest = MoneyUtil.computeDailyInterest(principal, rate, 30);
        // 10000 * 15/100/365 * 30 = 123.29
        assertTrue(interest.compareTo(BigDecimal.ZERO) > 0);
        assertEquals(new BigDecimal("123.29"), interest);
    }

    @Test
    void testAbs() {
        assertEquals(new BigDecimal("100.50"), MoneyUtil.abs(MoneyUtil.toMoney("-100.50")));
        assertEquals(new BigDecimal("100.50"), MoneyUtil.abs(MoneyUtil.toMoney("100.50")));
    }

    @Test
    void testNegate() {
        assertEquals(new BigDecimal("-100.50"), MoneyUtil.negate(MoneyUtil.toMoney("100.50")));
    }

    @Test
    void testIsZero() {
        assertTrue(MoneyUtil.isZero(BigDecimal.ZERO));
        assertTrue(MoneyUtil.isZero(new BigDecimal("0.00")));
        assertFalse(MoneyUtil.isZero(new BigDecimal("0.01")));
    }

    @Test
    void testIsPositiveNegative() {
        assertTrue(MoneyUtil.isPositive(new BigDecimal("1.00")));
        assertFalse(MoneyUtil.isPositive(new BigDecimal("-1.00")));
        assertTrue(MoneyUtil.isNegative(new BigDecimal("-1.00")));
        assertFalse(MoneyUtil.isNegative(new BigDecimal("1.00")));
    }

    @Test
    void testParseCobolSignedPositive() {
        // "00000001940{" → 194.00 ('{' = +0)
        assertEquals(new BigDecimal("194.00"), MoneyUtil.parseCobolSigned("00000001940{"));
    }

    @Test
    void testParseCobolSignedNegative() {
        // "0000009190}" → -919.00 ('}' = -0)
        assertEquals(new BigDecimal("-919.00"), MoneyUtil.parseCobolSigned("0000009190}"));
    }

    @Test
    void testParseCobolSignedWithLetterPositive() {
        // "0000005047G" → 504.77 ('G' = +7)
        assertEquals(new BigDecimal("504.77"), MoneyUtil.parseCobolSigned("0000005047G"));
    }

    @Test
    void testParseCobolSignedWithLetterNegative() {
        // "0000009190J" → -919.01 ('J' = -1)
        assertEquals(new BigDecimal("-919.01"), MoneyUtil.parseCobolSigned("0000009190J"));
    }

    @Test
    void testParseCobolSignedZero() {
        assertEquals(new BigDecimal("0.00"), MoneyUtil.parseCobolSigned("0000000000{"));
        assertEquals(new BigDecimal("0.00"), MoneyUtil.parseCobolSigned(""));
        assertEquals(new BigDecimal("0.00"), MoneyUtil.parseCobolSigned(null));
    }

    @Test
    void testParseCobolSignedPositiveH() {
        // "0000000678H" → 67.88 ('H' = +8)
        assertEquals(new BigDecimal("67.88"), MoneyUtil.parseCobolSigned("0000000678H"));
    }
}
