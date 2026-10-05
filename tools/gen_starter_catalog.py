#!/usr/bin/env python3
"""Generates content/starter/catalog.json (DRAFT) in the T05 domain model.

All tier numbers are PRODUCT_HEURISTIC (authored here, not copied from a book).
Books are cited only for the ORDER of the progression ladder and the movement
choice. Nothing here is REVIEWED; the owner reviews at G2/G3.
Run:  python3 tools/gen_starter_catalog.py
"""
import json, pathlib

OUT = pathlib.Path(__file__).resolve().parent.parent / "content" / "starter" / "catalog.json"

S_CC = "convict-conditioning-wade"
S_OG = "overcoming-gravity-2-low"
S_GB = "building-the-gymnastic-body-sommer"
S_YG = "you-are-your-own-gym-lauren"
S_NG = "never-gymless-enamait"

def rnd5(x): return int(5 * round(x / 5))

def reps_tiers(targets, rec=(60, 60, 60, 60, 60), stretch=None):
    tiers = []
    for i, t in enumerate(targets, 1):
        d = {"index": i, "target": {"type": "REPS", "value": t},
             "workWindowSeconds": max(30, rnd5(t * 6 + 15)), "minRecoverySeconds": rec[i - 1]}
        if stretch: d["earlyCompletionStretchId"] = stretch
        tiers.append(d)
    return tiers

def hold_tiers(targets, rec=(60, 60, 60, 60, 60), stretch=None):
    tiers = []
    for i, t in enumerate(targets, 1):
        d = {"index": i, "target": {"type": "HOLD_SECONDS", "value": t},
             "workWindowSeconds": t + 25, "minRecoverySeconds": rec[i - 1]}
        if stretch: d["earlyCompletionStretchId"] = stretch
        tiers.append(d)
    return tiers

NO_EQ = [{}]
def need(eid, **kw): return {"equipmentId": eid, **kw}

STRETCH_NOTE = "Stretch gently; mild tension is normal, pain is not. Stop if you feel sharp or pinching pain."
CAUTION_STRENGTH = "Stop the set on sharp or joint pain; do not push through it."

variations, policies = [], []

def strength(id, name, family, patterns, areas, kind, position, instructions, cues, tiers,
             sources, stretches, eq=NO_EQ, unilateral=False, rank=0, nxt=(), prereq=None,
             cautions=(CAUTION_STRENGTH,)):
    variations.append({
        "id": id, "familyId": family, "name": name, "patterns": patterns, "areas": areas,
        "kind": kind, "equipmentAlternatives": eq, "unilateral": unilateral, "position": position,
        "instructions": instructions, "formCues": cues, "cautions": list(cautions),
        "difficultyRank": rank, "sourceIds": sources, "reviewState": "DRAFT",
        "progressionPolicyId": "pol-" + id, "compatibleStretchIds": stretches,
    })
    pol = {"id": "pol-" + id, "variationId": id, "version": 1, "tiers": tiers,
           "evidenceSourceIds": sources, "policyKind": "PRODUCT_HEURISTIC", "approved": False}
    if nxt: pol["nextVariationIds"] = list(nxt)
    if prereq: pol["prerequisiteRule"] = prereq
    policies.append(pol)

def stretch(id, name, areas, seconds, uni, position, instructions, eq=NO_EQ, kind="STRETCH"):
    v = {"id": id, "familyId": id, "name": name,
         "patterns": ["STRETCH" if kind == "STRETCH" else "MOBILITY"],
         "kind": kind, "unilateral": uni, "position": position, "defaultSeconds": seconds,
         "equipmentAlternatives": eq, "instructions": instructions, "cautions": [STRETCH_NOTE],
         "sourceIds": [S_OG, S_GB], "reviewState": "DRAFT"}
    if areas: v["stretchAreas"] = areas
    variations.append(v)

def vt(variation, tier): return {"variationTierMet": {"variationId": variation, "tier": tier}}

# ---------------- stretches (compatibleStretchIds targets) ----------------
stretch("stretch-calf-wall", "Calf Stretch (wall)", ["CALF"], 30, True, "standing",
        ["Face a wall, hands on it at chest height.", "Step one foot back, heel flat, back leg straight.",
         "Lean the hips toward the wall until you feel a stretch in the back-leg calf.", "Hold, then switch sides."],
        eq=[{"capabilities": ["wall"]}])
stretch("stretch-hip-flexor", "Hip Flexor Lunge Stretch", ["HIP"], 30, True, "kneeling",
        ["Kneel on one knee, other foot flat in front (mat under the knee).", "Tuck the pelvis under and shift forward slightly.",
         "Feel the stretch at the front of the back-leg hip; keep the ribs down.", "Hold, then switch sides."])
