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
S_RR = "r/bodyweightfitness wiki, live check 2026-10-09"   # V21b: structure and numbers only, own wording

def rnd5(x): return int(5 * round(x / 5))

def reps_tiers(targets, rec=(60, 60, 60, 60, 60), stretch=None):
    tiers = []
    for i, t in enumerate(targets, 1):
        d = {"index": i, "target": {"type": "REPS", "value": t},
             "workWindowSeconds": max(30, rnd5(t * 6 + 15)), "minRecoverySeconds": rec[i - 1]}
        if stretch: d["earlyCompletionStretchId"] = stretch
        tiers.append(d)
    return tiers

# F13/Q4: a hold block lasts the target plus a 3 s get-ready, nothing more.
HOLD_SETUP_SECONDS = 3

def hold_tiers(targets, rec=(60, 60, 60, 60, 60), stretch=None):
    tiers = []
    for i, t in enumerate(targets, 1):
        d = {"index": i, "target": {"type": "HOLD_SECONDS", "value": t},
             "workWindowSeconds": t + HOLD_SETUP_SECONDS, "minRecoverySeconds": rec[i - 1]}
        if stretch: d["earlyCompletionStretchId"] = stretch
        tiers.append(d)
    return tiers

NO_EQ = [{}]
KB_WEIGHT = "Weight: use a bell you can move with a flat back on every rep (most people start with 8-12 kg; swings often 12-16 kg). The app counts levels per weight: set a heavier kettlebell in your equipment profile and this exercise starts again at level 1, your stars at the old weight are kept."
def need(eid, **kw): return {"equipmentId": eid, **kw}

STRETCH_NOTE = "Stretch gently; mild tension is normal, pain is not. Stop if you feel sharp or pinching pain."
CAUTION_STRENGTH = "Stop the set on sharp or joint pain; do not push through it."

variations, policies = [], []

# L01: the shared tier vocabulary (report "One gate, five tiers"); keep in sync with domain/.../progression/TierProfiles.kt (a test compares them)
PROFILE_SPECS = {
    "R": [(3, 5), (3, 6), (3, 7), (3, 8), (3, 10)], "RU": [(3, 5), (3, 6), (3, 7), (3, 8), (3, 10)],
    "C": [(3, 8), (3, 9), (3, 10), (3, 11), (3, 12)], "H30": [(3, 10), (3, 15), (3, 20), (3, 25), (3, 30)],
    "H60": [(6, 10), (4, 15), (3, 20), (2, 30), (1, 60)], "N": [(3, 3, 3), (3, 4, 5), (3, 5, 5), (3, 5, 8), (3, 5, 10)],
    "E": [(3, 15), (3, 18), (3, 20), (3, 22), (3, 25)], "M": [(1, 10)] * 5,
}
HOLD_PROFILES = {"H30", "H60", "M"}
def profile_tiers(code, rec=60, stretch=None):
    specs = PROFILE_SPECS[code]; hold = code in HOLD_PROFILES; tiers = []
    for i, sp in enumerate(specs, 1):
        v = sp[1]; desc = sp[2] if len(sp) > 2 else None
        if hold: t = {"type": "HOLD_SECONDS", "value": v}; win = v + HOLD_SETUP_SECONDS
        else: t = {"type": "REPS", "value": v}; win = max(30, rnd5(v * (desc + 3 if desc else 6) + 15))
        d = {"index": i, "target": t, "workWindowSeconds": win, "minRecoverySeconds": rec}
        if stretch: d["earlyCompletionStretchId"] = stretch
        tiers.append(d)
    return tiers

def strength(id, name, family, patterns, areas, kind, position, instructions, cues, tiers,
             sources, stretches, eq=NO_EQ, unilateral=False, rank=0, nxt=(), prereq=None,
             cautions=(CAUTION_STRENGTH,), tier_profile=None, tier_note=None):
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
    if tier_profile:
        pol["tierProfile"] = tier_profile; pol["tiers"] = profile_tiers(tier_profile, rec=tiers[0]["minRecoverySeconds"] if tiers else 60)
        if tier_note: pol["tierNote"] = tier_note
    policies.append(pol)

def stretch(id, name, areas, seconds, uni, position, instructions, eq=NO_EQ, kind="STRETCH", cues=(), cautions=(), primary=(), secondary=(), sources=None):
    v = {"id": id, "familyId": id, "name": name,
         "patterns": ["STRETCH" if kind == "STRETCH" else "MOBILITY"],
         "kind": kind, "unilateral": uni, "position": position, "defaultSeconds": seconds,
         "equipmentAlternatives": eq, "instructions": instructions, "cautions": [STRETCH_NOTE, *cautions],
         "sourceIds": list(sources or [S_OG, S_GB]), "reviewState": "DRAFT"}
    if cues: v["formCues"] = list(cues)
    if areas: v["stretchAreas"] = areas
    if primary: v["primaryMuscles"] = list(primary)
    if secondary: v["secondaryMuscles"] = list(secondary)
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

# V07 (C-A): the stretches the owner asked for (hips, hamstrings, quads, lats, shoulders, wrists, spine). DRAFT like all content.
stretch("stretch-pigeon", "Pigeon Stretch", ["HIP"], 30, True, "kneeling",
        ["Start on hands and knees on a mat.", "Slide one knee forward and turn that shin across the body; stretch the other leg straight back.",
         "Lower the hips toward the floor and fold forward over the front leg.", "Hold, breathe slowly, then switch sides."],
        cues=["Keep the hips level", "Fold only as far as is comfortable"],
        cautions=["Stop if you feel the knee of the front leg pinch or twist. Come out of the pose slowly."],
        primary=["GLUTES"], secondary=["HIP_FLEXORS"])
stretch("stretch-90-90", "90/90 Hip Stretch", ["HIP"], 30, True, "seated",
        ["Sit on the floor with the front knee bent at a right angle and the back knee bent to the other side.", "Sit tall, then lean the chest over the front shin.",
         "Feel the stretch in the outer hip of the front leg.", "Hold, then switch sides."],
        cues=["Sit tall before you lean", "Keep both sit bones near the floor"],
        cautions=["If the knees complain, sit on a folded towel or skip this stretch."],
        primary=["GLUTES"], secondary=["ADDUCTORS"])
stretch("stretch-frog", "Frog Stretch", ["HIP"], 30, False, "kneeling",
        ["Start on hands and knees; slide the knees wide apart, feet turned out.", "Lower onto the forearms and rock the hips back toward the heels.",
         "Feel the stretch along the inner thighs.", "Breathe slowly; come out by walking the knees together."],
        cues=["Knees on a mat or folded towel", "Move only as far as the stretch stays comfortable"],
        cautions=["Do not force the knees wider; stop on groin or knee pain."],
        primary=["ADDUCTORS"])
stretch("stretch-quad-couch", "Couch Stretch (quads and hip flexors)", ["QUAD", "HIP"], 30, True, "kneeling",
        ["Kneel with the back foot resting up against a wall or the side of a couch, the other foot flat in front.", "Lift the chest and tuck the pelvis under.",
         "Feel the stretch along the front of the back thigh and hip.", "Hold, then switch sides."],
        eq=[{"capabilities": ["wall"], "needs": [need("mat")]}],
        cues=["Pelvis tucked, ribs down", "Hands on the front knee for balance"],
        cautions=["A strong stretch for the knee: ease off if the knee hurts. Pad the knee with a mat."],
        primary=["QUADS"], secondary=["HIP_FLEXORS"])
stretch("stretch-quad-standing", "Standing Quad Stretch", ["QUAD"], 30, True, "standing",
        ["Stand tall; hold a wall or chair if you need balance.", "Bend one knee and hold the foot behind you, knees together.",
         "Tuck the pelvis slightly and feel the stretch along the front of the thigh.", "Hold, then switch sides."],
        cues=["Knees stay together", "Do not arch the lower back"],
        primary=["QUADS"])
stretch("stretch-hamstring-standing", "Standing Hamstring Stretch", ["HAMSTRING"], 30, True, "standing",
        ["Stand and place one heel a step ahead, toes up, leg straight (a little knee bend is fine).", "Hinge forward from the hips with a long back, hands on the front thigh.",
         "Feel the stretch behind the front thigh.", "Hold, then switch sides."],
        cues=["Hinge at the hips, not the lower back", "Keep the chest open"],
        primary=["HAMSTRINGS"], secondary=["CALVES"])
stretch("stretch-lat-wall", "Lat Stretch (hands on a wall)", ["BACK", "SHOULDER"], 30, False, "standing",
        ["Face a wall, hands on it at about hip height, feet well back.", "Push the hips back and let the chest sink toward the floor, arms and back in one line.",
         "Feel the stretch along the sides of the back and under the arms.", "Breathe slowly and hold."],
        eq=[{"capabilities": ["wall"]}],
        cues=["Arms stay straight", "Ribs down, do not collapse the lower back"],
        primary=["LATS"], secondary=["REAR_DELTS"])
stretch("stretch-sleeper", "Sleeper Stretch (shoulder)", ["SHOULDER"], 30, True, "floor",
        ["Lie on one side with the lower arm in front of you, elbow bent to a right angle, forearm pointing up.", "With the other hand, gently press the forearm toward the floor.",
         "Feel a mild stretch at the back of the lower shoulder.", "Hold, then switch sides."],
        cues=["Press gently; this is a small stretch", "Keep the shoulder blade relaxed"],
        cautions=["Stop on any pinching at the front of the shoulder. Not for an injured or recently operated shoulder."],
        primary=["REAR_DELTS"])
stretch("stretch-wrist-flexor", "Wrist Stretch (palms up)", ["WRIST"], 30, False, "kneeling",
        ["Kneel on hands and knees, palms up and fingers pointing back toward the knees.", "Rock the hips back slowly until you feel a stretch in the forearms.",
         "Keep the movement small; stay in a comfortable range.", "Hold, then release."],
        cues=["Rock back only a little", "Breathe slowly"],
        cautions=["Stop on wrist pain or tingling in the fingers; lift the hands off the floor."],
        primary=["FOREARMS"])
stretch("stretch-wrist-extensor", "Wrist Stretch (backs of the hands)", ["WRIST"], 30, False, "kneeling",
        ["Kneel on hands and knees, backs of the hands on the floor and fingers pointing back toward the knees.", "Lean the weight gently back toward the heels.",
         "Feel the stretch along the top of the forearms.", "Hold, then release."],
        cues=["Small, slow movement", "Never force the lean"],
        cautions=["Stop on wrist pain or tingling in the fingers. This is a strong stretch; start with a very small lean."],
        primary=["FOREARMS"])
stretch("stretch-thoracic", "Thoracic Rotation (thread the needle)", ["BACK"], 30, True, "kneeling",
        ["Start on hands and knees; place one hand behind the head.", "Rotate the elbow down toward the other arm, then open it up toward the ceiling.",
         "Move slowly with the breath; the rotation comes from the upper back.", "Repeat for the time, then switch sides."],
        cues=["Hips stay still", "Follow the elbow with your eyes"],
        cautions=["Stay in a pain-free range; skip if a shoulder or the neck complains."],
        primary=["UPPER_BACK"], secondary=["OBLIQUES"])

NEW_ST_UP = ["stretch-sleeper", "stretch-wrist-flexor", "stretch-wrist-extensor", "stretch-lat-wall"]
NEW_ST_LOW = ["stretch-pigeon", "stretch-90-90", "stretch-quad-couch", "stretch-quad-standing", "stretch-hamstring-standing", "stretch-frog"]
NEW_ST_CORE = ["stretch-thoracic", "stretch-lat-wall"]
UP = ["stretch-chest-door", "stretch-shoulder", *NEW_ST_UP]
LOW = ["stretch-hip-flexor", "stretch-hamstring", "stretch-calf-wall", *NEW_ST_LOW]
CORE_ST = ["stretch-back", "stretch-hip-flexor", *NEW_ST_CORE]
# the targeted muscles of the first seven stretches (V07), so their clips and the body figure show what is stretched
_MUSCLES = {"stretch-calf-wall": (["CALVES"], []), "stretch-hip-flexor": (["HIP_FLEXORS"], ["QUADS"]), "stretch-hamstring": (["HAMSTRINGS"], ["LOWER_BACK"]),
            "stretch-back": (["LOWER_BACK"], ["LATS"]), "stretch-chest-door": (["CHEST"], ["FRONT_DELTS"]), "stretch-shoulder": (["REAR_DELTS"], []),
            "stretch-ankle-mobility": (["CALVES"], [])}
for _v in variations:
    if _v["id"] in _MUSCLES:
        _v["primaryMuscles"], _v["secondaryMuscles"] = _MUSCLES[_v["id"]][0], _MUSCLES[_v["id"]][1]
        if not _v["secondaryMuscles"]: del _v["secondaryMuscles"]

