# Actual project status

## Completed for this handoff
- Product behavior consolidated and approved by the owner.
- Coding specification, task queue, verification scenarios and local-model prompt written.
- A Hermes desktop project was registered at `/Users/nils/Documents/Apps/calisthenics`.

## Completed during implementation
### T00 — Verify workspace and tool access (2026-09-25) — VERIFIED
- Working copy at `/workspace/app/calisthenics` (Docker sandbox); 20/20 files re-verified against `PACKAGE-MANIFEST.json` (sha256 + size).
- Ran `python3 tools/check_handoff.py` (exit 0), `python3 -m unittest discover -s tests -p 'test_*.py' -v` (19 tests, OK, exit 0), `git diff --check` (exit 0).
- Tool inventory recorded with real outputs: git 2.47.3, Python 3.11.15 present; **JDK, Gradle, Android SDK, adb, gh all MISSING** in sandbox; Galaxy S21 not reachable; apt package install blocked (403 via egress proxy); no local model runner present (agent model is a remote custom provider, recorded not substituted).
- Deliverable: `docs/evidence/environment.md`.
- **B1 (owner folder confirmation) closed 2026-09-25**: owner attached the handoff files from `/Users/nils/Documents/Apps/calisthenics` and directed implementation to start.
- T00 status: `verified`. No commits made (git identity unset, no remotes). No publishing, no token handling.

### T01 — Audit reuse candidates and choose one foundation (2026-09-25, build verified 2026-09-28) — VERIFIED
- Cloned and inspected all four survey candidates in `/workspace/app/research-checkouts/` (network was available early in the session):
  - **CalisthenicsMemory** `045b8577d1a5c7fa75d40bf6c1df95244af616f3` (codeberg, `master`, GPL-3.0): real `WorkoutTimerService` (foreground + `PARTIAL_WAKE_LOCK`), `SoundPlayer` (SoundPool, `R.raw.start_cue/set_complete/beep_short`), Room interval-program entities, 1..10 per-exercise levels, **no cloud deps**, AGP 8.13/Gradle 8.13/Kotlin 2.0.21/minSdk 26.
  - **Ironvellum** `d8d51ab35113c071099e25de7842138ce9e9d82e` (github, `main`, GPL-3.0): 704-line skill graph (`domain/Skills.kt`), but **no timer engine and no audio at all** — its `WorkoutSessionService` is only a notification mirror over a set/rep tracker; Supabase auth/keys baked in; AGP 9.4/Gradle 9.7.1/compileSdk 37.
  - **Ballast** `a4b7d74983bd5800b95cd96a6f19a68191d68537`: Vite+React **web PWA**, not Android — excluded as foundation.
  - **Calistenia**: clone never completed before the network outage — **not inspected** (recorded as a limit).
- **Build (real, retained)**: isolated copy at `/workspace/app/research-checkouts/cm-build-attempt/` (HEAD `045b8577`, `app/build.gradle.kts` **unmodified from upstream** — diff-verified against the clean checkout). First attempt 2026-09-25 failed at Gradle-distribution download (`UnknownHostException: services.gradle.org`, sandbox DNS down). On 2026-09-28 (network restored) re-ran in the same copy: `./gradlew assembleDebug` **exit 0** → `app-debug.apk` (14,673,181 bytes, BUILD SUCCESSFUL in 2m9s); `./gradlew testDebugUnitTest` **exit 0** → 13 classes, 209 tests, 0 failures/errors; `./gradlew lintDebug` exit 1 → 749 pre-existing upstream style errors (not build-breaking; baseline at T24). Two **environment-only** fixes were needed in this ARM64 sandbox (no candidate source changed): AGP's x86-64 `aapt2` runs under `qemu-user-static` once an x86-64 glibc loader is installed; and `sqlite-jdbc 3.41.2.2` (transitive via Room) ships no `Linux/aarch64` native, so the aarch64 `libsqlitejdbc.so` from `sqlite-jdbc 3.45.3.0` was placed in `/usr/lib/aarch64-linux-gnu`. Full evidence: `docs/evidence/t01-build.md`; logs `/workspace/cm-build-attempt.log`, `/workspace/cm-test.log`, `/workspace/cm-lint.log`.
- Deliverables written: `docs/research/reuse-audit.md`, `docs/adr/0001-foundation.md` (PROPOSED, awaiting G0).
- **Recommendation for G0**: fork/adapt CalisthenicsMemory (keep GPL-3.0 notices); Ironvellum reference-only; native-Kotlin fallback if owner rejects a GPL base.
- All T01 substeps complete as of 2026-09-28 (build + tests re-run with network restored; see `docs/evidence/t01-build.md`). T01 is `verified`; the remaining action is the owner's G0 approval of the reuse strategy.

