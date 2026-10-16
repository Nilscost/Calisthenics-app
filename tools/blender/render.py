"""Render the bake-off clips in Blender (run on the Mac; docs/16-video-clips-plan.md).

    /Applications/Blender.app/Contents/MacOS/Blender -b -P tools/blender/render.py -- [ids] [--engine WORKBENCH|EEVEE|CYCLES] [--start-only]

Reads work/clips-bakeoff/<id>/joints.json (made by tools/blender/poses.py) and
writes PNG frames to work/clips-bakeoff/<id>/blender/frames/, plus start.png
(= frame 0, also the first/last frame for the AI models). Default engine:
Cycles with a transparent background and a shadow-catcher floor. Encoding to mp4
happens in the sandbox (tools/blender/encode.py).

The mannequin is built here from simple shapes (capsules and ellipsoids):
our own geometry, no third-party model or animation, so the output is ours.
"""
import json
import math
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from camera_views import camera_stamp

import bpy
import bmesh
from mathutils import Matrix, Quaternion, Vector

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
WORK = os.path.join(ROOT, "work", "clips-bakeoff")
RES = (640, 480)  # the app shows 480x360; 640x480 keeps edges clean and halves render time

FIGURE = (0.60, 0.61, 0.64)
JOINT = (0.50, 0.51, 0.55)
PROP = (0.30, 0.31, 0.34)
FLOOR = (0.97, 0.97, 0.97)


# ---------------------------------------------------------------- helpers
def material(name, rgb):
    m = bpy.data.materials.new(name)
    m.diffuse_color = (*rgb, 1.0)
    try:  # use_nodes is deprecated (always on) in Blender 5
        m.use_nodes = True
    except Exception:
        pass
    bsdf = m.node_tree.nodes.get("Principled BSDF") if m.node_tree else None
    if bsdf:
        bsdf.inputs["Base Color"].default_value = (*rgb, 1.0)
        bsdf.inputs["Roughness"].default_value = 0.6
    return m


def link(obj):
    bpy.context.scene.collection.objects.link(obj)
    return obj


def capsule(name, length, r1, r2, mat, seg=28, rings=7):
    """Tapered capsule from (0,0,0) to (0,0,length)."""
    bm = bmesh.new()
    loops = []
    # bottom hemisphere (r1 at z=0), then top hemisphere (r2 at z=length)
    for i in range(rings, -1, -1):
        a = (math.pi / 2) * i / rings
        loops.append((-r1 * math.sin(a), r1 * math.cos(a)))
    for i in range(0, rings + 1):
        a = (math.pi / 2) * i / rings
        loops.append((length + r2 * math.sin(a), r2 * math.cos(a)))
    ring_verts = []
    for z, r in loops:
        if r < 1e-6:
            ring_verts.append([bm.verts.new((0, 0, z))])
            continue
        ring_verts.append([bm.verts.new((r * math.cos(2 * math.pi * k / seg),
                                         r * math.sin(2 * math.pi * k / seg), z)) for k in range(seg)])
    for a, b in zip(ring_verts, ring_verts[1:]):
        if len(a) == 1:
            for k in range(seg):
                bm.faces.new((a[0], b[(k + 1) % seg], b[k]))
        elif len(b) == 1:
            for k in range(seg):
                bm.faces.new((a[k], a[(k + 1) % seg], b[0]))
        else:
            for k in range(seg):
                bm.faces.new((a[k], a[(k + 1) % seg], b[(k + 1) % seg], b[k]))
    me = bpy.data.meshes.new(name)
    bm.to_mesh(me)
    bm.free()
    for p in me.polygons:
        p.use_smooth = True
    ob = link(bpy.data.objects.new(name, me))
    ob.data.materials.append(mat)
    ob.rotation_mode = "QUATERNION"
    return ob


def ellipsoid(name, rx, ry, rz, mat):
    bm = bmesh.new()
    bmesh.ops.create_uvsphere(bm, u_segments=32, v_segments=16, radius=1.0)
    me = bpy.data.meshes.new(name)
    bm.to_mesh(me)
    bm.free()
    for p in me.polygons:
        p.use_smooth = True
    ob = link(bpy.data.objects.new(name, me))
    ob.data.materials.append(mat)
    ob.scale = (rx, ry, rz)
    ob.rotation_mode = "QUATERNION"
    return ob


