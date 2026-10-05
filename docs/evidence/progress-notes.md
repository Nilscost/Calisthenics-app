# Progress note — 2026-10-02 (stopped on owner request)

## Done and saved in the project (Mac folder, git commit `4569f3e`)
- Review of the local model's work: `docs/evidence/review-2026-10-01.md`
- Full-version roadmap M0–M9: `docs/09-full-version-roadmap.md`
- Folder consolidated to `~/Documents/hermes/Apps/calisthenics`. Deleted the old copies (`Calsthenics V1/`, `pkg/`, zip) with your approval. T04 APK kept in `Apps/builds/` (checksum OK).
- Removed the broken `local.properties` and the duplicate `config/config` folder.
- `STATUS.md` updated with your decisions: G1 also tests spoken cues and music lowering, the phone has no USB, build the full version.

## Build environment (sandbox)
- Java 21 installed in the Hermes sandbox. `./gradlew --offline :domain:test` → **BUILD SUCCESSFUL** (the existing 8 tests).
- Android SDK download **started, then stopped** on your request, before it finished, to save hotspot data. The full Android app cannot be built yet.

## Work in progress: NOT compiled, NOT tested, NOT in the Mac project
These are draft "phone-free" building blocks. They live only in the sandbox copy `/workspace/cal` (cloned from `4569f3e`):

| File | Roadmap task | State |
|---|---|---|
| `domain/build.gradle.kts` | adds JSON serialization + a `review` report task | written |
| `domain/.../model/Model.kt` | T05 data model (exercises, tiers, equipment, routine, plan blocks) | written |
| `domain/.../equipment/Equipment.kt` | T08 Home/Travel profiles, equipment matching, optional suggestions | written |
| `domain/.../content/CatalogValidator.kt` | T06 catalog/skill-graph checks (IDs, 5 tiers, cycles, dangling links) | written |
| `domain/.../routine/Routine.kt` | T09 focus rules, today-only vs saved changes, weekly summary | written |
| `domain/.../feedback/Feedback.kt` | T12 optional feedback, "assumed met" labelling, evidence per session | written |
| `domain/.../progression/ProgressionEngine.kt` | T13 stars, slow automatic progression, discomfort hold, 14-day re-entry | written, mid-edit |

Not started: the starter `catalog.json`, the planner (T10/T11), the session engine (T14), the review report, and all unit tests for the new code.

## To resume
1. Compile `/workspace/cal` with `./gradlew --offline :domain:test` and fix the errors.
2. Write tests per file. Then copy the code into the Mac project and commit only once the tests pass.
3. Continue with T10/T11 planner → T14 session engine. Then M0 phone-test fixes, which need the Android SDK (~3–5 GB, so wait for proper Wi-Fi).

## Update 2026-10-04
Compiled and tested the draft code in the sandbox. `./gradlew --offline :domain:test` → BUILD SUCCESSFUL, 55 tests, 0 failures (8 existing + 47 new). Log: `domain-tests-2026-10-04.log`.
Committed: T05 model, T06 catalog validator, T08 equipment, T09 routine/focus, T12 feedback, T13 progression engine (domain module only).
Not started: starter `catalog.json`, planner (T10/T11), session engine (T14), M0 phone-test fixes (need Android SDK).
Known gap: the engine's equipment prerequisites are left to the planner; the owner-review report task was removed until it exists.

## Update 2026-10-04 (2) — planner + session reducer
`./gradlew --offline :domain:test` → BUILD SUCCESSFUL, 110 tests, 0 failures. Log: `domain-tests-2026-10-04-planner-session.log`.
Added: T10/T11 planner (`planner/Planner.kt`, 30 tests), T14 session reducer (`session/SessionReducer.kt`, 25 tests).

### Choices I made that are NOT in the approved ADR (owner review at G2)
- MAX_ROUNDS = 6 (cap on circuit repetitions).
- Minimum stretch segment 10 s (constant defined, not yet enforced).
- Unilateral stretches split the recovery window in two equal halves (L/R); a 20 s window gives 10 s per side.
- Slots are dropped only if `optional`; optional slots are dropped from the END of the routine first.
- Skill-goal planning (ADR B4) is NOT implemented: the planner warns and ignores a selected goal.
- Equipment prerequisites for progression successors are not checked in the engine.

