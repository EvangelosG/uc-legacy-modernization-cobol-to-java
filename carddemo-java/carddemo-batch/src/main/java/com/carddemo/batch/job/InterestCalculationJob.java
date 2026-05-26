package com.carddemo.batch.job;

import com.carddemo.common.util.MoneyUtil;
import com.carddemo.domain.entity.Account;
import com.carddemo.domain.entity.CardCrossReference;
import com.carddemo.domain.entity.DisclosureGroup;
import com.carddemo.domain.entity.DisclosureGroupId;
import com.carddemo.domain.entity.Transaction;
import com.carddemo.domain.entity.TransactionCategoryBalance;
import com.carddemo.domain.repository.AccountRepository;
import com.carddemo.domain.repository.CardCrossReferenceRepository;
import com.carddemo.domain.repository.DisclosureGroupRepository;
import com.carddemo.domain.repository.TransactionCategoryBalanceRepository;
import com.carddemo.domain.repository.TransactionRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Replaces CBACT04C.cbl — Interest Calculation.
 * Reads all category balances (sequentially by account), for each:
 *   - Looks up account data and card cross-reference
 *   - Looks up disclosure group interest rate (with DEFAULT fallback)
 *   - CRITICAL FORMULA: monthlyInterest = (categoryBalance * interestRate) / 1200
 *   - Accumulates total interest per account
 *   - When account changes: writes total interest as a transaction, updates account balance,
 *     resets cycle credit/debit to 0
 */
@Slf4j
@Component
public class InterestCalculationJob {

    private static final BigDecimal TWELVE_HUNDRED = new BigDecimal("1200");
    private static final String DEFAULT_GROUP = "DEFAULT";

    private final TransactionCategoryBalanceRepository categoryBalanceRepository;
    private final AccountRepository accountRepository;
    private final CardCrossReferenceRepository cardXrefRepository;
    private final DisclosureGroupRepository disclosureGroupRepository;
    private final TransactionRepository transactionRepository;

    @Getter
    private long recordCount;

    private final AtomicLong tranIdSuffix = new AtomicLong(0);

