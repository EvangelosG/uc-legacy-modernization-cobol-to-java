package com.carddemo.service;

import com.carddemo.domain.entity.Account;
import com.carddemo.domain.entity.Card;
import com.carddemo.domain.entity.CardCrossReference;
import com.carddemo.domain.repository.AccountRepository;
import com.carddemo.domain.repository.CardCrossReferenceRepository;
import com.carddemo.domain.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;
    @Mock
    private CardCrossReferenceRepository cardCrossReferenceRepository;
    @Mock
    private AccountRepository accountRepository;

    private CardService cardService;

    @BeforeEach
    void setUp() {
        cardService = new CardService(cardRepository, cardCrossReferenceRepository, accountRepository);
    }

    private Card sampleCard() {
        return Card.builder()
                .cardNum("4111111111111111").acctId(1L).cvvCd(123)
                .embossedName("JOHN DOE").expirationDate(LocalDate.of(2027, 12, 31))
                .activeStatus("Y").version(0L).build();
    }

    private CardCrossReference sampleXref() {
        return CardCrossReference.builder()
                .cardNum("4111111111111111").custId(100L).acctId(1L).build();
    }

    @Test
    void getCardDetailReturnsCardAndAccount() {
        Card card = sampleCard();
        CardCrossReference xref = sampleXref();
        Account account = Account.builder().acctId(1L).activeStatus("Y")
                .currBal(new BigDecimal("100.00")).creditLimit(new BigDecimal("5000.00"))
                .cashCreditLimit(new BigDecimal("1000.00"))
                .currCycCredit(BigDecimal.ZERO).currCycDebit(BigDecimal.ZERO).version(0L).build();

        when(cardRepository.findById("4111111111111111")).thenReturn(Optional.of(card));
        when(cardCrossReferenceRepository.findByCardNum("4111111111111111")).thenReturn(Optional.of(xref));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        CardService.CardDetail detail = cardService.getCardDetail("4111111111111111");
        assertThat(detail.card().getCardNum()).isEqualTo("4111111111111111");
        assertThat(detail.account().getAcctId()).isEqualTo(1L);
    }

    @Test
    void getCardDetailThrowsWhenCardNotFound() {
        when(cardRepository.findById("9999999999999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getCardDetail("9999999999999999"))
                .isInstanceOf(TransactionTypeService.ResourceNotFoundException.class);
    }

    @Test
    void listCardsByAccountReturnsPaginatedResults() {
        Card card = sampleCard();
        Pageable pageable = PageRequest.of(0, 20);
        when(cardRepository.findByAccountId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(card), pageable, 1));

        Page<Card> result = cardService.listCardsByAccount(1L, pageable);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getCardNum()).isEqualTo("4111111111111111");
    }

    @Test
    void updateCardSuccessfully() {
        Card card = sampleCard();
        CardCrossReference xref = sampleXref();

        when(cardRepository.findById("4111111111111111")).thenReturn(Optional.of(card));
        when(cardCrossReferenceRepository.findByCardNum("4111111111111111")).thenReturn(Optional.of(xref));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        CardService.CardUpdateRequest request = new CardService.CardUpdateRequest(
                "N", LocalDate.of(2028, 6, 15), "JANE DOE", 0L);

        Card updated = cardService.updateCard("4111111111111111", request);
        assertThat(updated.getActiveStatus()).isEqualTo("N");
        assertThat(updated.getEmbossedName()).isEqualTo("JANE DOE");
        assertThat(updated.getExpirationDate()).isEqualTo(LocalDate.of(2028, 6, 15));
    }

    @Test
    void updateCardThrowsOnVersionMismatch() {
        Card card = sampleCard();
        CardCrossReference xref = sampleXref();

        when(cardRepository.findById("4111111111111111")).thenReturn(Optional.of(card));
        when(cardCrossReferenceRepository.findByCardNum("4111111111111111")).thenReturn(Optional.of(xref));

        CardService.CardUpdateRequest request = new CardService.CardUpdateRequest(
                "N", null, null, 99L);

        assertThatThrownBy(() -> cardService.updateCard("4111111111111111", request))
                .isInstanceOf(CardService.OptimisticLockConflictException.class)
                .hasMessageContaining("modified by another user");
    }

    @Test
    void updateCardThrowsOnInvalidStatus() {
        Card card = sampleCard();
        CardCrossReference xref = sampleXref();

        when(cardRepository.findById("4111111111111111")).thenReturn(Optional.of(card));
        when(cardCrossReferenceRepository.findByCardNum("4111111111111111")).thenReturn(Optional.of(xref));

        CardService.CardUpdateRequest request = new CardService.CardUpdateRequest(
                "X", null, null, 0L);

        assertThatThrownBy(() -> cardService.updateCard("4111111111111111", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid card status");
    }

    @Test
    void updateCardThrowsOnPastExpirationDate() {
        Card card = sampleCard();
        CardCrossReference xref = sampleXref();

        when(cardRepository.findById("4111111111111111")).thenReturn(Optional.of(card));
        when(cardCrossReferenceRepository.findByCardNum("4111111111111111")).thenReturn(Optional.of(xref));

        CardService.CardUpdateRequest request = new CardService.CardUpdateRequest(
                null, LocalDate.of(2020, 1, 1), null, 0L);

        assertThatThrownBy(() -> cardService.updateCard("4111111111111111", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("past");
    }

    @Test
    void updateCardThrowsOnInvalidEmbossedName() {
        Card card = sampleCard();
        CardCrossReference xref = sampleXref();

        when(cardRepository.findById("4111111111111111")).thenReturn(Optional.of(card));
        when(cardCrossReferenceRepository.findByCardNum("4111111111111111")).thenReturn(Optional.of(xref));

        CardService.CardUpdateRequest request = new CardService.CardUpdateRequest(
                null, null, "INVALID@NAME#123", 0L);

        assertThatThrownBy(() -> cardService.updateCard("4111111111111111", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid characters");
    }

    @Test
    void updateCardThrowsWhenNotInCrossReference() {
        Card card = sampleCard();

        when(cardRepository.findById("4111111111111111")).thenReturn(Optional.of(card));
        when(cardCrossReferenceRepository.findByCardNum("4111111111111111")).thenReturn(Optional.empty());

        CardService.CardUpdateRequest request = new CardService.CardUpdateRequest(
                "N", null, null, 0L);

        assertThatThrownBy(() -> cardService.updateCard("4111111111111111", request))
                .isInstanceOf(TransactionTypeService.ResourceNotFoundException.class)
                .hasMessageContaining("cross-reference");
    }
}
