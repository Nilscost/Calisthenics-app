# User feedback execution plan — Sol orchestrator / Luna executors

Date: 2026-10-19

Status: historical handoff, implemented in the R8.3/R8.4 working source and included in R8.5. The old “implementation has not started” status is superseded; see `STATUS.md` and the release evidence. This file retains the original implementation contract. Inspect the actual branch and repository state before editing; do not assume the code is unchanged from this review. The reviewed baseline is the R8.2 implementation described in `STATUS.md`. Choose the next release number through the existing release workflow rather than assuming one here.

## Start here

Implement this plan with a Sol orchestrator and Luna executors. The owner has approved the product decisions below. Do not reopen settled choices. Read applicable repository instructions, `README.md`, `STATUS.md`, `docs/31-r8.1-plan.md`, `docs/33-r8.2-plan.md`, and the progression documentation referenced there. Follow existing verification and evidence conventions.

The orchestrator owns shared contracts, integration, migrations, and final verification. Executors implement bounded tasks with named files, dependencies, and acceptance cases. Do not claim phone verification from automated tests or approve owner gates on the owner's behalf.

## 1. Approved requirements

1. **Music and volume:** other apps' music must continue during workouts, including downloaded songs played through a local music player. Add one saved app-volume slider in Settings covering voice and beeps. The reporting user's phone/player are unknown. Start app audio quietly; retain the voice toggle.
2. **Upcoming exercise:** reuse the existing change-exercise block as “Prepare / Upcoming,” showing the next exercise's silent looping demonstration. Default duration stays 5 seconds. Make the duration adjustable in workout preview.
3. **Between sets:** move Flow out of workout preview to **Train → Between sets**, with mutually exclusive **Flow / Rest / Stretch** options. Flow means only the preparation interval, 5 seconds by default or the configured duration.
4. **Progress cards:** Pilates and conditioning must use small thumbnail cards matching stretches, with the same detail navigation. Static thumbnails, not autoplaying videos.
5. **Editable levels:** exercise-detail **Edit** must expose both the existing manual level adjustment and editable targets defining levels 1–5 for that exercise. Users can increase their personal targets before moving to another exercise.
6. **Above level 5:** in workout preview, follow app-level steps through level 5, then increase by **1 rep** or **5 seconds** per + tap. Retain custom targets when saving a routine. Record actual performance unchanged; the engine must consider qualifying above-threshold performance a **level-5 accomplishment**, never level 6.
7. **Appearance:** vivid ultramarine `#303AF2`, warm blanc cassé background `#F7F5F0`, white cards `#FFFFFF`, ink text `#18213A`. **Achievement stars stay yellow.** Light mode is the default; dark mode is available in Settings. The user's written choice of vivid ultramarine supersedes an earlier interactive selector state showing Klein-inspired blue.

Do not expand this into a redesign of typography, exercise content, navigation, or progression routes.

### Implementation defaults, not separately approved product decisions

Use these unless implementation evidence warrants a documented adjustment:

- Volume 0–100%, default 30%; 0 mutes voice and beeps. Gain is not a guarantee of perceived loudness relative to every song.
- Preparation time 1–30 seconds in one-second increments, default 5.
- Save preparation time with routines and preserve it in preview drafts.
- Personal target edits apply prospectively; historical results and earned achievements remain intact.
- Fresh installations default to Light; preserve explicitly saved appearance choices on upgrade.
- Retain existing deliberate exercise-specific prescription caps separately from the ordinary level-5 ceiling.

## 2. Code findings that matter

- `WorkoutSessionService.say()` requests `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`; beeps currently use full gain. This is a plausible contributor to the music issue, not a confirmed reproduction. Recovery currently forces voice on.
- `DemoPlayer` already mutes its video playback.
- Current Flow drops rests between exercises but retains some rests between sets. The new requirement changes that behavior.
- `TrainSettings` currently stores separate `flow` and `stretchOn` booleans. These need one authoritative interpretation.
- Planner transitions currently appear for position changes and some side changes; they are not guaranteed between every pair of work sets.
- `SessionScreen` resolves demonstrations from the current block's variation ID; transitions may have no variation ID, requiring an explicit upcoming-block resolver.
- `PlanEdits.freeTargets` intentionally do not count toward progression. `Feedback.deriveEvidence()` excludes them, and `ProgressionEngine` also checks prescribed tier against the tracked tier. UI changes alone will not satisfy level-5 credit.
- Flow programs currently have evidence exclusions in both `ProgressLoader` and `Feedback`. Preserve protection against invented timer-only performance while allowing valid logged results.
- Several exercises share catalog policies. Personal targets must be keyed to an exercise, not mutate a shared policy.
- `StarRow` uses the generic accent. Split star colors from the main accent before replacing gold with blue.
- `ThemePreferences` currently defaults new installations to Dark.

