package com.carddemo.service;

import com.carddemo.domain.entity.Account;
import com.carddemo.domain.entity.TransactionCategoryBalance;
import com.carddemo.domain.repository.AccountRepository;
import com.carddemo.domain.repository.TransactionCategoryBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final AccountRepository accountRepository;
    private final TransactionCategoryBalanceRepository categoryBalanceRepository;

    @Transactional(readOnly = true)
    public BillingSummary getBillingSummary(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new TransactionTypeService.ResourceNotFoundException(
                        "Account not found: " + accountId));

        List<TransactionCategoryBalance> balances = categoryBalanceRepository.findByAcctId(accountId);

        BigDecimal totalBalance = balances.stream()
                .map(TransactionCategoryBalance::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new BillingSummary(
                account.getAcctId(),
                account.getCurrBal(),
                account.getCreditLimit(),
                account.getCashCreditLimit(),
                account.getCurrCycCredit(),
                account.getCurrCycDebit(),
                totalBalance,
                balances
        );
    }

    @Transactional
    public PaymentResult processPayment(PaymentRequest request) {
        if (request.accountId() == null) {
            throw new IllegalArgumentException("Account ID is required");
        }

        Account account = accountRepository.findById(request.accountId())
                .orElseThrow(() -> new TransactionTypeService.ResourceNotFoundException(
                        "Account not found: " + request.accountId()));

        if (!"Y".equals(account.getActiveStatus())) {
            throw new IllegalArgumentException("Account is not active");
        }

        if (request.paymentAmount() == null) {
            throw new IllegalArgumentException("Payment amount is required");
        }
        if (request.paymentAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }

        BigDecimal paymentAmount = request.paymentAmount().setScale(2, RoundingMode.HALF_UP);

        BigDecimal newBalance = account.getCurrBal().subtract(paymentAmount)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal newCycCredit = account.getCurrCycCredit().add(paymentAmount)
                .setScale(2, RoundingMode.HALF_UP);

        account.setCurrBal(newBalance);
        account.setCurrCycCredit(newCycCredit);
        accountRepository.save(account);

        return new PaymentResult(
                account.getAcctId(),
                paymentAmount,
                newBalance,
                newCycCredit
        );
    }

    public record BillingSummary(
            Long accountId,
            BigDecimal currentBalance,
            BigDecimal creditLimit,
            BigDecimal cashCreditLimit,
            BigDecimal cycleCredits,
            BigDecimal cycleDebits,
            BigDecimal totalCategoryBalance,
            List<TransactionCategoryBalance> categoryBalances
    ) {}

    public record PaymentRequest(
            Long accountId,
            BigDecimal paymentAmount
    ) {}

    public record PaymentResult(
            Long accountId,
            BigDecimal paymentAmount,
            BigDecimal newBalance,
            BigDecimal newCycleCredits
    ) {}
}
