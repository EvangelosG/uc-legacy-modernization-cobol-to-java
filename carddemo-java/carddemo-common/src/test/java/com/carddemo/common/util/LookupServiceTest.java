package com.carddemo.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LookupServiceTest {

    @Test
    void testValidPhoneAreaCode() {
        assertTrue(LookupService.isValidPhoneAreaCode("201"));
        assertTrue(LookupService.isValidPhoneAreaCode("212"));
        assertTrue(LookupService.isValidPhoneAreaCode("415"));
    }

    @Test
    void testInvalidPhoneAreaCode() {
        assertFalse(LookupService.isValidPhoneAreaCode("000"));
        assertFalse(LookupService.isValidPhoneAreaCode(null));
    }

    @Test
    void testPhoneAreaCodesLoaded() {
        // CSLKPCDY has 980 entries but 490 unique area codes (duplicates across ranges)
        assertEquals(490, LookupService.getPhoneAreaCodes().size());
    }

    @Test
    void testValidUsStateCode() {
        assertTrue(LookupService.isValidUsStateCode("NY"));
        assertTrue(LookupService.isValidUsStateCode("CA"));
        assertTrue(LookupService.isValidUsStateCode("TX"));
        assertTrue(LookupService.isValidUsStateCode("DC"));
        assertTrue(LookupService.isValidUsStateCode("PR"));
    }

    @Test
    void testInvalidUsStateCode() {
        assertFalse(LookupService.isValidUsStateCode("XX"));
        assertFalse(LookupService.isValidUsStateCode(null));
    }

    @Test
    void testUsStateCodesLoaded() {
        // 50 states + DC + 5 territories = 56
        assertEquals(56, LookupService.getUsStateCodes().size());
    }

    @Test
    void testValidStateZipPrefix() {
        assertTrue(LookupService.isValidStateZipPrefix("NY", "10"));
        assertTrue(LookupService.isValidStateZipPrefix("CA", "90"));
    }

    @Test
    void testInvalidStateZipPrefix() {
        assertFalse(LookupService.isValidStateZipPrefix("NY", "99"));
        assertFalse(LookupService.isValidStateZipPrefix(null, "10"));
        assertFalse(LookupService.isValidStateZipPrefix("NY", null));
    }
}
