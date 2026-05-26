# CardDemo Application — Full Artifact Catalog

> **Repository:** `EvangelosG/uc-legacy-modernization-cobol-to-java`
> **Total Artifacts Cataloged:** 130+ (30+ COBOL programs, 30+ copybooks, 40+ JCL jobs, 3 sub-applications, plus BMS maps, assembler, scheduler, data files, samples, and scripts)

---

## 1. COBOL Programs — Core Application (`app/cbl/`)

### 1.1 Online (CICS) Programs

| # | Program | Transaction | Type | Function | Sub-Domain |
|---|---------|-------------|------|----------|------------|
| 1 | COSGN00C | CC00 | Online / CICS | Signon Screen | Authentication |
| 2 | COMEN01C | CM00 | Online / CICS | Main Menu (Regular Users) | Navigation |
| 3 | COADM01C | CA00 | Online / CICS | Admin Menu | Navigation / Admin |
| 4 | COACTVWC | CAVW | Online / CICS | Account View | Account Mgmt |
| 5 | COACTUPC | CAUP | Online / CICS | Account Update | Account Mgmt |
| 6 | COCRDLIC | CCLI | Online / CICS | Credit Card List | Card Mgmt |
| 7 | COCRDSLC | CCDL | Online / CICS | Credit Card View/Detail | Card Mgmt |
| 8 | COCRDUPC | CCUP | Online / CICS | Credit Card Update | Card Mgmt |
| 9 | COTRN00C | CT00 | Online / CICS | Transaction List | Transaction Mgmt |
| 10 | COTRN01C | CT01 | Online / CICS | Transaction View | Transaction Mgmt |
| 11 | COTRN02C | CT02 | Online / CICS | Transaction Add | Transaction Mgmt |
| 12 | CORPT00C | CR00 | Online / CICS | Submit Transaction Report (batch) | Reporting |
| 13 | COBIL00C | CB00 | Online / CICS | Bill Payment | Billing |
| 14 | COUSR00C | CU00 | Online / CICS | List Users | User Admin |
| 15 | COUSR01C | CU01 | Online / CICS | Add User | User Admin |
| 16 | COUSR02C | CU02 | Online / CICS | Update User | User Admin |
| 17 | COUSR03C | CU03 | Online / CICS | Delete User | User Admin |

### 1.2 Batch Programs

| # | Program | Type | Function | Sub-Domain |
|---|---------|------|----------|------------|
| 18 | CBACT01C | Batch | Read Account File and write reports | Account Mgmt |
| 19 | CBACT02C | Batch | Read and print Card Data file | Card Mgmt |
| 20 | CBACT03C | Batch | Read and print Account Cross-Reference file | Account Mgmt |
| 21 | CBACT04C | Batch | Interest Calculator | Financial Calc |
| 22 | CBCUS01C | Batch | Read and print Customer Data file | Customer Mgmt |
| 23 | CBTRN01C | Batch | Post daily transaction records (step 1) | Transaction Processing |
| 24 | CBTRN02C | Batch | Post daily transaction records (main) | Transaction Processing |
| 25 | CBTRN03C | Batch | Print transaction detail report | Reporting |
| 26 | CBSTM03A | Batch | Print Account Statements (text & HTML) | Statement Generation |
| 27 | CBSTM03B | Batch Subroutine | File processing for transaction report | Statement Generation |
| 28 | CBEXPORT | Batch | Export Customer Data for branch migration | Data Migration |
| 29 | CBIMPORT | Batch | Import Customer Data from branch export | Data Migration |

### 1.3 Utility Programs

| # | Program | Type | Function | Sub-Domain |
|---|---------|------|----------|------------|
| 30 | COBSWAIT | Batch Utility | Wait utility (PARM in centiseconds) | System Utility |
| 31 | CSUTLDTC | Batch Utility | Date conversion (CEEDAYS call) | System Utility |

---

## 2. Sub-Application: Authorization (IMS-DB2-MQ) (`app/app-authorization-ims-db2-mq/`)

### 2.1 Programs

