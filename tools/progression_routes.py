"""Reviewed R8 route map. No ordering or equivalence is inferred from rank/family.

Each route is authored as a main spine plus explicit non-main memberships.
Unassigned catalog entries are deliberately emitted as LIBRARY memberships.
"""
from __future__ import annotations

ROUTES = [
  dict(id="pushup", name="Push-up", progressionGroup="pushup", goalVariationId="pushup-one-arm", mainPath="pushup-wall pushup-incline-high pushup-incline pushup-standard pushup-feet-elevated pushup-archer pushup-one-arm-elevated pushup-one-arm-negative pushup-one-arm", defaultPool=True, goalIds=["one-arm-pushup"]),
  dict(id="power-pushup", name="Power push-up", progressionGroup="power-pushup", mainPath="pushup-pop-up-incline pushup-kneeling-push-off pushup-pop-up pushup-clap pushup-chest-strike", defaultPool=False, focus="POWER", goalIds=[]),
  dict(id="dip", name="Dip", progressionGroup="dip", mainPath="dip-support-hold dip-negative dip-parallel dip-straight-bar", defaultPool=True, goalIds=[]),
  dict(id="hspu", name="Handstand push-up", progressionGroup="hspu", goalVariationId="hspu-free", mainPath="pike-pushup pike-pushup-elevated wall-handstand-hold hspu-wall-negative hspu-wall hspu-wall-deficit hspu-free-head hspu-free", defaultPool=False, goalIds=["hspu"]),
  dict(id="handstand", name="Handstand balance", progressionGroup="handstand", goalVariationId="handstand-free", mainPath="handstand-wall-plank handstand-wall-walk-up wall-handstand-hold handstand-toe-pulls handstand-wall-one-foot handstand-kick-up handstand-free", defaultPool=False, goalIds=["handstand"]),
  dict(id="planche", name="Planche", progressionGroup="planche", goalVariationId="planche-full", mainPath="planche-frog-stand planche-lean planche-tuck planche-adv-tuck planche-straddle planche-full", defaultPool=False, goalIds=["planche"]),
  dict(id="pullup", name="Pull-up", progressionGroup="pullup", goalVariationId="pullup-full", mainPath="dead-hang scapular-pull arch-hang pullup-negative pullup-full pullup-chest-to-bar", defaultPool=True, goalIds=[]),
  dict(id="one-arm-pullup", name="One-arm pull-up", progressionGroup="pullup", goalVariationId="pullup-one-arm", mainPath="pullup-full pullup-archer pullup-uneven pullup-one-arm-assisted pullup-one-arm-negative pullup-one-arm", defaultPool=False, goalIds=["one-arm-pullup"]),
  dict(id="muscle-up", name="Muscle-up", progressionGroup="pullup", goalVariationId="muscle-up-bar", mainPath="pullup-chest-to-bar pullup-sternum muscle-up-negative muscle-up-bar", defaultPool=False, goalIds=["muscle-up"]),
  dict(id="row", name="Row", progressionGroup="row", mainPath="towel-door-row inverted-row-bent-knees inverted-row inverted-row-feet-elevated archer-row", defaultPool=True, goalIds=[]),
  dict(id="one-arm-row", name="One-arm row", progressionGroup="row", mainPath="archer-row row-one-arm-straddle row-one-arm", defaultPool=False, goalIds=[]),
  dict(id="front-lever", name="Front lever", progressionGroup="front-lever", goalVariationId="front-lever", mainPath="front-lever-tuck front-lever-adv-tuck front-lever-straddle front-lever-one-leg front-lever", defaultPool=False, goalIds=["front-lever"]),
  dict(id="back-lever", name="Back lever", progressionGroup="back-lever", goalVariationId="back-lever", mainPath="german-hang skin-the-cat back-lever-tuck back-lever-adv-tuck back-lever-straddle back-lever-one-leg back-lever", defaultPool=False, goalIds=["back-lever"]),
  dict(id="grip", name="Grip", progressionGroup="grip", mainPath="grip-one-arm-hang grip-towel-hang grip-twin-towel-hang grip-uneven-towel-hang grip-one-arm-towel-hang", defaultPool=False, goalIds=[]),
  dict(id="squat", name="Squat", progressionGroup="squat", goalVariationId="squat-pistol", mainPath="squat-assisted squat-air split-squat split-squat-bulgarian squat-pistol-assisted squat-pistol-box squat-pistol", defaultPool=True, goalIds=["pistol"]),
  dict(id="shrimp-squat", name="Shrimp squat", progressionGroup="squat", goalVariationId="squat-shrimp-advanced", mainPath="squat-shrimp squat-shrimp-intermediate squat-shrimp-advanced", defaultPool=False, goalIds=["shrimp-squat"]),
  dict(id="rdl-hinge", name="Romanian deadlift", progressionGroup="rdl-hinge", mainPath="rdl-bodyweight single-leg-deadlift", defaultPool=True, goalIds=[]),
  dict(id="nordic", name="Nordic curl", progressionGroup="nordic", mainPath="nordic-negative-banded nordic-banded nordic-curl", defaultPool=False, goalIds=[]),
  dict(id="quad", name="Quad strength", progressionGroup="quad", mainPath="sissy-squat-assisted sissy-squat natural-leg-extension", defaultPool=False, goalIds=[]),
  dict(id="calf", name="Calf raise", progressionGroup="calf", mainPath="calf-raise calf-raise-step calf-raise-single calf-raise-step-single calf-raise-single-loaded", defaultPool=True, goalIds=[]),
  dict(id="hip-bridge", name="Hip bridge", progressionGroup="hip-bridge", mainPath="glute-bridge bridge-single-leg", defaultPool=False, goalIds=[]),
  dict(id="hip-thrust", name="Hip thrust", progressionGroup="hip-bridge", mainPath="hip-thrust hip-thrust-single-leg", defaultPool=False, goalIds=[]),
  dict(id="back-bridge", name="Back bridge", progressionGroup="back-bridge", goalVariationId="bridge-back", mainPath="bridge-table bridge-incline bridge-head bridge-half bridge-wall-walkdown bridge-back bridge-wall-walkup bridge-stand-to-stand", defaultPool=False, goalIds=["back-bridge"]),
  dict(id="anti-extension", name="Plank and anti-extension", progressionGroup="anti-extension", mainPath="dead-bug-90-90 dead-bug-single-leg dead-bug plank plank-long-lever body-saw rollout-knees rollout-ramp rollout-negative rollout-standing", defaultPool=True, goalIds=[]),
  dict(id="hollow", name="Hollow", progressionGroup="hollow", mainPath="hollow-tuck hollow-one-leg hollow-hold hollow-rocks", defaultPool=False, goalIds=[]),
  dict(id="lsit", name="L-sit and compression", progressionGroup="lsit", goalVariationId="l-sit-floor", mainPath="l-sit-foot-supported l-sit-one-foot l-sit-tuck l-sit-advanced-tuck l-sit-one-leg l-sit-floor l-sit-straddle v-sit-floor", defaultPool=False, goalIds=["l-sit"]),
  dict(id="bird-dog", name="Bird dog", progressionGroup="bird-dog", mainPath="bird-dog-quadruped bird-dog-arm-raise bird-dog-leg-raise bird-dog", defaultPool=False, goalIds=[]),
  dict(id="floor-legraise", name="Floor leg raise", progressionGroup="legraise", mainPath="lying-knee-tuck lying-bent-knee-raise frog-raise leg-raise-lying v-up", defaultPool=True, goalIds=[]),
  dict(id="hanging-legraise", name="Hanging leg raise", progressionGroup="legraise", mainPath="knee-raise-supported hanging-knee-raise hanging-leg-raise-bent hanging-leg-raise toes-to-bar", defaultPool=True, goalIds=[]),
  dict(id="dragon-flag", name="Dragon flag", progressionGroup="dragon-flag", goalVariationId="dragon-flag", mainPath="dragon-flag-candlestick dragon-flag-tuck dragon-flag-one-leg dragon-flag-negative dragon-flag", defaultPool=False, goalIds=["dragon-flag"]),
  dict(id="side-plank", name="Side plank", progressionGroup="side-plank", mainPath="side-plank copenhagen-side-plank-short copenhagen-side-plank", defaultPool=True, goalIds=[]),
  dict(id="extension", name="Back extension", progressionGroup="extension", mainPath="superman-hold arch-hold-y arch-rocks", defaultPool=True, goalIds=[]),
  dict(id="flag", name="Clutch flag", progressionGroup="flag", goalVariationId="flag-clutch", mainPath="flag-clutch-hang flag-clutch-diagonal-tuck flag-clutch-diagonal flag-clutch-horizontal-tuck flag-clutch-bent-leg flag-clutch", defaultPool=False, goalIds=["flag"]),
  dict(id="press-flag", name="Press flag", progressionGroup="flag", mainPath="flag-clutch flag-support-press flag-vertical-press flag-bent-leg-press flag-press", defaultPool=False, goalIds=[]),
  dict(id="rings-support", name="Ring support", progressionGroup="rings-support", mainPath="ring-support-hold ring-support-rto", defaultPool=False, goalIds=[]),
  dict(id="rings-pushup", name="Ring push-up", progressionGroup="rings-pushup", mainPath="ring-pushup ring-pushup-rto ring-pushup-rto-archer ring-pppu-40 ring-pppu-60", defaultPool=False, goalIds=[]),
  dict(id="rings-dip", name="Ring dip", progressionGroup="rings-dip", mainPath="ring-dip-negative ring-dip ring-dip-rto", defaultPool=False, goalIds=[]),
  dict(id="rings-muscle-up", name="Ring muscle-up", progressionGroup="rings-muscle-up", mainPath="ring-false-grip-hang ring-muscle-up-negative ring-muscle-up", defaultPool=False, goalIds=[]),
  dict(id="elbow-lever", name="Elbow lever", progressionGroup="elbow-lever", mainPath="elbow-lever-umbrella elbow-lever-straddle elbow-lever", defaultPool=False, goalIds=[]),
  dict(id="jump", name="Jump", progressionGroup="jump", mainPath="landing-stick pogo-hop jump-squat jump-tuck box-jump box-jump-rebound depth-jump", defaultPool=False, focus="POWER", goalIds=[]),
  dict(id="bound", name="Bound", progressionGroup="bound", mainPath="jump-lateral jump-broad skater-bound hop-single-leg-stick hop-single-leg-lateral bounding-alternate", defaultPool=False, focus="POWER", goalIds=[]),
  dict(id="burpee", name="Burpee", progressionGroup="burpee", mainPath="squat-thrust burpee burpee-pushup", defaultPool=False, focus="CONDITIONING", goalIds=[]),
  dict(id="press-handstand", name="Press to handstand", progressionGroup="press-handstand", goalVariationId="handstand-press-straddle", mainPath="handstand-free handstand-press-wall-negative handstand-press-elevated handstand-press-straddle", defaultPool=False, goalIds=["handstand"]),
  dict(id="slide-hamstring", name="Slider hamstring curl", progressionGroup="slide-hamstring", goalVariationId="slide-single-leg", mainPath="slide-negative slide-hamstring slide-negative-single slide-single-leg", defaultPool=False, goalIds=[]),
  dict(id="core-flexion", name="Core flexion options", progressionGroup="core-flexion", mainPath="crunch", defaultPool=False, goalIds=[]),
  dict(id="core-rotation", name="Core rotation options", progressionGroup="core-rotation", mainPath="bicycle-crunch", defaultPool=False, goalIds=[]),
  dict(id="pallof-hold", name="Pallof hold", progressionGroup="pallof", mainPath="pallof-hold", defaultPool=False, goalIds=[]),
  dict(id="pallof-press", name="Pallof press", progressionGroup="pallof", mainPath="pallof-press", defaultPool=False, goalIds=[]),
  dict(id="conditioning-pool", name="Conditioning interval pool", progressionGroup="conditioning-pool", mainPath="", defaultPool=False, focus="CONDITIONING", goalIds=[]),
  dict(id="prehab-pool", name="Prehab supports", progressionGroup="prehab-pool", mainPath="", defaultPool=False, focus="PREHAB", goalIds=[]),
  dict(id="seated-twist", name="Seated twist", progressionGroup="seated-twist", mainPath="stretch-twist-easy", defaultPool=False, focus="MOBILITY", goalIds=[]),
  dict(id="program-only", name="Program exercises", progressionGroup="program-only", mainPath="", defaultPool=False, goalIds=[]),
]