## Not completed
- No Android application of **ours**, APK of ours, content review or Galaxy S21 test exists yet (the APK above is the unmodified upstream candidate build, used as T01 evidence only).
- No local Qwen model was launched; exact installed model/runner unverified (B6).
- No GitHub remote was created, no push occurred, and host GitHub authentication is unverified (B5).
- No protected AGENTS.md file was written because approval was not received.

## Next implementation task
**T03** (G0 closed 2026-09-28): apply ADR 0001 path mapping — import CalisthenicsMemory @ 045b8577 as the `:app` base, add `:domain`/`:data` modules, pin toolchain in `toolchain.lock.md`, add first domain + Compose launch tests, create `scripts/verify.sh` per the command contract in `docs/04-verification.md`.
Then T04 runtime spike on S21 (G1).

## Validation
Package-level test results are recorded separately in `docs/evidence/handoff-validation.md` after execution. These must not be confused with Android/application acceptance.

## Gates
G0 ACCEPTED — closed 2026-09-28. Foundation (ADR 0001: fork CalisthenicsMemory @ 045b8577, GPL-3.0 kept, open source if shared) + policy defaults (ADR 0002, incl. owner decisions A4/C5/F3) + content scope (content/catalog-plan.json) all owner-approved.
G1 NOT RUN — Galaxy S21 offline/background runtime.
G2 NOT RUN — planner/stretch review.
G3 NOT RUN — progression review.
G4 NOT RUN — actual usable hands-free workout.
G5 NOT RUN — backup restore.
G6 NOT RUN — personal pilot.

## Resume checkpoint template
Task/substep; files changed; commands + actual exit codes; evidence; unresolved blocker; next exact action. A new model must reproduce key checks rather than trusting a prior model's summary.

### Current checkpoint (2026-09-28)
- Task: T00 `verified`; T01 `verified` (audit + ADR 0001 + real build of the preferred candidate, all exit codes recorded).
- Files changed (2026-09-28): `tasks.json` (T01 → `verified`, evidence + note updated), `docs/research/reuse-audit.md` (build section updated with real results), `docs/evidence/t01-build.md` (new), `STATUS.md` (this file).
- Commands + exit codes (2026-09-28, isolated faithful copy `/workspace/app/research-checkouts/cm-build-attempt/`, build script diff-verified identical to upstream):
  - `./gradlew assembleDebug --no-daemon` → **exit 0**, `app-debug.apk` 14,673,181 bytes.
  - `./gradlew testDebugUnitTest --no-daemon` → **exit 0**, 209 tests / 0 failures / 0 errors (13 classes).
  - `./gradlew lintDebug --no-daemon` → exit 1, 749 pre-existing upstream style errors (not build-breaking; T24 will baseline lint).
  - `python3 tools/check_handoff.py` → re-run after this update (see Validation below).
- Environment fixes (sandbox-only, recorded in `docs/evidence/t01-build.md`): x86-64 glibc loader for AGP `aapt2` under qemu; aarch64 `libsqlitejdbc.so` for Room's KSP `DatabaseVerifier`. No candidate source changed.
- Unresolved blocker: none for T01. Still outstanding project-wide: S21 not connected (G1), gh unverified (B5), local model runner unverified (B6), G0 not accepted.
- Next exact action: **STOP at G0** — owner approves foundation (ADR 0001) + policy defaults/content scope (T02). After G0: T03 scaffold, T04 runtime spike on S21 (G1).
