# Owner phone check — Release R2 (visual system), version 0.5.0-r2

Install `Apps/builds/app-debug-R2.apk` over the existing app (same signing key, versionCode 5). Nothing was tested on a phone by the implementer; emulator screenshots: `docs/evidence/ui/R2-review.md`. Releases R1-R4 are not phone-tested separately (owner decision): this list is for the final test.

1. **New look (doc 17).** Dark by default, gold primary buttons and active tab, fixed colours (no Material You). Settings keeps System / Light / Dark. Is it readable and calm?
2. **Fonts.** The titles and numbers are drawn with the system condensed font for now; Barlow Condensed and IBM Plex Sans arrive in a small follow-up (V08c).
3. **Workout screen (R16).** The clip on white fills the area behind a transparent header (big ROUND 2 OF 4, markers, elapsed time, title, next). The dark bottom sheet has the centred gold timer with its bar, the exercise and set, - number + (during recovery) and Pause · DONE · Skip with DONE in the middle. Readable from 2 m? Does DONE during work end the set and during recovery fold the number?
4. **No chips in the workout.** Too hard / Too easy / Pain are only on the end screen now ("Edit what I logged").
5. **Clips (V06).** Look at: feet-elevated, diamond and archer push-ups (each should look different), the pull-up family (full range, chin over the bar, muscle-up over the bar), the 11 new stretches. The clips have no caption now (the header names the exercise). Are they understandable? Rejected or weak ones are listed in `docs/evidence/clips-v3-review.md` (Full Back Bridge, front levers, wrists).
6. **Stretch library (V07).** Progress -> Stretches shows 18; start a workout with stretch on and see the new ones rotate in.
7. **Body figure and thumbnails (V08, V09).** Preview cards, swap sheet, questionnaire cards, History and the stretch list show the picture of the exercise; Preview cards and the Progress sheet show front and back body figures (gold = main muscles, dim = helpers). Readable at card size?
8. **Questions for you:** is the white frame around clips in dark screens (e.g. the Progress sheet) acceptable? The clips are 4:3, so they are centred, not full-bleed.

Not checked by anyone yet: haptics, sound, voice cues, screen-off behaviour, real-workout feel.
