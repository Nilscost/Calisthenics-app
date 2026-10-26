"""Focused engineering gates for wall-walk and kick-up pilot poses.

These checks screen the encoded trajectories. In particular, a passing
kick-up does not establish the catalog's named forward-roll bail.
"""
import math


IDS = {"handstand-wall-walk-up", "handstand-kick-up"}
JOINTS = tuple(f"{joint}_{side}" for joint in
               ("hip", "knee", "ankle", "toe", "shoulder", "elbow", "wrist", "hand")
               for side in ("l", "r")) + ("pelvis", "spine", "chest", "neck", "head")


def _dist(a, b):
    return math.sqrt(sum((float(x) - float(y)) ** 2 for x, y in zip(a, b)))


def _metric(metrics, failures, name, value, minimum=None, maximum=None):
    finite = isinstance(value, (int, float)) and math.isfinite(value)
    passed = finite and (minimum is None or value >= minimum) and (maximum is None or value <= maximum)
    metrics[name] = {"value": round(float(value), 6) if finite else None,
                     "min": minimum, "max": maximum, "ok": bool(passed)}
    if not passed:
        failures.append({"check": name, "value": metrics[name]["value"]})


def _max_speed(frames, fps):
    return max(_dist(a[joint], b[joint]) * fps
               for a, b in zip(frames, frames[1:] + frames[:1])
               for joint in JOINTS)


def _run_at_least(flags, count):
    best = run = 0
    for flag in flags:
        run = run + 1 if flag else 0
        best = max(best, run)
    return best >= count


def _head_floor_proxy(frame):
    """Lowest point of the current rendered head ellipsoid, analytically.

    Mirrors render.py's head ellipsoid radii and 20 mm center offset. This is
    only a local source gate; native Blender geometry remains authoritative.
    """
    up = [frame["head"][i] - frame["neck"][i] for i in range(3)]
    length = _dist(up, (0, 0, 0))
    if length <= 1e-9:
        return float("nan")
    up = [v / length for v in up]
    left = list(frame.get("chest_left", (0, 1, 0)))
    dot = sum(left[i] * up[i] for i in range(3))
    left = [left[i] - dot * up[i] for i in range(3)]
    length = _dist(left, (0, 0, 0))
    if length <= 1e-9:
        return float("nan")
    left = [v / length for v in left]
    x_axis = [left[1] * up[2] - left[2] * up[1],
              left[2] * up[0] - left[0] * up[2],
              left[0] * up[1] - left[1] * up[0]]
    # Ellipsoid local radii are (0.098, 0.082, 0.118) m; basis axes are
    # (left cross up, left, up), matching render.place_basis.
    vertical_radius = math.sqrt((.098 * x_axis[2]) ** 2 + (.082 * left[2]) ** 2
                                + (.118 * up[2]) ** 2)
    center_z = frame["head"][2] + .020 * up[2]
    return center_z - vertical_radius


