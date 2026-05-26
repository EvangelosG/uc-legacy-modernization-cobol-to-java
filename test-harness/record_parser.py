"""Parse fixed-width ASCII data files using copybook field definitions.

Reads lines from a fixed-width text file, slices each line according to
field offsets and lengths from a parsed copybook, and decodes values
based on their PIC types.
"""

from __future__ import annotations

from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Any, Dict, List

from copybook_parser import (
    FieldDef,
    NEGATIVE_SIGNS,
    POSITIVE_SIGNS,
)


def decode_signed_numeric(raw: str, decimals: int) -> Decimal:
    """Decode a COBOL signed numeric field with trailing overpunch sign.

    COBOL DISPLAY format stores the sign as an overpunch character on the
    last digit. For example, PIC S9(10)V99 with value +1940.00 is stored as
    "000000019400{" where '{' means +0 (the trailing digit is 0, sign is +).
    """
    if not raw or raw.isspace():
        return Decimal("0")

    raw = raw.strip()
    if not raw:
        return Decimal("0")

    last_char = raw[-1]
    digits = raw[:-1]

    if last_char in POSITIVE_SIGNS:
        last_digit = str(POSITIVE_SIGNS[last_char])
        sign = ""
    elif last_char in NEGATIVE_SIGNS:
        last_digit = str(NEGATIVE_SIGNS[last_char])
        sign = "-"
    elif last_char.isdigit():
        last_digit = last_char
        sign = ""
    else:
        last_digit = "0"
        sign = ""

    full_digits = digits + last_digit

    if decimals > 0:
        integer_part = full_digits[:-decimals] or "0"
        decimal_part = full_digits[-decimals:]
        value_str = f"{sign}{integer_part}.{decimal_part}"
    else:
        value_str = f"{sign}{full_digits}"

    try:
        return Decimal(value_str)
    except InvalidOperation:
        return Decimal("0")


def decode_unsigned_numeric(raw: str, decimals: int) -> str | Decimal:
    """Decode an unsigned numeric field.

    If the field has decimal places (V in PIC), returns a Decimal.
    Otherwise returns a zero-padded string — these are typically
    identifiers or codes where leading zeros are significant.
    """
    if not raw or raw.isspace():
        return Decimal("0") if decimals > 0 else "0"

    raw = raw.strip()
    if not raw:
        return Decimal("0") if decimals > 0 else "0"

    if decimals > 0:
        integer_part = raw[:-decimals] or "0"
        decimal_part = raw[-decimals:]
        value_str = f"{integer_part}.{decimal_part}"
        try:
            return Decimal(value_str)
        except InvalidOperation:
            return Decimal("0")
    else:
        return raw


def decode_field(raw: str, field_def: FieldDef) -> Any:
    """Decode a single field value based on its type."""
    if field_def.field_type == "signed_numeric":
        return decode_signed_numeric(raw, field_def.decimals)
    elif field_def.field_type == "numeric":
        return decode_unsigned_numeric(raw, field_def.decimals)
    else:
        return raw.rstrip()


def parse_record(line: str, fields: List[FieldDef]) -> Dict[str, Any]:
    """Parse a single fixed-width line into a dict using field definitions.

    FILLER fields are consumed (offset advances) but excluded from output.
    """
    record: Dict[str, Any] = {}
    for f in fields:
        raw = line[f.offset : f.offset + f.length]
        if not f.is_filler:
            record[f.name] = decode_field(raw, f)
    return record


def parse_file(
    data_path: Path,
    fields: List[FieldDef],
    expected_length: int | None = None,
) -> List[Dict[str, Any]]:
    """Parse all records from a fixed-width ASCII data file.

    Args:
        data_path: Path to the ASCII data file.
        fields: Field definitions from the copybook parser.
        expected_length: Expected record length. If provided, lines that
            don't match (after stripping line endings) are skipped with
            a warning.

    Returns:
        List of parsed record dicts.
    """
    records: List[Dict[str, Any]] = []
    text = data_path.read_text()

    for line_num, line in enumerate(text.splitlines(), start=1):
        # Strip trailing \r if present (some files have \r\n)
        line = line.rstrip("\r")

        if not line.strip():
            continue

        if expected_length is not None and len(line) < expected_length:
            continue

        record = parse_record(line, fields)
        record["_line"] = line_num
        records.append(record)

    return records
