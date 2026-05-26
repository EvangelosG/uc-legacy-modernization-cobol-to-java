# CardDemo COBOL-to-Java Migration Tracker

## Overview

| Metric | Value |
|--------|-------|
| Total COBOL Programs | 43 |
| Total LOC (COBOL) | ~28,000 |
| Migration Start Date | 2026-05-26 |
| Current Phase | Phase 4 — Pending |
| Overall Status | **In Progress** |

## Phase Summary

| Phase | Name | Status | Child Session | Programs | Validation |
|-------|------|--------|---------------|----------|------------|
| 0 | Infrastructure & Foundation | **Done** | [d2db011](https://outpostai.devinenterprise.com/sessions/d2db011778f4423388673422e0876ede) | Copybooks → DTOs, CSUTLDTC, COBSWAIT | `mvn compile` + `mvn test` — 82 tests pass |
| 1 | Identity, Menus & User Admin | **Done** | [ff4b49c](https://outpostai.devinenterprise.com/sessions/ff4b49c4dcd14c9c86276bbf6e1b609a) | COSGN00C, COMEN01C, COADM01C, COUSR00C–03C | 117 tests pass (19 new) |
| 2 | Reference Data & Read-Only Screens | **Done** | [bfd08a5](https://outpostai.devinenterprise.com/sessions/bfd08a5920604f96a8c4714b36cf8416) | COTRTLIC, COTRTUPC, COACTVWC, COCRDSLC, COTRN00C/01C, CORPT00C, COBIL00C (read), COPAUS0C/1C | 150 tests pass (52 new) |
| 3 | Data Entry Screens | **Done** | [b8ef7e6](https://outpostai.devinenterprise.com/sessions/b8ef7e66a1c04628ba509871ce2a1034) | COCRDLIC, COCRDUPC, COTRN02C (online), COBIL00C (write) | 188 tests pass (26 new) |
| 4 | Batch Pipeline | Pending | — | CBTRN01C, CBTRN02C, CBACT04C, CBTRN03C, CBSTM03A/B | Zero-tolerance financial tests |
| 5 | Complex Screens & Sub-Apps | Pending | — | COACTUPC, COPAUA0C, COPAUS2C, CBPAUP0C, CODATE01, COACCT01 | Decomposition validation |
| 6 | Data Migration & Decommission | Pending | — | CBEXPORT, CBIMPORT, CBACT01C–03C, CBCUS01C | E2E migration tests |

## Detailed Program Migration Status

### Phase 0 — Infrastructure & Foundation

| COBOL Artifact | Java Target | Status | Notes |
|----------------|-------------|--------|-------|
| CVACT01Y.cpy | Account.java (entity) | **Done** | 300 bytes, account record |
| CVACT02Y.cpy | Card.java (entity) | **Done** | 150 bytes, card record |
| CVACT03Y.cpy | CardCrossReference.java (entity) | **Done** | 50 bytes, card xref |
| CVCUS01Y.cpy | Customer.java (entity) | **Done** | 500 bytes, customer record |
| CVTRA05Y.cpy | Transaction.java (entity) | **Done** | 350 bytes, transaction record |
| CVTRA06Y.cpy | DailyTransaction.java (entity) | **Done** | 350 bytes, daily transaction |
| CVTRA01Y.cpy | TransactionCategoryBalance.java (entity) | **Done** | 50 bytes, composite key |
| CVTRA02Y.cpy | DisclosureGroup.java (entity) | **Done** | 50 bytes, composite key |
| CVTRA03Y.cpy | TransactionType.java (entity) | **Done** | 60 bytes, tx type |
| CVTRA04Y.cpy | TransactionCategory.java (entity) | **Done** | 60 bytes, tx category |
| CSUSR01Y.cpy | UserSecurity.java (entity) | **Done** | 80 bytes, user security |
| COCOM01Y.cpy | CardDemoSession.java (DTO) | **Done** | COMMAREA layout |
| CSUTLDTC.cbl | DateUtil.java | **Done** | 157 LOC, date utility |
| COBSWAIT.cbl | — (eliminated) | **Done** | 41 LOC, wait utility |
| CSLKPCDY.cpy | LookupService.java | **Done** | 1,318-line area code table |
| CODATECN.cpy | DateUtil.java | **Done** | Date conversion |
| Flyway V001–V012 | SQL migrations | **Done** | Database schema + seed data |
| Value objects | Money, CardNumber, typed IDs | **Done** | Domain value objects |

### Phase 1 — Identity, Menus & User Admin

| COBOL Program | LOC | Java Target | Status | Notes |
|---------------|-----|-------------|--------|-------|
| COSGN00C.cbl | 260 | AuthController + AuthService | **Done** | JWT-based auth |
| COMEN01C.cbl | 308 | MenuController | **Done** | Regular user menu |
| COADM01C.cbl | 288 | MenuController | **Done** | Admin menu |
| COUSR00C.cbl | 695 | UserController (GET list) | **Done** | Paginated user list |
| COUSR01C.cbl | 299 | UserController (POST) | **Done** | Create user |
| COUSR02C.cbl | 414 | UserController (PUT) | **Done** | Update user |
| COUSR03C.cbl | 359 | UserController (DELETE) | **Done** | Delete user |

### Phase 2 — Reference Data & Read-Only Screens

| COBOL Program | LOC | Java Target | Status | Notes |
|---------------|-----|-------------|--------|-------|
| COTRTLIC.cbl | 2,098 | TransactionTypeController (REWRITE) | **Done** | 28 GO TOs, DB2+CICS |
| COTRTUPC.cbl | 1,702 | TransactionTypeController (REWRITE) | **Done** | 23 GO TOs, DB2+CICS |
| COBTUPDT.cbl | 237 | TransactionTypeService | **Done** | Batch tx type maintenance |
| COACTVWC.cbl | 941 | AccountController (GET) | **Done** | Account view |
| COCRDSLC.cbl | 887 | CardController (GET) | **Done** | Card detail/select |
| COTRN00C.cbl | 699 | TransactionController (GET list) | **Done** | Transaction list |
| COTRN01C.cbl | 330 | TransactionController (GET detail) | **Done** | Transaction view |
| CORPT00C.cbl | 649 | ReportController | **Done** | Report request |
| COBIL00C.cbl | 572 | BillingController (GET) | **Done** | Bill payment (read) |
| COPAUS0C.cbl | 1,032 | AuthorizationViewController (GET) | **Done** | Auth summary |
| COPAUS1C.cbl | 604 | AuthorizationViewController (GET) | **Done** | Auth detail |

### Phase 3 — Data Entry Screens

| COBOL Program | LOC | Java Target | Status | Notes |
|---------------|-----|-------------|--------|-------|
| COCRDLIC.cbl | 1,459 | CardController (GET list) | **Done** | 16 GO TOs, card browse |
| COCRDUPC.cbl | 1,560 | CardController (PUT) — REWRITE | **Done** | 21 GO TOs, 8-state machine → stateless REST |
| COTRN02C.cbl | 783 | TransactionController (POST) | **Done** | Online transaction add |
| COBIL00C.cbl | 572 | BillingController (POST) | **Done** | Bill payment (write) |

### Phase 4 — Batch Pipeline (HIGHEST RISK)

| COBOL Program | LOC | Hotspot Score | Java Target | Status | Notes |
|---------------|-----|---------------|-------------|--------|-------|
| CBTRN01C.cbl | 494 | 9 | TransactionInitJob | Pending | Transaction file init |
| CBTRN02C.cbl | 731 | **14** | DailyTransactionPostingJob | Pending | **CRITICAL** — daily posting |
| CBACT04C.cbl | 652 | **14** | InterestCalculationJob | Pending | **CRITICAL** — interest calc |
| CBTRN03C.cbl | 649 | 10 | DailyTransactionReportJob | Pending | Multi-level break report |
| CBSTM03A.CBL | — | 10 | StatementGenerationJob | Pending | Statement generation |
| CBSTM03B.CBL | — | — | StatementGenerationJob | Pending | Statement subroutine |

### Phase 5 — Complex Screens & Sub-Applications

| COBOL Program | LOC | Hotspot Score | Java Target | Status | Notes |
|---------------|-----|---------------|-------------|--------|-------|
| COACTUPC.cbl | 4,236 | **15** | AccountUpdateService + CustomerUpdateService + ValidationService — REWRITE | Pending | Most complex program |
| COPAUA0C.cbl | 1,026 | 10 | AuthorizationService | Pending | MQ-based auth → REST |
| COPAUS2C.cbl | 244 | 7 | FraudDetectionService | Pending | DB2 fraud check |
| CBPAUP0C.cbl | 386 | 7 | AuthorizationPurgeJob | Pending | Batch purge |
| CODATE01.cbl | 524 | 6 | SystemController (GET) | Pending | MQ date inquiry → REST |
| COACCT01.cbl | 620 | 6 | AccountController (GET extract) | Pending | MQ account inquiry → REST |

### Phase 6 — Data Migration & Decommission

| COBOL Program | LOC | Java Target | Status | Notes |
|---------------|-----|-------------|--------|-------|
| CBEXPORT.cbl | 582 | DataExportService | Pending | Data export |
| CBIMPORT.cbl | 487 | DataImportService | Pending | Data import |
| CBACT01C.cbl | 430 | — (replaced by Flyway V012) | Pending | Account file init |
| CBACT02C.cbl | 178 | — (replaced by Flyway V012) | Pending | Card file init |
| CBACT03C.cbl | 178 | — (replaced by Flyway V012) | Pending | Xref file init |
| CBCUS01C.cbl | 178 | — (replaced by Flyway V012) | Pending | Customer file init |
| VsamToPostgresqlMigrator | — | VsamToPostgresqlMigrator.java | Pending | ETL tool |
| MigrationValidationService | — | MigrationValidationService.java | Pending | Automated comparison |

## Go/No-Go Criteria (from CUTOVER_PLAN.md Section 13)

- [ ] All `mvn compile` passes across all modules
- [ ] All `mvn test` passes with 100% green
- [ ] Flyway migrations run successfully against PostgreSQL
- [ ] All seed data loads without errors
- [ ] JWT authentication flow works end-to-end
- [ ] All CRUD operations pass with role-based access
- [ ] All read-only endpoints return correct data matching COBOL output
- [ ] Optimistic locking produces 409 Conflict on concurrent updates
- [ ] Transaction posting produces identical balances as COBOL CBTRN02C
- [ ] Interest calculation matches COBOL CBACT04C to the penny
- [ ] COACTUPC decomposition passes all validation rule tests
- [ ] All ASCII data files load into PostgreSQL with matching row counts
- [ ] Zero orphan records (referential integrity)
- [ ] Full batch cycle runs successfully against migrated data

## Change Log

| Date | Phase | Action | Details |
|------|-------|--------|---------|
| 2026-05-26 | Setup | Project skeleton created | Maven multi-module structure at carddemo-java/ |
| 2026-05-26 | Phase 0 | **Completed** | 65 files, 4,137 LOC, 82 tests pass. 11 entities, 6 VOs, 11 repos, 12 Flyway migrations, 3 utils, 1 DTO |
| 2026-05-26 | Phase 1 | **Completed** | JWT auth, menu routing, user CRUD. 117 tests pass (35 new service+web tests) |
| 2026-05-26 | Phase 2 | **Completed** | 7 controllers, 7 services, tx type CRUD, read-only views. 150 tests pass (52 new) |
| 2026-05-26 | Phase 3 | **Completed** | Card update (optimistic locking), transaction add, bill payment. 188 tests pass (26 new) |
