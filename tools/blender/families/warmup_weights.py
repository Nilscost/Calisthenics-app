"""Warm-up and weighted exercises (tools/blender/POSES.md, HAIKU-TASK.md).

Structure follows families/squat_lowerlegs.py (squat-air). Numbers are copied from the finished exercises:
squats from squat_lowerlegs.py, hinges and bridges from hips_back.py, push-ups and dips from push_shoulders.py,
hangs and rows from pull_row.py, dead bug from core.py.

Facing: lying face up p = -90 (head toward -X); face down p = +90 (head toward +X).
Vests have no prop kind: the load is shown as a kettlebell-type weight on the chest (the vest itself is not drawn).
"""
import math

from poses import base_pose, both, dense, pt, rep

FLOOR_WRIST_Z = 0.035
BAR_Z = 2.30
WRIST_BAR = BAR_Z - 0.065
HANG = (1.205, -0.04, -3.0, -6.0)  # pelvis z, pelvis x, p1, p2
TOP = (1.74, -0.10, 2.0, -6.0)
FLAT_PALM = dict(palm_l=[0, 0, -1], palm_r=[0, 0, -1], grip_l=0.0, grip_r=0.0)


def _feet_flat(ax=0.0):
    """Both feet flat, shoulder width (as squat-air)."""
    return both(ankle=[ax, 0.14, 0.08], foot=[1, 0.25, 0], knee=[1, 0.25, 0])


def _hinge_legs():
    """Both feet flat under the hips; knees straight at the top (pelvis 0.975)."""
    return dict(ankle_l=[0.0, 0.10, 0.08], ankle_r=[0.0, -0.10, 0.08],
                knee_l=[1, 0, 0], knee_r=[1, 0, 0], foot_l=[1, 0, 0], foot_r=[1, 0, 0])


def _feet(ax, ay=0.10, knee=(1.0, 0.0, 0.3)):
    """Both feet flat on the floor in front of the body (world targets)."""
    return dict(ankle_l=[ax, ay, 0.08], ankle_r=[ax, -ay, 0.08], knee_l=list(knee), knee_r=list(knee),
                foot_l=[1, 0, 0], foot_r=[1, 0, 0])


def _lie(pz=0.11, **over):
    """Lying face up, pelvis on the floor (lying pelvis z 0.11)."""
    k = dict(p1=-90, p2=-90, hp=0, hand_l=[-1, 0, 0], hand_r=[-1, 0, 0])
    k.update(over)
    return base_pose(pelvis=[0.0, 0.0, pz], **k)


def _hands(x=-0.045, grip=0.25, wz=WRIST_BAR, palm=(1, 0, 0), hand=(0, 0, 1), elbow=(0.35, 1.0, -0.2)):
    """Both hands on the bar as world targets (same fields in every key)."""
    return dict(wrist_l=[x, grip, wz], wrist_r=[x, -grip, wz],
                palm_l=list(palm), palm_r=list(palm), hand_l=list(hand), hand_r=list(hand),
                grip_l=0.95, grip_r=0.95,
                elbow_l=list(elbow), elbow_r=[elbow[0], -elbow[1], elbow[2]])


def _hang_legs(fwd=0.06):
    """Feet hang free, toes down, legs nearly straight (knee about 170): nothing touches the floor."""
    return dict(ankle_l_rel=[fwd, -0.01, -0.865], ankle_r_rel=[fwd, 0.01, -0.865],
                knee_l=[1, 0, 0], knee_r=[1, 0, 0], foot_l=[0.45, 0, -1], foot_r=[0.45, 0, -1])


def _pose(pz, px, p1, p2, *parts, **extra):
    fields = {}
    for part in parts:
        fields.update(part)
    fields.update(extra)
    return base_pose(pelvis=[px, 0.0, pz], p1=p1, p2=p2, **fields)


def _bar():
    return dict(kind="bar", z=BAR_Z, half=0.65, r=0.017)


def _dip(pz, px, p1, legs, wz=1.05, wx=0.0, lean_p=3):
    bars = [dict(kind="rod", a=[-0.20, 0.25, 1.0], b=[0.20, 0.25, 1.0], r=0.017),
            dict(kind="rod", a=[-0.20, -0.25, 1.0], b=[0.20, -0.25, 1.0], r=0.017)]
    return base_pose(pelvis=[px, 0.0, pz], p1=p1, p2=p1 + lean_p, hp=0,
                     **both(wrist=[wx, 0.25, wz], elbow=[-1, 0.2, 0.0], hand=[1, 0, 0], palm=[0, 0, -1],
                            grip=0.9, knee=[1, 0, 0], ankle_rel=legs, foot=[1, 0, 0])), bars


