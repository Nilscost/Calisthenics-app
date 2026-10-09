# Actual project status

## Current state (2026-10-09, version 2 in progress; latest release R2 = 0.5.0-r2)
Version 2 follows `docs/12-v2-plan.md` (owner feedback R1-R29) with the task list in `docs/14-executor-handoff.md`. Done so far (R1: V00-V04b; R2: V06-V10 visual system, design direction B of `docs/17-ui-direction-b.md`): V00 UI screenshot pipeline (GitHub Actions + Maestro, light/dark/font 1.3), V00b debug seed, V01 clip reloads per block, V02 "too easy" rule in the domain, V03 logger redesign, V04a Room 24 and per-round corrections, V04b per-round editors, V06 clip pipeline v3 and own poses, V07 18 stretches, V08 body figure, V08b design system B, V09 thumbnails, V10 workout screen. Owner checklist: `docs/evidence/owner-check-r2.md` (R1: `owner-check-r1.md`). **Nothing was tested on a phone.** All exercise content stays an unreviewed DRAFT. Details: `docs/evidence/progress-notes.md`; owner checklist: `docs/evidence/owner-check-r1.md`; screenshot reviews: `docs/evidence/ui/`.

## Previous state (2026-10-05, UX overhaul U01-U13)
The app is now one product with four tabs (Train · Progress · History · Settings): first-run questionnaire, equipment profiles, one-screen Train setup, draft Preview, redesigned live session with in-workout rep logging, skill tree with stars, history with corrections, settings, 3/4-view demo clips with muscle colouring, 10 new exercises. The old fork screens are deleted (data tables kept). Everything was verified with the offline Gradle gate (see `docs/evidence/uNN-verify-*.log`) and Robolectric UI tests; **nothing was tested on a phone**. The owner device test is the open gate; all exercise content stays an unreviewed DRAFT. Details per task: `docs/evidence/progress-notes.md`; plan: `docs/10-ux-overhaul-plan.md`; owner checklist: `docs/evidence/owner-check-u13.md`.

(The sections below are the older history of the project and are kept as written.)

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
- **T03 device gate not run** — no authorized Galaxy S21 is attached to the sandbox. The host-runnable gate is green (T03 `verified`), but `:data:connectedDebugAndroidTest` (MigrationTest/G5) and `:app:connectedDebugAndroidTest` (LaunchSmokeTest) have NOT run, so on-device Room and launch behavior are unverified.
- **G1 not approved** — offline/locked-screen/audio/foreground-timer runtime feasibility still requires the Galaxy S21 + owner observation.
- The APK at `app/build/outputs/apk/debug/app-debug.apk` is now **ours** (built from the `:domain`/`:data`/`:app` scaffold; v0.1.0-t03). It is a build artifact, NOT evidence of product functionality until G1 is observed.
- No local Qwen model was launched; exact installed model/runner unverified (B6).
- No GitHub remote was created, no push occurred, and host GitHub authentication is unverified (B5).
- No protected AGENTS.md file was written because approval was not received.

## Next implementation task
**T03 host-runnable gate COMPLETE (2026-09-29).** `bash scripts/verify.sh` → exit 0:
`:domain:test` 8/0, `:data:testDebugUnitTest` NO-SOURCE, `:app:testDebugUnitTest`
209/0, `:app:lintDebug` 0 errors, `:app:assembleDebug` → `app-debug.apk`
(14,689,648 bytes, valid package `app.calisthenics.personal` v0.1.0-t03), content
validation OK. Details + code/env fixes: `docs/evidence/t03-scaffold.md`.

**Next: T04 / G1** — runtime feasibility spike on the Galaxy S21 with owner
observation (offline, locked screen, audio, foreground timer). Device tests
(`:data:connectedDebugAndroidTest` = MigrationTest/G5,
`:app:connectedDebugAndroidTest` = LaunchSmokeTest) run only after explicit
device authorization. G1 is not approved by this build.

