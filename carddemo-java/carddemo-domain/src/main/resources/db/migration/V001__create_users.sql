-- V001: Users table from CSUSR01Y.cpy — SEC-USER-DATA (80 bytes)
CREATE TABLE users (
    usr_id      VARCHAR(8)   NOT NULL PRIMARY KEY,
    usr_fname   VARCHAR(20),
    usr_lname   VARCHAR(20),
    usr_pwd     VARCHAR(72)  NOT NULL,  -- BCrypt hash
    usr_type    VARCHAR(1)   NOT NULL CHECK (usr_type IN ('A', 'U'))
);
