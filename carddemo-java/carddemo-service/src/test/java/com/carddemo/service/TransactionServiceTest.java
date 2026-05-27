package com.carddemo.service;

import com.carddemo.domain.entity.Account;
import com.carddemo.domain.entity.CardCrossReference;
import com.carddemo.domain.entity.Transaction;
import com.carddemo.domain.repository.AccountRepository;
import com.carddemo.domain.repository.CardCrossReferenceRepository;
import com.carddemo.domain.repository.TransactionRepository;
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
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private CardCrossReferenceRepository cardCrossReferenceRepository;
    @Mock
    private AccountRepository accountRepository;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(transactionRepository,
                cardCrossReferenceRepository, accountRepository);
    }

    private CardCrossReference sampleXref() {
        return CardCrossReference.builder()
                .cardNum("4111111111111111").custId(100L).acctId(1L).build();
    }

    private Account sampleAccount() {
        return Account.builder().acctId(1L).activeStatus("Y")
                .currBal(new BigDecimal("500.00")).creditLimit(new BigDecimal("5000.00"))
                .cashCreditLimit(new BigDecimal("1000.00"))
                .currCycCredit(BigDecimal.ZERO).currCycDebit(BigDecimal.ZERO).version(0L).build();
    }

    @Test
    void createTransactionSuccessfully() {
        when(cardCrossReferenceRepository.findByCardNum("4111111111111111"))
                .thenReturn(Optional.of(sampleXref()));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sampleAccount()));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionService.TransactionCreateRequest request = new TransactionService.TransactionCreateRequest(
                "01", 1, "POS", "Coffee Shop", new BigDecimal("4.50"),
                null, "Starbucks", "Seattle", "98101",
                "4111111111111111", null);

        Transaction result = transactionService.createTransaction(request);
        assertThat(result.getTranId()).hasSize(16);
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("4.50"));
        assertThat(result.getCardNum()).isEqualTo("4111111111111111");
        assertThat(result.getProcTs()).isNotNull();
    }

    @Test
    void createTransactionWithDateValidation() {
        when(cardCrossReferenceRepository.findByCardNum("4111111111111111"))
                .thenReturn(Optional.of(sampleXref()));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sampleAccount()));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionService.TransactionCreateRequest request = new TransactionService.TransactionCreateRequest(
                "01", 1, "POS", "Purchase", new BigDecimal("25.00"),
                null, "Store", null, null,
                "4111111111111111", LocalDate.of(2024, 6, 15));

        Transaction result = transactionService.createTransaction(request);
        assertThat(result.getOrigTs().toLocalDate()).isEqualTo(LocalDate.of(2024, 6, 15));
    }

    @Test
    void createTransactionThrowsOnFutureDate() {
        when(cardCrossReferenceRepository.findByCardNum("4111111111111111"))
                .thenReturn(Optional.of(sampleXref()));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sampleAccount()));

        TransactionService.TransactionCreateRequest request = new TransactionService.TransactionCreateRequest(
                "01", 1, "POS", "Future", new BigDecimal("25.00"),
                null, "Store", null, null,
                "4111111111111111", LocalDate.of(2099, 12, 31));

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");
    }

    @Test
    void createTransactionThrowsOnMissingCardNum() {
        TransactionService.TransactionCreateRequest request = new TransactionService.TransactionCreateRequest(
                "01", 1, "POS", "Purchase", new BigDecimal("25.00"),
                null, "Store", null, null, null, null);

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Card number is required");
    }

    @Test
    void createTransactionThrowsOnInvalidCardNum() {
        TransactionService.TransactionCreateRequest request = new TransactionService.TransactionCreateRequest(
                "01", 1, "POS", "Purchase", new BigDecimal("25.00"),
                null, "Store", null, null, "123", null);

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("16 characters");
    }

    @Test
    void createTransactionThrowsOnCardNotInXref() {
        when(cardCrossReferenceRepository.findByCardNum("9999999999999999"))
                .thenReturn(Optional.empty());

        TransactionService.TransactionCreateRequest request = new TransactionService.TransactionCreateRequest(
                "01", 1, "POS", "Purchase", new BigDecimal("25.00"),
                null, "Store", null, null, "9999999999999999", null);

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(TransactionTypeService.ResourceNotFoundException.class)
                .hasMessageContaining("cross-reference");
    }

    @Test
    void createTransactionThrowsOnNullAmount() {
        when(cardCrossReferenceRepository.findByCardNum("4111111111111111"))
                .thenReturn(Optional.of(sampleXref()));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sampleAccount()));

        TransactionService.TransactionCreateRequest request = new TransactionService.TransactionCreateRequest(
                "01", 1, "POS", "Purchase", null,
                null, "Store", null, null, "4111111111111111", null);

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount is required");
    }

    @Test
    void createTransactionThrowsOnNegativeAmount() {
        when(cardCrossReferenceRepository.findByCardNum("4111111111111111"))
                .thenReturn(Optional.of(sampleXref()));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sampleAccount()));

        TransactionService.TransactionCreateRequest request = new TransactionService.TransactionCreateRequest(
                "01", 1, "POS", "Purchase", new BigDecimal("-10.00"),
                null, "Store", null, null, "4111111111111111", null);

        assertThatThrownBy(() -> transactionService.createTransaction(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void createTransactionAmountUsesScale2RoundHalfUp() {
        when(cardCrossReferenceRepository.findByCardNum("4111111111111111"))
                .thenReturn(Optional.of(sampleXref()));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sampleAccount()));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        TransactionService.TransactionCreateRequest request = new TransactionService.TransactionCreateRequest(
                "01", 1, "POS", "Purchase", new BigDecimal("10.555"),
                null, "Store", null, null, "4111111111111111", null);

        Transaction result = transactionService.createTransaction(request);
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("10.56"));
        assertThat(result.getAmount().scale()).isEqualTo(2);
    }
}
