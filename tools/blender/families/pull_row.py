"""Pull, hang, front-lever and row exercises (tools/blender/POSES.md, HAIKU-TASK.md).

Pull-up bar at z 2.30 (hands wrapped just below it); low row bar at z 1.00.
Pull-up hang: pelvis z 1.205. Lever body: horizontal, head toward +X, hands straight up to the bar.
"""
import math

from poses import base_pose, pt, rep

BAR_Z = 2.30
WRIST_BAR = BAR_Z - 0.065
LOW_Z = 1.00
WRIST_ROW = LOW_Z - 0.065

HANG = (1.205, -0.04, -3.0, -6.0)   # pelvis z, pelvis x, p1, p2
TOP = (1.74, -0.10, 2.0, -6.0)


def _bar(z=BAR_Z):
    return dict(kind="bar", z=z, half=0.65, r=0.017)


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


def _hang(pose, **extra):
    return _pose(pose[0], pose[1], pose[2], pose[3], _hands(), _hang_legs(), **extra)


# --- pull-ups, hangs ----------------------------------------------------------

def pullup_band_assisted():
    # Right knee sits in a band looped over the bar; the band takes part of the weight.
    legs = _hang_legs()
    legs.update(ankle_r_rel=[0.05, 0.01, -0.42], knee_r=[1, 0, 0.4], foot_r=[0.8, 0, -0.5])
    band = dict(kind="rod", a=[0.0, 0.0, BAR_Z], b=pt("knee_r"), r=0.008)
    hang = _pose(*HANG, _hands(), legs)
    top = _pose(*TOP, _hands(), legs)
    return dict(seconds=3.0, keys=rep(hang, top), cam_az=35, cam_el=8, props=[_bar(), band])


def pullup_chest_to_bar():
    # Pull until the chest touches the bar, leaning back slightly at the top.
    legs = _hang_legs()
    hang = _pose(*HANG, _hands(), legs)
    top = _pose(1.74, 0.06, -10.0, -14.0, _hands(), legs)
    return dict(seconds=3.0, keys=rep(hang, top), cam_az=35, cam_el=8, props=[_bar()])


def muscle_up_bar():
    # Pull to the bar, transition with the elbows over it, press to straight arms on top.
    legs = _hang_legs()
    hang = _pose(*HANG, _hands(), legs)
    pull = _pose(1.74, -0.04, -10.0, -14.0, _hands(elbow=(-0.4, 1.0, -0.1)), legs)
    top_hands = _hands(x=0.0, wz=BAR_Z + 0.06, palm=(0, 0, -1), hand=(1, 0, 0), elbow=(0.0, 1.0, 0.0))
    top = _pose(2.45, 0.0, 2.0, 4.0, top_hands, legs)
    keys = [(0.0, hang), (0.1, hang), (0.35, pull), (0.5, top), (0.7, top), (0.9, pull)]
    return dict(seconds=4.0, keys=keys, cam_az=35, cam_el=8, props=[_bar()])


def dead_hang():
    # Straight arms, active shoulders; a breath moves the body by about a centimetre.
    a = _pose(*HANG, _hands(), _hang_legs())
    b = _pose(1.205, -0.03, -5.0, -8.0, _hands(), _hang_legs())
    return dict(seconds=3.0, keys=rep(a, b), cam_az=35, cam_el=8, props=[_bar()])


def german_hang():
    # A shallow, foot-supported shoulder-extension hold; feet remain planted to unload the shoulders.
    bar_z = 1.72
    bar_x = -0.62
    bar = dict(kind="bar", x=bar_x, z=bar_z, half=0.65, r=0.017)

    def key(pz, pitch, px=-0.08):
        return base_pose(pelvis=[px, 0.0, pz], p1=pitch, p2=pitch - 8, hp=0,
                         wrist_l=[bar_x, 0.18, bar_z - 0.06], wrist_r=[bar_x, -0.18, bar_z - 0.06],
                         palm_l=[1, 0, 0], palm_r=[1, 0, 0], hand_l=[0, 0, 1], hand_r=[0, 0, 1],
                         grip_l=0.95, grip_r=0.95, elbow_l=[0, 1, -0.2], elbow_r=[0, -1, -0.2],
                         ankle_l=[0.18, 0.10, 0.08], ankle_r=[0.18, -0.10, 0.08],
                         knee_l=[1, 0, 0.5], knee_r=[1, 0, 0.5], foot_l=[1, 0, 0], foot_r=[1, 0, 0],
                         yaw=0, roll=0)

    supported = key(0.78, -22)
    active = key(0.80, -22, -0.05)
    return dict(seconds=3.0, keys=rep(supported, active), cam_az=35, cam_el=12, props=[bar])


