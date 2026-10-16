"""Stick-figure contact sheet for tools/blender/poses.py output (sandbox check).

Rows = 6 moments of the loop; columns = side view (from the figure's right),
front view, and the 3/4 camera direction used by render.py (orthographic).
Grey line = floor, blue = props, red dots = hands/feet contact points.
"""
import math
from PIL import Image, ImageDraw

SEGS = [("pelvis", "spine"), ("spine", "chest"), ("chest", "neck"),
        ("hip_l", "hip_r"), ("shoulder_l", "shoulder_r"),
        ("hip_l", "knee_l"), ("knee_l", "ankle_l"), ("ankle_l", "toe_l"),
        ("hip_r", "knee_r"), ("knee_r", "ankle_r"), ("ankle_r", "toe_r"),
        ("shoulder_l", "elbow_l"), ("elbow_l", "wrist_l"), ("wrist_l", "hand_l"),
        ("shoulder_r", "elbow_r"), ("elbow_r", "wrist_r"), ("wrist_r", "hand_r")]
CELL = 300


def cam_axes(az, el):
    a, e = math.radians(az), math.radians(el)
    # camera looks from the figure's right-front toward the figure
    fwd = [-math.sin(a) * math.cos(e), math.cos(a) * math.cos(e), -math.sin(e)]
    right = [math.cos(a), math.sin(a), 0.0]
    up = [right[1] * fwd[2] - right[2] * fwd[1], right[2] * fwd[0] - right[0] * fwd[2],
          right[0] * fwd[1] - right[1] * fwd[0]]
    return right, up


def projectors(data):
    r, u = cam_axes(data["cam_az"], data["cam_el"])
    return [
        ("side", lambda p: (p[0], p[2])),
        ("front", lambda p: (-p[1], p[2])),
        ("camera", lambda p: (sum(p[i] * r[i] for i in range(3)), sum(p[i] * u[i] for i in range(3)))),
    ]


def _p(spec, fr):
    if isinstance(spec, dict):
        j, o = fr[spec["joint"]], spec.get("off", (0, 0, 0))
        return [j[0] + o[0], j[1] + o[1], j[2] + o[2]]
    return spec


def prop_lines(props, fr=None):
    out = []
    for pr in props:
        if pr["kind"] == "box":
            c, s = pr["center"], pr["size"]
            xs, ys, zs = (c[0] - s[0] / 2, c[0] + s[0] / 2), (c[1] - s[1] / 2, c[1] + s[1] / 2), (c[2] - s[2] / 2, c[2] + s[2] / 2)
            for x in xs:
                for y in ys:
                    out.append(([x, y, zs[0]], [x, y, zs[1]]))
                for z in zs:
                    out.append(([x, ys[0], z], [x, ys[1], z]))
            for y in ys:
                for z in zs:
                    out.append(([xs[0], y, z], [xs[1], y, z]))
        if pr["kind"] == "rod" and fr is not None:
            out.append((_p(pr["a"], fr), _p(pr["b"], fr)))
        if pr["kind"] == "weight" and fr is not None:
            w = fr[pr["joint"]]
            out.append(([w[0], w[1] - 0.09, w[2] - 0.05], [w[0], w[1] + 0.09, w[2] - 0.05]))
        if pr["kind"] == "plate":
            c, r = pr["center"], pr.get("r", 0.2)
            out.append(([c[0] - r, c[1], c[2]], [c[0] + r, c[1], c[2]]))
        if pr["kind"] == "bar":
            x = pr.get("x", 0)
            out.append(([x, -pr["half"], pr["z"]], [x, pr["half"], pr["z"]]))
            for y in (-pr["half"], pr["half"]):
                out.append(([x, y, 0], [x, y, pr["z"]]))
        if pr["kind"] == "chair":
            x, s = pr["x"], pr["seat"]
            for y in (-0.22, 0.22):
                out += [([x - 0.22, y, s], [x + 0.22, y, s]), ([x - 0.22, y, 0], [x - 0.22, y, s]),
                        ([x + 0.22, y, 0], [x + 0.22, y, s]), ([x - 0.22, y, s], [x - 0.22, y, s + 0.45])]
            out.append(([x - 0.22, -0.22, s], [x - 0.22, 0.22, s]))
            out.append(([x + 0.22, -0.22, s], [x + 0.22, 0.22, s]))
    return out


def sheet(data, path, moments=(0.0, 0.2, 0.35, 0.5, 0.75, 0.9)):
    frames = data["frames"]
    projs = projectors(data)
    props = prop_lines(data["props"], frames[0])
    img = Image.new("RGB", (CELL * len(projs), CELL * len(moments)), "white")
    d = ImageDraw.Draw(img)
    for ci, (name, pj) in enumerate(projs):
        pts = [pj(p) for fr in frames for k, p in fr.items() if not (k.endswith(("_up", "_left")) or k.startswith(("palmn_", "grip_", "finger_lift_", "foot_arch_")) or k == "head_detail")]
        pts += [pj(p) for ln in props for p in ln] + [pj([0, 0, 0])]
        xs, ys = [p[0] for p in pts], [p[1] for p in pts]
        span = max(max(xs) - min(xs), max(ys) - min(ys)) * 1.15
        cx, cy = (max(xs) + min(xs)) / 2, (max(ys) + min(ys)) / 2
        sc = CELL / span
        for ri, m in enumerate(moments):
            fr = frames[int(m * len(frames)) % len(frames)]
            ox, oy = ci * CELL, ri * CELL
            def T(p):
                x, y = pj(p)
                return (ox + CELL / 2 + (x - cx) * sc, oy + CELL / 2 - (y - cy) * sc)
            d.rectangle([ox, oy, ox + CELL - 1, oy + CELL - 1], outline=(220, 220, 220))
            # floor
            for a, b in (([-3, 0, 0], [3, 0, 0]), ([0, -3, 0], [0, 3, 0])):
                d.line([T(a), T(b)], fill=(170, 170, 170), width=1)
            for a, b in prop_lines(data["props"], fr):
                d.line([T(a), T(b)], fill=(60, 110, 220), width=3)
            for a, b in SEGS:
                col = (40, 40, 40) if a.endswith("_l") or b.endswith("_l") else (230, 120, 20)
                if not (a[-2:] in ("_l", "_r") or b[-2:] in ("_l", "_r")):
                    col = (40, 40, 40)
                d.line([T(fr[a]), T(fr[b])], fill=col, width=4)
            hx, hy = T(fr["head"])
            r = 0.11 * sc
            d.ellipse([hx - r, hy - r, hx + r, hy + r], outline=(40, 40, 40), width=3)
            for p in ("hand_l", "hand_r", "toe_l", "toe_r", "wrist_l", "wrist_r", "ankle_l", "ankle_r"):
                x, y = T(fr[p])
                d.ellipse([x - 3, y - 3, x + 3, y + 3], fill=(220, 30, 30))
            d.text((ox + 4, oy + 4), f"{name} t={m:.2f}", fill=(0, 0, 0))
    img.save(path)
