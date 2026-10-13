# Owner check, release R7 (0.10.0-r7, versionCode 10)

Nothing here was tested on a phone: this list is what you walk through on the Galaxy S21. All exercise content is an unreviewed DRAFT.
Install `app-debug-R7.apk`. **If a clip looks old (for example the feet-elevated push-up still looks like the standard one), clear the app cache once** (Settings > Apps > the app > Storage > Clear cache). The new build also clears old clip copies by itself on first start (B10).

Each step names the item it checks (B = bug of 0.9.0-r6, D = decision, O = answer).

## 1. Install and questionnaire
1. Install, open the app, go through the questionnaire. On the push-up or plank page choose "I do this" and type a number **above the top step** (plank 60 s; the plank steps are 15, 20, 30, 40, 50 s). On "You are ready" the plank card says all steps are done and the next exercise starts at level 1 (B1).
2. Type exactly the top number (plank 50 s): it stays level 5 with 4 stars (B1, as before).

## 2. Train
3. Pick "Focus: upper body", set Rounds to 3. Under Rounds there is a new **Exercises per round** card; its number makes the session at least 30 minutes (the minutes tile says so). Tap - and +: the minutes follow and the number is yours until you change the objective (D1).
4. Pick a skill (Front lever). A line "For Front lever: ..." lists the exercises it brought in; pick Handstand push-up: the list and the minutes change (D2, B11).
5. Switch on **Cool-down**: two modes appear, Stretching and Timed. The minutes tile grows (D10).

## 3. Preview
6. Open the Preview. Each round shows tiles; the stepper on a tile changes the level and the numbers follow (D12).
7. Choose the Rep range rule: a line says "Rep range: all sets at the top -> next exercise"; the tile stepper now steps to the easier or harder exercise of the chain and the range stays (O1, B12).
8. Open **Full timeline**: every set's tiles, each with its set number; per tile you can swap, remove, change the level, type a number or change the stretch for that one set (D4). There is no "Detailed edit" switch any more.
9. Tap "+" between two tiles: first choose a category (Push, Pull, Row, Squat, Hips and back, Core, Lower legs, Stretches), then only that list is shown (D11).
10. At the end of the Preview the Cool-down: stretches you can swap, or minutes with - / + (D10).

## 4. Workout
11. Start. Warm-up blocks read **WARM-UP 2 OF 8**; the cool-down reads **COOL-DOWN 1 OF 6** (B6).
12. On a break the caption ("STRETCH" with the stretch name, "REST") is large and readable (B5). **Tap DONE during the break: the next exercise starts** (B4). DONE also exists on breaks that have no logger.
13. On a rep exercise the **- number +** is on the exercise screen itself, with "Too easy" beside it; DONE saves the number and ends the set (D3). Do "Too easy" twice in a row for an exercise at its top level (see step 17).
14. A hold (dip support hold under the Recommended Routine) shows **30 s** (not 10-30 s) and the logger after it starts at 30 (B2). The same number is in "Edit what I logged" on the end screen (B3).
15. Jefferson curl and German hang (find them in Progress, steps 20-21): the workout screen and the detail show a **light-red** box with "Pay attention to" points (D9). Their clip is a stand-in (the Romanian deadlift and the dead hang) until the clip track delivers their own.
16. In a routine with stretching between sets: a deep stretch (pigeon, frog...) can come between sets and then has its own 45-60 s (per side) even if the break is longer than 60 s (D8).

## 5. End, History
17. After two "Too easy" in a row at level 5 the Progress tab shows a card **"Move on to <next exercise>?"**; the exercise detail shows **"Too easy: 1 of 2"** (B9). Closing the card brings it back when the count reaches 2 again.
18. History > Progressions: tap a card, the chart shows big dots with numbers (sets left "as planned" count as their target; corrections are applied); with one workout it says "A line appears after 2 sessions" (B7). Correct a number in a workout (more reps than the target) and open the chart again: the dot moved (B8). A level goes up only through the rule (two "Too easy" or the pacing gate), more reps alone do not (B8).
19. In History, tap an exercise name in a workout, or "Open exercise" on a Progression: the exercise detail opens in Progress (D6).

## 6. Progress
20. There is **no "With weights" tab**. Goblet squat sits after the air squat (Squat tab), dumbbell RDL after the bodyweight RDL (Hips and back), weighted pull-up after the pull-up (Pull), weighted dip and push-up in Push, dumbbell row in Row (D7, O3).
21. Planche lean (Push) shows "needs Plank"; tap it: the plank opens (O2).
22. Open an exercise detail: **Edit** sets your level 1-5 by hand (V-up at 3 = 3 stars, level 4 is now); Needs and Unlocks are links, Back returns step by step (D5, D6).

## 7. Settings
23. Settings > backup: export and import still work; saved routines keep their exercises per round and cool-down (the Train switches themselves are not part of the backup, as before).

## Score
Give each tab 1-5 (Train, Preview, Workout, Progress, History, Settings), one thing you like and one thing to fix.
