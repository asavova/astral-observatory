#!/usr/bin/env python3
"""Make a small offline TSV from the official HYG v4.1 catalogue.

Usage: python3 scripts/import_hyg.py hygdata_v41.csv data/hyg-full-v41.tsv
The original HYG CSV is downloaded beforehand, not by this script.
"""
import csv
import math
import sys
from pathlib import Path

FIELDS = ("id", "ra", "dec", "mag", "ci", "proper", "dist")


def convert(source: Path, target: Path) -> tuple[int, int]:
    total = 0
    kept = 0
    target.parent.mkdir(parents=True, exist_ok=True)
    with source.open(encoding="utf-8-sig", newline="") as src, target.open(
        "w", encoding="utf-8", newline=""
    ) as dst:
        reader = csv.DictReader(src)
        if not set(FIELDS).issubset(reader.fieldnames or []):
            raise ValueError(f"Unexpected HYG columns: {reader.fieldnames}")
        writer = csv.writer(dst, delimiter="\t", lineterminator="\n")
        writer.writerow(("id", "ra_hours", "dec_deg", "mag", "ci", "name"))
        for row in reader:
            total += 1
            # The Sun is not a fixed stellar point and is simulated separately later.
            if row["dist"] and float(row["dist"]) == 0:
                continue
            ra, dec = float(row["ra"]), float(row["dec"])
            mag = float(row["mag"]) if row["mag"] else 99.0
            ci = float(row["ci"]) if row["ci"] else 0.0
            if not math.isfinite(mag):
                mag = 99.0
            if not math.isfinite(ci):
                ci = 0.0
            if not all(map(math.isfinite, (ra, dec))):
                raise ValueError(f"Non-finite coordinates at source row {total}")
            if not (0 <= ra <= 24 and -90 <= dec <= 90):
                raise ValueError(f"Invalid coordinates at source row {total}")
            identifier = row["id"] or str(total)
            name = row["proper"].replace("\t", " ").replace("\n", " ").replace("\r", " ")
            writer.writerow((identifier, row["ra"], row["dec"], mag, ci, name))
            kept += 1
    if kept < 100_000:
        raise ValueError(f"Too few stars; wrong source file? Got {kept}")
    return total, kept


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("Usage: import_hyg.py <official HYG v4.1 CSV> <output TSV>")
    total, stars = convert(Path(sys.argv[1]), Path(sys.argv[2]))
    print(f"Imported {stars:,} stars from {total:,} source rows")
