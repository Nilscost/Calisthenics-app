# Release preflight (T24) — 2026-10-04, partial

| Check | Result |
|---|---|
| `python3 tools/preflight.py` (secrets, keystores, APKs, >400 KB files, machine paths) | OK, 207 tracked files |
| Keystore `Apps/keys/calisthenics-debug.jks` | outside Git, `*.jks` ignored |
| CI workflow `.github/workflows/android.yml` | written; NOT yet run on GitHub (no remote exists; no automatic upload) |
| `THIRD_PARTY_NOTICES.md` | runtime dependency inventory generated; per-artifact licence check still open |
| Project licence | NOT selected. Upstream is GPL-3.0, so a derivative must stay GPL-3.0-compatible. Owner decision. |
| Clean-clone build | not yet proven |
| Known limits | nothing run on a phone; catalog DRAFT; no demo media |