stretch("stretch-hamstring", "Hamstring Stretch (seated)", ["HAMSTRING"], 30, False, "seated",
        ["Sit on the floor with legs straight ahead (a little knee bend is fine).", "Hinge forward from the hips with a long back.",
         "Reach toward your shins until you feel a stretch behind the thighs.", "Breathe slowly; do not bounce."])
stretch("stretch-back", "Child's Pose / Cat-Cow", ["BACK"], 30, False, "kneeling",
        ["Start on hands and knees.", "Sit the hips back toward the heels and reach the arms forward (child's pose).",
         "Breathe into the back; optionally alternate round and arch (cat-cow).", "Stay relaxed; no forcing."])
stretch("stretch-chest-door", "Chest Stretch (doorway / wall)", ["CHEST"], 30, True, "standing",
        ["Place one forearm on a wall or door frame, elbow at about shoulder height.", "Turn the body gently away from the wall.",
         "Feel the stretch across the chest and front of the shoulder.", "Hold, then switch sides."],
        eq=[{"capabilities": ["wall"]}])
stretch("stretch-shoulder", "Shoulder Stretch (cross-body)", ["SHOULDER"], 30, True, "standing",
        ["Bring one arm across the chest.", "Use the other forearm to hold it close, shoulders relaxed.",
         "Feel the stretch in the back of the shoulder.", "Hold, then switch sides."])
stretch("stretch-ankle-mobility", "Ankle Mobility (knee-to-wall)", [], 30, True, "standing",
        ["Stand facing a wall, one foot a hand-width back from it.", "Bend the front knee toward the wall, heel flat.",
         "Move slowly in and out within a pain-free range.", "Switch sides."],
        eq=[{"capabilities": ["wall"]}], kind="MOBILITY")

UP = ["stretch-chest-door", "stretch-shoulder"]
LOW = ["stretch-hip-flexor", "stretch-hamstring", "stretch-calf-wall"]
CORE_ST = ["stretch-back", "stretch-hip-flexor"]

# ---------------- push ----------------
strength("pushup-incline", "Incline Push-Up", "pushup", ["PUSH_HORIZONTAL"], ["UPPER_BODY"], "REPS", "floor",
         ["Hands on a wall or a stable raised surface, body in one straight line.", "Lower the chest toward the surface, elbows about 45° from the body.",
          "Press back up to straight arms without sagging the hips.", "Count only full-range reps."],
         ["Straight line head to heels", "Elbows about 45°", "Full range"],
         reps_tiers([6, 8, 10, 12, 15], stretch="stretch-chest-door"), [S_CC, S_YG, S_NG], UP,
         eq=[{"capabilities": ["wall"]}, {"needs": [need("chair", suitability=["stable"])]}],
         rank=10, nxt=["pushup-standard"])
strength("pushup-knee", "Knee Push-Up", "pushup", ["PUSH_HORIZONTAL"], ["UPPER_BODY", "CORE"], "REPS", "floor",
         ["Kneel on a mat, hands under the shoulders, body straight from head to knees.", "Lower the chest toward the floor, elbows about 45°.",
          "Press up to straight arms; keep the hips in line.", "Count only full-range reps."],
         ["Straight line head to knees", "Do not sag the hips"],
         reps_tiers([5, 7, 9, 11, 14], stretch="stretch-chest-door"), [S_CC, S_YG, S_NG], UP,
         eq=[{}], rank=20, nxt=["pushup-standard"])
strength("pushup-standard", "Standard Push-Up", "pushup", ["PUSH_HORIZONTAL"], ["UPPER_BODY", "CORE"], "REPS", "floor",
         ["Plank position, hands slightly wider than the shoulders.", "Lower the chest to just above the floor, elbows about 45°.",
          "Press up to full lockout with a straight body.", "Count only full-range reps."],
         ["Brace the core and squeeze the glutes", "Chest to just above the floor", "Full lockout"],
         reps_tiers([4, 6, 8, 10, 12], stretch="stretch-chest-door"), [S_CC, S_OG, S_YG, S_NG], UP,
         eq=[{}], rank=30,
         prereq={"allOf": [[vt("pushup-incline", 5), vt("pushup-knee", 5)]]})

