package com.carddemo.service;

import com.carddemo.domain.entity.Account;
import com.carddemo.domain.entity.TransactionCategoryBalance;
import com.carddemo.domain.repository.AccountRepository;
import com.carddemo.domain.repository.TransactionCategoryBalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private TransactionCategoryBalanceRepository categoryBalanceRepository;

    private BillingService billingService;

    @BeforeEach
    void setUp() {
        billingService = new BillingService(accountRepository, categoryBalanceRepository);
    }

    private Account sampleAccount() {
        return Account.builder().acctId(1L).activeStatus("Y")
                .currBal(new BigDecimal("500.00")).creditLimit(new BigDecimal("5000.00"))
                .cashCreditLimit(new BigDecimal("1000.00"))
                .currCycCredit(BigDecimal.ZERO).currCycDebit(BigDecimal.ZERO).version(0L).build();
    }

    @Test
    void getBillingSummaryReturnsCorrectData() {
        Account account = sampleAccount();
        TransactionCategoryBalance bal = TransactionCategoryBalance.builder()
                .acctId(1L).typeCd("01").catCd(1).balance(new BigDecimal("150.00")).build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(categoryBalanceRepository.findByAcctId(1L)).thenReturn(List.of(bal));

        BillingService.BillingSummary summary = billingService.getBillingSummary(1L);
        assertThat(summary.accountId()).isEqualTo(1L);
        assertThat(summary.currentBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(summary.totalCategoryBalance()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    void processPaymentUpdatesBalance() {
        Account account = sampleAccount();
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        BillingService.PaymentRequest request = new BillingService.PaymentRequest(
                1L, new BigDecimal("100.00"));

        BillingService.PaymentResult result = billingService.processPayment(request);
        assertThat(result.accountId()).isEqualTo(1L);
        assertThat(result.paymentAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.newBalance()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(result.newCycleCredits()).isEqualByComparingTo(new BigDecimal("100.00"));

        verify(accountRepository).save(account);
        assertThat(account.getCurrBal()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(account.getCurrCycCredit()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void processPaymentThrowsOnAccountNotFound() {
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        BillingService.PaymentRequest request = new BillingService.PaymentRequest(
                999L, new BigDecimal("100.00"));

        assertThatThrownBy(() -> billingService.processPayment(request))
                .isInstanceOf(TransactionTypeService.ResourceNotFoundException.class)
                .hasMessageContaining("Account not found");
    }

    @Test
    void processPaymentThrowsOnInactiveAccount() {
        Account account = sampleAccount();
        account.setActiveStatus("N");
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        BillingService.PaymentRequest request = new BillingService.PaymentRequest(
                1L, new BigDecimal("100.00"));

        assertThatThrownBy(() -> billingService.processPayment(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void processPaymentThrowsOnNegativeAmount() {
        Account account = sampleAccount();
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        BillingService.PaymentRequest request = new BillingService.PaymentRequest(
                1L, new BigDecimal("-50.00"));

        assertThatThrownBy(() -> billingService.processPayment(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void processPaymentThrowsOnNullAmount() {
        Account account = sampleAccount();
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        BillingService.PaymentRequest request = new BillingService.PaymentRequest(1L, null);

        assertThatThrownBy(() -> billingService.processPayment(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
    }

    @Test
    void processPaymentUsesScale2RoundHalfUp() {
        Account account = sampleAccount();
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        BillingService.PaymentRequest request = new BillingService.PaymentRequest(
                1L, new BigDecimal("100.555"));

        BillingService.PaymentResult result = billingService.processPayment(request);
        assertThat(result.paymentAmount()).isEqualByComparingTo(new BigDecimal("100.56"));
        assertThat(result.paymentAmount().scale()).isEqualTo(2);
    }

    @Test
    void processPaymentAllowsOverpaymentReducingBalanceBelowZero() {
        Account account = sampleAccount();
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        BillingService.PaymentRequest request = new BillingService.PaymentRequest(
                1L, new BigDecimal("600.00"));

        BillingService.PaymentResult result = billingService.processPayment(request);
        assertThat(result.newBalance()).isEqualByComparingTo(new BigDecimal("-100.00"));
    }
}
