#!/usr/bin/env python3
"""Fail if the Android port drops, skips or fails an original app test."""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent.parent


def main():
    baseline = set((ROOT / "config/app-test-baseline.txt").read_text().splitlines())
    reports = sorted((ROOT / "app/build/test-results/testDebugUnitTest").glob("TEST-*.xml"))
    if not reports:
        raise ValueError("No app unit-test results found")
    actual = set()
    for report in reports:
        suite = ET.parse(report).getroot()
        for field in ("failures", "errors", "skipped"):
            if int(suite.get(field, "0")):
                raise ValueError(f"{report.name}: {field} is nonzero")
        for case in suite.iter("testcase"):
            if any(case.find(tag) is not None for tag in ("failure", "error", "skipped")):
                raise ValueError(f"{report.name}: test did not pass")
            identity = f"{case.get('classname')}#{case.get('name')}"
            if identity in actual:
                raise ValueError(f"Duplicate app test identity: {identity}")
            actual.add(identity)
    missing = baseline - actual
    if missing:
        raise ValueError(f"Missing {len(missing)} original app tests: {sorted(missing)[:20]}")
    print(f"App test preservation OK: {len(baseline)} original tests retained; "
          f"{len(actual)} executed; no skips or failures.")


if __name__ == "__main__":
    try:
        main()
    except (ValueError, ET.ParseError) as exc:
        print(exc, file=sys.stderr)
        sys.exit(1)