# ---------------- push ----------------
strength("pushup-incline", "Incline Push-Up", "pushup", ["PUSH_HORIZONTAL"], ["UPPER_BODY"], "REPS", "floor",
         ["Hands on a wall or a stable raised surface, body in one straight line.", "Lower the chest toward the surface, elbows about 45° from the body.",
          "Press back up to straight arms without sagging the hips.", "Count only full-range reps."],
         ["Straight line head to heels", "Elbows about 45°", "Full range"],
         reps_tiers([6, 8, 10, 12, 15], stretch="stretch-chest-door"), [S_CC, S_YG, S_NG], UP,
         eq=[{"capabilities": ["wall"]}, {"needs": [need("chair", suitability=["stable"])]}],
         rank=10, nxt=["pushup-standard"], prereq={"allOf": [[vt("pushup-incline-high", 3)]]})
strength("pushup-knee", "Knee Push-Up", "pushup", ["PUSH_HORIZONTAL"], ["UPPER_BODY", "CORE"], "REPS", "floor",
         ["Kneel on a mat, hands under the shoulders, body straight from head to knees.", "Lower the chest toward the floor, elbows about 45°.",
          "Press up to straight arms; keep the hips in line.", "Count only full-range reps."],
         ["Straight line head to knees", "Do not sag the hips"],
         reps_tiers([5, 7, 9, 11, 14], stretch="stretch-chest-door"), [S_CC, S_YG, S_NG], UP,
         eq=[{}], rank=20, nxt=["pushup-standard"], prereq={"allOf": [[vt("pushup-incline-high", 3)]]})
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
          "Drive through the feet to stand tall; do not swing the weight.", "Lower with control back to the floor.", KB_WEIGHT],
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
def ex(id, name, fam, pat, area, kind, pos, steps, tiers, src, st, eq=NO_EQ, rank=0, nxt=(), prereq=None, uni=False, cues=None, cautions=(CAUTION_STRENGTH,), tier_profile=None, tier_note=None):
    strength(id, name, fam, [pat], area, kind, pos, steps, cues or ["Controlled tempo", "Full range or clean position", "Stop if form breaks"],
             tiers, src, st, eq=eq, unilateral=uni, rank=rank, nxt=nxt, prereq=prereq, cautions=cautions, tier_profile=tier_profile, tier_note=tier_note)
U, L, C = ["UPPER_BODY"], ["LOWER_BODY"], ["CORE"]
# push (horizontal)
ex("pushup-wall", "Wall Push-Up", "pushup", "PUSH_HORIZONTAL", [ "UPPER_BODY", "CORE"], "REPS", "standing",
   ["Stand an arm's length from a wall, hands on it at shoulder height a little wider than the shoulders.", "Bend the elbows and bring the chest toward the wall, body in one straight line.", "Press back to straight arms."],
   reps_tiers([8, 10, 12, 15, 20], stretch="stretch-chest-door"), [S_CC, S_YG, S_RR], UP, eq=[{"capabilities": ["wall"]}], rank=5, nxt=["pushup-incline-high"],
   cues=["Straight body", "Elbows about 45 degrees", "Feet do not slide"])
ex("pushup-incline-high", "High Incline Push-Up (table or counter)", "pushup", "PUSH_HORIZONTAL", [ "UPPER_BODY", "CORE"], "REPS", "standing",
   ["Put your hands on a sturdy table or kitchen counter (about 60 cm high), feet back, body in one straight line.", "Lower the chest to the edge, elbows about 45 degrees, then press back up.", "The lower the support, the harder it gets: this is the step between the wall and the low incline."],
   reps_tiers([6, 8, 10, 12, 15], stretch="stretch-chest-door"), [S_CC, S_YG, S_RR], UP, rank=8, nxt=["pushup-incline", "pushup-knee"], prereq={"allOf": [[vt("pushup-wall", 4)]]},
   cues=["Straight body", "Support cannot slide or tip"], cautions=(CAUTION_STRENGTH, "Use a sturdy table or counter that cannot slide or tip."))
ex("pushup-feet-elevated", "Feet-Elevated Push-Up", "pushup", "PUSH_HORIZONTAL", [ "UPPER_BODY", "CORE"], "REPS", "floor",
   ["Feet on a stable chair or step, hands on the floor, body in one straight line.", "Lower the chest to the floor, elbows about 45 degrees from the body.", "Press to straight arms without sagging the hips."],
   reps_tiers([4, 6, 8, 10, 12], stretch="stretch-chest-door"), [S_CC, S_OG, S_YG], UP, eq=[{"needs": [need("chair", suitability=["stable"])]}], rank=35, nxt=["pushup-archer"], prereq={"allOf": [[vt("pushup-standard", 4)]]})
ex("pushup-diamond", "Diamond Push-Up", "pushup", "PUSH_HORIZONTAL", U, "REPS", "floor",
   ["Plank position, hands together under the chest, thumbs and index fingers forming a diamond.", "Lower the chest to the hands, elbows close to the body.", "Press to full lockout."],
   reps_tiers([4, 6, 8, 10, 12], stretch="stretch-chest-door"), [S_CC, S_YG], UP, rank=40, nxt=["pushup-archer"], prereq={"allOf": [[vt("pushup-standard", 4)]]})
ex("pushup-archer", "Archer Push-Up (per side)", "pushup", "PUSH_HORIZONTAL", U, "REPS", "floor",
   ["Wide hand position, hands turned slightly out.", "Lower toward one hand while the other arm stays straight.", "Press back up, alternate sides. Count each side."],
   reps_tiers([3, 4, 6, 8, 10], stretch="stretch-chest-door"), [S_CC, S_OG], UP, rank=50, nxt=["pushup-one-arm-negative"], prereq={"allOf": [[vt("pushup-diamond", 3)], [vt("pushup-feet-elevated", 3)]]})
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
   reps_tiers([5, 6, 8, 10, 12]), [S_YG, S_NG], LOW, eq=[{"needs": [need("chair", suitability=["stable"])]}], uni=True, rank=30, nxt=["squat-pistol-assisted", "squat-shrimp"], prereq={"allOf": [[vt("split-squat", 3)]]})
ex("squat-shrimp", "Beginner Shrimp Squat (per side)", "squat", "LUNGE", L, "REPS", "standing",
   ["Stand on one leg, hold the other foot behind you.", "Lower until the back knee touches the floor lightly (a folded towel under the knee is fine).", "Stand back up on the working leg. This is the first of three shrimp steps.", "Ankle check (information only): stand a fist-width from a wall and try to touch the knee to the wall with the heel flat. If you cannot, work on the ankle mobility stretch first; this is not a pass or fail test."],
   reps_tiers([2, 3, 5, 6, 8]), [S_OG, S_YG, S_RR], LOW, uni=True, rank=40, nxt=["squat-shrimp-intermediate"], prereq={"allOf": [[vt("split-squat-bulgarian", 4)]]})
ex("squat-shrimp-intermediate", "Intermediate Shrimp Squat (per side)", "squat", "LUNGE", L, "REPS", "standing",
   ["Hold the rear foot with the same-side hand and keep the chest up.", "Lower slowly until the back knee touches the floor; keep the weight over the middle of the working foot.", "Stand up without pushing off the back knee.", "Ankle check (information only): stand a fist-width from a wall and try to touch the knee to the wall with the heel flat. If you cannot, work on the ankle mobility stretch first; this is not a pass or fail test."],
   reps_tiers([2, 3, 5, 6, 8]), [S_OG, S_RR], LOW, uni=True, rank=42, nxt=["squat-shrimp-advanced"], prereq={"allOf": [[vt("squat-shrimp", 4)]]})
ex("squat-shrimp-advanced", "Advanced Shrimp Squat (per side)", "squat", "LUNGE", L, "REPS", "standing",
   ["Hold the rear foot behind you with both hands (or the same-side hand with the knee pointing down), torso upright.", "Lower slowly until the back knee touches the floor, then stand up.", "A parallel end point to the pistol squat: choose whichever suits your ankles.", "Ankle check (information only): stand a fist-width from a wall and try to touch the knee to the wall with the heel flat. If you cannot, work on the ankle mobility stretch first; this is not a pass or fail test."],
   reps_tiers([1, 2, 3, 4, 5]), [S_OG, S_RR], LOW, uni=True, rank=55, prereq={"allOf": [[vt("squat-shrimp-intermediate", 4)]]})
ex("squat-pistol-assisted", "Assisted Pistol Squat (per side)", "squat", "SQUAT", L, "REPS", "standing",
   ["Hold a door frame or post lightly for balance (or a resistance band anchored above you).", "Lower on one leg with the other leg straight out in front.", "Stand up using as little help as possible.", "Ankle check (information only): stand a fist-width from a wall and try to touch the knee to the wall with the heel flat. If you cannot, work on the ankle mobility stretch first; this is not a pass or fail test."],
   reps_tiers([2, 3, 5, 6, 8]), [S_CC, S_OG, S_NG], LOW, uni=True, rank=45, nxt=["squat-pistol-box", "squat-pistol-counterbalance"], prereq={"allOf": [[vt("split-squat-bulgarian", 4)]]})
ex("squat-pistol-box", "Box Pistol Squat (per side)", "squat", "SQUAT", L, "REPS", "standing",
   ["Stand in front of a stable chair or box, the free leg straight out in front.", "Sit down onto the seat on one leg, slowly, and stand back up without rocking.", "Lower the seat over the weeks (a chair, then a lower step).", "Ankle check (information only): stand a fist-width from a wall and try to touch the knee to the wall with the heel flat. If you cannot, work on the ankle mobility stretch first; this is not a pass or fail test."],
   reps_tiers([2, 3, 5, 6, 8]), [S_OG, S_RR], LOW, eq=[{"needs": [need("chair", suitability=["stable"])]}], uni=True, rank=50, nxt=["squat-pistol"], prereq={"allOf": [[vt("squat-pistol-assisted", 3)]]},
   cautions=(CAUTION_STRENGTH, "Check that the chair is stable and cannot slide; do not drop onto the seat."))
ex("squat-pistol-counterbalance", "Counterbalance Pistol Squat (per side)", "squat", "SQUAT", L, "REPS", "standing",
   ["Hold a light weight (a small dumbbell, a book or a water bottle) out in front of you with straight arms: it is an easier version because it balances the body.", "Lower on one leg with the free leg straight out in front, then stand up.", "Use less weight over time.", "Ankle check (information only): stand a fist-width from a wall and try to touch the knee to the wall with the heel flat. If you cannot, work on the ankle mobility stretch first; this is not a pass or fail test."],
   reps_tiers([2, 3, 5, 6, 8]), [S_OG, S_RR], LOW, uni=True, rank=48, nxt=["squat-pistol"], prereq={"allOf": [[vt("squat-pistol-assisted", 3)]]})
ex("squat-pistol", "Pistol Squat (per side)", "squat", "SQUAT", L, "REPS", "standing",
   ["Stand on one leg, other leg straight in front.", "Lower to full depth with the heel down.", "Stand up with control; switch sides."],
   reps_tiers([1, 2, 3, 5, 6]), [S_CC, S_OG, S_NG], LOW, uni=True, rank=60, prereq={"allOf": [[vt("squat-pistol-box", 4), vt("squat-pistol-counterbalance", 4)]]})
# bridge
ex("bridge-table", "Table Bridge (reverse tabletop)", "bridge", "HINGE", L, "REPS", "supine",
   ["Sit with the knees bent and the feet flat, hands on the floor behind you with the fingers pointing toward the feet.", "Press the hips up until the thighs are level and the body forms a table from the shoulders to the knees.", "Lower slowly. This is the first step of the back-bridge ladder."],
   reps_tiers([5, 6, 8, 10, 12]), [S_CC, S_GB, S_RR], ["stretch-hip-flexor", "stretch-back"], rank=15, nxt=["bridge-incline"], prereq={"allOf": [[vt("glute-bridge", 4)]]},
   cues=["Hips level with the shoulders", "Arms straight", "Wrists comfortable"], cautions=(CAUTION_STRENGTH, "Stop on wrist or shoulder pain; place the fingers wherever the wrists are comfortable."))
ex("bridge-single-leg", "Single-Leg Glute Bridge (per side)", "bridge", "HINGE", L, "REPS", "supine",
   ["Lie on your back, one foot flat, other leg straight up.", "Press through the heel to lift the hips level.", "Lower slowly; finish a side, then switch."],
   reps_tiers([5, 6, 8, 10, 12]), [S_CC, S_YG], ["stretch-hip-flexor", "stretch-back"], uni=True, rank=20, prereq={"allOf": [[vt("glute-bridge", 4)]]})   # L09: a strength branch, no longer on the back-bridge ladder
