"""Shared movement-class screening (doc 43 section 3): one checker, many exercises.

An exercise in motion_contracts.json names a `movement_class` (plank_hold, bridge, push_up,
pull, row, dip, squat, lunge, hinge, leg_raise, twist, static_hold, jump), optional
`params` that override the class defaults, and an optional `identity` list of numeric
checks that make this variant recognisable (e.g. elbows ahead of the shoulders).

Shared checks, all computed from the joint frames only (no rendering):
  planted_contacts_slide   a joint that touches the floor must not slide while it is down
  segment_lengths          thigh, shin, foot, upper arm, forearm keep their length
  main_range               the class's main joint/measure covers a minimum range
  endpoint_dwell           moving classes rest at both ends (sample_continuous with pauses)
  floor_clearance          body surface does not sink into the floor
  prop_clearance           non-contact joints stay out of box and bar props
  loop_seam                last frame returns to the first, without a jump across the seam
  joint_speed / joint_acceleration   no teleporting, no pops

Screening only; the side-by-side reference review is the form check.
"""
import math

SEGMENTS = (('hip', 'knee', 'thigh'), ('knee', 'ankle', 'shin'), ('ankle', 'toe', 'foot'),
            ('shoulder', 'elbow', 'upper'), ('elbow', 'wrist', 'fore'))
# Body surface radius per joint (same numbers as poses.RADIUS, copied to stay import-light).
RADIUS = dict(toe=0.02, ankle=0.04, knee=0.05, wrist=0.025, hand=0.012, elbow=0.04, hip=0.07,
              shoulder=0.05)
RADIUS_SINGLE = dict(chest=0.10, spine=0.09, pelvis=0.09, head=0.10)
CONTACT_JOINTS = ('toe', 'ankle', 'wrist', 'hand', 'elbow', 'knee')


def sub(a, b): return [x - y for x, y in zip(a, b)]
def norm(v): return math.sqrt(sum(x * x for x in v))
def dot(a, b): return sum(x * y for x, y in zip(a, b))
def unit(v):
    n = norm(v)
    return [x / n for x in v] if n > 1e-9 else [0, 0, 0]


def angle(a, b, c):
    u, v = sub(a, b), sub(c, b)
    length = norm(u) * norm(v)
    if length < 1e-10:
        return float('nan')
    return math.degrees(math.acos(max(-1, min(1, dot(u, v) / length))))


def _forward(f):
    """Horizontal heading of the trunk (pelvis to chest); falls back to +X."""
    h = [f['chest'][0] - f['pelvis'][0], f['chest'][1] - f['pelvis'][1], 0]
    return unit(h) if norm(h) > 0.05 else [1, 0, 0]


def _lateral_axis(f):
    return unit(sub(f['shoulder_l'], f['shoulder_r']))