def _line(ankle_x, a, wrist_x, wy=0.27, wz=0.035, elbow=(-0.6, 0.7, 1.0), knee=(0, 0, -1), foot=(0.95, 0, -0.3)):
    """Straight push-up body at angle a above the floor (deg), toes at ankle_x, hands at (wrist_x, +-wy, wz)."""
    ar = math.radians(a)
    d = [math.cos(ar), 0.0, math.sin(ar)]
    pelvis = [ankle_x + 0.898 * d[0], 0.0, 0.08 + 0.898 * d[2]]
    pitch = 90 - a
    return base_pose(pelvis=pelvis, p1=pitch, p2=pitch, hp=-8,
                     **both(ankle=[ankle_x, 0.08, 0.08], foot=list(foot), knee=list(knee),
                            wrist=[wrist_x, wy, wz], elbow=list(elbow), hand=[1, 0, 0],
                            palm=[0, 0, -1], grip=0.0))


def _vest(off):
    """Vest approximation: a kettlebell-type weight attached to the chest (no vest prop kind)."""
    return dict(kind="weight", type="kettlebell", joint="chest", off=list(off))


# --- warm-up: shoulders ---------------------------------------------------------

def _shoulder_loop(half, top_z, behind_x, rod_r):
    """Band or towel held wide: top arms straight overhead; bottom arms straight behind the body.
    half = world half-width of the grip (shoulders sit at y = +-0.18, so the relative y is half - 0.18).
    Reach (shoulder to wrist) is kept near 0.57 so the arms stay straight."""
    def key(wrist_rel, hand):
        return base_pose(pelvis=[0.0, 0.0, 0.975], p1=0.0, p2=0.0, hp=0.0,
                         **both(wrist_rel=wrist_rel, hand=hand, palm=[0, -1, 0], grip=0.9, elbow=[-1, 0.3, 0]))
    top = key([0.0, half - 0.18, top_z], [0, 0, 1])
    behind = key([behind_x, half - 0.18, 0.0], [-1, 0, 0])
    rod = dict(kind="rod", a=pt("wrist_l"), b=pt("wrist_r"), r=rod_r)
    return dict(seconds=3.5, keys=rep(top, behind), cam_az=35, cam_el=10, props=[rod])


def shoulder_band():
    # R7.2-T09 (F10): band pass-through. Wide overhand grip, arms kept straight the whole way: from the hips in front of the thighs, up in
    # front, over the head, down behind the back, and the same way back. The wrists travel on an arc about the shoulders (dense keys),
    # so the arms never bend; the band is a visible thin rod between the hands. Three-quarter camera so the band across the body and the arc both read.
    half = 0.44                      # half the grip width: well outside the shoulders (0.18)
    dy = half - 0.18
    rxz = math.sqrt(0.567 ** 2 - dy ** 2)   # in-plane radius: shoulder -> wrist 0.567 of the 0.57 reach (straight arms)

    def key(phi):
        # phi = arm angle about the shoulder axis: 0 hanging in front of the thighs, 90 forward, 180 overhead, 320 behind the back (down and back)
        r = math.radians(phi)
        d = [math.sin(r), 0.0, -math.cos(r)]
        n = [-math.cos(r), 0.0, -math.sin(r)]        # overhand grip: palms toward the body at the hips, down at 90, forward overhead
        return base_pose(pelvis=[0.0, 0.0, 0.975], p1=0.0, p2=0.0, hp=0.0,
                         **both(wrist_rel=[rxz * d[0], dy, rxz * d[2]], hand=d, palm=n, grip=0.9, elbow=[-n[0], 0.3, -n[2]]))   # a tiny bend, outward of the arc
    phi = lambda phi: dict(phi=phi)
    keyed = [(0.0, phi(0.0)), (0.06, phi(0.0)), (0.46, phi(320.0)), (0.56, phi(320.0))]
    rod = dict(kind="rod", a=pt("wrist_l"), b=pt("wrist_r"), r=0.012)
    return dict(seconds=3.5, keys=dense(key, keyed, 84), cam_az=55, cam_el=10, props=[rod])