| # | Program | Transaction | Type | Function |
|---|---------|-------------|------|----------|
| 32 | COPAUA0C | CP00 | Online / MQ Trigger | Card Authorization Decision (MQ req/resp, IMS insert/update) |
| 33 | COPAUS0C | CPVS | Online / CICS | Pending Authorization Summary View (IMS + VSAM read) |
| 34 | COPAUS1C | CPVD | Online / CICS | Pending Authorization Detail View (IMS update, DB2 insert) |
| 35 | COPAUS2C | — | Online / CICS | Mark Authorization Message as Fraud |
| 36 | CBPAUP0C | — | Batch | Delete/Purge Expired Pending Authorizations |
| 37 | DBUNLDGS | — | Batch / IMS Utility | IMS Database Unload (GSAM) |
| 38 | PAUDBLOD | — | Batch / IMS Utility | IMS Database Load |
| 39 | PAUDBUNL | — | Batch / IMS Utility | IMS Database Unload |

### 2.2 Copybooks

| # | Copybook | Purpose |
|---|----------|---------|
| 40 | CCPAUERY | Authorization error/response layout |
| 41 | CCPAURLY | Authorization reply layout |
| 42 | CCPAURQY | Authorization request layout |
| 43 | CIPAUDTY | Authorization detail CICS input area |
| 44 | CIPAUSMY | Authorization summary CICS input area |
| 45 | IMSFUNCS | IMS function call definitions |
| 46 | PADFLPCB | IMS PCB layout (detail file) |
| 47 | PASFLPCB | IMS PCB layout (summary file) |
| 48 | PAUTBPCB | IMS PCB layout (authorization table) |

### 2.3 BMS Maps

| # | Map | Screen |
|---|-----|--------|
| 49 | COPAU00 | Pending Authorization Summary screen |
| 50 | COPAU01 | Pending Authorization Detail screen |

### 2.4 JCL Jobs

| # | Job | Function |
|---|-----|----------|
| 51 | CBPAUP0J | Purge expired authorizations |
| 52 | DBPAUTP0 | IMS database operations |
| 53 | LOADPADB | Load authorization IMS database |
| 54 | UNLDGSAM | Unload IMS database (GSAM) |
| 55 | UNLDPADB | Unload authorization IMS database |

### 2.5 Other Artifacts

| Type | File(s) | Description |
|------|---------|-------------|
| DDL | AUTHFRDS.ddl, XAUTHFRD.ddl | DB2 table and index definitions for fraud records |
| DCL | AUTHFRDS.dcl | DB2 DCLGEN for fraud table |
| IMS DBD | DBPAUTP0.dbd, DBPAUTX0.dbd, PADFLDBD.DBD, PASFLDBD.DBD | IMS Database Definitions |
| IMS PSB | DLIGSAMP.PSB, PSBPAUTB.psb, PSBPAUTL.psb, PAUTBUNL.PSB | IMS Program Specification Blocks |
| CSD | CRDDEMO2.csd | CICS resource definitions for auth module |
| Data | AWS.M2.CARDDEMO.IMSDATA.DBPAUTP0.dat | EBCDIC IMS seed data |

---

## 3. Sub-Application: Transaction Type Management (DB2) (`app/app-transaction-type-db2/`)

### 3.1 Programs

| # | Program | Transaction | Type | Function |
|---|---------|-------------|------|----------|
| 56 | COTRTLIC | CTLI | Online / CICS + DB2 | Transaction Type List / Update / Delete (DB2 cursor & delete) |
| 57 | COTRTUPC | CTTU | Online / CICS + DB2 | Transaction Type Add / Edit (DB2 insert & update) |
| 58 | COBTUPDT | — | Batch / DB2 | Maintain transaction type table from batch input |

### 3.2 Copybooks

| # | Copybook | Purpose |
|---|----------|---------|
| 59 | CSDB2RPY | DB2 reply/return area |
| 60 | CSDB2RWY | DB2 work area |

### 3.3 BMS Maps

| # | Map | Screen |
|---|-----|--------|
| 61 | COTRTLI | Transaction Type List screen |
| 62 | COTRTUP | Transaction Type Add/Edit screen |

### 3.4 JCL Jobs

| # | Job | Function |
|---|-----|----------|
| 63 | CREADB21 | Create CardDemo DB2 database and load tables |
| 64 | MNTTRDB2 | Maintain transaction type table (batch) |
| 65 | TRANEXTR | Extract latest DB2 data for transaction types |

### 3.5 Other Artifacts

