package com.carddemo.service;

import com.carddemo.domain.entity.Account;
import com.carddemo.domain.entity.TransactionCategoryBalance;
import com.carddemo.domain.repository.AccountRepository;
import com.carddemo.domain.repository.TransactionCategoryBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
}
