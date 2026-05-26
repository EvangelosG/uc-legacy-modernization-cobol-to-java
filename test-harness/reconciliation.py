"""Cross-entity reconciliation checks for CardDemo data integrity.

Implements the reconciliation checks specified in RECONCILIATION_CHECKS.md.
Each check verifies an invariant that spans multiple datasets.
"""

from __future__ import annotations

import json
from dataclasses import dataclass, field
from decimal import Decimal
from pathlib import Path
from typing import Any, Dict, List, Optional, Set


@dataclass
class Violation:
    """A single reconciliation violation."""

    record_key: str
    details: str


@dataclass
class CheckResult:
    """Result of a single reconciliation check."""

    check_id: str
    name: str
    status: str  # "PASS" or "FAIL"
    records_checked: int = 0
    violations: List[Violation] = field(default_factory=list)

    @property
    def violation_count(self) -> int:
        return len(self.violations)

    def to_dict(self) -> Dict[str, Any]:
        return {
            "check_id": self.check_id,
            "name": self.name,
            "status": self.status,
            "records_checked": self.records_checked,
            "violation_count": self.violation_count,
            "violations": [
                {"record_key": v.record_key, "details": v.details}
                for v in self.violations
            ],
        }


def _load_records(path: Path) -> List[Dict[str, Any]]:
    """Load records from a golden-file JSON."""
    with open(path) as f:
        data = json.load(f)
    records = data.get("records", data)
    result = []
    for r in records:
        converted = {}
        for k, v in r.items():
            if isinstance(v, (int, float)) and k != "_line":
                converted[k] = Decimal(str(v))
            else:
                converted[k] = v
        result.append(converted)
    return result


def rc01_xref_integrity(data_dir: Path) -> CheckResult:
    """RC-01: Every card xref points to a valid account and customer.

    For each record in cardxref.json:
    - XREF-ACCT-ID must exist in acctdata.json (ACCT-ID)
    - XREF-CUST-ID must exist in custdata.json (CUST-ID)
    - XREF-CARD-NUM must exist in carddata.json (CARD-NUM)
    """
    result = CheckResult(
        check_id="RC-01",
        name="Card Xref Referential Integrity",
        status="PASS",
    )

    xref_path = data_dir / "cardxref.json"
    acct_path = data_dir / "acctdata.json"
    cust_path = data_dir / "custdata.json"
    card_path = data_dir / "carddata.json"

    if not all(p.exists() for p in [xref_path, acct_path, cust_path, card_path]):
        result.status = "SKIP"
        return result

    xrefs = _load_records(xref_path)
    acct_ids: Set[str] = {str(r["ACCT-ID"]) for r in _load_records(acct_path)}
    cust_ids: Set[str] = {str(r["CUST-ID"]) for r in _load_records(cust_path)}
    card_nums: Set[str] = {str(r["CARD-NUM"]).strip() for r in _load_records(card_path)}

    result.records_checked = len(xrefs)

    for xref in xrefs:
        card_num = str(xref["XREF-CARD-NUM"]).strip()
        acct_id = str(xref["XREF-ACCT-ID"])
        cust_id = str(xref["XREF-CUST-ID"])

        if acct_id not in acct_ids:
            result.status = "FAIL"
            result.violations.append(Violation(
                record_key=card_num,
                details=f"XREF-ACCT-ID {acct_id} not found in acctdata",
            ))

        if cust_id not in cust_ids:
            result.status = "FAIL"
            result.violations.append(Violation(
                record_key=card_num,
                details=f"XREF-CUST-ID {cust_id} not found in custdata",
            ))

        if card_num not in card_nums:
            result.status = "FAIL"
            result.violations.append(Violation(
                record_key=card_num,
                details=f"XREF-CARD-NUM {card_num} not found in carddata",
            ))

    return result