def box(name, center, size, mat):
    bm = bmesh.new()
    bmesh.ops.create_cube(bm, size=1.0)
    me = bpy.data.meshes.new(name)
    bm.to_mesh(me)
    bm.free()
    ob = link(bpy.data.objects.new(name, me))
    ob.data.materials.append(mat)
    ob.location = center
    ob.scale = size
    return ob


def place_segment(ob, a, b):
    a, b = Vector(a), Vector(b)
    ob.location = a
    ob.rotation_quaternion = Vector((0, 0, 1)).rotation_difference((b - a).normalized())


def place_basis(ob, center, up, left=(0, 1, 0)):
    z = Vector(up).normalized()
    y = Vector(left)
    y = (y - z * y.dot(z)).normalized()
    x = y.cross(z)
    ob.location = Vector(center)
    ob.rotation_quaternion = Matrix((x, y, z)).transposed().to_quaternion()


# ---------------------------------------------------------------- figure
LIMBS = {  # name: (from, to, r_from, r_to)
    "upper_l": ("shoulder_l", "elbow_l", 0.052, 0.042), "upper_r": ("shoulder_r", "elbow_r", 0.052, 0.042),
    "fore_l": ("elbow_l", "wrist_l", 0.040, 0.030), "fore_r": ("elbow_r", "wrist_r", 0.040, 0.030),
    "thigh_l": ("hip_l", "knee_l", 0.078, 0.054), "thigh_r": ("hip_r", "knee_r", 0.078, 0.054),
    "shin_l": ("knee_l", "ankle_l", 0.052, 0.038), "shin_r": ("knee_r", "ankle_r", 0.052, 0.038),
    "neck": ("chest", "neck", 0.045, 0.042),
    "shoulders": ("shoulder_l", "shoulder_r", 0.055, 0.055),
}


def foot_points(fr, side):
    ank, knee, toe = Vector(fr["ankle_" + side]), Vector(fr["knee_" + side]), Vector(fr["toe_" + side])
    fd = (toe - ank).normalized()
    away = ank - knee
    n = (away - fd * away.dot(fd))
    n = n.normalized() if n.length > 1e-6 else Vector((0, 0, -1))
    heel = ank - fd * 0.045 + n * 0.040
    tip = ank + fd * 0.165 + n * 0.045
    return heel, tip


def arch_points(fr, side):
    """An opt-in articulated foot: grounded heel/forefoot, long uncurled toes."""
    heel, tip = foot_points(fr, side)
    fd = (tip - heel).normalized()
    amount = max(0.0, min(0.025, fr["foot_arch_" + side][0]))
    # The forefoot draws slightly toward the fixed heel as the midfoot rises.
    ball = heel.lerp(tip, 0.70) - fd * amount * 0.5
    toe = tip - fd * amount * 0.5
    mid = heel.lerp(ball, 0.48) + Vector((0, 0, 0.018 + amount))
    return heel, mid, ball, toe


FINGERS = (  # (offset across the palm, proximal length, distal length)
    (0.030, 0.042, 0.036), (0.010, 0.047, 0.040), (-0.010, 0.045, 0.038), (-0.029, 0.037, 0.030))
PALM_LEN = 0.085


def hand_frame(fr, side):
    """Unit axes of a hand: u along the palm (wrist -> knuckles), n palm normal, s across."""
    wr, tip = Vector(fr["wrist_" + side]), Vector(fr["hand_" + side])
    u = (tip - wr).normalized()
    n = Vector(fr["palmn_" + side])
    n = (n - u * n.dot(u))
    n = n.normalized() if n.length > 1e-6 else Vector((0, 0, -1))
    s = u.cross(n).normalized()
    # make s point towards the body midline (thumb side)
    inward = Vector((0, -1 if side == "l" else 1, 0))
    if s.dot(inward) < 0:
        s = -s
    return wr, u, n, s


