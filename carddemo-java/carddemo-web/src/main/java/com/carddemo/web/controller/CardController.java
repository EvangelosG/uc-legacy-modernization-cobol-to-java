package com.carddemo.web.controller;

import com.carddemo.domain.entity.Account;
import com.carddemo.domain.entity.Card;
import com.carddemo.service.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @GetMapping
    public ResponseEntity<Page<CardResponse>> listCards(
            @RequestParam("accountId") Long accountId,
            Pageable pageable) {
        Page<CardResponse> page = cardService.listCardsByAccount(accountId, pageable)
                .map(CardResponse::fromCard);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{cardNumber}")
    public ResponseEntity<CardResponse> getCard(@PathVariable("cardNumber") String cardNumber) {
        CardService.CardDetail detail = cardService.getCardDetail(cardNumber);
        return ResponseEntity.ok(CardResponse.from(detail));
    }

    @PutMapping("/{cardNumber}")
    public ResponseEntity<CardResponse> updateCard(
            @PathVariable("cardNumber") String cardNumber,
            @RequestBody CardUpdateRequest request) {
        CardService.CardUpdateRequest serviceRequest = new CardService.CardUpdateRequest(
                request.activeStatus(),
                request.expirationDate(),
                request.embossedName(),
                request.version()
        );
        Card updated = cardService.updateCard(cardNumber, serviceRequest);
        return ResponseEntity.ok(CardResponse.fromCard(updated));
    }

    public record CardUpdateRequest(
            String activeStatus,
            LocalDate expirationDate,
            String embossedName,
            Long version
    ) {}

    public record CardResponse(
            String cardNum,
            Long acctId,
            Integer cvvCd,
            String embossedName,
            LocalDate expirationDate,
            String activeStatus,
            Long version,
            AccountSummary account
    ) {
        static CardResponse from(CardService.CardDetail detail) {
            Card c = detail.card();
            AccountSummary as = detail.account() != null
                    ? AccountSummary.from(detail.account()) : null;
            return new CardResponse(
                    c.getCardNum(), c.getAcctId(), c.getCvvCd(),
                    c.getEmbossedName(), c.getExpirationDate(), c.getActiveStatus(),
                    c.getVersion(), as
            );
        }

        static CardResponse fromCard(Card c) {
            return new CardResponse(
                    c.getCardNum(), c.getAcctId(), c.getCvvCd(),
                    c.getEmbossedName(), c.getExpirationDate(), c.getActiveStatus(),
                    c.getVersion(), null
            );
        }
    }

    public record AccountSummary(Long acctId, String activeStatus, BigDecimal currBal,
                                 BigDecimal creditLimit) {
        static AccountSummary from(Account a) {
            return new AccountSummary(a.getAcctId(), a.getActiveStatus(),
                    a.getCurrBal(), a.getCreditLimit());
        }
    }
}