# ---------------- pull ----------------
strength("row-band", "Band Row (seated)", "row", ["PULL_HORIZONTAL"], ["UPPER_BODY"], "REPS", "seated",
         ["Sit on the floor, legs forward, band anchored securely in front of you at hand height.", "Pull the handles toward the lower ribs, elbows close to the body.",
          "Squeeze the shoulder blades, then return slowly.", "Keep the back long, no leaning back."],
         ["Pull elbows back, not shoulders up", "Slow return"],
         reps_tiers([8, 10, 12, 14, 16]), [S_OG, S_YG, S_NG], UP,
         eq=[{"needs": [need("resistance-band", suitability=["stable-anchor"])]}], rank=10)
strength("pullup-band-assisted", "Band-Assisted Pull-Up", "pullup", ["PULL_VERTICAL"], ["UPPER_BODY"], "REPS", "hang",
         ["Loop a band over the bar and place a knee or foot in it.", "Hang with straight arms, shoulders pulled away from the ears.",
          "Pull until the chin clears the bar, then lower slowly with control.", "Use the lightest band that still allows clean reps."],
         ["Start from a controlled hang", "Chin over the bar", "Slow lowering"],
         reps_tiers([3, 4, 5, 6, 8], rec=(60, 60, 60, 60, 60)), [S_CC, S_OG, S_YG], UP,
         eq=[{"needs": [need("pullup-bar"), need("resistance-band")]}], rank=20,
         cautions=(CAUTION_STRENGTH, "Check that the bar and band are secure before every set."))

# ---------------- squat / lunge ----------------
strength("squat-air", "Air Squat", "squat", ["SQUAT"], ["LOWER_BODY"], "REPS", "standing",
         ["Feet about shoulder-width, toes slightly out.", "Sit the hips back and down until thighs are about parallel (or as deep as comfortable).",
          "Keep the heels down and the chest up.", "Stand tall and squeeze the glutes at the top."],
         ["Knees track over the toes", "Heels stay down", "Chest up"],
         reps_tiers([8, 10, 12, 15, 20]), [S_CC, S_YG, S_NG], LOW, rank=10, nxt=["split-squat"])
strength("split-squat", "Split Squat (per side)", "squat", ["LUNGE"], ["LOWER_BODY"], "REPS", "standing",
         ["Step one foot forward, the other back, feet hip-width apart.", "Lower the back knee toward the floor, front knee over the mid-foot.",
          "Push through the front foot to stand. Finish all reps, then switch sides."],
         ["Torso upright", "Front heel stays down", "Control the lowering"],
         reps_tiers([5, 6, 8, 10, 12]), [S_YG, S_NG, S_OG], LOW, unilateral=True, rank=20,
         prereq={"allOf": [[vt("squat-air", 3)]]})

# ---------------- hinge / posterior chain ----------------
strength("glute-bridge", "Glute Bridge", "bridge", ["HINGE"], ["LOWER_BODY"], "REPS", "supine",
         ["Lie on your back, knees bent, feet flat hip-width apart.", "Press through the heels and lift the hips until the body is straight from shoulders to knees.",
          "Squeeze the glutes for a moment at the top, then lower slowly."],
         ["Ribs down, no arching the lower back", "Squeeze at the top"],
         reps_tiers([8, 10, 12, 15, 18]), [S_CC, S_YG, S_NG], ["stretch-hip-flexor", "stretch-back"], rank=10)
strength("superman-hold", "Superman Hold", "superman", ["HINGE"], ["LOWER_BODY", "CORE"], "HOLD", "prone",
         ["Lie face down with arms extended overhead.", "Lift arms, chest and legs a few centimetres off the floor, neck neutral.",
          "Hold, breathing steadily, then lower with control."],
         ["Look at the floor, long neck", "Small lift is enough"],
         hold_tiers([10, 15, 20, 25, 30]), [S_OG, S_YG], ["stretch-back", "stretch-hip-flexor"], rank=10)
strength("kettlebell-deadlift", "Controlled Kettlebell Deadlift", "deadlift", ["HINGE"], ["LOWER_BODY"], "REPS", "standing",
         ["Stand with feet hip-width, kettlebell between the feet.", "Push the hips back with a flat back and grip the handle.",
          "Drive through the feet to stand tall; do not swing the weight.", "Lower with control back to the floor."],
         ["Flat back", "Hips back, not squat down", "Controlled tempo, no swinging"],
         reps_tiers([6, 8, 10, 12, 15]), [S_NG, S_YG], ["stretch-hamstring", "stretch-hip-flexor"],
         eq=[{"needs": [need("kettlebell", minMassGrams=8000)]}], rank=20,
         cautions=(CAUTION_STRENGTH, "Controlled tempo only; no ballistic or swinging work."))