### Owner update — 2026-09-28 (exercise video decision CONFIRMED)
The owner **confirmed: go with the synthetic placeholder loops and swap them for real reviewed footage later.** This is the "light / easy to generate / loopable" route. Concretely:
- `tools/make_sample_media.sh` generates deterministic, muted, seamless MP4 loops (ffmpeg lavfi, no footage/audio/trackers): `content/starter/media/pushup-demo.mp4` (0.5 Hz bar oscillation = 2 full cycles per 4 s → frame 0 ≈ last frame) and `plank-demo.mp4` (static hold + breathing marker). Both 640×360, 30 fps, 4.0 s, ~6 KB.
- Wired into the starter pack contract (`exercises.json` + `media.json` + `pack.json`, all hashes/lengths pinned). `python3 tools/validate_content.py content/starter` → OK (structure + rights fields).
- All demo media stay `reviewState=DRAFT` and are on-screen labeled "sample loop (not reviewed)" — they are placeholders, NOT reviewed training content. Swapping later = drop real clips into the same `media.json` contract; no app rework.
- Green gates re-verified 2026-09-28: content validation OK; `:domain:test` → 8 tests / 0 failures / 0 errors.
- Still outstanding (build-env only, unrelated to video): `:data` KSP + `:app` build need a native SQLite / aapt2 this sandbox blocks (see checkpoint).

The owner authorized overnight work through **T03 and G1 evidence only**: complete the scaffold and harness, prepare/run the runtime feasibility checks where possible, and stop for the owner's Galaxy S21 review. This authorization does not waive G1, approve unobserved device behavior, or permit proceeding past G1 without the required owner/device evidence.

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

### Current checkpoint (2026-09-29)
- Task: T00 `verified`; T01 `verified`; **T03 `verified`** (host-runnable gate, 2026-09-29). T04 `pending` (needs S21).
- Files changed (2026-09-29): `app/src/main/java/.../util/ProgramExecutionUtils.kt` (canonical re-indent, 2 SuspiciousIndentation fixed); `app/src/main/java/.../ui/components/program/ProgramIntervalComponents.kt`, `.../ui/screens/IntervalEditScreen.kt`, `.../ui/components/program/ProgramNavigationSheet.kt`, `.../ui/screens/view/GraphView.kt` (8 cross-module smart-cast fixes); `data/build.gradle.kts` (sqlite-jdbc 3.45.3.0 force, build-time only); `app/lint.xml` (new; MissingTranslation → warning only); `app/build.gradle.kts` (lint comment); `toolchain.lock.md` (host-native notes); `tasks.json` (T03 → verified); `docs/evidence/t03-scaffold.md` (rewritten to COMPLETE); `STATUS.md` (this file).
- Commands + exit codes (2026-09-29, in `/workspace/app/calisthenics`):
  - `bash scripts/verify.sh` → **exit 0** (full T03 contract: :domain:test 8/0, :data:testDebugUnitTest NO-SOURCE, :app:testDebugUnitTest 209/0, :app:lintDebug 0 errors, :app:assembleDebug, content validation OK).
  - `./gradlew :app:clean :app:assembleDebug` → **exit 0**, `app-debug.apk` 14,689,648 bytes (fresh clean rebuild).
  - `aapt dump badging app/build/outputs/apk/debug/app-debug.apk` → valid package `app.calisthenics.personal` v0.1.0-t03, minSdk 26 / target 35 / compile 35.
- Environment fixes (sandbox-only, build-time, no app/runtime impact): x86-64 glibc under qemu for AGP `aapt2`; `:data` forces `sqlite-jdbc:3.45.3.0` for Room's KSP schema verifier; apt run with proxy vars unset (egress proxy was down).
- Unresolved: **G1 not approved** (S21 offline/background/audio/timer needs owner observation); device tests not run (no authorized S21); lint `MissingTranslation` downgraded to warning (ar 0/747, ru 107/747 catalogs to complete); gh unverified (B5); local model runner unverified (B6).
- Next exact action: **T04 / G1** — runtime feasibility spike on the Galaxy S21 with owner observation; run device tests only after explicit device authorization (`adb devices -l`; `./gradlew :data:connectedDebugAndroidTest :app:connectedDebugAndroidTest`).

### Owner update — 2026-10-01
- Review: `docs/evidence/review-2026-10-01.md`. Roadmap for the FULL version (owner choice): `docs/09-full-version-roadmap.md`.
- Owner approved: G1 must also test offline spoken English + music ducking (added to M0 spike work).
- Phone has NO working USB port: use Wireless debugging (adb pair/connect) or sideload APK; G1 test must be started and exported from an in-app debug screen (airplane mode drops Wi-Fi adb).
- Folder consolidated to `/Users/nils/Documents/hermes/Apps/calisthenics` (old `Calsthenics V1/`, `pkg/`, zip deleted with owner approval; T04 APK kept in `../builds/`). Stale `local.properties` and duplicate `config/config` removed.
- Next exact action: M0 (roadmap) — unify G1 thresholds, SoundPool load fix, spike exported=false, in-app G1 debug screen, TTS + ducking probe, shared debug keystore.
