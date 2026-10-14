# 23 · New exercises and fixed routines (DRAFT)

The machine-readable contract is `docs/23-new-exercises.json`. This page summarises it. **Everything is DRAFT; nothing here has been reviewed.**

## Counts

- **379 new ids**: batch 1 = 121, batch 2 = 66, batch 3 = 192.
- 48 new families, 5 new tabs, 9 new equipment ids, 114 aliases, 30 option sets, 8 fixed routines, 24 pose groups.
- Batch 1 = missing main-line rungs (Tier M) plus owner-asked items (toes-to-bar, dragon flag, superman plank). Batch 2 = alternate paths and new ladders (Tier A, rings, flag). Batch 3 = categories, accessories, prehab, stretches, Army drill moves and Pilates (Tier C).

| Tab | New ids | Batch 1 | Batch 2 | Batch 3 |
|---|---|---|---|---|
| Push | 29 | 15 | 5 | 9 |
| Shoulders (Handstand) | 21 | 14 | 7 | 0 |
| Pull | 35 | 25 | 7 | 3 |
| Row | 11 | 8 | 1 | 2 |
| Squat | 20 | 10 | 6 | 4 |
| Hips and back | 11 | 6 | 3 | 2 |
| Core | 70 | 37 | 21 | 12 |
| Lower legs | 10 | 6 | 0 | 4 |
| Rings (new) | 16 | 0 | 16 | 0 |
| Power (new) | 30 | 0 | 0 | 30 |
| Conditioning (new) | 35 | 0 | 0 | 35 |
| Prehab (new) | 38 | 0 | 0 | 38 |
| Pilates (new) | 15 | 0 | 0 | 15 |
| Stretches | 18 | 0 | 0 | 18 |
| Warm-up (no tab) | 4 | 0 | 0 | 4 |
| Army drill movements (routine-only) | 16 | 0 | 0 | 16 |

New families (ids per family): `handstand` 14, `arm-balance` 3, `arms` 2, `grip` 8, `step-up` 2, `quad` 4, `balance` 4, `rollout` 5, `bird-dog` 6, `hanging-legraise` 9, `dragon-flag` 6, `crunch` 6, `flag` 16, `plyo` 13, `plyo-lateral` 11, `plyo-box` 6, `conditioning` 20, `agility` 6, `crawl` 9, `prehab-shoulder` 17, `prehab-wrist` 6, `prehab-hip` 6, `prehab-knee` 4, `neck` 5, `army-drill` 16, `twist-hold` 5, `pilates` 15, `ring-support` 3, `ring-dip` 4, `ring-pushup` 6, `ring-mu` 3. Each new stretch and warm-up item is its own one-id family, as in the catalog.

New tabs: **Rings** (ring-support, ring-dip, ring-pushup, ring-mu); **Power** (plyo, plyo-lateral, plyo-box); **Conditioning** (conditioning, agility, crawl); **Prehab** (prehab-shoulder, prehab-wrist, prehab-hip, prehab-knee, neck); **Pilates** (pilates). Existing tabs gain families as listed under `tabsExtended`.

New equipment ids: `rings`, `ab-wheel`, `step`, `box`, `bench`, `parallettes`, `pole`, `sliders`, `jump-rope`.

## Decisions

