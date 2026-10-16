# Progression comment: what to include, and how progressions should look

Written 2026-10-16 by the Opus coordinator, in response to [27-progression-review.md](27-progression-review.md) and [28-r8-simplicity-review.md](28-r8-simplicity-review.md). It is based on the generated catalog: 533 variations in 121 families. **Proposals only; all content stays DRAFT.**

## 1. Short verdict

I agree with Codex's diagnosis on every point. "Library inclusion ≠ prescription" is the right lens, and both proposals are right: "next" means the main successor, and the default workout pool stays small.

I would go one step further and make the decisions now instead of debating branch by branch. The 379 additions are fine as **a library**. They are wrong as **379 rungs**. Most of the clutter comes from three structural causes, and fixing those removes most of the 47 flagged branches at once:

1. **Every exercise gets exactly one role** (§2). Today an `insertAfter` edge silently means "rung".
2. **A family is one progression with one goal.** Today some families hold two or three progressions. `plank` mixes plank, hollow and L-sit; `pushup` mixes strength and power; `handstand` mixes headstand and handstand; `bridge` mixes hip strength and back-bridge mobility. Splitting those removes most "dangling branch" cases.
3. **Only a few progressions are trainable by default** (§4). Skills, power, conditioning, prehab and Pilates arrive only by a deliberate choice: a goal, a focus or a program.

## 2. Five roles (every exercise gets exactly one)

| Role | Meaning | In the tree | Auto-fill | Gates anything? |
|---|---|---|---|---|
| **Main step** | A rung on its progression's single main path, toward a named goal | Drawn as the spine | Yes, if the progression is in the pool | Only the next main step |
| **Alternate** | Same level as a main step, a different way to train it (equipment, grip, stance, range) | Collapsed under its rung ("other ways") | Only by rotation within its explicit group | Never |
| **Support** | Prep, accessory or specialty drill that helps a main step | Listed on the step's detail ("helps with") | Never by default | Never |
| **Program-only** | Exists for a fixed routine (Army, Pilates) | Hidden from trees; found by search | Never | Never |
| **Library-only** | Kept for completeness or reference; weak role, or the clip can't teach it well | Hidden from trees; found by search | Never | Never |

**Rules that follow:**
- **One main successor per main step.** A **branch** is a second main path inside a family, toward a different named goal (e.g. one-arm pull-up vs. muscle-up). It must either end at its own goal or rejoin a named main step. It never dangles.
- **Equipment and partial range never gate.** A box, band, elevated surface or partial-ROM drill is an **alternate** or **support** unless the progression's goal needs that equipment, like rings or a pole.
- **A gate is one achievement on the previous main step.** Use OR-groups when there are several entry routes. Never inherit gates from rank or adjacency.
- **The stepper means "next main step"** (agree with doc 28). Family browsing is a separate "Other ways" list.
- **Visible main path ≤ about 8 rungs per progression.** If a source has more, the extra source rungs become alternates or support. CC's 10 steps don't all need to be rungs.

## 3. Proposed progressions, family by family

Notation: `→` main path. **[alt]**, **[sup]**, **[lib]** and **[prog]** are the other roles. ★ marks a split or move to a new family.

### Push
- **Push-up** (goal: one-arm push-up): `wall → incline-high → incline → standard → {feet-elevated | diamond} → archer → one-arm-elevated → one-arm-negative → one-arm`.
  - [alt] knee (at incline), uneven and half-one-arm (at archer), one-arm-straight-body (at the goal).
  - [lib] wall-push-away, single-leg, hindu.
  - Pseudo planche push-up → [sup] of planche, and offered as the "after diamond" alternative for people following the planche goal (the RR main line).
- ★ **Power push-up** (new family, out of `pushup`): `pop-up-incline → kneeling-push-off → pop-up → clap → chest-strike`. pop-up-wide-close → [alt]. Keeps the 3-rep cap and the `pushup-standard` gate. Not in default auto-fill.
- **Dip** (goal: straight-bar dip): `support-hold → negative → parallel → straight-bar`.
  - [alt] bench-bent-knee and bench-feet-elevated as the no-bars entry at support/negative level; jumping (at negative).
  - [lib] bench-straight-leg, feet-supported.
  - [sup] Russian dip (muscle-up transition).
  - [lib] l-sit dip, forward-lean, Korean.
- **HSPU** (goal: freestanding HSPU): `pike → pike-elevated → wall-HS-hold → wall-negative → wall-HSPU → deficit → free-head → free`. pike-hands-elevated → [alt].
- ★ **Handstand** split in two:
  - **Handstand balance**: `wall-plank → wall-walk-up → chest-to-wall hold (reuse wall-handstand-hold; don't duplicate) → toe-pulls → one-foot → kick-up → free`.
  - **Headstand** becomes [sup] and an entry option for beginners, not rungs: tripod-tuck, tripod, wall, forearm.
  - **Press to handstand** stays a branch after free HS: `wall-negative → elevated → straddle`. bent-arm → [alt].
