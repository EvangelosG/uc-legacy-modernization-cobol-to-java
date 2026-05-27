-- V008: Category balances table from CVTRA01Y.cpy — TRAN-CAT-BAL-RECORD (50 bytes)
CREATE TABLE category_balances (
    acct_id   BIGINT        NOT NULL,
    type_cd   VARCHAR(2)    NOT NULL,
    cat_cd    INTEGER       NOT NULL,
    balance   NUMERIC(11,2) NOT NULL DEFAULT 0,
    PRIMARY KEY (acct_id, type_cd, cat_cd)
);
