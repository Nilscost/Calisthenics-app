"""Core exercises: planks, side planks, dead bug, hollow body, leg raises, L-sits, carry, pallof.

Lying face up (dead bug, hollow, leg raises, V-up): p1 = p2 = -90 (or -40 for the V). Face down (planks): p ~ +80.
Side-lying (side plank, Copenhagen): p1 = p2 = 90, roll = -90 (right side up, supported on the left forearm).
Structure follows families/squat_lowerlegs.py; the plank numbers follow families/push_shoulders.py (_line)."""
import math

from poses import base_pose, both, rep, pt

L_SH = 0.47  # pelvis centre to shoulder along the trunk (lumbar 0.24 + thorax 0.26 - 0.03)


def _c(deg):
    return math.cos(math.radians(deg))


def _s(deg):
    return math.sin(math.radians(deg))


# --- planks (face down, hands or forearms on the floor) ---------------------------------------
def _forearm_plank(bump=0.0):
    # Toes at x=-1.35, body line about 10 deg above the floor (shoulders ~0.31 high); elbows on the floor under the shoulders.
    a, ax = 11, -1.35
    p0 = [ax + 0.898 * _c(a), 0.0, 0.08 + 0.898 * _s(a)]
    sx = p0[0] + L_SH * _c(a)
    return base_pose(pelvis=[p0[0], 0.0, p0[2] + bump], p1=90 - a, p2=90 - a, hp=-8,
                     **both(wrist=[sx + 0.27, 0.20, 0.05], elbow=[-0.2, 0.3, -1.0], hand=[1, 0, 0],
                            palm=[0, 0, -1], grip=0.0, knee=[0, 0, 1], foot=[0.95, 0, -0.3]),
                     ankle_l=[ax, 0.10, 0.08], ankle_r=[ax, -0.10, 0.08])


def plank():
    a = _forearm_plank(0.005)
    b = _forearm_plank(-0.005)
    return dict(seconds=3.0, keys=rep(a, b, down_end=0.5), cam_az=30, cam_el=10, props=[])


def _side_forearm_base(pz, top_ankle, bottom_ankle=(-0.867, 0.0, 0.08)):
    """Lying on the left side (roll -90), left forearm on the floor; right side up."""
    return base_pose(pelvis=[0.0, 0.0, pz], p1=90, p2=90, roll=-90, hp=0,
                     wrist_l=[0.22, 0.24, 0.035], elbow_l=[-1.0, 0.0, -0.5], hand_l=[0, 1, 0],
                     palm_l=[0, 0, -1], grip_l=0.0,
                     ankle_l=list(bottom_ankle), foot_l=[0, 1, 0], knee_l=[0, 0, 1],
                     wrist_r_rel=[-0.20, 0.0, 0.40], elbow_r=[0.0, 0.0, 1.0], hand_r=[-1, 0, 0],
                     palm_r=[0, 0, 1], grip_r=0.0,
                     ankle_r=list(top_ankle), foot_r=[0, 1, 0], knee_r=[0, 0, 1])


def side_plank():
    a = _side_forearm_base(0.390, (-0.867, 0.0, 0.26))
    b = _side_forearm_base(0.380, (-0.867, 0.0, 0.26))
    return dict(seconds=3.0, keys=rep(a, b), cam_az=30, cam_el=10, props=[])


def side_plank_leg_raise():
    a = _side_forearm_base(0.390, (-0.867, 0.0, 0.26))
    b = _side_forearm_base(0.390, (-0.895, 0.0, 0.566))
    return dict(seconds=3.5, keys=rep(a, b, down_end=0.5), cam_az=30, cam_el=10, props=[])


def _copenhagen(pz, top_ankle, bench):
    return _side_forearm_base(pz, top_ankle, bottom_ankle=(0.55, 0.0, 0.12)), [bench]


def copenhagen_side_plank_short():
    # Top knee on the bench (bench top 0.45 m); the top shin hangs past the bench; bottom leg tucked forward.
    bench = dict(kind="box", center=[-0.44, 0.0, 0.225], size=[0.22, 0.6, 0.45])
    a, props = _copenhagen(0.405, (-0.62, 0.0, 0.16), bench)
    b, _ = _copenhagen(0.395, (-0.62, 0.0, 0.16), bench)
    return dict(seconds=3.0, keys=rep(a, b), cam_az=30, cam_el=10, props=props)


