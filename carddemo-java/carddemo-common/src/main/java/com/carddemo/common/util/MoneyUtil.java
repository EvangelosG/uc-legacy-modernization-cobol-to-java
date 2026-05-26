package com.carddemo.common.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * BigDecimal arithmetic matching COBOL COMPUTE semantics.
 * COBOL S9(n)V99 → BigDecimal(scale=2, ROUND_HALF_UP).
 */
public final class MoneyUtil {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    public static final MathContext MC = new MathContext(15, ROUNDING);

    private MoneyUtil() {
    }

    /**
     * Creates a BigDecimal with scale=2 and ROUND_HALF_UP from a raw value.
     */
    public static BigDecimal toMoney(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING);
    }

    /**
     * Creates a BigDecimal with scale=2 from a string representation.
     */
    public static BigDecimal toMoney(String value) {
        return new BigDecimal(value).setScale(SCALE, ROUNDING);
    }

    /**
     * Creates a BigDecimal with scale=2 from a long (whole units, no cents).
     */
    public static BigDecimal toMoney(long value) {
        return BigDecimal.valueOf(value).setScale(SCALE, ROUNDING);
    }

    /**
     * Adds two monetary values with proper scale.
     */
    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return a.add(b).setScale(SCALE, ROUNDING);
    }

    /**
     * Subtracts b from a with proper scale.
     */
    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return a.subtract(b).setScale(SCALE, ROUNDING);
    }

    /**
     * Multiplies two values, returning result with scale=2.
     * Matches COBOL COMPUTE a = b * c behavior.
     */
    public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
        return a.multiply(b, MC).setScale(SCALE, ROUNDING);
    }

    /**
     * Divides a by b with scale=2 and ROUND_HALF_UP.
     * Matches COBOL COMPUTE a = b / c behavior.
     */
    public static BigDecimal divide(BigDecimal a, BigDecimal b) {
        return a.divide(b, SCALE, ROUNDING);
    }

    /**
     * Computes interest: principal * rate / 100 / 365 * days.
     * Common pattern in CBACT04C interest calculation.
     */
    public static BigDecimal computeDailyInterest(BigDecimal principal, BigDecimal annualRate, int days) {
        return principal
                .multiply(annualRate, MC)
                .divide(BigDecimal.valueOf(100), MC.getPrecision(), ROUNDING)
                .divide(BigDecimal.valueOf(365), MC.getPrecision(), ROUNDING)
                .multiply(BigDecimal.valueOf(days), MC)
                .setScale(SCALE, ROUNDING);
    }

    /**
     * Returns the absolute value with scale=2.
     */
    public static BigDecimal abs(BigDecimal value) {
        return value.abs().setScale(SCALE, ROUNDING);
    }

    /**
     * Returns the negated value with scale=2.
     */
    public static BigDecimal negate(BigDecimal value) {
        return value.negate().setScale(SCALE, ROUNDING);
    }

    /**
     * Checks if a value is zero (comparison-safe for BigDecimal).
     */
    public static boolean isZero(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Checks if a value is positive.
     */
    public static boolean isPositive(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if a value is negative.
     */
    public static boolean isNegative(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Parses a COBOL zoned decimal signed field (S9(n)V99) from display format.
     * The last character encodes the sign: { = +0, A-I = +1..+9, } = -0, J-R = -1..-9.
     */
    public static BigDecimal parseCobolSigned(String cobolValue) {
        if (cobolValue == null || cobolValue.isBlank()) {
            return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        }
        String s = cobolValue.trim();
        char last = s.charAt(s.length() - 1);
        String digits = s.substring(0, s.length() - 1);
        String lastDigit;
        boolean negative;

        if (last >= '{' && last == '{') { lastDigit = "0"; negative = false; }
        else if (last >= 'A' && last <= 'I') { lastDigit = String.valueOf((char) ('0' + (last - 'A' + 1))); negative = false; }
        else if (last == '}') { lastDigit = "0"; negative = true; }
        else if (last >= 'J' && last <= 'R') { lastDigit = String.valueOf((char) ('0' + (last - 'J' + 1))); negative = true; }
        else if (last >= '0' && last <= '9') { lastDigit = String.valueOf(last); negative = false; }
        else { return BigDecimal.ZERO.setScale(SCALE, ROUNDING); }

        String full = digits + lastDigit;
        BigDecimal result = new BigDecimal(full).movePointLeft(SCALE);
        return (negative ? result.negate() : result).setScale(SCALE, ROUNDING);
    }
}
