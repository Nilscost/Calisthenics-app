# 15 — Batch 3: exercise and stretch library expansion (tasks L00–L13)

Status: **approved by the owner on 2026-10-09 for execution after V28.** It is the same executor and uses the same rules as `docs/14-executor-handoff.md`: one task per commit, tests first, verify, and a screenshot review for every UI change. All numbers are DRAFT.

Source: research report `/Users/nils/Documents/hermes/Apps/reports/Calisthenics exercise library expansion.md` (outside the repo; read it, but don't copy its wording into the app). The report's section "Prioritized catalog changes" is the basis for these tasks. Licence rule:
- numbers and progression orders may be reused;
- **write all text in our own words**;
- never bundle text or media from ExRx, ExerciseDB, GMB, Overcoming Gravity or Convict Conditioning;
- free-exercise-db (Unlicense) may be used as a reference;
- wger items need a per-item licence and attribution.

Overlap with v2:
- V07 (≈18 stretches), V21b (RR/Minimalist content) and V25 (C-E gaps) come first;
- the L-tasks extend and correct them;
- they never undo an owner decision: chair dips OK, support hold 30 s, unlogged rep-range sets = same as last time.

**Equipment assumption to show the owner:**
- the report assumes a doorway pull-up bar, a band, a 12 kg kettlebell, 2 × 2.5 kg dumbbells, a chair, a wall and a mat;
- every exercise must still be gated on the profile's equipment;
- bodyweight-only must stay complete (D9).

| # | Task | Notes / done when |
|---|---|---|
| L00 | **Verify against the live r/bodyweightfitness wiki** | **DONE (2026-10-09)** by the reviewer from the owner's saved pages: `docs/research/rr-live-check.md` (RR, all progression pages, Minimalist, Starting To Stretch). Use it as the source. |
| L01 | Tier profiles + source/draft flag | A `tierProfile` field (R, RU, C, H30, H60, N, E, M; report §"One gate, five tiers"), appended last with a default. The RR rule is one function (gate 3×8 or 30 s, reset to 3×5). Each exercise also gets a `draft`/`source` field. Tests: profiles produce the report's tier values; old catalogs decode. |
| L02 | Push-up chain reorder (Ebben 2011) | wall → high incline → knee ‖ low incline → full → diamond ‖ chair decline → archer → one-arm progression; pseudo-planche is its own branch. Migrate existing levels by exercise id (no star loss). Test: order + migration. |
| L03 | Pull-up path without a band | dead hang, scapular pull, arch hang (timed exercise + a rep-based warm-up variant), flexed-arm hang, negatives, chair-assisted pull-up; chin-up ‖ pull-up as parallel; flexed-hang placement test in the questionnaire. |
| L04 | Rows without a low bar | doorframe/towel vertical row and table row (with safety copy: sturdy frame, slow), KB one-arm row. Inverted row needs the low-bar equipment; front lever gated on 3×8 wide rows. |
| L05 | Single-leg squat restructure | beginner shrimp ‖ band-assisted pistol → intermediate shrimp ‖ box pistol ‖ counterbalance pistol (marked as easier) → advanced shrimp ‖ pistol; knee-to-wall ankle test as info. |
| L06 | Hinge split into four paths | bridge, SL-RDL, sliding leg curl, Nordic (band from the bar, couch anchor warning, low-volume defaults); reverse hyperextension on table/bed edge. |
| L07 | L-sit tuck steps | foot-supported → one-foot → tuck → advanced tuck → one-leg extended → full, H60 accumulation tiers. |
| L08 | Core planes | dead bug before plank; tuck → one-leg → full hollow → hollow rocks; banded Pallof hold/press (stance rule); KB suitcase carry; side plank → leg-raise → Copenhagen short → long; superman → arch hold → arch rocks. |
| L09 | Back-bridge ladder | glute bridge → table bridge → chair (box) bridge → head-supported → **feet-elevated / wall walk-down (middle step)** → full bridge. |
| L10 | Dips detail | chair dips with a 90° depth cap and "stop on front-shoulder pain" copy; support hold 3×30 s; chair-dip negatives. |
| L11 | Stretch library to 24 | Each stretch tagged between-sets (BS) or cool-down (CD). The planner puts only BS stretches between sets, ≤30 s each, and keeps static holds before strength work ≤60 s per muscle. Sleeper, Jefferson curl and German hang are opt-in. Each new stretch gets a clip and thumbnail through the v3 pipeline. |
| L11b | "Starting To Stretch" session (r/flexibility) | A ready-made stretch-only routine with the 10 stretches and the bump/hold protocol (see `docs/research/rr-live-check.md`, last section). It is listed under Ready-made routines and offered as an optional post-workout cool-down; never before strength work. Planner test: blocks, order, total of about 30 min; upper/lower half option. Screenshot review of the routine preview and a stretch block. |
| L12 | Warm-up as RAMP blocks | The RR's eight items in raise/activate/mobilise/potentiate order. Gated items unlock from ladder state (arch hang after negatives, support hold after negative dips, etc.). There is a wrist block before handstand/push days. |
| L13 | Calves + tibialis mini-ladder | Profile E; the 2.5 kg dumbbells as load. Then a **release R6**: full verify, CI, screenshots, `owner-check-r6.md`, `app-debug-R6.apk`, and `docs/review/draft-numbers.md` regenerated. |

Every new exercise needs: equipment, muscles, cautions, tiers, our own instructions, a clip and a thumbnail (v3 pipeline), and a place in a Progress tab. The catalog tests (no dead ends, equipment, counts) must be updated.
