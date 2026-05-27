package com.carddemo.domain.repository;

import com.carddemo.domain.entity.TransactionCategory;
import com.carddemo.domain.entity.TransactionCategoryId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionCategoryRepository extends JpaRepository<TransactionCategory, TransactionCategoryId> {

    List<TransactionCategory> findByTypeCd(String typeCd);

    Page<TransactionCategory> findAll(Pageable pageable);
}
