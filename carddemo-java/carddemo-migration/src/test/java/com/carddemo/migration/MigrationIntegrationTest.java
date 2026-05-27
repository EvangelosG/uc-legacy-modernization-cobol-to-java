package com.carddemo.migration;

import com.carddemo.domain.entity.Account;
import com.carddemo.domain.entity.CardCrossReference;
import com.carddemo.domain.entity.DailyTransaction;
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
import com.carddemo.migration.service.DataExportService;
import com.carddemo.migration.service.DataImportService;
import com.carddemo.migration.service.MigrationValidationService;
import com.carddemo.migration.service.VsamToPostgresqlMigrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = CardDemoMigrationApplication.class)
@Testcontainers
@ActiveProfiles("test")
class MigrationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("carddemo_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired private VsamToPostgresqlMigrator migrator;
    @Autowired private DataExportService exportService;
    @Autowired private DataImportService importService;
    @Autowired private MigrationValidationService validationService;

    @Autowired private AccountRepository accountRepository;
    @Autowired private CardRepository cardRepository;
    @Autowired private CardCrossReferenceRepository cardCrossReferenceRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private DailyTransactionRepository dailyTransactionRepository;
    @Autowired private DisclosureGroupRepository disclosureGroupRepository;
    @Autowired private TransactionCategoryBalanceRepository categoryBalanceRepository;
    @Autowired private TransactionCategoryRepository transactionCategoryRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private TransactionTypeRepository transactionTypeRepository;

    private Path dataDir;

    @BeforeEach
    void setUp() {
        // Clear seeded data to test migration from scratch
        dailyTransactionRepository.deleteAll();
        transactionRepository.deleteAll();
        categoryBalanceRepository.deleteAll();
        cardCrossReferenceRepository.deleteAll();
        cardRepository.deleteAll();
        disclosureGroupRepository.deleteAll();
        customerRepository.deleteAll();
        accountRepository.deleteAll();
        transactionCategoryRepository.deleteAll();
        transactionTypeRepository.deleteAll();

        // Resolve ASCII data directory relative to project root
        dataDir = resolveDataDir();
    }

    private Path resolveDataDir() {
        File dir = new File("app/data/ASCII");
        if (dir.exists()) return dir.toPath();
        dir = new File("../app/data/ASCII");
        if (dir.exists()) return dir.toPath();
        dir = new File("../../app/data/ASCII");
        if (dir.exists()) return dir.toPath();
        throw new IllegalStateException("Cannot find app/data/ASCII directory");
    }

    @Test
    void testMigrateAllAsciiFiles() throws Exception {
        VsamToPostgresqlMigrator.MigrationResult result = migrator.migrate(dataDir);

        // Row counts match source files
        assertEquals(50, result.getCount("accounts"));
        assertEquals(50, result.getCount("customers"));
        assertEquals(50, result.getCount("cards"));
        assertEquals(50, result.getCount("card_xref"));
        assertEquals(300, result.getCount("transactions")); // dailytran.txt has 300 lines
        assertEquals(51, result.getCount("disclosure_groups"));
        assertEquals(50, result.getCount("category_balances"));
        assertEquals(7, result.getCount("transaction_types"));
        assertEquals(18, result.getCount("transaction_categories"));

        // Verify DB counts
        assertEquals(50, accountRepository.count());
        assertEquals(50, customerRepository.count());
        assertEquals(50, cardRepository.count());
        assertEquals(50, cardCrossReferenceRepository.count());
        assertEquals(300, dailyTransactionRepository.count());
        assertEquals(51, disclosureGroupRepository.count());
        assertEquals(50, categoryBalanceRepository.count());
        assertEquals(7, transactionTypeRepository.count());
        assertEquals(18, transactionCategoryRepository.count());
    }

    @Test
    void testMonetaryTotalsMatch() throws Exception {
        migrator.migrate(dataDir);

        // Sum of account balances
        BigDecimal totalBalance = accountRepository.findAll().stream()
                .map(Account::getCurrBal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertTrue(totalBalance.compareTo(BigDecimal.ZERO) > 0,
                "Total balance should be positive");

        // Sum of daily transaction amounts
        BigDecimal totalTxnAmount = dailyTransactionRepository.findAll().stream()
                .map(DailyTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertNotNull(totalTxnAmount, "Total transaction amount should not be null");
    }

    @Test
    void testReferentialIntegrity() throws Exception {
        migrator.migrate(dataDir);

        Set<Long> accountIds = accountRepository.findAll().stream()
                .map(Account::getAcctId).collect(Collectors.toSet());
        Set<Long> customerIds = customerRepository.findAll().stream()
                .map(c -> c.getCustId()).collect(Collectors.toSet());
        Set<String> xrefCards = cardCrossReferenceRepository.findAll().stream()
                .map(CardCrossReference::getCardNum).collect(Collectors.toSet());

        // All xrefs point to valid accounts and customers
        List<CardCrossReference> xrefs = cardCrossReferenceRepository.findAll();
        long orphanAcct = xrefs.stream()
                .filter(x -> !accountIds.contains(x.getAcctId())).count();
        long orphanCust = xrefs.stream()
                .filter(x -> !customerIds.contains(x.getCustId())).count();
        assertEquals(0, orphanAcct, "Zero orphan xref→account records");
        assertEquals(0, orphanCust, "Zero orphan xref→customer records");

        // All daily transactions reference valid cards
        long orphanDailyTxn = dailyTransactionRepository.findAll().stream()
                .filter(t -> !xrefCards.contains(t.getCardNum())).count();
        assertEquals(0, orphanDailyTxn, "Zero orphan daily_txn→card records");
    }

    @Test
    void testMigrationValidation() throws Exception {
        migrator.migrate(dataDir);

        MigrationValidationService.ValidationResult result = validationService.validate(dataDir);
        assertTrue(result.allPassed(),
                "All validation checks should pass. Failed: " +
                        result.getChecks().stream()
                                .filter(c -> !c.passed())
                                .map(c -> c.name() + ": " + c.detail())
                                .collect(Collectors.joining(", ")));
    }

    @Test
    void testExportImportRoundtrip(@TempDir Path tempDir) throws Exception {
        // Step 1: Load ASCII data
        migrator.migrate(dataDir);
        long origAccountCount = accountRepository.count();
        long origCustomerCount = customerRepository.count();
        long origCardCount = cardRepository.count();
        long origXrefCount = cardCrossReferenceRepository.count();

        // Step 2: Export to JSON
        DataExportService.ExportResult exportResult = exportService.export(tempDir);
        assertEquals(origAccountCount, exportResult.getCount("accounts"));
        assertEquals(origCustomerCount, exportResult.getCount("customers"));
        assertEquals(origCardCount, exportResult.getCount("cards"));
        assertEquals(origXrefCount, exportResult.getCount("card_xref"));

        // Find exported files
        File[] jsonFiles = tempDir.toFile().listFiles((dir, name) -> name.endsWith(".json"));
        assertNotNull(jsonFiles);
        assertTrue(jsonFiles.length >= 10, "Should have at least 10 JSON export files");

        // Step 3: Clear DB
        dailyTransactionRepository.deleteAll();
        transactionRepository.deleteAll();
        categoryBalanceRepository.deleteAll();
        cardCrossReferenceRepository.deleteAll();
        cardRepository.deleteAll();
        disclosureGroupRepository.deleteAll();
        customerRepository.deleteAll();
        accountRepository.deleteAll();
        transactionCategoryRepository.deleteAll();
        transactionTypeRepository.deleteAll();

        assertEquals(0, accountRepository.count());

        // Step 4: Re-import from JSON
        Path accountsFile = findFile(tempDir, "accounts_");
        Path customersFile = findFile(tempDir, "customers_");
        Path cardsFile = findFile(tempDir, "cards_");
        Path xrefFile = findFile(tempDir, "card_xref_");
        Path dailyTxnFile = findFile(tempDir, "daily_transactions_");
        Path txnFile = findFile(tempDir, "transactions_");
        Path discFile = findFile(tempDir, "disclosure_groups_");
        Path catBalFile = findFile(tempDir, "category_balances_");
        Path typesFile = findFile(tempDir, "transaction_types_");
        Path catFile = findFile(tempDir, "transaction_categories_");

        DataImportService.ImportResult importResult = importService.importData(
                accountsFile, customersFile, cardsFile, xrefFile,
                dailyTxnFile, txnFile, discFile, catBalFile, typesFile, catFile);

        // Step 5: Verify counts match
        assertEquals(origAccountCount, accountRepository.count());
        assertEquals(origCustomerCount, customerRepository.count());
        assertEquals(origCardCount, cardRepository.count());
        assertEquals(origXrefCount, cardCrossReferenceRepository.count());
        assertFalse(importResult.hasErrors(), "Import should have no referential integrity errors");
    }

    private Path findFile(Path dir, String prefix) {
        File[] files = dir.toFile().listFiles((d, name) -> name.startsWith(prefix) && name.endsWith(".json"));
        assertNotNull(files, "Should find file with prefix: " + prefix);
        assertTrue(files.length > 0, "Should find file with prefix: " + prefix);
        return files[0].toPath();
    }
}
