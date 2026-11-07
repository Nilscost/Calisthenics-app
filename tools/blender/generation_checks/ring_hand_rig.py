"""Independent geometry screens for the rigid-phalanx ring hand model.

These bounds screen authored geometry and trajectory consistency. They do not
represent universal human anthropometry or certify exercise form.
"""
import math


def _v(value):
    return (isinstance(value, (list, tuple)) and len(value) == 3 and
            all(isinstance(x, (int, float)) and math.isfinite(x) for x in value))


def _distance(a, b):
    return math.sqrt(sum((x-y)**2 for x, y in zip(a, b)))


def audit(data):
    metrics, failures = {}, []

    def record(name, value, lo=None, hi=None):
        good = isinstance(value, (int, float)) and math.isfinite(value)
        passed = good and (lo is None or value >= lo) and (hi is None or value <= hi)
        metrics[name] = {"value": round(float(value), 6) if good else None,
                         "min": lo, "max": hi, "ok": bool(passed)}
        if not passed:
            failures.append({"check": name, "value": metrics[name]["value"]})

    frames = data.get("frames") if isinstance(data, dict) else None
    props = data.get("props", []) if isinstance(data, dict) else []
    if not isinstance(frames, list) or len(frames) < 2 or not isinstance(props, list):
        record("valid_timeline", 0, 1, 1)
        return {"metrics": metrics, "failures": failures}
    record("valid_timeline", 1, 1, 1)
    rings = [p for p in props if isinstance(p, dict) and p.get("kind") == "ring"]
    record("rigid_hand_models", sum(p.get("hand_model") == "rigid-phalanx-v1" for p in rings), 2, 2)

    # Import at call time to avoid cycles in source tools and unit tests.
    try:
        from ring_geometry import hand_segments, ring_frame, capsule_ring_clearance, palm_surface_clearance
        from hand_axes import axes
    except ImportError:
        record("validator_geometry_import", 0, 1, 1)
        return {"metrics": metrics, "failures": failures}

    for side in ("l", "r"):
        spec = next((p for p in rings if p.get("joint") == "wrist_" + side), None)
        if not spec:
            record("ring_" + side + "_spec_present", 0, 1, 1)
            continue
        record("ring_" + side + "_rigid_model_tag",
               int(spec.get("hand_model") == "rigid-phalanx-v1"), 1, 1)
        per_segment = [[] for _ in range(14)]
        joint_gaps, skin_clearances, hand_gaps = [], [], []
        frame_digit_gaps = [[] for _ in range(5)]
        phase_palm_gaps = {"hang": [], "support": []}
        mcp_local = [[] for _ in range(5)]
        data_valid = True
        for frame in frames:
            try:
                if not isinstance(frame, dict):
                    raise ValueError("frame must be an object")
                segments = hand_segments(frame, side, spec)
                if not isinstance(segments, (list, tuple)) or len(segments) != 14:
                    raise ValueError("hand_segments must return 14 segments")
                cooked = []
                for pair in segments:
                    if not isinstance(pair, (list, tuple)) or len(pair) != 2 or not all(_v(p) for p in pair):
                        raise ValueError("malformed hand segment")
                    cooked.append((list(pair[0]), list(pair[1])))
                lengths = [_distance(a, b) for a, b in cooked]
                for i, length in enumerate(lengths):
                    per_segment[i].append(length)
                # The 12 finger segments form four triples, followed by a
                # connected two-segment thumb. Test each internal joint.
                for first, count in ((0, 3), (3, 3), (6, 3), (9, 3), (12, 2)):
                    for j in range(first, first + count - 1):
                        joint_gaps.append(_distance(cooked[j][1], cooked[j+1][0]))
                u, palm_n, across = axes(frame["wrist_"+side], frame["hand_"+side],
                                         frame["palmn_"+side], side)
                for digit_index, first in enumerate((0, 3, 6, 9, 12)):
                    base = [cooked[first][0][i]-frame["wrist_"+side][i] for i in range(3)]
                    mcp_local[digit_index].append([sum(base[i]*axis[i] for i in range(3))
                                                    for axis in (u, palm_n, across)])
                for i, (a, b) in enumerate(cooked):
                    radius = .0105 if i >= 12 else .0088  # Renderer capsule radii; never scale down.
                    skin_clearances.append(capsule_ring_clearance(a, b, radius, radius, frame, spec))
                hand_gaps.append(palm_surface_clearance(frame, spec))
                # Proximity from actual capsule surface, not the declared wrist proxy.
                surface_distances = []
                for i, (a, b) in enumerate(cooked):
                    radius = .0105 if i >= 12 else .0088
                    # capsule_ring_clearance is the conservative signed skin gap.
                    surface_distances.append(capsule_ring_clearance(a, b, radius, radius, frame, spec))
                for digit_index, (first, count) in enumerate(((0, 3), (3, 3), (6, 3), (9, 3), (12, 2))):
                    # A fingertip may be close while the other four digits
                    # hover. Each digit must independently approach the hoop.
                    frame_digit_gaps[digit_index].append(min(surface_distances[first:first+count]))
                rel = frame["chest"][2] - ring_frame(frame, spec)[3][2]
                phase = "hang" if rel <= -.30 else ("support" if rel >= .45 else None)
                if phase:
                    phase_palm_gaps[phase].append(hand_gaps[-1])
            except (KeyError, TypeError, ValueError, ZeroDivisionError, OverflowError):
                data_valid = False
        variations = [max(v)-min(v) for v in per_segment if v]
        if len(variations) != 14 or any(len(v) != len(frames) for v in per_segment):
            data_valid = False
        if any(len(v) != len(frames) for v in frame_digit_gaps):
            data_valid = False
        if any(len(v) != len(frames) for v in mcp_local):
            data_valid = False
        record("ring_" + side + "_hand_data_valid", int(data_valid), 1, 1)
        max_var = max(variations) if len(variations) == 14 else None
        record("ring_" + side + "_segment_length_variation_max_m", max_var, hi=.0001)
        record("ring_" + side + "_joint_gap_max_m", max(joint_gaps) if joint_gaps else None, hi=.00001)

        mcp_ranges = []
        for digit, coordinates in zip(("index", "middle", "ring", "little", "thumb"), mcp_local):
            if coordinates:
                ranges = [max(row[axis] for row in coordinates)-min(row[axis] for row in coordinates)
                          for axis in range(3)]
                value = max(ranges)
                mcp_ranges.append(value)
                record("ring_" + side + "_" + digit + "_mcp_local_variation_max_m", value, hi=.0002)
            else:
                record("ring_" + side + "_" + digit + "_mcp_local_variation_max_m", None, hi=.0002)
        record("ring_" + side + "_mcp_local_variation_max_m",
               max(mcp_ranges) if len(mcp_ranges) == 5 else None, hi=.0002)

        if all(len(values) == len(frames) for values in mcp_local):
            # Broad palm-scale screens: MCPs should attach on a modest row in
            # front of and across the wrist, without prescribing exact digits.
            row_lengths = [row[0] for digit in mcp_local for row in digit]
            row_widths = [max(mcp_local[d][k][2] for d in range(5)) -
                          min(mcp_local[d][k][2] for d in range(5)) for k in range(len(frames))]
            depth = max(abs(row[1]) for digit in mcp_local for row in digit)
            record("ring_" + side + "_mcp_palm_length_min_m", min(row_lengths), lo=.020)
            record("ring_" + side + "_mcp_palm_length_max_m", max(row_lengths), hi=.120)
            record("ring_" + side + "_mcp_row_width_min_m", min(row_widths), lo=.035)
            record("ring_" + side + "_mcp_row_width_max_m", max(row_widths), hi=.100)
            record("ring_" + side + "_mcp_out_of_palm_max_m", depth, hi=.030)
        else:
            for metric, lo, hi in (("mcp_palm_length_min_m", .020, None),
                                   ("mcp_palm_length_max_m", None, .120),
                                   ("mcp_row_width_min_m", .035, None),
                                   ("mcp_row_width_max_m", None, .100),
                                   ("mcp_out_of_palm_max_m", None, .030)):
                record("ring_" + side + "_" + metric, None, lo=lo, hi=hi)

        # Enforce broad hand-scale bounds per digit and phalanx. Bounds permit
        # adult-size variation while rejecting oversized/deformed geometry.
        totals = []
        for first, count in ((0, 3), (3, 3), (6, 3), (9, 3), (12, 2)):
            complete = all(len(per_segment[i]) == len(frames) for i in range(first, first+count))
            vals = ([sum(per_segment[i][k] for i in range(first, first+count))
                     for k in range(len(frames))] if complete else [])
            totals.extend(vals)
            digit = "thumb" if first == 12 else "finger" + str(first // 3)
            record("ring_" + side + "_" + digit + "_length_min_m", min(vals) if vals else None,
                   lo=.035 if first == 12 else .055)
            record("ring_" + side + "_" + digit + "_length_max_m", max(vals) if vals else None,
                   hi=.105 if first == 12 else .145)
        phalanges = [x for group in per_segment for x in group]
        record("ring_" + side + "_phalanx_length_min_m", min(phalanges) if phalanges else None, lo=.010)
        record("ring_" + side + "_phalanx_length_max_m", max(phalanges) if phalanges else None, hi=.060)

        # Clearance uses the rendered capsule radii and sampled palm surface.
        # Both must avoid wood while the actual capsule skin remains close to it.
        min_capsule = min(skin_clearances) if skin_clearances else None
        min_palm = min(hand_gaps) if hand_gaps else None
        record("ring_" + side + "_capsule_wood_clearance_min_m", min_capsule, lo=.0005)
        record("ring_" + side + "_palm_surface_wood_clearance_min_m", min_palm, lo=.0004)
        min_skin = min(skin_clearances + hand_gaps) if skin_clearances and hand_gaps else None
        record("ring_" + side + "_skin_wood_clearance_min_m", min_skin, lo=.0005)
        digit_names = ("index", "middle", "ring", "little", "thumb")
        digit_maxima = []
        for digit, gaps in zip(digit_names, frame_digit_gaps):
            maximum = max(gaps) if gaps else None
            record("ring_" + side + "_" + digit + "_to_wood_gap_max_m",
                   maximum, lo=.0005, hi=.004)
            if maximum is not None:
                digit_maxima.append(maximum)
        record("ring_" + side + "_actual_finger_to_wood_gap_max_m",
               max(digit_maxima) if len(digit_maxima) == 5 else None, lo=.0005, hi=.004)
        for phase in ("hang", "support"):
            values = phase_palm_gaps[phase]
            record("ring_" + side + "_" + phase + "_palm_wood_clearance_min_m",
                   min(values) if values else None, lo=.0004)
            record("ring_" + side + "_" + phase + "_palm_wood_clearance_max_m",
                   max(values) if values else None, hi=.005)

    return {"metrics": metrics, "failures": failures}