# Frame metrics: name -> function(frame, side) -> number. `side` is 'l' or 'r' for sided ones.
FRAME_METRICS = {
    'elbow_angle': lambda f, s: angle(f['shoulder_' + s], f['elbow_' + s], f['wrist_' + s]),
    'knee_angle': lambda f, s: angle(f['hip_' + s], f['knee_' + s], f['ankle_' + s]),
    'hip_angle': lambda f, s: angle(f['chest'], f['hip_' + s], f['knee_' + s]),
    'shoulder_angle': lambda f, s: angle(f['hip_' + s], f['shoulder_' + s], f['elbow_' + s]),
    'trunk_from_vertical_deg': lambda f, s=None: math.degrees(math.atan2(
        math.hypot(f['chest'][0] - f['pelvis'][0], f['chest'][1] - f['pelvis'][1]), f['chest'][2] - f['pelvis'][2])),
    'body_line_deg': lambda f, s=None: angle(f['chest'], f['pelvis'], [(a + b) / 2 for a, b in zip(f['ankle_l'], f['ankle_r'])]),
    'pelvis_z': lambda f, s=None: f['pelvis'][2],
    'chest_z': lambda f, s=None: f['chest'][2],
    'head_z': lambda f, s=None: f['head'][2],
    'ankle_z': lambda f, s: f['ankle_' + s][2],
    'wrist_z': lambda f, s: f['wrist_' + s][2],
    'pelvis_x': lambda f, s=None: f['pelvis'][0],
    # Twist: signed angle of the shoulder line against the hip line about the vertical axis.
    'shoulder_yaw_deg': lambda f, s=None: math.degrees(math.atan2(
        f['shoulder_l'][1] - f['shoulder_r'][1], f['shoulder_l'][0] - f['shoulder_r'][0])),
    # Identity measures
    'elbow_ahead_of_shoulder_m': lambda f, s: dot(sub(f['elbow_' + s], f['shoulder_' + s]), _forward(f)),
    'elbow_below_shoulder_m': lambda f, s: f['shoulder_' + s][2] - f['elbow_' + s][2],
    'wrist_gap_m': lambda f, s=None: norm(sub(f['wrist_l'], f['wrist_r'])),
    'elbow_gap_m': lambda f, s=None: norm(sub(f['elbow_l'], f['elbow_r'])),
    'ankle_gap_m': lambda f, s=None: norm(sub(f['ankle_l'], f['ankle_r'])),
    'knee_gap_m': lambda f, s=None: norm(sub(f['knee_l'], f['knee_r'])),
    'shoulder_z': lambda f, s: f['shoulder_' + s][2],
    'hip_z': lambda f, s: f['hip_' + s][2],
    'hip_level_m': lambda f, s=None: abs(f['hip_l'][2] - f['hip_r'][2]),
    'knee_z': lambda f, s: f['knee_' + s][2],
    'toe_z': lambda f, s: f['toe_' + s][2],
    'thigh_pitch_deg': lambda f, s: math.degrees(math.atan2(f['knee_' + s][2] - f['hip_' + s][2],
                                                              math.hypot(f['knee_' + s][0] - f['hip_' + s][0], f['knee_' + s][1] - f['hip_' + s][1]))),
    'pelvis_over_ankles_m': lambda f, s=None: dot(sub(f['pelvis'], [(a + b) / 2 for a, b in zip(f['ankle_l'], f['ankle_r'])]), _forward(f)),
    'ankle_split_m': lambda f, s=None: abs(dot(sub(f['ankle_l'], f['ankle_r']), _forward(f))),
    'knee_over_ankle_m': lambda f, s: dot(sub(f['knee_' + s], f['ankle_' + s]), _forward(f)),
    'free_leg_line_deg': lambda f, s: angle(f['hip_' + s], f['knee_' + s], f['ankle_' + s]),
    'shoulder_pelvis_line_deg': lambda f, s=None: math.degrees(math.atan2(
        abs(f['chest'][2] - f['pelvis'][2]), math.hypot(f['chest'][0] - f['pelvis'][0], f['chest'][1] - f['pelvis'][1]) + 1e-9)),
}


def _series(fs, metric, sides):
    fn = FRAME_METRICS[metric]
    if metric in _UNSIDED:
        return {'': [fn(f) for f in fs]}
    return {s: [fn(f, s) for f in fs] for s in sides}


_UNSIDED = {k for k, v in FRAME_METRICS.items() if v.__code__.co_argcount == 1 or
            (v.__defaults__ and len(v.__defaults__) >= 1)}