def hand_segments(fr, side):
    """Palm centre/axes and finger + thumb segments (a, b) for one hand."""
    wr, u, n, s = hand_frame(fr, side)
    g = max(0.0, min(1.0, fr["grip_" + side][0]))
    a = math.radians(85) * g
    lifts = fr.get("finger_lift_" + side, [0, 0, 0, 0, 0])
    if len(lifts) != 5:
        lifts = [lifts[0]] * 5
    segs = []
    for index, (off, l1, l2) in enumerate(FINGERS):
        lifted = a - math.radians(lifts[index])
        base = wr + u * PALM_LEN - s * off
        d1 = u * math.cos(lifted) + n * math.sin(lifted)
        e1 = base + d1 * l1
        distal = 2 * a - math.radians(lifts[index])
        d2 = u * math.cos(distal) + n * math.sin(distal)
        segs.append((base, e1))
        segs.append((e1, e1 + d2 * l2))
    tb = wr + u * 0.025 + s * 0.032
    td = (u * 0.75 + s * 0.55 + n * (0.15 + 0.7 * g - math.sin(math.radians(lifts[4])))).normalized()
    segs.append((tb, tb + td * 0.055))
    palm_c = wr + u * (PALM_LEN * 0.55) + n * 0.004
    return palm_c, u, n, s, segs


class Figure:
    def __init__(self, f0):
        skin, joint = material("figure", FIGURE), material("joint", JOINT)
        self.parts = {}
        for name, (a, b, r1, r2) in LIMBS.items():
            length = (Vector(f0[b]) - Vector(f0[a])).length
            self.parts[name] = capsule(name, length, r1, r2, skin)
        for side in ("l", "r"):
            h, t = foot_points(f0, side)
            if "foot_arch_" + side in f0:
                points = arch_points(f0, side)
                for index, (a, b) in enumerate(zip(points, points[1:])):
                    r1, r2 = ((.036, .023), (.023, .027), (.027, .024))[index]
                    name = f"foot_{side}_{index}"
                    self.parts[name] = capsule(name, (b - a).length, r1, r2, skin)
            else:
                self.parts["foot_" + side] = capsule("foot_" + side, (t - h).length, 0.036, 0.026, skin)
            _, _, _, _, segs = hand_segments(f0, side)
            self.parts["palm_" + side] = ellipsoid("palm_" + side, 0.042, 0.016, 0.052, skin)
            for i, (a, b) in enumerate(segs):
                r = 0.0105 if i == len(segs) - 1 else 0.0088
                self.parts[f"fing_{side}_{i}"] = capsule(f"fing_{side}_{i}", (b - a).length, r, r * 0.92, skin, seg=12, rings=4)
            for j, r in (("knee", 0.056), ("elbow", 0.044), ("ankle", 0.040), ("wrist", 0.032)):
                self.parts[f"{j}_{side}_ball"] = ellipsoid(f"{j}_{side}_ball", r, r, r, joint)
        self.parts["pelvis"] = ellipsoid("pelvis", 0.105, 0.165, 0.11, skin)
        self.parts["belly"] = ellipsoid("belly", 0.10, 0.145, 0.13, skin)
        self.parts["chest"] = ellipsoid("chest", 0.115, 0.175, 0.17, skin)
        self.parts["head"] = ellipsoid("head", 0.098, 0.082, 0.118, skin)
        if f0.get("head_detail", [0])[0]:
            self.parts["nose"] = ellipsoid("nose", .028, .020, .024, skin)
            self.parts["chin"] = ellipsoid("chin", .035, .034, .026, skin)

    def pose(self, fr):
        for name, (a, b, _, _) in LIMBS.items():
            place_segment(self.parts[name], fr[a], fr[b])
        for side in ("l", "r"):
            h, t = foot_points(fr, side)
            if "foot_arch_" + side in fr:
                points = arch_points(fr, side)
                for index, (a, b) in enumerate(zip(points, points[1:])):
                    place_segment(self.parts[f"foot_{side}_{index}"], a, b)
            else:
                place_segment(self.parts["foot_" + side], h, t)
            for j in ("knee", "elbow", "ankle", "wrist"):
                self.parts[f"{j}_{side}_ball"].location = fr[f"{j}_{side}"]
            pc, u, n, sv, segs = hand_segments(fr, side)
            palm = self.parts["palm_" + side]
            palm.location = pc
            # ellipsoid axes: x = across (s), y = thickness (n), z = along (u)
            palm.rotation_quaternion = Matrix((sv, n, u)).transposed().to_quaternion()
            for i, (a, b) in enumerate(segs):
                place_segment(self.parts[f"fing_{side}_{i}"], a, b)
        up1, up2 = Vector(fr["pelvis_up"]), Vector(fr["chest_up"])
        l1 = Vector(fr.get("pelvis_left", (0, 1, 0)))
        l2 = Vector(fr.get("chest_left", (0, 1, 0)))
        P, S, C = Vector(fr["pelvis"]), Vector(fr["spine"]), Vector(fr["chest"])
        place_basis(self.parts["pelvis"], P + up1 * 0.03, up1, l1)
        place_basis(self.parts["belly"], (P + S) / 2 + up1 * 0.06, (up1 + up2).normalized(), (l1 + l2).normalized())
        place_basis(self.parts["chest"], C - up2 * 0.11, up2, l2)
        head_up = (Vector(fr["head"]) - Vector(fr["neck"])).normalized()
        head_left = Vector(fr.get("head_left", l2))
        center = Vector(fr["head"])
        place_basis(self.parts["head"], center + head_up * 0.02, head_up, head_left)
        if "nose" in self.parts:
            forward = head_left.cross(head_up).normalized()
            place_basis(self.parts["nose"], center + head_up * .025 + forward * .10, head_up, head_left)
            place_basis(self.parts["chin"], center - head_up * .060 + forward * .078, head_up, head_left)


