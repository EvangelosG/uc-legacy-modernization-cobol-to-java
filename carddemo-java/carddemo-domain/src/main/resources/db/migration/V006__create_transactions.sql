-- V006: Transactions table from CVTRA05Y.cpy — TRAN-RECORD (350 bytes)
CREATE TABLE transactions (
    tran_id        VARCHAR(16)    NOT NULL PRIMARY KEY,
    type_cd        VARCHAR(2)     NOT NULL,
    cat_cd         INTEGER        NOT NULL,
    source         VARCHAR(10),
    description    VARCHAR(100),
    amount         NUMERIC(11,2)  NOT NULL,
    merchant_id    BIGINT,
    merchant_name  VARCHAR(50),
    merchant_city  VARCHAR(50),
    merchant_zip   VARCHAR(10),
    card_num       VARCHAR(16)    NOT NULL,
    orig_ts        TIMESTAMP,
    proc_ts        TIMESTAMP
);

CREATE INDEX idx_transactions_card_num ON transactions (card_num);
CREATE INDEX idx_transactions_orig_ts ON transactions (orig_ts);