def scapular_pull():
    # Straight arms; shoulder blades pull down so the body rises a few centimetres.
    a = _pose(*HANG, _hands(), _hang_legs())
    b = _pose(1.215, -0.04, -3.0, -6.0, _hands(), _hang_legs())
    return dict(seconds=4.0, keys=rep(a, b), cam_az=35, cam_el=8, props=[_bar()])


def arch_hang():
    # Hang with the chest pushed forward and the back arched; the shoulders stay active.
    a = _pose(*HANG, _hands(), _hang_legs())
    b = _pose(1.21, -0.02, -14.0, 10.0, _hands(), _hang_legs())
    return dict(seconds=3.0, keys=rep(a, b), cam_az=35, cam_el=8, props=[_bar()])


def pullup_negative():
    # Start with the chin over the bar; lower slowly to straight arms.
    top = _pose(*TOP, _hands(), _hang_legs())
    bottom = _pose(*HANG, _hands(), _hang_legs())
    return dict(seconds=4.0, keys=rep(top, bottom), cam_az=35, cam_el=8, props=[_bar()])


def flexed_arm_hang():
    # Chin over the bar, elbows bent and close to the body; a small breath.
    elbow = (0.0, 1.0, -0.6)
    a = _pose(1.69, -0.04, 0.0, -4.0, _hands(elbow=elbow), _hang_legs())
    b = _pose(1.71, -0.04, 0.0, -4.0, _hands(elbow=elbow), _hang_legs())
    return dict(seconds=3.0, keys=rep(a, b), cam_az=35, cam_el=8, props=[_bar()])


def chair_assisted_pullup():
    # Both feet on a box step (top 0.85 m, in front); the arms do the work, the legs take part of the weight.
    step = dict(kind="box", center=[0.62, 0.0, 0.425], size=[0.40, 0.60, 0.85])
    feet = dict(ankle_l=[0.49, 0.10, 0.93], ankle_r=[0.49, -0.10, 0.93],
                knee_l=[0.3, 0, 1], knee_r=[0.3, 0, 1], foot_l=[1, 0, 0], foot_r=[1, 0, 0])
    # bottom: arms straight (pelvis 1.205), knees about 80 degrees; top: chin over the bar, legs nearly straight
    bottom = _pose(1.205, 0.0, -3.0, -6.0, _hands(), feet)
    top = _pose(1.74, 0.18, 2.0, -6.0, _hands(), feet)
    return dict(seconds=3.0, keys=rep(bottom, top), cam_az=35, cam_el=8, props=[_bar(), step])


def chinup_full():
    # Palms face the body (underhand grip); chin over the bar.
    hands = _hands(x=0.045, palm=(-1, 0, 0), elbow=(0.2, 1.0, -0.2))
    hang = _pose(HANG[0], HANG[1] + 0.09, HANG[2], HANG[3], hands, _hang_legs())
    top = _pose(TOP[0], TOP[1] + 0.09, TOP[2], TOP[3], hands, _hang_legs())
    return dict(seconds=3.0, keys=rep(hang, top), cam_az=35, cam_el=8, props=[_bar()])


# --- front levers -------------------------------------------------------------
# Body horizontal, face up (pitch -90, head toward -X, legs toward +X), arms straight up to the bar.

LEVER_PX = 0.425    # pelvis x so the shoulders sit under the hands (chest toward the ceiling, head toward -X)
LEVER_PZ = 1.67     # pelvis z so the arms are straight (shoulder to wrist about 0.57)


def _lever(legs, hp=0.0):
    return base_pose(pelvis=[LEVER_PX, 0.0, LEVER_PZ], p1=-90.0, p2=-90.0, hp=hp,
                     **_hands(), **legs)


def _tuck_legs(x=0.10, z=0.42):
    # Knees pulled toward the chest (chest side is up): thighs fold up and toward the head, shins fold back.
    return dict(ankle_l_rel=[x, 0.02, z], ankle_r_rel=[x, -0.02, z],
                knee_l=[-1, 0, 1], knee_r=[-1, 0, 1], foot_l=[1, 0, 0.3], foot_r=[1, 0, 0.3])