# ---------------------------------------------------------------- props
def P(spec, fr):
    """Prop point: [x,y,z] or {"joint": name, "off": [dx,dy,dz]} (follows the figure)."""
    if isinstance(spec, dict):
        return Vector(fr[spec["joint"]]) + Vector(spec.get("off", (0, 0, 0)))
    return Vector(spec)


class Props:
    """Static props are built once; joint-attached ones are re-placed every frame."""

    def __init__(self, spec, mat, f0):
        self.dynamic = []
        self.points = []
        for pr in spec:
            k = pr["kind"]
            if k == "bar":
                x, z, half, r = pr.get("x", 0.0), pr["z"], pr["half"], pr.get("r", 0.017)
                bar = capsule("bar", 2 * half, r, r, mat)
                place_segment(bar, (x, -half, z), (x, half, z))
                for y in (-half, half):
                    post = capsule("post", z, 0.028, 0.028, mat)
                    place_segment(post, (x, y, 0), (x, y, z))
                self.points += [(x, -half, z), (x, half, z), (x, -half, 0), (x, half, 0)]
            elif k == "chair":
                x, s_ = pr["x"], pr["seat"]
                box("seat", (x, 0, s_ - 0.02), (0.44, 0.44, 0.04), mat)
                for dx in (-0.2, 0.2):
                    for dy in (-0.2, 0.2):
                        box("leg", (x + dx, dy, (s_ - 0.04) / 2), (0.035, 0.035, s_ - 0.04), mat)
                box("back", (x - 0.2, 0, s_ + 0.25), (0.035, 0.44, 0.40), mat)
                for dy in (-0.2, 0.2):
                    box("back_post", (x - 0.2, dy, s_ + 0.2), (0.035, 0.035, 0.42), mat)
                self.points += [(x - 0.22, -0.22, 0), (x + 0.22, 0.22, s_ + 0.45)]
            elif k == "box":
                c, sz = pr["center"], pr["size"]
                box("box", c, sz, material("box", tuple(pr["color"])) if "color" in pr else mat)
                for dx in (-0.5, 0.5):
                    for dy in (-0.5, 0.5):
                        for dz in (-0.5, 0.5):
                            self.points.append((c[0] + dx * sz[0], c[1] + dy * sz[1], c[2] + dz * sz[2]))
            elif k == "rod":
                a_, b_ = P(pr["a"], f0), P(pr["b"], f0)
                r = pr.get("r", 0.017)
                ob = capsule("rod", 1.0, r, r, material("rod", tuple(pr["color"])) if "color" in pr else mat)
                self.dynamic.append(("rod", ob, pr))
                self._rod(ob, pr, f0)
                if not isinstance(pr["a"], dict):
                    self.points.append(tuple(a_))
                if not isinstance(pr["b"], dict):
                    self.points.append(tuple(b_))
            elif k == "weight":
                obs = self._weight(pr["type"], mat)
                self.dynamic.append(("weight", obs, pr))
                self._place_weight(obs, pr, f0)
            elif k == "plate":
                c = Vector(pr["center"])
                ob = ellipsoid("plate", pr.get("r", 0.2), pr.get("r", 0.2), 0.012, mat)
                ob.location = c
                ob.rotation_quaternion = Vector((0, 0, 1)).rotation_difference(Vector(pr.get("normal", (0, 0, 1))))
                self.points.append(tuple(c))

    @staticmethod
    def _rod(ob, pr, fr):
        a_, b_ = P(pr["a"], fr), P(pr["b"], fr)
        place_segment(ob, a_, b_)
        ob.scale = (1, 1, max((b_ - a_).length, 1e-4))

    @staticmethod
    def _weight(kind, mat):
        if kind == "kettlebell":
            body = ellipsoid("kb", 0.085, 0.085, 0.08, mat)
            handle = capsule("kb_handle", 0.12, 0.014, 0.014, mat)
            return [body, handle]
        bar = capsule("db_bar", 0.16, 0.016, 0.016, mat)
        h1 = ellipsoid("db_head", 0.045, 0.045, 0.03, mat)
        h2 = ellipsoid("db_head", 0.045, 0.045, 0.03, mat)
        return [bar, h1, h2]

    @staticmethod
    def _place_weight(obs, pr, fr):
        w = Vector(fr[pr["joint"]])
        side = pr["joint"][-1]
        hand = Vector(fr["hand_" + side]) if ("hand_" + side) in fr else w + Vector((0, 0, -0.17))
        c = w + (hand - w) * 0.45 + Vector(pr.get("off", (0, 0, 0)))
        if pr["type"] == "kettlebell":
            body, handle = obs
            body.location = c + Vector((0, 0, -0.13))
            body.rotation_quaternion = Quaternion()
            place_segment(handle, c + Vector((0, -0.06, -0.02)), c + Vector((0, 0.06, -0.02)))
        else:
            bar, h1, h2 = obs
            axis = Vector((0, 1, 0))
            place_segment(bar, c - axis * 0.08, c + axis * 0.08)
            for h, sgn in ((h1, -1), (h2, 1)):
                h.location = c + axis * (0.09 * sgn)
                h.rotation_quaternion = Vector((0, 0, 1)).rotation_difference(axis)

    def pose(self, fr):
        for kind, ob, pr in self.dynamic:
            if kind == "rod":
                self._rod(ob, pr, fr)
            else:
                self._place_weight(ob, pr, fr)


