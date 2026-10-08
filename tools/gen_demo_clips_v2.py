#!/usr/bin/env python3
"""Demo clips v2 (U11, plan §5): 3/4 view, solid grey capsule body, worked muscles orange-red, equipment in 3-D.

Reuses the sagittal poses of v1 (tools/gen_demo_clips.py) and adds the ten exercises U09 introduced. Each 2-D pose is lifted to
3-D (left/right limbs get a z offset), projected orthographically (yaw 30 deg, pitch 15 deg) and drawn back to front.
Only the muscles listed in the catalog are coloured: primary saturated, secondary light. Hidden ones (e.g. the chest in a
push-up seen from above) are drawn as a faint x-ray tint so the colour code stays readable.
Output: app/src/main/assets/demos/<variationId>.mp4 (480x360, 24 fps, 2 s loop, H.264, < 120 KB). Pillow + imageio-ffmpeg only.
Run:  python3 tools/gen_demo_clips_v2.py [--sheet out.png] [--check] [id ...]
"""
import json, math, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_demo_clips as v1
from PIL import Image, ImageDraw, ImageFont

W, H, SS, FPS, SECONDS = 480, 360, 2, 24, 2
THETA, PHI = math.radians(30), math.radians(15)
BG, GRID, OUTLINE = (255, 255, 255), (222, 226, 232), (96, 102, 114)
BODY, BODY_FAR = (200, 204, 212), (150, 156, 168)
PRIMARY, SECONDARY = (232, 89, 12), (255, 178, 122)
EQUIP, EQUIP_DARK = (120, 128, 140), (84, 90, 102)
R_TORSO, R_UARM, R_FARM, R_THIGH, R_SHIN, R_HEAD = 11, 5.2, 4.4, 7.2, 5.4, 11
Z_ARM, Z_LEG = 9, 6.5
lerp, lerp2, rep, sh, ik = v1.lerp, v1.lerp2, v1.rep, v1.sh, v1.ik
L_T = v1.L_T

# ----------------------------------------------------------------------------------------------- new poses (sagittal, v1 format)
def inv_row(knees_bent=False, feet_up=False, archer=False):
    bar_y, bar_x = 150, 206
    def f(p):
        s = rep(p)
        py = 84 if feet_up else 46
        pivot = (46, py)
        lo, hi = ((98, 135) if feet_up else (88, 135))
        sy = lerp(lo, hi, s)
        phi = math.asin(max(-1, min(1, (sy - py) / 144)))
        c, sn = math.cos(phi), math.sin(phi)
        hip = (pivot[0] + 84 * c, pivot[1] + 84 * sn)
        sho = (pivot[0] + 144 * c, pivot[1] + 144 * sn)
        hand = (bar_x, bar_y)
        if knees_bent: legs = [("ik", (hip[0] - 62, 46), (0, 1))] * 2
        else: legs = [("ik", pivot, (0, 1))] * 2
        arms = [("ik", hand, (-1, -0.2))] * 2
        pose = dict(hip=hip, a=math.degrees(phi), head_off=10, arms=arms, legs=legs,
                    scene=[("bar", bar_x, bar_y)] + ([("rect", (20, 44, 74, 84), v1.GRD)] if feet_up else []))
        if archer: pose["arm_z"] = [Z_ARM + 4, -70]; pose["arms"] = [("ik", (bar_x, bar_y), (-1, -0.2)), ("ik", (sho[0] + 18, lerp(sho[1] + 6, sho[1] + 40, 1 - s)), (-1, 0))]
        return pose
    return f

def arch_hold(y_arms):
    def f(p):
        s = 0.85 + 0.15 * math.sin(2 * math.pi * p)
        hip = (220, 50); a = lerp(8, 24, s); sho = sh(hip, a)
        d = math.radians(lerp(10, 30, s))
        hand = (sho[0] + 62 * math.cos(d), sho[1] + 62 * math.sin(d))
        ank = (hip[0] - 84 * math.cos(d), hip[1] + 84 * math.sin(d))
        pose = dict(hip=hip, a=a, arms=[("ik", hand, (0, 1))] * 2, legs=[("ik", ank, (0, 1))] * 2)
        if y_arms: pose["arm_z"] = [34, -34]
        return pose
    return f

