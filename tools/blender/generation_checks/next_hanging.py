"""Movement-specific source trajectory checks for three hanging-core clips.

These authored-variant screens are not human form approval. In particular,
bar/bench clearances use analytic proxies and still need native mesh review.
"""
import math


IDS = ("toes-to-bar", "heel-hook", "dragon-flag-negative")
TRACKED = ("pelvis", "chest", "pelvis_up", "chest_up", "hip_l", "hip_r",
           "knee_l", "knee_r", "ankle_l", "ankle_r", "wrist_l", "wrist_r")
SECTIONS = ((-.10,.12,.095),(0,.155,.10),(.09,.145,.095),(.20,.135,.09),
            (.29,.15,.10),(.39,.175,.112),(.45,.185,.12),(.48,.185,.11),
            (.51,.16,.095),(.57,.055,.055))


def _dist(a, b):
    return math.sqrt(sum((float(x)-float(y))**2 for x, y in zip(a, b)))


def _angle(a, b, c):
    u = [a[i]-b[i] for i in range(3)]
    v = [c[i]-b[i] for i in range(3)]
    den = _dist(u, (0,0,0))*_dist(v, (0,0,0))
    if den <= 1e-9:
        return float("nan")
    return math.degrees(math.acos(max(-1., min(1., sum(u[i]*v[i] for i in range(3))/den))))


def _metric(metrics, failures, name, value, minimum=None, maximum=None, frame=None):
    finite = isinstance(value, (int, float)) and math.isfinite(value)
    ok = finite and (minimum is None or value >= minimum) and (maximum is None or value <= maximum)
    shown = round(float(value), 6) if finite else None
    metrics[name] = {"value": shown, "min": minimum, "max": maximum, "ok": bool(ok)}
    if not ok:
        failure = {"check": name, "value": shown}
        if frame is not None:
            failure["frame"] = int(frame)
        failures.append(failure)


def _foot_segment(frame, side):
    ankle, knee, toe = (frame[k+"_"+side] for k in ("ankle", "knee", "toe"))
    direction = [toe[i]-ankle[i] for i in range(3)]
    length = _dist(direction, (0,0,0))
    if length <= 1e-9:
        return None
    direction = [x/length for x in direction]
    away = [ankle[i]-knee[i] for i in range(3)]
    along = sum(away[i]*direction[i] for i in range(3))
    normal = [away[i]-along*direction[i] for i in range(3)]
    nl = _dist(normal, (0,0,0))
    normal = [x/nl for x in normal] if nl > 1e-8 else [0.,0.,-1.]
    heel = [ankle[i]-.045*direction[i]+.040*normal[i] for i in range(3)]
    tip = [ankle[i]+.165*direction[i]+.045*normal[i] for i in range(3)]
    return heel, tip


def _foot_bar_gap(frame, side, bar):
    """Taper-aware foot capsule gap to the finite Y-axis bar."""
    segment = _foot_segment(frame, side)
    if segment is None:
        return float("nan")
    heel, tip = segment
    x, half, z = bar.get("x", 0.0), bar["half"], bar["z"]
    delta = [tip[i]-heel[i] for i in range(3)]

    def gap(s):
        point = [heel[i]+s*delta[i] for i in range(3)]
        nearest = (x, max(-half, min(half, point[1])), z)
        foot_radius = .036 - .010*s  # render.py: capsule(foot, ..., .036, .026)
        return _dist(point, nearest)-bar["r"]-foot_radius

    # The radius tapers toward the toe, so minimize the signed surface gap over
    # the centerline rather than assuming the fixed-radius closest point wins.
    lo, hi = 0.0, 1.0
    ratio = (math.sqrt(5.0)-1.0)/2.0
    a, b = hi-ratio*(hi-lo), lo+ratio*(hi-lo)
    ga, gb = gap(a), gap(b)
    for _ in range(32):
        if ga <= gb:
            hi, b, gb = b, a, ga
            a = hi-ratio*(hi-lo); ga = gap(a)
        else:
            lo, a, ga = a, b, gb
            b = lo+ratio*(hi-lo); gb = gap(b)
    return min(gap(0.0), gap(1.0), ga, gb)