# ---------------------------------------------------------------- camera
def camera(data, pts):
    az, el = math.radians(data["cam_az"]), math.radians(data["cam_el"])
    fwd = Vector((-math.sin(az) * math.cos(el), math.cos(az) * math.cos(el), -math.sin(el)))
    cam_data = bpy.data.cameras.new("cam")
    cam_data.lens = 50
    cam_data.sensor_width = 36
    cam = link(bpy.data.objects.new("cam", cam_data))
    bpy.context.scene.camera = cam
    lo = Vector([min(p[i] for p in pts) for i in range(3)])
    hi = Vector([max(p[i] for p in pts) for i in range(3)])
    center = (lo + hi) / 2
    rot = fwd.to_track_quat("-Z", "Y")
    cam.rotation_mode = "QUATERNION"
    cam.rotation_quaternion = rot
    inv = rot.inverted()
    tan_h = (cam_data.sensor_width / 2) / cam_data.lens
    tan_v = tan_h * RES[1] / RES[0]
    # centre the content in view, then back off until everything fits with a margin
    rel = [inv @ (Vector(p) - center) for p in pts]
    cx = (max(v.x for v in rel) + min(v.x for v in rel)) / 2
    cy = (max(v.y for v in rel) + min(v.y for v in rel)) / 2
    shift = rot @ Vector((cx, cy, 0))
    center = center + shift
    margin = 0.92
    def fits(dist):
        for p in pts:
            v = inv @ (Vector(p) - (center - fwd * dist))
            depth = -v.z
            if depth <= 0.05:
                return False
            if abs(v.x) / depth > tan_h * margin or abs(v.y) / depth > tan_v * margin:
                return False
        return True
    lo_d, hi_d = 0.5, 40.0
    for _ in range(50):
        mid = (lo_d + hi_d) / 2
        if fits(mid):
            hi_d = mid
        else:
            lo_d = mid
    cam.location = center - fwd * hi_d
    return cam


