"""Strict toes-to-bar legs constrained before the shared two-bone solve.

Hip-centered spherical travel keeps the legs long through the whole transition;
no solved joints or anatomical segment lengths are rewritten.
"""
import math

REST_RADIUS_M = 0.867


def _unit(v):
    length = math.sqrt(sum(float(x) * float(x) for x in v))
    if not math.isfinite(length) or length <= 1e-9:
        raise ValueError("degenerate hip-to-ankle vector")
    return [float(x) / length for x in v]


def _slerp(a, b, t):
    """Spherical interpolation of two unit direction vectors."""
    dot = max(-1.0, min(1.0, sum(a[i] * b[i] for i in range(3))))
    angle = math.acos(dot)
    if angle <= 1e-8:
        return _unit([(1.0 - t) * a[i] + t * b[i] for i in range(3)])
    sine = math.sin(angle)
    return [math.sin((1.0 - t) * angle) / sine * a[i] +
            math.sin(t * angle) / sine * b[i] for i in range(3)]


def _smoothstep(t):
    return t * t * (3.0 - 2.0 * t)


def with_straight_legs(source):
    """Densely key the authored body path with strict-leg ankle targets.

    Each key starts from the original toes-to-bar pose sampled on the output
    frame grid. Its ankle target is projected onto a fixed hip-centered sphere,
    and its knee pole is a stable perpendicular. ``poses.build`` then calls the
    unchanged ``poses.solve`` / ``two_bone`` implementation for every frame.
    """
    import poses

    n = int(round(source["seconds"] * poses.FPS))
    keyed = []
    endpoint_dirs = {}
    top_t = 0.56
    for side in ("l", "r"):
        for label, time in (("down", 0.0), ("top", top_t)):
            pose = poses.sample(source["keys"], time)
            solved = poses.solve(pose, {})
            hip = solved["hip_" + side]
            endpoint_dirs[(label, side)] = _unit(
                [pose["ankle_" + side][i] - hip[i] for i in range(3)])

    for frame_index in range(n):
        t = frame_index / n
        pose = poses.sample(source["keys"], t)
        solved = poses.solve(pose, {})
        for side in ("l", "r"):
            hip = solved["hip_" + side]
            if t <= .12:
                q = 0.0
            elif t < .50:
                q = _smoothstep((t - .12) / (.50 - .12))
            elif t <= .62:
                q = 1.0
            elif t < .88:
                q = 1.0 - _smoothstep((t - .62) / (.88 - .62))
            else:
                q = 0.0
            # Raise/lower the straight leg along the shorter spherical arc,
            # with a settled hang and a stable top-contact hold.
            direction = _slerp(endpoint_dirs[("down", side)], endpoint_dirs[("top", side)], q)
            pose["ankle_" + side] = [hip[i] + REST_RADIUS_M * direction[i] for i in range(3)]
            # Stable sagittal perpendicular; the existing two_bone solver
            # projects it against the hip-to-target vector as usual.
            pole = [direction[2], 0.0, -direction[0]]
            if math.sqrt(sum(x * x for x in pole)) <= 1e-8:
                pole = [1.0, 0.0, 0.0]
            pose["knee_" + side] = pole
        keyed.append((t, pose))
    keys = keyed

    return {
        "seconds": source["seconds"],
        "keys": keys,
        "cam_az": source["cam_az"],
        "cam_el": source["cam_el"],
        "props": source.get("props", []),
        "motion_profile": "strict-toes-hip-sphere-two-bone",
    }
