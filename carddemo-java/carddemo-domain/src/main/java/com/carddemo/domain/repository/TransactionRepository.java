package com.carddemo.domain.repository;

import com.carddemo.domain.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    @Query("SELECT t FROM Transaction t JOIN CardCrossReference x ON t.cardNum = x.cardNum " +
           "WHERE x.acctId = :acctId ORDER BY t.origTs DESC")
    Page<Transaction> findByAccountIdOrderByTimestampDesc(@Param("acctId") Long acctId, Pageable pageable);
}