ex("bridge-incline", "Incline Bridge (hands on a chair)", "bridge", "HINGE", L, "REPS", "supine",
   ["Sit in front of a sturdy chair seat, hands on the front edge behind you, fingers pointing toward the feet, feet flat on the floor.", "Press up so the hips rise and the arms straighten, the body arched like a small bridge.", "Lower slowly; lower the chair step by step (a sofa, then a low step) as it gets easier. This is the middle step before the full back bridge."],
   reps_tiers([4, 6, 8, 10, 12]), [S_CC, S_GB], ["stretch-hip-flexor", "stretch-back"], eq=[{"needs": [need("chair", suitability=["stable"])]}], rank=25, nxt=["bridge-head"], prereq={"allOf": [[vt("bridge-table", 4)]]},
   cues=["Push the hips up", "Arms straight", "Chair cannot slide"], cautions=(CAUTION_STRENGTH, "Needs some shoulder and wrist mobility; check that the chair is stable and cannot slide; stop on any pinching."))
ex("bridge-head", "Head-Supported Bridge", "bridge", "HINGE", L, "HOLD", "supine",
   ["Lie on your back, feet flat near the hips, hands on the floor beside the head with the fingers toward the shoulders.", "Press the hips up and rest the top of the head lightly on a folded towel or mat, the weight mostly on the feet and hands.", "Hold, breathing steadily; lower slowly. Do not put your weight on the neck."],
   hold_tiers([5, 10, 15, 20, 30]), [S_CC, S_GB, S_RR], ["stretch-hip-flexor", "stretch-back"], rank=27, nxt=["bridge-wall-walkdown"], prereq={"allOf": [[vt("bridge-incline", 4)]]},
   cautions=(CAUTION_STRENGTH, "Keep the weight on the feet and hands, not on the neck; stop on any neck, shoulder or lower-back pinching."))
ex("bridge-wall-walkdown", "Wall Walk-Down Bridge", "bridge", "HINGE", L, "REPS", "standing",
   ["Stand with your back about a foot from a wall, arms overhead.", "Lean back and place the hands on the wall, then walk them slowly down the wall as the back arches; walk back up the same way.", "A middle step before the full back bridge: the wall takes part of the weight. Stop when the back stops feeling even."],
   reps_tiers([3, 4, 5, 6, 8]), [S_CC, S_GB, S_RR], ["stretch-hip-flexor", "stretch-back"], eq=[{"capabilities": ["wall"]}], rank=29, nxt=["bridge-back"], prereq={"allOf": [[vt("bridge-head", 3)]]},
   cautions=(CAUTION_STRENGTH, "Needs shoulder and spine mobility; stay in a range where the back arches evenly; stop on any pinching; keep a way to stand up (a partner or a sofa within reach)."))
ex("bridge-back", "Full Back Bridge", "bridge", "HINGE", L, "HOLD", "supine",
   ["Lie on your back, hands by the ears, feet flat.", "Press up so arms and legs straighten and the back arches evenly.", "Hold, breathing steadily; lower slowly."],
   hold_tiers([5, 10, 15, 20, 30]), [S_CC, S_GB], ["stretch-hip-flexor", "stretch-back"], rank=30, prereq={"allOf": [[vt("bridge-wall-walkdown", 3)]]},
   cautions=(CAUTION_STRENGTH, "Needs shoulder and spine mobility; stop on any pinching."))
# core
ex("hollow-tuck", "Tucked Hollow Hold", "plank", "CORE_ANTI_EXTENSION", C, "HOLD", "supine",
   ["Lie on your back and press the lower back into the floor.", "Lift the shoulders a few centimetres and pull the knees over the hips, arms by the ears or sides.", "Hold with the lower back flat; breathe steadily."],
   hold_tiers([10, 15, 20, 25, 30]), [S_GB, S_OG, S_RR], CORE_ST, rank=18, nxt=["hollow-one-leg"], prereq={"allOf": [[vt("plank", 4)]]}, tier_profile="H30", tier_note="Tiers 10 / 15 / 20 / 25 / 30 s: the 30 s gate is sourced (RR, owner decision); the steps between are DRAFT.")
ex("hollow-one-leg", "One-Leg Hollow Hold", "plank", "CORE_ANTI_EXTENSION", C, "HOLD", "supine",
   ["From the tucked hollow hold, straighten one leg a few centimetres above the floor while the other stays tucked.", "Keep the lower back pressed down; switch legs between holds.", "Hold, breathing steadily."],
   hold_tiers([10, 15, 20, 25, 30]), [S_GB, S_OG, S_RR], CORE_ST, rank=20, nxt=["hollow-hold"], prereq={"allOf": [[vt("hollow-tuck", 4)]]}, tier_profile="H30", tier_note="Tiers 10 / 15 / 20 / 25 / 30 s: the 30 s gate is sourced (RR, owner decision); the steps between are DRAFT.")
ex("hollow-rocks", "Hollow Rocks", "plank", "CORE_ANTI_EXTENSION", C, "REPS", "supine",
   ["Get into the hollow body hold, arms by the ears.", "Rock slowly back and forth like a boat, keeping the hollow shape and the lower back pressed down.", "Stop the set when the lower back starts to arch."],
   reps_tiers([6, 8, 10, 12, 15]), [S_GB, S_OG], CORE_ST, rank=25, prereq={"allOf": [[vt("hollow-hold", 4)]]})
ex("hollow-hold", "Hollow Body Hold", "plank", "CORE_ANTI_EXTENSION", C, "HOLD", "supine",
   ["Lie on your back, press the lower back into the floor.", "Lift shoulders and straight legs a few centimetres, arms by the ears or sides.", "Hold; make it easier by bending the knees."],
   hold_tiers([10, 15, 20, 30, 40]), [S_GB, S_OG], CORE_ST, rank=22, prereq={"allOf": [[vt("hollow-one-leg", 4)]]}, nxt=["l-sit-foot-supported", "hollow-rocks"])
ex("l-sit-foot-supported", "Foot-Supported L-Sit", "plank", "CORE_ANTI_EXTENSION", C, "HOLD", "seated",
   ["Sit with straight legs and the hands beside the hips, fingers forward, heels on the floor.", "Press down, lift the hips off the floor while the heels stay on it and carry some of the weight.", "Hold with straight arms and the shoulders pushed down."],
   hold_tiers([10, 15, 20, 30, 60]), [S_OG, S_GB, S_RR], CORE_ST, rank=22, nxt=["l-sit-one-foot"], prereq={"allOf": [[vt("hollow-hold", 3)]]}, tier_profile="H60", tier_note="One accumulated minute: the numbers are the length of one hold (10 / 15 / 20 / 30 / 60 s); do as many holds as you need to add up to a minute, and make them longer over the weeks. The one-minute total is sourced for the L-sit (Antranik); applying it to every step is DRAFT.",
   cues=["Push the floor away", "Shoulders down", "Straight arms"])
ex("l-sit-one-foot", "One-Foot L-Sit", "plank", "CORE_ANTI_EXTENSION", C, "HOLD", "seated",
   ["From the foot-supported L-sit, lift one leg off the floor and hold it straight in front of you; the other heel stays down.", "Switch the lifted leg between holds.", "Keep the arms straight and the shoulders pushed down."],
   hold_tiers([10, 15, 20, 30, 60]), [S_OG, S_GB, S_RR], CORE_ST, rank=24, nxt=["l-sit-tuck"], prereq={"allOf": [[vt("l-sit-foot-supported", 4)]]}, tier_profile="H60", tier_note="One accumulated minute: the numbers are the length of one hold (10 / 15 / 20 / 30 / 60 s); do as many holds as you need to add up to a minute, and make them longer over the weeks. The one-minute total is sourced for the L-sit (Antranik); applying it to every step is DRAFT.",
   cues=["Straight lifted leg", "Switch legs", "Shoulders down"])
ex("l-sit-tuck", "Tuck L-Sit", "plank", "CORE_ANTI_EXTENSION", C, "HOLD", "seated",
   ["Sit with the knees bent and the hands beside the hips, fingers forward.", "Press down, lift the hips and the feet off the floor with the knees pulled in.", "Hold with straight arms; the shoulders stay pushed down."],
   hold_tiers([10, 15, 20, 30, 60]), [S_OG, S_GB, S_RR], CORE_ST, rank=26, nxt=["l-sit-advanced-tuck"], prereq={"allOf": [[vt("l-sit-one-foot", 4)]]}, tier_profile="H60", tier_note="One accumulated minute: the numbers are the length of one hold (10 / 15 / 20 / 30 / 60 s); do as many holds as you need to add up to a minute, and make them longer over the weeks. The one-minute total is sourced for the L-sit (Antranik); applying it to every step is DRAFT.",
   cues=["Push the floor away", "Shoulders down", "Knees tight to the chest"])
ex("l-sit-advanced-tuck", "Advanced Tuck L-Sit", "plank", "CORE_ANTI_EXTENSION", C, "HOLD", "seated",
   ["From the tuck L-sit, move the knees away from the chest so the thighs are close to level and the shins hang down.", "Keep the back upright and the arms straight.", "Hold, then lower with control."],
   hold_tiers([10, 15, 20, 30, 60]), [S_OG, S_GB, S_RR], CORE_ST, rank=28, nxt=["l-sit-one-leg"], prereq={"allOf": [[vt("l-sit-tuck", 4)]]}, tier_profile="H60", tier_note="One accumulated minute: the numbers are the length of one hold (10 / 15 / 20 / 30 / 60 s); do as many holds as you need to add up to a minute, and make them longer over the weeks. The one-minute total is sourced for the L-sit (Antranik); applying it to every step is DRAFT.",
   cues=["Thighs level", "Straight arms", "Upright back"])
ex("l-sit-one-leg", "One-Leg Extended L-Sit", "plank", "CORE_ANTI_EXTENSION", C, "HOLD", "seated",
   ["From the advanced tuck, straighten one leg in front of you while the other stays tucked.", "Switch legs between holds.", "Keep the arms straight and the shoulders pushed down."],
   hold_tiers([10, 15, 20, 30, 60]), [S_OG, S_GB, S_RR], CORE_ST, rank=29, nxt=["l-sit-floor"], prereq={"allOf": [[vt("l-sit-advanced-tuck", 4)]]}, tier_profile="H60", tier_note="One accumulated minute: the numbers are the length of one hold (10 / 15 / 20 / 30 / 60 s); do as many holds as you need to add up to a minute, and make them longer over the weeks. The one-minute total is sourced for the L-sit (Antranik); applying it to every step is DRAFT.",
   cues=["Straight leg locked", "Switch legs", "Shoulders down"])
ex("l-sit-floor", "L-Sit (floor)", "plank", "CORE_ANTI_EXTENSION", C, "HOLD", "seated",
   ["Sit with straight legs, hands beside the hips.", "Press down, lift the hips and legs off the floor.", "Hold with legs straight and locked."],
   hold_tiers([10, 15, 20, 30, 60]), [S_OG, S_GB, S_CC, S_RR], CORE_ST, rank=30, nxt=["v-sit-floor"], prereq={"allOf": [[vt("l-sit-one-leg", 4)]]}, tier_profile="H60", tier_note="One accumulated minute: the numbers are the length of one hold (10 / 15 / 20 / 30 / 60 s); do as many holds as you need to add up to a minute, and make them longer over the weeks. The one-minute total is sourced for the L-sit (Antranik); applying it to every step is DRAFT.")
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
   hold_tiers([3, 5, 8, 10, 15]), [S_OG, S_GB], UP, eq=HIGH_BAR, rank=40, nxt=["front-lever-adv-tuck"], prereq={"allOf": [[vt("pullup-full", 3)], [vt("hollow-hold", 3)], [vt("inverted-row-wide", 4)]]},
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
# ---------------- U09: new progression chains (owner OK on plan 4.1, 2026-10-05; DRAFT numbers) ----------------
LOWBAR = [{"needs": [need("low-bar")]}]
CHAIR_STABLE = [{"needs": [need("chair", suitability=["stable"])]}]
KB = [{"needs": [need("kettlebell", minMassGrams=8000)]}]

# horizontal pull: band row -> inverted rows -> archer row
strength("inverted-row-bent-knees", "Inverted Row (knees bent)", "row", ["PULL_HORIZONTAL"], ["UPPER_BODY"], "REPS", "hang",
         ["Lie under a low bar or a sturdy table edge, hands on it a little wider than the shoulders, knees bent, heels on the floor.", "Pull the chest to the bar, body in one line from the shoulders to the knees.",
          "Lower slowly to straight arms.", "Make sure the bar or table cannot slide or tip."],
         ["Squeeze the shoulder blades", "Hips stay up", "Slow lowering"],
         reps_tiers([5, 7, 9, 11, 13]), [S_OG, S_YG], UP, eq=LOWBAR, rank=20, nxt=["inverted-row"], prereq={"allOf": [[vt("row-band", 4), vt("towel-door-row", 4)]]},
         cautions=(CAUTION_STRENGTH, "Check that the bar or table is stable before every set."))