- **Planche** (goal-only): `frog-stand → lean → tuck → adv-tuck → straddle → full`. straight-arm frog stand → [alt]. tuck push-up and PPPU → [sup].

### Pull
- **Pull-up** (goal: pull-up, then chest-to-bar): `dead-hang → scapular-pull → arch-hang → negative → pull-up → chest-to-bar`.
  - [alt] band, chair and jumping (at negative); flexed-arm-hang (at negative); chin-up (at pull-up).
  - [sup] partial, kip-swing.
  - [lib] commando, kipping, pullover-jumping.
- **Branch: one-arm pull-up** (from pull-up): `archer → uneven → one-arm-assisted → one-arm-negative → one-arm`. typewriter → [alt] at archer; L-sit pull-up → [alt] at uneven level; one-arm-flex-hang → [sup].
- **Branch: muscle-up** (from chest-to-bar, plus straight-bar dip): `sternum → muscle-up-negative → muscle-up-bar`. False-grip hang and Russian dip → [sup]. kipping muscle-up → [lib] with its caution; don't make it a rung. pullover → [sup] (bar skill).
- **Row** (goal: archer row): `towel-door → bent-knees → inverted → feet-elevated → archer`. band → [alt]; wide → [alt].
  - **Branch: one-arm row**: `archer → one-arm-straddle → one-arm`. archer-arm-in → [alt].
  - Front lever rows → **moved to Lever** as [sup] (see below).
- **Front lever** (goal-only): `tuck → adv-tuck → straddle → one-leg → full`. This order (straddle before one-leg/half lay) matches OG2 L6/L7 and BtGB, so keep it. FL rows (tuck, adv, straddle, full) → [sup] on the matching rung. ice-cream-maker, hang-to-row → [sup].
- **Back lever** (goal-only): `german-hang → skin-the-cat → tuck → adv-tuck → straddle → one-leg → full`, as the catalog has it (OG2 L1–L7).
- **Grip** → all [sup]. The towel ladder becomes one progression: `one-arm-hang → towel → twin-towel → uneven-towel → one-arm-towel`; horizontal-hang and uneven-hang → [alt]. Never auto-filled.

