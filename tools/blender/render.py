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

import bpy
import bmesh
from mathutils import Matrix, Quaternion, Vector

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
WORK = os.path.join(ROOT, "work", "clips-bakeoff")
RES = (960, 720)

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
    "hand_l": ("wrist_l", "hand_l", 0.032, 0.024), "hand_r": ("wrist_r", "hand_r", 0.032, 0.024),
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


class Figure:
    def __init__(self, f0):
        skin, joint = material("figure", FIGURE), material("joint", JOINT)
        self.parts = {}
        for name, (a, b, r1, r2) in LIMBS.items():
            length = (Vector(f0[b]) - Vector(f0[a])).length
            self.parts[name] = capsule(name, length, r1, r2, skin)
        for side in ("l", "r"):
            h, t = foot_points(f0, side)
            self.parts["foot_" + side] = capsule("foot_" + side, (t - h).length, 0.036, 0.026, skin)
            for j, r in (("knee", 0.056), ("elbow", 0.044), ("ankle", 0.040), ("wrist", 0.032)):
                self.parts[f"{j}_{side}_ball"] = ellipsoid(f"{j}_{side}_ball", r, r, r, joint)
        self.parts["pelvis"] = ellipsoid("pelvis", 0.105, 0.165, 0.11, skin)
        self.parts["belly"] = ellipsoid("belly", 0.10, 0.145, 0.13, skin)
        self.parts["chest"] = ellipsoid("chest", 0.115, 0.175, 0.17, skin)
        self.parts["head"] = ellipsoid("head", 0.098, 0.082, 0.118, skin)

    def pose(self, fr):
        for name, (a, b, _, _) in LIMBS.items():
            place_segment(self.parts[name], fr[a], fr[b])
        for side in ("l", "r"):
            h, t = foot_points(fr, side)
            place_segment(self.parts["foot_" + side], h, t)
            for j in ("knee", "elbow", "ankle", "wrist"):
                self.parts[f"{j}_{side}_ball"].location = fr[f"{j}_{side}"]
        up1, up2 = Vector(fr["pelvis_up"]), Vector(fr["chest_up"])
        P, S, C = Vector(fr["pelvis"]), Vector(fr["spine"]), Vector(fr["chest"])
        place_basis(self.parts["pelvis"], P + up1 * 0.03, up1)
        place_basis(self.parts["belly"], (P + S) / 2 + up1 * 0.06, (up1 + up2).normalized())
        place_basis(self.parts["chest"], C - up2 * 0.11, up2)
        head_up = (Vector(fr["head"]) - Vector(fr["neck"])).normalized()
        place_basis(self.parts["head"], Vector(fr["head"]) + head_up * 0.02, head_up)


# ---------------------------------------------------------------- props
def props(spec, mat):
    pts = []
    for pr in spec:
        if pr["kind"] == "bar":
            z, half, r = pr["z"], pr["half"], pr["r"]
            bar = capsule("bar", 2 * half, r, r, mat)
            place_segment(bar, (0, -half, z), (0, half, z))
            for y in (-half, half):
                post = capsule("post", z, 0.028, 0.028, mat)
                place_segment(post, (0, y, 0), (0, y, z))
            pts += [(0, -half, z), (0, half, z)]
        if pr["kind"] == "chair":
            x, s = pr["x"], pr["seat"]
            box("seat", (x, 0, s - 0.02), (0.44, 0.44, 0.04), mat)
            for dx in (-0.2, 0.2):
                for dy in (-0.2, 0.2):
                    box("leg", (x + dx, dy, (s - 0.04) / 2), (0.035, 0.035, s - 0.04), mat)
            box("back", (x - 0.2, 0, s + 0.25), (0.035, 0.44, 0.40), mat)
            for dy in (-0.2, 0.2):
                box("back_post", (x - 0.2, dy, s + 0.2), (0.035, 0.035, 0.42), mat)
            pts += [(x - 0.22, -0.22, 0), (x + 0.22, 0.22, s + 0.45)]
    return pts


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
        sc.cycles.samples = 48
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
    sc = scene_setup(engine)
    pts = props(data["props"], material("prop", PROP))
    fig = Figure(data["frames"][0])
    for fr in data["frames"]:
        for k, v in fr.items():
            if not k.endswith("_up"):
                pts.append(tuple(v))
    # pad every point by the body's thickness so limbs and head stay inside the frame
    pad = 0.12
    pts = [(p[0] + dx, p[1] + dy, max(p[2] + dz, 0.0)) for p in set(pts)
           for dx, dy, dz in ((0, 0, 0), (pad, 0, 0), (-pad, 0, 0), (0, pad, 0), (0, -pad, 0), (0, 0, pad), (0, 0, -pad))]
    camera(data, pts)
    out = os.path.join(WORK, ex_id, "blender", "frames")
    os.makedirs(out, exist_ok=True)
    frames = data["frames"][:1] if start_only else data["frames"]
    t0 = time.time()
    for i, fr in enumerate(frames):
        fig.pose(fr)
        sc.render.filepath = os.path.join(out, f"{i:04d}.png")
        bpy.ops.render.render(write_still=True)
        if i == 0:
            sc.render.filepath = os.path.join(WORK, ex_id, "blender", "start.png")
            bpy.ops.render.render(write_still=True)
    print(f"[bakeoff] {ex_id}: {len(frames)} frames in {time.time() - t0:.0f} s -> {out}")


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
    for ex in ids:
        render(ex, engine, start_only)
    print("[bakeoff] done:", ", ".join(ids))


main()