def _max_knee_speed(frames, fps):
    return max((_dist(a["knee_"+s], b["knee_"+s])*fps
                for a, b in zip(frames, frames[1:]) for s in ("l", "r")), default=float("nan"))


def _bench_margin(frame):
    """Sample renderer torso section rings above the bench footprint."""
    p, spine = frame["pelvis"], frame["spine"]
    u1, u2 = frame["pelvis_up"], frame["chest_up"]
    l1, l2 = frame["pelvis_left"], frame["chest_left"]
    lowest = math.inf
    for length, width, depth in SECTIONS:
        if length <= .24:
            center = [p[i]+u1[i]*length for i in range(3)]
            up, left = u1, l1
        else:
            center = [spine[i]+u2[i]*(length-.24) for i in range(3)]
            up, left = u2, l2
        forward = [left[1]*up[2]-left[2]*up[1], left[2]*up[0]-left[0]*up[2],
                   left[0]*up[1]-left[1]*up[0]]
        norm = _dist(forward, (0,0,0))
        if norm <= 1e-9:
            return float("nan")
        forward = [x/norm for x in forward]
        for j in range(64):
            theta = 2*math.pi*j/64
            point = [center[k]+left[k]*width*math.cos(theta)+forward[k]*depth*math.sin(theta)
                     for k in range(3)]
            if -.55 <= point[0] <= .4 and -.325 <= point[1] <= .325:
                lowest = min(lowest, point[2])
    return lowest-.55 if math.isfinite(lowest) else float("nan")


def _common(data, metrics, failures):
    frames = data.get("frames") if isinstance(data, dict) else None
    exercise = data.get("id") if isinstance(data, dict) else None
    _metric(metrics, failures, "known_variant", 1 if exercise in IDS else 0, 1, 1)
    if not isinstance(frames, list) or len(frames) < 4:
        _metric(metrics, failures, "valid_timeline", 0, 1, 1)
        return None, None
    _metric(metrics, failures, "valid_timeline", 1, 1, 1)
    fps = data.get("fps")
    if not isinstance(fps, (int, float)) or not math.isfinite(fps) or fps <= 0:
        _metric(metrics, failures, "valid_fps", 0, 1, 1)
        return None, None
    _metric(metrics, failures, "valid_fps", 1, 1, 1)
    valid = all(isinstance(f, dict) and all(name in f and isinstance(f[name], (list, tuple))
                  and len(f[name]) == 3 and all(isinstance(v, (int,float)) and math.isfinite(v)
                  for v in f[name]) for name in TRACKED) for f in frames)
    _metric(metrics, failures, "finite_required_geometry", int(valid), 1, 1)
    return (frames, float(fps)) if valid else (None, None)


