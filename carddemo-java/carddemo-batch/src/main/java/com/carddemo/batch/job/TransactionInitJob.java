package com.carddemo.batch.job;

import com.carddemo.common.util.MoneyUtil;
import com.carddemo.domain.entity.Account;
import com.carddemo.domain.entity.CardCrossReference;
import com.carddemo.domain.entity.DailyTransaction;
import com.carddemo.domain.repository.AccountRepository;
import com.carddemo.domain.repository.CardCrossReferenceRepository;
import com.carddemo.domain.repository.DailyTransactionRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Replaces CBTRN01C.cbl — Transaction file init/validate.
 * Reads a flat file of daily transactions, validates each one by:
 *   - Looking up card cross-reference (XREFFILE)
 *   - Looking up account (ACCTFILE)
 * Valid transactions are saved to the daily_transactions table.
 */
@Slf4j
@Component
public class TransactionInitJob {

    private static final DateTimeFormatter TS_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    private final DailyTransactionRepository dailyTransactionRepository;
    private final CardCrossReferenceRepository cardXrefRepository;
    private final AccountRepository accountRepository;

    @Getter
    private long processedCount;
    @Getter
    private long skippedCount;
    @Getter
    private final List<String> skippedReasons = new ArrayList<>();

    public TransactionInitJob(
            DailyTransactionRepository dailyTransactionRepository,
            CardCrossReferenceRepository cardXrefRepository,
            AccountRepository accountRepository) {
        this.dailyTransactionRepository = dailyTransactionRepository;
        this.cardXrefRepository = cardXrefRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public void execute(Path inputFile) throws IOException {
        log.info("START OF EXECUTION OF PROGRAM CBTRN01C (TransactionInitJob)");
        processedCount = 0;
        skippedCount = 0;
        skippedReasons.clear();

        List<DailyTransaction> batch = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(inputFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                DailyTransaction dt = parseDailyTransaction(line);
                if (dt == null) {
                    skippedCount++;
                    skippedReasons.add("PARSE ERROR for line: " + line.substring(0, Math.min(16, line.length())));
                    continue;
                }

                // 2000-LOOKUP-XREF
                Optional<CardCrossReference> xref = cardXrefRepository.findByCardNum(dt.getCardNum());
                if (xref.isEmpty()) {
                    skippedCount++;
                    skippedReasons.add("CARD NUMBER " + dt.getCardNum()
                            + " COULD NOT BE VERIFIED. SKIPPING TRANSACTION ID-" + dt.getTranId());
                    log.warn("CARD NUMBER {} COULD NOT BE VERIFIED. SKIPPING TRANSACTION ID-{}",
                            dt.getCardNum(), dt.getTranId());
                    continue;
                }

                // 3000-READ-ACCOUNT
                Long acctId = xref.get().getAcctId();
                Optional<Account> account = accountRepository.findById(acctId);
                if (account.isEmpty()) {
                    skippedCount++;
                    skippedReasons.add("ACCOUNT " + acctId + " NOT FOUND");
                    log.warn("ACCOUNT {} NOT FOUND", acctId);
                    continue;
                }

                batch.add(dt);
                processedCount++;
            }
        }

        dailyTransactionRepository.saveAll(batch);

        log.info("PROCESSED: {}, SKIPPED: {}", processedCount, skippedCount);
        log.info("END OF EXECUTION OF PROGRAM CBTRN01C (TransactionInitJob)");
    }

    /**
     * Parses a fixed-width line according to CVTRA06Y copybook layout (350 bytes):
     *   DALYTRAN-ID           PIC X(16)   pos 0-15
     *   DALYTRAN-TYPE-CD      PIC X(02)   pos 16-17
     *   DALYTRAN-CAT-CD       PIC 9(04)   pos 18-21
     *   DALYTRAN-SOURCE       PIC X(10)   pos 22-31
     *   DALYTRAN-DESC         PIC X(100)  pos 32-131
     *   DALYTRAN-AMT          PIC S9(09)V99  pos 132-142 (11 chars + sign in last char)
     *   DALYTRAN-MERCHANT-ID  PIC 9(09)   pos 143-151
     *   DALYTRAN-MERCHANT-NAME PIC X(50)  pos 152-201
     *   DALYTRAN-MERCHANT-CITY PIC X(50)  pos 202-251
     *   DALYTRAN-MERCHANT-ZIP  PIC X(10)  pos 252-261
     *   DALYTRAN-CARD-NUM     PIC X(16)   pos 262-277
     *   DALYTRAN-ORIG-TS      PIC X(26)   pos 278-303
     *   DALYTRAN-PROC-TS      PIC X(26)   pos 304-329
     *   FILLER                PIC X(20)   pos 330-349
     */
    DailyTransaction parseDailyTransaction(String line) {
        try {
            if (line.length() < 304) {
                return null;
            }
            String tranId = line.substring(0, 16).trim();
            String typeCd = line.substring(16, 18).trim();
            int catCd = Integer.parseInt(line.substring(18, 22).trim());
            String source = line.substring(22, 32).trim();
            String desc = line.substring(32, 132).trim();
            BigDecimal amount = MoneyUtil.parseCobolSigned(line.substring(132, 143));
            long merchantId = Long.parseLong(line.substring(143, 152).trim());
            String merchantName = line.substring(152, 202).trim();
            String merchantCity = line.substring(202, 252).trim();
            String merchantZip = line.substring(252, 262).trim();
            String cardNum = line.substring(262, 278).trim();
            String origTsStr = line.substring(278, 304).trim();

            LocalDateTime origTs = null;
            if (!origTsStr.isBlank()) {
                try {
                    origTs = LocalDateTime.parse(origTsStr, TS_FORMATTER);
                } catch (Exception e) {
                    log.debug("Could not parse origTs '{}', skipping timestamp", origTsStr);
                }
            }

            return DailyTransaction.builder()
                    .tranId(tranId)
                    .typeCd(typeCd)
                    .catCd(catCd)
                    .source(source)
                    .description(desc)
                    .amount(amount)
                    .merchantId(merchantId)
                    .merchantName(merchantName)
                    .merchantCity(merchantCity)
                    .merchantZip(merchantZip)
                    .cardNum(cardNum)
                    .origTs(origTs)
                    .build();
        } catch (Exception e) {
            log.warn("Error parsing daily transaction line: {}", e.getMessage());
            return null;
        }
    }
}
