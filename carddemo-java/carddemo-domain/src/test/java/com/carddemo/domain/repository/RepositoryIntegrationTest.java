package com.carddemo.domain.repository;

import com.carddemo.domain.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryIntegrationTest {

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

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private CardCrossReferenceRepository cardCrossReferenceRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionTypeRepository transactionTypeRepository;

    @Autowired
    private TransactionCategoryRepository transactionCategoryRepository;

    @Autowired
    private TransactionCategoryBalanceRepository categoryBalanceRepository;

    @Autowired
    private DisclosureGroupRepository disclosureGroupRepository;

    @Autowired
    private UserSecurityRepository userSecurityRepository;

    @Autowired
    private DailyTransactionRepository dailyTransactionRepository;

    @Test
    void testFlywayMigrationsApplied() {
        // Seed data from V012 should be loaded
        assertTrue(accountRepository.count() > 0, "Accounts should be seeded");
        assertTrue(customerRepository.count() > 0, "Customers should be seeded");
        assertTrue(cardRepository.count() > 0, "Cards should be seeded");
        assertTrue(cardCrossReferenceRepository.count() > 0, "Card xrefs should be seeded");
        assertTrue(transactionTypeRepository.count() > 0, "Transaction types should be seeded");
        assertTrue(transactionCategoryRepository.count() > 0, "Transaction categories should be seeded");
        assertTrue(categoryBalanceRepository.count() > 0, "Category balances should be seeded");
        assertTrue(disclosureGroupRepository.count() > 0, "Disclosure groups should be seeded");
        assertTrue(userSecurityRepository.count() > 0, "Users should be seeded");
        assertTrue(dailyTransactionRepository.count() > 0, "Daily transactions should be seeded");
    }

    @Test
    void testAccountCRUD() {
        Account account = accountRepository.findById(1L).orElse(null);
        assertNotNull(account);
        assertEquals("Y", account.getActiveStatus());
        assertEquals(new BigDecimal("194.00"), account.getCurrBal());
        assertEquals(0L, account.getVersion());
    }

    @Test
    void testCardCrossReferenceByCardNumber() {
        // The seed data should have card xrefs
        List<CardCrossReference> allXrefs = cardCrossReferenceRepository.findAll();
        assertFalse(allXrefs.isEmpty());

        String cardNum = allXrefs.get(0).getCardNum();
        Optional<CardCrossReference> found = cardCrossReferenceRepository.findByCardNum(cardNum);
        assertTrue(found.isPresent());
        assertEquals(cardNum, found.get().getCardNum());
    }

    @Test
    void testCardCrossReferenceByAccountId() {
        List<CardCrossReference> allXrefs = cardCrossReferenceRepository.findAll();
        assertFalse(allXrefs.isEmpty());

        Long acctId = allXrefs.get(0).getAcctId();
        List<CardCrossReference> byAcct = cardCrossReferenceRepository.findByAcctId(acctId);
        assertFalse(byAcct.isEmpty());
        assertTrue(byAcct.stream().allMatch(x -> x.getAcctId().equals(acctId)));
    }

    @Test
    void testTransactionTypes() {
        assertEquals(7, transactionTypeRepository.count());
        TransactionType purchase = transactionTypeRepository.findById("01").orElse(null);
        assertNotNull(purchase);
        assertEquals("Purchase", purchase.getTypeDesc());
    }

    @Test
    void testTransactionCategories() {
        assertEquals(18, transactionCategoryRepository.count());
    }

    @Test
    void testUserSecurity() {
        UserSecurity admin = userSecurityRepository.findById("ADMIN01 ").orElse(null);
        assertNotNull(admin);
        assertEquals("A", admin.getUsrType());
        assertTrue(admin.getUsrPwd().startsWith("$2a$"));
    }

    @Test
    void testDisclosureGroupCompositeKey() {
        DisclosureGroupId key = new DisclosureGroupId("A000000000", "01", 1);
        Optional<DisclosureGroup> dg = disclosureGroupRepository.findById(key);
        assertTrue(dg.isPresent());
        assertTrue(dg.get().getIntRate().compareTo(BigDecimal.ZERO) >= 0);
    }

    @Test
    void testCategoryBalanceCompositeKey() {
        List<TransactionCategoryBalance> all = categoryBalanceRepository.findAll();
        assertFalse(all.isEmpty());

        TransactionCategoryBalance first = all.get(0);
        TransactionCategoryBalanceId key = new TransactionCategoryBalanceId(
                first.getAcctId(), first.getTypeCd(), first.getCatCd());
        assertTrue(categoryBalanceRepository.findById(key).isPresent());
    }

    @Test
    void testSaveAndFindTransaction() {
        Transaction txn = Transaction.builder()
                .tranId("TEST000000000001")
                .typeCd("01")
                .catCd(1)
                .source("TEST")
                .description("Test transaction")
                .amount(new BigDecimal("100.50"))
                .merchantId(999L)
                .merchantName("Test Merchant")
                .merchantCity("Test City")
                .merchantZip("12345")
                .cardNum("1234567890123456")
                .origTs(LocalDateTime.now())
                .build();

        transactionRepository.save(txn);

        Optional<Transaction> found = transactionRepository.findById("TEST000000000001");
        assertTrue(found.isPresent());
        assertEquals(new BigDecimal("100.50"), found.get().getAmount());
    }
}
