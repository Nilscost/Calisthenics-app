"""Exercise-scoped floor/core motion checks; never certify exercise form."""
import math


_RADII = {
    "toe_l": .02, "toe_r": .02, "ankle_l": .04, "ankle_r": .04,
    "knee_l": .05, "knee_r": .05, "wrist_l": .025, "wrist_r": .025,
    "hand_l": .012, "hand_r": .012, "elbow_l": .04, "elbow_r": .04,
    "hip_l": .07, "hip_r": .07, "shoulder_l": .05, "shoulder_r": .05,
    "chest": .10, "spine": .09, "pelvis": .09, "head": .10,
}
_JOINTS = (
    "pelvis", "spine", "chest", "neck", "head",
    *(name+"_"+side for name in ("hip", "knee", "ankle", "toe", "shoulder", "elbow", "wrist", "hand")
      for side in ("l", "r")),
)


def _distance(a, b):
    return math.sqrt(sum((x-y)**2 for x, y in zip(a, b)))


def _angle(a, b):
    na, nb = _distance(a, [0., 0., 0.]), _distance(b, [0., 0., 0.])
    if na < 1e-9 or nb < 1e-9:
        return math.nan
    cosine = sum(x*y for x, y in zip(a, b))/(na*nb)
    return math.degrees(math.acos(max(-1., min(1., cosine))))


def _heel_frame(frame, side):
    """Return normalized shin projection and heel normal used by render.foot_points."""
    shin = [frame["ankle_"+side][i]-frame["knee_"+side][i] for i in range(3)]
    foot = [frame["toe_"+side][i]-frame["ankle_"+side][i] for i in range(3)]
    shin_length, foot_length = _distance(shin, [0.,0.,0.]), _distance(foot, [0.,0.,0.])
    if shin_length < 1e-9 or foot_length < 1e-9:
        return math.nan, None
    shin = [x/shin_length for x in shin]
    foot = [x/foot_length for x in foot]
    along = sum(a*b for a, b in zip(shin, foot))
    normal = [shin[i]-along*foot[i] for i in range(3)]
    condition = _distance(normal, [0.,0.,0.])
    if condition < 1e-9:
        return condition, None
    return condition, [x/condition for x in normal]