strength("inverted-row", "Inverted Row", "row", ["PULL_HORIZONTAL"], ["UPPER_BODY"], "REPS", "hang",
         ["Same set-up with straight legs, heels on the floor, body in a straight plank line.", "Pull the chest to the bar.", "Lower slowly to straight arms.", "Keep the hips from sagging."],
         ["Plank line", "Chest to the bar", "Slow lowering"],
         reps_tiers([4, 6, 8, 10, 12]), [S_OG, S_YG, S_CC], UP, eq=LOWBAR, rank=30, nxt=["inverted-row-wide"], prereq={"allOf": [[vt("inverted-row-bent-knees", 4)]]},
         cautions=(CAUTION_STRENGTH, "Check that the bar or table is stable before every set."))
strength("inverted-row-wide", "Wide Inverted Row", "row", ["PULL_HORIZONTAL"], ["UPPER_BODY"], "REPS", "hang",
         ["Set up like an inverted row with the hands about one and a half shoulder widths apart.", "Pull the chest to the bar, body in a straight line; the wide grip makes it harder.", "Lower slowly to straight arms. Three sets of eight is the gate for the front lever work."],
         ["Plank line", "Chest to the bar", "Slow lowering"],
         reps_tiers([4, 6, 8, 10, 12]), [S_OG, S_RR], UP, eq=LOWBAR, rank=35, nxt=["inverted-row-feet-elevated"], prereq={"allOf": [[vt("inverted-row", 4)]]},
         cautions=(CAUTION_STRENGTH, "Check that the bar or table is stable before every set."))
strength("inverted-row-feet-elevated", "Inverted Row (feet elevated)", "row", ["PULL_HORIZONTAL"], ["UPPER_BODY"], "REPS", "hang",
         ["Rest the heels on a stable step or chair so the body is level or higher than the bar.", "Pull the chest to the bar.", "Lower slowly; keep the body straight."],
         ["Level body", "Controlled lowering"],
         reps_tiers([4, 6, 8, 10, 12]), [S_OG, S_YG], UP, eq=LOWBAR, rank=40, nxt=["archer-row"], prereq={"allOf": [[vt("inverted-row-wide", 4)]]},
         cautions=(CAUTION_STRENGTH, "Check that the bar and the step are stable before every set."))
strength("archer-row", "Archer Row (per side)", "row", ["PULL_HORIZONTAL"], ["UPPER_BODY"], "REPS", "hang",
         ["Set up like an inverted row with a wide grip.", "Pull toward one hand while the other arm stays straight and helps a little.", "Lower slowly and alternate sides; count both sides as one rep."],
         ["Straight assisting arm", "Slow lowering"],
         reps_tiers([2, 3, 4, 6, 8]), [S_OG, S_GB], UP, eq=LOWBAR, unilateral=True, rank=50, prereq={"allOf": [[vt("inverted-row-feet-elevated", 4)]]},
         cautions=(CAUTION_STRENGTH, "Check that the bar is stable; stop on elbow or shoulder pain."))

# back extension: superman -> Y arch hold -> arch rocks
strength("arch-hold-y", "Arch Hold (arms overhead, Y)", "superman", ["HINGE"], ["LOWER_BODY", "CORE"], "HOLD", "prone",
         ["Lie face down, arms overhead in a Y, forehead near the floor.", "Lift arms, chest and legs together a few centimetres.", "Hold with a long neck and steady breathing, then lower with control."],
         ["Squeeze the glutes", "Long neck", "Small lift"],
         hold_tiers([15, 20, 30, 40, 50]), [S_GB, S_OG], CORE_ST, rank=20, nxt=["arch-rocks"], prereq={"allOf": [[vt("superman-hold", 4)]]},
         cautions=(CAUTION_STRENGTH, "Stop on any pinching in the lower back."))
strength("arch-rocks", "Arch Rocks", "superman", ["HINGE"], ["LOWER_BODY", "CORE"], "REPS", "prone",
         ["Get into the arch hold position, arms overhead.", "Rock slowly forward and back a few centimetres, keeping the lift.", "Stay controlled; stop the set when the lift drops."],
         ["Slow rocking", "Keep the arch"],
         reps_tiers([6, 8, 10, 12, 15]), [S_GB], CORE_ST, rank=30, prereq={"allOf": [[vt("arch-hold-y", 4)]]},
         cautions=(CAUTION_STRENGTH, "Stop on any pinching in the lower back."))

# side plank: plank -> leg raise -> Copenhagen
strength("side-plank-leg-raise", "Side Plank with Leg Raise (per side)", "side-plank", ["CORE_ANTI_LATERAL"], ["CORE"], "REPS", "floor",
         ["Get into a side plank on the forearm.", "Raise the top leg a little, lower it slowly.", "Keep the hips high all the time; switch sides after the set."],
         ["Hips high", "Slow leg"],
         reps_tiers([4, 6, 8, 10, 12]), [S_OG, S_YG], CORE_ST, unilateral=True, rank=30, nxt=["copenhagen-side-plank-short"], prereq={"allOf": [[vt("side-plank", 4)]]})
strength("copenhagen-side-plank-short", "Short-Lever Copenhagen Side Plank (per side)", "side-plank", ["CORE_ANTI_LATERAL"], ["CORE"], "HOLD", "floor",
         ["Lie on your side, forearm on the floor, the top knee (not the foot) resting on a stable chair seat.", "Lift the hips so the body forms a line from the head to the knee; the lower leg hangs free or tucks.", "Hold, then switch sides. This short lever comes before the long one."],
         ["Straight line", "Hips high", "Knee on the seat"],
         hold_tiers([5, 8, 12, 16, 20]), [S_OG, S_RR], CORE_ST, eq=CHAIR_STABLE, unilateral=True, rank=35, nxt=["copenhagen-side-plank"], prereq={"allOf": [[vt("side-plank-leg-raise", 4)]]},
         cautions=(CAUTION_STRENGTH, "Stop on groin or inner-thigh pain; build up slowly; check that the chair is stable."))
strength("copenhagen-side-plank", "Copenhagen Side Plank (long lever, per side)", "side-plank", ["CORE_ANTI_LATERAL"], ["CORE"], "HOLD", "floor",
         ["Lie on your side, forearm on the floor, top leg resting on a stable chair seat.", "Lift the hips so the body forms a line; the lower leg hangs free or tucks.", "Hold, then switch sides."],
         ["Straight line", "Hips high"],
         hold_tiers([5, 8, 12, 16, 20]), [S_OG, S_GB], CORE_ST, eq=CHAIR_STABLE, unilateral=True, rank=40, prereq={"allOf": [[vt("copenhagen-side-plank-short", 4)]]},
         cautions=(CAUTION_STRENGTH, "Stop on groin or inner-thigh pain; build up slowly."))

# kettlebell hinge: deadlift -> single-leg RDL -> swing (ballistic: only after the two before)
strength("kettlebell-single-leg-rdl", "Kettlebell Single-Leg RDL (per side)", "deadlift", ["HINGE"], ["LOWER_BODY"], "REPS", "standing",
         ["Hold the kettlebell in the hand opposite the standing leg.", "Hinge at the hip, back flat, the free leg reaching behind.", "Stand tall again by squeezing the glute; do not twist.", KB_WEIGHT],
         ["Flat back", "Hips square", "Controlled tempo"],
         reps_tiers([4, 6, 8, 10, 12]), [S_NG, S_YG], ["stretch-hamstring", "stretch-hip-flexor"], eq=KB, unilateral=True, rank=30, nxt=["kettlebell-swing"],
         prereq={"allOf": [[vt("kettlebell-deadlift", 4)]]}, cautions=(CAUTION_STRENGTH, "Controlled tempo only; no ballistic or swinging work in this one."))
strength("kettlebell-swing", "Kettlebell Swing", "deadlift", ["HINGE"], ["LOWER_BODY"], "REPS", "standing",
         ["Stand with feet a little wider than the hips, kettlebell in front of you.", "Hike the bell back between the legs, then drive the hips forward so the bell floats to chest height.", "Let it fall back and hinge again; the arms only guide it.", KB_WEIGHT],
         ["Hips drive, arms guide", "Flat back", "Stop when the form slips"],
         reps_tiers([8, 10, 12, 15, 20]), [S_NG, S_YG], ["stretch-hamstring", "stretch-hip-flexor"], eq=KB, rank=40, nxt=["kettlebell-swing-one-arm"],
         prereq={"allOf": [[vt("kettlebell-single-leg-rdl", 3)], [vt("kettlebell-deadlift", 5)]]},
         cautions=(CAUTION_STRENGTH, "Ballistic movement: only after the deadlift and the single-leg RDL feel easy; use a light bell; stop on any back pain."))

# same bell, harder exercise: one-arm swing after the two-hand swing (owner 2026-10-08: progress with the same weight OR more weight)
strength("kettlebell-swing-one-arm", "One-Arm Kettlebell Swing (per side)", "deadlift", ["HINGE"], ["LOWER_BODY"], "REPS", "standing",
         ["Same set-up as the two-hand swing; hold the handle with one hand, thumb forward.", "Drive the hips forward so the bell floats to chest height; the free arm moves naturally with it.", "Do all reps on one side, then switch hands.", KB_WEIGHT],
         ["Hips drive, arm guides", "Shoulders square, do not twist", "Stop when the form slips"],
         reps_tiers([6, 8, 10, 12, 15]), [S_NG, S_YG], ["stretch-hamstring", "stretch-hip-flexor"], eq=KB, unilateral=True, rank=50,
         prereq={"allOf": [[vt("kettlebell-swing", 4)]]},
         cautions=(CAUTION_STRENGTH, "Ballistic movement with one hand: only after the two-hand swing feels easy at this weight; keep the shoulders level; stop on any back pain."))


# =============================================================================================================
# V21b: the r/bodyweightfitness Recommended Routine pieces that were missing (warm-up items, dips, hamstring paths, core, Minimalist).
# Structure and numbers from the live wiki check (docs/research/rr-live-check.md); instructions are our own wording. All DRAFT.
# =============================================================================================================
RR = [S_RR]
# --- warm-up items (30 s mobility blocks; the warm-up switch in V23 uses them)
stretch("warmup-shoulder-band", "Shoulder Band Warm-up (pass-throughs)", ["SHOULDER"], 30, False, "standing",
        ["Hold a resistance band (or a towel) with both hands, wide enough that the arms stay straight.", "Lift it forward and over your head, then behind you as far as it stays comfortable.",
         "Reverse the same arc back to the front; move slowly and breathe.", "Use a wider grip if the shoulders pinch."],
        kind="MOBILITY", cues=["Straight arms", "Wider grip if it pinches"], cautions=["Stay in a pain-free range; a stick or T-shirt version is a weaker alternative."],
        primary=["FRONT_DELTS"], secondary=["REAR_DELTS"], sources=RR)
stretch("warmup-shoulder-towel", "Shoulder Towel Warm-up (pass-throughs)", ["SHOULDER"], 30, False, "standing",
        ["Hold a towel stretched between both hands, wide enough that the arms stay straight.", "Raise it over your head and behind you as far as it stays comfortable, then back.",
         "Move slowly; widen the grip if the shoulders pinch."],
        kind="MOBILITY", cues=["Straight arms", "Wider grip if it pinches"], cautions=["Stay in a pain-free range."],
        primary=["FRONT_DELTS"], secondary=["REAR_DELTS"], sources=RR)
stretch("warmup-squat-sky-reach", "Squat Sky Reach", ["HIP"], 30, False, "standing",
        ["Stand with the feet about shoulder width apart.", "Sit down into a deep squat with the chest up and hands together in front.",
         "Stand up and reach both arms overhead; look up at your hands.", "Hold on to a wall or post with one hand if the squat is hard."],
        kind="MOBILITY", cues=["Heels down", "Reach tall at the top"], cautions=["Squat only as deep as is comfortable; hold on to something to assist."],
        primary=["QUADS", "GLUTES"], secondary=["CALVES"], sources=RR)
stretch("warmup-wrist-prep", "Wrist Prep (rocking)", ["WRIST"], 30, False, "kneeling",
        ["Kneel on hands and knees with the fingers pointing forward.", "Rock the body slowly forward and back over the hands; keep the palms flat.",
         "Then repeat with the fingers pointing to the sides and backward (small range).", "Stay gentle; this is a warm-up."],
        kind="MOBILITY", cues=["Small, slow range", "Palms flat"], cautions=["Stop on wrist pain or tingling in the fingers."],
        primary=["FOREARMS"], sources=RR)
stretch("warmup-dead-bug", "Dead Bug (warm-up)", [], 30, False, "supine",
        ["Lie on your back, arms up, knees bent over the hips.", "Lower the opposite arm and leg slowly while the lower back stays pressed to the floor.",
         "Return and alternate sides for the time."],
        kind="MOBILITY", cues=["Back flat", "Slow and controlled"], primary=["ABS"], secondary=["HIP_FLEXORS"], sources=RR)