def front_lever_tuck():
    a = _lever(_tuck_legs(z=0.42))
    b = _lever(_tuck_legs(z=0.40))
    return dict(seconds=3.0, keys=rep(a, b), cam_az=35, cam_el=20, props=[_bar()])


def front_lever_adv_tuck():
    # Hips more open than the tuck: thighs about 90 degrees from the body, shins horizontal.
    a = _lever(dict(ankle_l_rel=[0.43, 0.02, 0.44], ankle_r_rel=[0.43, -0.02, 0.44],
                    knee_l=[-1, 0, 1], knee_r=[-1, 0, 1], foot_l=[1, 0, 0], foot_r=[1, 0, 0]))
    b = _lever(dict(ankle_l_rel=[0.43, 0.02, 0.42], ankle_r_rel=[0.43, -0.02, 0.42],
                    knee_l=[-1, 0, 1], knee_r=[-1, 0, 1], foot_l=[1, 0, 0], foot_r=[1, 0, 0]))
    return dict(seconds=3.0, keys=rep(a, b), cam_az=35, cam_el=20, props=[_bar()])


def front_lever_straddle():
    # Legs straight and apart, body horizontal.
    a = _lever(dict(ankle_l_rel=[0.80, 0.33, 0.0], ankle_r_rel=[0.80, -0.33, 0.0],
                    knee_l=[-1, 0, 0], knee_r=[-1, 0, 0], foot_l=[1, 0.25, 0], foot_r=[1, -0.25, 0]))
    b = _lever(dict(ankle_l_rel=[0.80, 0.31, 0.0], ankle_r_rel=[0.80, -0.31, 0.0],
                    knee_l=[-1, 0, 0], knee_r=[-1, 0, 0], foot_l=[1, 0.25, 0], foot_r=[1, -0.25, 0]))
    return dict(seconds=3.0, keys=rep(a, b), cam_az=35, cam_el=20, props=[_bar()])


# --- rows (low bar at z 1.00, body under it, heels on the floor) --------------

def _ankles(ax, az, sy=0.0):
    return [ax, sy + 0.10, az], [ax, sy - 0.10, az]


def _ang(a):
    return math.radians(a), (math.cos(math.radians(a)), math.sin(math.radians(a)))


def _S_from_ankle(ankle, ang):
    """Shoulder (x, z) of a straight body: 1.365 m from the ankle along the body line at `ang` degrees."""
    _, (wx, wz) = _ang(ang)
    return ankle[0] - 1.365 * wx, ankle[1] - 1.365 * wz


def _S_from_knee(knee, ang):
    """Shoulder (x, z) when the body line ends at the knee (0.94 m from the shoulder)."""
    _, (wx, wz) = _ang(ang)
    return knee[0] - 0.94 * wx, knee[1] - 0.94 * wz


def _row_pose(S, ang, ankles, grip=0.18, wrists=None, sy=0.0, wz=WRIST_ROW):
    """Body from the shoulders S (x, z) along the line at `ang` (deg, negative = feet lower); pitch p = -90 - ang."""
    _, (wx, wz_) = _ang(ang)
    pelvis = [S[0] + 0.47 * wx, sy, S[1] + 0.47 * wz_]
    p = -90.0 - ang
    if wrists is None:
        wrists = [[-0.045, grip, wz], [-0.045, -grip, wz]]
    (al, ar) = ankles
    return base_pose(pelvis=pelvis, p1=p, p2=p, hp=0.0,
                     wrist_l=wrists[0], wrist_r=wrists[1],
                     palm_l=[1, 0, 0], palm_r=[1, 0, 0], hand_l=[0, 0, 1], hand_r=[0, 0, 1],
                     grip_l=0.95, grip_r=0.95,
                     elbow_l=[0.0, 1.0, -0.6], elbow_r=[0.0, -1.0, -0.6],
                     ankle_l=al, ankle_r=ar, knee_l=[0, 0, 1], knee_r=[0, 0, 1],
                     foot_l=[1, 0, 0], foot_r=[1, 0, 0])


def inverted_row():
    # Straight body, heels on the floor; top: chest pulled up toward the bar.
    A = (1.287, 0.08)
    bottom = _row_pose(_S_from_ankle(A, -12.1), -12.1, _ankles(*A))
    top = _row_pose(_S_from_ankle(A, -34.0), -34.0, _ankles(*A))
    return dict(seconds=3.0, keys=rep(top, bottom), cam_az=30, cam_el=15, props=[dict(kind="bar", z=LOW_Z, half=0.65, r=0.017)])


