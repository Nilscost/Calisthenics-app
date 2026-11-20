# Phone feedback follow-up — Sol orchestrator / Luna executors

Date: 2026-10-20. Status: implemented as R8.4 candidate; automated build evidence and remaining device gates are recorded in `docs/evidence/r8.4-implementation-2026-10-20.md`. The contracts and acceptance criteria below are retained as the implementation handoff.

## Scope and baseline

Implement the owner's five findings from the new build: synchronized Flow/Rest/Stretch controls in Train and Preview, App levels as the ordinary default, recognition of demonstrated levels, restored warning beeps, and prevention of automatic screen timeout during a workout.

This plan supersedes the instruction in document 34 to remove the controls from Preview. Retain document 34's other delivered behavior, particularly external music coexistence, saved app volume, silent demonstrations, preparation intervals, personal targets, above-level-5 numbers, and appearance.

The inspected working tree contains extensive modified and untracked R8.3 implementation files. Sol must inventory and preserve them before assigning edits. Do not reset, clean, restore, or replace them with HEAD. Record the actual source/build identity; document 34's old “implementation has not started” status is not a description of this working tree. Read README, STATUS, applicable repository instructions, document 34 and R8.3 evidence before execution.

## Product contracts to settle before coding

1. **One current workout configuration.** Both Train and both Preview implementations display and edit the same effective Flow/Rest/Stretch choice. The most recent explicit choice wins. Opening or leaving a screen must not overwrite a newer choice. A saved routine supplies initial values; modifying today's copy does not rewrite the saved routine until Save.
2. **Stretch produces real stretch blocks.** The selected label, timeline, duration estimate, and started session agree. Preserve compatibility and stretch repetition limits. Where no compatible stretch exists, use the established safe fallback and explain it in Preview; never silently show “Stretch” while an available stretch is suppressed by stale state.
3. **App levels is the ordinary default.** Fresh generated workouts and new unsaved workout contexts start with App levels. Saved routines retain their saved rule, including Rep range and Custom. A deliberate rule edit in the current draft persists through navigation. It must not leak into another objective. Fixed programs retain their authored exercise/timing structure; apply this default to level-based strength prescriptions, not by inventing five strength levels for timed mobility/Pilates content.
4. **Demonstrated level becomes current level.** With automatic progression enabled, verified performance meeting level N makes N the current level when N exceeds the tracked level. Selection of N or mere timer expiry does not demonstrate N. The first qualifying session establishes N and counts as success 1; two further qualifying sessions give 3 total and permit N+1. Keep the existing distinct-day and rolling 14-day rules. This is the implementation interpretation of “2–3 times,” not a newly confirmed owner decision.
5. **No double advancement.** The adoption session cannot both adopt N and advance to N+1. Clear lower-level streak credit on adoption. No level 6; level-5 mastery and successor eligibility remain separate.
6. **Audio controls remain independent.** Voice off suppresses speech, not warning beeps. App volume applies to both; zero intentionally mutes both. External music continues, videos remain silent, and system volume is never changed.
7. **Screen stays on while the workout is active and the app is visible.** Include work, rest, stretch, preparation, warm-up, cool-down, and a manually paused unfinished session. Completion/cancellation releases the screen flag. Manual lock is always respected; the app must never wake or unlock the screen. Android permits normal timeout when another app is foreground, so do not promise a device-wide lock override.

## Findings from inspected code

| Area | Evidence and implication |
|---|---|
| Stale mode | `PreviewDraft.betweenSetMode` is persisted. `WorkoutPreview` prefers it over opening settings. `TrainScreen.change()` clears a draft only when its objective key changes. Selecting Stretch on Train can therefore leave an older Rest/Flow draft authoritative in Preview. |
| Inconsistent consumers | Train reads its draft with `remember` keyed only to goal/routine/program, and builds plans using settings. Preview has its own remembered opening state. Inspect both navigation re-entry and recomposition; adding buttons alone is insufficient. |
| Defaults | `ProgressionRule()` already defaults to APP_LEVELS. `RoutineStore` loads serialized routine rules, drafts carry `PlanEdits.rule`, and `resolveSaved()` explicitly restores a saved rule. Trace the reported Rep range through these sources; changing the enum default alone cannot fix it. Exact source of the owner's range remains unconfirmed. |
| Level mismatch | `ProgressionEngine.replay()` accepts qualifying evidence only at the tracked tier, except its level-five special case. `deriveEvidence()` also excludes arbitrary free targets and mixed prescribed tiers. UI wording is not the root fix. |
| Countdown wiring | Reducer emits COUNTDOWN_3/2/1 and service routes them to `playCue(sBeep)`. App gain now defaults quietly; SoundPool loading/playback failures are not usefully checked. Beeps have not simply been deleted. Root cause requires reproduction. |
| Screen timeout | Service holds a PARTIAL_WAKE_LOCK. No KEEP_SCREEN_ON handling was found in MainActivity or SessionScreen. Keeping the CPU running does not keep the visible screen on. |

