"""Source-trajectory checks for Russian dips and flag kick presses.

These gates screen authored paths and contact proxies; passing them does not
approve exercise form. A rendered, encoded visual review is still required.
The flag-kick foot/toe speed ceiling is a movement-specific engineering cap
for its explosive entry; the torso center, chest and head retain a 2.5 m/s cap.
"""
import math


JOINTS = (
    "pelvis", "spine", "chest", "neck", "head",
    "shoulder_l", "shoulder_r", "elbow_l", "elbow_r",
    "wrist_l", "wrist_r", "hand_l", "hand_r",
    "hip_l", "hip_r", "knee_l", "knee_r",
    "ankle_l", "ankle_r", "toe_l", "toe_r",
)
CORE_SPEED_JOINTS = ("pelvis", "chest", "head")
FLAG_FEET_SPEED_JOINTS = ("ankle_l", "ankle_r", "toe_l", "toe_r")


def _dist(a, b):
    return math.sqrt(sum((x-y)**2 for x, y in zip(a, b)))


def _angle(a, b, c):
    u, v = [x-y for x, y in zip(a, b)], [x-y for x, y in zip(c, b)]
    nu = math.sqrt(sum(x*x for x in u)); nv = math.sqrt(sum(x*x for x in v))
    if nu*nv <= 1e-10:
        return float("nan")
    return math.degrees(math.acos(max(-1., min(1., sum(x*y for x, y in zip(u, v))/(nu*nv)))))


def _point_to_bar(point, side):
    """Distance from a joint center to the finite Russian-dip bar axis."""
    x = max(-.25, min(.25, point[0]))
    y = .25 if side == "l" else -.25
    return math.sqrt((point[0]-x)**2 + (point[1]-y)**2 + (point[2]-1.0)**2)


def _joint_speeds(frames, fps, names):
    return {name: max((_dist(frames[i-1][name], frames[i][name])*fps
                       for i in range(1, len(frames))), default=float("nan"))
            for name in names}


def audit(data):
    """Return ``{metrics, failures, requires_visual_review}`` for scoped IDs."""
    metrics, failures = {}, []

    def record(name, value, lo=None, hi=None, frame=None):
        valid = isinstance(value, (int, float)) and math.isfinite(value)
        ok = valid and (lo is None or value >= lo) and (hi is None or value <= hi)
        metrics[name] = {"value": round(float(value), 6) if valid else None,
                         "min": lo, "max": hi, "ok": bool(ok)}
        if not ok:
            failure = {"check": name, "value": metrics[name]["value"]}
            if frame is not None:
                failure["frame"] = int(frame)
            failures.append(failure)

    exercise = data.get("id")
    record("exercise_id", int(exercise in ("dip-russian", "flag-kick-press")), 1, 1)
    frames, fps = data.get("frames"), data.get("fps", 0)
    valid_timeline = (exercise in ("dip-russian", "flag-kick-press")
                      and isinstance(frames, list) and len(frames) >= 16
                      and isinstance(fps, (int, float)) and math.isfinite(fps) and fps > 0)
    record("valid_timeline", int(valid_timeline), 1, 1)
    if not valid_timeline:
        return {"metrics": metrics, "failures": failures, "requires_visual_review": True}

    finite = all(name in f and isinstance(f[name], (list, tuple)) and len(f[name]) == 3
                 and all(isinstance(x, (int, float)) and math.isfinite(x) for x in f[name])
                 for f in frames for name in JOINTS)
    record("finite_geometry", int(finite), 1, 1)
    if not finite:
        return {"metrics": metrics, "failures": failures, "requires_visual_review": True}

    duration = len(frames)/fps
    record("duration_s", duration, 3.0 if exercise == "dip-russian" else 2.8,
           5.0 if exercise == "dip-russian" else 4.0)
    seam = max((_dist(frames[0][name], frames[-1][name]) for name in JOINTS), default=float("nan"))
    record("loop_seam_max_m", seam, hi=.012)

    speed_by_joint = _joint_speeds(frames, fps, JOINTS)
    for name, speed in speed_by_joint.items():
        # Individual records expose all landmark rates, even where no ceiling
        # has been established for a movement-specific limb action.
        cap = 2.5 if exercise == "dip-russian" or name in CORE_SPEED_JOINTS else None
        if exercise == "flag-kick-press" and name in FLAG_FEET_SPEED_JOINTS:
            cap = 6.0
        record("max_speed_" + name + "_m_s", speed, hi=cap)
    max_speed = max(speed_by_joint.values())
    record("max_joint_speed_m_s", max_speed,
           hi=(2.5 if exercise == "dip-russian" else 6.0))

    if exercise == "dip-russian":
        low = [i for i, f in enumerate(frames) if .645 <= f["pelvis"][2] <= .655]
        record("forearm_support_samples", len(low), lo=6)
        elbow_max = max((_point_to_bar(frames[i]["elbow_"+side], side)
                         for i in low for side in ("l", "r")), default=float("nan"))
        wrist_max = max((_point_to_bar(frames[i]["wrist_"+side], side)
                         for i in low for side in ("l", "r")), default=float("nan"))
        bend_min = min((_angle(frames[i]["shoulder_"+side], frames[i]["elbow_"+side],
                               frames[i]["wrist_"+side])
                        for i in low for side in ("l", "r")), default=float("nan"))
        record("forearm_elbow_bar_axis_distance_max_m", elbow_max, hi=.067)
        record("wrist_bar_axis_distance_max_m", wrist_max, hi=.045)
        record("supported_elbow_angle_min_deg", bend_min, lo=125.)
        record("starts_in_upper_support", int(frames[0]["pelvis"][2] > .9), 1, 1)
        record("reaches_forearm_dip", int(min(f["pelvis"][2] for f in frames) < .67), 1, 1)
        record("returns_to_upper_support", int(frames[-3]["pelvis"][2] < frames[0]["pelvis"][2]+.02), 1, 1)
    else:
        peak = max(range(len(frames)), key=lambda i: frames[i]["pelvis"][2])
        peak_z = frames[peak]["pelvis"][2]
        apex_count = sum(f["pelvis"][2] >= peak_z-.005 for f in frames)
        record("kick_hip_peak_z_m", max(frames[peak]["hip_l"][2], frames[peak]["hip_r"][2]), lo=1.65, frame=peak)
        record("kick_ankle_peak_z_m", max(max(f["ankle_l"][2], f["ankle_r"][2]) for f in frames), lo=1.6)
        record("kick_apex_samples_within_5mm", apex_count, hi=2, frame=peak)

        grip_distance = max((math.hypot(f["wrist_"+side][0], f["wrist_"+side][1])
                             for f in frames for side in ("l", "r")), default=float("nan"))
        record("pole_wrist_centerline_distance_max_m", grip_distance, hi=.03)
        landing_indices = [i for i, f in enumerate(frames) if i > peak
                           and f["ankle_l"][2] <= .09 and f["ankle_r"][2] <= .09]
        landing_bends = [_angle(frames[i]["hip_l"], frames[i]["knee_l"], frames[i]["ankle_l"])
                         for i in landing_indices[:6]]
        record("bilateral_landing_samples", len(landing_indices), lo=3)
        record("landing_knee_angle_min_deg", min(landing_bends, default=float("nan")), hi=165.)

    return {"metrics": metrics, "failures": failures, "requires_visual_review": True}
