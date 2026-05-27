package com.carddemo.domain.valueobject;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ValueObjectTest {

    @Test
    void testMoneyCreation() {
        Money m = Money.of("100.50");
        assertEquals(new BigDecimal("100.50"), m.getAmount());
        assertEquals(2, m.getAmount().scale());
    }

    @Test
    void testMoneyArithmetic() {
        Money a = Money.of("100.50");
        Money b = Money.of("50.25");

        assertEquals(Money.of("150.75"), a.add(b));
        assertEquals(Money.of("50.25"), a.subtract(b));
    }

    @Test
    void testMoneyMultiplyDivide() {
        Money m = Money.of("100.00");
        assertEquals(Money.of("15.00"), m.multiply(new BigDecimal("0.15")));
        assertEquals(Money.of("33.33"), m.divide(new BigDecimal("3")));
    }

    @Test
    void testMoneyComparisons() {
        assertTrue(Money.of("100.00").isPositive());
        assertTrue(Money.of("-100.00").isNegative());
        assertTrue(Money.zero().isZero());
        assertTrue(Money.of("100.00").isGreaterThan(Money.of("50.00")));
        assertTrue(Money.of("50.00").isLessThan(Money.of("100.00")));
    }

    @Test
    void testMoneyRoundingHalfUp() {
        Money m = Money.of(new BigDecimal("10.005"));
        assertEquals(new BigDecimal("10.01"), m.getAmount());
    }

    @Test
    void testCardNumber() {
        CardNumber cn = CardNumber.of("1234567890123456");
        assertEquals("1234567890123456", cn.getValue());
        assertEquals("****-****-****-3456", cn.masked());
    }

    @Test
    void testCardNumberInvalidLength() {
        assertThrows(IllegalArgumentException.class, () -> CardNumber.of("12345"));
        assertThrows(IllegalArgumentException.class, () -> CardNumber.of(null));
    }

    @Test
    void testAccountId() {
        AccountId id = AccountId.of(12345678901L);
        assertEquals(12345678901L, id.getValue());
        assertEquals("12345678901", id.toString());
    }

    @Test
    void testAccountIdFormatting() {
        AccountId id = AccountId.of(1L);
        assertEquals("00000000001", id.toString());
    }

    @Test
    void testCustomerId() {
        CustomerId id = CustomerId.of(123456789L);
        assertEquals(123456789L, id.getValue());
        assertEquals("123456789", id.toString());
    }

    @Test
    void testCustomerIdFormatting() {
        CustomerId id = CustomerId.of(1L);
        assertEquals("000000001", id.toString());
    }

    @Test
    void testTransactionId() {
        TransactionId id = TransactionId.of("0000000000683580");
        assertEquals("0000000000683580", id.getValue());
    }

    @Test
    void testTransactionIdInvalid() {
        assertThrows(IllegalArgumentException.class, () -> TransactionId.of(null));
        assertThrows(IllegalArgumentException.class, () -> TransactionId.of(""));
        assertThrows(IllegalArgumentException.class, () -> TransactionId.of("   "));
    }

    @Test
    void testUserType() {
        assertEquals(UserType.ADMIN, UserType.fromCode('A'));
        assertEquals(UserType.REGULAR, UserType.fromCode('U'));
        assertEquals('A', UserType.ADMIN.getCode());
        assertEquals('U', UserType.REGULAR.getCode());
    }

    @Test
    void testUserTypeFromString() {
        assertEquals(UserType.ADMIN, UserType.fromCode("A"));
        assertEquals(UserType.REGULAR, UserType.fromCode("U"));
    }

    @Test
    void testUserTypeInvalid() {
        assertThrows(IllegalArgumentException.class, () -> UserType.fromCode('X'));
        assertThrows(IllegalArgumentException.class, () -> UserType.fromCode("XX"));
    }
}