def shoulder_towel():
    # Towel stretched between the hands (thicker, wider grip); overhead, then behind the back.
    return _shoulder_loop(half=0.33, top_z=0.548, behind_x=-0.545, rod_r=0.02)


# --- warm-up: squats, hinges ----------------------------------------------------

def squat_sky_reach():
    # Top: standing, arms straight overhead with the hands together. Bottom: deep squat, hands together at the chest.
    top = base_pose(pelvis=[0.0, 0.0, 0.975], **_feet_flat(0.0),
                    **both(wrist_rel=[0.0, -0.16, 0.546], elbow=[-1, 0.2, 0], hand=[0, 0, 1], palm=[0, -1, 0],
                           grip=0.0))
    bottom = base_pose(pelvis=[-0.24, 0.0, 0.40], p1=35, p2=40, hp=-15, **_feet_flat(0.0),
                       **both(wrist_rel=[0.40, -0.16, -0.05], elbow=[0, 1, -1], hand=[1, 0, 0], palm=[0, -1, 0],
                              grip=0.0))
    return dict(seconds=3.5, keys=rep(top, bottom), cam_az=35, cam_el=8, props=[])


def wrist_prep():
    # Hands and knees, fingers forward. Keys: hips back (shoulders over the hands), hips forward. Small range.
    hands = dict(wrist_l=[0.45, 0.20, FLOOR_WRIST_Z], wrist_r=[0.45, -0.20, FLOOR_WRIST_Z],
                 hand_l=[1, 0, 0], hand_r=[1, 0, 0], elbow_l=[-0.5, 1, 0], elbow_r=[-0.5, -1, 0], **FLAT_PALM)
    legs = dict(ankle_l=[-0.48, 0.10, 0.07], ankle_r=[-0.48, -0.10, 0.07], knee_l=[0, 0, -1], knee_r=[0, 0, -1],
                foot_l=[-1, 0, 0], foot_r=[-1, 0, 0])

    def key(px, pz):
        return base_pose(pelvis=[px, 0.0, pz], p1=90, p2=90, hp=-8, **hands, **legs)
    back = key(-0.02, 0.53)
    fwd = key(0.08, 0.51)
    return dict(seconds=4.0, keys=rep(back, fwd), cam_az=35, cam_el=20, props=[])


def hinge_easier():
    # Bodyweight hinge, smaller range than rdl_bodyweight: hands slide to mid-thigh, trunk about 40 at the bottom.
    top = base_pose(pelvis=[0.0, 0.0, 0.975], p1=0, p2=5, hp=0, **_hinge_legs(),
                    **both(wrist_rel=[0.03, -0.08, -0.56], palm=[0, -1, 0], hand=[0, 0, -1], grip=0.0,
                           elbow=[0, 1, -0.2]))
    bottom = base_pose(pelvis=[-0.10, 0.0, 0.93], p1=40, p2=45, hp=0, **_hinge_legs(),
                       **both(wrist_rel=[-0.08, -0.08, -0.55], palm=[0, -1, 0], hand=[0, 0, -1], grip=0.0,
                              elbow=[0, 1, -0.2]))
    return dict(seconds=3.5, keys=rep(top, bottom), cam_az=35, cam_el=12, props=[])


def squat_chair():
    # Easier squat: sit back onto a chair seat (45 cm), heels down; top standing with knees straight.
    chair = dict(kind="chair", x=-0.25, seat=0.45)
    top = base_pose(pelvis=[0.07, 0.0, 0.975], **_feet_flat(0.15),
                    **both(wrist_rel=[0.05, 0, -0.565], elbow=[-0.5, 0.5, 0], hand=[0, 0, -1]))
    bottom = base_pose(pelvis=[-0.25, 0.0, 0.56], p1=25, p2=30, hp=-5, **_feet_flat(0.15),
                       **both(wrist_rel=[0.50, 0, -0.05], elbow=[0, 1, -1], hand=[1, 0, 0]))
    return dict(seconds=3.0, keys=rep(top, bottom), cam_az=35, cam_el=10, props=[chair])


# --- warm-up: core, pulls, support ----------------------------------------------

