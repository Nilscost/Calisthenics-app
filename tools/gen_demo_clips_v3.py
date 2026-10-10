#!/usr/bin/env python3
"""Demo clips v3 (V06a; v2 was U11, plan §5): 3/4 view, solid grey capsule body, worked muscles orange-red, equipment in 3-D.

Reuses the sagittal poses of v1 (tools/gen_demo_clips.py) and adds the ten exercises U09 introduced. Each 2-D pose is lifted to
3-D (left/right limbs get a z offset), projected orthographically (yaw 30 deg, pitch 15 deg) and drawn back to front.
Only the muscles listed in the catalog are coloured: primary saturated, secondary light. Hidden ones (e.g. the chest in a
push-up seen from above) are drawn as a faint x-ray tint so the colour code stays readable.
Output per exercise and stretch (V06a, plan §5.1 one drawing pipeline):
  app/src/main/assets/demos/<variationId>.mp4   clip (480x360, 24 fps, 2 s loop, H.264, < 120 KB)
  app/src/main/assets/thumbs/<variationId>.webp still key frame, body cropped, transparent background (< 12 KB)
  app/src/main/assets/clip_meta.json            per id: kind, pose group, primary/secondary muscles (what the body figure needs)
Pillow + imageio-ffmpeg only.
Run:  python3 tools/gen_demo_clips_v3.py [--sheet out.png] [--check] [--thumbs] [id ...]
  --check   assertions only (every id has a pose, every pose is its own unless declared in SHARED_POSES, muscles have regions)
  --thumbs  thumbnails + clip_meta.json only (no mp4)
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

# ----------------------------------------------------------------------------------------------- V07 stretch poses (3-D: depth via arm_z / arm_zm / leg_z)
def _breath(p): return 0.5 + 0.5 * math.sin(2 * math.pi * p)

def st_pigeon(p):
    b = _breath(p); hip = (150, 50); a = lerp(52, 26, b); sho = sh(hip, a)
    return dict(hip=hip, a=a, head_off=-14, arms=[("ik", (sho[0] + 46, 46), (-1, -0.2))] * 2,
                legs=[("fix", (192, 48), (150, 46)), ("ik", (66, 46), (0, 1))], leg_z=[(6.5, 6.5), (-6.5, -6.5)])

def st_90_90(p):
    b = _breath(p); hip = (140, 46); a = lerp(92, 62, b); sho = sh(hip, a)
    return dict(hip=hip, a=a, head_off=-8, arms=[("ik", (sho[0] + 30, 50), (0, 1)), ("ik", (sho[0] + 20, 50), (0, 1))],
                legs=[("fix", (184, 48), (184, 46)), ("fix", (140, 48), (100, 46))], leg_z=[(6.5, 60), (-52, -52)])

def st_frog(p):
    b = _breath(p); hip = (lerp(122, 108, b), 68); a = lerp(24, 14, b)
    return dict(hip=hip, a=a, head_off=-10, arms=[("ik", (232, 46), (0, 1))] * 2, arm_z=[Z_ARM, -Z_ARM],
                legs=[("fix", (hip[0] + 8, 46), (hip[0] - 34, 46))] * 2, leg_z=[(54, 74), (-54, -74)])

def st_couch(p):
    b = _breath(p); hip = (lerp(136, 128, b), 88); a = lerp(92, 100, b); sho = sh(hip, a)
    return dict(hip=hip, a=a, head_off=0, arms=[("ik", (sho[0] + 40, sho[1] - 38), (0, -1))] * 2,
                legs=[("ik", (194, 46), (1, 0.4)), ("fix", (118, 46), (80, 74))], scene=[("rect", (60, 44, 68, 170), v1.GRD)])

def st_quad_standing(p):
    b = _breath(p); hip = (150, 124); sho = sh(hip, 90 + 2 * b)
    kn, an = (hip[0] + 4, hip[1] - 42), (hip[0] - 24, hip[1] - 12)
    return dict(hip=hip, a=90 + 2 * b, arms=[("ik", an, (0, -1)), ("ik", (sho[0] + 38, sho[1] - 30), (0, -1))],
                legs=[("ik", (150, 46), (1, 0)), ("fix", kn, an)])

def st_ham_standing(p):
    b = _breath(p); hip = (130, 112); a = lerp(62, 40, b); sho = sh(hip, a)
    return dict(hip=hip, a=a, head_off=-10, arms=[("ik", (hip[0] + 34, 74), (-1, 0))] * 2,
                legs=[("ik", (hip[0] + 56, 46), (0, 1)), ("ik", (hip[0] - 16, 46), (1, 0))])

def st_lat_wall(p):
    b = _breath(p); hip = (100, 128); a = 3 * b - 2; sho = sh(hip, a)
    return dict(hip=hip, a=a, head_off=-14, arms=[("ik", (222, 130), (0, -1))] * 2, legs=[("ik", (100, 46), (1, 0))] * 2,
                scene=[("rect", (226, 44, 234, 200), v1.GRD)])

def st_sleeper(p):
    b = _breath(p); hip = (170, 50); a = 180; sho = sh(hip, a)
    pressing = lerp(0, 8, b)
    return dict(hip=hip, a=a, head_off=0, arms=[("fix", (sho[0] + 2, 52), (sho[0] + 2, 82 - pressing)), ("ik", (sho[0] + 2, 82 - pressing), (0, 1))],
                arm_z=[54, 54], arm_zm=[54, None], legs=[("ik", (170 + 78, 52), (0, 1)), ("ik", (170 + 72, 56), (0, 1))])

def _quad(hip_x, a=18):
    hip = (hip_x, 88); sho = sh(hip, a)
    return hip, a, sho

def st_wrist_flexor(p):
    b = _breath(p); hip, a, sho = _quad(lerp(118, 98, b)); hand = (sho[0] + 6, 44)
    return dict(hip=hip, a=a, head_off=-6, arms=[("ik", hand, (-1, 0))] * 2, legs=[("fix", (hip[0] + 2, 46), (hip[0] - 40, 46))] * 2)

def st_wrist_extensor(p):
    b = _breath(p); hip, a, sho = _quad(lerp(122, 108, b)); hand = (sho[0] + 6, 44)
    return dict(hip=hip, a=a, head_off=-6, arms=[("ik", hand, (-1, 0))] * 2, arm_z=[Z_ARM + 8, -Z_ARM - 8], legs=[("fix", (hip[0] + 2, 46), (hip[0] - 40, 46))] * 2)

def st_thoracic(p):
    s = rep(p, 1); hip, a, sho = _quad(120)
    reach = lerp2((sho[0] + 4, sho[1] + 60), (sho[0] - 14, 46), 1 - s)    # the threaded arm: up to the ceiling <-> under the body
    return dict(hip=hip, a=a, head_off=-6, arms=[("ik", (sho[0] + 6, 44), (-1, 0)), ("ik", reach, (-1, 0))],
                arm_z=[Z_ARM, lerp(-64, 30, 1 - s)], legs=[("fix", (hip[0] + 2, 46), (hip[0] - 40, 46))] * 2)

NEW.update({
    "stretch-pigeon": (st_pigeon, "hold"), "stretch-90-90": (st_90_90, "hold"), "stretch-frog": (st_frog, "hold"), "stretch-quad-couch": (st_couch, "hold"),
    "stretch-quad-standing": (st_quad_standing, "hold"), "stretch-hamstring-standing": (st_ham_standing, "hold"), "stretch-lat-wall": (st_lat_wall, "hold"),
    "stretch-sleeper": (st_sleeper, "hold"), "stretch-wrist-flexor": (st_wrist_flexor, "hold"), "stretch-wrist-extensor": (st_wrist_extensor, "hold"),
    "stretch-thoracic": (st_thoracic, "rep"),
})

# ----------------------------------------------------------------------------------------------- V21b poses (RR warm-up, dips, hinge, core, Minimalist)
def _arc_hand(sho, deg, r=62): d = math.radians(deg); return (sho[0] + r * math.cos(d), sho[1] + r * math.sin(d))

def wu_shoulder_band(p):
    hip = (150, 124); a = 90; sho = sh(hip, a); deg = lerp(-12, 188, (1 - math.cos(2 * math.pi * p)) / 2)
    hand = _arc_hand(sho, deg)
    return dict(hip=hip, a=a, arms=[("ik", hand, (0, -1))] * 2, arm_z=[Z_ARM + 20, -Z_ARM - 20], legs=[("ik", (150, 46), (1, 0))] * 2,
                props=[("line", "hand0", "hand1", v1.PROP)])

def wu_shoulder_towel(p):
    hip = (150, 124); a = 90; sho = sh(hip, a); deg = lerp(-6, 176, (1 - math.cos(2 * math.pi * p)) / 2)
    hand = _arc_hand(sho, deg, 56)
    return dict(hip=hip, a=a, arms=[("ik", hand, (0, -1))] * 2, arm_z=[Z_ARM + 26, -Z_ARM - 26], legs=[("ik", (150, 46), (1, 0))] * 2,
                props=[("line", "hand0", "hand1", (160, 140, 110))])

def wu_sky_reach(p):
    s = rep(p); hip = (lerp(150, 118, s), lerp(124, 82, s)); a = lerp(90, 66, s); sho = sh(hip, a)
    reach = lerp(1.0, 0.0, s)   # hands overhead standing, in front of the chest in the squat, so each rep is "down, then reach for the sky"
    hand = (lerp(sho[0] + 40, sho[0] + 4, reach), lerp(sho[1] - 4, sho[1] + 62, reach))
    return dict(hip=hip, a=a, head_off=lerp(0, 12, s), arms=[("ik", hand, (-1, 0))] * 2, legs=[("ik", (lerp(150, 168, s), 46), (1, 0))] * 2)

def wu_wrist_prep(p):
    b = (1 - math.cos(2 * math.pi * p)) / 2; hip, a, sho = _quad(lerp(92, 128, b))
    return dict(hip=hip, a=a, head_off=-6, arms=[("ik", (sho[0] + lerp(-2, 16, b), 44), (-1, 0))] * 2, arm_z=[Z_ARM + 10, -Z_ARM - 10],
                legs=[("fix", (hip[0] + 2, 46), (hip[0] - 40, 46))] * 2)

def wu_arch_hang(p):
    b = (1 - math.cos(2 * math.pi * p)) / 2; sy = lerp(214, 224, b); sx = 192; hip = (sx - 4, sy - 58); sho = (sx, sy)
    return dict(hip=hip, a=94, head_off=-8, arms=[("ik", (200, BAR_Y), (-1, -0.2))] * 2, legs=[("ik", (hip[0] - 34, hip[1] - 72), (1, 0))] * 2, scene=[("bar", 200, BAR_Y)])

def _dip_pose(sy, lean, legs_back=True):
    hand = (200, 112); sx = 196 + lean
    hip = (sx - 4, sy - 58)
    return dict(hip=hip, a=90, arms=[("ik", hand, (-1, -0.4))] * 2, arm_z=[22, -22],
                legs=[("ik", (hip[0] - 26, hip[1] - 66), (1, 0)), ("ik", (hip[0] - 34, hip[1] - 62), (1, 0))], scene=[("pbars", 150, 250, 112)])

def dip_support(p):
    return _dip_pose(172 + 1.5 * math.sin(2 * math.pi * p), 0)

def dip_negative(p):
    s = min(1.0, p / 0.8) if p < 0.8 else 1 - (p - 0.8) / 0.2     # slow descent over 80 % of the loop, a quick push back up with the feet
    return _dip_pose(lerp(172, 140, s), lerp(0, 6, s))

def dip_full(p):
    s = rep(p, 1)
    return _dip_pose(lerp(172, 138, s), lerp(0, 8, s))

def rdl_bodyweight(p):
    s = rep(p); hip = (lerp(150, 120, s), lerp(124, 112, s)); a = lerp(90, 22, s); sho = sh(hip, a)
    return dict(hip=hip, a=a, head_off=lerp(0, 24, s), arms=[("ik", (sho[0] + 6, sho[1] - 60), (-1, 0))] * 2,
                legs=[("ik", (150, 46), (1, 0.1))] * 2)

def single_leg_deadlift(p):
    s = rep(p); hip = (lerp(190, 176, s), lerp(124, 118, s)); a = lerp(90, 12, s); sho = sh(hip, a)
    r = math.radians(a); free = (hip[0] - 84 * math.cos(r) * 0.95, hip[1] - 84 * math.sin(r) * 0.95)
    if a > 60: free = (hip[0] - 4, hip[1] - 84)
    return dict(hip=hip, a=a, head_off=lerp(0, 24, s), arms=[("ik", (sho[0] + 4, sho[1] - 60), (-1, 0))] * 2,
                legs=[("ik", (190, 46), (1, 0)), ("ik", free, (0, 1))])

NORDIC_OFF = 18
def _nordic(phi_fn, hands_ahead=True):
    def f(p):
        phi = math.radians(phi_fn(p)); knee = (118, 46)
        hip = (knee[0] + 42 * math.sin(phi), knee[1] + 42 * math.cos(phi)); sho = (hip[0] + 60 * math.sin(phi), hip[1] + 60 * math.cos(phi))
        a = math.degrees(math.atan2(sho[1] - hip[1], sho[0] - hip[0])) + NORDIC_OFF * max(0.0, 1 - math.degrees(phi) / 45)
        return dict(hip=hip, a=a, head_off=-4, arms=[("ik", (sho[0] + 30 + 30 * math.sin(phi), max(46, sho[1] - 52)), (0, -1))] * 2,
                    legs=[("fix", knee, (74, 46))] * 2, scene=[("rect", (52, 44, 70, 62), v1.GRD)])
    return f

def nordic_negative_banded(p):
    s = min(1.0, p / 0.75) if p < 0.75 else 1 - (p - 0.75) / 0.25     # lower slowly, come back up fast with the band's help
    return _nordic(lambda q: lerp(8, 62, s))(p)

def nordic_banded(p):
    return _nordic(lambda q: lerp(8, 52, (1 - math.cos(2 * math.pi * p)) / 2))(p)

def nordic_full(p):
    return _nordic(lambda q: lerp(6, 74, (1 - math.cos(2 * math.pi * p)) / 2))(p)

def _slide(ank_d, single=False):
    """Hamstring slide: shoulders on the floor, hips in a bridge, heels on a towel sliding away from the hips (ank_d = heel distance behind the hip line)."""
    def f(p):
        S = (62, 50); beta = math.radians(30); hip = (S[0] + L_T * math.cos(beta), S[1] + L_T * math.sin(beta))
        d = ank_d(p)
        legs = [("ik", (hip[0] + d, 46), (0, 1)), ("ik", (hip[0] + d, 46), (0, 1))]
        if single: legs[1] = ("ik", (hip[0] + 70, hip[1] + 6), (0, 1))     # the other leg is held up, straight
        return dict(hip=hip, a=180 + math.degrees(beta), head_off=0, arms=[("ik", (S[0] + 54, 46), (0, 1))] * 2, legs=legs)
    return f

def _down_up(p, a, b):   # slow way out over 80 % of the loop, quick way back
    s = p / 0.8 if p < 0.8 else 1 - (p - 0.8) / 0.2
    return lerp(a, b, s)

L_T = v1.L_T
slide_negative = _slide(lambda p: _down_up(p, 42, 76))
slide_both = _slide(lambda p: lerp(42, 76, (1 - math.cos(2 * math.pi * p)) / 2))
slide_negative_single = _slide(lambda p: _down_up(p, 42, 76), single=True)
slide_single = _slide(lambda p: lerp(42, 76, (1 - math.cos(2 * math.pi * p)) / 2), single=True)

def pallof_press(p):
    s = (1 - math.cos(2 * math.pi * p)) / 2; hip = (150, 124); sho = sh(hip, 90)
    hand = (lerp(sho[0] + 16, sho[0] + 62, s), sho[1] - 14)
    return dict(hip=hip, a=90, arms=[("ik", hand, (0, -1))] * 2, legs=[("ik", (160, 46), (1, 0)), ("ik", (134, 46), (1, 0))],
                props=[("line", "hand0", (30, sho[1] - 14), v1.PROP)])

def reverse_hyper(p):
    s = (1 - math.cos(2 * math.pi * p)) / 2; hip = (176, 86)
    return dict(hip=hip, a=180, head_off=0, arms=[("ik", (90, 86), (0, 1))] * 2, legs=[("ik", (hip[0] + lerp(8, 82, s), hip[1] + lerp(-80, 2, s)), (0, 1))] * 2,
                scene=[("rect", (60, 44, 178, 84), v1.GRD)])

def plank_shoulder_tap(p):
    d = _std_pushup(104, 100)(0.0)
    sho = sh(d["hip"], d["a"]); first = p < 0.5; q = (p % 0.5) / 0.5; lift = math.sin(math.pi * q)
    tap = (sho[0] - 8 + 30 * (1 - lift), sho[1] + 6 - 0 * lift + 16 * (1 - lift))
    hand = d["arms"][0]
    arm_up = ("ik", (sho[0] - 2, sho[1] - 4 + (1 - lift) * 40 - 20 * 0), (-1, 0.3)) if lift > 0.05 else hand
    arms = [arm_up, hand] if first else [hand, arm_up]
    d["arms"] = arms; d["hip"] = (d["hip"][0], d["hip"][1] + 0); return d

def walking_lunge(p):
    step = (p * 2) % 1.0; s = math.sin(math.pi * step)
    hx = lerp(112, 176, p); hip = (hx, lerp(124, 78, s)); front = (hx + 62, 46); back = (hx - 54 - 10 * (1 - s), 46 + 6 * s)
    legs = [("ik", front, (1, 0.2)), ("ik", back, (1, 0.5))] if step < 2 and (p < 0.5) else [("ik", (hx + 62, 46), (1, 0.2)), ("ik", back, (1, 0.5))]
    return dict(hip=hip, a=90, arms=[("ik", (hip[0] + 6, hip[1] - 8), (0, -1))] * 2, legs=legs)

NEW.update({
    "warmup-shoulder-band": (wu_shoulder_band, "rep"), "warmup-shoulder-towel": (wu_shoulder_towel, "rep"), "warmup-squat-sky-reach": (wu_sky_reach, "rep"),
    "warmup-wrist-prep": (wu_wrist_prep, "rep"), "warmup-arch-hang": (wu_arch_hang, "rep"),
    "dip-support-hold": (dip_support, "hold"), "dip-negative": (dip_negative, "rep"), "dip-parallel": (dip_full, "rep"),
    "rdl-bodyweight": (rdl_bodyweight, "rep"), "single-leg-deadlift": (single_leg_deadlift, "rep"),
    "nordic-negative-banded": (nordic_negative_banded, "rep"), "nordic-banded": (nordic_banded, "rep"), "nordic-curl": (nordic_full, "rep"),
    "slide-negative": (slide_negative, "rep"), "slide-hamstring": (slide_both, "rep"), "slide-negative-single": (slide_negative_single, "rep"), "slide-single-leg": (slide_single, "rep"),
    "pallof-press": (pallof_press, "rep"), "reverse-hyperextension": (reverse_hyper, "rep"), "plank-shoulder-tap": (plank_shoulder_tap, "rep"), "walking-lunge": (walking_lunge, "rep"),
})
# the warm-up versions of exercises that already exist are the same movement for 30 s: one pose, declared in SHARED_POSES below
NEW["warmup-dead-bug"] = POSES_DEAD = (v1.POSES["dead-bug"][0], "rep")
NEW["warmup-support-hold"] = NEW["dip-support-hold"]

# ----------------------------------------------------------------------------------------------- V25 poses (C-E)
def _rep_phase(s_target):   # the phase q (0..0.25) for which rep(q) with its default 2 reps per loop equals s_target (0..1)
    return math.acos(max(-1.0, min(1.0, 1 - 2 * s_target))) / (4 * math.pi)

def scapular_pull(p):
    """Hang with straight arms; the shoulders rise a little while the elbows stay straight, and hold at the top."""
    s = min(1.0, rep(p, 1) * 1.8); sx = 192; sy = lerp(216, 230, s); a = 90
    hip = (sx - 2, sy - L_T)
    return dict(hip=hip, a=a, head_off=-4 * s, arms=[("ik", (200, BAR_Y), (-1, -0.2))] * 2,
                legs=[("ik", (hip[0] - 22, hip[1] - 66), (1, 0)), ("ik", (hip[0] - 30, hip[1] - 60), (1, 0))], scene=[("bar", 200, BAR_Y)])

def arch_hang_hold(p):
    b = 0.5 + 0.5 * math.sin(2 * math.pi * p); sy = 224 + 2 * b; sx = 192; hip = (sx - 8, sy - 58)
    return dict(hip=hip, a=96, head_off=-8, arms=[("ik", (200, BAR_Y), (-1, -0.2))] * 2,
                legs=[("ik", (hip[0] - 34, hip[1] - 72), (1, 0))] * 2, scene=[("bar", 200, BAR_Y)])

def pullup_negative(p):
    """Chin over the bar, then lowered very slowly (80 % of the loop); the quick part is stepping back up."""
    s = 1 - p / 0.8 if p < 0.8 else (p - 0.8) / 0.2
    return pull_hang(218, 274, 6)(_rep_phase(max(0.0, min(1.0, s))))

def l_sit_tuck(p):
    b = 1.2 * math.sin(2 * math.pi * p); hip = (160, 70 + b)
    return dict(hip=hip, a=106, arms=[("fix", (152, 114), (152, 44))] * 2, legs=[("ik", (hip[0] + 34, hip[1] - 12), (0, 1))] * 2)

def bridge_incline(p):
    s = rep(p); sho = (78, 136); a = lerp(152, 137, s); r = math.radians(a)
    hip = (sho[0] - L_T * math.cos(r), sho[1] - L_T * math.sin(r))
    return dict(hip=hip, a=a, head_off=-6, arms=[("ik", (80, 78), (0, 1))] * 2, legs=[("ik", (192, 46), (0, 1))] * 2,
                scene=[("rect", (48, 46, 112, 78), v1.GRD)])

NEW.update({"scapular-pull": (scapular_pull, "rep"), "arch-hang": (arch_hang_hold, "hold"), "pullup-negative": (pullup_negative, "rep"),
            "l-sit-tuck": (l_sit_tuck, "hold"), "bridge-incline": (bridge_incline, "rep")})

# ----------------------------------------------------------------------------------------------- V27 poses (dumbbell, barbell, vest)
def goblet_squat(p):
    s = rep(p); hip = (lerp(210, 172, s), lerp(124, 76, s)); a = lerp(90, 62, s); sho = sh(hip, a)
    hand = (sho[0] + 18, sho[1] - 12 + 4 * s)
    return dict(hip=hip, a=a, head_off=lerp(0, 18, s), arms=[("ik", hand, (0, -1))] * 2, arm_z=[6, -6], legs=[("ik", (210, 46), (1, 0))] * 2,
                props=[("line", (hand[0] - 2, hand[1] - 7), (hand[0] - 2, hand[1] + 7), v1.PROP)])

def db_rdl(p):
    s = rep(p); hip = (lerp(150, 120, s), lerp(124, 112, s)); a = lerp(90, 24, s); sho = sh(hip, a)
    hand = (sho[0] + 4, sho[1] - 56 + 6 * s)
    return dict(hip=hip, a=a, head_off=lerp(0, 22, s), arms=[("ik", hand, (-1, 0))] * 2, legs=[("ik", (150, 46), (1, 0.1))] * 2,
                props=[("line", (hand[0] - 7, hand[1]), (hand[0] + 7, hand[1]), v1.PROP)])

def db_single_leg_rdl(p):
    s = rep(p); hip = (lerp(190, 178, s), lerp(124, 118, s)); a = lerp(90, 14, s); sho = sh(hip, a)
    r = math.radians(a); free = (hip[0] - 84 * math.cos(r) * 0.95, hip[1] - 84 * math.sin(r) * 0.95)
    if a > 60: free = (hip[0] - 4, hip[1] - 84)
    hand = (sho[0] + 4, sho[1] - 56 + 6 * s)
    return dict(hip=hip, a=a, head_off=lerp(0, 22, s), arms=[("ik", hand, (-1, 0))] * 2, legs=[("ik", (190, 46), (1, 0)), ("ik", free, (0, 1))],
                props=[("line", (hand[0] - 7, hand[1]), (hand[0] + 7, hand[1]), v1.PROP)])

def db_row(p):
    s = rep(p); hip = (128, 98); a = 34; sho = sh(hip, a)
    work = (sho[0] + lerp(2, -16, s), sho[1] + lerp(-58, -22, s)); rest_hand = (sho[0] + 26, 78)
    return dict(hip=hip, a=a, head_off=-14, arms=[("ik", work, (-1, 0)), ("ik", rest_hand, (0, -1))], arm_z=[Z_ARM, -Z_ARM],
                legs=[("ik", (150, 46), (1, 0)), ("ik", (84, 46), (1, 0))], scene=[("rect", (sho[0] + 12, 46, sho[0] + 44, 78), v1.GRD)],
                props=[("line", (work[0] - 6, work[1]), (work[0] + 6, work[1]), v1.PROP)])

def bb_squat(p):
    s = rep(p); hip = (lerp(210, 172, s), lerp(124, 76, s)); a = lerp(90, 52, s); sho = sh(hip, a)
    hand = (sho[0] - 4, sho[1] + 4)
    return dict(hip=hip, a=a, head_off=lerp(0, 20, s), arms=[("ik", hand, (-1, 0))] * 2, arm_z=[Z_ARM + 14, -Z_ARM - 14], legs=[("ik", (210, 46), (1, 0))] * 2,
                props=[("line", "hand0", "hand1", v1.PROP)])

def bb_rdl(p):
    s = rep(p); hip = (lerp(150, 120, s), lerp(124, 112, s)); a = lerp(90, 22, s); sho = sh(hip, a)
    hand = (sho[0] + 6, sho[1] - 56 + 8 * s)
    return dict(hip=hip, a=a, head_off=lerp(0, 22, s), arms=[("ik", hand, (-1, 0))] * 2, arm_z=[Z_ARM + 10, -Z_ARM - 10], legs=[("ik", (150, 46), (1, 0.1))] * 2,
                props=[("line", "hand0", "hand1", v1.PROP)])

def weighted_bridge(p):
    d = v1.POSES["glute-bridge"][0](p); hip = d["hip"]
    d["props"] = [("line", (hip[0] - 7, hip[1] + 8), (hip[0] + 7, hip[1] + 8), v1.PROP)]; return d

NEW.update({"goblet-squat": (goblet_squat, "rep"), "dumbbell-rdl": (db_rdl, "rep"), "dumbbell-single-leg-rdl": (db_single_leg_rdl, "rep"), "dumbbell-row": (db_row, "rep"),
            "barbell-squat": (bb_squat, "rep"), "barbell-rdl": (bb_rdl, "rep"), "weighted-glute-bridge": (weighted_bridge, "rep")})

# ----------------------------------------------------------------------------------------------- L02 poses (push-up chain)
def pushup_wall(p):
    return v1.plank_like((110, 46), 144, (190, 176), 189, 177, incline_bench=None)(p) | {"scene": [("rect", (196, 40, 206, 262), v1.GRD)]}

def pushup_incline_high(p):
    return v1.plank_like((95, 46), 144, (182, 134), 186, 156, incline_bench=(168, 40, 232, 130))(p)

NEW.update({"pushup-wall": (pushup_wall, "rep"), "pushup-incline-high": (pushup_incline_high, "rep")})
POSES = dict(v1.POSES); POSES.update(NEW)
POSES["warmup-dead-bug"] = (v1.POSES["dead-bug"][0], "rep")
# ----------------------------------------------------------------------------------------------- V06b: own poses (K2, K5)
BAR_Y = 280

def _std_pushup(sy_up=104, sy_down=64):
    return v1.plank_like((95, 46), 144, (232, 44), sy_up, sy_down)

def pushup_diamond(p):
    """Standard push-up path, hands together under the chest (a diamond): the hands meet at the body's midline."""
    d = _std_pushup()(p); d["arm_z"] = [3, -3]; return d

