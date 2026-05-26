-- V009: Disclosure groups table from CVTRA02Y.cpy — DIS-GROUP-RECORD (50 bytes)
CREATE TABLE disclosure_groups (
    group_id      VARCHAR(10)   NOT NULL,
    tran_type_cd  VARCHAR(2)    NOT NULL,
    tran_cat_cd   INTEGER       NOT NULL,
    int_rate      NUMERIC(6,2)  NOT NULL DEFAULT 0,
    PRIMARY KEY (group_id, tran_type_cd, tran_cat_cd)
);
