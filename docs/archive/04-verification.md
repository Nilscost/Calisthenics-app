# Verification and human acceptance

## What has and has not run
Handoff lint/tests can run now. The Android commands below are the contract T03 must implement, not evidence that an app currently exists. Manual checks remain NOT RUN until performed on an actual device. A local model must report BLOCKED rather than invent evidence.

## Command contract
Current handoff only:
```sh
python3 tools/check_handoff.py
python3 -m unittest discover -s tests -p 'test_*.py' -v
git diff --check
```

After T03, from the Android project root:
```sh
./gradlew --version
./gradlew :domain:test :data:testDebugUnitTest :app:testDebugUnitTest
./gradlew :app:lintDebug :app:assembleDebug
python3 tools/validate_content.py content/starter
```
On an authorized connected device/emulator:
```sh
adb devices -l
./gradlew :data:connectedDebugAndroidTest :app:connectedDebugAndroidTest
```
`adb devices` is only device discovery, not permission to install or control a phone. Ask before installation/debugging changes. `scripts/verify.sh` created in T03 must use fail-fast exit handling and these steps; do not create a script that exits zero when Gradle/content checks are missing. Record precise task name adjustments in the foundation ADR if an existing base changes modules.

## Evidence format
For each run: scenario ID, app commit, APK SHA-256, device model + Android/One UI version, time, prerequisites, exact actions, expected result, actual result, PASS/FAIL/BLOCKED, log/screenshot path and owner acceptance if needed. Use ignored `evidence/` for private logs and sanitized `docs/evidence/` for versioned summaries. Never record tokens, phone numbers from calls, personal audio or private notifications.

Statuses: NOT RUN → PASS or FAIL or BLOCKED. Human acceptance is a separate field. A checkbox without evidence is not a pass.

## Automated suites and golden fixtures
Test source files should carry stable scenario IDs in test names/comments. Test actual domain logic, not mocks that reproduce expected values. Inject clock, catalog and repositories. For each bug add a failing reproducer before fixing it. Unit, database, UI and device tests prove different things.

| Suite | Required cases |
|---|---|
| ModelContractTest | round trips, stable IDs, null actual reps with assumed success, frozen snapshots, reject negative/overflow time/mass |
| CatalogValidationTest | five ordered tiers when progressive, unknown refs, graph cycle, invalid prerequisites, missing license/demo/hash, duplicate IDs, invalid bilateral data |
| EquipmentTest | OR/AND requirements, quantity/mass, unknown band strength, missing anchor, unsuitable chair, home/travel overlay, no false pulling credit |
| RoutineTest | focus union, Full body exclusivity, separate stretch focus, temporary swap vs saved revision, goal edit preserves history |
| PlannerTest | deterministic same input, reviewed minima, exact duration arithmetic, paired blocks, short infeasible input, no hidden padding, exclusions, unavailable goals |
| StretchAllocatorTest | no passive blocks when on, recovery retained when off, left/right balance, limited coverage warning, impossible pairings, transitions not disguised rest |
| FeedbackResolutionTest | completed-only assumptions, exercise versus block priority, actual target conflict, partial/skip not MET, discomfort wins, no mandatory feedback |
| ProgressionTest | exact exposure/window boundaries, one advancement cap, no calendar-only growth, assumed source labels, tier-five transition, equipment/prerequisite holds, duplicates/late edits, discomfort clearance, inactivity |
| SessionReducerTest | pause/resume same remaining time, delayed ticks, idempotent effects, skip semantics, early completion, easier amendment, partial finish, restart paused, boot-clock reset |
| Backup/pack tests | round trip, invalid schema, bad hash/length, zip traversal/symlinks/bombs, atomic failure, full disk, active session restrictions, migration, no duplicate IDs |

Test-only synthetic exercises may simplify fixtures. They must be visibly marked TEST_ONLY and excluded from release assets. A green synthetic fixture does not validate a real training policy.

### Required numeric progression fixtures (after G0 approval)
For the proposed policy, enroll at current tier on day 0: three qualifying exposures on days 0/3/7 advance by one tier; days 0/2/6 do not. Extra rounds or same-day sessions do not accelerate progression. Three exposures at seven days with one BELOW do not advance. A skipped required block prevents that exposure qualifying. Another advancement less than seven days after the previous one is refused. Completion with no feedback advances under the same rules, with ASSUMED provenance. Discomfort followed by unlimited silent sessions remains on hold; clearance alone does not award a tier. Reprocessing identical events produces identical output and no new award. Update fixtures if G0 changes these values; document the approved change rather than editing tests merely to pass.

## Device/user scenarios

### A01 — First use and personal defaults
Fresh local install; network off; no account. Complete short assessment or choose familiar variations. Expect 45-minute default, Home profile, stretch on/full body, warm-up/cooldown off, English guidance. Inspect exact Home equipment and chair-only Travel. Change settings and verify only approved preferences persist. Do not seed the user's workout history or fabricate measured ability.

### A02 — Full-duration offline locked-screen runtime (G1)
Use a synthetic 45-minute timer schedule for feasibility, then approved real content later. Airplane mode from cold start; media downloaded/bundled. Lock screen and keep it locked through several transitions, with another app foregrounded before locking. Run once on normal battery settings and record them.
Expected: cues/timing continue; no network needed; no dropped/duplicate block transitions. Proposed engineering tolerance: each start cue within one second of its intended boundary, final timer within two seconds of the planned duration excluding genuine pauses, no audible overlapping speech. Measure expected and observed timestamps; do not estimate from "felt fine". If OS restrictions prevent this, G1 fails until resolved or owner approves a changed requirement.