def pushup_feet_elevated(p):
    """Feet on a box: the body slopes slightly head-down at the bottom; same hand place as the standard push-up."""
    d = v1.plank_like((95, 84), 144, (232, 44), 104, 62)(p)
    d["scene"] = [("rect", (40, 44, 118, 84), v1.GRD)]
    return d

def pushup_archer(p):
    """Wide hands: one arm bends, the other stays straight out to the side (the hand slides, seen as a long straight arm)."""
    d = _std_pushup()(p)
    sho = sh(d["hip"], d["a"])
    d["arms"] = [d["arms"][0], ("ik", (sho[0] + 3, sho[1] - 70), (-1, -0.3))]
    d["arm_z"] = [Z_ARM + 3, -66]
    return d

def pull_hang(top, bottom, lean=0, kind="pullup"):
    """Hang from the bar and pull. top/bottom = shoulder height at the lowest / highest point (bar at BAR_Y, arm length 62).
    lean = torso lean back (degrees) at the top, so the chest travels to the bar."""
    def f(p):
        s = rep(p); sy = lerp(top, bottom, s); sx = 192
        a = 90 + lean * s
        hip = (sx - L_T * math.cos(math.radians(a)), sy - L_T * math.sin(math.radians(a)))
        hip = (sx + (hip[0] - sx), hip[1])
        legs = [("ik", (hip[0] - 22 + 8 * s, hip[1] - 66), (1, 0)), ("ik", (hip[0] - 30 + 8 * s, hip[1] - 60), (1, 0))]   # knees bent, feet crossed behind
        pose = dict(hip=hip, a=a, head_off=-6 * s, arms=[("ik", (200, BAR_Y), (-1, -0.2))] * 2, legs=legs, scene=[("bar", 200, BAR_Y)])
        if kind == "band": pose["props"] = [("line", (232, BAR_Y), "ankle0", v1.PROP)]
        return pose
    return f

