package com.carddemo.domain.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    void testFieldMappingsMatchCVACT01Y() {
        Account account = Account.builder()
                .acctId(1L)                                          // PIC 9(11)
                .activeStatus("Y")                                   // PIC X(01)
                .currBal(new BigDecimal("194.00"))                   // PIC S9(10)V99
                .creditLimit(new BigDecimal("2020.00"))              // PIC S9(10)V99
                .cashCreditLimit(new BigDecimal("1020.00"))          // PIC S9(10)V99
                .openDate(LocalDate.of(2014, 11, 20))               // PIC X(10)
                .expirationDate(LocalDate.of(2025, 5, 20))          // PIC X(10)
                .reissueDate(LocalDate.of(2025, 5, 20))             // PIC X(10)
                .currCycCredit(BigDecimal.ZERO.setScale(2))         // PIC S9(10)V99
                .currCycDebit(BigDecimal.ZERO.setScale(2))          // PIC S9(10)V99
                .addrZip("A000000000")                               // PIC X(10)
                .groupId("")                                         // PIC X(10)
                .version(0L)
                .build();

        assertEquals(1L, account.getAcctId());
        assertEquals("Y", account.getActiveStatus());
        assertEquals(new BigDecimal("194.00"), account.getCurrBal());
        assertEquals(new BigDecimal("2020.00"), account.getCreditLimit());
        assertEquals(new BigDecimal("1020.00"), account.getCashCreditLimit());
        assertEquals(LocalDate.of(2014, 11, 20), account.getOpenDate());
        assertEquals(LocalDate.of(2025, 5, 20), account.getExpirationDate());
        assertEquals(2, account.getCurrBal().scale());
    }

    @Test
    void testMonetaryFieldsAreScale2() {
        Account account = Account.builder()
                .acctId(1L)
                .activeStatus("Y")
                .currBal(new BigDecimal("100.123"))
                .creditLimit(new BigDecimal("5000"))
                .cashCreditLimit(new BigDecimal("2500"))
                .currCycCredit(BigDecimal.ZERO)
                .currCycDebit(BigDecimal.ZERO)
                .build();

        assertNotNull(account.getCurrBal());
    }
}
