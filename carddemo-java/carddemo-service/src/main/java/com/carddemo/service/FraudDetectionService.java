package com.carddemo.service;

import com.carddemo.domain.entity.Authorization;
import com.carddemo.domain.entity.AuthorizationFraudRecord;
import com.carddemo.domain.repository.AuthorizationFraudRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Replaces COPAUS2C.cbl (244 LOC) — DB2 fraud check and recording.
 * Handles fraud detection, flagging, and removal for authorization records.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private static final String FRAUD_CONFIRMED = "F";
    private static final String FRAUD_REMOVED = "R";

    private final AuthorizationFraudRecordRepository fraudRecordRepository;

    /**
     * Checks if a card has been flagged for fraud.
     */
    @Transactional(readOnly = true)
    public boolean isCardFlagged(String cardNum) {
        return fraudRecordRepository.existsByCardNumAndAuthFraud(cardNum, FRAUD_CONFIRMED);
    }

    /**
     * Reports fraud for an authorization.
     * Replaces the WS-REPORT-FRAUD path in COPAUS2C.
     */
    @Transactional
    public FraudActionResult reportFraud(Authorization auth) {
        AuthorizationFraudRecord record = AuthorizationFraudRecord.builder()
                .cardNum(auth.getCardNum())
                .authTs(auth.getCreatedAt() != null ? auth.getCreatedAt() : LocalDateTime.now())
                .authType(auth.getAuthType())
                .cardExpiryDate(auth.getCardExpiry())
                .messageType(auth.getMessageType())
                .messageSource(auth.getMessageSource())
                .authIdCode(auth.getAuthIdCode())
                .authRespCode(auth.getAuthRespCode())
                .authRespReason(auth.getAuthRespReason())
                .processingCode(auth.getProcessingCode())
                .transactionAmt(auth.getTransactionAmt())
                .approvedAmt(auth.getApprovedAmt())
                .merchantCategoryCode(auth.getMerchantCategoryCode())
                .acqrCountryCode(auth.getAcqrCountryCode())
                .posEntryMode(auth.getPosEntryMode())
                .merchantId(auth.getMerchantId())
                .merchantName(auth.getMerchantName())
                .merchantCity(auth.getMerchantCity())
                .merchantState(auth.getMerchantState())
                .merchantZip(auth.getMerchantZip())
                .transactionId(auth.getTransactionId())
                .matchStatus(auth.getMatchStatus())
                .authFraud(FRAUD_CONFIRMED)
                .fraudRptDate(LocalDate.now())
                .acctId(auth.getAcctId())
                .custId(auth.getCustId())
                .build();

        fraudRecordRepository.save(record);
        log.info("Fraud reported for card {}, auth_id={}", auth.getCardNum(), auth.getAuthId());

        return new FraudActionResult(true, "Fraud reported successfully");
    }

    /**
     * Removes fraud flag from a card's fraud records.
     * Replaces the WS-REMOVE-FRAUD path in COPAUS2C.
     */
    @Transactional
    public FraudActionResult removeFraud(String cardNum) {
        List<AuthorizationFraudRecord> records = fraudRecordRepository.findByCardNum(cardNum);

        if (records.isEmpty()) {
            return new FraudActionResult(false, "No fraud records found for card");
        }

        int updated = 0;
        for (AuthorizationFraudRecord record : records) {
            if (FRAUD_CONFIRMED.equals(record.getAuthFraud())) {
                record.setAuthFraud(FRAUD_REMOVED);
                record.setFraudRptDate(LocalDate.now());
                fraudRecordRepository.save(record);
                updated++;
            }
        }

        log.info("Fraud removed for card {}, {} records updated", cardNum, updated);
        return new FraudActionResult(true,
                "Fraud removed: " + updated + " record(s) updated");
    }

    /**
     * Gets all fraud records for an account.
     */
    @Transactional(readOnly = true)
    public List<AuthorizationFraudRecord> getFraudRecordsByAccount(Long acctId) {
        return fraudRecordRepository.findByAcctId(acctId);
    }

    public record FraudActionResult(boolean success, String message) {}
}