def arch_rocks(p):
    s = rep(p)
    hip = (lerp(212, 228, s), 50); a = lerp(26, 12, s); sho = sh(hip, a)
    d = math.radians(lerp(30, 16, s))
    hand = (sho[0] + 62 * math.cos(d), sho[1] + 62 * math.sin(d)); ank = (hip[0] - 84 * math.cos(d), hip[1] + 84 * math.sin(d))
    return dict(hip=hip, a=a, arms=[("ik", hand, (0, 1))] * 2, legs=[("ik", ank, (0, 1))] * 2, arm_z=[34, -34])

def side_plank_raise(p):
    b = math.sin(2 * math.pi * p) * 0.0
    pivot = (95, 46); sy = 110
    phi = math.asin((sy - 46) / 144)
    hip = (95 + 84 * math.cos(phi), 46 + 84 * math.sin(phi)); sx = 95 + 144 * math.cos(phi); sho = (sx, sy)
    th = math.radians(lerp(0, 38, rep(p)))
    dx, dy = pivot[0] - hip[0], pivot[1] - hip[1]
    rx, ry = dx * math.cos(th) - dy * math.sin(th), dx * math.sin(th) + dy * math.cos(th)
    return dict(hip=hip, a=math.degrees(phi), head_off=10,
                arms=[("ik", (sx + 2, 46), (1, 0)), ("ik", (sho[0], sho[1] + 60), (1, 0))],
                legs=[("ik", pivot, (0, 1)), ("ik", (hip[0] + rx, hip[1] + ry), (0, 1))])

def copenhagen(p):
    b = math.sin(2 * math.pi * p)
    pivot = (88, 88); sy = 112 + 1.5 * b
    phi = math.asin((sy - 88) / 144)
    hip = (pivot[0] + 84 * math.cos(phi), pivot[1] + 84 * math.sin(phi)); sx = pivot[0] + 144 * math.cos(phi); sho = (sx, sy)
    return dict(hip=hip, a=math.degrees(phi), head_off=10,
                arms=[("ik", (sx + 2, 46), (1, 0)), ("ik", (sho[0] + 4, sho[1] + 14), (1, 0))],
                legs=[("ik", pivot, (0, 1)), ("ik", (hip[0] - 18, 50), (0, 1))],
                scene=[("rect", (55, 44, 125, 84), v1.GRD)])

def kb_rdl(p):
    s = rep(p)
    a = lerp(90, 14, s)
    hip = (lerp(205, 190, s), lerp(128, 118, s))
    sho = sh(hip, a)
    r = math.radians(a)
    ank_free = (hip[0] - 84 * math.cos(r), hip[1] - 84 * math.sin(r))
    if a > 60: ank_free = (hip[0] - 2, hip[1] - 84)
    hand = (sho[0] + 3, sho[1] - 62)
    return dict(hip=hip, a=a, head_off=lerp(0, 24, s), arms=[("ik", hand, (-1, 0))] * 2,
                legs=[("ik", (205, 46), (1, 0)), ("ik", ank_free, (0, 1))], props=[("circle", "hand0", 9, v1.PROP, (0, -10))])

def kb_swing(p):
    s = rep(p)
    a = lerp(42, 88, s)
    hip = (lerp(176, 205, s), lerp(100, 128, s)); sho = sh(hip, a)
    hand = lerp2((hip[0] - 6, hip[1] - 26), (sho[0] + 64, sho[1] - 4), s)
    return dict(hip=hip, a=a, head_off=lerp(14, 0, s), arms=[("ik", hand, (0, -1))] * 2,
                legs=[("ik", (205, 46), (1, 0))] * 2, props=[("circle", "hand0", 9, v1.PROP, (0, -10))])

def kb_swing_one_arm(p):
    """Like the two-hand swing, but only the near hand holds the bell; the free hand stays by the hip."""
    d = kb_swing(p)
    hip, sho = d["hip"], sh(d["hip"], d["a"])
    free = lerp2((hip[0] + 4, hip[1] - 22), (sho[0] + 26, sho[1] - 40), rep(p))
    d["arms"] = [d["arms"][0], ("ik", free, (0, -1))]
    return d

