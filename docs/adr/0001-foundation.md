# ADR 0001 — Foundation: fork/adapt CalisthenicsMemory

Status: **ACCEPTED — foundation approved by the owner on 2026-09-28.**
Date: 2026-09-25 (proposed), 2026-09-28 (accepted). Evidence: `docs/research/reuse-audit.md`, `docs/evidence/t01-build.md`.

Owner statement (2026-09-28, verbatim): *"Use the faster route — I would want to keep my app open source if it is shared widely."*
Consequence recorded: the fork stays **GPL-3.0** (upstream notices preserved); if the app is ever shared publicly it is released as open source. Final license selection remains a T24 action (see `LICENSE-NOT-SELECTED.md`), but the direction is settled: open source.
Gate note: the *foundation* part of G0 is now approved. The *policy defaults + production content scope* part of G0 (T02, ADR 0002 + `content/` deliverables) is still pending owner review.

## Context

The spec (docs/01) requires a personal-first, offline, local-data Android app
for timed calisthenics circuits with locked-screen audio cues, stretch mode,
assumed-met automatic progression, and portable backup. T01 audited the
survey's four reuse candidates in real checkouts:

- CalisthenicsMemory @ `045b8577d1a5c7fa75d40bf6c1df95244af616f3` (GPL-3.0) —
  the only candidate with a foreground timer service (WakeLock) + SoundPool
  offline cues + interval program model; fully local; stable toolchain.
- Ironvellum @ `d8d51ab35113c071099e25de7842138ce9e9d82e` (GPL-3.0) — rich
  skill-tree data model, but **no timer and no audio** (its service is a
  notification mirror over a set/rep tracker), plus Supabase cloud baked in and
  an aggressive toolchain (AGP 9.4 / compileSdk 37).
- Ballast (web PWA, not Android) and Calistenia (not inspectable in this
  session) are excluded as foundations.

## Decision (proposed)

1. **Foundation = fork/adapt CalisthenicsMemory** pinned at
   `045b8577d1a5c7fa75d40bf6c1df95244af616f3`, imported into this repository
   as the `:app` starting point when G0 passes.
2. **License consequence**: upstream is GPL-3.0. Until the owner decides the
   app's license (see `LICENSE-NOT-SELECTED.md`), the practical safe choice is
   to keep the fork **GPL-3.0** and preserve upstream copyright notices in
   `THIRD_PARTY_NOTICES.md`. A permissive re-licensing of forked code is not
   possible unilaterally.
3. **Ironvellum is reference-only** in this decision: its `Skills.kt` graph
   may be read for design ideas. Importing its code would add file-level GPL
   obligations for content-shaped code; the skill graph is to be built from
   G0-reviewed content in T06 instead.
4. **Structural refactors required by the spec** (recorded now, implemented
   later):
   - Move timing ownership from `WorkoutScreen.kt` into the service
     (RUN-01: single timer owner) — T14/T15.
   - Replace the 1..10 manual level ladder with the spec's progression engine
     (PROG-01..06) — T12/T13.
   - Add stretch allocator invariants (STR-01..05) — T11.
   - Add planner, backup, demo packs — T10, T20, T21.
5. **Fallback**: if the owner rejects a GPL foundation, use **native Kotlin
   from scratch** (T03 scaffold, AGP 8.x/Gradle 8.x/Kotlin 2.0.x to match the
   audited stable toolchain) and re-run T04 first.

## Path mapping (proposed, applied at T03 after G0)

| Upstream (CalisthenicsMemory) | Planned module |
|---|---|
| `app/src/main/java/.../data/` (Room entities, DAOs) | `:data` `db/` + `repository/` (rewritten to spec contracts, T07) |
| `app/src/main/java/.../service/WorkoutTimerService.kt` | `:app` `session/` runtime (extended, T15) |
| `app/src/main/java/.../util/SoundPlayer.kt` | `:app` `audio/` (extended to cue priority/dedup, T15) |
| `app/src/main/java/.../ui/screens/` (Compose) | `:app` `feature/` (reorganized, T16-T19) |
| `app/build.gradle.kts`, `gradle/libs.versions.toml`, wrapper | kept at audited versions; pinned in `toolchain.lock.md` (T03) |

## Consequences

- GPL-3.0 obligation: notices + source availability for modified upstream
  files (T24 inventory).
- T04 (runtime spike + S21 G1) is still the first hard gate: nothing in the
  fork is evidence of spec compliance until A02/A03 pass on the device.
- Build verification of the fork is currently **blocked by sandbox network**
  (Gradle distribution + SDK manifests unreachable; real error retained in
  `/workspace/cm-build-attempt.log`). T03 re-runs the documented build once
  toolchain access exists and must record real exit codes.

## Rejected alternatives

- **Ironvellum base**: rejected — no timer, no audio, cloud deps, newer
  toolchain; would require building the spec's core runtime from scratch while
  carrying Supabase removal.
- **Native Kotlin from scratch**: viable fallback, slower; loses the existing
  timer/audio/Room starting points.
- **Ballast/Calistenia**: not Android foundations (web PWA / uninspected).
