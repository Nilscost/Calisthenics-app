# 19 — Executor handoff, R7 batch (paste as the first message to a Claude Code instance)

You implement the R7 batch of a personal calisthenics Android app (Kotlin, Jetpack Compose, Room; modules `:domain` pure Kotlin, `:data` Room, `:app` UI). The owner is Nils. He sideloads APKs on a Galaxy S21; you cannot reach his phone. **Never claim phone testing. Never approve owner gates. All training content stays DRAFT.**

Owner delivery decision for this batch: **do every task below, then ONE owner phone test at the end.** You still work in small slices, each verified, screenshot-reviewed and committed on its own. Don't stop between tasks to ask for a phone test.

## 1. Read first, in this order
1. `docs/18-r7-plan.md`: the contract. Bugs B1–B12 with causes and file:line references (from 0.9.0-r6; re-check them, lines drift), owner decisions D0–D12, answers O1–O3.
2. `docs/14-executor-handoff.md` §2 (environment and commands) and §3 (UI screenshots, Maestro flows, screenshot review). They apply unchanged, with the deltas in §2 below.
3. `docs/17-ui-direction-b.md`: the visual system. Every new control follows it (dark default, gold accent, Barlow Condensed titles and numbers, IBM Plex text).
4. `docs/evidence/progress-notes.md`, last ~10 entries: house style for the per-task entry.
5. The code itself. The paths in the plan are starting points, not promises.

## 2. Environment deltas since doc 14
- Version now: `versionCode 9`, `versionName 0.9.0-r6` in `app/build.gradle.kts`. This batch ships as **`0.10.0-r7`, versionCode 10**.
- The Hermes sandbox runs behind an egress proxy that **refuses github.com for git**. Push with the proxy variables unset:
  `env -u HTTPS_PROXY -u https_proxy -u HTTP_PROXY -u http_proxy GIT_TERMINAL_PROMPT=0 git -c credential.helper= -c credential.helper='!f() { echo username=x-access-token; printf "password=%s\n" "$(tr -d "\r\n" < /Users/nils/Documents/hermes/.secrets/github_token)"; }; f' push origin main`
  Use the same `env -u …` prefix for `curl` to the GitHub API. Never print the token.
- If you run on the Mac without the Android toolchain, build and verify inside the Hermes sandbox as doc 14 §2 describes (`docker exec`). Otherwise rely on CI (`android.yml` = verify, `ui-screens.yml` = screenshots) after each push. Either way, **a task is not done until its own verify exits 0 and its ui-screens run is all PASS.**
- Verify command (unchanged; never plain `./gradlew test`, it needs the network):
  `./gradlew :domain:test :data:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --offline --console=plain > /workspace/verify-r7-TNN.log 2>&1; echo exit=$?`
- Adding a field to a model data class (`WorkoutPlan`, `TimelineBlock`, `SavedRoutine`, `TrainSettings`, …): append it **last, with a default**. Tests construct them positionally.
- Room schema changes need a migration and a `MigrationTest` case. Backups (export/import) must carry any new stored field.
- **Don't touch:** `tools/blender/`, `tools/gen_clips_ai.py`, `docs/16-*`, `work/`, `docs/evidence/clips-bakeoff/`. They belong to the clip track, which runs in parallel. Clips in `app/src/main/assets/demos/` change only through that track.

## 3. Per-task loop (every task)
1. `git status` clean, `git pull`. Read the task and the code it touches.
2. Write the failing test first where the task names one (domain tests in `:domain`, UI semantics in `:app` Robolectric tests).
3. Implement, then run the full verify; read the real exit code.
4. Add or extend a Maestro flow in the **same commit** as the UI it shows (doc 14 §3). Tag new controls with `Modifier.testTag`.
5. Commit `R7-TNN: <summary>`, with `Co-Authored-By: Claude <noreply@anthropic.com>` at the end of the message. Then push.
6. Wait for `ui-screens`. Fetch the shots. Open **every** changed screen in light, dark and font13, plus the light shot of every other flow. Score each 1–5 and fix anything below 4 in the same task. Write `docs/evidence/ui/R7-TNN-review.md`, then commit and push it.
7. Copy the verify log to `docs/evidence/r7-tNN-verify-<date>.log`. Append `### R7-TNN` to `docs/evidence/progress-notes.md` with: what changed, tests, **NOT done**, and **choices not in the plan**.

If a task needs a decision the plan doesn't cover, take the option closest to the owner decisions and record it under "choices not in the plan". Only stop and ask for something irreversible or outside this repo.

## 4. Tasks (in this order)

