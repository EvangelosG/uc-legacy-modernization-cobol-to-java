package com.carddemo.migration.service;

import com.carddemo.domain.entity.Account;
import com.carddemo.domain.entity.Card;
import com.carddemo.domain.entity.CardCrossReference;
import com.carddemo.domain.entity.Customer;
import com.carddemo.domain.entity.DisclosureGroup;
import com.carddemo.domain.entity.TransactionCategory;
import com.carddemo.domain.entity.TransactionCategoryBalance;
import com.carddemo.domain.entity.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class VsamParserTest {

    private final VsamToPostgresqlMigrator migrator = new VsamToPostgresqlMigrator(
            null, null, null, null, null, null, null, null, null, null);

    @Test
    void testParseSignedDecimalPositiveZero() {
        // '{' = positive 0
        assertEquals(new BigDecimal("194.00"),
                VsamToPostgresqlMigrator.parseSignedDecimal("00000001940{", 0, 12, 2));
        assertEquals(new BigDecimal("0.00"),
                VsamToPostgresqlMigrator.parseSignedDecimal("00000000000{", 0, 12, 2));
    }

    @Test
    void testParseSignedDecimalPositiveLetters() {
        // 'A'-'I' = positive 1-9
        // G = positive 7
        BigDecimal result = VsamToPostgresqlMigrator.parseSignedDecimal("0000005047G", 0, 11, 2);
        assertEquals(new BigDecimal("504.77"), result);
    }

    @Test
    void testParseSignedDecimalNegative() {
        // '}' = negative 0, 'J'-'R' = negative 1-9
        assertEquals(new BigDecimal("-194.00"),
                VsamToPostgresqlMigrator.parseSignedDecimal("00000001940}", 0, 12, 2));
        // J = negative 1
        assertEquals(new BigDecimal("-504.71"),
                VsamToPostgresqlMigrator.parseSignedDecimal("0000005047J", 0, 11, 2));
    }

    @Test
    void testParseAccountFromActualData() {
        // First line from acctdata.txt
        String line = "00000000001Y00000001940{00000020200{00000010200{" +
                "2014-11-202025-05-202025-05-2000000000000{00000000000{A000000000" +
                " ".repeat(188);

        Account account = migrator.parseAccount(line);
        assertEquals(1L, account.getAcctId());
        assertEquals("Y", account.getActiveStatus());
        assertEquals(new BigDecimal("194.00"), account.getCurrBal());
        assertEquals(new BigDecimal("2020.00"), account.getCreditLimit());
        assertEquals(new BigDecimal("1020.00"), account.getCashCreditLimit());
        assertEquals(LocalDate.of(2014, 11, 20), account.getOpenDate());
        assertEquals(LocalDate.of(2025, 5, 20), account.getExpirationDate());
        assertEquals(LocalDate.of(2025, 5, 20), account.getReissueDate());
        assertEquals(new BigDecimal("0.00"), account.getCurrCycCredit());
        assertEquals(new BigDecimal("0.00"), account.getCurrCycDebit());
        assertEquals("A000000000", account.getAddrZip());
    }

    @Test
    void testParseCustomer() {
        StringBuilder sb = new StringBuilder();
        sb.append("000000001");
        sb.append(padRight("Immanuel", 25));
        sb.append(padRight("Madeline", 25));
        sb.append(padRight("Kessler", 25));
        sb.append(padRight("618 Deshaun Route", 50));
        sb.append(padRight("Apt. 802", 50));
        sb.append(padRight("Altenwerthshire", 50));
        sb.append("NC");
        sb.append("USA");
        sb.append(padRight("12546", 10));
        sb.append(padRight("(908)119-8310", 15));
        sb.append(padRight("(373)693-8684", 15));
        sb.append("020973888");
        sb.append(padRight("000000000", 20));
        sb.append("1961-06-08");
        sb.append(padRight("0053581756", 10));
        sb.append("Y");
        sb.append("274");
        sb.append(" ".repeat(168));

        Customer customer = migrator.parseCustomer(sb.toString());
        assertEquals(1L, customer.getCustId());
        assertEquals("Immanuel", customer.getFirstName());
        assertEquals("Madeline", customer.getMiddleName());
        assertEquals("Kessler", customer.getLastName());
        assertEquals("618 Deshaun Route", customer.getAddrLine1());
        assertEquals("NC", customer.getAddrStateCd());
        assertEquals("USA", customer.getAddrCountryCd());
        assertEquals("12546", customer.getAddrZip());
        assertEquals("020973888", customer.getSsn());
        assertEquals(LocalDate.of(1961, 6, 8), customer.getDob());
        assertEquals("Y", customer.getPriCardHolderInd());
        assertEquals(274, customer.getFicoCreditScore());
    }

    @Test
    void testParseCard() {
        String line = "0500024453765740" + "00000000050" + "747" +
                padRight("Aniya Von", 50) + "2023-03-09" + "Y" + " ".repeat(59);

        Card card = migrator.parseCard(line);
        assertEquals("0500024453765740", card.getCardNum());
        assertEquals(50L, card.getAcctId());
        assertEquals(747, card.getCvvCd());
        assertEquals("Aniya Von", card.getEmbossedName());
        assertEquals(LocalDate.of(2023, 3, 9), card.getExpirationDate());
        assertEquals("Y", card.getActiveStatus());
    }

    @Test
    void testParseCardXref() {
        String line = "0500024453765740" + "000000050" + "00000000050" + " ".repeat(14);

        CardCrossReference xref = migrator.parseCardXref(line);
        assertEquals("0500024453765740", xref.getCardNum());
        assertEquals(50L, xref.getCustId());
        assertEquals(50L, xref.getAcctId());
    }

    @Test
    void testParseTransactionType() {
        String line = "01" + padRight("Purchase", 50) + "00000000";

        TransactionType tt = migrator.parseTransactionType(line);
        assertEquals("01", tt.getTypeCd());
        assertEquals("Purchase", tt.getTypeDesc());
    }

    @Test
    void testParseTransactionCategory() {
        String line = "01" + "0001" + padRight("Regular Sales Draft", 50) + "0000";

        TransactionCategory tc = migrator.parseTransactionCategory(line);
        assertEquals("01", tc.getTypeCd());
        assertEquals(1, tc.getCatCd());
        assertEquals("Regular Sales Draft", tc.getTypeDesc());
    }

    @Test
    void testParseDisclosureGroup() {
        // From actual discgrp.txt: A00000000001000100150{...
        String line = "A000000000" + "01" + "0001" + "00150{" + " ".repeat(28);

        DisclosureGroup dg = migrator.parseDisclosureGroup(line);
        assertEquals("A000000000", dg.getGroupId());
        assertEquals("01", dg.getTranTypeCd());
        assertEquals(1, dg.getTranCatCd());
        assertEquals(new BigDecimal("15.00"), dg.getIntRate());
    }

    @Test
    void testParseCategoryBalance() {
        // From actual tcatbal.txt: 000000000010100010000000000{...
        String line = "00000000001" + "01" + "0001" + "0000000000{" + " ".repeat(22);

        TransactionCategoryBalance cb = migrator.parseCategoryBalance(line);
        assertEquals(1L, cb.getAcctId());
        assertEquals("01", cb.getTypeCd());
        assertEquals(1, cb.getCatCd());
        assertEquals(new BigDecimal("0.00"), cb.getBalance());
    }

    @Test
    void testSubstrBeyondEnd() {
        assertEquals("ab   ", VsamToPostgresqlMigrator.substr("ab", 0, 5));
        assertEquals("     ", VsamToPostgresqlMigrator.substr("ab", 10, 5));
    }

    private static String padRight(String s, int len) {
        if (s.length() >= len) return s.substring(0, len);
        return s + " ".repeat(len - s.length());
    }
}
