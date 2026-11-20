#!/usr/bin/env python3
"""Require the original domain tests and matching common coverage on JVM/Wasm."""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent.parent
BASELINE = set((ROOT / "config/domain-test-baseline.txt").read_text().splitlines())
JVM_ONLY = {"app.calisthenics.domain.SeedFixtureDiskTest#committedFilesMatchGeneratedFixture"}


def read_results(task):
    files = sorted((ROOT / "domain/build/test-results" / task).glob("TEST-*.xml"))
    if not files:
        raise ValueError(f"{task}: no XML reports; run the target's tests first")
    identities = set()
    suffix = "[jvm]" if task == "jvmTest" else "[wasmJs, node]"
    for path in files:
        suite = ET.parse(path).getroot()
        for attr in ("failures", "errors", "skipped"):
            if int(suite.get(attr, "0")):
                raise ValueError(f"{task}: {path.name} has {attr}")
        for case in suite.iter("testcase"):
            if any(case.find(tag) is not None for tag in ("failure", "error", "skipped")):
                raise ValueError(f"{task}: unsuccessful test {case.get('name')}")
            cls, name = case.get("classname", ""), case.get("name", "")
            if task == "wasmJsNodeTest" and cls.startswith("wasmJsNodeTest."):
                cls = cls.removeprefix("wasmJsNodeTest.")
            if name.endswith(suffix):
                name = name[:-len(suffix)]
            identity = f"{cls}#{name}"
            if identity in identities:
                raise ValueError(f"{task}: duplicate test identity {identity}")
            identities.add(identity)
    missing = BASELINE - identities
    if missing:
        raise ValueError(f"{task}: missing {len(missing)} original tests: {sorted(missing)[:20]}")
    return identities


def main():
    jvm = read_results("jvmTest")
    wasm = read_results("wasmJsNodeTest")
    if not JVM_ONLY <= jvm:
        raise ValueError("JVM fixture disk verification did not run")
    shared = jvm - JVM_ONLY
    if shared != wasm:
        raise ValueError(f"Common test mismatch: JVM only {sorted(shared - wasm)[:20]}; "
                         f"Wasm only {sorted(wasm - shared)[:20]}")
    print(f"Domain parity OK: {len(BASELINE)} original tests retained on both targets; "
          f"{len(shared)} common tests, {len(JVM_ONLY)} additional JVM disk test; no skips or failures.")


if __name__ == "__main__":
    try:
        main()
    except (ValueError, ET.ParseError) as exc:
        print(exc, file=sys.stderr)
        sys.exit(1)
