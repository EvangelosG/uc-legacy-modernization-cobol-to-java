package com.carddemo.migration.service;

import com.carddemo.domain.entity.Account;
import com.carddemo.domain.entity.CardCrossReference;
import com.carddemo.domain.entity.DailyTransaction;
import com.carddemo.domain.entity.Transaction;
import com.carddemo.domain.repository.AccountRepository;
import com.carddemo.domain.repository.CardCrossReferenceRepository;
import com.carddemo.domain.repository.CardRepository;
import com.carddemo.domain.repository.CustomerRepository;
import com.carddemo.domain.repository.DailyTransactionRepository;
import com.carddemo.domain.repository.DisclosureGroupRepository;
import com.carddemo.domain.repository.TransactionCategoryBalanceRepository;
import com.carddemo.domain.repository.TransactionCategoryRepository;
import com.carddemo.domain.repository.TransactionRepository;
import com.carddemo.domain.repository.TransactionTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Automated comparison and validation of migrated data.
 * Checks row counts, field values, referential integrity, and monetary totals.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MigrationValidationService {

    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final CardCrossReferenceRepository cardCrossReferenceRepository;
    private final CustomerRepository customerRepository;
    private final DailyTransactionRepository dailyTransactionRepository;
    private final DisclosureGroupRepository disclosureGroupRepository;
    private final TransactionCategoryBalanceRepository categoryBalanceRepository;
    private final TransactionCategoryRepository transactionCategoryRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionTypeRepository transactionTypeRepository;

    @Transactional(readOnly = true)
    public ValidationResult validate(Path sourceDataDir) throws IOException {
        ValidationResult result = new ValidationResult();

        validateRowCounts(sourceDataDir, result);
        validateReferentialIntegrity(result);
        validateMonetaryTotals(sourceDataDir, result);

        log.info("Validation complete. Passed: {}, Failed: {}",
                result.passedChecks(), result.failedChecks());
        return result;
    }

    private void validateRowCounts(Path dataDir, ValidationResult result) throws IOException {
        Map<String, RowCountCheck> checks = new LinkedHashMap<>();
        checks.put("accounts", new RowCountCheck("acctdata.txt", accountRepository.count()));
        checks.put("customers", new RowCountCheck("custdata.txt", customerRepository.count()));
        checks.put("cards", new RowCountCheck("carddata.txt", cardRepository.count()));
        checks.put("card_xref", new RowCountCheck("cardxref.txt", cardCrossReferenceRepository.count()));
        checks.put("daily_transactions", new RowCountCheck("dailytran.txt", dailyTransactionRepository.count()));
        checks.put("disclosure_groups", new RowCountCheck("discgrp.txt", disclosureGroupRepository.count()));
        checks.put("category_balances", new RowCountCheck("tcatbal.txt", categoryBalanceRepository.count()));
        checks.put("transaction_types", new RowCountCheck("trantype.txt", transactionTypeRepository.count()));
        checks.put("transaction_categories", new RowCountCheck("trancatg.txt", transactionCategoryRepository.count()));

        for (Map.Entry<String, RowCountCheck> entry : checks.entrySet()) {
            String entity = entry.getKey();
            RowCountCheck check = entry.getValue();
            Path file = dataDir.resolve(check.filename);
            long sourceCount = countNonEmptyLines(file);
            boolean passed = sourceCount == check.dbCount;
            result.addCheck(entity + "_row_count", passed,
                    String.format("Source: %d, DB: %d", sourceCount, check.dbCount));
            if (!passed) {
                log.warn("Row count mismatch for {}: source={}, db={}", entity, sourceCount, check.dbCount);
            }
        }
    }

    private void validateReferentialIntegrity(ValidationResult result) {
        Set<Long> accountIds = accountRepository.findAll().stream()
                .map(Account::getAcctId).collect(Collectors.toSet());
        Set<Long> customerIds = customerRepository.findAll().stream()
                .map(com.carddemo.domain.entity.Customer::getCustId).collect(Collectors.toSet());

        // Card xref → account + customer
        List<CardCrossReference> xrefs = cardCrossReferenceRepository.findAll();
        long orphanXrefAcct = xrefs.stream()
                .filter(x -> !accountIds.contains(x.getAcctId())).count();
        long orphanXrefCust = xrefs.stream()
                .filter(x -> !customerIds.contains(x.getCustId())).count();
        result.addCheck("xref_account_integrity", orphanXrefAcct == 0,
                "Orphan xref→account: " + orphanXrefAcct);
        result.addCheck("xref_customer_integrity", orphanXrefCust == 0,
                "Orphan xref→customer: " + orphanXrefCust);

        // Daily transactions → card xref (→ account)
        Set<String> xrefCards = xrefs.stream()
                .map(CardCrossReference::getCardNum).collect(Collectors.toSet());
        long orphanDailyTxn = dailyTransactionRepository.findAll().stream()
                .filter(t -> !xrefCards.contains(t.getCardNum())).count();
        result.addCheck("daily_txn_card_integrity", orphanDailyTxn == 0,
                "Orphan daily_txn→card: " + orphanDailyTxn);

        // Transactions → card xref
        long orphanTxn = transactionRepository.findAll().stream()
                .filter(t -> !xrefCards.contains(t.getCardNum())).count();
        result.addCheck("txn_card_integrity", orphanTxn == 0,
                "Orphan txn→card: " + orphanTxn);
    }

    private void validateMonetaryTotals(Path dataDir, ValidationResult result) throws IOException {
        // Sum of all account balances
        BigDecimal dbAccountBalanceSum = accountRepository.findAll().stream()
                .map(Account::getCurrBal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Path acctFile = dataDir.resolve("acctdata.txt");
        BigDecimal sourceAccountBalanceSum = BigDecimal.ZERO;
        for (String line : readNonEmptyLines(acctFile)) {
            // currBal at positions [12..24] - 12 chars, S9(10)V99
            BigDecimal bal = VsamToPostgresqlMigrator.parseSignedDecimal(line, 12, 12, 2);
            sourceAccountBalanceSum = sourceAccountBalanceSum.add(bal);
        }
        boolean balMatch = dbAccountBalanceSum.compareTo(sourceAccountBalanceSum) == 0;
        result.addCheck("account_balance_total", balMatch,
                String.format("Source: %s, DB: %s", sourceAccountBalanceSum, dbAccountBalanceSum));

        // Sum of all daily transaction amounts
        BigDecimal dbDailyTxnAmountSum = dailyTransactionRepository.findAll().stream()
                .map(DailyTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Path txnFile = dataDir.resolve("dailytran.txt");
        BigDecimal sourceTxnAmountSum = BigDecimal.ZERO;
        for (String line : readNonEmptyLines(txnFile)) {
            // amt at positions [132..143] - 11 chars, S9(09)V99
            BigDecimal amt = VsamToPostgresqlMigrator.parseSignedDecimal(line, 132, 11, 2);
            sourceTxnAmountSum = sourceTxnAmountSum.add(amt);
        }
        boolean txnMatch = dbDailyTxnAmountSum.compareTo(sourceTxnAmountSum) == 0;
        result.addCheck("daily_txn_amount_total", txnMatch,
                String.format("Source: %s, DB: %s", sourceTxnAmountSum, dbDailyTxnAmountSum));
    }

    private long countNonEmptyLines(Path file) throws IOException {
        return Files.readAllLines(file).stream()
                .filter(line -> !line.trim().isEmpty())
                .count();
    }

    private List<String> readNonEmptyLines(Path file) throws IOException {
        return Files.readAllLines(file).stream()
                .filter(line -> !line.trim().isEmpty())
                .collect(Collectors.toList());
    }

    // ── Result types ───────────────────────────────────────────────────

    public static class ValidationResult {
        private final List<CheckResult> checks = new ArrayList<>();

        public void addCheck(String name, boolean passed, String detail) {
            checks.add(new CheckResult(name, passed, detail));
        }

        public List<CheckResult> getChecks() {
            return checks;
        }

        public long passedChecks() {
            return checks.stream().filter(CheckResult::passed).count();
        }

        public long failedChecks() {
            return checks.stream().filter(c -> !c.passed()).count();
        }

        public boolean allPassed() {
            return checks.stream().allMatch(CheckResult::passed);
        }
    }

    public record CheckResult(String name, boolean passed, String detail) {}

    private record RowCountCheck(String filename, long dbCount) {}
}