def inverted_row_wide():
    # Same as the inverted row, hands about one and a half shoulder widths apart.
    A = (1.287, 0.08)
    wide = [[-0.045, 0.27, WRIST_ROW], [-0.045, -0.27, WRIST_ROW]]
    bottom = _row_pose(_S_from_ankle(A, -12.5), -12.5, _ankles(*A), wrists=wide)
    top = _row_pose(_S_from_ankle(A, -34.0), -34.0, _ankles(*A), wrists=wide)
    return dict(seconds=3.0, keys=rep(top, bottom), cam_az=30, cam_el=15, props=[dict(kind="bar", z=LOW_Z, half=0.65, r=0.017)])


def inverted_row_bent_knees():
    # Body in one line from the shoulders to the knees; knees bent, heels on the floor under the knees.
    K = (0.886, 0.51)
    ankles = _ankles(K[0], 0.08)
    bottom = _row_pose(_S_from_knee(K, 8.3), 8.3, ankles)
    top = _row_pose(_S_from_knee(K, -21.0), -21.0, ankles)
    return dict(seconds=3.0, keys=rep(top, bottom), cam_az=30, cam_el=15, props=[dict(kind="bar", z=LOW_Z, half=0.65, r=0.017)])


def inverted_row_feet_elevated():
    # Heels on a box of 0.45 m; body nearly level.
    A = (1.311, 0.53)
    box = dict(kind="box", center=[1.311, 0.0, 0.225], size=[0.40, 0.80, 0.45])
    bottom = _row_pose(_S_from_ankle(A, 6.9), 6.9, _ankles(*A))
    top = _row_pose(_S_from_ankle(A, -13.5), -13.5, _ankles(*A))
    return dict(seconds=3.0, keys=rep(top, bottom), cam_az=30, cam_el=15,
                props=[dict(kind="bar", z=LOW_Z, half=0.65, r=0.017), box])


def archer_row():
    # Right (near) hand pulls; the body shifts toward it, the left arm assists on a wide grip.
    A = (1.287, 0.08)
    wrists = [[-0.045, 0.45, WRIST_ROW], [-0.045, -0.45, WRIST_ROW]]
    bottom = _row_pose(_S_from_ankle(A, -15.3), -15.3, _ankles(*A), wrists=wrists)
    top = _row_pose(_S_from_ankle(A, -31.9), -31.9, _ankles(*A), wrists=wrists, sy=-0.06)
    return dict(seconds=4.0, keys=rep(top, bottom), cam_az=30, cam_el=15,
                props=[dict(kind="bar", z=LOW_Z, half=0.65, r=0.017)])


def towel_door_row():
    # Feet near the door, body leaning back with straight arms; the towel is held at the handle.
    door_x, hand_x = 0.50, 0.40
    towel = [dict(kind="rod", a=[door_x - 0.03, 0.0, 1.0], b=pt("wrist_l"), r=0.02),
             dict(kind="rod", a=[door_x - 0.03, 0.0, 1.0], b=pt("wrist_r"), r=0.02)]
    door = dict(kind="box", center=[door_x + 0.02, 0.0, 1.1], size=[0.04, 0.8, 2.2])
    wrists = [[hand_x, 0.20, 1.0], [hand_x, -0.20, 1.0]]

    def key(phi):
        a = math.radians(phi)
        h = (-math.cos(a), math.sin(a))           # feet -> head direction in XZ
        A = (0.353, 0.08)                         # feet fixed in both keys
        pelvis = [A[0] + 0.895 * h[0], 0.0, A[1] + 0.895 * h[1]]
        p = phi - 90.0
        ankles = _ankles(*A)
        return base_pose(pelvis=pelvis, p1=p, p2=p, hp=0.0,
                         wrist_l=wrists[0], wrist_r=wrists[1],
                         palm_l=[0, 0, -1], palm_r=[0, 0, -1], hand_l=[1, 0, 0], hand_r=[1, 0, 0],
                         grip_l=0.95, grip_r=0.95,
                         elbow_l=[0.0, 1.0, -0.4], elbow_r=[0.0, -1.0, -0.4],
                         ankle_l=ankles[0], ankle_r=ankles[1], knee_l=[1, 0, 0], knee_r=[1, 0, 0],
                         foot_l=[0.3, 0.95, 0], foot_r=[0.3, -0.95, 0])

    return dict(seconds=4.0, keys=rep(key(75), key(88)), cam_az=-30, cam_el=10, props=[door] + towel)


