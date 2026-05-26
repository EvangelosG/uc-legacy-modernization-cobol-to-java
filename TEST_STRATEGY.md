# CardDemo Migration Test Strategy

> **Repository:** `EvangelosG/uc-legacy-modernization-cobol-to-java`
> **Companion Documents:** [RECONCILIATION_CHECKS.md](RECONCILIATION_CHECKS.md) | [MODERNIZATION_BLUEPRINT.md](MODERNIZATION_BLUEPRINT.md) | [CUTOVER_PLAN.md](CUTOVER_PLAN.md)
> **Test Harness:** [`test-harness/`](test-harness/) | **Golden Files:** [`golden-files/`](golden-files/)

---

## Table of Contents

1. [Overview](#1-overview)
2. [Testing Dimensions](#2-testing-dimensions)
3. [Dimension 1: Golden-File Testing](#3-dimension-1-golden-file-testing)
4. [Dimension 2: Differential Testing](#4-dimension-2-differential-testing)
5. [Dimension 3: Reconciliation Testing](#5-dimension-3-reconciliation-testing)
6. [Dimension 4: Contract Testing](#6-dimension-4-contract-testing)
7. [Test Data Management](#7-test-data-management)
8. [Test Harness Architecture](#8-test-harness-architecture)
9. [Coverage Matrix](#9-coverage-matrix)
10. [Test Execution Playbook](#10-test-execution-playbook)

---

## 1. Overview

This test strategy ensures that the Java modernization of CardDemo produces **functionally identical results** to the COBOL system across all data transformations, business rules, and output formats. It is organized around four complementary testing dimensions, each addressing a different failure mode.

### Why Four Dimensions?

| Dimension | What It Catches | When It Runs |
|-----------|----------------|--------------|
| **Golden-File** | Parsing errors, field misalignment, data type conversion bugs | Unit/integration tests — every build |
| **Differential** | Business logic divergence between COBOL and Java | Parallel-run — Phase 4 batch, Phase 2–3 online |
| **Reconciliation** | Cross-entity integrity violations (orphans, balance mismatches, broken xrefs) | Post-migration and post-batch — nightly |
| **Contract** | API shape regressions, DTO drift, breaking changes between bounded contexts | CI pipeline — every PR |

No single dimension is sufficient. Golden-file tests verify parsing but not logic. Differential tests verify logic but not data integrity. Reconciliation catches cross-entity drift. Contract tests prevent API breakage between services.

---

## 2. Testing Dimensions

```
┌─────────────────────────────────────────────────────────────┐
│                    Migration Test Pyramid                    │
│                                                             │
│                     ┌─────────────┐                         │
│                     │ Differential│  ← Phase 4 parallel-run │
│                     │   Testing   │    (batch + online)     │
│                   ┌─┴─────────────┴─┐                       │
│                   │ Reconciliation  │  ← Nightly checks     │
│                   │    Testing      │    (data integrity)   │
│                 ┌─┴─────────────────┴─┐                     │
│                 │   Contract Testing   │  ← Every PR        │
│                 │   (API + DTO shape)  │    (CI pipeline)   │
│               ┌─┴─────────────────────┴─┐                   │
│               │    Golden-File Testing   │  ← Every build   │
│               │   (parsing + field maps) │    (unit tests)  │
│               └─────────────────────────┘                   │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. Dimension 1: Golden-File Testing

### Purpose

Verify that the Java data parsers produce **identical structured output** from COBOL fixed-width data files. Golden files are the "source of truth" — they capture exactly what the COBOL system sees when it reads a VSAM record.

### How It Works

1. **Parse** the ASCII data files (`app/data/ASCII/*.txt`) using the copybook layouts (`app/cpy/*.cpy`) to extract every field.
2. **Produce** structured JSON files (`golden-files/*.json`) with typed field values.
3. **Commit** the golden files to the repo — they are version-controlled references.
4. **Test** the Java parsers by feeding them the same ASCII data and comparing output to golden files.

### Golden File Structure

Each golden file is a JSON array of records. Each record has:
- All named fields from the copybook (FILLER is excluded)
- Numeric fields parsed to their actual value (e.g., `PIC S9(10)V99` → decimal number)
- COBOL sign conventions decoded (`{` = +0, `A` = +1, ..., `}` = -0, `J` = -1, ...)
- Alphanumeric fields trimmed of trailing spaces

```json
{
  "metadata": {
    "source_file": "acctdata.txt",
    "copybook": "CVACT01Y.cpy",
    "record_length": 300,
    "record_count": 50,
    "generated_at": "2025-05-26T07:00:00Z"
  },
  "records": [
    {
      "_line": 1,
      "ACCT-ID": "00000000001",
      "ACCT-ACTIVE-STATUS": "Y",
      "ACCT-CURR-BAL": 1940.00,
      "ACCT-CREDIT-LIMIT": 20200.00,
      ...
    }
  ]
}
```

### Datasets Covered

| Data File | Copybook | Record Length | Records | Key Fields |
|-----------|----------|:------------:|:-------:|------------|
| `acctdata.txt` | CVACT01Y | 300 | 50 | ACCT-ID (key), ACCT-CURR-BAL, ACCT-CREDIT-LIMIT |
| `carddata.txt` | CVACT02Y | 150 | 50 | CARD-NUM (key), CARD-ACCT-ID, CARD-CVV-CD |
| `cardxref.txt` | CVACT03Y | 50 (36 used) | 50 | XREF-CARD-NUM (key), XREF-CUST-ID, XREF-ACCT-ID |
| `custdata.txt` | CVCUS01Y | 500 | 50 | CUST-ID (key), CUST-SSN, CUST-FICO-CREDIT-SCORE |
| `dailytran.txt` | CVTRA06Y | 350 | 300 | DALYTRAN-ID, DALYTRAN-AMT, DALYTRAN-CARD-NUM |
| `discgrp.txt` | CVTRA02Y | 50 | 51 | DIS-ACCT-GROUP-ID + DIS-TRAN-TYPE-CD + DIS-TRAN-CAT-CD (compound key) |
| `tcatbal.txt` | CVTRA01Y | 50 | 50 | TRANCAT-ACCT-ID + TRANCAT-TYPE-CD + TRANCAT-CD (compound key) |
| `trantype.txt` | CVTRA03Y | 60 | 7 | TRAN-TYPE (key), TRAN-TYPE-DESC |
| `trancatg.txt` | CVTRA04Y | 60 | 18 | TRAN-TYPE-CD + TRAN-CAT-CD (compound key), TRAN-CAT-TYPE-DESC |

### Test Cases

For each dataset, the golden-file test verifies:

| Test Case | Assertion |
|-----------|-----------|
| Record count | Parsed record count == golden file record count |
| Field presence | Every named field in copybook appears in parsed output |
| Numeric precision | `PIC S9(10)V99` → `BigDecimal("1940.00")`, not `1940` or `1940.0` |
| Sign decoding | COBOL overpunch sign characters (`{`, `A`–`I`, `}`, `J`–`R`) decoded correctly |
| String trimming | Trailing spaces removed; embedded spaces preserved |
| FILLER exclusion | FILLER bytes consumed but not included in output |
| Round-trip | Golden file → flat-file writer → re-parse → identical golden file |

---

## 4. Dimension 2: Differential Testing

### Purpose

Run both the COBOL and Java implementations on **identical input data** and compare their outputs field-by-field. This is the primary validation mechanism for Phase 4 (batch pipeline) and Phase 2–3 (online screens).

### Batch Differential Tests

```
Input: DALYTRAN.PS (300 daily transactions)
       ├── COBOL: CBTRN01C → CBTRN02C → CBACT04C → CBTRN03C
       └── Java:  TxnInitJob → TxnPostJob → InterestCalcJob → TxnReportJob

Comparison Points:
  1. TRANSACT records:     COBOL VSAM output  vs  Java PostgreSQL output
  2. ACCTDATA balances:    COBOL VSAM output  vs  Java PostgreSQL output
  3. TCATBALF balances:    COBOL VSAM output  vs  Java PostgreSQL output
  4. DALYREJS rejects:     COBOL sequential   vs  Java reject table/file
  5. TRANREPT report:      COBOL report file  vs  Java report file
  6. Interest postings:    COBOL SYSTRAN      vs  Java interest results
```

### Online Differential Tests (Shadow Mode)

During Phases 2–3, the API Gateway runs in shadow mode for migrated endpoints:

1. User request → routed to **Java service** (primary)
2. Same request → replayed to **CICS program** (shadow)
3. Both responses captured and compared field-by-field
4. Any mismatch → alert (T-2.2 trigger from CUTOVER_PLAN.md)

### Comparison Rules

| Field Type | Comparison | Tolerance |
|-----------|------------|-----------|
| Financial amounts (`S9(n)V99`) | BigDecimal equals | **Zero** — must match to the penny |
| Numeric IDs (`9(n)`) | String equals | **Zero** |
| Alphanumeric (`X(n)`) | Trimmed string equals | **Zero** |
| Timestamps (`X(26)`) | ISO-8601 parsed, compared to second | 1 second (clock skew) |
| Record counts | Integer equals | **Zero** |
| Report output | Character-by-character | **Zero** (regulatory) |

### Differential Test Output

```json
{
  "run_id": "diff-2025-05-26-001",
  "input": "dailytran.txt",
  "status": "FAIL",
  "summary": {
    "records_compared": 300,
    "matches": 298,
    "mismatches": 2,
    "cobol_only": 0,
    "java_only": 0
  },
  "mismatches": [
    {
      "record_key": "00000000003",
      "field": "ACCT-CURR-BAL",
      "cobol_value": "1470.01",
      "java_value": "1470.02",
      "difference": "0.01",
      "probable_cause": "Rounding mode mismatch in interest calculation"
    }
  ]
}
```

---

## 5. Dimension 3: Reconciliation Testing

### Purpose

Verify **cross-entity data integrity** after batch processing or data migration. Even if individual records are correct, the relationships between entities can break. Reconciliation tests check invariants that span multiple datasets.

### Reconciliation Check Categories

| Category | What It Validates | Frequency |
|----------|------------------|-----------|
| **Referential Integrity** | Every card xref points to a valid account and customer | After every batch run |
| **Balance Consistency** | Sum of category balances == account current balance | After POSTTRAN, INTCALC |
| **Cross-Entity Counts** | Every account has at least one card; every card has an xref entry | After data migration |
| **Transaction Completeness** | No daily transactions lost during posting (input count == posted + rejected) | After POSTTRAN |
| **Financial Totals** | Sum of all account balances matches independently computed total | After any balance-affecting job |

### Implementation

Reconciliation checks are implemented in `test-harness/reconciliation.py` and specified in [RECONCILIATION_CHECKS.md](RECONCILIATION_CHECKS.md). Each check:
- Has a unique ID (e.g., `RC-01`)
- Specifies input datasets and expected invariant
- Returns PASS/FAIL with details on violations
- Runs against both VSAM-parsed data and PostgreSQL data

---

## 6. Dimension 4: Contract Testing

### Purpose

Verify that the **API contracts** between bounded contexts remain stable as each service evolves independently. Prevents breaking changes when one service updates its DTOs or endpoints.

### Contract Types

| Contract | Provider | Consumer(s) | Verified By |
|----------|----------|-------------|-------------|
| Account API (`AccountSummaryDto`) | BC-2 Account | BC-4 Transaction, BC-5 Financial, BC-6 Billing, BC-8 Auth | Provider-side schema test |
| Card Xref API (`CardXrefDto`) | BC-3 Card | BC-4 Transaction, BC-5 Financial, BC-6 Billing | Provider-side schema test |
| Transaction API (`TransactionDto`) | BC-4 Transaction | BC-6 Billing, BC-7 Reporting | Provider-side schema test |
| Reference Data API (`TransactionTypeDto`) | BC-9 Reference | BC-4 Transaction, BC-7 Reporting | Provider-side schema test |
| Domain Events (`TransactionPostedEvent`, `InterestCalculatedEvent`) | BC-4, BC-5 | BC-2 Account | Event schema registry |

### Contract Test Implementation

```
Provider Service                      Consumer Service
  │                                      │
  ├─ Publishes contract (OpenAPI         ├─ Generates client from
  │  spec or Avro schema)                │  published contract
  │                                      │
  └─ Contract test:                      └─ Consumer test:
     "My API still produces                 "My client still parses
      what I promised"                       what the provider sends"
```

**Tooling Options:**
- **REST APIs:** Spring Cloud Contract or Pact
- **Domain Events:** Avro schemas in a schema registry; backward/forward compatibility checks
- **DTOs:** JSON Schema validation on serialized DTOs

### Contract Verification in CI

Every PR runs:
1. **Provider contract tests** — serialize DTOs, validate against committed schemas
2. **Consumer compatibility tests** — deserialize provider's sample output into consumer DTOs
3. **Event schema compatibility** — check new event schemas against the registry for backward compatibility

---

## 7. Test Data Management

### Golden Reference Data

The `golden-files/` directory contains the canonical JSON representations of all 9 VSAM datasets, parsed from the ASCII data files using copybook layouts. These are **version-controlled** and serve as the baseline for all test dimensions.

| File | Source | Records | Size |
|------|--------|:-------:|------|
| `golden-files/acctdata.json` | `app/data/ASCII/acctdata.txt` | 50 | Account records with decoded financial fields |
| `golden-files/carddata.json` | `app/data/ASCII/carddata.txt` | 50 | Card records |
| `golden-files/cardxref.json` | `app/data/ASCII/cardxref.txt` | 50 | Card-to-account-to-customer cross-references |
| `golden-files/custdata.json` | `app/data/ASCII/custdata.txt` | 50 | Customer records with PII |
| `golden-files/dailytran.json` | `app/data/ASCII/dailytran.txt` | 300 | Daily transaction input records |
| `golden-files/discgrp.json` | `app/data/ASCII/discgrp.txt` | 51 | Disclosure/interest rate groups |
| `golden-files/tcatbal.json` | `app/data/ASCII/tcatbal.txt` | 50 | Transaction category balances |
| `golden-files/trantype.json` | `app/data/ASCII/trantype.txt` | 7 | Transaction type lookup |
| `golden-files/trancatg.json` | `app/data/ASCII/trancatg.txt` | 18 | Transaction category lookup |

### Test Data Refresh Process

1. Modify ASCII data files if needed (e.g., add edge cases)
2. Run `python test-harness/generate_golden_files.py` to regenerate golden files
3. Review diffs — any change to golden files requires investigation
4. Commit updated golden files

### Synthetic Test Data for Edge Cases

Beyond the 50-record sample data, the test harness supports generating synthetic data for:
- **Boundary values:** Maximum `S9(10)V99` (9999999999.99), zero, negative max
- **Sign overpunch edge cases:** All 20 overpunch characters
- **Empty/space-filled fields:** All-spaces in alphanumeric fields
- **Date edge cases:** Leap year dates, century boundaries, future dates
- **Unicode in names:** Extended ASCII characters in customer names (EBCDIC → UTF-8 edge cases)

---

## 8. Test Harness Architecture

### Directory Structure

```
test-harness/
├── __init__.py
├── copybook_parser.py      # Parses COBOL copybook PIC clauses into field definitions
├── record_parser.py        # Reads fixed-width ASCII data using field definitions
├── comparator.py           # Field-by-field comparison with configurable tolerance
├── reconciliation.py       # Cross-entity integrity checks
├── generate_golden_files.py # Generates golden-files/*.json from ASCII data
├── tests/
│   ├── __init__.py
│   ├── test_copybook_parser.py
│   ├── test_record_parser.py
│   ├── test_comparator.py
│   └── test_reconciliation.py
└── README.md
```

### Key Components

| Component | Responsibility | Input | Output |
|-----------|---------------|-------|--------|
| `copybook_parser.py` | Parse COBOL PIC clauses into field definitions (name, type, length, decimals) | `.cpy` file | List of `FieldDef` objects |
| `record_parser.py` | Read fixed-width lines, slice by field offsets, decode values | ASCII data file + field defs | List of `dict` (record objects) |
| `comparator.py` | Compare two record sets field-by-field with configurable tolerance | Two JSON record sets | `ComparisonResult` with matches/mismatches |
| `reconciliation.py` | Run cross-entity integrity checks (xref validity, balance consistency, etc.) | Multiple parsed datasets | List of `CheckResult` (pass/fail per check) |
| `generate_golden_files.py` | Orchestrate parsing of all ASCII files → JSON golden files | All data files + copybooks | `golden-files/*.json` |

---

## 9. Coverage Matrix

### Programs × Test Dimensions

| Program | Golden-File | Differential | Reconciliation | Contract |
|---------|:-----------:|:------------:|:--------------:|:--------:|
| **CBTRN01C** (Txn Init) | ✓ DALYTRAN parse | ✓ Reject count match | ✓ RC-05 Txn completeness | — |
| **CBTRN02C** (Txn Post) | ✓ TRANSACT, ACCTDATA, TCATBALF | ✓ Balance comparison | ✓ RC-01 Xref, RC-02 Balance, RC-05 | ✓ TransactionPostedEvent |
| **CBACT04C** (Interest) | ✓ ACCTDATA, DISCGRP | ✓ Interest amount match | ✓ RC-02 Balance post-interest | ✓ InterestCalculatedEvent |
| **CBTRN03C** (Report) | ✓ TRANREPT output | ✓ Report char-by-char | — | — |
| **CBSTM03A** (Statement) | ✓ Statement output | ✓ Statement content match | — | — |
| **COACTUPC** (Acct Update) | ✓ ACCTDATA, CUSTDATA | ✓ Before/after comparison | ✓ RC-01 Xref integrity | ✓ AccountSummaryDto |
| **COCRDUPC** (Card Update) | ✓ CARDDATA | ✓ Before/after comparison | ✓ RC-01 Xref integrity | ✓ CardDetailDto |
| **COTRN02C** (Online Txn) | ✓ TRANSACT | ✓ Shadow mode comparison | ✓ RC-05 Txn completeness | ✓ TransactionDto |
| **COBIL00C** (Billing) | ✓ ACCTDATA | ✓ Shadow mode comparison | ✓ RC-02 Balance | — |
| **COPAUA0C** (Auth) | — | ✓ Auth decision match | — | ✓ AuthorizationDto |
| **Data Migration** | ✓ All datasets | ✓ VSAM vs PostgreSQL | ✓ All RC checks | — |

### Phase × Test Execution

| Phase | Golden-File | Differential | Reconciliation | Contract |
|-------|:-----------:|:------------:|:--------------:|:--------:|
| 0 (Foundation) | **Build** golden files, parser, comparator | — | — | Define schemas |
| 1 (Identity) | USRSEC parse test | — | — | Auth contract |
| 2 (Read-Only) | All dataset parse tests | Shadow mode (reads) | RC-01, RC-03 | Read API contracts |
| 3 (Data Entry) | Write verification | Shadow mode (writes) | RC-01, RC-02, RC-03 | Write API contracts |
| 4 (Batch) | Pre/post batch parse | **Parallel run** | RC-01–RC-06 (all) | Event contracts |
| 5 (Complex) | COACTUPC field tests | Before/after comparison | RC-01, RC-02 | Account/Auth contracts |
| 6 (Migration) | Full dataset parse | VSAM vs PostgreSQL diff | **All RC checks** | — |

---

## 10. Test Execution Playbook

### Daily (CI Pipeline)

```bash
# 1. Run golden-file tests (< 30 seconds)
python -m pytest test-harness/tests/ -v

# 2. Run reconciliation checks against golden data (< 10 seconds)
python test-harness/reconciliation.py --data-dir golden-files/

# 3. Contract tests (integrated with Spring Boot test suite)
# ./mvnw test -pl contract-tests
```

### Phase 4 Parallel Run (Daily During Parallel-Run Period)

```bash
# 1. Export COBOL batch output to ASCII
#    (IDCAMS REPRO of post-run VSAM files)

# 2. Parse COBOL output to JSON
python test-harness/generate_golden_files.py \
  --data-dir /path/to/cobol-output/ \
  --output-dir /tmp/cobol-results/

# 3. Parse Java batch output to JSON
python test-harness/generate_golden_files.py \
  --data-dir /path/to/java-output/ \
  --output-dir /tmp/java-results/

# 4. Run differential comparison
python test-harness/comparator.py \
  --expected /tmp/cobol-results/ \
  --actual /tmp/java-results/ \
  --tolerance zero \
  --report /tmp/diff-report.json

# 5. Run reconciliation on Java output
python test-harness/reconciliation.py \
  --data-dir /tmp/java-results/ \
  --report /tmp/recon-report.json
```

### Data Migration Validation (Phase 6)

```bash
# 1. Export all VSAM datasets to ASCII
# 2. Export all PostgreSQL tables to JSON

# 3. Run full comparison
python test-harness/comparator.py \
  --expected golden-files/ \
  --actual /tmp/postgresql-export/ \
  --tolerance zero \
  --report /tmp/migration-diff.json

# 4. Run all reconciliation checks
python test-harness/reconciliation.py \
  --data-dir /tmp/postgresql-export/ \
  --checks all \
  --report /tmp/migration-recon.json
```

---

*Cross-references: [RECONCILIATION_CHECKS.md](RECONCILIATION_CHECKS.md) | [CUTOVER_PLAN.md](CUTOVER_PLAN.md) | [RISK_REGISTER.md](RISK_REGISTER.md)*
