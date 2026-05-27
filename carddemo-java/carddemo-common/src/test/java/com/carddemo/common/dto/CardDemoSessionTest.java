package com.carddemo.common.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CardDemoSessionTest {

    @Test
    void testBuilderAndFields() {
        CardDemoSession session = CardDemoSession.builder()
                .fromTransactionId("CC00")
                .fromProgram("COSGN00C")
                .toProgram("COMEN01C")
                .userId("USER0001")
                .userType("U")
                .programContext(0)
                .customerId(123456789L)
                .accountId(12345678901L)
                .cardNumber("4000123456789012")
                .lastMap("COSGN0A")
                .lastMapset("COSGN00")
                .build();

        assertEquals("CC00", session.getFromTransactionId());
        assertEquals("COSGN00C", session.getFromProgram());
        assertEquals("COMEN01C", session.getToProgram());
        assertEquals("USER0001", session.getUserId());
        assertEquals("U", session.getUserType());
        assertEquals(0, session.getProgramContext());
        assertEquals(123456789L, session.getCustomerId());
        assertEquals(12345678901L, session.getAccountId());
        assertEquals("4000123456789012", session.getCardNumber());
    }

    @Test
    void testIsAdmin() {
        CardDemoSession admin = CardDemoSession.builder().userType("A").build();
        CardDemoSession regular = CardDemoSession.builder().userType("U").build();

        assertTrue(admin.isAdmin());
        assertFalse(admin.isRegularUser());
        assertFalse(regular.isAdmin());
        assertTrue(regular.isRegularUser());
    }

    @Test
    void testProgramContext() {
        CardDemoSession enter = CardDemoSession.builder().programContext(0).build();
        CardDemoSession reenter = CardDemoSession.builder().programContext(1).build();
        CardDemoSession nullCtx = CardDemoSession.builder().build();

        assertTrue(enter.isEntering());
        assertFalse(enter.isReentering());
        assertFalse(reenter.isEntering());
        assertTrue(reenter.isReentering());
        assertTrue(nullCtx.isEntering());
    }
}
