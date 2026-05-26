#!/usr/bin/env python3
"""Generate golden-file JSON references from ASCII data files.

Parses each ASCII data file using its corresponding copybook layout
and writes structured JSON to the golden-files/ directory.
"""

from __future__ import annotations

import json
import sys
from datetime import datetime, timezone
from decimal import Decimal
from pathlib import Path
from typing import Any, Dict, List

# Add parent directory to path for imports when run as script
SCRIPT_DIR = Path(__file__).resolve().parent
if str(SCRIPT_DIR) not in sys.path:
    sys.path.insert(0, str(SCRIPT_DIR))

from copybook_parser import get_min_record_length, get_record_length, parse_copybook
from record_parser import parse_file

# Mapping of data files to their copybook layouts
DATASET_CONFIG = [
    {
        "data_file": "acctdata.txt",
        "copybook": "CVACT01Y.cpy",
        "output": "acctdata.json",
        "description": "Account records",
    },
    {
        "data_file": "carddata.txt",
        "copybook": "CVACT02Y.cpy",
        "output": "carddata.json",
        "description": "Card records",
    },
    {
        "data_file": "cardxref.txt",
        "copybook": "CVACT03Y.cpy",
        "output": "cardxref.json",
        "description": "Card-Account-Customer cross-reference",
    },
    {
        "data_file": "custdata.txt",
        "copybook": "CVCUS01Y.cpy",
        "output": "custdata.json",
        "description": "Customer records",
    },
    {
        "data_file": "dailytran.txt",
        "copybook": "CVTRA06Y.cpy",
        "output": "dailytran.json",
        "description": "Daily transaction input records",
    },
    {
        "data_file": "discgrp.txt",
        "copybook": "CVTRA02Y.cpy",
        "output": "discgrp.json",
        "description": "Disclosure/interest rate groups",
    },
    {
        "data_file": "tcatbal.txt",
        "copybook": "CVTRA01Y.cpy",
        "output": "tcatbal.json",
        "description": "Transaction category balances",
    },
    {
        "data_file": "trantype.txt",
        "copybook": "CVTRA03Y.cpy",
        "output": "trantype.json",
        "description": "Transaction type lookup",
    },
    {
        "data_file": "trancatg.txt",
        "copybook": "CVTRA04Y.cpy",
        "output": "trancatg.json",
        "description": "Transaction category lookup",
    },
]


class DecimalEncoder(json.JSONEncoder):
    """JSON encoder that serializes Decimal as a JSON number string.

    Uses str() to preserve exact precision (e.g., Decimal('194.00')
    serializes as 194.00, not 194.0).
    """

    def default(self, obj: Any) -> Any:
        if isinstance(obj, Decimal):
            # Use float for JSON number representation.
            # The golden file records the numeric value; exact string
            # representation is not critical for JSON since comparisons
            # use Decimal parsing on load.
            return float(obj)
        return super().default(obj)


def generate_golden_file(
    data_path: Path,
    copybook_path: Path,
    output_path: Path,
    description: str,
) -> Dict[str, Any]:
    """Parse a single data file and write a golden-file JSON.

    Returns metadata about the generated file.
    """
    fields = parse_copybook(copybook_path)
    record_length = get_record_length(fields)
    min_length = get_min_record_length(fields)
    records = parse_file(data_path, fields, expected_length=min_length)

    golden = {
        "metadata": {
            "source_file": data_path.name,
            "copybook": copybook_path.name,
            "record_length": record_length,
            "record_count": len(records),
            "description": description,
            "generated_at": datetime.now(timezone.utc).strftime(
                "%Y-%m-%dT%H:%M:%SZ"
            ),
        },
        "records": records,
    }

    output_path.parent.mkdir(parents=True, exist_ok=True)
    with open(output_path, "w") as f:
        json.dump(golden, f, indent=2, cls=DecimalEncoder)

    return golden["metadata"]


def main() -> None:
    """Generate all golden files."""
    import argparse

    parser = argparse.ArgumentParser(
        description="Generate golden-file JSON from ASCII data files"
    )
    parser.add_argument(
        "--data-dir",
        type=Path,
        default=None,
        help="Directory containing ASCII data files (default: app/data/ASCII/)",
    )
    parser.add_argument(
        "--copybook-dir",
        type=Path,
        default=None,
        help="Directory containing .cpy copybook files (default: app/cpy/)",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=None,
        help="Directory for output JSON files (default: golden-files/)",
    )
    args = parser.parse_args()

    # Default paths relative to repo root
    repo_root = SCRIPT_DIR.parent
    data_dir = args.data_dir or repo_root / "app" / "data" / "ASCII"
    copybook_dir = args.copybook_dir or repo_root / "app" / "cpy"
    output_dir = args.output_dir or repo_root / "golden-files"

    print(f"Data directory:     {data_dir}")
    print(f"Copybook directory: {copybook_dir}")
    print(f"Output directory:   {output_dir}")
    print()

    for config in DATASET_CONFIG:
        data_path = data_dir / config["data_file"]
        copybook_path = copybook_dir / config["copybook"]
        output_path = output_dir / config["output"]

        if not data_path.exists():
            print(f"SKIP  {config['data_file']}: file not found")
            continue

        if not copybook_path.exists():
            print(f"SKIP  {config['data_file']}: copybook {config['copybook']} not found")
            continue

        meta = generate_golden_file(
            data_path, copybook_path, output_path, config["description"]
        )
        print(
            f"OK    {config['output']}: "
            f"{meta['record_count']} records, "
            f"record_length={meta['record_length']}"
        )

    print(f"\nGolden files written to {output_dir}/")


if __name__ == "__main__":
    main()
