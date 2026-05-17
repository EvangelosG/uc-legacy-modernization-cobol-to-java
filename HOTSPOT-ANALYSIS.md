# CardDemo Hotspot Analysis

Prioritized module assessment by **complexity**, **risk**, and **business impact** to guide modernization sequencing.

---

## Table of Contents

1. [Scoring Methodology](#scoring-methodology)
2. [Priority Tier 1 — Critical Hotspots](#priority-tier-1--critical-hotspots)
3. [Priority Tier 2 — High Hotspots](#priority-tier-2--high-hotspots)
4. [Priority Tier 3 — Moderate Hotspots](#priority-tier-3--moderate-hotspots)
5. [Priority Tier 4 — Low Hotspots](#priority-tier-4--low-hotspots)
6. [Complexity Metrics Summary](#complexity-metrics-summary)
7. [Shared Copybook Coupling Matrix](#shared-copybook-coupling-matrix)
8. [Program Call Graph](#program-call-graph)
9. [VSAM File Coupling](#vsam-file-coupling)
10. [Modernization Sequencing Recommendations](#modernization-sequencing-recommendations)

---

## Scoring Methodology

Each program is scored on three axes (1–5 scale each, 15 max combined):

| Axis | What It Measures | Scoring Criteria |
|------|-----------------|------------------|
| **Complexity** | Structural difficulty of understanding and converting the code | LOC, IF/EVALUATE branches, PERFORM count, GO TO usage, nested control flow, REDEFINES, 88-level flags, CICS command count |
| **Risk** | Likelihood of defects or regressions during modernization | Shared copybook coupling, VSAM I/O operations, cross-program calls (XCTL/LINK/CALL), external system integration (MQ/DB2/IMS), GO TO spaghetti flow, financial calculations |
| **Business Impact** | Criticality to daily operations and revenue | Core transaction path, customer-facing, batch-critical (nightly processing), financial accuracy, regulatory (audit/fraud), data integrity |

**Combined Score** = Complexity + Risk + Business Impact (out of 15).

---

## Priority Tier 1 — Critical Hotspots

> Combined score ≥ 12. These modules require the most careful modernization planning, the deepest testing, and should be tackled by senior engineers.

### 1. COACTUPC — Account Update (Online)

| Axis | Score | Rationale |
|------|-------|-----------|
| Complexity | **5** | Largest program in the system at **4,236 LOC**. 168 IF statements, 10 EVALUATEs, 64 PERFORMs, **51 GO TOs**, 73 paragraphs. Includes inline SSN validation, phone number parsing, date validation, and extensive 88-level flag logic. 56 COPY statements — highest copybook coupling of any program. Uses CSUTLDPY (procedure-division COPY) making control flow hard to trace. |
| Risk | **5** | Touches **5 VSAM datasets** via CICS (Account, Card Xref, Customer, Card, User Security). 17 EXEC CICS commands. GO TO-driven flow creates spaghetti control paths that are extremely error-prone to refactor. Includes CSLKPCDY (1,318-line lookup table) inline. Manages both account AND customer records in a single program — high blast radius. |
| Business Impact | **5** | Primary account modification screen. Changes to credit limits, balances, customer PII (SSN, address, phone). Any defect directly corrupts financial records and customer data. |

**Key Concerns:**
- 51 GO TOs make automated control-flow conversion unreliable — manual rewrite likely required
- Mixed account + customer update in single program violates separation of concerns
- Inline validation logic (SSN, phone, date) should be extracted to shared services
- REDEFINES overlays on phone numbers and SSN require careful type mapping

---

### 2. CBTRN02C — Daily Transaction Posting (Batch)

| Axis | Score | Rationale |
|------|-------|-----------|
| Complexity | **4** | 731 LOC, 48 IFs, 61 PERFORMs, 15 paragraphs. Reads daily transaction file, validates each record, posts to transaction master, updates account balances, and writes rejects. Touches 6 VSAM files with 12 I/O operations. |
| Risk | **5** | Core daily batch job — **the central nervous system of nightly processing**. Reads DALYTRAN, writes TRANFILE, updates ACCTFILE and TCATBALF, cross-references via XREFFILE, and logs rejects to DALYREJS. A bug here silently corrupts account balances for every customer. Financial arithmetic on `S9(09)V99` fields. |
| Business Impact | **5** | Called by POSTTRAN JCL — the most critical batch job in the system. Processes every transaction from the day. Failure means no transactions post, balances don't update, and the entire daily cycle stalls. Feeds downstream interest calculation (CBACT04C). |

**Key Concerns:**
- Must preserve exact COBOL decimal arithmetic (no floating-point drift)
- Reject-file logic must be regression-tested exhaustively
- File status checking pattern is repetitive but must be faithfully preserved
- TRAN-CAT-BAL update creates/rewrites records conditionally — tricky state machine

---

### 3. CBACT04C — Interest Calculation (Batch)

| Axis | Score | Rationale |
|------|-------|-----------|
| Complexity | **4** | 652 LOC, 43 IFs, 56 PERFORMs. Iterates TCATBAL sequentially, looks up disclosure group interest rates, computes interest per category, accumulates per-account totals, then updates account master. Break-logic on account boundaries. |
| Risk | **5** | Financial calculation engine — computes interest charges applied to every account. Reads 5 VSAM files, REWRITEs account records with new balances. Rounding errors or rate-lookup failures directly impact billing accuracy. Uses LINKAGE SECTION with external date parameter from JCL. |
| Business Impact | **5** | Determines how much interest every customer is charged. Regulatory and financial accuracy requirements. Downstream of CBTRN02C — if transaction posting is wrong, interest calculation compounds the error. Any defect = incorrect billing for all customers. |

**Key Concerns:**
- `COMPUTE` statements with `S9(09)V99` must preserve COBOL truncation/rounding semantics
- Account break-logic (WS-LAST-ACCT-NUM boundary detection) is a common COBOL pattern that maps poorly to Java streams/iterators
- Disclosure group lookup drives rate selection — must be regression-tested with real rate tables
- REWRITE of account records means this batch job directly mutates production data

---

### 4. COTRTLIC — Transaction Type List (Online, DB2)

| Axis | Score | Rationale |
|------|-------|-----------|
| Complexity | **5** | 2,098 LOC — second largest program. 16 EVALUATEs, 63 PERFORMs, **28 GO TOs**. Zero IF statements (uses EVALUATE exclusively) — unusual pattern requiring careful mapping. 16 EXEC SQL statements with DB2 cursor management (DECLARE, OPEN, FETCH, CLOSE). 12 EXEC CICS commands. |
| Risk | **4** | Dual-technology program: CICS + DB2. Cursor-based paging with dynamic SQL filter construction. GO TO-based flow between DB2 error handling and CICS screen management. 4 VSAM SELECTs alongside DB2 access. Sub-application boundary — different deployment and testing requirements. |
| Business Impact | **3** | Transaction type reference data management. Admin-only screen. Not on the critical transaction path but controls the lookup tables that CBTRN02C and CBTRN03C depend on. Incorrect type codes cascade to transaction validation failures. |

**Key Concerns:**
- EVALUATE-only control flow (no IFs) is an uncommon pattern — verify conversion tool handles it
- DB2 cursor lifecycle (OPEN → FETCH loop → CLOSE) must map to JDBC ResultSet or JPA correctly
- Dynamic SQL filter construction via string concatenation — SQL injection risk in modernized code if not handled
- GO TO targets cross DB2 and CICS error-handling boundaries

---

### 5. COTRTUPC — Transaction Type Update (Online, DB2)

| Axis | Score | Rationale |
|------|-------|-----------|
| Complexity | **5** | 1,702 LOC. 26 EVALUATEs (zero IFs — same unusual pattern as COTRTLIC), 40 PERFORMs, **23 GO TOs**. 12 EXEC CICS, 7 EXEC SQL. 21 VSAM I/O operations — highest of any online program. 13 copybook dependencies. |
| Risk | **4** | Same dual-technology risk as COTRTLIC. REWRITE/DELETE operations on DB2 tables. 21 VSAM I/O ops in an online program is unusually high and suggests complex data synchronization between VSAM and DB2. |
| Business Impact | **3** | Admin-only transaction type maintenance. Modifies the reference data that drives transaction categorization and interest rate lookups. Changes here affect all downstream batch processing. |

**Key Concerns:**
- 21 VSAM I/O operations in an online program suggests it may be doing batch-style work interactively
- DB2 UPDATE/DELETE combined with VSAM REWRITE creates two-phase commit concerns
- Same EVALUATE-only pattern as COTRTLIC — test conversion tooling on this pattern first

---

## Priority Tier 2 — High Hotspots

> Combined score 10–11. Significant complexity or risk but slightly lower blast radius.

### 6. COCRDUPC — Card Update (Online)

| Axis | Score | Rationale |
|------|-------|-----------|
| Complexity | **4** | 1,560 LOC. 74 IFs, 16 EVALUATEs, **21 GO TOs**, 39 paragraphs. Complex state machine (CCUP-CHANGE-ACTION with 8 states). Manages old-vs-new card details comparison, lock-for-update, and confirmation flow. |
| Risk | **4** | 12 EXEC CICS commands, 3 CICS READ operations. Cross-references card, account, customer, and xref files. Pessimistic locking pattern (read-for-update, then rewrite) must be faithfully reproduced. |
| Business Impact | **3** | Card detail modification screen. Updates card status, expiry, embossed name. Incorrect updates could deactivate valid cards or activate expired ones. Customer-facing impact but limited to card-level changes. |

**Key Concerns:**
- 8-state change-action machine (CCUP-DETAILS-NOT-FETCHED through CCUP-CHANGES-OKAYED-BUT-FAILED) is hard to map to REST/stateless patterns
- Pessimistic lock → compare → rewrite pattern has no direct equivalent in stateless web APIs
- GO TO-based error recovery paths

---

### 7. COCRDLIC — Card List (Online)

| Axis | Score | Rationale |
|------|-------|-----------|
| Complexity | **4** | 1,459 LOC. 61 IFs, 18 EVALUATEs, **16 GO TOs**, 37 paragraphs. CICS browse operations (STARTBR/READNEXT/ENDBR) for paginated card listing. Manages screen-level pagination state across pseudo-conversational boundaries. |
| Risk | **4** | 18 EXEC CICS commands — highest CICS coupling of any program. 2 VSAM SELECTs with STARTBR pagination. Cross-references card and account data. CALLs to 3 external programs. |
| Business Impact | **3** | Card search/list screen. Gateway to card update (COCRDUPC) and card detail (COCRDSLC). Read-only but critical for account servicing workflows. |

**Key Concerns:**
- CICS STARTBR/READNEXT/ENDBR pagination must map to database cursor or offset/limit queries
- Pseudo-conversational state management across RETURN/XCTL boundaries
- 3 external CALL dependencies

---

### 8. COPAUA0C — Authorization Processor (Online, MQ)

| Axis | Score | Rationale |
|------|-------|-----------|
| Complexity | **4** | 1,026 LOC. 26 IFs, 10 EVALUATEs, 38 PERFORMs, 41 paragraphs. **MQ integration** — CALLs to MQOPEN, MQGET, MQPUT1, MQCLOSE. Processes authorization requests and generates responses. 16 copybooks. |
| Risk | **5** | **Only program with MQ integration** — external system coupling. 12 EXEC CICS, 8 CALLs (4 to MQ APIs). Reads account, customer, and xref data to make authorization decisions. Writes to AUTHFRDS via error log. Real-time authorization path — latency-sensitive. |
| Business Impact | **3** | Authorization request processing. Not in the core base CardDemo path (sub-application) but critical for any deployment that includes the authorization module. Fraud detection dependency. |

**Key Concerns:**
- MQ API calls (MQOPEN/MQGET/MQPUT1/MQCLOSE) require message broker integration in target platform
- Real-time authorization decisions — latency requirements may conflict with modernized architecture
- Error logging to CCPAUERY must be preserved for audit trail

---

### 9. CBTRN03C — Daily Transaction Report (Batch)

| Axis | Score | Rationale |
|------|-------|-----------|
| Complexity | **4** | 649 LOC. 38 IFs, 4 EVALUATEs, 72 PERFORMs (highest PERFORM count of any batch program), 18 paragraphs. Report generation with headers, detail lines, page breaks, and totals. Multiple break levels (date, card, account). |
| Risk | **3** | 6 VSAM files, 10 I/O operations. Reads transaction types and categories for description lookups. Report formatting with PIC-edited output fields. Sequential file output. |
| Business Impact | **4** | Daily transaction report — operational visibility into all posted transactions. Used for reconciliation and audit. Incorrect totals or missing transactions undermine financial controls. |

**Key Concerns:**
- Multi-level break reporting (date → card → account) is a classic COBOL pattern
- PIC-edited output fields (report lines) must produce identical formatting
- CVTRA07Y copybook defines header/detail/total line layouts — verify field alignment

---

### 10. CBTRN01C — Transaction File Initialization (Batch)

| Axis | Score | Rationale |
|------|-------|-----------|
| Complexity | **3** | 494 LOC. 33 IFs, 42 PERFORMs. Generates initial transaction records with sequenced IDs. Reads multiple reference files. |
| Risk | **4** | 6 VSAM files, 6 I/O operations. Creates the transaction master file that CBTRN02C posts to. Incorrect initialization means the transaction file is corrupt before posting begins. |
| Business Impact | **4** | Upstream of the entire transaction processing chain. Generates transaction IDs and initial records. If this fails, CBTRN02C has nothing to post. |

**Key Concerns:**
- Transaction ID generation (sequential, unique) must be preserved exactly
- Feeds directly into CBTRN02C — test as a pipeline

---

## Priority Tier 3 — Moderate Hotspots

> Combined score 7–9. Moderate complexity or focused scope limiting blast radius.

### 11. COPAUS0C — Auth Summary Display (Online)

| Axis | Score | Rationale |
|------|-------|-----------|
| Complexity | **3** | 1,032 LOC. 25 IFs, 11 EVALUATEs, 46 PERFORMs. |
| Risk | **3** | 10 EXEC CICS. 14 copybooks. Read-only VSAM access. |
| Business Impact | **2** | Auth summary view — sub-application, read-only. |

### 12. COPAUS1C — Auth Detail Display (Online)

| Axis | Score | Rationale |
|------|-------|-----------|
| Complexity | **3** | 604 LOC. 17 IFs, 5 EVALUATEs, 34 PERFORMs. |
| Risk | **3** | 8 EXEC CICS. 10 copybooks. |
| Business Impact | **2** | Auth detail view — sub-application, read-only. |

### 13. COACTVWC — Account View (Online)

| Axis | Score | Rationale |
|------|-------|-----------|
| Complexity | **3** | 941 LOC. 29 IFs, 10 EVALUATEs, 21 PERFORMs, 9 GO TOs. |
| Risk | **3** | 15 EXEC CICS, 3 CICS READ operations. Read-only account display. |
| Business Impact | **3** | Account detail view screen. Read-only but customer-facing. |

### 14. COCRDSLC — Card Detail/Select (Online)

| Axis | Score | Rationale |
|------|-------|-----------|
| Complexity | **3** | 887 LOC. 33 IFs, 8 EVALUATEs, 19 PERFORMs, 9 GO TOs. |
| Risk | **3** | 14 EXEC CICS. Card browse with STARTBR/READNEXT. |
| Business Impact | **2** | Card selection screen — navigation intermediary. |

### 15. COTRN02C — Transaction Add (Online)

| Axis | Score | Rationale |
|------|-------|-----------|
| Complexity | **3** | 783 LOC. 14 IFs, 13 EVALUATEs, 61 PERFORMs. |
| Risk | **3** | 11 EXEC CICS. WRITE to transaction file. CALLs CSUTLDTC for date validation. |
| Business Impact | **3** | Manual transaction entry screen. Writes new transactions. |

### 16. COTRN00C — Transaction List (Online)

| Axis | Score | Rationale |
|------|-------|-----------|
| Complexity | **3** | 699 LOC. 26 IFs, 8 EVALUATEs, 43 PERFORMs. |
| Risk | **2** | 10 EXEC CICS. STARTBR-based browsing. Read-only. |
| Business Impact | **3** | Transaction search/list screen. Read-only inquiry. |

### 17. COUSR00C — User List (Online)

| Axis | Score | Rationale |
|------|-------|-----------|
| Complexity | **3** | 695 LOC. 25 IFs, 8 EVALUATEs, 41 PERFORMs. |
| Risk | **2** | 11 EXEC CICS. STARTBR-based user browsing. |
| Business Impact | **3** | User administration list. Admin-only. |

### 18. CBEXPORT — Data Export (Batch)

| Axis | Score | Rationale |
|------|-------|-----------|
| Complexity | **3** | 582 LOC. 16 IFs, 45 PERFORMs, 20 paragraphs. |
| Risk | **3** | 6 VSAM files, 10 I/O operations. CVEXPORT REDEFINES overlay — writes multiple entity types into a single sequential file. |
| Business Impact | **2** | Data migration utility. Used for branch-to-branch transfers. Not on critical daily path. |

### 19. CBIMPORT — Data Import (Batch)

| Axis | Score | Rationale |
|------|-------|-----------|
| Complexity | **3** | 487 LOC. 14 IFs, 1 EVALUATE, 29 PERFORMs. |
| Risk | **3** | 7 VSAM files, 8 I/O operations. EVALUATE on record type to route to correct VSAM output file. CVEXPORT REDEFINES overlay for reading multi-entity input. |
| Business Impact | **2** | Data import utility. Inverse of CBEXPORT. Not on critical daily path. |

### 20. COBIL00C — Billing/Statement (Online)

| Axis | Score | Rationale |
|------|-------|-----------|
| Complexity | **3** | 572 LOC. 10 IFs, 9 EVALUATEs, 38 PERFORMs. |
| Risk | **3** | 13 EXEC CICS. STARTBR/WRITE operations. Transaction browsing with account filtering. |
| Business Impact | **3** | Billing statement display. Customer-facing financial data. |

### 21. CORPT00C — Report Generation (Online)

| Axis | Score | Rationale |
|------|-------|-----------|
| Complexity | **3** | 649 LOC. 20 IFs, 5 EVALUATEs, 34 PERFORMs. |
| Risk | **2** | 7 EXEC CICS. 1 WRITE, CALLs CSUTLDTC. |
| Business Impact | **3** | Report request screen. Triggers batch report generation. |

### 22. CBACT01C — Account File Initialization (Batch)

| Axis | Score | Rationale |
|------|-------|-----------|
| Complexity | **3** | 430 LOC. 22 IFs, 35 PERFORMs. |
| Risk | **3** | 4 VSAM files, 11 I/O operations. CALLs CEE3ABD and COBDATFT. Account master file creation. |
| Business Impact | **2** | Initial account file setup. One-time or periodic utility. |

### 23. CBPAUP0C — Auth Pending Update (Batch)

| Axis | Score | Rationale |
|------|-------|-----------|
| Complexity | **2** | 386 LOC. 17 IFs, 2 EVALUATEs, 17 PERFORMs. |
| Risk | **3** | 16 VSAM I/O operations — high for a batch program. No external calls. |
| Business Impact | **3** | Updates pending authorization records. Sub-application batch job. |

---

## Priority Tier 4 — Low Hotspots

> Combined score ≤ 6. Simple structure, limited coupling, or narrow scope.

| # | Program | LOC | Type | Function | Complexity | Risk | Impact | Total |
|---|---------|-----|------|----------|-----------|------|--------|-------|
| 24 | COUSR02C | 414 | Online | User Update | 2 | 2 | 2 | 6 |
| 25 | COUSR03C | 359 | Online | User Delete | 2 | 2 | 2 | 6 |
| 26 | COTRN01C | 330 | Online | Transaction Detail | 2 | 1 | 2 | 5 |
| 27 | COMEN01C | 308 | Online | Main Menu | 1 | 2 | 2 | 5 |
| 28 | COUSR01C | 299 | Online | User Add | 2 | 1 | 2 | 5 |
| 29 | COADM01C | 288 | Online | Admin Menu | 1 | 2 | 2 | 5 |
| 30 | COSGN00C | 260 | Online | Signon | 2 | 2 | 3 | 7 |
| 31 | COPAUS2C | 244 | Online | Auth DB2 Query | 2 | 2 | 1 | 5 |
| 32 | COBTUPDT | 237 | Batch | DB2 Batch Update | 2 | 2 | 2 | 6 |
| 33 | CBACT02C | 178 | Batch | Card File Init | 1 | 1 | 1 | 3 |
| 34 | CBACT03C | 178 | Batch | Xref File Init | 1 | 1 | 1 | 3 |
| 35 | CBCUS01C | 178 | Batch | Customer File Init | 1 | 1 | 1 | 3 |
| 36 | CSUTLDTC | 157 | Shared | Date Utility | 1 | 2 | 2 | 5 |
| 37 | COACCT01 | 620 | Online | Account Display (VSAM-MQ) | 2 | 1 | 1 | 4 |
| 38 | CODATE01 | 524 | Online | Date Utility (VSAM-MQ) | 2 | 1 | 1 | 4 |
| 39 | COBSWAIT | 41 | Batch | Wait Stub | 1 | 1 | 1 | 3 |

**Note:** COSGN00C scores 7 overall but is listed here because its complexity and risk are low (2/2). Its business impact of 3 reflects that it's the application entry point — the signon screen — and must work correctly for any user to access the system. However, structurally it's simple (260 LOC, 4 IFs, 10 EXEC CICS).

---

## Complexity Metrics Summary

Ranked by composite complexity indicators:

| Program | LOC | IFs | EVALs | PERFORMs | GO TOs | CICS Cmds | SQL Stmts | VSAM I/O | Copybooks | Paragraphs |
|---------|-----|-----|-------|----------|--------|-----------|-----------|----------|-----------|------------|
| COACTUPC | 4,236 | 168 | 10 | 64 | **51** | 17 | 0 | 2 | **56** | 73 |
| COTRTLIC | 2,098 | 0 | 16 | 63 | **28** | 12 | **16** | 8 | 11 | — |
| COTRTUPC | 1,702 | 0 | 26 | 40 | **23** | 12 | 7 | **21** | 13 | — |
| COCRDUPC | 1,560 | 74 | 16 | 26 | **21** | 12 | 0 | 3 | 15 | 39 |
| COCRDLIC | 1,459 | 61 | 18 | 34 | **16** | **18** | 0 | 4 | 13 | 37 |
| COPAUS0C | 1,032 | 25 | 11 | 46 | 0 | 10 | 0 | 0 | 14 | 15 |
| COPAUA0C | 1,026 | 26 | 10 | 38 | 0 | 12 | 0 | 7 | 16 | 41 |
| COACTVWC | 941 | 29 | 10 | 21 | 9 | 15 | 0 | 3 | 15 | 27 |
| COCRDSLC | 887 | 33 | 8 | 19 | 9 | 14 | 0 | 2 | 15 | 28 |
| COTRN02C | 783 | 14 | 13 | 61 | 0 | 11 | 0 | 0 | 10 | 9 |
| CBTRN02C | 731 | 48 | 0 | 61 | 0 | 0 | 0 | 12 | 5 | 15 |
| COTRN00C | 699 | 26 | 8 | 43 | 0 | 10 | 0 | 0 | 8 | 10 |
| COUSR00C | 695 | 25 | 8 | 41 | 0 | 11 | 0 | 0 | 8 | 10 |
| CBACT04C | 652 | 43 | 0 | 56 | 0 | 0 | 0 | 10 | 5 | 13 |
| CBTRN03C | 649 | 38 | 4 | **72** | 0 | 0 | 0 | 10 | 5 | 18 |
| CORPT00C | 649 | 20 | 5 | 34 | 1 | 7 | 0 | 0 | 8 | 10 |
| **TOTALS** | **27,969** | **834** | **219** | **887** | **158** | **217** | **53** | **119** | **—** | **—** |

### Key Observations

- **GO TO concentration**: 5 programs account for all 158 GO TOs (COACTUPC=51, COTRTLIC=28, COTRTUPC=23, COCRDUPC=21, COCRDLIC=16). These are the highest-risk for automated conversion.
- **EVALUATE-only programs**: COTRTLIC and COTRTUPC use zero IF statements and rely entirely on EVALUATE — an unusual pattern that may trip up conversion tools expecting IF/ELSE.
- **Highest PERFORM density**: CBTRN03C has 72 PERFORMs in 649 LOC (1 per 9 lines) — deeply factored but many tiny paragraphs to map.
- **Highest CICS coupling**: COCRDLIC at 18 EXEC CICS commands, followed by COACTUPC at 17.

---

## Shared Copybook Coupling Matrix

Copybooks used by 5+ programs represent tight coupling and high-impact change surfaces:

| Copybook | Programs Using | Type | Impact |
|----------|---------------|------|--------|
| COCOM01Y | **21** | Communication area | Changing this affects every online program |
| COTTL01Y | **21** | Screen title | Low-impact (display only) |
| CSDAT01Y | **21** | Date/time storage | Changing format affects every online program |
| CSMSG01Y | **21** | Common messages | Low-impact (display only) |
| CSUSR01Y | **14** | User security record | Auth/access control in 14 programs |
| CVACT01Y | **15** | Account record | Core entity — 15 programs read/write |
| CVACT03Y | **15** | Card xref record | Cross-reference — 15 programs depend |
| CVTRA05Y | **11** | Transaction record | Core entity — 11 programs |
| CVACT02Y | **10** | Card record | 10 programs |
| CVCUS01Y | **10** | Customer record | 10 programs |
| CSMSG02Y | **8** | Abend data | Error handling in 8 programs |
| CSSTRPFY | **7** | String processing | Utility in 7 programs |
| CVCRD01Y | **7** | CICS work areas | UI navigation in 7 programs |

**Highest-risk copybooks for modernization:**
1. **COCOM01Y** (21 programs) — The COMMAREA structure. Any field change breaks 21 programs simultaneously. Must be converted to a shared DTO/context object.
2. **CVACT01Y** (15 programs) — Account record layout. The core data entity of the entire system.
3. **CVACT03Y** (15 programs) — Card cross-reference. Links cards to customers and accounts.

---

## Program Call Graph

```
CICS Online Entry Points:
  CC00 → COSGN00C (Signon)
           ↓
  CM00 → COMEN01C (Main Menu) ──→ dispatches via COMMAREA to:
           ├── COACTVWC (Account View)
           ├── COACTUPC (Account Update) ★ CRITICAL
           ├── COCRDLIC (Card List) → COCRDSLC (Card Select) → COCRDUPC (Card Update)
           ├── COBIL00C (Billing)
           ├── COTRN00C (Transaction List) → COTRN01C (Detail) → COTRN02C (Add)
           ├── CORPT00C (Reports) → CALL CSUTLDTC
           └── COUSR00C (User List) → COUSR01C (Add) / COUSR02C (Update) / COUSR03C (Delete)

  CA00 → COADM01C (Admin Menu) ──→ dispatches to:
           ├── All regular menu options above
           ├── COTRTLIC (Tran Type List) → COTRTUPC (Tran Type Update) [DB2 sub-app]
           └── COPAUS0C (Auth Summary) → COPAUS1C (Auth Detail) [IMS sub-app]

Batch Chain (Daily Processing):
  POSTTRAN JCL:
    1. CBTRN01C (Initialize transaction file)
    2. CBTRN02C (Post daily transactions) ★ CRITICAL
    3. CBACT04C (Calculate interest) ★ CRITICAL
    4. CBTRN03C (Generate daily report)

  CBACT01C (Account file init) → CALL COBDATFT (date formatting)

Authorization Sub-App:
  COPAUA0C → CALL MQOPEN/MQGET/MQPUT1/MQCLOSE (MQ integration)
  CBPAUP0C (Batch pending update)

Data Migration:
  CBEXPORT → reads 5 VSAM files → writes single CVEXPORT sequential file
  CBIMPORT → reads CVEXPORT sequential file → writes 5 VSAM files
```

---

## VSAM File Coupling

Programs grouped by the VSAM files they access:

| VSAM Dataset | Batch Programs | Online Programs | Access Mode |
|--------------|----------------|-----------------|-------------|
| **Account (ACCTFILE)** | CBACT01C, CBACT04C, CBTRN02C, CBEXPORT, CBIMPORT | COACTUPC, COACTVWC, COBIL00C, COTRN02C, COPAUA0C, COPAUS0C | Read, I-O, Output |
| **Transaction (TRANFILE)** | CBTRN01C, CBTRN02C, CBTRN03C, CBACT04C, CBEXPORT, CBIMPORT | COTRN00C, COTRN01C, COTRN02C, COBIL00C, CORPT00C | Read, I-O, Output |
| **Card Xref (XREFFILE)** | CBTRN01C, CBTRN02C, CBTRN03C, CBACT04C, CBEXPORT, CBIMPORT | COACTUPC, COACTVWC, COCRDSLC, COCRDUPC, COBIL00C, COPAUA0C, COPAUS0C | Read, Random |
| **Card (CARDDAT)** | CBACT02C, CBTRN01C, CBEXPORT, CBIMPORT | COCRDLIC, COCRDSLC, COCRDUPC, COACTVWC, COPAUS0C | Read, Browse, I-O |
| **Customer (CUSTFILE)** | CBCUS01C, CBEXPORT, CBIMPORT | COACTUPC, COACTVWC, COCRDSLC, COCRDUPC, COPAUA0C, COPAUS0C | Read, Output |
| **User Security (USRSEC)** | — | COSGN00C, COUSR00C–03C, COMEN01C, COADM01C, + most online programs via COCOM01Y | Read, I-O |
| **Tran Cat Balance (TCATBALF)** | CBTRN02C, CBACT04C | — | I-O, Sequential |
| **Disclosure Group (DISCGRP)** | CBACT04C | — | Read, Random |
| **Daily Transaction (DALYTRAN)** | CBTRN01C, CBTRN02C | — | Sequential Input |

---

## Modernization Sequencing Recommendations

Based on the analysis above, here is a recommended modernization order that minimizes risk while enabling incremental delivery:

### Phase 1: Foundation (Low risk, high reuse)

**Convert shared utilities and data structures first.**

1. **Copybook-to-Java DTO conversion** — CVACT01Y, CVACT02Y, CVACT03Y, CVCUS01Y, CVTRA05Y, CVTRA01Y, CVTRA02Y, CSUSR01Y, COCOM01Y
2. **Utility programs** — CSUTLDTC (date validation), COBDATFT (date formatting), CODATECN (date conversion)
3. **Simple CRUD screens** — COUSR01C (User Add), COUSR02C (User Update), COUSR03C (User Delete)
4. **Navigation/menu programs** — COSGN00C, COMEN01C, COADM01C

### Phase 2: Read-Only Screens (Medium risk)

**Convert inquiry screens that don't modify data.**

5. **COACTVWC** — Account View
6. **COCRDSLC** — Card Select/Detail
7. **COTRN00C** → **COTRN01C** — Transaction List → Detail
8. **COUSR00C** — User List
9. **COPAUS0C** → **COPAUS1C** — Auth Summary → Detail (sub-app)
10. **COACCT01** / **CODATE01** — VSAM-MQ sub-app screens

### Phase 3: Data Entry Screens (High risk)

**Convert screens that write data — require extensive regression testing.**

11. **COCRDUPC** — Card Update
12. **COCRDLIC** — Card List (browse + select for update)
13. **COTRN02C** — Transaction Add
14. **COBIL00C** — Billing
15. **CORPT00C** — Report Generation

### Phase 4: Critical Business Logic (Highest risk)

**Convert the programs that process financial data. Requires parallel-run validation.**

16. **CBTRN01C** — Transaction File Init
17. **CBTRN02C** — Daily Transaction Posting ★
18. **CBACT04C** — Interest Calculation ★
19. **CBTRN03C** — Daily Transaction Report

### Phase 5: Complex Screens & Sub-Apps (Highest complexity)

**These programs require the most effort and should benefit from patterns established in earlier phases.**

20. **COACTUPC** — Account Update ★ (4,236 LOC, 51 GO TOs — likely requires manual rewrite)
21. **COTRTLIC** — Transaction Type List (DB2 + CICS dual-technology)
22. **COTRTUPC** — Transaction Type Update (DB2 + CICS dual-technology)
23. **COPAUA0C** — Authorization Processor (MQ integration)

### Phase 6: Migration Utilities

**Convert last — only needed for data migration scenarios.**

24. **CBEXPORT** / **CBIMPORT** — Data export/import
25. **CBACT01C** / **CBACT02C** / **CBACT03C** / **CBCUS01C** — File initialization utilities
26. **CBPAUP0C** — Auth pending update batch
27. **COBTUPDT** — DB2 batch update
28. **COBSWAIT** — Wait stub

---

### Cross-Cutting Concerns for All Phases

| Concern | Description | Programs Affected |
|---------|-------------|-------------------|
| **COBOL decimal arithmetic** | `S9(n)V99` → Java `BigDecimal`. Must preserve truncation and rounding semantics exactly. | CBTRN02C, CBACT04C, COACTUPC, COBIL00C |
| **COMMAREA state management** | COCOM01Y defines pseudo-conversational state passed between all online programs. Must convert to session/request scope. | All 21 online programs |
| **CICS file I/O** | EXEC CICS READ/WRITE/REWRITE/DELETE/STARTBR/READNEXT must map to JPA/JDBC or equivalent. | All online programs with VSAM access |
| **GO TO elimination** | 158 GO TOs across 5 programs. Automated conversion may produce incorrect control flow. Manual verification required. | COACTUPC, COTRTLIC, COTRTUPC, COCRDUPC, COCRDLIC |
| **88-level condition names** | Boolean flags defined via level-88 entries. Must map to Java enums or boolean constants with identical semantics. | Nearly all programs |
| **REDEFINES overlays** | Same storage reinterpreted as different types. Requires careful type mapping — no Java equivalent. | COACTUPC, COCRDUPC, CBEXPORT/CBIMPORT, CBTRN02C |
| **PIC-edited output** | Report formatting with PIC `Z`, `+`, `.`, `,` editing characters. Must replicate exactly for regulatory reports. | CBTRN03C, COACTUPC |
