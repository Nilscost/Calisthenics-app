"""Authored-variant checks for the complete burpee push-up cycle.

Passing these source-trajectory checks is not exercise-form approval.
"""
import math


TRACKED = (
    "pelvis", "chest", "head", "shoulder_l", "shoulder_r",
    "elbow_l", "elbow_r", "wrist_l", "wrist_r",
    "knee_l", "knee_r", "ankle_l", "ankle_r", "toe_l", "toe_r",
)
REQUIRED = TRACKED


def _dist(a, b):
    return math.sqrt(sum((x-y)**2 for x, y in zip(a, b)))


def _mean(values):
    return sum(values) / len(values) if values else float("nan")


def _foot_capsule_min_z(frame, side, rings=7):
    """Analytic lowest mesh point from render.foot_points + render.capsule.

    Uses the renderer's heel/tip offsets and tapered capsule radii. The
    continuous ring minimum is a conservative bound on its 28-sided mesh.
    """
    ankle, knee, toe = (frame[name + "_" + side] for name in ("ankle", "knee", "toe"))
    fd = [toe[i] - ankle[i] for i in range(3)]
    fl = math.sqrt(sum(x*x for x in fd))
    if fl <= 1e-8: return float("nan")
    fd = [x/fl for x in fd]
    away = [ankle[i] - knee[i] for i in range(3)]
    along = sum(away[i] * fd[i] for i in range(3))
    normal = [away[i] - fd[i] * along for i in range(3)]
    nl = math.sqrt(sum(x*x for x in normal))
    normal = [x/nl for x in normal] if nl > 1e-6 else [0,0,-1]
    heel = [ankle[i] - fd[i]*.045 + normal[i]*.040 for i in range(3)]
    tip = [ankle[i] + fd[i]*.165 + normal[i]*.045 for i in range(3)]
    axis = [tip[i] - heel[i] for i in range(3)]
    length = math.sqrt(sum(x*x for x in axis))
    if length <= 1e-8: return float("nan")
    axis = [x/length for x in axis]
    radial_z = math.sqrt(max(0., 1. - axis[2]*axis[2]))
    loops = [(-.036*math.sin(math.pi/2*i/rings), .036*math.cos(math.pi/2*i/rings))
             for i in range(rings, -1, -1)]
    loops += [(length + .026*math.sin(math.pi/2*i/rings), .026*math.cos(math.pi/2*i/rings))
              for i in range(0, rings + 1)]
    return min(heel[2] + axis[2]*z - radius*radial_z for z, radius in loops)


def _plank_indices(frames):
    """Find the planted push-up interval from stable ground contacts."""
    from generation_checks.burpee_articulation import _plank_mask
    return [i for i, planted in enumerate(_plank_mask(frames)) if planted]


