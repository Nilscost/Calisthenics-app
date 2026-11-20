# Release 9.4: review and plan for the owner's 9.3 feedback (2026-11-20)

Written by the reviewer (Opus) after replaying the owner's backup of 2026-11-20 with the real engine and end-screen summary
(`work/OwnerReplayProbe.kt.txt`, a local probe: it mirrors `data/.../ProgressLoader.load` and is **never committed**; the backup is private).
Nothing in the app was changed. Implementation waits for the owner's answers in §4, then runs with Sonnet executors as in [doc 43](43-feedback-2026-11-12-review-and-plan.md).

Note: the backup has no hand-set levels or accepted suggestions (`LevelStore`, `SuggestionStore` are not in it). The replay still matches
every screenshot, so none were involved here, but it is a gap of its own (§3, F9).

## 1. Screenshots matched to sessions

| Screenshot | Session | Build |
| --- | --- | --- |
| "Level 2 unlocked · 0 of 3" (single-leg bridge), Dip, Inverted Row "Level kept"; preview "Welcome back… Dip level 1 / Inverted Row level 3" | Thu 12 Nov | 9.2 |
| Archer "Level 5 unlocked" + Mastered; Reverse Lunge Mastered; Pull-up "Next target 4 · Level kept"; RKC "Level 4 unlocked"; bent-knee row "Level 3 unlocked · Mastered · Next: Inverted Row · Level 1" | Sat 14 Nov | 9.3 |
| Preview "Welcome back… Plank with Arm-Leg Reach level 1"; Chin-up 0 of 3; Arm-leg reach "Level 4 unlocked" | Mon 16 Nov | 9.3 |
| Archer Mastered; Lateral Lunge "Level 4 unlocked"; RKC "Next target 35 s"; bridge "Level 3 unlocked · 3 of 3 at Level 2"; bent-knee row "Level 4 unlocked" + 5 stars | Wed 18 Nov | 9.3 |
| Preview with five long lines (Front Plank, Glute Bridge, three variations) | Fri 20 Nov | 9.3 |

## 2. What happened, per report

