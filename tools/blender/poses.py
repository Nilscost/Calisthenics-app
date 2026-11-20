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
    names = set(keys[0][1])
    for _, k in keys:
        if set(k) != names:
            raise ValueError(f"every key needs the same fields; differs: {sorted(names ^ set(k))}")
    for (t0, k0), (t1, k1) in zip(ks, ks[1:]):
        if t0 <= t <= t1:
            s = 0.0 if t1 == t0 else ease((t - t0) / (t1 - t0))
            return {n: lerp(k0[n], k1[n], s) for n in k0}
    return dict(keys[0][1])


def sample_continuous(keys, t):
    """Monotone cubic Hermite sampling: internal keys retain velocity.

    Flat intervals/reversals have zero tangent; harmonic-mean tangents prevent
    overshoot of the keyed reach limits. Endpoints rest for a seamless loop.
    """
    keys = keys if keys[-1][0] == 1 else keys + [(1., keys[0][1])]
    times = [k[0] for k in keys]
    index = next((i for i in range(len(keys)-1) if times[i] <= t <= times[i+1]), 0)
    def component(values):
        h = [b-a for a,b in zip(times,times[1:])]
        d = [(b-a)/dt for a,b,dt in zip(values,values[1:],h)]
        m = [0.]*len(values)
        for j in range(1,len(values)-1):
            if d[j-1]*d[j] > 0:
                w1,w2=2*h[j]+h[j-1],h[j]+2*h[j-1]
                m[j]=(w1+w2)/(w1/d[j-1]+w2/d[j])
        i=index;u=(t-times[i])/h[i]
        return ((2*u**3-3*u*u+1)*values[i]+(u**3-2*u*u+u)*h[i]*m[i]
                +(-2*u**3+3*u*u)*values[i+1]+(u**3-u*u)*h[i]*m[i+1])
    result={}
    for name,value in keys[0][1].items():
        result[name]=(component([k[name] for _,k in keys]) if isinstance(value,(int,float))
                      else [component([k[name][j] for _,k in keys]) for j in range(len(value))])
    return result


def steady_progress(t, ramp=.12):
    """Integrated cosine speed ramps; constant speed over the central 76%.

    Position, velocity and acceleration join continuously at both ramps.
    """
    t=max(0.,min(1.,t))
    if t>1-ramp:
        return 1-steady_progress(1-t,ramp)
    if t<ramp:
        return .5*(t-ramp/math.pi*math.sin(math.pi*t/ramp))/(1-ramp)
    return (t-.5*ramp)/(1-ramp)


def sample_paced_pullup(keys,t):
    """Keep the authored pose path, replacing its uneven time spacing.

    Invert the monotone height curve separately for ascent/descent. This
    preserves shoulder/arch articulation without inheriting key-to-key speed.
    """
    start,end=(.08,.5) if t<=.5 else (.58,1.)
    progress=steady_progress((t-start)/(end-start))
    height_keys=[(k,{'pelvis':v['pelvis']}) for k,v in keys]
    z0=sample_continuous(height_keys,start)['pelvis'][2]
    z1=sample_continuous(height_keys,end)['pelvis'][2]
    target=z0+(z1-z0)*progress
    lo,hi=start,end
    for _ in range(36):
        mid=(lo+hi)/2
        z=sample_continuous(height_keys,mid)['pelvis'][2]
        if (z<target)==(z1>z0):lo=mid
        else:hi=mid
    return sample_continuous(keys,(lo+hi)/2)


def rot(v, axis, deg):
    """Rotate v about a unit axis (Rodrigues)."""
    a = math.radians(deg)
    c, s_ = math.cos(a), math.sin(a)
    return add(add(mul(v, c), mul(cross(axis, v), s_)), mul(axis, dot(axis, v) * (1 - c)))


def trunk_frame(pitch, yaw=0.0, roll=0.0):
    """(up, left) of a trunk segment: pitch about the left axis (0 up, +90 face down head +X),
    then roll about the body's long axis (+90 = lying on the left side when pitch 90), then yaw about Z."""
    up, left = [0.0, 0.0, 1.0], [0.0, 1.0, 0.0]
    up, left = rot(up, [0, 0, 1], roll), rot(left, [0, 0, 1], roll)
    up, left = rot(up, [0, 1, 0], pitch), rot(left, [0, 1, 0], pitch)
    up, left = rot(up, [0, 0, 1], yaw), rot(left, [0, 0, 1], yaw)
    return up, left


