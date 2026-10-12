# 18 — R7 batch: owner phone feedback of 2026-10-12 (on 0.9.0-r6)

Status: **plan, decisions mostly answered (2026-10-12).** Open: O1–O3 below.
Delivery: owner decision D0 — all changes in ONE batch, one owner phone test at the end (`owner-check-r7.md`). Work still goes in small verified slices (verify, ui-screens run, screenshot review, commit each).

Code facts were checked by three read-only code reviews on 2026-10-12 (file:line references below are from 0.9.0-r6).

## Bugs (B) — cause → fix

| # | Owner report | Cause (code) | Fix |
|---|---|---|---|
| B1 | Plank 60 s in the questionnaire → level 4, 4 stars, "50s" | Plank tiers 15/20/30/40/50 s; `suggestStartingStep` (`Intake.kt:13`) puts him on tier 5 as a self-assessment, `earnedStars()` (`ProgressionEngine.kt:75-81`) only counts achieved tiers → 4. `exceedsLadder` (`Intake.kt:17`) is never called. | Questionnaire value ≥ top target → tier 5 achieved (5 stars) and the successor unlocked at level 1. Same for any exercise. Test. |
| B2 | Dip support hold shows "10–30 s", logger starts at 10 | Rep-range rule gives holds `holdFrom 10 / holdTo 30` (`Planner.kt:331`); `targetLabel` prints the range (`TargetText.kt:11-17`); `LoggerState` defaults to `target.value` = 10 (`SessionScreen.kt:98`). | Holds: show and pre-fill the top value (30 s); the range stays inside the rule only. One helper "display/default = max ?: value" for `targetLabel`, `LoggerState`, `RoundEditor`. |
| B3 | "Edit what I logged" shows 10 s for a 30 s hold | `RoundEditor.kt:36` same root cause (data saved correctly as "as planned"). | As B2. |
| B4 | DONE during a break does nothing | Break DONE = `logger_done` only confirms the number (`SessionScreen.kt:207-214`); reducer ignores `EarlyDone` outside WORK (`SessionReducer.kt:220`). | DONE in any non-work block: save the log, then skip to the next block. Show DONE on breaks without a logger too. |
| B5 | "Stretch" between sets unreadable | Caption is `labelSmall` (~11 sp) muted grey (`SessionScreen.kt:187`), only place the word appears. | `titleMedium`, on-background colour; stretch name visible too. |
| B6 | No warm-up header | Header falls back to "STEP n OF N" over ALL blocks (`Header.kt:15`), e.g. STEP 1 OF 92. | Header kinds WARM-UP n OF N and COOL-DOWN n OF N counting only those blocks. |
| B7 | Progressions chart shows only the dashed line | `lowestSetSeries` (`History.kt:389-396`) only uses numbers typed in the workout; "as planned" sets and corrections are dropped; one session = one small dot. | Use effective values (as planned = target, corrections applied); bigger labelled dots; hint "line from 2 sessions". |
| B8 | Corrected 14 reps, progression didn't move | By design progression sees MET/BELOW/ABOVE only (14 ≥ target = MET). Real gaps: chart ignores corrections (`History.kt:387`), History doesn't reload progress after a correction (`HistoryScreen.kt:62`). | B7 + reload after `reviseRounds`. Explain in the checklist that more reps than the target alone does not level up (two "Too easy" or the pacing gate does). |
| B9 | Two "Too easy" gave no Move-up card | Silent blockers: no suggestion at tier 5 (`ProgressionEngine.kt:198`, `Suggestions.kt:402`) — his exercises are tier 5 after the questionnaire; "Too easy" only counts from the end screen / History editor. | At tier 5 suggest the successor (or the heavier step); progress hint "Too easy 1 of 2" on the exercise; "Too easy" also on the workout logger. |
| B10 | Feet-elevated push-up clip = standard | Shipped asset is the own v3 pose (different file), but `DemoClips.file` (`DemoClips.kt:20-27`) copies each asset into the cache once and never refreshes → old clip on the phone. | Key the cache on versionCode + asset size; clear `cacheDir/demos` on upgrade. (Quick owner check: clear the app cache.) |
| B11 | Changing the skill doesn't change the routine | `Goals.routineFor` (`Goals.kt:102-112`) swaps ONE slot; Train doesn't show it; stale warning "goal-slot planning is not implemented yet" (`Planner.kt:86`). | See D2. |
| B12 | Preview level stepper doesn't change reps | With Rep range / Custom rule the target comes from the rule, not the tier (`Planner.kt:331-333`). | See O1. |