# ---------------- core ----------------
strength("plank", "Front Plank", "plank", ["CORE_ANTI_EXTENSION"], ["CORE"], "HOLD", "floor",
         ["Forearms on the floor, elbows under the shoulders.", "Body in one straight line; squeeze the glutes and brace the core.",
          "Hold without letting the hips sag or pike."],
         ["Ribs down", "Squeeze glutes", "Breathe steadily"],
         hold_tiers([15, 20, 30, 40, 50]), [S_OG, S_GB, S_YG], CORE_ST, rank=10)
strength("side-plank", "Side Plank (per side)", "side-plank", ["CORE_ANTI_LATERAL"], ["CORE"], "HOLD", "floor",
         ["Lie on one side, forearm under the shoulder, legs stacked (or feet staggered).", "Lift the hips so the body forms a straight line.",
          "Hold, then switch sides."],
         ["Hips high", "Body in one line"],
         hold_tiers([10, 15, 20, 30, 40]), [S_OG, S_YG], CORE_ST, unilateral=True, rank=20)
strength("dead-bug", "Dead Bug", "dead-bug", ["CORE_ANTI_EXTENSION"], ["CORE"], "REPS", "supine",
         ["Lie on your back, arms up, hips and knees at 90°.", "Lower the opposite arm and leg slowly toward the floor, lower back flat.",
          "Return and alternate sides. One rep = both sides."],
         ["Lower back stays flat", "Slow and controlled"],
         reps_tiers([6, 8, 10, 12, 14]), [S_OG, S_YG], CORE_ST, rank=10)

# ---------------- progression chains toward the hard skills (DRAFT numbers) ----------------
BAR = [{"needs": [need("pullup-bar")]}]
WALL = [{"capabilities": ["wall"]}]
HIGH_BAR = [{"needs": [need("pullup-bar"), need("high-bar")]}]  # hang with feet clear and get above it
def ex(id, name, fam, pat, area, kind, pos, steps, tiers, src, st, eq=NO_EQ, rank=0, nxt=(), prereq=None, uni=False, cues=None, cautions=(CAUTION_STRENGTH,)):
    strength(id, name, fam, [pat], area, kind, pos, steps, cues or ["Controlled tempo", "Full range or clean position", "Stop if form breaks"],
             tiers, src, st, eq=eq, unilateral=uni, rank=rank, nxt=nxt, prereq=prereq, cautions=cautions)
U, L, C = ["UPPER_BODY"], ["LOWER_BODY"], ["CORE"]
# push (horizontal)
ex("pushup-feet-elevated", "Feet-Elevated Push-Up", "pushup", "PUSH_HORIZONTAL", [ "UPPER_BODY", "CORE"], "REPS", "floor",
   ["Feet on a stable chair or step, hands on the floor, body in one straight line.", "Lower the chest to the floor, elbows about 45 degrees from the body.", "Press to straight arms without sagging the hips."],
   reps_tiers([4, 6, 8, 10, 12], stretch="stretch-chest-door"), [S_CC, S_OG, S_YG], UP, eq=[{"needs": [need("chair", suitability=["stable"])]}], rank=35, nxt=["pushup-diamond"], prereq={"allOf": [[vt("pushup-standard", 4)]]})
ex("pushup-diamond", "Diamond Push-Up", "pushup", "PUSH_HORIZONTAL", U, "REPS", "floor",
   ["Plank position, hands together under the chest, thumbs and index fingers forming a diamond.", "Lower the chest to the hands, elbows close to the body.", "Press to full lockout."],
   reps_tiers([4, 6, 8, 10, 12], stretch="stretch-chest-door"), [S_CC, S_YG], UP, rank=40, nxt=["pushup-archer"], prereq={"allOf": [[vt("pushup-feet-elevated", 3)]]})
ex("pushup-archer", "Archer Push-Up (per side)", "pushup", "PUSH_HORIZONTAL", U, "REPS", "floor",
   ["Wide hand position, hands turned slightly out.", "Lower toward one hand while the other arm stays straight.", "Press back up, alternate sides. Count each side."],
   reps_tiers([3, 4, 6, 8, 10], stretch="stretch-chest-door"), [S_CC, S_OG], UP, rank=50, nxt=["pushup-one-arm-negative"], prereq={"allOf": [[vt("pushup-diamond", 3)]]})