def muscle_up(p):
    """Pull explosively (leaning back), rotate over the bar, press out to straight arms above it."""
    s = rep(p)
    if s < 0.5:
        u = s / 0.5; sy = lerp(218, 290, u); a = 90 + 22 * u
    else:
        u = (s - 0.5) / 0.5; sy = lerp(290, 346, u); a = lerp(112, 82, u)
    sx = 192
    hip = (sx - L_T * math.cos(math.radians(a)), sy - L_T * math.sin(math.radians(a)))
    legs = [("ik", (hip[0] - 14, hip[1] - 70), (1, 0)), ("ik", (hip[0] - 22, hip[1] - 66), (1, 0))]
    return dict(hip=hip, a=a, head_off=0, arms=[("ik", (200, BAR_Y), (-1, -0.2))] * 2, legs=legs, scene=[("bar", 200, BAR_Y)])

OWN_POSES = {
    "pushup-diamond": (pushup_diamond, "rep"), "pushup-feet-elevated": (pushup_feet_elevated, "rep"), "pushup-archer": (pushup_archer, "rep"),
    "pullup-band-assisted": (pull_hang(218, 266, 4, "band"), "rep"), "pullup-full": (pull_hang(218, 274, 6), "rep"),
    "pullup-chest-to-bar": (pull_hang(218, 288, 22), "rep"), "muscle-up-bar": (muscle_up, "rep"),
}
POSES.update(OWN_POSES)

