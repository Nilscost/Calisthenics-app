# Side display: total vs per side (2026-10-26)

Owner request: "6 reps per side" was ambiguous (12 in total, or 6 and 6?) and some per-side exercises were split in two while others were not.

Every exercise now has a side mode (`sides` in the catalog; derived from `unilateral` when absent):

- **NONE**: the target is the total (or the exercise has no sides). Nothing says "per side".
- **SPLIT** (`unilateral: true`): two consecutive work blocks, Left then Right, each with its own target. The Preview row reads "6 reps · left, then right".
- **ALTERNATING**: one block that works both sides in one set. Stored tier and program numbers stay per side (no progression change); every screen shows the total first: "12 reps (6 per side)", holds "40 s (20 s per side)". The voice cue says "12 reps, 6 per side". The logger/end-screen editor labels the number "Reps per side you did" and stores the per-side value as before.

## ALTERNATING (10 exercises)

| id | name now | tier targets (per side) | why |
|---|---|---|---|
| pushup-archer | Archer Push-Up | [3, 4, 6, 8, 10] | name said "(per side)"; instructions "alternate sides, count each side" |
| pushup-single-leg | Single-Leg Push-Up | [8, 9, 10, 11, 12] | program item "10 per leg"; legs alternate each rep |
| bicycle-crunch | Bicycle Crunch (pause at centre) | [8, 9, 10, 11, 12] | Pilates program item written per side; sides alternate within the set |
| pilates-single-leg-stretch | Single-Leg Stretch | [3, 5, 6, 8, 10] | Pilates program item written per side; sides alternate within the set |
| pilates-scissors | Scissors | [3, 5, 6, 8, 10] | Pilates program item written per side; sides alternate within the set |
| pilates-corkscrew | Corkscrew | [3, 5, 6, 8, 10] | Pilates program item written per side; sides alternate within the set |
| pilates-saw | Saw | [3, 5, 6, 8, 10] | Pilates program item written per side; sides alternate within the set |
| pilates-spine-twist | Spine Twist | [3, 5, 6, 8, 10] | Pilates program item written per side; sides alternate within the set |
| pilates-double-leg-kick | Double-Leg Kick | [3, 5, 6, 8, 10] | Pilates program item written per side; sides alternate within the set |
| ring-pushup-rto-archer | Archer Ring Push-Up, Rings Turned Out | [8, 9, 10, 11, 12] | name said "(per side)"; alternates sides |

## Newly SPLIT

| id | name | tier targets (now per leg) | note |
|---|---|---|---|
| lunge-reverse | Reverse Lunge | [5, 6, 7, 8, 10] | owner example. Instructions now say finish one leg, then switch. Army PRT / Physical Demands items: written 10 alternating reps became 5 per leg (same total) |

The 137 exercises that were already `unilateral` are unchanged (SPLIT).

## Decisions

- **Walking lunge: NONE.** Steps alternate legs and each step counts as one rep, so its number is already the total and nothing says "per side". (ALTERNATING would have doubled the displayed volume because tiers would become per side.)
- **Single-leg bridge in the Army programs**: it was one block "30 s, switch legs every 5 s" on a two-sided exercise; the item is now 15 s per leg as two blocks.
- Fixed programs follow the exercise: SPLIT items are two blocks, ALTERNATING items one block lasting twice the per-side estimate, and `perSide` is true exactly for those (a catalog build check and a test enforce it). A `perSide` flag on an unsided exercise is ignored.
- Dead bug, bird dog, mountain climbers, bicycle-style twists, windshield wipers, typewriter pull-up, plank shoulder taps and similar alternating drills were **left NONE**: their written convention ("one rep = both sides", "count both") already makes the target the total, so no per-side number exists to show.
- Hold exercises that "switch legs between holds" (one-leg hollow, one-foot L-sit, one-leg levers, wall handstand one foot) were left NONE: each hold is a separate hold.
- The Progress screen still lists level targets as stored (per side for ALTERNATING exercises).
