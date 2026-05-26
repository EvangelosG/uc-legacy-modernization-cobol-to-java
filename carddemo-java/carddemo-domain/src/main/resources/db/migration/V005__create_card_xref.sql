-- V005: Card cross-reference table from CVACT03Y.cpy — CARD-XREF-RECORD (50 bytes)
CREATE TABLE card_xref (
    card_num  VARCHAR(16)  NOT NULL PRIMARY KEY,
    cust_id   BIGINT       NOT NULL,
    acct_id   BIGINT       NOT NULL
);

CREATE INDEX idx_card_xref_acct_id ON card_xref (acct_id);
CREATE INDEX idx_card_xref_cust_id ON card_xref (cust_id);
