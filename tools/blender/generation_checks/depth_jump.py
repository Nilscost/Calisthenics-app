"""Authored-variant contact and ballistic checks for the depth-jump candidate.

These checks screen generated motion data; passing them is not form approval.
"""
import math


TRACKED = ("pelvis", "chest", "head", "wrist_l", "wrist_r", "ankle_l", "ankle_r")
REQUIRED = TRACKED + ("shoulder_l", "shoulder_r", "elbow_l", "elbow_r", "hand_l", "hand_r",
                     "hip_l", "hip_r", "knee_l", "knee_r")
FLOOR_Z = .08
# Ankles are landmark proxies, not rendered sole vertices; use a narrow band
# and retain the resulting one-sample event-time uncertainty in the report.
FLOOR_TOL = .02


def _dist(a, b): return math.sqrt(sum((x-y)**2 for x, y in zip(a, b)))


def _angle(a, b, c):
    u, v = [x-y for x, y in zip(a, b)], [x-y for x, y in zip(c, b)]
    nu = math.sqrt(sum(x*x for x in u)); nv = math.sqrt(sum(x*x for x in v))
    if nu*nv <= 1e-10: return float("nan")
    cosine = max(-1., min(1., sum(x*y for x, y in zip(u, v))/(nu*nv)))
    return math.degrees(math.acos(cosine))


def _longest_run(indices):
    best = run = 0; previous = None
    for i in indices:
        run = run + 1 if previous is not None and i == previous + 1 else 1
        best = max(best, run); previous = i
    return best


def _quadratic(times, values):
    """Least-squares c + v*t + .5*a*t^2; return acceleration and RMS residual."""
    if len(times) < 4 or len(times) != len(values): return float("nan"), float("nan")
    centre = (times[0] + times[-1]) * .5
    rows = [[1., t-centre, (t-centre)**2] for t in times]
    normal = [[sum(r[i]*r[j] for r in rows) for j in range(3)] for i in range(3)]
    rhs = [sum(r[i]*y for r, y in zip(rows, values)) for i in range(3)]
    # Small 3x3 Gaussian elimination with pivoting, stdlib only.
    aug = [normal[i] + [rhs[i]] for i in range(3)]
    for col in range(3):
        pivot = max(range(col, 3), key=lambda r: abs(aug[r][col]))
        if abs(aug[pivot][col]) < 1e-12: return float("nan"), float("nan")
        aug[col], aug[pivot] = aug[pivot], aug[col]
        scale = aug[col][col]; aug[col] = [x/scale for x in aug[col]]
        for row in range(3):
            if row == col: continue
            factor = aug[row][col]
            aug[row] = [x-factor*y for x, y in zip(aug[row], aug[col])]
    c, v, c2 = (aug[i][3] for i in range(3))
    residual = math.sqrt(sum((y-(c+v*(t-centre)+c2*(t-centre)**2))**2 for t, y in zip(times, values))/len(values))
    return 2*c2, residual


def _contact_flags(data):
    box = next((p for p in data.get("props", []) if p.get("kind") == "box"), None)
    if not box: return None, None
    cx, cy, cz = box["center"]; sx, sy, sz = box["size"]; top = cz + sz/2 + FLOOR_Z
    def floor(fr, side): return abs(fr["ankle_"+side][2] - FLOOR_Z) <= FLOOR_TOL
    def on_box(fr, side):
        a = fr["ankle_"+side]
        return abs(a[2]-top) <= FLOOR_TOL and abs(a[0]-cx) <= sx/2 and abs(a[1]-cy) <= sy/2
    return floor, on_box