CLASS_DEFAULTS = {
    'plank_hold': dict(main_range=None, contacts=['toe_l', 'toe_r', 'wrist_l', 'wrist_r'], dwell_s=0, slide_max_m=.01),
    'static_hold': dict(main_range=None, contacts=[], dwell_s=0, slide_max_m=.01),
    'bridge': dict(contacts=['ankle_l', 'ankle_r'], slide_max_m=.012, dwell_s=.2),
    'push_up': dict(contacts=['wrist_l', 'wrist_r', 'toe_l', 'toe_r'], slide_max_m=.01, dwell_s=.15),
    'pull': dict(contacts=[], slide_max_m=.01, dwell_s=.15),
    'row': dict(contacts=['ankle_l', 'ankle_r'], slide_max_m=.012, dwell_s=.15),
    'dip': dict(contacts=[], slide_max_m=.01, dwell_s=.15),
    'squat': dict(contacts=['toe_l', 'toe_r'], slide_max_m=.012, dwell_s=.15),
    'lunge': dict(contacts=[], slide_max_m=.015, dwell_s=.1),
    'hinge': dict(contacts=['toe_l', 'toe_r'], slide_max_m=.012, dwell_s=.15),
    'leg_raise': dict(contacts=[], slide_max_m=.012, dwell_s=.1),
    'twist': dict(contacts=[], slide_max_m=.012, dwell_s=.0),
    'jump': dict(contacts=[], slide_max_m=.02, dwell_s=0),
}
SHARED_DEFAULTS = dict(
    contact_height_m=.075,         # a contact joint closer than this to the floor counts as down
    slide_step_max_m_s=.08,        # horizontal speed while down
    length_tol_m=.004, floor_min_m=-.02, loop_gap_m=.012, seam_ratio_max=2.0,
    speed_max_m_s=2.5, accel_max_m_s2=60., dwell_tol=.04, prop_tol_m=.01, tracked=['pelvis', 'chest', 'head', 'wrist_l', 'wrist_r', 'ankle_l', 'ankle_r'])


def resolve(definition):
    """Class defaults, then the exercise's own params."""
    cls = definition['movement_class']
    p = dict(SHARED_DEFAULTS)
    p.update(CLASS_DEFAULTS[cls])
    p.update(definition.get('params', {}))
    return cls, p


def _record_factory(metrics, failures, fps):
    def record(name, value, lo=None, hi=None, frame=None):
        good = math.isfinite(value) and (lo is None or value >= lo) and (hi is None or value <= hi)
        metrics[name] = {'value': round(value, 5) if math.isfinite(value) else None, 'min': lo, 'max': hi, 'ok': good}
        if not good:
            failures.append({'check': name, 'value': metrics[name]['value'], 'frame': frame,
                             'time_s': round(frame / fps, 3) if frame is not None else None})
    return record


def _joint(f, name): return f[name]


def planted_slide(fs, joint, height, step_max_m_s, fps, radius=0.0):
    """Worst horizontal drift over any run of frames in which `joint` is down, and worst speed."""
    worst_drift, worst_speed, at = 0., 0., None
    run_start = None
    for i, f in enumerate(fs):
        down = f[joint][2] - radius <= height
        if down and run_start is None:
            run_start = i
        if not down:
            run_start = None
            continue
        ref = fs[run_start][joint]
        d = math.hypot(f[joint][0] - ref[0], f[joint][1] - ref[1])
        if d > worst_drift:
            worst_drift, at = d, i
        if i > run_start:
            prev = fs[i - 1][joint]
            v = math.hypot(f[joint][0] - prev[0], f[joint][1] - prev[1]) * fps
            worst_speed = max(worst_speed, v)
    return worst_drift, worst_speed, at


def _prop_boxes(props):
    return [p for p in props if p.get('kind') == 'box']


def _resolve_point(spec, f):
    if isinstance(spec, dict):
        j, o = f[spec['joint']], spec.get('off', (0, 0, 0))
        return [j[0] + o[0], j[1] + o[1], j[2] + o[2]]
    return list(spec)


def point_segment_distance(p, a, b):
    ab = sub(b, a)
    L2 = dot(ab, ab)
    t = 0 if L2 < 1e-12 else max(0, min(1, dot(sub(p, a), ab) / L2))
    return norm(sub(p, [a[i] + t * ab[i] for i in range(3)]))