# V27: the weighted-vest versions are the same movement (the vest is not drawn); declared in SHARED_POSES
for _w, _b in (("weighted-pushup", "pushup-standard"), ("weighted-pullup", "pullup-full"), ("weighted-dip", "dip-parallel"), ("weighted-squat", "squat-air")):
    POSES[_w] = POSES[_b]
# V06a: a pose function may only be shared by several ids when the group is declared here with the difference that the
# clip itself shows. V06b gives every variant of the first group its own pose (K2) and removes it from this table.
SHARED_POSES = {
    frozenset({"dead-bug", "warmup-dead-bug"}): "the warm-up item is the same exercise for 30 s",
    frozenset({"dip-support-hold", "warmup-support-hold"}): "the RR warm-up support hold is the same hold",
    frozenset({"pushup-standard", "weighted-pushup"}): "the vest is the only difference and is not drawn",
    frozenset({"pullup-full", "weighted-pullup"}): "the vest is the only difference and is not drawn",
    frozenset({"dip-parallel", "weighted-dip"}): "the vest is the only difference and is not drawn",
    frozenset({"squat-air", "weighted-squat"}): "the vest is the only difference and is not drawn",
    frozenset({"pushup-one-arm-negative", "pushup-one-arm"}): "same one-arm push-up pose; the negative is the slow lowering half, only the cue text differs",
}
def pose_groups():
    g = {}
    for k, (fn, _kind) in POSES.items(): g.setdefault(id(fn), set()).add(k)
    return [frozenset(v) for v in g.values() if len(v) > 1]

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
YAW_OVERRIDE = {"pushup-diamond": 62, "pushup-archer": 48, "wall-handstand-hold": 62, "hspu-wall-negative": 62, "hspu-wall": 62, "front-lever-tuck": 55, "front-lever-adv-tuck": 55, "front-lever-straddle": 55}
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
    arm_z, arm_zm, leg_z = pose.get("arm_z"), pose.get("arm_zm"), pose.get("leg_z")   # V07: elbow depth and knee/ankle depth can be set
    for key, zoff in (("arms", Z_ARM), ("legs", Z_LEG)):
        for i, (root, mid, end) in enumerate(j[key]):
            zs = (1 if i == 0 else -1) * zoff
            if key == "arms":
                ze = arm_z[i] if arm_z else zs
                zm = arm_zm[i] if (arm_zm and arm_zm[i] is not None) else (zs + ze) / 2
            else:
                zm, ze = leg_z[i] if leg_z else (zs, zs)
            r3, m3, e3 = (root[0], root[1], zs), (mid[0], mid[1], zm), (end[0], end[1], ze)
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
    def __init__(self, cam, transparent=False):
        self.cam = cam; self.items = []
        self.img = Image.new("RGBA", (W * SS, H * SS), (0, 0, 0, 0)) if transparent else Image.new("RGB", (W * SS, H * SS), BG)
        self.d = ImageDraw.Draw(self.img)
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
        elif sc[0] == "pbars":   # V21b: parallel bars (dip bars) running front to back at height y between x0 and x1
            x0, x1, y = sc[1], sc[2], sc[3]
            for z in (-24, 24):
                for x in (x0 + 6, x1 - 6): cv.cap((x, GROUND, z), (x, y, z), 2.4, EQUIP, bias=-3, outline=False)
                cv.cap((x0, y, z), (x1, y, z), 3.4, EQUIP_DARK, bias=-2)
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
            if sc[0] == "pbars": pts += [(sc[1], sc[3], -24), (sc[2], sc[3], 24)]
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

