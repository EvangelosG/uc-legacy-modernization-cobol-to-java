package com.carddemo.domain.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void testFieldMappingsMatchCVTRA05Y() {
        Transaction txn = Transaction.builder()
                .tranId("0000000000683580")          // PIC X(16)
                .typeCd("01")                        // PIC X(02)
                .catCd(1)                            // PIC 9(04)
                .source("POS TERM")                  // PIC X(10)
                .description("Purchase at Store")    // PIC X(100)
                .amount(new BigDecimal("504.77"))    // PIC S9(09)V99
                .merchantId(800000000L)              // PIC 9(09)
                .merchantName("Abshire-Lowe")        // PIC X(50)
                .merchantCity("North Enoshaven")     // PIC X(50)
                .merchantZip("72112")                // PIC X(10)
                .cardNum("4859452612877065")          // PIC X(16)
                .origTs(LocalDateTime.of(2022, 6, 10, 19, 27, 53))  // PIC X(26)
                .build();

        assertEquals("0000000000683580", txn.getTranId());
        assertEquals("01", txn.getTypeCd());
        assertEquals(1, txn.getCatCd());
        assertEquals(new BigDecimal("504.77"), txn.getAmount());
        assertEquals(2, txn.getAmount().scale());
        assertEquals("4859452612877065", txn.getCardNum());
    }
}
