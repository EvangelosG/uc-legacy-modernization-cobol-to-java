"""Tests for the reconciliation checks."""

import json
import sys
from decimal import Decimal
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from reconciliation import (
    rc01_xref_integrity,
    rc02_balance_consistency,
    rc03_account_card_coverage,
    rc04_card_account_validity,
    rc05_transaction_completeness,
    rc06_disclosure_group_coverage,
    rc07_transaction_posting_completeness,
    rc08_interest_calc_integrity,
    run_all_checks,
)

GOLDEN_DIR = Path(__file__).resolve().parent.parent.parent / "golden-files"


def _write_json(path: Path, records: list, metadata: dict | None = None) -> None:
    """Helper to write a golden-file JSON."""
    data = {"metadata": metadata or {}, "records": records}
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, default=str)


class TestRC01XrefIntegrity:
    def test_passes_on_golden_data(self):
        result = rc01_xref_integrity(GOLDEN_DIR)
        assert result.status == "PASS"
        assert result.records_checked == 50
        assert result.violation_count == 0

    def test_detects_orphan_account(self, tmp_path):
        _write_json(tmp_path / "cardxref.json", [
            {"XREF-CARD-NUM": "1234567890123456", "XREF-CUST-ID": "000000001", "XREF-ACCT-ID": "99999999999"},
        ])
        _write_json(tmp_path / "acctdata.json", [
            {"ACCT-ID": "00000000001"},
        ])
        _write_json(tmp_path / "custdata.json", [
            {"CUST-ID": "000000001"},
        ])
        _write_json(tmp_path / "carddata.json", [
            {"CARD-NUM": "1234567890123456"},
        ])
        result = rc01_xref_integrity(tmp_path)
        assert result.status == "FAIL"
        assert any("99999999999" in v.details for v in result.violations)

    def test_detects_orphan_customer(self, tmp_path):
        _write_json(tmp_path / "cardxref.json", [
            {"XREF-CARD-NUM": "1234567890123456", "XREF-CUST-ID": "999999999", "XREF-ACCT-ID": "00000000001"},
        ])
        _write_json(tmp_path / "acctdata.json", [{"ACCT-ID": "00000000001"}])
        _write_json(tmp_path / "custdata.json", [{"CUST-ID": "000000001"}])
        _write_json(tmp_path / "carddata.json", [{"CARD-NUM": "1234567890123456"}])
        result = rc01_xref_integrity(tmp_path)
        assert result.status == "FAIL"
        assert any("999999999" in v.details for v in result.violations)

    def test_skip_when_files_missing(self, tmp_path):
        result = rc01_xref_integrity(tmp_path)
        assert result.status == "SKIP"


class TestRC02BalanceConsistency:
    def test_passes_when_balanced(self, tmp_path):
        _write_json(tmp_path / "acctdata.json", [
            {"ACCT-ID": "00000000001", "ACCT-CURR-BAL": 300.0},
        ])
        _write_json(tmp_path / "tcatbal.json", [
            {"TRANCAT-ACCT-ID": "00000000001", "TRANCAT-TYPE-CD": "01", "TRANCAT-CD": "0001", "TRAN-CAT-BAL": 200.0},
            {"TRANCAT-ACCT-ID": "00000000001", "TRANCAT-TYPE-CD": "02", "TRANCAT-CD": "0001", "TRAN-CAT-BAL": 100.0},
        ])
        result = rc02_balance_consistency(tmp_path)
        assert result.status == "PASS"

    def test_fails_when_imbalanced(self, tmp_path):
        _write_json(tmp_path / "acctdata.json", [
            {"ACCT-ID": "00000000001", "ACCT-CURR-BAL": 300.0},
        ])
        _write_json(tmp_path / "tcatbal.json", [
            {"TRANCAT-ACCT-ID": "00000000001", "TRANCAT-TYPE-CD": "01", "TRANCAT-CD": "0001", "TRAN-CAT-BAL": 100.0},
        ])
        result = rc02_balance_consistency(tmp_path)
        assert result.status == "FAIL"
        assert result.violation_count == 1

    def test_golden_data_pre_batch(self):
        result = rc02_balance_consistency(GOLDEN_DIR)
        # Pre-batch data: category balances are all zero, account balances are not
        assert result.status == "FAIL"
        assert result.violation_count == 50


class TestRC03AccountCardCoverage:
    def test_passes_on_golden_data(self):
        result = rc03_account_card_coverage(GOLDEN_DIR)
        assert result.status == "PASS"

    def test_detects_uncovered_account(self, tmp_path):
        _write_json(tmp_path / "acctdata.json", [
            {"ACCT-ID": "00000000001"},
            {"ACCT-ID": "00000000002"},
        ])
        _write_json(tmp_path / "cardxref.json", [
            {"XREF-CARD-NUM": "1234", "XREF-CUST-ID": "1", "XREF-ACCT-ID": "00000000001"},
        ])
        result = rc03_account_card_coverage(tmp_path)
        assert result.status == "FAIL"
        assert result.violation_count == 1


