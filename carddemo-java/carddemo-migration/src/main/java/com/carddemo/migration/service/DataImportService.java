package com.carddemo.migration.service;

import com.carddemo.domain.entity.Account;
import com.carddemo.domain.entity.Card;
import com.carddemo.domain.entity.CardCrossReference;
import com.carddemo.domain.entity.Customer;
import com.carddemo.domain.entity.DailyTransaction;
import com.carddemo.domain.entity.DisclosureGroup;
import com.carddemo.domain.entity.TransactionCategory;
import com.carddemo.domain.entity.TransactionCategoryBalance;
import com.carddemo.domain.entity.TransactionType;
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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Replaces CBIMPORT. Imports data from JSON export files.
 * Validates referential integrity before persisting.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataImportService {

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

    private static final ObjectMapper MAPPER = createMapper();

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    @Transactional
    public ImportResult importData(Path accountsFile, Path customersFile, Path cardsFile,
                                   Path cardXrefFile, Path dailyTransactionsFile,
                                   Path transactionsFile, Path disclosureGroupsFile,
                                   Path categoryBalancesFile, Path transactionTypesFile,
                                   Path transactionCategoriesFile) throws IOException {
        ImportResult result = new ImportResult();

        List<Account> accounts = readFile(accountsFile, new TypeReference<>() {});
        List<Customer> customers = readFile(customersFile, new TypeReference<>() {});
        List<Card> cards = readFile(cardsFile, new TypeReference<>() {});
        List<CardCrossReference> xrefs = readFile(cardXrefFile, new TypeReference<>() {});
        List<DailyTransaction> dailyTrans = readFile(dailyTransactionsFile, new TypeReference<>() {});
        List<com.carddemo.domain.entity.Transaction> transactions =
                readFile(transactionsFile, new TypeReference<>() {});
        List<DisclosureGroup> discGroups = readFile(disclosureGroupsFile, new TypeReference<>() {});
        List<TransactionCategoryBalance> catBals = readFile(categoryBalancesFile, new TypeReference<>() {});
        List<TransactionType> types = readFile(transactionTypesFile, new TypeReference<>() {});
        List<TransactionCategory> categories = readFile(transactionCategoriesFile, new TypeReference<>() {});

        // Validate referential integrity
        Set<Long> accountIds = accounts.stream().map(Account::getAcctId).collect(Collectors.toSet());
        Set<Long> customerIds = customers.stream().map(Customer::getCustId).collect(Collectors.toSet());

        List<String> errors = new ArrayList<>();
        validateXrefs(xrefs, accountIds, customerIds, errors);
        validateDailyTransactions(dailyTrans, xrefs, accountIds, errors);
        validateTransactions(transactions, xrefs, accountIds, errors);

        if (!errors.isEmpty()) {
            result.setErrors(errors);
            log.warn("Referential integrity validation found {} issues", errors.size());
        }

        // Persist in dependency order
        result.put("accounts", persistAll(accountRepository, accounts));
        result.put("customers", persistAll(customerRepository, customers));
        result.put("cards", persistAll(cardRepository, cards));
        result.put("card_xref", persistAll(cardCrossReferenceRepository, xrefs));
        result.put("daily_transactions", persistAll(dailyTransactionRepository, dailyTrans));
        result.put("transactions", persistAll(transactionRepository, transactions));
        result.put("disclosure_groups", persistAll(disclosureGroupRepository, discGroups));
        result.put("category_balances", persistAll(categoryBalanceRepository, catBals));
        result.put("transaction_types", persistAll(transactionTypeRepository, types));
        result.put("transaction_categories", persistAll(transactionCategoryRepository, categories));

        log.info("Import complete. Total records: {}", result.totalRecords());
        return result;
    }

    private void validateXrefs(List<CardCrossReference> xrefs, Set<Long> accountIds,
                               Set<Long> customerIds, List<String> errors) {
        for (CardCrossReference xref : xrefs) {
            if (!accountIds.contains(xref.getAcctId())) {
                errors.add("Card xref " + xref.getCardNum() +
                        " references non-existent account " + xref.getAcctId());
            }
            if (!customerIds.contains(xref.getCustId())) {
                errors.add("Card xref " + xref.getCardNum() +
                        " references non-existent customer " + xref.getCustId());
            }
        }
    }

    private void validateDailyTransactions(List<DailyTransaction> transactions,
                                           List<CardCrossReference> xrefs,
                                           Set<Long> accountIds,
                                           List<String> errors) {
        Set<String> xrefCardNums = xrefs.stream()
                .map(CardCrossReference::getCardNum).collect(Collectors.toSet());
        for (DailyTransaction txn : transactions) {
            if (!xrefCardNums.contains(txn.getCardNum())) {
                errors.add("Daily transaction " + txn.getTranId() +
                        " references card " + txn.getCardNum() + " with no xref");
            }
        }
    }

    private void validateTransactions(List<com.carddemo.domain.entity.Transaction> transactions,
                                      List<CardCrossReference> xrefs,
                                      Set<Long> accountIds,
                                      List<String> errors) {
        Set<String> xrefCardNums = xrefs.stream()
                .map(CardCrossReference::getCardNum).collect(Collectors.toSet());
        for (com.carddemo.domain.entity.Transaction txn : transactions) {
            if (!xrefCardNums.contains(txn.getCardNum())) {
                errors.add("Transaction " + txn.getTranId() +
                        " references card " + txn.getCardNum() + " with no xref");
            }
        }
    }

    private <T> int persistAll(org.springframework.data.jpa.repository.JpaRepository<T, ?> repo,
                               List<T> entities) {
        if (entities.isEmpty()) return 0;
        repo.saveAll(entities);
        return entities.size();
    }

    private <T> List<T> readFile(Path file, TypeReference<List<T>> type) throws IOException {
        if (file == null || !file.toFile().exists()) {
            return List.of();
        }
        return MAPPER.readValue(file.toFile(), type);
    }

    public static class ImportResult {
        private final Map<String, Integer> counts = new LinkedHashMap<>();
        private List<String> errors = new ArrayList<>();

        public void put(String entity, int count) {
            counts.put(entity, count);
        }

        public int getCount(String entity) {
            return counts.getOrDefault(entity, 0);
        }

        public Map<String, Integer> getCounts() {
            return counts;
        }

        public int totalRecords() {
            return counts.values().stream().mapToInt(Integer::intValue).sum();
        }

        public List<String> getErrors() {
            return errors;
        }

        public void setErrors(List<String> errors) {
            this.errors = errors;
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }
}
