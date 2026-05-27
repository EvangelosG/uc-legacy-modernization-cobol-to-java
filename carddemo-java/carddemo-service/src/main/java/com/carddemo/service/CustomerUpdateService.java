package com.carddemo.service;

import com.carddemo.domain.entity.Customer;
import com.carddemo.domain.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Replaces customer PII update logic from COACTUPC.cbl.
 * Validates customer fields: name, address, phone, SSN, DOB.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerUpdateService {

    private final CustomerRepository customerRepository;
    private final ValidationService validationService;

    @Transactional
    public Customer updateCustomer(Long customerId, CustomerUpdateCommand command) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new TransactionTypeService.ResourceNotFoundException(
                        "Customer not found: " + customerId));

        List<String> errors = new ArrayList<>();

        if (command.firstName() != null) {
            validateName(command.firstName(), "First name", errors);
            customer.setFirstName(command.firstName().trim());
        }

        if (command.middleName() != null) {
            customer.setMiddleName(command.middleName().trim());
        }

        if (command.lastName() != null) {
            validateName(command.lastName(), "Last name", errors);
            customer.setLastName(command.lastName().trim());
        }

        if (command.addrLine1() != null) {
            customer.setAddrLine1(command.addrLine1().trim());
        }
        if (command.addrLine2() != null) {
            customer.setAddrLine2(command.addrLine2().trim());
        }
        if (command.addrLine3() != null) {
            customer.setAddrLine3(command.addrLine3().trim());
        }

        if (command.addrStateCd() != null) {
            ValidationService.ValidationResult stateResult =
                    validationService.validateStateCode(command.addrStateCd());
            if (!stateResult.valid()) {
                errors.addAll(stateResult.errors());
            }
            customer.setAddrStateCd(command.addrStateCd().trim().toUpperCase());
        }

        if (command.addrCountryCd() != null) {
            customer.setAddrCountryCd(command.addrCountryCd().trim());
        }

        if (command.addrZip() != null) {
            customer.setAddrZip(command.addrZip().trim());
        }

        if (command.phoneNum1() != null) {
            ValidationService.PhoneParseResult phoneResult =
                    validationService.parseUSPhoneNumber(command.phoneNum1());
            if (!phoneResult.valid()) {
                errors.addAll(phoneResult.errors());
            }
            customer.setPhoneNum1(command.phoneNum1().trim());
        }

        if (command.phoneNum2() != null) {
            if (!command.phoneNum2().isBlank()) {
                ValidationService.PhoneParseResult phoneResult =
                        validationService.parseUSPhoneNumber(command.phoneNum2());
                if (!phoneResult.valid()) {
                    errors.addAll(phoneResult.errors());
                }
            }
            customer.setPhoneNum2(command.phoneNum2().trim());
        }

        if (command.ssn() != null) {
            ValidationService.ValidationResult ssnResult =
                    validationService.validateSSN(command.ssn());
            if (!ssnResult.valid()) {
                errors.addAll(ssnResult.errors());
            }
            customer.setSsn(command.ssn().replaceAll("[\\s-]", ""));
        }

        if (command.dob() != null) {
            validateDob(command.dob(), errors);
            customer.setDob(command.dob());
        }

        if (command.govtIssuedId() != null) {
            customer.setGovtIssuedId(command.govtIssuedId().trim());
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }

        log.info("Updating customer {}: fields changed", customerId);
        return customerRepository.save(customer);
    }

    private void validateName(String name, String fieldName, List<String> errors) {
        if (name.isBlank()) {
            errors.add(fieldName + " must not be blank");
        }
        if (name.length() > 25) {
            errors.add(fieldName + " must not exceed 25 characters");
        }
    }

    private void validateDob(LocalDate dob, List<String> errors) {
        if (dob.isAfter(LocalDate.now())) {
            errors.add("Date of birth cannot be in the future");
        }
    }

    public record CustomerUpdateCommand(
            String firstName,
            String middleName,
            String lastName,
            String addrLine1,
            String addrLine2,
            String addrLine3,
            String addrStateCd,
            String addrCountryCd,
            String addrZip,
            String phoneNum1,
            String phoneNum2,
            String ssn,
            LocalDate dob,
            String govtIssuedId
    ) {}
}
