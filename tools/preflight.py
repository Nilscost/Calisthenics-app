#!/usr/bin/env python3
"""Repo hygiene preflight (T24): fails on tracked secrets, keystores, APKs, big files, personal paths.
Run from repo root: python3 tools/preflight.py"""
import subprocess, re, sys, os
files = subprocess.check_output(["git", "ls-files"], text=True).splitlines()
bad = []
BIG = 1024 * 1024   # 1 MB: the generated catalog is about 400 KB and the bundled fonts about 500 KB
SECRET = re.compile(r"AKIA[0-9A-Z]{16}|-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY|ghp_[A-Za-z0-9]{30,}|xox[baprs]-[A-Za-z0-9-]{10,}")
PATHS = re.compile(r"/Users/[a-z0-9_.-]+/")
SKIP_PATH_CHECK = ("docs/", "STATUS.md", "tasks.json", "PACKAGE-MANIFEST.json", "tools/preflight.py")  # historical notes
for f in files:
    if not os.path.isfile(f): continue
    if re.search(r"\.(jks|keystore|apk|aab|p12|pem|db)$", f): bad.append(f"forbidden file type: {f}")
    if f in ("local.properties",): bad.append(f"machine file tracked: {f}")
    if os.path.getsize(f) > BIG and not f.endswith((".png", ".webp")): bad.append(f"large file: {f} ({os.path.getsize(f)//1024} KB)")
    try: t = open(f, encoding="utf-8").read()
    except Exception: continue
    if SECRET.search(t): bad.append(f"possible secret in {f}")
    if PATHS.search(t) and not f.startswith(SKIP_PATH_CHECK): bad.append(f"machine path in {f}")
print("\n".join(bad) or "preflight OK", f"({len(files)} tracked files)")
sys.exit(1 if bad else 0)
