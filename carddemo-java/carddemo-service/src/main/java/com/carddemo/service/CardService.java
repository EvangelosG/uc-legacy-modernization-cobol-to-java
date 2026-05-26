package com.carddemo.service;

import com.carddemo.domain.entity.Account;
import com.carddemo.domain.entity.Card;
import com.carddemo.domain.entity.CardCrossReference;
import com.carddemo.domain.repository.AccountRepository;
import com.carddemo.domain.repository.CardCrossReferenceRepository;
import com.carddemo.domain.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CardService {

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

    public record CardDetail(Card card, Account account) {}
}