| Type | File(s) | Description |
|------|---------|-------------|
| DDL | TRNTYCAT.ddl, TRNTYPE.ddl, XTRNTYCAT.ddl, XTRNTYPE.ddl | DB2 table and index definitions |
| DCL | DCLTRCAT.dcl, DCLTRTYP.dcl | DB2 DCLGEN for category & type tables |
| CTL | DB2CREAT.ctl, DB2FREE.ctl, DB2LTCAT.ctl, DB2LTTYP.ctl, DB2TEP41.ctl, DB2TIAD1.ctl, REPROCT.ctl | DB2 control cards |
| CSD | CRDDEMOD.csd | CICS resource definitions for DB2 module |

---

## 4. Sub-Application: Account Extractions (VSAM-MQ) (`app/app-vsam-mq/`)

### 4.1 Programs

| # | Program | Transaction | Type | Function |
|---|---------|-------------|------|----------|
| 66 | CODATE01 | CDRD | Online / MQ | Inquire System Date via MQ request/response |
| 67 | COACCT01 | CDRA | Online / MQ | Inquire Account Details via MQ request/response |

### 4.2 Other Artifacts

| Type | File(s) | Description |
|------|---------|-------------|
| CSD | CRDDEMOM.csd | CICS resource definitions for MQ module |

---

## 5. Copybooks — Core Application (`app/cpy/`)

### 5.1 Data Record Layouts (VSAM file structures)

| # | Copybook | Record Layout | Domain |
|---|----------|---------------|--------|
| 68 | CVACT01Y | Account Record (300 bytes) | Account Data |
| 69 | CVACT02Y | Card Record (150 bytes) | Card Data |
| 70 | CVACT03Y | Card Cross-Reference Record (50 bytes) | Account/Card XREF |
| 71 | CVCUS01Y | Customer Record (500 bytes) | Customer Data |
| 72 | CUSTREC | Customer Record (alternate layout) | Customer Data |
| 73 | CVTRA01Y | Transaction Category Balance Record (50 bytes) | Transaction Data |
| 74 | CVTRA02Y | Disclosure Group Record (50 bytes) | Transaction Data |
| 75 | CVTRA03Y | Transaction Type Record (60 bytes) | Transaction Data |
| 76 | CVTRA04Y | Transaction Category Record (60 bytes) | Transaction Data |
| 77 | CVTRA05Y | Transaction Record (350 bytes) | Transaction Data |
| 78 | CVTRA06Y | Daily Transaction Record (350 bytes) | Transaction Data |
| 79 | CVTRA07Y | Transaction work area (additional) | Transaction Data |
| 80 | CSUSR01Y | User Security Record (80 bytes) | Security |
| 81 | CVEXPORT | Multi-Record Export Layout (500 bytes, REDEFINES) | Data Migration |

### 5.2 Application Communication & Control Areas

| # | Copybook | Purpose | Domain |
|---|----------|---------|--------|
| 82 | COCOM01Y | Communication area for CardDemo programs | Inter-program comm |
| 83 | COMEN02Y | Menu option definitions | Navigation |
| 84 | COADM02Y | Admin menu data area | Admin Navigation |
| 85 | COTTL01Y | Title/header line layout | Display |
| 86 | CSMSG01Y | Message area layout | Messaging |
| 87 | CSMSG02Y | Abend routine work areas | Error Handling |
| 88 | CSSETATY | Set attribute byte definitions | Screen Control |
| 89 | CSSTRPFY | String processing functions | Utility |
| 90 | CSLKPCDY | North America phone area code lookup | Lookup/Validation |
| 91 | CODATECN | Date conversion copybook | Utility |
| 92 | CSDAT01Y | Date/time work areas | Utility |
| 93 | CSUTLDPY | Utility display parameters | Utility |
| 94 | CSUTLDWY | Utility work areas | Utility |
| 95 | CVCRD01Y | Credit card work areas | Card Processing |
| 96 | COSTM01 | Statement format layout | Statement Generation |
| 97 | UNUSED1Y | Unused / placeholder | N/A |

### 5.3 BMS-Generated Copybooks (`app/cpy-bms/`)