NEW = {
    "inverted-row-bent-knees": (inv_row(knees_bent=True), "rep"), "inverted-row": (inv_row(), "rep"),
    "inverted-row-feet-elevated": (inv_row(feet_up=True), "rep"), "archer-row": (inv_row(archer=True), "rep"),
    "arch-hold-y": (arch_hold(True), "hold"), "arch-rocks": (arch_rocks, "rep"),
    "side-plank-leg-raise": (side_plank_raise, "rep"), "copenhagen-side-plank": (copenhagen, "hold"),
    "kettlebell-single-leg-rdl": (kb_rdl, "rep"), "kettlebell-swing": (kb_swing, "rep"),
    "kettlebell-swing-one-arm": (kb_swing_one_arm, "rep"),
}
POSES = dict(v1.POSES); POSES.update(NEW)

# Poses whose v1 function repeats twice per 0..1 are played once per 2 s loop; the rest are already periodic in p.
ALTERNATING = {"dead-bug"}   # left/right alternate inside one loop: play as is
def loop_fn(vid, fn, kind):
    if kind == "hold" or vid in ALTERNATING: return fn
    a, b = v1.solve(fn(0.0)), v1.solve(fn(0.5))
    same = all(abs(a["hip"][i] - b["hip"][i]) < 1e-6 and abs(a["sho"][i] - b["sho"][i]) < 1e-6 for i in (0, 1))
    return (lambda p: fn(p * 0.5)) if same else fn

# ----------------------------------------------------------------------------------------------- muscle -> body region
# (segment(s), t0, t1, side, radius factor). Sides: ant/post relative to the torso; lat = outer side; inner; fold/extend = joint bend.
REGIONS = {
    "CHEST": [("torso", .55, .96, "ant", .8)], "UPPER_BACK": [("torso", .55, .98, "post", .8)], "LATS": [("torso", .28, .85, "lat", .75)],
    "LOWER_BACK": [("torso", 0.0, .42, "post", .8)], "ABS": [("torso", .04, .55, "ant", .8)], "OBLIQUES": [("torso", .06, .55, "lat", .72)],
    "GLUTES": [("torso", 0.0, .13, "post", 1.0), ("thigh", 0.0, .3, "back", .8)], "HIP_FLEXORS": [("torso", 0.0, .1, "ant", .9), ("thigh", 0.0, .35, "front", .75)],
    "QUADS": [("thigh", .12, .92, "front", .8)], "HAMSTRINGS": [("thigh", .12, .92, "back", .8)], "ADDUCTORS": [("thigh", .15, .85, "inner", .7)],
    "CALVES": [("shin", .08, .78, "back", .8)],
    "FRONT_DELTS": [("uarm", 0.0, .34, "ant", 1.0)], "SIDE_DELTS": [("uarm", 0.0, .34, "lat", 1.0)], "REAR_DELTS": [("uarm", 0.0, .34, "post", 1.0)],
    "TRICEPS": [("uarm", .28, .95, "extend", .85)], "BICEPS": [("uarm", .28, .95, "fold", .85)], "FOREARMS": [("farm", .05, .85, "fold", .85)],
}
ALL_MUSCLES = {"CHEST", "FRONT_DELTS", "SIDE_DELTS", "REAR_DELTS", "TRICEPS", "BICEPS", "FOREARMS", "LATS", "UPPER_BACK", "LOWER_BACK",
               "ABS", "OBLIQUES", "GLUTES", "QUADS", "HAMSTRINGS", "CALVES", "HIP_FLEXORS", "ADDUCTORS"}

# ----------------------------------------------------------------------------------------------- 3-D lifting and projection
def add(a, b): return tuple(x + y for x, y in zip(a, b))
def mul(a, k): return tuple(x * k for x in a)
def lerp3(a, b, t): return tuple(x + (y - x) * t for x, y in zip(a, b))
TO_VIEWER = (math.sin(THETA) * math.cos(PHI), math.sin(PHI), math.cos(THETA) * math.cos(PHI))
# Poses that are almost edge-on at 30 degrees (the figure is a thin line seen from the side) get a wider yaw so the depth reads.
YAW_OVERRIDE = {"wall-handstand-hold": 62, "hspu-wall-negative": 62, "hspu-wall": 62, "front-lever-tuck": 55, "front-lever-adv-tuck": 55, "front-lever-straddle": 55}
def set_view(yaw_deg):
    global THETA, TO_VIEWER
    THETA = math.radians(yaw_deg)
    TO_VIEWER = (math.sin(THETA) * math.cos(PHI), math.sin(PHI), math.cos(THETA) * math.cos(PHI))

