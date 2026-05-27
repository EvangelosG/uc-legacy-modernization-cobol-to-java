package com.carddemo.domain.repository;

import com.carddemo.domain.entity.CardCrossReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardCrossReferenceRepository extends JpaRepository<CardCrossReference, String> {

    Optional<CardCrossReference> findByCardNum(String cardNumber);

    List<CardCrossReference> findByAcctId(Long accountId);
}
