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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Replaces CBEXPORT. Exports all entity data to JSON format
 * (separate files per entity type).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataExportService {

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
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

    @Transactional(readOnly = true)
    public ExportResult export(Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        ExportResult result = new ExportResult();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

        result.put("accounts", exportEntities(outputDir, "accounts_" + timestamp + ".json",
                accountRepository.findAll()));
        result.put("customers", exportEntities(outputDir, "customers_" + timestamp + ".json",
                customerRepository.findAll()));
        result.put("cards", exportEntities(outputDir, "cards_" + timestamp + ".json",
                cardRepository.findAll()));
        result.put("card_xref", exportEntities(outputDir, "card_xref_" + timestamp + ".json",
                cardCrossReferenceRepository.findAll()));
        result.put("daily_transactions", exportEntities(outputDir, "daily_transactions_" + timestamp + ".json",
                dailyTransactionRepository.findAll()));
        result.put("transactions", exportEntities(outputDir, "transactions_" + timestamp + ".json",
                transactionRepository.findAll()));
        result.put("disclosure_groups", exportEntities(outputDir, "disclosure_groups_" + timestamp + ".json",
                disclosureGroupRepository.findAll()));
        result.put("category_balances", exportEntities(outputDir, "category_balances_" + timestamp + ".json",
                categoryBalanceRepository.findAll()));
        result.put("transaction_types", exportEntities(outputDir, "transaction_types_" + timestamp + ".json",
                transactionTypeRepository.findAll()));
        result.put("transaction_categories", exportEntities(outputDir, "transaction_categories_" + timestamp + ".json",
                transactionCategoryRepository.findAll()));

        log.info("Export complete to {}. Total records: {}", outputDir, result.totalRecords());
        return result;
    }

    private <T> int exportEntities(Path dir, String filename, java.util.List<T> entities) throws IOException {
        Path file = dir.resolve(filename);
        MAPPER.writeValue(file.toFile(), entities);
        log.info("Exported {} records to {}", entities.size(), filename);
        return entities.size();
    }

    public static class ExportResult {
        private final Map<String, Integer> counts = new LinkedHashMap<>();

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
    }
}
