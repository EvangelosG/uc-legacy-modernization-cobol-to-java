package com.carddemo.service;

import com.carddemo.common.util.LookupService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Replaces inline validation logic from COACTUPC.cbl (4,236 LOC).
 * Extracts SSN validation, phone/area code parsing, state validation,
 * and date range validation into reusable methods.
 */
@Service
public class ValidationService {

    private static final Pattern NUMERIC = Pattern.compile("\\d+");
    private static final Set<Integer> INVALID_SSN_AREA_CODES = Set.of(0, 666);

    /**
     * Validates a US Social Security Number (9 digits).
     * Replaces inline SSN parsing in COACTUPC lines 117-146.
     * Rules from COBOL:
     *  - Part 1 (area, 3 digits): cannot be 000, 666, or 900-999
     *  - Part 2 (group, 2 digits): cannot be 00
     *  - Part 3 (serial, 4 digits): cannot be 0000
     */
    public ValidationResult validateSSN(String ssn) {
        List<String> errors = new ArrayList<>();

        if (ssn == null || ssn.isBlank()) {
            errors.add("SSN is required");
            return new ValidationResult(false, errors);
        }

        String cleaned = ssn.replaceAll("[\\s-]", "");

        if (cleaned.length() != 9 || !NUMERIC.matcher(cleaned).matches()) {
            errors.add("SSN must be exactly 9 digits");
            return new ValidationResult(false, errors);
        }

        int part1 = Integer.parseInt(cleaned.substring(0, 3));
        int part2 = Integer.parseInt(cleaned.substring(3, 5));
        int part3 = Integer.parseInt(cleaned.substring(5, 9));

        if (INVALID_SSN_AREA_CODES.contains(part1) || part1 >= 900) {
            errors.add("SSN area code (first 3 digits) is invalid");
        }
        if (part2 == 0) {
            errors.add("SSN group number (digits 4-5) cannot be 00");
        }
        if (part3 == 0) {
            errors.add("SSN serial number (digits 6-9) cannot be 0000");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Looks up and validates a US phone area code using the CSLKPCDY lookup table.
     * Delegates to LookupService from Phase 0.
     */
    public ValidationResult lookupAreaCode(String areaCode) {
        List<String> errors = new ArrayList<>();

        if (areaCode == null || areaCode.isBlank()) {
            errors.add("Area code is required");
            return new ValidationResult(false, errors);
        }

        String cleaned = areaCode.trim();
        if (cleaned.length() != 3 || !NUMERIC.matcher(cleaned).matches()) {
            errors.add("Area code must be exactly 3 digits");
            return new ValidationResult(false, errors);
        }

        if (!LookupService.isValidPhoneAreaCode(cleaned)) {
            errors.add("Area code " + cleaned + " is not a valid US area code");
            return new ValidationResult(false, errors);
        }

        return new ValidationResult(true, List.of());
    }

    /**
     * Validates a US state code using the LookupService.
     */
    public ValidationResult validateStateCode(String stateCode) {
        List<String> errors = new ArrayList<>();

        if (stateCode == null || stateCode.isBlank()) {
            errors.add("State code is required");
            return new ValidationResult(false, errors);
        }

        if (!LookupService.isValidUsStateCode(stateCode.trim())) {
            errors.add("Invalid US state code: " + stateCode.trim());
            return new ValidationResult(false, errors);
        }

        return new ValidationResult(true, List.of());
    }

    /**
     * Validates that a date range is logically correct.
     * Uses DateUtil from Phase 0 for date validation support.
     */
    public ValidationResult validateDateRange(LocalDate startDate, LocalDate endDate) {
        List<String> errors = new ArrayList<>();

        if (startDate == null) {
            errors.add("Start date is required");
        }
        if (endDate == null) {
            errors.add("End date is required");
        }
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            errors.add("End date must not be before start date");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Parses and validates a US phone number in format (XXX)XXX-XXXX.
     * Replaces COACTUPC inline phone parsing logic.
     */
    public PhoneParseResult parseUSPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return new PhoneParseResult(false, null, null, null,
                    List.of("Phone number is required"));
        }

        String digits = phoneNumber.replaceAll("[^0-9]", "");
        if (digits.length() != 10) {
            return new PhoneParseResult(false, null, null, null,
                    List.of("Phone number must contain exactly 10 digits"));
        }

        String areaCode = digits.substring(0, 3);
        String exchange = digits.substring(3, 6);
        String subscriber = digits.substring(6, 10);

        List<String> errors = new ArrayList<>();

        if (!LookupService.isValidPhoneAreaCode(areaCode)) {
            errors.add("Invalid area code: " + areaCode);
        }

        if (!NUMERIC.matcher(exchange).matches() || Integer.parseInt(exchange) == 0) {
            errors.add("Invalid exchange: " + exchange);
        }

        if (!NUMERIC.matcher(subscriber).matches() || Integer.parseInt(subscriber) == 0) {
            errors.add("Invalid subscriber number: " + subscriber);
        }

        return new PhoneParseResult(errors.isEmpty(), areaCode, exchange, subscriber, errors);
    }

    public record ValidationResult(boolean valid, List<String> errors) {}

    public record PhoneParseResult(
            boolean valid,
            String areaCode,
            String exchange,
            String subscriber,
            List<String> errors
    ) {}
}
