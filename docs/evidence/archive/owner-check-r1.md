# Owner phone check — Release R1 (bugs + logging), version 0.4.0-r1

Install `Apps/builds/app-debug-R1.apk` over the existing app (same signing key, versionCode 4). Your history, levels and backups are kept (the database moves to version 24 by adding two empty columns). Nothing was tested on a phone by the implementer; the emulator screenshots are in `docs/evidence/ui/R1-review.md`.

1. **Clip changes with the block (K1).** Start a workout. Does the clip change at every block: stretch -> next exercise -> stretch? (Plan R20.)
2. **Logger (R17).** After a work block the stretch screen shows the exercise name, `−`, a big number, `+`, the chips **Too hard · Too easy · Pain** and **Done** below. Does it feel like the work screen? Does the clip, title and timer still fit without overlap on your phone, also with a long stretch name? "Done" folds the logger into one "Logged: N · Change" line; it does not skip the stretch.
3. **Too easy.** Tap it in two workouts in a row for the same exercise: nothing changes by itself (a "harder target suggested" note is only recorded; there is no button for it yet).
4. **End screen (R21).** "Edit what I logged" shows one number per round for every exercise, plus Too hard / Too easy / Pain. Change one round only.
5. **History (R22).** Open a workout: every round has its own number and can be corrected; corrected rounds say "corrected". Check that your Progress stars follow the corrected numbers (a low round corrected to the target should no longer block the level).
6. **Status bar.** In dark mode the bar at the top is dark, not grey.
7. **Backup.** Settings -> Backup -> Export, then check it still imports (the file now carries the round corrections).

Not checked by anyone yet: haptics, sound, voice cues, screen-off behaviour, how it feels in a real workout.
