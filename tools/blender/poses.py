"""Joint motion for the Blender bake-off clips (docs/16-video-clips-plan.md).

Pure Python (no numpy, no bpy). For each exercise it interpolates a few keyed
poses over one loop, solves arms and legs with two-bone IK so hands and feet
stay exactly on their targets (bar, floor, chair), and writes per-frame joint
positions to work/clips-bakeoff/<id>/joints.json. tools/blender/render.py
(run on the Mac in Blender) turns that file into frames; nothing here needs
Blender, so poses can be checked in the sandbox with --preview.

World axes: Z up, figure faces +X, +Y is the figure's left. Metres.

    python3 tools/blender/poses.py                 # write joints.json for all
    python3 tools/blender/poses.py --preview DIR   # plus stick-figure sheets
"""
import json
import math
import os
import sys

# Segment lengths for a ~1.75 m adult.
L = dict(lumbar=0.24, thorax=0.26, neck=0.10, head_r=0.11,
         hip_w=0.09, sh_w=0.18,
         thigh=0.44, shin=0.43, foot=0.20,
         upper=0.30, fore=0.27, hand=0.17)

FPS = 24


# --- small vector helpers -------------------------------------------------
def add(a, b): return [a[i] + b[i] for i in range(3)]
def sub(a, b): return [a[i] - b[i] for i in range(3)]
def mul(a, s): return [a[i] * s for i in range(3)]
def dot(a, b): return sum(a[i] * b[i] for i in range(3))
def norm(a): return math.sqrt(dot(a, a))
def unit(a):
    n = norm(a)
    return [x / n for x in a]
def cross(a, b):
    return [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]]
def pitch_dir(deg):
    """Unit vector in the XZ plane, 0 = straight up, +90 = forward (+X)."""
    r = math.radians(deg)
    return [math.sin(r), 0.0, math.cos(r)]


def two_bone(root, target, a, b, pole, report, name):
    """Middle joint and (possibly clamped) end for a two-bone chain."""
    d_vec = sub(target, root)
    d = norm(d_vec)
    reach = a + b
    if d > reach + 0.004:
        report[name] = max(report.get(name, 0.0), d - reach)
    d_c = min(max(d, abs(a - b) + 1e-4), reach * 0.99995)
    u = unit(d_vec)
    x = (a * a - b * b + d_c * d_c) / (2 * d_c)
    h = math.sqrt(max(a * a - x * x, 0.0))
    p = sub(pole, mul(u, dot(pole, u)))
    p = unit(p)
    mid = add(add(root, mul(u, x)), mul(p, h))
    end = add(root, mul(u, d_c))
    return mid, end


# --- keyed poses -------------------------------------------------------------
def ease(x):
    return x * x * (3 - 2 * x)


def lerp(a, b, s):
    if isinstance(a, (int, float)):
        return a + (b - a) * s
    return [a[i] + (b[i] - a[i]) * s for i in range(len(a))]


def sample(keys, t):
    """keys: list of (t, dict); loops back to keys[0] at t = 1."""
    ks = keys + [(1.0, keys[0][1])]
    for (t0, k0), (t1, k1) in zip(ks, ks[1:]):
        if t0 <= t <= t1:
            s = 0.0 if t1 == t0 else ease((t - t0) / (t1 - t0))
            return {n: lerp(k0[n], k1[n], s) for n in k0}
    return dict(keys[0][1])


def solve(k, report):
    """Joint positions from one interpolated pose dict."""
    J = {}
    P = k["pelvis"]
    up1, up2 = pitch_dir(k["p1"]), pitch_dir(k["p2"])
    left = [0.0, 1.0, 0.0]
    J["pelvis"] = P
    J["spine"] = add(P, mul(up1, L["lumbar"]))
    J["chest"] = add(J["spine"], mul(up2, L["thorax"]))
    uph = pitch_dir(k["p2"] + k.get("hp", 0.0))
    J["neck"] = add(J["chest"], mul(uph, L["neck"]))
    J["head"] = add(J["neck"], mul(uph, L["head_r"] * 0.9))
    J["pelvis_up"], J["chest_up"] = up1, up2
    for side, sgn in (("l", 1), ("r", -1)):
        hip = add(add(P, mul(left, sgn * L["hip_w"])), mul(up1, -0.03))
        sh = add(add(J["chest"], mul(left, sgn * L["sh_w"])), mul(up2, -0.03))
        J["hip_" + side], J["shoulder_" + side] = hip, sh
        # legs
        tgt = k.get("ankle_" + side)
        if tgt is None:
            tgt = add(hip, k["ankle_" + side + "_rel"])
        knee, ank = two_bone(hip, tgt, L["thigh"], L["shin"], k["knee_" + side], report, "leg_" + side)
        J["knee_" + side], J["ankle_" + side] = knee, ank
        J["toe_" + side] = add(ank, mul(unit(k["foot_" + side]), L["foot"]))
        # arms
        tgt = k.get("wrist_" + side)
        if tgt is None:
            tgt = add(sh, k["wrist_" + side + "_rel"])
        elb, wr = two_bone(sh, tgt, L["upper"], L["fore"], k["elbow_" + side], report, "arm_" + side)
        J["elbow_" + side], J["wrist_" + side] = elb, wr
        J["hand_" + side] = add(wr, mul(unit(k["hand_" + side]), L["hand"]))
    return J


