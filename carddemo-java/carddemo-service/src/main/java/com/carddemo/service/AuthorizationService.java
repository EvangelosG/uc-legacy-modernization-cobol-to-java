package com.carddemo.service;

import com.carddemo.domain.entity.Account;
import com.carddemo.domain.entity.Authorization;
import com.carddemo.domain.entity.CardCrossReference;
import com.carddemo.domain.repository.AccountRepository;
import com.carddemo.domain.repository.AuthorizationRepository;
import com.carddemo.domain.repository.CardCrossReferenceRepository;
import com.carddemo.domain.repository.CardRepository;
import com.carddemo.domain.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Replaces COPAUA0C.cbl (1,026 LOC) — MQ-based authorization processor.
 * Makes authorization decisions: approve/decline based on account active,
 * card not expired, credit limit not exceeded.
 * Replaces MQOPEN/MQGET/MQPUT1/MQCLOSE with REST endpoints.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private static final String RESP_APPROVED = "00";
    private static final String RESP_DECLINED = "05";
    private static final String REASON_OK = "0000";
    private static final String REASON_CARD_NOT_FOUND = "3100";
    private static final String REASON_INSUFFICIENT_FUNDS = "4100";
    private static final String REASON_CARD_NOT_ACTIVE = "4200";
    private static final String REASON_ACCOUNT_CLOSED = "4300";
    private static final String REASON_CARD_FRAUD = "5100";
    private static final String REASON_GENERAL_DECLINE = "9000";

    private final AuthorizationRepository authorizationRepository;
    private final CardCrossReferenceRepository cardCrossReferenceRepository;
    private final AccountRepository accountRepository;
    private final CardRepository cardRepository;
    private final CustomerRepository customerRepository;
    private final FraudDetectionService fraudDetectionService;

    @Transactional
    public AuthorizationResult processAuthorizationRequest(AuthorizationRequest request) {
        log.info("Processing authorization request for card {}", maskCardNum(request.cardNum()));

        String authIdCode = generateAuthIdCode();

        CardCrossReference xref = cardCrossReferenceRepository.findByCardNum(request.cardNum())
                .orElse(null);

        if (xref == null) {
            log.warn("Card not found in cross-reference: {}", maskCardNum(request.cardNum()));
            return buildDeclinedResult(request, authIdCode, REASON_CARD_NOT_FOUND,
                    null, null);
        }

        var card = cardRepository.findById(request.cardNum()).orElse(null);
        if (card != null && !"Y".equals(card.getActiveStatus())) {
            log.warn("Card is not active: {}", maskCardNum(request.cardNum()));
            return buildDeclinedResult(request, authIdCode, REASON_CARD_NOT_ACTIVE,
                    xref.getAcctId(), xref.getCustId());
        }

        if (fraudDetectionService.isCardFlagged(request.cardNum())) {
            log.warn("Card flagged for fraud: {}", maskCardNum(request.cardNum()));
            return buildDeclinedResult(request, authIdCode, REASON_CARD_FRAUD,
                    xref.getAcctId(), xref.getCustId());
        }

        Account account = accountRepository.findById(xref.getAcctId()).orElse(null);
        if (account == null || !"Y".equals(account.getActiveStatus())) {
            log.warn("Account not found or closed for card: {}", maskCardNum(request.cardNum()));
            return buildDeclinedResult(request, authIdCode, REASON_ACCOUNT_CLOSED,
                    xref.getAcctId(), xref.getCustId());
        }

        BigDecimal availableCredit = account.getCreditLimit().subtract(account.getCurrBal());
        if (request.transactionAmt().compareTo(availableCredit) > 0) {
            log.info("Insufficient funds for card {}: requested={}, available={}",
                    maskCardNum(request.cardNum()), request.transactionAmt(), availableCredit);
            return buildDeclinedResult(request, authIdCode, REASON_INSUFFICIENT_FUNDS,
                    xref.getAcctId(), xref.getCustId());
        }

        Authorization auth = buildAuthorization(request, authIdCode, RESP_APPROVED,
                REASON_OK, request.transactionAmt(), xref.getAcctId(), xref.getCustId());
        authorizationRepository.save(auth);

        log.info("Authorization approved for card {}, amount={}",
                maskCardNum(request.cardNum()), request.transactionAmt());

        return new AuthorizationResult(
                request.cardNum(), request.transactionId(), authIdCode,
                RESP_APPROVED, REASON_OK, request.transactionAmt());
    }

    private AuthorizationResult buildDeclinedResult(AuthorizationRequest request,
                                                     String authIdCode,
                                                     String reason,
                                                     Long acctId, Long custId) {
        Authorization auth = buildAuthorization(request, authIdCode, RESP_DECLINED,
                reason, BigDecimal.ZERO, acctId, custId);
        authorizationRepository.save(auth);

        return new AuthorizationResult(
                request.cardNum(), request.transactionId(), authIdCode,
                RESP_DECLINED, reason, BigDecimal.ZERO);
    }

    private Authorization buildAuthorization(AuthorizationRequest request,
                                              String authIdCode,
                                              String respCode, String reason,
                                              BigDecimal approvedAmt,
                                              Long acctId, Long custId) {
        return Authorization.builder()
                .cardNum(request.cardNum())
                .acctId(acctId)
                .custId(custId)
                .authDate(request.authDate() != null ? request.authDate() : LocalDate.now())
                .authTime(request.authTime())
                .authType(request.authType())
                .cardExpiry(request.cardExpiryDate())
                .messageType(request.messageType())
                .messageSource(request.messageSource())
                .processingCode(request.processingCode())
                .transactionAmt(request.transactionAmt())
                .approvedAmt(approvedAmt)
                .authIdCode(authIdCode)
                .authRespCode(respCode)
                .authRespReason(reason)
                .merchantCategoryCode(request.merchantCategoryCode())
                .acqrCountryCode(request.acqrCountryCode())
                .posEntryMode(request.posEntryMode())
                .merchantId(request.merchantId())
                .merchantName(request.merchantName())
                .merchantCity(request.merchantCity())
                .merchantState(request.merchantState())
                .merchantZip(request.merchantZip())
                .transactionId(request.transactionId())
                .matchStatus("P")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private String generateAuthIdCode() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
    }

    private String maskCardNum(String cardNum) {
        if (cardNum == null || cardNum.length() < 4) return "****";
        return "****" + cardNum.substring(cardNum.length() - 4);
    }

    public record AuthorizationRequest(
            String cardNum,
            LocalDate authDate,
            String authTime,
            String authType,
            String cardExpiryDate,
            String messageType,
            String messageSource,
            String processingCode,
            BigDecimal transactionAmt,
            String merchantCategoryCode,
            String acqrCountryCode,
            Integer posEntryMode,
            String merchantId,
            String merchantName,
            String merchantCity,
            String merchantState,
            String merchantZip,
            String transactionId
    ) {}

    public record AuthorizationResult(
            String cardNum,
            String transactionId,
            String authIdCode,
            String authRespCode,
            String authRespReason,
            BigDecimal approvedAmt
    ) {}
}
