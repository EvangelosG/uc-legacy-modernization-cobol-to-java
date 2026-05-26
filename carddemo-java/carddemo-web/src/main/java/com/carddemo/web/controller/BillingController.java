package com.carddemo.web.controller;

import com.carddemo.domain.entity.TransactionCategoryBalance;
import com.carddemo.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @GetMapping("/account/{accountId}")
    public ResponseEntity<BillingSummaryResponse> getBillingSummary(
            @PathVariable("accountId") Long accountId) {
        BillingService.BillingSummary summary = billingService.getBillingSummary(accountId);
        return ResponseEntity.ok(BillingSummaryResponse.from(summary));
    }

    public record BillingSummaryResponse(
            Long accountId,
            BigDecimal currentBalance,
            BigDecimal creditLimit,
            BigDecimal cashCreditLimit,
            BigDecimal cycleCredits,
            BigDecimal cycleDebits,
            BigDecimal totalCategoryBalance,
            List<CategoryBalanceResponse> categoryBalances
    ) {
        static BillingSummaryResponse from(BillingService.BillingSummary s) {
            List<CategoryBalanceResponse> balances = s.categoryBalances().stream()
                    .map(CategoryBalanceResponse::from).toList();
            return new BillingSummaryResponse(
                    s.accountId(), s.currentBalance(), s.creditLimit(),
                    s.cashCreditLimit(), s.cycleCredits(), s.cycleDebits(),
                    s.totalCategoryBalance(), balances
            );
        }
    }

    public record CategoryBalanceResponse(Long acctId, String typeCd, Integer catCd,
                                          BigDecimal balance) {
        static CategoryBalanceResponse from(TransactionCategoryBalance b) {
            return new CategoryBalanceResponse(b.getAcctId(), b.getTypeCd(),
                    b.getCatCd(), b.getBalance());
        }
    }
}