## 3. Orchestration and sequencing

Use three bounded execution lanes:

| Lane | Scope |
|---|---|
| Luna A | Audio, volume Settings, appearance |
| Luna B | Between-set modes, preparation time, upcoming video |
| Luna C | Personal level definitions, above-level-5 targets, Progress cards |

Sol establishes shared contracts first. Do not let executors independently invent serialized fields or migration rules. Assign single ownership or sequence edits to SettingsScreen, ProgressScreen, PreviewScreen, model files, and string resources. Each executor reports changed files, tests, unresolved issues, and deviations.

Order:

1. Shared contracts, migration rules, and regression cases.
2. Parallel lanes with explicit file ownership.
3. Between-set planning before final upcoming-video integration.
4. Personal level definitions before final above-level-5 evidence integration.
5. Shared UI integration, persistence, backups, history replay, recovery.
6. Full verification, screenshot review, build, and owner phone-test checklist.

Use small verified slices and the repository's existing evidence conventions. Do not publish or deploy anything merely because this plan exists; follow the current session's authorization and release workflow.

## 4. Sol foundation task — shared contracts

Define and review these before executor edits:

1. One authoritative Flow / Rest / Stretch mode.
2. Preparation duration propagated through planning, preview drafts, saved routines, and session snapshots.
3. Exercise-specific, versioned personal level-target overrides.
4. Explicit progression eligibility distinguishing new above-level-5 targets from legacy arbitrary free targets.

Append serialized fields with compatible defaults. Preserve old routines, backups, plans, and recovery checkpoints. Room changes require migrations and migration tests. New stored fields must survive backup/restore. Do not reinterpret past session semantics silently.

## 5. Luna A — audio coexistence and volume

Primary files/components:

- `app/src/main/java/io/github/gonbei774/calisthenicsmemory/session/WorkoutSessionService.kt`
- `app/src/main/java/io/github/gonbei774/calisthenicsmemory/ui/screens/PrefsStore.kt`
- `app/src/main/java/io/github/gonbei774/calisthenicsmemory/ui/screens/SettingsScreen.kt`
- `domain/src/main/kotlin/app/calisthenics/domain/model/Model.kt` (`Preferences`)
- `app/src/main/java/io/github/gonbei774/calisthenicsmemory/ui/screens/DemoClips.kt`

Implementation:

- Audit speech, beeps, video, and notification playback for anything that pauses or ducks external music.
- Configure workout cues to coexist with music without requesting that it pause or duck. Check the actual supported Android behavior during implementation.
- Add a persistent Settings volume slider. Apply gain to SoundPool and text-to-speech.
- Preserve the voice toggle. App volume controls both voice and beeps.
- Apply saved preferences on start, resume, and recovery; remove recovery's forced voice-on behavior.
- Keep demonstration videos silent. Never alter system media volume.
- Ensure speech failure or initialization delay cannot disrupt workout timing.

Acceptance:

- Downloaded music continues through start, countdowns, speech, transitions, and finish.
- Slider changes app audio only; zero silences voice and beeps.
- Preferences survive restart and workout recovery.
- Automated tests cover storage and gain routing.
- Phone testing covers a local music player with speakers and Bluetooth headphones; record phone, Android version, and player. Do not claim this verified from unit tests alone.

## 6. Luna B — Train Between sets and planning

Primary files:

- `app/src/main/java/io/github/gonbei774/calisthenicsmemory/ui/train/{TrainScreen,PreviewScreen,ProgramPreview,PreviewDraft}.kt`
- `domain/src/main/kotlin/app/calisthenics/domain/planner/{TrainPlan,Planner,ProgramPlan}.kt`
- `domain/src/main/kotlin/app/calisthenics/domain/routine/SavedRoutine.kt`

Implementation:

- Train shows Flow / Rest / Stretch. Remove Flow switches from both preview implementations.
- Support generated workouts, saved routines, and editable programs.
- Flow removes between-set recovery/stretch blocks and retains one preparation interval before the next work set, including repeated sets of the same exercise.
- Rest keeps configured rest intervals. Stretch keeps compatible selection, repetition limits, and fallback behavior.
- Preserve independent warm-up, conditioning-finisher, and cool-down behavior.
- Make Train estimates, Preview timeline, and actual session agree.
- Adapt the Settings stretch default so it cannot contradict the new mode.

Legacy mapping: Flow=true → Flow; otherwise Stretch=true → Stretch; otherwise Rest. Preserve saved-routine choices and program starting defaults through the new contract.