def copenhagen_side_plank():
    # Full version: top foot (ankle) resting on the bench (top 0.45 m); bottom leg tucked forward.
    bench = dict(kind="box", center=[-0.92, 0.0, 0.225], size=[0.30, 0.6, 0.45])
    a, props = _copenhagen(0.405, (-0.86, 0.0, 0.52), bench)
    b, _ = _copenhagen(0.395, (-0.86, 0.0, 0.52), bench)
    return dict(seconds=3.0, keys=rep(a, b), cam_az=30, cam_el=10, props=props)


def _high_plank(bump=0.0, tap=False):
    # Straight arms under the shoulders (shoulder height ~0.60), feet a little wider than hip width.
    a, ax = 22.5, -1.245
    p0 = [ax + 0.898 * _c(a), 0.0, 0.08 + 0.898 * _s(a) + bump]
    sx = p0[0] + L_SH * _c(a)
    sz = p0[2] + L_SH * _s(a)
    right = [0.02, 0.12, 0.58] if tap else [sx, -0.18, 0.035]
    return base_pose(pelvis=p0[:2] + [p0[2] + bump] if False else [p0[0], 0.0, p0[2]], p1=90 - a, p2=90 - a, hp=-8,
                     wrist_l=[sx, 0.18, 0.035], wrist_r=right,
                     elbow_l=[-0.6, 0.7, 1.0], elbow_r=[-0.6, -0.7, 1.0],
                     hand_l=[1, 0, 0], hand_r=[1, 0, 0], palm_l=[0, 0, -1], palm_r=[0, 0, -1],
                     grip_l=0.0, grip_r=0.0, knee_l=[0, 0, -1], knee_r=[0, 0, -1],
                     ankle_l=[ax, 0.14, 0.08], ankle_r=[ax, -0.14, 0.08],
                     foot_l=[0.95, 0, -0.3], foot_r=[0.95, 0, -0.3])


def plank_shoulder_tap():
    a = _high_plank(0.0, tap=False)
    b = _high_plank(0.0, tap=True)
    return dict(seconds=3.0, keys=rep(a, b, down_end=0.5), cam_az=30, cam_el=10, props=[])


# --- lying face up -----------------------------------------------------------------------------
def _lie(p1=-90, p2=-90, pz=0.11, hp=0, **over):
    k = dict(p1=p1, p2=p2, hp=hp, hand_l=[-1, 0, 0], hand_r=[-1, 0, 0])
    k.update(over)
    return base_pose(pelvis=[0.0, 0.0, pz], **k)


ARMS_UP = dict(wrist_l_rel=[0.0, 0.03, 0.56], wrist_r_rel=[0.0, -0.03, 0.56])
ARMS_BY_EARS = dict(wrist_l_rel=[-0.55, 0.12, -0.07], wrist_r_rel=[-0.55, -0.12, 0.0])


def dead_bug():
    # Arms up, knees over the hips (tabletop). Lower the right arm overhead and extend the left leg.
    a = _lie(**ARMS_UP, elbow_r=[-1, -0.2, 1.0], ankle_l_rel=[0.43, 0.0, 0.44], ankle_r_rel=[0.43, 0.0, 0.44],
             knee_l=[0.3, 0, 1], knee_r=[0.3, 0, 1])
    b = _lie(wrist_l_rel=[0.0, 0.03, 0.56], wrist_r_rel=[-0.55, -0.12, 0.0], elbow_r=[-1, -0.2, 1.0],
             ankle_l_rel=[0.85, 0.0, 0.0], ankle_r_rel=[0.43, 0.0, 0.44], knee_l=[0, 0, 1], knee_r=[0.3, 0, 1])
    return dict(seconds=3.0, keys=rep(a, b), cam_az=30, cam_el=10, props=[])


def hollow_hold():
    a = _lie(p2=-80, pz=0.115, **ARMS_BY_EARS, ankle_l_rel=[0.85, 0.0, 0.12], ankle_r_rel=[0.85, 0.0, 0.12],
             knee_l=[0, 0, 1], knee_r=[0, 0, 1])
    b = _lie(p2=-80, pz=0.105, **ARMS_BY_EARS, ankle_l_rel=[0.85, 0.0, 0.12], ankle_r_rel=[0.85, 0.0, 0.12],
             knee_l=[0, 0, 1], knee_r=[0, 0, 1])
    return dict(seconds=3.0, keys=rep(a, b), cam_az=30, cam_el=10, props=[])


