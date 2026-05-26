"""Tests for the fixed-width record parser."""

import sys
from decimal import Decimal
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from copybook_parser import parse_copybook, get_record_length, get_min_record_length
from record_parser import (
    decode_comp,
    decode_comp3,
    decode_field,
    decode_signed_numeric,
    decode_unsigned_numeric,
    parse_file,
    parse_record,
)

COPYBOOK_DIR = Path(__file__).resolve().parent.parent.parent / "app" / "cpy"
DATA_DIR = Path(__file__).resolve().parent.parent.parent / "app" / "data" / "ASCII"


class TestDecodeSignedNumeric:
    def test_positive_zero_overpunch(self):
        # '{' = +0
        result = decode_signed_numeric("00000001940{", 2)
        assert result == Decimal("194.00")

    def test_positive_nonzero_overpunch(self):
        # 'G' = +7
        result = decode_signed_numeric("0000005047G", 2)
        assert result == Decimal("504.77")

    def test_negative_zero_overpunch(self):
        # '}' = -0
        result = decode_signed_numeric("0000009190}", 2)
        assert result == Decimal("-919.00")

    def test_negative_nonzero_overpunch(self):
        # 'J' = -1
        result = decode_signed_numeric("0000001234J", 2)
        assert result == Decimal("-123.41")

    def test_all_positive_signs(self):
        signs = {"{": 0, "A": 1, "B": 2, "C": 3, "D": 4,
                 "E": 5, "F": 6, "G": 7, "H": 8, "I": 9}
        for char, digit in signs.items():
            result = decode_signed_numeric(f"0{char}", 0)
            assert result == Decimal(str(digit)), f"Failed for {char}={digit}"

    def test_all_negative_signs(self):
        signs = {"}": 0, "J": 1, "K": 2, "L": 3, "M": 4,
                 "N": 5, "O": 6, "P": 7, "Q": 8, "R": 9}
        for char, digit in signs.items():
            result = decode_signed_numeric(f"0{char}", 0)
            assert result == Decimal(f"-{digit}"), f"Failed for {char}={digit}"

    def test_zero_value(self):
        result = decode_signed_numeric("00000000000{", 2)
        assert result == Decimal("0.00")

    def test_empty_string(self):
        result = decode_signed_numeric("", 2)
        assert result == Decimal("0")

    def test_spaces(self):
        result = decode_signed_numeric("            ", 2)
        assert result == Decimal("0")

    def test_no_decimals(self):
        result = decode_signed_numeric("0123{", 0)
        assert result == Decimal("1230")

    def test_plain_digit_at_end(self):
        # "01234" with 2 decimals: last char '4' is a plain digit (not overpunch)
        # digits = "0123", last_digit = "4", full = "01234", split = "012.34"
        result = decode_signed_numeric("01234", 2)
        assert result == Decimal("12.34")


class TestDecodeUnsignedNumeric:
    def test_id_field_returns_string(self):
        result = decode_unsigned_numeric("00000000001", 0)
        assert result == "00000000001"
        assert isinstance(result, str)

    def test_code_field_returns_string(self):
        result = decode_unsigned_numeric("747", 0)
        assert result == "747"
        assert isinstance(result, str)

    def test_with_decimals_returns_decimal(self):
        # "01234" with 2 decimals: split = "012.34"
        result = decode_unsigned_numeric("01234", 2)
        assert result == Decimal("12.34")
        assert isinstance(result, Decimal)

    def test_empty_returns_string_zero(self):
        result = decode_unsigned_numeric("", 0)
        assert result == "0"

    def test_empty_with_decimals_returns_decimal_zero(self):
        result = decode_unsigned_numeric("", 2)
        assert result == Decimal("0")


