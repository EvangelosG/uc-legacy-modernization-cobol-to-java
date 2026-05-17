# CardDemo Data Dictionary

A business-friendly reference of every data field in the CardDemo application, extracted from COBOL copybook PIC clauses, VSAM record layouts, IMS segments, and DB2 DCL declarations.

> **How to read this document**
>
> | Column | Meaning |
> |--------|---------|
> | **Field** | COBOL data name |
> | **Type** | `A` = Alphanumeric (PIC X), `N` = Unsigned Numeric (PIC 9), `SN` = Signed Numeric (PIC S9), `AN` = Alphanumeric/Numeric display |
> | **Length** | Character positions (for numeric: total digits; `V` marks the implied decimal point) |
> | **Format** | The raw PIC clause |
> | **Business Description** | Plain-language meaning |
>
> Storage qualifiers: **COMP** = binary, **COMP-3** = packed decimal, **DISPLAY** (default) = zoned decimal.

---

## Table of Contents

1. [Account Record (CVACT01Y)](#1-account-record--cvact01y)
2. [Card Record (CVACT02Y)](#2-card-record--cvact02y)
3. [Card Cross-Reference (CVACT03Y)](#3-card-cross-reference--cvact03y)
4. [Customer Record (CVCUS01Y)](#4-customer-record--cvcus01y)
5. [Transaction Record (CVTRA05Y)](#5-transaction-record--cvtra05y)
6. [Daily Transaction Record (CVTRA06Y)](#6-daily-transaction-record--cvtra06y)
7. [Transaction Category Balance (CVTRA01Y)](#7-transaction-category-balance--cvtra01y)
8. [Disclosure Group (CVTRA02Y)](#8-disclosure-group--cvtra02y)
9. [Transaction Type (CVTRA03Y)](#9-transaction-type--cvtra03y)
10. [Transaction Category Type (CVTRA04Y)](#10-transaction-category-type--cvtra04y)
11. [Transaction — Reporting Layout (COSTM01)](#11-transaction--reporting-layout--costm01)
12. [Report Structures (CVTRA07Y)](#12-report-structures--cvtra07y)
13. [Multi-Record Export (CVEXPORT)](#13-multi-record-export--cvexport)
14. [User Security Record (CSUSR01Y)](#14-user-security-record--csusr01y)
15. [Application Communication Area (COCOM01Y)](#15-application-communication-area--cocom01y)
16. [CICS Work Areas (CVCRD01Y)](#16-cics-work-areas--cvcrd01y)
17. [Date/Time Working Storage (CSDAT01Y)](#17-datetime-working-storage--csdat01y)
18. [Date Conversion Record (CODATECN)](#18-date-conversion-record--codatecn)
19. [Date Validation Working Storage (CSUTLDWY)](#19-date-validation-working-storage--csutldwy)
20. [Abend Data (CSMSG02Y)](#20-abend-data--csmsg02y)
21. [Common Messages (CSMSG01Y)](#21-common-messages--csmsg01y)
22. [Screen Title (COTTL01Y)](#22-screen-title--cottl01y)
23. [Lookup Codes (CSLKPCDY)](#23-lookup-codes--cslkpcdy)
24. [Menu Options — Regular User (COMEN02Y)](#24-menu-options--regular-user--comen02y)
25. [Menu Options — Admin (COADM02Y)](#25-menu-options--admin--coadm02y)
26. [Unused Record (UNUSED1Y)](#26-unused-record--unused1y)
27. [Sub-App: Authorization Request (CCPAURQY)](#27-sub-app-authorization-request--ccpaurqy)
28. [Sub-App: Authorization Response (CCPAURLY)](#28-sub-app-authorization-response--ccpaurly)
29. [Sub-App: Authorization Error Log (CCPAUERY)](#29-sub-app-authorization-error-log--ccpauery)
30. [Sub-App: IMS Pending Auth Detail (CIPAUDTY)](#30-sub-app-ims-pending-auth-detail--cipaudty)
31. [Sub-App: IMS Pending Auth Summary (CIPAUSMY)](#31-sub-app-ims-pending-auth-summary--cipausmy)
32. [Sub-App: IMS Function Codes (IMSFUNCS)](#32-sub-app-ims-function-codes--imsfuncs)
33. [Sub-App: DB2 Authorization Fraud Table (AUTHFRDS)](#33-sub-app-db2-authorization-fraud-table--authfrds)
34. [Sub-App: DB2 Transaction Type Table (DCLTRTYP)](#34-sub-app-db2-transaction-type-table--dcltrtyp)
35. [Sub-App: DB2 Transaction Category Table (DCLTRCAT)](#35-sub-app-db2-transaction-category-table--dcltrcat)
36. [Sub-App: DB2 Common Working Storage (CSDB2RWY)](#36-sub-app-db2-common-working-storage--csdb2rwy)

---

## 1. Account Record — CVACT01Y

**Copybook**: `app/cpy/CVACT01Y.cpy` · **Record**: `ACCOUNT-RECORD` · **VSAM KSDS** · **Record Length**: 300 bytes

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 1 | ACCT-ID | N | 11 | `PIC 9(11)` | Unique account identifier (primary key) |
| 2 | ACCT-ACTIVE-STATUS | A | 1 | `PIC X(01)` | Account status flag (e.g., active/inactive) |
| 3 | ACCT-CURR-BAL | SN | 12.2 | `PIC S9(10)V99` | Current account balance (signed, 2 decimals) |
| 4 | ACCT-CREDIT-LIMIT | SN | 12.2 | `PIC S9(10)V99` | Maximum credit limit |
| 5 | ACCT-CASH-CREDIT-LIMIT | SN | 12.2 | `PIC S9(10)V99` | Maximum cash advance limit |
| 6 | ACCT-OPEN-DATE | A | 10 | `PIC X(10)` | Date account was opened (YYYY-MM-DD) |
| 7 | ACCT-EXPIRAION-DATE | A | 10 | `PIC X(10)` | Account expiration date (YYYY-MM-DD) |
| 8 | ACCT-REISSUE-DATE | A | 10 | `PIC X(10)` | Date of last card reissue (YYYY-MM-DD) |
| 9 | ACCT-CURR-CYC-CREDIT | SN | 12.2 | `PIC S9(10)V99` | Total credits in the current billing cycle |
| 10 | ACCT-CURR-CYC-DEBIT | SN | 12.2 | `PIC S9(10)V99` | Total debits in the current billing cycle |
| 11 | ACCT-ADDR-ZIP | A | 10 | `PIC X(10)` | Account holder's ZIP / postal code |
| 12 | ACCT-GROUP-ID | A | 10 | `PIC X(10)` | Disclosure / interest-rate group assignment |
| 13 | FILLER | A | 178 | `PIC X(178)` | Reserved for future use |

---

## 2. Card Record — CVACT02Y

**Copybook**: `app/cpy/CVACT02Y.cpy` · **Record**: `CARD-RECORD` · **VSAM KSDS** · **Record Length**: 150 bytes

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 1 | CARD-NUM | A | 16 | `PIC X(16)` | Credit card number (primary key) |
| 2 | CARD-ACCT-ID | N | 11 | `PIC 9(11)` | Parent account ID (FK → ACCOUNT-RECORD) |
| 3 | CARD-CVV-CD | N | 3 | `PIC 9(03)` | Card verification value |
| 4 | CARD-EMBOSSED-NAME | A | 50 | `PIC X(50)` | Name printed on the physical card |
| 5 | CARD-EXPIRAION-DATE | A | 10 | `PIC X(10)` | Card expiration date |
| 6 | CARD-ACTIVE-STATUS | A | 1 | `PIC X(01)` | Card status (active/inactive/lost/stolen) |
| 7 | FILLER | A | 59 | `PIC X(59)` | Reserved for future use |

---

## 3. Card Cross-Reference — CVACT03Y

**Copybook**: `app/cpy/CVACT03Y.cpy` · **Record**: `CARD-XREF-RECORD` · **VSAM KSDS** · **Record Length**: 50 bytes

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 1 | XREF-CARD-NUM | A | 16 | `PIC X(16)` | Card number (primary key) |
| 2 | XREF-CUST-ID | N | 9 | `PIC 9(09)` | Customer ID that owns this card |
| 3 | XREF-ACCT-ID | N | 11 | `PIC 9(11)` | Account ID linked to this card |
| 4 | FILLER | A | 14 | `PIC X(14)` | Reserved for future use |

---

## 4. Customer Record — CVCUS01Y

**Copybook**: `app/cpy/CVCUS01Y.cpy` · **Record**: `CUSTOMER-RECORD` · **VSAM KSDS** · **Record Length**: 500 bytes

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 1 | CUST-ID | N | 9 | `PIC 9(09)` | Unique customer identifier (primary key) |
| 2 | CUST-FIRST-NAME | A | 25 | `PIC X(25)` | Customer first name |
| 3 | CUST-MIDDLE-NAME | A | 25 | `PIC X(25)` | Customer middle name |
| 4 | CUST-LAST-NAME | A | 25 | `PIC X(25)` | Customer last name |
| 5 | CUST-ADDR-LINE-1 | A | 50 | `PIC X(50)` | Address line 1 |
| 6 | CUST-ADDR-LINE-2 | A | 50 | `PIC X(50)` | Address line 2 |
| 7 | CUST-ADDR-LINE-3 | A | 50 | `PIC X(50)` | Address line 3 |
| 8 | CUST-ADDR-STATE-CD | A | 2 | `PIC X(02)` | US state code (e.g., "NY", "CA") |
| 9 | CUST-ADDR-COUNTRY-CD | A | 3 | `PIC X(03)` | ISO country code |
| 10 | CUST-ADDR-ZIP | A | 10 | `PIC X(10)` | ZIP / postal code |
| 11 | CUST-PHONE-NUM-1 | A | 15 | `PIC X(15)` | Primary phone number |
| 12 | CUST-PHONE-NUM-2 | A | 15 | `PIC X(15)` | Secondary phone number |
| 13 | CUST-SSN | N | 9 | `PIC 9(09)` | Social Security Number |
| 14 | CUST-GOVT-ISSUED-ID | A | 20 | `PIC X(20)` | Government-issued ID (passport, driver's license) |
| 15 | CUST-DOB-YYYY-MM-DD | A | 10 | `PIC X(10)` | Date of birth (YYYY-MM-DD) |
| 16 | CUST-EFT-ACCOUNT-ID | A | 10 | `PIC X(10)` | Electronic funds transfer / bank account ID |
| 17 | CUST-PRI-CARD-HOLDER-IND | A | 1 | `PIC X(01)` | Primary cardholder indicator (Y/N) |
| 18 | CUST-FICO-CREDIT-SCORE | N | 3 | `PIC 9(03)` | FICO credit score (300–850) |
| 19 | FILLER | A | 168 | `PIC X(168)` | Reserved for future use |

> **Note**: `CUSTREC.cpy` is a duplicate of this layout with minor naming differences (e.g., `CUST-DOB-YYYYMMDD` instead of `CUST-DOB-YYYY-MM-DD`).

---

## 5. Transaction Record — CVTRA05Y

**Copybook**: `app/cpy/CVTRA05Y.cpy` · **Record**: `TRAN-RECORD` · **VSAM KSDS** · **Record Length**: 350 bytes

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 1 | TRAN-ID | A | 16 | `PIC X(16)` | Unique transaction identifier |
| 2 | TRAN-TYPE-CD | A | 2 | `PIC X(02)` | Transaction type code (FK → TRAN-TYPE-RECORD) |
| 3 | TRAN-CAT-CD | N | 4 | `PIC 9(04)` | Transaction category code |
| 4 | TRAN-SOURCE | A | 10 | `PIC X(10)` | Origination channel (e.g., POS, online, phone) |
| 5 | TRAN-DESC | A | 100 | `PIC X(100)` | Free-text transaction description |
| 6 | TRAN-AMT | SN | 11.2 | `PIC S9(09)V99` | Transaction amount (signed, 2 decimals) |
| 7 | TRAN-MERCHANT-ID | N | 9 | `PIC 9(09)` | Merchant identifier |
| 8 | TRAN-MERCHANT-NAME | A | 50 | `PIC X(50)` | Merchant business name |
| 9 | TRAN-MERCHANT-CITY | A | 50 | `PIC X(50)` | Merchant city |
| 10 | TRAN-MERCHANT-ZIP | A | 10 | `PIC X(10)` | Merchant ZIP code |
| 11 | TRAN-CARD-NUM | A | 16 | `PIC X(16)` | Card number used for this transaction |
| 12 | TRAN-ORIG-TS | A | 26 | `PIC X(26)` | Original transaction timestamp |
| 13 | TRAN-PROC-TS | A | 26 | `PIC X(26)` | Processing / posting timestamp |
| 14 | FILLER | A | 20 | `PIC X(20)` | Reserved for future use |

---

## 6. Daily Transaction Record — CVTRA06Y

**Copybook**: `app/cpy/CVTRA06Y.cpy` · **Record**: `DALYTRAN-RECORD` · **Record Length**: 350 bytes

Identical layout to TRAN-RECORD (§5) but prefixed `DALYTRAN-`. Used for the daily transaction input file that feeds batch posting.

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 1 | DALYTRAN-ID | A | 16 | `PIC X(16)` | Daily transaction identifier |
| 2 | DALYTRAN-TYPE-CD | A | 2 | `PIC X(02)` | Transaction type code |
| 3 | DALYTRAN-CAT-CD | N | 4 | `PIC 9(04)` | Transaction category code |
| 4 | DALYTRAN-SOURCE | A | 10 | `PIC X(10)` | Origination channel |
| 5 | DALYTRAN-DESC | A | 100 | `PIC X(100)` | Transaction description |
| 6 | DALYTRAN-AMT | SN | 11.2 | `PIC S9(09)V99` | Transaction amount |
| 7 | DALYTRAN-MERCHANT-ID | N | 9 | `PIC 9(09)` | Merchant identifier |
| 8 | DALYTRAN-MERCHANT-NAME | A | 50 | `PIC X(50)` | Merchant name |
| 9 | DALYTRAN-MERCHANT-CITY | A | 50 | `PIC X(50)` | Merchant city |
| 10 | DALYTRAN-MERCHANT-ZIP | A | 10 | `PIC X(10)` | Merchant ZIP code |
| 11 | DALYTRAN-CARD-NUM | A | 16 | `PIC X(16)` | Card number |
| 12 | DALYTRAN-ORIG-TS | A | 26 | `PIC X(26)` | Original timestamp |
| 13 | DALYTRAN-PROC-TS | A | 26 | `PIC X(26)` | Processing timestamp |
| 14 | FILLER | A | 20 | `PIC X(20)` | Reserved |

---

## 7. Transaction Category Balance — CVTRA01Y

**Copybook**: `app/cpy/CVTRA01Y.cpy` · **Record**: `TRAN-CAT-BAL-RECORD` · **VSAM KSDS** · **Record Length**: 50 bytes

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 1 | TRANCAT-ACCT-ID | N | 11 | `PIC 9(11)` | Account ID (part of composite key) |
| 2 | TRANCAT-TYPE-CD | A | 2 | `PIC X(02)` | Transaction type code (part of composite key) |
| 3 | TRANCAT-CD | N | 4 | `PIC 9(04)` | Transaction category code (part of composite key) |
| 4 | TRAN-CAT-BAL | SN | 11.2 | `PIC S9(09)V99` | Running balance for this account + type + category |
| 5 | FILLER | A | 22 | `PIC X(22)` | Reserved |

---

## 8. Disclosure Group — CVTRA02Y

**Copybook**: `app/cpy/CVTRA02Y.cpy` · **Record**: `DIS-GROUP-RECORD` · **VSAM KSDS** · **Record Length**: 50 bytes

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 1 | DIS-ACCT-GROUP-ID | A | 10 | `PIC X(10)` | Account group identifier (part of composite key) |
| 2 | DIS-TRAN-TYPE-CD | A | 2 | `PIC X(02)` | Transaction type code (part of composite key) |
| 3 | DIS-TRAN-CAT-CD | N | 4 | `PIC 9(04)` | Transaction category code (part of composite key) |
| 4 | DIS-INT-RATE | SN | 6.2 | `PIC S9(04)V99` | Interest rate for this group + type + category |
| 5 | FILLER | A | 28 | `PIC X(28)` | Reserved |

---

## 9. Transaction Type — CVTRA03Y

**Copybook**: `app/cpy/CVTRA03Y.cpy` · **Record**: `TRAN-TYPE-RECORD` · **VSAM KSDS** · **Record Length**: 60 bytes

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 1 | TRAN-TYPE | A | 2 | `PIC X(02)` | Transaction type code (primary key, e.g., "SA" = Sale) |
| 2 | TRAN-TYPE-DESC | A | 50 | `PIC X(50)` | Human-readable description of this transaction type |
| 3 | FILLER | A | 8 | `PIC X(08)` | Reserved |

---

## 10. Transaction Category Type — CVTRA04Y

**Copybook**: `app/cpy/CVTRA04Y.cpy` · **Record**: `TRAN-CAT-RECORD` · **VSAM KSDS** · **Record Length**: 60 bytes

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 1 | TRAN-TYPE-CD | A | 2 | `PIC X(02)` | Transaction type code (part of composite key) |
| 2 | TRAN-CAT-CD | N | 4 | `PIC 9(04)` | Category code within the type (part of composite key) |
| 3 | TRAN-CAT-TYPE-DESC | A | 50 | `PIC X(50)` | Description of this type + category combination |
| 4 | FILLER | A | 4 | `PIC X(04)` | Reserved |

---

## 11. Transaction — Reporting Layout — COSTM01

**Copybook**: `app/cpy/COSTM01.CPY` · **Record**: `TRNX-RECORD`

Re-keyed transaction layout used for reporting. Key is (CARD-NUM + TRAN-ID) to allow card-level ordering.

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 1 | TRNX-CARD-NUM | A | 16 | `PIC X(16)` | Card number (part of composite key) |
| 2 | TRNX-ID | A | 16 | `PIC X(16)` | Transaction ID (part of composite key) |
| 3 | TRNX-TYPE-CD | A | 2 | `PIC X(02)` | Transaction type code |
| 4 | TRNX-CAT-CD | N | 4 | `PIC 9(04)` | Transaction category code |
| 5 | TRNX-SOURCE | A | 10 | `PIC X(10)` | Origination channel |
| 6 | TRNX-DESC | A | 100 | `PIC X(100)` | Transaction description |
| 7 | TRNX-AMT | SN | 11.2 | `PIC S9(09)V99` | Transaction amount |
| 8 | TRNX-MERCHANT-ID | N | 9 | `PIC 9(09)` | Merchant ID |
| 9 | TRNX-MERCHANT-NAME | A | 50 | `PIC X(50)` | Merchant name |
| 10 | TRNX-MERCHANT-CITY | A | 50 | `PIC X(50)` | Merchant city |
| 11 | TRNX-MERCHANT-ZIP | A | 10 | `PIC X(10)` | Merchant ZIP |
| 12 | TRNX-ORIG-TS | A | 26 | `PIC X(26)` | Original timestamp |
| 13 | TRNX-PROC-TS | A | 26 | `PIC X(26)` | Processing timestamp |
| 14 | FILLER | A | 20 | `PIC X(20)` | Reserved |

---

## 12. Report Structures — CVTRA07Y

**Copybook**: `app/cpy/CVTRA07Y.cpy` · Print-line structures for the Daily Transaction Report.

### Report Header (`REPORT-NAME-HEADER`)

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 1 | REPT-SHORT-NAME | A | 38 | `PIC X(38)` | Report short name (default "DALYREPT") |
| 2 | REPT-LONG-NAME | A | 41 | `PIC X(41)` | Report title (default "Daily Transaction Report") |
| 3 | REPT-DATE-HEADER | A | 12 | `PIC X(12)` | Label "Date Range: " |
| 4 | REPT-START-DATE | A | 10 | `PIC X(10)` | Report start date |
| 5 | REPT-END-DATE | A | 10 | `PIC X(10)` | Report end date |

### Transaction Detail Line (`TRANSACTION-DETAIL-REPORT`)

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 1 | TRAN-REPORT-TRANS-ID | A | 16 | `PIC X(16)` | Transaction ID |
| 2 | TRAN-REPORT-ACCOUNT-ID | A | 11 | `PIC X(11)` | Account ID |
| 3 | TRAN-REPORT-TYPE-CD | A | 2 | `PIC X(02)` | Type code |
| 4 | TRAN-REPORT-TYPE-DESC | A | 15 | `PIC X(15)` | Type description |
| 5 | TRAN-REPORT-CAT-CD | N | 4 | `PIC 9(04)` | Category code |
| 6 | TRAN-REPORT-CAT-DESC | A | 29 | `PIC X(29)` | Category description |
| 7 | TRAN-REPORT-SOURCE | A | 10 | `PIC X(10)` | Transaction source |
| 8 | TRAN-REPORT-AMT | AN | 14 | `PIC -ZZZ,ZZZ,ZZZ.ZZ` | Formatted amount (edited) |

### Totals Lines

| Field | Format | Business Description |
|-------|--------|----------------------|
| REPT-PAGE-TOTAL | `PIC +ZZZ,ZZZ,ZZZ.ZZ` | Page subtotal |
| REPT-ACCOUNT-TOTAL | `PIC +ZZZ,ZZZ,ZZZ.ZZ` | Account subtotal |
| REPT-GRAND-TOTAL | `PIC +ZZZ,ZZZ,ZZZ.ZZ` | Grand total for the report |

---

## 13. Multi-Record Export — CVEXPORT

**Copybook**: `app/cpy/CVEXPORT.cpy` · **Record**: `EXPORT-RECORD` · **Sequential file** · **Record Length**: 500 bytes

Used for branch migration. A single record structure with a type indicator and REDEFINES for each entity.

### Common Header Fields

| # | Field | Type | Length | Format | Storage | Business Description |
|---|-------|------|--------|--------|---------|----------------------|
| 1 | EXPORT-REC-TYPE | A | 1 | `PIC X(1)` | Display | Record type indicator (C=Customer, A=Account, T=Transaction, X=Xref, R=Card) |
| 2 | EXPORT-TIMESTAMP | A | 26 | `PIC X(26)` | Display | Export timestamp (redefines to date + time) |
| 3 | EXPORT-SEQUENCE-NUM | N | 9 | `PIC 9(9)` | COMP | Sequence number within the export batch |
| 4 | EXPORT-BRANCH-ID | A | 4 | `PIC X(4)` | Display | Source branch identifier |
| 5 | EXPORT-REGION-CODE | A | 5 | `PIC X(5)` | Display | Region code |

### Customer Overlay (`EXPORT-CUSTOMER-DATA`)

| # | Field | Format | Storage | Business Description |
|---|-------|--------|---------|----------------------|
| 1 | EXP-CUST-ID | `PIC 9(09)` | COMP | Customer ID |
| 2 | EXP-CUST-FIRST-NAME | `PIC X(25)` | Display | First name |
| 3 | EXP-CUST-MIDDLE-NAME | `PIC X(25)` | Display | Middle name |
| 4 | EXP-CUST-LAST-NAME | `PIC X(25)` | Display | Last name |
| 5 | EXP-CUST-ADDR-LINE | `PIC X(50)` | Display | Address line (OCCURS 3 TIMES) |
| 6 | EXP-CUST-ADDR-STATE-CD | `PIC X(02)` | Display | State code |
| 7 | EXP-CUST-ADDR-COUNTRY-CD | `PIC X(03)` | Display | Country code |
| 8 | EXP-CUST-ADDR-ZIP | `PIC X(10)` | Display | ZIP code |
| 9 | EXP-CUST-PHONE-NUM | `PIC X(15)` | Display | Phone number (OCCURS 2 TIMES) |
| 10 | EXP-CUST-SSN | `PIC 9(09)` | Display | SSN |
| 11 | EXP-CUST-GOVT-ISSUED-ID | `PIC X(20)` | Display | Government ID |
| 12 | EXP-CUST-DOB-YYYY-MM-DD | `PIC X(10)` | Display | Date of birth |
| 13 | EXP-CUST-EFT-ACCOUNT-ID | `PIC X(10)` | Display | EFT account ID |
| 14 | EXP-CUST-PRI-CARD-HOLDER-IND | `PIC X(01)` | Display | Primary cardholder |
| 15 | EXP-CUST-FICO-CREDIT-SCORE | `PIC 9(03)` | COMP-3 | FICO score |

### Account Overlay (`EXPORT-ACCOUNT-DATA`)

| # | Field | Format | Storage | Business Description |
|---|-------|--------|---------|----------------------|
| 1 | EXP-ACCT-ID | `PIC 9(11)` | Display | Account ID |
| 2 | EXP-ACCT-ACTIVE-STATUS | `PIC X(01)` | Display | Active status |
| 3 | EXP-ACCT-CURR-BAL | `PIC S9(10)V99` | COMP-3 | Current balance |
| 4 | EXP-ACCT-CREDIT-LIMIT | `PIC S9(10)V99` | Display | Credit limit |
| 5 | EXP-ACCT-CASH-CREDIT-LIMIT | `PIC S9(10)V99` | COMP-3 | Cash advance limit |
| 6 | EXP-ACCT-OPEN-DATE | `PIC X(10)` | Display | Open date |
| 7 | EXP-ACCT-EXPIRAION-DATE | `PIC X(10)` | Display | Expiration date |
| 8 | EXP-ACCT-REISSUE-DATE | `PIC X(10)` | Display | Reissue date |
| 9 | EXP-ACCT-CURR-CYC-CREDIT | `PIC S9(10)V99` | Display | Cycle credits |
| 10 | EXP-ACCT-CURR-CYC-DEBIT | `PIC S9(10)V99` | COMP | Cycle debits |
| 11 | EXP-ACCT-ADDR-ZIP | `PIC X(10)` | Display | ZIP code |
| 12 | EXP-ACCT-GROUP-ID | `PIC X(10)` | Display | Group ID |

### Transaction Overlay (`EXPORT-TRANSACTION-DATA`)

| # | Field | Format | Storage | Business Description |
|---|-------|--------|---------|----------------------|
| 1 | EXP-TRAN-ID | `PIC X(16)` | Display | Transaction ID |
| 2 | EXP-TRAN-TYPE-CD | `PIC X(02)` | Display | Type code |
| 3 | EXP-TRAN-CAT-CD | `PIC 9(04)` | Display | Category code |
| 4 | EXP-TRAN-SOURCE | `PIC X(10)` | Display | Source |
| 5 | EXP-TRAN-DESC | `PIC X(100)` | Display | Description |
| 6 | EXP-TRAN-AMT | `PIC S9(09)V99` | COMP-3 | Amount |
| 7 | EXP-TRAN-MERCHANT-ID | `PIC 9(09)` | COMP | Merchant ID |
| 8 | EXP-TRAN-MERCHANT-NAME | `PIC X(50)` | Display | Merchant name |
| 9 | EXP-TRAN-MERCHANT-CITY | `PIC X(50)` | Display | Merchant city |
| 10 | EXP-TRAN-MERCHANT-ZIP | `PIC X(10)` | Display | Merchant ZIP |
| 11 | EXP-TRAN-CARD-NUM | `PIC X(16)` | Display | Card number |
| 12 | EXP-TRAN-ORIG-TS | `PIC X(26)` | Display | Original timestamp |
| 13 | EXP-TRAN-PROC-TS | `PIC X(26)` | Display | Processing timestamp |

### Card Cross-Reference Overlay (`EXPORT-CARD-XREF-DATA`)

| # | Field | Format | Storage | Business Description |
|---|-------|--------|---------|----------------------|
| 1 | EXP-XREF-CARD-NUM | `PIC X(16)` | Display | Card number |
| 2 | EXP-XREF-CUST-ID | `PIC 9(09)` | Display | Customer ID |
| 3 | EXP-XREF-ACCT-ID | `PIC 9(11)` | COMP | Account ID |

### Card Overlay (`EXPORT-CARD-DATA`)

| # | Field | Format | Storage | Business Description |
|---|-------|--------|---------|----------------------|
| 1 | EXP-CARD-NUM | `PIC X(16)` | Display | Card number |
| 2 | EXP-CARD-ACCT-ID | `PIC 9(11)` | COMP | Account ID |
| 3 | EXP-CARD-CVV-CD | `PIC 9(03)` | COMP | CVV code |
| 4 | EXP-CARD-EMBOSSED-NAME | `PIC X(50)` | Display | Embossed name |
| 5 | EXP-CARD-EXPIRAION-DATE | `PIC X(10)` | Display | Expiration date |
| 6 | EXP-CARD-ACTIVE-STATUS | `PIC X(01)` | Display | Active status |

---

## 14. User Security Record — CSUSR01Y

**Copybook**: `app/cpy/CSUSR01Y.cpy` · **Record**: `SEC-USER-DATA` · **VSAM KSDS** · **Record Length**: 80 bytes

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 1 | SEC-USR-ID | A | 8 | `PIC X(08)` | User login ID (primary key) |
| 2 | SEC-USR-FNAME | A | 20 | `PIC X(20)` | User first name |
| 3 | SEC-USR-LNAME | A | 20 | `PIC X(20)` | User last name |
| 4 | SEC-USR-PWD | A | 8 | `PIC X(08)` | User password (plaintext) |
| 5 | SEC-USR-TYPE | A | 1 | `PIC X(01)` | User type: "A" = Admin, "U" = Regular |
| 6 | SEC-USR-FILLER | A | 23 | `PIC X(23)` | Reserved |

---

## 15. Application Communication Area — COCOM01Y

**Copybook**: `app/cpy/COCOM01Y.cpy` · **Record**: `CARDDEMO-COMMAREA`

Passed between CICS programs via COMMAREA to maintain session state.

### General Info (`CDEMO-GENERAL-INFO`)

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 1 | CDEMO-FROM-TRANID | A | 4 | `PIC X(04)` | CICS transaction ID of the calling program |
| 2 | CDEMO-FROM-PROGRAM | A | 8 | `PIC X(08)` | Calling program name |
| 3 | CDEMO-TO-TRANID | A | 4 | `PIC X(04)` | Target transaction ID |
| 4 | CDEMO-TO-PROGRAM | A | 8 | `PIC X(08)` | Target program name |
| 5 | CDEMO-USER-ID | A | 8 | `PIC X(08)` | Authenticated user ID |
| 6 | CDEMO-USER-TYPE | A | 1 | `PIC X(01)` | "A" = Admin, "U" = Regular user |
| 7 | CDEMO-PGM-CONTEXT | N | 1 | `PIC 9(01)` | 0 = First entry, 1 = Re-entry (pseudo-conversational) |

### Customer Info (`CDEMO-CUSTOMER-INFO`)

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 8 | CDEMO-CUST-ID | N | 9 | `PIC 9(09)` | Current customer ID in context |
| 9 | CDEMO-CUST-FNAME | A | 25 | `PIC X(25)` | Customer first name |
| 10 | CDEMO-CUST-MNAME | A | 25 | `PIC X(25)` | Customer middle name |
| 11 | CDEMO-CUST-LNAME | A | 25 | `PIC X(25)` | Customer last name |

### Account Info (`CDEMO-ACCOUNT-INFO`)

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 12 | CDEMO-ACCT-ID | N | 11 | `PIC 9(11)` | Current account ID in context |
| 13 | CDEMO-ACCT-STATUS | A | 1 | `PIC X(01)` | Account active status |

### Card Info (`CDEMO-CARD-INFO`)

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 14 | CDEMO-CARD-NUM | N | 16 | `PIC 9(16)` | Current card number in context |

### Navigation Info (`CDEMO-MORE-INFO`)

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 15 | CDEMO-LAST-MAP | A | 7 | `PIC X(7)` | Last BMS map displayed |
| 16 | CDEMO-LAST-MAPSET | A | 7 | `PIC X(7)` | Last BMS mapset used |

---

## 16. CICS Work Areas — CVCRD01Y

**Copybook**: `app/cpy/CVCRD01Y.cpy` · **Record**: `CC-WORK-AREAS`

Working-storage fields shared across online programs for navigation, key handling, and context.

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 1 | CCARD-AID | A | 5 | `PIC X(5)` | Captured attention identifier (ENTER, CLEAR, PF1–PF12, PA1–PA2) |
| 2 | CCARD-NEXT-PROG | A | 8 | `PIC X(8)` | Next program to XCTL to |
| 3 | CCARD-NEXT-MAPSET | A | 7 | `PIC X(7)` | Next BMS mapset to send |
| 4 | CCARD-NEXT-MAP | A | 7 | `PIC X(7)` | Next BMS map to send |
| 5 | CCARD-ERROR-MSG | A | 75 | `PIC X(75)` | Error message for screen display |
| 6 | CCARD-RETURN-MSG | A | 75 | `PIC X(75)` | Return / informational message |
| 7 | CC-ACCT-ID | A | 11 | `PIC X(11)` | Account ID work field |
| 8 | CC-CARD-NUM | A | 16 | `PIC X(16)` | Card number work field |
| 9 | CC-CUST-ID | A | 9 | `PIC X(09)` | Customer ID work field |

---

## 17. Date/Time Working Storage — CSDAT01Y

**Copybook**: `app/cpy/CSDAT01Y.cpy` · **Record**: `WS-DATE-TIME`

Utility fields populated by `FUNCTION CURRENT-DATE` for formatting and display.

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 1 | WS-CURDATE-YEAR | N | 4 | `PIC 9(04)` | Current year (YYYY) |
| 2 | WS-CURDATE-MONTH | N | 2 | `PIC 9(02)` | Current month (MM) |
| 3 | WS-CURDATE-DAY | N | 2 | `PIC 9(02)` | Current day (DD) |
| 4 | WS-CURTIME-HOURS | N | 2 | `PIC 9(02)` | Current hour (HH) |
| 5 | WS-CURTIME-MINUTE | N | 2 | `PIC 9(02)` | Current minute (MM) |
| 6 | WS-CURTIME-SECOND | N | 2 | `PIC 9(02)` | Current second (SS) |
| 7 | WS-CURTIME-MILSEC | N | 2 | `PIC 9(02)` | Hundredths of a second |
| 8 | WS-CURDATE-MM | N | 2 | `PIC 9(02)` | Display-formatted month |
| 9 | WS-CURDATE-DD | N | 2 | `PIC 9(02)` | Display-formatted day |
| 10 | WS-CURDATE-YY | N | 2 | `PIC 9(02)` | Display-formatted 2-digit year |
| 11 | WS-TIMESTAMP-DT-YYYY | N | 4 | `PIC 9(04)` | Timestamp year |
| 12 | WS-TIMESTAMP-DT-MM | N | 2 | `PIC 9(02)` | Timestamp month |
| 13 | WS-TIMESTAMP-DT-DD | N | 2 | `PIC 9(02)` | Timestamp day |
| 14 | WS-TIMESTAMP-TM-HH | N | 2 | `PIC 9(02)` | Timestamp hour |
| 15 | WS-TIMESTAMP-TM-MM | N | 2 | `PIC 9(02)` | Timestamp minute |
| 16 | WS-TIMESTAMP-TM-SS | N | 2 | `PIC 9(02)` | Timestamp second |
| 17 | WS-TIMESTAMP-TM-MS6 | N | 6 | `PIC 9(06)` | Timestamp microseconds |

---

## 18. Date Conversion Record — CODATECN

**Copybook**: `app/cpy/CODATECN.cpy` · **Record**: `CODATECN-REC`

Input/output structure for the date conversion utility. Accepts dates in YYYYMMDD or YYYY-MM-DD format and converts between them.

### Input Fields (`CODATECN-IN-REC`)

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 1 | CODATECN-TYPE | A | 1 | `PIC X` | Input format: "1" = YYYYMMDD, "2" = YYYY-MM-DD |
| 2 | CODATECN-INP-DATE | A | 20 | `PIC X(20)` | Raw input date string |

### Output Fields (`CODATECN-OUT-REC`)

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 3 | CODATECN-OUTTYPE | A | 1 | `PIC X` | Output format: "1" = YYYY-MM-DD, "2" = YYYYMMDD |
| 4 | CODATECN-0UT-DATE | A | 20 | `PIC X(20)` | Converted output date string |
| 5 | CODATECN-ERROR-MSG | A | 38 | `PIC X(38)` | Error message if conversion fails |

---

## 19. Date Validation Working Storage — CSUTLDWY

**Copybook**: `app/cpy/CSUTLDWY.cpy`

Working-storage fields used by the date validation procedures in CSUTLDPY. Includes century check, month/day validation flags, and leap-year logic.

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 1 | WS-EDIT-DATE-CC | A | 2 | `PIC X(2)` | Century portion (19 or 20) |
| 2 | WS-EDIT-DATE-YY | A | 2 | `PIC X(2)` | Year within century |
| 3 | WS-EDIT-DATE-MM | A | 2 | `PIC X(2)` | Month (01–12) |
| 4 | WS-EDIT-DATE-DD | A | 2 | `PIC X(2)` | Day (01–31) |
| 5 | WS-EDIT-DATE-BINARY | SN | 9 | `PIC S9(9) BINARY` | Integer date for comparison |
| 6 | WS-CURRENT-DATE-YYYYMMDD | A | 8 | `PIC X(8)` | Current date for DOB validation |
| 7 | WS-SEVERITY | A | 4 | `PIC X(04)` | LE date-validation severity code |
| 8 | WS-MSG-NO | A | 4 | `PIC X(04)` | LE message number |
| 9 | WS-RESULT | A | 15 | `PIC X(15)` | Validation result text |
| 10 | WS-DATE-FMT | A | 10 | `PIC X(10)` | Date mask used |

**88-level flags**: `WS-EDIT-DATE-IS-VALID`, `WS-EDIT-DATE-IS-INVALID`, `FLG-YEAR-ISVALID`, `FLG-YEAR-NOT-OK`, `FLG-YEAR-BLANK`, `FLG-MONTH-ISVALID`, `FLG-MONTH-NOT-OK`, `FLG-MONTH-BLANK`, `FLG-DAY-ISVALID`, `FLG-DAY-NOT-OK`, `FLG-DAY-BLANK`, `THIS-CENTURY` (20), `LAST-CENTURY` (19), `WS-VALID-MONTH` (1–12), `WS-31-DAY-MONTH` (1,3,5,7,8,10,12), `WS-FEBRUARY` (2), `WS-VALID-DAY` (1–31), `WS-DAY-31`, `WS-DAY-30`, `WS-DAY-29`.

---

## 20. Abend Data — CSMSG02Y

**Copybook**: `app/cpy/CSMSG02Y.cpy` · **Record**: `ABEND-DATA`

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 1 | ABEND-CODE | A | 4 | `PIC X(4)` | CICS/system abend code |
| 2 | ABEND-CULPRIT | A | 8 | `PIC X(8)` | Program that caused the abend |
| 3 | ABEND-REASON | A | 50 | `PIC X(50)` | Reason text for the abend |
| 4 | ABEND-MSG | A | 72 | `PIC X(72)` | Formatted abend message for display/logging |

---

## 21. Common Messages — CSMSG01Y

**Copybook**: `app/cpy/CSMSG01Y.cpy` · **Record**: `CCDA-COMMON-MESSAGES`

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 1 | CCDA-MSG-THANK-YOU | A | 50 | `PIC X(50)` | Sign-off "Thank you" message |
| 2 | CCDA-MSG-INVALID-KEY | A | 50 | `PIC X(50)` | Invalid key press message |

---

## 22. Screen Title — COTTL01Y

**Copybook**: `app/cpy/COTTL01Y.cpy` · **Record**: `CCDA-SCREEN-TITLE`

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 1 | CCDA-TITLE01 | A | 40 | `PIC X(40)` | Title line 1 ("AWS Mainframe Modernization") |
| 2 | CCDA-TITLE02 | A | 40 | `PIC X(40)` | Title line 2 ("CardDemo") |
| 3 | CCDA-THANK-YOU | A | 40 | `PIC X(40)` | Sign-off message |

---

## 23. Lookup Codes — CSLKPCDY

**Copybook**: `app/cpy/CSLKPCDY.cpy` · 1,318 lines

Contains three hard-coded lookup tables used for input validation:

| Table | Variable | 88-Level | Business Description |
|-------|----------|----------|----------------------|
| North America Area Codes | `WS-US-PHONE-AREA-CODE-TO-EDIT PIC XXX` | `VALID-PHONE-AREA-CODE` | ~490 valid NANPA area codes |
| US State Codes | (same variable) | `VALID-GENERAL-PURP-CODE` | General-purpose code validation |
| State + ZIP Prefix | (embedded) | (embedded) | State code to first-2-digits-of-ZIP mapping |

---

## 24. Menu Options — Regular User — COMEN02Y

**Copybook**: `app/cpy/COMEN02Y.cpy` · **Record**: `CARDDEMO-MAIN-MENU-OPTIONS`

Defines the 11 menu items available to regular users. Each entry contains:

| Field | Format | Business Description |
|-------|--------|----------------------|
| CDEMO-MENU-OPT-NUM | `PIC 9(02)` | Menu option number (1–11) |
| CDEMO-MENU-OPT-NAME | `PIC X(35)` | Display label |
| CDEMO-MENU-OPT-PGMNAME | `PIC X(08)` | Target COBOL program |
| CDEMO-MENU-OPT-USRTYPE | `PIC X(01)` | Required user type ("U" = any user) |

**Menu Items**: Account View, Account Update, Credit Card List, Credit Card View, Credit Card Update, Transaction List, Transaction View, Transaction Add, Transaction Reports, Bill Payment, Pending Authorization View.

---

## 25. Menu Options — Admin — COADM02Y

**Copybook**: `app/cpy/COADM02Y.cpy` · **Record**: `CARDDEMO-ADMIN-MENU-OPTIONS`

Defines 6 admin-only menu items:

| Field | Format | Business Description |
|-------|--------|----------------------|
| CDEMO-ADMIN-OPT-NUM | `PIC 9(02)` | Option number (1–6) |
| CDEMO-ADMIN-OPT-NAME | `PIC X(35)` | Display label |
| CDEMO-ADMIN-OPT-PGMNAME | `PIC X(08)` | Target program |

**Menu Items**: User List (Security), User Add (Security), User Update (Security), User Delete (Security), Transaction Type List/Update (DB2), Transaction Type Maintenance (DB2).

---

## 26. Unused Record — UNUSED1Y

**Copybook**: `app/cpy/UNUSED1Y.cpy` · **Record**: `UNUSED-DATA`

Appears to be a deprecated copy of the security record layout. Same structure as `SEC-USER-DATA` (§14) with field names changed to `UNUSED-*`.

---

## 27. Sub-App: Authorization Request — CCPAURQY

**Copybook**: `app/app-authorization-ims-db2-mq/cpy/CCPAURQY.cpy`

MQ message layout for incoming authorization requests from the payment network.

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 1 | PA-RQ-AUTH-DATE | A | 6 | `PIC X(06)` | Authorization request date (MMDDYY) |
| 2 | PA-RQ-AUTH-TIME | A | 6 | `PIC X(06)` | Authorization request time (HHMMSS) |
| 3 | PA-RQ-CARD-NUM | A | 16 | `PIC X(16)` | Card number being authorized |
| 4 | PA-RQ-AUTH-TYPE | A | 4 | `PIC X(04)` | Authorization type |
| 5 | PA-RQ-CARD-EXPIRY-DATE | A | 4 | `PIC X(04)` | Card expiry (MMYY) |
| 6 | PA-RQ-MESSAGE-TYPE | A | 6 | `PIC X(06)` | ISO 8583 message type (e.g., "0100") |
| 7 | PA-RQ-MESSAGE-SOURCE | A | 6 | `PIC X(06)` | Source system identifier |
| 8 | PA-RQ-PROCESSING-CODE | N | 6 | `PIC 9(06)` | ISO 8583 processing code |
| 9 | PA-RQ-TRANSACTION-AMT | AN | 13 | `PIC +9(10).99` | Requested transaction amount (edited) |
| 10 | PA-RQ-MERCHANT-CATAGORY-CODE | A | 4 | `PIC X(04)` | MCC (Merchant Category Code) |
| 11 | PA-RQ-ACQR-COUNTRY-CODE | A | 3 | `PIC X(03)` | Acquirer country code |
| 12 | PA-RQ-POS-ENTRY-MODE | N | 2 | `PIC 9(02)` | POS entry mode (chip, swipe, manual) |
| 13 | PA-RQ-MERCHANT-ID | A | 15 | `PIC X(15)` | Merchant ID |
| 14 | PA-RQ-MERCHANT-NAME | A | 22 | `PIC X(22)` | Merchant name |
| 15 | PA-RQ-MERCHANT-CITY | A | 13 | `PIC X(13)` | Merchant city |
| 16 | PA-RQ-MERCHANT-STATE | A | 2 | `PIC X(02)` | Merchant state |
| 17 | PA-RQ-MERCHANT-ZIP | A | 9 | `PIC X(09)` | Merchant ZIP |
| 18 | PA-RQ-TRANSACTION-ID | A | 15 | `PIC X(15)` | Network-assigned transaction ID |

---

## 28. Sub-App: Authorization Response — CCPAURLY

**Copybook**: `app/app-authorization-ims-db2-mq/cpy/CCPAURLY.cpy`

MQ message layout for outgoing authorization responses sent back to the payment network.

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 1 | PA-RL-CARD-NUM | A | 16 | `PIC X(16)` | Card number |
| 2 | PA-RL-TRANSACTION-ID | A | 15 | `PIC X(15)` | Transaction ID (echoed from request) |
| 3 | PA-RL-AUTH-ID-CODE | A | 6 | `PIC X(06)` | Authorization identification code (approval code) |
| 4 | PA-RL-AUTH-RESP-CODE | A | 2 | `PIC X(02)` | Response code ("00" = approved, others = declined) |
| 5 | PA-RL-AUTH-RESP-REASON | A | 4 | `PIC X(04)` | Decline reason code |
| 6 | PA-RL-APPROVED-AMT | AN | 13 | `PIC +9(10).99` | Approved amount |

---

## 29. Sub-App: Authorization Error Log — CCPAUERY

**Copybook**: `app/app-authorization-ims-db2-mq/cpy/CCPAUERY.cpy` · **Record**: `ERROR-LOG-RECORD`

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 1 | ERR-DATE | A | 6 | `PIC X(06)` | Error date (MMDDYY) |
| 2 | ERR-TIME | A | 6 | `PIC X(06)` | Error time (HHMMSS) |
| 3 | ERR-APPLICATION | A | 8 | `PIC X(08)` | Application name |
| 4 | ERR-PROGRAM | A | 8 | `PIC X(08)` | Program name where error occurred |
| 5 | ERR-LOCATION | A | 4 | `PIC X(04)` | Location within program (paragraph/section) |
| 6 | ERR-LEVEL | A | 1 | `PIC X(01)` | Severity: L=Log, I=Info, W=Warning, C=Critical |
| 7 | ERR-SUBSYSTEM | A | 1 | `PIC X(01)` | Subsystem: A=App, C=CICS, I=IMS, D=DB2, M=MQ, F=File |
| 8 | ERR-CODE-1 | A | 9 | `PIC X(09)` | Primary error/return code |
| 9 | ERR-CODE-2 | A | 9 | `PIC X(09)` | Secondary error/reason code |
| 10 | ERR-MESSAGE | A | 50 | `PIC X(50)` | Human-readable error message |
| 11 | ERR-EVENT-KEY | A | 20 | `PIC X(20)` | Key identifying the event that caused the error |

---

## 30. Sub-App: IMS Pending Auth Detail — CIPAUDTY

**Copybook**: `app/app-authorization-ims-db2-mq/cpy/CIPAUDTY.cpy`

IMS database segment storing detailed authorization records under a card-level parent.

| # | Field | Type | Length | Format | Storage | Business Description |
|---|-------|------|--------|--------|---------|----------------------|
| 1 | PA-AUTH-DATE-9C | SN | 5 | `PIC S9(05)` | COMP-3 | Auth date (packed, for IMS key sequencing) |
| 2 | PA-AUTH-TIME-9C | SN | 9 | `PIC S9(09)` | COMP-3 | Auth time (packed, for IMS key sequencing) |
| 3 | PA-AUTH-ORIG-DATE | A | 6 | `PIC X(06)` | Display | Original authorization date |
| 4 | PA-AUTH-ORIG-TIME | A | 6 | `PIC X(06)` | Display | Original authorization time |
| 5 | PA-CARD-NUM | A | 16 | `PIC X(16)` | Display | Card number |
| 6 | PA-AUTH-TYPE | A | 4 | `PIC X(04)` | Display | Authorization type |
| 7 | PA-CARD-EXPIRY-DATE | A | 4 | `PIC X(04)` | Display | Card expiry date |
| 8 | PA-MESSAGE-TYPE | A | 6 | `PIC X(06)` | Display | ISO message type |
| 9 | PA-MESSAGE-SOURCE | A | 6 | `PIC X(06)` | Display | Message source |
| 10 | PA-AUTH-ID-CODE | A | 6 | `PIC X(06)` | Display | Authorization ID / approval code |
| 11 | PA-AUTH-RESP-CODE | A | 2 | `PIC X(02)` | Display | Response code ("00" = approved) |
| 12 | PA-AUTH-RESP-REASON | A | 4 | `PIC X(04)` | Display | Decline reason code |
| 13 | PA-PROCESSING-CODE | N | 6 | `PIC 9(06)` | Display | Processing code |
| 14 | PA-TRANSACTION-AMT | SN | 12.2 | `PIC S9(10)V99` | COMP-3 | Requested transaction amount |
| 15 | PA-APPROVED-AMT | SN | 12.2 | `PIC S9(10)V99` | COMP-3 | Approved amount |
| 16 | PA-MERCHANT-CATAGORY-CODE | A | 4 | `PIC X(04)` | Display | Merchant Category Code |
| 17 | PA-ACQR-COUNTRY-CODE | A | 3 | `PIC X(03)` | Display | Acquirer country code |
| 18 | PA-POS-ENTRY-MODE | N | 2 | `PIC 9(02)` | Display | POS entry mode |
| 19 | PA-MERCHANT-ID | A | 15 | `PIC X(15)` | Display | Merchant ID |
| 20 | PA-MERCHANT-NAME | A | 22 | `PIC X(22)` | Display | Merchant name |
| 21 | PA-MERCHANT-CITY | A | 13 | `PIC X(13)` | Display | Merchant city |
| 22 | PA-MERCHANT-STATE | A | 2 | `PIC X(02)` | Display | Merchant state |
| 23 | PA-MERCHANT-ZIP | A | 9 | `PIC X(09)` | Display | Merchant ZIP |
| 24 | PA-TRANSACTION-ID | A | 15 | `PIC X(15)` | Display | Transaction ID |
| 25 | PA-MATCH-STATUS | A | 1 | `PIC X(01)` | Display | P=Pending, D=Declined, E=Expired, M=Matched |
| 26 | PA-AUTH-FRAUD | A | 1 | `PIC X(01)` | Display | F=Fraud confirmed, R=Fraud removed |
| 27 | PA-FRAUD-RPT-DATE | A | 8 | `PIC X(08)` | Display | Date fraud was reported |
| 28 | FILLER | A | 17 | `PIC X(17)` | Display | Reserved |

---

## 31. Sub-App: IMS Pending Auth Summary — CIPAUSMY

**Copybook**: `app/app-authorization-ims-db2-mq/cpy/CIPAUSMY.cpy`

IMS database segment storing account-level authorization summary (parent segment in the IMS hierarchy).

| # | Field | Type | Length | Format | Storage | Business Description |
|---|-------|------|--------|--------|---------|----------------------|
| 1 | PA-ACCT-ID | SN | 11 | `PIC S9(11)` | COMP-3 | Account ID (IMS key) |
| 2 | PA-CUST-ID | N | 9 | `PIC 9(09)` | Display | Customer ID |
| 3 | PA-AUTH-STATUS | A | 1 | `PIC X(01)` | Display | Overall authorization status |
| 4 | PA-ACCOUNT-STATUS | A | 2 | `PIC X(02)` | Display | Account status codes (OCCURS 5 TIMES) |
| 5 | PA-CREDIT-LIMIT | SN | 11.2 | `PIC S9(09)V99` | COMP-3 | Credit limit |
| 6 | PA-CASH-LIMIT | SN | 11.2 | `PIC S9(09)V99` | COMP-3 | Cash advance limit |
| 7 | PA-CREDIT-BALANCE | SN | 11.2 | `PIC S9(09)V99` | COMP-3 | Credit balance |
| 8 | PA-CASH-BALANCE | SN | 11.2 | `PIC S9(09)V99` | COMP-3 | Cash advance balance |
| 9 | PA-APPROVED-AUTH-CNT | SN | 4 | `PIC S9(04)` | COMP | Count of approved authorizations |
| 10 | PA-DECLINED-AUTH-CNT | SN | 4 | `PIC S9(04)` | COMP | Count of declined authorizations |
| 11 | PA-APPROVED-AUTH-AMT | SN | 11.2 | `PIC S9(09)V99` | COMP-3 | Total approved amount |
| 12 | PA-DECLINED-AUTH-AMT | SN | 11.2 | `PIC S9(09)V99` | COMP-3 | Total declined amount |
| 13 | FILLER | A | 34 | `PIC X(34)` | Display | Reserved |

---

## 32. Sub-App: IMS Function Codes — IMSFUNCS

**Copybook**: `app/app-authorization-ims-db2-mq/cpy/IMSFUNCS.cpy` · **Record**: `FUNC-CODES`

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 1 | FUNC-GU | A | 4 | `PIC X(04)` | Get Unique (read by key) |
| 2 | FUNC-GHU | A | 4 | `PIC X(04)` | Get Hold Unique (read for update) |
| 3 | FUNC-GN | A | 4 | `PIC X(04)` | Get Next (sequential read) |
| 4 | FUNC-GHN | A | 4 | `PIC X(04)` | Get Hold Next (sequential read for update) |
| 5 | FUNC-GNP | A | 4 | `PIC X(04)` | Get Next within Parent |
| 6 | FUNC-GHNP | A | 4 | `PIC X(04)` | Get Hold Next within Parent |
| 7 | FUNC-REPL | A | 4 | `PIC X(04)` | Replace (update segment) |
| 8 | FUNC-ISRT | A | 4 | `PIC X(04)` | Insert (add segment) |
| 9 | FUNC-DLET | A | 4 | `PIC X(04)` | Delete (remove segment) |
| 10 | PARMCOUNT | SN | 5 | `PIC S9(05)` | COMP-5 | Parameter count for DL/I calls |

---

## 33. Sub-App: DB2 Authorization Fraud Table — AUTHFRDS

**DCL file**: `app/app-authorization-ims-db2-mq/dcl/AUTHFRDS.dcl` · **DB2 Table**: `CARDDEMO.AUTHFRDS`

Authorization records persisted to DB2 for fraud analysis. Contains 26 columns.

| # | SQL Column | DB2 Type | COBOL Field | COBOL Format | Business Description |
|---|-----------|----------|-------------|--------------|----------------------|
| 1 | CARD_NUM | CHAR(16) NOT NULL | CARD-NUM | `PIC X(16)` | Card number (PK part 1) |
| 2 | AUTH_TS | TIMESTAMP NOT NULL | AUTH-TS | `PIC X(26)` | Authorization timestamp (PK part 2) |
| 3 | AUTH_TYPE | CHAR(4) | AUTH-TYPE | `PIC X(4)` | Authorization type |
| 4 | CARD_EXPIRY_DATE | CHAR(4) | CARD-EXPIRY-DATE | `PIC X(4)` | Card expiry (MMYY) |
| 5 | MESSAGE_TYPE | CHAR(6) | MESSAGE-TYPE | `PIC X(6)` | ISO 8583 message type |
| 6 | MESSAGE_SOURCE | CHAR(6) | MESSAGE-SOURCE | `PIC X(6)` | Source system |
| 7 | AUTH_ID_CODE | CHAR(6) | AUTH-ID-CODE | `PIC X(6)` | Authorization ID / approval code |
| 8 | AUTH_RESP_CODE | CHAR(2) | AUTH-RESP-CODE | `PIC X(2)` | Response code |
| 9 | AUTH_RESP_REASON | CHAR(4) | AUTH-RESP-REASON | `PIC X(4)` | Decline reason |
| 10 | PROCESSING_CODE | CHAR(6) | PROCESSING-CODE | `PIC X(6)` | Processing code |
| 11 | TRANSACTION_AMT | DECIMAL(12,2) | TRANSACTION-AMT | `PIC S9(10)V9(2) COMP-3` | Transaction amount |
| 12 | APPROVED_AMT | DECIMAL(12,2) | APPROVED-AMT | `PIC S9(10)V9(2) COMP-3` | Approved amount |
| 13 | MERCHANT_CATAGORY_CODE | CHAR(4) | MERCHANT-CATAGORY-CODE | `PIC X(4)` | Merchant Category Code |
| 14 | ACQR_COUNTRY_CODE | CHAR(3) | ACQR-COUNTRY-CODE | `PIC X(3)` | Acquirer country code |
| 15 | POS_ENTRY_MODE | SMALLINT | POS-ENTRY-MODE | `PIC S9(4) COMP` | POS entry mode |
| 16 | MERCHANT_ID | CHAR(15) | MERCHANT-ID | `PIC X(15)` | Merchant ID |
| 17 | MERCHANT_NAME | VARCHAR(22) | MERCHANT-NAME-TEXT | `PIC X(22)` | Merchant name |
| 18 | MERCHANT_CITY | CHAR(13) | MERCHANT-CITY | `PIC X(13)` | Merchant city |
| 19 | MERCHANT_STATE | CHAR(2) | MERCHANT-STATE | `PIC X(2)` | Merchant state |
| 20 | MERCHANT_ZIP | CHAR(9) | MERCHANT-ZIP | `PIC X(9)` | Merchant ZIP |
| 21 | TRANSACTION_ID | CHAR(15) | TRANSACTION-ID | `PIC X(15)` | Transaction ID |
| 22 | MATCH_STATUS | CHAR(1) | MATCH-STATUS | `PIC X(1)` | Match status (P/D/E/M) |
| 23 | AUTH_FRAUD | CHAR(1) | AUTH-FRAUD | `PIC X(1)` | Fraud flag (F/R) |
| 24 | FRAUD_RPT_DATE | DATE | FRAUD-RPT-DATE | `PIC X(10)` | Fraud report date |
| 25 | ACCT_ID | DECIMAL(11,0) | ACCT-ID | `PIC S9(11)V COMP-3` | Account ID |
| 26 | CUST_ID | DECIMAL(9,0) | CUST-ID | `PIC S9(9)V COMP-3` | Customer ID |

---

## 34. Sub-App: DB2 Transaction Type Table — DCLTRTYP

**DCL file**: `app/app-transaction-type-db2/dcl/DCLTRTYP.dcl` · **DB2 Table**: `CARDDEMO.TRANSACTION_TYPE`

| # | SQL Column | DB2 Type | COBOL Field | COBOL Format | Business Description |
|---|-----------|----------|-------------|--------------|----------------------|
| 1 | TR_TYPE | CHAR(2) NOT NULL | DCL-TR-TYPE | `PIC X(2)` | Transaction type code (PK) |
| 2 | TR_DESCRIPTION | VARCHAR(50) NOT NULL | DCL-TR-DESCRIPTION-TEXT | `PIC X(50)` | Type description |

---

## 35. Sub-App: DB2 Transaction Category Table — DCLTRCAT

**DCL file**: `app/app-transaction-type-db2/dcl/DCLTRCAT.dcl` · **DB2 Table**: `CARDDEMO.TRANSACTION_TYPE_CATEGORY`

| # | SQL Column | DB2 Type | COBOL Field | COBOL Format | Business Description |
|---|-----------|----------|-------------|--------------|----------------------|
| 1 | TRC_TYPE_CODE | CHAR(2) NOT NULL | DCL-TRC-TYPE-CODE | `PIC X(2)` | Type code (composite PK part 1) |
| 2 | TRC_TYPE_CATEGORY | CHAR(4) NOT NULL | DCL-TRC-TYPE-CATEGORY | `PIC X(4)` | Category code (composite PK part 2) |
| 3 | TRC_CAT_DATA | VARCHAR(50) NOT NULL | DCL-TRC-CAT-DATA-TEXT | `PIC X(50)` | Category description |

---

## 36. Sub-App: DB2 Common Working Storage — CSDB2RWY

**Copybook**: `app/app-transaction-type-db2/cpy/CSDB2RWY.cpy`

Working-storage variables used by all DB2-accessing programs for SQL error handling and DSNTIAC message formatting.

| # | Field | Type | Length | Format | Business Description |
|---|-------|------|--------|--------|----------------------|
| 1 | WS-DISP-SQLCODE | AN | 5 | `PIC ----9` | Formatted SQLCODE for display |
| 2 | WS-DUMMY-DB2-INT | SN | 4 | `PIC S9(4) COMP-3` | Dummy integer for DB2 connectivity test |
| 3 | WS-DB2-PROCESSING-FLAG | A | 1 | `PIC X(1)` | "0" = OK, "1" = error |
| 4 | WS-DB2-CURRENT-ACTION | A | 72 | `PIC X(72)` | Description of current DB2 action (for error messages) |
| 5 | WS-DSNTIAC-MESG-LEN | SN | 4 | `PIC S9(4) COMP` | DSNTIAC message buffer length (720) |
| 6 | WS-DSNTIAC-FMTD-TEXT-LINE | A | 72 | `PIC X(72)` | Formatted message line (OCCURS 10) |
| 7 | WS-DSNTIAC-LRECL | SN | 4 | `PIC S9(4) COMP` | DSNTIAC logical record length (72) |
| 8 | WS-DSNTIAC-ERR-MSG | A | 10 | `PIC X(10)` | DSNTIAC error label |
| 9 | WS-DSNTIAC-ERR-CD | N | 2 | `PIC 9(02)` | DSNTIAC return code |

---

## Entity Relationship Summary

```
CUSTOMER (CVCUS01Y)
  │
  ├── 1:N ── CARD-XREF (CVACT03Y)  ── links Customer to Card + Account
  │              │
  │              └── → CARD (CVACT02Y)
  │              └── → ACCOUNT (CVACT01Y)
  │
  └── via CARD-XREF:
         │
         ACCOUNT (CVACT01Y)
           │
           ├── 1:N ── TRANSACTION (CVTRA05Y / CVTRA06Y)
           ├── 1:N ── TRAN-CAT-BALANCE (CVTRA01Y)  ── running balance per category
           └── N:1 ── DISCLOSURE-GROUP (CVTRA02Y)   ── interest rates by group+type+cat
                        │
                        └── uses TRAN-TYPE (CVTRA03Y) + TRAN-CAT (CVTRA04Y)

  Authorization Sub-App (IMS hierarchy):
     AUTH-SUMMARY (CIPAUSMY)  ── keyed by Account
       └── AUTH-DETAIL (CIPAUDTY)  ── keyed by Date+Time under Account
              ↕ (MQ messages)
           AUTH-REQUEST (CCPAURQY) / AUTH-RESPONSE (CCPAURLY)
              ↕ (persisted to DB2)
           AUTHFRDS table

  Security:
     SEC-USER-DATA (CSUSR01Y)  ── standalone, keyed by User ID
```

---

*Generated from CardDemo copybooks in `app/cpy/`, `app/app-authorization-ims-db2-mq/cpy/`, `app/app-transaction-type-db2/cpy/`, and DCL files. Cross-reference with `CATALOG.md` for the full artifact inventory.*
