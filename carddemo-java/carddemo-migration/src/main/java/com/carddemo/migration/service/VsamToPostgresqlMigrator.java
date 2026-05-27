package com.carddemo.migration.service;

import com.carddemo.domain.entity.Account;
import com.carddemo.domain.entity.Card;
import com.carddemo.domain.entity.CardCrossReference;
import com.carddemo.domain.entity.Customer;
import com.carddemo.domain.entity.DailyTransaction;
import com.carddemo.domain.entity.DisclosureGroup;
import com.carddemo.domain.entity.Transaction;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Purpose-built ETL tool that reads ASCII data files from app/data/ASCII/,
 * parses fixed-width fields based on copybook PIC clauses, and inserts
 * into PostgreSQL via JPA repositories.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VsamToPostgresqlMigrator {

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

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    @Transactional
    public MigrationResult migrate(Path dataDir) throws IOException {
        MigrationResult result = new MigrationResult();

        log.info("Starting VSAM-to-PostgreSQL migration from: {}", dataDir);

        result.put("accounts", migrateAccounts(dataDir.resolve("acctdata.txt")));
        result.put("customers", migrateCustomers(dataDir.resolve("custdata.txt")));
        result.put("cards", migrateCards(dataDir.resolve("carddata.txt")));
        result.put("card_xref", migrateCardXrefs(dataDir.resolve("cardxref.txt")));
        result.put("transactions", migrateTransactions(dataDir.resolve("dailytran.txt")));
        result.put("disclosure_groups", migrateDisclosureGroups(dataDir.resolve("discgrp.txt")));
        result.put("category_balances", migrateCategoryBalances(dataDir.resolve("tcatbal.txt")));
        result.put("transaction_types", migrateTransactionTypes(dataDir.resolve("trantype.txt")));
        result.put("transaction_categories", migrateTransactionCategories(dataDir.resolve("trancatg.txt")));

        log.info("Migration complete. Total records: {}", result.totalRecords());
        return result;
    }

    int migrateAccounts(Path file) throws IOException {
        List<String> lines = readNonEmptyLines(file);
        List<Account> entities = new ArrayList<>(lines.size());
        for (String line : lines) {
            entities.add(parseAccount(line));
        }
        accountRepository.saveAll(entities);
        log.info("Migrated {} accounts", entities.size());
        return entities.size();
    }

    int migrateCustomers(Path file) throws IOException {
        List<String> lines = readNonEmptyLines(file);
        List<Customer> entities = new ArrayList<>(lines.size());
        for (String line : lines) {
            entities.add(parseCustomer(line));
        }
        customerRepository.saveAll(entities);
        log.info("Migrated {} customers", entities.size());
        return entities.size();
    }

    int migrateCards(Path file) throws IOException {
        List<String> lines = readNonEmptyLines(file);
        List<Card> entities = new ArrayList<>(lines.size());
        for (String line : lines) {
            entities.add(parseCard(line));
        }
        cardRepository.saveAll(entities);
        log.info("Migrated {} cards", entities.size());
        return entities.size();
    }

    int migrateCardXrefs(Path file) throws IOException {
        List<String> lines = readNonEmptyLines(file);
        List<CardCrossReference> entities = new ArrayList<>(lines.size());
        for (String line : lines) {
            entities.add(parseCardXref(line));
        }
        cardCrossReferenceRepository.saveAll(entities);
        log.info("Migrated {} card cross-references", entities.size());
        return entities.size();
    }

    int migrateTransactions(Path file) throws IOException {
        List<String> lines = readNonEmptyLines(file);
        List<DailyTransaction> entities = new ArrayList<>(lines.size());
        for (String line : lines) {
            entities.add(parseDailyTransaction(line));
        }
        dailyTransactionRepository.saveAll(entities);
        log.info("Migrated {} daily transactions", entities.size());
        return entities.size();
    }

    int migrateDisclosureGroups(Path file) throws IOException {
        List<String> lines = readNonEmptyLines(file);
        List<DisclosureGroup> entities = new ArrayList<>(lines.size());
        for (String line : lines) {
            entities.add(parseDisclosureGroup(line));
        }
        disclosureGroupRepository.saveAll(entities);
        log.info("Migrated {} disclosure groups", entities.size());
        return entities.size();
    }

    int migrateCategoryBalances(Path file) throws IOException {
        List<String> lines = readNonEmptyLines(file);
        List<TransactionCategoryBalance> entities = new ArrayList<>(lines.size());
        for (String line : lines) {
            entities.add(parseCategoryBalance(line));
        }
        categoryBalanceRepository.saveAll(entities);
        log.info("Migrated {} category balances", entities.size());
        return entities.size();
    }

    int migrateTransactionTypes(Path file) throws IOException {
        List<String> lines = readNonEmptyLines(file);
        List<TransactionType> entities = new ArrayList<>(lines.size());
        for (String line : lines) {
            entities.add(parseTransactionType(line));
        }
        transactionTypeRepository.saveAll(entities);
        log.info("Migrated {} transaction types", entities.size());
        return entities.size();
    }

    int migrateTransactionCategories(Path file) throws IOException {
        List<String> lines = readNonEmptyLines(file);
        List<TransactionCategory> entities = new ArrayList<>(lines.size());
        for (String line : lines) {
            entities.add(parseTransactionCategory(line));
        }
        transactionCategoryRepository.saveAll(entities);
        log.info("Migrated {} transaction categories", entities.size());
        return entities.size();
    }

    // ── Parsers ────────────────────────────────────────────────────────

    /**
     * CVACT01Y: ACCOUNT-RECORD (300 bytes)
     * PIC S9(10)V99 = 12 display chars (10+2 digits, sign overpunched on last)
     */
    Account parseAccount(String line) {
        int pos = 0;
        long acctId = parseLong(line, pos, 11); pos += 11;
        String status = substr(line, pos, 1); pos += 1;
        BigDecimal currBal = parseSignedDecimal(line, pos, 12, 2); pos += 12;
        BigDecimal creditLimit = parseSignedDecimal(line, pos, 12, 2); pos += 12;
        BigDecimal cashCreditLimit = parseSignedDecimal(line, pos, 12, 2); pos += 12;
        LocalDate openDate = parseDate(line, pos, 10); pos += 10;
        LocalDate expDate = parseDate(line, pos, 10); pos += 10;
        LocalDate reissueDate = parseDate(line, pos, 10); pos += 10;
        BigDecimal cycCredit = parseSignedDecimal(line, pos, 12, 2); pos += 12;
        BigDecimal cycDebit = parseSignedDecimal(line, pos, 12, 2); pos += 12;
        String zip = substr(line, pos, 10).trim(); pos += 10;
        String groupId = substr(line, pos, 10).trim();

        return Account.builder()
                .acctId(acctId)
                .activeStatus(status)
                .currBal(currBal)
                .creditLimit(creditLimit)
                .cashCreditLimit(cashCreditLimit)
                .openDate(openDate)
                .expirationDate(expDate)
                .reissueDate(reissueDate)
                .currCycCredit(cycCredit)
                .currCycDebit(cycDebit)
                .addrZip(zip)
                .groupId(groupId)
                .build();
    }

    /**
     * CVCUS01Y: CUSTOMER-RECORD (500 bytes)
     * custId PIC 9(09) [0..9]
     * firstName PIC X(25) [9..34]
     * middleName PIC X(25) [34..59]
     * lastName PIC X(25) [59..84]
     * addr1 PIC X(50) [84..134]
     * addr2 PIC X(50) [134..184]
     * addr3 PIC X(50) [184..234]
     * stateCd PIC X(02) [234..236]
     * countryCd PIC X(03) [236..239]
     * zip PIC X(10) [239..249]
     * phone1 PIC X(15) [249..264]
     * phone2 PIC X(15) [264..279]
     * ssn PIC 9(09) [279..288]
     * govtId PIC X(20) [288..308]
     * dob PIC X(10) [308..318]
     * eftAcctId PIC X(10) [318..328]
     * priHolder PIC X(01) [328..329]
     * fico PIC 9(03) [329..332]
     */
    Customer parseCustomer(String line) {
        int pos = 0;
        long custId = parseLong(line, pos, 9); pos += 9;
        String first = substr(line, pos, 25).trim(); pos += 25;
        String middle = substr(line, pos, 25).trim(); pos += 25;
        String last = substr(line, pos, 25).trim(); pos += 25;
        String addr1 = substr(line, pos, 50).trim(); pos += 50;
        String addr2 = substr(line, pos, 50).trim(); pos += 50;
        String addr3 = substr(line, pos, 50).trim(); pos += 50;
        String state = substr(line, pos, 2); pos += 2;
        String country = substr(line, pos, 3); pos += 3;
        String zip = substr(line, pos, 10).trim(); pos += 10;
        String phone1 = substr(line, pos, 15).trim(); pos += 15;
        String phone2 = substr(line, pos, 15).trim(); pos += 15;
        String ssn = substr(line, pos, 9); pos += 9;
        String govtId = substr(line, pos, 20).trim(); pos += 20;
        LocalDate dob = parseDate(line, pos, 10); pos += 10;
        String eft = substr(line, pos, 10).trim(); pos += 10;
        String priHolder = substr(line, pos, 1); pos += 1;
        int fico = parseInt(line, pos, 3);

        return Customer.builder()
                .custId(custId)
                .firstName(first)
                .middleName(middle)
                .lastName(last)
                .addrLine1(addr1)
                .addrLine2(addr2)
                .addrLine3(addr3)
                .addrStateCd(state)
                .addrCountryCd(country)
                .addrZip(zip)
                .phoneNum1(phone1)
                .phoneNum2(phone2)
                .ssn(ssn)
                .govtIssuedId(govtId)
                .dob(dob)
                .eftAccountId(eft)
                .priCardHolderInd(priHolder)
                .ficoCreditScore(fico)
                .build();
    }

    /**
     * CVACT02Y: CARD-RECORD (150 bytes)
     * cardNum PIC X(16) [0..16]
     * acctId PIC 9(11) [16..27]
     * cvv PIC 9(03) [27..30]
     * embossedName PIC X(50) [30..80]
     * expDate PIC X(10) [80..90]
     * activeStatus PIC X(01) [90..91]
     */
    Card parseCard(String line) {
        int pos = 0;
        String cardNum = substr(line, pos, 16).trim(); pos += 16;
        long acctId = parseLong(line, pos, 11); pos += 11;
        int cvv = parseInt(line, pos, 3); pos += 3;
        String name = substr(line, pos, 50).trim(); pos += 50;
        LocalDate expDate = parseDate(line, pos, 10); pos += 10;
        String status = substr(line, pos, 1);

        return Card.builder()
                .cardNum(cardNum)
                .acctId(acctId)
                .cvvCd(cvv)
                .embossedName(name)
                .expirationDate(expDate)
                .activeStatus(status)
                .build();
    }

    /**
     * CVACT03Y: CARD-XREF-RECORD (50 bytes)
     * cardNum PIC X(16) [0..16]
     * custId PIC 9(09) [16..25]
     * acctId PIC 9(11) [25..36]
     */
    CardCrossReference parseCardXref(String line) {
        int pos = 0;
        String cardNum = substr(line, pos, 16).trim(); pos += 16;
        long custId = parseLong(line, pos, 9); pos += 9;
        long acctId = parseLong(line, pos, 11);

        return CardCrossReference.builder()
                .cardNum(cardNum)
                .custId(custId)
                .acctId(acctId)
                .build();
    }

    /**
     * CVTRA05Y/CVTRA06Y: TRAN-RECORD / DALYTRAN-RECORD (350 bytes)
     * tranId PIC X(16) [0..16]
     * typeCd PIC X(02) [16..18]
     * catCd PIC 9(04) [18..22]
     * source PIC X(10) [22..32]
     * desc PIC X(100) [32..132]
     * amt PIC S9(09)V99 [132..143] (11 chars, sign overpunched on last)
     * merchantId PIC 9(09) [143..152]
     * merchantName PIC X(50) [152..202]
     * merchantCity PIC X(50) [202..252]
     * merchantZip PIC X(10) [252..262]
     * cardNum PIC X(16) [262..278]
     * origTs PIC X(26) [278..304]
     * procTs PIC X(26) [304..330]
     */
    DailyTransaction parseDailyTransaction(String line) {
        int pos = 0;
        String tranId = substr(line, pos, 16).trim(); pos += 16;
        String typeCd = substr(line, pos, 2); pos += 2;
        int catCd = parseInt(line, pos, 4); pos += 4;
        String source = substr(line, pos, 10).trim(); pos += 10;
        String desc = substr(line, pos, 100).trim(); pos += 100;
        BigDecimal amt = parseSignedDecimal(line, pos, 11, 2); pos += 11;
        long merchantId = parseLong(line, pos, 9); pos += 9;
        String merchantName = substr(line, pos, 50).trim(); pos += 50;
        String merchantCity = substr(line, pos, 50).trim(); pos += 50;
        String merchantZip = substr(line, pos, 10).trim(); pos += 10;
        String cardNum = substr(line, pos, 16).trim(); pos += 16;
        LocalDateTime origTs = parseTimestamp(line, pos, 26); pos += 26;
        LocalDateTime procTs = parseTimestamp(line, pos, 26);

        return DailyTransaction.builder()
                .tranId(tranId)
                .typeCd(typeCd)
                .catCd(catCd)
                .source(source)
                .description(desc)
                .amount(amt)
                .merchantId(merchantId)
                .merchantName(merchantName)
                .merchantCity(merchantCity)
                .merchantZip(merchantZip)
                .cardNum(cardNum)
                .origTs(origTs)
                .procTs(procTs)
                .build();
    }

    /**
     * CVTRA02Y: DIS-GROUP-RECORD (50 bytes)
     * groupId PIC X(10) [0..10]
     * tranTypeCd PIC X(02) [10..12]
     * tranCatCd PIC 9(04) [12..16]
     * intRate PIC S9(04)V99 [16..22] (6 chars, sign overpunched on last)
     */
    DisclosureGroup parseDisclosureGroup(String line) {
        int pos = 0;
        String groupId = substr(line, pos, 10).trim(); pos += 10;
        String typeCd = substr(line, pos, 2); pos += 2;
        int catCd = parseInt(line, pos, 4); pos += 4;
        BigDecimal rate = parseSignedDecimal(line, pos, 6, 2);

        return DisclosureGroup.builder()
                .groupId(groupId)
                .tranTypeCd(typeCd)
                .tranCatCd(catCd)
                .intRate(rate)
                .build();
    }

    /**
     * CVTRA01Y: TRAN-CAT-BAL-RECORD (50 bytes)
     * acctId PIC 9(11) [0..11]
     * typeCd PIC X(02) [11..13]
     * catCd PIC 9(04) [13..17]
     * bal PIC S9(09)V99 [17..28] (11 chars, sign overpunched on last)
     */
    TransactionCategoryBalance parseCategoryBalance(String line) {
        int pos = 0;
        long acctId = parseLong(line, pos, 11); pos += 11;
        String typeCd = substr(line, pos, 2); pos += 2;
        int catCd = parseInt(line, pos, 4); pos += 4;
        BigDecimal bal = parseSignedDecimal(line, pos, 11, 2);

        return TransactionCategoryBalance.builder()
                .acctId(acctId)
                .typeCd(typeCd)
                .catCd(catCd)
                .balance(bal)
                .build();
    }

    /**
     * CVTRA03Y: TRAN-TYPE-RECORD (60 bytes)
     * typeCd PIC X(02) [0..2]
     * typeDesc PIC X(50) [2..52]
     */
    TransactionType parseTransactionType(String line) {
        String typeCd = substr(line, 0, 2);
        String desc = substr(line, 2, 50).trim();

        return TransactionType.builder()
                .typeCd(typeCd)
                .typeDesc(desc)
                .build();
    }

    /**
     * CVTRA04Y: TRAN-CAT-RECORD (60 bytes)
     * typeCd PIC X(02) [0..2]
     * catCd PIC 9(04) [2..6]
     * typeDesc PIC X(50) [6..56]
     */
    TransactionCategory parseTransactionCategory(String line) {
        String typeCd = substr(line, 0, 2);
        int catCd = parseInt(line, 2, 4);
        String desc = substr(line, 6, 50).trim();

        return TransactionCategory.builder()
                .typeCd(typeCd)
                .catCd(catCd)
                .typeDesc(desc)
                .build();
    }

    // ── Utility methods ────────────────────────────────────────────────

    static String substr(String line, int start, int len) {
        int end = Math.min(start + len, line.length());
        if (start >= line.length()) {
            return " ".repeat(len);
        }
        String s = line.substring(start, end);
        if (s.length() < len) {
            s = s + " ".repeat(len - s.length());
        }
        return s;
    }

    static long parseLong(String line, int start, int len) {
        String s = substr(line, start, len).trim();
        if (s.isEmpty()) return 0L;
        return Long.parseLong(s);
    }

    static int parseInt(String line, int start, int len) {
        String s = substr(line, start, len).trim();
        if (s.isEmpty()) return 0;
        return Integer.parseInt(s);
    }

    /**
     * Parses a COBOL signed numeric with implied decimal and trailing overpunch sign.
     * COBOL PIC S9(n)V99 uses trailing overpunch: last character encodes both
     * digit and sign. '{' = +0, 'A'-'I' = +1..+9, '}' = -0, 'J'-'R' = -1..-9.
     * The 'V' is implied (no decimal point in data).
     */
    static BigDecimal parseSignedDecimal(String line, int start, int len, int scale) {
        String raw = substr(line, start, len).trim();
        if (raw.isEmpty()) return BigDecimal.ZERO;

        char lastChar = raw.charAt(raw.length() - 1);
        String digits;
        boolean negative = false;

        if (Character.isDigit(lastChar)) {
            digits = raw;
        } else {
            int digitValue;
            switch (lastChar) {
                case '{': digitValue = 0; negative = false; break;
                case 'A': digitValue = 1; negative = false; break;
                case 'B': digitValue = 2; negative = false; break;
                case 'C': digitValue = 3; negative = false; break;
                case 'D': digitValue = 4; negative = false; break;
                case 'E': digitValue = 5; negative = false; break;
                case 'F': digitValue = 6; negative = false; break;
                case 'G': digitValue = 7; negative = false; break;
                case 'H': digitValue = 8; negative = false; break;
                case 'I': digitValue = 9; negative = false; break;
                case '}': digitValue = 0; negative = true; break;
                case 'J': digitValue = 1; negative = true; break;
                case 'K': digitValue = 2; negative = true; break;
                case 'L': digitValue = 3; negative = true; break;
                case 'M': digitValue = 4; negative = true; break;
                case 'N': digitValue = 5; negative = true; break;
                case 'O': digitValue = 6; negative = true; break;
                case 'P': digitValue = 7; negative = true; break;
                case 'Q': digitValue = 8; negative = true; break;
                case 'R': digitValue = 9; negative = true; break;
                default:
                    digits = raw;
                    return new BigDecimal(digits).movePointLeft(scale);
            }
            digits = raw.substring(0, raw.length() - 1) + digitValue;
        }

        BigDecimal value = new BigDecimal(digits).movePointLeft(scale);
        return negative ? value.negate() : value;
    }

    static LocalDate parseDate(String line, int start, int len) {
        String s = substr(line, start, len).trim();
        if (s.isEmpty() || s.equals("0000-00-00")) return null;
        try {
            return LocalDate.parse(s, DATE_FMT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    static LocalDateTime parseTimestamp(String line, int start, int len) {
        String s = substr(line, start, len).trim();
        if (s.isEmpty()) return null;
        try {
            return LocalDateTime.parse(s, TS_FMT);
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(s.substring(0, 10), DATE_FMT).atStartOfDay();
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private List<String> readNonEmptyLines(Path file) throws IOException {
        List<String> allLines = Files.readAllLines(file);
        List<String> result = new ArrayList<>(allLines.size());
        for (String line : allLines) {
            if (!line.trim().isEmpty()) {
                result.add(line);
            }
        }
        return result;
    }

    // ── Result holder ──────────────────────────────────────────────────

    public static class MigrationResult {
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
