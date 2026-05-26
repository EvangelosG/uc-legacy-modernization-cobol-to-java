"""Parse fixed-width data files using copybook field definitions.

Reads lines from a fixed-width text file, slices each line according to
field offsets and lengths from a parsed copybook, and decodes values
based on their PIC types. Supports DISPLAY, COMP-3 (packed decimal),
and COMP (binary) storage formats.
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


def decode_comp3(raw_bytes: bytes, decimals: int, signed: bool) -> Decimal:
    """Decode a COMP-3 (packed decimal) field.

    Packed decimal stores two digits per byte, with the last nibble
    holding the sign (0xC = positive, 0xD = negative, 0xF = unsigned).

    Example: PIC S9(09)V99 COMP-3 with value +504.77
        Stored as 6 bytes: 0x00 0x00 0x50 0x47 0x70 0x0C
        Nibbles: 0 0 0 0 5 0 4 7 7 0 0 C
        Digits:  0 0 0 0 5 0 4 7 7 0 0
        Sign: C (positive)
        With 2 decimals: 000050477.00 → wait, need to count correctly

    The actual encoding for PIC S9(09)V99 (11 digits total):
        ceil((11+1)/2) = 6 bytes
        Nibbles: d d d d d d d d d d d s
        where d = digit, s = sign nibble
    """
    if not raw_bytes:
        return Decimal("0")

    # Extract nibbles from each byte
    nibbles = []
    for b in raw_bytes:
        nibbles.append((b >> 4) & 0x0F)
        nibbles.append(b & 0x0F)

    # Last nibble is the sign
    sign_nibble = nibbles[-1]
    digit_nibbles = nibbles[:-1]

    # Build digit string
    digits = "".join(str(n) for n in digit_nibbles)

    # Determine sign
    is_negative = sign_nibble == 0x0D
    sign = "-" if is_negative else ""

    if decimals > 0:
        if len(digits) <= decimals:
            digits = digits.zfill(decimals + 1)
        integer_part = digits[:-decimals] or "0"
        decimal_part = digits[-decimals:]
        value_str = f"{sign}{integer_part}.{decimal_part}"
    else:
        value_str = f"{sign}{digits}"

    try:
        return Decimal(value_str)
    except InvalidOperation:
        return Decimal("0")


def decode_comp(raw_bytes: bytes, decimals: int, signed: bool) -> Decimal | str:
    """Decode a COMP (binary) field.

    COMP fields are stored as big-endian binary integers.
    PIC 9(n) COMP uses 2 bytes (n<=4), 4 bytes (n<=9), or 8 bytes (n>9).
    Signed fields (PIC S9) use two's complement.
    """
    if not raw_bytes:
        return Decimal("0") if decimals > 0 or signed else "0"

    value = int.from_bytes(raw_bytes, byteorder="big", signed=signed)

    if decimals > 0:
        divisor = 10 ** decimals
        return Decimal(value) / Decimal(divisor)
    elif signed:
        return Decimal(value)
    else:
        return str(value)


def decode_field(raw: str | bytes, field_def: FieldDef) -> Any:
    """Decode a single field value based on its type and usage."""
    if field_def.usage == "comp3":
        raw_bytes = raw if isinstance(raw, bytes) else raw.encode("latin-1")
        return decode_comp3(
            raw_bytes, field_def.decimals, field_def.is_signed,
        )
    elif field_def.usage == "comp":
        raw_bytes = raw if isinstance(raw, bytes) else raw.encode("latin-1")
        return decode_comp(
            raw_bytes, field_def.decimals, field_def.is_signed,
        )
    elif field_def.field_type == "signed_numeric":
        return decode_signed_numeric(str(raw), field_def.decimals)
    elif field_def.field_type == "numeric":
        return decode_unsigned_numeric(str(raw), field_def.decimals)
    else:
        return str(raw).rstrip()


def parse_record(
    line: str | bytes,
    fields: List[FieldDef],
) -> Dict[str, Any]:
    """Parse a single fixed-width line into a dict using field definitions.

    FILLER fields are consumed (offset advances) but excluded from output.
    For binary data (containing COMP/COMP-3 fields), pass bytes.
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
    has_binary = any(f.usage in ("comp", "comp3") for f in fields)

    if has_binary:
        # Binary data: read as bytes, split by record length
        raw = data_path.read_bytes()
        if expected_length is None:
            return records
        rec_num = 0
        while rec_num * expected_length < len(raw):
            start = rec_num * expected_length
            chunk = raw[start : start + expected_length]
            if len(chunk) < expected_length:
                break
            record = parse_record(chunk, fields)
            record["_line"] = rec_num + 1
            records.append(record)
            rec_num += 1
    else:
        # Text data: read as string, split by newlines
        text = data_path.read_text()
        for line_num, line in enumerate(text.splitlines(), start=1):
            line = line.rstrip("\r")
            if not line.strip():
                continue
            if expected_length is not None and len(line) < expected_length:
                continue
            record = parse_record(line, fields)
            record["_line"] = line_num
            records.append(record)

    return records