def proj(p):
    x, y, z = p
    X = x * math.cos(THETA) - z * math.sin(THETA)
    D = x * math.sin(THETA) + z * math.cos(THETA)
    return X, y * math.cos(PHI) - D * math.sin(PHI), D

def lift(pose):
    """2-D solved pose -> dict of 3-D joints + the normals the muscle regions need."""
    j = v1.solve(pose)
    hip, sho = j["hip"], j["sho"]
    d = (sho[0] - hip[0], sho[1] - hip[1]); n = math.hypot(*d) or 1
    ant = (d[1] / n, -d[0] / n)                                   # anterior normal of the torso (clockwise perpendicular)
    out = {"hip": (hip[0], hip[1], 0), "sho": (sho[0], sho[1], 0), "ant": ant, "arms": [], "legs": []}
    arm_z = pose.get("arm_z")
    for key, zoff in (("arms", Z_ARM), ("legs", Z_LEG)):
        for i, (root, mid, end) in enumerate(j[key]):
            zs = (1 if i == 0 else -1) * zoff
            ze = arm_z[i] if (key == "arms" and arm_z) else zs
            r3, m3, e3 = (root[0], root[1], zs), (mid[0], mid[1], (zs + ze) / 2 if key == "arms" else zs), (end[0], end[1], ze if key == "arms" else zs)
            u = (mid[0] - root[0], mid[1] - root[1]); w = (end[0] - mid[0], end[1] - mid[1])
            cr = u[0] * w[1] - u[1] * w[0]
            un = math.hypot(*u) or 1
            left = (-u[1] / un, u[0] / un)
            fold = mul(left, 1 if cr > 0 else -1) if abs(cr) > 1e-3 * un else ((ant if key == "arms" else (-ant[0], -ant[1])))
            out[key].append({"pts": (r3, m3, e3), "z": zs, "fold": fold})
    out["head"] = d
    out["pose"] = pose
    out["refs"] = j["refs"]
    return out

class Canvas:
    def __init__(self, cam):
        self.cam = cam; self.img = Image.new("RGB", (W * SS, H * SS), BG); self.d = ImageDraw.Draw(self.img); self.items = []
    def P(self, p3):
        X, Y, D = proj(p3); sc, ox, oy = self.cam
        return ((X * sc + ox) * SS, (oy - Y * sc) * SS), D
    def cap(self, a, b, r, col, bias=0.0, outline=True):
        (pa, da), (pb, db) = self.P(a), self.P(b); self.items.append(((da + db) / 2 + bias, "cap", pa, pb, r * self.cam[0] * SS, col, outline))
    def ball(self, c, r, col, bias=0.0, outline=True):
        pc, dc = self.P(c); self.items.append((dc + bias, "ball", pc, r * self.cam[0] * SS, col, outline))
    def poly(self, pts3, col, bias=0.0):
        pp = [self.P(p) for p in pts3]; self.items.append((sum(q[1] for q in pp) / len(pp) + bias, "poly", [q[0] for q in pp], col))
    def flush(self):
        for it in sorted(self.items, key=lambda t: t[0]):
            kind = it[1]
            if kind == "cap":
                _, _, pa, pb, r, col, outline = it
                if outline: self._cap(pa, pb, r + 1.6 * SS, OUTLINE)
                self._cap(pa, pb, r, col)
            elif kind == "ball":
                _, _, c, r, col, outline = it
                if outline: self.d.ellipse([c[0] - r - 1.6 * SS, c[1] - r - 1.6 * SS, c[0] + r + 1.6 * SS, c[1] + r + 1.6 * SS], fill=OUTLINE)
                self.d.ellipse([c[0] - r, c[1] - r, c[0] + r, c[1] + r], fill=col)
            else:
                self.d.polygon(it[2], fill=it[3], outline=OUTLINE)
    def _cap(self, pa, pb, r, col):
        self.d.line([pa, pb], fill=col, width=max(1, int(2 * r)))
        for c in (pa, pb): self.d.ellipse([c[0] - r, c[1] - r, c[0] + r, c[1] + r], fill=col)