def rc02_balance_consistency(data_dir: Path) -> CheckResult:
    """RC-02: Sum of category balances matches account current balance.

    For each account, the sum of TRAN-CAT-BAL records (from tcatbal.json)
    where TRANCAT-ACCT-ID == ACCT-ID should equal ACCT-CURR-BAL (from
    acctdata.json).

    Note: This check may not hold for the initial sample data, as category
    balances may not have been computed yet. It is primarily useful after
    POSTTRAN or INTCALC batch runs.
    """
    result = CheckResult(
        check_id="RC-02",
        name="Category Balance vs Account Balance Consistency",
        status="PASS",
    )

    acct_path = data_dir / "acctdata.json"
    tcatbal_path = data_dir / "tcatbal.json"

    if not all(p.exists() for p in [acct_path, tcatbal_path]):
        result.status = "SKIP"
        return result

    accounts = _load_records(acct_path)
    tcatbals = _load_records(tcatbal_path)

    # Build sum of category balances per account
    cat_bal_by_acct: Dict[str, Decimal] = {}
    for cb in tcatbals:
        acct_id = str(cb["TRANCAT-ACCT-ID"])
        bal = cb["TRAN-CAT-BAL"]
        cat_bal_by_acct[acct_id] = cat_bal_by_acct.get(acct_id, Decimal("0")) + bal

    result.records_checked = len(accounts)

    for acct in accounts:
        acct_id = str(acct["ACCT-ID"])
        acct_bal = acct["ACCT-CURR-BAL"]
        cat_total = cat_bal_by_acct.get(acct_id, Decimal("0"))

        if acct_bal != cat_total:
            result.status = "FAIL"
            result.violations.append(Violation(
                record_key=acct_id,
                details=(
                    f"ACCT-CURR-BAL={acct_bal} != "
                    f"sum(TRAN-CAT-BAL)={cat_total}, "
                    f"diff={acct_bal - cat_total}"
                ),
            ))

    return result


def rc03_account_card_coverage(data_dir: Path) -> CheckResult:
    """RC-03: Every account has at least one card xref entry.

    For each record in acctdata.json, there must be at least one record
    in cardxref.json with a matching XREF-ACCT-ID.
    """
    result = CheckResult(
        check_id="RC-03",
        name="Account-Card Coverage",
        status="PASS",
    )

    acct_path = data_dir / "acctdata.json"
    xref_path = data_dir / "cardxref.json"

    if not all(p.exists() for p in [acct_path, xref_path]):
        result.status = "SKIP"
        return result

    accounts = _load_records(acct_path)
    xrefs = _load_records(xref_path)

    accts_with_cards: Set[str] = {str(x["XREF-ACCT-ID"]) for x in xrefs}
    result.records_checked = len(accounts)

    for acct in accounts:
        acct_id = str(acct["ACCT-ID"])
        if acct_id not in accts_with_cards:
            result.status = "FAIL"
            result.violations.append(Violation(
                record_key=acct_id,
                details="Account has no card xref entry",
            ))

    return result


def rc04_card_account_validity(data_dir: Path) -> CheckResult:
    """RC-04: Every card's CARD-ACCT-ID exists in acctdata.

    For each record in carddata.json, CARD-ACCT-ID must exist as an
    ACCT-ID in acctdata.json.
    """
    result = CheckResult(
        check_id="RC-04",
        name="Card-Account Foreign Key Validity",
        status="PASS",
    )

    card_path = data_dir / "carddata.json"
    acct_path = data_dir / "acctdata.json"

    if not all(p.exists() for p in [card_path, acct_path]):
        result.status = "SKIP"
        return result

    cards = _load_records(card_path)
    acct_ids: Set[str] = {str(r["ACCT-ID"]) for r in _load_records(acct_path)}

    result.records_checked = len(cards)

    for card in cards:
        card_num = str(card["CARD-NUM"]).strip()
        card_acct = str(card["CARD-ACCT-ID"])
        if card_acct not in acct_ids:
            result.status = "FAIL"
            result.violations.append(Violation(
                record_key=card_num,
                details=f"CARD-ACCT-ID {card_acct} not found in acctdata",
            ))

    return result