# Hand-authored alternate/support/program memberships. Values map route id to
# role and optional anchor; any unlisted item remains explicitly LIBRARY.
EXTRAS = {
 "pushup-diamond":("pushup","ALTERNATE","pushup-feet-elevated"), "pushup-knee":("pushup","ALTERNATE","pushup-incline"), "pushup-uneven":("pushup","ALTERNATE","pushup-archer"), "pushup-half-one-arm":("pushup","ALTERNATE","pushup-archer"), "pushup-one-arm-straight-body":("pushup","ALTERNATE","pushup-one-arm"), "pushup-wall-push-away":("pushup","LIBRARY",None), "pushup-single-leg":("pushup","LIBRARY",None), "pushup-hindu":("pushup","LIBRARY",None), "pushup-pseudo-planche":("planche","SUPPORT","planche-lean"), "pushup-pop-up-wide-close":("power-pushup","ALTERNATE","pushup-pop-up"),
"bend-and-reach":("program-only","PROGRAM",None), "high-jumper":("program-only","PROGRAM",None), "squat-bender":("program-only","PROGRAM",None), "windmill-army":("program-only","PROGRAM",None), "prone-row-army":("program-only","PROGRAM",None), "turn-and-lunge":("program-only","PROGRAM",None), "y-squat":("program-only","PROGRAM",None), "front-kick-toe-touch":("program-only","PROGRAM",None), "leg-tuck-and-twist":("program-only","PROGRAM",None), "side-to-side-knee-lifts":("program-only","PROGRAM",None), "straddle-run":("program-only","PROGRAM",None), "half-squat-laterals":("program-only","PROGRAM",None), "frog-jumps":("program-only","PROGRAM",None), "quarter-turn-jump":("program-only","PROGRAM",None), "criss-cross-jump":("program-only","PROGRAM",None), "alternate-twist-jump":("program-only","PROGRAM",None), "pilates-roll-up":("program-only","PROGRAM",None), "pilates-rolling-like-a-ball":("program-only","PROGRAM",None), "pilates-single-leg-circle":("program-only","PROGRAM",None), "pilates-single-leg-stretch":("program-only","PROGRAM",None), "pilates-double-leg-stretch":("program-only","PROGRAM",None), "pilates-scissors":("program-only","PROGRAM",None), "pilates-spine-stretch-forward":("program-only","PROGRAM",None), "pilates-open-leg-rocker":("program-only","PROGRAM",None), "pilates-corkscrew":("program-only","PROGRAM",None), "pilates-saw":("program-only","PROGRAM",None), "pilates-roll-over":("program-only","PROGRAM",None), "pilates-seal":("program-only","PROGRAM",None), "pilates-spine-twist":("program-only","PROGRAM",None), "pilates-double-leg-kick":("program-only","PROGRAM",None), "pilates-mermaid":("program-only","PROGRAM",None),
  "squat-thrust-step-back":("burpee","ALTERNATE","squat-thrust"),
 "dip-bench-bent-knee":("dip","ALTERNATE","dip-support-hold"), "dip-bench-feet-elevated":("dip","ALTERNATE","dip-negative"), "dip-jumping":("dip","ALTERNATE","dip-negative"), "dip-bench-straight-leg":("dip","LIBRARY",None), "dip-feet-supported":("dip","LIBRARY",None), "dip-russian":("muscle-up","SUPPORT","pullup-sternum"),
 "headstand-tripod-tuck":("handstand","SUPPORT","handstand-wall-plank"), "headstand-tripod":("handstand","SUPPORT","handstand-wall-plank"), "headstand-wall":("handstand","SUPPORT","handstand-wall-plank"), "headstand-forearm":("handstand","SUPPORT","handstand-wall-plank"), "planche-frog-stand-straight-arm":("planche","ALTERNATE","planche-frog-stand"), "planche-tuck-pushup":("planche","SUPPORT","planche-tuck"),
 "pullup-band-assisted":("pullup","ALTERNATE","pullup-negative"), "chair-assisted-pullup":("pullup","ALTERNATE","pullup-negative"), "pullup-jumping":("pullup","ALTERNATE","pullup-negative"), "flexed-arm-hang":("pullup","ALTERNATE","pullup-negative"), "chinup-full":("pullup","ALTERNATE","pullup-full"), "pullup-partial":("pullup","SUPPORT","pullup-full"), "pullup-kip-swing":("pullup","SUPPORT","pullup-chest-to-bar"), "pullup-archer":("one-arm-pullup","MAIN",None), "pullup-typewriter":("one-arm-pullup","ALTERNATE","pullup-archer"), "pullup-l-sit":("one-arm-pullup","ALTERNATE","pullup-uneven"), "pullup-one-arm-flex-hang":("one-arm-pullup","SUPPORT","pullup-one-arm-negative"), "pullup-false-grip-hang":("muscle-up","SUPPORT","pullup-sternum"), "muscle-up-kipping":("muscle-up","LIBRARY",None), "pullup-pullover":("muscle-up","SUPPORT","pullup-sternum"),
 "row-band":("row","ALTERNATE","inverted-row-bent-knees"), "inverted-row-wide":("row","ALTERNATE","inverted-row"), "row-archer-arm-in":("one-arm-row","ALTERNATE","row-one-arm-straddle"),
 "front-lever-row-tuck":("front-lever","SUPPORT","front-lever-tuck"), "front-lever-row-adv-tuck":("front-lever","SUPPORT","front-lever-adv-tuck"), "front-lever-row-straddle":("front-lever","SUPPORT","front-lever-straddle"), "front-lever-row-hang":("front-lever","SUPPORT","front-lever-one-leg"), "front-lever-row":("front-lever","SUPPORT","front-lever"), "ice-cream-maker":("front-lever","SUPPORT","front-lever-adv-tuck"),
 "calf-raise-bent-knee":("calf","ALTERNATE","calf-raise"), "calf-raise-single-bent-knee":("calf","ALTERNATE","calf-raise-single"), "calf-raise-step-bent-knee":("calf","ALTERNATE","calf-raise-step"), "calf-raise-step-single-bent-knee":("calf","ALTERNATE","calf-raise-step-single"),
 "step-up":("squat","ALTERNATE","split-squat"), "step-up-deep":("squat","ALTERNATE","split-squat-bulgarian"), "walking-lunge":("squat","ALTERNATE","split-squat"), "lunge-reverse":("squat","ALTERNATE","split-squat"), "lunge-lateral":("squat","ALTERNATE","split-squat"), "squat-pistol-negative":("squat","ALTERNATE","squat-pistol-box"), "squat-pistol-elevated":("squat","ALTERNATE","squat-pistol-box"), "squat-pistol-counterbalance":("squat","ALTERNATE","squat-pistol-box"), "squat-box":("squat","ALTERNATE","squat-assisted"), "squat-half":("squat","ALTERNATE","squat-assisted"), "squat-wall-sit":("squat","SUPPORT","squat-air"), "squat-cossack":("squat","SUPPORT","split-squat"),
 "bridge-reverse-plank":("back-bridge","SUPPORT","bridge-table"), "bridge-closing":("back-bridge","SUPPORT","bridge-back"), "bridge-wall-push-off":("back-bridge","SUPPORT","bridge-back"),
 "dead-bug-brace":("anti-extension","SUPPORT","dead-bug"), "bent-leg-raise-army":("anti-extension","PROGRAM",None), "l-sit-compression-lift":("lsit","SUPPORT","l-sit-tuck"), "ring-l-sit":("lsit","ALTERNATE","l-sit-floor"), "swimmer":("extension","SUPPORT","superman-hold"), "quadraplex":("bird-dog","PROGRAM",None), "plank-contralateral":("anti-extension","ALTERNATE","plank-long-lever"), "plank-arm-leg-reach":("anti-extension","ALTERNATE","plank-long-lever"), "plank-shoulder-tap":("anti-extension","ALTERNATE","plank-long-lever"), "rollout-one-arm":("anti-extension","LIBRARY",None),
 "hanging-knee-raise-twisting":("hanging-legraise","SUPPORT","toes-to-bar"), "heel-hook":("hanging-legraise","PROGRAM",None), "hanging-windshield-wiper-half":("hanging-legraise","SUPPORT","toes-to-bar"), "hanging-windshield-wiper":("hanging-legraise","SUPPORT","hanging-windshield-wiper-half"), "dragon-flag-adv-tuck":("dragon-flag","ALTERNATE","dragon-flag-tuck"), "side-plank-knee":("side-plank","ALTERNATE","side-plank"), "side-plank-hip-raise":("side-plank","ALTERNATE","side-plank"), "side-plank-hip-raise":("side-plank","ALTERNATE","side-plank"), "back-extension-floor":("extension","ALTERNATE","superman-hold"),
 "lying-knee-tuck":("floor-legraise","MAIN",None), "lying-bent-knee-raise":("floor-legraise","MAIN",None), "seated-knee-raise":("floor-legraise","ALTERNATE","lying-knee-tuck"), "reverse-crunch":("floor-legraise","ALTERNATE","leg-raise-lying"), "v-up-tuck":("floor-legraise","ALTERNATE","v-up"), "v-up-straddle":("floor-legraise","ALTERNATE","v-up"), "windshield-wiper-bent-knee":("floor-legraise","SUPPORT","leg-raise-lying"), "windshield-wiper-floor":("floor-legraise","SUPPORT","leg-raise-lying"),
 "stretch-twist-straight-leg":("seated-twist","ALTERNATE","stretch-twist-easy"), "stretch-twist-half":("seated-twist","ALTERNATE","stretch-twist-easy"), "stretch-twist-three-quarter":("seated-twist","ALTERNATE","stretch-twist-easy"), "stretch-twist-full":("seated-twist","ALTERNATE","stretch-twist-easy"),
 "sit-up":("core-flexion","ALTERNATE","crunch"), "rower-situp":("core-flexion","ALTERNATE","crunch"), "curl-up-mcgill":("core-flexion","ALTERNATE","crunch"), "russian-twist":("core-rotation","ALTERNATE","bicycle-crunch"),
 "flag-clutch-diagonal-split":("flag","ALTERNATE","flag-clutch-diagonal"), "flag-clutch-horizontal-split":("flag","ALTERNATE","flag-clutch-horizontal-tuck"), "flag-press-hang":("press-flag","LIBRARY",None), "flag-kick-press":("press-flag","LIBRARY",None), "flag-vertical-chamber-press":("press-flag","LIBRARY",None), "flag-split-press":("press-flag","LIBRARY",None),
}

