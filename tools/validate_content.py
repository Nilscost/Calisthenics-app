#!/usr/bin/env python3
"""Validate a content pack directory against the media/catalog contract (spec §8).

T03 deliverable — the command contract in docs/04-verification.md runs:
    python3 tools/validate_content.py content/starter

A pack directory contains:
  - pack.json        (manifest: packId, version, schemaVersion, minAppVersion,
                      catalogVersion, files[{name, sha256, length}], licenses[])
  - exercises.json   (array of ExerciseVariation-shaped records; see spec §3)
  - media.json       (array of media records; see spec §8)
  - media/           (local MP4 loops, muted, no trackers)

Exit codes: 0 = valid, 1 = validation error(s), 2 = usage error.
This tool validates STRUCTURE and RIGHTS fields. It does not judge training
quality, decode video, or claim any content is reviewed.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path

REQUIRED_PACK_KEYS = [
    "packId", "version", "schemaVersion", "minAppVersion",
    "catalogVersion", "files", "licenses",
]
REQUIRED_MEDIA_KEYS = [
    "id", "relativePath", "mimeType", "byteSize", "sha256",
    "durationMs", "creator", "licenseId", "attributionText",
    "redistributionAllowed", "offlineAllowed", "reviewState",
]
VARIATION_REQUIRED = [
    "id", "familyId", "name", "kind", "equipmentAlternatives",
    "instructions", "sourceIds", "reviewState",
]
VALID_KINDS = {"REPS", "HOLD", "STRETCH", "MOBILITY"}
VALID_REVIEW_STATES = {"DRAFT", "REVIEWED"}


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1 << 16), b""):
            h.update(chunk)
    return h.hexdigest()


def fail(errors: list[str], path: Path, key: str) -> None:
    errors.append(f"{path}: missing required key '{key}'")


def validate_pack(pack_dir: Path, errors: list[str]) -> None:
    if not pack_dir.is_dir():
        errors.append(f"pack directory not found: {pack_dir}")
        return

    pack_path = pack_dir / "pack.json"
    if not pack_path.exists():
        errors.append(f"missing pack manifest: {pack_path}")
        return
    try:
        pack = json.loads(pack_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as e:
        errors.append(f"pack.json is not valid JSON: {e}")
        return

    for key in REQUIRED_PACK_KEYS:
        if key not in pack:
            fail(errors, pack_path, key)

    # File manifest integrity: every listed file must exist, hash and length must match.
    listed = {f.get("name") for f in pack.get("files", []) if isinstance(f, dict)}
    for entry in pack.get("files", []):
        if not isinstance(entry, dict):
            errors.append("pack.json: files[] entry is not an object")
            continue
        for key in ("name", "sha256", "length"):
            if key not in entry:
                fail(errors, pack_path, f"files[].{key}")
        name = entry.get("name")
        if not name:
            continue
        if ".." in name or Path(name).is_absolute():
            errors.append(f"pack.json: unsafe file path (traversal/absolute): {name}")
            continue
        target = pack_dir / name
        if not target.exists():
            errors.append(f"pack.json: listed file missing on disk: {name}")
            continue
        if target.stat().st_size != entry.get("length"):
            errors.append(f"pack.json: length mismatch for {name}: "
                          f"manifest={entry.get('length')} actual={target.stat().st_size}")
        actual = sha256_file(target)
        if actual != entry.get("sha256"):
            errors.append(f"pack.json: sha256 mismatch for {name}: "
                          f"manifest={entry.get('sha256')} actual={actual}")

    # Every media file on disk must be listed in the manifest (no unlisted assets).
    media_dir = pack_dir / "media"
    if media_dir.is_dir():
        for f in sorted(media_dir.rglob("*")):
            if f.is_file():
                rel = str(f.relative_to(pack_dir))
                if rel not in listed:
                    errors.append(f"unlisted media file (not in pack.json): {rel}")

    # Media records: rights fields present, licenses referenced, review state valid.
    media_path = pack_dir / "media.json"
    if not media_path.exists():
        errors.append(f"missing media records: {media_path}")
        return
    try:
        media = json.loads(media_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as e:
        errors.append(f"media.json is not valid JSON: {e}")
        return
    if not isinstance(media, list):
        errors.append("media.json: top level must be an array")
        return

    license_ids = {l.get("id") for l in pack.get("licenses", []) if isinstance(l, dict)}
    seen_media_ids = set()
    for i, rec in enumerate(media):
        label = rec.get("id", f"[{i}]") if isinstance(rec, dict) else f"[{i}]"
        if not isinstance(rec, dict):
            errors.append(f"media.json[{i}]: not an object")
            continue
        for key in REQUIRED_MEDIA_KEYS:
            if key not in rec:
                fail(errors, media_path, f"{label}.{key}")
        if rec.get("id") in seen_media_ids:
            errors.append(f"media.json: duplicate media id {rec.get('id')}")
        seen_media_ids.add(rec.get("id"))
        if rec.get("mimeType") not in {"video/mp4"}:
            errors.append(f"media.json {label}: mimeType must be video/mp4 (spec §8), got {rec.get('mimeType')}")
        if rec.get("licenseId") not in license_ids:
            errors.append(f"media.json {label}: licenseId '{rec.get('licenseId')}' not declared in pack.json licenses[]")
        if rec.get("reviewState") not in VALID_REVIEW_STATES:
            errors.append(f"media.json {label}: invalid reviewState {rec.get('reviewState')}")
        if rec.get("redistributionAllowed") is not True or rec.get("offlineAllowed") is not True:
            errors.append(f"media.json {label}: redistributionAllowed/offlineAllowed must be true for bundled starter media")
        rel = rec.get("relativePath")
        if rel:
            target = pack_dir / rel
            if not target.exists():
                errors.append(f"media.json {label}: relativePath missing on disk: {rel}")
            elif sha256_file(target) != rec.get("sha256"):
                errors.append(f"media.json {label}: on-disk sha256 mismatch for {rel}")

    # Exercise variations: structure, ids, kinds, review state.
    ex_path = pack_dir / "exercises.json"
    if not ex_path.exists():
        errors.append(f"missing exercise records: {ex_path}")
        return
    try:
        exercises = json.loads(ex_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as e:
        errors.append(f"exercises.json is not valid JSON: {e}")
        return
    if not isinstance(exercises, list):
        errors.append("exercises.json: top level must be an array")
        return
    seen_var_ids = set()
    for i, rec in enumerate(exercises):
        label = rec.get("id", f"[{i}]") if isinstance(rec, dict) else f"[{i}]"
        if not isinstance(rec, dict):
            errors.append(f"exercises.json[{i}]: not an object")
            continue
        for key in VARIATION_REQUIRED:
            if key not in rec:
                fail(errors, ex_path, f"{label}.{key}")
        if rec.get("id") in seen_var_ids:
            errors.append(f"exercises.json: duplicate variation id {rec.get('id')}")
        seen_var_ids.add(rec.get("id"))
        if rec.get("kind") not in VALID_KINDS:
            errors.append(f"exercises.json {label}: invalid kind {rec.get('kind')}")
        if rec.get("reviewState") not in VALID_REVIEW_STATES:
            errors.append(f"exercises.json {label}: invalid reviewState {rec.get('reviewState')}")
        if not rec.get("sourceIds"):
            errors.append(f"exercises.json {label}: sourceIds must not be empty (provenance required)")


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("pack_dir", help="path to the pack directory, e.g. content/starter")
    args = ap.parse_args(argv)

    errors: list[str] = []
    validate_pack(Path(args.pack_dir), errors)

    if errors:
        print(f"FAIL: {len(errors)} validation error(s) in {args.pack_dir}:")
        for e in errors:
            print(f"  - {e}")
        return 1
    print(f"OK: {args.pack_dir} passed content validation (structure + rights fields).")
    print("NOTE: structural validation only — training quality, tiers and media content are not judged by this tool.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