def solve(k, report):
    """Joint positions from one interpolated pose dict."""
    J = {}
    P = k["pelvis"]
    yaw, roll = k.get("yaw", 0.0), k.get("roll", 0.0)
    up1, left1 = trunk_frame(k["p1"], yaw, roll)
    upper_roll = roll + k.get("thoracic_twist", 0.0)
    up2, left2 = trunk_frame(k["p2"], yaw, upper_roll)
    J["pelvis"] = P
    J["spine"] = add(P, mul(up1, L["lumbar"]))
    J["chest"] = add(J["spine"], mul(up2, L["thorax"]))
    uph, lefth = trunk_frame(k["p2"] + k.get("hp", 0.0), yaw, upper_roll)
    forwardh = unit(cross(lefth, uph))
    side_tilt = k.get("head_side_tilt", 0.0)
    if side_tilt:
        uph, lefth = rot(uph, forwardh, side_tilt), rot(lefth, forwardh, side_tilt)
    if k.get("head_turn", 0.0):
        lefth = rot(lefth, uph, k["head_turn"])
    J["neck"] = add(J["chest"], mul(uph, L["neck"]))
    if k.get("head_retract", 0.0):
        J["neck"] = add(J["neck"], mul(unit(cross(left2, up2)), -k["head_retract"]))
    J["head"] = add(J["neck"], mul(uph, L["head_r"] * 0.9))
    if any(name in k for name in ("head_turn", "head_side_tilt", "head_retract", "head_detail")):
        J["head_left"] = lefth
        J["head_detail"] = [k.get("head_detail", 1.0), 0.0, 0.0]
    J["pelvis_up"], J["chest_up"] = up1, up2
    J["pelvis_left"], J["chest_left"] = left1, left2
    for side, sgn in (("l", 1), ("r", -1)):
        hip = add(add(P, mul(left1, sgn * L["hip_w"])), mul(up1, -0.03))
        sh = add(add(J["chest"], mul(left2, sgn * L["sh_w"])), mul(up2, -0.03))
        offset = k.get("shoulder_offset_" + side)
        if offset is not None:
            forward = unit(cross(left2, up2))
            sh = add(sh, add(add(mul(forward, offset[0]), mul(left2, offset[1])), mul(up2, offset[2])))
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
        # palm normal (direction the palm faces) and finger curl 0..1, for render.py's hands
        J["palmn_" + side] = unit(k.get("palm_" + side, [0, 0, -1]))
        J["grip_" + side] = [k.get("grip_" + side, 0.15), 0.0, 0.0]
        for feature in ("finger_lift", "foot_arch", "grip_offset"):
            name = feature + "_" + side
            if name in k:
                value = k[name]
                J[name] = list(value) if isinstance(value, (list, tuple)) else [value, 0.0, 0.0]
    # Scoped apparatus trajectories are explicit pose channels. Legacy poses
    # omit them and retain byte-identical body geometry.
    for name,value in k.items():
        if name.startswith('ring_center_') or name.startswith('ring_plane_') or name.startswith('ring_radial_'):
            J[name]=list(value)
    return J


