package com.carddemo.service;

import com.carddemo.domain.entity.Account;
import com.carddemo.domain.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountUpdateServiceTest {

    @Mock
    private AccountRepository accountRepository;

    private AccountUpdateService accountUpdateService;

    @BeforeEach
    void setUp() {
        accountUpdateService = new AccountUpdateService(accountRepository);
    }

    private Account sampleAccount() {
        return Account.builder()
                .acctId(1L).activeStatus("Y")
                .currBal(new BigDecimal("500.00"))
                .creditLimit(new BigDecimal("5000.00"))
                .cashCreditLimit(new BigDecimal("1000.00"))
                .openDate(LocalDate.of(2020, 1, 1))
                .expirationDate(LocalDate.of(2027, 12, 31))
                .currCycCredit(BigDecimal.ZERO)
                .currCycDebit(BigDecimal.ZERO)
                .version(0L)
                .build();
    }

    @Test
    void updateAccountSuccessfully() {
        Account account = sampleAccount();
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account result = accountUpdateService.updateAccount(1L,
                new AccountUpdateService.AccountUpdateCommand(
                        new BigDecimal("10000.00"), null, null, null, null, null, 0L));

        assertThat(result.getCreditLimit()).isEqualByComparingTo("10000.00");
    }

    @Test
    void updateAccountThrowsWhenNotFound() {
        when(accountRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountUpdateService.updateAccount(999L,
                new AccountUpdateService.AccountUpdateCommand(
                        null, null, null, null, null, null, null)))
                .isInstanceOf(TransactionTypeService.ResourceNotFoundException.class);
    }

    @Test
    void updateAccountThrowsOnVersionMismatch() {
        Account account = sampleAccount();
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountUpdateService.updateAccount(1L,
                new AccountUpdateService.AccountUpdateCommand(
                        null, null, null, null, null, null, 5L)))
                .isInstanceOf(CardService.OptimisticLockConflictException.class);
    }

    @Test
    void updateAccountRejectsNegativeCreditLimit() {
        Account account = sampleAccount();
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountUpdateService.updateAccount(1L,
                new AccountUpdateService.AccountUpdateCommand(
                        new BigDecimal("-100.00"), null, null, null, null, null, 0L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credit limit must not be negative");
    }

    @Test
    void updateAccountRejectsCashLimitExceedingCreditLimit() {
        Account account = sampleAccount();
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountUpdateService.updateAccount(1L,
                new AccountUpdateService.AccountUpdateCommand(
                        null, new BigDecimal("99999.00"), null, null, null, null, 0L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cash credit limit cannot exceed credit limit");
    }

    @Test
    void updateAccountRejectsInvalidStatus() {
        Account account = sampleAccount();
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountUpdateService.updateAccount(1L,
                new AccountUpdateService.AccountUpdateCommand(
                        null, null, "X", null, null, null, 0L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid account status");
    }

    @Test
    void updateAccountRejectsPastExpirationDate() {
        Account account = sampleAccount();
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountUpdateService.updateAccount(1L,
                new AccountUpdateService.AccountUpdateCommand(
                        null, null, null, LocalDate.of(2020, 1, 1), null, null, 0L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Expiration date cannot be in the past");
    }

    @Test
    void updateAccountRejectsReissueDateAfterExpiration() {
        Account account = sampleAccount();
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountUpdateService.updateAccount(1L,
                new AccountUpdateService.AccountUpdateCommand(
                        null, null, null, null,
                        LocalDate.of(2030, 1, 1), null, 0L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Reissue date cannot be after expiration date");
    }

    @Test
    void updateAccountAllowsStatusChangeToN() {
        Account account = sampleAccount();
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account result = accountUpdateService.updateAccount(1L,
                new AccountUpdateService.AccountUpdateCommand(
                        null, null, "N", null, null, null, 0L));

        assertThat(result.getActiveStatus()).isEqualTo("N");
    }
}