def mix(a, b, t): return tuple(int(x + (y - x) * t) for x, y in zip(a, b))

def segments(L):
    hip, sho = L["hip"], L["sho"]
    segs = {"torso": [(hip, sho, R_TORSO, None)], "uarm": [], "farm": [], "thigh": [], "shin": []}
    for k, arm in enumerate(L["arms"]):
        r, m, e = arm["pts"]; segs["uarm"].append((r, m, R_UARM, arm)); segs["farm"].append((m, e, R_FARM, arm))
    for k, leg in enumerate(L["legs"]):
        r, m, e = leg["pts"]; segs["thigh"].append((r, m, R_THIGH, leg)); segs["shin"].append((m, e, R_SHIN, leg))
    return segs

def side_normal(L, seg_name, side, limb, a, b):
    ant = (L["ant"][0], L["ant"][1], 0.0)
    if side == "ant": return ant
    if side == "post": return mul(ant, -1)
    z = (limb["z"] if limb else 1.0)
    if side == "lat": return (0, 0, 1.0 if z >= 0 else -1.0)
    if side == "inner": return (0, 0, -1.0 if z >= 0 else 1.0)
    f = (limb["fold"][0], limb["fold"][1], 0.0) if limb else ant
    if seg_name == "thigh":                       # the knee bends backwards: back of thigh = fold side, quads opposite
        return mul(f, -1) if side == "front" else f
    if side == "fold": return f
    if side == "extend" or side == "back": return mul(f, -1) if side == "extend" else f
    return f if side == "back" else mul(f, -1)

def draw_figure(cv, L, muscles):
    pose = L["pose"]
    segs = segments(L)
    is_far = lambda limb: limb is not None and limb["z"] < 0
    # base body
    for name in ("thigh", "shin"):
        for a, b, r, limb in segs[name]: cv.cap(a, b, r, BODY_FAR if is_far(limb) else BODY)
    for arm in L["arms"]:
        r, m, e = arm["pts"]
    for a, b, r, limb in segs["uarm"] + segs["farm"]: cv.cap(a, b, r, BODY_FAR if is_far(limb) else BODY)
    for limb in L["legs"] + L["arms"]:                             # hands and feet
        end = limb["pts"][2]; cv.ball(end, 4.6, BODY_FAR if is_far(limb) else BODY)
    hip, sho = L["hip"], L["sho"]
    bulge = pose.get("bulge", 0)
    if bulge:
        a = math.radians(pose["a"]); mid = lerp3(hip, sho, .5); mid = (mid[0] - math.sin(a) * bulge, mid[1] + math.cos(a) * bulge, 0)
        cv.cap(hip, mid, R_TORSO, BODY); cv.cap(mid, sho, R_TORSO, BODY)
    else: cv.cap(hip, sho, R_TORSO, BODY)
    ha = math.radians(pose["a"] + pose.get("head_off", 0))
    hc = (sho[0] + (v1.NECK + R_HEAD) * math.cos(ha), sho[1] + (v1.NECK + R_HEAD) * math.sin(ha), 0)
    cv.cap(sho, (sho[0] + v1.NECK * math.cos(ha), sho[1] + v1.NECK * math.sin(ha), 0), 4.2, BODY)
    cv.ball(hc, R_HEAD, BODY, bias=2)
    # muscle overlays
    for ms, col in muscles:
        for seg_name, t0, t1, side, rf in REGIONS[ms]:
            for a, b, r, limb in segs[seg_name]:
                n = side_normal(L, seg_name, side, limb, a, b)
                facing = sum(n[i] * TO_VIEWER[i] for i in range(3)) > 0.08
                p0, p1 = lerp3(a, b, t0), lerp3(a, b, t1)
                off = mul(n, r * 0.42)
                c = col if facing else mix(col, BODY_FAR if is_far(limb) else BODY, 0.28)   # hidden side: faint x-ray tint
                cv.cap(add(p0, off), add(p1, off), r * rf * 0.78, c, bias=1.5 if not is_far(limb) else 0.4, outline=False)