def hollow_tuck():
    a = _lie(p2=-80, pz=0.115, **ARMS_BY_EARS, ankle_l_rel=[0.12, 0.0, 0.42], ankle_r_rel=[0.12, 0.0, 0.42],
             knee_l=[0, 0, 1], knee_r=[0, 0, 1])
    b = _lie(p2=-80, pz=0.105, **ARMS_BY_EARS, ankle_l_rel=[0.12, 0.0, 0.42], ankle_r_rel=[0.12, 0.0, 0.42],
             knee_l=[0, 0, 1], knee_r=[0, 0, 1])
    return dict(seconds=3.0, keys=rep(a, b), cam_az=30, cam_el=10, props=[])


def hollow_one_leg():
    # Right leg straight a few cm above the floor, left knee tucked.
    a = _lie(p2=-80, pz=0.115, **ARMS_BY_EARS, ankle_l_rel=[0.12, 0.0, 0.42], ankle_r_rel=[0.85, 0.0, 0.12],
             knee_l=[0, 0, 1], knee_r=[0, 0, 1])
    b = _lie(p2=-80, pz=0.105, **ARMS_BY_EARS, ankle_l_rel=[0.12, 0.0, 0.42], ankle_r_rel=[0.85, 0.0, 0.12],
             knee_l=[0, 0, 1], knee_r=[0, 0, 1])
    return dict(seconds=3.0, keys=rep(a, b), cam_az=30, cam_el=10, props=[])


def hollow_rocks():
    # Rock like a boat: the hollow body (a) and the rocked-back position with arms and legs lower (b).
    a = _lie(p2=-80, pz=0.115, **ARMS_BY_EARS, ankle_l_rel=[0.85, 0.0, 0.12], ankle_r_rel=[0.85, 0.0, 0.12],
             knee_l=[0, 0, 1], knee_r=[0, 0, 1])
    b = _lie(p2=-88, pz=0.11, **ARMS_BY_EARS, ankle_l_rel=[0.85, 0.0, 0.04], ankle_r_rel=[0.85, 0.0, 0.04],
             knee_l=[0, 0, 1], knee_r=[0, 0, 1])
    return dict(seconds=3.5, keys=rep(a, b, down_end=0.5), cam_az=30, cam_el=10, props=[])


def leg_raise_lying():
    # Legs straight: flat on the floor (b, lowered near the floor), then vertical (a).
    a = _lie(**ARMS_BY_EARS, ankle_l_rel=[0.0, 0.0, 0.85], ankle_r_rel=[0.0, 0.0, 0.85],
             knee_l=[1, 0, 0], knee_r=[1, 0, 0])
    b = _lie(**ARMS_BY_EARS, ankle_l_rel=[0.85, 0.0, 0.04], ankle_r_rel=[0.85, 0.0, 0.04],
             knee_l=[0, 0, 1], knee_r=[0, 0, 1])
    return dict(seconds=3.5, keys=rep(a, b, down_end=0.5), cam_az=30, cam_el=10, props=[])


def v_up():
    # a: lying, arms overhead. b: V, torso and straight legs at about 45 deg to the floor, hands reach to the toes.
    a = _lie(**ARMS_BY_EARS, ankle_l_rel=[0.85, 0.0, 0.0], ankle_r_rel=[0.85, 0.0, 0.0],
             knee_l=[0, 0, 1], knee_r=[0, 0, 1])
    b = _lie(p1=-40, p2=-40, hp=0, wrist_l_rel=[0.50, 0.12, 0.03], wrist_r_rel=[0.50, -0.12, 0.03],
             ankle_l_rel=[0.66, 0.0, 0.55], ankle_r_rel=[0.66, 0.0, 0.55], knee_l=[0, 0, 1], knee_r=[0, 0, 1])
    return dict(seconds=3.0, keys=rep(a, b), cam_az=30, cam_el=10, props=[])


