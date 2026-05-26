-- V010: Transaction types table from CVTRA03Y.cpy — TRAN-TYPE-RECORD (60 bytes)
CREATE TABLE transaction_types (
    type_cd    VARCHAR(2)   NOT NULL PRIMARY KEY,
    type_desc  VARCHAR(50)
);
