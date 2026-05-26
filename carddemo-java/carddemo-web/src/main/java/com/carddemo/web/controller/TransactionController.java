package com.carddemo.web.controller;

import com.carddemo.domain.entity.Transaction;
import com.carddemo.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> listTransactions(
            @RequestParam("accountId") Long accountId,
            Pageable pageable) {
        Page<TransactionResponse> page = transactionService.listByAccount(accountId, pageable)
                .map(TransactionResponse::from);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable("id") String id) {
        Transaction txn = transactionService.getTransaction(id);
        return ResponseEntity.ok(TransactionResponse.from(txn));
    }

    public record TransactionResponse(
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
        static TransactionResponse from(Transaction t) {
            return new TransactionResponse(
                    t.getTranId(), t.getTypeCd(), t.getCatCd(),
                    t.getSource(), t.getDescription(), t.getAmount(),
                    t.getMerchantId(), t.getMerchantName(),
                    t.getMerchantCity(), t.getMerchantZip(),
                    t.getCardNum(), t.getOrigTs(), t.getProcTs()
            );
        }
    }
}
