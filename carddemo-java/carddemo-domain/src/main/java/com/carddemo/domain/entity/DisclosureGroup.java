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
 * JPA entity mapped from CVTRA02Y.cpy — DIS-GROUP-RECORD (50 bytes).
 * Composite key: (group_id, tran_type_cd, tran_cat_cd).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "disclosure_groups")
@IdClass(DisclosureGroupId.class)
public class DisclosureGroup {

    @Id
    @Column(name = "group_id", length = 10, nullable = false)
    private String groupId;

    @Id
    @Column(name = "tran_type_cd", length = 2, nullable = false)
    private String tranTypeCd;

    @Id
    @Column(name = "tran_cat_cd", nullable = false)
    private Integer tranCatCd;

    @Column(name = "int_rate", precision = 6, scale = 2, nullable = false)
    private BigDecimal intRate;
}
