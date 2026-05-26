package com.carddemo.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * JPA entity mapped from CVTRA01Y.cpy — TRAN-CAT-BAL-RECORD (50 bytes).
 * Composite key: (acct_id, type_cd, cat_cd).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "category_balances")
@IdClass(TransactionCategoryBalanceId.class)
public class TransactionCategoryBalance {

    @Id
    @Column(name = "acct_id", nullable = false)
    private Long acctId;

    @Id
    @Column(name = "type_cd", length = 2, nullable = false)
    private String typeCd;

    @Id
    @Column(name = "cat_cd", nullable = false)
    private Integer catCd;

    @Column(name = "balance", precision = 11, scale = 2, nullable = false)
    private BigDecimal balance;
}
