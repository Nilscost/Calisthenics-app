"""Authored-variant knee articulation checks for burpee-pushup.

These checks look for out-of-plane knee travel and discontinuous knee paths;
they are engineering screens for this animation, not universal form limits.
"""
import math


SIDES = ("l", "r")
POINTS = tuple(f"{joint}_{side}" for side in SIDES
               for joint in ("hip", "knee", "ankle"))


def _sub(a, b):
    return [a[i] - b[i] for i in range(3)]


def _dot(a, b):
    return sum(a[i] * b[i] for i in range(3))


def _norm(a):
    return math.sqrt(_dot(a, a))


def _dist(a, b):
    return _norm(_sub(a, b))


def _knee_metrics(frame, side):
    hip, knee, ankle = (frame[f"{name}_{side}"] for name in ("hip", "knee", "ankle"))
    thigh, shin = _sub(hip, knee), _sub(ankle, knee)
    den = _norm(thigh) * _norm(shin)
    angle = math.degrees(math.acos(max(-1.0, min(1.0, _dot(thigh, shin) / den)))) if den else float("nan")
    chord = _sub(ankle, hip)
    chord_sq = _dot(chord, chord)
    t = max(0.0, min(1.0, _dot(_sub(knee, hip), chord) / chord_sq)) if chord_sq else .5
    chord_point = [hip[j] + t * chord[j] for j in range(3)]
    offset = _sub(knee, chord_point)
    return angle, offset


def _plank_mask(frames):
    """Stable two-hand/two-foot contact identifies the push-up interval."""
    return [
        f["wrist_l"][2] <= .04 and f["wrist_r"][2] <= .04
        and abs(f["ankle_l"][0] + .80) <= .002
        and abs(f["ankle_r"][0] + .80) <= .002
        and abs(f["ankle_l"][2] - .08) <= .002
        and abs(f["ankle_r"][2] - .08) <= .002
        for f in frames
    ]


def _fit_quadratic(points):
    """Least-squares ``z = a*t² + b*t + c`` without a numeric dependency."""
    if len(points) < 3:
        return None
    xs, ys = zip(*points)
    sums = [sum(x ** power for x in xs) for power in range(5)]
    rhs = [sum((x ** power) * y for x, y in points) for power in range(3)]
    matrix = [[sums[4], sums[3], sums[2], rhs[2]],
              [sums[3], sums[2], sums[1], rhs[1]],
              [sums[2], sums[1], sums[0], rhs[0]]]
    for col in range(3):
        pivot = max(range(col, 3), key=lambda row: abs(matrix[row][col]))
        if abs(matrix[pivot][col]) < 1e-12:
            return None
        matrix[col], matrix[pivot] = matrix[pivot], matrix[col]
        scale = matrix[col][col]
        matrix[col] = [value / scale for value in matrix[col]]
        for row in range(3):
            if row == col:
                continue
            scale = matrix[row][col]
            matrix[row] = [matrix[row][j] - scale * matrix[col][j] for j in range(4)]
    a, b, c = (matrix[row][3] for row in range(3))
    rmse = math.sqrt(sum((a*x*x + b*x + c - y) ** 2 for x, y in points) / len(points))
    return a, b, c, rmse


def _longest_consecutive_run(indices):
    """Length of the longest consecutive integer run in a sorted sequence."""
    if not indices:
        return 0
    best = run = 1
    previous = indices[0]
    for index in indices[1:]:
        run = run + 1 if index == previous + 1 else 1
        best = max(best, run)
        previous = index
    return best


