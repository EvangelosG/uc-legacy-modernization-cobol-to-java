package com.carddemo.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CardTest {

    @Test
    void testFieldMappingsMatchCVACT02Y() {
        Card card = Card.builder()
                .cardNum("0500024453765740")     // PIC X(16)
                .acctId(50L)                      // PIC 9(11)
                .cvvCd(747)                       // PIC 9(03)
                .embossedName("Aniya Von")        // PIC X(50)
                .expirationDate(LocalDate.of(2023, 3, 9)) // PIC X(10)
                .activeStatus("Y")               // PIC X(01)
                .build();

        assertEquals("0500024453765740", card.getCardNum());
        assertEquals(50L, card.getAcctId());
        assertEquals(747, card.getCvvCd());
        assertEquals("Aniya Von", card.getEmbossedName());
        assertEquals(LocalDate.of(2023, 3, 9), card.getExpirationDate());
        assertEquals("Y", card.getActiveStatus());
    }

    @Test
    void testCardNumLength() {
        Card card = Card.builder()
                .cardNum("1234567890123456")
                .acctId(1L)
                .activeStatus("Y")
                .build();
        assertEquals(16, card.getCardNum().length());
    }
}
