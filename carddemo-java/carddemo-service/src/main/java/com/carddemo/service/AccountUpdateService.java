package com.carddemo.service;

import com.carddemo.domain.entity.Account;
import com.carddemo.domain.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Replaces account update logic from COACTUPC.cbl.
 * Validates account fields: credit limit, cash advance limit,
 * status, expiration date, reissue date.
 * Uses @Version for optimistic locking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountUpdateService {

    private static final Set<String> VALID_STATUSES = Set.of("Y", "N");

    private final AccountRepository accountRepository;

    @Transactional
    public Account updateAccount(Long accountId, AccountUpdateCommand command) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new TransactionTypeService.ResourceNotFoundException(
                        "Account not found: " + accountId));

        if (command.version() != null && !command.version().equals(account.getVersion())) {
            throw new CardService.OptimisticLockConflictException(
                    "Account has been modified by another user. Expected version "
                            + command.version() + " but found " + account.getVersion());
        }

        List<String> errors = new ArrayList<>();

        if (command.creditLimit() != null) {
            validateCreditLimit(command.creditLimit(), errors);
            account.setCreditLimit(command.creditLimit());
        }

        if (command.cashCreditLimit() != null) {
            BigDecimal effectiveCreditLimit = command.creditLimit() != null
                    ? command.creditLimit() : account.getCreditLimit();
            validateCashCreditLimit(command.cashCreditLimit(), effectiveCreditLimit, errors);
            account.setCashCreditLimit(command.cashCreditLimit());
        }

        if (command.activeStatus() != null) {
            validateActiveStatus(command.activeStatus(), errors);
            account.setActiveStatus(command.activeStatus());
        }

        if (command.expirationDate() != null) {
            validateExpirationDate(command.expirationDate(), errors);
            account.setExpirationDate(command.expirationDate());
        }

        if (command.reissueDate() != null) {
            LocalDate effectiveExpiration = command.expirationDate() != null
                    ? command.expirationDate() : account.getExpirationDate();
            validateReissueDate(command.reissueDate(), effectiveExpiration, errors);
            account.setReissueDate(command.reissueDate());
        }

        if (command.groupId() != null) {
            account.setGroupId(command.groupId());
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }

        log.info("Updating account {}: fields changed", accountId);
        return accountRepository.save(account);
    }

    private void validateCreditLimit(BigDecimal creditLimit, List<String> errors) {
        if (creditLimit.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Credit limit must not be negative");
        }
    }

    private void validateCashCreditLimit(BigDecimal cashLimit, BigDecimal creditLimit,
                                          List<String> errors) {
        if (cashLimit.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Cash credit limit must not be negative");
        }
        if (creditLimit != null && cashLimit.compareTo(creditLimit) > 0) {
            errors.add("Cash credit limit cannot exceed credit limit");
        }
    }

    private void validateActiveStatus(String status, List<String> errors) {
        if (!VALID_STATUSES.contains(status)) {
            errors.add("Invalid account status: " + status + ". Must be Y or N");
        }
    }

    private void validateExpirationDate(LocalDate expirationDate, List<String> errors) {
        if (expirationDate.isBefore(LocalDate.now())) {
            errors.add("Expiration date cannot be in the past");
        }
    }

    private void validateReissueDate(LocalDate reissueDate, LocalDate expirationDate,
                                      List<String> errors) {
        if (expirationDate != null && reissueDate.isAfter(expirationDate)) {
            errors.add("Reissue date cannot be after expiration date");
        }
    }

    public record AccountUpdateCommand(
            BigDecimal creditLimit,
            BigDecimal cashCreditLimit,
            String activeStatus,
            LocalDate expirationDate,
            LocalDate reissueDate,
            String groupId,
            Long version
    ) {}
}