Paths below are repository-relative.

## Sol foundation and ownership

Sol owns contract decisions, serialized semantics, migrations, shared resources, integration and final evidence. Executors implement bounded slices and report files changed, tests run, unresolved failures and deviations.

Before parallel work:

- Capture the dirty baseline and run relevant existing regression cases. Separate existing failures from new failures.
- Add reproductions for Preview → Train → Stretch → Preview, existing-install App-level defaults, above-tracked-level performance, emitted-and-played countdowns, and visible-rest timeout.
- Specify a shared resolver/update contract for current workout configuration. Prefer existing stores and a small shared helper over introducing another competing state store.
- Specify demonstrated-level evidence with actual-result provenance, policy/target revision and replay version. Do not let each executor invent its own serialization fields.
- Reserve `Model.kt`, backup/checkpoint schema decisions and shared string resources to Sol. Executors request contract amendments before changing those files.

| Executor | Exclusive primary ownership | Required input |
|---|---|---|
| Luna A — configuration | `ui/train/{TrainScreen,PreviewScreen,ProgramPreview,PreviewDraft,TrainData}.kt`; focused Train/Preview tests; planner-mode fixes only if demonstrated necessary | Sol's precedence and update contract |
| Luna B — progression | `domain/.../feedback/Feedback.kt`, `progression/ProgressionEngine.kt`, `Progression.kt`, related evidence/rule tests; `ui/screens/ProgressLoader.kt` | Sol's actual-performance and versioning contract |
| Luna C — audio and awake lifecycle | `session/WorkoutSessionService.kt`, `AppAudioGain.kt`, `MainActivity.kt`, `ui/session/SessionScreen.kt`; focused service/activity tests | Sol's cue and screen lifecycle contract |
| Sol — integration | Shared models, persistence/migrations, strings, backup integration, cross-lane tests, release evidence and documentation | Outputs from all lanes |

Additional files may be assigned explicitly. No concurrent edits to a shared file. B gives required Preview wording to Sol/A rather than editing A's screens. C does not edit progression evidence. A does not change reducer cue scheduling to make stretches work.

Execution sequence: foundation → A/B/C in parallel → Sol integrates configuration and progression → all integrated tests → device acceptance → release packaging through the existing workflow. This document requests future Sol/Luna execution; preparing it does not launch executors.

## Luna A — current configuration and defaults

1. Extract/reuse one Flow/Rest/Stretch selector in Train, ordinary WorkoutPreview and ProgramPreview. Give each option stable accessibility/test identifiers and sufficient width at enlarged font size.
2. Use one effective configuration resolver for display, estimates, preview building, direct Train Start, Preview Start and routine saving. Do not fix only one start path.
3. On a mode edit, update the authoritative current configuration and reconcile the matching draft's mode. Preserve swaps, ordering, removed/added exercises, targets, preparation duration, rest durations and unrelated cooldown edits. Do not clear the whole draft as a shortcut.
4. Ensure returning from Preview refreshes Train's selected mode and plan. A delayed persistence effect must not overwrite a later Train edit; use explicit update events or a revision check, not mutually-triggering effects.
5. Define override semantics: the latest global mode change replaces conflicting per-break mode overrides for this draft. Preserve dormant numeric rest durations and compatible stretch picks so switching back restores useful values. An explicit per-break edit made afterward may customize that break. Audit `stretchPicks`, per-round picks and saved edits for hidden Rest overrides.
6. Trace rule provenance from ordinary routine, saved routine/preset and draft. Initialize ordinary contexts to App levels and preserve explicit current-draft edits. Correct legacy inherited defaults at the ordinary-context boundary with a versioned, idempotent migration if needed. Do not rewrite every serialized `ProgressionRule`, historical plan or saved routine. Legacy draft rule provenance may be unknown: document a narrowly scoped one-time rule reset if required and preserve all unrelated edits.
7. Treat built-in saved-style presets according to their authored rule; do not erase intentional Recommended Routine ranges. Record this exception alongside user-saved routines.
8. Preserve preparation intervals and stretch compatibility, and rebuild durations from actual blocks. A rest-duration overlay must never convert a stretch block into passive recovery.

Acceptance:

