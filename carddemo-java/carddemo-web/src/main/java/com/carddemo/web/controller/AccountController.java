package com.carddemo.web.controller;

import com.carddemo.domain.entity.Account;
import com.carddemo.domain.entity.Card;
import com.carddemo.domain.entity.Customer;
import com.carddemo.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable("id") Long id) {
        AccountService.AccountDetail detail = accountService.getAccountDetail(id);
        return ResponseEntity.ok(AccountResponse.from(detail));
    }

    public record AccountResponse(
            Long acctId,
            String activeStatus,
            BigDecimal currBal,
            BigDecimal creditLimit,
            BigDecimal cashCreditLimit,
            LocalDate openDate,
            LocalDate expirationDate,
            LocalDate reissueDate,
            BigDecimal currCycCredit,
            BigDecimal currCycDebit,
            String addrZip,
            String groupId,
            CustomerSummary customer,
            List<CardSummary> cards
    ) {
        static AccountResponse from(AccountService.AccountDetail detail) {
            Account a = detail.account();
            CustomerSummary cs = detail.customer() != null
                    ? CustomerSummary.from(detail.customer()) : null;
            List<CardSummary> cardList = detail.cards().stream()
                    .map(CardSummary::from).toList();
            return new AccountResponse(
                    a.getAcctId(), a.getActiveStatus(), a.getCurrBal(),
                    a.getCreditLimit(), a.getCashCreditLimit(),
                    a.getOpenDate(), a.getExpirationDate(), a.getReissueDate(),
                    a.getCurrCycCredit(), a.getCurrCycDebit(),
                    a.getAddrZip(), a.getGroupId(), cs, cardList
            );
        }
    }

    public record CustomerSummary(Long custId, String firstName, String middleName,
                                  String lastName, String addrZip, int ficoCreditScore) {
        static CustomerSummary from(Customer c) {
            return new CustomerSummary(c.getCustId(), c.getFirstName(), c.getMiddleName(),
                    c.getLastName(), c.getAddrZip(), c.getFicoCreditScore());
        }
    }

    public record CardSummary(String cardNum, String embossedName, LocalDate expirationDate,
                              String activeStatus) {
        static CardSummary from(Card c) {
            return new CardSummary(c.getCardNum(), c.getEmbossedName(),
                    c.getExpirationDate(), c.getActiveStatus());
        }
    }
}
