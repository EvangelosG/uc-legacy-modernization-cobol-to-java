-- V002: Accounts table from CVACT01Y.cpy — ACCOUNT-RECORD (300 bytes)
CREATE TABLE accounts (
    acct_id           BIGINT         NOT NULL PRIMARY KEY,
    active_status     VARCHAR(1)     NOT NULL,
    curr_bal          NUMERIC(12,2)  NOT NULL DEFAULT 0,
    credit_limit      NUMERIC(12,2)  NOT NULL DEFAULT 0,
    cash_credit_limit NUMERIC(12,2)  NOT NULL DEFAULT 0,
    open_date         DATE,
    expiration_date   DATE,
    reissue_date      DATE,
    curr_cyc_credit   NUMERIC(12,2)  NOT NULL DEFAULT 0,
    curr_cyc_debit    NUMERIC(12,2)  NOT NULL DEFAULT 0,
    addr_zip          VARCHAR(10),
    group_id          VARCHAR(10),
    version           BIGINT         NOT NULL DEFAULT 0
);
