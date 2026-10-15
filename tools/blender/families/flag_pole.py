"""Human flag progressions: clutch (torso clamp) and press (straight-arm push/pull)."""
import math

from poses import base_pose, solve


POLE = dict(kind="rod", a=[0, 0, 0], b=[0, 0, 2.5], r=0.025)


def _press_pose(p, legs="straight", tuck_sign=1):
    """Pole grip and shoulder location keep both arms within reach as body rotates."""
    r = math.radians(p)
    u = [math.sin(r), 0.0, math.cos(r)]
    v = [math.cos(r), 0.0, -math.sin(r)]
    def bent_knee_pole(ankle_rel):
        d = math.sqrt(sum(x*x for x in ankle_rel))
        line = [x/d for x in ankle_rel]
        thigh = [-0.44*x for x in u]
        along = sum(thigh[i]*line[i] for i in range(3))
        return [thigh[i] - along*line[i] for i in range(3)]
    rx, rz = 0.36 * math.cos(r), 1.0 - 0.36 * math.sin(r)
    rr = math.hypot(rx, rz)
    h = math.sqrt(0.568 ** 2 - rr ** 2 / 4)
    cx = -rz / rr * h
    cz = 1.8 + rx / rr * h
    pelvis = [cx - 0.47 * u[0], 0.0, cz - 0.47 * u[2]]
    # Top (right) hand pulls; lower (left) hand pushes. Tiny X offset places palms at pole surface.
    common = dict(pelvis=pelvis, p1=p, p2=p, roll=-90,
                  wrist_r=[0.0, 0.0, 2.3], wrist_l=[0.0, 0.0, 1.3],
                  hand_r=[-1, 0, 0], hand_l=[-1, 0, 0],
                  palm_r=[-1, 0, 0], palm_l=[-1, 0, 0], grip_r=0.92, grip_l=0.92,
                  elbow_r=[0, -1, 0], elbow_l=[0, 1, 0],
                  ankle_r_rel=[-0.8695*u[0], 0.0, -0.8695*u[2]],
                  ankle_l_rel=[-0.8695*u[0], 0.0, -0.8695*u[2]],
                  foot_r=[-u[0], 0, -u[2]], foot_l=[-u[0], 0, -u[2]],
                  knee_r=[math.cos(r), 0, -math.sin(r)],
                  knee_l=[math.cos(r), 0, -math.sin(r)])
    def bent_rel(angle):
        a = math.radians(angle)
        w = [math.cos(r), 0.0, -math.sin(r)]
        return [-0.44*u[0] + 0.43*(math.cos(a)*u[0] + math.sin(a)*w[0]),
                0.0,
                -0.44*u[2] + 0.43*(math.cos(a)*u[2] + math.sin(a)*w[2])]
    if legs in ("tuck", "bent"):
        # Knee interior angle controls lever length: tuck 50°, bent-leg flag 90°.
        ankle_rel = bent_rel(50 if legs == "tuck" else 90)
        common["ankle_r_rel"] = ankle_rel
        common["ankle_l_rel"] = ankle_rel
        common["knee_r"] = bent_knee_pole(ankle_rel)
        common["knee_l"] = bent_knee_pole(ankle_rel)
        common.pop("ankle_r", None); common.pop("ankle_l", None)
    elif legs == "split":
        # Near/right thigh folds to 90° while the far leg remains a straight lever.
        common["ankle_r_rel"] = bent_rel(90)
        ankle_rel = common["ankle_r_rel"]
        common["knee_r"] = bent_knee_pole(ankle_rel)
        common.pop("ankle_r", None)
    return base_pose(**common)


