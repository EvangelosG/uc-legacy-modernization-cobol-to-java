package com.carddemo.domain.repository;

import com.carddemo.domain.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, String> {

    List<Card> findByAcctId(Long acctId);

    @Query("SELECT c FROM Card c JOIN CardCrossReference x ON c.cardNum = x.cardNum " +
           "WHERE x.acctId = :acctId ORDER BY c.cardNum")
    Page<Card> findByAccountId(@Param("acctId") Long acctId, Pageable pageable);
}
