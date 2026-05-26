package com.carddemo.service;

import com.carddemo.domain.entity.Transaction;
import com.carddemo.domain.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

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
}