- One id per progression step. Time targets on the same position (freestanding handstand 15-20 s / 30 s / 60 s) are policy tiers, not ids.
- Grip, hand, stance, width, range, tempo, direction and equipment variants are options, not ids (see Options). Rings versions of existing rows, curls, levers and rollouts are an extra `equipmentAlternatives` entry on the existing id.
- `insertAfter` names the predecessor in the ladder. A few cross families: `handstand-toe-pulls` follows `wall-handstand-hold` (hspu), `grip-uneven-hang` follows `dead-hang` (pullup), `rollout-knees` follows `plank-long-lever` (plank). `alsoRequires` holds extra gates (e.g. `pullup-l-sit` needs `l-sit-floor`; FL rows need the matching FL hold).
- ADD-WITH-CAUTION items from the new notes are included with `addWithCaution: true`; high-risk items carry `cautionStrong: true`.
- Army drill moves that exist only for drills are `routineOnly: true` in family `army-drill` (no tab).
- Plyo and power push-up items follow EC and Chu: sets of 1-3 reps, PARC-style self-check, no rep targets that reward fatigue. Depth jumps are gated by an app rule (not Chu's): clean 45-60 cm box jumps plus a stable single-leg squat.
- Pilates: mat only. Classical names that match existing or new ids are aliases; 15 genuinely new mat exercises in family `pilates`, instructions in the author's own words.
- `sourceRefs` are short book codes with printed pages (OG2, CC1/CC2, EC = PDF pages, SW, ATP, CHU, BTGB, SH; BB/WOD/HCL are corroboration only). `sourceIds` map them to catalog source ids; new ids are in `sourcesNew`.
- Patterns, muscles and positions use only the existing enums. Plyo moves use SQUAT/LUNGE/CALF_RAISE; jacks, skips and drills use MOBILITY; neck work uses UPPER_BACK as a stand-in (see open questions).

## Exclusions

- **Tier D (all)**: one-arm leg raise, rollover, meathook, V-sit angle steps, manna drills, flutter kicks, explosive/clap/kipping-plus pull-ups beyond the kipping on-ramp, headbanger and behind-the-neck pull-ups, FL pull-ups, rope climbing (no `climb-rope` id), weighted/wushu/dragon/rail pistols, OAHS route and handstand walking, bent-arm press L6-10, crane/side crow, tiger and other exotic push-ups, wall PPPU, one-arm dip, one-arm elbow lever, scorpion planche, advanced flag variants, back-lever pullouts, gecko/rotation bridges, kip-up, depth-jump variants beyond the first rung.
- **Deliberate skips**: flips and handsprings; rings elite (cross, Maltese, felge, kips, rings planche); one-arm handstand, full manna, one-arm front lever and planche, one-arm HSPU; neck bridges; P3T pregnancy drills.
- **EC SKIP**: suicide and reverse suicide jump, wall threads, hip-strike/convict/half-super/full-body/superman push-ups, round-the-clock and sideways pop-ups, push get-up, Kojaks, kip-up steps 4-10 and sitting/prone kips, the front- and back-flip chains, pull-up hop, clap pull-up, explosive dips.
- **EC CAUTION but left out**: Thread Jump (the note itself says low value, could skip).
- **Chu SKIP / optional**: all medicine-ball and partner drills, loaded drills (5-5-5 squat jump, wave squat), rim and hoop drills, pike jumps, handstand and 360 depth jumps, triple jumps (need a pit), multi-box and pyramid box drills, hurdle-height series, combination bounding, Munoz/Krumrie/eight-square patterns, the incline push-up depth jump.
- **BtGB NICHE/SKIP**: manna rungs, L-sit lift, advanced L-sit, straddle-L one-hand-centre and partial roll, Youngs, hanging V-to-L and single-leg HLL, archups, German-hang pulls, inverted chin-ups, chest rolls, deck squats, weighted HLL, RLL variants, side-lever pulls, cranks, Bowers/Erbs/Galimores, Bulgarian dips, GHR.
- **Merged instead of new ids**: ring rows, ring curls/triceps, ring face pull, ring rollouts (equipment alternatives); close/wide/neutral grips; half push-up and top-half dip (range options); legs-forward dip; band wall slides; overhead pull-apart; soleus calf stretch; side-lying thigh stretch; Army Forward Lunge (option on `walking-lunge`).
- **Not modelled**: planche bent-arm/open straddle (form unclear after the BtGB check); OG 'side seal' stretch (meaning unclear); hip gates; Army CD1-3, CL1-2, GD, MMD, STC and PMCS as routines (most entries now exist); Soldier Carry (partner).

## New ids by tab

### Push (29)

| id | name | after | equipment | batch | sources |
|---|---|---|---|---|---|
| `dip-bench-bent-knee` | Bench Dip (knees bent) | — | chair | 1 | SW p. 50; EC dips step 1; SH p. 103 |
| `dip-bench-straight-leg` | Bench Dip (legs straight) | `dip-bench-bent-knee` | chair | 1 | EC dips step 2; SW p. 50 |
| `dip-bench-feet-elevated` | Bench Dip (feet on a second chair) | `dip-bench-straight-leg` | chair | 1 | EC dips step 3; WOD p. 59 |
| `dip-feet-supported` | Feet-Supported Dip (parallel bars) | `dip-bench-feet-elevated` | dip-support+chair | 1 | EC dips steps 4-6 |
| `dip-jumping` | Jumping Dip | `dip-support-hold` | dip-support | 1 | OG2 p. 505 |
| `dip-straight-bar` | Straight-Bar Dip | `dip-parallel` | low-bar | 1 | SW p. 53; SH p. 107; EC dips step 10; BTGB p. 71 |
| `dip-l-sit` | L-Sit Dip | `dip-parallel` | dip-support | 1 | OG2 p. 508; BTGB p. 70 |
| `dip-forward-lean` | 45-Degree Lean Dip | `dip-l-sit` | dip-support | 1 | OG2 p. 509 |
| `dip-russian` | Russian Dip (!) | `dip-parallel` | dip-support | 2 | SW p. 55; BTGB p. 70 |
| `dip-korean` | Korean Dip (behind the back) (!) | `dip-straight-bar` | low-bar | 2 | SW p. 56; BTGB p. 72 |
| `planche-frog-stand` | Frog Stand | — | none | 1 | OG2 p. 458; SW p. 172; BTGB p. 50 |
| `planche-frog-stand-straight-arm` | Straight-Arm Frog Stand | `planche-frog-stand` | none | 1 | OG2 p. 459; BTGB p. 50 |
| `planche-tuck-pushup` | Tuck Planche Push-Up | `planche-tuck` | none | 2 | OG2 pp. 470-472 |
| `planche-straddle` | Straddle Planche (!) | `planche-adv-tuck` | none | 2 | OG2 pp. 474-476; BTGB p. 52; SW p. 212 |
| `planche-full` | Full Planche (!) | `planche-straddle` | none | 2 | OG2 pp. 480-482; BTGB p. 54 |
| `pushup-wall-push-away` | One-Arm Wall Push-Away (per side) | `pushup-wall` | wall | 3 | EC PDF pp. 260-262 |
| `pushup-pop-up-incline` | Incline Pop-Up | `pushup-standard` | none | 3 | EC ch. 5 step 1 (PDF p. 110) |
| `pushup-single-leg` | Single-Leg Push-Up | `pushup-standard` | none | 3 | ATP p. 5-4; SW p. 34 |
| `pushup-kneeling-push-off` | Kneeling Push-Off (!) | `pushup-pop-up-incline` | mat | 3 | EC ch. 5 step 2 |
| `pushup-pop-up` | Pop-Up Push-Up | `pushup-kneeling-push-off` | none | 3 | EC ch. 5 step 3; SW p. 42 |
| `pushup-hindu` | Hindu Push-Up (dive-bomber) | `pushup-standard` | none | 3 | SW p. 29 |
| `pushup-pop-up-wide-close` | Wide-to-Close Pop-Up | `pushup-pop-up` | none | 3 | EC PDF pp. 97-99 |
| `pushup-clap` | Clap Push-Up (!) | `pushup-pop-up` | none | 3 | EC ch. 5 step 4; OG2 pp. 497-498; SH p. 80 |
| `pushup-uneven` | Uneven Push-Up (per side) | `pushup-diamond` | none | 1 | CC1 push-up step 7; SW p. 39 |
| `pushup-chest-strike` | Chest-Strike Push-Up (!) | `pushup-clap` | none | 3 | EC ch. 5 step 5; OG2 p. 498 |
| `pushup-pseudo-planche` | Pseudo Planche Push-Up (feet raised) | `pushup-diamond` | chair or box | 1 | OG2 pp. 462-463; SH p. 76 |
| `pushup-half-one-arm` | Half One-Arm Push-Up (per side) | `pushup-archer` | none | 1 | CC1 push-up step 8 |
| `pushup-one-arm-elevated` | Hands-Elevated One-Arm Push-Up (per side) | `pushup-half-one-arm` | none | 1 | OG2 pp. 500-501 |
| `pushup-one-arm-straight-body` | One-Arm Push-Up, Feet Together (per side) | `pushup-one-arm` | none | 1 | OG2 pp. 502-503 |

### Shoulders (Handstand) (21)

| id | name | after | equipment | batch | sources |
|---|---|---|---|---|---|
| `elbow-lever-umbrella` | Closed Umbrella (elbow lever prep) | — | none | 2 | OG2 p. 539; SW p. 199 |
| `elbow-lever-straddle` | Straddle Elbow Lever | `elbow-lever-umbrella` | none | 2 | SW p. 200 |
| `elbow-lever` | Elbow Lever | `elbow-lever-straddle` | none | 2 | OG2 p. 539; SW p. 201 |
| `headstand-tripod-tuck` | Tripod Tuck (knees on elbows) (!) | — | mat | 1 | SW p. 179 |
| `handstand-wall-plank` | Wall Plank (feet on the wall) | — | wall | 1 | OG2 p. 321; SH p. 99 |
| `headstand-wall` | Wall Headstand (!) | `headstand-tripod-tuck` | wall+mat | 1 | CC1 HSPU step 1; SW p. 180 |
| `headstand-tripod` | Tripod Headstand (!) | `headstand-wall` | mat | 1 | SW pp. 180-181 |
| `handstand-wall-walk-up` | Wall Walk-Up (toes only) | `handstand-wall-plank` | wall | 1 | OG2 p. 321; SH p. 148 |
| `headstand-forearm` | Forearm Headstand (!) | `headstand-tripod` | mat | 1 | SW p. 181 |
| `handstand-toe-pulls` | Wall Handstand Toe Pulls | `wall-handstand-hold` | wall | 1 | OG2 pp. 322-323 |
| `handstand-wall-one-foot` | Wall Handstand, One Foot Off | `handstand-toe-pulls` | wall | 1 | OG2 p. 323 |
| `handstand-kick-up` | Kick-Up to Handstand with Roll-Out Bail (!) | `handstand-wall-one-foot` | mat | 1 | OG2 pp. 322-324; SW p. 186 |
| `handstand-free` | Freestanding Handstand | `handstand-kick-up` | mat | 1 | OG2 p. 326; SW p. 190; SH p. 152 |
| `handstand-press-wall-negative` | Wall Straddle-Press Negative | `handstand-free` | wall | 2 | OG2 p. 369 |
| `handstand-press-bent-arm` | Bent-Arm Press to Handstand | `handstand-free` | none | 2 | OG2 p. 353 |
| `handstand-press-elevated` | Elevated Straddle Press | `handstand-press-wall-negative` | box | 2 | OG2 p. 371 |
| `handstand-press-straddle` | Straddle Press to Handstand | `handstand-press-elevated` | none | 2 | OG2 pp. 372-373 |
| `pike-pushup-hands-elevated` | Hands-Elevated Pike Push-Up | — | chair | 1 | SW p. 46; BB pp. 73-74 |
| `hspu-wall-deficit` | Deficit Wall Handstand Push-Up (!) | `hspu-wall` | wall+parallettes or wall+mat | 1 | OG2 p. 345; SW p. 49 |
| `hspu-free-head` | Freestanding Headstand Push-Up (!) | `hspu-wall-deficit` | mat | 1 | OG2 p. 346 |
| `hspu-free` | Freestanding Handstand Push-Up (parallettes) (!) | `hspu-free-head` | parallettes | 1 | OG2 pp. 347-348; SH pp. 163-168 |

### Pull (35)

| id | name | after | equipment | batch | sources |
|---|---|---|---|---|---|
| `skin-the-cat` | Skin the Cat (!) | `german-hang` | pullup-bar+high-bar or rings | 1 | OG2 p. 392 |
| `back-lever-tuck` | Tuck Back Lever | `skin-the-cat` | pullup-bar+high-bar or rings | 1 | OG2 p. 393; BTGB p. 41; SW p. 224 |
| `back-lever-adv-tuck` | Advanced Tuck Back Lever (flat back) | `back-lever-tuck` | pullup-bar+high-bar or rings | 1 | OG2 p. 394; BTGB p. 42 |
| `back-lever-straddle` | Straddle Back Lever | `back-lever-adv-tuck` | pullup-bar+high-bar or rings | 1 | OG2 p. 395; BTGB p. 42; SW p. 226 |
| `back-lever-one-leg` | Half-Lay / One-Leg Back Lever | `back-lever-straddle` | pullup-bar+high-bar or rings | 1 | OG2 p. 396; BTGB p. 43 |
| `back-lever` | Full Back Lever (!) | `back-lever-one-leg` | pullup-bar+high-bar or rings | 1 | OG2 p. 397; BTGB p. 44; SW p. 228 |
| `grip-eagle-claw` | Eagle Claws | — | none | 3 | CC2 pp. 52-53 |
| `grip-horizontal-hang` | Horizontal Hang (table edge) | — | low-bar | 1 | CC2 pp. 20-21 |
| `grip-uneven-hang` | Uneven Hang (per side) | `dead-hang` | pullup-bar | 2 | CC2 pp. 22-23 |
| `grip-one-arm-hang` | One-Arm Hang (per side) | `grip-uneven-hang` | pullup-bar | 1 | CC2 pp. 24-27; OG2 p. 431; SW pp. 84-85 |
| `grip-towel-hang` | Towel Hang | `grip-one-arm-hang` | pullup-bar | 2 | CC2 pp. 28-29 |
| `grip-twin-towel-hang` | Twin Towel Hang | `grip-towel-hang` | pullup-bar | 2 | CC2 pp. 30-31 |
| `grip-uneven-towel-hang` | Uneven Towel Hang (per side) | `grip-twin-towel-hang` | pullup-bar | 2 | CC2 pp. 32-33 |
| `grip-one-arm-towel-hang` | One-Arm Towel Hang (per side) | `grip-uneven-towel-hang` | pullup-bar | 2 | CC2 pp. 34-35 |
| `front-lever-one-leg` | One-Leg Front Lever | `front-lever-straddle` | pullup-bar+high-bar or rings | 1 | OG2 p. 405; SW p. 236 |
| `front-lever` | Full Front Lever (!) | `front-lever-one-leg` | pullup-bar+high-bar or rings | 1 | OG2 p. 406; SW p. 238; BTGB p. 48 |
| `pullup-kip-swing` | Kip Swing | `dead-hang` | pullup-bar | 3 | EC ch. 9 step 1 (PDF p. 272) |
| `pullup-false-grip-hang` | False-Grip Hang | `dead-hang` | pullup-bar | 1 | OG2 pp. 523-524 |
| `pullup-jumping` | Jumping Pull-Up | `scapular-pull` | pullup-bar | 1 | OG2 p. 423; EC ch. 9 step 2 |
| `pullup-partial` | Top-Half Pull-Up | `pullup-negative` | pullup-bar | 1 | CC1 pull-up step 4 |
| `pullup-commando` | Commando Pull-Up (alternating grip) | `pullup-full` | pullup-bar | 3 | ATP p. 6-5; SW p. 78 |
| `pullup-kipping` | Kipping Pull-Up (!) | `pullup-full` | pullup-bar | 2 | EC ch. 9 step 3; OG2 p. 437 |
| `pullup-pullover-jumping` | Jumping Pullover | `pullup-full` | low-bar or pullup-bar | 1 | EC ch. 9 step 8 |
| `pullup-pullover` | Bar Pullover | `pullup-pullover-jumping` | pullup-bar | 1 | OG2 p. 427; EC ch. 9 step 9; BTGB p. 106 |
| `pullup-l-sit` | L-Sit Pull-Up | `pullup-chest-to-bar` | pullup-bar | 1 | OG2 p. 426; SW p. 82; SH p. 128; BTGB p. 98 |
| `pullup-sternum` | Sternum Pull-Up (extended range) | `pullup-chest-to-bar` | pullup-bar | 1 | OG2 p. 525; EC ch. 9 step 7 |
| `muscle-up-negative` | Muscle-Up Negative | `pullup-sternum` | pullup-bar+high-bar | 1 | OG2 p. 526 |
| `muscle-up-kipping` | Kipping Muscle-Up (practice) (!) | `muscle-up-negative` | pullup-bar+high-bar | 1 | OG2 p. 527; SW p. 221 |
| `pullup-archer` | Archer Pull-Up (per side) | `pullup-chest-to-bar` | pullup-bar | 1 | SW p. 83; SH p. 130; OG2 pp. 428-430 |
| `pullup-typewriter` | Typewriter Pull-Up | `pullup-archer` | pullup-bar | 2 | SH p. 133 |
| `pullup-uneven` | Uneven Pull-Up (towel, per side) | `pullup-archer` | pullup-bar | 1 | CC1 pull-up step 7; SW p. 84 |
| `pullup-one-arm-flex-hang` | One-Arm Flexed Hang (per side) | `pullup-uneven` | pullup-bar | 1 | OG2 p. 431 |
| `pullup-one-arm-assisted` | Assisted One-Arm Pull-Up (per side) (!) | `pullup-one-arm-flex-hang` | pullup-bar | 1 | OG2 p. 432; SW p. 86 |
| `pullup-one-arm-negative` | One-Arm Pull-Up Negative (per side) (!) | `pullup-one-arm-assisted` | pullup-bar | 1 | OG2 p. 433 |
| `pullup-one-arm` | One-Arm Pull-Up (per side) (!) | `pullup-one-arm-negative` | pullup-bar | 1 | OG2 p. 434; SW pp. 87-88; CC1 pull-up step 10 |

### Row (11)

| id | name | after | equipment | batch | sources |
|---|---|---|---|---|---|
| `arms-biceps-curl` | Bodyweight Biceps Curl | — | low-bar or rings | 3 | OG2 p. 139 |
| `arms-triceps-extension` | Bodyweight Triceps Extension | — | low-bar or rings | 3 | BWF |
| `front-lever-row-tuck` | Tuck Front Lever Row | `archer-row` | pullup-bar+high-bar or rings | 1 | OG2 p. 410; BTGB p. 92 |
| `row-archer-arm-in` | Archer Row, Straight Arm In | `archer-row` | low-bar or rings | 1 | OG2 p. 420 |
| `front-lever-row-adv-tuck` | Advanced Tuck Front Lever Row | `front-lever-row-tuck` | pullup-bar+high-bar or rings | 1 | OG2 p. 411; BTGB p. 93 |
| `row-one-arm-straddle` | Straddle One-Arm Row (per side) | `row-archer-arm-in` | low-bar or rings | 1 | OG2 p. 421 |
| `ice-cream-maker` | Ice Cream Maker | `front-lever-row-adv-tuck` | pullup-bar+high-bar or rings | 2 | OG2 p. 410 |
| `row-one-arm` | One-Arm Row (per side) | `row-one-arm-straddle` | low-bar or rings | 1 | OG2 p. 422; SW p. 68 |
| `front-lever-row-straddle` | Straddle Front Lever Row | `front-lever-row-adv-tuck` | pullup-bar+high-bar or rings | 1 | OG2 p. 412; BTGB p. 93 |
| `front-lever-row-hang` | Hang to Front Lever Row | `front-lever-row-straddle` | pullup-bar+high-bar or rings | 1 | OG2 p. 413 |
| `front-lever-row` | Full Front Lever Row (!) | `front-lever-row-hang` | pullup-bar+high-bar or rings | 1 | OG2 p. 414; BTGB p. 94 |

### Squat (20)

| id | name | after | equipment | batch | sources |
|---|---|---|---|---|---|
| `lunge-reverse` | Reverse Lunge | — | none | 3 | ATP p. 3-2; SH p. 294 |
| `lunge-lateral` | Lateral Lunge (per side) | `lunge-reverse` | none | 3 | ATP p. 16-4; SH p. 294 |
| `reverse-nordic` | Reverse Nordic | — | mat | 2 | BWF |
| `sissy-squat-assisted` | Assisted Sissy Squat | — | none | 2 | BWF |
| `sissy-squat` | Sissy Squat | `sissy-squat-assisted` | none | 2 | BWF |
| `natural-leg-extension` | Natural Leg Extension | `sissy-squat` | foot-anchor | 2 | BWF |
| `squat-assisted` | Assisted Squat (holding a door frame) | — | none | 1 | SW pp. 92-93; CC1 squat step 3 |
| `squat-box` | Box Squat (sit and stand) | `squat-assisted` | chair | 1 | SW p. 93 |
| `squat-half` | Half Squat | `squat-assisted` | none | 1 | CC1 squat step 4 |
| `squat-wall-sit` | Wall Sit | `squat-air` | wall | 3 | BWF |
| `squat-horse-stance` | Horse Stance | `squat-air` | none | 3 | BWF |
| `squat-close` | Close Squat | `squat-air` | none | 1 | CC1 squat step 6 |
| `squat-uneven` | Uneven Squat (per side) | `squat-close` | none | 1 | CC1 squat step 7 |
| `squat-cossack` | Cossack Squat (per side) | `squat-air` | none | 1 | OG2 p. 572; SW p. 104 |
| `squat-pistol-negative` | Pistol Squat Negative (per side) | `squat-pistol-assisted` | none | 1 | BTGB p. 159 |
| `squat-pistol-elevated` | Elevated Pistol (on a step, per side) | `squat-pistol-box` | step or box | 1 | SW p. 112; OG2 pp. 573-574 |
| `squat-shrimp-two-hand` | Two-Hand Shrimp Squat (per side) | `squat-shrimp-advanced` | none | 1 | SW p. 122 |
| `squat-shrimp-deficit` | Deficit (Jumbo) Shrimp Squat (per side) | `squat-shrimp-two-hand` | step or box | 1 | SW p. 122 |
| `step-up` | Step-Up (stair) | — | step | 2 | OG2 p. 576; SW p. 107; SH p. 319 |
| `step-up-deep` | Deep Step-Up (chair height) | `step-up` | chair or box | 2 | OG2 pp. 577-578 |

### Hips and back (11)

| id | name | after | equipment | batch | sources |
|---|---|---|---|---|---|
| `bridge-reverse-plank` | Reverse Plank (straight bridge) | `glute-bridge` | none | 3 | CC2 p. 193; SW p. 154; OG2 p. 81 |
| `hip-thrust` | Shoulder-Elevated Hip Thrust | `glute-bridge` | bench or chair | 2 | HCL pp. 61, 71 |
| `hip-thrust-single-leg` | Single-Leg Hip Thrust (per side) | `hip-thrust` | bench or chair | 2 | HCL p. 71 |
| `bridge-half` | Half Bridge | `bridge-head` | none | 1 | CC1 bridge step 5 |
| `bridge-wall-walkup` | Wall Walk-Up Bridge | `bridge-wall-walkdown` | wall | 1 | CC1 bridge step 8 |
| `bridge-wall-push-off` | Wall Bridge Push-Off (!) | `bridge-wall-walkup` | wall | 3 | EC PDF pp. 181-184 |
| `bridge-closing` | Closing Bridge (!) | `bridge-wall-walkup` | mat | 1 | CC1 bridge step 9; SW p. 166 |
| `bridge-stand-to-stand` | Stand-to-Stand Bridge (!) | `bridge-closing` | mat | 1 | CC1 bridge step 10; SW p. 166 |
| `harop-curl-beginner` | Harop Curl, Beginner | `nordic-banded` | foot-anchor | 1 | BWF |
| `harop-curl` | Harop Curl, Intermediate | `nordic-curl` | foot-anchor | 1 | BWF |
| `back-extension-floor` | Floor Back Extension | `superman-hold` | none | 2 | OG2 p. 111; BB pp. 41-43 |

### Core (70)

| id | name | after | equipment | batch | sources |
|---|---|---|---|---|---|
| `bird-dog-quadruped` | Quadruped Hold | — | none | 1 | NE; BWF |
| `bird-dog-arm-raise` | Quadruped Arm Raise | `bird-dog-quadruped` | none | 1 | NE |
| `bird-dog-leg-raise` | Quadruped Leg Raise | `bird-dog-arm-raise` | none | 1 | NE |
| `bird-dog` | Bird Dog | `bird-dog-leg-raise` | none | 1 | ATP p. 4-11; OG2 p. 278; BWF |
| `swimmer` | Swimmer (prone) | `bird-dog` | none | 1 | ATP p. 5-11; OG2 p. 278 |
| `quadraplex` | Quadraplex (per side) | `bird-dog` | none | 1 | ATP p. 4-11 |
| `curl-up-mcgill` | McGill Curl-Up | — | none | 3 | BWF |
| `crunch` | Crunch | `curl-up-mcgill` | none | 3 | WOD |
| `bicycle-crunch` | Bicycle Crunch (pause at centre) | `crunch` | none | 3 | ATP p. 5-10; SW |
| `sit-up` | Sit-Up | `crunch` | none | 3 | ATP p. 3-4; SH |
| `rower-situp` | Rower (sit-up to tuck) | `sit-up` | none | 3 | ATP p. 3-4 |
| `russian-twist` | Russian Twist | `sit-up` | none | 3 | SH p. 241 |
| `dead-bug-brace` | Supine Bracing (feet down) | — | none | 1 | ACE; BWF |
| `dead-bug-90-90` | 90/90 Hold | `dead-bug-brace` | none | 1 | ACE |
| `bent-leg-raise-army` | Bent-Leg Raise (Army 4C) | `dead-bug-90-90` | none | 1 | ATP p. 4-9 |
| `dead-bug-single-leg` | Single-Leg Extension Dead Bug | `bent-leg-raise-army` | none | 1 | ACE; P1 |
| `dragon-flag-candlestick` | Candlestick Hold | — | bench or foot-anchor | 1 | OG2 p. 81 |
| `dragon-flag-tuck` | Tuck Dragon Flag | `dragon-flag-candlestick` | bench or foot-anchor | 1 | SW p. 136; SH p. 256 |
| `dragon-flag-adv-tuck` | Advanced Tuck Dragon Flag | `dragon-flag-tuck` | bench or foot-anchor | 1 | SH p. 258 |
| `dragon-flag-one-leg` | One-Leg Dragon Flag | `dragon-flag-adv-tuck` | bench or foot-anchor | 1 | SH p. 260 |
| `dragon-flag-negative` | Dragon Flag Negative | `dragon-flag-one-leg` | bench or foot-anchor | 1 | SH p. 262 |
| `dragon-flag` | Dragon Flag | `dragon-flag-negative` | bench or foot-anchor | 1 | SW p. 136; SH p. 264; BTGB p. 147 |
| `flag-clutch-hang` | Clutch Hang | — | pole | 2 | CC2 p. 78 |
| `flag-support-press` | Support Press | — | pole | 2 | CC2 p. 100 |
| `flag-clutch-diagonal-split` | Diagonal Split Clutch | `flag-clutch-hang` | pole | 2 | CC2 p. 79 |
| `flag-press-hang` | Press Hang | `flag-support-press` | pole | 2 | CC2 p. 101 |
| `flag-clutch-diagonal-tuck` | Diagonal Tuck Clutch | `flag-clutch-diagonal-split` | pole | 2 | CC2 p. 80 |
| `flag-kick-press` | Kick Press | `flag-press-hang` | pole | 2 | CC2 p. 102 |
| `flag-clutch-diagonal` | Diagonal Clutch | `flag-clutch-diagonal-tuck` | pole | 2 | CC2 p. 81 |
| `flag-vertical-chamber-press` | Vertical Chamber Press | `flag-kick-press` | pole | 2 | CC2 p. 103 |
| `flag-clutch-horizontal-tuck` | Horizontal Tuck Clutch | `flag-clutch-diagonal` | pole | 2 | CC2 p. 82 |
| `flag-vertical-press` | Vertical Press | `flag-vertical-chamber-press` | pole | 2 | CC2 p. 104 |
| `flag-clutch-horizontal-split` | Horizontal Split Clutch | `flag-clutch-horizontal-tuck` | pole | 2 | CC2 p. 83 |
| `flag-clutch-bent-leg` | Bent Leg Clutch Flag | `flag-clutch-horizontal-split` | pole | 2 | CC2 p. 84 |
| `flag-split-press` | Split Press Flag | `flag-vertical-press` | pole | 2 | CC2 p. 105 |
| `flag-clutch` | Clutch Flag | `flag-clutch-bent-leg` | pole | 2 | CC2 p. 85 |
| `flag-bent-leg-press` | Bent Leg Press Flag | `flag-split-press` | pole | 2 | CC2 p. 106 |
| `flag-press` | Press Flag | `flag-bent-leg-press` | pole | 2 | CC2 p. 107; OG2 p. 544; SW pp. 245-268 |
| `knee-raise-supported` | Supported Knee Raise (parallel bars) | — | dip-support or chair | 1 | SW p. 138 |
| `hanging-knee-raise` | Hanging Knee Raise | `knee-raise-supported` | pullup-bar | 1 | SW p. 139; ATP p. 6-4; SH p. 249 |
| `hanging-knee-raise-twisting` | Twisting Hanging Knee Raise | `hanging-knee-raise` | pullup-bar | 1 | SW p. 139 |
| `heel-hook` | Heel Hook | `hanging-knee-raise` | pullup-bar | 3 | ATP p. 6-2 |
| `hanging-leg-raise-bent` | Hanging Bent-Leg Raise | `hanging-knee-raise-twisting` | pullup-bar | 1 | SW p. 140; CC1 leg-raise step 9 |
| `hanging-leg-raise` | Hanging Leg Raise | `hanging-leg-raise-bent` | pullup-bar | 1 | SW p. 140; BTGB p. 128; CC1 leg-raise step 10 |
| `toes-to-bar` | Toes-to-Bar | `hanging-leg-raise` | pullup-bar | 1 | SW p. 141; BTGB p. 130; SH p. 251 |
| `hanging-windshield-wiper-half` | Hanging Half Windshield Wiper | `toes-to-bar` | pullup-bar | 2 | SW p. 144; BTGB p. 139; SH p. 253 |
| `hanging-windshield-wiper` | Hanging Windshield Wiper | `hanging-windshield-wiper-half` | pullup-bar | 2 | BTGB p. 141; SW p. 144 |
| `lying-knee-tuck` | Lying Knee Tuck | — | none | 1 | SW p. 126; CC1 leg-raise step 1 |
| `lying-bent-knee-raise` | Lying Bent-Knee Leg Raise | `lying-knee-tuck` | none | 1 | SW p. 127; CC1 leg-raise step 3 |
| `seated-knee-raise` | Seated Knee Raise | `lying-bent-knee-raise` | none | 1 | SW p. 128 |
| `frog-raise` | Lying Frog Raise | `seated-knee-raise` | none | 1 | CC1 leg-raise step 4; SW p. 129 |
| `reverse-crunch` | Reverse Crunch | `lying-bent-knee-raise` | none | 1 | SW p. 130; BB p. 95 |
| `windshield-wiper-bent-knee` | Bent-Knee Floor Wiper | — | none | 2 | ATP p. 3-8 |
| `v-up-tuck` | Tuck V-Up | `leg-raise-lying` | none | 1 | BTGB p. 123 |
| `v-up-straddle` | Straddle V-Up | `v-up-tuck` | none | 1 | BTGB p. 124 |
| `windshield-wiper-floor` | Straight-Leg Floor Wiper | `windshield-wiper-bent-knee` | none | 2 | SW p. 129; BTGB p. 139 |
| `plank-long-lever` | Long-Lever (RKC) Plank | `plank` | none | 1 | OG2 p. 545; SH p. 238 |
| `plank-contralateral` | Contralateral Plank (Superman plank, per side) | `plank` | none | 1 | OG2 p. 545; WOD p. 69 |
| `plank-arm-leg-reach` | Plank with Arm-Leg Reach | `plank-contralateral` | none | 3 | WOD p. 69 |
| `body-saw` | Body Saw | `plank-long-lever` | sliders | 3 | McGill 2014 (prior report) |
| `l-sit-compression-lift` | Seated Compression Lifts | `l-sit-floor` | none | 1 | OG2 p. 298; OG2 p. 111 |
| `l-sit-straddle` | Straddle L-Sit | `l-sit-compression-lift` | none | 1 | OG2 pp. 382-383; BTGB p. 32 |
| `rollout-knees` | Kneeling Ab-Wheel Rollout | `plank-long-lever` | ab-wheel or rings | 1 | OG2 pp. 545-546; BB pp. 101-103 |
| `rollout-ramp` | Ramp or Wall-Stop Rollout | `rollout-knees` | wall+ab-wheel or rings | 1 | OG2 p. 546 |
| `rollout-negative` | Standing Rollout Negative | `rollout-ramp` | ab-wheel+mat or rings | 1 | OG2 p. 547 |
| `rollout-standing` | Standing Ab-Wheel Rollout | `rollout-negative` | ab-wheel or rings | 1 | OG2 p. 547 |
| `rollout-one-arm` | One-Arm Rollout (per side) (!) | `rollout-standing` | ab-wheel | 2 | OG2 p. 548 |
| `side-plank-knee` | Knee Side Plank (per side) | — | none | 3 | SB; KAV |
| `side-plank-hip-raise` | Side Plank Hip Raise (per side) | `side-plank` | none | 3 | KAV |
| `copenhagen-adduction` | Copenhagen Adduction Raise (per side) | `copenhagen-side-plank` | chair | 3 | Haroy 2019 (BJSM) |

### Lower legs (10)

| id | name | after | equipment | batch | sources |
|---|---|---|---|---|---|
| `balance-single-leg` | Single-Leg Stand (per side) | — | none | 3 | SW p. 106 |
| `short-foot` | Short Foot | — | none | 3 | BWF |
| `balance-single-leg-eyes-closed` | Single-Leg Stand, Eyes Closed (per side) | `balance-single-leg` | none | 3 | SW p. 106 |
| `balance-single-leg-cushion` | Single-Leg Stand on a Cushion (per side) | `balance-single-leg-eyes-closed` | none | 3 | BWF |
| `calf-raise-bent-knee` | Bent-Knee Calf Raise | — | none | 1 | CC2 p. 136 |
| `calf-raise-single-bent-knee` | Single-Leg Bent-Knee Calf Raise (per side) | `calf-raise` | none | 1 | CC2 p. 137 |
| `calf-raise-step-bent-knee` | Bent-Knee Calf Raise off a Step | `calf-raise-single` | step or box | 1 | CC2 pp. 138-139 |
| `calf-raise-step` | Calf Raise off a Step | `calf-raise-step-bent-knee` | step or box | 1 | CC2 p. 139 |
| `calf-raise-step-single-bent-knee` | Single-Leg Bent-Knee Calf Raise off a Step (per side) | `calf-raise-step` | step or box | 1 | CC2 pp. 139-140 |
| `calf-raise-step-single` | Single-Leg Calf Raise off a Step (per side) | `calf-raise-step-single-bent-knee` | step or box | 1 | CC2 p. 140; SH p. 280 |

### Rings (new) (16)

| id | name | after | equipment | batch | sources |
|---|---|---|---|---|---|
| `ring-dip-negative` | Ring Dip Negative | — | rings | 2 | OG2 p. 514 |
| `ring-dip` | Ring Dip | `ring-dip-negative` | rings | 2 | OG2 pp. 514-515; BTGB p. 74 |
| `ring-dip-l` | L-Sit Ring Dip | `ring-dip` | rings | 2 | OG2 p. 516 |
| `ring-dip-rto` | Ring Dip, Rings Turned Out | `ring-dip-l` | rings | 2 | OG2 pp. 517-519 |
| `ring-false-grip-hang` | Ring False-Grip Hang | — | rings | 2 | OG2 pp. 523-524 |
| `ring-muscle-up-negative` | Ring Muscle-Up Negative | `ring-false-grip-hang` | rings | 2 | OG2 p. 526; BTGB p. 113 |
| `ring-muscle-up` | Ring Muscle-Up | `ring-muscle-up-negative` | rings | 2 | OG2 pp. 528-529; BTGB p. 113 |
| `ring-pushup-wide` | Wide Ring Push-Up | — | rings | 2 | OG2 p. 486; BTGB p. 62 |
| `ring-pushup` | Ring Push-Up | `ring-pushup-wide` | rings | 2 | OG2 p. 487; BTGB p. 62 |
| `ring-pushup-rto` | Ring Push-Up, Rings Turned Out | `ring-pushup` | rings | 2 | OG2 p. 488 |
| `ring-pushup-rto-archer` | Archer Ring Push-Up, Rings Turned Out (per side) | `ring-pushup-rto` | rings | 2 | OG2 p. 489 |
| `ring-pppu-40` | Ring Pseudo Planche Push-Up, 40 Degrees | `ring-pushup-rto-archer` | rings | 2 | OG2 p. 490 |
| `ring-pppu-60` | Ring Pseudo Planche Push-Up, 60 Degrees | `ring-pppu-40` | rings | 2 | OG2 p. 491 |
| `ring-support-hold` | Ring Support Hold | — | rings | 2 | OG2 pp. 83-84, 513 |
| `ring-support-rto` | Ring Support, Rings Turned Out | `ring-support-hold` | rings | 2 | OG2 p. 513 |
| `ring-l-sit` | Ring L-Sit | `ring-support-hold` | rings | 2 | BTGB p. 29 |

### Power (new) (30)

| id | name | after | equipment | batch | sources |
|---|---|---|---|---|---|
| `landing-stick` | Jump Landing Stick | — | none | 3 | CHU p. 111 (landing drill, adapted to the floor) |
| `pogo-hop` | Pogo Hop (two-foot ankle hop) | `landing-stick` | none | 3 | CHU p. 80; ATP p. 7-6; EC ch. 4 step 1 |
| `ankle-hop-side-to-side` | Side-to-Side Ankle Hop | `pogo-hop` | none | 3 | CHU p. 81 |
| `ankle-hop-hip-twist` | Hip-Twist Ankle Hop | `ankle-hop-side-to-side` | none | 3 | CHU p. 82 |
| `jump-squat` | Squat Jump | `pogo-hop` | none | 3 | ATP p. 5-2; EC ch. 4 step 2; SH p. 309 |
| `jump-vertical` | Vertical Jump with Arm Swing | `jump-squat` | none | 3 | CHU p. 87; EC ch. 4 step 3 |
| `jump-butt-kick` | Butt-Kick Jump | `jump-squat` | none | 3 | EC ch. 4 step 5; CHU p. 83 |
| `jump-tuck-slap` | Slap Tuck Jump | `jump-butt-kick` | none | 3 | EC ch. 4 step 6 |
| `jump-tuck` | Tuck Jump | `jump-tuck-slap` | none | 3 | CHU p. 82; ATP p. 5-17; EC ch. 4 step 7 |
| `jump-split` | Split Jump (scissor) | `jump-squat` | none | 3 | ATP p. 5-23; CHU p. 83; EC PDF pp. 260-262 |
| `jump-tuck-catch` | Catch Tuck Jump (!) | `jump-tuck` | none | 3 | EC ch. 4 step 8 |
| `jump-split-cycle` | Split Jump with Cycle | `jump-split` | none | 3 | CHU p. 84 |
| `jump-block` | Block Jump (short run-up) | `jump-vertical` | none | 3 | EC ch. 4 step 4 |
| `box-step-off-landing` | Box Step-Off Landing | — | box or step | 3 | CHU p. 111 |
| `box-push-off-alternating` | Alternating Box Push-Off | `box-step-off-landing` | step or box | 3 | CHU p. 104 |
| `box-push-off-single` | Single-Leg Box Push-Off (per side) | `box-push-off-alternating` | step or box | 3 | CHU p. 105 |
| `box-jump` | Box Jump (step down) | `box-step-off-landing` | box | 3 | CHU p. 106; SH p. 311; WOD pp. 48-50 |
| `box-jump-rebound` | Box Jump with Rebound (!) | `box-jump` | box | 3 | CHU p. 106 |
| `depth-jump` | Depth Jump (!) | `box-jump-rebound` | box | 3 | CHU pp. 17, 113 |
| `jump-lateral` | Lateral Two-Foot Jump | — | none | 3 | CHU p. 88 |
| `jump-broad` | Standing Broad Jump | — | none | 3 | CHU p. 86 |
| `cone-hops-lateral` | Lateral Hops over Markers | `jump-lateral` | none | 3 | CHU p. 98 |
| `cone-hops-front` | Forward Hops over Markers | `jump-broad` | none | 3 | CHU p. 95 |
| `jump-broad-consecutive` | Consecutive Broad Jumps | `jump-broad` | none | 3 | CHU p. 97 |
| `skater-bound` | Skater Bound (lateral bound) | `jump-lateral` | none | 3 | SH pp. 315-317 |
| `hop-single-leg-stick` | Single-Leg Hop and Stick (per side) | `jump-broad-consecutive` | none | 3 | CHU p. 102; ATP p. 7-6 |
| `hop-single-leg-lateral` | Single-Leg Lateral Hop (per side) | `hop-single-leg-stick` | none | 3 | CHU p. 90 |
| `bounding-alternate` | Alternate-Leg Bounding | `skater-bound` | none | 3 | CHU pp. 126-127 |
| `zigzag-hop` | Zigzag One-Foot Hops | `hop-single-leg-lateral` | none | 3 | CHU p. 103 |
| `bounding-single-leg` | Single-Leg Bounding (per side) (!) | `zigzag-hop` | none | 3 | CHU p. 130 |

### Conditioning (new) (35)

| id | name | after | equipment | batch | sources |
|---|---|---|---|---|---|
| `lateral-shuffle` | Lateral Shuffle | — | none | 3 | ATP p. 8-2; CHU p. 12 |
| `four-square-hops` | Four-Square Hops | — | none | 3 | CHU p. 72 |
| `carioca` | Carioca (crossover) | `lateral-shuffle` | none | 3 | ATP p. 8-5; CHU p. 14 |
| `hexagon-drill` | Hexagon Drill | `four-square-hops` | none | 3 | CHU p. 94 |
| `shuttle-sprint` | Shuttle Sprint | — | none | 3 | ATP p. 8-3 |
| `crouch-run` | Crouch Run (Army) | `shuttle-sprint` | none | 3 | ATP p. 8-6 |
| `half-jack` | Half Jack (Army) | — | none | 3 | ATP p. 5-11 |
| `butt-kicks` | Butt Kicks | — | none | 3 | CHU p. 11 |
| `skipping` | Skipping | — | none | 3 | CHU p. 122 |
| `squat-thrust-step-back` | Step-Back Squat Thrust | — | none | 3 | BWF |
| `jumping-jack` | Jumping Jack | `half-jack` | none | 3 | OG2 p. 81; SH |
| `mountain-climber-slow` | Slow Mountain Climber | — | none | 3 | ATP p. 5-3 |
| `seal-jack` | Seal Jack | `jumping-jack` | none | 3 | WOD |
| `high-knees` | High Knees | `butt-kicks` | none | 3 | ATP p. 8-1 |
| `squat-thrust` | Squat Thrust | `squat-thrust-step-back` | none | 3 | BWF; EC PDF p. 222 |
| `side-skip` | Side Skip with Arm Swing | `skipping` | none | 3 | CHU p. 124 |
| `jump-rope` | Jump Rope (single unders) | — | jump-rope | 3 | WOD pp. 61-62; SH p. 343 |
| `mountain-climber` | Mountain Climber | `mountain-climber-slow` | none | 3 | ATP p. 5-3 |
| `power-skip` | Power Skip | `skipping` | none | 3 | CHU p. 123; ATP p. 8-4 |
| `star-jack` | Star Jack | `seal-jack` | none | 3 | WOD |
| `sprawl` | Sprawl | `squat-thrust` | none | 3 | BWF |
| `mountain-climber-cross-body` | Cross-Body Mountain Climber | `mountain-climber` | none | 3 | WOD |
| `burpee` | Burpee | `sprawl` | none | 3 | BWF; SH p. 329 |
| `burpee-pushup` | Push-Up Burpee | `burpee` | none | 3 | SH pp. 330-339 |
| `eight-count-t-pushup` | 8-Count T Push-Up (Army) | `burpee-pushup` | none | 3 | ATP p. 5-12 |
| `jump-rope-double-under` | Jump Rope Double Unders | `jump-rope` | jump-rope | 3 | WOD p. 62 |
| `bear-plank` | Bear Plank Hold | — | none | 3 | BWF; OG2 p. 278 |
| `inchworm` | Inchworm | — | none | 3 | BWF |
| `rolling-sit-up` | Rolling Sit-Up (!) | — | mat | 3 | EC ch. 6 step 1 (PDF p. 160) |
| `bear-crawl` | Bear Crawl | `bear-plank` | none | 3 | SH p. 40; OG2 pp. 81-82 |
| `crab-walk` | Crab Walk | — | none | 3 | BWF |
| `frogger` | Frogger | — | none | 3 | BWF |
| `rolling-squat` | Rolling Squat (!) | `rolling-sit-up` | mat | 3 | EC ch. 6 step 2; BTGB p. 156 |
| `shoulder-roll` | Shoulder Roll (forward roll) | — | mat | 3 | ATP p. 6-9; OG2 pp. 322-323 |
| `shoulder-pop` | Shoulder Pop (!) | `rolling-squat` | mat | 3 | EC ch. 6 step 3 |

### Prehab (new) (38)

| id | name | after | equipment | batch | sources |
|---|---|---|---|---|---|
| `chin-nod-supine` | Supine Chin Nod | — | none | 3 | LIT; OG2 p. 295 |
| `chin-tuck-seated` | Seated Chin Tuck | `chin-nod-supine` | none | 3 | LIT; OG2 p. 295 |
| `chin-tuck-head-lift` | Chin Tuck with Head Lift | `chin-tuck-seated` | none | 3 | LIT |
| `neck-isometric-4-way` | Neck Isometrics (4-way) | `chin-tuck-head-lift` | none | 3 | LIT; MM |
| `neck-head-raise-manual` | Manual-Resistance Head Raise | `neck-isometric-4-way` | none | 3 | CC2 p. 129 |
| `hip-lateral-leg-raise` | Lateral Leg Raise (side-lying, per side) | — | none | 3 | ATP p. 4-2 |
| `hip-medial-leg-raise` | Medial Leg Raise (side-lying, per side) | — | none | 3 | ATP p. 4-2 |
| `clamshell` | Bent-Leg Lateral Raise (clamshell, per side) | — | none | 3 | ATP p. 4-3 |
| `side-lying-leg-tuck` | Single-Leg Tuck (side-lying, per side) | — | none | 3 | ATP p. 4-3 |
| `fire-hydrant` | Fire Hydrant (per side) | — | none | 3 | BWF |
| `donkey-kick` | Donkey Kick (per side) | — | none | 3 | BWF; EC PDF pp. 260-262 |
| `terminal-knee-extension` | Terminal Knee Extension (per side) | — | resistance-band | 3 | ATG; LIT |
| `stair-descent-slow` | Slow Stair Descent (per side) | — | step | 3 | LIT |
| `petersen-step-up` | Petersen Step-Up (per side) | — | step | 3 | OG2 pp. 266-267; ATG |
| `spanish-squat` | Spanish Squat Hold | — | resistance-band | 3 | ATG; LIT |
| `scapular-pushup-wall` | Scapular Push-Up (wall) | — | wall | 3 | OG2 pp. 303-304 |
| `prone-i-raise` | Prone I Raise | — | none | 3 | ATP p. 4-5; OG2 pp. 302-303 |
| `wall-slide` | Scapular Wall Slide | — | wall | 3 | OG2 p. 304 |
| `floor-angel` | Floor Angel | — | none | 3 | OG2 p. 305 |
| `external-rotation-side-lying` | Side-Lying External Rotation (per side) | — | none | 3 | OG2 p. 292; Reinold 2004 |
| `band-pull-apart` | Band Pull-Apart | — | resistance-band | 3 | OG2 p. 305 |
| `prone-t-raise` | Prone T Raise | — | none | 3 | ATP p. 4-6; OG2 pp. 302-303 |
| `scapular-pushup-knee` | Scapular Push-Up (knees) | `scapular-pushup-wall` | none | 3 | OG2 p. 304 |
| `prone-y-raise` | Prone Y Raise | — | none | 3 | ATP p. 4-6; OG2 pp. 302-303 |
| `prone-l-raise` | Prone L Raise | — | none | 3 | ATP p. 4-7; OG2 pp. 302-303 |
| `prone-w-raise` | Prone W Raise | — | none | 3 | ATP p. 4-8; OG2 pp. 302-303 |
| `scapular-pushup` | Scapular Push-Up | `scapular-pushup-knee` | none | 3 | OG2 p. 304; SH p. 26 |
| `external-rotation-band` | Band External Rotation (per side) | `external-rotation-side-lying` | resistance-band | 3 | OG2 p. 280 |
| `face-pull` | Face Pull | `band-pull-apart` | resistance-band or rings | 3 | BWF; OG2 p. 305 |
| `support-scapular-shrug` | Support Scapula Shrug | — | dip-support | 3 | OG2 pp. 295-296; SH p. 28 |
| `external-rotation-band-90` | Band External Rotation at 90 Degrees (per side) | `external-rotation-band` | resistance-band | 3 | OG2 p. 280 |
| `cuban-press` | Cuban Press | — | weight or resistance-band | 3 | BWF |
| `wrist-palm-pulses` | Palm Pulses and Finger Lifts | — | none | 3 | OG2 p. 307 |
| `wrist-seated-mobility` | Seated Wrist Circles and Bends | — | none | 3 | GMB |
| `forearm-rotation-fist` | Fist-on-Floor Forearm Rotations | — | none | 3 | SW p. 135 |
| `wrist-pushup-knees` | Back-of-Hand Push-Up (knees) | `wrist-palm-pulses` | none | 3 | OG2 p. 308; SH p. 78 |
| `wrist-curl-eccentric` | Eccentric Wrist Extension (per side) | — | weight | 3 | OG2 pp. 264-269 |
| `wrist-pushup` | Back-of-Hand Push-Up | `wrist-pushup-knees` | none | 3 | OG2 p. 308 |

### Pilates (new) (15)

| id | name | after | equipment | batch | sources |
|---|---|---|---|---|---|
| `pilates-mermaid` | Mermaid (side stretch, per side) | — | mat | 3 | Pilates (general knowledge, author's wording) |
| `pilates-spine-stretch-forward` | Spine Stretch Forward | — | mat | 3 | Pilates (general knowledge, author's wording) |
| `pilates-spine-twist` | Spine Twist | — | mat | 3 | Pilates (general knowledge, author's wording) |
| `pilates-single-leg-circle` | Single-Leg Circle (per side) | — | mat | 3 | Pilates (general knowledge, author's wording) |
| `pilates-saw` | Saw | — | mat | 3 | Pilates (general knowledge, author's wording) |
| `pilates-rolling-like-a-ball` | Rolling Like a Ball | — | mat | 3 | Pilates (general knowledge, author's wording) |
| `pilates-single-leg-stretch` | Single-Leg Stretch | — | mat | 3 | Pilates (general knowledge, author's wording) |
| `pilates-seal` | Seal | — | mat | 3 | Pilates (general knowledge, author's wording) |
| `pilates-roll-up` | Roll-Up | — | mat | 3 | Pilates (general knowledge, author's wording) |
| `pilates-double-leg-stretch` | Double-Leg Stretch | — | mat | 3 | Pilates (general knowledge, author's wording) |
| `pilates-double-leg-kick` | Double-Leg Kick | — | mat | 3 | Pilates (general knowledge, author's wording) |
| `pilates-scissors` | Scissors | — | mat | 3 | Pilates (general knowledge, author's wording) |
| `pilates-corkscrew` | Corkscrew | — | mat | 3 | Pilates (general knowledge, author's wording) |
| `pilates-open-leg-rocker` | Open-Leg Rocker | — | mat | 3 | Pilates (general knowledge, author's wording) |
| `pilates-roll-over` | Roll-Over | — | mat | 3 | Pilates (general knowledge, author's wording) |

### Stretches (18)

| id | name | after | equipment | batch | sources |
|---|---|---|---|---|---|
| `stretch-downward-dog` | Downward Dog | — | none | 3 | ATP p. 16-3 |
| `stretch-figure-four` | Figure-Four Stretch | — | none | 3 | BWF |
| `stretch-groin` | Groin Stretch (lateral lunge hold) | — | none | 3 | ATP p. 16-4 |
| `stretch-knee-to-chest` | Knee to Chest | — | none | 3 | ATP p. 17-6 |
| `stretch-neck-side` | Neck Side Stretch | — | none | 3 | OG2 p. 295 |
| `stretch-overhead-arm-pull` | Overhead Arm Pull | — | none | 3 | ATP p. 16-1 |
| `stretch-shoulder-er` | External Rotation Stretch (door frame) | — | wall | 3 | OG2 p. 293 |
| `stretch-sphinx` | Sphinx | — | none | 3 | ATP p. 16-7 |
| `stretch-spiderman` | Spiderman Lunge Stretch | — | none | 3 | BWF |
| `stretch-split-front` | Front Split | — | none | 3 | OG2 p. 297 |
| `stretch-split-middle` | Middle Split | — | none | 3 | OG2 p. 298 |
| `stretch-wrist-prayer` | Prayer Stretch | — | none | 3 | OG2 p. 308 |
| `stretch-wrist-reverse-prayer` | Reverse Prayer Stretch | — | none | 3 | OG2 p. 308 |
| `stretch-twist-straight-leg` | Straight-Leg Twist Hold (per side) | — | none | 3 | CC2 p. 214 |
| `stretch-twist-easy` | Easy Twist Hold (per side) | `stretch-twist-straight-leg` | none | 3 | CC2 p. 215 |
| `stretch-twist-half` | Half Twist Hold (per side) | `stretch-twist-easy` | none | 3 | CC2 p. 216 |
| `stretch-twist-three-quarter` | 3/4 Twist Hold (per side) | `stretch-twist-half` | none | 3 | CC2 p. 217 |
| `stretch-twist-full` | Full Twist Hold (per side) | `stretch-twist-three-quarter` | none | 3 | CC2 p. 218 |

### Warm-up (no tab) (4)

| id | name | after | equipment | batch | sources |
|---|---|---|---|---|---|
| `deep-squat-hold` | Deep Squat Hold | — | none | 3 | OG2 pp. 299, 569; SH p. 38 |
| `hip-raise-pushup` | Hip-Raise Push-Up (Army) | — | none | 3 | ATP pp. 7-10-7-11 |
| `joint-circles` | Joint Circles (neck to ankles) | — | none | 3 | OG2 p. 83; SH pp. 23-36 |
| `leg-swings` | Leg Swings (front and side) | — | none | 3 | ATP p. 7-8 |

### Army drill movements (routine-only) (16)

| id | name | after | equipment | batch | sources |
|---|---|---|---|---|---|
| `bend-and-reach` | Bend and Reach (routine-only) | — | none | 3 | ATP p. 3-1 |
| `prone-row-army` | Prone Row (Army) (routine-only) | — | none | 3 | ATP p. 3-8 |
| `squat-bender` | Squat Bender (routine-only) | — | none | 3 | ATP p. 3-5 |
| `windmill-army` | Windmill (Army) (routine-only) | — | none | 3 | ATP p. 3-6 |
| `high-jumper` | High Jumper (routine-only) | — | none | 3 | ATP p. 3-3 |
| `turn-and-lunge` | Turn and Lunge (routine-only) | — | none | 3 | ATP p. 5-9 |
| `y-squat` | Y Squat (routine-only) | — | none | 3 | ATP p. 5-13 |
| `criss-cross-jump` | Criss Cross Jump (routine-only) | — | none | 3 | ATP p. 7-8 |
| `alternate-twist-jump` | Alternate Twist Jump (routine-only) | — | none | 3 | ATP p. 7-9 |
| `front-kick-toe-touch` | Front Kick Alternate Toe Touch (routine-only) | — | none | 3 | ATP p. 5-16 |
| `side-to-side-knee-lifts` | Side-to-Side Knee Lifts (routine-only) | — | none | 3 | ATP p. 5-15 |
| `half-squat-laterals` | Half-Squat Laterals (routine-only) | — | none | 3 | ATP p. 5-19 |
| `leg-tuck-and-twist` | Leg-Tuck and Twist (routine-only) | — | none | 3 | ATP p. 5-4 |
| `straddle-run` | Straddle Run Forward and Backward (routine-only) | — | none | 3 | ATP p. 5-18 |
| `frog-jumps` | Frog Jumps Forward and Backward (routine-only) | — | none | 3 | ATP p. 5-20 |
| `quarter-turn-jump` | Alternate Quarter-Turn Jump (routine-only) | — | none | 3 | ATP p. 5-21 |

(!) = `cautionStrong`.

## Aliases

| existing or new id | alias | source |
|---|---|---|
| `pike-pushup` | Pike Headstand Push-Up (OG L1) | OG2 |
| `pike-pushup-elevated` | Box Headstand Push-Up (OG L2) | OG2 |
| `hspu-wall-negative` | Wall HeSPU Eccentric (OG L3) | OG2 |
| `hspu-wall` | Wall Headstand Push-Up (OG L4) | OG2 |
| `wall-handstand-hold` | Wall Handstand (OG L1-3) | OG2 |
| `wall-handstand-hold` | Handstand Facing the Wall | SW p. 186 |
| `inverted-row-feet-elevated` | Ring Row, feet raised (OG L2) | OG2 pp. 416-417 |
| `inverted-row-wide` | Wide Ring Row (OG L3) | OG2 |
| `archer-row` | Archer Ring Row (OG L4) | OG2 |
| `inverted-row` | Ring Row | OG2 / BTGB p. 91 |
| `inverted-row` | Aussie Pull-Up | SW |
| `inverted-row-bent-knees` | Bent-Knee Aussie Pull-Up | SW |
| `archer-row` | Archer Aussie Pull-Up | SW |
| `split-squat-bulgarian` | Chair Lunge | OG2 |
| `goblet-squat` | Goblet Squat (OG) | OG2 |
| `l-sit-one-leg` | One-Leg-Bent L-Sit | OG2 |
| `l-sit-tuck` | N-Hold / Bent Leg Hold | CC2 pp. 202-203 |
| `l-sit-one-leg` | Uneven N-Hold | CC2 |
| `l-sit-floor` | L-Hold / Straight Leg Hold | CC2 pp. 202-203 |
| `glute-bridge` | Short Bridge Hold | CC2 |
| `bridge-incline` | Angled Bridge Hold | CC2 |
| `bridge-head` | Head Bridge Hold | CC2 |
| `bridge-back` | Full Bridge Hold | CC2 |
| `dead-hang` | Bar Hang | CC2 p. 22 / SW |
| `calf-raise` | Calf Raise, floor, straight legs (CC2 step 2) | CC2 p. 136 |
| `calf-raise-single` | Single-Leg Calf Raise, floor, straight (CC2 step 4) | CC2 p. 137 |
| `scapular-pull` | Straight-Arm Pull | ATP p. 6-1 |
| `flexed-arm-hang` | Flexed-Arm Hang (Army, 5 s reps) | ATP p. 6-6 |
| `pullup-full` | Pull-Up (Army) | ATP p. 6-3 |
| `side-plank` | Side Bridge | ATP p. 4-10 |
| `bridge-single-leg` | Back Bridge (Army 4C, alternating) | ATP p. 4-10 |
| `v-up` | V-Up (Army) | ATP p. 5-2 |
| `single-leg-deadlift` | Single-Leg Deadlift (Army) | ATP p. 5-14 |
| `plank` | Front Leaning Rest (held) | ATP p. 5-9 |
| `stretch-supine-twist` | Single Leg Over | ATP p. 4-4 / p. 16-4 |
| `stretch-hamstring` | Hamstring Stretch (Army) | ATP p. 16-6 |
| `stretch-calf-wall` | Calf Stretch (Army) | ATP p. 16-5 |
| `stretch-quad-standing` | Thigh Stretch (Army) | ATP p. 16-3 |
| `stretch-shoulder` | Cross-Body Arm Stretch (RD MOD) | ATP p. 16-6 |
| `squat-shrimp` | Hover Lunge | SW |
| `single-leg-deadlift` | Drinking Bird | SW |
| `arch-hang` | Exaggerated Bar Hang | SW |
| `pushup-archer` | Lever Push-Up | CC1 |
| `towel-door-row` | Vertical Pull | CC1 |
| `chair-assisted-pullup` | Jackknife Pull-Up | CC1 |
| `jefferson-curl` | Jefferson Curl (GB) | GB |
| `warmup-shoulder-band` | Band Dislocates | GB |
| `hanging-knee-raise` | Leg Tuck (Army; thighs to elbows, bent-arm option) | ATP p. 6-4 |
| `pogo-hop` | Double-Leg Hop (Army) | ATP p. 7-6 |
| `pogo-hop` | Straight Hop | EC ch. 4 step 1 |
| `pogo-hop` | Two-Foot Ankle Hop | CHU p. 80 |
| `jump-squat` | Power Jump (Army) | ATP p. 5-2 |
| `jump-split` | Alternate Staggered Squat Jump (Army) | ATP p. 5-23 |
| `jump-split` | Scissors Jump | EC PDF p. 261 |
| `jump-vertical` | Vertical Leap | EC ch. 4 step 3 |
| `jump-vertical` | Standing Jump-and-Reach | CHU p. 87 |
| `box-step-off-landing` | Jump From Box | CHU p. 111 |
| `jump-broad-consecutive` | Double Leg Hops (Chu) | CHU p. 97 |
| `high-knees` | Vertical (Army MMD1) | ATP p. 8-1 |
| `lateral-shuffle` | Lateral (Army MMD1) | ATP p. 8-2 |
| `carioca` | Crossover (Army MMD2) | ATP p. 8-5 |
| `leg-swings` | Pendulum (Army) | ATP p. 7-8 |
| `walking-lunge` | Lunge Walk (Army Guerilla Drill) | ATP p. 6-9 |
| `walking-lunge` | Forward Lunge (Army PD; step out and return) | ATP p. 3-7 |
| `stretch-hip-flexor` | Rear Lunge (Army Recovery Drill stretch) | ATP p. 16-2 |
| `stretch-cobra` | Extend (Army 'Extend and Flex', hip sag) | ATP p. 16-3 |
| `stretch-downward-dog` | Flex (Army 'Extend and Flex') | ATP p. 16-3 |
| `stretch-overhead-arm-pull` | Standing Side Bend | BWF |
| `stretch-chest-door` | Corner Stretch | OG2 p. 293 |
| `stretch-sleeper` | Posterior Capsule Stretch | OG2 p. 294 |
| `stretch-hamstring` | Pike Stretch (stomach toward the legs) | OG2 p. 297 |
| `stretch-calf-wall` | Soleus Stretch (bent-knee option) | ATP p. 16-5 |
| `slide-hamstring` | Bridge Curl | HCL |
| `squat-cossack` | Cossacks (EC small-space drill) | EC PDF p. 97 |
| `squat-cossack` | Archer Squat (OG Squats L3) | OG2 p. 572 |
| `pullup-chest-to-bar` | Chest Pull-Up | EC ch. 9 step 6 |
| `pullup-sternum` | Hip Pull-Up (EC; explosive, bar to the hips - close match) | EC ch. 9 step 7 |
| `pullup-pullover` | Bar Pullover (EC) | EC ch. 9 step 9 |
| `muscle-up-bar` | The Muscle-Up (EC master step) | EC ch. 9 step 10 |
| `squat-thrust` | Thruster (EC: burpee without the jump) | EC PDF p. 222 |
| `nordic-curl` | Natural Leg Curl | BTGB p. 165 |
| `squat-pistol-box` | Graduated Single-Leg Squat | BTGB p. 160 |
| `dragon-flag` | Body Lever | BTGB p. 147 |
| `toes-to-bar` | Hanging Leg Lift (HLL) | BTGB p. 130 |
| `planche-adv-tuck` | Flat Tuck Planche | BTGB p. 52 |
| `front-lever-adv-tuck` | Flat Tuck Front Lever | BTGB p. 46 |
| `back-lever-adv-tuck` | Flat Tuck Back Lever | BTGB p. 42 |
| `planche-frog-stand` | Crow (close match; knees higher on the arms) | SW p. 174 |
| `frog-jumps` | Frog Hops | BWF |
| `bridge-reverse-plank` | Straight Bridge (CC1 step 2 / CC2 Trifecta) | CC2 p. 193 |
| `quadraplex` | Quadraplex (Army 4C) | ATP p. 4-11 |
| `windshield-wiper-bent-knee` | Bent-Leg Body Twist (Army PD) | ATP p. 3-8 |
| `bicycle-crunch` | Supine Bicycle (Army CD2) | ATP p. 5-10 |
| `half-jack` | Half Jack (Army CD2) | ATP p. 5-11 |
| `hip-lateral-leg-raise` | Lateral Leg Raise (Army HSD 1) | ATP p. 4-2 |
| `hip-medial-leg-raise` | Medial Leg Raise (Army HSD 2) | ATP p. 4-2 |
| `clamshell` | Bent-Leg Lateral Raise (Army HSD 3) | ATP p. 4-3 |
| `side-lying-leg-tuck` | Single-Leg Tuck (Army HSD 4) | ATP p. 4-3 |
| `hollow-tuck` | The Hundred (Pilates; legs in tabletop, add small arm pumps) | Pilates |
| `hollow-hold` | The Hundred, legs extended (Pilates) | Pilates |
| `v-up` | Teaser (Pilates) | Pilates |
| `swimmer` | Swimming (Pilates) | Pilates |
| `stretch-cobra` | Swan (Pilates, prep) | Pilates |
| `bridge-single-leg` | Shoulder Bridge (Pilates) | Pilates |
| `glute-bridge` | Shoulder Bridge Prep (Pilates) | Pilates |
| `hip-lateral-leg-raise` | Side Kick Series, up and down (Pilates) | Pilates |
| `plank` | Leg Pull Front (Pilates; add slow alternate leg lifts) | Pilates |
| `bridge-reverse-plank` | Leg Pull Back (Pilates) | Pilates |
| `bicycle-crunch` | Criss-Cross (Pilates) | Pilates |
| `pushup-standard` | Pilates Push-Up (walk-out, 3 push-ups) | Pilates |
| `side-plank` | Side Bend (Pilates, held) | Pilates |
| `sit-up` | Neck Pull (Pilates, slow roll-up with hands behind the head) | Pilates |
| `pilates-roll-over` | Jackknife (Pilates, advanced roll-over) | Pilates |
| `stretch-hamstring` | Spine Stretch (seated, Pilates prep) | Pilates |

## Options

| applies to | type | values |
|---|---|---|
| family:pullup, family:row, dead-hang, flexed-arm-hang, family:grip | grip | pronated; supinated; neutral; mixed; wide; close; commando (side-on); false grip; towel |
| family:pushup, family:handstand, family:planche | hand | flat; cambered (handstand balance); fist/knuckle; fingertip (cap about 5 reps, 2-3 sets, never explosive); claw |
| family:pushup, pushup-one-arm, pushup-one-arm-elevated, pushup-half-one-arm | stance | feet together; straddle (easier); wide; one leg raised |
| family:squat, squat-air, family:db-squat | stance | narrow; shoulder width; wide / sumo; prisoner arms |
| pushup-standard | width | standard; wide push-up |
| family:plank (L-sit line), l-sit-straddle, family:planche, family:handstand, ring-l-sit | surface | floor; parallettes; dip bars; chair arms |
| squat-air | depth | parallel (OG L1); full (OG L2) |
| pushup-one-arm-elevated | angle | 45 degrees; 35 degrees; 25 degrees; 15 degrees; floor |
| pushup-standard, pushup-knee | range | full; half push-up (CC step 4) |
| dip-parallel, dip-support-hold | range | full; top-half dip |
| dip-parallel | leg position | legs behind; legs forward (EC step 8) |
| * | mode/tempo | eccentric only; top hold 5-10 s (rows); 1.5 reps; hybrid set (singles of the next step + current step); timed hold; Army slow cadence (50 counts/min, about 4.8 s per 4-count rep); Army moderate cadence (80 counts/min, about 3 s per rep) |
| family:plyo, family:plyo-lateral, family:plyo-box | mode | stick each landing; continuous (rebound) |
| bridge-single-leg | mode | reps; alternating hold, switch every 5 s (Army 4C: 6 x 5 s) |
| * | assistance/load | band; partner; pulley; weighted vest; ankle weights; added weight (rollouts +20 lb, OG L9) |
| inverted-row, inverted-row-wide, inverted-row-feet-elevated, archer-row, pullup-archer, front-lever-tuck, front-lever-adv-tuck, front-lever-straddle, pullup-false-grip-hang, arms-biceps-curl, arms-triceps-extension | equipment alternative | rings |
| family:rollout | equipment alternative | ab wheel; rings (higher rings = easier) |
| cone-hops-front | pattern | straight line; diagonal zigzag |
| bear-crawl, crab-walk | direction | forward; backward; lateral |
| leg-swings | direction | front-back; side to side |
| wall-slide | variant | no band; band wall slide |
| band-pull-apart | variant | chest height; overhead pull-apart |
| muscle-up-bar, ring-muscle-up | variant | strict; kipping |
| stretch-quad-standing | variant | standing; side-lying (Army Thigh Stretch) |
| stretch-calf-wall | variant | straight knee (calf); bent knee (soleus) |
| family:* | variant | standard; Army modified (MOD) |
| family:stretch-*, stretch-split-front, stretch-split-middle, stretch-pancake | stretch mode | static; PNF / contract-relax (10 s contractions, about 80 s total); loaded end-range (small load steps) |
| hollow-tuck, hollow-hold | variant | still; Hundred arm pumps (5 in, 5 out breathing) |
| plank | variant | still; alternate slow leg lifts (Pilates leg pull front) |
| handstand-free | progression target | 15-20 s; 30 s; 60 s |

## Fixed routines

**Four for the Core (Army 4C)** (`army-4c`, flow = false; ATP 7-22.02 C1 pp. 4-8-4-11). Rest between items is a 3-5 s return to the start position. flow=false so hold times can be logged (4C works as a core test).

1. `bent-leg-raise-army`: 60 s. Up to 60 s; if the low-back pressure on the hands drops, knees to chest 3-5 s, then resume.
2. `side-plank`: 60 s per side. Side Bridge on the elbow.
3. `bridge-single-leg`: 30 s. Back Bridge: hips up with one knee straight, switch legs every 5 s, 6 x 5 s (alternating-hold option).
4. `quadraplex`: 60 s per side

**Hip Stability Drill (Army HSD)** (`army-hsd`, flow = true; ATP 7-22.02 pp. 4-1-4-4). Start sitting; change sides by swivelling on the buttocks. Slow cadence = 50 counts/min (about 4.8 s per 4-count rep).

1. `hip-lateral-leg-raise`: 10 reps per side. Manual: 5-10 per side, slow cadence.
2. `hip-medial-leg-raise`: 10 reps per side. 5-10 per side.
3. `clamshell`: 10 reps per side. Bent-Leg Lateral Raise; 5-10 per side.
4. `side-lying-leg-tuck`: 10 reps per side. Single-Leg Tuck; 5-10 per side.
5. `stretch-supine-twist`: 30 s per side. Single Leg Over; hold 20-30 s per side.

**Shoulder Stability Drill (Army SSD)** (`army-ssd`, flow = true; ATP 7-22.02 pp. 4-4-4-8). All prone, head slightly raised, arms lifted 3-6 in, slow 4-count. REP COUNT FLAGGED: 10 per raise chosen by the app from the PD/CD 10-rep standard.

1. `prone-i-raise`: 10 reps. 10 reps is the app's choice (FLAG: the manual prints no rep count).
2. `prone-t-raise`: 10 reps. 10 reps is the app's choice (FLAG: the manual prints no rep count).
3. `prone-y-raise`: 10 reps. 10 reps is the app's choice (FLAG: the manual prints no rep count).
4. `prone-l-raise`: 10 reps. 10 reps is the app's choice (FLAG: the manual prints no rep count).
5. `prone-w-raise`: 10 reps. 10 reps is the app's choice (FLAG: the manual prints no rep count).

**Preparation Drill (Army PD)** (`army-pd`, flow = true; ATP 7-22.02 pp. 3-1-3-9). 10 reps each as 4-count reps. Order is fixed. Alternatives (Table 1-1) not modelled: power-focused, condensed-time, PD MOD.

1. `bend-and-reach`: 10 reps. slow
2. `lunge-reverse`: 10 reps. Rear Lunge; slow; alternate legs.
3. `high-jumper`: 10 reps. moderate
4. `rower-situp`: 10 reps. Rower; slow; manual allows 5-10.
5. `squat-bender`: 10 reps. slow; manual allows 5-10.
6. `windmill-army`: 10 reps. slow; manual allows 5-10.
7. `walking-lunge`: 10 reps. Forward Lunge: step out and return (option), alternate legs; slow.
8. `prone-row-army`: 10 reps. slow
9. `windshield-wiper-bent-knee`: 10 reps. Bent-Leg Body Twist; slow.
10. `pushup-standard`: 10 reps. moderate

**Recovery Drill (Army RD)** (`army-rd`, flow = true; ATP 7-22.02 pp. 16-1-16-6). Eight stretches, 20-30 s per side. Extend and Flex is split into two app items.

1. `stretch-overhead-arm-pull`: 30 s per side
2. `stretch-hip-flexor`: 30 s per side. Rear Lunge stretch.
3. `stretch-cobra`: 20 s. Extend and Flex, part 1: straight-arm hip sag.
4. `stretch-downward-dog`: 20 s. Extend and Flex, part 2: hips up, heels down.
5. `stretch-quad-standing`: 30 s per side. Thigh Stretch: side-lying option.
6. `stretch-supine-twist`: 30 s per side. Single-Leg Over.
7. `stretch-groin`: 30 s per side
8. `stretch-calf-wall`: 30 s per side. Bent knee first (soleus), then straight knee.
9. `stretch-hamstring`: 30 s. Seated; reach, then repeat reaching further.

**Pilates Mat: Beginner** (`pilates-mat-beginner`, flow = true; Classical mat order (author's general knowledge); DRAFT). About 11 exercises, roughly 12-15 min. Continuous: auto-advance, no rest timer, no per-set logging.

1. `hollow-tuck`: 45 s. The Hundred, legs in tabletop, small arm pumps.
2. `pilates-roll-up`: 5 reps. Bend the knees if needed.
3. `pilates-single-leg-circle`: 5 reps per side. 5 each direction.
4. `pilates-rolling-like-a-ball`: 6 reps
5. `pilates-single-leg-stretch`: 8 reps per side
6. `pilates-double-leg-stretch`: 6 reps
7. `pilates-spine-stretch-forward`: 5 reps
8. `swimmer`: 20 s. Swimming.
9. `glute-bridge`: 8 reps. Shoulder Bridge prep.
10. `hip-lateral-leg-raise`: 8 reps per side. Side Kick series, up and down.
11. `pilates-seal`: 6 reps

**Pilates Mat: Intermediate** (`pilates-mat-intermediate`, flow = true; Classical mat order (author's general knowledge); DRAFT). 23 items in classical order, roughly 25-30 min. Continuous: auto-advance, no rest timer, no per-set logging.

1. `hollow-hold`: 60 s. The Hundred, legs extended low.
2. `pilates-roll-up`: 6 reps
3. `pilates-roll-over`: 5 reps
4. `pilates-single-leg-circle`: 5 reps per side
5. `pilates-rolling-like-a-ball`: 8 reps
6. `pilates-single-leg-stretch`: 10 reps per side
7. `pilates-double-leg-stretch`: 10 reps
8. `pilates-scissors`: 8 reps per side
9. `bicycle-crunch`: 8 reps per side. Criss-Cross.
10. `pilates-spine-stretch-forward`: 5 reps
11. `pilates-open-leg-rocker`: 6 reps
12. `pilates-corkscrew`: 3 reps per side
13. `pilates-saw`: 4 reps per side
14. `stretch-cobra`: 20 s. Swan (prep).
15. `pilates-double-leg-kick`: 3 reps per side
16. `pilates-spine-twist`: 5 reps per side
17. `v-up`: 5 reps. Teaser.
18. `swimmer`: 30 s. Swimming.
19. `plank`: 30 s. Leg Pull Front: slow alternate leg lifts.
20. `bridge-reverse-plank`: 20 s. Leg Pull Back.
21. `hip-lateral-leg-raise`: 10 reps per side. Side Kick series.
22. `bridge-single-leg`: 5 reps per side. Shoulder Bridge.
23. `pilates-seal`: 8 reps

**Pilates 10-Minute Core Flow** (`pilates-core-10`, flow = true; Author's design from classical mat exercises; DRAFT). Work is about 8.5 min plus about 5 s auto-advance per item, about 10 min total. Continuous.

1. `hollow-tuck`: 60 s. The Hundred.
2. `pilates-roll-up`: 6 reps
3. `pilates-single-leg-circle`: 5 reps per side
4. `pilates-single-leg-stretch`: 10 reps per side
5. `pilates-double-leg-stretch`: 8 reps
6. `pilates-scissors`: 8 reps per side
7. `bicycle-crunch`: 8 reps per side. Criss-Cross.
8. `v-up-tuck`: 5 reps. Teaser prep (tuck).
9. `plank`: 40 s. Leg Pull Front.
10. `side-plank`: 30 s per side. Side Bend, held.
11. `swimmer`: 30 s. Swimming.
12. `pilates-rolling-like-a-ball`: 8 reps
13. `pilates-spine-stretch-forward`: 5 reps

Flow routines auto-advance with no rest timer and no per-set logging; each item carries `estSeconds` (per side) for the auto-advance.

## Pose groups

| group | ids |
|---|---|
| `pushup_new` | 14 |
| `dip_planche` | 15 |
| `handstand_balance` | 21 |
| `pullup_new` | 19 |
| `levers` | 14 |
| `grip_row_arms` | 13 |
| `squat_legs` | 20 |
| `hips_bridge` | 11 |
| `calf_balance` | 10 |
| `core_floor` | 21 |
| `hanging_core` | 15 |
| `core_supine` | 18 |
| `flag_pole` | 16 |
| `plyo_a` | 13 |
| `plyo_b` | 17 |
| `conditioning` | 20 |
| `crawl_agility` | 15 |
| `prehab_shoulder` | 17 |
| `prehab_wrist_hip` | 12 |
| `neck_knee_warmup` | 13 |
| `army_drill` | 16 |
| `stretches_c` | 18 |
| `pilates_mat` | 15 |
| `rings` | 16 |

All group names are new (none reuses an existing `tools/blender/families/*.py` name). Ring clips: two short straps (rods) hanging from a bar; the handle height is in each id's `props`.

## Open questions

- **SSD reps**: the manual prints none; the routine uses 10 per raise (flagged in the JSON).
- **Enums**: no PLYO/CONDITIONING pattern, no NECK or TIBIALIS muscle. Add them, or keep the stand-ins?
- **EC calls overridden**: Russian and L-sit dips kept (EC says 'skip or later'; both are OG/BtGB rungs); shoulder roll kept (EC skips its roll chain, but it is an Army drill and OG's handstand bail); kipping pull-up kept as a cautioned batch-2 on-ramp although the report files it under Tier D.
- **Pilates mapping**: Criss-Cross is an alias of `bicycle-crunch` (not a new id); Single-Leg Stretch is a new id (no close match); Hundred maps to `hollow-tuck`/`hollow-hold` with an arm-pump option. OK?
- **Equipment**: `step` and `box` are separate ids; `bench` is new (dragon flag, hip thrust); a towel has no id. Merge `step` into `box`?
- **Renames** suggested by the report: Shoulders tab to 'Handstand'; family `german-hang` to 'Back lever'. Not done here.
- **Flag steps**: CC2 step names are verified; the posture wording is a paraphrase to check against the book photos.
- **4C flow**: set to false so 60 s holds can be logged; the other Army drills are flow. Change if 4C should run as a continuous drill.
- **Existing entries to touch** (outside this file): add rings alternatives to the ids listed under Options; fix the `stretch-supine-twist` citation; `arch-hang` vs `warmup-arch-hang` duplicate.
- **More routines**: CD1/CD2 (and gated CD3), CL1/CL2 could be added cheaply now that their entries exist.