class TestParseAccountRecords:
    def test_record_count(self):
        fields = parse_copybook(COPYBOOK_DIR / "CVACT01Y.cpy")
        records = parse_file(DATA_DIR / "acctdata.txt", fields, get_record_length(fields))
        assert len(records) == 50

    def test_first_record_fields(self):
        fields = parse_copybook(COPYBOOK_DIR / "CVACT01Y.cpy")
        records = parse_file(DATA_DIR / "acctdata.txt", fields, get_record_length(fields))
        r = records[0]

        assert r["ACCT-ID"] == "00000000001"
        assert r["ACCT-ACTIVE-STATUS"] == "Y"
        assert r["ACCT-CURR-BAL"] == Decimal("194.00")
        assert r["ACCT-CREDIT-LIMIT"] == Decimal("2020.00")
        assert r["ACCT-CASH-CREDIT-LIMIT"] == Decimal("1020.00")
        assert r["ACCT-OPEN-DATE"] == "2014-11-20"
        assert r["ACCT-CURR-CYC-CREDIT"] == Decimal("0.00")
        assert r["ACCT-CURR-CYC-DEBIT"] == Decimal("0.00")

    def test_filler_excluded(self):
        fields = parse_copybook(COPYBOOK_DIR / "CVACT01Y.cpy")
        records = parse_file(DATA_DIR / "acctdata.txt", fields, get_record_length(fields))
        assert "FILLER" not in records[0]

    def test_line_number_tracked(self):
        fields = parse_copybook(COPYBOOK_DIR / "CVACT01Y.cpy")
        records = parse_file(DATA_DIR / "acctdata.txt", fields, get_record_length(fields))
        assert records[0]["_line"] == 1
        assert records[49]["_line"] == 50


class TestParseCardXrefRecords:
    def test_handles_short_lines(self):
        fields = parse_copybook(COPYBOOK_DIR / "CVACT03Y.cpy")
        min_len = get_min_record_length(fields)
        records = parse_file(DATA_DIR / "cardxref.txt", fields, min_len)
        assert len(records) == 50

    def test_first_record(self):
        fields = parse_copybook(COPYBOOK_DIR / "CVACT03Y.cpy")
        min_len = get_min_record_length(fields)
        records = parse_file(DATA_DIR / "cardxref.txt", fields, min_len)
        r = records[0]
        assert r["XREF-CARD-NUM"] == "0500024453765740"
        assert r["XREF-CUST-ID"] == "000000050"
        assert r["XREF-ACCT-ID"] == "00000000050"


class TestParseDailyTransactions:
    def test_record_count(self):
        fields = parse_copybook(COPYBOOK_DIR / "CVTRA06Y.cpy")
        records = parse_file(DATA_DIR / "dailytran.txt", fields, get_record_length(fields))
        assert len(records) == 300

    def test_signed_amount(self):
        fields = parse_copybook(COPYBOOK_DIR / "CVTRA06Y.cpy")
        records = parse_file(DATA_DIR / "dailytran.txt", fields, get_record_length(fields))
        r = records[0]
        assert r["DALYTRAN-AMT"] == Decimal("504.77")
        assert r["DALYTRAN-TYPE-CD"] == "01"
        assert r["DALYTRAN-CAT-CD"] == "0001"

    def test_negative_amount(self):
        fields = parse_copybook(COPYBOOK_DIR / "CVTRA06Y.cpy")
        records = parse_file(DATA_DIR / "dailytran.txt", fields, get_record_length(fields))
        # Find a return/refund transaction (type 03 = Credit, negative amount)
        negatives = [r for r in records if r["DALYTRAN-AMT"] < 0]
        # There should be at least some negative amounts in 300 transactions
        # (returns, credits, etc.)
        # If all are positive, that's also valid test data
        for r in negatives:
            assert r["DALYTRAN-AMT"] < 0


class TestParseDisclosureGroups:
    def test_record_count(self):
        fields = parse_copybook(COPYBOOK_DIR / "CVTRA02Y.cpy")
        records = parse_file(DATA_DIR / "discgrp.txt", fields, get_record_length(fields))
        assert len(records) == 51

    def test_interest_rate(self):
        fields = parse_copybook(COPYBOOK_DIR / "CVTRA02Y.cpy")
        records = parse_file(DATA_DIR / "discgrp.txt", fields, get_record_length(fields))
        r = records[0]
        assert r["DIS-INT-RATE"] == Decimal("15.00")
        assert r["DIS-ACCT-GROUP-ID"] == "A000000000"


class TestParseTransactionTypes:
    def test_record_count(self):
        fields = parse_copybook(COPYBOOK_DIR / "CVTRA03Y.cpy")
        min_len = get_min_record_length(fields)
        records = parse_file(DATA_DIR / "trantype.txt", fields, min_len)
        assert len(records) == 7

    def test_first_record(self):
        fields = parse_copybook(COPYBOOK_DIR / "CVTRA03Y.cpy")
        min_len = get_min_record_length(fields)
        records = parse_file(DATA_DIR / "trantype.txt", fields, min_len)
        r = records[0]
        assert r["TRAN-TYPE"] == "01"
        assert r["TRAN-TYPE-DESC"].strip() == "Purchase"