ex("pushup-one-arm-negative", "One-Arm Push-Up Negative (per side)", "pushup", "PUSH_HORIZONTAL", U, "REPS", "floor",
   ["Feet wide, one hand under the chest, other hand behind the back.", "Lower slowly for about 4 seconds to just above the floor.", "Reset with both hands; repeat, then switch sides."],
   reps_tiers([2, 3, 4, 5, 6], stretch="stretch-chest-door"), [S_CC, S_OG], UP, uni=True, rank=60, nxt=["pushup-one-arm"], prereq={"allOf": [[vt("pushup-archer", 3)]]},
   cautions=(CAUTION_STRENGTH, "Stop if the wrist or elbow hurts."))
ex("pushup-one-arm", "One-Arm Push-Up (per side)", "pushup", "PUSH_HORIZONTAL", U, "REPS", "floor",
   ["Feet wide, one hand under the chest, other hand behind the back.", "Lower with the body square to the floor.", "Press up; finish a side, then switch."],
   reps_tiers([1, 2, 3, 4, 5], stretch="stretch-chest-door"), [S_CC, S_OG], UP, uni=True, rank=70, prereq={"allOf": [[vt("pushup-one-arm-negative", 4)]]})
# vertical push -> handstand push-up
ex("pike-pushup", "Pike Push-Up", "hspu", "PUSH_VERTICAL", U, "REPS", "floor",
   ["Hips high, body in an upside-down V, hands shoulder-width.", "Bend the elbows and lower the top of the head toward the floor.", "Press back to straight arms."],
   reps_tiers([4, 6, 8, 10, 12], stretch="stretch-shoulder"), [S_CC, S_OG, S_YG], UP, rank=10, nxt=["pike-pushup-elevated"], prereq={"allOf": [[vt("pushup-standard", 3)]]})
ex("pike-pushup-elevated", "Elevated Pike Push-Up", "hspu", "PUSH_VERTICAL", U, "REPS", "floor",
   ["Feet on a stable chair or step, hips high, hands shoulder-width.", "Lower the head between the hands.", "Press up to straight arms."],
   reps_tiers([3, 5, 7, 9, 12], stretch="stretch-shoulder"), [S_CC, S_OG], UP, eq=[{"needs": [need("chair", suitability=["stable"])]}], rank=20, nxt=["wall-handstand-hold"], prereq={"allOf": [[vt("pike-pushup", 4)]]})
ex("wall-handstand-hold", "Wall Handstand Hold (chest to wall)", "hspu", "PUSH_VERTICAL", U, "HOLD", "inverted",
   ["Walk the feet up a wall from plank until the chest faces the wall.", "Arms straight, shoulders pushed up toward the ears.", "Hold with a tight body; walk back down to leave."],
   hold_tiers([10, 20, 30, 45, 60], stretch="stretch-shoulder"), [S_OG, S_GB, S_CC], UP, eq=WALL, rank=30, nxt=["hspu-wall-negative"], prereq={"allOf": [[vt("pike-pushup-elevated", 3)]]},
   cautions=(CAUTION_STRENGTH, "Inverted work: clear the space, build up gradually, stop if dizzy."))
ex("hspu-wall-negative", "Wall Handstand Push-Up Negative", "hspu", "PUSH_VERTICAL", U, "REPS", "inverted",
   ["Kick up to a wall handstand, belly to the wall.", "Lower slowly (about 4 seconds) until the head touches a folded mat.", "Come down safely and repeat."],
   reps_tiers([2, 3, 4, 5, 6], stretch="stretch-shoulder"), [S_CC, S_OG], UP, eq=[{"capabilities": ["wall"], "needs": [need("mat")]}], rank=40, nxt=["hspu-wall"], prereq={"allOf": [[vt("wall-handstand-hold", 3)]]},
   cautions=(CAUTION_STRENGTH, "Inverted work: use a mat under the head."))
ex("hspu-wall", "Wall Handstand Push-Up", "hspu", "PUSH_VERTICAL", U, "REPS", "inverted",
   ["Wall handstand, belly to the wall, hands shoulder-width.", "Lower the head to a folded mat.", "Press back to full lockout."],
   reps_tiers([1, 2, 3, 5, 8], stretch="stretch-shoulder"), [S_CC, S_OG], UP, eq=[{"capabilities": ["wall"], "needs": [need("mat")]}], rank=50,
   prereq={"allOf": [[vt("hspu-wall-negative", 4)], [vt("wall-handstand-hold", 4)]]}, cautions=(CAUTION_STRENGTH, "Inverted work: use a mat under the head."))
