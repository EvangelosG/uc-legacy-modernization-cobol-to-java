package com.carddemo.domain.repository;

import com.carddemo.domain.entity.TransactionCategory;
import com.carddemo.domain.entity.TransactionCategoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionCategoryRepository extends JpaRepository<TransactionCategory, TransactionCategoryId> {
}