| # | Copybook | Screen | Notes |
|---|----------|--------|-------|
| 98 | COSGN00 | Signon | Auto-generated from BMS map |
| 99 | COMEN01 | Main Menu | Auto-generated from BMS map |
| 100 | COADM01 | Admin Menu | Auto-generated from BMS map |
| 101 | COACTUP | Account Update | Auto-generated from BMS map |
| 102 | COACTVW | Account View | Auto-generated from BMS map |
| 103 | COCRDLI | Card List | Auto-generated from BMS map |
| 104 | COCRDSL | Card Detail | Auto-generated from BMS map |
| 105 | COCRDUP | Card Update | Auto-generated from BMS map |
| 106 | COTRN00 | Transaction List | Auto-generated from BMS map |
| 107 | COTRN01 | Transaction View | Auto-generated from BMS map |
| 108 | COTRN02 | Transaction Add | Auto-generated from BMS map |
| 109 | CORPT00 | Transaction Report | Auto-generated from BMS map |
| 110 | COBIL00 | Bill Payment | Auto-generated from BMS map |
| 111 | COUSR00 | User List | Auto-generated from BMS map |
| 112 | COUSR01 | User Add | Auto-generated from BMS map |
| 113 | COUSR02 | User Update | Auto-generated from BMS map |
| 114 | COUSR03 | User Delete | Auto-generated from BMS map |

---

## 6. JCL Jobs — Core Application (`app/jcl/`)

### 6.1 Environment Setup / VSAM File Loading

| # | Job | Program | Function | Classification |
|---|-----|---------|----------|----------------|
| 115 | DUSRSECJ | IEBGENER | Initial load of User Security file | File Init |
| 116 | ACCTFILE | IDCAMS | Refresh Account Master VSAM | File Init |
| 117 | CARDFILE | IDCAMS | Refresh Card Master VSAM | File Init |
| 118 | CUSTFILE | IDCAMS | Refresh Customer Master VSAM | File Init |
| 119 | XREFFILE | IDCAMS | Load Account/Card/Customer cross-reference | File Init |
| 120 | TRANFILE | IDCAMS | Load Transaction Master to VSAM | File Init |
| 121 | DISCGRP | IDCAMS | Load Disclosure Group file | File Init |
| 122 | TCATBALF | IDCAMS | Refresh Transaction Category Balance | File Init |
| 123 | TRANCATG | IDCAMS | Load Transaction Category types | File Init |
| 124 | TRANTYPE | IDCAMS | Load Transaction Type file | File Init |
| 125 | DEFGDGB | IDCAMS | Setup GDG Bases | Infrastructure |
| 126 | DEFGDGD | IDCAMS | Setup additional GDG Bases (for DB2) | Infrastructure |
| 127 | ESDSRRDS | IDCAMS | Create ESDS and RRDS VSAM files | Infrastructure |
| 128 | DEFCUST | — | Define Customer VSAM cluster | Infrastructure |
| 129 | TRANIDX | IDCAMS | Define Alternate Index on transaction file | Infrastructure |

### 6.2 CICS File Management

| # | Job | Program | Function | Classification |
|---|-----|---------|----------|----------------|
| 130 | CLOSEFIL | IEFBR14 | Close VSAM files in CICS | CICS Control |
| 131 | OPENFIL | IEFBR14 | Open/make files available to CICS | CICS Control |

### 6.3 Batch Processing (Core Business)

| # | Job | Program | Function | Classification |
|---|-----|---------|----------|----------------|
| 132 | POSTTRAN | CBTRN02C | Core daily transaction posting | Transaction Processing |
| 133 | INTCALC | CBACT04C | Run interest calculations | Financial Calc |
| 134 | TRANBKP | IDCAMS | Backup/refresh Transaction Master | Data Backup |
| 135 | COMBTRAN | SORT | Combine system + daily transaction files | Transaction Processing |
| 136 | CREASTMT | CBSTM03A | Produce account statements | Statement Generation |
| 137 | TRANREPT | CBTRN03C | Transaction detail report (from CICS) | Reporting |
| 138 | WAITSTEP | COBSWAIT | Wait/delay job step | System Utility |
| 139 | CBADMCDJ | — | Admin card processing job | Admin |

### 6.4 Data Read / Print Utilities

| # | Job | Function | Classification |
|---|-----|----------|----------------|
| 140 | READACCT | Read Account file | Data Utility |
| 141 | READCARD | Read Card file | Data Utility |
| 142 | READCUST | Read Customer file | Data Utility |
| 143 | READXREF | Read Cross-Reference file | Data Utility |
| 144 | PRTCATBL | Print Category Balance | Data Utility |
| 145 | REPTFILE | Report file processing | Data Utility |
| 146 | DALYREJS | Daily rejection processing | Data Utility |

### 6.5 Data Migration / Export-Import

