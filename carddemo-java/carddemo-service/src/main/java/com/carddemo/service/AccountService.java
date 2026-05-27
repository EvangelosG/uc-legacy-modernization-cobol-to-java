package com.carddemo.service;

import com.carddemo.domain.entity.Account;
import com.carddemo.domain.entity.Card;
import com.carddemo.domain.entity.CardCrossReference;
import com.carddemo.domain.entity.Customer;
import com.carddemo.domain.repository.AccountRepository;
import com.carddemo.domain.repository.CardCrossReferenceRepository;
import com.carddemo.domain.repository.CardRepository;
import com.carddemo.domain.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final CardRepository cardRepository;
    private final CardCrossReferenceRepository cardCrossReferenceRepository;

    @Transactional(readOnly = true)
    public AccountDetail getAccountDetail(Long acctId) {
        Account account = accountRepository.findById(acctId)
                .orElseThrow(() -> new TransactionTypeService.ResourceNotFoundException(
                        "Account not found: " + acctId));

        List<CardCrossReference> xrefs = cardCrossReferenceRepository.findByAcctId(acctId);
        Customer customer = null;
        if (!xrefs.isEmpty()) {
            Long custId = xrefs.get(0).getCustId();
            customer = customerRepository.findById(custId).orElse(null);
        }

        List<Card> cards = cardRepository.findByAcctId(acctId);

        return new AccountDetail(account, customer, cards);
    }

    public record AccountDetail(Account account, Customer customer, List<Card> cards) {}
}