Acceptance:

- Test all three modes across Straight sets, Pairs, and Circuit.
- Flow has no unintended recovery blocks between work sets.
- Switching away from Flow restores the relevant rest/stretch settings.
- Navigation, restart, save/load, and backup restore preserve the choice.
- No obsolete Preview Flow switch remains.

## 7. Luna B — upcoming video and preparation duration

Primary files/components:

- Planner transition generation and `domain/src/main/kotlin/app/calisthenics/domain/planner/SideChange.kt`
- `app/src/main/java/io/github/gonbei774/calisthenicsmemory/ui/session/SessionScreen.kt`
- `domain/src/main/kotlin/app/calisthenics/domain/session/{SessionReducer,CueText}.kt`
- DemoClips / DemoPlayer and both preview implementations

Implementation:

- Reuse `TRANSITION`; never add a second countdown beside an existing transition.
- Resolve the following exercise and show its silent looping video, name, target, and side where relevant.
- Keep preparation distinct from execution and logging.
- Add “Preparation time” to Preview, default 5 seconds; persist through drafts, routine saves, and recovery.
- Deduplicate exercise-change and side-change transitions; do not add one after the final exercise.
- Keep rest/stretch content intact; upcoming video belongs in preparation.
- Use existing thumbnail/instruction fallback if media is unavailable.

Acceptance:

- Duration estimate and countdown match.
- Correct clip after swaps, removals, and reordering.
- Pause/resume and recovery retain remaining time.
- Preparation awards no reps, hold time, or exercise completion.
- Repeated exercises, side changes, final blocks, and missing media behave correctly.
- No duplicate announcements or audible video playback.

## 8. Luna C — editable personal level targets

Primary files/components:

- `app/src/main/java/io/github/gonbei774/calisthenicsmemory/ui/progress/ProgressScreen.kt`
- `app/src/main/java/io/github/gonbei774/calisthenicsmemory/ui/screens/{LevelStore,ProgressLoader}.kt`
- Catalog-policy resolution and `domain/src/main/kotlin/app/calisthenics/domain/progression/{TierProfiles,Rule,ProgressionEngine}.kt`

Implementation:

- Keep current level-adjustment controls in Edit mode; add editable rep/time targets for all five levels with explicit units.
- Provide Save, Cancel, and Reset to app defaults for personal target edits.
- Key overrides by exercise. Never modify sibling exercises sharing a policy.
- Preserve existing set requirements, tempo, and other compound policy properties.
- Validate positive targets and sensible ordering while allowing legitimate equal targets and compound definitions already present.
- Use effective targets consistently in Progress, Train, Preview, logging, and progression gates.
- Audit hardcoded/shared-profile gates so advancement uses personal numbers instead of old defaults.
- Editing targets does not award stars; explicit manual level adjustment retains its existing semantics.
- Historical plans retain their original targets. Preserve earned history, but do not use an incompatible pre-edit streak to satisfy newly increased thresholds.
- Persist overrides and revision information in backups.

Acceptance:

- Editing one exercise leaves policy-sharing exercises unchanged.
- Save/cancel/reset/restart/restore work.
- Raising targets prevents advancement at the old lower threshold.
- History and earned achievements are unchanged.
- Manual level adjustment and target editing work independently.
- Cover reps, holds, and existing compound tier definitions.

## 9. Luna C — above-level-5 targets and evidence

Primary files/components:

- `domain/src/main/kotlin/app/calisthenics/domain/planner/{PlanEdits,Planner,ProgramPlan,ExerciseRotation}.kt`
- PreviewScreen / ProgramPreview
- `domain/src/main/kotlin/app/calisthenics/domain/feedback/Feedback.kt`
- ProgressLoader / ProgressionEngine / SavedRoutine

Stepper contract:

- Below level 5: follow effective level targets.
- At level 5: + keeps the exercise and adds 1 rep or 5 seconds.
- Above level 5: − subtracts the same increment.
- At the level-5 boundary, the next − selects level 4.
- No level 6. Changing exercises clears or deliberately remaps the old custom target.

Persistence and progression:

- Preserve custom targets through navigation and routine saves, including routines created from programs.
- Store actual performance unchanged; do not truncate history to the level-5 target.
- Preserve hold durations and unilateral behavior.
- Keep deliberate exercise-specific prescription caps distinct from the ordinary level ceiling.
- Explicitly mark new above-level-5 prescriptions eligible for level-5 evidence; blindly reusing `freeTarget=true` will fail.
- Update evidence derivation AND engine consumption. Valid level-5 evidence must not be silently discarded because the tracked tier is lower.
- Preserve required-session, completion, discomfort, load, and prerequisite rules. A large target does not automatically bypass all advancement gates.
- Do not retroactively promote historical free targets.
- Audit Flow-program exclusions: valid logged results should count, while timer-only completion must not invent strength results.

