# User feedback

## Workout feedback · 2026-10-15

1. **Avoid two exercises from the same progression in one generated workout.** A general-strength workout included two types of push-up. Multiple exercises from the wider push category are fine; two steps or variants of the same push-up progression should be avoided.
2. **Slow the voice by about 25%.**
3. **Announce the working side.** Say left and right when an exercise is split into two sides.
4. **Allow five seconds to change sides for holds.** Add a get-ready interval between left and right holds, such as side planks. Do not shorten either hold.
5. **Critically review progression before implementing new ladder changes.** Write down how the new exercises fit together and consider a round of debate before deciding. Unapproved structural ladder changes are proposals for review.
6. **Repair two stretch clips.** Thoracic rotation has an unnatural shoulder/arm pose resembling a dislocation; check joint anatomy carefully. Rear hand clasp hides the action behind the back; change the viewing angle so the hand position is visible.

### Execution status

- Recorded all six comments. Duplicate-progression prevention, voice speed/side cues and five-second hold-side setup are implemented; all 188 app tests now pass after media installation.
- Structural progression changes were held for review, then authorized on 2026-10-16 after the owner accepted the review of `progression-comment.md`. See `30-progression-implementation.md` for the implementation and verification checkpoint.
- Rear hand clasp: corrected overhead arm and rear camera, reviewed four Blender frames across the loop, rendered, encoded and installed the replacement clip and thumbnail. Phone verification remains pending.
- Thoracic rotation: the first repair improves the arm position but does not show enough independent upper-back rotation. Candidate saved for further work; its replacement is not installed or accepted.
- No phone verification is claimed.

## Review lens clarified by the owner · 2026-10-15

Keep the exercise library comprehensive while keeping the app uncluttered and the progressions coherent. Library inclusion does not imply automatic prescription, a mandatory progression step, a new top-level screen, or a permanent visible choice. Prefer a small understandable main path, optional alternatives when relevant, and supporting work clearly separated from mastery requirements. Question or omit an addition that cannot justify its role; do not expand a ladder merely to accommodate every catalog entry.

## Historical integration hold · 2026-10-15

The current R8 catalog and app work remain a draft checkpoint, not approved final inclusion. Pause catalog expansion and app feature work while pose work is completed. Complete and review all 379 exercise pose sources for Blender before discussing which exercises to integrate. Preserve the existing work and behavior during this hold.


## Workout preview rest editing · 2026-10-16

Owner reports that rest durations cannot be changed in the workout preview. Make rest times editable there and carry the chosen durations into the started workout and preview total. This is explicitly requested app work alongside the remaining pose review; exercise-inclusion decisions remain held. Implemented: plain rest has a compact15-second stepper (15–300s), applied across sets and reflected in the preview total and started workout. Rest/stretch swapping and Reset remain available. Four focused preview checks pass after the final compact-row adjustment; the prior progression-name test failure also reproduces on unchanged HEAD. See `docs/evidence/r8-preview-rest-verify-2026-10-16.log`. No phone verification or APK installation is claimed.

Layout clarification: place the rest/stretch card and existing “+” button in one horizontal row. The card fills the available width after reserving the existing button size and gap.

## Progression implementation authorized · 2026-10-16

The owner accepted the review of Opus's `progression-comment.md` and asked to implement it. Use explicit main paths, route-specific alternatives and support, separate RDL and Nordic work, preserve app-achievement prerequisites and existing history, and keep specialist exercise pools off by default. Main-path mastery controls advancement; alternatives retain independent targets and rotate across completed workouts. Difficulty rank remains useful for display ordering and does not establish the next required exercise. This authorization supersedes the progression/app integration hold above; rendered demonstration acceptance and phone verification remain separate.

## Starting targets for alternatives · 2026-10-16

Owner: if feet-elevated push-ups are at12 reps, a first diamond push-up workout should start around8–10 instead of at the bottom. Implement a conservative product starting estimate for an untried, explicitly same-rung alternative:75% of the main exercise's current target, rounded down to an authored rep target and below the alternative's top tier. The example selects8 reps. Require the same family, progression group, movement pattern and side structure, with bodyweight reps only; do not convert holds, weighted work or unrelated core movements. Existing alternative history, explicit manual targets, rep-range rules and discomfort safeguards take precedence. The estimate establishes a starting level; completed tiers are not copied. Subsequent sessions use the alternative's own results. This percentage is a product heuristic, not a researched equivalence between exercises.


