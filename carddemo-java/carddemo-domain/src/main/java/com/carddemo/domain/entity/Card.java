package com.carddemo.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * JPA entity mapped from CVACT02Y.cpy — CARD-RECORD (150 bytes).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cards")
public class Card {

    @Id
    @Column(name = "card_num", length = 16, nullable = false)
    private String cardNum;

    @Column(name = "acct_id", nullable = false)
    private Long acctId;

    @Column(name = "cvv_cd")
    private Integer cvvCd;

    @Column(name = "embossed_name", length = 50)
    private String embossedName;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "active_status", length = 1, nullable = false)
    private String activeStatus;
}
