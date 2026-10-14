# Owner phone check — what Release R4 would have covered (folded into R5)

R4 (editor, formats, routines: V16–V23) was **not cut as its own release**. Owner decision: the phone test happens once, at the end, so R4 is folded into R5. This list is part of the R5 checklist (`owner-check-r5.md` repeats it). Nothing here was tested on a phone by the implementer; screenshots are in `docs/evidence/ui/V12-V27-review.md`.

1. **Preview editor (V16, V17).** Open Workout preview. Each exercise card has a level stepper (− / +); "+" between cards adds an exercise; long-press removes one; the break between exercises can be a chosen stretch. Swap shows the whole chain of that movement plus other movement types. Turn on **Detailed edit**: every set is listed; change one set's stretch; type a number for one exercise (it is marked "typed · not counted" and never moves your level).
2. **Pairs and Straight sets (V19, V20).** On Train choose Pairs: the workout header reads PAIR 1 · SET 2 OF 3, the two exercises alternate, a rest/stretch follows every set. Straight sets: EXERCISE 2 · SET 1 OF 3. History and the end screen say "Set n". Does a Pairs workout feel right?
3. **Progression rule (V21).** In the preview pick Rep range (e.g. 3 × 5–8; holds 10–30 s) or Custom: targets show ranges; a session where every set reaches the top moves you to the next exercise; sets you did not log count as the same as last time.
4. **Saved routines (V18).** Save as routine (name it); it appears under Routine on Train; Update this routine / Save as new; it survives a backup and restore.
5. **Ready-made routines (V22).** Train → Routine → Recommended Routine (pairs with 90 s rest, core triplet, warm-up) and Minimalist Routine (circuit, no rest between exercises). Do they run like the originals? Is the credit line shown?
6. **Warm-up (V23).** The "Warm-up first" switch on Train adds a few mobility moves before any workout; the time estimate grows.
