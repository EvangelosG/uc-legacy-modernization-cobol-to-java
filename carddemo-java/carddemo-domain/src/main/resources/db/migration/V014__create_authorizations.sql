-- Phase 5: Authorization tables replacing IMS-DB2-MQ sub-application

CREATE TABLE authorizations (
    auth_id         BIGSERIAL PRIMARY KEY,
    card_num        VARCHAR(16) NOT NULL,
    acct_id         BIGINT,
    cust_id         BIGINT,
    auth_date       DATE NOT NULL,
    auth_time       VARCHAR(6),
    auth_type       VARCHAR(4),
    card_expiry     VARCHAR(4),
    message_type    VARCHAR(6),
    message_source  VARCHAR(6),
    processing_code VARCHAR(6),
    transaction_amt DECIMAL(12,2) NOT NULL,
    approved_amt    DECIMAL(12,2),
    auth_id_code    VARCHAR(6),
    auth_resp_code  VARCHAR(2),
    auth_resp_reason VARCHAR(4),
    merchant_category_code VARCHAR(4),
    acqr_country_code VARCHAR(3),
    pos_entry_mode  INTEGER,
    merchant_id     VARCHAR(15),
    merchant_name   VARCHAR(22),
    merchant_city   VARCHAR(13),
    merchant_state  VARCHAR(2),
    merchant_zip    VARCHAR(9),
    transaction_id  VARCHAR(15),
    match_status    VARCHAR(1) DEFAULT 'P',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version         BIGINT DEFAULT 0
);

CREATE INDEX idx_auth_card_num ON authorizations(card_num);
CREATE INDEX idx_auth_acct_id ON authorizations(acct_id);
CREATE INDEX idx_auth_match_status ON authorizations(match_status);
CREATE INDEX idx_auth_created_at ON authorizations(created_at);

CREATE TABLE authorization_fraud_records (
    id              BIGSERIAL PRIMARY KEY,
    card_num        VARCHAR(16) NOT NULL,
    auth_ts         TIMESTAMP NOT NULL,
    auth_type       VARCHAR(4),
    card_expiry_date VARCHAR(4),
    message_type    VARCHAR(6),
    message_source  VARCHAR(6),
    auth_id_code    VARCHAR(6),
    auth_resp_code  VARCHAR(2),
    auth_resp_reason VARCHAR(4),
    processing_code VARCHAR(6),
    transaction_amt DECIMAL(12,2),
    approved_amt    DECIMAL(12,2),
    merchant_category_code VARCHAR(4),
    acqr_country_code VARCHAR(3),
    pos_entry_mode  INTEGER,
    merchant_id     VARCHAR(15),
    merchant_name   VARCHAR(22),
    merchant_city   VARCHAR(13),
    merchant_state  VARCHAR(2),
    merchant_zip    VARCHAR(9),
    transaction_id  VARCHAR(15),
    match_status    VARCHAR(1),
    auth_fraud      VARCHAR(1),
    fraud_rpt_date  DATE,
    acct_id         BIGINT,
    cust_id         BIGINT,
    UNIQUE(card_num, auth_ts)
);

CREATE INDEX idx_fraud_card_num ON authorization_fraud_records(card_num);
CREATE INDEX idx_fraud_acct_id ON authorization_fraud_records(acct_id);