### T01 Hold targets and logger defaults (B2, B3)
- One helper in `:domain` (e.g. `Target.shownValue()` = `max ?: value`), used by `targetLabel` (`TargetText.kt`), `LoggerState` (`SessionScreen.kt`) and `RoundEditor`.
- Holds under a range rule show **"30 s"**, not "10–30 s". The logger and the end editor pre-fill 30. Rep ranges still show "5–8 reps" on the card, but the logger pre-fills the top of the range.
- Keep `ruleMet` judging against the top, as now.
- Tests: domain test for the helper; a UI test showing that a dip support hold under the RR shows 30 s and pre-fills 30.
- Screenshot: workout block of a hold under the Recommended Routine; end editor.

### T02 Workout screen: DONE in breaks, readable labels, headers (B4, B5, B6)
- DONE on any non-work block: push the pending log, then send skip (`ACTION_SKIP`). Show DONE on breaks without a logger too.
- The block caption ("STRETCH", "REST", …): `titleMedium`, on-background colour. The stretch's own name is shown on stretch blocks.
- New header kinds **WARM-UP n OF N** and **COOL-DOWN n OF N**. They count only blocks of that type and replace "STEP n OF N". `Header.kt` plus tests.
- Screenshot: break with DONE, stretch block, warm-up header.

### T03 Rep logging on the exercise screen (D3)
- For `TargetType.REPS` work blocks, the − number + stepper is on the **work block**, pre-filled per T01. DONE saves the number, then ends the set.
- Holds keep logging on the following break.
- Add "Too easy" next to the stepper (it feeds the same `tooEasy` as the end screen, so B9 counts it).
- Update `SessionScreenTest` and the `c_session` flow.

