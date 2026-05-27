-- V011: Transaction categories table from CVTRA04Y.cpy — TRAN-CAT-RECORD (60 bytes)
CREATE TABLE transaction_categories (
    type_cd    VARCHAR(2)   NOT NULL,
    cat_cd     INTEGER      NOT NULL,
    type_desc  VARCHAR(50),
    PRIMARY KEY (type_cd, cat_cd)
);
