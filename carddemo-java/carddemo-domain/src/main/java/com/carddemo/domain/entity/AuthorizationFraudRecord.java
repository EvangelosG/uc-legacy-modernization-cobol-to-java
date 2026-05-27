package com.carddemo.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA entity mapped from AUTHFRDS.ddl — DB2 fraud records table.
 * Replaces COPAUS2C DB2 fraud check logic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "authorization_fraud_records")
public class AuthorizationFraudRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "card_num", length = 16, nullable = false)
    private String cardNum;

    @Column(name = "auth_ts", nullable = false)
    private LocalDateTime authTs;

    @Column(name = "auth_type", length = 4)
    private String authType;

    @Column(name = "card_expiry_date", length = 4)
    private String cardExpiryDate;

    @Column(name = "message_type", length = 6)
    private String messageType;

    @Column(name = "message_source", length = 6)
    private String messageSource;

    @Column(name = "auth_id_code", length = 6)
    private String authIdCode;

    @Column(name = "auth_resp_code", length = 2)
    private String authRespCode;

    @Column(name = "auth_resp_reason", length = 4)
    private String authRespReason;

    @Column(name = "processing_code", length = 6)
    private String processingCode;

    @Column(name = "transaction_amt", precision = 12, scale = 2)
    private BigDecimal transactionAmt;

    @Column(name = "approved_amt", precision = 12, scale = 2)
    private BigDecimal approvedAmt;

    @Column(name = "merchant_category_code", length = 4)
    private String merchantCategoryCode;

    @Column(name = "acqr_country_code", length = 3)
    private String acqrCountryCode;

    @Column(name = "pos_entry_mode")
    private Integer posEntryMode;

    @Column(name = "merchant_id", length = 15)
    private String merchantId;

    @Column(name = "merchant_name", length = 22)
    private String merchantName;

    @Column(name = "merchant_city", length = 13)
    private String merchantCity;

    @Column(name = "merchant_state", length = 2)
    private String merchantState;

    @Column(name = "merchant_zip", length = 9)
    private String merchantZip;

    @Column(name = "transaction_id", length = 15)
    private String transactionId;

    @Column(name = "match_status", length = 1)
    private String matchStatus;

    @Column(name = "auth_fraud", length = 1)
    private String authFraud;

    @Column(name = "fraud_rpt_date")
    private LocalDate fraudRptDate;

    @Column(name = "acct_id")
    private Long acctId;

    @Column(name = "cust_id")
    private Long custId;
}
