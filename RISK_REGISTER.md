# CardDemo Risk Register

> **Repository:** `EvangelosG/uc-legacy-modernization-cobol-to-java`
> **Companion Documents:** [MODERNIZATION_BLUEPRINT.md](MODERNIZATION_BLUEPRINT.md) | [DOMAIN_DECOMPOSITION.md](DOMAIN_DECOMPOSITION.md) | [CUTOVER_PLAN.md](CUTOVER_PLAN.md)
> **Source Analysis:** [APPLICATION_INVENTORY.md](APPLICATION_INVENTORY.md) | [DEPENDENCY-MAP.md](DEPENDENCY-MAP.md) | [HOTSPOT-ANALYSIS.md](HOTSPOT-ANALYSIS.md) | [DATA-DICTIONARY.md](DATA-DICTIONARY.md)

---

## Table of Contents

1. [Risk Scoring Methodology](#1-risk-scoring-methodology)
2. [Risk Summary Heat Map](#2-risk-summary-heat-map)
3. [Critical Risks (Score >= 15)](#3-critical-risks-score--15)
4. [High Risks (Score 10–14)](#4-high-risks-score-10-14)
5. [Medium Risks (Score 6–9)](#5-medium-risks-score-6-9)
6. [Low Risks (Score <= 5)](#6-low-risks-score--5)
7. [Cross-Cutting Risk Themes](#7-cross-cutting-risk-themes)
8. [Risk Monitoring Plan](#8-risk-monitoring-plan)

---

## 1. Risk Scoring Methodology

Each risk is scored on two axes:

| Axis | Scale | Description |
|------|-------|-------------|
| **Likelihood** | 1–5 | Probability of occurrence (1 = rare, 5 = almost certain) |
| **Impact** | 1–5 | Severity if it occurs (1 = negligible, 5 = catastrophic — financial loss, data corruption, regulatory violation) |

**Risk Score** = Likelihood x Impact (max 25)

| Score Range | Level | Action Required |
|-------------|-------|-----------------|
| **15–25** | **Critical** | Requires active mitigation plan, executive sponsorship, and dedicated resources. Cannot proceed to the affected phase without mitigation in place. |
| **10–14** | **High** | Requires mitigation plan and regular monitoring. May delay affected phase if not addressed. |
| **6–9** | **Medium** | Documented mitigation; monitored during phase execution. |
| **1–5** | **Low** | Accepted with standard practices. |

---

## 2. Risk Summary Heat Map

| ID | Risk | L | I | Score | Phase | Category |
|----|------|---|---|-------|-------|----------|
| R-01 | Financial arithmetic divergence | 4 | 5 | **20** | 4 | Technical |
| R-02 | COACTUPC rewrite — logic loss | 4 | 5 | **20** | 5 | Technical |
| R-03 | Data migration corruption | 3 | 5 | **15** | 6 | Data |
| R-04 | GO TO conversion errors | 4 | 4 | **16** | 3, 5 | Technical |
| R-05 | COMMAREA state translation failure | 3 | 4 | **12** | 1–3 | Integration |
| R-06 | VSAM dual-write inconsistency | 3 | 4 | **12** | 3–5 | Data |
| R-07 | MQ/IMS replacement — message loss | 3 | 4 | **12** | 5 | Integration |
| R-08 | Batch pipeline performance degradation | 3 | 3 | **9** | 4 | Performance |
| R-09 | CICS browse → SQL pagination mismatch | 3 | 3 | **9** | 2–3 | Technical |
| R-10 | Copybook coupling — cascading breakage | 2 | 4 | **8** | 0–3 | Technical |
| R-11 | COBOL SME availability | 3 | 3 | **9** | All | Organizational |
| R-12 | Regulatory report formatting drift | 3 | 4 | **12** | 4 | Compliance |
| R-13 | Pessimistic → optimistic locking — race conditions | 3 | 3 | **9** | 3 | Technical |
| R-14 | 88-level condition semantics lost | 2 | 3 | **6** | All | Technical |
| R-15 | REDEFINES overlay — data misinterpretation | 3 | 3 | **9** | 5–6 | Technical |
| R-16 | Mainframe decommission timeline pressure | 3 | 3 | **9** | All | Organizational |
| R-17 | EVALUATE-only control flow mishandled | 3 | 3 | **9** | 2, 5 | Technical |
| R-18 | DB2 → PostgreSQL SQL dialect gaps | 2 | 2 | **4** | 2, 5 | Technical |
| R-19 | Password migration — cleartext exposure | 2 | 3 | **6** | 1 | Security |
| R-20 | Parallel-run infrastructure cost | 2 | 2 | **4** | 4 | Operational |

---

## 3. Critical Risks (Score >= 15)

### R-01: Financial Arithmetic Divergence

| | |
|---|---|
| **ID** | R-01 |
| **Category** | Technical |
| **Phase** | 4 (Batch Pipeline) |
| **Likelihood** | **4** — COBOL and Java have fundamentally different numeric representation. COBOL uses fixed-point packed decimal (COMP-3); Java `BigDecimal` uses arbitrary precision. Rounding behavior, truncation on size error, and intermediate result precision all differ subtly. |
| **Impact** | **5** — Incorrect account balances or interest charges directly impact customer billing. Even a $0.01 per-account error across 10,000 accounts = $100 daily. Regulatory and audit implications. |
| **Score** | **20 — Critical** |

**Affected Programs:** CBTRN02C (transaction posting), CBACT04C (interest calculation), COBIL00C (billing)

**Specific Concerns:**
- COBOL `COMPUTE ROUNDED` uses a default rounding mode that may not match Java's `RoundingMode.HALF_EVEN`
- COBOL `ON SIZE ERROR` truncates silently; Java throws `ArithmeticException`
- Intermediate result precision in multi-step calculations: COBOL truncates at each step; Java may preserve full precision and truncate only at the final step
- `PIC S9(10)V99` implies 2-decimal precision throughout; Java `BigDecimal` may introduce unintended precision in division

**Mitigations:**

| # | Mitigation | Owner | Status |
|---|-----------|-------|--------|
| M-01a | Create a `CobolArithmetic` utility class that replicates COBOL truncation/rounding semantics exactly. Document the rounding mode mapping for every `COMPUTE` statement in CBTRN02C and CBACT04C. | Dev Lead | Planned (Phase 0) |
| M-01b | Build a comprehensive arithmetic test suite using real COBOL program output. Extract actual intermediate values from COBOL execution (DISPLAY statements) and verify Java produces identical results at each step. | QA Lead | Planned (Phase 4) |
| M-01c | **Parallel-run validation is mandatory.** Run both COBOL and Java batch pipelines on the same daily input for at least 2 full billing cycles. Compare every account balance, interest amount, and category total. **Zero tolerance** — any discrepancy blocks cutover. | Migration Lead | Planned (Phase 4) |
| M-01d | Engage an independent auditor to validate the arithmetic conversion for regulatory compliance before cutover. | Program Manager | Planned (Phase 4) |

---

### R-02: COACTUPC Rewrite — Logic Loss

| | |
|---|---|
| **ID** | R-02 |
| **Category** | Technical |
| **Phase** | 5 (Complex Screens) |
| **Likelihood** | **4** — COACTUPC is 4,236 lines with 168 IF statements, 51 GO TOs, 56 copybook dependencies, and mixed account + customer update logic. The GO TO-driven control flow makes it extremely difficult to trace all execution paths. Automated conversion will produce unreadable spaghetti Java; manual rewrite risks missing edge-case paths. |
| **Impact** | **5** — COACTUPC is the primary account modification screen. Logic loss could result in: incorrect credit limit changes, corrupted customer PII (SSN, address), or silent data loss for specific field combinations. |
| **Score** | **20 — Critical** |

**Specific Concerns:**
- 51 GO TOs create non-linear control flow — some paths are effectively dead code, others are critical error recovery
- Inline SSN validation (embedded in the program, not a CALL) handles edge cases that may not be documented
- Phone number REDEFINES overlay reinterprets the same bytes as area code + number — no Java equivalent
- The program validates 10+ account fields, 8+ customer fields, and cross-references 5 VSAM files — hundreds of validation combinations

**Mitigations:**

| # | Mitigation | Owner | Status |
|---|-----------|-------|--------|
| M-02a | Before rewriting, create a comprehensive path analysis of COACTUPC: enumerate every GO TO target, document which execution paths reach which validation blocks, identify dead code. Use COBOL analysis tools (SonarQube COBOL plugin, Heirloom Elastic COBOL). | COBOL SME | Planned (Phase 5) |
| M-02b | Build a test harness around the legacy COACTUPC: send CICS BMS input maps with every combination of valid/invalid fields and capture the output (CICS response, VSAM writes, error messages). This becomes the acceptance test suite for the rewrite. | QA Lead | Planned (Phase 5) |
| M-02c | Decompose the rewrite into 4 independently testable components: AccountUpdateService, CustomerUpdateService, ValidationService, Controller. Each component has its own unit test suite. | Dev Lead | Planned (Phase 5) |
| M-02d | Conduct a formal code review of the rewrite by a COBOL SME who can verify that every validation rule from the original program is preserved. | COBOL SME | Planned (Phase 5) |

---

### R-03: Data Migration Corruption

| | |
|---|---|
| **ID** | R-03 |
| **Category** | Data |
| **Phase** | 6 (Data Migration & Decommission) |
| **Likelihood** | **3** — VSAM records use EBCDIC encoding, packed decimal (COMP-3), and zoned decimal formats. Migration to PostgreSQL requires character set conversion, numeric format translation, and handling of COBOL-specific data patterns (low-values, high-values, filler bytes). |
| **Impact** | **5** — Corrupted data in the production database = wrong account balances, invalid customer records, or lost transaction history. Potential regulatory consequences and customer-facing errors. |
| **Score** | **15 — Critical** |

**Specific Concerns:**
- EBCDIC → UTF-8 character conversion may lose or mangle special characters in customer names and addresses
- COMP-3 (packed decimal) fields must be decoded correctly — off-by-one nibble errors produce wildly wrong numbers
- VSAM record FILLER bytes may contain residual data that existing programs depend on (e.g., REDEFINES overlays)
- CVEXPORT overlay format (multiple entity types in one sequential file) requires type-discriminator logic

**Mitigations:**

| # | Mitigation | Owner | Status |
|---|-----------|-------|--------|
| M-03a | Build migration with record-by-record validation: after migrating each VSAM dataset, read every record from both VSAM and PostgreSQL, compare every field, report any discrepancy. | Data Engineer | Planned (Phase 6) |
| M-03b | Run migration on a copy of production data (not just test data) at least 3 times before the real cutover. Fix issues found in each dry run. | Data Engineer | Planned (Phase 6) |
| M-03c | Maintain a point-in-time VSAM backup that can be restored within 4 hours. Do not decommission VSAM storage until 30 days after successful migration. | Ops Team | Planned (Phase 6) |
| M-03d | Use proven EBCDIC conversion libraries (e.g., JRecord, Legstar) rather than custom conversion code. Validate with a known-good dataset that includes edge-case characters. | Data Engineer | Planned (Phase 6) |

---

### R-04: GO TO Conversion Errors

| | |
|---|---|
| **ID** | R-04 |
| **Category** | Technical |
| **Phase** | 3 (Data Entry), 5 (Complex Screens) |
| **Likelihood** | **4** — 158 GO TOs are concentrated in 5 programs: COACTUPC (51), COTRTLIC (28), COTRTUPC (23), COCRDUPC (21), COCRDLIC (16). GO TOs create non-structured control flow that automated conversion tools handle poorly. |
| **Impact** | **4** — Incorrect control flow conversion causes wrong screen states, missing validation, or data corruption. Not catastrophic for read-only screens but dangerous for update programs. |
| **Score** | **16 — Critical** |

**Mitigations:**

| # | Mitigation | Owner | Status |
|---|-----------|-------|--------|
| M-04a | For programs being rewritten (COACTUPC, COCRDUPC): perform manual GO TO → structured flow analysis before coding. Map every GO TO target and document which paragraphs they bypass. | COBOL SME | Planned |
| M-04b | For programs being refactored (if any with GO TOs): use COBOL restructuring tools (e.g., Heirloom, Raincode) to eliminate GO TOs in the COBOL source first, then convert the restructured COBOL. | Dev Lead | Planned |
| M-04c | For COTRTLIC and COTRTUPC (rewrite): the underlying logic is simple CRUD despite the complex GO TO flow. Write the Java service from the business requirements (not from the COBOL code), then validate outputs against the COBOL program. | Dev Lead | Planned |
| M-04d | Establish a regression test suite for each GO TO-heavy program before any conversion work begins. | QA Lead | Planned |

---

## 4. High Risks (Score 10-14)

### R-05: COMMAREA State Translation Failure

| | |
|---|---|
| **ID** | R-05 |
| **Score** | **12** (L:3 x I:4) |
| **Phase** | 1–3 |
| **Description** | During the strangler migration, new Java screens coexist with legacy CICS screens. Navigation between them requires translating JWT claims to/from COMMAREA (COCOM01Y) fields. COCOM01Y is used by 21 programs and carries 30+ fields including program names, entity IDs, screen state flags, and navigation history. A translation error causes incorrect screen routing, lost entity context, or broken back-navigation (PF3). |

**Mitigations:**

| # | Mitigation | Owner |
|---|-----------|-------|
| M-05a | Map every COCOM01Y field to its JWT or request-parameter equivalent. Document which fields are still needed by legacy programs and which can be dropped. | Dev Lead |
| M-05b | Build integration tests that navigate across the legacy/modern boundary: Java screen → CICS screen → Java screen. Verify entity context (account ID, card number) is preserved. | QA Lead |
| M-05c | Implement the ACL as a dedicated middleware component with its own unit tests and logging, not as inline code in each service. | Dev Lead |

---

### R-06: VSAM Dual-Write Inconsistency

| | |
|---|---|
| **ID** | R-06 |
| **Score** | **12** (L:3 x I:4) |
| **Phase** | 3–5 |
| **Description** | During Phases 3–5, write operations go to both PostgreSQL and VSAM (dual-write). If one write succeeds and the other fails, the two data stores diverge. Divergence in account balances or transaction records causes downstream batch processing errors. |

**Mitigations:**

| # | Mitigation | Owner |
|---|-----------|-------|
| M-06a | VSAM is the source of truth during dual-write. If PostgreSQL write fails, log the error and retry asynchronously; do not fail the VSAM write. | Dev Lead |
| M-06b | Run a nightly reconciliation job comparing PostgreSQL and VSAM records for every entity modified that day. Alert on any discrepancy. | Data Engineer |
| M-06c | Design the dual-write layer as a transactional outbox: write to PostgreSQL, then publish an event that a separate consumer writes to VSAM. This ensures at-least-once delivery. | Dev Lead |

---

### R-07: MQ/IMS Replacement — Message Loss

| | |
|---|---|
| **ID** | R-07 |
| **Score** | **12** (L:3 x I:4) |
| **Phase** | 5 |
| **Description** | COPAUA0C processes authorization requests via MQ (MQGET → process → MQPUT1). Replacing MQ with Kafka or RabbitMQ risks message loss during the transition — in-flight authorization messages in the old MQ queues could be lost, and the new messaging system may have different delivery guarantees (at-most-once vs. at-least-once). |

**Mitigations:**

| # | Mitigation | Owner |
|---|-----------|-------|
| M-07a | Drain all MQ queues to zero before switching to the new messaging system. Schedule the cutover during a maintenance window with no authorization requests in flight. | Ops Team |
| M-07b | Use a message bridge (MQ → Kafka) during transition rather than a hard cutover. The bridge forwards MQ messages to Kafka topics. Legacy producers write to MQ; new consumers read from Kafka. | Dev Lead |
| M-07c | Implement idempotent authorization processing in the new service. Each authorization request has a unique ID; duplicate processing produces the same result. | Dev Lead |

---

### R-12: Regulatory Report Formatting Drift

| | |
|---|---|
| **ID** | R-12 |
| **Score** | **12** (L:3 x I:4) |
| **Phase** | 4 |
| **Description** | CBTRN03C produces regulatory transaction reports using PIC-edited output fields (CVTRA07Y). These reports have specific column alignments, decimal formatting, and page-break rules. The Java replacement must produce character-identical output. Any formatting drift may trigger regulatory review or audit findings. |

**Mitigations:**

| # | Mitigation | Owner |
|---|-----------|-------|
| M-12a | Extract 30 days of actual COBOL report output. The Java report job must produce byte-identical output for the same input data. Automate this comparison in the parallel-run framework. | QA Lead |
| M-12b | Build a PIC-edit formatting library in Java that replicates COBOL's `Z`, `+`, `.`, `,` edit characters exactly. Unit test with every PIC pattern used in CVTRA07Y. | Dev Lead |
| M-12c | Have a compliance officer sign off on the Java report output before cutover. | Program Manager |

---

## 5. Medium Risks (Score 6-9)

### R-08: Batch Pipeline Performance Degradation

| | |
|---|---|
| **ID** | R-08 |
| **Score** | **9** (L:3 x I:3) |
| **Phase** | 4 |
| **Description** | COBOL batch programs process VSAM files with raw I/O — no abstraction layers, no ORM overhead. The Java equivalent (Spring Batch + JPA + PostgreSQL) adds connection pooling, SQL parsing, object mapping, and transaction management overhead. The daily batch window may be exceeded. |

**Mitigations:**

| # | Mitigation | Owner |
|---|-----------|-------|
| M-08a | Benchmark the Java batch pipeline early with production-scale data volumes. If the batch window is exceeded, optimize: use JDBC batch inserts (not individual JPA saves), tune PostgreSQL WAL settings, use `COPY` for bulk loads. | Dev Lead |
| M-08b | Design Spring Batch jobs with chunk-oriented processing. Tune chunk size based on benchmarks (start with 1,000 records/chunk). | Dev Lead |
| M-08c | If performance is still insufficient, consider partitioned parallel processing — split the input file by account range and run multiple job instances concurrently. | Dev Lead |

---

### R-09: CICS Browse → SQL Pagination Mismatch

| | |
|---|---|
| **ID** | R-09 |
| **Score** | **9** (L:3 x I:3) |
| **Phase** | 2–3 |
| **Description** | COCRDLIC and COUSR00C use CICS STARTBR/READNEXT/READPREV/ENDBR for browsing VSAM files. This is cursor-based sequential access. The REST API replacement uses offset/limit pagination. The two approaches can produce different results when records are inserted or deleted between page requests (phantom reads). |

**Mitigations:**

| # | Mitigation | Owner |
|---|-----------|-------|
| M-09a | Use keyset pagination (WHERE id > last_seen_id ORDER BY id LIMIT n) instead of offset pagination. This matches CICS READNEXT semantics more closely and avoids phantom read issues. | Dev Lead |
| M-09b | Test pagination behavior with concurrent insert/delete operations. Document any behavioral differences between CICS browse and SQL pagination. | QA Lead |

---

### R-11: COBOL SME Availability

| | |
|---|---|
| **ID** | R-11 |
| **Score** | **9** (L:3 x I:3) |
| **Phase** | All |
| **Description** | COBOL expertise is scarce. The modernization requires COBOL SMEs to: analyze GO TO control flow, verify business rule preservation, explain undocumented program behavior, and validate parallel-run results. Loss of COBOL SME availability during critical phases (4, 5) delays the project. |

**Mitigations:**

| # | Mitigation | Owner |
|---|-----------|-------|
| M-11a | Identify and engage 2+ COBOL SMEs before Phase 0 begins. Establish retention agreements covering the full project duration. | Program Manager |
| M-11b | Front-load knowledge transfer: have COBOL SMEs document business rules, undocumented behaviors, and edge cases for all Tier 1 programs (COACTUPC, CBTRN02C, CBACT04C) during Phase 0. | COBOL SME |
| M-11c | Record screen-capture walkthroughs of COBOL SMEs explaining each critical program's logic. Archive as institutional knowledge. | Program Manager |

---

### R-13: Pessimistic → Optimistic Locking Race Conditions

| | |
|---|---|
| **ID** | R-13 |
| **Score** | **9** (L:3 x I:3) |
| **Phase** | 3 |
| **Description** | COCRDUPC and COACTUPC use CICS READ FOR UPDATE (pessimistic locking) — the record is locked when fetched and released on REWRITE. The Java replacement uses optimistic locking (@Version). Under concurrent access, optimistic locking may allow a user to overwrite another user's changes if the version check is not implemented correctly. |

**Mitigations:**

| # | Mitigation | Owner |
|---|-----------|-------|
| M-13a | Implement `@Version` on all updatable JPA entities. Configure Spring to throw `OptimisticLockException` on conflict. The UI must handle this gracefully (display "record modified by another user, please refresh"). | Dev Lead |
| M-13b | Test with concurrent update scenarios: two users loading the same record, both editing, both submitting. Verify the second submit is rejected. | QA Lead |
| M-13c | For high-contention records (e.g., account balance during batch posting), consider short-lived pessimistic locks using `SELECT ... FOR UPDATE SKIP LOCKED`. | Dev Lead |

---

### R-15: REDEFINES Overlay — Data Misinterpretation

| | |
|---|---|
| **ID** | R-15 |
| **Score** | **9** (L:3 x I:3) |
| **Phase** | 5–6 |
| **Description** | COBOL REDEFINES allows the same memory area to be interpreted as different data types. COACTUPC uses REDEFINES on phone numbers and SSN fields. CBEXPORT/CBIMPORT use CVEXPORT REDEFINES for multi-entity records. Java has no equivalent — the REDEFINES semantics must be explicitly modeled. |

**Mitigations:**

| # | Mitigation | Owner |
|---|-----------|-------|
| M-15a | For each REDEFINES usage, document both interpretations and which program paths use which interpretation. Model in Java as explicit conversion methods (e.g., `PhoneNumber.fromRedefines(byte[] raw)` and `PhoneNumber.toRedefines()`). | Dev Lead |
| M-15b | For CVEXPORT: replace the REDEFINES overlay with a discriminated union type (sealed interface with record subtypes) or separate entity-specific export formats. | Dev Lead |
| M-15c | Test REDEFINES conversions with boundary values: maximum/minimum numeric values, empty fields, all-nines, all-spaces. | QA Lead |

---

### R-16: Mainframe Decommission Timeline Pressure

| | |
|---|---|
| **ID** | R-16 |
| **Score** | **9** (L:3 x I:3) |
| **Phase** | All |
| **Description** | If the mainframe hosting contract has a fixed end date, the migration timeline cannot slip. Pressure to meet the deadline may cause teams to skip parallel-run validation, accept known discrepancies, or cut testing short. This is the most common root cause of failed mainframe modernizations. |

**Mitigations:**

| # | Mitigation | Owner |
|---|-----------|-------|
| M-16a | Negotiate a mainframe contract extension option (e.g., 6-month extension at a premium) as insurance. | Program Manager |
| M-16b | If timeline pressure is real, use the **Replatform** strategy for critical batch programs (Phase 4) as an interim step — move COBOL to Micro Focus on Linux to decommission the mainframe, then refactor to Java on a relaxed timeline. | Architect |
| M-16c | Establish non-negotiable gates: the parallel-run validation (R-01, M-01c) cannot be skipped regardless of timeline pressure. | Executive Sponsor |

---

### R-17: EVALUATE-Only Control Flow Mishandled

| | |
|---|---|
| **ID** | R-17 |
| **Score** | **9** (L:3 x I:3) |
| **Phase** | 2, 5 |
| **Description** | COTRTLIC and COTRTUPC use zero IF statements and rely entirely on EVALUATE (COBOL's equivalent of switch/case). This unusual pattern may trip conversion tools that expect IF/ELSE structures. EVALUATE with ALSO (multi-dimensional case matching) has no direct Java switch equivalent. |

**Mitigations:**

| # | Mitigation | Owner |
|---|-----------|-------|
| M-17a | Since COTRTLIC and COTRTUPC are being rewritten (not refactored), this risk is lower. Write the Java service from business requirements, not from COBOL structure. | Dev Lead |
| M-17b | If any EVALUATE-only programs are refactored instead: test the conversion tool on a small EVALUATE-only sample before converting the full program. | Dev Lead |

---

## 6. Low Risks (Score <= 5)

| ID | Risk | L | I | Score | Mitigation |
|----|------|---|---|-------|------------|
| R-14 | 88-level condition semantics lost | 2 | 3 | 6 | Map all 88-level flags to Java enums or boolean constants. Document the COBOL 88-level → Java mapping for every copybook. Review with COBOL SME. |
| R-18 | DB2 → PostgreSQL SQL dialect gaps | 2 | 2 | 4 | Only 4 programs use DB2 SQL (COTRTLIC: 16, COTRTUPC: 7, COBTUPDT: 5, COPAUS2C: 4). These are being rewritten with JPA — SQL dialect differences are handled by the JPA provider. Test DB2-specific SQL patterns (FETCH FIRST n ROWS, WITH UR isolation) against PostgreSQL equivalents. |
| R-19 | Password migration — cleartext exposure | 2 | 3 | 6 | USRSEC stores passwords in PIC X(08) cleartext. During migration (Phase 1), hash all passwords with bcrypt before inserting into PostgreSQL. Never log or display cleartext passwords during migration. Use a secure migration script that reads VSAM, hashes in memory, and writes to PostgreSQL — cleartext never touches disk in the new system. |
| R-20 | Parallel-run infrastructure cost | 2 | 2 | 4 | Running both COBOL and Java pipelines simultaneously requires additional compute and storage. Budget for 2x infrastructure during Phase 4. Use cloud auto-scaling to minimize cost outside the batch window. |

---

## 7. Cross-Cutting Risk Themes

### Theme 1: Financial Accuracy (R-01, R-12)

The highest-impact risks all relate to financial accuracy. COBOL's fixed-point decimal arithmetic, PIC-edited formatting, and implicit truncation rules create a "precision contract" with the business that must be exactly preserved.

**Overall Mitigation Strategy:**
1. Build a `CobolArithmetic` library (Phase 0) that replicates COBOL semantics
2. Parallel-run validation (Phase 4) with zero-tolerance comparison
3. Independent audit of financial calculations before cutover
4. Retain COBOL batch as hot standby for 1 billing cycle after cutover

### Theme 2: Control Flow Complexity (R-02, R-04, R-17)

158 GO TOs, EVALUATE-only programs, and 4,236-line monoliths make the COBOL codebase structurally hostile to automated conversion.

**Overall Mitigation Strategy:**
1. Rewrite (don't refactor) GO TO-heavy programs
2. Front-load path analysis and test harness construction
3. Require COBOL SME review of every rewritten program

### Theme 3: Data Integrity During Transition (R-03, R-06, R-13)

The multi-month transition period creates dual-system complexity. Data must remain consistent across VSAM and PostgreSQL.

**Overall Mitigation Strategy:**
1. VSAM remains source of truth until Phase 6
2. Dual-write with VSAM-primary semantics
3. Nightly reconciliation checks
4. Optimistic locking with conflict detection

### Theme 4: Organizational & Timeline (R-11, R-16)

Technical risks are manageable with sufficient time and expertise. The real danger is organizational: losing COBOL expertise mid-project or cutting validation short due to timeline pressure.

**Overall Mitigation Strategy:**
1. Retain COBOL SMEs for the full project duration
2. Front-load knowledge capture (documentation, screen recordings)
3. Negotiate mainframe contract extension option
4. Non-negotiable validation gates at Phase 4

---

## 8. Risk Monitoring Plan

### Phase-Level Risk Reviews

| Phase | Review Cadence | Key Risks to Monitor | Escalation Trigger |
|-------|---------------|---------------------|-------------------|
| 0 | Bi-weekly | R-11, R-16 | COBOL SME unavailable; mainframe contract issue |
| 1 | Weekly | R-05, R-19 | Auth failures; password exposure; COMMAREA ACL errors |
| 2 | Weekly | R-09, R-10, R-17 | Pagination mismatches; copybook breakage; EVALUATE conversion errors |
| 3 | **Daily** | R-04, R-06, R-13 | GO TO conversion bugs; dual-write inconsistencies; locking race conditions |
| 4 | **Daily** | **R-01**, R-08, **R-12** | Any financial discrepancy; batch window exceeded; report formatting drift |
| 5 | **Daily** | **R-02**, R-04, R-07, R-15 | COACTUPC logic loss; MQ message loss; REDEFINES misinterpretation |
| 6 | **Daily** | **R-03** | Any data migration discrepancy |

### Metrics to Track

| Metric | Target | Source |
|--------|--------|--------|
| Parallel-run discrepancy count | 0 | Comparison engine output |
| Dual-write inconsistency count | 0 | Nightly reconciliation job |
| Test coverage (rewritten programs) | >= 95% branch coverage | JaCoCo |
| COBOL SME hours available | >= 20 hrs/week during Phases 4-5 | Resource tracking |
| Batch job execution time (Java) | <= COBOL time x 1.5 | Spring Batch metrics |
| User-reported defects post-cutover | < 5 per phase | Issue tracker |

### Risk Retirement Schedule

| Risk | Retired After | Condition |
|------|--------------|-----------|
| R-05 | Phase 3 complete | All CICS programs replaced; COMMAREA ACL decommissioned |
| R-06 | Phase 5 complete | Dual-write decommissioned; PostgreSQL is sole target |
| R-09 | Phase 3 complete | All browse screens converted to REST pagination |
| R-13 | Phase 3 complete | All update screens use optimistic locking |
| R-01 | Phase 4 complete | 2 billing cycles with zero discrepancies |
| R-12 | Phase 4 complete | Report output validated by compliance |
| R-04 | Phase 5 complete | All GO TO-heavy programs rewritten |
| R-02 | Phase 5 complete | COACTUPC rewrite validated by COBOL SME |
| R-07 | Phase 5 complete | MQ → Kafka cutover validated |
| R-03 | Phase 6 complete | Full data migration validated |
| R-11 | Phase 6 complete | COBOL SME contract ends |
| R-16 | Phase 6 complete | Mainframe decommissioned |

---

*Cross-references: [MODERNIZATION_BLUEPRINT.md](MODERNIZATION_BLUEPRINT.md) | [DOMAIN_DECOMPOSITION.md](DOMAIN_DECOMPOSITION.md) | [CUTOVER_PLAN.md](CUTOVER_PLAN.md)*
