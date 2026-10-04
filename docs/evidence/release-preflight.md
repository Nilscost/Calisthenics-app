# Release preflight (T24) — 2026-10-04, partial

| Check | Result |
|---|---|
| `python3 tools/preflight.py` (secrets, keystores, APKs, >400 KB files, machine paths) | OK, 259 tracked files (clean clone) |
| Keystore `Apps/keys/calisthenics-debug.jks` | outside Git, `*.jks` ignored |
| CI workflow `.github/workflows/android.yml` | written; NOT yet run on GitHub (no remote exists; no automatic upload) |
| `THIRD_PARTY_NOTICES.md` | runtime dependency inventory generated; per-artifact licence check still open |
| Project licence | NOT selected. Upstream is GPL-3.0, so a derivative must stay GPL-3.0-compatible. Owner decision. |
| Clean-clone build | PROVEN 2026-10-04: fresh git clone at 4a34591, scripts/verify.sh exit 0 (log clean-clone-verify-2026-10-04.log) |
| Known limits | nothing run on a phone; catalog DRAFT; no demo media |
