# Ordered implementation workplan

This is a task queue, not a record of completed app development. Every task starts pending. The local model should execute one task or a named substep at a time, using test-first vertical slices. Inspect before editing, preserve upstream licenses, and record real commands/exit codes. Do not fabricate tests, permissions, sources or human approval.

## State and dependency rules
- States: pending, in_progress, blocked, implemented, verified, accepted. A task becomes verified only after required checks actually pass; a human-gated task becomes accepted only after recorded owner approval.
- Every dependency must be verified, or accepted when it carries a human gate. Only one task should be in_progress.
- Keep `STATUS.md` and tasks.json synchronized. Evidence paths can point to ignored local logs; retain sanitized summaries in docs/evidence/.
- No task requires publishing. GitHub creation/push and phone installation require explicit owner authorization.
- G0 approval does not mean sources have validated a universal five-star system. Record which rules are product heuristics.
- Paths below are intended deliverables. T01 maps them to a selected existing repository if reuse changes layout; do not invent existing paths.
- If a task is too large for local-model context, split it into test-first substeps in STATUS without skipping its acceptance criteria.

## Human checkpoints
G0: foundation, policies, named starter library, media rights. G1: real-device runtime feasibility. G2: session generator/stretching. G3: progression/feedback. G4: usable hands-free workout. G5: backup restore. G6: personal pilot.

## Tasks

### T00 — Verify workspace and tool access
Entry: none. Requirements: PLAT-01, SCOPE-02.
Deliverables: `docs/evidence/environment.md`.
1. Confirm the real Mac project folder, Git status, available JDK/Android SDK/adb/Gradle/Python and Galaxy S21 Android/One UI versions when connected. Do not confuse Docker paths with Mac paths.
2. Record local-model runner and exact model ID from installed inventory; do not silently substitute another Qwen version or a cloud model.
3. Check gh authentication without printing tokens; do not publish or change global config. Record unavailable tools as blockers.

Verification:
- pwd; git status --short; git --version; java -version; adb version; python3 --version
- gh auth status only if gh exists; no tokens in evidence

Owner-visible check / stop rule: Owner confirms target folder. Missing JDK/SDK/device requires an installation/device step, not fake build output.

### T01 — Audit reuse candidates and choose one foundation
Entry: T00. Requirements: SCOPE-01, PLAT-01, PLAT-02.
Deliverables: `docs/adr/0001-foundation.md`, `docs/research/reuse-audit.md`.
1. Inspect the actual public repositories linked in the prior survey; verify repository identity, exact SHA, license, build scripts, offline persistence, skill model, timers and background-service implementation.
2. Prioritize Ironvellum and Calisthenics Memory. Compare Calistenia and Ballast for reusable data/ideas if Android bases fail. Record code/data/media licenses independently.
3. Attempt documented build and smallest tests for the preferred candidate in an isolated directory; retain real errors. Choose fork/adapt or native Kotlin fallback with explicit justification and import obligations. No assumed maturity from README claims.
4. Pin the chosen source revision and record path mappings to the planned modules. If no candidate can be audited, stop architecture-dependent work.

Verification:
- git rev-parse HEAD for each inspected checkout
- Run preferred candidate documented build/test; report exit code
- Ensure ADR cites exact source paths and license text

Owner-visible check / stop rule: Owner approves the selected reuse strategy at G0; do not create several competing apps.

### T02 — Approve policy defaults and production content scope
Entry: T01. Requirements: SCOPE-01, PROG-01, PROG-02, PROG-03, PROG-04, PROG-06, STR-02, PLAN-06.
Deliverables: `docs/adr/0002-policies.md`, `content/catalog-plan.json`, `content/source-ledger.json`, `content/licenses.json`.
1. Review every G0 DEFAULT in the coding specification. Produce a decision table with recommendation, evidence/provenance, uncertainty and exact default.
2. Source exercise families/skill prerequisites from credible guidance and inspected existing apps; distinguish guidance from product heuristics. Review recovery/stretch compatibility and early-rep-completion behavior explicitly.
3. Propose a named broad starter catalog plus expansion packs. For each variation record source, five reviewed tiers if progressive, equipment constraints, media creator/license and review owner. Count actual releasable records, not search results.
4. Resolve five-star starting display, family-level discomfort hold, automatic pacing, inactivity prompt, plan bounds and transition rules with the owner. No clinical guarantees. Identify missing licensed demos as blockers.

