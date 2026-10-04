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

def reps_tiers(targets, rec=(30, 30, 40, 40, 45), stretch=None):
    tiers = []
    for i, t in enumerate(targets, 1):
        d = {"index": i, "target": {"type": "REPS", "value": t},
             "workWindowSeconds": max(30, rnd5(t * 4 + 12)), "minRecoverySeconds": rec[i - 1]}
        if stretch: d["earlyCompletionStretchId"] = stretch
        tiers.append(d)
    return tiers

def hold_tiers(targets, rec=(30, 30, 40, 40, 45), stretch=None):
    tiers = []
    for i, t in enumerate(targets, 1):
        d = {"index": i, "target": {"type": "HOLD_SECONDS", "value": t},
             "workWindowSeconds": t + 15, "minRecoverySeconds": rec[i - 1]}
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
         reps_tiers([3, 4, 5, 6, 8], rec=(45, 45, 50, 50, 60)), [S_CC, S_OG, S_YG], UP,
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
strength("side-plank", "Side Plank (per side)", "plank", ["CORE_ANTI_LATERAL"], ["CORE"], "HOLD", "floor",
         ["Lie on one side, forearm under the shoulder, legs stacked (or feet staggered).", "Lift the hips so the body forms a straight line.",
          "Hold, then switch sides."],
         ["Hips high", "Body in one line"],
         hold_tiers([10, 15, 20, 30, 40]), [S_OG, S_YG], CORE_ST, unilateral=True, rank=20)
strength("dead-bug", "Dead Bug", "dead-bug", ["CORE_ANTI_EXTENSION"], ["CORE"], "REPS", "supine",
         ["Lie on your back, arms up, hips and knees at 90°.", "Lower the opposite arm and leg slowly toward the floor, lower back flat.",
          "Return and alternate sides. One rep = both sides."],
         ["Lower back stays flat", "Slow and controlled"],
         reps_tiers([6, 8, 10, 12, 14]), [S_OG, S_YG], CORE_ST, rank=10)

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