| # | Job | Function | Classification |
|---|-----|----------|----------------|
| 147 | CBEXPORT | Export customer data for branch migration | Data Migration |
| 148 | CBIMPORT | Import customer data from branch export | Data Migration |

### 6.6 JCL Utilities (Optional)

| # | Job | Function | Classification |
|---|-----|----------|----------------|
| 149 | FTPJCL | FTP file transfer | Network Utility |
| 150 | TXT2PDF1 | Text-to-PDF conversion | Document Utility |
| 151 | INTRDRJ1 | Internal Reader job 1 | System Utility |
| 152 | INTRDRJ2 | Internal Reader job 2 | System Utility |

---

## 7. BMS Maps (`app/bms/`)

| # | Map | Screen | Domain |
|---|-----|--------|--------|
| 153 | COSGN00 | Signon | Authentication |
| 154 | COMEN01 | Main Menu | Navigation |
| 155 | COADM01 | Admin Menu | Admin Navigation |
| 156 | COACTUP | Account Update | Account Mgmt |
| 157 | COACTVW | Account View | Account Mgmt |
| 158 | COCRDLI | Card List | Card Mgmt |
| 159 | COCRDSL | Card Detail | Card Mgmt |
| 160 | COCRDUP | Card Update | Card Mgmt |
| 161 | COTRN00 | Transaction List | Transaction Mgmt |
| 162 | COTRN01 | Transaction View | Transaction Mgmt |
| 163 | COTRN02 | Transaction Add | Transaction Mgmt |
| 164 | CORPT00 | Transaction Report | Reporting |
| 165 | COBIL00 | Bill Payment | Billing |
| 166 | COUSR00 | User List | User Admin |
| 167 | COUSR01 | User Add | User Admin |
| 168 | COUSR02 | User Update | User Admin |
| 169 | COUSR03 | User Delete | User Admin |

---

## 8. Assembler Programs (`app/asm/`)

| # | Program | Function | Classification |
|---|---------|----------|----------------|
| 170 | MVSWAIT | Timer control for batch jobs | System Utility |
| 171 | COBDATFT | Date format conversion utility | System Utility |

**Macro Library (`app/maclib/`):** ASMWAIT.mac, COCDATFT.mac

---

## 9. Procedures (`app/proc/`)

| # | Procedure | Function |
|---|-----------|----------|
| 172 | REPROC | Reprocessing procedure |
| 173 | TRANREPT | Transaction report procedure |

---

## 10. Scheduler Definitions (`app/scheduler/`)

| File | Format | Purpose |
|------|--------|---------|
| CardDemo.ca7 | CA-7 | Job scheduling definitions for CA-7 |
| CardDemo.controlm | Control-M XML | Job scheduling definitions for Control-M |

---

## 11. CICS Resource Definitions (`app/csd/`)

| File | Scope |
|------|-------|
| CARDDEMO.CSD | Base application CSD (transaction, program, file, mapset definitions) |

---

## 12. Data Files (`app/data/`)

### EBCDIC (Mainframe format)

| File | Copybook | Content |
|------|----------|---------|
| AWS.M2.CARDDEMO.USRSEC.PS | CSUSR01Y | User Security records |
| AWS.M2.CARDDEMO.ACCTDATA.PS | CVACT01Y | Account master data |
| AWS.M2.CARDDEMO.CARDDATA.PS | CVACT02Y | Card data |
| AWS.M2.CARDDEMO.CUSTDATA.PS | CVCUS01Y | Customer data |
| AWS.M2.CARDDEMO.CARDXREF.PS | CVACT03Y | Card/Account/Customer XREF |
| AWS.M2.CARDDEMO.DALYTRAN.PS | CVTRA06Y | Daily transaction data |
| AWS.M2.CARDDEMO.DALYTRAN.PS.INIT | CVTRA06Y | Transaction init record |
| AWS.M2.CARDDEMO.DISCGRP.PS | CVTRA02Y | Disclosure Groups |
| AWS.M2.CARDDEMO.TRANCATG.PS | CVTRA04Y | Transaction Categories |
| AWS.M2.CARDDEMO.TRANTYPE.PS | CVTRA03Y | Transaction Types |
| AWS.M2.CARDDEMO.TCATBALF.PS | CVTRA01Y | Category Balances |
| AWS.M2.CARDDEMO.ACCDATA.PS | — | Additional account data |
| AWS.M2.CARDDEMO.EXPORT.DATA.PS | CVEXPORT | Branch export data |

