"""Scoped phase, contact and identity checks for lower-body pilot clips."""
import math


DEPTH_ID = "depth-jump"
PISTOL_ID = "squat-pistol"


def _dist(a, b):
    return math.sqrt(sum((float(x) - float(y)) ** 2 for x, y in zip(a, b)))


def _joint_angle(a, b, c):
    u = [a[i] - b[i] for i in range(3)]
    v = [c[i] - b[i] for i in range(3)]
    den = _dist(u, [0, 0, 0]) * _dist(v, [0, 0, 0])
    if den <= 1e-9:
        return float("nan")
    cosine = max(-1.0, min(1.0, sum(u[i] * v[i] for i in range(3)) / den))
    return math.degrees(math.acos(cosine))


def _audit_finite(data):
    frames = data.get("frames") if isinstance(data, dict) else None
    if not isinstance(frames, list) or not frames:
        return False, 0
    for i, frame in enumerate(frames):
        if not isinstance(frame, dict):
            return False, i
        for value in frame.values():
            if isinstance(value, (int, float)) and not math.isfinite(value):
                return False, i
            if isinstance(value, (list, tuple)):
                if not all(isinstance(v, (int, float)) and math.isfinite(v) for v in value):
                    return False, i
    return True, None


def _metric(metrics, failures, check, value, minimum, maximum, frame=None):
    finite = isinstance(value, (int, float)) and math.isfinite(value)
    ok = finite and minimum <= value <= maximum
    reported = value if finite else None
    metrics[check] = {"value": reported, "min": minimum, "max": maximum, "ok": bool(ok)}
    if not ok:
        failure = {"check": check, "value": reported}
        if frame is not None:
            failure["frame"] = frame
        failures.append(failure)


def _max_speed(data):
    frames = data.get("frames", [])
    fps = float(data.get("fps", 0) or 0)
    if len(frames) < 2 or fps <= 0:
        return float("nan")
    max_speed = 0.0
    for a, b in zip(frames, frames[1:] + frames[:1]):
        for name in ("pelvis", "ankle_l", "ankle_r", "knee_l", "knee_r", "wrist_l", "wrist_r"):
            if name in a and name in b:
                max_speed = max(max_speed, _dist(a[name], b[name]) * fps)
    return max_speed


def _hand_drift(data, metrics, failures, maximum):
    frames = data["frames"]
    for side in ("l", "r"):
        name = "wrist_" + side
        drift = max(_dist(frames[0][name], f[name]) for f in frames)
        _metric(metrics, failures, name + "_drift_m", drift, 0.0, maximum)




def _pistol(data, metrics, failures):
    frames = data["frames"]
    first = frames[0]
    drift = max(_dist(first["ankle_l"], f["ankle_l"]) for f in frames)
    _metric(metrics, failures, "support_ankle_drift_m", drift, 0.0, .003)
    pelvis_z = [f["pelvis"][2] for f in frames]
    lo, hi = min(pelvis_z), max(pelvis_z)
    depth_frame = pelvis_z.index(lo)
    _metric(metrics, failures, "full_depth_range_m", hi - lo, .52, .64, depth_frame)
    _metric(metrics, failures, "deepest_pelvis_m", lo, .32, .43, depth_frame)
    at_bottom = frames[depth_frame]
    forward = at_bottom["ankle_r"][0] - at_bottom["pelvis"][0]
    _metric(metrics, failures, "raised_leg_forward_reach_m", forward, .42, .90, depth_frame)
    _metric(metrics, failures, "raised_foot_clearance_m", at_bottom["ankle_r"][2], .10, .30, depth_frame)
    straightness = min(_joint_angle(f["hip_r"], f["knee_r"], f["ankle_r"]) for f in frames)
    _metric(metrics, failures, "raised_leg_knee_angle_deg", straightness, 160.0, 180.0)
    last = frames[-1]
    return_error = _dist(first["pelvis"], last["pelvis"])
    _metric(metrics, failures, "cycle_return_error_m", return_error, 0.0, .08, len(frames) - 1)
    seam = max(_dist(first[name], last[name]) for name in
               ("pelvis", "ankle_l", "ankle_r", "knee_l", "knee_r", "wrist_l", "wrist_r"))
    _metric(metrics, failures, "loop_seam_joint_gap_m", seam, 0.0, .012, len(frames) - 1)
    foot_floor_min = min(f["ankle_l"][2] for f in frames)
    foot_floor_max = max(f["ankle_l"][2] for f in frames)
    _metric(metrics, failures, "support_ankle_height_range_m", foot_floor_max - foot_floor_min, 0.0, .003)


def audit(data):
    """Return scoped lower-body metrics and explicit failures for two IDs."""
    if isinstance(data, dict) and data.get("id") == DEPTH_ID:
        from generation_checks.depth_jump import audit as depth_audit
        return depth_audit(data)
    metrics, failures = {}, []
    finite, bad_frame = _audit_finite(data)
    _metric(metrics, failures, "finite_geometry", 1 if finite else 0, 1, 1, bad_frame)
    exercise_id = data.get("id") if isinstance(data, dict) else None
    if exercise_id not in (DEPTH_ID, PISTOL_ID):
        _metric(metrics, failures, "known_variant", 0, 1, 1)
        return {"metrics": metrics, "failures": failures}
    _metric(metrics, failures, "known_variant", 1, 1, 1)
    speed = _max_speed(data)
    speed_max = 2.5 if exercise_id == PISTOL_ID else 8.0
    hand_drift_max = 0.75 if exercise_id == PISTOL_ID else 1.5
    _metric(metrics, failures, "max_joint_speed_m_s", speed, 0.0, speed_max)
    if not finite:
        return {"metrics": metrics, "failures": failures}
    _hand_drift(data, metrics, failures, hand_drift_max)
    _pistol(data, metrics, failures)
    return {"metrics": metrics, "failures": failures}