# pull -> muscle-up
ex("pullup-full", "Pull-Up", "pullup", "PULL_VERTICAL", U, "REPS", "hang",
   ["Hang from the bar, hands just wider than the shoulders.", "Pull until the chin clears the bar.", "Lower under control to straight arms."],
   reps_tiers([1, 3, 5, 8, 10], rec=(60, 60, 60, 60, 60)), [S_CC, S_OG, S_YG], UP, eq=BAR, rank=30, nxt=["pullup-chest-to-bar"], prereq={"allOf": [[vt("pullup-band-assisted", 4)]]})
ex("pullup-chest-to-bar", "Chest-to-Bar Pull-Up", "pullup", "PULL_VERTICAL", U, "REPS", "hang",
   ["Hang, then pull explosively but controlled so the chest reaches the bar.", "Lean back slightly at the top.", "Lower slowly."],
   reps_tiers([1, 3, 5, 6, 8], rec=(60, 60, 60, 60, 60)), [S_OG, S_GB], UP, eq=BAR, rank=40, nxt=["muscle-up-bar"], prereq={"allOf": [[vt("pullup-full", 4)]]})
ex("muscle-up-bar", "Bar Muscle-Up", "pullup", "PULL_VERTICAL", U, "REPS", "hang",
   ["False grip or strong overgrip, pull the bar to the lower chest.", "Transition the elbows over the bar quickly.", "Press to straight arms on top, then lower with control."],
   reps_tiers([1, 2, 3, 4, 5], rec=(60, 60, 60, 60, 60)), [S_OG, S_GB], UP, eq=HIGH_BAR, rank=50, prereq={"allOf": [[vt("pullup-chest-to-bar", 4)]]},
   cautions=(CAUTION_STRENGTH, "Check the bar is secure; skill work, consider coaching."))
# squat -> pistol
ex("split-squat-bulgarian", "Bulgarian Split Squat (per side)", "squat", "LUNGE", L, "REPS", "standing",
   ["Back foot on a stable chair behind you, front foot a stride ahead.", "Lower until the front thigh is about parallel.", "Drive up through the front heel; finish a side, then switch."],
   reps_tiers([5, 6, 8, 10, 12]), [S_YG, S_NG], LOW, eq=[{"needs": [need("chair", suitability=["stable"])]}], uni=True, rank=30, nxt=["squat-shrimp"], prereq={"allOf": [[vt("split-squat", 3)]]})
ex("squat-shrimp", "Shrimp Squat (per side)", "squat", "LUNGE", L, "REPS", "standing",
   ["Stand on one leg, hold the other foot behind you.", "Lower until the back knee touches the floor lightly.", "Stand back up on the working leg."],
   reps_tiers([2, 3, 5, 6, 8]), [S_OG, S_YG], LOW, uni=True, rank=40, nxt=["squat-pistol-assisted"], prereq={"allOf": [[vt("split-squat-bulgarian", 4)]]})
ex("squat-pistol-assisted", "Assisted Pistol Squat (per side)", "squat", "SQUAT", L, "REPS", "standing",
   ["Hold a door frame or post lightly for balance.", "Lower on one leg with the other leg straight out in front.", "Stand up using as little help as possible."],
   reps_tiers([2, 3, 5, 6, 8]), [S_CC, S_OG, S_NG], LOW, uni=True, rank=50, nxt=["squat-pistol"], prereq={"allOf": [[vt("squat-shrimp", 3)]]})
ex("squat-pistol", "Pistol Squat (per side)", "squat", "SQUAT", L, "REPS", "standing",
   ["Stand on one leg, other leg straight in front.", "Lower to full depth with the heel down.", "Stand up with control; switch sides."],
   reps_tiers([1, 2, 3, 5, 6]), [S_CC, S_OG, S_NG], LOW, uni=True, rank=60, prereq={"allOf": [[vt("squat-pistol-assisted", 4)]]})
# bridge
ex("bridge-single-leg", "Single-Leg Glute Bridge (per side)", "bridge", "HINGE", L, "REPS", "supine",
   ["Lie on your back, one foot flat, other leg straight up.", "Press through the heel to lift the hips level.", "Lower slowly; finish a side, then switch."],
   reps_tiers([5, 6, 8, 10, 12]), [S_CC, S_YG], ["stretch-hip-flexor", "stretch-back"], uni=True, rank=20, nxt=["bridge-back"], prereq={"allOf": [[vt("glute-bridge", 4)]]})
