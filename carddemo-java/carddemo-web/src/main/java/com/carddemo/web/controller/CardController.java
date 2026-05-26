package com.carddemo.web.controller;

import com.carddemo.domain.entity.Account;
import com.carddemo.domain.entity.Card;
import com.carddemo.service.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @GetMapping("/{cardNumber}")
    public ResponseEntity<CardResponse> getCard(@PathVariable("cardNumber") String cardNumber) {
        CardService.CardDetail detail = cardService.getCardDetail(cardNumber);
        return ResponseEntity.ok(CardResponse.from(detail));
    }

    public record CardResponse(
            String cardNum,
            Long acctId,
            Integer cvvCd,
            String embossedName,
            LocalDate expirationDate,
            String activeStatus,
            AccountSummary account
    ) {
        static CardResponse from(CardService.CardDetail detail) {
            Card c = detail.card();
            AccountSummary as = detail.account() != null
                    ? AccountSummary.from(detail.account()) : null;
            return new CardResponse(
                    c.getCardNum(), c.getAcctId(), c.getCvvCd(),
                    c.getEmbossedName(), c.getExpirationDate(), c.getActiveStatus(), as
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