- Reproduce the exact owner path with an existing Rest draft, then Flow draft; choosing Stretch displays actual stretches and starts the same blocks.
- Reverse path: select a mode in Preview, return to Train, start directly; selection and blocks agree.
- Repeat all modes for Straight sets, Pairs, Circuit, generated workouts, a saved routine and an editable fixed program.
- Navigate away/back, restart, save/load and backup/restore; unrelated draft edits survive.
- Existing install with an ordinary inherited Rep range opens in App levels after the scoped fix; explicit current rule edits persist; saved Rep range/Custom remain exact; leaving the saved routine restores the ordinary default.
- Warm-up, finisher, cool-down, preparation timing and above-level-5 targets remain unchanged.

## Luna B — demonstrated level followed by consolidation

Implement in evidence derivation AND replay, not just the tier equality predicate.

1. Derive the highest demonstrated level from explicit actual results and the applicable exercise-specific effective target definition. Check required sets, both sides where relevant, hold seconds/reps, load and compound requirements. A single exceptional set does not establish a level for the whole exercise.
2. Allow recognition when the user selects a higher app level and meets it, and when explicitly logged actual values meet a higher level than prescribed. For mixed sets, use the highest level that the full required result set demonstrates. If targets are equal at adjacent tiers, compare the full tier requirements rather than the scalar alone.
3. Typed/free targets in new App-level workouts may provide demonstrated evidence when all required results are explicitly logged and attributable to the current policy. Do not globally mark arbitrary historical free targets eligible. Saved Rep range/Custom retain their rule-based advancement semantics.
4. Adopt a demonstrated higher level once; update the subsequent prescription, Progress display and explanation consistently. Do not mark the adopted level mastered immediately. Handle lower stars consistently with the existing baseline convention, without fabricating historical workouts.
5. On adoption, discard the old tier's streak and count only the new qualifying exposure. Preserve existing normal same-level progression. Keep automatic progression off, manual easier overrides, discomfort holds, re-entry choices, target-revision isolation and per-weight tracks authoritative.
6. Same-rung alternatives may continue sharing eligible consolidation credit, but must not donate lower-level or incompatible-target evidence to the adopted tier. Adoption of one exercise does not establish unperformed strength on a different exercise.
7. Above-level-5 actual values remain unchanged in History, map to level 5 for progress, and never create level 6. Keep existing successor/prerequisite gates. Do not turn the first level-5 demonstration into an immediate successor change.
8. Progress is rebuilt by replay. Add a backwards-compatible semantics/version boundary so old snapshots are replayed under their original rules and new sessions use demonstrated-level recognition. Do not silently recalculate all historical free-target workouts under the new policy.
9. Corrections must deterministically recompute adoption and consolidation; duplicate logs, replay, restore and same-day sessions cannot award extra credit. A correction invalidating the evidence may remove the resulting adoption without corrupting explicit manual actions or unrelated achievements.
10. Replace stale explanatory text and comments. Suggested meaning: “Meet a higher level's targets to make it your current level. Three successful sessions at that level earn the next one.” Where useful show “1 of 3” after adoption. Do not claim the full five-level ladder exists for exercises without it.

Required examples:

| Starting state / action | Expected |
|---|---|
| Level 1; explicitly complete all requirements for level 3 | Current level 3; success 1/3; no level 4 |
| Next distinct qualifying day at level 3 | Level 3; success 2/3 |
| Third qualifying day within the window | Level 4; new consolidation count |
| Existing level-1 streak then demonstrate level 3 | Previous streak cannot immediately award level 4 |
| Select level 3, skip or leave results unconfirmed | No demonstrated-level adoption |
| One strong set, other required sets below level 3 | No level-3 adoption |
| Log performance above level 5 | Exact actual numbers retained; at most level 5; normal mastery gate |
| Auto progression off, pain hold, lighter weight or manual easier override | Respective existing protections still apply |

Extend ProgressionEngineTest, PersonalTargetsTest, ActualRepsTest, LevelUpWindowTest, WeightTrackTest, RoundCorrectionTest and relevant backup/replay tests. Cover timed holds, unilateral work, same-rung alternatives and late corrections.

## Luna C — restore warning beeps without undoing music support

1. Reproduce and instrument the pipeline: reducer event → cue mapping → loaded sample ID/status → requested gain → SoundPool play return value → audible output route. Check the installed build and persisted volume; do not diagnose from the gain helper test alone.
2. Preserve existing warning cadence: 3/2/1 seconds on eligible non-final blocks longer than 3 seconds, including rest/stretch/preparation; start and finish cues remain. Preserve short-block suppression, cue deduplication and pause behavior. No burst of missed historical countdowns after recovery or a delayed tick.
3. Check per-sample load success rather than treating any load callback as successful. Record playback failures usefully; do not block session timing indefinitely on unavailable audio. Verify bundled beep assets are non-silent.
4. Fix the observed playback/routing/gain cause. Do not restore the old ducking audio-focus request, force system volume, make silent video audible or bypass saved zero volume. Avoid simply forcing beeps to full volume while leaving speech on the slider.
5. Voice toggle controls TTS only. Rename misleading “Voice and sound cues” text if necessary through Sol. App volume controls both paths on start, settings changes, resume and recovery; TTS unavailable must not disable SoundPool cues.
6. Tests need an injected/observable audio sink that verifies actual cue delivery, not just source-text searches. Include load failure, playback failure, voice disabled, gain 0/30/100, pause/resume, recovery and rapid block transitions.