    public InterestCalculationJob(
            TransactionCategoryBalanceRepository categoryBalanceRepository,
            AccountRepository accountRepository,
            CardCrossReferenceRepository cardXrefRepository,
            DisclosureGroupRepository disclosureGroupRepository,
            TransactionRepository transactionRepository) {
        this.categoryBalanceRepository = categoryBalanceRepository;
        this.accountRepository = accountRepository;
        this.cardXrefRepository = cardXrefRepository;
        this.disclosureGroupRepository = disclosureGroupRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public void execute(LocalDate parmDate) {
        log.info("START OF EXECUTION OF PROGRAM CBACT04C (InterestCalculationJob)");
        recordCount = 0;
        tranIdSuffix.set(0);

        String dateStr = parmDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        List<TransactionCategoryBalance> allBalances = categoryBalanceRepository.findAll();
        allBalances.sort(Comparator.comparing(TransactionCategoryBalance::getAcctId)
                .thenComparing(TransactionCategoryBalance::getTypeCd)
                .thenComparing(TransactionCategoryBalance::getCatCd));

        Long lastAcctId = null;
        BigDecimal totalInterest = BigDecimal.ZERO.setScale(MoneyUtil.SCALE, MoneyUtil.ROUNDING);
        Account currentAccount = null;
        String currentCardNum = null;
        boolean firstTime = true;

        for (TransactionCategoryBalance tcb : allBalances) {
            recordCount++;
            log.debug("Processing category balance: acct={}, type={}, cat={}, bal={}",
                    tcb.getAcctId(), tcb.getTypeCd(), tcb.getCatCd(), tcb.getBalance());

            if (!tcb.getAcctId().equals(lastAcctId)) {
                // Account break — process accumulated interest for previous account
                if (!firstTime && currentAccount != null) {
                    updateAccount(currentAccount, totalInterest);
                } else {
                    firstTime = false;
                }

                // Reset for new account
                totalInterest = BigDecimal.ZERO.setScale(MoneyUtil.SCALE, MoneyUtil.ROUNDING);
                lastAcctId = tcb.getAcctId();

                // 1100-GET-ACCT-DATA
                currentAccount = accountRepository.findById(tcb.getAcctId()).orElse(null);
                if (currentAccount == null) {
                    log.error("ACCOUNT NOT FOUND: {}", tcb.getAcctId());
                    continue;
                }

                // 1110-GET-XREF-DATA (lookup by account ID)
                List<CardCrossReference> xrefs = cardXrefRepository.findByAcctId(tcb.getAcctId());
                currentCardNum = xrefs.isEmpty() ? "" : xrefs.get(0).getCardNum();
            }

            if (currentAccount == null) {
                continue;
            }

            // 1200-GET-INTEREST-RATE
            BigDecimal interestRate = getInterestRate(
                    currentAccount.getGroupId(), tcb.getTypeCd(), tcb.getCatCd());

            if (interestRate.compareTo(BigDecimal.ZERO) != 0) {
                // 1300-COMPUTE-INTEREST
                // CRITICAL FORMULA from CBACT04C line 464-465:
                // COMPUTE WS-MONTHLY-INT = (TRAN-CAT-BAL * DIS-INT-RATE) / 1200
                BigDecimal monthlyInterest = tcb.getBalance()
                        .multiply(interestRate)
                        .divide(TWELVE_HUNDRED, MoneyUtil.SCALE, RoundingMode.HALF_UP);

                totalInterest = MoneyUtil.add(totalInterest, monthlyInterest);

                // 1300-B-WRITE-TX: write interest transaction
                writeInterestTransaction(tcb.getAcctId(), currentCardNum,
                        monthlyInterest, dateStr);
            }
        }

        // Process last account
        if (currentAccount != null && !firstTime) {
            updateAccount(currentAccount, totalInterest);
        }

        log.info("END OF EXECUTION OF PROGRAM CBACT04C (InterestCalculationJob)");
    }

    /**
     * Mirrors paragraph 1200. Tries account group first, falls back to DEFAULT.
     */
    private BigDecimal getInterestRate(String groupId, String typeCd, Integer catCd) {
        DisclosureGroupId id = new DisclosureGroupId(groupId, typeCd, catCd);
        Optional<DisclosureGroup> dg = disclosureGroupRepository.findById(id);

        if (dg.isPresent()) {
            return dg.get().getIntRate();
        }

        // Fallback to DEFAULT group (paragraph 1200-A-GET-DEFAULT-INT-RATE)
        log.debug("DISCLOSURE GROUP RECORD MISSING for {}/{}/{}. TRY WITH DEFAULT GROUP CODE",
                groupId, typeCd, catCd);
        DisclosureGroupId defaultId = new DisclosureGroupId(DEFAULT_GROUP, typeCd, catCd);
        Optional<DisclosureGroup> defaultDg = disclosureGroupRepository.findById(defaultId);

        if (defaultDg.isPresent()) {
            return defaultDg.get().getIntRate();
        }

        log.warn("DEFAULT DISCLOSURE GROUP also not found for type={}, cat={}", typeCd, catCd);
        return BigDecimal.ZERO;
    }

    /**
     * Mirrors paragraph 1300-B-WRITE-TX.
     * Transaction ID = date + suffix (STRING PARM-DATE, WS-TRANID-SUFFIX INTO TRAN-ID)
     */
    private void writeInterestTransaction(Long acctId, String cardNum,
                                           BigDecimal monthlyInterest, String dateStr) {
        long suffix = tranIdSuffix.incrementAndGet();
        String tranId = String.format("%-10s%06d", dateStr, suffix);
        if (tranId.length() > 16) {
            tranId = tranId.substring(0, 16);
        }

        LocalDateTime now = LocalDateTime.now();
        Transaction tx = Transaction.builder()
                .tranId(tranId)
                .typeCd("01")
                .catCd(5)
                .source("System")
                .description(String.format("Int. for a/c %d", acctId))
                .amount(monthlyInterest)
                .merchantId(0L)
                .merchantName("")
                .merchantCity("")
                .merchantZip("")
                .cardNum(cardNum)
                .origTs(now)
                .procTs(now)
                .build();

        transactionRepository.save(tx);
    }

    /**
     * Mirrors paragraph 1050-UPDATE-ACCOUNT.
     * ADD WS-TOTAL-INT TO ACCT-CURR-BAL
     * MOVE 0 TO ACCT-CURR-CYC-CREDIT
     * MOVE 0 TO ACCT-CURR-CYC-DEBIT
     */
    private void updateAccount(Account account, BigDecimal totalInterest) {
        account.setCurrBal(MoneyUtil.add(account.getCurrBal(), totalInterest));
        account.setCurrCycCredit(BigDecimal.ZERO.setScale(MoneyUtil.SCALE, MoneyUtil.ROUNDING));
        account.setCurrCycDebit(BigDecimal.ZERO.setScale(MoneyUtil.SCALE, MoneyUtil.ROUNDING));
        accountRepository.save(account);
    }
}