# --- exercises ---------------------------------------------------------------
BAR_Z = 2.30
DOWN = [0, 0, -1]


def pullup():
    grip = 0.25
    w = BAR_Z - 0.035  # wrist just under the bar centre
    def key(pz, px, p1, p2, legs_fwd):
        return dict(pelvis=[px, 0, pz], p1=p1, p2=p2, hp=0,
                    wrist_l=[0, grip, w], wrist_r=[0, -grip, w],
                    elbow_l=[0.35, 1, -0.2], elbow_r=[0.35, -1, -0.2],
                    hand_l=[0, 0, 1], hand_r=[0, 0, 1],
                    ankle_l_rel=[legs_fwd, -0.01, -0.84], ankle_r_rel=[legs_fwd, 0.01, -0.84],
                    knee_l=[1, 0, 0], knee_r=[1, 0, 0],
                    foot_l=[0.45, 0, -1], foot_r=[0.45, 0, -1])
    hang = key(BAR_Z - 1.068, -0.02, -3, -6, 0.10)
    top = key(BAR_Z - 0.62, -0.10, 4, -14, 0.18)
    keys = [(0.0, hang), (0.12, hang), (0.48, top), (0.58, top), (1.0, hang)]
    return dict(seconds=3.0, keys=keys, cam_az=35, cam_el=8,
                props=[dict(kind="bar", z=BAR_Z, half=0.65, r=0.017)])


def dense(fn, keyed, steps=48):
    """Key a pose function densely on interpolated parameters (arcs stay arcs)."""
    return [(i / steps, fn(**sample(keyed, i / steps))) for i in range(steps)]


def pushup_fe():
    seat = 0.45
    T = [-1.40, 0.0, seat + 0.026]  # toe tip (render.py foot shape) on the seat
    def key(a, phi):
        # a: body line angle (deg, + = head higher); phi: feet roll over the toes
        p = math.radians(phi)
        fd = [-math.sin(p), 0, -math.cos(p)]
        n = [-math.cos(p), 0, math.sin(p)]
        ankle = sub(sub(T, mul(fd, 0.165)), mul(n, 0.045))
        d = [math.cos(math.radians(a)), 0, math.sin(math.radians(a))]
        pelvis = add(ankle, mul(d, 0.898))  # hip joint sits 0.03 behind the pelvis centre
        pitch = 90 - a
        return dict(pelvis=pelvis, p1=pitch, p2=pitch, hp=-8,
                    wrist_l=[-0.10, 0.27, 0.035], wrist_r=[-0.10, -0.27, 0.035],
                    elbow_l=[-0.6, 0.7, 1.0], elbow_r=[-0.6, -0.7, 1.0],
                    hand_l=[1, 0.15, 0], hand_r=[1, -0.15, 0],
                    ankle_l=add(ankle, [0, 0.08, 0]), ankle_r=add(ankle, [0, -0.08, 0]),
                    knee_l=[0, 0, -1], knee_r=[0, 0, -1],
                    foot_l=fd, foot_r=fd)
    keyed = [(0.0, {"a": TOP_A, "phi": 0.0}), (0.1, {"a": TOP_A, "phi": 0.0}),
             (0.5, {"a": BOT_A, "phi": BOT_PHI}), (0.58, {"a": BOT_A, "phi": BOT_PHI})]
    return dict(seconds=2.5, keys=dense(key, keyed), cam_az=18, cam_el=12,
                props=[dict(kind="chair", x=T[0] - 0.10, seat=seat)])


TOP_A, BOT_A, BOT_PHI = -2.25, -16.0, 40.0