ex("bridge-back", "Full Back Bridge", "bridge", "HINGE", L, "HOLD", "supine",
   ["Lie on your back, hands by the ears, feet flat.", "Press up so arms and legs straighten and the back arches evenly.", "Hold, breathing steadily; lower slowly."],
   hold_tiers([5, 10, 15, 20, 30]), [S_CC, S_GB], ["stretch-hip-flexor", "stretch-back"], rank=30, prereq={"allOf": [[vt("bridge-single-leg", 3)]]},
   cautions=(CAUTION_STRENGTH, "Needs shoulder and spine mobility; stop on any pinching."))
# core
ex("hollow-hold", "Hollow Body Hold", "plank", "CORE_ANTI_EXTENSION", C, "HOLD", "supine",
   ["Lie on your back, press the lower back into the floor.", "Lift shoulders and straight legs a few centimetres, arms by the ears or sides.", "Hold; make it easier by bending the knees."],
   hold_tiers([10, 15, 20, 30, 40]), [S_GB, S_OG], CORE_ST, rank=20, prereq={"allOf": [[vt("plank", 4)]]}, nxt=["l-sit-floor"])
ex("l-sit-floor", "L-Sit (floor)", "plank", "CORE_ANTI_EXTENSION", C, "HOLD", "seated",
   ["Sit with straight legs, hands beside the hips.", "Press down, lift the hips and legs off the floor.", "Hold with legs straight and locked."],
   hold_tiers([5, 10, 15, 20, 30]), [S_OG, S_GB, S_CC], CORE_ST, rank=30, nxt=["v-sit-floor"], prereq={"allOf": [[vt("hollow-hold", 3)]]})
ex("v-sit-floor", "V-Sit (floor)", "plank", "CORE_ANTI_EXTENSION", C, "HOLD", "seated",
   ["From an L-sit, lift the legs higher while leaning back slightly.", "Keep the arms straight and the legs locked.", "Hold; come down with control."],
   hold_tiers([3, 5, 8, 12, 15]), [S_OG, S_GB], CORE_ST, rank=40, prereq={"allOf": [[vt("l-sit-floor", 4)]]})
ex("leg-raise-lying", "Lying Leg Raise", "legraise", "CORE_ANTI_EXTENSION", C, "REPS", "supine",
   ["Lie flat, lower back pressed down, legs straight.", "Raise legs to vertical, lower slowly without arching.", "Keep the movement slow."],
   reps_tiers([6, 8, 10, 12, 15]), [S_CC, S_YG], CORE_ST, rank=20, nxt=["v-up"])
ex("v-up", "V-Up", "legraise", "CORE_ANTI_EXTENSION", C, "REPS", "supine",
   ["Lie flat with arms overhead.", "Lift legs and torso together, reaching hands toward the toes.", "Lower slowly; keep control."],
   reps_tiers([5, 7, 9, 12, 15]), [S_OG, S_YG], CORE_ST, rank=30, prereq={"allOf": [[vt("leg-raise-lying", 3)]]})
# front lever
ex("front-lever-tuck", "Tuck Front Lever", "lever", "PULL_VERTICAL", ["UPPER_BODY", "CORE"], "HOLD", "hang",
   ["Hang from a bar, pull the shoulders down and back.", "Tuck the knees, raise the hips until the back is horizontal.", "Hold with straight arms."],
   hold_tiers([3, 5, 8, 10, 15]), [S_OG, S_GB], UP, eq=HIGH_BAR, rank=40, nxt=["front-lever-adv-tuck"], prereq={"allOf": [[vt("pullup-full", 3)], [vt("hollow-hold", 3)]]},
   cautions=(CAUTION_STRENGTH, "Hard skill: stop on any elbow or shoulder pain; consider coaching."))
ex("front-lever-adv-tuck", "Advanced Tuck Front Lever", "lever", "PULL_VERTICAL", ["UPPER_BODY", "CORE"], "HOLD", "hang",
   ["As the tuck lever, but open the hips so the thighs are about 90 degrees from the torso.", "Keep the back flat and horizontal.", "Hold."],
   hold_tiers([3, 5, 8, 10, 15]), [S_OG, S_GB], UP, eq=HIGH_BAR, rank=50, nxt=["front-lever-straddle"], prereq={"allOf": [[vt("front-lever-tuck", 4)]]},
   cautions=(CAUTION_STRENGTH, "Hard skill: stop on any elbow or shoulder pain."))
ex("front-lever-straddle", "Straddle Front Lever", "lever", "PULL_VERTICAL", ["UPPER_BODY", "CORE"], "HOLD", "hang",
   ["Straddle the legs wide with the body horizontal.", "Arms straight, shoulders depressed.", "Hold."],
   hold_tiers([2, 3, 5, 8, 10]), [S_OG, S_GB], UP, eq=HIGH_BAR, rank=60, prereq={"allOf": [[vt("front-lever-adv-tuck", 4)]]},
   cautions=(CAUTION_STRENGTH, "Hard skill: stop on any elbow or shoulder pain."))
