package com.carddemo.batch.job;

import com.carddemo.common.util.MoneyUtil;
import com.carddemo.domain.entity.Account;
import com.carddemo.domain.entity.CardCrossReference;
import com.carddemo.domain.entity.DailyTransaction;
import com.carddemo.domain.entity.Transaction;
import com.carddemo.domain.entity.TransactionCategoryBalance;
import com.carddemo.domain.entity.TransactionCategoryBalanceId;
import com.carddemo.domain.repository.AccountRepository;
import com.carddemo.domain.repository.CardCrossReferenceRepository;
import com.carddemo.domain.repository.DailyTransactionRepository;
import com.carddemo.domain.repository.TransactionCategoryBalanceRepository;
import com.carddemo.domain.repository.TransactionRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Replaces CBTRN02C.cbl — Daily Transaction Posting.
 * Reads daily transactions, validates each one (xref lookup, account lookup,
 * credit limit check, expiration date check), then posts valid transactions:
 *   - Writes to transactions table
 *   - Updates category balances (TCATBALF)
 *   - Updates account balance (ACCTFILE)
 * Invalid transactions are tracked as rejects.
 */
@Slf4j
@Component
public class DailyTransactionPostingJob {

    private final DailyTransactionRepository dailyTransactionRepository;
    private final CardCrossReferenceRepository cardXrefRepository;
    private final AccountRepository accountRepository;
    private final TransactionCategoryBalanceRepository categoryBalanceRepository;
    private final TransactionRepository transactionRepository;

    @Getter
    private long transactionCount;
    @Getter
    private long rejectCount;
    @Getter
    private final List<RejectedTransaction> rejectedTransactions = new ArrayList<>();

