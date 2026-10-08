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

### U03 — hold timing, stretch-is-the-recovery, cue text (2026-10-05)
- Catalog: `hold_tiers` window = target + 3 s (`HOLD_SETUP_SECONDS` in `gen_starter_catalog.py`); catalog regenerated and copied to the assets. A 15 s plank block is now 18 s.
- Planner: in timed mode holds are no longer padded to 60 s; they last their tier window and the unused part of the 60 s cycle is added to the recovery that follows (the stretch when stretch is on). Rep work still fills 60 s (30 s per side).
- New `domain/session/CueText.kt` (pure): block titles and the "Next" announcement. Transition = "Get ready: <next exercise>"; stretch block = "Stretch: <name>"; "Rest" only exists for `PASSIVE_RECOVERY`, which the planner only creates when stretch is off (with stretch on a passive recovery would even be labelled "Recover"). `WorkoutSessionService.label()` and `SessionScreen` use it; the spoken "Next:" skips the short get-ready block.
- Tests: new `HoldTimingTest` (every hold tier = target+3; 15 s plank = 18 s with and without stretch; timed holds not padded and leftover goes to the recovery; no block/cue contains "Rest" with stretch on in normal and timed mode; transition text). `ExplicitRoundsTest.timedRoundsAreSixtyWorkSixtyRest` updated for the new rule. 4-round plan stays inside the 40–50 min band. Verify exit 0.
- `docs/review/draft-numbers.md`: hold windows updated by script (+3).
- NOT done: the app UI does not yet show a visible 3 s get-ready countdown inside a hold block (the first 3 s of the block are just part of it; the existing 3-2-1 beeps are unchanged) — U07 session redesign. Reducer is unchanged: a hold that runs its full block counts as completed, as before.
- Choices not in the plan: label "Recover" for a passive recovery when stretch is on (cannot occur from the planner; defensive). Old saved plans in history keep their old +25 s windows (plans are immutable snapshots).

### U04 — equipment profiles (2026-10-05)
- Domain `equipment/ProfileEdit.kt` (pure): the checklist (`EquipmentOptions`: pull-up bar, high bar, low bar/sturdy table, band, kettlebell+weight, dumbbells+weight, chair (stable), wall (capability), mat, parallettes (future, nothing needs it yet)), `buildProfile`/`selectionOf` (checklist ⇄ `EquipmentProfile`), `ProfileOps` (unique ids, upsert, delete never removes the last profile and moves the selection, name validation).
- App: `ProfileStore` (JSON in app-private prefs, seeds Home/Travel on first run, replaces hard-coded `SeedProfiles` at runtime), `ProfileScreens.kt` (`ProfilesScreen` list from Settings → "Equipment profiles"; `ProfileEditScreen` with name, icon checklist, weight steppers in 0.5 kg steps, delete with confirm), original one-colour pictograms in `ui/theme/EquipmentIcons.kt`. Train tab: profile chips come from the store, "+" chip opens the editor for a new profile (it becomes the selected one), "Edit … equipment" link. The loose "I have a high bar" chip and `ModeStore.highBar` are gone; an old `true` value is migrated once into an item on Home.
- Backup: `BackupPayload.profiles` (appended last, default empty, so old files still import); export includes profiles, restore replaces them when the file has some.
- Tests: `ProfileEditTest` (7: seed round-trip, every catalog equipment id has a checklist row, muscle-up locked without high bar and unlocked with it, wall = capability, weights, list rules, name validation, backup round trip incl. old-format payload), `ProfileStoreTest` (Robolectric: seeds, CRUD persisted, last profile kept, legacy high-bar migration, editor creates "Gym"+high bar → muscle-up available only there, editor refuses empty/duplicate name). Verify exit 0.
- NOT done: the Train tab layout is still the old long scroll (U05); no icon for "Other"; profile reorder; the planner still ignores catalog items that need the not-yet-existing `low-bar` (U09 adds rows that use it).
- Choices not in the plan: dumbbells are a pair (quantity 2, default 2.5 kg each = the old Home seed); kettlebell default 12 kg; profile name max 30 chars; editing a profile does not change which one is selected, creating one selects it.