Device gate: audible 3/2/1 beeps with voice on/off, speakers/Bluetooth, downloaded music playing, default app gain and a higher gain. Verify no music pause/duck, no system-volume change and silent demonstration videos. Record device, Android version, player, route and gain. Automated mocks cannot prove audibility.

## Luna C — screen-awake lifecycle

1. Put the screen policy at Activity/window scope, driven by actual active-session state from SessionBus and the Activity lifecycle. It must cover pauses and rest screens, including blocks with no video.
2. Use `FLAG_KEEP_SCREEN_ON` (or the equivalent attached-view API) for a visible unfinished active session. Reapply correctly after rotation/recreation/recovery. Clear when there is no active session, on completion/cancellation and when the owning window is disposed. A recovery prompt alone is not an active workout.
3. Keep this independent of video-player flags and CPU wake-lock management. Do not “fix” the screen by changing session reducer events, disabling keyguard, changing system timeout settings or acquiring a wake-up lock.
4. Preserve background session timing and notification controls. A power-button lock must remain locked; unlocking back to an active workout reinstates screen-on behavior.
5. Test flag ownership for RUNNING, PAUSED, terminal, absent and recovered sessions. On a device with a short system timeout, leave a long passive rest untouched beyond that timeout; repeat with Stretch, Flow preparation, no video and pause. Then manually lock/unlock, finish and verify normal timeout returns.

Platform basis: [Android keep-screen-on guidance](https://developer.android.com/develop/background-work/background-tasks/awake/screen-on) places the flag in the Activity and allows ordinary screen-off when backgrounded. [SoundPool reference](https://developer.android.com/reference/android/media/SoundPool) documents loading and playback APIs used for audio diagnostics. Respect Android's normal lifecycle rather than promising prevention of system-enforced locks.

## Integration safeguards and release gate

| Interdependency | Required safeguard |
|---|---|
| Mode → block order → cue timing | Test actual generated Rest/Stretch/Flow plans through reducer and audio sink; UI button tests alone are insufficient. |
| Preparation → short countdown | Preserve 1–30 second setting and existing short-block warning policy; no duplicated preparation or extra cues. |
| App levels → logging → evidence → future plan | Run one end-to-end adoption scenario including saved actual numbers, Progress and the next generated prescription. |
| Draft/routine precedence → backup | Same resolver at every start/save path; old saved rules remain readable and no save occurs implicitly. |
| Screen flag → background timing/audio | Manual lock leaves timing and intended cue behavior operational; foreground screen fix cannot alter service timing. |
| Personal target changes → history replay | Versioned definitions and evidence prevent old lower thresholds satisfying new requirements. |
| Music fix → warning beeps | Gate every audio change on both music continuity AND audible beeps, independently of speech. |

After targeted tests pass, Sol runs the repository's complete `scripts/verify.sh` workflow and required fresh-build, migration, content and UI checks. Follow existing evidence conventions. Review actual screenshots for Train and both Preview forms in light/dark and enlarged text. Check upgrade from the owner's build with existing routines, drafts, personal targets and an unfinished checkpoint. Do not use clearing app data as an upgrade fix.

Combined owner-path acceptance:

1. Start music and open a workout with an existing Rest preview draft.
2. Return to Train; select Stretch; reopen Preview; verify real stretch blocks and synchronized buttons.
3. Change to Flow in Preview; return to Train; confirm direct Start uses the same preparation and no unwanted recovery.
4. Open an ordinary workout in App levels; demonstrate a higher level using explicit actual logs. Confirm adoption and 1/3, then consolidation using distinct-day test fixtures.
5. Load a saved Rep range routine and verify its targets and rule remain intact.
6. During an actual long rest, hear warning beeps and leave the screen untouched beyond system timeout.
7. Manually lock/unlock, pause/resume and recover a session; confirm correct timing, no duplicate cues, music continuity and screen policy.
8. Finish; screen timeout returns to normal. Backup/restore retains expected state.

Sol's delivery must distinguish implementation, automated evidence and actual device evidence for each of the five findings. Include unresolved root causes, source/build identity and remaining owner checks. Do not mark audible beeps or manual-lock behavior verified from unit tests or screenshots. Package a build only through the established release workflow; retain the prior build for comparison.
