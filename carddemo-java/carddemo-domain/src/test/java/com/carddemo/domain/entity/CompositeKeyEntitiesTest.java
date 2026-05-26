package com.carddemo.domain.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CompositeKeyEntitiesTest {

    @Test
    void testTransactionCategoryBalanceMatchesCVTRA01Y() {
        TransactionCategoryBalance tcb = TransactionCategoryBalance.builder()
                .acctId(1L)                               // TRANCAT-ACCT-ID PIC 9(11)
                .typeCd("01")                             // TRANCAT-TYPE-CD PIC X(02)
                .catCd(1)                                 // TRANCAT-CD PIC 9(04)
                .balance(new BigDecimal("0.00"))          // TRAN-CAT-BAL PIC S9(09)V99
                .build();

        assertEquals(1L, tcb.getAcctId());
        assertEquals("01", tcb.getTypeCd());
        assertEquals(1, tcb.getCatCd());
        assertEquals(new BigDecimal("0.00"), tcb.getBalance());
    }

    @Test
    void testDisclosureGroupMatchesCVTRA02Y() {
        DisclosureGroup dg = DisclosureGroup.builder()
                .groupId("A000000000")                    // DIS-ACCT-GROUP-ID PIC X(10)
                .tranTypeCd("01")                         // DIS-TRAN-TYPE-CD PIC X(02)
                .tranCatCd(1)                             // DIS-TRAN-CAT-CD PIC 9(04)
                .intRate(new BigDecimal("1.50"))          // DIS-INT-RATE PIC S9(04)V99
                .build();

        assertEquals("A000000000", dg.getGroupId());
        assertEquals("01", dg.getTranTypeCd());
        assertEquals(1, dg.getTranCatCd());
        assertEquals(new BigDecimal("1.50"), dg.getIntRate());
    }

    @Test
    void testTransactionTypeMatchesCVTRA03Y() {
        TransactionType tt = TransactionType.builder()
                .typeCd("01")                             // TRAN-TYPE PIC X(02)
                .typeDesc("Purchase")                     // TRAN-TYPE-DESC PIC X(50)
                .build();

        assertEquals("01", tt.getTypeCd());
        assertEquals("Purchase", tt.getTypeDesc());
    }

    @Test
    void testTransactionCategoryMatchesCVTRA04Y() {
        TransactionCategory tc = TransactionCategory.builder()
                .typeCd("01")                             // TRAN-TYPE-CD PIC X(02)
                .catCd(1)                                 // TRAN-CAT-CD PIC 9(04)
                .typeDesc("Regular Sales Draft")          // TRAN-CAT-TYPE-DESC PIC X(50)
                .build();

        assertEquals("01", tc.getTypeCd());
        assertEquals(1, tc.getCatCd());
        assertEquals("Regular Sales Draft", tc.getTypeDesc());
    }

    @Test
    void testCardCrossReferenceMatchesCVACT03Y() {
        CardCrossReference xref = CardCrossReference.builder()
                .cardNum("0500024453765740")              // XREF-CARD-NUM PIC X(16)
                .custId(500L)                             // XREF-CUST-ID PIC 9(09)
                .acctId(50L)                              // XREF-ACCT-ID PIC 9(11)
                .build();

        assertEquals("0500024453765740", xref.getCardNum());
        assertEquals(500L, xref.getCustId());
        assertEquals(50L, xref.getAcctId());
    }

    @Test
    void testCompositeKeyEquality() {
        TransactionCategoryBalanceId id1 = new TransactionCategoryBalanceId(1L, "01", 1);
        TransactionCategoryBalanceId id2 = new TransactionCategoryBalanceId(1L, "01", 1);
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());

        DisclosureGroupId dgId1 = new DisclosureGroupId("A000000000", "01", 1);
        DisclosureGroupId dgId2 = new DisclosureGroupId("A000000000", "01", 1);
        assertEquals(dgId1, dgId2);
    }
}
