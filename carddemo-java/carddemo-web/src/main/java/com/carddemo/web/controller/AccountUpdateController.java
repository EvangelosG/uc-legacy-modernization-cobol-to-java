package com.carddemo.web.controller;

import com.carddemo.domain.entity.Account;
import com.carddemo.domain.entity.Customer;
import com.carddemo.service.AccountUpdateService;
import com.carddemo.service.CustomerUpdateService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * REST controller for account and customer updates.
 * Replaces COACTUPC.cbl account/customer update screen logic.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AccountUpdateController {

    private final AccountUpdateService accountUpdateService;
    private final CustomerUpdateService customerUpdateService;

    @PutMapping("/accounts/{id}")
    public ResponseEntity<AccountUpdateResponse> updateAccount(
            @PathVariable("id") Long id,
            @Valid @RequestBody AccountUpdateRequest request) {
        Account account = accountUpdateService.updateAccount(id,
                new AccountUpdateService.AccountUpdateCommand(
                        request.creditLimit(),
                        request.cashCreditLimit(),
                        request.activeStatus(),
                        request.expirationDate(),
                        request.reissueDate(),
                        request.groupId(),
                        request.version()
                ));
        return ResponseEntity.ok(AccountUpdateResponse.from(account));
    }

    @PutMapping("/customers/{id}")
    public ResponseEntity<CustomerUpdateResponse> updateCustomer(
            @PathVariable("id") Long id,
            @Valid @RequestBody CustomerUpdateRequest request) {
        Customer customer = customerUpdateService.updateCustomer(id,
                new CustomerUpdateService.CustomerUpdateCommand(
                        request.firstName(),
                        request.middleName(),
                        request.lastName(),
                        request.addrLine1(),
                        request.addrLine2(),
                        request.addrLine3(),
                        request.addrStateCd(),
                        request.addrCountryCd(),
                        request.addrZip(),
                        request.phoneNum1(),
                        request.phoneNum2(),
                        request.ssn(),
                        request.dob(),
                        request.govtIssuedId()
                ));
        return ResponseEntity.ok(CustomerUpdateResponse.from(customer));
    }

    public record AccountUpdateRequest(
            BigDecimal creditLimit,
            BigDecimal cashCreditLimit,
            String activeStatus,
            LocalDate expirationDate,
            LocalDate reissueDate,
            String groupId,
            Long version
    ) {}

    public record CustomerUpdateRequest(
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
            String govtIssuedId
    ) {}

    public record AccountUpdateResponse(
            Long acctId,
            String activeStatus,
            BigDecimal currBal,
            BigDecimal creditLimit,
            BigDecimal cashCreditLimit,
            LocalDate openDate,
            LocalDate expirationDate,
            LocalDate reissueDate,
            String groupId,
            Long version
    ) {
        static AccountUpdateResponse from(Account a) {
            return new AccountUpdateResponse(
                    a.getAcctId(), a.getActiveStatus(), a.getCurrBal(),
                    a.getCreditLimit(), a.getCashCreditLimit(),
                    a.getOpenDate(), a.getExpirationDate(), a.getReissueDate(),
                    a.getGroupId(), a.getVersion());
        }
    }

    public record CustomerUpdateResponse(
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
            String govtIssuedId
    ) {
        static CustomerUpdateResponse from(Customer c) {
            return new CustomerUpdateResponse(
                    c.getCustId(), c.getFirstName(), c.getMiddleName(), c.getLastName(),
                    c.getAddrLine1(), c.getAddrLine2(), c.getAddrLine3(),
                    c.getAddrStateCd(), c.getAddrCountryCd(), c.getAddrZip(),
                    c.getPhoneNum1(), c.getPhoneNum2(), c.getSsn(),
                    c.getDob(), c.getGovtIssuedId());
        }
    }
}