# ---------------------------------------------------------------- scene
def scene_setup(engine):
    bpy.ops.wm.read_factory_settings(use_empty=True)
    sc = bpy.context.scene
    sc.render.resolution_x, sc.render.resolution_y = RES
    sc.render.resolution_percentage = 100
    sc.render.image_settings.file_format = "PNG"
    sc.render.image_settings.color_mode = "RGB"
    sc.view_settings.view_transform = "Standard"
    world = bpy.data.worlds.new("world")
    world.color = (1, 1, 1)
    try:
        world.use_nodes = True
    except Exception:
        pass
    bg = world.node_tree.nodes.get("Background") if world.node_tree else None
    if bg:
        bg.inputs[0].default_value = (1, 1, 1, 1)
        bg.inputs[1].default_value = 1.0
    sc.world = world
    if engine == "WORKBENCH":
        sc.render.engine = "BLENDER_WORKBENCH"
        sh = sc.display.shading
        sh.light = "STUDIO"
        sh.color_type = "MATERIAL"
        sh.show_shadows = True
        sh.shadow_intensity = 0.25
        sh.show_cavity = True
        sh.cavity_type = "WORLD"
        sc.display.light_direction = (0.25, -0.35, 0.90)
        sc.display.render_aa = "16"
    else:
        sc.render.engine = "CYCLES"
        sc.cycles.samples = 24
        sc.render.use_persistent_data = True
        sc.cycles.use_denoising = True
        try:  # Apple GPU if available
            prefs = bpy.context.preferences.addons["cycles"].preferences
            prefs.compute_device_type = "METAL"
            prefs.get_devices()
            for dev in prefs.devices:
                dev.use = True
            sc.cycles.device = "GPU"
        except Exception as exc:
            print("[bakeoff] GPU not used:", exc)
        # white background added later (encode.py); the floor only catches shadows
        sc.render.film_transparent = True
        sc.render.image_settings.color_mode = "RGBA"
        if bg:
            bg.inputs[1].default_value = 0.35
        sun = link(bpy.data.objects.new("sun", bpy.data.lights.new("sun", "SUN")))
        sun.data.energy = 1.1
        sun.data.angle = math.radians(25)
        sun.rotation_euler = (math.radians(20), math.radians(-12), math.radians(30))
    floor = box("floor", (0, 0, -0.005), (60, 60, 0.01), material("floor", FLOOR))
    if engine != "WORKBENCH":
        floor.is_shadow_catcher = True
    return sc