# --- helpers for exercise modules (tools/blender/families/*.py) ------------------
def base_pose(**over):
    """A neutral standing pose with every field solve() needs; override what changes.

    Fields: pelvis [x,y,z]; p1/p2 pitch of the lower/upper trunk (deg, 0 = upright, +90 = lying face down
    with the head toward +X, -90 = lying on the back with the head toward -X); yaw (deg, turn about Z,
    +90 = facing +Y); roll (deg, about the body's long axis: with p 90, roll +90 turns the LEFT side up = lying on the right side;
    roll -90 turns the RIGHT (camera-near) side up = lying on the left side); hp head pitch relative to the trunk;
    per side s in (l, r): ankle_s (world) OR ankle_s_rel (from the hip), knee_s (knee pole direction),
    foot_s (ankle -> toe direction), wrist_s (world) OR wrist_s_rel (from the shoulder),
    elbow_s (elbow pole), hand_s (wrist -> fingertips), palm_s (palm normal), grip_s (0 flat .. 1 fist).
    Use the SAME field names in every key of one exercise (sample() checks this).
    """
    k = dict(pelvis=[0.0, 0.0, 0.955], p1=1.0, p2=2.0, hp=0.0, yaw=0.0, roll=0.0)
    for s_, sg in (("l", 1), ("r", -1)):
        k["ankle_" + s_] = [0.0, sg * 0.10, 0.08]
        k["knee_" + s_] = [1.0, 0.0, 0.0]
        k["foot_" + s_] = [1.0, sg * 0.08, 0.0]
        k["wrist_" + s_ + "_rel"] = [0.02, sg * 0.04, -0.565]
        k["elbow_" + s_] = [-1.0, sg * 0.2, 0.0]
        k["hand_" + s_] = [0.0, 0.0, -1.0]
        k["palm_" + s_] = [0.0, -sg * 1.0, 0.0]
        k["grip_" + s_] = 0.25
    for name, v in over.items():
        if name.endswith("_rel") and name.replace("_rel", "") in k:
            del k[name.replace("_rel", "")]
        elif (name + "_rel") in k:
            del k[name + "_rel"]
        k[name] = v
    return k


def both(**per_side):
    """both(wrist=[x, y, z]) -> wrist_l=[x, +y, z], wrist_r=[x, -y, z] (y mirrored).
    both(wrist_rel=v) -> wrist_l_rel / wrist_r_rel. Scalars (grip=0.9) are copied to both sides."""
    out = {}
    for name, v in per_side.items():
        base, suffix = (name[:-4], "_rel") if name.endswith("_rel") else (name, "")
        if isinstance(v, (int, float)):
            out[f"{base}_l{suffix}"], out[f"{base}_r{suffix}"] = v, v
        else:
            out[f"{base}_l{suffix}"] = [v[0], v[1], v[2]]
            out[f"{base}_r{suffix}"] = [v[0], -v[1], v[2]]
    return out


def rep(keys_top, keys_bottom, hold_top=0.1, down_end=0.48, hold_bottom=0.1):
    """Typical rep timing: hold the top, go down, hold the bottom, come back up (loops)."""
    return [(0.0, keys_top), (hold_top, keys_top), (down_end, keys_bottom),
            (min(down_end + hold_bottom, 0.95), keys_bottom)]


def pt(joint, off=(0, 0, 0)):
    """A prop end point attached to a joint (moves with the figure)."""
    return {"joint": joint, "off": list(off)}


# Prop kinds (render.py and preview_sheet.py draw them):
#   dict(kind="bar", z=2.3, half=0.65, r=0.017[, x=0])     pull-up bar with posts (bar along Y at x)
#   dict(kind="chair", x=-1.5, seat=0.45)                   chair, seat centre at x, back on the -X side
#   dict(kind="box", center=[x,y,z], size=[sx,sy,sz])       step, bench, wall (thin box), mat (thin)
#   dict(kind="rod", a=P, b=P, r=0.017)                     bar, dip bar, band, towel, barbell, door edge;
#                                                           P = [x,y,z] or pt("wrist_r", (0, 0, 0))
#   dict(kind="weight", type="dumbbell"|"kettlebell", joint="wrist_r", off=[0,0,-0.05])
#   dict(kind="plate", center=[x,y,z], r=0.2, normal=[0,1,0])  sliders/plates/discs


# --- exercises ---------------------------------------------------------------
BAR_Z = 2.30
DOWN = [0, 0, -1]