def audit(data):
    """Return scoped metrics and failures for roll-over or full hanging wiper."""
    metrics, failures = {}, []

    def metric(name, value, low=None, high=None, frame=None):
        finite = isinstance(value, (int, float)) and math.isfinite(value)
        ok = finite and (low is None or low <= value) and (high is None or value <= high)
        value_out = value if finite else None
        metrics[name] = {"value": value_out, "min": low, "max": high, "ok": bool(ok)}
        if not ok:
            item = {"check": name, "value": value_out}
            if frame is not None:
                item["frame"] = frame
            failures.append(item)

    if not isinstance(data, dict):
        metric("valid_geometry", 0, 1, 1)
        return {"metrics": metrics, "failures": failures}
    exercise_id = data.get("id")
    if exercise_id not in ("pilates-roll-over", "hanging-windshield-wiper"):
        metric("wrong_variant", 0, 1, 1)
        return {"metrics": metrics, "failures": failures}
    frames = data.get("frames")
    fps = data.get("fps")
    if (not isinstance(frames, list) or len(frames) < 2 or
            not isinstance(fps, (int, float)) or not math.isfinite(fps) or fps <= 0):
        metric("valid_geometry", 0, 1, 1)
        return {"metrics": metrics, "failures": failures}

    required = set(_JOINTS) | {"pelvis_up", "pelvis_left", "chest_up", "chest_left"}
    valid = True
    for i, frame in enumerate(frames):
        if not isinstance(frame, dict) or not required.issubset(frame):
            valid = False
            failures.append({"check": "valid_geometry", "value": "missing required joint", "frame": i})
            continue
        for name in required:
            value = frame[name]
            if (not isinstance(value, (list, tuple)) or len(value) != 3 or
                    any(not isinstance(x, (int, float)) or not math.isfinite(x) for x in value)):
                valid = False
                failures.append({"check": "valid_geometry", "value": f"invalid vector {name}", "frame": i})
                break
        for name, value in frame.items():
            if isinstance(value, list):
                if any(not isinstance(x, (int, float)) or not math.isfinite(x) for x in value):
                    valid = False
                    failures.append({"check": "valid_geometry", "value": f"non-finite {name}", "frame": i})
                    break
    metrics["valid_geometry"] = {"value": int(valid), "min": 1, "max": 1, "ok": valid}
    if not valid:
        return {"metrics": metrics, "failures": failures}

    duration = len(frames)/fps
    metric("duration_s", duration, 3.9 if exercise_id == "pilates-roll-over" else 6.4,
           4.1 if exercise_id == "pilates-roll-over" else 6.6)

    point_names = list(_JOINTS)
    speeds = [(_distance(frames[i][name], frames[(i+1) % len(frames)][name])*fps, i)
              for i in range(len(frames)) for name in point_names]
    max_speed, speed_frame = max(speeds, default=(None, None))
    metric("max_joint_speed_m_s", max_speed, 0, 2.25 if exercise_id == "pilates-roll-over" else 3.5,
           speed_frame)

    seam = max((_distance(frames[0][name], frames[-1][name]) for name in point_names), default=None)
    metric("loop_seam_max_m", seam, 0, .04)
    wrist_drift = max((_distance(frame["wrist_l"], frames[0]["wrist_l"]) for frame in frames), default=None)
    wrist_drift = max(wrist_drift, max((_distance(frame["wrist_r"], frames[0]["wrist_r"])
                                        for frame in frames), default=None))
    metric("hand_drift_max_m", wrist_drift, 0, .005)

    if exercise_id == "pilates-roll-over":
        bend = max(_angle(frame["pelvis_up"], frame["chest_up"]) for frame in frames)
        metric("trunk_curl_max_deg", bend, 45, 65)
        pelvis_lift = max(frame["pelvis"][2] for frame in frames)-min(frame["pelvis"][2] for frame in frames)
        metric("pelvis_lift_range_m", pelvis_lift, .18, .40)
        ankle_separation = max(_distance(frame["ankle_l"], frame["ankle_r"]) for frame in frames)
        metric("leg_opening_max_m", ankle_separation, .30, .42)
    else:
        roll = max(_angle(frames[0]["pelvis_left"], frame["pelvis_left"]) for frame in frames)
        metric("torso_counterroll_max_deg", roll, 3.5, 18)
        center_y = [(frame["ankle_l"][1]+frame["ankle_r"][1])*.5 for frame in frames]
        sweep = max(center_y)-min(center_y)
        metric("lateral_sweep_range_m", sweep, .60, .85)
        side_signs = (min(center_y), max(center_y))
        metric("both_sweep_sides_m", min(abs(side_signs[0]), abs(side_signs[1])), .20, .50)

    floor_surface = min((frame[name][2]-radius for frame in frames
                         for name, radius in _RADII.items() if name in frame), default=None)
    metric("floor_surface_min_m", floor_surface, -.02, 10)

    if exercise_id == "pilates-roll-over":
        # Exact vertical support extent of render.Figure's chest ellipsoid in
        # the sagittal plane. Its center is chest - .11*chest_up; local X and
        # Z radii are .115 m and .17 m. This catches the old supine 5 mm clip.
        chest_surfaces = []
        for i, frame in enumerate(frames):
            up = frame["chest_up"]
            radius = math.sqrt((.115*up[0])**2 + (.17*up[2])**2)
            surface_z = frame["chest"][2] - .11*up[2] - radius
            chest_surfaces.append((surface_z, i))
        chest_min, chest_frame = min(chest_surfaces, default=(math.nan, None))
        metric("chest_surface_min_m", chest_min, .001, None, chest_frame)

    heel_frames = {side: [_heel_frame(frame, side) for frame in frames] for side in ("l", "r")}
    heel_condition = min((item[0] for values in heel_frames.values() for item in values), default=math.nan)
    metric("heel_frame_condition_min", heel_condition, .15, None)
    normal_steps = []
    for values in heel_frames.values():
        for i, current in enumerate(values):
            following = values[(i+1) % len(values)]
            if current[1] is None or following[1] is None:
                normal_steps.append(math.nan)
            else:
                normal_steps.append(_angle(current[1], following[1]))
    max_normal_step = max(normal_steps, default=math.nan)
    metric("heel_frame_normal_step_max_deg", max_normal_step, None, 45)
    return {"metrics": metrics, "failures": failures}