def _clutch_pose(p, legs="straight"):
    """Side-on torso clamp: upper right arm wraps high, lower left elbow braces at waist."""
    q = math.radians(p)
    u = [math.sin(q), 0.0, math.cos(q)]
    # Right armpit is the fixed anchor. The high arm wraps below it; the low arm braces at the waist.
    A = [-0.02, -0.10, 1.50]
    v = [math.cos(q), 0.0, -math.sin(q)]
    C = [A[i] + 0.18*v[i] for i in range(3)]
    pelvis = [C[i] - 0.47*u[i] for i in range(3)]
    def bent_knee_pole(ankle_rel):
        d = math.sqrt(sum(x*x for x in ankle_rel))
        line = [x/d for x in ankle_rel]
        thigh = [-0.44*x for x in u]
        along = sum(thigh[i]*line[i] for i in range(3))
        return [thigh[i] - along*line[i] for i in range(3)]
    waist_elbow = [pelvis[i] + 0.24*u[i] for i in range(3)]
    k = dict(pelvis=pelvis, p1=p, p2=p, roll=-90,
             wrist_r=[0.0, 0.0, 1.30], wrist_l=[0.0, 0.0, 1.05 + 0.15*math.sin(q)],
             hand_r=[0, 0, -1], hand_l=[1, 0, 0],
             palm_r=[1, 0, 0], palm_l=[-1, 0, 0], grip_r=0.95, grip_l=0.92,
             elbow_r=[0, -1, 0], elbow_l=waist_elbow,
             ankle_r_rel=[-0.8695*u[0], 0.0, -0.8695*u[2]],
             ankle_l_rel=[-0.8695*u[0], 0.0, -0.8695*u[2]],
             foot_r=[-u[0], 0, -u[2]], foot_l=[-u[0], 0, -u[2]],
             knee_r=[math.cos(q), 0, -math.sin(q)],
             knee_l=[math.cos(q), 0, -math.sin(q)])
    def bent_rel(angle):
        a = math.radians(angle)
        w = [math.cos(q), 0.0, -math.sin(q)]
        return [-0.44*u[0] + 0.43*(math.cos(a)*u[0] + math.sin(a)*w[0]),
                0.0,
                -0.44*u[2] + 0.43*(math.cos(a)*u[2] + math.sin(a)*w[2])]
    if legs in ("tuck", "bent"):
        rel = bent_rel(50 if legs == "tuck" else 90)
        k["ankle_r_rel"] = rel; k["ankle_l_rel"] = rel
        pole = bent_knee_pole(rel)
        k["knee_r"] = pole; k["knee_l"] = pole
        k.pop("ankle_r", None); k.pop("ankle_l", None)
    elif legs == "split":
        k["ankle_r_rel"] = bent_rel(90)
        k["knee_r"] = bent_knee_pole(k["ankle_r_rel"])
        k.pop("ankle_r", None)
    return base_pose(**k)


def _hold(a, b=None, props=None, seconds=3.0, cam=26):
    b = b or a
    # A subtle breath keeps holds alive without changing contact targets.
    if b is a:
        b = dict(a)
        b["hp"] = a.get("hp", 0.0) + 0.3
    return dict(seconds=seconds, keys=[(0.0, a), (0.5, b)], cam_az=cam, cam_el=12, props=props or [POLE])


def flag_clutch_hang():
    return _hold(_clutch_pose(5, "bent"))


def flag_clutch_diagonal_split():
    return _hold(_clutch_pose(45, "split"))


def flag_clutch_diagonal_tuck():
    return _hold(_clutch_pose(45, "tuck"))


def flag_clutch_diagonal():
    return _hold(_clutch_pose(45))


def flag_clutch_horizontal_tuck():
    return _hold(_clutch_pose(88, "tuck"))


def flag_clutch_horizontal_split():
    return _hold(_clutch_pose(88, "split"))


def flag_clutch_bent_leg():
    return _hold(_clutch_pose(88, "bent"))


def flag_clutch():
    return _hold(_clutch_pose(88))


