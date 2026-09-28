# ADR 0002 — Policy defaults and progression rules

Status: **ACCEPTED — approved by the owner on 2026-09-28 at gate G0.**
Date: 2026-09-28 (drafted and accepted same day). Companion to ADR 0001 (foundation: fork CalisthenicsMemory).
Source of every default: `docs/02-coding-specification.md` (lines quoted as §-refs).
Reminder from the spec: these are **testable heuristics, not scientifically
validated exercise prescriptions** and not medical advice.

Owner decisions (2026-09-28, verbatim):
- Cue style (A4): *"Ok"* — technique cues **off** by default.
- Stars on screen (C5): *"Ok"* — self-reported baseline shown separately from earned stars.
- Demo video (F3): *"Yes, let's create light/easy to generate videos that can be looped for the duration of the exercise."* — original, **lightweight and easy to generate**, each demo a **loop** that can run for the full exercise duration (muted, local, no trackers). F3 below is amended accordingly.

All other rows (A1–E1, B1–B4, C1–C4, C6–C8, D1, E1, F1–F2, F4–F5) were approved as drafted ("adopt as proposed"). Human approval given by the owner; not synthesized.

Legend: **Verdict** = agent recommendation for this draft. All verdicts below are
"adopt as proposed" unless a different choice is explicitly made; items marked
**(choice)** are the few places the spec leaves a real decision to the owner.

## A. Session defaults (what the app uses out of the box, all changeable in-app)

| ID | Item | Exact default (spec) | Plain meaning | Verdict |
|---|---|---|---|---|
| A1 | Workout duration | 2700 s = **45 min** (§3) | Middle of the supported range; good for a full-body circuit. | Adopt. |
| A2 | Stretch mode | **On**, areas **FULL_BODY** (§3) | Rest time is filled with stretching by default — the app's "no passive waiting" core rule. | Adopt. |
| A3 | Warm-up / cooldown | **Off** (§3) | Off until reviewed templates exist in the catalog; then the user can switch on. | Adopt. |
| A4 | Language / audio / cues | English; audio **on**; technique cues **off by default** (§3) | Audio cues are the core of a hands-free workout; technique talk is optional clutter. **(choice)** — I recommend cues **off** initially. | Adopt as recommended. |

## B. Plan boundaries (what the planner is allowed to do)

| ID | Item | Exact default (spec) | Plain meaning | Verdict |
|---|---|---|---|---|
| B1 | Supported duration | **10–90 min**, 1-minute steps (§4.1) | Below 10 min there's not enough work; above 90 the app won't promise it. | Adopt. |
| B2 | Fit-to-time rule | Never **exceed** requested time; ready plan within **60 s under** it; if shorter by more than 60 s the app **asks** you to accept the real length or change constraints — no padding, no "pretending to be 45 min" (§4.9) | Honest timer rule. | Adopt. |
| B3 | Transitions | ≤ **5 s** per actual equipment/position change; **0 s** when nothing changes (§3) | Setup time is setup time, never hidden rest. | Adopt. |
| B4 | Skill goal | Replaces **at most one** strength slot per round; explains itself when goal/equipment/focus make practice impossible (§4.5) | A skill goal is a guest, not a guest that takes over the party. | Adopt. |

## C. Progression pacing (how the app makes things harder/easier on its own)

