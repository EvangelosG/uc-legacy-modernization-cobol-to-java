# CardDemo Modernization Blueprint

> **Repository:** `EvangelosG/uc-legacy-modernization-cobol-to-java`
> **Companion Documents:** [DOMAIN_DECOMPOSITION.md](DOMAIN_DECOMPOSITION.md) | [CUTOVER_PLAN.md](CUTOVER_PLAN.md) | [RISK_REGISTER.md](RISK_REGISTER.md)
> **Source Analysis:** [APPLICATION_INVENTORY.md](APPLICATION_INVENTORY.md) | [DEPENDENCY-MAP.md](DEPENDENCY-MAP.md) | [HOTSPOT-ANALYSIS.md](HOTSPOT-ANALYSIS.md) | [DATA-DICTIONARY.md](DATA-DICTIONARY.md)

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Strategy Definitions](#2-strategy-definitions)
3. [Strategy Evaluation by Functional Area](#3-strategy-evaluation-by-functional-area)
   - [3.1 Authentication & Session Management](#31-authentication--session-management)
   - [3.2 Account Management](#32-account-management)
   - [3.3 Card Management](#33-card-management)
   - [3.4 Transaction Processing (Online)](#34-transaction-processing-online)
   - [3.5 Transaction Processing (Batch)](#35-transaction-processing-batch)
   - [3.6 Financial Calculation (Interest)](#36-financial-calculation-interest)
   - [3.7 Billing & Statements](#37-billing--statements)
   - [3.8 Reporting](#38-reporting)
   - [3.9 User Administration](#39-user-administration)
   - [3.10 Authorization & Fraud (IMS-DB2-MQ Sub-App)](#310-authorization--fraud-ims-db2-mq-sub-app)
   - [3.11 Transaction Type Management (DB2 Sub-App)](#311-transaction-type-management-db2-sub-app)
   - [3.12 Account Extractions (VSAM-MQ Sub-App)](#312-account-extractions-vsam-mq-sub-app)
   - [3.13 Data Migration Utilities](#313-data-migration-utilities)
   - [3.14 System Utilities & Infrastructure](#314-system-utilities--infrastructure)
4. [Strategy Summary Matrix](#4-strategy-summary-matrix)
5. [Cross-Cutting Technology Decisions](#5-cross-cutting-technology-decisions)
6. [Recommended Target Architecture](#6-recommended-target-architecture)

---

## 1. Executive Summary

CardDemo is a mainframe credit card management system comprising **43 COBOL programs**, **47+ copybooks**, **46 JCL jobs**, and **17 BMS maps** organized into a base application and three optional sub-applications. The system processes credit card account lifecycles through online (CICS) screens and batch (JCL) jobs, touching **10+ VSAM datasets**, **DB2 tables**, **IMS databases**, and **MQ message queues**.

No single modernization strategy fits all functional areas. This blueprint evaluates four strategies — **Strangler Fig**, **Replatform**, **Refactor**, and **Rewrite** — against each of the 14 functional domains, recommending the approach that optimizes for:

- **Risk minimization** — preserving financial accuracy and data integrity
- **Incremental delivery** — enabling parallel-run validation before cutover
- **Operational continuity** — avoiding big-bang migration scenarios
- **Long-term maintainability** — producing clean, idiomatic Java

**Overall Recommendation:** A **hybrid approach** combining Strangler Fig (for online screens), Refactor (for batch pipelines), and targeted Rewrite (for the most complex programs like COACTUPC). Replatform is reserved for sub-applications with external system dependencies (IMS, MQ) where interim compatibility is needed.

---

## 2. Strategy Definitions

| Strategy | Description | When to Use | Risk Level |
|----------|-------------|-------------|------------|
| **Strangler Fig** | Incrementally replace individual programs/screens behind an API facade or routing layer. Old and new systems run in parallel; traffic shifts gradually. | Online screens with clear boundaries; programs that can be replaced one-at-a-time without disrupting others. | Low–Medium |
| **Replatform** ("Lift & Shift") | Move COBOL programs to a modern runtime (e.g., Micro Focus, UniKix on Linux) with minimal code changes. Retain COBOL logic but run on commodity hardware. | Programs with heavy external system coupling (IMS, MQ) where immediate conversion is impractical; interim step before deeper modernization. | Low |
| **Refactor** | Systematically convert COBOL to Java using automated tooling, then manually optimize the output. Preserves existing logic flow but produces Java code. | Batch programs with clear input/output contracts; programs with moderate complexity where tooling can handle 80%+ of the conversion. | Medium |
| **Rewrite** | Redesign and reimplementing from scratch in Java/Spring using modern patterns (REST APIs, domain-driven design, event-driven batch). | Programs with excessive GO TOs, mixed concerns, or fundamentally incompatible patterns (e.g., COACTUPC at 4,236 LOC with 51 GO TOs). | High |

---

## 3. Strategy Evaluation by Functional Area

### 3.1 Authentication & Session Management

| | |
|---|---|
| **Programs** | COSGN00C (Signon) |
| **Complexity** | Low (260 LOC, 4 IFs, 10 EXEC CICS) |
| **Data** | USRSEC.VSAM.KSDS — read-only for auth |
| **Coupling** | Entry point for all users; routes to COMEN01C or COADM01C |

#### Strategy Evaluation

| Strategy | Fit | Rationale |
|----------|-----|-----------|
| **Strangler Fig** | **Recommended** | Replace with a modern auth layer (Spring Security/JWT). Route authenticated sessions to either legacy CICS or new Java screens during transition. The signon screen is the natural "front door" for the strangler pattern. |
| Replatform | Poor | Retaining COBOL authentication provides no security benefit and blocks modern auth integration (OAuth, LDAP). |
| Refactor | Acceptable | Simple enough to auto-convert, but CICS SEND MAP / RECEIVE MAP commands have no meaningful Java equivalent. The result would still require rewriting the UI layer. |
| Rewrite | Acceptable | Trivially small (260 LOC), so a rewrite is low effort. But the Strangler approach provides the critical benefit of routing traffic between legacy and modern systems. |

**Decision:** **Strangler Fig** — Implement a new authentication microservice as the first component. This becomes the routing layer for the entire strangler migration.

---

### 3.2 Account Management

| | |
|---|---|
| **Programs** | COACTVWC (Account View, 941 LOC), COACTUPC (Account Update, **4,236 LOC**) |
| **Complexity** | COACTVWC: Moderate (9 GO TOs). COACTUPC: **Critical** (51 GO TOs, 168 IFs, 56 copybooks, 5 VSAM datasets) |
| **Data** | ACCTDATA, CARDDATA, CARDXREF, CUSTDATA, USRSEC |
| **Coupling** | COACTUPC touches account + customer records in a single program; highest copybook coupling in the system |

#### Strategy Evaluation

| Strategy | Fit (View) | Fit (Update) | Rationale |
|----------|------------|--------------|-----------|
| **Strangler Fig** | **Recommended** | Partial | Account View is read-only with clear data boundaries — ideal for strangler. Account Update is too complex for simple replacement; needs decomposition first. |
| Replatform | Poor | Poor | No long-term value; these programs don't depend on exotic middleware. |
| Refactor | Acceptable | **Poor** | View can be auto-converted. Update has 51 GO TOs — automated tooling will produce unreadable spaghetti Java. |
| Rewrite | Unnecessary (View) | **Recommended** | Update must be manually rewritten. Decompose into Account Update + Customer Update services. Extract inline SSN/phone/date validation into shared services. |

**Decision:**
- **COACTVWC (View):** **Strangler Fig** — Replace with a read-only Account API + UI component.
- **COACTUPC (Update):** **Rewrite** — Decompose into separate Account and Customer update services. Extract validation logic. This is the single highest-effort program in the system (score 15/15).

---

### 3.3 Card Management

| | |
|---|---|
| **Programs** | COCRDLIC (Card List, 1,459 LOC), COCRDSLC (Card Detail, 887 LOC), COCRDUPC (Card Update, 1,560 LOC) |
| **Complexity** | COCRDLIC: 16 GO TOs, 18 EXEC CICS (highest). COCRDUPC: 21 GO TOs, 8-state change-action machine. |
| **Data** | CARDDATA, CARDXREF, ACCTDATA, CUSTDATA |
| **Coupling** | Sub-screen navigation chain: List → Detail → Update |

#### Strategy Evaluation

| Strategy | Fit | Rationale |
|----------|-----|-----------|
| **Strangler Fig** | **Recommended** (List + Detail) | Read-only screens with CICS browse operations can be replaced by paginated REST APIs. The browse (STARTBR/READNEXT) pattern maps cleanly to database cursor/offset queries. |
| Refactor | Acceptable (Detail) | Card Detail is simple enough for auto-conversion. |
| **Rewrite** | **Recommended** (Update) | The 8-state change-action machine (CCUP-DETAILS-NOT-FETCHED → CCUP-CHANGES-OKAYED-BUT-FAILED) and pessimistic locking pattern require redesign for stateless REST. 21 GO TOs make auto-conversion unreliable. |
| Replatform | Poor | No external middleware dependencies; replatforming adds no value. |

**Decision:**
- **COCRDLIC + COCRDSLC:** **Strangler Fig** — Replace list/detail with paginated REST endpoints.
- **COCRDUPC:** **Rewrite** — Redesign the state machine for stateless web; implement optimistic locking with version stamps.

---

### 3.4 Transaction Processing (Online)

| | |
|---|---|
| **Programs** | COTRN00C (List, 699 LOC), COTRN01C (View, 330 LOC), COTRN02C (Add, 783 LOC) |
| **Complexity** | Low to Moderate (no GO TOs; COTRN02C calls CSUTLDTC for date validation) |
| **Data** | TRANSACT, ACCTDATA, CARDXREF |
| **Coupling** | Linear navigation: List → View → Add |

#### Strategy Evaluation

| Strategy | Fit | Rationale |
|----------|-----|-----------|
| **Strangler Fig** | **Recommended** | Clean, well-structured programs with no GO TOs. List/View are read-only. Add writes to TRANSACT with date validation — straightforward REST endpoint. The entire chain can be replaced incrementally behind the strangler facade. |
| Refactor | Acceptable | Simple enough for automated conversion, but UI layer (BMS) still needs replacement. |
| Rewrite | Unnecessary | Complexity doesn't warrant full rewrite. |
| Replatform | Poor | No external dependencies. |

**Decision:** **Strangler Fig** — Replace all three screens with REST endpoints + modern UI. Convert the CSUTLDTC date validation to a shared Java utility first (Foundation phase).

---

### 3.5 Transaction Processing (Batch)

| | |
|---|---|
| **Programs** | CBTRN01C (File Init, 494 LOC), CBTRN02C (Posting, 731 LOC — **score 14/15**), CBTRN03C (Report, 649 LOC) |
| **Complexity** | CBTRN02C: 48 IFs, 6 VSAM files, 12 I/O ops, financial arithmetic. CBTRN03C: 72 PERFORMs, multi-level break reporting. |
| **Data** | DALYTRAN, TRANSACT, ACCTDATA, XREFFILE, TCATBALF, DALYREJS, DATEPARM |
| **Coupling** | Core daily batch chain: CBTRN01C → CBTRN02C → CBACT04C → CBTRN03C. POSTTRAN JCL orchestrates the pipeline. |

#### Strategy Evaluation

| Strategy | Fit | Rationale |
|----------|-----|-----------|
| **Refactor** | **Recommended** | These are the highest-risk programs but have clean input/output contracts (sequential file processing). No GO TOs. Auto-convert to Java, then manually verify all financial arithmetic (`S9(09)V99` → `BigDecimal`). Spring Batch is the natural target framework. **Parallel-run validation is mandatory**: run COBOL and Java side-by-side and compare outputs record-by-record. |
| Rewrite | Risky | These programs contain the financial logic that determines account balances. Rewriting from scratch introduces regression risk. |
| Strangler Fig | Poor fit | Batch programs don't have user-facing endpoints to strangle. |
| Replatform | Interim option | Could replatform on UniKix/Micro Focus as Phase 0 to decommission mainframe hardware while retaining COBOL logic, then refactor later. |

**Decision:** **Refactor** — Auto-convert to Spring Batch jobs with manual `BigDecimal` arithmetic verification. Run parallel COBOL and Java pipelines for at least 2 billing cycles before cutover. Consider **Replatform** as an interim step if mainframe decommissioning timeline is aggressive.

---

### 3.6 Financial Calculation (Interest)

| | |
|---|---|
| **Programs** | CBACT04C (Interest Calculator, 652 LOC — **score 14/15**) |
| **Complexity** | 43 IFs, 56 PERFORMs, 5 VSAM files, account break-logic, COMPUTE with S9(09)V99 |
| **Data** | ACCTDATA, XREFFILE, DISCGRP, TCATBALF, SYSTRAN |
| **Coupling** | Downstream of CBTRN02C; writes interest transactions to SYSTRAN; REWRITEs account balances |

#### Strategy Evaluation

| Strategy | Fit | Rationale |
|----------|-----|-----------|
| **Refactor** | **Recommended** | No GO TOs, well-structured PERFORM logic. The account break-logic pattern (WS-LAST-ACCT-NUM boundary detection) is a common COBOL idiom that can be mapped to Java grouping operations. Financial arithmetic must use `BigDecimal` with explicit rounding modes matching COBOL COMPUTE semantics. |
| Rewrite | Risky | Interest calculation directly impacts every customer's billing. A from-scratch rewrite introduces rounding-difference risk. |
| Strangler Fig | N/A | Batch program; no UI. |
| Replatform | Interim option | Same rationale as 3.5 — replatform only if mainframe hardware timeline demands it. |

**Decision:** **Refactor** — Convert alongside CBTRN02C as part of the batch pipeline. Test with real disclosure group rate tables. **Parallel-run validation is mandatory** — compare interest amounts to the penny for every account across multiple billing cycles.

---

### 3.7 Billing & Statements

| | |
|---|---|
| **Programs** | COBIL00C (Bill Payment online, 572 LOC), CBSTM03A (Statement Gen batch, LOC varies), CBSTM03B (subroutine) |
| **Complexity** | COBIL00C: Moderate (13 EXEC CICS). CBSTM03A: multi-file read, generates both PS and HTML output. |
| **Data** | TRANSACT, ACCTDATA, CARDXREF, CUSTDATA, STATEMNT.PS, STATEMNT.HTML |
| **Coupling** | CBSTM03A uses SORT step + IDCAMS REPRO as preprocessing |

#### Strategy Evaluation

| Strategy | Fit | Rationale |
|----------|-----|-----------|
| **Strangler Fig** | **Recommended** (COBIL00C) | Online billing screen with browse + write operations. Replace with a REST endpoint that renders in a modern UI. |
| **Refactor** | **Recommended** (CBSTM03A/B) | Batch statement generation has clear file I/O contracts. Convert to Spring Batch job producing PDF output (replacing TXT2PDF1 JCL step). HTML output can be modernized to a templating engine (Thymeleaf). |
| Rewrite | Unnecessary | Neither program has extreme complexity. |
| Replatform | Poor | No exotic middleware. |

**Decision:**
- **COBIL00C:** **Strangler Fig** — Replace online screen with billing API.
- **CBSTM03A/B:** **Refactor** — Convert to Spring Batch; modernize output to PDF (replacing JCL TXT2PDF1) and responsive HTML.

---

### 3.8 Reporting

| | |
|---|---|
| **Programs** | CORPT00C (Report screen, 649 LOC) |
| **Complexity** | Low–Moderate (20 IFs, 1 GO TO, 7 EXEC CICS, calls CSUTLDTC) |
| **Data** | Triggers batch report generation (TRANREPT JCL → CBTRN03C) |
| **Coupling** | Loose — submits batch job and displays confirmation |

#### Strategy Evaluation

| Strategy | Fit | Rationale |
|----------|-----|-----------|
| **Strangler Fig** | **Recommended** | Simple online screen that submits a batch job. Replace with a report-request API that triggers the modernized Spring Batch report job. Add async status tracking. |
| Refactor | Acceptable | Simple enough, but BMS UI still needs replacement. |
| Rewrite | Unnecessary | Low complexity. |
| Replatform | Poor | No external dependencies. |

**Decision:** **Strangler Fig** — Replace with an async report-request API. Wire to the refactored CBTRN03C Spring Batch job.

---

### 3.9 User Administration

| | |
|---|---|
| **Programs** | COUSR00C (List, 695 LOC), COUSR01C (Add, 299 LOC), COUSR02C (Update, 414 LOC), COUSR03C (Delete, 359 LOC) |
| **Complexity** | Low (all score 5–6/15; no GO TOs except minor) |
| **Data** | USRSEC.VSAM.KSDS |
| **Coupling** | Self-contained CRUD on user security records; accessed only from Admin Menu |

#### Strategy Evaluation

| Strategy | Fit | Rationale |
|----------|-----|-----------|
| **Strangler Fig** | **Recommended** | Textbook CRUD operations on a single dataset. Replace with a User Management REST API + admin UI. The simple structure makes these ideal early candidates for the strangler pattern — low risk, high confidence, and establishes patterns for later conversions. |
| Refactor | Acceptable | Auto-conversion would work, but CICS BMS interaction still needs rewriting. |
| Rewrite | Unnecessary | Too simple to warrant from-scratch development. |
| Replatform | Poor | No value in keeping COBOL for basic CRUD. |

**Decision:** **Strangler Fig** — Replace with User Management CRUD API (Spring Data JPA). Migrate USRSEC data to a relational user table with password hashing (the COBOL system stores passwords in plaintext PIC X fields).

---

### 3.10 Authorization & Fraud (IMS-DB2-MQ Sub-App)

| | |
|---|---|
| **Programs** | COPAUA0C (Auth Processor, 1,026 LOC — MQ), COPAUS0C (Summary, 1,032 LOC), COPAUS1C (Detail, 604 LOC), COPAUS2C (Fraud Check, 244 LOC — DB2), CBPAUP0C (Batch Purge, 386 LOC) |
| **IMS Programs** | PAUDBLOD, PAUDBUNL, DBUNLDGS (IMS DLI database load/unload/restructure) |
| **Complexity** | COPAUA0C: High (MQ API calls, real-time latency requirements). DB2 + IMS + MQ triple integration. |
| **Data** | IMS databases (auth summary/detail segments), DB2 AUTHFRDS table, MQ queues, VSAM files |
| **Coupling** | External system: MQ for auth requests, IMS for persistence, DB2 for fraud logging |

#### Strategy Evaluation

| Strategy | Fit | Rationale |
|----------|-----|-----------|
| **Replatform** | **Recommended** (Phase 1) | The IMS + MQ + DB2 triple integration makes immediate conversion impractical. Replatform the entire sub-app on Micro Focus / UniKix to run on Linux while preserving COBOL logic. This decommissions mainframe hardware without touching the complex integration code. |
| **Rewrite** | **Recommended** (Phase 2) | After replatforming, rewrite as an event-driven Authorization microservice using Spring Boot + Kafka/RabbitMQ (replacing MQ) + PostgreSQL (replacing IMS/DB2). Modern fraud detection can leverage ML models rather than rule-based checks. |
| Strangler Fig | Partial | The display screens (COPAUS0C, COPAUS1C) can be strangled behind a read API, but the core auth processor (COPAUA0C) must be migrated as a unit. |
| Refactor | Poor | IMS DLI calls and MQ API calls have no direct Java equivalents for auto-conversion. |

**Decision:** **Replatform** (interim) → **Rewrite** (target). Replatform to decommission mainframe hardware. Then design a new Authorization microservice with modern messaging (Kafka/RabbitMQ) and relational persistence (PostgreSQL). The IMS programs (PAUDBLOD, PAUDBUNL, DBUNLDGS) are eliminated entirely — their function is absorbed by JPA repositories.

---

### 3.11 Transaction Type Management (DB2 Sub-App)

| | |
|---|---|
| **Programs** | COTRTLIC (List, 2,098 LOC — **score 12/15**), COTRTUPC (Update, 1,702 LOC — **score 12/15**), COBTUPDT (Batch, 237 LOC) |
| **Complexity** | COTRTLIC: 28 GO TOs, 16 SQL, EVALUATE-only (zero IFs). COTRTUPC: 23 GO TOs, 7 SQL, 21 VSAM I/O. |
| **Data** | DB2 TR_TYPE and TR_TYPE_CATEGORY tables + VSAM files |
| **Coupling** | Dual-technology: CICS + DB2 with VSAM shadow data |

#### Strategy Evaluation

| Strategy | Fit | Rationale |
|----------|-----|-----------|
| **Rewrite** | **Recommended** | The EVALUATE-only control flow, 28+ GO TOs, and dual CICS+DB2 technology make auto-conversion unreliable. The underlying business logic is simple reference data CRUD, but it's expressed in 3,800 lines of complex COBOL. A Spring Data JPA + REST API rewrite would be ~300 lines of Java and far easier to maintain. |
| Refactor | Poor | EVALUATE-only pattern (zero IFs) is an uncommon idiom that conversion tools may mishandle. GO TO-heavy flow crossing DB2 error handling and CICS screen management is extremely error-prone for automated conversion. |
| Strangler Fig | Partial | Could strangle the UI, but the DB2 cursor management needs replacement regardless. |
| Replatform | Interim option | If DB2 access from Java is initially complex, replatform the COBOL to run DB2 queries from a Micro Focus runtime. |

**Decision:** **Rewrite** — Redesign as a Transaction Type microservice with Spring Data JPA + REST API. The VSAM shadow copies of DB2 data are eliminated — use the relational database as the single source of truth. COBTUPDT batch logic is absorbed into the same service as a scheduled job.

---

### 3.12 Account Extractions (VSAM-MQ Sub-App)

| | |
|---|---|
| **Programs** | CODATE01 (Date Inquiry, 524 LOC), COACCT01 (Account Inquiry, 620 LOC) |
| **Complexity** | Low (scores 4/15 each). MQ request/response for simple inquiries. |
| **Data** | VSAM + MQ queues |
| **Coupling** | MQ integration for system date and account data inquiries |

#### Strategy Evaluation

| Strategy | Fit | Rationale |
|----------|-----|-----------|
| **Rewrite** | **Recommended** | These are simple inquiry programs wrapped in MQ request/response boilerplate. In a modern architecture, they're trivial REST endpoints (~50 lines each). The MQ overhead is eliminated entirely. |
| Replatform | Unnecessary | The programs are too simple to justify replatforming infrastructure. |
| Refactor | Poor | MQ API calls don't auto-convert. |
| Strangler Fig | N/A | No legacy UI to strangle — these are backend services. |

**Decision:** **Rewrite** — Replace with REST endpoints in the Account Management service. The MQ queues are decommissioned.

---

### 3.13 Data Migration Utilities

| | |
|---|---|
| **Programs** | CBEXPORT (582 LOC), CBIMPORT (487 LOC) |
| **Complexity** | Moderate (REDEFINES overlay for multi-entity sequential file format) |
| **Data** | All 5 core VSAM files ↔ EXPORT.DATA sequential file |
| **Coupling** | Reads/writes every core dataset |

#### Strategy Evaluation

| Strategy | Fit | Rationale |
|----------|-----|-----------|
| **Rewrite** | **Recommended** | In the target architecture, data migration uses standard database tools (pg_dump/pg_restore, Flyway, Liquibase) rather than custom COBOL programs. The CVEXPORT REDEFINES overlay (multiple entity types in one sequential file) has no Java equivalent worth preserving. Write new migration tools using JDBC/JPA or ETL tooling. |
| Refactor | Poor | REDEFINES overlays are fundamentally incompatible with Java type safety. |
| Strangler Fig | N/A | Utility programs, no UI. |
| Replatform | Unnecessary | These are migration tools; they're only needed during the transition and can be written fresh. |

**Decision:** **Rewrite** — Build new data migration tooling using modern ETL patterns. These are needed only during the VSAM → relational database migration and can be purpose-built.

---

### 3.14 System Utilities & Infrastructure

| | |
|---|---|
| **Programs** | CSUTLDTC (Date Utility, 157 LOC), COBDATFT (Assembler date formatting), COBSWAIT (Wait, 41 LOC), MVSWAIT (Assembler) |
| **Copybooks** | CODATECN, CSDAT01Y, CSUTLDWY, CSLKPCDY (1,318-line lookup table), CSSTRPFY |
| **JCL Infra** | OPENFIL, CLOSEFIL, DEFGDGB, DEFGDGD, etc. |
| **Complexity** | Low (utility programs are small and well-defined) |
| **Coupling** | CSUTLDTC called by CORPT00C and COTRN02C. CSLKPCDY embedded in COACTUPC. |

#### Strategy Evaluation

| Strategy | Fit | Rationale |
|----------|-----|-----------|
| **Rewrite** | **Recommended** | Utility functions map directly to Java standard library equivalents: `java.time` replaces CEEDAYS/date conversion; `java.util.concurrent` replaces MVSWAIT; lookup tables become enum classes or database reference data. JCL infrastructure jobs (OPENFIL, CLOSEFIL, IDCAMS) are eliminated entirely — their function is handled by the application server and database. |
| Refactor | Unnecessary | Too small and too different in paradigm. |
| Strangler Fig | N/A | No UI. |
| Replatform | N/A | Assembler programs can't be replatformed to Java. |

**Decision:** **Rewrite** — Implement as shared Java utility classes. These are the first components to build (Foundation phase) because many programs depend on them.

---

## 4. Strategy Summary Matrix

| Functional Area | Programs | LOC | Hotspot Score | Strategy | Effort | Risk |
|----------------|----------|-----|---------------|----------|--------|------|
| Authentication | COSGN00C | 260 | 7 | Strangler Fig | Low | Low |
| Account View | COACTVWC | 941 | 9 | Strangler Fig | Medium | Low |
| Account Update | COACTUPC | 4,236 | **15** | **Rewrite** | **Very High** | **High** |
| Card List/Detail | COCRDLIC, COCRDSLC | 2,346 | 11, 8 | Strangler Fig | Medium | Low |
| Card Update | COCRDUPC | 1,560 | 11 | Rewrite | High | Medium |
| Transaction (Online) | COTRN00C/01C/02C | 1,812 | 8, 5, 9 | Strangler Fig | Medium | Low |
| Transaction (Batch) | CBTRN01C/02C/03C | 1,874 | 11, **14**, 11 | Refactor | High | **High** |
| Interest Calculation | CBACT04C | 652 | **14** | Refactor | High | **High** |
| Billing (Online) | COBIL00C | 572 | 9 | Strangler Fig | Medium | Low |
| Statements (Batch) | CBSTM03A/B | ~600 | 8 | Refactor | Medium | Medium |
| Reporting | CORPT00C | 649 | 8 | Strangler Fig | Low | Low |
| User Admin | COUSR00C–03C | 1,767 | 5–6 | Strangler Fig | Low | Low |
| Auth & Fraud | COPAUA0C + 4 more + 3 IMS | ~3,300 | 8–10 | Replatform → Rewrite | Very High | High |
| Tx Type Mgmt (DB2) | COTRTLIC, COTRTUPC, COBTUPDT | 4,037 | **12**, **12**, 6 | Rewrite | High | Medium |
| Account Extractions | CODATE01, COACCT01 | 1,144 | 4 | Rewrite | Low | Low |
| Data Migration | CBEXPORT, CBIMPORT | 1,069 | 8 | Rewrite | Medium | Low |
| Utilities | CSUTLDTC, COBSWAIT, etc. | ~200 | 3–5 | Rewrite | Low | Low |

### Strategy Distribution

| Strategy | Programs | % of Total LOC | Risk Profile |
|----------|----------|---------------|-------------|
| **Strangler Fig** | 14 online programs | ~35% | Low — incremental, parallel-run capable |
| **Refactor** | 5 batch programs | ~20% | High — financial logic, requires parallel validation |
| **Rewrite** | 12 programs (complex online + utilities + sub-apps) | ~35% | Medium to High — design from scratch |
| **Replatform** | 8 programs (auth sub-app, interim) | ~10% | Low — preserve existing logic temporarily |

---

## 5. Cross-Cutting Technology Decisions

These decisions apply across all functional areas regardless of modernization strategy.

### 5.1 Data Layer: VSAM → Relational Database

| COBOL Construct | Java Target | Notes |
|----------------|-------------|-------|
| VSAM KSDS | PostgreSQL tables with primary keys | Preserve key structures (e.g., ACCT-ID PIC 9(11) → BIGINT) |
| VSAM AIX (Alternate Index) | Secondary indexes | CARDXREF AIX for account-based lookups |
| VSAM ESDS/RRDS | Standard tables | Used only for USRSEC alternate structures |
| CICS file I/O (READ/WRITE/REWRITE) | Spring Data JPA repositories | STARTBR/READNEXT → paginated queries |
| Batch file I/O (OPEN/READ/WRITE) | Spring Batch `ItemReader`/`ItemWriter` | Sequential access pattern preserved |
| GDG (Generation Data Groups) | Timestamped backup tables or S3 versioning | TRANSACT.BKUP, TRANREPT |

### 5.2 Financial Arithmetic

| COBOL Pattern | Java Equivalent | Validation |
|--------------|----------------|------------|
| `PIC S9(10)V99` | `BigDecimal(12, 2)` | Compare every computed value during parallel run |
| `COMPUTE ... ROUNDED` | `BigDecimal.setScale(2, RoundingMode.HALF_EVEN)` | Verify rounding mode matches COBOL default |
| `ON SIZE ERROR` | Exception handling + overflow checks | Must reproduce COBOL truncation semantics |

### 5.3 COMMAREA → Session/Request Context

The COCOM01Y COMMAREA (used by 21 programs) manages pseudo-conversational state. In Java:

| COMMAREA Field | Java Equivalent |
|---------------|----------------|
| `CDEMO-FROM-PROGRAM` | Request routing metadata (Spring MVC handler) |
| `CDEMO-TO-PROGRAM` | Navigation state (React Router / controller dispatch) |
| `CDEMO-PGM-REENTER` | Not needed (stateless REST) |
| `CDEMO-ACCT-ID`, `CDEMO-CARD-NUM` | Request parameters or session-scoped DTO |
| `CDEMO-LAST-MAP` / `CDEMO-LAST-MAPSET` | Not needed (SPA state management) |

### 5.4 88-Level Condition Names → Enums

```
// COBOL                          // Java
05 CDEMO-USR-TYPE     PIC X.      public enum UserType {
   88 CDEMO-USR-ADMIN VALUE 'A'.      ADMIN('A'),
   88 CDEMO-USR-USER  VALUE 'U'.      REGULAR('U');
                                      // ...
                                  }
```

### 5.5 Batch Orchestration: JCL → Spring Batch

| JCL Pattern | Spring Batch Equivalent |
|------------|------------------------|
| JCL JOB/STEP | `Job` with `Step` definitions |
| SORT utility | `ORDER BY` in SQL query, or Java Comparator in `ItemProcessor` |
| IDCAMS REPRO | Database-to-database copy via `ItemReader` → `ItemWriter` |
| COND/IF-THEN | Spring Batch `FlowBuilder` with conditional transitions |
| GDG | Timestamped output directories or database partitions |
| DD statements | Spring Batch `@Value` configuration properties |
| OPENFIL/CLOSEFIL | Not needed — database handles concurrent access |

---

## 6. Recommended Target Architecture

```
                          ┌─────────────────────────────────────┐
                          │          API Gateway / BFF           │
                          │     (Spring Cloud Gateway)          │
                          └──────────┬──────────────────────────┘
                                     │
        ┌────────────────────────────┼────────────────────────────┐
        │                            │                            │
  ┌─────▼──────┐            ┌───────▼───────┐           ┌───────▼───────┐
  │   Auth     │            │   Account     │           │    Card       │
  │  Service   │            │   Service     │           │   Service     │
  │ (Spring    │            │ (Acct CRUD +  │           │ (Card CRUD +  │
  │  Security) │            │  Customer)    │           │  Xref)        │
  └─────┬──────┘            └───────┬───────┘           └───────┬───────┘
        │                           │                           │
        │                  ┌────────┼────────┐                  │
        │                  │                 │                  │
  ┌─────▼──────┐    ┌──────▼──────┐  ┌──────▼──────┐  ┌───────▼───────┐
  │Transaction │    │  Billing    │  │  Reporting  │  │  User Admin   │
  │  Service   │    │  Service    │  │  Service    │  │   Service     │
  │ (Txn CRUD) │    │ (Payments + │  │ (Async      │  │ (CRUD +       │
  │            │    │  Statements)│  │  Reports)   │  │  Security)    │
  └────────────┘    └─────────────┘  └─────────────┘  └───────────────┘

  ┌──────────────────────────────────────────────────────────────────────┐
  │                    Batch Processing Layer                            │
  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │
  │  │ Transaction  │  │  Interest    │  │  Statement   │              │
  │  │ Posting Job  │  │  Calc Job    │  │  Gen Job     │              │
  │  │ (Spring      │  │ (BigDecimal  │  │ (PDF/HTML    │              │
  │  │  Batch)      │  │  arithmetic) │  │  output)     │              │
  │  └──────────────┘  └──────────────┘  └──────────────┘              │
  └──────────────────────────────────────────────────────────────────────┘

  ┌──────────────────────────────────────────────────────────────────────┐
  │                     Data Layer                                       │
  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │
  │  │  PostgreSQL  │  │   Redis      │  │  Kafka /     │              │
  │  │  (Accounts,  │  │  (Session,   │  │  RabbitMQ    │              │
  │  │   Cards,     │  │   Cache)     │  │  (Events,    │              │
  │  │   Customers, │  │              │  │   Auth Msgs) │              │
  │  │   Txns,      │  │              │  │              │              │
  │  │   Users)     │  │              │  │              │              │
  │  └──────────────┘  └──────────────┘  └──────────────┘              │
  └──────────────────────────────────────────────────────────────────────┘
```

### Key Architecture Principles

1. **Domain-Aligned Services** — Each service maps to a bounded context identified in [DOMAIN_DECOMPOSITION.md](DOMAIN_DECOMPOSITION.md)
2. **Shared Nothing** — Services communicate via REST APIs and events, not shared databases
3. **Financial Accuracy** — All monetary calculations use `BigDecimal` with explicit rounding modes
4. **Parallel-Run Capable** — During transition, both COBOL and Java systems process the same inputs; outputs are compared automatically
5. **Audit Trail** — Event sourcing for all financial mutations, replacing COBOL's implicit audit-by-file-access

---

*Cross-references: [DOMAIN_DECOMPOSITION.md](DOMAIN_DECOMPOSITION.md) | [CUTOVER_PLAN.md](CUTOVER_PLAN.md) | [RISK_REGISTER.md](RISK_REGISTER.md)*
