# 17 — UI direction "B" (owner-approved design, 2026-10-09)

Status: **owner decision.** The owner reviewed three UI directions on a design canvas, chose **B** ("Logbook": dark, numbers first, full control) and refined it with about 25 comments.
- Design canvas (private to the owner): https://claude.ai/artifact/9j78URnue1J4LApizYrCXn. Screens: B · Today, B · Round preview & edit, B · Workout, B · History, B · Progress (paths), B · Exercise detail.
- The HTML mockups are the visual reference. This file is the contract. Where a mockup and this file differ, this file wins.
- Numbers in the mockups are sample data.

## 1. Design tokens
- **Dynamic colour (Material You) is OFF.** The app uses this fixed palette, so the look is the same on every phone.
- **Dark is the default theme.** Settings keeps System / Light / Dark, but a fresh install starts in Dark. Light keeps the same structure with the light tokens.

| Token | Dark | Light | Use |
|---|---|---|---|
| background | #111416 | #F6F7F8 | screen |
| surface | #1B2024 | #FFFFFF | cards, segmented-control track, tiles, sheets |
| surfaceHigh | #2E353A | #E6E9EC | selected-but-neutral segment, borders of outlined buttons |
| divider | #252B30 | #DDE2E5 | list separators, nav top border |
| text | #ECEFF1 | #111416 | primary text |
| textSecondary | #C9D0D4 | #33393E | secondary lines |
| textMuted | #9AA3A9 | #5E676D | captions, labels |
| accent (gold) | #E9C046 | #E9C046 | primary button fill, selected segment, timers, stars, active tab |
| onAccent | #111416 | #111416 | text on gold |
| accentText on a light background | — | #B8901A | gold used as text or thin marks on white |
| accentDim | #8A7A3E | #D8C88A | secondary muscles on the body map |
| caution surface | #2A2410 / border #5C4A12 / text #F3E3B0 | #FFF4E5 / #E8C98A / #5C3A00 | caution boxes |

Contrast:
- Keep 4.5:1 for text, which the contrast test checks; update it for the new palette.
- Gold is never used as text on white; use accentText there.

**Typography:**
- **Barlow Condensed** (600/700) for screen titles, big numbers, timers and primary button labels, mostly in UPPER CASE.
- **IBM Plex Sans** (400/500/600) for everything else.
- Both are SIL OFL: bundle the TTFs in `app/src/main/res/font/` and add them to `THIRD_PARTY_NOTICES.md`.
- They are not a Gradle dependency. Fetch the font files once, from the google/fonts GitHub repo (OFL licence). If that isn't possible offline, stop and ask.

**Shapes and sizes:**
- Corner radius 12–14 dp for cards, tiles and buttons; 9 dp inside segmented controls.
- Primary buttons 54–58 dp high; every touch target ≥ 48 dp.

**Labels:**
- Every choice has a small UPPER-CASE caption above it in textMuted, letter-spaced (OBJECTIVE, EQUIPMENT, WORKOUT FORMAT, SETS, BETWEEN SETS, WARM-UP, TIME, EXERCISES).

**Icons:**
- Line icons; exercise icons come from the v3 thumbnails (V06a), never letter circles.

**Bottom navigation:** Train · Progress · History · Settings. The active tab is gold.

## 2. Screens