## Owner decisions (D)

- **D0** One batch, one phone test at the end.
- **D1 Session length.** A stepper "Exercises per round" on Train; its default is set so the session is **at least 30 min** for the chosen rounds. Focus sessions (upper / lower / core / skill) are filled with more exercises of that category (e.g. upper: push, pull, dip, row, vertical push).
- **D2 Skill sessions.** A chosen skill builds the session around **the exercises that make that skill possible, at the user's level**: the skill's own next step plus its unmet prerequisites and supporting chains (e.g. front lever: tuck front lever step, hollow hold, wide rows, pull-ups), then filled per D1. Train shows which exercises the skill brought in.
- **D3 Logging reps.** Rep exercises: the − number + stepper is on the exercise screen itself, so the number can be set before DONE. Holds: logged on the following break (as now). DONE in a break skips the break (B4).
- **D4 Full timeline = the editor.** Same block design as the round preview, each tile carries its round number, every tile can be edited individually (level, swap, remove, numbers). The separate "Detailed edit" list is dropped.
- **D5 Manual level.** An **Edit** mode on the exercise detail sets the level 1–5 by hand; everything below counts as done (stars follow). Use case: owner already does V-ups.
- **D6 Navigation.** In the exercise detail and node panel, "Unlocks" (next exercises) and "Needs" (prerequisites, also from other tabs, e.g. tuck front lever's requirements) are tappable and open that exercise. History (workout detail rows and Progressions) opens the exercise detail.
- **D7 Weighted exercises.** The "With weights" tab disappears; weighted exercises move into their movement's tab (see O3 for where in the chain).
- **D8 Deep stretches between sets** are allowed again; a stretch gets its own (longer) time between sets even if that makes the rest longer than 60 s.
- **D9 Jefferson curl and German hang** are added and may be used by the app (not opt-in only), each with a **light-red** "pay attention to" warning (new caution style, distinct from the amber one).
- **D10 Cool-down switch** on Train (next to Warm-up first) with two modes: **Stretching** (stretches matched to the session's exercises, about 5–8 min) or **Timed** relax/meditate (default 5 min, minutes changeable in the workout preview).
- **D11 Add-exercise sheet:** choose a category first (Push, Pull, Row, Squat, Hips, Core, Lower legs, Stretches), then the list of that category.
- **D12 Level stepper shows the new level's numbers** in the preview (owner: "if you move the level up you should see the new level"). See O1.

## Open (O) — with recommendation

- **O1 Level stepper under a Rep-range routine** (Recommended Routine uses 3 × 5–8): when the user moves an exercise's level in the preview, that exercise switches to App levels so its reps follow the level (recommended), or the stepper is greyed out with a note.
- **O2 Planche lean placement:** keep it in Push as the first planche step; its prerequisite "plank level 4" (Core tab) is listed in the detail as a tappable "Needs" entry and shown on the tile as a small "needs Plank" note (recommended). Today the Push tree only draws links inside Push, so the plank requirement is invisible.
- **O3 Weighted exercises in the chain:** at the bottom of each movement chain as "add load" after the top bodyweight step (e.g. squat → … → pistol, then goblet squat → barbell squat) (recommended), or as intermediate steps between bodyweight levels.

## Implementation slices (each: verify, ui-screens, screenshot review, commit)

1. **S1 Workout screen:** B2, B3, B4, B5, B6, D3 (logger on rep work blocks, Too easy on the logger).
2. **S2 Progress logic:** B1, B9, B8 reload, B7 chart.
3. **S3 Clip cache:** B10.
4. **S4 Session length + skills:** D1, D2, B11 (stale warning removed), Train shows the skill's exercises.
5. **S5 Preview:** D4 timeline editor, D11 add sheet, D12/O1 level stepper.
6. **S6 Progress UI:** D5 Edit mode, D6 navigation, D7 + O3 tabs, O2 planche note.
7. **S7 Content:** D8 between-sets stretches, D9 Jefferson curl + German hang (poses for clips via the clip track), new light-red caution style.
8. **S8 Cool-down:** D10.
9. **S9 Release:** version bump, owner checklist `owner-check-r7.md` (one walk-through mapping every B/D to a step), STATUS, README, draft numbers regenerated.

Clips run separately (doc 16 bake-off); the Jefferson curl and German hang get clips from whichever route the owner picks.