### T04 Questionnaire at or beyond the top level (B1)
- A questionnaire answer at or above the top tier's target means tier 5 is achieved: 5 stars, and the successor (`nextVariationIds.first()`) is active at level 1.
- Use the existing `exceedsLadder` (`Intake.kt`). Apply it to every exercise, not just the plank.
- Tests: plank 60 s gives 5 stars, and hollow tuck (or the plank's successor) is active at level 1. 50 s gives level 5, 4 stars, as today.

### T05 "Too easy" and suggestions (B9)
- At tier 5, two consecutive "Too easy" for the same exercise suggest **the next exercise** (or the next weight step for loaded exercises) instead of nothing.
- On the exercise detail, show the counter "Too easy: 1 of 2". Show a dismissed suggestion again when the count reaches 2 once more.
- Domain tests for tier 5 and for the counter.

### T06 Progressions chart and corrections (B7, B8)
- `lowestSetSeries` uses effective values: an "as planned" set counts as its target, corrections are applied, and holds are in seconds.
- Points are visible: dots of at least 8 dp with value labels. With one session, show the hint "a line appears after 2 sessions".
- History reloads progress after a correction (`reviseRounds`).
- Tests: series with "as planned", corrected and hold values.
- Screenshot: e_seeded Progressions.

### T07 Clip cache refresh (B10)
- `DemoClips.file`: key the cached copy on `versionCode` and the asset size, and clear stale `cacheDir/demos` files on first run of a new version.
- Unit test with a fake cache dir.

### T08 Session length: exercises per round (D1)
- New "Exercises per round" stepper on Train, next to Rounds. Stored in `TrainSettings`, saved routines and the backup.
- Its **default** is the smallest count that makes the session **≥ 30 min** at the chosen rounds, format and warm-up setting (planner time estimate).
- Focus sessions (upper, lower, core) are filled with more exercises **of that area**: e.g. upper adds dips, rows and vertical push; lower adds a lunge/split squat, a hinge variant and calves; core adds the other planes.
- Changing the stepper by hand overrides the default.
- Tests: upper focus with 3 rounds plans ≥ 30 min; the stepper count is respected; no duplicate exercises.
- Update `StarterPlanTest` expectations only where the plan says so, and note it.

### T09 Skill sessions (D2, B11)
- A chosen skill builds the session from **the exercises that make the skill possible, at the user's level**:
  - the skill chain's current step;
  - its unmet prerequisites (`Goals.unmet`, also from other tabs; e.g. tuck front lever: hollow hold, wide row, pull-up);
  - the supporting chains' current steps;
  - then filled per T08.
- Remove the stale warning "goal-slot planning is not implemented yet" (`Planner.kt`).
- Train shows the skill's exercises as a short list ("For Front lever: …").
- Tests: front lever and handstand produce different, skill-relevant sessions; changing the skill changes the plan.

### T10 Workout preview: timeline editor, add sheet, level stepper (D4, D11, D12, O1)
- **Full timeline = the editor.**
  - It uses the same tile design as the round preview, with a round number badge on each tile.
  - Every tile can be edited individually: level (or exercise), swap, remove, number.
  - Drop the separate "Detailed edit" list and its flows; move its useful parts (per-set stretch, typed number) onto the tiles.
- **Add sheet:** category chips first (Push, Pull, Row, Squat, Hips and back, Core, Lower legs, Stretches; same names as the Progress tabs), then only that category's list.
- **Level stepper:**
  - Under App levels, the card's numbers follow the level immediately.
  - Under a range rule (Recommended Routine, Custom), the stepper steps to the **easier or harder exercise of the chain**, and the numbers stay the range (O1).
  - Show a one-line note under range rules: "Rep range: all sets at the top → next exercise".
- Tests and flows: b_train_preview, h_ready.

### T11 Progress: edit mode, navigation, tabs (D5, D6, D7, O2, O3)
- **Edit mode** on the exercise detail: set the level 1–5 by hand; every level below counts as done and the stars follow. Store it as a `UserAction.SelfAssessment` through the existing path (`LevelStore` / `ProgressLoader`). Test: V-up set to level 3 gives 3 stars.
- **Unlocks and Needs are tappable** in the node panel and the detail, including prerequisites from other tabs. Tapping one opens that exercise's detail; back returns to the previous detail (a small stack, not one level).
- **History opens the exercise detail:** the exercise rows in a workout detail, and the Progressions list. This needs an app-level route or store for "open exercise X".
- **Tabs:**
  - Remove the "With weights" tab.
  - Each weighted exercise moves into its movement's tab, **right after its bodyweight counterpart**, as a side branch from that node via its existing prerequisite edge (O3). Examples: goblet squat after the air squat, dumbbell RDL after the bodyweight RDL, weighted pull-up after the pull-up, weighted dip after dips, dumbbell row after the band row.
  - Weight tracks (V26) stay on their details.
- **Planche lean** stays in Push. Its tile shows a small "needs Plank" note, tappable (O2).
- Update `SkillTreeTest` and the `TreeTabs` list. Keep `tools/gen_review_table.py` `TABS` in sync and regenerate `docs/review/draft-numbers.md`.

### T12 Content: stretches between sets, Jefferson curl, German hang, red cautions (D8, D9)
- **Between sets**, deep (cool-down) stretches are allowed again:
  - The planner's filter keeps only `optIn` out.
  - A stretch between sets gets `max(rest window, its defaultSeconds)`, per side for unilateral stretches.
  - Give the deep stretches realistic defaults (45–60 s per side).
  - Update `StretchUseTest` and the ≤30 s assertion in `StarterCatalogTest`, with a note.
- **Jefferson curl** and **German hang:**
  - Add them in `tools/gen_starter_catalog.py` as usable by the app (not opt-in), with sources, muscles, compatible links and conservative numbers (DRAFT).
  - Each gets cautions written as "pay attention to" points:
    - Jefferson curl: start with no weight, slow segment-by-segment roll, stop at any back pain, avoid if you have a disc injury.
    - German hang: only after comfortable dead hangs, feet support first, shoulders stay active, stop at any shoulder pain.
  - Regenerate the catalog and copy it to the assets.
  - Clips: until the clip track delivers, use a placeholder (the closest existing clip, listed in `SHARED_POSES` with a comment). Record this under NOT done.
- **Light-red caution style:** new colour constants (light and dark), used for these two exercises' cautions in the detail and the workout screen. Keep amber for the others.

### T13 Cool-down switch (D10)
- A "Cool-down" switch on Train next to "Warm-up first", with two modes:
  - **Stretching:** stretches matched to the session's exercises (`compatibleStretchIds`, cool-down stretches allowed), about 5–8 min;
  - **Timed:** a relax/meditate block, default 5 min.
- Store the switch and mode in `TrainSettings`, saved routines and the backup.
- The preview shows the cool-down. Timed minutes can be changed there (− / +), and stretches can be swapped like other tiles.
- The workout header reads COOL-DOWN n OF N (T02).
- Timed block: new block kind, or a catalog item `cooldown-relax` with a calm screen (no clip, a breathing cue), counted in the time estimate.
- Tests: both modes plan the right blocks and minutes.

### T14 Release R7
- Version `0.10.0-r7`, versionCode 10.
- Full verify on a fresh clone; ui-screens all PASS; `docs/evidence/ui/R7-review.md` covering every flow.
- APK to `/Users/nils/Documents/hermes/Apps/builds/app-debug-R7.apk`. Update `STATUS.md` and `README.md` (honest: not phone-tested, content DRAFT).
- **Owner checklist `docs/evidence/owner-check-r7.md`:** one numbered walk-through (install → questionnaire → Train → preview → workout → end → History → Progress → Settings). Every B and D item maps to a step ("B4: tap DONE during the break → the next exercise starts"). End with a 1–5 score per tab, one like and one fix. Also tell him to clear the app cache once if clips look old (B10).
- Report to the owner: a table of what changed, what's verified, what's NOT done, and choices not in the plan.

## 5. Definition of done (whole batch)
- Every B1–B12 and D1–D12 item is implemented or explicitly listed as NOT done with a reason.
- All verify logs exit 0. The last ui-screens run is all PASS. Screenshot reviews are written. No test was deleted to make it pass; a changed assertion is explained in the progress notes.
- Nothing is claimed as phone-tested.