stretch("warmup-arch-hang", "Arch Hang (warm-up)", ["SHOULDER"], 30, False, "hang",
        ["Hang from a bar with straight arms and pull the shoulder blades down and back so the chest comes forward; the back arches a little.", "Hold for a moment, relax, repeat for about ten pulls.",
         "Feet may touch the floor or a chair to take weight off."],
        eq=[{"needs": [need("pullup-bar")]}], kind="MOBILITY", cues=["Straight arms", "Chest forward"], cautions=["Stop on shoulder pain. Take weight off with the feet if the grip tires."],
        primary=["LATS", "UPPER_BACK"], sources=RR)
stretch("warmup-support-hold", "Support Hold (warm-up)", ["SHOULDER"], 30, False, "standing",
        ["Support the body on two parallel bars (or two sturdy chairs, or a counter corner) with straight arms.", "Push the shoulders down away from the ears and hold.",
         "Check that the chairs cannot tip; put some weight on the seats."],
        eq=[{"needs": [need("dip-support")]}], kind="MOBILITY", cues=["Straight arms", "Shoulders down"], cautions=["Make sure the support is stable; step down if the shoulders hurt."],
        primary=["TRICEPS", "FRONT_DELTS"], sources=RR)

# --- dips: parallel-bar support hold -> negative dips -> dips (two sturdy chairs or a counter corner work; no bench dips)
DIPEQ = [{"needs": [need("dip-support")]}]
DIP_NOTE = "Support: parallel bars, two sturdy chairs (put weight on the seats so they cannot tip), or a 90-degree kitchen-counter corner. Bench dips with the hands behind you are not used here: they carry over poorly to real dips."
strength("dip-support-hold", "Dip Support Hold", "dip", ["PUSH_VERTICAL"], ["UPPER_BODY"], "HOLD", "standing",
         ["Support the body on two parallel bars with straight arms, shoulders pushed down away from the ears.", "Keep the body still and breathe; the legs can hang or be bent.", DIP_NOTE,
          "Move on when all sets reach 30 s (the wiki aims for one minute; 30 s is the app's choice)."],
         ["Straight arms", "Shoulders down", "Body still"],
         hold_tiers([10, 15, 20, 25, 30]), RR, UP, eq=DIPEQ, rank=60, nxt=["dip-negative"], tier_profile="H30", tier_note="Tiers 10 / 15 / 20 / 25 / 30 s: the 30 s gate is sourced (RR, owner decision); the steps between are DRAFT.",
         cautions=(CAUTION_STRENGTH, "Check that the support is stable before every set; step down on shoulder pain."))
strength("dip-negative", "Negative Dip", "dip", ["PUSH_VERTICAL"], ["UPPER_BODY"], "REPS", "standing",
         ["Start in the support hold with straight arms.", "Lower yourself slowly (about 5 seconds, working toward 10) until the upper arms are about parallel to the floor.",
          "Step or jump back up to the top; do not push up with the arms.", DIP_NOTE],
         ["Slow lowering", "Elbows close to the body", "Shoulders down"],
         reps_tiers([3, 4, 5, 6, 8]), RR, UP, eq=DIPEQ, rank=65, nxt=["dip-parallel"], prereq={"allOf": [[vt("dip-support-hold", 4)]]},
         cautions=(CAUTION_STRENGTH, "Check that the support is stable before every set; stop on shoulder or elbow pain."))
strength("dip-parallel", "Dip", "dip", ["PUSH_VERTICAL"], ["UPPER_BODY"], "REPS", "standing",
         ["Start in the support hold with straight arms.", "Lower until the upper arms are about parallel to the floor, then press back up to straight arms.", "Keep the shoulders down and the body slightly forward.", DIP_NOTE],
         ["Controlled lowering", "Full lockout", "Shoulders down"],
         reps_tiers([3, 5, 6, 8, 10]), RR, UP, eq=DIPEQ, rank=70, prereq={"allOf": [[vt("dip-negative", 4)]]},
         cautions=(CAUTION_STRENGTH, "Check that the support is stable before every set; stop on shoulder or elbow pain."))

# --- hinge: Romanian deadlift -> single-leg deadlift -> banded Nordic negatives -> banded Nordic curl -> Nordic curl (or the floor-slide path)
HAM_ST = ["stretch-hamstring", "stretch-hip-flexor", "stretch-hamstring-standing"]
ANCHOR = [{"needs": [need("foot-anchor")]}]
ANCHOR_BAND = [{"needs": [need("foot-anchor"), need("resistance-band", suitability=["stable-anchor"])]}]
strength("rdl-bodyweight", "Romanian Deadlift (bodyweight)", "rdl", ["HINGE"], ["LOWER_BODY"], "REPS", "standing",
         ["Stand with the feet hip width apart and a soft bend in the knees.", "Push the hips back and lower the chest with a flat back until you feel the hamstrings stretch.",
          "Stand tall by driving the hips forward; the knees barely change."],
         ["Hips back", "Flat back", "Glutes squeeze at the top"],
         reps_tiers([8, 10, 12, 15, 20]), RR, HAM_ST, rank=10, nxt=["single-leg-deadlift"])
strength("single-leg-deadlift", "Single-Leg Deadlift (per side)", "rdl", ["HINGE"], ["LOWER_BODY"], "REPS", "standing",
         ["Stand on one leg with a soft knee; hold a wall or chair with a fingertip if you need balance.", "Hinge forward, the free leg reaching straight behind you and the back flat.",
          "Return to standing by squeezing the glute. Do all reps on one side, then switch."],
         ["Hips square", "Flat back", "Free leg in line with the body"],
         reps_tiers([4, 5, 6, 7, 8]), RR, HAM_ST, unilateral=True, rank=20, nxt=["nordic-negative-banded", "slide-negative"], prereq={"allOf": [[vt("rdl-bodyweight", 4)]]})
NORDIC_NOTE = "Anchor the heels under something sturdy (a sofa or heavy furniture); the anchor point is about a fist away from the ankles. Pad the knees with a mat."
COUCH_WARNING = "Couch anchor: check that the sofa is heavy, cannot slide or tip, and that the heels cannot slip out from under it. A partner holding the ankles also works. Keep the volume low: two or three sets of a few reps; the hamstrings need days to recover."
NORDIC_BAND = "Band from the bar: loop a resistance band over the pull-up bar (or another high, sturdy anchor), hold it in both hands in front of you so it lifts you as you lower; a thicker band helps more, a thinner one less."
strength("nordic-negative-banded", "Banded Nordic Curl Negative", "nordic", ["HINGE"], ["LOWER_BODY"], "REPS", "kneeling",
         ["Kneel on a mat with the heels anchored under something sturdy. Hold a resistance band anchored in front of you (door or post) so it takes some weight.", "Keep the body straight from the knees to the head and lower yourself slowly forward; the band helps at the bottom.",
          "Catch yourself with the hands, then push back up to the start. Use a band that lets you lower for about 3-5 seconds.", NORDIC_BAND, NORDIC_NOTE, COUCH_WARNING],
         ["Straight body from knees to head", "Slow lowering", "Hands ready to catch"],
         reps_tiers([2, 3, 4, 5, 6]), RR, HAM_ST, eq=ANCHOR_BAND, rank=30, nxt=["nordic-banded"], prereq={"allOf": [[vt("single-leg-deadlift", 4)]]},
         cautions=(CAUTION_STRENGTH, "Hamstring strain risk: build up slowly; stop on a pulling pain behind the thigh."))
strength("nordic-banded", "Banded Nordic Curl", "nordic", ["HINGE"], ["LOWER_BODY"], "REPS", "kneeling",
         ["Same set-up as the negative: heels anchored, band held in front.", "Lower forward under control, then curl yourself back up using the hamstrings with the band's help.",
          "When this feels easy, repeat with a weaker band; then try the Nordic curl.", NORDIC_BAND, NORDIC_NOTE, COUCH_WARNING],
         ["Straight body from knees to head", "Pull up with the hamstrings", "Weaker band over time"],
         reps_tiers([2, 3, 4, 5, 6]), RR, HAM_ST, eq=ANCHOR_BAND, rank=40, nxt=["nordic-curl"], prereq={"allOf": [[vt("nordic-negative-banded", 4)]]},
         cautions=(CAUTION_STRENGTH, "Hamstring strain risk: build up slowly; stop on a pulling pain behind the thigh."))
strength("nordic-curl", "Nordic Curl", "nordic", ["HINGE"], ["LOWER_BODY"], "REPS", "kneeling",
         ["Kneel on a mat with the heels anchored under something sturdy.", "Lower the body forward as slowly as you can, keeping it straight from the knees to the head.",
          "Use the hands to catch and help up if you need to; aim to come back up with the hamstrings.", NORDIC_NOTE, COUCH_WARNING],
         ["Straight body", "As slow as you can", "Little help from the hands"],
         reps_tiers([1, 2, 3, 4, 5]), RR, HAM_ST, eq=ANCHOR, rank=50, prereq={"allOf": [[vt("nordic-banded", 4)]]},
         cautions=(CAUTION_STRENGTH, "Hamstring strain risk: build up slowly; stop on a pulling pain behind the thigh."))
SLIDE_NOTE = "Use a towel on a smooth floor, socks on a hard floor, or sliders under the heels. A mat under the hands helps if the floor is slippery."
strength("slide-negative", "Hamstring Slide Negative", "slide", ["HINGE"], ["LOWER_BODY"], "REPS", "supine",
         ["Lie on your back with the knees bent and the heels on towels, hips lifted in a bridge.", "Slide both heels slowly away from you until the legs are straight, keeping the hips up.",
          "Bring the heels back with the hands on the floor helping, or by lowering and restarting.", SLIDE_NOTE],
         ["Hips stay high", "Slow slide", "Stop when the hips drop"],
         reps_tiers([3, 4, 5, 6, 8]), RR, HAM_ST, rank=30, nxt=["slide-hamstring"], prereq={"allOf": [[vt("single-leg-deadlift", 4)]]},
         cautions=(CAUTION_STRENGTH, "Hamstring strain risk: build up slowly; stop on a pulling pain behind the thigh."))
strength("slide-hamstring", "Hamstring Slide", "slide", ["HINGE"], ["LOWER_BODY"], "REPS", "supine",
         ["Start in the bridge with the heels on towels or sliders.", "Slide both heels out until the legs are straight, then pull them back using the hamstrings.", "Keep the hips high all the time.", SLIDE_NOTE],
         ["Hips high", "Pull the heels in with the hamstrings"],
         reps_tiers([4, 6, 8, 10, 12]), RR, HAM_ST, rank=40, nxt=["slide-negative-single"], prereq={"allOf": [[vt("slide-negative", 4)]]},
         cautions=(CAUTION_STRENGTH, "Hamstring strain risk: build up slowly; stop on a pulling pain behind the thigh."))
strength("slide-negative-single", "Single-Leg Slide Negative (per side)", "slide", ["HINGE"], ["LOWER_BODY"], "REPS", "supine",
         ["Bridge with one heel on a towel and the other foot lifted.", "Slide the working heel out slowly with the hips staying level and high.", "Bring the foot back and repeat; switch sides after the set.", SLIDE_NOTE],
         ["Hips level", "Slow slide"],
         reps_tiers([3, 4, 5, 6, 8]), RR, HAM_ST, unilateral=True, rank=50, nxt=["slide-single-leg"], prereq={"allOf": [[vt("slide-hamstring", 4)]]},
         cautions=(CAUTION_STRENGTH, "Hamstring strain risk: build up slowly; stop on a pulling pain behind the thigh."))
strength("slide-single-leg", "Single-Leg Slide (per side)", "slide", ["HINGE"], ["LOWER_BODY"], "REPS", "supine",
         ["Bridge with one heel on a towel and the other foot lifted.", "Slide the working heel out and pull it back in with the hamstring; the hips stay level and high.", "Switch sides after the set.", SLIDE_NOTE],
         ["Hips level and high", "Pull the heel in"],
         reps_tiers([3, 4, 5, 6, 8]), RR, HAM_ST, unilateral=True, rank=60, prereq={"allOf": [[vt("slide-negative-single", 4)]]},
         cautions=(CAUTION_STRENGTH, "Hamstring strain risk: build up slowly; stop on a pulling pain behind the thigh."))

# --- core: anti-rotation (Pallof), extension (reverse hyperextension), plus the Minimalist circuit pieces
strength("kettlebell-suitcase-carry", "Kettlebell Suitcase Carry (per side)", "carry", ["CORE_ANTI_LATERAL"], ["CORE", "LOWER_BODY"], "HOLD", "standing",
         ["Hold the kettlebell in one hand at your side like a suitcase and stand tall, shoulders level.", "Walk slowly in a straight line (or on the spot) without leaning toward the bell; the trunk resists being pulled sideways.", "Walk for the time, then switch hands.", KB_WEIGHT],
         ["Stand tall", "Shoulders level", "Do not lean"],
         hold_tiers([20, 30, 40, 50, 60]), [S_NG, S_RR], CORE_ST, eq=KB, unilateral=True, rank=30,
         cautions=(CAUTION_STRENGTH, "Controlled tempo only; no ballistic or swinging work."))