def flag_support_press():
    # One hand remains on the overhead bar; the other presses a wall beside the body.
    bar = dict(kind="bar", z=2.3, half=0.65, r=0.017, x=0)
    wall = dict(kind="box", center=[0, 0.80, 1.2], size=[1.0, 0.05, 2.4])
    k = base_pose(pelvis=[-0.045, 0, 1.197], p1=0, p2=0, roll=0,
                  wrist_r=[-0.045, -0.18, 2.235], wrist_l=[-0.045, 0.748, 1.667],
                  hand_r=[0, 0, 1], hand_l=[0, 0, 1],
                  palm_r=[0, 0, -1], palm_l=[0, 1, 0], grip_r=0.9, grip_l=0.15,
                  elbow_r=[0, -1, 0], elbow_l=[0, 0, 1],
                  ankle_r_rel=[0, 0, -0.869], ankle_l_rel=[0, 0, -0.869],
                  foot_r=[-1, 0, 0], foot_l=[-1, 0, 0])
    return _hold(k, props=[bar, wall], cam=32)


def flag_press_hang():
    return _hold(_press_pose(0))


def flag_kick_press():
    # Kick from standing contact; grips and pelvis are shifted together to keep the start at floor level.
    def kick_pose(p, legs="straight"):
        k = _press_pose(p, legs)
        k["pelvis"] = [k["pelvis"][0], k["pelvis"][1], k["pelvis"][2] - 0.42]
        k["wrist_r"] = [k["wrist_r"][0], k["wrist_r"][1], k["wrist_r"][2] - 0.42]
        k["wrist_l"] = [k["wrist_l"][0], k["wrist_l"][1], k["wrist_l"][2] - 0.42]
        # Feet point sideways in the body's local frame: flat at the start, aligned through the kick.
        r = math.radians(p)
        foot = [math.cos(r), 0.0, -math.sin(r)]
        k["foot_r"] = foot; k["foot_l"] = foot
        # World targets make the floor contacts explicit and let the loop interpolate the kick path.
        joints = solve(k, {})
        k["ankle_r"] = joints["ankle_r"]
        k["ankle_l"] = joints["ankle_l"]
        k.pop("ankle_r_rel", None); k.pop("ankle_l_rel", None)
        return k

    # Densely follow the pole-grip arc so interpolated shoulders stay within arm reach.
    angles = list(range(0, 181, 10)) + list(range(170, -1, -10))
    keys = []
    for i, p in enumerate(angles):
        legs = "tuck" if p >= 135 else "straight"
        keys.append((0.012 * i, kick_pose(p, legs)))
    landing = kick_pose(0)
    landing["pelvis"] = [landing["pelvis"][0] + 0.06, landing["pelvis"][1], landing["pelvis"][2] - 0.03]
    keys.extend([(0.46, landing), (0.62, kick_pose(0)), (0.85, kick_pose(0))])
    return dict(seconds=2.4, keys=keys, cam_az=26, cam_el=12, props=[POLE])


def flag_vertical_chamber_press():
    return _hold(_press_pose(180, "tuck"))


def flag_vertical_press():
    return _hold(_press_pose(180))


def flag_split_press():
    return _hold(_press_pose(90, "split"))


def flag_bent_leg_press():
    return _hold(_press_pose(90, "bent"))


def flag_press():
    return _hold(_press_pose(90))


EXERCISES = {
    "flag-clutch-hang": flag_clutch_hang,
    "flag-clutch-diagonal-split": flag_clutch_diagonal_split,
    "flag-clutch-diagonal-tuck": flag_clutch_diagonal_tuck,
    "flag-clutch-diagonal": flag_clutch_diagonal,
    "flag-clutch-horizontal-tuck": flag_clutch_horizontal_tuck,
    "flag-clutch-horizontal-split": flag_clutch_horizontal_split,
    "flag-clutch-bent-leg": flag_clutch_bent_leg,
    "flag-clutch": flag_clutch,
    "flag-support-press": flag_support_press,
    "flag-press-hang": flag_press_hang,
    "flag-kick-press": flag_kick_press,
    "flag-vertical-chamber-press": flag_vertical_chamber_press,
    "flag-vertical-press": flag_vertical_press,
    "flag-split-press": flag_split_press,
    "flag-bent-leg-press": flag_bent_leg_press,
    "flag-press": flag_press,
}