def pistol():
    # stands on the left (far) leg; the right (near) leg stays straight and rises forward
    st_ankle = [0.0, 0.09, 0.08]
    def key(pz, px, p1, p2, lift):
        r = math.radians(lift)
        return dict(pelvis=[px, 0.0, pz], p1=p1, p2=p2, hp=-10,
                    ankle_l=st_ankle, knee_l=[1, 0.15, 0], foot_l=[1, 0, 0],
                    ankle_r_rel=[0.862 * math.sin(r), -0.03, -0.862 * math.cos(r)],
                    knee_r=[0.3, 0, 1], foot_r=[math.cos(r) * 0.6, 0, math.sin(r) + 0.3],
                    wrist_l_rel=[0.565, 0.04, -0.02], wrist_r_rel=[0.565, -0.04, -0.02],
                    elbow_l=[0, 0.3, -1], elbow_r=[0, -0.3, -1],
                    hand_l=[1, 0, 0], hand_r=[1, 0, 0])
    top = {"pz": 0.955, "px": 0.0, "p1": 2, "p2": 4, "lift": 30}
    bottom = {"pz": 0.36, "px": -0.27, "p1": 38, "p2": 48, "lift": 78}
    keyed = [(0.0, top), (0.1, top), (0.48, bottom), (0.58, bottom)]
    return dict(seconds=4.0, keys=dense(key, keyed, 64), cam_az=22, cam_el=10, props=[])


def pigeon():
    # right (near) leg in front, shin across the body; left leg straight back
    def key(pz, px, pitch, hands_x, elbow_out):
        # folding forward = walking the hands out along the floor (the usual cue)
        return dict(pelvis=[px, 0.0, pz], p1=pitch * 0.8, p2=pitch, hp=-15 if pitch > 40 else 0,
                    ankle_r=[0.31, 0.10, 0.07], knee_r=[0.4, -0.6, -0.1], foot_r=[0.1, 1, 0.0],
                    ankle_l=[-0.885, 0.12, 0.075], knee_l=[0, 0.1, -1], foot_l=[-1, 0, -0.32],
                    wrist_l=[hands_x, 0.24, 0.035], wrist_r=[hands_x, -0.27, 0.035],
                    elbow_l=[-0.3, elbow_out, -1], elbow_r=[-0.3, -elbow_out, -1],
                    hand_l=[1, 0, 0], hand_r=[1, 0, 0])
    up = key(0.15, -0.02, 35, 0.44, 0.3)
    fold = key(0.13, 0.0, 70, 0.82, 0.3)
    keys = [(0.0, up), (0.15, up), (0.5, fold), (0.7, fold), (1.0, up)]
    return dict(seconds=4.0, keys=keys, cam_az=62, cam_el=30, props=[])


EXERCISES = {
    "pullup-full": pullup,
    "pushup-feet-elevated": pushup_fe,
    "squat-pistol": pistol,
    "stretch-pigeon": pigeon,
}


def build(ex_id):
    spec = EXERCISES[ex_id]()
    n = int(round(spec["seconds"] * FPS))
    frames, report = [], {}
    for f in range(n):
        J = solve(sample(spec["keys"], f / n), report)
        frames.append({k: [round(x, 5) for x in v] for k, v in J.items()})
    msgs = [f"{k}: target up to {v:.3f} m out of reach" for k, v in sorted(report.items())]
    return dict(id=ex_id, fps=FPS, frames=frames, props=spec["props"],
                cam_az=spec["cam_az"], cam_el=spec["cam_el"],
                lengths=L), msgs


def checks(data):
    """Contact and floor checks: list of strings (empty = OK)."""
    out, seen = [], set()
    pts = ["toe_l", "toe_r", "ankle_l", "ankle_r", "knee_l", "knee_r", "wrist_l",
           "wrist_r", "hand_l", "hand_r", "pelvis", "head", "elbow_l", "elbow_r"]
    for i, fr in enumerate(data["frames"]):
        for p in pts:
            if fr[p][2] < -0.005 and p not in seen:
                seen.add(p)
                out.append(f"{p} below floor from frame {i} ({fr[p][2]:.3f})")
    return out


def main(argv):
    root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    preview = argv[argv.index("--preview") + 1] if "--preview" in argv else None
    ids = [a for a in argv if a in EXERCISES] or list(EXERCISES)
    ok = True
    for ex in ids:
        data, report = build(ex)
        problems = report + checks(data)
        out = os.path.join(root, "work", "clips-bakeoff", ex)
        os.makedirs(out, exist_ok=True)
        with open(os.path.join(out, "joints.json"), "w") as fh:
            json.dump(data, fh)
        print(f"{ex}: {len(data['frames'])} frames", "OK" if not problems else "")
        for p in problems:
            print("   ", p)
        ok = ok and not problems
        if preview:
            import preview_sheet
            preview_sheet.sheet(data, os.path.join(preview, ex + ".png"))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    sys.exit(main(sys.argv[1:]))
