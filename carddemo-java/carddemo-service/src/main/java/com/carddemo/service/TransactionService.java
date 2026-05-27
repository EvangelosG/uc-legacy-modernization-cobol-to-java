package com.carddemo.service;

import com.carddemo.domain.entity.CardCrossReference;
import com.carddemo.domain.entity.Transaction;
import com.carddemo.domain.repository.CardCrossReferenceRepository;
import com.carddemo.domain.repository.AccountRepository;
import com.carddemo.domain.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CardCrossReferenceRepository cardCrossReferenceRepository;
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public Page<Transaction> listByAccount(Long accountId, Pageable pageable) {
        return transactionRepository.findByAccountIdOrderByTimestampDesc(accountId, pageable);
    }

    @Transactional(readOnly = true)
    public Transaction getTransaction(String tranId) {
        return transactionRepository.findById(tranId)
                .orElseThrow(() -> new TransactionTypeService.ResourceNotFoundException(
                        "Transaction not found: " + tranId));
    }

    @Transactional
    public Transaction createTransaction(TransactionCreateRequest request) {
        if (request.cardNum() == null || request.cardNum().isBlank()) {
            throw new IllegalArgumentException("Card number is required");
        }
        if (request.cardNum().length() != 16) {
            throw new IllegalArgumentException("Card number must be exactly 16 characters");
        }

        CardCrossReference xref = cardCrossReferenceRepository.findByCardNum(request.cardNum())
                .orElseThrow(() -> new TransactionTypeService.ResourceNotFoundException(
                        "Card not found in cross-reference: " + request.cardNum()));

        accountRepository.findById(xref.getAcctId())
                .orElseThrow(() -> new TransactionTypeService.ResourceNotFoundException(
                        "Account not found for card: " + request.cardNum()));

        if (request.amount() == null) {
            throw new IllegalArgumentException("Transaction amount is required");
        }
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be positive");
        }

        if (request.typeCd() == null || request.typeCd().isBlank()) {
            throw new IllegalArgumentException("Transaction type code is required");
        }

        if (request.transactionDate() != null) {
            validateTransactionDate(request.transactionDate());
        }

        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);

        String tranId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        Transaction transaction = Transaction.builder()
                .tranId(tranId)
                .typeCd(request.typeCd())
                .catCd(request.catCd() != null ? request.catCd() : 0)
                .source(request.source())
                .description(request.description())
                .amount(amount)
                .merchantId(request.merchantId())
                .merchantName(request.merchantName())
                .merchantCity(request.merchantCity())
                .merchantZip(request.merchantZip())
                .cardNum(request.cardNum())
                .origTs(request.transactionDate() != null
                        ? request.transactionDate().atStartOfDay()
                        : LocalDateTime.now())
                .procTs(LocalDateTime.now())
                .build();

        return transactionRepository.save(transaction);
    }

    private void validateTransactionDate(LocalDate date) {
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Transaction date cannot be in the future");
        }
    }

    public record TransactionCreateRequest(
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
            LocalDate transactionDate
    ) {}
}
