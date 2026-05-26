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

/**
 * JPA entity mapped from CVTRA04Y.cpy — TRAN-CAT-RECORD (60 bytes).
 * Composite key: (type_cd, cat_cd).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "transaction_categories")
@IdClass(TransactionCategoryId.class)
public class TransactionCategory {

    @Id
    @Column(name = "type_cd", length = 2, nullable = false)
    private String typeCd;

    @Id
    @Column(name = "cat_cd", nullable = false)
    private Integer catCd;

    @Column(name = "type_desc", length = 50)
    private String typeDesc;
}