| # | Report | What the history says | Root cause |
| --- | --- | --- | --- |
| R1 | Single-leg bridge "Level 2 unlocked" but "0 of 3". | Level 2 was earned properly: good sessions on 6, 9 and 12 Nov. | **Already fixed in 9.3** (doc 43 #7): that screenshot is from 9.2. Replayed with 9.3 the card says "3 of 3 good sessions at Level 1", as on 18 Nov. No work. |
| R2 | Archer "Level 5 unlocked" but five stars and "Mastered"; "I was still at level 4". | Level 4 sessions on 6, 9 and 14 Nov: three good sessions in 8 days, so level 5 on 14 Nov is **correct**. The archer has 4 stars of its own (`achievedTiers` 3, 4). | **Stars bug.** `displayStars` (`tree/SkillTree.kt:173`) returns 5 as soon as *any* later exercise of the route has a progress record. `pushup-half-one-arm` got one from a workout abandoned after one partial set on 6 Nov 15:03. So the card hides the dots and says "Mastered". Same on 18 Nov (level 5, really 1 of 3). |
| R3 | Bent-knee inverted row "Level 4 unlocked" + five stars + "Next: Inverted Row · Level 1"; "I was at level 3". | You swapped to the bent-knee row in Preview on 14 Nov. Level 3 sessions on 14, 16 and 18 Nov, so level 4 on 18 Nov is **correct**. | Same stars bug (the full inverted row has a record), and `end_next_exercise` is a fixed string "Next: %1$s · Level 1": the full inverted row is really at level 4. |
| R4 | Reverse lunge "Mastered". | Level 4, 3 stars. | Same stars bug. Also affects plank-contralateral and the Progress tab. |
| R5 | Pull-up and chin-up "0 of 3" after sessions that felt fine. | Chin-up: 2 Nov 3·3·3 at target 5, so the next target dropped to 3 (lowest set). 12 Nov 3s done, 16 Nov 4s done, 20 Nov 5·5·4. Pull-up: 14 Nov 5·5·4, so 4 next time. | By the rules: a session only counts toward the next level when every set reaches the **level's** target (5). Sessions at the lowered target (3, 4) never add a dot. And one short set (5·5·4) immediately drops the next target to the lowest set (W2). The card does not say any of this: "0 of 3" plus "Next target: 4" reads as moving *up* from 3 to 4. |
| R6 | RKC plank "Next target: 35 s": "maybe implement that". | Implemented in 9.3 (L4 ramp): the 20 Nov plan uses it. | The chip only appears when the ramp moved, so most cards have no "next time" line. Proposal: one "Next time" line on every card (§3, F4). |
| R7 | Preview text too long. | 20 Nov: five sentences, two of them "You finished every level of Front Plank / Glute Bridge" (finished weeks ago). | `Planner.kt:292` writes the line on **every** plan after the move-on, not only on the first. Each rotation is its own two-sentence line (`Planner.kt:320`). Nothing limits the count. |
| R8 | "Welcome back… starts at level 1" after 14 days feels like starting from scratch; prefer one level down. | Dip was at level **1** in the replay (the 22 Oct level 3 plan was only a starting estimate and the sets were below target). So "starts at level 1" changed nothing. The inverted row went 4 → 3. | The rule **is already one level down** (`reentryNeeded`: `max(1, tier − 1)`). The wording is the problem: it never says "one level easier", and it shows even when nothing changes. Second problem: after doing that easier level, the engine still keeps the old level. On 12 Nov the inverted row card said "Level 4 of 5 · Level kept" after a level 3 workout. |
| R9 | First workout of an exercise says "Level 3 unlocked" (bent-knee row 14 Nov, pallof press, contralateral plank). | First session ever, planned at a starting estimate or a level picked in Preview, and done as planned. | Shown as an unlock with the gold card. Nothing was unlocked: it is the starting level. |

## 3. Proposed fixes (release 9.4)

### F1 · Stars follow your own level while you still train the exercise (R2, R3, R4)

`displayStars` counts a later exercise as "moved past" only if **both**:
- it has completed work (an abandoned plan or skipped-only evidence does not count), and
- it was trained more recently than this exercise.

Otherwise the exercise shows its own stars. The knee push-up you left long ago keeps five stars; the archer push-up, the bent-knee row and the reverse lunge show 4, 3 and 3.
Same function feeds the end screen, Progress and the exercise page.

Also:
- `end_next_exercise` shows the next exercise's real level, not a fixed "Level 1".
- The engine stops creating a progress record (and a "last trained" day) from evidence with no completed work. This is the root of the phantom half one-arm push-up.

Acceptance: replay of the owner's history (local probe) gives archer 4 stars and "Level 5 unlocked · 3 of 3 good sessions at Level 4" on 14 Nov. The bent-knee row shows "Level 4 unlocked · 3 of 3 at Level 3" on 18 Nov, without "Mastered". Fixture tests reproduce both shapes without the private data.

### F2 · Missed sets: hold first, step down after two misses (R5, owner's rule)

Replace the W2 rule "lowest logged set becomes the next target" with:

| Today, at target T | Next time | Counts toward the next level? |
| --- | --- | --- |
| Every set ≥ T | T climbs (the 9.3 ramp) | Yes, when T ≥ the level's target |
| One miss (e.g. 5·5·4 at 5) | **T again** ("Level kept") | No; earlier dots are kept |
| Second miss in a row at T | T − 1 rep (− 5 s for holds), never below the previous level's target | No |
| Far off (lowest set ≤ half of T, e.g. 3·3·3 at 8) | Lowest set, as today; "Go down a level" stays offered | No |

Building back up from a lowered target is unchanged: + 1 rep per good session up to the level's target.
Replay must leave every level and star where it is. Only the next prescription changes, so chin-up next time is 5, not 4.

### F3 · Plain progress lines on the card (R5, R6)

Each card gets exactly two progress lines:
1. **Next time:** "Next time: 6 reps" (or "Next time: 5 reps again", "Next time: 4 reps, then back to 5"). Always shown for exercises with levels. It replaces the "Next target" chip.
2. **Dots** with the number in it: "●○○ 1 of 3 good sessions at 5+ reps → Level 4 (8 reps)".

With the ramp, each good session is one more rep (pull-up 5 → 6 → 7 → Level 4 at 8). This makes the in-between steps visible without changing the rule.

### F4 · Re-entry after 14 days, said plainly and kept (R8)

- Keep the rule: one level down after 14 days without the exercise, stars kept.
- Show it only when the level actually goes down: "Back after 2 weeks: Inverted Row one level easier today (3, was 4)."
- Doing that easier workout makes it the current level, as if the re-entry choice had been accepted. The card then reads "Level 3 of 5 · back after a break", not "Level 4 · Level kept". Re-earning level 4 takes the normal 3 good sessions. The gold "unlocked" card is not shown for re-earning a level you had before (show "Level 4 again").

### F5 · First session: "Starting level", not "unlocked" (R9)

On the first ever session of an exercise, the card says "Starting at Level 3" (no gold border, no star animation), followed by the normal dots.

### F6 · Preview top text: three lines at most (R7)

- "You finished every level of X, so today you do Y" appears only in the first plan after the move-on. Later plans never repeat it.
- All rotations become one line: "Swapped in today: Uneven Push-Up (for Archer), Chin-Up (for Pull-Up), Shoulder Taps (for RKC Plank)." The rotated rows in the list get a small "Rotated" caption.
- Order: things to act on first (missing equipment, re-entry), then rotation, then info. At most three lines visible; the rest behind "More".

### F7 · "Mix it up" button in Preview (owner decision 2026-11-20)

Today variety exists only *inside* a rung: alternates of the same rung rotate in (uneven ↔ archer, chin-up ↔ pull-up, shoulder taps / contralateral / arm-leg reach ↔ RKC plank). The owner likes keeping the same 8 exercises, so nothing becomes automatic. Instead there is a manual button.

- **Where:** a small "Mix it up" text button above the exercise list in Preview, with a caption ("Swap 2 exercises for a different type today"). There is no nudge or reminder; it only acts on a tap.
- **Which 2 are swapped: the ones with the least progress to lose.** Order:
  1. exercises with nothing left to earn (mastered, no next step);
  2. then the fewest good sessions toward the next level (the same `levelProgress` as the card dots);
  3. ties go to plan order, last first.
  Exercises the person swapped or added by hand are never picked. Example from 20 Nov: the first tap swaps Side Plank (mastered) and Chin-Up (the last of the 0-of-3 ones in plan order). The next tap swaps Split Squat and Uneven Push-Up instead; each tap replaces the previous pair. Dip (2 of 3) stays.
- **What replaces them: a different type, missing from today's plan,** whichever was trained least recently (never trained counts as oldest). The types:
  - back extension (superman hold, reverse hyper)
  - anti-rotation (pallof hold / press)
  - calves
  - hip hinge (single-leg RDL, bodyweight RDL)
  - lateral (lateral lunge, Copenhagen side plank)
  Within a type, use the exercise the person already trains at their own level (route active step); otherwise the type's entry exercise, with the usual starting estimate. Equipment and the excluded list apply. Each type is defined once in the domain as a list of route/family ids; no catalog change unless a type has no usable exercise.
- **Tap again:** the next pair in the same order, with the next replacement types. **Undo** restores the original plan. Each change goes through the existing draft-edit path (`PreviewDraft.swaps`), so Start, Save as routine and the round preview behave as for a hand swap.
- **Only this workout:** the next plan is the usual 8 again. The swapped-out exercises lose nothing. A skipped exercise is simply not trained that day: it keeps its dots and level, and re-entry only applies after 14 days.
- **Rotated tag:** the replaced rows show a "Mixed in" caption. They also appear in the F6 one-line summary ("Mixed in today: Superman Hold (for Side Plank), Pallof Press (for Uneven Push-Up)").

### F8 · Small consistency items found on the way

- `earnedStars` counts only consecutive achieved levels from level 1. Split squat (levels 3 and 4 achieved, now level 5) and pull-up (level 2 achieved, now level 3) have **0** earned stars. Count stars as "levels below the current one" when the current level was reached by the engine. Replay must not change any card that is already right.
- `lastTrainedDay` is set by fully skipped evidence too, so re-entry can be postponed by an abandoned plan. Fixed together with F1's second bullet.

### F9 · Backup misses hand-set levels and accepted suggestions

`BackupPayload` has no `LevelStore` actions or accepted suggestions ("Go down to level N−1", "Too easy" moves). A restore therefore loses them. Add both (appended fields with defaults, the old files still import), with a round-trip test.

## 4. Owner decisions (2026-11-20)

1. **F2 missed-set rule:** approved as written.
2. **F7 variety:** a manual "Mix it up" button in Preview that swaps 2 exercises. Swap the ones with the least progress to lose and replace them with a different type. Only on tap, no nudge. Details in F7.
3. **F4:** 14 days stays.

## 5. Lanes (after the answers)

| Lane | Owns | Items |
| --- | --- | --- |
| L1 engine | `ProgressionEngine.kt`, `TargetRamp.kt`, `SkillTree.kt` (`displayStars`, stars), tests | F1 engine part, F2, F4 engine part, F8 |
| L2 end screen | `SessionProgressSummary.kt`, `EndScreen.kt`, strings | F1 card part, F3, F4 card, F5 |
| L3 planner text | `Planner.kt` notes, Preview notes UI | F4 wording, F6 |
| L4 mix it up | new `domain/.../planner/MixItUp.kt` (+ tests), Preview button and caption, strings | F7 |
| L5 backup | `Backup.kt`, `BackupApply.kt`, tests | F9 |

L2 depends on L1's `ExerciseProgress` fields; it starts after L1 merges. L3, L4 and L5 are independent.
Every lane: tests first, `scripts/verify.sh` green. Before release, the coordinator runs the local probe and compares the owner's history before and after. Expected: no level or star change except F1/F8 display, and next targets differ only by F2.
Release as 9.3, steps unchanged: version `0.12.4-r9`, code 23, APK `Apps/builds/app-debug-R9.4.apk`, web redeploy, STATUS.md section with phone checks.