class TestRC04CardAccountValidity:
    def test_passes_on_golden_data(self):
        result = rc04_card_account_validity(GOLDEN_DIR)
        assert result.status == "PASS"

    def test_detects_invalid_account_ref(self, tmp_path):
        _write_json(tmp_path / "carddata.json", [
            {"CARD-NUM": "1234567890123456", "CARD-ACCT-ID": "99999999999"},
        ])
        _write_json(tmp_path / "acctdata.json", [
            {"ACCT-ID": "00000000001"},
        ])
        result = rc04_card_account_validity(tmp_path)
        assert result.status == "FAIL"


class TestRC05TransactionCompleteness:
    def test_passes_on_golden_data(self):
        result = rc05_transaction_completeness(GOLDEN_DIR)
        assert result.status == "PASS"
        assert result.records_checked == 300

    def test_detects_invalid_card(self, tmp_path):
        _write_json(tmp_path / "dailytran.json", [
            {"DALYTRAN-ID": "0001", "DALYTRAN-CARD-NUM": "9999999999999999"},
        ])
        _write_json(tmp_path / "cardxref.json", [
            {"XREF-CARD-NUM": "1234567890123456", "XREF-CUST-ID": "1", "XREF-ACCT-ID": "1"},
        ])
        result = rc05_transaction_completeness(tmp_path)
        assert result.status == "FAIL"


class TestRC06DisclosureGroupCoverage:
    def test_detects_missing_group(self, tmp_path):
        _write_json(tmp_path / "acctdata.json", [
            {"ACCT-ID": "001", "ACCT-GROUP-ID": "GROUP_A"},
        ])
        _write_json(tmp_path / "discgrp.json", [
            {"DIS-ACCT-GROUP-ID": "GROUP_B", "DIS-TRAN-TYPE-CD": "01",
             "DIS-TRAN-CAT-CD": "0001", "DIS-INT-RATE": 15.0},
        ])
        result = rc06_disclosure_group_coverage(tmp_path)
        assert result.status == "FAIL"
        assert result.violation_count == 1


class TestRC07TransactionPostingCompleteness:
    def test_skips_without_output_files(self):
        result = rc07_transaction_posting_completeness(GOLDEN_DIR)
        # golden-files/ has dailytran.json but no transact.json or dalyrejs.json
        assert result.status == "SKIP"

    def test_all_posted(self, tmp_path):
        _write_json(tmp_path / "dailytran.json", [
            {"DALYTRAN-ID": "T001", "DALYTRAN-AMT": 100.00},
            {"DALYTRAN-ID": "T002", "DALYTRAN-AMT": 200.00},
        ])
        _write_json(tmp_path / "transact.json", [
            {"TRAN-ID": "T001", "TRAN-AMT": 100.00},
            {"TRAN-ID": "T002", "TRAN-AMT": 200.00},
        ])
        result = rc07_transaction_posting_completeness(tmp_path)
        assert result.status == "PASS"
        assert result.records_checked == 2

    def test_mixed_posted_and_rejected(self, tmp_path):
        _write_json(tmp_path / "dailytran.json", [
            {"DALYTRAN-ID": "T001", "DALYTRAN-AMT": 100.00},
            {"DALYTRAN-ID": "T002", "DALYTRAN-AMT": 200.00},
            {"DALYTRAN-ID": "T003", "DALYTRAN-AMT": 50.00},
        ])
        _write_json(tmp_path / "transact.json", [
            {"TRAN-ID": "T001", "TRAN-AMT": 100.00},
            {"TRAN-ID": "T002", "TRAN-AMT": 200.00},
        ])
        _write_json(tmp_path / "dalyrejs.json", [
            {"DALYTRAN-ID": "T003", "DALYTRAN-AMT": 50.00},
        ])
        result = rc07_transaction_posting_completeness(tmp_path)
        assert result.status == "PASS"

    def test_detects_dropped_transaction(self, tmp_path):
        _write_json(tmp_path / "dailytran.json", [
            {"DALYTRAN-ID": "T001", "DALYTRAN-AMT": 100.00},
            {"DALYTRAN-ID": "T002", "DALYTRAN-AMT": 200.00},
        ])
        _write_json(tmp_path / "transact.json", [
            {"TRAN-ID": "T001", "TRAN-AMT": 100.00},
        ])
        result = rc07_transaction_posting_completeness(tmp_path)
        assert result.status == "FAIL"
        # Should detect T002 missing AND count mismatch
        assert result.violation_count >= 1

    def test_detects_duplicate_in_both(self, tmp_path):
        _write_json(tmp_path / "dailytran.json", [
            {"DALYTRAN-ID": "T001", "DALYTRAN-AMT": 100.00},
        ])
        _write_json(tmp_path / "transact.json", [
            {"TRAN-ID": "T001", "TRAN-AMT": 100.00},
        ])
        _write_json(tmp_path / "dalyrejs.json", [
            {"DALYTRAN-ID": "T001", "DALYTRAN-AMT": 100.00},
        ])
        result = rc07_transaction_posting_completeness(tmp_path)
        assert result.status == "FAIL"
        dup_violations = [v for v in result.violations
                          if "both" in v.details.lower()]
        assert len(dup_violations) == 1

    def test_detects_amount_mismatch(self, tmp_path):
        _write_json(tmp_path / "dailytran.json", [
            {"DALYTRAN-ID": "T001", "DALYTRAN-AMT": 100.00},
            {"DALYTRAN-ID": "T002", "DALYTRAN-AMT": 200.00},
        ])
        _write_json(tmp_path / "transact.json", [
            {"TRAN-ID": "T001", "TRAN-AMT": 100.00},
            {"TRAN-ID": "T002", "TRAN-AMT": 150.00},  # wrong amount
        ])
        result = rc07_transaction_posting_completeness(tmp_path)
        assert result.status == "FAIL"
        amount_violations = [v for v in result.violations
                             if "AMOUNT_SUM" in v.record_key]
        assert len(amount_violations) == 1