strength("pallof-hold", "Pallof Hold (per side)", "pallof", ["CORE_ANTI_ROTATION"], ["CORE"], "HOLD", "standing",
         ["Anchor a resistance band at chest height beside you and stand sideways to it, hands at the chest.", "Press the hands straight out in front of you and hold with the arms straight while the band tries to turn you.", "Stance rule: feet wide apart is easiest, feet together is harder, a split stance is the hardest. Switch sides after the hold."],
         ["Do not rotate", "Ribs down", "Breathe steadily"],
         hold_tiers([10, 15, 20, 25, 30]), RR, CORE_ST, eq=[{"needs": [need("resistance-band", suitability=["stable-anchor"])]}], unilateral=True, rank=15, nxt=["pallof-press"], tier_profile="H30", tier_note="Tiers 10 / 15 / 20 / 25 / 30 s: the 30 s gate is sourced (RR, owner decision); the steps between are DRAFT.")
strength("pallof-press", "Pallof Press (per side)", "pallof", ["CORE_ANTI_ROTATION"], ["CORE"], "REPS", "standing",
         ["Anchor a resistance band at chest height beside you and stand sideways to it, holding the band at the chest with both hands.", "Press the hands straight out in front of you and pause for a moment with the arms straight.",
          "Do not let the band turn you; bring the hands back to the chest. Do all reps on one side, then switch.", "Stance rule: feet wide apart is easiest, feet together is harder, one foot in front of the other (split stance) is the hardest. Change the stance before you change the band."],
         ["Do not rotate", "Pause with arms straight", "Ribs down"],
         reps_tiers([6, 8, 10, 12, 15]), RR, CORE_ST, eq=[{"needs": [need("resistance-band", suitability=["stable-anchor"])]}], unilateral=True, rank=20, tier_profile="C", tier_note="Core reps 8 to 12: the range is sourced (RR); the steps and the 12-rep gate are DRAFT.")
strength("reverse-hyperextension", "Reverse Hyperextension", "reverse-hyper", ["HINGE"], ["LOWER_BODY", "CORE"], "REPS", "prone",
         ["Lie face down over the edge of a stable table, counter or bed so the hips are at the edge and the legs hang; hold the sides with your hands.", "Squeeze the glutes and raise both legs until they are in line with the body.",
          "Lower with control and repeat. Make sure the surface cannot slide or tip."],
         ["Glutes drive the lift", "Do not swing", "Stop at body height"],
         reps_tiers([6, 8, 10, 12, 15]), RR, CORE_ST, rank=25, tier_profile="C", tier_note="Core reps 8 to 12: the range is sourced (RR); the steps and the 12-rep gate are DRAFT.",
         cautions=(CAUTION_STRENGTH, "Check that the table or counter is stable. Stop on any pinching in the lower back; arch raises (Arch Hold) are the floor alternative."))
strength("plank-shoulder-tap", "Plank Shoulder Taps", "plank-tap", ["CORE_ANTI_EXTENSION"], ["CORE"], "REPS", "floor",
         ["Start in a high plank, hands under the shoulders, feet a little wider than hip width.", "Lift one hand and tap the opposite shoulder without letting the hips rock.",
          "Put it back and tap with the other hand. One tap on each side counts as two reps."],
         ["Hips stay level", "Feet wider for balance", "Slow taps"],
         reps_tiers([6, 8, 10, 12, 16]), RR, CORE_ST, rank=25, prereq={"allOf": [[vt("plank", 2)]]})
strength("walking-lunge", "Walking Lunge", "lunge", ["LUNGE"], ["LOWER_BODY"], "REPS", "standing",
         ["Step forward into a lunge until both knees are bent at about a right angle.", "Push through the front foot and bring the back foot forward into the next step.", "Keep the chest up; each step counts as one rep."],
         ["Chest up", "Front knee over the foot", "Controlled steps"],
         reps_tiers([6, 8, 10, 12, 16]), RR, LOW, rank=15, prereq={"allOf": [[vt("squat-air", 3)]]})


# =============================================================================================================
# V25 (C-E): the pull-up path without a band, from the r/bodyweightfitness wiki (live check 2026-10-09): scapular pulls -> arch hangs -> negatives -> pull-ups.
# =============================================================================================================
strength("dead-hang", "Dead Hang", "pullup", ["PULL_VERTICAL"], ["UPPER_BODY"], "HOLD", "hang",
         ["Hang from a bar with straight arms, hands a little wider than the shoulders, feet off the floor (or just touching it).", "Let the shoulders stay active: do not shrug up to the ears and do not swing.", "Hold, then step down. Use a band or a chair under the feet to take weight off if you need to."],
         ["Straight arms", "Active shoulders", "Breathe steadily"],
         hold_tiers([10, 15, 20, 30, 40]), [S_NG, S_RR], UP, eq=BAR, rank=2, nxt=["scapular-pull"],
         cautions=(CAUTION_STRENGTH, "Stop on shoulder pain or if the grip is failing; step down rather than drop."))
strength("towel-door-row", "Towel Door Row", "row", ["PULL_HORIZONTAL"], ["UPPER_BODY"], "REPS", "standing",
         ["Loop a towel (or a knotted bedsheet) round the handles of a sturdy door and close the door towards you, so it cannot open. Check the knot and the towel before every set.", "Lean back with straight arms and the body in one line, feet near the door.", "Pull the chest to the hands, then lower slowly. The more you lean, the harder it is."],
         ["Straight body", "Slow lowering", "Door stays closed"],
         reps_tiers([6, 8, 10, 12, 15]), [S_RR, S_NG], UP, rank=6, nxt=["inverted-row-bent-knees"],
         cautions=(CAUTION_STRENGTH, "Use a sturdy door that closes towards you and a strong towel or sheet; check the knot and the handles before every set; stop if anything slips."))
strength("kettlebell-one-arm-row", "Kettlebell One-Arm Row (per side)", "kb-row", ["PULL_HORIZONTAL"], ["UPPER_BODY"], "REPS", "standing",
         ["Put one hand and the same-side knee (or just the hand) on a stable chair, back flat, the kettlebell in the other hand hanging straight down.", "Pull the elbow up and back toward the hip, shoulder blade squeezing in.", "Lower slowly to a straight arm; finish a side, then switch.", KB_WEIGHT],
         ["Flat back", "Elbow to the hip", "No twisting"], reps_tiers([6, 8, 10, 12, 15]), [S_NG, S_YG], UP, eq=KB, unilateral=True, rank=25, prereq={"allOf": [[vt("row-band", 4)]]},
         cautions=(CAUTION_STRENGTH, "Controlled tempo only; no ballistic or swinging work."))
strength("scapular-pull", "Scapular Pull", "pullup", ["PULL_VERTICAL"], ["UPPER_BODY"], "REPS", "hang",
         ["Hang from a bar with straight arms (feet may rest on the floor or a chair to take weight off).", "Without bending the elbows, pull the shoulder blades down and together so the body rises a little.",
          "Hold for 3-5 seconds at the top, then lower slowly. Use a band or your feet to help if you cannot do it yet."],
         ["Straight arms", "Shoulders down and back", "Hold 3-5 s at the top"],
         reps_tiers([4, 6, 8, 10, 12]), RR, UP, eq=BAR, rank=5, nxt=["arch-hang"],
         cautions=(CAUTION_STRENGTH, "Stop on shoulder pain; take weight off with the feet if the grip tires."))
strength("arch-hang", "Arch Hang", "pullup", ["PULL_VERTICAL"], ["UPPER_BODY"], "HOLD", "hang",
         ["Hang from a bar and pull the shoulder blades down and back, so the chest comes forward and the back arches a little.", "Work toward the arms reaching about 90 degrees at the shoulder.",
          "Hold, then relax; feet may take weight off at first."],
         ["Straight arms", "Chest forward", "Shoulders away from the ears"],
         hold_tiers([5, 10, 15, 20, 30]), RR, UP, eq=BAR, rank=10, nxt=["pullup-negative", "chair-assisted-pullup", "flexed-arm-hang"], prereq={"allOf": [[vt("scapular-pull", 4)]]},
         cautions=(CAUTION_STRENGTH, "Stop on shoulder pain; take weight off with the feet if the grip tires."))
strength("pullup-negative", "Negative Pull-Up", "pullup", ["PULL_VERTICAL"], ["UPPER_BODY"], "REPS", "hang",
         ["Step or jump up so the chin is over the bar, arms bent.", "Lower yourself as slowly as you can, about 3-5 seconds at first and working toward 10 seconds, to straight arms.",
          "Step back up for the next repetition; do not pull up with the arms."],
         ["Slow lowering", "Chin over the bar to start", "Shoulders stay active"],
         reps_tiers([3, 4, 5, 6, 8]), RR, UP, eq=BAR, rank=25, nxt=["pullup-full"], prereq={"allOf": [[vt("arch-hang", 4)]]},
         cautions=(CAUTION_STRENGTH, "Stop on shoulder or elbow pain; use a stable chair or step to get up."))
strength("flexed-arm-hang", "Flexed-Arm Hang", "pullup", ["PULL_VERTICAL"], ["UPPER_BODY"], "HOLD", "hang",
         ["Step or jump up so the chin is over the bar, arms bent, elbows close to the body.", "Hold the chin above the bar without swinging, breathing steadily.", "Lower slowly to straight arms when the hold ends."],
         ["Chin over the bar", "Elbows in", "No swinging"],
         hold_tiers([3, 5, 8, 12, 15]), [S_NG, S_RR], UP, eq=BAR, rank=26, nxt=["pullup-full"], prereq={"allOf": [[vt("arch-hang", 3)]]},
         cautions=(CAUTION_STRENGTH, "Use a stable chair or step to get up; stop on shoulder or elbow pain."))
strength("chair-assisted-pullup", "Chair-Assisted Pull-Up", "pullup", ["PULL_VERTICAL"], ["UPPER_BODY"], "REPS", "hang",
         ["Stand a sturdy chair under the bar and put one or both feet on it, so the legs take part of the weight.", "Pull the chin over the bar with the arms first; use the legs only as much as you must.", "Lower slowly to straight arms. Use less leg over the weeks."],
         ["Arms do the work", "Slow lowering", "Less leg each week"],
         reps_tiers([3, 5, 6, 8, 10]), [S_NG, S_RR], UP, eq=[{"needs": [need("pullup-bar"), need("chair", suitability=["stable"])]}], rank=24, nxt=["pullup-full"], prereq={"allOf": [[vt("arch-hang", 3)]]},
         cautions=(CAUTION_STRENGTH, "Check that the chair is stable and cannot slide; stop on shoulder or elbow pain."))
strength("chinup-full", "Chin-Up", "pullup", ["PULL_VERTICAL"], ["UPPER_BODY"], "REPS", "hang",
         ["Hang from the bar with the palms facing you, hands about shoulder width.", "Pull until the chin is over the bar, then lower slowly to straight arms.", "A parallel option to the pull-up: the wiki treats them as either-or; the grip changes the load a little toward the biceps."],
         ["Straight arms at the bottom", "Chin over the bar", "No swinging"],
         reps_tiers([1, 3, 5, 6, 8]), [S_NG, S_RR], UP, eq=BAR, rank=30, prereq={"allOf": [[vt("pullup-band-assisted", 4), vt("pullup-negative", 4), vt("chair-assisted-pullup", 4), vt("flexed-arm-hang", 4)]]},
         cautions=(CAUTION_STRENGTH, "Stop on elbow pain."))
for pol in policies:
    if pol["variationId"] == "pullup-full": pol["prerequisiteRule"] = {"allOf": [[vt("pullup-band-assisted", 4), vt("pullup-negative", 4), vt("chair-assisted-pullup", 4), vt("flexed-arm-hang", 4)]]}   # the band path, the negative path, or the chair / hang options


# =============================================================================================================
# V27 (O3, D9): content for the extra equipment. Everything here is optional: the bodyweight-only profile stays complete (tested).
# Dumbbells ("weight", the weight of each), a barbell (the bar plus plates in total) and a weighted vest are loaded equipment: the weight in the
# profile decides the load, and a heavier one starts the exercise again at level 1 (stars at the lighter weight are kept), like the kettlebell.
# =============================================================================================================
DB = [{"needs": [need("weight")]}]
BARBELL = [{"needs": [need("barbell")]}]
VEST = [{"needs": [need("weighted-vest")]}]
LOAD_WEIGHT = "Weight: use a load you can move with a clean position on every rep. The app counts levels per weight: set a heavier weight in your equipment profile and this exercise starts again at level 1, your stars at the old weight are kept."
strength("goblet-squat", "Goblet Squat", "db-squat", ["SQUAT"], ["LOWER_BODY"], "REPS", "standing",
         ["Hold one dumbbell upright at the chest with both hands, feet a little wider than the hips.", "Sit down between the knees, chest tall, elbows inside the knees.", "Stand up by pushing the floor away; keep the weight close.", LOAD_WEIGHT],
         ["Chest tall", "Weight close to the chest", "Heels down"], reps_tiers([6, 8, 10, 12, 15]), [S_NG, S_YG], LOW, eq=DB, rank=15, prereq={"allOf": [[vt("squat-air", 4)]]})