Verification:
- Every proposed production record has provenance/license/review status
- No proprietary media copied on assumption of public availability
- G0 decisions record owner approval; human approval may not be synthesized

Owner-visible check / stop rule: Review foundation, policy defaults, named catalog and media strategy together. STOP until G0 accepted.

**Human gate: G0 — owner acceptance required before dependent tasks.**

### T03 — Create reproducible Android scaffold and test harness
Entry: T02. Requirements: PLAT-01, SCOPE-02.
Deliverables: `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `gradlew`, `toolchain.lock.md`, `domain/build.gradle.kts`, `data/build.gradle.kts`, `app/build.gradle.kts`.
1. Apply approved foundation and path mapping. Configure :domain, :data and :app contracts or equivalent tested adapters; preserve upstream license notices.
2. Pin compatible stable toolchain/dependency versions and wrapper checksum; record installation commands appropriate to the actual Mac.
3. Add one simple JVM domain test and one Compose launch test. Create scripts/verify.sh with the command contract in verification.md.

Verification:
- ./gradlew --version
- ./gradlew :domain:test :app:assembleDebug :app:lintDebug
- ./gradlew :app:connectedDebugAndroidTest when authorized device/emulator available

Owner-visible check / stop rule: APK opens without login/network dependency. Configuration/generated scaffold is not evidence of product functionality.

### T04 — Prove offline and locked-screen audio feasibility
Entry: T03. Requirements: PLAT-02, AUD-01, AUD-02, RUN-01.
Deliverables: `spikes/android-session-runtime/README.md`, `docs/adr/0003-runtime.md`.
1. Build a disposable instrumented session service with synthetic timed blocks and offline speech/cues, notification controls and observed monotonic timestamps.
2. Read official Android docs for target SDK foreground types, audio focus, wake behavior and permissions. Use only an appropriate documented mechanism; no silent-media survival hack.
3. Exercise lock, background, other audio, call interruption and resume on Galaxy S21 in airplane mode. Record timing and cue results against A02/A03.
4. Discard prototype implementation as needed; retain findings and tests. Stop if the architecture cannot meet the requirement under documented conditions.

Verification:
- Run A02 and A03 real-device checks from verification.md
- Log expected/observed cue times and pause events; no network used

Owner-visible check / stop rule: Owner follows a full-length synthetic timing session with locked screen and music. STOP until runtime strategy passes G1.

**Human gate: G1 — owner acceptance required before dependent tasks.**

### T05 — Implement immutable domain contracts and serialization
Entry: T04. Requirements: PLAN-01, PROG-02, DATA-01.
Deliverables: `domain/src/main/kotlin/model/`, `domain/src/test/kotlin/model/`.
1. Implement typed IDs, exercise/equipment/policy/skill/plan/session records from coding spec; inject Clock and IdGenerator.
2. Make plan and prescription snapshots immutable; distinguish actual metrics, assumed performance and feedback provenance.
3. Add JSON serialization round-trip, invalid-unit/target and enum compatibility tests.

Verification:
- ./gradlew :domain:test --tests "*ModelContractTest*"

Owner-visible check / stop rule: Show one frozen plan and an edited routine side by side; old plan must not change.

### T06 — Implement content/graph validation and real starter pack
Entry: T05. Requirements: UI-01, SCOPE-01, PROG-02.
Deliverables: `tools/validate_content.py`, `content/starter/`, `domain/src/main/kotlin/content/`, `domain/src/test/kotlin/content/`.
1. Implement catalog schema validation, stable ID references, five-tier ordering, graph acyclicity, equipment conditions, per-side pairing and media manifest rules.
2. Import only G0-approved reviewed records and licensed media; keep fictional fixtures exclusively under tests.
3. Verify media hashes, attribution inventory, offline permission and decodability; fail production build on missing reviewed content. No placeholder GIFs accepted as demos.

Verification:
- python3 tools/validate_content.py content/starter
- ./gradlew :domain:test --tests "*CatalogValidationTest*"

Owner-visible check / stop rule: Owner reviews representative push/pull/legs/core/stretch demonstrations and accessible instructions.

### T07 — Implement Room persistence and migration harness
Entry: T05. Requirements: DATA-01, DATA-02.
Deliverables: `data/src/main/kotlin/db/`, `data/src/main/kotlin/repository/`, `data/src/androidTest/kotlin/db/`.
1. Implement schema and repository ports with transactions and uniqueness constraints for session/event IDs; DataStore only for preferences.
2. Persist versioned plan snapshots, block results, feedback revisions and progression events; never overwrite historical prescriptions.
3. Add migration harness and rollback tests; do not use destructive fallback.

Verification:
- ./gradlew :data:testDebugUnitTest
- ./gradlew :data:connectedDebugAndroidTest

Owner-visible check / stop rule: Restart app and confirm routines/settings/history survive; injected write failure leaves prior records intact.

### T08 — Implement equipment profiles, suitability and substitution
Entry: T06, T07. Requirements: EQ-01, EQ-02, EQ-03, EQ-04, PLAN-05.
Deliverables: `domain/src/main/kotlin/equipment/`, `app/src/main/kotlin/feature/equipment/`, `domain/src/test/kotlin/equipment/`.
1. Seed exact approved Home and chair-only Travel profiles; allow quantity/mass/band labels and suitability flags without inventing measurements.
2. Implement OR-of-AND equipment requirements, temporary availability overlays, explicit profile saving and excluded variations.
3. Implement equivalent substitution ranking and clearly labeled non-equivalent alternatives; add optional dismissible equipment suggestions.

Verification:
- ./gradlew :domain:test --tests "*EquipmentTest*"
- Run A05

Owner-visible check / stop rule: Switch Home→Travel→Home; no bar-dependent work in Travel and no progress reset. Dips never earn pull-up credit.

### T09 — Implement familiar routines, focus and optional goals
Entry: T06, T07. Requirements: PLAN-01, PLAN-04, PLAN-05, PLAN-06, PROG-01.
Deliverables: `domain/src/main/kotlin/routine/`, `domain/src/test/kotlin/routine/`.
1. Implement routine slots, revisions, session overlays, Full body/specific-focus normalization and independent stretch focus.
2. Implement save-to-usual-plan explicitly; preserve slot order and select skill-goal practice according to approved G0 policy.
3. Implement reviewed weekly frequency suggestion and on-demand session preparation; no streak penalty or missed-day failure.

Verification:
- ./gradlew :domain:test --tests "*RoutineTest*"

Owner-visible check / stop rule: Upper-body strength can coexist with full-body stretching; temporary swap does not persist without save.

### T10 — Implement deterministic duration-budget planner
Entry: T08, T09. Requirements: PLAN-01, PLAN-02, PLAN-03, PLAN-04, EQ-03, STR-04, STR-05.
Deliverables: `domain/src/main/kotlin/planner/`, `domain/src/test/kotlin/planner/`.
1. Implement PlanInput/Ready/Infeasible, candidate filtering and deterministic ranking, reviewed block timing and optional warm-up/cooldown.
2. Fit complete rounds/pairs into budget with explicit tolerance; never speed movement, truncate recovery or silently pad.
3. Explain every change and every unmet movement goal; reject impossible combinations with actionable alternatives.

Verification:
- ./gradlew :domain:test --tests "*PlannerTest*"
- Run A04 fixture matrix including 10/20/45/90 minutes and impossible inputs

Owner-visible check / stop rule: Owner compares default and short previews, including actual duration and explanations.

### T11 — Implement stretch coverage and timeline invariants
Entry: T10. Requirements: STR-01, STR-02, STR-03, STR-04, STR-05.
Deliverables: `domain/src/main/kotlin/planner/StretchAllocator.kt`, `domain/src/test/kotlin/planner/StretchAllocatorTest.kt`.
1. Fill reviewed recovery windows with compatible stretches when on; ordinary recovery when off; respect focus and exclusions.
2. Preserve left/right pairs and equal duration; distinguish setup transitions from passive recovery.
3. Validate early-completion alternatives and fail infeasible pairing explicitly; do not relabel passive rest as a stretch or setup.

Verification:
- ./gradlew :domain:test --tests "*StretchAllocatorTest*"
- Run A06 and generated-input invariants

Owner-visible check / stop rule: Owner reviews timeline with stretching on/off and optional warm-up/cooldown. STOP at G2.

**Human gate: G2 — owner acceptance required before dependent tasks.**

### T12 — Implement feedback resolution and assumed-met evidence
Entry: T07, T11. Requirements: PROG-05, DATA-01.
Deliverables: `domain/src/main/kotlin/feedback/`, `domain/src/test/kotlin/feedback/`.
1. Implement per-block and per-exercise-row feedback precedence, optional actual metrics and discomfort flag.
2. Only completed blocks without feedback become ASSUMED/MET; skipped/partial/not-started never qualify.
3. Store feedback revisions and expose conflicting actual-versus-rating entries for correction; no fabricated reps.

Verification:
- ./gradlew :domain:test --tests "*FeedbackResolutionTest*"
- Run A07

Owner-visible check / stop rule: Done can close summary without feedback; history explicitly labels assumed target attainment.

### T13 — Implement stars and bounded automatic progression
Entry: T12. Requirements: PROG-03, PROG-04, PROG-06, PROG-01.
Deliverables: `domain/src/main/kotlin/progression/`, `domain/src/test/kotlin/progression/`.
1. Implement the approved exact G0 exposure/window/cap/tier rules as pure functions over versioned history.
2. Implement prerequisite/equipment transitions, family discomfort hold, approved easier adjustment, explicit clearance and overrides.
3. Recompute after late feedback idempotently without changing past prescriptions or repeatedly awarding achievements; keep source and assumption explanations.

Verification:
- ./gradlew :domain:test --tests "*ProgressionTest*"
- Run A08 boundary/date/duplicate/late-feedback matrix

Owner-visible check / stop rule: Owner reviews a simulated multi-week history with assumed successes, lower feedback and discomfort. STOP at G3.

**Human gate: G3 — owner acceptance required before dependent tasks.**

### T14 — Implement pure session reducer and checkpoint rules
Entry: T05, T07, T12. Requirements: RUN-01, UI-02, PROG-05.
Deliverables: `domain/src/main/kotlin/session/`, `domain/src/test/kotlin/session/`.
1. Implement all states/events/effects in coding spec; use fake monotonic clock tests before Android adapter code.
2. Implement pause/resume, skip, early done, easier replacement, finish and process recovery; preserve unfinished block provenance.
3. Make transition persistence idempotent and at most one-second checkpoint loss explicit; no elapsed wall-time auto-completion on restart.

Verification:
- ./gradlew :domain:test --tests "*SessionReducerTest*"
- Run A09 with repeated/late callbacks and process recovery

Owner-visible check / stop rule: Inspect a transition/event trace and demonstrate no duplicate completion or lost pause position.

### T15 — Implement production foreground runtime and offline cues
Entry: T04, T14. Requirements: PLAT-02, AUD-01, AUD-02, RUN-01.
Deliverables: `app/src/main/kotlin/session/`, `app/src/main/kotlin/audio/`, `app/src/main/AndroidManifest.xml`.
1. Implement the approved runtime ADR with single service timer ownership, notification actions and checkpoint persistence.
2. Implement offline cue preflight, cue priority/dedup, preview/countdown/round cues, audio focus and headphone/call pause behavior.
3. Keep video lifecycle independent and release locks/focus/resources on every stop/error/pause path.

Verification:
- ./gradlew :app:testDebugUnitTest :app:lintDebug
- Rerun A02/A03/A09 on S21

Owner-visible check / stop rule: No active workout feature may rely on a foreground Activity or network connection.

### T16 — Implement onboarding and Today/preview flow
Entry: T08, T09, T11, T13, T15. Requirements: ONB-01, PLAN-02, PLAN-03, PLAN-04, PLAN-05, PLAN-06, STR-01, STR-05, PROG-01.
Deliverables: `app/src/main/kotlin/feature/onboarding/`, `app/src/main/kotlin/feature/plan/`, `app/src/androidTest/kotlin/plan/`.
1. Build short assessment/manual variation selection with conservative mapping, no maximum-effort requirement.
2. Build duration/focus/profile/stretch/warm-up/cooldown controls and routine preview, swaps, explanations, explicit saves and start preflight.
3. Remember correct settings only; display blocked content/constraints rather than showing a start button that cannot work.

Verification:
- ./gradlew :app:connectedDebugAndroidTest
- Run A04/A05/A06 with real UI

Owner-visible check / stop rule: First launch to offline workout preview without account creation.

### T17 — Implement active session screen and optional feedback
Entry: T15, T16. Requirements: UI-01, UI-02, PROG-05, AUD-01.
Deliverables: `app/src/main/kotlin/feature/session/`, `app/src/androidTest/kotlin/session/`.
1. Render current demo/name/timer/target/round/next/progress from service state; do not implement a second timer.
2. Add accessible pause/resume, skip, easier, feedback and stop; no interaction required for normal playback.
3. End summary supports optional exercise-row feedback, actual values, discomfort and Done without entries. Rotations/recreation preserve state.

Verification:
- ./gradlew :app:connectedDebugAndroidTest
- Run A07/A09

Owner-visible check / stop rule: Complete a circuit hands-free, then try each optional control. STOP at G4 after an actual usable offline session.

**Human gate: G4 — owner acceptance required before dependent tasks.**

### T18 — Implement skill tree and library browsing
Entry: T06, T13, T17. Requirements: PROG-01, PROG-02, PROG-03, UI-01, EQ-04.
Deliverables: `app/src/main/kotlin/feature/progress/`, `app/src/main/kotlin/feature/library/`.
1. Render graph and accessible grouped list from same validated node/edge data; distinguish prerequisites from recommended preparation.
2. Show per-variation stars/next criteria/provenance and approved goal selection; retain historic achievements.
3. Show demonstrations, technique/equipment requirements, provenance/attribution and optional equipment suggestions.

Verification:
- ./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest
- Run A10

Owner-visible check / stop rule: Browse current exercise toward a skill and back; no need to select a goal to use the app.

### T19 — Implement history, weekly summary and late edits
Entry: T13, T17. Requirements: DATA-01, PLAN-06, PROG-05.
Deliverables: `app/src/main/kotlin/feature/history/`, `data/src/androidTest/kotlin/history/`.
1. Show completed/partial sessions, duration, skipped blocks, assumed/user ratings and progress over time.
2. Compute weekly sessions using saved time zones; do not punish missed weeks.
3. Allow feedback correction through versioned events/recomputation; never overwrite the original plan.

Verification:
- ./gradlew :domain:test :data:connectedDebugAndroidTest
- Run A07/A08 history edits

Owner-visible check / stop rule: Edit old feedback; observe explained recomputation with no duplicate sessions or changed old targets.

### T20 — Implement safe local export/import backup
Entry: T07, T19. Requirements: DATA-02, PLAT-02.
Deliverables: `data/src/main/kotlin/backup/`, `app/src/main/kotlin/feature/settings/`, `data/src/androidTest/kotlin/backup/`.
1. Implement SAF export with versioned logical records, checksums and content snapshots/pack references; explain unencrypted data and excluded demo binaries.
2. Implement validated staged replace-only import, explicit confirmation, pre-import recovery copy, schema checks and atomic transaction.
3. Reject active-session import, corrupt/future/oversized/traversal archives without touching live data; disable implicit OS cloud backup for v1.

Verification:
- ./gradlew :data:connectedDebugAndroidTest
- Run A11 malicious/corrupt fixtures and clean-install restore

Owner-visible check / stop rule: Owner restores an export into clean app storage and compares history/settings/progression. STOP at G5.

**Human gate: G5 — owner acceptance required before dependent tasks.**

### T21 — Implement versioned downloadable demonstration packs
Entry: T06, T17, T20. Requirements: UI-01, PLAT-02, SCOPE-01.
Deliverables: `data/src/main/kotlin/packs/`, `app/src/main/kotlin/feature/library/`, `data/src/androidTest/kotlin/packs/`.
1. Implement approved HTTPS manifest origin, staged download/hash/size/path validation and atomic activation; no demo downloads while active.
2. Preserve old pack on interruption/corruption and protect packs referenced by active sessions. Test version incompatibility, storage full and cancellation.
3. Configure a real owner-approved content endpoint with reusable media; verify manifest and actual files. If no endpoint/license, mark blocked, not completed.

Verification:
- ./gradlew :data:connectedDebugAndroidTest
- Run A12 offline/HTTP/error fixture matrix

Owner-visible check / stop rule: Download a pack, go offline, use demos; failed update leaves old content usable.

### T22 — Harden interruption, migration and offline edge cases
Entry: T18, T20, T21. Requirements: RUN-01, PLAT-02, DATA-02, AUD-02.
Deliverables: `app/src/androidTest/kotlin/reliability/`, `data/src/androidTest/kotlin/migration/`.
1. Exercise calls, background/lock, Bluetooth disconnect, process kill, force stop, reboot, notification actions and interrupted export/download.
2. Verify unknown time never becomes training, migrations preserve real fixtures, and no work is double-counted.
3. Document force-stop/OS limits honestly; resumption can require reopening app. Test airplane mode from cold start.

Verification:
- ./gradlew :domain:test :data:connectedDebugAndroidTest :app:connectedDebugAndroidTest
- Run A02/A03/A09/A11/A12

Owner-visible check / stop rule: Owner repeats interruption checklist on S21; every unsupported case is surfaced, not hidden.

### T23 — Accessibility, UI polish and resource checks
Entry: T22. Requirements: UI-02, PLAT-01, AUD-01.
Deliverables: `app/src/main/kotlin/ui/`, `docs/evidence/accessibility.md`.
1. Test TalkBack, large fonts, contrast, control reachability and portrait/rotation behavior; stars/status require text labels.
2. Measure cold start, database queries, media memory and full-session battery/thermal behavior on actual device; record device-specific baseline.
3. Fix observed usability problems without changing policies or adding features.

Verification:
- ./gradlew :app:lintDebug :app:connectedDebugAndroidTest
- Run A13

Owner-visible check / stop rule: Owner can pause/stop without scrolling and read targets at large font.

### T24 — CI, repository hygiene and licensing review
Entry: T23. Requirements: SCOPE-01, SCOPE-02, DATA-02.
Deliverables: `scripts/verify.sh`, `.github/workflows/android.yml`, `THIRD_PARTY_NOTICES.md`, `docs/evidence/release-preflight.md`.
1. Run reproducible full verification; CI must fail on test/content/lint failure and must not require private user data.
2. Validate licenses for all imported code/data/media and select project license compatible with approved foundation; keep signing keys and private evidence outside Git.
3. Check tracked files for secrets, user history, machine paths and large media; prove clean-clone build. No automatic upload.

Verification:
- bash scripts/verify.sh
- git diff --check; git status --short
- Run clean-clone build and secret/size preflight

Owner-visible check / stop rule: Owner sees license inventory, actual test results and remaining known limitations.

### T25 — Personal pilot and acceptance
Entry: T24. Requirements: PLAT-01, PLAT-02, PLAN-01, PROG-04, STR-02, UI-02, DATA-01.
Deliverables: `docs/evidence/pilot-summary.md`.
1. Run real full sessions with the owner: Home default, shorter targeted session and chair-only Travel; use approved content only.
2. Review progressive changes through deterministic history replay, then observe ordinary usage over multiple sessions without mandatory feedback.
3. Record bugs/limitations with reproduction steps and regression tests; do not close acceptance from emulator tests alone.

Verification:
- Run A01 through A13; record PASS/FAIL/BLOCKED and evidence per check
- All blocker issues resolved and regression suite rerun

Owner-visible check / stop rule: Owner accepts G6 for personal use. STOP if any critical runtime/data/content requirement fails.

**Human gate: G6 — owner acceptance required before dependent tasks.**

### T26 — Package installable release and optional GitHub handoff
Entry: T25. Requirements: PLAT-01, SCOPE-01, SCOPE-02, DATA-02.
Deliverables: `docs/INSTALL.md`, `docs/RELEASE.md`.
1. Produce APK with checksums and version metadata; record signing-key retention/backup outside Git, supported device and limitations.
2. Document authorized adb/manual installation and non-destructive update/rollback with export safeguards; demonstrate update retains history.
3. Check actual GitHub authentication, confirm account/repo name/visibility, then only if explicitly authorized create/push. Read remote commit SHA back and compare local HEAD.

Verification:
- ./gradlew :app:assembleRelease only after real signing config is supplied securely
- Verify APK SHA-256 and update install on authorized S21
- git ls-remote origin refs/heads/main after authorized push

Owner-visible check / stop rule: Deliver APK/source/install instructions and actual verification summary. No claim of GitHub upload without matching remote SHA.
