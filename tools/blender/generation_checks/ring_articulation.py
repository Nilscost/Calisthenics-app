"""Engineering screens for dynamic ring and hand articulation in a strict muscle-up.

These authored-trajectory bounds do not certify universal exercise form.
"""
import math


def _v(value):
    return isinstance(value, (list, tuple)) and len(value) == 3 and all(
        isinstance(x, (int, float)) and math.isfinite(x) for x in value)


def _dot(a, b): return sum(x*y for x, y in zip(a, b))
def _sub(a, b): return [x-y for x, y in zip(a, b)]
def _add(a, b): return [x+y for x, y in zip(a, b)]
def _mul(a, s): return [x*s for x in a]
def _norm(a): return math.sqrt(_dot(a, a))
def _unit(a):
    n = _norm(a)
    return [x/n for x in a] if n > 1e-10 else None

def _angle(a, b):
    ua, ub = _unit(a), _unit(b)
    return math.degrees(math.acos(max(-1., min(1., _dot(ua, ub))))) if ua and ub else None


def audit(data):
    metrics, failures = {}, []
    def record(name, value, lo=None, hi=None):
        valid = isinstance(value, (int, float)) and math.isfinite(value)
        ok = valid and (lo is None or value >= lo) and (hi is None or value <= hi)
        shown = round(float(value), 6) if valid else None
        metrics[name] = {"value": shown, "min": lo, "max": hi, "ok": bool(ok)}
        if not ok: failures.append({"check": name, "value": shown})
    frames, props = data.get('frames'), data.get('props', [])
    if not isinstance(frames, list) or len(frames) < 4:
        record('valid_timeline', 0, 1, 1)
        return {'metrics': metrics, 'failures': failures}
    record('valid_timeline', 1, 1, 1)
    rings = [p for p in props if p.get('kind') == 'ring']
    record('two_rings', len(rings), 2, 2)
    from ring_geometry import ring_frame, radial_axis, strap_attachment
    wrist_bends, flexions, deviations, hand_axes = ({'l': [], 'r': []} for _ in range(4))
    phase_bends = {'hang': [], 'support': []}
    phase_flex = {'hang': [], 'support': []}
    phase_deviation = {'hang': [], 'support': []}
    # False-grip identity cues are checked only in the initial low-chest hang,
    # before the first support phase. They describe the authored grip pose.
    initial_hang_wrist_over_wood = []
    initial_hang_palm_normal_z = []
    left_spec = next((p for p in rings if p.get('joint') == 'wrist_l'), None)
    first_support_frame = len(frames)
    if left_spec:
        for i, f in enumerate(frames):
            try:
                if _v(f.get('chest')) and f['chest'][2] - ring_frame(f, left_spec)[3][2] >= .45:
                    first_support_frame = i
                    break
            except (KeyError, TypeError, ValueError, ZeroDivisionError):
                pass
    for side in ('l', 'r'):
        spec = next((p for p in rings if p.get('joint') == 'wrist_' + side), None)
        dynamic = bool(spec and spec.get('frame_center') and spec.get('frame_normal') and spec.get('frame_radial'))
        record('ring_' + side + '_dynamic_channels', 1 if dynamic else 0, 1, 1)
        centers, normals, radials, contacts, strap_errors, anchor_errors = [], [], [], [], [], []
        palm_orthogonality = []
        frame_data_valid = True
        for frame_index, f in enumerate(frames):
            try:
                if not spec: raise KeyError('ring spec')
                c, n, tangent, target = ring_frame(f, spec)
                if not all(_v(x) for x in (c, n, tangent, target)): raise ValueError('invalid ring frame')
                radial = radial_axis(spec, f)
                normal = f[spec['frame_normal']] if dynamic else n
                if not _v(radial) or not _v(normal) or _norm(radial) < 1e-8 or _norm(normal) < 1e-8:
                    raise ValueError('zero or invalid ring axis')
                centers.append(c); normals.append(normal); radials.append(radial)
                hand_axis = _unit(_sub(f['hand_'+side], f['wrist_'+side]))
                palm_axis = _unit(f['palmn_'+side])
                offset = f.get('grip_offset_' + side, (.065, .045))
                if hand_axis and palm_axis:
                    palm_orthogonality.append(abs(_dot(hand_axis, palm_axis)))
                if not isinstance(offset, (list, tuple)) or len(offset) not in (2, 3) or not all(isinstance(v, (int, float)) and math.isfinite(v) for v in offset):
                    raise ValueError('invalid grip offset')
                actual=_add(_add(f['wrist_'+side], _mul(hand_axis or [0,0,0], offset[0])), _mul(palm_axis or [0,0,0], offset[1]))
                if len(offset)==3:actual=_add(actual,_mul(n,offset[2]))
                contacts.append(_norm(_sub(actual,target)))
                top = strap_attachment(f, spec)
                strap_errors.append(abs(_norm(_sub(top, c)) - spec['radius']))
                if _v(spec.get('anchor')):
                    anchor_errors.append(_norm(_sub(top, spec['anchor'])))
            except (KeyError, TypeError, ValueError, ZeroDivisionError):
                frame_data_valid = False
        record('ring_' + side + '_frame_data_valid', 1 if frame_data_valid else 0, 1, 1)
        if centers:
            excursion = max(_norm(_sub(c, centers[0])) for c in centers)
            record('ring_' + side + '_center_excursion_m', excursion, .025, .18)
            nr = [_angle(normals[0], n) for n in normals] if normals else []
            record('ring_' + side + '_plane_rotation_range_deg', max((v for v in nr if v is not None), default=None), 8, 50)
            if dynamic and all(radials):
                dots = [abs(_dot(n, r)) for n, r in zip(normals, radials)]
                norms = [_norm(n) for n in normals] + [_norm(r) for r in radials]
                record('ring_' + side + '_unit_error', max((abs(x-1) for x in norms), default=None), hi=.002)
                record('ring_' + side + '_orthogonality_error', max(dots) if dots else None, hi=.003)
                rr = [_angle(radials[0], r) for r in radials] if radials else []
                record('ring_' + side + '_radial_rotation_range_deg', max((v for v in rr if v is not None), default=None), 15, 100)
                steps = [max(_angle(normals[i-1], normals[i]) or 0, _angle(radials[i-1], radials[i]) or 0) for i in range(1, min(len(normals), len(radials)))]
                record('ring_' + side + '_max_frame_twist_deg', max(steps) if steps else None, hi=12)
                record('ring_' + side + '_strap_radius_error_m', max(strap_errors) if strap_errors else None, hi=.00005)
                record('ring_' + side + '_strap_length_variation_m', max((abs(v-anchor_errors[0]) for v in anchor_errors), default=None), hi=.002)
                record('wrist_' + side + '_grip_contact_error_m', max(contacts) if contacts else None, hi=.00005)
            else:
                record('ring_' + side + '_unit_error', None, hi=.002)
                record('ring_' + side + '_orthogonality_error', None, hi=.003)
                record('ring_' + side + '_radial_rotation_range_deg', None, 15, 100)
                record('ring_' + side + '_max_frame_twist_deg', None, hi=12)
                record('ring_' + side + '_strap_radius_error_m', max(strap_errors) if strap_errors else None, hi=.00005)
                record('ring_' + side + '_strap_length_variation_m', max((abs(v-anchor_errors[0]) for v in anchor_errors), default=None), hi=.002)
                record('wrist_' + side + '_grip_contact_error_m', max(contacts) if contacts else None, hi=.00005)
        record('hand_' + side + '_palm_orthogonality_error', max(palm_orthogonality) if palm_orthogonality else None, hi=.02)
        for frame_index, f in enumerate(frames):
            try:
                fore = _unit(_sub(f['wrist_'+side], f['elbow_'+side]))
                hand = _unit(_sub(f['hand_'+side], f['wrist_'+side]))
                palm = _unit(f['palmn_'+side])
                if not fore or not hand or not palm: raise ValueError()
                across = _unit([hand[1]*palm[2]-hand[2]*palm[1], hand[2]*palm[0]-hand[0]*palm[2], hand[0]*palm[1]-hand[1]*palm[0]])
                bend = _angle(fore, hand)
                flexion = math.degrees(math.atan2(_dot(fore, palm), _dot(fore, hand)))
                deviation = math.degrees(math.atan2(_dot(fore, across), math.hypot(_dot(fore, hand), _dot(fore, palm)))) if across else None
                wrist_bends[side].append(bend)
                hand_axes[side].append(hand)
                flexions[side].append(flexion)
                deviations[side].append(deviation)
                if not spec: raise KeyError('ring spec')
                rel = f['chest'][2] - ring_frame(f, spec)[3][2]
                phase = 'hang' if rel <= -.30 else ('support' if rel >= .45 else None)
                if phase == 'hang' and frame_index < first_support_frame:
                    initial_hang_wrist_over_wood.append(f['wrist_'+side][2] - ring_frame(f, spec)[3][2])
                    initial_hang_palm_normal_z.append(palm[2])
                if phase:
                    phase_bends[phase].append(bend)
                    phase_flex[phase].append(abs(flexion))
                    phase_deviation[phase].append(abs(deviation) if deviation is not None else 0.)
            except (KeyError, TypeError, ValueError):
                wrist_bends[side].append(None); flexions[side].append(None); deviations[side].append(None); hand_axes[side].append(None)
        vals = [x for x in wrist_bends[side] if x is not None]
        record('wrist_' + side + '_bend_deg', max(vals) if vals else None, hi=110)
        axes = [a for a in hand_axes[side] if a is not None]
        hand_range = max((_angle(axes[0], a) or 0 for a in axes), default=None)
        record('hand_' + side + '_axis_rotation_range_deg', hand_range, lo=30)
        steps = [abs(a-b) for a,b in zip(wrist_bends[side], wrist_bends[side][1:]) if a is not None and b is not None]
        record('wrist_' + side + '_bend_step_deg', max(steps) if steps else None, hi=8)
        record('wrist_' + side + '_flexion_abs_deg', max((abs(x) for x in flexions[side] if x is not None), default=None), hi=110)
        record('wrist_' + side + '_deviation_abs_deg', max((abs(x) for x in deviations[side] if x is not None), default=None), hi=25)
    for phase, limits in (('hang', (45, 105)), ('support', (0, 35))):
        bend_values = phase_bends[phase]
        flex_values = phase_flex[phase]
        dev_values = phase_deviation[phase]
        record(phase + '_sample_count', len(bend_values), lo=1)
        if phase == 'hang':
            record('hang_wrist_angle_min_deg', min(bend_values) if bend_values else None, lo=limits[0])
            record('hang_wrist_angle_max_deg', max(bend_values) if bend_values else None, hi=limits[1])
            record('hang_flexion_abs_min_deg', min(flex_values) if flex_values else None, lo=45)
            record('hang_flexion_abs_max_deg', max(flex_values) if flex_values else None, hi=105)
        else:
            record('support_wrist_angle_max_deg', max(bend_values) if bend_values else None, hi=limits[1])
        record(phase + '_deviation_abs_max_deg', max(dev_values) if dev_values else None, hi=25)
    record('false_grip_hang_wrist_over_wood_min_m',
           min(initial_hang_wrist_over_wood) if initial_hang_wrist_over_wood else None, lo=.010)
    record('false_grip_hang_palm_normal_z_max',
           max(initial_hang_palm_normal_z) if initial_hang_palm_normal_z else None, hi=-.10)
    seam_names = ('pelvis', 'chest', 'wrist_l', 'wrist_r', 'hand_l', 'hand_r')
    seam = max((_norm(_sub(frames[0][k], frames[-1][k])) for k in seam_names if _v(frames[0].get(k)) and _v(frames[-1].get(k))), default=None)
    for side in ('l', 'r'):
        for prefix in ('ring_center_', 'ring_plane_', 'ring_radial_'):
            key = prefix + side + ('_up' if prefix != 'ring_center_' else '')
            if _v(frames[0].get(key)) and _v(frames[-1].get(key)):
                seam = max(seam or 0., _norm(_sub(frames[0][key], frames[-1][key])))
    record('loop_seam_max_m', seam, hi=.012)
    # The chest must participate in a meaningful hang-to-support rise and return.
    chest_z = [f.get('chest', [None, None, None])[2] for f in frames if _v(f.get('chest'))]
    record('chest_vertical_excursion_m', max(chest_z)-min(chest_z) if chest_z else None, lo=.65)
    return {'metrics': metrics, 'failures': failures}