def dead_bug_easy():
    # Lying, arms up, knees over the hips. Bottom: the right arm lowers only partway and the left leg reaches
    # only partway out (short range); the lower back stays on the floor.
    a = _lie(wrist_l_rel=[0.0, 0.03, 0.56], wrist_r_rel=[0.0, -0.03, 0.56], elbow_r=[-1, -0.2, 1.0],
             ankle_l_rel=[0.43, 0.0, 0.44], ankle_r_rel=[0.43, 0.0, 0.44], knee_l=[0.3, 0, 1], knee_r=[0.3, 0, 1])
    b = _lie(wrist_l_rel=[0.0, 0.03, 0.56], wrist_r_rel=[-0.36, -0.12, 0.40], elbow_r=[-1, -0.2, 1.0],
             ankle_l_rel=[0.62, 0.0, 0.22], ankle_r_rel=[0.43, 0.0, 0.44], knee_l=[0, 0, 1], knee_r=[0.3, 0, 1])
    return dict(seconds=3.0, keys=rep(a, b), cam_az=30, cam_el=10, props=[])


def arch_hang_feet_up():
    # Hang with the feet resting on a low box (weight taken off the arms); the chest comes forward and the back arches.
    step = dict(kind="box", center=[0.15, 0.0, 0.225], size=[0.40, 0.60, 0.45])
    feet = dict(ankle_l=[0.25, 0.10, 0.53], ankle_r=[0.25, -0.10, 0.53], knee_l=[1, 0, 0.3], knee_r=[1, 0, 0.3],
                foot_l=[1, 0, 0], foot_r=[1, 0, 0])
    hands = _hands()
    a = _pose(*HANG, hands, feet)
    b = _pose(1.21, -0.02, -14.0, 10.0, hands, feet)
    return dict(seconds=3.0, keys=rep(a, b), cam_az=35, cam_el=8, props=[_bar(), step])


def support_hold_legs_back():
    # Parallel bars, straight arms. Two keys: shoulders shrugged up, then pushed down (a small breath).
    # Legs bent with the feet kicked back (easier to stand than legs straight down).
    legs = [-0.20, 0.07, -0.78]
    a, bars = _dip(1.14, 0.0, 2, legs)
    b, _ = _dip(1.15, 0.0, 2, legs)
    return dict(seconds=3.0, keys=rep(a, b), cam_az=30, cam_el=10, props=bars)


# --- dumbbells, goblet, barbell -----------------------------------------------

def goblet_squat():
    # One dumbbell held upright at the chest with both hands; sit between the knees, chest tall.
    weight = dict(kind="weight", type="dumbbell", joint="wrist_r", off=[0.0, 0.0, 0.0])
    hands = dict(elbow=[0, 1, -1], palm=[0, -1, 0], hand=[1, 0, 0], grip=0.9)
    top = base_pose(pelvis=[0.0, 0.0, 0.975], **_feet_flat(0.0),
                    **both(wrist_rel=[0.22, -0.16, -0.22], **hands))
    bottom = base_pose(pelvis=[-0.21, 0.0, 0.50], p1=20, p2=25, hp=-5, **_feet_flat(0.0),
                       **both(wrist_rel=[0.30, -0.16, -0.08], **hands))
    return dict(seconds=3.0, keys=rep(top, bottom), cam_az=35, cam_el=10, props=[weight])


def dumbbell_rdl():
    # A dumbbell in each hand in front of the thighs; hips back, flat back, weights slide down the legs.
    weights = [dict(kind="weight", type="dumbbell", joint="wrist_l", off=[0, 0, 0]),
               dict(kind="weight", type="dumbbell", joint="wrist_r", off=[0, 0, 0])]
    arms = dict(palm=[0, -1, 0], hand=[0, 0, -1], grip=0.9, elbow=[0, 1, -0.2])
    top = base_pose(pelvis=[0.0, 0.0, 0.975], p1=0, p2=5, hp=0, **_hinge_legs(),
                    **both(wrist_rel=[0.03, -0.08, -0.56], **arms))
    bottom = base_pose(pelvis=[-0.20, 0.0, 0.92], p1=60, p2=65, hp=0, **_hinge_legs(),
                       **both(wrist_rel=[-0.13, -0.08, -0.55], **arms))
    return dict(seconds=3.5, keys=rep(top, bottom), cam_az=35, cam_el=12, props=weights)


