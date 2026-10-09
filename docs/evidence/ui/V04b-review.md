# V04b — per-round editors: screenshot review

Run: https://github.com/Nilscost/Calisthenics-app/actions/runs/37937080629 (commit `02146c7`), `results.txt` all 15 flow x variant PASS (5 flows x light/dark/font13); the `connected` job (Room tests on the emulator) is green too. Shots on branch `ui-shots` under `02146c7/`.

| Screen | Variant | Score | Defects |
|---|---|---|---|
| End screen, editor open (`c_session/09-end-editor`) | light, font13 (opened both in run 1bceef1; layout unchanged since) | 4 | one row per round with - number +, "Reps you did", three chips; the summary says "1 rounds" (existing wording) |
| History detail with per-round rows (`e_seeded/02`, `03`) | font13 (02), dark (03) | 4 | long list for exercises with sides (6 rows); every touch target 48 dp; dark contrast fine |

Open: no visual cue for a corrected round besides the small "corrected" note (not in the seeded run, which has no corrections); a later task may show it in `e_seeded` by seeding a correction.
