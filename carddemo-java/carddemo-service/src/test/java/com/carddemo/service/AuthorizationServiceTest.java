package com.carddemo.service;

import com.carddemo.domain.entity.Account;
import com.carddemo.domain.entity.Card;
import com.carddemo.domain.entity.CardCrossReference;
import com.carddemo.domain.repository.AccountRepository;
import com.carddemo.domain.repository.AuthorizationRepository;
import com.carddemo.domain.repository.CardCrossReferenceRepository;
import com.carddemo.domain.repository.CardRepository;
import com.carddemo.domain.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock private AuthorizationRepository authorizationRepository;
    @Mock private CardCrossReferenceRepository cardCrossReferenceRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private CardRepository cardRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private FraudDetectionService fraudDetectionService;

    private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService(
                authorizationRepository, cardCrossReferenceRepository,
                accountRepository, cardRepository, customerRepository,
                fraudDetectionService);
    }

    private AuthorizationService.AuthorizationRequest sampleRequest(BigDecimal amount) {
        return new AuthorizationService.AuthorizationRequest(
                "4111111111111111", LocalDate.now(), "120000",
                "AUTH", "2712", "ONLINE", "POS", "000001",
                amount, "5411", "US", 5,
                "MERCHANT001", "TEST MERCHANT", "NEW YORK", "NY", "10001",
                "TXN00001");
    }

    private CardCrossReference sampleXref() {
        return CardCrossReference.builder()
                .cardNum("4111111111111111").custId(100L).acctId(1L).build();
    }

    private Account sampleAccount(BigDecimal balance, BigDecimal creditLimit) {
        return Account.builder()
                .acctId(1L).activeStatus("Y")
                .currBal(balance).creditLimit(creditLimit)
                .cashCreditLimit(new BigDecimal("1000.00"))
                .currCycCredit(BigDecimal.ZERO).currCycDebit(BigDecimal.ZERO)
                .version(0L).build();
    }

    private Card sampleCard() {
        return Card.builder()
                .cardNum("4111111111111111").acctId(1L).activeStatus("Y")
                .expirationDate(LocalDate.of(2027, 12, 31)).version(0L).build();
    }

    @Test
    void approvesWhenSufficientFunds() {
        when(cardCrossReferenceRepository.findByCardNum("4111111111111111"))
                .thenReturn(Optional.of(sampleXref()));
        when(cardRepository.findById("4111111111111111"))
                .thenReturn(Optional.of(sampleCard()));
        when(fraudDetectionService.isCardFlagged("4111111111111111")).thenReturn(false);
        when(accountRepository.findById(1L))
                .thenReturn(Optional.of(sampleAccount(new BigDecimal("100.00"),
                        new BigDecimal("5000.00"))));
        when(authorizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = authorizationService.processAuthorizationRequest(
                sampleRequest(new BigDecimal("200.00")));

        assertThat(result.authRespCode()).isEqualTo("00");
        assertThat(result.authRespReason()).isEqualTo("0000");
        assertThat(result.approvedAmt()).isEqualByComparingTo("200.00");
    }

    @Test
    void declinesWhenInsufficientFunds() {
        when(cardCrossReferenceRepository.findByCardNum("4111111111111111"))
                .thenReturn(Optional.of(sampleXref()));
        when(cardRepository.findById("4111111111111111"))
                .thenReturn(Optional.of(sampleCard()));
        when(fraudDetectionService.isCardFlagged("4111111111111111")).thenReturn(false);
        when(accountRepository.findById(1L))
                .thenReturn(Optional.of(sampleAccount(new BigDecimal("4900.00"),
                        new BigDecimal("5000.00"))));
        when(authorizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = authorizationService.processAuthorizationRequest(
                sampleRequest(new BigDecimal("200.00")));

        assertThat(result.authRespCode()).isEqualTo("05");
        assertThat(result.authRespReason()).isEqualTo("4100");
        assertThat(result.approvedAmt()).isEqualByComparingTo("0");
    }

    @Test
    void declinesWhenCardNotFound() {
        when(cardCrossReferenceRepository.findByCardNum("4111111111111111"))
                .thenReturn(Optional.empty());
        when(authorizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = authorizationService.processAuthorizationRequest(
                sampleRequest(new BigDecimal("100.00")));

        assertThat(result.authRespCode()).isEqualTo("05");
        assertThat(result.authRespReason()).isEqualTo("3100");
    }

    @Test
    void declinesWhenCardNotActive() {
        Card inactiveCard = sampleCard();
        inactiveCard.setActiveStatus("N");

        when(cardCrossReferenceRepository.findByCardNum("4111111111111111"))
                .thenReturn(Optional.of(sampleXref()));
        when(cardRepository.findById("4111111111111111"))
                .thenReturn(Optional.of(inactiveCard));
        when(authorizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = authorizationService.processAuthorizationRequest(
                sampleRequest(new BigDecimal("100.00")));

        assertThat(result.authRespCode()).isEqualTo("05");
        assertThat(result.authRespReason()).isEqualTo("4200");
    }

    @Test
    void declinesWhenAccountClosed() {
        Account closedAccount = sampleAccount(BigDecimal.ZERO, new BigDecimal("5000.00"));
        closedAccount.setActiveStatus("N");

        when(cardCrossReferenceRepository.findByCardNum("4111111111111111"))
                .thenReturn(Optional.of(sampleXref()));
        when(cardRepository.findById("4111111111111111"))
                .thenReturn(Optional.of(sampleCard()));
        when(fraudDetectionService.isCardFlagged("4111111111111111")).thenReturn(false);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(closedAccount));
        when(authorizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = authorizationService.processAuthorizationRequest(
                sampleRequest(new BigDecimal("100.00")));

        assertThat(result.authRespCode()).isEqualTo("05");
        assertThat(result.authRespReason()).isEqualTo("4300");
    }

    @Test
    void declinesWhenCardFlagged() {
        when(cardCrossReferenceRepository.findByCardNum("4111111111111111"))
                .thenReturn(Optional.of(sampleXref()));
        when(cardRepository.findById("4111111111111111"))
                .thenReturn(Optional.of(sampleCard()));
        when(fraudDetectionService.isCardFlagged("4111111111111111")).thenReturn(true);
        when(authorizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = authorizationService.processAuthorizationRequest(
                sampleRequest(new BigDecimal("100.00")));

        assertThat(result.authRespCode()).isEqualTo("05");
        assertThat(result.authRespReason()).isEqualTo("5100");
    }
}