def pullup():
    # Controlled mild arch: initiate at the shoulder girdle, pull the upper
    # chest toward the bar and return without a leg kick.
    grip = 0.32
    w = BAR_Z - 0.065
    def key(pz, px, p1, p2, legs_fwd, scap, hp=8):
        return dict(pelvis=[px,0,pz],p1=p1,p2=p2,hp=hp,
                    wrist_l=[-.045,grip,w],wrist_r=[-.045,-grip,w],
                    palm_l=[1,0,0],palm_r=[1,0,0],grip_l=.95,grip_r=.95,
                    elbow_l=[.35,1,-.2],elbow_r=[.35,-1,-.2],
                    hand_l=[0,0,1],hand_r=[0,0,1],
                    shoulder_offset_l=[0,0,scap],shoulder_offset_r=[0,0,scap],
                    ankle_l_rel=[legs_fwd,-.01,-.85],ankle_r_rel=[legs_fwd,.01,-.85],
                    knee_l=[1,0,0],knee_r=[1,0,0],
                    foot_l=[.45,0,-1],foot_r=[.45,0,-1])
    hang=key(1.201,-.04,-3,-6,.10,.015,4)
    active=key(1.235,-.035,-3,-7,.10,-.010,4)
    mid=key(1.45,0,-5,-14,.12,-.015,8)
    top=key(1.70,.15,-9,-24,.15,-.020,12)
    keys=[(0,hang),(.08,hang),(.18,active),(.34,mid),(.5,top),
          (.58,top),(.76,mid),(.92,active),(1,hang)]
    return dict(seconds=4,keys=keys,cam_az=25,cam_el=8,
                props=[dict(kind="bar",z=BAR_Z,half=.65,r=.017)],
                motion_profile='pullup-controlled-mild-arch',anatomy_detail=True,
                interpolation='paced-pullup',bar_grip=dict(z=BAR_Z,r=.017))


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
                    hand_l=[1, 0.15, 0], hand_r=[1, -0.15, 0], grip_l=0.0, grip_r=0.0,
                    ankle_l=add(ankle, [0, 0.08, 0]), ankle_r=add(ankle, [0, -0.08, 0]),
                    knee_l=[0, 0, -1], knee_r=[0, 0, -1],
                    foot_l=fd, foot_r=fd)
    keyed = [(0.0, {"a": TOP_A, "phi": 0.0}), (0.1, {"a": TOP_A, "phi": 0.0}),
             (0.5, {"a": BOT_A, "phi": BOT_PHI}), (0.58, {"a": BOT_A, "phi": BOT_PHI})]
    return dict(seconds=2.5, keys=dense(key, keyed), cam_az=18, cam_el=12,
                props=[dict(kind="chair", x=T[0] - 0.10, seat=seat)])


TOP_A, BOT_A, BOT_PHI = -2.25, -16.0, 40.0


def pistol():
    from pistol_refinement import specification
    return specification()