def audit(data):
    """Return ``{metrics, failures}`` for a built pose JSON document."""
    metrics, failures = {}, []

    def record(name, value, lo=None, hi=None, frame=None):
        valid = isinstance(value, (int, float)) and math.isfinite(value)
        ok = valid and (lo is None or value >= lo) and (hi is None or value <= hi)
        shown = round(float(value), 6) if valid else None
        metrics[name] = {"value": shown, "min": lo, "max": hi, "ok": bool(ok)}
        if not ok:
            failure = {"check": name, "value": shown}
            if frame is not None:
                failure["frame"] = int(frame)
            failures.append(failure)

    if data.get("id") != "burpee-pushup":
        record("exercise_id", 1 if data.get("id") == "burpee-pushup" else 0, lo=1, hi=1)
    frames = data.get("frames")
    fps = data.get("fps", 0)
    if not isinstance(frames, list) or len(frames) < 4 or not isinstance(fps, (int, float)) or fps <= 0:
        record("valid_timeline", 0, lo=1, hi=1)
        return {"metrics": metrics, "failures": failures}
    record("valid_timeline", 1, lo=1, hi=1)

    finite = all(all(name in f and isinstance(f[name], (list, tuple)) and len(f[name]) == 3
                     for name in REQUIRED) for f in frames) and all(isinstance(v, (list, tuple)) and
                 all(isinstance(x, (int, float)) and math.isfinite(x) for x in v)
                 for f in frames for v in f.values())
    record("finite_geometry", 1 if finite else 0, lo=1, hi=1)
    if not finite:
        return {"metrics": metrics, "failures": failures}

    foot_extrema = [(_foot_capsule_min_z(fr, side), i, side)
                    for i, fr in enumerate(frames) for side in ("l", "r")]
    foot_min, foot_frame, foot_side = min(foot_extrema, key=lambda x: x[0])
    record("foot_mesh_floor_min_z_m", foot_min, lo=-.002, frame=foot_frame)

    duration = len(frames) / fps
    record("duration_s", duration, lo=8.0)
    first, last = frames[0], frames[-1]
    seam_names = [n for n in TRACKED if n in first and n in last]
    seam = max((_dist(first[n], last[n]) for n in seam_names), default=float("nan"))
    record("loop_seam_max_m", seam, hi=.08)

    speed = max((_dist(frames[i][name], frames[i-1][name]) * fps
                 for i in range(1, len(frames)) for name in TRACKED
                 if name in frames[i] and name in frames[i-1]), default=float("nan"))
    # Arm drive in the short vertical jump is faster than controlled floor
    # phases. Knee transitions and jump physics have separate scoped guards.
    record("max_joint_speed_m_s", speed, hi=8.0)

    plank = _plank_indices(frames)
    record("pushup_plank_frames", len(plank), lo=12)
    if len(plank) < 3:
        for name, lo, hi in (("pushup_chest_excursion_m", .12, None),
                             ("pushup_press_rise_m", .12, None),
                             ("pushup_phase_order", 1, 1),
                             ("plank_contact_drift_m", None, .003)):
            record(name, float("nan"), lo=lo, hi=hi)
    else:
        chest = [frames[i]["chest"][2] for i in plank]
        split = max(1, len(plank) // 3)
        first_high = max(range(split), key=lambda j: chest[j])
        low_local = min(range(len(plank)), key=lambda j: chest[j])
        last_start = len(plank) - split
        last_high = max(range(last_start, len(plank)), key=lambda j: chest[j])
        low = chest[low_local]
        excursion = min(chest[first_high], chest[last_high]) - low
        rise = chest[last_high] - low
        def elbow_angle(fr, side):
            a, b, c = (fr[k + "_" + side] for k in ("shoulder", "elbow", "wrist"))
            u, v = [a[j]-b[j] for j in range(3)], [c[j]-b[j] for j in range(3)]
            den = _dist(u, [0,0,0]) * _dist(v, [0,0,0])
            if den <= 1e-10:
                return float("nan")
            cosv = sum(x*y for x, y in zip(u, v)) / den
            return math.degrees(math.acos(max(-1.0, min(1.0, cosv))))
        first_i, low_i, last_i = plank[first_high], plank[low_local], plank[last_high]
        top_elbow = min(elbow_angle(frames[i], side) for i in (first_i,last_i) for side in ("l","r"))
        bottom_bend = max(180-elbow_angle(frames[low_i], side) for side in ("l","r"))
        phase_order = int(first_high < low_local < last_high and excursion >= .12 and rise >= .12
                          and top_elbow >= 145 and bottom_bend >= 30)
        record("pushup_chest_excursion_m", max(chest) - low, lo=.12)
        record("pushup_press_rise_m", rise, lo=.12)
        record("pushup_top_elbow_deg", top_elbow, lo=145)
        record("pushup_bottom_elbow_bend_deg", bottom_bend, lo=30)
        record("pushup_phase_order", phase_order, lo=1, hi=1, frame=plank[low_local])

        contact_points = ("wrist_l", "wrist_r", "ankle_l", "ankle_r", "toe_l", "toe_r")
        drift = max((_dist(frames[i][name], frames[plank[0]][name])
                     for i in plank for name in contact_points), default=float("nan"))
        record("plank_contact_drift_m", drift, hi=.003)
        transfer = max(abs(frames[i]["ankle_l"][0] - frames[0]["ankle_l"][0])
                       for i in range(len(frames)))
        transfer = max(transfer, max(abs(frames[i]["ankle_r"][0] - frames[0]["ankle_r"][0])
                                     for i in range(len(frames)))
                       )
        record("leg_transfer_m", transfer, lo=.65)

    ankle_mean = [(_mean([f["ankle_l"][2], f["ankle_r"][2]])) for f in frames]
    apex = max(range(len(frames)), key=lambda i: ankle_mean[i])
    record("jump_height_m", ankle_mean[apex] - .08, lo=.18, frame=apex)
    # Only inspect the immediate loading interval before flight; earlier
    # crouching on the way down into the push-up cannot satisfy this check.
    window = max(1, int(round(.5 * fps)))
    preload_start = max(0, apex - window)
    standing_z = (frames[0]["pelvis"][2] + frames[-1]["pelvis"][2]) / 2
    preload_z = min(f["pelvis"][2] for f in frames[preload_start:apex]) if apex > preload_start else float("nan")
    record("jump_preload_depth_m", standing_z - preload_z, lo=.10,
           frame=preload_start + min(range(max(1, apex-preload_start)),
                                     key=lambda j: frames[preload_start+j]["pelvis"][2]))

    landings = [i for i in range(apex + 1, len(frames))
                if frames[i]["ankle_l"][2] <= .105 and frames[i]["ankle_r"][2] <= .105
                and frames[i]["pelvis"][2] <= standing_z - .08]
    record("landing_contact", int(bool(landings)), lo=1, hi=1, frame=landings[0] if landings else None)
    if landings:
        landing = landings[0]
        record("landing_absorption_m", standing_z - frames[landing]["pelvis"][2], lo=.08, frame=landing)
        recovery = _dist(frames[-1]["pelvis"], first["pelvis"])
        record("recovery_to_start_m", recovery, hi=.08)
    else:
        record("landing_absorption_m", float("nan"), lo=.08)
        record("recovery_to_start_m", float("nan"), hi=.08)

    # Keep anatomical knee screening in the normal generation/install audit,
    # rather than relying on a separate optional review command.
    from generation_checks.burpee_articulation import audit as articulation_audit
    articulation = articulation_audit(data)
    metrics.update(articulation["metrics"])
    failures.extend(articulation["failures"])
    from generation_checks.burpee_feet import audit as feet_audit
    feet = feet_audit(data)
    metrics.update(feet["metrics"])
    failures.extend(feet["failures"])

    return {"metrics": metrics, "failures": failures}
