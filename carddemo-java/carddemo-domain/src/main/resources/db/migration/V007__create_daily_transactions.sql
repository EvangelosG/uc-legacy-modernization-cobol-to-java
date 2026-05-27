-- V007: Daily transactions table from CVTRA06Y.cpy — DALYTRAN-RECORD (350 bytes)
CREATE TABLE daily_transactions (
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

CREATE INDEX idx_daily_transactions_card_num ON daily_transactions (card_num);