## Installation scope clarified · 2026-10-16

The owner authorized everything needed for an installable release and clarified that feet-elevated to diamond was an example for comparable progressions. The final implementation supports explicit same-rung alternatives sharing progression group, movement pattern, side structure, unloaded kind and target units, including comparable timed holds. Legacy physical-family splits do not prevent a reviewed match; discomfort in either family does. Rep and seconds targets never convert across units. An alternative encountered after main-path advancement uses its original anchor's history. Existing alternative history and manual targets win; estimates do not complete tiers. This extends the initial rep-only checkpoint above.

All 379 new clips and thumbnails are installed. Six camera-only corrections were fully rendered and encoded. Twenty-five unclear demonstrations, including thoracic rotation, use written instructions instead of playback. The rest card shares its row with the unchanged-size + button, and plain rests are editable. A fresh checkout passes 435 domain tests, 188 app tests, lint and assembly. The R8 APK is built with the same signing certificate as R7.2; emulator screenshot and phone acceptance results are recorded separately.


## Final scope corrections · 2026-10-16

The owner wants videos for every included exercise; written instructions do not replace an unclear demonstration. Reopen and redo all 25 held videos, then review their actual rendered movements before final installation. Extend the mannequin where necessary instead of accepting missing neck, scapular, finger, arch or thoracic motion.

Fixed programs belong in the Routine section alongside saved routines. Remove the separate header Programs action. The owner did not request extra switches: remove Extras and the Power, Conditioning, Preparation and Mobility pool switches. These exercises remain available for manual addition in Workout preview and fixed routines, with equipment filtering. Clear old hidden pool selections so they cannot silently broaden generated strength workouts. Flow mode is explicitly accepted and stays. New information expanders show progression alternatives/support and program details; they do not change training settings.


## Manual specialist stretch policy · 2026-10-16

Applying the final scope corrections also to flexibility: new contract stretches and mobility drills are available through manual Preview choices and fixed routines, with equipment filtering. They are excluded from automatic inter-set and cool-down matching, so a general-strength workout does not unexpectedly prescribe a front/middle split or another specialist flexibility drill. Existing default stretches remain available for automatic recovery. No new switches are added.


## Final feedback audit · 2026-10-16

Extended the five-second same-exercise left-to-right setup to stretch-only sessions and unilateral cool-down holds, preserving the full hold durations. Preview rest and exercise edits now survive the asynchronous history refresh. At larger system text sizes, exercise names use the card width so long labels stay readable. Pop-up sheet controls now expose their existing identifiers to the emulator checks.


## Archer push-up level 3 not counted; exercise history in Progress · 2026-11-05

Owner report (release 8.2): the archer push-up was raised to level 3 by hand, the level-3 reps were done, and the session was not counted as one of the 3 good sessions. Also: tapping an exercise in Progress should show its history below.

**Owner's real steps (added later the same day).** The archer push-up was at level 1; he tapped + twice on the exercise's level stepper in the Workout preview (level 3, "12 reps, 6 per side"), did the reps and pressed DONE. He did not use the detail-page chips. The Preview + only writes `PlanEdits.tierOverrides` (stamped with its exercise and day, kept in the Preview draft); the planner then prescribes level 3 (stored target 6 per side, shown as 12 total; the logger asks "reps done per side" and starts at 6). Nothing is written to progress. The logger sends no number when DONE is pressed without editing it ("untouched = as planned"), so the session had no explicit results.

**Finding (cause of this report).** Progress is rebuilt from the sessions. A session prescribed above the person's current level counted only when explicit logged numbers met that level (`DemonstratedProgressionTest`, doc 35 item 2): "assumed" results never raised a level. So a level chosen in the Preview and done as shown, with the pre-filled number accepted, was a completed session at level 3 for an exercise whose current level was 1: it neither made level 3 current nor counted as a good session, and the end screen showed no progress. Typing the number (6 per side or more) did count; the per-side/total display was not involved (reproduced both ways in `PreviewLevelRaisedSessionTest`).