### 2.1 Train ("Today's workout")
The screen holds choices only, no per-exercise list. Top to bottom:
1. Title **TODAY'S WORKOUT**, with no weekday or date.
2. **OBJECTIVE**:
   - a segmented control: Body part · Skill · Routine;
   - under it, a dropdown with the choice inside the picked type: full / upper / lower / core; the skill goals; or the ready-made and saved routines (Recommended Routine, Minimalist, Starting To Stretch, the owner's own).
   
   The routine is part of the objective, not a separate title.
3. **EQUIPMENT**: a profile dropdown row (Home, Travel …, plus a "new profile" entry).
4. **WORKOUT FORMAT**: segmented control, Circuit · Pairs · Straight sets.
5. A row of two cards:
   - **SETS** (Rounds in Circuit): a − / + stepper;
   - **BETWEEN SETS**: Stretch | Rest (with the rest time).
6. **WARM-UP**: a switch row ("RR warm-up · 8 min"), on or off.
7. Two summary tiles:
   - **TIME**: "≈ 52 min", with "with warm-up" under it when the warm-up is on;
   - **EXERCISES**: e.g. "6 + 3", with a caption like "3 pairs + core triplet".
   
   Don't repeat the sets count here.
8. Bottom: **START WORKOUT** (gold, two thirds of the width) and **Workout preview** (outlined, one third), which opens 2.2.

### 2.2 Round preview and edit (the Preview screen)
- Header with back and **ROUND PREVIEW**.
- A segmented control **Set 1 · Set 2 · Set 3** shows **one round at a time**.
  - The caption says "Changes apply to all sets"; per-set changes are the "Detailed edit" (D3/D6).
  - Time per set on the right.
- **Exercise row** (surface card), with these elements:
  - slot tag in gold (1A · PULL);
  - thumbnail;
  - name;
  - **− level +** stepper with the target number (D3);
  - swap button, which opens the swap sheet (D1).
- **Between rows:** a slim break row: "Stretch · chest doorway · 0:45 · change" (or the rest time) and a small dashed **+** to insert an exercise or stretch there (R14).
- Removing an exercise: swipe or long-press, with confirm.
- Bottom: **Save as routine** (outlined) and **START WORKOUT** (gold).

### 2.3 Workout (session)
- **Full-screen clip on a white background.** Clips are made on pure white (see doc 16).
- **Header:** transparent over the clip, in dark text:
  - the big Barlow line **PAIR 1 · SET 2 OF 3** (or ROUND 2 OF 4 in Circuit);
  - set markers under it (filled in accentText gold);
  - elapsed time small on the right;
  - the block title (e.g. REST · CHEST DOORWAY STRETCH);
  - "Next: …".
- **Bottom sheet** (dark surface #111416, light text), top to bottom:
  1. the **rest timer, centred and large** (Barlow ~60 sp, gold), with REST above it and a full-width horizontal progress bar under it;
  2. the exercise and set ("Pull-up negatives · set 2") and its target;
  3. **− big number +** (the logger);
  4. a row **Pause · DONE · Skip**, with Done in the **middle**, gold and a little wider. The label is just "DONE", never "Save", and the rep count isn't repeated on the button.
- **No round timer.** No Too hard / Too easy / Pain chips during the workout.

### 2.4 End screen
- Per exercise: the per-round editor (V04b) plus **Too hard · Too easy · Pain**. This is where those ratings live now.
- D7 still works: two "Too easy" in a row suggest a level up, never automatically.

### 2.5 History
- What was "Stats" in the mockup: title **HISTORY**;
- summary tiles (sessions this week, level-ups this month, minutes);
- a per-exercise chart of the lowest set per session, with the gate noted;
- a list of all progressions with the current best;
- the existing week strip, workout list and per-round detail (V04b), restyled.

### 2.6 Progress (paths)
1. Title **PROGRESS**.
2. Below it, a full-width, prominent **By type | By skill** segmented control (R24).
3. Family chips: Pull, Row, Push, Dip, Squat, Hinge, Core, Stretch. The selected chip is gold-outlined, not filled.
4. **The tree, vertical**, top = easier (R23):
   - Nodes show **only the name** (and stars) under a **horizontal tile**:
     - the left third holds the **level number** (✓ when mastered, a lock when locked);
     - the right two thirds hold the **exercise icon**.
   - States:
     - mastered: gold number cell, gold icon;
     - current: gold outline;
     - locked: grey.
   - **Branching is visible:** after the main chain, a connector splits into the alternative paths side by side (e.g. Weighted · L-sit pull-up · Typewriter → Archer).
5. **First tap** on a node selects it (ring) and opens a **panel above the tab bar** with:
   - the details (status, level, current numbers vs the gate, what unlocks it);
   - an **OPEN EXERCISE DETAIL** button;
   - × to close.
   
   No sub-text in the tree itself.

### 2.7 Exercise detail (sub-page of Progress)
- Breadcrumb "PROGRESS › PULL · STEP 3 OF 5", back arrow, name, and stars.
- The clip on white, next to the front and back body map: primary muscles gold, secondary accentDim (V08).
- The 5 levels with done / now / next; UNLOCKS / NEEDS; the caution box.
- **No action buttons.** No swap and no practice here.

## 3. Rules from the owner's comments
- Captions on every choice; no redundant information (no weekday, no repeated set counts, no rep count on Done).
- "Done", not "Save", for logging a set.
- Primary action in gold; secondary actions outlined.
- Previews and editing live on their own screen, not on Train.
- During a workout show only what is needed now; ratings come after the workout.
