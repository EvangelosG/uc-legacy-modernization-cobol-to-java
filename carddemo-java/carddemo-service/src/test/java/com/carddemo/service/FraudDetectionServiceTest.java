package com.carddemo.service;

import com.carddemo.domain.entity.Authorization;
import com.carddemo.domain.entity.AuthorizationFraudRecord;
import com.carddemo.domain.repository.AuthorizationFraudRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private AuthorizationFraudRecordRepository fraudRecordRepository;

    private FraudDetectionService fraudDetectionService;

    @BeforeEach
    void setUp() {
        fraudDetectionService = new FraudDetectionService(fraudRecordRepository);
    }

    @Test
    void isCardFlaggedReturnsTrueWhenFraudExists() {
        when(fraudRecordRepository.existsByCardNumAndAuthFraud("4111111111111111", "F"))
                .thenReturn(true);

        assertThat(fraudDetectionService.isCardFlagged("4111111111111111")).isTrue();
    }

    @Test
    void isCardFlaggedReturnsFalseWhenNoFraud() {
        when(fraudRecordRepository.existsByCardNumAndAuthFraud("4111111111111111", "F"))
                .thenReturn(false);

        assertThat(fraudDetectionService.isCardFlagged("4111111111111111")).isFalse();
    }

    @Test
    void reportFraudCreatesRecord() {
        Authorization auth = Authorization.builder()
                .authId(1L).cardNum("4111111111111111").acctId(1L).custId(100L)
                .authDate(LocalDate.now()).transactionAmt(new BigDecimal("100.00"))
                .createdAt(LocalDateTime.now()).build();

        when(fraudRecordRepository.save(any(AuthorizationFraudRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var result = fraudDetectionService.reportFraud(auth);
        assertThat(result.success()).isTrue();
    }

    @Test
    void removeFraudUpdatesRecords() {
        AuthorizationFraudRecord record = AuthorizationFraudRecord.builder()
                .id(1L).cardNum("4111111111111111").authFraud("F")
                .authTs(LocalDateTime.now()).build();

        when(fraudRecordRepository.findByCardNum("4111111111111111"))
                .thenReturn(List.of(record));
        when(fraudRecordRepository.save(any(AuthorizationFraudRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var result = fraudDetectionService.removeFraud("4111111111111111");
        assertThat(result.success()).isTrue();
        assertThat(record.getAuthFraud()).isEqualTo("R");
    }

    @Test
    void removeFraudReturnsFailureWhenNoRecords() {
        when(fraudRecordRepository.findByCardNum("9999999999999999"))
                .thenReturn(List.of());

        var result = fraudDetectionService.removeFraud("9999999999999999");
        assertThat(result.success()).isFalse();
    }
}