def audit(data):
    """Return numeric metric bounds/failures for these two authored variants."""
    metrics, failures = {}, []
    exercise_id = data.get("id") if isinstance(data, dict) else None
    _metric(metrics, failures, "known_variant", int(exercise_id in IDS), 1, 1)
    if exercise_id not in IDS:
        return {"metrics": metrics, "failures": failures}

    frames = data.get("frames")
    fps = data.get("fps", 0)
    timeline_ok = (isinstance(frames, list) and len(frames) >= 16
                   and isinstance(fps, (int, float)) and math.isfinite(fps) and fps > 0
                   and all(isinstance(frame, dict) for frame in frames))
    _metric(metrics, failures, "valid_timeline", int(timeline_ok), 1, 1)
    if not timeline_ok:
        return {"metrics": metrics, "failures": failures}
    finite = all(joint in frame and isinstance(frame[joint], (list, tuple)) and len(frame[joint]) == 3
                 and all(isinstance(v, (int, float)) and math.isfinite(v) for v in frame[joint])
                 for frame in frames for joint in JOINTS)
    _metric(metrics, failures, "finite_world_joints", int(finite), 1, 1)
    if not finite:
        return {"metrics": metrics, "failures": failures}

    _metric(metrics, failures, "max_joint_speed_m_s", _max_speed(frames, float(fps)), 0.0, 2.5)
    n = len(frames)
    if exercise_id == "handstand-wall-walk-up":
        head_floor = min(_head_floor_proxy(frame) for frame in frames)
        _metric(metrics, failures, "head_ellipsoid_floor_proxy_m", head_floor, -.002, None)
        toe_error = max(abs(frame[side][0]) for frame in frames for side in ("toe_l", "toe_r"))
        _metric(metrics, failures, "max_toe_wall_anchor_error_m", toe_error, 0.0, .006)
        # Alternating planted steps: wrists sit on the floor (z = .035) except
        # during their own swing; planted wrists do not slide; the two hands
        # never swing together; each hand makes several separate steps.
        floor = .035
        _metric(metrics, failures, "wrist_below_floor_contact_m",
                max(floor - frame[side][2] for frame in frames for side in ("wrist_l", "wrist_r")),
                -1.0, .001)
        lifted = {side: [frame[side][2] > floor + .001 for frame in frames]
                  for side in ("wrist_l", "wrist_r")}
        slide = 0.0
        for side in ("wrist_l", "wrist_r"):
            for a, b in zip(frames, frames[1:] + frames[:1]):
                if a[side][2] <= floor + .001 and b[side][2] <= floor + .001:
                    slide = max(slide, math.hypot(b[side][0] - a[side][0], b[side][1] - a[side][1]))
        _metric(metrics, failures, "planted_wrist_slide_m_per_frame", slide, 0.0, .001)
        both_up = sum(1 for l, r in zip(lifted["wrist_l"], lifted["wrist_r"]) if l and r)
        _metric(metrics, failures, "frames_with_both_hands_lifted", both_up, 0, 0)
        for side in ("wrist_l", "wrist_r"):
            steps = sum(1 for i, v in enumerate(lifted[side]) if v and not lifted[side][i - 1])
            _metric(metrics, failures, side + "_step_count", steps, 6, 20)
        wrist_x = [frame["wrist_l"][0] for frame in frames]
        travel = max(wrist_x) - min(wrist_x)
        _metric(metrics, failures, "hand_travel_range_m", travel, .20, 1.80)
        middle = n // 2
        _metric(metrics, failures, "walk_up_wrist_progress_m",
                wrist_x[middle] - wrist_x[0], .08, None)
        _metric(metrics, failures, "walk_down_wrist_progress_m",
                wrist_x[middle] - wrist_x[-1], .08, None)
        return {"metrics": metrics, "failures": failures}

    wrist_drift = max(_dist(frames[0][side], frame[side])
                      for side in ("wrist_l", "wrist_r") for frame in frames)
    _metric(metrics, failures, "max_hand_anchor_drift_m", wrist_drift, 0.0, .003)
    transition = frames[int(.16 * n):int(.32 * n)]
    stagger = max(abs(frame["ankle_l"][2] - frame["ankle_r"][2]) for frame in transition)
    _metric(metrics, failures, "kick_leg_stagger_m", stagger, .30, None)
    upright = [abs(frame["ankle_l"][2] - 1.965) < .005
               and abs(frame["ankle_r"][2] - 1.965) < .005 for frame in frames]
    upright_seconds = sum(upright) / float(fps)
    _metric(metrics, failures, "vertical_hold_s", upright_seconds, .70, None)

    # Form-promotion gate: current catalog variant requires a practiced bail
    # (forward roll or side step-out), absent from this generated pose channel.
    # Keep this explicit binary metric separate from engineering checks.
    metrics["catalog_bail_branch_present"] = {
        "value": 0, "min": 1, "max": 1, "ok": False,
        "status": "promotion_blocked_missing_named_bail"}
    failures.append({"check": "catalog_bail_branch_present", "value": 0})
    return {"metrics": metrics, "failures": failures}