### Legs
- **Squat** (goal: pistol): `assisted → air → split → bulgarian → pistol-assisted → {box | counterbalance} → pistol`.
  - [alt] box squat and half squat (at assisted); pistol-negative and pistol-elevated (at box/counterbalance; equipment never gates).
  - [sup] wall-sit, horse-stance, cossack.
  - [lib] close and uneven squat (CC-specific; they duplicate the rung's purpose).
  - **Shrimp branch**: `shrimp → intermediate → advanced`. two-hand and deficit → [lib].
- **Step-up** is not its own family. step-up → [alt] at split squat; step-up-deep → [alt] at bulgarian.
- **Lunge**: walking, reverse and lateral are [alt]s at split-squat level, not a progression.
- **Hinge** (goal: Nordic curl): `rdl-bodyweight → single-leg-deadlift → nordic-negative-banded → nordic-banded → nordic`. harop-beginner and harop → [alt] at nordic-banded / nordic. Slides stay their own short progression (equipment: sliders).
- **Quad**: `sissy-assisted → sissy → natural-leg-extension`; reverse-nordic → [sup]. Goal-only, not auto-filled.
- **Calf**: collapse 9 rungs into `calf-raise → step-calf-raise → single → step-single → single-loaded`. Every bent-knee (soleus) version → [alt] at its straight-knee twin, as one rotation group. That removes 4 rungs.
- ★ **Bridge** split in two:
  - **Hip strength**: `glute-bridge → single-leg-bridge → hip-thrust → single-leg-hip-thrust`.
  - **Back bridge** (mobility goal): `table → incline → head → half → wall-walk-down → back-bridge → wall-walk-up → stand-to-stand`. closing and wall-push-off → [sup]; reverse-plank → [sup].

### Core (the most crowded tab: 93 entries, so the most pruning)
- ★ Split `plank` into three progressions:
  - **Plank / anti-extension**: `dead-bug → plank → long-lever → body-saw → rollout-knees → rollout-ramp → rollout-negative → rollout-standing`. This merges the rollout family as its top. rollout-one-arm → [lib].
  - **Hollow**: `hollow-tuck → one-leg → hollow → hollow-rocks`.
  - **L-sit / compression**: `foot-supported → one-foot → tuck → adv-tuck → one-leg → L-sit → straddle-L → V-sit`. compression-lift → [sup]; ring L-sit → [alt].
- **Dead bug**: the 4 new steps (brace, 90-90, single-leg, and the Army bent-leg raise) are too fine. Keep `dead-bug-90-90 → dead-bug-single-leg → dead-bug` as the plank entry. brace → [sup]; bent-leg-raise-army → [prog].
- **Bird dog** (agree with doc 28): `quadruped → arm-raise → leg-raise → bird-dog`. swimmer → [sup] (prone, so it belongs with superman/extension); quadraplex → [prog] (Army 4C) and an alias of bird-dog.
- Plank-contralateral and arm-leg-reach → [alt] of bird-dog / plank-tap (anti-rotation), not plank rungs.
- **Floor leg raise**: `knee-tuck → bent-knee-raise → frog-raise → leg-raise → V-up`. seated-knee-raise → [alt]; reverse-crunch → [alt]; V-up tuck and straddle → [alt] at V-up; floor windshield wipers → [sup].
- **Hanging leg raise** (the owner's toes-to-bar ask): `supported-knee-raise → hanging-knee-raise → bent-leg → straight → toes-to-bar`. Twisting → [sup]; windshield wipers (half → full) = a short branch after toes-to-bar; heel-hook → [prog].
- **Dragon flag**: `candlestick → tuck → one-leg → negative → full`. adv-tuck → [alt] (OG/BWF order is tuck → adv tuck → one-leg; keep one of the two middle rungs as main).
- **Side plank / Copenhagen**: as is; knee side plank → [alt] entry. hip-raise → [alt].
- **Extension** (superman): `superman-hold → arch-hold-y → arch-rocks`; back-extension-floor and swimmer → [alt].
- **Crunch family** → [alt], rotated into the core slots (owner decision 2026-10-16: more core variety is welcome). Flexion group: crunch, sit-up, rower, McGill curl-up. Rotation group: bicycle crunch, Russian twist. They add variety without being rungs, so they never gate anything.

### Skills that need special equipment (all goal-only, hidden without the equipment)
- **Flag** (16 rungs over two routes): keep **one** main path to the clutch flag, about 6 rungs: `clutch-hang → diagonal-tuck → diagonal → horizontal-tuck → bent-leg → clutch`. The other clutch steps → [alt]. The press-flag route → **branch** with 4 rungs: `support-press → vertical-press → bent-leg-press → press-flag`; the rest → [lib].
- **Rings**: keep the Rings tab, shown only when the profile has rings:
  - support: `support → RTO`;
  - push-ups: `ring → RTO → RTO-archer → PPPU-40 → PPPU-60`;
  - dips: `negative → dip → RTO`;
  - muscle-up: `false-grip → negative → MU`.

  ring-pushup-wide and ring-dip-L → [alt].
- **Elbow lever**: `umbrella → straddle → full`. Fine as is; goal-only.

### Power, conditioning, mobility, prehab, Pilates (never in the default pool)
- **Jump** (one vertical ladder): `landing-stick → pogo → jump-squat → tuck-jump → box-jump → box-jump-rebound → depth-jump`. jump-vertical, block, tuck-slap and catch → [alt]; split jump and cycle → [alt] at tuck; box push-offs → [sup].
- **Bound** (horizontal/lateral): `lateral-jump → broad-jump → skater-bound → single-leg-hop-stick → lateral-single-leg-hop → bounding`. cone hops, zigzag and single-leg bounding → [alt]/[lib].
- **Conditioning** is not a progression. Make it an **interval pool**: burpee is the one ladder (`squat-thrust → burpee → burpee-push-up`). jacks, high knees, skips, mountain climbers, sprawl and jump rope → pool items for a "Conditioning finisher" option. Agility and crawl → same pool. eight-count-T and Army moves → [prog].
- **Prehab**: no ladders in Progress. Present it as **support attached to the exercises it protects** (wrist prep on handstand and planche; external rotation and YTW on dips and HSPU; Spanish squat on pistol/sissy), plus the Army SSD/HSD programs.
  - The five prone I/T/Y/L/W raises → one library entry "Prone I-T-Y-L-W" with a variant option; the five ids stay [prog] for SSD.
- **Neck** → [lib], plus support for bridging. The chin-tuck movies can't show the action (doc 28), so hold them until a better demo exists.
- **Pilates** → [prog] (the 3 programs) plus library. No ladder, never auto-filled. Hold roll-up, roll-over, seal, saw, spine-twist and double-leg-kick from the library until the render review confirms the clip teaches the movement. The programs can still list them with text cues.
- **Twist holds** (5 stretch steps) → one stretch, "Seated twist", with a level option.
- **Army drill moves** → [prog] (already `routineOnly`).

## 4. Default pool vs. deliberate additions

- **Default automatic workouts** draw only from these progressions:
  - push-up, dip
  - pull-up, row
  - squat, hinge (RDL → Nordic)
  - plank/anti-extension, hanging or floor leg raise (whichever the equipment allows), side plank, extension
  - **Core variety (owner decision 2026-10-16):** each core slot rotates through its alternates between workouts, at the person's current level. Anti-extension rotates plank, long-lever plank, body saw and dead-bug variants. Flexion rotates leg raise, V-up variants, reverse crunch and the crunch group. Rotation rotates side plank, Pallof, bird dog, contralateral plank, bicycle crunch and Russian twist. Extension rotates superman, back extension and swimmer. Progress still follows the main step only.
  - calf

  That is about what R7 had, so new catalog rows don't change an ordinary workout.
- **A goal adds its progression** to the pool: handstand, HSPU, front or back lever, planche, muscle-up, one-arm pull-up or push-up, pistol or shrimp, dragon flag, L-sit, flag, rings, back bridge, elbow lever.
- **Focus toggles add pools**, each off by default: power (jump and bound plus power push-up), a conditioning finisher, a prehab block attached to the session's main exercises, mobility.
- **Programs** cover Army and Pilates (already done; the compact list from doc 28 §5 is good).
- **Visibility**: Progress shows only the default progressions plus active goals, with an "All progressions" entry for the rest. Search covers everything, including [lib].

## 5. Rough effect

From the ladder dump: about **200 main steps**, **~110 alternates**, **~60 support**, **~35 program-only** and **~75 library-only**. Nothing is deleted; every exercise keeps its clip and data. What changes:

- **Visible main paths**: the longest drops from 30 (pull-up) to 6 + two short branches.
- **Default pool**: unchanged from R7 in size.
- **Flagged branches**: of the 47, about 40 are resolved by the role rules and family splits above. Codex should list any that remain.

## 6. On doc 27's specific findings

| Doc 27 finding | My call |
|---|---|
| Intermediates that end without rejoining (partial pull-up, negative/elevated pistol, half bridge, beginner Harop) | Partial → [sup]; negative and elevated pistol → [alt] at box pistol; half bridge → main rung in the back-bridge path (it now rejoins); beginner Harop → [alt] at banded Nordic. |
| Elevated pistol as a mandatory step | Agree: [alt]. Equipment never gates. |
| Partial pull-ups | Agree: [sup]. Full range is taught by negatives and assisted reps. |
| V-ups vs. L-sit compression | Separate progressions (§3 Core); compression lift → [sup] of L-sit. |
| Bird dog → swimmer | Agree with doc 28: quadruped ramp is main; swimmer → [sup] in extension. |
| Adv-tuck FL row → ice-cream maker | Move FL rows to the lever as [sup]. Ice-cream maker → [sup]. |
| Range-mode stepper sorts by rank | "Next main step". Rank-sorted lists only in "Other ways". |
| Loaded variants in a separate family | Add an explicit `progressionGroup` key, so bodyweight and weighted versions of one movement count as one progression for the no-duplicate rule. |
| Mixed achievement thresholds | One rule: a gate is tier 3 (or the policy's "solid" tier) on the previous main step. Branch entry is stated once. |
| Different power profiles | Agree: keep the rep cap only on the six explosive push-ups. Jumps get their own low-rep prescriptions; conditioning works by time. |

## 7. Suggested order of work (after the owner agrees)

1. Add `role` (main/alternate/support/program/library) and `progressionGroup` to the contract. Then fill them from §3, as a reviewed table, not by a generator heuristic.
2. Do the family splits: plank → plank/hollow/L-sit; pushup → push-up/power push-up; handstand → handstand/headstand-support; bridge → hip/back-bridge. Also move the rollouts and FL rows.
3. Engine:
   - the stepper follows the main successor;
   - alternates rotate only inside explicit groups;
   - gates come only from main steps, with OR-groups;
   - the default pool is the fixed list in §4.
4. UI:
   - the tree draws the spine and collapses "Other ways" and "Helps with";
   - Progress shows the default progressions plus active goals;
   - the focus toggles;
   - the compact Programs list.
5. After the render review: move any clip that can't teach its movement to [lib], or hold it, as doc 28 says.

Owner decisions, 2026-10-16:
- **Default pool:** accepted as proposed in §4. The owner defers to the coordinator here.
- **Core:** more options and variations. The crunch family and the other core alternates rotate within the core slots (see §3 Core and §4).
