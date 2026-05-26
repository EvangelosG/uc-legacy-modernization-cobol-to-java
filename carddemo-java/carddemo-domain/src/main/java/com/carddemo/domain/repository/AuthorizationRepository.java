package com.carddemo.domain.repository;

import com.carddemo.domain.entity.Authorization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuthorizationRepository extends JpaRepository<Authorization, Long> {

    Page<Authorization> findByAcctIdOrderByCreatedAtDesc(Long acctId, Pageable pageable);

    Page<Authorization> findByCardNumOrderByCreatedAtDesc(String cardNum, Pageable pageable);

    @Modifying
    @Query("DELETE FROM Authorization a WHERE a.matchStatus IN ('E', 'M') AND a.createdAt < :cutoffDate")
    int deleteExpiredAuthorizations(@Param("cutoffDate") LocalDateTime cutoffDate);

    long countByAcctIdAndAuthRespCode(Long acctId, String authRespCode);
}