def _phase_indices(frames, floor, on_box):
    n = len(frames)
    both_box = [floor(fr, "l") is False and floor(fr, "r") is False and on_box(fr, "l") and on_box(fr, "r") for fr in frames]
    box_start = _longest_run(i for i, yes in enumerate(both_box[:max(1, n//3)]) if yes)
    first_box_end = next((i for i, yes in enumerate(both_box) if not yes), None)
    box_clear = next((i for i in range(first_box_end or 0, n)
                      if not on_box(frames[i], "l") and not on_box(frames[i], "r")), None)
    mixed = []
    if box_clear is not None:
        for i in range(box_clear):
            f = frames[i]
            if ((floor(f, "l") and on_box(f, "r")) or (floor(f, "r") and on_box(f, "l"))): mixed.append(i)
    first_touch = next((i for i in range(box_clear or 0, n)
                        if floor(frames[i], "l") or floor(frames[i], "r")), None)
    first_bilateral = next((i for i in range(first_touch or 0, n)
                            if floor(frames[i], "l") and floor(frames[i], "r")), None)
    first_takeoff = None
    if first_bilateral is not None:
        first_takeoff = next((i for i in range(first_bilateral+1, n)
                              if not floor(frames[i], "l") and not floor(frames[i], "r")), None)
    second_touch = None
    if first_takeoff is not None:
        second_touch = next((i for i in range(first_takeoff+1, n)
                             if floor(frames[i], "l") and floor(frames[i], "r")), None)
    both_air = []
    if box_clear is not None:
        stop = first_touch if first_touch is not None else n
        both_air = [i for i in range(box_clear, stop)
                    if not floor(frames[i], "l") and not floor(frames[i], "r")
                    and not on_box(frames[i], "l") and not on_box(frames[i], "r")]
    return dict(both_box=both_box, box_start=box_start, first_box_end=first_box_end,
                box_clear=box_clear, mixed=mixed, first_touch=first_touch,
                first_bilateral=first_bilateral, first_takeoff=first_takeoff,
                second_touch=second_touch, both_air=both_air)


def audit(data):
    """Return ``{metrics, failures}`` for the built depth-jump pose document."""
    metrics, failures = {}, []

    def record(name, value, lo=None, hi=None, frame=None):
        valid = isinstance(value, (int, float)) and math.isfinite(value)
        ok = valid and (lo is None or value >= lo) and (hi is None or value <= hi)
        shown = round(float(value), 6) if valid else None
        metrics[name] = {"value": shown, "min": lo, "max": hi, "ok": bool(ok)}
        if not ok:
            failure = {"check": name, "value": shown}
            if frame is not None: failure["frame"] = int(frame)
            failures.append(failure)

    record("exercise_id", int(data.get("id") == "depth-jump"), 1, 1)
    frames, fps = data.get("frames"), data.get("fps", 0)
    valid_timeline = isinstance(frames, list) and len(frames) >= 16 and isinstance(fps, (int, float)) and fps > 0
    record("valid_timeline", int(valid_timeline), 1, 1)
    if not valid_timeline: return {"metrics": metrics, "failures": failures}
    finite = all(all(name in f and isinstance(f[name], (list, tuple)) and len(f[name]) == 3
                     and all(isinstance(x, (int,float)) and math.isfinite(x) for x in f[name]) for name in REQUIRED)
                 for f in frames)
    record("finite_geometry", int(finite), 1, 1)
    if not finite: return {"metrics": metrics, "failures": failures}
    duration = len(frames)/fps
    record("duration_s", duration, 7.5, 9.0)
    floor, on_box = _contact_flags(data)
    record("box_prop_present", int(floor is not None), 1, 1)
    if floor is None: return {"metrics": metrics, "failures": failures}
    phase = _phase_indices(frames, floor, on_box); n = len(frames)
    record("initial_both_box_frames", phase["box_start"], 3, None)
    record("early_floor_while_box_supported_frames", len(phase["mixed"]), 0, 0)
    box_clear, first_touch = phase["box_clear"], phase["first_touch"]
    first_bilateral, first_takeoff, second_touch = (phase["first_bilateral"], phase["first_takeoff"], phase["second_touch"])
    if box_clear is not None and first_touch is not None:
        drop_duration = (first_touch-box_clear)/fps
        record("box_drop_flight_duration_s", drop_duration, .18, .55, box_clear)
        record("box_drop_bilateral_air_frames", len(phase["both_air"]), 3, 15)
        z0 = frames[box_clear]["pelvis"][2]
        peak = max((frames[i]["pelvis"][2], i) for i in range(box_clear, first_touch+1))
        record("step_departure_pelvis_rise_m", peak[0]-z0, None, .07, peak[1])
        if first_touch-box_clear >= 5:
            # Include box-clear as the first free-flight sample; omit the first
            # floor-band sample to avoid contact interpolation contaminating fit.
            fit_lo, fit_hi = box_clear, first_touch
            accel, residual = _quadratic([i/fps for i in range(fit_lo, fit_hi)],
                                         [frames[i]["pelvis"][2] for i in range(fit_lo, fit_hi)])
        else: accel, residual = float("nan"), float("nan")
        record("box_drop_gravity_m_s2", -accel, 5.0, 15.0)
        record("box_drop_parabola_rmse_m", residual, None, .065)
    else:
        for name, lo, hi in (("box_drop_flight_duration_s", .18, .55), ("box_drop_bilateral_air_frames", 3, 15),
                             ("step_departure_pelvis_rise_m", None, .07), ("box_drop_gravity_m_s2", 5., 15.),
                             ("box_drop_parabola_rmse_m", None, .065)):
            record(name, float("nan"), lo, hi)
    record("first_touch_pairing_frames", ((first_bilateral-first_touch) if first_touch is not None and first_bilateral is not None else float("nan")), 0, 3)
    # `first_takeoff` is the first airborne sample; the previous sample is the
    # last planted launch pose. Time is measured between event samples, not by
    # counting both endpoints as whole frames.
    launch = first_takeoff-1 if first_takeoff is not None else None
    contact_span = launch-first_bilateral if launch is not None and first_bilateral is not None else None
    record("first_landing_contact_duration_s", contact_span/fps if contact_span is not None else float("nan"), .12, .42)
    if first_bilateral is not None and first_takeoff is not None:
        planted = [i for i in range(first_bilateral, first_takeoff)
                   if floor(frames[i], "l") and floor(frames[i], "r")]
        drift = max((_dist(frames[i]["ankle_"+s], frames[first_bilateral]["ankle_"+s])
                     for i in planted for s in ("l","r")), default=float("nan"))
        record("first_landing_planted_slip_m", drift, None, .003)
        low_z, low_i = min((frames[i]["pelvis"][2], i) for i in range(first_bilateral, first_takeoff))
        record("first_landing_compression_m", frames[first_bilateral]["pelvis"][2]-low_z, .015, .24, low_i)
    else:
        record("first_landing_planted_slip_m", float("nan"), None, .003)
        record("first_landing_compression_m", float("nan"), .015, .24)

    if launch is not None and first_bilateral is not None:
        launch_frame = frames[launch]
        knee_angles = {
            s: [_angle(frames[i]["hip_"+s], frames[i]["knee_"+s], frames[i]["ankle_"+s])
                for i in range(first_bilateral, launch+1)]
            for s in ("l", "r")
        }
        launch_knee_min = min(values[-1] for values in knee_angles.values())
        extension_gain = min(values[-1]-min(values) for values in knee_angles.values())
        record("takeoff_knee_min_deg", launch_knee_min, 155., None, launch)
        record("takeoff_knee_extension_gain_deg", extension_gain, 20., None, launch)

        # Compare pelvis proxy finite differences over the last planted and
        # first airborne intervals. This catches a launch dip or velocity pop
        # at the contact boundary without imposing a universal acceleration.
        before = frames[launch-1]["pelvis"]
        at = frames[launch]["pelvis"]
        after = frames[launch+1]["pelvis"]
        v_before = [(at[k]-before[k])*fps for k in range(3)]
        v_after = [(after[k]-at[k])*fps for k in range(3)]
        boundary_delta = _dist(v_before, v_after)
        step_delta = _dist([at[k]-before[k] for k in range(3)],
                           [after[k]-at[k] for k in range(3)])
        record("takeoff_boundary_velocity_delta_m_s", boundary_delta, None, 1.5, launch)
        record("takeoff_boundary_step_delta_m", step_delta, None, .06, launch)
        record("takeoff_upward_boundary_min_velocity_m_s", min(v_before[2], v_after[2]), 0., None, launch)
    else:
        record("takeoff_knee_min_deg", float("nan"), 155., None)
        record("takeoff_knee_extension_gain_deg", float("nan"), 20., None)
        record("takeoff_boundary_velocity_delta_m_s", float("nan"), None, 1.5)
        record("takeoff_boundary_step_delta_m", float("nan"), None, .06)
        record("takeoff_upward_boundary_min_velocity_m_s", float("nan"), 0., None)

    if launch is not None and second_touch is not None and second_touch-launch >= 7:
        flight_s = (second_touch-launch)/fps
        apex_i = max(range(launch, second_touch+1), key=lambda i: frames[i]["pelvis"][2])
        # Use the last planted sample for both rise and flight timing, so the
        # rise-time check has the same launch endpoint as the parabola fit.
        rise = frames[apex_i]["pelvis"][2]-frames[launch]["pelvis"][2]
        record("rebound_flight_duration_s", flight_s, .38, .66)
        record("rebound_pelvis_rise_m", rise, .40, .55, apex_i)
        # Ignore two samples on either boundary to keep interpolated contact out of the fit.
        lo, hi = launch+2, second_touch-1
        accel, residual = _quadratic([i/fps for i in range(lo, hi)], [frames[i]["pelvis"][2] for i in range(lo, hi)])
        record("rebound_gravity_m_s2", -accel, 6.0, 13.5, apex_i)
        record("rebound_parabola_rmse_m", residual, None, .035, apex_i)
        g_from_rise_time = 8*rise/(flight_s*flight_s) if flight_s > 0 else float("nan")
        record("rebound_rise_time_gravity_m_s2", g_from_rise_time, 6.0, 13.5, apex_i)
        vel = [(frames[i+1]["pelvis"][2]-frames[i-1]["pelvis"][2])*fps/2
               for i in range(launch+1, second_touch)]
        near = [launch+1+j for j, v in enumerate(vel) if abs(v) < .10]
        dwell = _longest_run(near)
        record("rebound_apex_dwell_frames", dwell, 0, 2, apex_i)
        horiz = max((frames[i]["pelvis"][0] for i in range(launch, second_touch+1)))-min(frames[i]["pelvis"][0] for i in range(launch, second_touch+1))
        lateral = max((frames[i]["pelvis"][1] for i in range(launch, second_touch+1)))-min(frames[i]["pelvis"][1] for i in range(launch, second_touch+1))
        record("rebound_horizontal_range_m", horiz, None, .14)
        record("rebound_lateral_range_m", lateral, None, .12)
    else:
        apex_i = None
        for name, lo, hi in (("rebound_flight_duration_s", .38, .66), ("rebound_pelvis_rise_m", .40, .55),
                             ("rebound_gravity_m_s2", 6., 13.5), ("rebound_parabola_rmse_m", None, .035),
                             ("rebound_rise_time_gravity_m_s2", 6., 13.5), ("rebound_apex_dwell_frames", 0, 2),
                             ("rebound_horizontal_range_m", None, .14), ("rebound_lateral_range_m", None, .12)):
            record(name, float("nan"), lo, hi)
    record("second_landing_contact", int(second_touch is not None), 1, 1, second_touch)
    if second_touch is not None:
        double_support = [i for i in range(second_touch, n) if floor(frames[i], "l") and floor(frames[i], "r")]
        record("second_landing_planted_frames", _longest_run(double_support), 2, None, second_touch)
        # Only inspect the initial soft-settle phase. Later walking return may
        # legitimately keep both feet on the floor while their positions move.
        planted = [i for i in double_support[:4] if floor(frames[i], "l") and floor(frames[i], "r")]
        slip = max((_dist(frames[i]["ankle_"+s], frames[second_touch]["ankle_"+s])
                    for i in planted for s in ("l", "r")), default=float("nan"))
        record("second_landing_planted_slip_m", slip, None, .003, second_touch)
    else:
        record("second_landing_planted_frames", float("nan"), 2, None)
        record("second_landing_planted_slip_m", float("nan"), None, .003)

    # Arm swing: start low or behind the shoulders, then reach overhead with extended elbows at takeoff.
    start = frames[0]
    down_or_back = []
    for s in ("l", "r"):
        sh, wr = start["shoulder_"+s], start["wrist_"+s]
        down_or_back.append(max(sh[2]-wr[2], sh[0]-wr[0]))
    record("arm_start_down_or_backswing_m", min(down_or_back), .08, None)
    if first_takeoff is not None:
        arm_takeoff = launch if launch is not None else first_takeoff
        take = frames[arm_takeoff]
        angles = [_angle(take["shoulder_"+s], take["elbow_"+s], take["wrist_"+s]) for s in ("l","r")]
        overhead = [take["wrist_"+s][2]-take["shoulder_"+s][2] for s in ("l","r")]
        record("arm_takeoff_elbow_min_deg", min(angles), 145, None, arm_takeoff)
        record("arm_takeoff_overhead_reach_m", min(overhead), .04, None, arm_takeoff)
        end = second_touch if second_touch is not None else min(n-1, arm_takeoff+20)
        excursions = []
        reaches = []
        for s in ("l","r"):
            values = [frames[i]["wrist_"+s][2]-frames[i]["shoulder_"+s][2] for i in range(0, end+1)]
            excursions.append(max(values)-min(values)); reaches.append(values)
        record("arm_swing_relative_height_range_m", min(excursions), .22, None)
        if apex_i is not None:
            phase_offsets = [abs(max(range(len(reaches[j])), key=lambda k: reaches[j][k])-apex_i)/fps for j in range(2)]
            record("arm_reach_apex_offset_s", max(phase_offsets), None, .32, apex_i)
        else: record("arm_reach_apex_offset_s", float("nan"), None, .32)
    else:
        for name, lo in (("arm_takeoff_elbow_min_deg",145),("arm_takeoff_overhead_reach_m",.04),
                         ("arm_swing_relative_height_range_m",.22)):
            record(name, float("nan"), lo, None)
        record("arm_reach_apex_offset_s", float("nan"), None, .32)

    # Treat hand_l/r as the hand-axis point used by the renderer. A fixed
    # world-oriented hand can create a sharp wrist bend as the forearm swings.
    wrist_bends = []
    for i, frame in enumerate(frames):
        for side in ("l", "r"):
            forearm = [frame["wrist_"+side][k]-frame["elbow_"+side][k] for k in range(3)]
            hand_axis = [frame["hand_"+side][k]-frame["wrist_"+side][k] for k in range(3)]
            nf = math.sqrt(sum(x*x for x in forearm)); nh = math.sqrt(sum(x*x for x in hand_axis))
            if nf*nh <= 1e-10:
                wrist_bends.append((float("inf"), i))
            else:
                cosine = max(-1., min(1., sum(a*b for a,b in zip(forearm, hand_axis))/(nf*nh)))
                wrist_bends.append((math.degrees(math.acos(cosine)), i))
    max_wrist_bend, wrist_frame = max(wrist_bends, default=(float("nan"), None))
    record("max_wrist_axis_bend_deg", max_wrist_bend, None, 45., wrist_frame)

    end_box = [i for i, f in enumerate(frames[max(0, int(.94*n)):], start=max(0, int(.94*n)))
               if on_box(f,"l") and on_box(f,"r")]
    record("return_both_feet_to_box_frames", len(end_box), 2, None)
    start_gap = max(_dist(frames[-1][name], frames[0][name]) for name in TRACKED)
    record("loop_return_joint_gap_m", start_gap, None, .10)
    from generation_checks.depth_return import audit as return_audit
    reset = return_audit(data)
    metrics.update(reset['metrics']); failures.extend(reset['failures'])
    return {"metrics": metrics, "failures": failures}