def pigeon():
    # right (near) leg in front, shin across the body; left leg straight back
    def key(pz, px, pitch, hands_x, elbow_out):
        # folding forward = walking the hands out along the floor (the usual cue)
        return dict(pelvis=[px, 0.0, pz], p1=pitch * 0.8, p2=pitch, hp=-15 if pitch > 40 else 0,
                    ankle_r=[0.31, 0.10, 0.07], knee_r=[0.4, -0.6, -0.1], foot_r=[0.1, 1, 0.0],
                    ankle_l=[-0.894, 0.12, 0.075], knee_l=[0, 0.1, -1], foot_l=[-1, 0, -0.32],
                    wrist_l=[hands_x, 0.24, 0.035], wrist_r=[hands_x, -0.27, 0.035], grip_l=0.0, grip_r=0.0,
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


def load_families():
    """Merge EXERCISES dicts from tools/blender/families/*.py into EXERCISES."""
    import importlib
    here = os.path.dirname(os.path.abspath(__file__))
    fam_dir = os.path.join(here, "families")
    if here not in sys.path:
        sys.path.insert(0, here)
    sys.modules.setdefault("poses", sys.modules[__name__])
    if not os.path.isdir(fam_dir):
        return
    for f in sorted(os.listdir(fam_dir)):
        if f.endswith(".py") and not f.startswith("_"):
            mod = importlib.import_module("families." + f[:-3])
            for k, v in getattr(mod, "EXERCISES", {}).items():
                if k in EXERCISES and EXERCISES[k] is not v:
                    raise ValueError(f"{k} defined twice ({f})")
                EXERCISES[k] = v


def build(ex_id):
    spec = EXERCISES[ex_id]()
    if ex_id in ("chinup-full", "pullup-full"):
        spec["keys"] = [(t, {**key, "head_detail": 1}) for t, key in spec["keys"]]
    n = int(round(spec["seconds"] * FPS))
    frames, report = [], {}
    for f in range(n):
        sampler = {"continuous": sample_continuous, "paced-pullup": sample_paced_pullup}.get(spec.get("interpolation"), sample)
        J = solve(sampler(spec["keys"], f / n), report)
        frames.append({k: [round(x, 5) for x in v] for k, v in J.items()})
    msgs = [f"{k}: target up to {v:.3f} m out of reach" for k, v in sorted(report.items())]
    data = dict(id=ex_id, fps=FPS, frames=frames, props=spec.get("props", []),
                cam_az=spec["cam_az"], cam_el=spec["cam_el"],
                lengths=L)
    for name in ("motion_profile", "anatomy_detail", "bar_grip", "ring_grip", "grip_detail"):
        if name in spec:
            data[name] = spec[name]
    if "camera_focus" in spec:
        data["camera_focus"] = spec["camera_focus"]
    if "camera_padding" in spec:
        data["camera_padding"] = spec["camera_padding"]
    if ex_id in ("chinup-full", "pullup-full"):
        data["grip_detail"] = "PALMS TOWARD FACE" if ex_id == "chinup-full" else "PALMS AWAY FROM FACE"
    return data, msgs


# body surface radius around each joint (render.py shapes), for the floor check
RADIUS = dict(toe_l=0.02, toe_r=0.02, ankle_l=0.04, ankle_r=0.04, knee_l=0.05, knee_r=0.05, wrist_l=0.025,
              wrist_r=0.025, hand_l=0.012, hand_r=0.012, elbow_l=0.04, elbow_r=0.04, hip_l=0.07, hip_r=0.07,
              shoulder_l=0.05, shoulder_r=0.05, chest=0.10, spine=0.09, pelvis=0.09, head=0.10)


def checks(data):
    """Floor checks: a joint's body surface more than 2 cm below the floor (list of strings; empty = OK)."""
    out, seen = [], set()
    pts = list(RADIUS)
    for i, fr in enumerate(data["frames"]):
        for p in pts:
            if fr[p][2] - RADIUS.get(p, 0.0) < -0.02 and p not in seen:
                seen.add(p)
                out.append(f"{p} sinks into the floor from frame {i} (surface {fr[p][2] - RADIUS.get(p, 0.0):+.3f} m)")
    return out


def _angle(a, b, c):
    u, v = sub(a, b), sub(c, b)
    return math.degrees(math.acos(max(-1.0, min(1.0, dot(u, v) / (norm(u) * norm(v) + 1e-9)))))


def describe(data, moments=(0.0, 0.25, 0.5, 0.75)):
    """Text check of a clip, for reviewing poses without images."""
    out = []
    fr_all = data["frames"]
    for m in moments:
        f = fr_all[int(m * len(fr_all)) % len(fr_all)]
        on_floor = [j for j in ("toe_l", "toe_r", "ankle_l", "ankle_r", "knee_l", "knee_r", "hand_l", "hand_r",
                                "wrist_l", "wrist_r", "elbow_l", "elbow_r", "pelvis", "chest", "head")
                    if f[j][2] - RADIUS.get(j, 0.0) < 0.05]
        up = f["chest_up"]
        trunk = math.degrees(math.atan2(math.hypot(up[0], up[1]), up[2]))
        out.append(
            f"  t={m:.2f} pelvis z={f['pelvis'][2]:.2f} x={f['pelvis'][0]:+.2f} | head z={f['head'][2]:.2f} | "
            f"trunk {trunk:.0f} deg from vertical | knee L/R {_angle(f['hip_l'], f['knee_l'], f['ankle_l']):.0f}/"
            f"{_angle(f['hip_r'], f['knee_r'], f['ankle_r']):.0f} | elbow L/R "
            f"{_angle(f['shoulder_l'], f['elbow_l'], f['wrist_l']):.0f}/{_angle(f['shoulder_r'], f['elbow_r'], f['wrist_r']):.0f}"
            f" | hip(thigh-trunk) R {_angle(f['knee_r'], f['hip_r'], f['chest']):.0f} | "
            f"wrist R z={f['wrist_r'][2]:.2f} ankle R z={f['ankle_r'][2]:.2f} | on floor: {', '.join(on_floor) or 'nothing'}")
    return "\n".join(out)


def main(argv):
    root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    load_families()
    if "--list" in argv:
        print("\n".join(sorted(EXERCISES)))
        return 0
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
        if "--describe" in argv:
            print(describe(data))
        if preview:
            import preview_sheet
            preview_sheet.sheet(data, os.path.join(preview, ex + ".png"))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    sys.exit(main(sys.argv[1:]))