# ----------------------------------------------------------------------------------------------- scenery
def draw_scene(cv, L, vid_scene, props, floor_x):
    pose = L["pose"]
    for sc in vid_scene:
        if sc[0] == "rect":
            x0, y0, x1, y1 = sc[1]; zz = 34
            wall = (x1 - x0) < 20 and (y1 - y0) > 100          # a wall: always behind the figure, and no taller than the figure
            if wall: y1 = min(y1, y0 + 190); zz = 60
            c = [(x, y, z) for x in (x0, x1) for y in (y0, y1) for z in (-zz, zz)]
            faces = [[(x0, y0, -zz), (x1, y0, -zz), (x1, y1, -zz), (x0, y1, -zz)], [(x0, y0, zz), (x1, y0, zz), (x1, y1, zz), (x0, y1, zz)],
                     [(x0, y1, -zz), (x1, y1, -zz), (x1, y1, zz), (x0, y1, zz)], [(x1, y0, -zz), (x1, y0, zz), (x1, y1, zz), (x1, y1, -zz)],
                     [(x0, y0, -zz), (x0, y0, zz), (x0, y1, zz), (x0, y1, -zz)]]
            shades = [EQUIP_DARK, EQUIP, (168, 174, 184), EQUIP, EQUIP_DARK]
            for f, s in zip(faces, shades): cv.poly(f, s, bias=-1000 if wall else -4)
        elif sc[0] == "bar":
            x, y = sc[1], sc[2]
            for z in (-62, 62): cv.cap((x, GROUND, z), (x, y, z), 2.6, EQUIP, bias=-3, outline=False)
            cv.cap((x, y, -62), (x, y, 62), 3.6, EQUIP_DARK, bias=-2)
    ref = lambda r: L["refs"][r] if isinstance(r, str) else r
    for pr in props:
        if pr[0] == "line":
            a, b = ref(pr[1]), ref(pr[2]); cv.cap((a[0], a[1], Z_ARM), (b[0], b[1], Z_LEG), 1.6, (217, 119, 6), bias=1, outline=False)
        if pr[0] == "circle":
            q = ref(pr[1]); cv.ball((q[0] + pr[4][0], q[1] + pr[4][1], Z_ARM), pr[2], EQUIP_DARK, bias=3)

GROUND = v1.GROUND