**Fix.** In `ProgressionEngine.replay`, a session that was prescribed a level above the current one (the planner only does this for a level chosen by hand; an inferred starting estimate is still no evidence), completed in full, not below target, no discomfort, with no typed numbers, is treated like typed results at that level's own target: level 3 becomes current and the day is the first of 3 good sessions ("1 of 3 toward level 4"). A skipped or partial set, a number below the target, or the manual-easier / hold / paused states still do not adopt. Because progress is recomputed from history on every load, his past session is re-evaluated by this rule automatically (provided it was saved as a completed session on release 8.2); no stored data changes. The older test `selectedLevelWithoutConfirmedFullResultsDoesNotAdopt` encoded the previous rule and was changed (same name; its first assertion now expects level 3, the skipped-set one still level 1). Tests: `PreviewLevelRaisedSessionTest`.

**Separate, also real issue (earlier the same day).** A hand edit of a level (the detail-page chips) is dated to its day and, by design, restarts that exercise from that day (older sessions cannot undo it). The progression replay ran the sessions of a day before the actions of the same day, so a session done on the day of the edit (raise the level, then train) was replayed first and the edit then wiped its streak: the end screen showed no good session. Sessions on later days were counted correctly. Nothing is specific to the archer push-up (the side display change is unrelated).

**Fix (same-day edit).** A dated level edit is now applied before the sessions of its own day (`ProgressionEngine.replay`); other actions (accept harder/easier, overrides) still follow the sessions of their day. A session logged on the same day as a level edit now counts; sessions on days before the edit still do not. Tests: `ManualLevelSameDayTest`.

**History.** The exercise detail page (reached from the Progress tab) has a History section under the levels: the past sessions of that exercise, newest first, with date, level, the logged sets (reps or seconds; per side for alternating exercises) and a "Good session" marker; a small caption when there are none. Read through the shared history repository (Android and web). Tests: `HistoryStatsTest`; the Maestro `e_seeded` flow opens a seeded exercise and screenshots it.

## Train count differs from the Preview · 2026-11-05

Owner report (release 8.2, Android): Train showed 6 exercises, the Workout preview had 7. Finding: the "Exercises per round" number was the number of slots requested from the routine, not the number of exercises the plan holds. The planner leaves a requested slot out without telling Train (a second skill step that works the same movement as the first, or an exercise the equipment rules out with nothing to replace it), so the default count (`defaultExercisesPerRound`, `TrainPlan.kt`) and the stepper (`TrainScreen.kt`) could show one more than the list had (reproduced: back lever, Home profile, default 7 slots, 6 exercises; `TrainCountTest`). Both screens now show what the workout holds. Fix: the plan asks for the next filler while it holds fewer exercises than requested (`planFor`), the default and the stepper top count only exercises the plan really builds (`reachableExercisesPerRound`), and Train shows the plan's own count. Exercises taken out or added by hand are never put back. The Workout preview also has the Exercises per round stepper (`preview_exercises`), next to Rounds, sharing the Train setting and starting a fresh selection as on Train. No phone verification is claimed. The owner's direction (Train 6, Preview 7) comes from the same cause on the other side: an exercise added by hand in the Preview stays in the saved draft, so the workout holds the set count plus one while Train still showed the set count; Train now shows the count of the plan itself (`aHandAddedExerciseIsPartOfTheCountTheWorkoutHolds`). The conditioning finisher, Pairs and swaps were ruled out (they never change the count).

**Owner decision (2026-11-05):** a level raised in the Workout preview counts once every set is completed at the planned reps and Done is pressed. Typed numbers are not required; "too hard", pain or a below-target set still block it. This replaces doc 35 item 4 / table row "Select level 3, skip or leave results unconfirmed → no adoption" for completed sessions; skipped or unconfirmed sets still do not adopt.

## Release 9.1 test · 2026-11-06

Owner report (9.1, Android), handled in release 9.2 ([plan](42-feedback-2026-11-06.md)):
- **Half One-Arm Push-Up in the Preview, too hard, while on the Archer Push-Up.** It was authored as an alternative at the archer rung, and alternatives rotate with the main exercise. It is now the main step after the archer push-up.
- **"Too easy" during training.** Removed from the training screen; kept on the end screen next to "Too hard".
- **"New exercise unlocked: Bulgarian Split Squat" while Split Squat is not at its top level.** The next main step of an exercise still being trained is no longer announced until the route moves on to it.
- **Two cards for one exercise on the end screen.** One card per exercise; a level-up shows on that card.
- **Stars read one level low.** The current level is now a dark star with a gold outline.
- **Calf Raise and Plank Shoulder Taps too easy.** Calf raise 20–40 (was 15–25), shoulder taps 10–20 (was 6–16).
