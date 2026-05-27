package com.carddemo.web.controller;

import com.carddemo.domain.entity.Account;
import com.carddemo.domain.entity.Card;
import com.carddemo.domain.entity.CardCrossReference;
import com.carddemo.domain.entity.Customer;
import com.carddemo.domain.repository.AccountRepository;
import com.carddemo.domain.repository.CardCrossReferenceRepository;
import com.carddemo.domain.repository.CardRepository;
import com.carddemo.domain.repository.CustomerRepository;
import com.carddemo.service.TransactionTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * System and extraction endpoints replacing MQ-based utilities:
 * - GET /api/system/date replaces CODATE01 (524 LOC) MQ date inquiry
 * - GET /api/accounts/{id}/extract replaces COACCT01 (620 LOC) MQ account inquiry
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SystemController {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final CardRepository cardRepository;
    private final CardCrossReferenceRepository cardCrossReferenceRepository;

    @GetMapping("/system/date")
    public ResponseEntity<SystemDateResponse> getSystemDate() {
        LocalDateTime now = LocalDateTime.now();
        return ResponseEntity.ok(new SystemDateResponse(
                now.toLocalDate(),
                now.toLocalTime().toString(),
                now
        ));
    }

    @GetMapping("/accounts/{id}/extract")
    public ResponseEntity<AccountExtractResponse> getAccountExtract(@PathVariable("id") Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new TransactionTypeService.ResourceNotFoundException(
                        "Account not found: " + id));

        List<CardCrossReference> xrefs = cardCrossReferenceRepository.findByAcctId(id);

        Customer customer = null;
        if (!xrefs.isEmpty()) {
            customer = customerRepository.findById(xrefs.get(0).getCustId()).orElse(null);
        }

        List<Card> cards = cardRepository.findByAcctId(id);

        return ResponseEntity.ok(AccountExtractResponse.from(account, customer, cards));
    }

    public record SystemDateResponse(
            LocalDate date,
            String time,
            LocalDateTime timestamp
    ) {}

    public record AccountExtractResponse(
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
            CustomerExtract customer,
            List<CardExtract> cards
    ) {
        static AccountExtractResponse from(Account a, Customer c, List<Card> cards) {
            CustomerExtract ce = c != null ? CustomerExtract.from(c) : null;
            List<CardExtract> cardList = cards.stream().map(CardExtract::from).toList();
            return new AccountExtractResponse(
                    a.getAcctId(), a.getActiveStatus(), a.getCurrBal(),
                    a.getCreditLimit(), a.getCashCreditLimit(),
                    a.getOpenDate(), a.getExpirationDate(), a.getReissueDate(),
                    a.getCurrCycCredit(), a.getCurrCycDebit(),
                    a.getAddrZip(), a.getGroupId(), ce, cardList);
        }
    }

    public record CustomerExtract(
            Long custId,
            String firstName,
            String middleName,
            String lastName,
            String addrLine1,
            String addrLine2,
            String addrLine3,
            String addrStateCd,
            String addrCountryCd,
            String addrZip,
            String phoneNum1,
            String phoneNum2,
            String ssn,
            LocalDate dob,
            int ficoCreditScore
    ) {
        static CustomerExtract from(Customer c) {
            return new CustomerExtract(
                    c.getCustId(), c.getFirstName(), c.getMiddleName(), c.getLastName(),
                    c.getAddrLine1(), c.getAddrLine2(), c.getAddrLine3(),
                    c.getAddrStateCd(), c.getAddrCountryCd(), c.getAddrZip(),
                    c.getPhoneNum1(), c.getPhoneNum2(), c.getSsn(),
                    c.getDob(), c.getFicoCreditScore());
        }
    }

    public record CardExtract(
            String cardNum,
            Long acctId,
            String embossedName,
            LocalDate expirationDate,
            String activeStatus
    ) {
        static CardExtract from(Card c) {
            return new CardExtract(
                    c.getCardNum(), c.getAcctId(), c.getEmbossedName(),
                    c.getExpirationDate(), c.getActiveStatus());
        }
    }
}