# Memberships that intentionally appear in more than one route. Supports can
# belong to the exercise they prepare and to the deliberate prehab collection.
EXTRA_MEMBERSHIPS = [
    ("crunch", "floor-legraise", "ALTERNATE", "leg-raise-lying"),
    ("sit-up", "floor-legraise", "ALTERNATE", "leg-raise-lying"),
    ("rower-situp", "floor-legraise", "ALTERNATE", "leg-raise-lying"),
    ("curl-up-mcgill", "floor-legraise", "ALTERNATE", "leg-raise-lying"),
    ("bicycle-crunch", "side-plank", "ALTERNATE", "side-plank"),
    ("russian-twist", "side-plank", "ALTERNATE", "side-plank"),
    ("pallof-hold", "side-plank", "ALTERNATE", "side-plank"),
    ("pallof-press", "side-plank", "ALTERNATE", "side-plank"),
    ("bird-dog", "side-plank", "ALTERNATE", "side-plank"),
    ("wrist-palm-pulses", "handstand", "SUPPORT", "handstand-wall-plank"),
    ("wrist-seated-mobility", "handstand", "SUPPORT", "handstand-wall-plank"),
    ("forearm-rotation-fist", "handstand", "SUPPORT", "handstand-wall-plank"),
    ("wrist-pushup-knees", "handstand", "SUPPORT", "handstand-wall-plank"),
    ("wrist-pushup", "handstand", "SUPPORT", "handstand-wall-plank"),
    ("wrist-curl-eccentric", "handstand", "SUPPORT", "handstand-wall-plank"),
    ("wrist-palm-pulses", "planche", "SUPPORT", "planche-lean"),
    ("wrist-seated-mobility", "planche", "SUPPORT", "planche-lean"),
    ("forearm-rotation-fist", "planche", "SUPPORT", "planche-lean"),
    ("wrist-pushup-knees", "planche", "SUPPORT", "planche-lean"),
    ("wrist-pushup", "planche", "SUPPORT", "planche-lean"),
    ("wrist-curl-eccentric", "planche", "SUPPORT", "planche-lean"),
    ("prone-i-raise", "dip", "SUPPORT", "dip-parallel"),
    ("prone-t-raise", "dip", "SUPPORT", "dip-parallel"),
    ("prone-y-raise", "dip", "SUPPORT", "dip-parallel"),
    ("prone-l-raise", "dip", "SUPPORT", "dip-parallel"),
    ("prone-w-raise", "dip", "SUPPORT", "dip-parallel"),
    ("external-rotation-side-lying", "dip", "SUPPORT", "dip-parallel"),
    ("external-rotation-band", "dip", "SUPPORT", "dip-parallel"),
    ("external-rotation-band-90", "dip", "SUPPORT", "dip-parallel"),
    ("band-pull-apart", "dip", "SUPPORT", "dip-parallel"),
    ("face-pull", "dip", "SUPPORT", "dip-parallel"),
    ("cuban-press", "dip", "SUPPORT", "dip-parallel"),
    ("prone-i-raise", "hspu", "SUPPORT", "hspu-wall"),
    ("prone-t-raise", "hspu", "SUPPORT", "hspu-wall"),
    ("prone-y-raise", "hspu", "SUPPORT", "hspu-wall"),
    ("prone-l-raise", "hspu", "SUPPORT", "hspu-wall"),
    ("prone-w-raise", "hspu", "SUPPORT", "hspu-wall"),
    ("external-rotation-side-lying", "hspu", "SUPPORT", "hspu-wall"),
    ("external-rotation-band", "hspu", "SUPPORT", "hspu-wall"),
    ("external-rotation-band-90", "hspu", "SUPPORT", "hspu-wall"),
    ("band-pull-apart", "hspu", "SUPPORT", "hspu-wall"),
    ("face-pull", "hspu", "SUPPORT", "hspu-wall"),
    ("cuban-press", "hspu", "SUPPORT", "hspu-wall"),
    ("spanish-squat", "squat", "SUPPORT", "squat-pistol"),
    ("terminal-knee-extension", "squat", "SUPPORT", "squat-pistol"),
    ("petersen-step-up", "squat", "SUPPORT", "split-squat"),
    ("spanish-squat", "quad", "SUPPORT", "sissy-squat"),
    ("terminal-knee-extension", "quad", "SUPPORT", "sissy-squat"),
    ("petersen-step-up", "quad", "SUPPORT", "sissy-squat"),

]

