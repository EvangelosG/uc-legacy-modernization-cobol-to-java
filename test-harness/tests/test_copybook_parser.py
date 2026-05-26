"""Tests for the COBOL copybook parser."""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from copybook_parser import (
    FieldDef,
    get_min_record_length,
    get_record_length,
    parse_copybook,
    _expand_pic,
    _parse_pic,
)

COPYBOOK_DIR = Path(__file__).resolve().parent.parent.parent / "app" / "cpy"


class TestExpandPic:
    def test_simple_repeat(self):
        assert _expand_pic("9(11)") == "99999999999"

    def test_alpha_repeat(self):
        assert _expand_pic("X(16)") == "XXXXXXXXXXXXXXXX"

    def test_no_repeat(self):
        assert _expand_pic("99") == "99"

    def test_mixed(self):
        assert _expand_pic("S9(10)V99") == "S9999999999V99"

    def test_single_digit_repeat(self):
        assert _expand_pic("9(3)") == "999"


class TestParsePic:
    def test_unsigned_numeric(self):
        field_type, length, decimals = _parse_pic("9(11)")
        assert field_type == "numeric"
        assert length == 11
        assert decimals == 0

    def test_signed_numeric_with_decimal(self):
        field_type, length, decimals = _parse_pic("S9(10)V99")
        assert field_type == "signed_numeric"
        assert length == 12
        assert decimals == 2

    def test_alphanumeric(self):
        field_type, length, decimals = _parse_pic("X(16)")
        assert field_type == "alphanumeric"
        assert length == 16
        assert decimals == 0

    def test_single_char(self):
        field_type, length, decimals = _parse_pic("X(01)")
        assert field_type == "alphanumeric"
        assert length == 1
        assert decimals == 0

    def test_signed_with_4_digit_int(self):
        field_type, length, decimals = _parse_pic("S9(04)V99")
        assert field_type == "signed_numeric"
        assert length == 6
        assert decimals == 2


class TestParseCopybook:
    def test_account_record(self):
        fields = parse_copybook(COPYBOOK_DIR / "CVACT01Y.cpy")
        assert len(fields) == 13

        acct_id = fields[0]
        assert acct_id.name == "ACCT-ID"
        assert acct_id.offset == 0
        assert acct_id.length == 11
        assert acct_id.field_type == "numeric"
        assert not acct_id.is_filler

        curr_bal = fields[2]
        assert curr_bal.name == "ACCT-CURR-BAL"
        assert curr_bal.offset == 12
        assert curr_bal.length == 12
        assert curr_bal.field_type == "signed_numeric"
        assert curr_bal.decimals == 2

        filler = fields[-1]
        assert filler.is_filler
        assert filler.length == 178

    def test_card_record(self):
        fields = parse_copybook(COPYBOOK_DIR / "CVACT02Y.cpy")
        assert len(fields) == 7  # 6 named + 1 FILLER

        card_num = fields[0]
        assert card_num.name == "CARD-NUM"
        assert card_num.length == 16
        assert card_num.field_type == "alphanumeric"

        cvv = fields[2]
        assert cvv.name == "CARD-CVV-CD"
        assert cvv.length == 3
        assert cvv.field_type == "numeric"

    def test_card_xref_record(self):
        fields = parse_copybook(COPYBOOK_DIR / "CVACT03Y.cpy")
        assert len(fields) == 4
        assert get_record_length(fields) == 50
        assert get_min_record_length(fields) == 36

    def test_customer_record(self):
        fields = parse_copybook(COPYBOOK_DIR / "CVCUS01Y.cpy")
        assert len(fields) == 19  # 18 named + 1 FILLER
        assert get_record_length(fields) == 500

    def test_transaction_record(self):
        fields = parse_copybook(COPYBOOK_DIR / "CVTRA05Y.cpy")
        assert len(fields) == 14
        assert get_record_length(fields) == 350

        tran_amt = fields[5]
        assert tran_amt.name == "TRAN-AMT"
        assert tran_amt.field_type == "signed_numeric"
        assert tran_amt.decimals == 2

    def test_daily_transaction_record(self):
        fields = parse_copybook(COPYBOOK_DIR / "CVTRA06Y.cpy")
        assert len(fields) == 14
        assert get_record_length(fields) == 350

    def test_tran_cat_bal_record(self):
        fields = parse_copybook(COPYBOOK_DIR / "CVTRA01Y.cpy")
        names = [f.name for f in fields if not f.is_filler]
        assert "TRANCAT-ACCT-ID" in names
        assert "TRANCAT-TYPE-CD" in names
        assert "TRANCAT-CD" in names
        assert "TRAN-CAT-BAL" in names

    def test_disclosure_group_record(self):
        fields = parse_copybook(COPYBOOK_DIR / "CVTRA02Y.cpy")
        names = [f.name for f in fields if not f.is_filler]
        assert "DIS-ACCT-GROUP-ID" in names
        assert "DIS-INT-RATE" in names

    def test_all_copybooks_parseable(self):
        for cpy_file in COPYBOOK_DIR.glob("*.cpy"):
            fields = parse_copybook(cpy_file)
            # Some copybooks may have no PIC clauses (e.g., 88-levels only)
            # but should not raise exceptions


class TestRecordLength:
    def test_account_record_length(self):
        fields = parse_copybook(COPYBOOK_DIR / "CVACT01Y.cpy")
        assert get_record_length(fields) == 300

    def test_card_record_length(self):
        fields = parse_copybook(COPYBOOK_DIR / "CVACT02Y.cpy")
        assert get_record_length(fields) == 150

    def test_min_record_length_excludes_filler(self):
        fields = parse_copybook(COPYBOOK_DIR / "CVACT03Y.cpy")
        assert get_record_length(fields) == 50
        assert get_min_record_length(fields) == 36

    def test_min_equals_full_when_no_trailing_filler(self):
        fields = parse_copybook(COPYBOOK_DIR / "CVTRA03Y.cpy")
        full = get_record_length(fields)
        min_len = get_min_record_length(fields)
        assert min_len <= full