def _single_leg_hinge(pz_top, px_bot, pz_bot, p_bot, arms_top, arms_bot, props):
    """Right leg stands; the left leg hangs behind and lifts in line with the body at the bottom."""
    def key(pz, px, p, free_leg, arms):
        fields = dict(pelvis=[px, 0, pz], p1=p, p2=p + 4, hp=0,
                      ankle_r=[0.0, -0.10, 0.08], knee_r=[1, 0, 0], foot_r=[1, 0, 0],
                      ankle_l_rel=free_leg, knee_l=[1, 0, 0], foot_l=[-1, 0, 0])
        fields.update(arms)
        return base_pose(**fields)
    top = key(pz_top, 0.0, 0, [-0.04, 0.01, -0.86], arms_top)
    bottom = key(pz_bot, px_bot, p_bot, [-0.80, 0.01, -0.35], arms_bot)
    return dict(seconds=3.5, keys=rep(top, bottom), cam_az=35, cam_el=10, props=props)


def dumbbell_single_leg_rdl():
    # Right leg stands; a dumbbell in EACH hand (not one); the left leg reaches straight behind at the bottom.
    weights = [dict(kind="weight", type="dumbbell", joint="wrist_l", off=[0, 0, 0]),
               dict(kind="weight", type="dumbbell", joint="wrist_r", off=[0, 0, 0])]
    arms = dict(palm=[0, -1, 0], hand=[0, 0, -1], grip=0.9, elbow=[0, 1, -0.2])
    arms_top = both(wrist_rel=[0.03, -0.08, -0.56], **arms)
    arms_bot = both(wrist_rel=[-0.12, -0.08, -0.55], **arms)
    return _single_leg_hinge(0.975, -0.12, 0.92, 60, arms_top, arms_bot, weights)


def weighted_glute_bridge():
    # Lying, feet flat, a dumbbell resting on the hips held with both hands; hips lift and lower.
    weight = dict(kind="weight", type="dumbbell", joint="pelvis", off=[0.0, 0.0, 0.20])

    def key(px, pz, p, wz):
        return base_pose(pelvis=[px, 0.0, pz], p1=p, p2=p, hp=0,
                         wrist_l=[px, 0.08, wz], wrist_r=[px, -0.08, wz],
                         palm_l=[0, 0, -1], palm_r=[0, 0, -1], hand_l=[1, 0, 0], hand_r=[1, 0, 0],
                         grip_l=0.9, grip_r=0.9, elbow_l=[0, 1, 0.5], elbow_r=[0, -1, 0.5],
                         **_feet(0.60))
    lying = key(0.10, 0.11, -90, 0.23)
    top = key(0.05, 0.33, -105, 0.45)
    return dict(seconds=3.0, keys=rep(lying, top), cam_az=35, cam_el=15, props=[weight])


def dumbbell_row():
    # Left hand and left knee on a bench, back flat; the right (near) arm pulls a dumbbell up to the hip.
    bench = dict(kind="box", center=[-0.20, 0.12, 0.225], size=[0.75, 0.25, 0.45])
    db = dict(kind="weight", type="dumbbell", joint="wrist_r", off=[0, 0, 0])

    def key(right_wrist):
        return base_pose(pelvis=[-0.05, 0.0, 0.92], p1=90.0, p2=90.0, hp=0.0,
                         ankle_l=[-0.46, 0.10, 0.50], ankle_r=[0.12, -0.12, 0.08],
                         knee_l=[1, 0, -1], knee_r=[1, 0, 0], foot_l=[-1, 0, 0], foot_r=[1, 0, 0],
                         wrist_l=[0.10, 0.12, 0.485], wrist_r=right_wrist,
                         palm_l=[0, 0, -1], palm_r=[1, 0, 0], hand_l=[1, 0, 0], hand_r=[0, 0, -1],
                         grip_l=0.1, grip_r=0.9, elbow_l=[0.0, 1.0, 0.0], elbow_r=[-0.3, -1.0, -0.5])
    top = key([0.12, -0.18, 0.82])
    bottom = key([0.42, -0.18, 0.355])
    return dict(seconds=3.0, keys=rep(top, bottom), cam_az=25, cam_el=10, props=[bench, db])


def barbell_squat():
    # Bar across the upper back (hands on the bar just outside the shoulders); sit to thighs about parallel.
    bar = dict(kind="rod", a=pt("wrist_l"), b=pt("wrist_r"), r=0.02)
    grip = dict(hand=[0, 0, 1], palm=[-1, 0, 0], grip=0.9, elbow=[0, 1, -1])

    def key(pz, px, p, wrist_rel):
        return base_pose(pelvis=[px, 0.0, pz], p1=p, p2=p + 3, hp=-5, **_feet_flat(0.0),
                         **both(wrist_rel=wrist_rel, **grip))
    top = key(0.975, 0.0, 0, [-0.05, 0.15, 0.0])
    bottom = key(0.50, -0.27, 35, [-0.08, 0.15, -0.02])
    return dict(seconds=3.0, keys=rep(top, bottom), cam_az=35, cam_el=10, props=[bar])