def render(vid, name, kind, fn, p, cam, muscles, font, caption=True):
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
    if caption:   # V10: the workout screen has its own header, so the clips themselves carry no caption (pure white background)
        cv.d.text((16 * SS, 12 * SS), name, fill=(31, 41, 55), font=font)
        cv.d.text((16 * SS, 42 * SS), "hold steady" if kind == "hold" else "slow and controlled", fill=(107, 114, 128), font=font)
    return cv.img.resize((W, H), Image.LANCZOS)

THUMB_PX, THUMB_KB = 128, 12

def render_thumb(fn, cam, muscles, p=0.25):
    """Key frame on a transparent background, cropped to the figure (and its equipment), fitted into a square."""
    cv = Canvas(cam[:3], transparent=True)
    L = lift(fn(p))
    draw_scene(cv, L, L["pose"].get("scene", []), L["pose"].get("props", []), None)
    draw_figure(cv, L, muscles)
    cv.flush()
    img = cv.img.resize((W, H), Image.LANCZOS)
    box = img.getchannel("A").getbbox()
    img = img.crop(box)
    pad = max(2, int(max(img.size) * 0.06))
    side = max(img.size) + 2 * pad
    sq = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    sq.paste(img, ((side - img.width) // 2, (side - img.height) // 2))
    return sq.resize((THUMB_PX, THUMB_PX), Image.LANCZOS)

def main():
    args = sys.argv[1:]
    sheet = None
    if "--sheet" in args: i = args.index("--sheet"); sheet = args[i + 1]; del args[i:i + 2]
    if "--frames" in args:   # review aid: a contact sheet of 6 frames per id, one row per id:  --frames out.png id ...
        i = args.index("--frames"); out_png = args[i + 1]; ids = [x for x in args[i + 2:] if not x.startswith("--")]
        root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        byid = {v["id"]: v for v in json.load(open(os.path.join(root, "app/src/main/assets/catalog.json")))["variations"]}
        try: font = ImageFont.load_default(size=22 * SS)
        except TypeError: font = ImageFont.load_default()
        rows = []
        for vid in ids:
            set_view(YAW_OVERRIDE.get(vid, 30)); fn0, kind = POSES[vid]; fn = loop_fn(vid, fn0, kind); v = byid[vid]
            muscles = [(m, SECONDARY) for m in v.get("secondaryMuscles", [])] + [(m, PRIMARY) for m in v.get("primaryMuscles", [])]
            cam = camera_for(fn, vid)
            rows.append([render(vid, v["name"], kind, fn, q / 6, cam, muscles, font).resize((W // 2, H // 2), Image.LANCZOS) for q in range(6)])
        sheet_im = Image.new("RGB", (W // 2 * 6, H // 2 * len(rows)), BG)
        for r, row in enumerate(rows):
            for c, t in enumerate(row): sheet_im.paste(t, (c * W // 2, r * H // 2))
        sheet_im.save(out_png); return
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
    undeclared = [sorted(g) for g in pose_groups() if g not in SHARED_POSES]
    assert not undeclared, f"ids share one pose without a declared difference (SHARED_POSES): {undeclared}"
    stale = [sorted(g) for g in SHARED_POSES if g not in pose_groups()]
    assert not stale, f"SHARED_POSES lists groups that no longer share a pose: {stale}"
    if check: print("checks passed:", len(byid), "poses,", len(used), "muscles used"); return
    try: font = ImageFont.load_default(size=22 * SS)
    except TypeError: font = ImageFont.load_default()
    outdir = os.path.join(root, "app/src/main/assets/demos"); os.makedirs(outdir, exist_ok=True)
    thumbdir = os.path.join(root, "app/src/main/assets/thumbs"); os.makedirs(thumbdir, exist_ok=True)
    thumbs_only = "--thumbs" in args
    args = [a for a in args if a != "--thumbs"]
    ids = args or list(byid)
    groups = {i: sorted(g) for g in pose_groups() for i in g}
    meta = {}
    thumbs = []
    for vid in ids:
        set_view(YAW_OVERRIDE.get(vid, 30))
        fn0, kind = POSES[vid]; fn = loop_fn(vid, fn0, kind)
        v = byid[vid]
        muscles = [(m, SECONDARY) for m in v.get("secondaryMuscles", [])] + [(m, PRIMARY) for m in v.get("primaryMuscles", [])]
        cam = camera_for(fn, vid)
        n = FPS * SECONDS
        out = os.path.join(outdir, vid + ".mp4")
        if not thumbs_only:
            v1.encode((render(vid, v["name"], kind, fn, i / n, cam, muscles, font, caption=False) for i in range(n)), out)
        size = os.path.getsize(out) / 1024
        assert size < 120, f"{vid}: {size:.0f} KB is over the 120 KB budget"
        tp = os.path.join(thumbdir, vid + ".webp")
        render_thumb(fn, cam, muscles).save(tp, "WEBP", quality=82, method=6)
        tsize = os.path.getsize(tp) / 1024
        assert tsize < THUMB_KB, f"{vid}: thumbnail {tsize:.1f} KB is over the {THUMB_KB} KB budget"
        meta[vid] = {"kind": kind, "primary": v.get("primaryMuscles", []), "secondary": v.get("secondaryMuscles", []), "sharedPoseWith": [g for g in groups.get(vid, []) if g != vid]}
        print(f"{vid:28s} {kind:5s} {size:6.0f} KB  thumb {tsize:4.1f} KB")
        if sheet: thumbs.append((vid, [render(vid, v["name"], kind, fn, q, cam, muscles, font) for q in (0.0, 0.25, 0.5)]))
    if not args or len(ids) == len(byid):
        with open(os.path.join(root, "app/src/main/assets/clip_meta.json"), "w") as fh: json.dump(dict(sorted(meta.items())), fh, indent=1, sort_keys=True); fh.write("\n")
    if sheet:
        tw, th = W // 2, H // 2
        im = Image.new("RGB", (tw * 3, th * len(thumbs)), BG)
        for r, (vid, row) in enumerate(thumbs):
            for c, t in enumerate(row): im.paste(t.resize((tw, th), Image.LANCZOS), (c * tw, r * th))
        im.save(sheet)

if __name__ == "__main__": main()