def render(ex_id, engine, start_only):
    path = os.path.join(WORK, ex_id, "joints.json")
    with open(path) as fh:
        data = json.load(fh)
    override = camera_stamp(ex_id)
    if override:
        data.update(override)
    sc = scene_setup(engine)
    props_ = Props(data["props"], material("prop", PROP), data["frames"][0])
    pts = list(props_.points)
    fig = Figure(data["frames"][0])
    for fr in data["frames"]:
        for k, v in fr.items():
            if not (k.endswith(("_up", "_left")) or k.startswith(("palmn_", "grip_", "finger_lift_", "foot_arch_")) or k == "head_detail"):
                pts.append(tuple(v))
    if data.get("camera_focus"):
        pts = [tuple(fr[name]) for fr in data["frames"] for name in data["camera_focus"]]
    # pad every point by the body's thickness so limbs and head stay inside the frame
    pad = data.get("camera_padding", 0.12)
    pts = [(p[0] + dx, p[1] + dy, max(p[2] + dz, 0.0)) for p in set(pts)
           for dx, dy, dz in ((0, 0, 0), (pad, 0, 0), (-pad, 0, 0), (0, pad, 0), (0, -pad, 0), (0, 0, pad), (0, 0, -pad))]
    camera(data, pts)
    out = os.path.join(WORK, ex_id, "blender", "frames")
    os.makedirs(out, exist_ok=True)
    frames = data["frames"][:1] if start_only else data["frames"]
    t0 = time.time()
    for i, fr in enumerate(frames):
        fig.pose(fr)
        props_.pose(fr)
        sc.render.filepath = os.path.join(out, f"{i:04d}.png")
        bpy.ops.render.render(write_still=True)
        if i == 0:
            sc.render.filepath = os.path.join(WORK, ex_id, "blender", "start.png")
            bpy.ops.render.render(write_still=True)
    # A start-only preview is deliberately incomplete and must never make a
    # cached full render appear current. Stamp only after every frame succeeds.
    if not start_only and override:
        stamp_path = os.path.join(WORK, ex_id, "blender", "render-camera.json")
        with open(stamp_path, "w") as fh:
            json.dump(override, fh, sort_keys=True)
            fh.write("\n")
    print(f"[bakeoff] {ex_id}: {len(frames)} frames in {time.time() - t0:.0f} s -> {out}")


def done_already(ex_id):
    """Resume: all frames exist and are newer than joints.json."""
    j = os.path.join(WORK, ex_id, "joints.json")
    out = os.path.join(WORK, ex_id, "blender", "frames")
    expected_camera = camera_stamp(ex_id)
    if expected_camera:
        stamp_path = os.path.join(WORK, ex_id, "blender", "render-camera.json")
        try:
            with open(stamp_path) as fh:
                if json.load(fh) != expected_camera:
                    return False
        except (OSError, ValueError):
            return False
    if not os.path.isdir(out):
        return False
    with open(j) as fh:
        n = len(json.load(fh)["frames"])
    names = [f for f in os.listdir(out) if f.endswith(".png")]
    return len(names) >= n and min(os.path.getmtime(os.path.join(out, f)) for f in names) > os.path.getmtime(j)


def main():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    engine = "CYCLES"
    if "--engine" in argv:
        engine = argv[argv.index("--engine") + 1].upper()
    start_only = "--start-only" in argv
    ids = [a for a in argv if not a.startswith("--") and a.upper() != engine]
    if not ids:
        ids = sorted(d for d in os.listdir(WORK) if os.path.exists(os.path.join(WORK, d, "joints.json")))
    with open(os.path.join(WORK, "blender-version.txt"), "w") as fh:
        fh.write(f"{bpy.app.version_string} engine={engine}\n")
    resume = "--resume" in argv
    ok, skipped, failed = [], [], []
    t_all = time.time()
    for n, ex in enumerate(ids, 1):
        if resume and not start_only and done_already(ex):
            skipped.append(ex)
            continue
        print(f"[bakeoff] {n}/{len(ids)} {ex} (elapsed {(time.time() - t_all) / 60:.0f} min)", flush=True)
        try:
            render(ex, engine, start_only)
            ok.append(ex)
        except Exception as exc:  # keep going overnight; report at the end
            import traceback
            traceback.print_exc()
            failed.append(f"{ex}: {exc}")
    summary = (f"[bakeoff] done in {(time.time() - t_all) / 60:.0f} min: {len(ok)} rendered, "
               f"{len(skipped)} already done, {len(failed)} failed")
    print(summary)
    for f in failed:
        print("[bakeoff] FAILED", f)
    with open(os.path.join(WORK, "render-summary.txt"), "w") as fh:
        fh.write(summary + "\n" + "\n".join("FAILED " + f for f in failed) + "\n")


if __name__ == "__main__":
    main()