### Still open
Starter catalog (`content/starter/catalog.json`) in the new model; Android side (M0 fixes, Room, UI, service); G1 phone test.

## Update 2026-10-05: G1 approved by owner; M5 first slice
- G1 approved with the Spotify no-ducking limit accepted (ADR 0003 updated).
- New `session/WorkoutSessionService` (foreground, service-owned monotonic timer driving the pure reducer; SoundPool + offline TTS; focus MAY_DUCK, refusal ignored; saves finished session to Room) and `SessionScreen` (pause/resume/skip/finish). Start on Today is enabled.
- NOT built yet: mid-session checkpoint persistence/crash recovery (Persist effects ignored), feedback prompts after blocks, late feedback edits, reopening the live session from Home after leaving the app, audio-interruption pause, removing the spike/G1-test code. Not device-tested.

## Update 2026-10-05 (2): M5 slice 2 — crash-safe checkpoints + feedback
- Service writes a checkpoint (SharedPreferences, commit()) at every reducer Persist effect and at least every 5 s; cleared only after the session row is in Room.
- Today offers "Continue it (paused)" / "Save what was done and end" for an unfinished checkpoint; recovery uses reducer ProcessRecovered + RecoveryChoice (old deadlines never trusted).
- End screen: optional per-exercise rating (Too hard / As planned / Easy) + Discomfort; each tap adds a new feedback revision; untouched = assumed met.
- Verified: unit tests (CheckpointTest round-trip + recovery), lint, assembleDebug. NOT tested: real process kill on the phone; feedback revisions are stored but do not yet feed the progression engine at the next plan; late edit from History not built.

## Update 2026-10-05 (3): M5 slice 3 — feedback drives progression, cleanup
- ProgressLoader replays Room history (sessions, block results, latest feedback revision) + self-assessed levels through ProgressionEngine; Today plans from it.
- History: tap a past workout to correct feedback (new revision; progress recomputes on next open).
- Auto-pause when a phone call/ringtone/communication audio mode is detected (AudioInterrupted).
- G1 spike service, G1 test screen, androidTest spike and debug manifest REMOVED (evidence kept in docs/evidence/g1).
- Verified: unit tests, lint, assembleDebug, compileDebugAndroidTestKotlin. NOT device-tested. No unit test yet for ProgressLoader (needs Room); call detection by audio mode is unverified on the S21.
- Videos: none exist. No player, no media, no hosting (T21 blocked on owner decision).

## Update 2026-10-05 (4): demo clips, licence, chair
- Owner decisions: GPL-3.0 (LICENSE = GPLv3 text); Travel chair "stable"; audience = owner + a few friends, keep distributable; full history restore wanted; demo videos = DRAWN ANIMATIONS delivered as short video clips (owner will not film).
- tools/gen_demo_clips.py renders 20 side-view stick-figure MP4 loops (480x360 H.264, ~590 KB total) into app/src/main/assets/demos/<variationId>.mp4. Shown in Library and on the live session screen (muted, looping, VideoView). Checked by eye on contact sheets: most read well; superman hold, band pull-up and cat-cow are the weakest. Artwork is original (generated), no third-party footage.
- Not built: history restore, pre-import safety copy.

## UX overhaul (docs/10, handoff docs/11)