### U05 — Train tab (2026-10-05)
- `ui/train/TrainScreen.kt` replaces `TodayScreen`: Goal dropdown (`ExposedDropdownMenuBox`, grouped Skills / Body focus), profile chips + "+" (and an edit pencil), Rounds card (stepper + live "≈ N min") next to a Style card (Reps/Timed segmented), Focus segmented (Full/Upper/Lower/Core, multi), Stretch switch, one full-width "Preview workout" button; unfinished/running-session banner on top. No old chips/slider/checkboxes, no "Remember these settings", no skill tree here (it moves to the Progress tab in U10), no "Set my starting level" (it lives in Settings).
- Domain `planner/TrainPlan.kt`: `TrainSettings` + `buildTrainPlan(...)`, the one function Train and Preview both call (so the minutes shown always equal the draft). `focus = null` means "follow the goal", so picking "Focus: upper body" shows Upper selected.
- `TrainSettingsStore` reads/writes goal, profile, rounds, style and stretch on every change (rounds is new; the others reuse their old stores).
- Interim `PreviewScreen` (U05 stand-in, U06 replaces it): header, block list, warnings, Start. Route `PREVIEW` added to the nav.
- **Bug found and fixed:** the old Start used the plan id `"preview"` for every workout, and history keeps one plan snapshot per id with INSERT-IGNORE, so every session after the first pointed at the first workout's plan (wrong exercises/targets for progression evidence and History corrections). New workouts now get a unique plan id (`plan.copy(id = UUID)`). Sessions already recorded on the owner's phone with plan id "preview" cannot be repaired.
- Tests: `TrainPlanTest` (domain, 8), `TrainScreenTest` (Robolectric at the S21's 411×891 dp: all controls are visible without scrolling, Preview enabled and opens the preview route, rounds stepper changes the minutes, style/stretch/focus persist, goal dropdown groups and switches the focus, profile chips + "+"), `NavShellTest` adjusted. Verify exit 0.
- NOT done: font-scale 1.3 check (U13); "≈ min" uses the plan length rounded to the nearest minute; a goal's description is shown as one line under the field (long text is cut).
- Open issue: `nowDay` is still passed as 0 to the planner (as before), so the 14-day re-entry warning can never trigger. Not changed (outside U05); I recommend fixing it in a later task.
- Choices not in the plan: rounds range 1–10 (planner's `MAX_EXPLICIT_ROUNDS`); the "Auto rounds from minutes" mode and the minutes slider are gone (owner decision: rounds are the length mechanism); style hint line under the Reps/Timed buttons.

### U06 — Preview (draft workout) (2026-10-05)
- `ui/train/PreviewScreen.kt`: header "4 rounds · 44 min · Reps"; one compact info row (draft-content note, expands to the planner's warnings and explanations); one card per exercise of the circuit (round 1, unilateral = one card "per side"): initial-letter avatar, name, target, movement chip, swap button; collapsible "Full timeline" (block by block, using the U03 cue labels); sticky full-width Start.
- Swap sheet (`ModalBottomSheet`): same movement and area, only exercises usable with the selected profile, current one marked; today only, with a "Keep for next time" checkbox that saves a NEW routine revision (`saveSwapsToRoutine`, old plan snapshots untouched).
- Domain: `trainRoutine` and `swapOptions` in `TrainPlan.kt` (+ test). Start encodes the plan with a fresh UUID plan id and calls the existing `startWorkout`.
- Tests: `PreviewScreenTest` (8, Robolectric at S21 size: header, one card per slot, swap changes only that card and offers only same-movement options, keep-for-next-time vs today-only against `RoutineStore`, timeline collapsed by default, info row, Start launches `WorkoutSessionService` with ACTION_START and a plan whose id is neither "draft" nor "preview"), `TrainPlanTest.swapOptions…`. Verify exit 0.
- NOT done: muscle chips (the catalog has no muscles until U09 — the chip shows the movement pattern instead); real family icons (U10 — an initial letter for now); no clip preview in the sheet. Touch input does not reach the bottom-sheet window under Robolectric, so taps inside the sheet are tested through the semantic click (the real touch path is untested; owner check on the phone).
- Choices not in the plan: minutes in the header are rounded to the nearest minute; swap options are sorted by difficulty; Start is disabled when the plan is infeasible.

### U07 — session redesign + in-session logger (2026-10-05)
- `ui/session/SessionScreen.kt` (old `screens/SessionScreen.kt` removed; `startWorkout`/`recoverWorkout` moved to `SessionCommands.kt`, the late-edit `FeedbackForm` to `FeedbackForm.kt`): header "Round r of R" + progress bar + overflow (End workout with confirm); big looping clip (no media controller); name, target, side; ring timer around the time (`displayLarge` only here); "Next: …"; Pause/Resume · Skip · Done (work blocks only). Titles come from `CueText` (U03): stretch = "Stretch: …", get-ready = "Get ready: …", never "Rest" with stretch on.
- **Logger (F10/F14):** while a stretch/recovery/get-ready block runs after a work block, a compact card at the top shows "<exercise> − n + reps (or seconds held)" prefilled with the target, plus "Too hard" and "Pain" chips. Every change sends `ACTION_LOG`; the reducer stores it per work block (= per round and side) in `SessionState.logged` (`LogBlock` event) and emits a Persist effect, so it is in the crash-safe checkpoint immediately (`Checkpoint.logged`, appended last with a default). Untouched = nothing stored = assumed met.
- At the end the service writes, in the same Room transaction as the session: `block_results.achievedValue` = logged value per block, and one first feedback revision per logged exercise (`rowLogsFrom`: reps = the LOWEST round, BELOW if any round is below target or "Too hard" was pressed, discomfort if "Pain" in any round). `HistoryDao.saveFinishedSession` got an optional `feedback` parameter. The progression engine already reads the latest revision, so evidence uses the lowest round.
- **Reducer rule changed:** `EarlyDone` ("Done") used to do nothing unless the tier had a reviewed early-completion stretch. It now always ends a work block and moves on (the next block is the recovery, i.e. the stretch). Holds count as completed only if the target time was held, otherwise partial. The old unit test was replaced.
- End screen: title, one summary card ("N min · R rounds · E exercises") with today's progress events (replayed from history) and a "Edit what I logged" link that opens the old rating form on demand; no long form.
- Tests: domain `SessionLogTest` (8: stores per block, latest wins, only started WORK blocks, clamp + terminal immutable, assumed met, lowest round, too hard/pain, evidence non-qualifying when a round is low), reducer tests for Done, `CheckpointTest` (logs survive a crash; old checkpoints still decode), `SessionScreenTest` (10, Robolectric at S21 size: work block UI, stretch with logger on top and prefilled target, − sends ACTION_LOG with target−1 for the right block, too-hard/pain flags, values restored from state, no "Rest" anywhere with stretch on and "Get ready: …" on transitions, pause/resume, Done/Skip/Pause commands, End needs confirmation, end screen), androidTest `feedbackLoggedDuringTheWorkoutIsSavedWithTheSession` (compiles; needs a device to run). Verify exit 0.
- NOT done / limits: no real service test (the service itself is not unit-testable here; the Room write of logs is covered only by the androidTest that cannot run offline); no hold-specific 3 s get-ready countdown inside the block (the first 3 s of a hold block are just part of it); ring is a countdown for all blocks (the plan's "count-up for reps" was not built); "Pain" is called `discomfort` in the data; `Done` on a rep block during a PAUSED session is disabled; END screen stars are shown only as today's progress-event messages (no star icons until U10); the logger shows only for the most recent work block.
- Choices not in the plan: logs are saved to the checkpoint immediately and become feedback revisions when the session row is created (the feedback table needs the session row to exist); the logger also stays visible during the get-ready block that follows the recovery.

### U08 — first-run questionnaire (2026-10-05)
- Catalog: new `onboardingFamilies` (appended last, default empty; `gen_starter_catalog.py` + validator): push-up (anchor standard push-up), squat (split squat), pull and row (band-assisted pull-up), core (plank), hips and back (glute bridge), shoulders (pike push-up, asked only when the push-up answer is at least a standard push-up). Ladders are entry level only; a test fails if one reaches a hard skill.
- Domain `intake/Intake.kt`: `FamilyAnswer` (`Does` / `NotSure`), `levelFor` (owner Q2: Normal = anchor step 3; Easy/Hard = neighbour on the ladder at step 3, else anchor step 1 / step 5; an anchor ruled out by equipment falls back to the nearest usable exercise, e.g. no pull-up bar → band row), `levelFromDoes` (reps → step via `suggestStartingStep`), `shouldAsk`, `routineFromAnswers` (a pull-up answer turns the usual plan's pull slot into a vertical pull — the starter routine only had a band row), `suggestedRounds`.
- UI `ui/onboarding/OnboardingScreen.kt` (old per-exercise screen deleted): Welcome · Goal dropdown (Skills / Body focus) · Equipment (the profile checklist, first profile "Home") · one page per family (I know: carousel of entry exercises, tap opens a sheet with clip + how-to + "I do this", then reps/seconds and rounds steppers; I don't know: Easy/Normal/Hard cards that name the exercise and target) · Workout style (reps/timed, rounds prefilled from the answers, stretch) · Summary with 0 stars. Dots, Back/Next, Next disabled until a family is answered, slide transition. Redo from Settings asks for confirmation and can be cancelled; the redo overwrites the levels.
- `OnboardingSave` writes levels (overwrite), the Home profile, the routine, rounds/style/stretch/goal and the done flag. `StarRow` component added (used again in U10).
- **Content assumption to confirm:** `row-band` needs a band with the "stable-anchor" flag, but the Home seed band never had it, so the band row was unusable everywhere (the planner silently substituted). Ticking a band in the checklist now means "you can anchor it" (label says so), the Home seed has the flag, and profiles saved earlier are upgraded on load. This is a safety assumption the owner should confirm.
- Tests: `QuestionnaireTest` (domain, 8) and `OnboardingTest` (Robolectric, S21 size: page order and titles, Next disabled until answered, Normal answers store the anchor at step 3, Easy skips shoulders and uses step 1 for core, "I do this" with reps → step 4 and rounds carried to the style page, profile name/high bar/timed/stretch saved, no bar → "Band Row" is Normal for pull, redo needs confirmation and is cancellable). Verify exit 0.
- NOT done: family icons are an initial letter until U10; the clip sheet shows no video when an exercise has none; rounds typed per family only feed the default rounds suggestion (one global rounds setting).

### U09 — new chains and muscles (2026-10-05; chains: owner OK 2026-10-05, provisional)
- Ten new exercises, all DRAFT/unreviewed, numbers are heuristics: rows (band row → inverted row knees bent → inverted row → feet elevated → archer row; low bar or sturdy table), back extension (superman → Y arch hold → arch rocks), anti-rotation (dead bug now hands over to the hollow hold; the hollow hold's prerequisite is plank 4 OR dead bug 5), side plank (→ leg raise → Copenhagen with a stable chair), kettlebell hinge (deadlift → single-leg RDL → swing). Catalog: 60 variations / 53 policies.
- `Muscle` enum (18) and `primaryMuscles` / `secondaryMuscles` on `ExerciseVariation` (appended last, default empty), filled for every strength exercise in `gen_starter_catalog.py` (the script fails on a missing one). Preview cards show the muscles as chips (primary = accent, secondary = muted); the pattern chip is gone.
- Tests: `noDeadEndsExceptExplicitTops` (the explicit top list is in the test; any new dead end fails), `everyStrengthExerciseNamesItsPrimaryMuscles`, `newChainsAreInOrderAndNeedTheRightEquipment`, existing scope count 43 → 53, `cardsShowTheMusclesWorked`. Verify exit 0.
- **Rule change to confirm:** the old test "no ballistic kettlebell work" is relaxed for the swing only (the owner's OK on the §4.1 list includes it). The swing needs the single-leg RDL at step 3 and the deadlift at step 5, carries a "Ballistic movement" caution, and the test now requires every kettlebell exercise to carry a ballistic-related caution.
- NOT done: tree/skill-graph nodes (`skillNodes`) were not extended for the new chains (the tree in U10 draws from `nextVariationIds`/prerequisites); no clips yet for the new exercises (U11).
- Choices not in the plan: prerequisites for the new steps are "previous step at tier 4" (swing: RDL 3 + deadlift 5); the archer row is the top of the row chain (no hand-off to pull-ups).

### U10 — Progress tab: skill tree with stars (2026-10-05)
- Domain `tree/SkillTree.kt`: `TreeTabs` (Push, Shoulders, Pull, Row, Squat, Hips and back, Core, Stretches — each strength exercise is in exactly one), `layoutTree` (columns = longest path easier→harder, rows split exercises sharing a column, independent chains stacked; deterministic; edges from `nextVariationIds` and in-tab prerequisites, so several prerequisites merge into one node), `nodeState` (Mastered = all five steps; Locked = prerequisites missing and not started; Needs equipment; Current = training now; Available), `trainingNow`, `stars` (from `earnedStars()`).
- UI `ui/progress/`: `ProgressScreen` (family picker chips with pictograms, goal's tab chosen first, goal chain outlined in the accent colour), `SkillTreeCanvas` (round family-icon nodes, curved edges drawn on a `Canvas`, 0–5 stars per node, lock badge or equipment badge, "Training now" label, two-way scroll), node bottom sheet (clip, muscles, how-to, form cues, five steps with a "you are here" marker, what unlocks it, equipment and what is missing, cautions, sources, draft notice), stretches tab. Original stick-figure family pictograms in `ui/theme/FamilyIcons.kt`. Old `LibraryScreen` and `SkillTree` deleted.
- Tests: domain `SkillTreeTest` (9: every exercise in exactly one tab, no two nodes in a cell, edges left to right, layout deterministic regardless of family order, push chain merges into the standard push-up, HSPU chain, stacked chains + dead bug → hollow hold edge, node states incl. equipment and mastered, stars from earnedStars, goal→tab), app `ProgressScreenTest` (5, S21 size: nodes with star/state descriptions, goal picks tab and is shown, family picker switches trees incl. the archer row (F15), sheet shows the five steps, stretches tab). Verify exit 0.
- NOT done / limits: `NodeUi` accessibility text gives state and stars but the canvas edges have no description; no pan-to-goal on open; screenshots of the drawn tree could not be checked by eye (Robolectric does not render pixels) — owner check on the phone; the stars for exercises mastered before this release are 0 until new sessions are logged.
- Choices not in the plan: node with a self-assessed starting level is never shown as locked even if its prerequisites are not met; icons are one pose per family (not per exercise).

### U11 — clips v2 (2026-10-05)
- New `tools/gen_demo_clips_v2.py` (v1 kept): 3/4 view (30° yaw, 15° pitch, orthographic), capsule body in light grey with a darker far side, muscles from the catalog drawn orange-red (primary) and light orange (secondary), floor grid + shadow, bars/boxes/walls in 3-D, 2 s loops at 24 fps, 480×360 H.264. Reuses v1's sagittal poses (lifted to 3-D, near/far limbs offset) and adds poses for the ten U09 exercises. All 60 catalog exercises regenerated into `assets/demos/` (1.25 MB total, each 13–38 KB).
- Checks: the script asserts every catalog id has a pose, every muscle maps to a body region, every strength exercise has primary muscles, every clip < 120 KB (`--check` runs only the first three; `tests/test_clips_v2.py` wraps it, skipped without Pillow); `ClipAssetsTest` (app unit test) checks a clip exists for every exercise, none is orphaned, each < 120 KB. Verify exit 0.
- Review: `docs/evidence/clips-v2-review.md` + contact sheets in `docs/evidence/clips-v2/`. It is the implementing model's own look at the sheets, not a human review: most clips read correctly; weakest are the wall-handstand clips, cat-cow, the front levers and dead bug. Hidden muscles are shown as a faint x-ray tint.
- NOT done: no camera tuning for the handstand/lever clips; no clips for muscles of stretches; the clips were not watched as video (only 3 frames each); playback inside the app was not re-checked beyond the existing `DemoPlayer`.

### U12 — History and Settings tabs (2026-10-05)
- Domain `history/SessionDetail.kt` (+ `SessionDetailTest`, 8): `overviewOf` (minutes, rounds and exercises count only work that was done, never skipped blocks; works without the plan snapshot), `exerciseDetails` (per exercise: every round/side with the logged value, lowest typed value), `ratingFor` (a corrected number below target = BELOW, "too hard" = BELOW, otherwise MET), week helpers (weeks start on Monday), `finishedSessions`.
- `ui/history/`: `HistorySource` (small interface + `RoomHistorySource`, so the screens are testable without SQLite) and `HistoryScreen`: week header with previous/next week (no future weeks), a 7-day strip with a dot on every day you trained (tap a day to filter), a one-line weekly summary, a card per workout ("40 min · 3 rounds · 6 exercises", "Finished early"); empty states never shame a missed week. `SessionDetailScreen`: per exercise, each round's logged value ("as planned" when nothing was typed) and an editor (reps, too hard, pain) where every change is saved as a NEW feedback revision (earlier ones kept). The same `FeedbackEditor` replaces the old rating form on the end screen's "Edit what I logged". Old `HistoryScreen` and `FeedbackForm` deleted.
- Settings (`SettingsScreen`): Equipment profiles; Workout defaults (rounds, reps/timed, stretch — shared with the Train tab and the questionnaire through `TrainSettingsStore`); voice and sound cues (now really switches the spoken cues in `startWorkout`); Redo the starting questionnaire (confirm dialog); Appearance (System / Light / Dark, applied at once); Backup and restore (strings moved to resources; backup also carries profiles, see U04); Privacy policy (in-app, `assets/privacy.md`, same text as `PRIVACY.md`, flashlight sentence removed because that code is gone); Licences; About (version, GPL-3.0, draft notice).
- No literal UI strings remain: the legacy exemption list of the string-scan test is now empty.
- Tests: `HistorySettingsTest` (13, Robolectric at S21 size with a fake history source): strip dots and the week's list, day filter and opening a workout, week browsing without the future, empty-history message, detail rounds + correction creates three revisions with the right ratings, unknown workout, every settings control present, defaults saved, voice off saved, theme reported, redo needs confirmation, privacy text. Verify exit 0.
- NOT done: round values themselves cannot be edited per round (the correction is per exercise, like the rating it feeds; per-round values are shown read-only because block results are append-only history); no export of single workouts; the real Room read path is only covered on a device.
- Choices not in the plan: History detail is a full-screen route (no bottom bar); the week summary uses the existing `weeklySummaries`.

### U13 — polish pass (2026-10-05)
- Motion: `AppNav` uses `AnimatedContent` with a shared-axis slide (deeper = in from the right, back = in from the left, tab switches cross-fade). Haptics: stepper tick (U02), long-press tick on Start, Skip and Done, light tick on Pause/Resume. Switch rows (Train stretch, Settings, questionnaire) are now one toggleable row, so a screen reader reads "Stretch between exercises, switch, on" instead of an unlabelled switch.
- Accessibility tests (`PolishTest.kt`): on a tall viewport at font scale 1.3 and in BOTH light and dark themes, every clickable control on Train, Preview, Session (work block and stretch with logger), Settings, Progress, Profile editor and the questionnaire has a text or content description and a touch area of at least 48 dp; a second class at the S21 size with font scale 1.3 checks that the key controls (Preview, Start, Next, every settings row, …) stay reachable. `ContrastTest`: both fallback palettes meet WCAG AA 4.5:1 for the text pairs, and the accent colour is ≥ 3:1 for graphics; text on the accent colour is now near-black (white on orange was 3.7:1).
- `EndToEndFlowTest`: fresh install → questionnaire → Train → Preview → Start launches the service and shows the session screen with the tab bar hidden; an existing install goes straight to Train and keeps its levels.
- Clips: the handstand and front-lever clips use a wider camera yaw (see the review file). `THIRD_PARTY_NOTICES.md` updated; `STATUS.md` has a current-state header; version 0.3.0-ux, versionCode 2 (Room schema still 23, no migration). Owner checklist: `docs/evidence/owner-check-u13.md`.
- **NOT done: the Robolectric screenshot set (`docs/evidence/ui/`).** Robolectric's native graphics runtime is not supported on linux-aarch64 (the sandbox), so no pixels can be rendered here; dark-mode and font-scale quality is covered by the token/semantics tests above instead. The owner rates each screen on the phone.
- Open issues: planner `nowDay` is still 0 (14-day re-entry warning never fires); clips for handstands are the weakest; first run has no skip for returning users who reinstall (their levels are gone, so they see the questionnaire, which is intended); no French strings (Q3).

### KB1 — kettlebell weight progression (2026-10-08)
- Owner request: kettlebell progression with the same weight (harder exercise) OR more weight (same exercise); owner has a 12 kg bell.
- Domain `load/Load.kt` (`isLoaded`, `EquipmentProfile.loadGrams()` = the profile's kettlebell, `formatKg`). `TimelineBlock.loadGrams`, `SessionEvidence.loadGrams`, `VariationProgress.loadGrams` + `starsByLoad` (all appended last with defaults; old plans/evidence decode as "unknown weight"; no DB migration).
- Planner: kettlebell work blocks carry the profile's bell weight. A heavier bell than the recorded one -> the same exercise restarts at tier 1 (explained); a lighter bell -> warning, tier kept.
- Engine: first weighted session records the weight; heavier -> tier 1, achieved tiers cleared, stars at the old weight kept in `starsByLoad` + event; lighter -> logged (last trained day) but never counts. Top-tier messages for kettlebell exercises offer "same bell -> next exercise" or "heavier bell -> repeat this one from tier 1".
- Content: new `kettlebell-swing-one-arm` (per side, tiers 6/8/10/12/15, after two-hand swing tier 4, ballistic caution) as the same-bell step after the swing; every kettlebell exercise has a "Weight:" instruction (start weight, levels count per weight). Clip v2 pose added (own pose: bell in one hand, free hand by the hip). Catalog 61 variations / 54 policies.
- UI: Preview card, timeline and session target show "10 reps · 12 kg"; Progress node sheet shows the current bell and "Earlier at 12 kg: n of 5 stars".
- Tests: `KettlebellLoadTest` (11), `PreviewScreenTest.kettlebellCardsShowTheBellWeight...`, StarterCatalogTest counts/tops/chain updated. Verify exit 0, 308 tests, 0 failures. Version 0.3.1-kb (versionCode 3). APK `Apps/builds/app-debug-KB1.apk`.
- NOT done: only one kettlebell per profile (heaviest is used); no "which bell today" picker in the Preview; weight changes are made in the profile editor; dumbbells ("weight") are still unused by any exercise.
- Checked: the Archer Push-Up is intentionally one block (alternate sides, count each side), not a bug.