strength("dumbbell-rdl", "Dumbbell Romanian Deadlift", "db-hinge", ["HINGE"], ["LOWER_BODY"], "REPS", "standing",
         ["Stand with a dumbbell in each hand in front of the thighs, a soft bend in the knees.", "Push the hips back and slide the weights down the legs with a flat back.", "Stand tall by driving the hips forward. The arms only hold the weights.", LOAD_WEIGHT],
         ["Hips back", "Flat back", "Weights close to the legs"], reps_tiers([8, 10, 12, 15, 20]), [S_NG, S_YG], HAM_ST, eq=DB, rank=15, nxt=["dumbbell-single-leg-rdl"], prereq={"allOf": [[vt("rdl-bodyweight", 4)]]})
strength("dumbbell-single-leg-rdl", "Dumbbell Single-Leg RDL (per side)", "db-hinge", ["HINGE"], ["LOWER_BODY"], "REPS", "standing",
         ["Stand on one leg with a dumbbell in the hand opposite the standing leg (or one in each hand).", "Hinge forward with the free leg reaching straight behind, back flat.", "Return by squeezing the glute; finish a side, then switch.", LOAD_WEIGHT],
         ["Hips square", "Flat back", "Slow and controlled"], reps_tiers([4, 5, 6, 8, 10]), [S_NG, S_YG], HAM_ST, eq=DB, unilateral=True, rank=25, prereq={"allOf": [[vt("dumbbell-rdl", 4)], [vt("single-leg-deadlift", 3)]]})
strength("weighted-glute-bridge", "Weighted Glute Bridge", "db-bridge", ["HINGE"], ["LOWER_BODY"], "REPS", "supine",
         ["Lie on your back, feet flat, a dumbbell resting on the hips (hold it with both hands).", "Press through the heels to lift the hips until the body is straight from the shoulders to the knees.", "Lower slowly; the weight stays on the hips.", LOAD_WEIGHT],
         ["Squeeze the glutes", "Ribs down", "Slow lowering"], reps_tiers([8, 10, 12, 15, 20]), [S_NG, S_YG], ["stretch-hip-flexor", "stretch-back"], eq=DB, rank=15, prereq={"allOf": [[vt("glute-bridge", 4)]]})
strength("dumbbell-row", "One-Arm Dumbbell Row (per side)", "db-row", ["PULL_HORIZONTAL"], ["UPPER_BODY"], "REPS", "standing",
         ["Put one hand and the same-side knee (or just the hand) on a stable chair or bench, back flat, a dumbbell in the other hand hanging straight down.", "Pull the elbow up and back toward the hip, shoulder blade squeezing in.", "Lower slowly to straight arm; do all reps on one side, then switch.", LOAD_WEIGHT],
         ["Flat back", "Elbow to the hip", "No twisting"], reps_tiers([6, 8, 10, 12, 15]), [S_NG, S_YG], UP, eq=DB, unilateral=True, rank=25, prereq={"allOf": [[vt("row-band", 4)]]})
strength("barbell-squat", "Barbell Squat", "bb-squat", ["SQUAT"], ["LOWER_BODY"], "REPS", "standing",
         ["Rest the bar across the upper back (never on the neck), hands holding it, feet shoulder width apart.", "Sit down until the thighs are about parallel, chest up, knees over the feet.", "Stand up with the whole foot pressing the floor. Use a rack or safety pins if you can; stop a set well before failure.", LOAD_WEIGHT],
         ["Chest up", "Knees over the feet", "Stop short of failure"], reps_tiers([5, 6, 7, 8, 10]), RR, LOW, eq=BARBELL, rank=25, prereq={"allOf": [[vt("squat-air", 4)]]},
         cautions=(CAUTION_STRENGTH, "Heavy load: use a rack or safety pins, keep a spotter or an escape route, and stop a set well before failure."))
strength("barbell-rdl", "Barbell Romanian Deadlift", "bb-hinge", ["HINGE"], ["LOWER_BODY"], "REPS", "standing",
         ["Hold the bar in front of the thighs, hands just outside the legs, a soft bend in the knees.", "Push the hips back and slide the bar down the legs with a flat back until the hamstrings stretch.", "Stand tall by driving the hips forward.", LOAD_WEIGHT],
         ["Hips back", "Bar close to the legs", "Flat back"], reps_tiers([6, 8, 10, 12, 15]), RR, HAM_ST, eq=BARBELL, rank=25, prereq={"allOf": [[vt("rdl-bodyweight", 4)]]},
         cautions=(CAUTION_STRENGTH, "Heavy load: keep the back flat all the time; drop the bar rather than twist if you lose it."))
# the weighted vest as a load for push-ups, pull-ups, dips and squats: the same movement once the plain one is mastered
strength("weighted-pushup", "Weighted Push-Up", "vest-push", ["PUSH_HORIZONTAL"], ["UPPER_BODY", "CORE"], "REPS", "floor",
         ["Put on the vest and get into a push-up position, body in one straight line.", "Lower the chest to the floor and press back up as in a normal push-up.", LOAD_WEIGHT],
         ["Straight body", "Full range"], reps_tiers([5, 6, 8, 10, 12]), RR, UP, eq=VEST, rank=45, prereq={"allOf": [[vt("pushup-standard", 5)]]})
strength("weighted-pullup", "Weighted Pull-Up", "vest-pull", ["PULL_VERTICAL"], ["UPPER_BODY"], "REPS", "hang",
         ["Put on the vest and hang from the bar with straight arms.", "Pull until the chin is over the bar, then lower slowly to straight arms.", LOAD_WEIGHT],
         ["Straight arms at the bottom", "Chin over the bar"], reps_tiers([3, 4, 5, 6, 8]), RR, UP, eq=[{"needs": [need("pullup-bar"), need("weighted-vest")]}], rank=45, prereq={"allOf": [[vt("pullup-full", 5)]]})
strength("weighted-dip", "Weighted Dip", "vest-dip", ["PUSH_VERTICAL"], ["UPPER_BODY"], "REPS", "standing",
         ["Put on the vest and support yourself on the parallel bars (or two sturdy chairs / a counter corner) with straight arms.", "Lower until the upper arms are about parallel to the floor, then press back up.", DIP_NOTE, LOAD_WEIGHT],
         ["Shoulders down", "Full lockout"], reps_tiers([3, 5, 6, 8, 10]), RR, UP, eq=[{"needs": [need("dip-support"), need("weighted-vest")]}], rank=40, prereq={"allOf": [[vt("dip-parallel", 5)]]},
         cautions=(CAUTION_STRENGTH, "Check that the support is stable before every set; stop on shoulder or elbow pain."))
strength("weighted-squat", "Weighted Squat", "vest-squat", ["SQUAT"], ["LOWER_BODY"], "REPS", "standing",
         ["Put on the vest, feet shoulder width apart.", "Sit down until the thighs are about parallel, chest up, then stand.", LOAD_WEIGHT],
         ["Chest up", "Heels down"], reps_tiers([8, 10, 12, 15, 20]), RR, LOW, eq=VEST, rank=15, prereq={"allOf": [[vt("squat-air", 5)]]})

# plank no longer a dead end: top of the plank ladder leads to the hollow hold
for pol in policies:
    if pol["variationId"] == "plank": pol["nextVariationIds"] = ["hollow-tuck"]
    if pol["variationId"] == "pullup-band-assisted": pol["nextVariationIds"] = ["pullup-full"]
    if pol["variationId"] == "pushup-standard": pol["nextVariationIds"] = ["pushup-diamond", "pushup-feet-elevated"]   # L02: siblings (Ebben 2011: a 30 cm decline is about as hard as the diamond)
    if pol["variationId"] == "split-squat": pol["nextVariationIds"] = ["split-squat-bulgarian"]
    if pol["variationId"] == "glute-bridge": pol["nextVariationIds"] = ["bridge-table", "bridge-single-leg"]
    if pol["variationId"] == "row-band": pol["nextVariationIds"] = ["inverted-row-bent-knees"]
    if pol["variationId"] == "superman-hold": pol["nextVariationIds"] = ["arch-hold-y"]
    if pol["variationId"] == "dead-bug": pol["nextVariationIds"] = ["plank"]   # L08: the supported-spine entry comes before the plank
    if pol["variationId"] == "side-plank": pol["nextVariationIds"] = ["side-plank-leg-raise"]
    if pol["variationId"] == "kettlebell-deadlift": pol["nextVariationIds"] = ["kettlebell-single-leg-rdl"]
    # dead bug top hands over to the hollow hold (alternative to a plank-4 prerequisite)
    if pol["variationId"] == "hollow-hold": pol["prerequisiteRule"] = {"allOf": [[vt("hollow-one-leg", 4)]]}
    if pol["variationId"] == "plank": pol["prerequisiteRule"] = {"allOf": [[vt("dead-bug", 3)]]}