PREHAB_SUPPORT_ROUTES = {"handstand", "planche", "dip", "hspu", "squat", "quad"}
PREHAB_SOURCE_MEMBERSHIPS = [
    item for item in EXTRA_MEMBERSHIPS if item[1] in PREHAB_SUPPORT_ROUTES and item[2] == "SUPPORT"
]
EXTRA_MEMBERSHIPS.extend(
    (variation_id, "prehab-pool", role, anchor)
    for variation_id, (role, anchor) in {
        item[0]: (item[2], item[3]) for item in reversed(PREHAB_SOURCE_MEMBERSHIPS)
    }.items()
)

CONDITIONING_POOL_IDS = {
    "sprawl", "mountain-climber-slow", "mountain-climber", "mountain-climber-cross-body",
    "half-jack", "jumping-jack", "seal-jack", "star-jack", "butt-kicks", "high-knees",
    "skipping", "side-skip", "power-skip", "jump-rope", "jump-rope-double-under",
    "lateral-shuffle", "carioca", "shuttle-sprint", "crouch-run", "four-square-hops", "hexagon-drill",
    "bear-plank", "bear-crawl", "crab-walk", "inchworm", "frogger", "shoulder-roll", "rolling-sit-up", "rolling-squat", "shoulder-pop",
}

