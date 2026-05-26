package com.carddemo.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JPA entity mapped from CVACT03Y.cpy — CARD-XREF-RECORD (50 bytes).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "card_xref")
public class CardCrossReference {

    @Id
    @Column(name = "card_num", length = 16, nullable = false)
    private String cardNum;

    @Column(name = "cust_id", nullable = false)
    private Long custId;

    @Column(name = "acct_id", nullable = false)
    private Long acctId;
}