# ---------------- U09: muscles worked (F9). Drives the clip colouring, the preview chips and the tree sheet. ----------------
M = {
    "pushup-wall": (["CHEST", "TRICEPS"], ["FRONT_DELTS"]),
    "pushup-incline-high": (["CHEST", "TRICEPS"], ["FRONT_DELTS", "ABS"]),
    "pushup-incline": (["CHEST", "TRICEPS"], ["FRONT_DELTS", "ABS"]),
    "pushup-knee": (["CHEST", "TRICEPS"], ["FRONT_DELTS"]),
    "pushup-standard": (["CHEST", "TRICEPS"], ["FRONT_DELTS", "ABS"]),
    "pushup-feet-elevated": (["CHEST", "FRONT_DELTS"], ["TRICEPS", "ABS"]),
    "pushup-diamond": (["TRICEPS", "CHEST"], ["FRONT_DELTS"]),
    "pushup-archer": (["CHEST", "TRICEPS"], ["FRONT_DELTS", "OBLIQUES"]),
    "pushup-one-arm-negative": (["CHEST", "TRICEPS"], ["FRONT_DELTS", "ABS", "OBLIQUES"]),
    "pushup-one-arm": (["CHEST", "TRICEPS"], ["FRONT_DELTS", "ABS", "OBLIQUES"]),
    "pike-pushup": (["FRONT_DELTS", "TRICEPS"], ["SIDE_DELTS", "UPPER_BACK"]),
    "pike-pushup-elevated": (["FRONT_DELTS", "TRICEPS"], ["SIDE_DELTS", "UPPER_BACK"]),
    "wall-handstand-hold": (["FRONT_DELTS", "TRICEPS"], ["UPPER_BACK", "ABS"]),
    "hspu-wall-negative": (["FRONT_DELTS", "TRICEPS"], ["SIDE_DELTS", "UPPER_BACK", "ABS"]),
    "hspu-wall": (["FRONT_DELTS", "TRICEPS"], ["SIDE_DELTS", "UPPER_BACK", "ABS"]),
    "row-band": (["UPPER_BACK", "LATS"], ["BICEPS", "REAR_DELTS"]),
    "inverted-row-bent-knees": (["UPPER_BACK", "LATS"], ["BICEPS", "REAR_DELTS", "ABS"]),
    "inverted-row": (["UPPER_BACK", "LATS"], ["BICEPS", "REAR_DELTS", "ABS"]),
    "inverted-row-feet-elevated": (["UPPER_BACK", "LATS"], ["BICEPS", "REAR_DELTS", "ABS", "FOREARMS"]),
    "archer-row": (["LATS", "UPPER_BACK"], ["BICEPS", "FOREARMS", "OBLIQUES"]),
    "pullup-band-assisted": (["LATS", "BICEPS"], ["UPPER_BACK", "FOREARMS", "REAR_DELTS"]),
    "pullup-full": (["LATS", "BICEPS"], ["UPPER_BACK", "FOREARMS", "REAR_DELTS"]),
    "pullup-chest-to-bar": (["LATS", "BICEPS"], ["UPPER_BACK", "FOREARMS", "REAR_DELTS"]),
    "muscle-up-bar": (["LATS", "TRICEPS"], ["CHEST", "BICEPS", "FOREARMS"]),
    "squat-air": (["QUADS", "GLUTES"], ["HAMSTRINGS", "CALVES"]),
    "split-squat": (["QUADS", "GLUTES"], ["HAMSTRINGS", "ADDUCTORS"]),
    "split-squat-bulgarian": (["QUADS", "GLUTES"], ["HAMSTRINGS", "HIP_FLEXORS"]),
    "squat-shrimp": (["QUADS", "GLUTES"], ["HAMSTRINGS", "HIP_FLEXORS"]),
    "squat-shrimp-intermediate": (["QUADS", "GLUTES"], ["HAMSTRINGS", "HIP_FLEXORS"]),
    "squat-shrimp-advanced": (["QUADS", "GLUTES"], ["HAMSTRINGS", "HIP_FLEXORS", "CALVES"]),
    "squat-pistol-box": (["QUADS", "GLUTES"], ["HAMSTRINGS", "ADDUCTORS", "CALVES"]),
    "squat-pistol-counterbalance": (["QUADS", "GLUTES"], ["HAMSTRINGS", "ADDUCTORS", "CALVES"]),
    "squat-pistol-assisted": (["QUADS", "GLUTES"], ["HAMSTRINGS", "ADDUCTORS", "CALVES"]),
    "squat-pistol": (["QUADS", "GLUTES"], ["HAMSTRINGS", "ADDUCTORS", "CALVES"]),
    "glute-bridge": (["GLUTES", "HAMSTRINGS"], ["LOWER_BACK"]),
    "bridge-table": (["GLUTES", "TRICEPS"], ["HAMSTRINGS", "FRONT_DELTS"]),
    "bridge-head": (["GLUTES", "LOWER_BACK"], ["FRONT_DELTS", "TRICEPS", "QUADS"]),
    "bridge-wall-walkdown": (["GLUTES", "LOWER_BACK"], ["FRONT_DELTS", "TRICEPS", "QUADS"]),
    "bridge-single-leg": (["GLUTES", "HAMSTRINGS"], ["LOWER_BACK", "OBLIQUES"]),
    "bridge-back": (["GLUTES", "LOWER_BACK"], ["FRONT_DELTS", "TRICEPS", "QUADS"]),
    "superman-hold": (["LOWER_BACK", "GLUTES"], ["REAR_DELTS", "HAMSTRINGS"]),
    "arch-hold-y": (["LOWER_BACK", "GLUTES"], ["UPPER_BACK", "REAR_DELTS"]),
    "arch-rocks": (["LOWER_BACK", "GLUTES"], ["UPPER_BACK"]),
    "kettlebell-deadlift": (["GLUTES", "HAMSTRINGS"], ["LOWER_BACK", "FOREARMS"]),
    "kettlebell-single-leg-rdl": (["HAMSTRINGS", "GLUTES"], ["LOWER_BACK", "FOREARMS"]),
    "kettlebell-swing": (["GLUTES", "HAMSTRINGS"], ["LOWER_BACK", "FRONT_DELTS", "FOREARMS"]),
    "kettlebell-swing-one-arm": (["GLUTES", "HAMSTRINGS"], ["LOWER_BACK", "OBLIQUES", "FRONT_DELTS", "FOREARMS"]),
    "plank": (["ABS"], ["OBLIQUES", "FRONT_DELTS", "GLUTES"]),
    "side-plank": (["OBLIQUES"], ["ABS", "GLUTES", "SIDE_DELTS"]),
    "side-plank-leg-raise": (["OBLIQUES", "GLUTES"], ["ABS", "SIDE_DELTS"]),
    "copenhagen-side-plank": (["ADDUCTORS", "OBLIQUES"], ["ABS", "GLUTES"]),
    "dead-bug": (["ABS"], ["HIP_FLEXORS", "OBLIQUES"]),
    "hollow-hold": (["ABS"], ["HIP_FLEXORS", "QUADS"]),
    "l-sit-floor": (["ABS", "HIP_FLEXORS"], ["TRICEPS", "QUADS", "FRONT_DELTS"]),
    "v-sit-floor": (["ABS", "HIP_FLEXORS"], ["TRICEPS", "QUADS", "FRONT_DELTS"]),
    "leg-raise-lying": (["ABS", "HIP_FLEXORS"], ["OBLIQUES"]),
    "v-up": (["ABS", "HIP_FLEXORS"], ["QUADS"]),
    "front-lever-tuck": (["LATS", "ABS"], ["BICEPS", "FOREARMS", "REAR_DELTS", "UPPER_BACK"]),
    "front-lever-adv-tuck": (["LATS", "ABS"], ["BICEPS", "FOREARMS", "REAR_DELTS", "UPPER_BACK"]),
    "front-lever-straddle": (["LATS", "ABS"], ["BICEPS", "FOREARMS", "REAR_DELTS", "UPPER_BACK"]),
    "planche-lean": (["FRONT_DELTS", "CHEST"], ["TRICEPS", "ABS", "FOREARMS"]),
    "planche-tuck": (["FRONT_DELTS", "CHEST"], ["TRICEPS", "ABS", "FOREARMS"]),
    "planche-adv-tuck": (["FRONT_DELTS", "CHEST"], ["TRICEPS", "ABS", "FOREARMS"]),
    "dead-hang": (["LATS", "FOREARMS"], ["UPPER_BACK", "BICEPS"]),
    "flexed-arm-hang": (["BICEPS", "LATS"], ["UPPER_BACK", "FOREARMS"]),
    "chair-assisted-pullup": (["LATS", "BICEPS"], ["UPPER_BACK", "FOREARMS", "QUADS"]),
    "chinup-full": (["BICEPS", "LATS"], ["UPPER_BACK", "FOREARMS", "REAR_DELTS"]),
    "inverted-row-wide": (["UPPER_BACK", "LATS"], ["BICEPS", "REAR_DELTS", "ABS", "FOREARMS"]),
    "towel-door-row": (["UPPER_BACK", "LATS"], ["BICEPS", "REAR_DELTS", "FOREARMS"]),
    "kettlebell-one-arm-row": (["LATS", "UPPER_BACK"], ["BICEPS", "REAR_DELTS", "FOREARMS"]),
    "scapular-pull": (["LATS", "UPPER_BACK"], ["BICEPS", "FOREARMS", "REAR_DELTS"]),
    "arch-hang": (["LATS", "UPPER_BACK"], ["REAR_DELTS", "FOREARMS", "BICEPS"]),
    "pullup-negative": (["LATS", "BICEPS"], ["UPPER_BACK", "FOREARMS", "REAR_DELTS"]),
    "l-sit-foot-supported": (["ABS", "HIP_FLEXORS"], ["TRICEPS", "FRONT_DELTS", "QUADS"]),
    "l-sit-one-foot": (["ABS", "HIP_FLEXORS"], ["TRICEPS", "FRONT_DELTS", "QUADS"]),
    "l-sit-advanced-tuck": (["ABS", "HIP_FLEXORS"], ["TRICEPS", "FRONT_DELTS", "QUADS"]),
    "l-sit-one-leg": (["ABS", "HIP_FLEXORS"], ["TRICEPS", "FRONT_DELTS", "QUADS"]),
    "l-sit-tuck": (["ABS", "HIP_FLEXORS"], ["TRICEPS", "FRONT_DELTS", "QUADS"]),
    "bridge-incline": (["GLUTES", "HAMSTRINGS"], ["LOWER_BACK", "FRONT_DELTS", "TRICEPS"]),
    "goblet-squat": (["QUADS", "GLUTES"], ["ABS", "UPPER_BACK"]),
    "dumbbell-rdl": (["HAMSTRINGS", "GLUTES"], ["LOWER_BACK", "FOREARMS"]),
    "dumbbell-single-leg-rdl": (["HAMSTRINGS", "GLUTES"], ["LOWER_BACK", "FOREARMS", "CALVES"]),
    "weighted-glute-bridge": (["GLUTES", "HAMSTRINGS"], ["LOWER_BACK"]),
    "dumbbell-row": (["LATS", "UPPER_BACK"], ["BICEPS", "REAR_DELTS", "FOREARMS"]),
    "barbell-squat": (["QUADS", "GLUTES"], ["HAMSTRINGS", "LOWER_BACK", "ABS"]),
    "barbell-rdl": (["HAMSTRINGS", "GLUTES"], ["LOWER_BACK", "FOREARMS"]),
    "weighted-pushup": (["CHEST", "TRICEPS"], ["FRONT_DELTS", "ABS"]),
    "weighted-pullup": (["LATS", "BICEPS"], ["UPPER_BACK", "FOREARMS", "REAR_DELTS"]),
    "weighted-dip": (["TRICEPS", "CHEST"], ["FRONT_DELTS", "UPPER_BACK"]),
    "weighted-squat": (["QUADS", "GLUTES"], ["HAMSTRINGS", "CALVES"]),
    "dip-support-hold": (["TRICEPS", "FRONT_DELTS"], ["CHEST", "UPPER_BACK"]),
    "dip-negative": (["TRICEPS", "CHEST"], ["FRONT_DELTS", "UPPER_BACK"]),
    "dip-parallel": (["TRICEPS", "CHEST"], ["FRONT_DELTS", "UPPER_BACK"]),
    "rdl-bodyweight": (["HAMSTRINGS", "GLUTES"], ["LOWER_BACK"]),
    "single-leg-deadlift": (["HAMSTRINGS", "GLUTES"], ["LOWER_BACK", "CALVES"]),
    "nordic-negative-banded": (["HAMSTRINGS"], ["GLUTES", "CALVES"]),
    "nordic-banded": (["HAMSTRINGS"], ["GLUTES", "CALVES"]),
    "nordic-curl": (["HAMSTRINGS"], ["GLUTES", "CALVES"]),
    "slide-negative": (["HAMSTRINGS", "GLUTES"], ["CALVES", "LOWER_BACK"]),
    "slide-hamstring": (["HAMSTRINGS", "GLUTES"], ["CALVES", "LOWER_BACK"]),
    "slide-negative-single": (["HAMSTRINGS", "GLUTES"], ["CALVES", "LOWER_BACK"]),
    "slide-single-leg": (["HAMSTRINGS", "GLUTES"], ["CALVES", "LOWER_BACK"]),
    "hollow-tuck": (["ABS"], ["HIP_FLEXORS"]),
    "hollow-one-leg": (["ABS"], ["HIP_FLEXORS", "QUADS"]),
    "hollow-rocks": (["ABS"], ["HIP_FLEXORS", "QUADS"]),
    "pallof-hold": (["OBLIQUES", "ABS"], ["FRONT_DELTS", "GLUTES"]),
    "copenhagen-side-plank-short": (["ADDUCTORS", "OBLIQUES"], ["ABS", "GLUTES"]),
    "kettlebell-suitcase-carry": (["OBLIQUES", "FOREARMS"], ["ABS", "GLUTES", "UPPER_BACK"]),
    "pallof-press": (["OBLIQUES", "ABS"], ["FRONT_DELTS", "GLUTES"]),
    "reverse-hyperextension": (["GLUTES", "LOWER_BACK"], ["HAMSTRINGS"]),
    "plank-shoulder-tap": (["ABS", "OBLIQUES"], ["FRONT_DELTS", "GLUTES"]),
    "walking-lunge": (["QUADS", "GLUTES"], ["HAMSTRINGS", "HIP_FLEXORS", "CALVES"]),
}
for v in variations:
    if v["kind"] in ("REPS", "HOLD"):
        p, sec = M[v["id"]]  # KeyError = an exercise without muscles: fix the table
        v["primaryMuscles"], v["secondaryMuscles"] = p, sec

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

# U08 / owner Q2: the questionnaire families. anchor = "Normal" (step 3). Ladders are entry level only: no hard skill is reachable via "I don't know".
onboarding_families = [
    {"id": "pushup", "title": "Push-up", "ladder": ["pushup-incline", "pushup-knee", "pushup-standard", "pushup-feet-elevated"], "anchorVariationId": "pushup-standard"},
    {"id": "squat", "title": "Squat", "ladder": ["squat-air", "split-squat", "split-squat-bulgarian"], "anchorVariationId": "split-squat"},
    {"id": "pull", "title": "Pull and row", "ladder": ["row-band", "pullup-band-assisted", "flexed-arm-hang", "pullup-full"], "anchorVariationId": "pullup-band-assisted"},
    {"id": "core", "title": "Core", "ladder": ["plank", "hollow-hold"], "anchorVariationId": "plank"},
    {"id": "hinge", "title": "Hips and back", "ladder": ["glute-bridge", "bridge-single-leg"], "anchorVariationId": "glute-bridge"},
    {"id": "shoulders", "title": "Shoulders", "ladder": ["pike-pushup", "pike-pushup-elevated"], "anchorVariationId": "pike-pushup", "requiresPushupAtLeast": "pushup-standard"},
]

catalog = {"catalogVersion": 1, "variations": variations, "policies": policies,
           "skillNodes": nodes, "skillEdges": edges,
           "warmupTemplate": ["warmup-shoulder-band", "warmup-squat-sky-reach", "warmup-wrist-prep", "warmup-dead-bug"], "cooldownTemplate": ["stretch-back", "stretch-hamstring"],
           "onboardingFamilies": onboarding_families}

OUT.write_text(json.dumps(catalog, indent=2, ensure_ascii=False) + "\n")
print(f"wrote {OUT}: {len(variations)} variations, {len(policies)} policies")