PROGRESSION_GROUP_OVERRIDES = {
    "weighted-pushup": "pushup", "weighted-pullup": "pullup", "weighted-dip": "dip",
    "weighted-squat": "squat", "barbell-squat": "squat", "goblet-squat": "squat",
    "weighted-glute-bridge": "hip-bridge", "barbell-rdl": "rdl-hinge", "dumbbell-rdl": "rdl-hinge",
    "dumbbell-single-leg-rdl": "rdl-hinge", "kettlebell-single-leg-rdl": "rdl-hinge",
    "dumbbell-row": "row", "kettlebell-one-arm-row": "row",
    "leg-raise-lying": "legraise", "v-up": "legraise", "lying-knee-tuck": "legraise",
    "lying-bent-knee-raise": "legraise", "frog-raise": "legraise", "seated-knee-raise": "legraise",
    "reverse-crunch": "legraise", "v-up-tuck": "legraise", "v-up-straddle": "legraise",
    "windshield-wiper-bent-knee": "legraise", "windshield-wiper-floor": "legraise",
    "knee-raise-supported": "legraise", "hanging-knee-raise": "legraise",
    "hanging-knee-raise-twisting": "legraise", "hanging-leg-raise-bent": "legraise",
    "hanging-leg-raise": "legraise", "toes-to-bar": "legraise", "heel-hook": "legraise",
    "hanging-windshield-wiper-half": "legraise", "hanging-windshield-wiper": "legraise",
}

