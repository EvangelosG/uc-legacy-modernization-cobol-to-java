package com.carddemo.service;

import com.carddemo.domain.entity.Account;
import com.carddemo.domain.entity.Card;
import com.carddemo.domain.entity.CardCrossReference;
import com.carddemo.domain.repository.AccountRepository;
import com.carddemo.domain.repository.CardCrossReferenceRepository;
import com.carddemo.domain.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CardService {

    private static final Set<String> VALID_STATUSES = Set.of("Y", "N");
    private static final Set<String> VALID_STATUS_TRANSITIONS_FROM_Y = Set.of("Y", "N");
    private static final Set<String> VALID_STATUS_TRANSITIONS_FROM_N = Set.of("N", "Y");

    private final CardRepository cardRepository;
    private final CardCrossReferenceRepository cardCrossReferenceRepository;
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public CardDetail getCardDetail(String cardNumber) {
        Card card = cardRepository.findById(cardNumber)
                .orElseThrow(() -> new TransactionTypeService.ResourceNotFoundException(
                        "Card not found: " + cardNumber));

        CardCrossReference xref = cardCrossReferenceRepository.findByCardNum(cardNumber)
                .orElse(null);

        Account account = null;
        if (xref != null) {
            account = accountRepository.findById(xref.getAcctId()).orElse(null);
        }

        return new CardDetail(card, account);
    }

    @Transactional(readOnly = true)
    public Page<Card> listCardsByAccount(Long accountId, Pageable pageable) {
        return cardRepository.findByAccountId(accountId, pageable);
    }

    @Transactional
    public Card updateCard(String cardNumber, CardUpdateRequest request) {
        Card card = cardRepository.findById(cardNumber)
                .orElseThrow(() -> new TransactionTypeService.ResourceNotFoundException(
                        "Card not found: " + cardNumber));

        cardCrossReferenceRepository.findByCardNum(cardNumber)
                .orElseThrow(() -> new TransactionTypeService.ResourceNotFoundException(
                        "Card not in cross-reference: " + cardNumber));

        if (request.version() != null && !request.version().equals(card.getVersion())) {
            throw new OptimisticLockConflictException(
                    "Card has been modified by another user. Expected version "
                            + request.version() + " but found " + card.getVersion());
        }

        if (request.activeStatus() != null) {
            validateStatusTransition(card.getActiveStatus(), request.activeStatus());
            card.setActiveStatus(request.activeStatus());
        }

        if (request.expirationDate() != null) {
            validateExpirationDate(request.expirationDate());
            card.setExpirationDate(request.expirationDate());
        }

        if (request.embossedName() != null) {
            validateEmbossedName(request.embossedName());
            card.setEmbossedName(request.embossedName());
        }

        return cardRepository.save(card);
    }

    private void validateStatusTransition(String currentStatus, String newStatus) {
        if (!VALID_STATUSES.contains(newStatus)) {
            throw new IllegalArgumentException("Invalid card status: " + newStatus
                    + ". Must be one of: " + VALID_STATUSES);
        }
        Set<String> allowed = "Y".equals(currentStatus)
                ? VALID_STATUS_TRANSITIONS_FROM_Y
                : VALID_STATUS_TRANSITIONS_FROM_N;
        if (!allowed.contains(newStatus)) {
            throw new IllegalArgumentException("Invalid status transition from "
                    + currentStatus + " to " + newStatus);
        }
    }

    private void validateExpirationDate(LocalDate expirationDate) {
        if (expirationDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Expiration date cannot be in the past");
        }
    }

    private void validateEmbossedName(String embossedName) {
        if (embossedName.isBlank()) {
            throw new IllegalArgumentException("Embossed name must not be blank");
        }
        if (embossedName.length() > 50) {
            throw new IllegalArgumentException("Embossed name must not exceed 50 characters");
        }
        if (!embossedName.matches("^[A-Za-z .'-]+$")) {
            throw new IllegalArgumentException(
                    "Embossed name contains invalid characters. Only letters, spaces, periods, apostrophes, and hyphens are allowed");
        }
    }

    public record CardDetail(Card card, Account account) {}

    public record CardUpdateRequest(
            String activeStatus,
            LocalDate expirationDate,
            String embossedName,
            Long version
    ) {}

    public static class OptimisticLockConflictException extends RuntimeException {
        public OptimisticLockConflictException(String message) {
            super(message);
        }
    }
}