# --- L-sits: hands on the floor beside the hips, arms straight (shoulder z 0.60) ---------------
def _sit(p, legs_l, legs_r, bump=0.0):
    """legs_*: ('rel', [dx, dy, dz], knee_pole) offset from the hip, or ('w', [x, y, z], knee_pole) world."""
    pz = 0.60 - L_SH * _c(p) + bump
    sx = L_SH * _s(p)
    hip_x = -0.03 * _s(p)
    k = dict(p1=p, p2=p, hp=0,
             wrist_l=[sx - 0.10, 0.18, 0.035], wrist_r=[sx - 0.10, -0.18, 0.035],
             elbow_l=[-1.0, 0.2, 0.0], elbow_r=[-1.0, -0.2, 0.0], hand_l=[1, 0, 0], hand_r=[1, 0, 0],
             palm_l=[0, 0, -1], palm_r=[0, 0, -1], grip_l=0.0, grip_r=0.0)
    for side, spec, sg in (("l", legs_l, 1), ("r", legs_r, -1)):
        kind, v, knee = spec
        if kind == "rel":
            k["ankle_" + side + "_rel"] = list(v)
        else:
            k["ankle_" + side] = list(v)
        k["knee_" + side] = list(knee)
        k["foot_" + side] = [1, 0, 0]
    return base_pose(pelvis=[0.0, 0.0, pz], **k)


def _heels(p, side):
    # Heel on the floor, leg straight forward from the hip.
    sg = 1 if side == "l" else -1
    hip_x = -0.03 * _s(p)
    return ("w", [hip_x + 0.86, 0.09 * sg, 0.08], [0, 0, 1])


STRAIGHT = ("rel", [0.85, 0.0, 0.0], [0, 0, 1])
STRAIGHT_FULL = ("rel", [0.868, 0.0, 0.0], [0, 0, 1])
LIFT_FULL = ("rel", [0.665, 0.0, 0.557], [0, 0, 1])
PARA_WRIST_Z = 0.145  # hand on a parallette, top 0.11 m
PARA_SH_Z = 0.715     # shoulder height with straight arms (wrist -> shoulder 0.57)


def _para(sx, sy):
    return dict(kind="box", center=[sx, sy, 0.055], size=[0.10, 0.04, 0.11])


def _sit_para(p, legs_l, legs_r, bump=0.0):
    """L-sit with the hands on two parallettes (top 0.11 m), straight arms, pelvis ~0.24 m."""
    s = math.radians(p)
    pz = PARA_SH_Z - L_SH * math.cos(s) + bump
    sx = L_SH * math.sin(s)
    k = dict(p1=p, p2=p, hp=0,
             wrist_l=[sx, 0.18, PARA_WRIST_Z], wrist_r=[sx, -0.18, PARA_WRIST_Z],
             elbow_l=[-1.0, 0.2, 0.0], elbow_r=[-1.0, -0.2, 0.0], hand_l=[1, 0, 0], hand_r=[1, 0, 0],
             palm_l=[0, 0, -1], palm_r=[0, 0, -1], grip_l=0.9, grip_r=0.9)
    for side, spec in (("l", legs_l), ("r", legs_r)):
        kind, v, knee = spec
        k["ankle_" + side + ("_rel" if kind == "rel" else "")] = list(v)
        k["knee_" + side] = list(knee)
        k["foot_" + side] = [1, 0, 0]
    return base_pose(pelvis=[0.0, 0.0, pz], **k)


def _para_props(p):
    sx = L_SH * math.sin(math.radians(p))
    return [_para(sx, 0.18), _para(sx, -0.18)]
TUCK = ("rel", [0.36, 0.0, 0.17], [1, 0, 1])
ADV_TUCK = ("rel", [0.613, 0.0, -0.046], [0.5, 0, 1])


def l_sit_floor():
    a = _sit_para(0, STRAIGHT_FULL, STRAIGHT_FULL)
    b = _sit_para(0, STRAIGHT_FULL, STRAIGHT_FULL, bump=-0.003)
    return dict(seconds=3.0, keys=rep(a, b), cam_az=30, cam_el=10, props=_para_props(0))


def l_sit_foot_supported():
    a = _sit(30, _heels(30, "l"), _heels(30, "r"))
    b = _sit(30, _heels(30, "l"), _heels(30, "r"), bump=-0.005)
    return dict(seconds=3.0, keys=rep(a, b), cam_az=30, cam_el=10, props=[])


def l_sit_one_foot():
    straight_r = ("rel", [0.85, 0.0, 0.0], [0, 0, 1])
    a = _sit(30, _heels(30, "l"), straight_r)
    b = _sit(30, _heels(30, "l"), straight_r, bump=-0.005)
    return dict(seconds=3.0, keys=rep(a, b), cam_az=30, cam_el=10, props=[])


def l_sit_tuck():
    a = _sit_para(10, TUCK, TUCK)
    b = _sit_para(10, TUCK, TUCK, bump=-0.003)
    return dict(seconds=3.0, keys=rep(a, b), cam_az=30, cam_el=10, props=_para_props(10))