Acceptance examples:

- Level 5 = 12 reps: 12 → 13 → 14; decrement 14 → 13 → 12 → level-4 target.
- Level 5 = 30 seconds: 30 → 35 → 40; decrement 40 → 35 → 30 → level-4 target.
- Save 40 push-ups, restart/reload, complete and log 40: history shows 40, eligible evidence is level 5.
- Repeat for timed holds above level 5.
- Test skipped, incomplete, mixed-set, and corrected results against false credit.
- Personal level-5 edits update the stepper boundary.
- Include tracked-level-below-5 and same-level alternative cases to verify engine behavior explicitly.

## 10. Luna C — Pilates and conditioning thumbnail cards

Reuse/extract StretchList's card layout and ExerciseThumb in ProgressScreen. Preserve names, spacing, tap behavior, accessibility, category membership, and search. Use static thumbnails, not autoplay.

Acceptance: every eligible item appears; no duplicates or lost search results; correct detail navigation; missing-image fallback; small-screen and enlarged-font readability. Sequence this with other ProgressScreen edits.

## 11. Luna A — ultramarine appearance, yellow stars

Primary files:

- `app/src/main/java/io/github/gonbei774/calisthenicsmemory/ui/theme/{Color,Theme}.kt`
- `app/src/main/java/io/github/gonbei774/calisthenicsmemory/ui/components/StarRow.kt`
- `data/src/main/java/io/github/gonbei774/calisthenicsmemory/data/ThemePreferences.kt`
- SettingsScreen and components using AppAccentTheme

Light tokens:

| Role | Color |
|---|---|
| Primary accent | `#303AF2` |
| Background | `#F7F5F0` |
| Cards | `#FFFFFF` |
| Main text | `#18213A` |
| Filled stars | Yellow; retain existing `#E9C046` as starting token |

Implementation:

- Separate star colors from generic accent colors before changing primary to blue.
- Use ultramarine for primary actions, active navigation, selection, and relevant indicators.
- Filled stars remain yellow in both modes; empty stars remain neutral. Check yellow-star visibility on white; use a darker yellow edge if necessary without changing fill to blue.
- Provide navy-based dark surfaces with a lighter blue accent for contrast.
- Fresh install defaults to Light; Dark stays available in Settings. Preserve explicit existing preferences.
- Audit hardcoded gold/black surfaces, especially the live workout screen.
- Bundled videos may retain white media surfaces within the blanc cassé layout.
- Keep success/warning/error meanings separate.

Acceptance: Train, Preview, live workout, Progress, History, and Settings are consistent; stars are yellow everywhere; normal text contrast ≥4.5:1 and large text/essential graphics meet applicable requirements; screenshot review includes light, dark, and font scale 1.3; preferences persist.

## 12. Verification and end-to-end acceptance

Run targeted regression tests during implementation, then the repository's full verification workflow, including `bash scripts/verify.sh` (domain/data/app tests, lint, debug APK, content validation). Follow existing Docker/fresh-clone and screenshot evidence conventions where applicable. Review screenshots, not just test exit codes.

Existing suites worth extending include FlowOptionTest, SessionReducerTest, SavedRoutineTest, BackupHistoryTest, ProgressionEngineTest, ActualRepsTest, TierProfilesTest, and the TrainScreen, PreviewScreen, ProgramPreview, SessionScreen, ProgressScreen, and HistorySettings app tests. Keep historical regression guarantees unless this plan explicitly changes their behavior.

Critical walkthrough:

1. Start downloaded music in a local player.
2. Select Flow on Train.
3. Set preparation to 8 seconds in Preview.
4. Set push-ups above level 5 and save the routine.
5. Restart and reload; confirm both settings persist.
6. Start the workout: uninterrupted music, quieter app cues, correct upcoming video and countdown.
7. Record actual reps; verify history and eligible level-5 credit.
8. Edit personal level targets; confirm future planning and gates use them.
9. Export/restore a backup and repeat persistence checks.
10. Confirm ultramarine controls, blanc cassé light surfaces, and yellow stars in both appearances.

Final Sol delivery:

- Requirement-by-requirement implementation summary.
- Changed-file and migration summaries.
- Automated results and reviewed screenshots.
- Build artifact through the existing release workflow.
- Separate automated and actual phone verification status.
- Remaining limitations with reproduction steps.
- Updated STATUS and owner acceptance checklist.

Do not mark music coexistence verified solely from tests. Do not mark owner acceptance complete on the owner's behalf.