def audit(data):
    """Return ``{"metrics": ..., "failures": ...}`` for the three scoped IDs."""
    metrics, failures = {}, []
    frames, fps = _common(data, metrics, failures)
    if frames is None:
        return {"metrics": metrics, "failures": failures}
    exercise = data["id"]
    duration = len(frames)/fps
    _metric(metrics, failures, "duration_s", duration, 0., 15.)
    speed = _max_knee_speed(frames, fps)
    _metric(metrics, failures, "max_knee_speed_m_s", speed, 0., 4.)
    wrists = {}
    for side in ("l", "r"):
        name = "wrist_"+side
        wrists[side] = max(_dist(frames[0][name], f[name]) for f in frames)
        _metric(metrics, failures, name+"_drift_m", wrists[side], 0., .005)

    if exercise == "toes-to-bar":
        bars = [p for p in data.get("props", []) if p.get("kind") == "bar"]
        bar = bars[0] if bars else None
        _metric(metrics, failures, "bar_prop_present", int(bar is not None), 1, 1)
        if bar is None:
            return {"metrics": metrics, "failures": failures}
        all_gaps = [_foot_bar_gap(frame, side, bar) for frame in frames for side in ("l", "r")]
        _metric(metrics, failures, "minimum_foot_bar_gap_m", min(all_gaps), -.002, None,
                min(range(len(all_gaps)), key=all_gaps.__getitem__)//2)
        mean_z = [(f["ankle_l"][2]+f["ankle_r"][2])*.5 for f in frames]
        peak = max(mean_z)
        top_indices = [i for i, z in enumerate(mean_z) if z >= peak-.03]
        contacts, extensions = [], []
        for side in ("l", "r"):
            gaps = [_foot_bar_gap(frames[i], side, bar) for i in top_indices]
            contact_indices = [i for i, gap in zip(top_indices, gaps) if abs(gap) <= .006]
            _metric(metrics, failures, "{}_top_foot_bar_gap_m".format(side),
                    min(abs(g) for g in gaps), 0., .006, top_indices[gaps.index(min(gaps,key=abs))])
            _metric(metrics, failures, "{}_bar_contact".format(side), int(bool(contact_indices)), 1, 1)
            contacts.extend(contact_indices)
            ext = min(_angle(frames[i]["hip_"+side], frames[i]["knee_"+side], frames[i]["ankle_"+side])
                      for i in contact_indices) if contact_indices else float("nan")
            extensions.append(ext)
            _metric(metrics, failures, side+"_knee_extension_at_contact_deg", ext, 165., 180.)
        _metric(metrics, failures, "both_feet_contact_overlap_frames",
                sum(1 for i in set(contacts) if all(abs(_foot_bar_gap(frames[i], s, bar)) <= .006
                                                    for s in ("l","r"))), 1, None)

    elif exercise == "heel-hook":
        bars = [p for p in data.get("props", []) if p.get("kind") == "bar"]
        bar = bars[0] if bars else None
        _metric(metrics, failures, "bar_prop_present", int(bar is not None), 1, 1)
        if bar is None:
            return {"metrics": metrics, "failures": failures}
        all_gaps = [_foot_bar_gap(frame, side, bar) for frame in frames for side in ("l", "r")]
        _metric(metrics, failures, "minimum_foot_bar_gap_m", min(all_gaps), -.002, None,
                min(range(len(all_gaps)), key=all_gaps.__getitem__)//2)
        initial_order = frames[0]["ankle_l"][1] > frames[0]["ankle_r"][1]
        crossing = [i for i, f in enumerate(frames)
                    if ((f["ankle_l"][1] < f["ankle_r"][1]) == initial_order
                        and min(f["ankle_l"][2], f["ankle_r"][2]) >= bar["z"]+bar["r"])]
        _metric(metrics, failures, "both_ankles_crossed_above_bar_frames", len(crossing), 1, None)
        for side in ("l", "r"):
            gap = min(abs(_foot_bar_gap(frames[i], side, bar)) for i in crossing) if crossing else float("nan")
            idx = min(crossing, key=lambda i: abs(_foot_bar_gap(frames[i], side, bar))) if crossing else None
            _metric(metrics, failures, side+"_hook_foot_gap_m", gap, 0., .006, idx)

    elif exercise == "dragon-flag-negative":
        angles = [_angle(f["hip_l"], f["knee_l"], f["ankle_l"]) for f in frames]
        tuck_start = next((i for i, value in enumerate(angles) if value < 160.), len(frames))
        pre_tuck = frames[:tuck_start]
        _metric(metrics, failures, "straight_negative_frames", len(pre_tuck), 2, None)
        trunk_angles = [math.degrees(math.acos(max(-1., min(1., sum(
            a*b for a,b in zip(f["pelvis_up"], f["chest_up"])))))) for f in pre_tuck]
        _metric(metrics, failures, "negative_trunk_alignment_max_deg",
                max(trunk_angles) if trunk_angles else float("nan"), 0., 8.)
        _metric(metrics, failures, "negative_knee_extension_min_deg",
                min(angles[:tuck_start]) if tuck_start else float("nan"), 165., 180.)
        later = angles[tuck_start:]
        _metric(metrics, failures, "recovery_tuck_knee_angle_min_deg",
                min(later) if later else float("nan"), 0., 140.)
        _metric(metrics, failures, "recovery_final_knee_extension_deg", angles[-1], 165., 180., len(frames)-1)
        margin = [_bench_margin(f) for f in frames]
        min_margin = min(margin) if margin and all(math.isfinite(v) for v in margin) else float("nan")
        _metric(metrics, failures, "torso_bench_proxy_clearance_m", min_margin, .005, None,
                margin.index(min_margin) if math.isfinite(min_margin) else None)

    return {"metrics": metrics, "failures": failures}