def floor(cv, xs):
    x0, x1 = xs
    pts = lambda x, z: cv.P((x, GROUND, z))[0]
    n0, n1 = int(x0 // 40) * 40 - 40, int(x1 // 40) * 40 + 80
    for x in range(n0, n1 + 1, 40): cv.d.line([pts(x, -90), pts(x, 90)], fill=GRID, width=SS)
    for z in range(-80, 81, 40): cv.d.line([pts(n0, z), pts(n1, z)], fill=GRID, width=SS)

def camera_for(fn, vid, margin=(420, 250)):
    xs, ys = [], []
    for i in range(FPS * SECONDS // 2):
        L = lift(fn(i / (FPS * SECONDS // 2)))
        pts = [L["hip"], L["sho"]] + [q for l in L["arms"] + L["legs"] for q in l["pts"]]
        ha = math.radians(L["pose"]["a"] + L["pose"].get("head_off", 0)); pts.append((L["sho"][0] + 30 * math.cos(ha), L["sho"][1] + 30 * math.sin(ha), 0))
        for sc in L["pose"].get("scene", []):
            if sc[0] == "bar": pts += [(sc[1], sc[2], -62), (sc[1], sc[2], 62)]
            if sc[0] == "rect": x0, y0, x1, y1 = sc[1]; pts += [(x0, y0, -34), (x1, min(y1, y0 + 190) if (x1 - x0) < 20 and (y1 - y0) > 100 else y1, 34)]
        for pr in L["pose"].get("props", []): pts.append((L["refs"].get(pr[1], (0, 0))[0] if isinstance(pr[1], str) else 0, 0, 0)) if pr[0] == "circle" else None
        pts.append((0, GROUND, 0))
        for p in pts:
            X, Y, _ = proj(p); xs.append(X); ys.append(Y)
    x0, x1, y0, y1 = min(xs), max(xs), min(ys), max(ys)
    sc = min(2.6, margin[0] / max(x1 - x0, 1), margin[1] / max(y1 - y0, 1))
    ox = W / 2 - sc * (x0 + x1) / 2
    oy = (76 + 346) / 2 + sc * (y0 + y1) / 2          # centre the art between the caption and the bottom edge
    return sc, ox, oy, (x0, x1)

def render(vid, name, kind, fn, p, cam, muscles, font):
    cv = Canvas(cam[:3])
    L = lift(fn(p))
    floor(cv, cam[3] if False else (-40, 330))
    # soft shadow under the figure
    sxs = [q["pts"][2][0] for q in L["legs"]] + [L["hip"][0], L["sho"][0]]
    c = cv.P(((min(sxs) + max(sxs)) / 2, GROUND, 0))[0]; rx = (max(sxs) - min(sxs)) / 2 * cam[0] * SS + 24 * SS
    cv.d.ellipse([c[0] - rx, c[1] - 7 * SS, c[0] + rx, c[1] + 7 * SS], fill=(232, 235, 240))
    draw_scene(cv, L, L["pose"].get("scene", []), L["pose"].get("props", []), None)
    draw_figure(cv, L, muscles)
    cv.flush()
    cv.d.text((16 * SS, 12 * SS), name, fill=(31, 41, 55), font=font)
    cv.d.text((16 * SS, 42 * SS), "hold steady" if kind == "hold" else "slow and controlled", fill=(107, 114, 128), font=font)
    return cv.img.resize((W, H), Image.LANCZOS)

def main():
    args = sys.argv[1:]
    sheet = None
    if "--sheet" in args: i = args.index("--sheet"); sheet = args[i + 1]; del args[i:i + 2]
    check = "--check" in args
    args = [a for a in args if a != "--check"]
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    cat = json.load(open(os.path.join(root, "app/src/main/assets/catalog.json")))
    byid = {v["id"]: v for v in cat["variations"]}
    # assertions the plan asks for
    missing = [v for v in byid if v not in POSES]
    assert not missing, f"no pose for catalog ids: {missing}"
    used = {m for v in byid.values() for m in v.get("primaryMuscles", []) + v.get("secondaryMuscles", [])}
    assert used <= set(REGIONS), f"muscles without a body region: {used - set(REGIONS)}"
    assert set(REGIONS) == ALL_MUSCLES, "every Muscle needs a region"
    assert all(v.get("primaryMuscles") for v in byid.values() if v["kind"] in ("REPS", "HOLD")), "strength exercise without primary muscles"
    if check: print("checks passed:", len(byid), "poses,", len(used), "muscles used"); return
    try: font = ImageFont.load_default(size=22 * SS)
    except TypeError: font = ImageFont.load_default()
    outdir = os.path.join(root, "app/src/main/assets/demos"); os.makedirs(outdir, exist_ok=True)
    ids = args or list(byid)
    thumbs = []
    for vid in ids:
        set_view(YAW_OVERRIDE.get(vid, 30))
        fn0, kind = POSES[vid]; fn = loop_fn(vid, fn0, kind)
        v = byid[vid]
        muscles = [(m, SECONDARY) for m in v.get("secondaryMuscles", [])] + [(m, PRIMARY) for m in v.get("primaryMuscles", [])]
        cam = camera_for(fn, vid)
        n = FPS * SECONDS
        out = os.path.join(outdir, vid + ".mp4")
        v1.encode((render(vid, v["name"], kind, fn, i / n, cam, muscles, font) for i in range(n)), out)
        size = os.path.getsize(out) / 1024
        assert size < 120, f"{vid}: {size:.0f} KB is over the 120 KB budget"
        print(f"{vid:28s} {kind:5s} {size:6.0f} KB")
        if sheet: thumbs.append((vid, [render(vid, v["name"], kind, fn, q, cam, muscles, font) for q in (0.0, 0.25, 0.5)]))
    if sheet:
        tw, th = W // 2, H // 2
        im = Image.new("RGB", (tw * 3, th * len(thumbs)), BG)
        for r, (vid, row) in enumerate(thumbs):
            for c, t in enumerate(row): im.paste(t.resize((tw, th), Image.LANCZOS), (c * tw, r * th))
        im.save(sheet)

if __name__ == "__main__": main()
