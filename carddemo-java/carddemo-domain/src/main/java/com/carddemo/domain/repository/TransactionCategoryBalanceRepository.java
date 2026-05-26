package com.carddemo.domain.repository;

import com.carddemo.domain.entity.TransactionCategoryBalance;
import com.carddemo.domain.entity.TransactionCategoryBalanceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionCategoryBalanceRepository
        extends JpaRepository<TransactionCategoryBalance, TransactionCategoryBalanceId> {
}