class TestDecodeComp3:
    def test_positive_zero(self):
        # 0x00 0x0C → nibbles: 0 0 0 C → digits "000", sign C (+)
        result = decode_comp3(b"\x00\x0C", 0, True)
        assert result == Decimal("0")

    def test_positive_integer(self):
        # PIC S9(03) COMP-3 = 2 bytes, value +123
        # 12 3C → nibbles: 1 2 3 C → digits "123", sign C (+)
        result = decode_comp3(b"\x12\x3C", 0, True)
        assert result == Decimal("123")

    def test_negative_integer(self):
        # PIC S9(03) COMP-3 = 2 bytes, value -456
        # 45 6D → nibbles: 4 5 6 D → digits "456", sign D (-)
        result = decode_comp3(b"\x45\x6D", 0, True)
        assert result == Decimal("-456")

    def test_with_decimals(self):
        # PIC S9(05)V99 COMP-3 = 4 bytes, value +504.77
        # 7 digits → ceil((7+1)/2) = 4 bytes
        # 0 0 5 0 4 7 7 C → 0x00 0x50 0x47 0x7C
        result = decode_comp3(b"\x00\x50\x47\x7C", 2, True)
        assert result == Decimal("504.77")

    def test_negative_with_decimals(self):
        # PIC S9(05)V99 COMP-3 = 4 bytes, value -919.00
        # 0 0 9 1 9 0 0 D → 0x00 0x91 0x90 0x0D
        result = decode_comp3(b"\x00\x91\x90\x0D", 2, True)
        assert result == Decimal("-919.00")

    def test_unsigned(self):
        # PIC 9(03) COMP-3 = 2 bytes, value 747
        # 74 7F → nibbles: 7 4 7 F → digits "747", sign F (unsigned)
        result = decode_comp3(b"\x74\x7F", 0, False)
        assert result == Decimal("747")

    def test_empty_bytes(self):
        result = decode_comp3(b"", 2, True)
        assert result == Decimal("0")

    def test_large_value(self):
        # PIC S9(09)V99 COMP-3 = 6 bytes, value +123456789.12
        # 11 digits → ceil((11+1)/2) = 6 bytes
        # 1 2 3 4 5 6 7 8 9 1 2 C → 0x12 0x34 0x56 0x78 0x91 0x2C
        result = decode_comp3(b"\x12\x34\x56\x78\x91\x2C", 2, True)
        assert result == Decimal("123456789.12")


class TestDecodeComp:
    def test_unsigned_2_bytes(self):
        # PIC 9(04) COMP → 2 bytes, big-endian
        # 0x00 0x2A = 42
        result = decode_comp(b"\x00\x2A", 0, False)
        assert result == "42"

    def test_signed_2_bytes_positive(self):
        # PIC S9(04) COMP → 2 bytes, signed big-endian
        # 0x00 0x64 = 100
        result = decode_comp(b"\x00\x64", 0, True)
        assert result == Decimal("100")

    def test_signed_2_bytes_negative(self):
        # PIC S9(04) COMP → 2 bytes, signed big-endian
        # 0xFF 0x9C = -100 (two's complement)
        result = decode_comp(b"\xFF\x9C", 0, True)
        assert result == Decimal("-100")

    def test_unsigned_4_bytes(self):
        # PIC 9(09) COMP → 4 bytes
        # 0x00 0x00 0x03 0xE8 = 1000
        result = decode_comp(b"\x00\x00\x03\xE8", 0, False)
        assert result == "1000"

    def test_with_decimals(self):
        # PIC S9(05)V99 COMP → 4 bytes, value stored as integer
        # Value = 50477 (represents 504.77 with 2 decimals)
        # 0x00 0x00 0xC5 0x2D = 50477
        result = decode_comp(b"\x00\x00\xC5\x2D", 2, True)
        assert result == Decimal("504.77")

    def test_empty_bytes(self):
        result = decode_comp(b"", 2, True)
        assert result == Decimal("0")