### ASCII (Readable format)

acctdata.txt, carddata.txt, cardxref.txt, custdata.txt, dailytran.txt, discgrp.txt, tcatbal.txt, trancatg.txt, trantype.txt

---

## 13. Samples (`samples/`)

### Sample JCL (Compilation)

| Job | Purpose |
|-----|---------|
| BATCMP | Compile batch COBOL programs |
| BMSCMP | Compile BMS maps |
| CICCMP | Compile CICS online COBOL programs |
| CICDBCMP | Compile CICS + DB2 programs |
| IMSMQCMP | Compile IMS/MQ programs |
| RACFCMDS | RACF security setup commands |
| LISTCAT | List VSAM catalog entries |
| REPRTEST | Reprocessing test |
| SORTTEST | SORT utility test |

### Sample Procedures

| Procedure | Purpose |
|-----------|---------|
| BUILDBAT | Build batch programs |
| BUILDONL | Build online (CICS) programs |
| BUILDBMS | Build BMS maps |
| BLDCIDB2 | Build CICS + DB2 programs |

### M2 Runtime Artifacts

| Package | Purpose |
|---------|---------|
| samples/m2/mf/CardDemo_runtime.zip | Micro Focus runtime for AWS M2 |
| samples/m2/unikix/UniKix_CardDemo_runtime_v1.zip | UniKix runtime for AWS M2 |

---

## 14. Scripts (`scripts/`)

| Script | Purpose |
|--------|---------|
| compile_batch.jcl.template | Template for batch compilation JCL |
| local_compile.sh | Compile programs locally |
| remote_compile.sh | Compile programs on remote mainframe |
| remote_refresh.sh | Refresh remote environment |
| remote_submit.sh | Submit JCL to remote mainframe |
| run_full_batch.sh | Execute full batch cycle |
| run_posting.sh | Execute transaction posting |
| run_interest_calc.sh | Execute interest calculation |
| upld_module.sh | Upload compiled modules |
| pad.awk | AWK padding utility |
| git-addSrcVersionInfo.sh | Add version info to source |

---

## 15. Classification Summary

### By Execution Type
| Type | Count |
|------|-------|
| Online (CICS) Programs | 22 |
| Batch Programs | 14 |
| Batch Utility Programs | 2 |
| IMS Utility Programs | 3 |
| Assembler Programs | 2 |
| **Total Programs** | **43** |

### By Functional Domain
| Domain | Programs |
|--------|----------|
| Authentication / Security | COSGN00C |
| Navigation / Menus | COMEN01C, COADM01C |
| Account Management | COACTVWC, COACTUPC, CBACT01C, CBACT03C |
| Card Management | COCRDLIC, COCRDSLC, COCRDUPC, CBACT02C |
| Transaction Processing | COTRN00C, COTRN01C, COTRN02C, CBTRN01C, CBTRN02C |
| Reporting / Statements | CORPT00C, CBTRN03C, CBSTM03A, CBSTM03B |
| Billing | COBIL00C |
| User Administration | COUSR00C–03C |
| Financial Calculation | CBACT04C |
| Customer Data | CBCUS01C |
| Data Migration | CBEXPORT, CBIMPORT |
| Authorization / Fraud (IMS-DB2-MQ) | COPAUA0C, COPAUS0C, COPAUS1C, COPAUS2C, CBPAUP0C, DBUNLDGS, PAUDBLOD, PAUDBUNL |
| Transaction Type Mgmt (DB2) | COTRTLIC, COTRTUPC, COBTUPDT |
| MQ Integration | CODATE01, COACCT01 |
| System Utilities | COBSWAIT, CSUTLDTC, MVSWAIT, COBDATFT |

### By Sub-Application
| Sub-Application | Directory | Technology | Programs | Copybooks | JCL |
|-----------------|-----------|------------|----------|-----------|-----|
| **Base CardDemo** | `app/` | COBOL, CICS, VSAM, JCL | 31 | 30 (+17 BMS-gen) | 38 |
| **Authorization** | `app/app-authorization-ims-db2-mq/` | + IMS DB, DB2, MQ | 8 | 9 | 5 |
| **Transaction Type Mgmt** | `app/app-transaction-type-db2/` | + DB2 | 3 | 2 | 3 |
| **Account Extractions** | `app/app-vsam-mq/` | + MQ, VSAM | 2 | 0 | 0 |