def l_sit_advanced_tuck():
    a = _sit(35, ADV_TUCK, ADV_TUCK)
    b = _sit(35, ADV_TUCK, ADV_TUCK, bump=-0.005)
    return dict(seconds=3.0, keys=rep(a, b), cam_az=30, cam_el=10, props=[])


def l_sit_one_leg():
    # Right leg straight in front, left leg advanced-tucked.
    a = _sit(35, ADV_TUCK, STRAIGHT)
    b = _sit(35, ADV_TUCK, STRAIGHT, bump=-0.005)
    return dict(seconds=3.0, keys=rep(a, b), cam_az=30, cam_el=10, props=[])


def v_sit_floor():
    # Leaning back slightly, straight legs lifted 40 deg above horizontal.
    a = _sit_para(-10, LIFT_FULL, LIFT_FULL)
    b = _sit_para(-10, LIFT_FULL, LIFT_FULL, bump=-0.003)
    return dict(seconds=3.0, keys=rep(a, b), cam_az=30, cam_el=10, props=_para_props(-10))


# --- carry and pallof (standing, band at chest height on the right side) -----------------------
def suitcase_carry():
    kb = [dict(kind="weight", type="kettlebell", joint="wrist_r", off=[0, 0, 0])]

    def key(bump):
        return base_pose(pelvis=[0.0, 0.0, 0.980 + bump], hand_r=[0, 0, -1], grip_r=0.9,
                         ankle_l=[0.0, 0.10, 0.08], ankle_r=[0.0, -0.10, 0.08])
    return dict(seconds=3.0, keys=rep(key(0.002), key(-0.002)), cam_az=30, cam_el=8, props=kb)


PALLOF_BAND = dict(kind="rod", a=[0.0, -1.4, 1.25], b=pt("wrist_r"), r=0.008)


def _pallof(hx, hy, bump=0.0):
    # Wide stance (feet 0.25 m either side), hands at x = hx, y = +-hy, height 1.25.
    return base_pose(pelvis=[0.0, 0.0, 0.955 + bump], hp=0,
                     wrist_l=[hx, hy, 1.25], wrist_r=[hx, -hy, 1.25], hand_l=[1, 0, 0], hand_r=[1, 0, 0],
                     palm_l=[0, -1, 0], palm_r=[0, 1, 0], grip_l=0.9, grip_r=0.9,
                     ankle_l=[0.0, 0.20, 0.08], ankle_r=[0.0, -0.20, 0.08])


def pallof_hold():
    # Arms pressed straight out and held; the band tries to turn the body.
    a = _pallof(0.55, 0.12, 0.003)
    b = _pallof(0.55, 0.11, -0.003)
    return dict(seconds=3.0, keys=rep(a, b), cam_az=30, cam_el=10, props=[PALLOF_BAND])


def pallof_press():
    # Hands at the chest, press out straight, pause, return.
    a = _pallof(0.12, 0.06)
    b = _pallof(0.55, 0.12)
    return dict(seconds=3.5, keys=rep(a, b, down_end=0.45), cam_az=30, cam_el=10, props=[PALLOF_BAND])


EXERCISES = {
    "plank": plank,
    "side-plank": side_plank,
    "dead-bug": dead_bug,
    "hollow-tuck": hollow_tuck,
    "hollow-one-leg": hollow_one_leg,
    "hollow-rocks": hollow_rocks,
    "hollow-hold": hollow_hold,
    "l-sit-foot-supported": l_sit_foot_supported,
    "l-sit-one-foot": l_sit_one_foot,
    "l-sit-tuck": l_sit_tuck,
    "l-sit-advanced-tuck": l_sit_advanced_tuck,
    "l-sit-one-leg": l_sit_one_leg,
    "l-sit-floor": l_sit_floor,
    "v-sit-floor": v_sit_floor,
    "leg-raise-lying": leg_raise_lying,
    "v-up": v_up,
    "side-plank-leg-raise": side_plank_leg_raise,
    "copenhagen-side-plank-short": copenhagen_side_plank_short,
    "copenhagen-side-plank": copenhagen_side_plank,
    "kettlebell-suitcase-carry": suitcase_carry,
    "pallof-hold": pallof_hold,
    "pallof-press": pallof_press,
    "plank-shoulder-tap": plank_shoulder_tap,
}
