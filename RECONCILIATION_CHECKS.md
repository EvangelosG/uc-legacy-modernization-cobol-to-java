# CardDemo Reconciliation Checks

> **Repository:** `EvangelosG/uc-legacy-modernization-cobol-to-java`
> **Companion Documents:** [TEST_STRATEGY.md](TEST_STRATEGY.md) | [CUTOVER_PLAN.md](CUTOVER_PLAN.md) | [RISK_REGISTER.md](RISK_REGISTER.md)
> **Implementation:** [`test-harness/reconciliation.py`](test-harness/reconciliation.py)

---

## Table of Contents

1. [Overview](#1-overview)
2. [Check Summary](#2-check-summary)
3. [RC-01: Card Xref Referential Integrity](#3-rc-01-card-xref-referential-integrity)
4. [RC-02: Category Balance vs Account Balance Consistency](#4-rc-02-category-balance-vs-account-balance-consistency)
5. [RC-03: Account-Card Coverage](#5-rc-03-account-card-coverage)
6. [RC-04: Card-Account Foreign Key Validity](#6-rc-04-card-account-foreign-key-validity)
7. [RC-05: Daily Transaction Card Xref Validity](#7-rc-05-daily-transaction-card-xref-validity)
8. [RC-06: Disclosure Group Coverage](#8-rc-06-disclosure-group-coverage)
9. [RC-07: Transaction Posting Completeness](#9-rc-07-transaction-posting-completeness)
10. [RC-08: Interest Calculation Balance Integrity](#10-rc-08-interest-calculation-balance-integrity)
11. [RC-09: Statement Completeness](#11-rc-09-statement-completeness)
12. [RC-10: Data Migration Record Count Parity](#12-rc-10-data-migration-record-count-parity)
13. [Per-Job Validation Matrix](#13-per-job-validation-matrix)
14. [Execution Schedule](#14-execution-schedule)

---

## 1. Overview

Reconciliation checks verify **cross-entity data integrity** — invariants that span multiple datasets. Even if individual records are correct, relationships between entities can break during migration, batch processing, or dual-write periods. These checks catch:

- Orphaned records (foreign keys pointing to nonexistent parents)
- Balance mismatches (account totals ≠ sum of components)
- Lost transactions (input count ≠ output count after processing)
- Data drift during dual-write (VSAM ≠ PostgreSQL)

### Severity Levels

| Severity | Meaning | Action |
|----------|---------|--------|
| **CRITICAL** | Financial data integrity compromised | Halt processing, escalate immediately |
| **HIGH** | Referential integrity broken | Investigate within 4 hours, may require rollback |
| **MEDIUM** | Coverage gap or non-financial mismatch | Investigate within 24 hours |
| **LOW** | Informational — potential data quality issue | Log and review in next sprint |

---

## 2. Check Summary

| Check ID | Name | Datasets | Severity | When to Run | Automated |
|----------|------|----------|----------|-------------|:---------:|
| **RC-01** | Card Xref Referential Integrity | cardxref, acctdata, custdata, carddata | HIGH | After migration, after card/acct changes | ✓ |
| **RC-02** | Category Balance vs Account Balance | acctdata, tcatbal | CRITICAL | After POSTTRAN, after INTCALC | ✓ |
| **RC-03** | Account-Card Coverage | acctdata, cardxref | MEDIUM | After migration, weekly | ✓ |
| **RC-04** | Card-Account Foreign Key | carddata, acctdata | HIGH | After migration, after card changes | ✓ |
| **RC-05** | Daily Transaction Card Xref | dailytran, cardxref | HIGH | Before POSTTRAN (input validation) | ✓ |
| **RC-06** | Disclosure Group Coverage | acctdata, discgrp | MEDIUM | After migration, after group changes | ✓ |
| **RC-07** | Transaction Posting Completeness | dailytran, transact, dalyrejs | CRITICAL | After every POSTTRAN run | — |
| **RC-08** | Interest Calculation Balance Integrity | acctdata (pre/post) | CRITICAL | After every INTCALC run | — |
| **RC-09** | Statement Completeness | transact, statemnt output | MEDIUM | After every CREASTMT run | — |
| **RC-10** | Data Migration Record Count Parity | all VSAM, all PostgreSQL tables | CRITICAL | During Phase 6 migration | — |

Checks RC-01 through RC-06 are implemented in `test-harness/reconciliation.py` and can be run against the golden-file JSON. Checks RC-07 through RC-10 require batch output or database access and are specified here for implementation during the corresponding migration phase.

---

## 3. RC-01: Card Xref Referential Integrity

### Purpose

The card cross-reference file (`CARDXREF` / `cardxref`) is the central link between cards, accounts, and customers. Every xref record must point to valid entities in all three master files.

### Invariant

```
∀ xref ∈ CARDXREF:
    xref.XREF-ACCT-ID ∈ ACCTDATA.ACCT-ID
    ∧ xref.XREF-CUST-ID ∈ CUSTDATA.CUST-ID
    ∧ xref.XREF-CARD-NUM ∈ CARDDATA.CARD-NUM
```

### Datasets

| Dataset | Role | Key Field |
|---------|------|-----------|
| `cardxref` | Subject (iterate) | XREF-CARD-NUM |
| `acctdata` | Parent lookup | ACCT-ID |
| `custdata` | Parent lookup | CUST-ID |
| `carddata` | Parent lookup | CARD-NUM |

### Failure Modes

| Failure | Probable Cause | Risk |
|---------|---------------|------|
| XREF-ACCT-ID not in acctdata | Account deleted without cascading xref cleanup | R-03, R-06 |
| XREF-CUST-ID not in custdata | Customer record lost during migration | R-03 |
| XREF-CARD-NUM not in carddata | Card record not migrated or orphaned xref | R-03 |

### When to Run

- After every data migration dry run (Phase 6)
- After any batch job that modifies card, account, or customer data
- Nightly during dual-write periods (Phases 3–5)

### Golden Data Baseline

**Status:** PASS (50 records, 0 violations)

---

## 4. RC-02: Category Balance vs Account Balance Consistency

### Purpose

After transaction posting (`CBTRN02C` / POSTTRAN), the sum of all category balance records for an account should equal the account's current balance. This is the primary financial integrity check.

### Invariant

```
∀ acct ∈ ACCTDATA:
    acct.ACCT-CURR-BAL = Σ(tcb.TRAN-CAT-BAL)
        where tcb ∈ TCATBALF
        and tcb.TRANCAT-ACCT-ID = acct.ACCT-ID
```

### Datasets

| Dataset | Role | Key Field |
|---------|------|-----------|
| `acctdata` | Expected total | ACCT-ID → ACCT-CURR-BAL |
| `tcatbal` | Component balances | TRANCAT-ACCT-ID + TRANCAT-TYPE-CD + TRANCAT-CD → TRAN-CAT-BAL |

### Tolerance

**Zero.** Any discrepancy, even $0.01, is a critical failure requiring investigation. This directly maps to Risk R-01 (Financial arithmetic divergence).

### Failure Modes

| Failure | Probable Cause | Risk |
|---------|---------------|------|
| Sum > ACCT-CURR-BAL | Category balance updated but account balance not | R-01 |
| Sum < ACCT-CURR-BAL | Account balance updated but category not | R-01 |
| Category records missing for account | TCATBALF not initialized for new account | R-03 |

### When to Run

- After every POSTTRAN batch run
- After every INTCALC batch run
- During Phase 4 parallel-run comparison (both COBOL and Java output)

### Golden Data Baseline

**Status:** FAIL (50 violations — expected for pre-batch data where category balances are all zero but account balances are non-zero). This check becomes meaningful **after** the first POSTTRAN run.

---

## 5. RC-03: Account-Card Coverage

### Purpose

Every account should have at least one associated card (via the cross-reference). An account without a card entry may indicate a migration gap or an orphaned account record.

### Invariant

```
∀ acct ∈ ACCTDATA:
    ∃ xref ∈ CARDXREF: xref.XREF-ACCT-ID = acct.ACCT-ID
```

### Datasets

| Dataset | Role | Key Field |
|---------|------|-----------|
| `acctdata` | Subject (iterate) | ACCT-ID |
| `cardxref` | Coverage lookup | XREF-ACCT-ID |

### Severity: MEDIUM

An account without a card is a data quality issue but not a financial integrity issue. However, it may indicate incomplete migration.

### Golden Data Baseline

**Status:** PASS (50 accounts, all have at least one card xref)

---

## 6. RC-04: Card-Account Foreign Key Validity

### Purpose

Every card record's `CARD-ACCT-ID` must reference a valid account. A card pointing to a nonexistent account would cause failures in transaction posting (CBTRN02C looks up accounts via card xref).

### Invariant

```
∀ card ∈ CARDDATA:
    card.CARD-ACCT-ID ∈ ACCTDATA.ACCT-ID
```

### Datasets

| Dataset | Role | Key Field |
|---------|------|-----------|
| `carddata` | Subject (iterate) | CARD-NUM → CARD-ACCT-ID |
| `acctdata` | Parent lookup | ACCT-ID |

### Golden Data Baseline

**Status:** PASS (50 cards, all reference valid accounts)

---

## 7. RC-05: Daily Transaction Card Xref Validity

### Purpose

Before running POSTTRAN, verify that every daily transaction references a card number that exists in the cross-reference file. CBTRN02C uses the xref to resolve card → account for posting. Invalid card numbers result in rejected transactions (DALYREJS).

### Invariant

```
∀ txn ∈ DALYTRAN:
    txn.DALYTRAN-CARD-NUM ∈ CARDXREF.XREF-CARD-NUM
```

### Datasets

| Dataset | Role | Key Field |
|---------|------|-----------|
| `dailytran` | Subject (iterate) | DALYTRAN-CARD-NUM |
| `cardxref` | Lookup | XREF-CARD-NUM |

### Pre-Batch Validation

This check should run **before** POSTTRAN to predict how many transactions will be rejected. If the reject rate exceeds 5%, investigate the daily transaction feed before processing.

### Golden Data Baseline

**Status:** PASS (300 daily transactions, all reference valid card xref entries)

---

## 8. RC-06: Disclosure Group Coverage

### Purpose

Every account's group ID must have corresponding disclosure group entries that define interest rates. Without these entries, CBACT04C (interest calculation) cannot compute interest for the account.

### Invariant

```
∀ acct ∈ ACCTDATA where acct.ACCT-GROUP-ID ≠ '':
    ∃ disc ∈ DISCGRP: disc.DIS-ACCT-GROUP-ID = acct.ACCT-GROUP-ID
```

### Datasets

| Dataset | Role | Key Field |
|---------|------|-----------|
| `acctdata` | Subject (iterate) | ACCT-GROUP-ID |
| `discgrp` | Coverage lookup | DIS-ACCT-GROUP-ID |

### Golden Data Baseline

**Status:** PASS (0 accounts with non-empty group IDs to check — the sample data stores group information in ACCT-ADDR-ZIP rather than ACCT-GROUP-ID)

---

## 9. RC-07: Transaction Posting Completeness

### Purpose

After POSTTRAN (`CBTRN02C`), every daily transaction must be accounted for — either posted to `TRANSACT` or rejected to `DALYREJS`. No transaction should be silently dropped.

### Invariant

```
count(DALYTRAN) = count(posted to TRANSACT) + count(DALYREJS)

∀ txn ∈ DALYTRAN:
    txn.DALYTRAN-ID ∈ TRANSACT.TRAN-ID
    ∨ txn.DALYTRAN-ID ∈ DALYREJS
```

### Datasets

| Dataset | Role | Key Field |
|---------|------|-----------|
| `dailytran` | Input (pre-batch) | DALYTRAN-ID |
| `transact` | Posted output | TRAN-ID |
| `dalyrejs` | Rejected output | Reject record ID |

### Additional Validations

| Validation | Rule | Tolerance |
|------------|------|-----------|
| Amount sum | Σ(posted TRAN-AMT) + Σ(rejected AMT) = Σ(DALYTRAN-AMT) | Zero |
| Reject reason | Every reject has a non-blank reason code | Zero |
| Duplicate check | No DALYTRAN-ID appears in both TRANSACT and DALYREJS | Zero |

### Implementation Phase

Phase 4 — requires batch output files. Not implemented in current test harness (pre-batch data only).

---

## 10. RC-08: Interest Calculation Balance Integrity

### Purpose

After INTCALC (`CBACT04C`), account balances must reflect the computed interest. The difference between pre-INTCALC and post-INTCALC `ACCT-CURR-BAL` must equal the sum of interest transactions posted for that account.

### Invariant

```
∀ acct ∈ ACCTDATA:
    acct.ACCT-CURR-BAL(post) - acct.ACCT-CURR-BAL(pre) =
        Σ(interest_txn.AMT) where interest_txn.ACCT-ID = acct.ACCT-ID
```

### Datasets

| Dataset | Role | Key Field |
|---------|------|-----------|
| `acctdata` (pre-run snapshot) | Baseline balances | ACCT-ID → ACCT-CURR-BAL |
| `acctdata` (post-run) | Updated balances | ACCT-ID → ACCT-CURR-BAL |
| `systran` (interest output) | Interest amounts | Account ID → Interest AMT |

### Additional Validations

| Validation | Rule | Tolerance |
|------------|------|-----------|
| Interest rate bounds | Every computed rate matches disclosure group for account's group/type/category | Zero |
| Balance sign | No account balance changes sign unexpectedly (credit → debit) | Investigate |
| Cycle totals | ACCT-CURR-CYC-CREDIT and ACCT-CURR-CYC-DEBIT updated correctly | Zero |

### Implementation Phase

Phase 4 — requires pre/post snapshots and SYSTRAN output. This is the most critical reconciliation check for financial accuracy (Risk R-01).

---

## 11. RC-09: Statement Completeness

### Purpose

After CREASTMT (`CBSTM03A/B`), every account with transactions in the billing period must have a generated statement, and statement totals must match transaction sums.

### Invariant

```
∀ acct ∈ ACCTDATA where ∃ txn ∈ TRANSACT: txn.ACCT-ID = acct.ACCT-ID:
    ∃ stmt ∈ STATEMNT: stmt.ACCT-ID = acct.ACCT-ID
    ∧ stmt.TOTAL = Σ(txn.TRAN-AMT) for that account
```

### Datasets

| Dataset | Role | Key Field |
|---------|------|-----------|
| `transact` | Transaction source | TRAN-CARD-NUM → (via xref) ACCT-ID |
| `statemnt` | Statement output | Account ID → Statement total |

### Implementation Phase

Phase 4 — requires statement batch output.

---

## 12. RC-10: Data Migration Record Count Parity

### Purpose

During Phase 6 data migration, every VSAM dataset must have an identical record count in the corresponding PostgreSQL table. This is the gating check before mainframe decommission.

### Invariant

```
∀ dataset ∈ {ACCTDATA, CARDDATA, CARDXREF, CUSTDATA, TRANSACT,
              TCATBALF, DISCGRP, TRANTYPE, TRANCATG, USRSEC}:
    count(VSAM[dataset]) = count(PostgreSQL[table])
    ∧ ∀ record: VSAM[record] = PostgreSQL[record]  (field-by-field)
```

### Validation Steps

| Step | Check | Tool |
|------|-------|------|
| 1 | Record count parity | SQL `COUNT(*)` vs IDCAMS `LISTCAT` |
| 2 | Key coverage | Every VSAM key exists in PostgreSQL and vice versa |
| 3 | Field-by-field comparison | `test-harness/comparator.py` with zero tolerance |
| 4 | Checksum verification | MD5 of sorted, serialized records must match |

### Dataset-to-Table Mapping

| VSAM Dataset | PostgreSQL Table | Primary Key |
|-------------|-----------------|-------------|
| ACCTDATA | `accounts` | `account_id` |
| CARDDATA | `cards` | `card_number` |
| CARDXREF | `card_xrefs` | `card_number` |
| CUSTDATA | `customers` | `customer_id` |
| TRANSACT | `transactions` | `transaction_id` |
| TCATBALF | `transaction_category_balances` | `account_id, type_code, category_code` |
| DISCGRP | `disclosure_groups` | `group_id, type_code, category_code` |
| TRANTYPE | `transaction_types` | `type_code` |
| TRANCATG | `transaction_categories` | `type_code, category_code` |
| USRSEC | `users` | `user_id` |

### Implementation Phase

Phase 6 — requires PostgreSQL access.

---

## 13. Per-Job Validation Matrix

This matrix specifies which reconciliation checks run after each batch job or migration event:

| Batch Job / Event | RC-01 | RC-02 | RC-03 | RC-04 | RC-05 | RC-06 | RC-07 | RC-08 | RC-09 | RC-10 |
|-------------------|:-----:|:-----:|:-----:|:-----:|:-----:|:-----:|:-----:|:-----:|:-----:|:-----:|
| **POSTTRAN** (CBTRN02C) | — | ✓ | — | — | ✓ (pre) | — | ✓ | — | — | — |
| **INTCALC** (CBACT04C) | — | ✓ | — | — | — | ✓ | — | ✓ | — | — |
| **CREASTMT** (CBSTM03A) | — | — | — | — | — | — | — | — | ✓ | — |
| **TRANREPT** (CBTRN03C) | — | — | — | — | — | — | — | — | — | — |
| **Card Update** (COCRDUPC) | ✓ | — | — | ✓ | — | — | — | — | — | — |
| **Account Update** (COACTUPC) | ✓ | — | ✓ | — | — | — | — | — | — | — |
| **Data Migration** (Phase 6) | ✓ | ✓ | ✓ | ✓ | — | ✓ | — | — | — | ✓ |
| **Nightly (dual-write)** | ✓ | — | ✓ | ✓ | — | — | — | — | — | — |
| **Weekly** | ✓ | ✓ | ✓ | ✓ | — | ✓ | — | — | — | — |

### Job-Specific Validation Details

#### POSTTRAN (CBTRN02C)

```
Pre-run:
  1. RC-05: Validate daily transaction card numbers
  2. Snapshot ACCTDATA balances and TCATBALF balances

Post-run:
  3. RC-07: Verify posted + rejected = input count
  4. RC-02: Verify category balances equal account balances
  5. Compare COBOL output vs Java output (differential test)
```

#### INTCALC (CBACT04C)

```
Pre-run:
  1. Snapshot ACCTDATA balances

Post-run:
  2. RC-08: Verify balance changes match interest postings
  3. RC-02: Re-verify category balance consistency
  4. Compare COBOL interest amounts vs Java interest amounts
```

#### Data Migration (Phase 6)

```
1. RC-10: Record count parity for all 10 datasets
2. RC-01: Xref integrity in PostgreSQL
3. RC-02: Balance consistency in PostgreSQL
4. RC-03: Account-card coverage in PostgreSQL
5. RC-04: Card-account validity in PostgreSQL
6. RC-06: Disclosure group coverage in PostgreSQL
7. Field-by-field comparison (comparator.py) for all datasets
```

---

## 14. Execution Schedule

### During Development (Phases 0–3)

| Frequency | Checks | Data Source |
|-----------|--------|-------------|
| Every CI build | RC-01, RC-03, RC-04, RC-05 | Golden files |
| Nightly (dual-write) | RC-01, RC-03, RC-04 | PostgreSQL export |

### During Parallel Run (Phase 4)

| Frequency | Checks | Data Source |
|-----------|--------|-------------|
| Every batch run | RC-02, RC-05 (pre), RC-07, RC-08 | COBOL output + Java output |
| Daily | RC-01, RC-03, RC-04 | PostgreSQL + VSAM |
| Per billing cycle | All RC-01 through RC-09 | Full comparison |

### During Data Migration (Phase 6)

| Frequency | Checks | Data Source |
|-----------|--------|-------------|
| Per migration run | RC-10 | VSAM export + PostgreSQL |
| Post-migration | RC-01 through RC-06 | PostgreSQL |
| Daily for 30 days | All checks | PostgreSQL (production) |

### CLI Usage

```bash
# Run all checks against golden files
python test-harness/reconciliation.py --data-dir golden-files/

# Run specific checks
python test-harness/reconciliation.py --data-dir golden-files/ --checks RC-01 RC-02

# Write JSON report
python test-harness/reconciliation.py --data-dir golden-files/ --report report.json
```

---

*Cross-references: [TEST_STRATEGY.md](TEST_STRATEGY.md) | [CUTOVER_PLAN.md](CUTOVER_PLAN.md) | [RISK_REGISTER.md](RISK_REGISTER.md)*
