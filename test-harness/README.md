# CardDemo Migration Test Harness

Python utilities for parsing COBOL fixed-width data files, comparing COBOL vs Java outputs, and running cross-entity reconciliation checks.

## Quick Start

```bash
# Generate golden-file JSON from ASCII data
python test-harness/generate_golden_files.py

# Run reconciliation checks against golden data
python test-harness/reconciliation.py --data-dir golden-files/

# Run test suite
python -m pytest test-harness/tests/ -v
```

## Components

| Module | Purpose |
|--------|---------|
| `copybook_parser.py` | Parse COBOL `.cpy` PIC clauses into field definitions |
| `record_parser.py` | Read fixed-width ASCII data using field definitions |
| `comparator.py` | Field-by-field comparison with configurable tolerance |
| `reconciliation.py` | Cross-entity integrity checks (RC-01 through RC-06) |
| `generate_golden_files.py` | Generate `golden-files/*.json` from `app/data/ASCII/` |

## Supported PIC Types

- `PIC 9(n)` — unsigned numeric → zero-padded string (IDs, codes)
- `PIC S9(n)V99` — signed numeric with implied decimal → `Decimal`
- `PIC X(n)` — alphanumeric → trimmed string
- Trailing overpunch sign decoding (COBOL DISPLAY format)
- FILLER fields consumed but excluded from output

## Companion Documents

- [TEST_STRATEGY.md](../TEST_STRATEGY.md) — four testing dimensions
- [RECONCILIATION_CHECKS.md](../RECONCILIATION_CHECKS.md) — per-job validation specs