class TestRC08InterestCalcIntegrity:
    def test_skips_without_acctdata(self, tmp_path):
        result = rc08_interest_calc_integrity(tmp_path)
        assert result.status == "SKIP"

    def test_validates_rate_reasonability(self):
        result = rc08_interest_calc_integrity(GOLDEN_DIR)
        assert result.status == "PASS"

    def test_detects_unreasonable_rate(self, tmp_path):
        _write_json(tmp_path / "acctdata.json", [
            {"ACCT-ID": "001", "ACCT-CURR-BAL": 1000.00,
             "ACCT-GROUP-ID": "GRP1"},
        ])
        _write_json(tmp_path / "discgrp.json", [
            {"DIS-ACCT-GROUP-ID": "GRP1", "DIS-INT-RATE": 150.0},
        ])
        result = rc08_interest_calc_integrity(tmp_path)
        assert result.status == "FAIL"

    def test_validates_interest_account_refs(self, tmp_path):
        _write_json(tmp_path / "acctdata.json", [
            {"ACCT-ID": "001", "ACCT-CURR-BAL": 1000.00},
        ])
        _write_json(tmp_path / "systran.json", [
            {"ACCT-ID": "999", "TRAN-AMT": 15.00},
        ])
        result = rc08_interest_calc_integrity(tmp_path)
        assert result.status == "FAIL"
        assert any("nonexistent" in v.details for v in result.violations)

    def test_validates_balance_change_vs_interest(self, tmp_path):
        # Post-run data
        _write_json(tmp_path / "acctdata.json", [
            {"ACCT-ID": "001", "ACCT-CURR-BAL": 1015.00},
        ])
        _write_json(tmp_path / "systran.json", [
            {"ACCT-ID": "001", "TRAN-AMT": 15.00},
        ])
        # Pre-run snapshot
        pre_dir = tmp_path / "pre"
        pre_dir.mkdir()
        _write_json(pre_dir / "acctdata.json", [
            {"ACCT-ID": "001", "ACCT-CURR-BAL": 1000.00},
        ])
        result = rc08_interest_calc_integrity(tmp_path, pre_dir=pre_dir)
        assert result.status == "PASS"

    def test_detects_balance_interest_discrepancy(self, tmp_path):
        _write_json(tmp_path / "acctdata.json", [
            {"ACCT-ID": "001", "ACCT-CURR-BAL": 1020.00},
        ])
        _write_json(tmp_path / "systran.json", [
            {"ACCT-ID": "001", "TRAN-AMT": 15.00},
        ])
        pre_dir = tmp_path / "pre"
        pre_dir.mkdir()
        _write_json(pre_dir / "acctdata.json", [
            {"ACCT-ID": "001", "ACCT-CURR-BAL": 1000.00},
        ])
        result = rc08_interest_calc_integrity(tmp_path, pre_dir=pre_dir)
        assert result.status == "FAIL"
        assert any("Balance change" in v.details for v in result.violations)


class TestRunAllChecks:
    def test_runs_all_eight_checks(self):
        results = run_all_checks(GOLDEN_DIR)
        assert len(results) == 8
        check_ids = {r.check_id for r in results}
        assert check_ids == {
            "RC-01", "RC-02", "RC-03", "RC-04",
            "RC-05", "RC-06", "RC-07", "RC-08",
        }
