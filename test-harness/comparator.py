"""Field-by-field comparison of two record sets with configurable tolerance.

Compares expected (golden/COBOL) records against actual (Java/migrated)
records. Supports zero-tolerance mode for financial fields and configurable
tolerance for timestamps.
"""

from __future__ import annotations

import json
from dataclasses import dataclass, field
from decimal import Decimal
from pathlib import Path
from typing import Any, Dict, List, Optional


@dataclass
class FieldMismatch:
    """A single field-level mismatch between expected and actual."""

    record_key: str
    field_name: str
    expected_value: Any
    actual_value: Any
    difference: Optional[str] = None


@dataclass
class ComparisonResult:
    """Result of comparing two record sets."""

    status: str  # "PASS" or "FAIL"
    records_compared: int = 0
    matches: int = 0
    expected_only: int = 0
    actual_only: int = 0
    field_mismatches: List[FieldMismatch] = field(default_factory=list)

    @property
    def mismatch_count(self) -> int:
        return len(self.field_mismatches)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "status": self.status,
            "summary": {
                "records_compared": self.records_compared,
                "matches": self.matches,
                "mismatches": self.mismatch_count,
                "expected_only": self.expected_only,
                "actual_only": self.actual_only,
            },
            "mismatches": [
                {
                    "record_key": m.record_key,
                    "field": m.field_name,
                    "expected_value": _serialize(m.expected_value),
                    "actual_value": _serialize(m.actual_value),
                    "difference": m.difference,
                }
                for m in self.field_mismatches
            ],
        }


def _serialize(val: Any) -> Any:
    """Convert a value to JSON-serializable form."""
    if isinstance(val, Decimal):
        return str(val)
    return val


def _values_equal(expected: Any, actual: Any, tolerance: Decimal) -> bool:
    """Compare two values with optional tolerance for numeric fields."""
    if isinstance(expected, Decimal) and isinstance(actual, Decimal):
        if tolerance == Decimal("0"):
            return expected == actual
        return abs(expected - actual) <= tolerance
    if isinstance(expected, str) and isinstance(actual, str):
        return expected.rstrip() == actual.rstrip()
    return expected == actual


def compare_records(
    expected: List[Dict[str, Any]],
    actual: List[Dict[str, Any]],
    key_fields: List[str],
    tolerance: Decimal = Decimal("0"),
    ignore_fields: Optional[List[str]] = None,
) -> ComparisonResult:
    """Compare two lists of parsed records field-by-field.

    Args:
        expected: The golden/COBOL records.
        actual: The Java/migrated records.
        key_fields: Fields that uniquely identify a record for matching.
        tolerance: Maximum allowed difference for numeric fields.
            Default is zero (exact match).
        ignore_fields: Fields to skip during comparison (e.g., _line,
            timestamps that are expected to differ).

    Returns:
        ComparisonResult with match/mismatch details.
    """
    ignore = set(ignore_fields or [])
    ignore.add("_line")

    def _make_key(record: Dict[str, Any]) -> str:
        return "|".join(str(record.get(k, "")) for k in key_fields)

    expected_by_key = {_make_key(r): r for r in expected}
    actual_by_key = {_make_key(r): r for r in actual}

    all_keys = set(expected_by_key.keys()) | set(actual_by_key.keys())

    result = ComparisonResult(
        status="PASS",
        records_compared=len(all_keys),
    )

    for key in sorted(all_keys):
        exp = expected_by_key.get(key)
        act = actual_by_key.get(key)

        if exp is None:
            result.actual_only += 1
            result.status = "FAIL"
            continue

        if act is None:
            result.expected_only += 1
            result.status = "FAIL"
            continue

        record_match = True
        all_fields = set(exp.keys()) | set(act.keys())

        for field_name in sorted(all_fields):
            if field_name in ignore:
                continue

            exp_val = exp.get(field_name)
            act_val = act.get(field_name)

            if not _values_equal(exp_val, act_val, tolerance):
                record_match = False
                diff = None
                if isinstance(exp_val, Decimal) and isinstance(act_val, Decimal):
                    diff = str(abs(exp_val - act_val))

                result.field_mismatches.append(FieldMismatch(
                    record_key=key,
                    field_name=field_name,
                    expected_value=exp_val,
                    actual_value=act_val,
                    difference=diff,
                ))

        if record_match:
            result.matches += 1
        else:
            result.status = "FAIL"

    return result


def compare_files(
    expected_path: Path,
    actual_path: Path,
    key_fields: List[str],
    tolerance: Decimal = Decimal("0"),
    ignore_fields: Optional[List[str]] = None,
) -> ComparisonResult:
    """Compare two golden-file JSON files."""
    with open(expected_path) as f:
        expected_data = json.load(f)
    with open(actual_path) as f:
        actual_data = json.load(f)

    expected_records = expected_data.get("records", expected_data)
    actual_records = actual_data.get("records", actual_data)

    # Convert string decimals back to Decimal for comparison
    expected_records = [_deserialize_record(r) for r in expected_records]
    actual_records = [_deserialize_record(r) for r in actual_records]

    return compare_records(
        expected_records,
        actual_records,
        key_fields,
        tolerance,
        ignore_fields,
    )


def _deserialize_record(record: Dict[str, Any]) -> Dict[str, Any]:
    """Convert JSON values back to typed Python values."""
    result = {}
    for k, v in record.items():
        if isinstance(v, (int, float)) and k != "_line":
            result[k] = Decimal(str(v))
        else:
            result[k] = v
    return result