DEFAULT_POOL_ROUTES = {r["id"] for r in ROUTES if r.get("defaultPool")}

# Authored readiness thresholds for each main-path spine. These are not
# mastery auto-advance thresholds; existing reviewed gates take precedence.
ROUTE_GATE_TIERS = {
    "pushup": 3, "power-pushup": 3, "dip": 3, "hspu": 3, "handstand": 3,
    "planche": 3, "pullup": 3, "one-arm-pullup": 3, "muscle-up": 3,
    "row": 3, "one-arm-row": 3, "front-lever": 3, "back-lever": 3,
    "grip": 3, "squat": 3, "shrimp-squat": 3, "rdl-hinge": 3,
    "nordic": 3, "quad": 3, "calf": 3, "hip-bridge": 3, "hip-thrust": 3,
    "back-bridge": 3, "anti-extension": 3, "hollow": 3, "lsit": 3,
    "bird-dog": 3, "floor-legraise": 3, "hanging-legraise": 3,
    "dragon-flag": 3, "side-plank": 3, "extension": 3, "flag": 3,
    "press-flag": 3, "rings-support": 3, "rings-pushup": 3, "rings-dip": 3,
    "rings-muscle-up": 3, "elbow-lever": 3, "jump": 3, "bound": 3,
    "burpee": 3, "press-handstand": 3, "slide-hamstring": 3,
    "core-flexion": 3, "core-rotation": 3, "pallof-hold": 3,
    "pallof-press": 3, "conditioning-pool": 3, "prehab-pool": 3,
    "seated-twist": 3, "program-only": 3,
}