def kettlebell_one_arm_row():
    # Left hand and left knee on a bench (shin lying along it); right (near) arm pulls the kettlebell to the hip.
    bench = dict(kind="box", center=[-0.20, 0.12, 0.225], size=[0.75, 0.25, 0.45])
    kb = dict(kind="weight", type="kettlebell", joint="wrist_r", off=[0, 0, -0.05])

    def key(right_wrist):
        return base_pose(pelvis=[-0.05, 0.0, 0.92], p1=90.0, p2=90.0, hp=0.0,
                         ankle_l=[-0.46, 0.10, 0.50], ankle_r=[0.12, -0.12, 0.08],
                         knee_l=[1, 0, -1], knee_r=[1, 0, 0], foot_l=[-1, 0, 0], foot_r=[1, 0, 0],
                         wrist_l=[0.10, 0.12, 0.485], wrist_r=right_wrist,
                         palm_l=[0, 0, -1], palm_r=[1, 0, 0], hand_l=[1, 0, 0], hand_r=[0, 0, -1],
                         grip_l=0.1, grip_r=0.9, elbow_l=[0.0, 1.0, 0.0], elbow_r=[-0.3, -1.0, -0.5])

    top = key([0.12, -0.18, 0.82])
    bottom = key([0.42, -0.18, 0.355])
    return dict(seconds=3.0, keys=rep(top, bottom), cam_az=25, cam_el=10, props=[bench, kb])


def row_band():
    # Seated, legs straight; band anchored in front at hand height, pulled to the lower ribs.
    anchor = [1.10, 0.0, 0.58]
    legs = dict(ankle_l=[0.845, 0.10, 0.08], ankle_r=[0.845, -0.10, 0.08],
                knee_l=[0, 0, 1], knee_r=[0, 0, 1], foot_l=[1, 0, 0], foot_r=[1, 0, 0])

    def key(wl, wr, elbow):
        return base_pose(pelvis=[-0.02, 0.0, 0.11], p1=0.0, p2=0.0, hp=0.0,
                         wrist_l=wl, wrist_r=wr, palm_l=[1, 0, 0], palm_r=[1, 0, 0],
                         hand_l=[1, 0, 0], hand_r=[1, 0, 0], grip_l=0.9, grip_r=0.9,
                         elbow_l=elbow, elbow_r=[elbow[0], -elbow[1], elbow[2]], **legs)

    extended = key([0.55, 0.18, 0.58], [0.55, -0.18, 0.58], [0.0, 1.0, 0.0])
    pulled = key([0.10, 0.20, 0.36], [0.10, -0.20, 0.36], [-1.0, 1.0, 0.0])
    props = [dict(kind="rod", a=anchor, b=pt("wrist_l"), r=0.008),
             dict(kind="rod", a=anchor, b=pt("wrist_r"), r=0.008)]
    return dict(seconds=3.0, keys=rep(extended, pulled), cam_az=30, cam_el=10, props=props)


EXERCISES = {
    "pullup-band-assisted": pullup_band_assisted,
    "pullup-chest-to-bar": pullup_chest_to_bar,
    "muscle-up-bar": muscle_up_bar,
    "front-lever-tuck": front_lever_tuck,
    "front-lever-adv-tuck": front_lever_adv_tuck,
    "front-lever-straddle": front_lever_straddle,
    "dead-hang": dead_hang,
    "german-hang": german_hang,
    "scapular-pull": scapular_pull,
    "arch-hang": arch_hang,
    "pullup-negative": pullup_negative,
    "flexed-arm-hang": flexed_arm_hang,
    "chair-assisted-pullup": chair_assisted_pullup,
    "chinup-full": chinup_full,
    "row-band": row_band,
    "inverted-row-bent-knees": inverted_row_bent_knees,
    "inverted-row": inverted_row,
    "inverted-row-wide": inverted_row_wide,
    "inverted-row-feet-elevated": inverted_row_feet_elevated,
    "archer-row": archer_row,
    "towel-door-row": towel_door_row,
    "kettlebell-one-arm-row": kettlebell_one_arm_row,
}