### U01 — navigation shell (2026-10-05)
- New `ui/nav/AppNav.kt`: bottom bar Train · Progress · History · Settings; full-screen routes Session, Onboarding (first run + redo), Backup, Licenses. `HomeScreen` and the old `Screen` sealed class are gone from navigation; `MainActivity` now only hosts the theme + `AppNav`. Train = old TodayScreen, Progress = old LibraryScreen, History = old HistoryScreen (all still the old content; redesigned in later tasks). Settings = new minimal `SettingsScreen` (questionnaire redo, backup, licences).
- `OnboardingStore` (done flag; existing installs with saved levels count as done). Fresh install → old levels screen as "first run" until U08.
- Tests: `NavShellTest` (Robolectric, SDK 34): 4 tabs exist, tabs switch, first run hides the bar, no old fork entries reachable. 363 unit tests total, 0 failures; lint + assembleDebug OK (exit 0).
- Build changes: Robolectric 4.13, ui-test-junit4, androidx.test core, espresso-core 3.6.1 (pin; ui-test asks for uncached 3.5.0), conscrypt-openjdk-uber 2.7.0 (Robolectric's 2.5.2 has no linux-aarch64 native).
- NOT done / limits: Robolectric's native SQLite does not run on linux-aarch64, so UI tests must not open screens that read Room (History tab). `TodayScreen` history load now catches `Throwable` so a missing SQLite never crashes the screen.
- Choices not in the plan: androidx.navigation not cached → own route enum; tab icons are material-icons-core (PlayArrow, Star, DateRange, Settings), to be replaced by the U02 icon set; the language switcher (AppLanguage) was dropped from `MainActivity`/Application (English only, Q3) — data-module classes kept.

### U01b — old fork code deleted (2026-10-05)
- Deleted from `:app`: all fork screens (To Do, Record, Workout, Create, programs, intervals, community share, CSV, old backup, old settings, view/graph/calendar), `ui/components/*`, `UiMessage`, `util/*`, `viewmodel/*`, `service/WorkoutTimerService` (+ manifest entry, FLASHLIGHT/camera-flash entries), the fork's unit tests (CSV, backup, reorder, program loop, timer settings, search) and androidTest `CsvImportTest`. About 30k lines.
- Deleted from `:data`: the 10 old DAOs, `WorkoutPreferences`, `SavedWorkoutState`, `ProgramExecutionModels`, `LanguagePreferences` and the androidTest that used the old DAOs (`DataLayerInstrumentedTest`). **Kept:** every `@Entity` and all migrations 9→23 and the exported schema, so existing installs upgrade and the old tables stay untouched (no destructive migration). `AppDatabase` only lost the unused DAO accessors.
- All translations (`values-ar/de/es/fr/it/ja/ru/uk/zh-rCN`) removed — they only translated the fork's strings (English only, Q3). `strings.xml` now holds only strings in use. `lint.xml` no longer downgrades MissingTranslation. Dropped the `reorderable` and mockito test dependencies (unused).
- Tests: `OldForkRemovedTest` scans resources and sources for the old strings/classes. Verify exit 0 (see log).
- NOT done: `data/schemas` JSON files untouched; `docs/` older documents still mention the fork screens; `THIRD_PARTY_NOTICES.md` still lists the fork's libraries (reorderable) — needs a tidy in U13.
- Choice not in the plan: `ThemePreferences`/`AppTheme` kept (theme choice still read by `MainActivity`).

### U02 — design system (2026-10-05)
- `ui/theme/`: Material 3 theme with dynamic colour on Android 12+ (`dynamicColor = true`), fallback palette (deep teal primary, orange accent) for light and dark, `AppTypography` (M3 scale only), `Spacing` (4/8/12/16/24 + 48 dp touch), `Radius` (card 16, button 28), `AppShapes`, `AppAccentTheme` (orange for muscles and stars, kept constant under dynamic colour), `AppIcons` (icons not in material-icons-core; Apache-2.0 note added to THIRD_PARTY_NOTICES.md).
- `ui/components/Stepper.kt`: − value + with haptic tick, 48 dp targets, content descriptions (used by U05/U08).
- Old `AppColors`/`LocalAppColors`/Slate palette removed; `LicensesScreen` now uses `MaterialTheme`.
- Tests: `DesignSystemTest` — palette per theme, light/dark compose, stepper clamp/enable state, and a source scan failing on hard-coded `Text("…")`/`contentDescription = "…"` outside a named legacy list (Today, Library, Onboarding, Session, History, Backup2, SkillTree: each leaves the list when its task rewrites it). Verify exit 0.
- NOT done: no font-scale 1.3 check yet (U13); French strings (Q3); `HardcodedText` Android lint does not look at Compose, hence the source-scan test.
- Choices not in the plan: dark/light selection still follows `ThemePreferences` (system by default); no Settings control yet.