| ID | Item | Exact default (spec) | Plain meaning | Verdict |
|---|---|---|---|---|
| C1 | Starting level | From your **self-assessment**, conservative when unknown; "unassessed" stays *unassessed* — not 5 stars, not zero (§5.1) | The app won't pretend to know your level. | Adopt. |
| C2 | What counts as one qualifying session ("exposure") | One session with the variation's **minimum number of completed work blocks**, no skipped/partial work for that exercise, no BELOW/discomfort ratings, no "easier" override. Multiple rounds = one; multiple sessions same day = one (§5.2) | The strict definition that keeps "progress" honest. | Adopt. |
| C3 | Advancing up | **3 qualifying sessions spanning ≥ 7 days** → +1 tier; first advancement also needs **7 days** in the tier; **max 1 automatic advancement per 7-day window** per exercise (§5.4) | Roughly "three solid weeks of practice before it gets harder." Deliberately conservative; it is a heuristic, not physiology. | Adopt. |
| C4 | When you don't rate a block | Missing rating → **ASSUMED/MET** (labeled as assumed); actual value you enter **below target → BELOW** even if you tapped MET; **discomfort always wins** over everything (§5.3) | Your honest input beats the app's optimism. | Adopt. |
| C5 | Stars display | Stars = **highest consecutive tiers achieved**; initial self-assessment shown **separately as "self-reported baseline"**, distinct from earned stars. **(choice)** — proposed display: baseline shown as e.g. "3/5 (your self-assessment)" next to earned "★☆☆ earned". | Keeps "what you said" vs "what you proved" visually apart. | Adopt as proposed. |
| C6 | Going easier | **1 BELOW** → streak broken, difficulty held; **2 consecutive BELOW** → app **suggests** a lower target (never auto-changes, never diagnoses fatigue) (§5.7) | One-off bad day ≠ you got weaker. | Adopt. |
| C7 | Discomfort | Sets a **persistent family-level hold** (you can't dodge it by swapping to a near-identical variation); app offers a reviewed easier alternative; the slot is blocked at preflight until you explicitly clear it (§5.8) | Safety beats progression, and "no pain" is on record. | Adopt. |
| C8 | Coming back after a break | After **14 days** without training a variation → app proposes a **lower re-entry target** and asks you to choose; **never erases** history (§5.9) | No silent demotion. | Adopt. (14 days is a heuristic — easy to tune later.) |

## D. Audio cues

| ID | Item | Exact default (spec) | Plain meaning | Verdict |
|---|---|---|---|---|
| D1 | Cue timing | Preview **5 s** before transition; countdown **3/2/1**; overlapping previews suppressed on short blocks (§7) | You always hear what's next, exactly once. | Adopt (on-device verification happens at G1). |

## E. Early completion — the design distinction the spec requires to be shown at G0

| ID | Item | Exact default (spec) | Plain meaning | Verdict |
|---|---|---|---|---|
| E1 | "Done" button | If a **reviewed early-completion stretch** exists for that exercise, it is announced at block start; an optional **Done** button jumps into that pre-planned stretch for the remaining window. The app **never requires extra reps** and **never auto-creates passive rest**. If no safe remainder action exists, the content review must flag that the interval can't be fully occupied — no silent text-only padding (§4, "Early repetition completion") | Finish early = stretch early, never sit around, never forced reps. | Adopt. |

## F. Content and media strategy (scope half of G0)

| ID | Item | Proposal | Verdict |
|---|---|---|---|
| F1 | Starter catalog | Named pack **"Starter: Home Full-Body"** — one broad pack covering: push (incline / knee / standard push-up), pull (band-assisted pull-up, band row), squat/lunge (squat, split squat), hip hinge/posterior chain (controlled 12 kg kettlebell deadlift, bridge, superman), core/holds (plank, side plank, dead bug), plus the stretch families the spec names (calf, ankle, hip, hamstring, back, chest, shoulder). See `content/catalog-plan.json`. | Adopt as draft; per-variation sources, five tiers and media are filled in during T02 research **before** G0 sign-off. |
| F2 | Expansion packs | Equipment-keyed packs (pull-up bar; 2×2.5 kg weights) added when rights + review capacity allow; count of releasable records is decided at G0, **not promised** in advance (spec §8). | Adopt. |
| F3 | Demo media | **Owner-approved direction (2026-09-28):** original, **lightweight, easy-to-generate** looping demos — each demo a short clip that **loops for the duration of the exercise**; muted, local, no trackers; open license (proposed **CC BY-SA 4.0** to match the GPL-3.0 code, or CC0). **No third-party clips** until individual rights are cleared per the media record fields (spec §8). | ACCEPTED. |
| F4 | Audio cues | Offline English cue assets bundled in the APK (spec §7); no cloud TTS during workouts. | Adopt. |
| F5 | Downloadable packs | Blocked as a release feature until a real allowlisted HTTPS endpoint exists — no pretend download button (spec §8). | Adopt. |

## Open items for the owner at G0 (the actual asks)

1. Approve A1–E1 as drafted (or name any row to change). ✅ **Approved 2026-09-28.**
2. Confirm the three **(choice)** items: A4 (technique cues off by default), C5
   (baseline-vs-earned stars display) and F3 (original demos, open license).
   ✅ **All three confirmed 2026-09-28** (F3 refined: lightweight, easy-to-generate, loop-per-exercise).
3. Approve the starter-pack scope in `content/catalog-plan.json` and the media
   strategy F3/F4 — including that **releasable record counts are set at G0**,
   not promised. ✅ **Approved 2026-09-28.**

**G0 is CLOSED (2026-09-28).** Implementation may proceed to T03.