### A03 — Other media, call and output interruption
Play a music/podcast app, start workout, hear a cue, receive a test call without recording its content. Also disconnect headphones separately.
Expected: cue briefly ducks other media and restores it; call/headphone interruption pauses workout; focus restoration does not resume it. Owner resumes and remaining time is preserved. Test speaker and Bluetooth if used. Record actual media-app differences; no claiming universal ducking from one app.

### A04 — Plan budget, familiar routine and manual edits (G2)
Generate 45, 20 and 10 minute sessions; compare slot ordering and round counts. Toggle warm-up/cooldown, choose upper body+core, edit a timing, swap an exercise.
Expected: total block/transition sum equals displayed planned time; never exceeds selected budget; any underfill beyond approved tolerance needs acceptance. No below-minimum recovery, accelerated movement or silent goal loss. Same inputs give same plan. Temporary edits vanish on a new session; saved edits create a routine revision without rewriting prior sessions.

### A05 — Travel and substitution
Record progress, select chair-only Travel, exclude an exercise, then switch back Home.
Expected: no bar/band/weight exercise without required equipment/capabilities; unsuitable-chair constraints honored; unavailable pull movement explained, not mislabeled as dips; no pull-up credit for substituted non-pulling exercise. Home progress/routine survives. Equipment suggestions are optional and dismissible.

### A06 — Stretching modes and bilateral balance (G2)
Generate same plan with stretches on and off; choose calf/hip focus; exclude a relevant stretch; reduce session duration; test optional early-completion control.
Expected: on has no PASSIVE_RECOVERY blocks or disguised rest transitions; off retains ordinary recovery. Left/right equal where unilateral; focus and coverage truthful. Impossible pairing blocks start with alternatives. Optional Done never forces extra reps or silently manufactures passive rest. Human voluntary rest/pause remains possible. Warm-up/cooldown remain independent.

### A07 — Optional feedback and partial work
Complete some blocks, skip one, finish early, leave feedback blank; then repeat with per-block and exercise-row ratings.
Expected: only completed work defaults to assumed met; actual repetitions remain null unless entered; partial/skipped blocks do not earn success. No mandatory review form. Explicit block feedback overrides row defaults; discomfort overrides successful ratings. Weekly/history screen distinguishes partial and completed work.

### A08 — Multi-week progression replay (G3)
Replay the approved numeric fixtures above with injected dates and a reviewed test catalog. Show owner stars, next targets and explanations, including assumed evidence, below-target results, late feedback and discomfort.
Expected: bounded automatic changes without routine approval prompt, no inactivity-based gains, no repeated achievements, no form verification claims, no progress reset on equipment/goal changes. Discomfort hold survives missing feedback/app restart until explicit clearance. Historical prescriptions never mutate.

### A09 — Session controls, recreation and crash recovery (G4)
Pause/resume repeatedly, rotate/recreate UI, switch apps, skip a block, choose easier alternative, then force process death in a test build and reopen. Separately force-stop and reboot; do not conflate these with ordinary process death.
Expected: one timer owner, no duplicate cues/completion, correct remaining time, at most one second explicitly uncheckpointed work on recovery. Reopening offers resume/finish early; downtime does not auto-complete blocks. Force-stop may require reopening and must not be falsely advertised as background survival. No erased completed records.

### A10 — Library and skill map
Inspect demo/instructions for representative families and browse prerequisites. Choose/remove a goal and compare routine.
Expected: same graph/list relationships, distinct required versus recommended edges, stars per variation with criteria and evidence labels, media attribution, no ghost/unlicensed demos or fabricated mastery.

### A11 — Backup, corruption and migration (G5)
Export real test state; restore to a clean app; compare preferences, routines, profiles, snapshots, ratings and progression. Try interrupted export/import, bad hash, future schema, duplicate IDs and traversal fixture; cancel confirmation; attempt import during a session.
Expected: successful logical round trip, no duplicates, atomic failure leaves prior state unchanged, recoverable pre-import backup. Explain excluded demo binaries and redownload requirements. Upgrade an older schema fixture without destructive reset.

### A12 — Content packs and offline readiness
Install approved pack from real endpoint; airplane mode; use demos; cancel/update download; simulate malformed manifest, wrong hash and low storage.
Expected: staged/atomic activation, old pack stays usable, no missing media accepted silently, no network requirement during active training. Active-session referenced content cannot be removed. Actual production URLs and licenses must exist before this passes.

### A13 — Accessibility and pilot (G6)
Use large font and TalkBack; test visual states without relying on color; pause/stop without scrolling. Run approved Home default, short targeted and Travel sessions.
Expected: owner can follow without touching phone during normal operation, readable controls, no blocker data/audio/timer problems. Retain measured resource observations and known limitations, not invented universal battery claims.

## Release gate
All approved requirement IDs must map to a task and at least one automated and/or device test, with both required where appropriate. All G gates accepted. No unresolved critical runtime/data/license/content issue. APK built from exact recorded commit; signing key retained privately; update preserves history; rollback documented using a compatible export. A clean build alone does not constitute personal acceptance.
