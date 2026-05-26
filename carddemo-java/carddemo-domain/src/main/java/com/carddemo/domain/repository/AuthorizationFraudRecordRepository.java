package com.carddemo.domain.repository;

import com.carddemo.domain.entity.AuthorizationFraudRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthorizationFraudRecordRepository extends JpaRepository<AuthorizationFraudRecord, Long> {

    List<AuthorizationFraudRecord> findByCardNum(String cardNum);

    List<AuthorizationFraudRecord> findByAcctId(Long acctId);

    boolean existsByCardNumAndAuthFraud(String cardNum, String authFraud);
}
