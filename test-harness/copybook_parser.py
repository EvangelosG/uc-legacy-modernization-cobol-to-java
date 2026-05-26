"""Parse COBOL copybook PIC clauses into field definitions.

Reads .cpy files and extracts field names, types, lengths, and decimal
positions. Supports PIC 9, PIC X, PIC S9 (signed), V (implied decimal),
group-level items, and FILLER.
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from pathlib import Path
from typing import List, Optional


@dataclass
class FieldDef:
    """A single field extracted from a COBOL copybook."""

    name: str
    pic: str
    offset: int
    length: int
    field_type: str  # "alphanumeric", "numeric", "signed_numeric"
    decimals: int = 0
    is_filler: bool = False
    level: int = 5

    @property
    def is_numeric(self) -> bool:
        return self.field_type in ("numeric", "signed_numeric")

    @property
    def is_signed(self) -> bool:
        return self.field_type == "signed_numeric"


# Pattern to match a COBOL data item with PIC clause
_DATA_ITEM_RE = re.compile(
    r"^\s*(\d{2})\s+"           # level number
    r"([\w-]+|FILLER)\s+"       # field name
    r"PIC\s+"                   # PIC keyword
    r"([SXx90-9()V]+)\s*\.",    # PIC clause (digits for counts inside parens)
    re.IGNORECASE,
)

# Overpunch sign characters for trailing sign in ASCII representation
POSITIVE_SIGNS = {
    "{": 0, "A": 1, "B": 2, "C": 3, "D": 4,
    "E": 5, "F": 6, "G": 7, "H": 8, "I": 9,
}
NEGATIVE_SIGNS = {
    "}": 0, "J": 1, "K": 2, "L": 3, "M": 4,
    "N": 5, "O": 6, "P": 7, "Q": 8, "R": 9,
}


def _expand_pic(pic: str) -> str:
    """Expand PIC shorthand: 9(11) -> 99999999999, X(16) -> XXXXXXXXXXXXXXXX."""
    def _replace(m: re.Match) -> str:
        char = m.group(1)
        count = int(m.group(2))
        return char * count
    return re.sub(r"([9Xx])\((\d+)\)", _replace, pic)


def _parse_pic(pic: str) -> tuple[str, int, int]:
    """Parse a PIC clause into (field_type, display_length, decimal_places).

    Returns the display length (number of characters the field occupies in
    the fixed-width file) and the number of decimal places (from V).
    """
    raw = pic.upper().strip()
    expanded = _expand_pic(raw)

    is_signed = expanded.startswith("S")
    if is_signed:
        expanded = expanded[1:]

    if "V" in expanded:
        integer_part, decimal_part = expanded.split("V", 1)
        int_digits = integer_part.count("9")
        dec_digits = decimal_part.count("9")
        # In DISPLAY format, the V is implied (no physical character).
        # The sign character replaces the last digit (trailing overpunch).
        display_len = int_digits + dec_digits
        field_type = "signed_numeric" if is_signed else "numeric"
        return field_type, display_len, dec_digits
    elif "9" in expanded:
        digits = expanded.count("9")
        field_type = "signed_numeric" if is_signed else "numeric"
        return field_type, digits, 0
    else:
        char_count = expanded.count("X")
        return "alphanumeric", char_count, 0


def parse_copybook(path: Path) -> List[FieldDef]:
    """Parse a COBOL copybook file and return a list of FieldDef objects.

    Lines that are comments (start with *) or don't contain PIC clauses
    are skipped. Multi-line PIC clauses are not supported (all CardDemo
    copybooks use single-line definitions).
    """
    fields: List[FieldDef] = []
    offset = 0

    text = path.read_text()
    for line in text.splitlines():
        # Skip comments (column 7 = *)
        stripped = line.lstrip()
        if stripped.startswith("*"):
            continue

        m = _DATA_ITEM_RE.search(line)
        if not m:
            continue

        level = int(m.group(1))
        name = m.group(2).upper()
        pic = m.group(3)

        is_filler = name == "FILLER"
        field_type, length, decimals = _parse_pic(pic)

        fields.append(FieldDef(
            name=name,
            pic=pic,
            offset=offset,
            length=length,
            field_type=field_type,
            decimals=decimals,
            is_filler=is_filler,
            level=level,
        ))
        offset += length

    return fields


def get_record_length(fields: List[FieldDef]) -> int:
    """Compute total record length from field definitions."""
    if not fields:
        return 0
    last = fields[-1]
    return last.offset + last.length


def get_min_record_length(fields: List[FieldDef]) -> int:
    """Compute minimum record length (up to last non-FILLER field).

    Some data files omit trailing FILLER padding. This returns the
    offset + length of the last named field, which is the minimum
    number of characters needed to parse all meaningful data.
    """
    if not fields:
        return 0
    for f in reversed(fields):
        if not f.is_filler:
            return f.offset + f.length
    return 0