def barbell_rdl():
    # Bar in the hands in front of the thighs; hips back, bar lowered along the legs to below the knees.
    bar = dict(kind="rod", a=pt("wrist_l"), b=pt("wrist_r"), r=0.02)
    arms = dict(palm=[-1, 0, 0], hand=[0, 0, -1], grip=0.9, elbow=[0, 1, -0.2])
    top = base_pose(pelvis=[0.0, 0.0, 0.975], p1=0, p2=5, hp=0, **_hinge_legs(),
                    **both(wrist_rel=[0.08, -0.06, -0.56], **arms))
    bottom = base_pose(pelvis=[-0.30, 0.0, 0.86], p1=80, p2=82, hp=0, **_hinge_legs(),
                       **both(wrist_rel=[-0.10, -0.06, -0.55], **arms))
    return dict(seconds=3.5, keys=rep(top, bottom), cam_az=35, cam_el=10, props=[bar])


# --- weighted bodyweight --------------------------------------------------------

def weighted_pushup():
    # Plank position, body in one line, chest to just above the floor; load on the back (vest approximation).
    top = _line(-1.1, 17, -0.14)
    bottom = _line(-1.1, 7, -0.12)
    return dict(seconds=2.5, keys=rep(top, bottom), cam_az=30, cam_el=10, props=[_vest([0.0, 0.0, 0.12])])


def weighted_pullup():
    # Hang with straight arms; pull until the chin is over the bar; load on the chest (vest approximation).
    legs = _hang_legs()
    hang = _pose(*HANG, _hands(), legs)
    top = _pose(*TOP, _hands(), legs)
    return dict(seconds=3.0, keys=rep(hang, top), cam_az=35, cam_el=8,
                props=[_bar(), _vest([-0.16, 0.0, 0.0])])


def weighted_dip():
    # Parallel bars, straight arms at the top; lower until the upper arms are about parallel to the floor.
    a, bars = _dip(1.15, 0.0, 2, [0.03, 0.07, -0.84])
    b, _ = _dip(0.85, 0.0, 12, [0.30, 0.07, -0.60])
    return dict(seconds=3.0, keys=rep(a, b), cam_az=30, cam_el=10, props=bars + [_vest([-0.16, 0.0, 0.0])])


def weighted_squat():
    # Squat as squat-air with a load on the chest (vest approximation); thighs about parallel at the bottom.
    top = base_pose(pelvis=[0.0, 0.0, 0.975], **_feet_flat(0.0),
                    **both(wrist_rel=[0.05, 0.0, -0.565], hand=[0, 0, -1]))
    bottom = base_pose(pelvis=[-0.25, 0.0, 0.50], p1=35, p2=40, hp=-15, **_feet_flat(0.0),
                       **both(wrist_rel=[0.56, 0.0, -0.05], elbow=[0, 0.3, -1], hand=[1, 0, 0]))
    return dict(seconds=3.0, keys=rep(top, bottom), cam_az=35, cam_el=8, props=[_vest([-0.16, 0.0, 0.0])])


EXERCISES = {
    "warmup-shoulder-band": shoulder_band,
    "warmup-shoulder-towel": shoulder_towel,
    "warmup-squat-sky-reach": squat_sky_reach,
    "warmup-wrist-prep": wrist_prep,
    "warmup-dead-bug": dead_bug_easy,
    "warmup-arch-hang": arch_hang_feet_up,
    "warmup-support-hold": support_hold_legs_back,
    "warmup-squat-easier": squat_chair,
    "warmup-hinge-easier": hinge_easier,
    "goblet-squat": goblet_squat,
    "dumbbell-rdl": dumbbell_rdl,
    "dumbbell-single-leg-rdl": dumbbell_single_leg_rdl,
    "weighted-glute-bridge": weighted_glute_bridge,
    "dumbbell-row": dumbbell_row,
    "barbell-squat": barbell_squat,
    "barbell-rdl": barbell_rdl,
    "weighted-pushup": weighted_pushup,
    "weighted-pullup": weighted_pullup,
    "weighted-dip": weighted_dip,
    "weighted-squat": weighted_squat,
}