    public DailyTransactionPostingJob(
            DailyTransactionRepository dailyTransactionRepository,
            CardCrossReferenceRepository cardXrefRepository,
            AccountRepository accountRepository,
            TransactionCategoryBalanceRepository categoryBalanceRepository,
            TransactionRepository transactionRepository) {
        this.dailyTransactionRepository = dailyTransactionRepository;
        this.cardXrefRepository = cardXrefRepository;
        this.accountRepository = accountRepository;
        this.categoryBalanceRepository = categoryBalanceRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public void execute() {
        log.info("START OF EXECUTION OF PROGRAM CBTRN02C (DailyTransactionPostingJob)");
        transactionCount = 0;
        rejectCount = 0;
        rejectedTransactions.clear();

        List<DailyTransaction> dailyTransactions = dailyTransactionRepository.findAll();

        for (DailyTransaction dt : dailyTransactions) {
            transactionCount++;
            ValidationResult validation = validateTransaction(dt);

            if (validation.isValid()) {
                postTransaction(dt, validation.getXref(), validation.getAccount());
            } else {
                rejectCount++;
                rejectedTransactions.add(new RejectedTransaction(
                        dt.getTranId(), validation.getFailReasonCode(), validation.getFailReasonDesc()));
            }
        }

        log.info("TRANSACTIONS PROCESSED: {}", transactionCount);
        log.info("TRANSACTIONS REJECTED : {}", rejectCount);
        log.info("END OF EXECUTION OF PROGRAM CBTRN02C (DailyTransactionPostingJob)");
    }

    private ValidationResult validateTransaction(DailyTransaction dt) {
        // 1500-A-LOOKUP-XREF: look up card cross-reference
        Optional<CardCrossReference> xrefOpt = cardXrefRepository.findByCardNum(dt.getCardNum());
        if (xrefOpt.isEmpty()) {
            return ValidationResult.fail(100, "INVALID CARD NUMBER FOUND");
        }
        CardCrossReference xref = xrefOpt.get();

        // 1500-B-LOOKUP-ACCT: look up account
        Optional<Account> acctOpt = accountRepository.findById(xref.getAcctId());
        if (acctOpt.isEmpty()) {
            return ValidationResult.fail(101, "ACCOUNT RECORD NOT FOUND");
        }
        Account account = acctOpt.get();

        // Credit limit check: COMPUTE WS-TEMP-BAL = ACCT-CURR-CYC-CREDIT - ACCT-CURR-CYC-DEBIT + DALYTRAN-AMT
        BigDecimal tempBal = MoneyUtil.add(
                MoneyUtil.subtract(account.getCurrCycCredit(), account.getCurrCycDebit()),
                dt.getAmount());
        if (account.getCreditLimit().compareTo(tempBal) < 0) {
            return ValidationResult.fail(102, "OVERLIMIT TRANSACTION");
        }

        // Expiration date check: ACCT-EXPIRAION-DATE >= DALYTRAN-ORIG-TS(1:10)
        if (account.getExpirationDate() != null && dt.getOrigTs() != null) {
            LocalDate txDate = dt.getOrigTs().toLocalDate();
            if (account.getExpirationDate().isBefore(txDate)) {
                return ValidationResult.fail(103, "TRANSACTION RECEIVED AFTER ACCT EXPIRATION");
            }
        }

        return ValidationResult.success(xref, account);
    }

    private void postTransaction(DailyTransaction dt, CardCrossReference xref, Account account) {
        // 2000-POST-TRANSACTION: map daily transaction fields to transaction record
        Transaction tx = Transaction.builder()
                .tranId(dt.getTranId())
                .typeCd(dt.getTypeCd())
                .catCd(dt.getCatCd())
                .source(dt.getSource())
                .description(dt.getDescription())
                .amount(dt.getAmount())
                .merchantId(dt.getMerchantId())
                .merchantName(dt.getMerchantName())
                .merchantCity(dt.getMerchantCity())
                .merchantZip(dt.getMerchantZip())
                .cardNum(dt.getCardNum())
                .origTs(dt.getOrigTs())
                .procTs(LocalDateTime.now())
                .build();

        // 2700-UPDATE-TCATBAL: update transaction category balance
        updateCategoryBalance(xref.getAcctId(), dt.getTypeCd(), dt.getCatCd(), dt.getAmount());

        // 2800-UPDATE-ACCOUNT-REC: update account balances
        updateAccountRecord(account, dt.getAmount());

        // 2900-WRITE-TRANSACTION-FILE
        transactionRepository.save(tx);
    }

    /**
     * Mirrors paragraph 2700 of CBTRN02C.
     * Looks up the category balance by composite key (acctId, typeCd, catCd).
     * If not found, creates a new record (2700-A).
     * If found, updates existing balance (2700-B).
     */
    private void updateCategoryBalance(Long acctId, String typeCd, Integer catCd, BigDecimal amount) {
        TransactionCategoryBalanceId id = new TransactionCategoryBalanceId(acctId, typeCd, catCd);
        Optional<TransactionCategoryBalance> existing = categoryBalanceRepository.findById(id);

        if (existing.isPresent()) {
            // 2700-B-UPDATE-TCATBAL-REC: ADD DALYTRAN-AMT TO TRAN-CAT-BAL
            TransactionCategoryBalance tcb = existing.get();
            tcb.setBalance(MoneyUtil.add(tcb.getBalance(), amount));
            categoryBalanceRepository.save(tcb);
        } else {
            // 2700-A-CREATE-TCATBAL-REC: initialize and write new record
            log.info("TCATBAL record not found for key: {}/{}/{}.. Creating.", acctId, typeCd, catCd);
            TransactionCategoryBalance tcb = TransactionCategoryBalance.builder()
                    .acctId(acctId)
                    .typeCd(typeCd)
                    .catCd(catCd)
                    .balance(MoneyUtil.toMoney(amount))
                    .build();
            categoryBalanceRepository.save(tcb);
        }
    }

    /**
     * Mirrors paragraph 2800 of CBTRN02C.
     * ADD DALYTRAN-AMT TO ACCT-CURR-BAL
     * IF DALYTRAN-AMT >= 0 → ADD to ACCT-CURR-CYC-CREDIT
     * ELSE → ADD to ACCT-CURR-CYC-DEBIT
     */
    private void updateAccountRecord(Account account, BigDecimal amount) {
        account.setCurrBal(MoneyUtil.add(account.getCurrBal(), amount));
        if (amount.compareTo(BigDecimal.ZERO) >= 0) {
            account.setCurrCycCredit(MoneyUtil.add(account.getCurrCycCredit(), amount));
        } else {
            account.setCurrCycDebit(MoneyUtil.add(account.getCurrCycDebit(), amount));
        }
        accountRepository.save(account);
    }

    public record RejectedTransaction(String tranId, int failReasonCode, String failReasonDesc) {
    }

    @Getter
    public static class ValidationResult {
        private final boolean valid;
        private final int failReasonCode;
        private final String failReasonDesc;
        private final CardCrossReference xref;
        private final Account account;

        private ValidationResult(boolean valid, int failReasonCode, String failReasonDesc,
                                 CardCrossReference xref, Account account) {
            this.valid = valid;
            this.failReasonCode = failReasonCode;
            this.failReasonDesc = failReasonDesc;
            this.xref = xref;
            this.account = account;
        }

        public static ValidationResult success(CardCrossReference xref, Account account) {
            return new ValidationResult(true, 0, null, xref, account);
        }

        public static ValidationResult fail(int code, String desc) {
            return new ValidationResult(false, code, desc, null, null);
        }
    }
}
