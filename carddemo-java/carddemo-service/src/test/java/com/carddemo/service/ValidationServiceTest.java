package com.carddemo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationServiceTest {

    private ValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new ValidationService();
    }

    // ── SSN validation ──

    @Test
    void validateSSNAcceptsValidSSN() {
        var result = validationService.validateSSN("123456789");
        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void validateSSNAcceptsSSNWithDashes() {
        var result = validationService.validateSSN("123-45-6789");
        assertThat(result.valid()).isTrue();
    }

    @Test
    void validateSSNRejectsNull() {
        var result = validationService.validateSSN(null);
        assertThat(result.valid()).isFalse();
    }

    @Test
    void validateSSNRejectsBlank() {
        var result = validationService.validateSSN("   ");
        assertThat(result.valid()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"000456789", "666456789", "900456789", "999456789"})
    void validateSSNRejectsInvalidAreaCodes(String ssn) {
        var result = validationService.validateSSN(ssn);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("area code"));
    }

    @Test
    void validateSSNRejectsZeroGroupNumber() {
        var result = validationService.validateSSN("123006789");
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("group number"));
    }

    @Test
    void validateSSNRejectsZeroSerialNumber() {
        var result = validationService.validateSSN("123450000");
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("serial number"));
    }

    @Test
    void validateSSNRejectsNonNumeric() {
        var result = validationService.validateSSN("12345678A");
        assertThat(result.valid()).isFalse();
    }

    @Test
    void validateSSNRejectsTooShort() {
        var result = validationService.validateSSN("12345");
        assertThat(result.valid()).isFalse();
    }

    // ── Area code lookup ──

    @Test
    void lookupAreaCodeAcceptsValidCode() {
        var result = validationService.lookupAreaCode("212");
        assertThat(result.valid()).isTrue();
    }

    @Test
    void lookupAreaCodeRejectsInvalidCode() {
        var result = validationService.lookupAreaCode("000");
        assertThat(result.valid()).isFalse();
    }

    @Test
    void lookupAreaCodeRejectsNull() {
        var result = validationService.lookupAreaCode(null);
        assertThat(result.valid()).isFalse();
    }

    @Test
    void lookupAreaCodeRejectsNonNumeric() {
        var result = validationService.lookupAreaCode("abc");
        assertThat(result.valid()).isFalse();
    }

    @Test
    void lookupAreaCodeRejectsTooShort() {
        var result = validationService.lookupAreaCode("21");
        assertThat(result.valid()).isFalse();
    }

    // ── State code validation ──

    @Test
    void validateStateCodeAcceptsValid() {
        var result = validationService.validateStateCode("NY");
        assertThat(result.valid()).isTrue();
    }

    @Test
    void validateStateCodeRejectsInvalid() {
        var result = validationService.validateStateCode("ZZ");
        assertThat(result.valid()).isFalse();
    }

    @Test
    void validateStateCodeRejectsNull() {
        var result = validationService.validateStateCode(null);
        assertThat(result.valid()).isFalse();
    }

    // ── Date range validation ──

    @Test
    void validateDateRangeAcceptsValidRange() {
        var result = validationService.validateDateRange(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        assertThat(result.valid()).isTrue();
    }

    @Test
    void validateDateRangeAcceptsSameDate() {
        LocalDate date = LocalDate.of(2025, 6, 15);
        var result = validationService.validateDateRange(date, date);
        assertThat(result.valid()).isTrue();
    }

    @Test
    void validateDateRangeRejectsEndBeforeStart() {
        var result = validationService.validateDateRange(
                LocalDate.of(2025, 12, 31), LocalDate.of(2025, 1, 1));
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("before"));
    }

    @Test
    void validateDateRangeRejectsNullStart() {
        var result = validationService.validateDateRange(null, LocalDate.of(2025, 12, 31));
        assertThat(result.valid()).isFalse();
    }

    // ── Phone number parsing ──

    @Test
    void parsePhoneAcceptsValidNumber() {
        var result = validationService.parseUSPhoneNumber("(212)555-1234");
        assertThat(result.valid()).isTrue();
        assertThat(result.areaCode()).isEqualTo("212");
        assertThat(result.exchange()).isEqualTo("555");
        assertThat(result.subscriber()).isEqualTo("1234");
    }

    @Test
    void parsePhoneAcceptsDigitsOnly() {
        var result = validationService.parseUSPhoneNumber("2125551234");
        assertThat(result.valid()).isTrue();
    }

    @Test
    void parsePhoneRejectsNull() {
        var result = validationService.parseUSPhoneNumber(null);
        assertThat(result.valid()).isFalse();
    }

    @Test
    void parsePhoneRejectsTooFewDigits() {
        var result = validationService.parseUSPhoneNumber("555-1234");
        assertThat(result.valid()).isFalse();
    }
}
