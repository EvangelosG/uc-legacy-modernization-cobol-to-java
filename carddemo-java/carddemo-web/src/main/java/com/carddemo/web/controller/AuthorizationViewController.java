package com.carddemo.web.controller;

import com.carddemo.domain.entity.Transaction;
import com.carddemo.service.AuthorizationViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/authorizations")
@RequiredArgsConstructor
public class AuthorizationViewController {

    private final AuthorizationViewService authorizationViewService;

    @GetMapping("/pending")
    public ResponseEntity<Page<AuthorizationResponse>> getPendingAuthorizations(
            @RequestParam("accountId") Long accountId,
            Pageable pageable) {
        Page<AuthorizationResponse> page = authorizationViewService
                .getPendingAuthorizations(accountId, pageable)
                .map(AuthorizationResponse::from);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuthorizationResponse> getAuthorization(@PathVariable("id") String id) {
        Transaction txn = authorizationViewService.getAuthorization(id);
        return ResponseEntity.ok(AuthorizationResponse.from(txn));
    }

    public record AuthorizationResponse(
            String tranId,
            String typeCd,
            Integer catCd,
            String source,
            String description,
            BigDecimal amount,
            Long merchantId,
            String merchantName,
            String merchantCity,
            String merchantZip,
            String cardNum,
            LocalDateTime origTs,
            LocalDateTime procTs
    ) {
        static AuthorizationResponse from(Transaction t) {
            return new AuthorizationResponse(
                    t.getTranId(), t.getTypeCd(), t.getCatCd(),
                    t.getSource(), t.getDescription(), t.getAmount(),
                    t.getMerchantId(), t.getMerchantName(),
                    t.getMerchantCity(), t.getMerchantZip(),
                    t.getCardNum(), t.getOrigTs(), t.getProcTs()
            );
        }
    }
}