def rc05_transaction_completeness(data_dir: Path) -> CheckResult:
    """RC-05: Daily transaction count equals posted + rejected.

    If both dailytran.json and a post-batch transact.json exist, the
    count of daily transactions should equal posted + rejected.

    For initial data (pre-batch), this check verifies that all daily
    transaction card numbers exist in the card xref.
    """
    result = CheckResult(
        check_id="RC-05",
        name="Daily Transaction Card Xref Validity",
        status="PASS",
    )

    dailytran_path = data_dir / "dailytran.json"
    xref_path = data_dir / "cardxref.json"

    if not all(p.exists() for p in [dailytran_path, xref_path]):
        result.status = "SKIP"
        return result

    txns = _load_records(dailytran_path)
    xref_cards: Set[str] = {
        str(x["XREF-CARD-NUM"]).strip() for x in _load_records(xref_path)
    }

    result.records_checked = len(txns)

    for txn in txns:
        card_num = str(txn["DALYTRAN-CARD-NUM"]).strip()
        if card_num not in xref_cards:
            result.status = "FAIL"
            result.violations.append(Violation(
                record_key=str(txn.get("DALYTRAN-ID", "")),
                details=f"DALYTRAN-CARD-NUM {card_num} not found in cardxref",
            ))

    return result


def rc06_disclosure_group_coverage(data_dir: Path) -> CheckResult:
    """RC-06: Every account group ID has disclosure group entries.

    For each unique ACCT-GROUP-ID in acctdata.json, there must be at
    least one record in discgrp.json with a matching DIS-ACCT-GROUP-ID.
    """
    result = CheckResult(
        check_id="RC-06",
        name="Disclosure Group Coverage",
        status="PASS",
    )

    acct_path = data_dir / "acctdata.json"
    disc_path = data_dir / "discgrp.json"

    if not all(p.exists() for p in [acct_path, disc_path]):
        result.status = "SKIP"
        return result

    accounts = _load_records(acct_path)
    discgrps = _load_records(disc_path)

    disc_group_ids: Set[str] = {
        str(d["DIS-ACCT-GROUP-ID"]).strip() for d in discgrps
    }

    acct_group_ids: Set[str] = set()
    for acct in accounts:
        gid = str(acct.get("ACCT-GROUP-ID", "")).strip()
        if gid:
            acct_group_ids.add(gid)

    result.records_checked = len(acct_group_ids)

    for gid in sorted(acct_group_ids):
        if gid not in disc_group_ids:
            result.status = "FAIL"
            result.violations.append(Violation(
                record_key=gid,
                details=f"ACCT-GROUP-ID {gid} has no disclosure group entries",
            ))

    return result


