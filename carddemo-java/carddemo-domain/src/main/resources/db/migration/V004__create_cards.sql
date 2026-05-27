-- V004: Cards table from CVACT02Y.cpy — CARD-RECORD (150 bytes)
CREATE TABLE cards (
    card_num        VARCHAR(16)  NOT NULL PRIMARY KEY,
    acct_id         BIGINT       NOT NULL,
    cvv_cd          INTEGER,
    embossed_name   VARCHAR(50),
    expiration_date DATE,
    active_status   VARCHAR(1)   NOT NULL
);

CREATE INDEX idx_cards_acct_id ON cards (acct_id);
