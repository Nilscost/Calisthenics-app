# Owner phone check — finished version (U13)

Install `Apps/builds/app-debug-U13.apk` over the existing app (same signing key, versionCode 2). Your old workout history, levels and backups are kept; nothing was tested on a phone by the implementer.

## First minutes
1. **Questionnaire (fresh install only; on an update it is skipped).** Settings → "Redo the starting questionnaire" lets you see it. Check: one question per page, "I don't know" gives Easy / Normal / Hard cards that name the exercise and the number, pull-up page offers the band row if you untick the pull-up bar.
2. **Tabs.** Only Train · Progress · History · Settings.
3. **Train.** Goal dropdown, profile chips with "+", rounds stepper next to Reps/Timed, focus, stretch switch, one "Preview workout" button. Is it all on one screen, no scrolling? Do the minutes change with the rounds?
4. **Profiles.** "+" creates a profile (name + equipment checklist). A high bar on a profile unlocks the muscle-up / levers.
5. **Preview.** One card per exercise with muscle chips; swap button (today only, or "Keep for next time"); "Full timeline" opens; sticky Start.

## During a workout
6. 15 s plank: lasts 15 s plus a 3 s get-ready (18 s block), not 45 s.
7. With stretch on you never see or hear "Rest"; the stretch is the recovery; the short block says "Get ready: <next>".
8. After a work block, the stretch screen shows "<exercise> − n + reps", "Too hard", "Pain" at the top. Change the number once; leave another round untouched.
9. Done / Skip / Pause, ⋮ → End workout (asks to confirm). Rotate nothing: the screen is portrait only.
10. End screen: short summary, "Edit what I logged".

## Afterwards
11. **History:** week strip with dots, your workout, tap it → per-round values, correct a number (it saves as a correction).
12. **Progress:** pick Push / Pull / Row / Core …; nodes have icons and stars; locked ones show a lock, ones needing equipment show the item; tap a node for the clip, steps and cautions. The band row now leads to inverted rows.
13. **Settings:** profiles, defaults, voice cues off/on, Light/Dark, backup, privacy, licences.
14. **Clips:** look at a few (push-up, pull-up, squat, inverted row, kettlebell swing, a handstand): is the movement understandable, is only the worked muscle orange? The handstand clips are the weakest.

## Things the implementer could not check (please look)
- How it looks on the phone: spacing, colours (dynamic colour on Android 12+), dark mode, font size 1.3 — only automated checks exist (labels, 48 dp touch areas, contrast of the fallback palette, reachability).
- Real taps in bottom sheets (swap, node details), screen-off/audio behaviour of the workout service, the unfinished-workout recovery.
- Whether the new numbers (all DRAFT) feel right, and the new exercises, especially kettlebell swing and Copenhagen plank.
- The band assumption: ticking "Resistance band" means you can anchor it (door anchor); the band row depends on it.