# These drills share one selection key in the dedicated prehab pool. Their
# separate SUPPORT memberships stay anchored to the routes they prepare.
PREHAB_IDS = {item[0] for item in PREHAB_SOURCE_MEMBERSHIPS}













# Explicit reviewed LIBRARY role table, checked for exact catalog coverage.
EXPLICIT_LIBRARY_IDS = set("""
ankle-hop-hip-twist
ankle-hop-side-to-side
arms-biceps-curl
arms-triceps-extension
balance-single-leg
balance-single-leg-cushion
balance-single-leg-eyes-closed
barbell-rdl
barbell-squat
bounding-single-leg
box-push-off-alternating
box-push-off-single
box-step-off-landing
chin-nod-supine
chin-tuck-head-lift
chin-tuck-seated
clamshell
cone-hops-front
cone-hops-lateral
copenhagen-adduction
deep-squat-hold
dip-forward-lean
dip-korean
dip-l-sit
donkey-kick
dumbbell-rdl
dumbbell-row
dumbbell-single-leg-rdl
eight-count-t-pushup
fire-hydrant
floor-angel
goblet-squat
grip-eagle-claw
grip-horizontal-hang
grip-uneven-hang
handstand-press-bent-arm
harop-curl
harop-curl-beginner
hip-lateral-leg-raise
hip-medial-leg-raise
hip-raise-pushup
jefferson-curl
joint-circles
jump-block
jump-broad-consecutive
jump-butt-kick
jump-split
jump-split-cycle
jump-tuck-catch
jump-tuck-slap
jump-vertical
kettlebell-deadlift
kettlebell-one-arm-row
kettlebell-single-leg-rdl
kettlebell-suitcase-carry
kettlebell-swing
kettlebell-swing-one-arm
leg-swings
neck-head-raise-manual
neck-isometric-4-way
pike-pushup-hands-elevated
pullup-commando
pullup-kipping
pullup-pullover-jumping
reverse-hyperextension
reverse-nordic
ring-dip-l
ring-pushup-wide
scapular-pushup
scapular-pushup-knee
scapular-pushup-wall
short-foot
side-lying-leg-tuck
side-plank-leg-raise
squat-close
squat-horse-stance
squat-shrimp-deficit
squat-shrimp-two-hand
squat-uneven
stair-descent-slow
stretch-90-90
stretch-ankle-mobility
stretch-back
stretch-butterfly
stretch-calf-wall
stretch-chest-door
stretch-cobra
stretch-downward-dog
stretch-figure-four
stretch-frog
stretch-groin
stretch-hamstring
stretch-hamstring-standing
stretch-hip-flexor
stretch-knee-to-chest
stretch-lat-wall
stretch-neck-side
stretch-overhead-arm-pull
stretch-pancake
stretch-pigeon
stretch-pike-one-leg
stretch-quad-couch
stretch-quad-standing
stretch-rear-clasp
stretch-shoulder
stretch-shoulder-backbend
stretch-shoulder-er
stretch-sleeper
stretch-sphinx
stretch-spiderman
stretch-split-front
stretch-split-middle
stretch-supine-twist
stretch-thoracic
stretch-wrist-biceps
stretch-wrist-extensor
stretch-wrist-flexor
stretch-wrist-prayer
stretch-wrist-reverse-prayer
support-scapular-shrug
tibialis-raise
tibialis-raise-single
wall-slide
warmup-arch-hang
warmup-dead-bug
warmup-hinge-easier
warmup-shoulder-band
warmup-shoulder-towel
warmup-squat-easier
warmup-squat-sky-reach
warmup-support-hold
warmup-wrist-prep
weighted-dip
weighted-glute-bridge
weighted-pullup
weighted-pushup
weighted-squat
zigzag-hop
""".split())