# planche
ex("planche-lean", "Planche Lean", "planche", "PUSH_HORIZONTAL", ["UPPER_BODY", "CORE"], "HOLD", "floor",
   ["Push-up position, fingers turned slightly out.", "Lean the shoulders forward past the hands, arms straight.", "Hold with a rounded upper back and tight body."],
   hold_tiers([5, 10, 15, 20, 30], stretch="stretch-shoulder"), [S_OG, S_GB], UP, rank=40, nxt=["planche-tuck"], prereq={"allOf": [[vt("pushup-standard", 4)], [vt("plank", 4)]]},
   cautions=(CAUTION_STRENGTH, "Wrist-heavy: warm up the wrists, stop on wrist pain."))
ex("planche-tuck", "Tuck Planche", "planche", "PUSH_HORIZONTAL", ["UPPER_BODY", "CORE"], "HOLD", "floor",
   ["From the lean, lift the feet with knees tucked to the chest.", "Arms straight, hips level with the shoulders.", "Hold."],
   hold_tiers([3, 5, 8, 10, 15], stretch="stretch-shoulder"), [S_OG, S_GB], UP, rank=50, nxt=["planche-adv-tuck"], prereq={"allOf": [[vt("planche-lean", 4)]]},
   cautions=(CAUTION_STRENGTH, "Wrist-heavy: stop on wrist pain."))
ex("planche-adv-tuck", "Advanced Tuck Planche", "planche", "PUSH_HORIZONTAL", ["UPPER_BODY", "CORE"], "HOLD", "floor",
   ["Open the hips so the back is flat and horizontal.", "Arms straight, shoulders forward.", "Hold."],
   hold_tiers([2, 3, 5, 8, 10], stretch="stretch-shoulder"), [S_OG, S_GB], UP, rank=60, prereq={"allOf": [[vt("planche-tuck", 4)]]},
   cautions=(CAUTION_STRENGTH, "Wrist-heavy: stop on wrist pain."))
# plank no longer a dead end: top of the plank ladder leads to the hollow hold
for pol in policies:
    if pol["variationId"] == "plank": pol["nextVariationIds"] = ["hollow-hold"]
    if pol["variationId"] == "pullup-band-assisted": pol["nextVariationIds"] = ["pullup-full"]
    if pol["variationId"] == "pushup-standard": pol["nextVariationIds"] = ["pushup-feet-elevated"]
    if pol["variationId"] == "split-squat": pol["nextVariationIds"] = ["split-squat-bulgarian"]
    if pol["variationId"] == "glute-bridge": pol["nextVariationIds"] = ["bridge-single-leg"]

# ---------------- skill graph (small, DRAFT) ----------------
nodes = [
    {"id": "skill-push-strength", "name": "Push-up strength", "description": "From wall/incline or knees to a full push-up.",
     "variationIds": ["pushup-incline", "pushup-knee", "pushup-standard"], "sourceIds": [S_CC, S_OG]},
    {"id": "skill-pull-strength", "name": "First pull-up", "description": "Rows to a band-assisted pull-up.",
     "variationIds": ["row-band", "pullup-band-assisted"], "sourceIds": [S_OG, S_CC]},
    {"id": "skill-squat-strength", "name": "Squat and single-leg strength", "description": "Air squat to split squat.",
     "variationIds": ["squat-air", "split-squat"], "sourceIds": [S_CC, S_YG]},
    {"id": "skill-core-line", "name": "Straight-body core", "description": "Plank family foundation for later lever work.",
     "variationIds": ["plank", "side-plank", "dead-bug"], "sourceIds": [S_GB, S_OG]},
]
edges = [
    {"id": "edge-row-to-pullup", "fromId": "skill-pull-strength", "toId": "skill-core-line",
     "relation": "RECOMMENDED_PREPARATION", "criterion": "A solid plank supports clean pulling."},
]

catalog = {"catalogVersion": 1, "variations": variations, "policies": policies,
           "skillNodes": nodes, "skillEdges": edges,
           "warmupTemplate": ["stretch-ankle-mobility"], "cooldownTemplate": ["stretch-back", "stretch-hamstring"]}

OUT.write_text(json.dumps(catalog, indent=2, ensure_ascii=False) + "\n")
print(f"wrote {OUT}: {len(variations)} variations, {len(policies)} policies")