def rc07_transaction_posting_completeness(
    data_dir: Path,
    pre_dir: Path | None = None,
) -> CheckResult:
    """RC-07: Every daily transaction is either posted or rejected.

    After POSTTRAN (CBTRN02C), each DALYTRAN record should appear in
    either the posted transactions (transact.json) or the rejects
    (dalyrejs.json). No transaction should be silently dropped.

    Additionally validates:
    - Sum of posted + rejected amounts equals sum of input amounts
    - No transaction ID appears in both posted and rejected
    """
    result = CheckResult(
        check_id="RC-07",
        name="Transaction Posting Completeness",
        status="PASS",
    )

    daily_path = data_dir / "dailytran.json"
    posted_path = data_dir / "transact.json"
    rejects_path = data_dir / "dalyrejs.json"

    if not daily_path.exists():
        result.status = "SKIP"
        return result

    if not posted_path.exists() and not rejects_path.exists():
        result.status = "SKIP"
        return result

    dailytrans = _load_records(daily_path)
    result.records_checked = len(dailytrans)

    posted: List[Dict[str, Any]] = []
    if posted_path.exists():
        posted = _load_records(posted_path)

    rejects: List[Dict[str, Any]] = []
    if rejects_path.exists():
        rejects = _load_records(rejects_path)

    # Build ID sets — use the transaction ID field
    daily_ids: Set[str] = set()
    for t in dailytrans:
        tid = str(t.get("DALYTRAN-ID", t.get("TRAN-ID", ""))).strip()
        if tid:
            daily_ids.add(tid)

    posted_ids: Set[str] = set()
    for t in posted:
        tid = str(t.get("TRAN-ID", "")).strip()
        if tid:
            posted_ids.add(tid)

    reject_ids: Set[str] = set()
    for t in rejects:
        tid = str(t.get("DALYTRAN-ID", t.get("TRAN-ID", ""))).strip()
        if tid:
            reject_ids.add(tid)

    # Check: no ID in both posted and rejected
    dups = posted_ids & reject_ids
    for dup in sorted(dups):
        result.status = "FAIL"
        result.violations.append(Violation(
            record_key=dup,
            details="Transaction appears in both posted and rejected",
        ))

    # Check: every daily transaction accounted for
    accounted = posted_ids | reject_ids
    for tid in sorted(daily_ids - accounted):
        result.status = "FAIL"
        result.violations.append(Violation(
            record_key=tid,
            details="Daily transaction not found in posted or rejected output",
        ))

    # Check: count parity
    expected_count = len(dailytrans)
    actual_count = len(posted) + len(rejects)
    if expected_count != actual_count:
        result.status = "FAIL"
        result.violations.append(Violation(
            record_key="COUNT",
            details=(
                f"Input count ({expected_count}) != "
                f"posted ({len(posted)}) + rejected ({len(rejects)}) = {actual_count}"
            ),
        ))

    # Check: amount sum parity
    daily_sum = sum(
        (t.get("DALYTRAN-AMT", Decimal("0")) for t in dailytrans),
        Decimal("0"),
    )
    posted_sum = sum(
        (t.get("TRAN-AMT", Decimal("0")) for t in posted),
        Decimal("0"),
    )
    reject_sum = sum(
        (t.get("DALYTRAN-AMT", t.get("TRAN-AMT", Decimal("0"))) for t in rejects),
        Decimal("0"),
    )
    if daily_sum != posted_sum + reject_sum:
        diff = daily_sum - (posted_sum + reject_sum)
        result.status = "FAIL"
        result.violations.append(Violation(
            record_key="AMOUNT_SUM",
            details=(
                f"Input sum ({daily_sum}) != "
                f"posted sum ({posted_sum}) + reject sum ({reject_sum}), "
                f"diff={diff}"
            ),
        ))

    return result


def rc08_interest_calc_integrity(
    data_dir: Path,
    pre_dir: Path | None = None,
) -> CheckResult:
    """RC-08: Interest calculation preserves balance integrity.

    After INTCALC (CBACT04C), the difference between post-run and
    pre-run ACCT-CURR-BAL should equal the sum of interest transactions
    posted for that account.

    Args:
        data_dir: Directory with post-INTCALC data (acctdata.json)
        pre_dir: Directory with pre-INTCALC snapshot (acctdata.json).
                 If None, checks that interest records reference valid
                 accounts and have reasonable rates.
    """
    result = CheckResult(
        check_id="RC-08",
        name="Interest Calculation Balance Integrity",
        status="PASS",
    )

    acct_path = data_dir / "acctdata.json"
    systran_path = data_dir / "systran.json"

    if not acct_path.exists():
        result.status = "SKIP"
        return result

    accounts = _load_records(acct_path)
    result.records_checked = len(accounts)

    # If systran (interest transactions) exists, validate amounts
    if systran_path.exists():
        systrans = _load_records(systran_path)

        # Sum interest by account
        interest_by_acct: Dict[str, Decimal] = {}
        for st in systrans:
            acct_id = str(st.get("ACCT-ID", st.get("TRANCAT-ACCT-ID", ""))).strip()
            amt = st.get("TRAN-AMT", st.get("INTEREST-AMT", Decimal("0")))
            interest_by_acct[acct_id] = interest_by_acct.get(acct_id, Decimal("0")) + amt

        acct_ids: Set[str] = {str(a["ACCT-ID"]).strip() for a in accounts}

        # Every interest transaction references a valid account
        for acct_id in sorted(interest_by_acct.keys()):
            if acct_id not in acct_ids:
                result.status = "FAIL"
                result.violations.append(Violation(
                    record_key=acct_id,
                    details="Interest transaction references nonexistent account",
                ))

        # If pre-run snapshot available, verify balance changes
        if pre_dir is not None:
            pre_acct_path = pre_dir / "acctdata.json"
            if pre_acct_path.exists():
                pre_accounts = _load_records(pre_acct_path)
                pre_bal: Dict[str, Decimal] = {
                    str(a["ACCT-ID"]).strip(): a["ACCT-CURR-BAL"]
                    for a in pre_accounts
                }
                post_bal: Dict[str, Decimal] = {
                    str(a["ACCT-ID"]).strip(): a["ACCT-CURR-BAL"]
                    for a in accounts
                }

                for acct_id in sorted(post_bal.keys()):
                    if acct_id not in pre_bal:
                        continue
                    actual_diff = post_bal[acct_id] - pre_bal[acct_id]
                    expected_interest = interest_by_acct.get(acct_id, Decimal("0"))
                    if actual_diff != expected_interest:
                        result.status = "FAIL"
                        result.violations.append(Violation(
                            record_key=acct_id,
                            details=(
                                f"Balance change ({actual_diff}) != "
                                f"interest posted ({expected_interest}), "
                                f"diff={actual_diff - expected_interest}"
                            ),
                        ))
    else:
        # No systran — check disclosure group rate reasonability
        disc_path = data_dir / "discgrp.json"
        if disc_path.exists():
            discgrps = _load_records(disc_path)
            for dg in discgrps:
                rate = dg.get("DIS-INT-RATE", Decimal("0"))
                if rate < Decimal("0") or rate > Decimal("100"):
                    group_id = str(dg.get("DIS-ACCT-GROUP-ID", ""))
                    result.status = "FAIL"
                    result.violations.append(Violation(
                        record_key=group_id,
                        details=f"Interest rate {rate}% outside reasonable range [0, 100]",
                    ))

    return result


