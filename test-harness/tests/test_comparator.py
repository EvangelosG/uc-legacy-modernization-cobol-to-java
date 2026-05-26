"""Tests for the record comparison utilities."""

import sys
from decimal import Decimal
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from comparator import ComparisonResult, FieldMismatch, compare_records


class TestCompareRecordsExactMatch:
    def test_identical_records(self):
        records = [
            {"ACCT-ID": "001", "BAL": Decimal("100.00")},
            {"ACCT-ID": "002", "BAL": Decimal("200.00")},
        ]
        result = compare_records(records, records, key_fields=["ACCT-ID"])
        assert result.status == "PASS"
        assert result.matches == 2
        assert result.mismatch_count == 0

    def test_field_mismatch(self):
        expected = [{"ACCT-ID": "001", "BAL": Decimal("100.00")}]
        actual = [{"ACCT-ID": "001", "BAL": Decimal("100.01")}]
        result = compare_records(expected, actual, key_fields=["ACCT-ID"])
        assert result.status == "FAIL"
        assert result.mismatch_count == 1
        assert result.field_mismatches[0].field_name == "BAL"
        assert result.field_mismatches[0].difference == "0.01"

    def test_missing_in_actual(self):
        expected = [
            {"ACCT-ID": "001", "BAL": Decimal("100.00")},
            {"ACCT-ID": "002", "BAL": Decimal("200.00")},
        ]
        actual = [{"ACCT-ID": "001", "BAL": Decimal("100.00")}]
        result = compare_records(expected, actual, key_fields=["ACCT-ID"])
        assert result.status == "FAIL"
        assert result.expected_only == 1

    def test_extra_in_actual(self):
        expected = [{"ACCT-ID": "001", "BAL": Decimal("100.00")}]
        actual = [
            {"ACCT-ID": "001", "BAL": Decimal("100.00")},
            {"ACCT-ID": "002", "BAL": Decimal("200.00")},
        ]
        result = compare_records(expected, actual, key_fields=["ACCT-ID"])
        assert result.status == "FAIL"
        assert result.actual_only == 1


class TestCompareRecordsWithTolerance:
    def test_within_tolerance(self):
        expected = [{"ACCT-ID": "001", "BAL": Decimal("100.00")}]
        actual = [{"ACCT-ID": "001", "BAL": Decimal("100.004")}]
        result = compare_records(
            expected, actual,
            key_fields=["ACCT-ID"],
            tolerance=Decimal("0.01"),
        )
        assert result.status == "PASS"

    def test_exceeds_tolerance(self):
        expected = [{"ACCT-ID": "001", "BAL": Decimal("100.00")}]
        actual = [{"ACCT-ID": "001", "BAL": Decimal("100.02")}]
        result = compare_records(
            expected, actual,
            key_fields=["ACCT-ID"],
            tolerance=Decimal("0.01"),
        )
        assert result.status == "FAIL"

    def test_zero_tolerance_penny_difference(self):
        expected = [{"ACCT-ID": "001", "BAL": Decimal("1940.00")}]
        actual = [{"ACCT-ID": "001", "BAL": Decimal("1940.01")}]
        result = compare_records(
            expected, actual,
            key_fields=["ACCT-ID"],
            tolerance=Decimal("0"),
        )
        assert result.status == "FAIL"
        assert result.field_mismatches[0].difference == "0.01"


class TestCompareRecordsIgnoreFields:
    def test_ignore_line_number(self):
        expected = [{"ACCT-ID": "001", "_line": 1, "BAL": Decimal("100.00")}]
        actual = [{"ACCT-ID": "001", "_line": 5, "BAL": Decimal("100.00")}]
        result = compare_records(expected, actual, key_fields=["ACCT-ID"])
        assert result.status == "PASS"

    def test_ignore_custom_fields(self):
        expected = [{"ACCT-ID": "001", "TS": "2025-01-01", "BAL": Decimal("100.00")}]
        actual = [{"ACCT-ID": "001", "TS": "2025-01-02", "BAL": Decimal("100.00")}]
        result = compare_records(
            expected, actual,
            key_fields=["ACCT-ID"],
            ignore_fields=["TS"],
        )
        assert result.status == "PASS"


class TestCompareRecordsStringFields:
    def test_string_comparison_trimmed(self):
        expected = [{"ACCT-ID": "001", "NAME": "Alice   "}]
        actual = [{"ACCT-ID": "001", "NAME": "Alice"}]
        result = compare_records(expected, actual, key_fields=["ACCT-ID"])
        assert result.status == "PASS"

    def test_string_mismatch(self):
        expected = [{"ACCT-ID": "001", "NAME": "Alice"}]
        actual = [{"ACCT-ID": "001", "NAME": "Bob"}]
        result = compare_records(expected, actual, key_fields=["ACCT-ID"])
        assert result.status == "FAIL"


class TestCompareRecordsCompoundKeys:
    def test_compound_key(self):
        expected = [
            {"TYPE": "01", "CAT": "0001", "DESC": "Purchase"},
            {"TYPE": "01", "CAT": "0002", "DESC": "Cash Advance"},
        ]
        actual = [
            {"TYPE": "01", "CAT": "0001", "DESC": "Purchase"},
            {"TYPE": "01", "CAT": "0002", "DESC": "Cash Advance"},
        ]
        result = compare_records(
            expected, actual,
            key_fields=["TYPE", "CAT"],
        )
        assert result.status == "PASS"
        assert result.matches == 2


class TestComparisonResultSerialization:
    def test_to_dict(self):
        result = ComparisonResult(
            status="FAIL",
            records_compared=10,
            matches=9,
            field_mismatches=[
                FieldMismatch(
                    record_key="001",
                    field_name="BAL",
                    expected_value=Decimal("100.00"),
                    actual_value=Decimal("100.01"),
                    difference="0.01",
                )
            ],
        )
        d = result.to_dict()
        assert d["status"] == "FAIL"
        assert d["summary"]["mismatches"] == 1
        assert d["mismatches"][0]["expected_value"] == "100.00"