def audit(data):
    """Return ``{metrics, failures}`` for a built burpee-pushup pose document."""
    metrics, failures = {}, []

    def record(name, value, *, lo=None, hi=None, frame=None):
        valid = isinstance(value, (int, float)) and math.isfinite(value)
        shown = round(float(value), 6) if valid else None
        ok = valid and (lo is None or value >= lo) and (hi is None or value <= hi)
        metrics[name] = {"value": shown, "min": lo, "max": hi, "ok": bool(ok)}
        if not ok:
            entry = {"check": name, "value": shown}
            if frame is not None:
                entry["frame"] = int(frame)
            failures.append(entry)

    record("exercise_id", 1 if data.get("id") == "burpee-pushup" else 0, lo=1, hi=1)
    frames, fps = data.get("frames"), data.get("fps")
    if (not isinstance(frames, list) or len(frames) < 8
            or not isinstance(fps, (int, float)) or not math.isfinite(fps) or fps <= 0):
        record("valid_timeline", 0, lo=1, hi=1)
        return {"metrics": metrics, "failures": failures}
    record("valid_timeline", 1, lo=1, hi=1)
    finite = all(isinstance(f, dict) and all(
        isinstance(f.get(k), (list, tuple)) and len(f[k]) == 3
        and all(isinstance(v, (int, float)) and math.isfinite(v) for v in f[k])
        for k in POINTS) for f in frames)
    record("finite_leg_geometry", 1 if finite else 0, lo=1, hi=1)
    if not finite:
        return {"metrics": metrics, "failures": failures}

    geometry = [[_knee_metrics(frame, side) for side in SIDES] for frame in frames]
    flexed = [(i, side_i, angle, offset)
              for i, pair in enumerate(geometry) for side_i, (angle, offset) in enumerate(pair)
              if angle <= 165.0]
    plane = [(abs(offset[1]), i, side_i) for i, side_i, _, offset in flexed]
    max_plane = max(plane, default=(None, None, None))
    record("max_knee_lateral_excursion_m", max_plane[0], hi=.10,
           frame=max_plane[1])
    record("flexed_knee_samples", len(flexed), lo=8)
    knee_angles = [angle for pair in geometry for angle, _ in pair]
    record("minimum_knee_angle_deg", min(knee_angles, default=None), hi=125)
    record("meaningfully_bent_knee_samples", sum(angle <= 145 for angle in knee_angles), lo=12)

    # In this exercise the body faces +X. Bent knees should travel in the
    # forward/back sagittal plane, not escape laterally; require clear forward
    # sagittal displacement in a substantial share of flexed samples.
    forward_samples = sum(offset[0] >= .025 for _, _, _, offset in flexed)
    forward_fraction = forward_samples / len(flexed) if flexed else None
    record("forward_sagittal_bend_fraction", forward_fraction, lo=.60)

    # A knee should stay on the same sagittal flexion branch while bent. Ignore
    # near-straight samples, where the signed offset is numerically ambiguous.
    flips = 0
    for side_i in range(2):
        seq = [(geometry[i][side_i][0], geometry[i][side_i][1][0]) for i in range(len(frames))]
        for (a0, x0), (a1, x1) in zip(seq, seq[1:]):
            if a0 <= 165 and a1 <= 165 and abs(x0) >= .025 and abs(x1) >= .025 and x0 * x1 < 0:
                flips += 1
    record("flexed_sagittal_branch_flips", flips, hi=0)

    mask = _plank_mask(frames)
    runs, start = [], None
    for i, planted in enumerate(mask + [False]):
        if planted and start is None:
            start = i
        elif not planted and start is not None:
            runs.append((start, i - 1))
            start = None
    plank = max(runs, key=lambda run: run[1] - run[0], default=None)
    record("stable_plank_frames", plank[1] - plank[0] + 1 if plank else 0, lo=12)
    if plank is None:
        for name in ("squat_to_plank_knee_accel_m_s2", "plank_to_squat_knee_accel_m_s2",
                     "peak_lateral_knee_accel_m_s2"):
            record(name, None, hi=30)
        return {"metrics": metrics, "failures": failures}

    # Use acceleration of the knee relative to its moving hip-ankle chord. This
    # isolates knee-branch pops from whole-leg translation. Inspect the handoff
    # on both sides of the planted push-up interval.
    offset_series = [[geometry[i][s][1] for i in range(len(frames))] for s in range(2)]
    accel = [[None] * len(frames) for _ in SIDES]
    for s in range(2):
        for i in range(1, len(frames) - 1):
            delta2 = [offset_series[s][i + 1][j] - 2 * offset_series[s][i][j] + offset_series[s][i - 1][j]
                      for j in range(3)]
            accel[s][i] = [_ * fps * fps for _ in delta2]

    pad_before, pad_after = max(2, round(.12 * fps)), max(2, round(.18 * fps))
    to_window = range(max(1, plank[0] - pad_before), min(len(frames) - 1, plank[0] + pad_after + 1))
    from_window = range(max(1, plank[1] - pad_after), min(len(frames) - 1, plank[1] + pad_before + 1))

    def peak(window, component=None):
        vals = []
        for i in window:
            for s in range(2):
                if accel[s][i] is not None:
                    value = (abs(accel[s][i][component]) if component is not None
                             else _norm(accel[s][i]))
                    vals.append((value, i))
        return max(vals, default=(None, None), key=lambda item: item[0])

    to_peak = peak(to_window)
    from_peak = peak(from_window)
    # Retain the original transition bounds while refining the trajectory;
    # accepting a new pose must not silently weaken its regression guard.
    record("squat_to_plank_knee_accel_m_s2", to_peak[0], hi=30, frame=to_peak[1])
    record("plank_to_squat_knee_accel_m_s2", from_peak[0], hi=30, frame=from_peak[1])
    all_transition = list(to_window) + list(from_window)
    lateral_peak = peak(all_transition, component=1)
    record("peak_lateral_knee_accel_m_s2", lateral_peak[0], hi=24, frame=lateral_peak[1])

    # Locate the last bilateral airborne interval after the planted push-up.
    # It is the final jump (earlier feet-in transitions remain outside this
    # screen). Use ankle height rather than key times so the check follows the
    # generated trajectory when phase timing changes.
    flight_runs, flight_start = [], None
    for i in range(plank[1] + 1, len(frames)):
        airborne = min(frames[i]["ankle_l"][2], frames[i]["ankle_r"][2]) > .14
        if airborne and flight_start is None:
            flight_start = i
        elif not airborne and flight_start is not None:
            flight_runs.append((flight_start, i - 1))
            flight_start = None
    if flight_start is not None:
        flight_runs.append((flight_start, len(frames) - 1))
    final_flight = max(flight_runs, key=lambda run: run[1], default=None)
    if final_flight is None:
        for name, lo, hi in (
            ("final_jump_flight_duration_s", .30, .75),
            ("final_jump_peak_rise_m", .10, .35),
            ("final_jump_apex_dwell_s", None, .16),
            ("final_jump_fit_frame_count", 3, None),
            ("final_jump_takeoff_knee_min_deg", 155, None),
            ("final_jump_gravity_fit_m_s2", 6.0, 13.5),
            ("final_jump_fit_rmse_m", None, .035),
            ("final_jump_apex_fraction", .25, .75),
        ):
            record(name, None, lo=lo, hi=hi)
    else:
        start, end = final_flight
        duration = (end - start + 1) / fps
        record("final_jump_flight_duration_s", duration, lo=.30, hi=.75, frame=start)
        flight_pelvis = [frames[i]["pelvis"][2] for i in range(start, end + 1)]
        apex_offset = max(range(len(flight_pelvis)), key=flight_pelvis.__getitem__)
        apex_i = start + apex_offset
        peak_z = flight_pelvis[apex_offset]
        launch_z = frames[max(plank[1], start - 1)]["pelvis"][2]
        record("final_jump_peak_rise_m", peak_z - launch_z, lo=.10, hi=.35, frame=apex_i)
        near_apex = [i for i in range(start, end + 1) if peak_z - frames[i]["pelvis"][2] <= .015]
        dwell = _longest_consecutive_run(near_apex) / fps
        record("final_jump_apex_dwell_s", dwell, hi=.16, frame=apex_i)
        time_to_apex = max(0, apex_i - start)
        record("final_jump_apex_fraction", time_to_apex / max(1, end - start), lo=.25, hi=.75,
               frame=apex_i)
        ground_start = max(plank[1] + 1, start - round(.40 * fps))
        grounded = [i for i in range(ground_start, start)
                    if frames[i]["ankle_l"][2] <= .095 and frames[i]["ankle_r"][2] <= .095]
        takeoff_i = grounded[-1] if grounded else None
        if takeoff_i is None:
            record("final_jump_takeoff_knee_min_deg", None, lo=155)
        else:
            takeoff_angles = [_knee_metrics(frames[takeoff_i], side)[0] for side in SIDES]
            record("final_jump_takeoff_knee_min_deg", min(takeoff_angles), lo=155,
                   frame=takeoff_i)
        points = [(i / fps, frames[i]["pelvis"][2]) for i in range(start, end + 1)]
        origin = points[0][0] if points else 0.0
        fit = _fit_quadratic([(t - origin, z) for t, z in points])
        gravity = -2 * fit[0] if fit else None
        rmse = fit[3] if fit else None
        record("final_jump_fit_frame_count", len(points), lo=3)
        record("final_jump_gravity_fit_m_s2", gravity, lo=6.0, hi=13.5)
        record("final_jump_fit_rmse_m", rmse, hi=.035)

    return {"metrics": metrics, "failures": failures}