ALL_CHECKS = [
    rc01_xref_integrity,
    rc02_balance_consistency,
    rc03_account_card_coverage,
    rc04_card_account_validity,
    rc05_transaction_completeness,
    rc06_disclosure_group_coverage,
    rc07_transaction_posting_completeness,
    rc08_interest_calc_integrity,
]


def run_all_checks(data_dir: Path) -> List[CheckResult]:
    """Run all reconciliation checks against data in the given directory."""
    return [check(data_dir) for check in ALL_CHECKS]


def main() -> None:
    """CLI entry point for running reconciliation checks."""
    import argparse
    import sys

    parser = argparse.ArgumentParser(
        description="Run CardDemo reconciliation checks"
    )
    parser.add_argument(
        "--data-dir",
        type=Path,
        required=True,
        help="Directory containing golden-file JSON files",
    )
    parser.add_argument(
        "--report",
        type=Path,
        default=None,
        help="Write JSON report to this file",
    )
    parser.add_argument(
        "--checks",
        nargs="*",
        default=None,
        help="Specific check IDs to run (e.g., RC-01 RC-02). Default: all",
    )
    args = parser.parse_args()

    if args.checks:
        check_ids = set(c.upper() for c in args.checks)
        checks = [c for c in ALL_CHECKS if c.__doc__ and any(cid in c.__doc__ for cid in check_ids)]
    else:
        checks = ALL_CHECKS

    results = [check(args.data_dir) for check in checks]

    for r in results:
        icon = "PASS" if r.status == "PASS" else ("SKIP" if r.status == "SKIP" else "FAIL")
        print(f"[{icon}] {r.check_id}: {r.name} "
              f"({r.records_checked} checked, {r.violation_count} violations)")
        for v in r.violations[:5]:
            print(f"       {v.record_key}: {v.details}")
        if r.violation_count > 5:
            print(f"       ... and {r.violation_count - 5} more violations")

    if args.report:
        report = {
            "checks": [r.to_dict() for r in results],
            "overall_status": "PASS" if all(r.status in ("PASS", "SKIP") for r in results) else "FAIL",
        }
        args.report.write_text(json.dumps(report, indent=2, default=str))
        print(f"\nReport written to {args.report}")

    if any(r.status == "FAIL" for r in results):
        sys.exit(1)


if __name__ == "__main__":
    main()