def audit(data, definition, contract=None):
    """Return the same report shape as motion_quality.audit for a class-based exercise."""
    ex = data['id']
    fs = data.get('frames', [])
    fps = data.get('fps', 24)
    metrics, failures = {}, []
    record = _record_factory(metrics, failures, fps or 1)
    cls, p = resolve(definition)
    if len(fs) < 4 or fps <= 0:
        return dict(exercise=ex, status='fail', failures=[{'check': 'valid_timeline'}], metrics={})
    need = ('pelvis', 'chest', 'head', 'wrist_l', 'wrist_r', 'ankle_l', 'ankle_r', 'hip_l', 'hip_r', 'knee_l', 'knee_r',
            'toe_l', 'toe_r', 'shoulder_l', 'shoulder_r', 'elbow_l', 'elbow_r')
    for f in fs:
        for n in need:
            v = f.get(n)
            if not (isinstance(v, (list, tuple)) and len(v) == 3 and all(isinstance(x, (int, float)) and math.isfinite(x) for x in v)):
                return dict(exercise=ex, status='fail', failures=[{'check': 'finite_geometry', 'joint': n}], metrics={})
    # 1. planted contacts do not slide
    for joint in p['contacts']:
        base = joint.rsplit('_', 1)[0]
        drift, speed, at = planted_slide(fs, joint, p['contact_height_m'] + RADIUS.get(base, 0), p['slide_step_max_m_s'], fps)
        record(joint + '_slide_m', drift, hi=p['slide_max_m'], frame=at)
        record(joint + '_slide_speed_m_s', speed, hi=p['slide_step_max_m_s'])
    # 2. segment lengths
    lengths = data.get('lengths') or {}
    for a, b, key in SEGMENTS:
        for s in ('l', 'r'):
            series = [norm(sub(f[b + '_' + s], f[a + '_' + s])) for f in fs]
            nominal = lengths.get(key) or series[0]
            worst = max(abs(v - nominal) for v in series)
            record(f'{key}_{s}_length_error_m', worst, hi=p['length_tol_m'], frame=max(range(len(fs)), key=lambda i: abs(series[i] - nominal)))
    # 3. main range
    mr = p.get('main_range')
    main_values = None
    if mr:
        series = _series(fs, mr['metric'], mr.get('sides', ['l', 'r']))
        ranges = {s: max(v) - min(v) for s, v in series.items()}
        chosen = max(ranges.values()) if mr.get('agg', 'max') == 'max' else min(ranges.values())
        record('main_range_' + mr['metric'], chosen, lo=mr.get('min'), hi=mr.get('max'))
        main_values = series[max(ranges, key=ranges.get)]
    # 4. dwell at both ends of a moving class
    if main_values is not None and p.get('dwell_s', 0) > 0:
        lo, hi = min(main_values), max(main_values)
        span = hi - lo
        if span > 1e-6:
            tol = p['dwell_tol'] * span
            low_frames = sum(1 for v in main_values if v <= lo + tol)
            high_frames = sum(1 for v in main_values if v >= hi - tol)
            record('dwell_low_s', low_frames / fps, lo=p['dwell_s'])
            record('dwell_high_s', high_frames / fps, lo=p['dwell_s'])
    # 5. floor clearance and prop clearance
    worst, wf, wj = math.inf, 0, ''
    for i, f in enumerate(fs):
        for base, r in RADIUS.items():
            for s in ('l', 'r'):
                z = f[base + '_' + s][2] - r
                if z < worst:
                    worst, wf, wj = z, i, base + '_' + s
        for base, r in RADIUS_SINGLE.items():
            z = f[base][2] - r
            if z < worst:
                worst, wf, wj = z, i, base
    record('floor_clearance_m', worst, lo=p['floor_min_m'], frame=wf)
    inside = 0
    first = None
    free = [(b, s) for b in ('hip', 'knee', 'shoulder', 'elbow', 'ankle', 'toe') for s in ('l', 'r')]
    exempt = set(p.get('prop_contacts', []))
    boxes = _prop_boxes(data.get('props', []))
    for i, f in enumerate(fs):
        for box in boxes:
            c, sz = box['center'], box['size']
            for b, s in free + [('chest', ''), ('pelvis', ''), ('head', '')]:
                name = b + ('_' + s if s else '')
                if name in exempt:
                    continue
                r = RADIUS.get(b, RADIUS_SINGLE.get(b, 0))
                pt = f[name]
                if all(abs(pt[k] - c[k]) < sz[k] / 2 + r - p['prop_tol_m'] - 0.02 for k in range(3)) and \
                        all(abs(pt[k] - c[k]) < sz[k] / 2 - p['prop_tol_m'] for k in range(3)):
                    inside += 1
                    first = first or i
    record('prop_box_interior_joints', inside, hi=0, frame=first)
    rod_gap = math.inf
    for i, f in enumerate(fs):
        for pr in data.get('props', []):
            if pr.get('kind') == 'rod' and pr.get('body_clearance', True):
                a, b2 = _resolve_point(pr['a'], f), _resolve_point(pr['b'], f)
                for base in ('hip', 'knee', 'elbow', 'shoulder', 'ankle', 'toe'):
                    for s in ('l', 'r'):
                        name = base + '_' + s
                        if name in exempt:
                            continue
                        gap = point_segment_distance(f[name], a, b2) - pr.get('r', .017) - RADIUS[base]
                        rod_gap = min(rod_gap, gap)
                for name in ('chest', 'pelvis', 'head'):
                    gap = point_segment_distance(f[name], a, b2) - pr.get('r', .017) - RADIUS_SINGLE[name]
                    rod_gap = min(rod_gap, gap)
    if math.isfinite(rod_gap):
        record('prop_rod_clearance_m', rod_gap, lo=-p['prop_tol_m'])
    # 6. loop seam
    names = p['tracked']
    gap = max(norm(sub(fs[-1][n], fs[0][n])) for n in names)
    record('loop_gap_m', gap, hi=p['loop_gap_m'])
    steps = [norm(sub(fs[i][n], fs[i - 1][n])) for i in range(1, len(fs)) for n in names]
    seam = max(norm(sub(fs[0][n], fs[-1][n])) for n in names)
    typical = max(steps) if steps else 0
    record('seam_step_ratio', seam / typical if typical > 1e-9 else 0, hi=p['seam_ratio_max'])
    # 7. speed and acceleration
    speeds = [(norm(sub(fs[i][n], fs[i - 1][n])) * fps, i) for i in range(1, len(fs)) for n in names]
    v, i = max(speeds)
    record('max_joint_speed_m_s', v, hi=p['speed_max_m_s'], frame=i)
    acc = [(norm([fs[i + 1][n][k] - 2 * fs[i][n][k] + fs[i - 1][n][k] for k in range(3)]) * fps * fps, i)
           for i in range(1, len(fs) - 1) for n in names]
    v, i = max(acc)
    record('max_joint_accel_m_s2', v, hi=p['accel_max_m_s2'], frame=i)
    # 8. identity checks of this variant
    for chk in definition.get('identity', []):
        series = _series(fs, chk['metric'], chk.get('sides', ['l', 'r']))
        stat = chk.get('stat', 'min')
        for s, vals in series.items():
            val = {'min': min, 'max': max, 'mean': lambda v: sum(v) / len(v), 'first': lambda v: v[0],
                   'range': lambda v: max(v) - min(v)}[stat](vals)
            name = f"{chk['metric']}_{stat}" + (('_' + s) if s else '')
            frame = vals.index(min(vals) if stat == 'min' else max(vals)) if stat in ('min', 'max') else None
            record(name, val, lo=chk.get('lo'), hi=chk.get('hi'), frame=frame)
    return dict(exercise=ex, style=definition.get('style', ''), movement_class=cls,
                status='pass' if not failures else 'fail', metrics=metrics, failures=failures,
                meaning='Movement-class screening only; side-by-side reference review still required')
