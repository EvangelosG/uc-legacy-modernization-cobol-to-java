package com.carddemo.web.controller;

import com.carddemo.service.AuthorizationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * REST controller for authorization processing.
 * Replaces MQ-based request/response pattern from COPAUA0C.
 * POST /api/authorizations/request — submit auth request.
 */
@RestController
@RequestMapping("/api/authorizations")
@RequiredArgsConstructor
public class AuthorizationController {

    private final AuthorizationService authorizationService;

    @PostMapping("/request")
    public ResponseEntity<AuthorizationResponse> submitAuthorizationRequest(
            @Valid @RequestBody AuthorizationRequestBody request) {

        AuthorizationService.AuthorizationResult result =
                authorizationService.processAuthorizationRequest(
                        new AuthorizationService.AuthorizationRequest(
                                request.cardNum(),
                                request.authDate(),
                                request.authTime(),
                                request.authType(),
                                request.cardExpiryDate(),
                                request.messageType(),
                                request.messageSource(),
                                request.processingCode(),
                                request.transactionAmt(),
                                request.merchantCategoryCode(),
                                request.acqrCountryCode(),
                                request.posEntryMode(),
                                request.merchantId(),
                                request.merchantName(),
                                request.merchantCity(),
                                request.merchantState(),
                                request.merchantZip(),
                                request.transactionId()
                        ));

        return ResponseEntity.ok(AuthorizationResponse.from(result));
    }

    public record AuthorizationRequestBody(
            @NotBlank String cardNum,
            LocalDate authDate,
            String authTime,
            String authType,
            String cardExpiryDate,
            String messageType,
            String messageSource,
            String processingCode,
            @NotNull BigDecimal transactionAmt,
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

    public record AuthorizationResponse(
            String cardNum,
            String transactionId,
            String authIdCode,
            String authRespCode,
            String authRespReason,
            BigDecimal approvedAmt
    ) {
        static AuthorizationResponse from(AuthorizationService.AuthorizationResult r) {
            return new AuthorizationResponse(
                    r.cardNum(), r.transactionId(), r.authIdCode(),
                    r.authRespCode(), r.authRespReason(), r.approvedAmt());
        }
    }
}
