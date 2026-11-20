"""Rigid, connected phalanx chains for moving false-grip ring contact.

All points are derived from the body's authored wrist/hand frame and the live
ring frame.  Segment lengths are constants; only joint angles change.
"""
import math


FINGER_LENGTHS = (0.050, 0.026, 0.021)
THUMB_LENGTHS = (0.040, 0.027)
FINGER_BASES = (0.029, 0.010, -0.010, -0.029)


def _add(a, b): return [a[i] + b[i] for i in range(3)]
def _sub(a, b): return [a[i] - b[i] for i in range(3)]
def _mul(a, s): return [x * s for x in a]
def _dot(a, b): return sum(x*y for x, y in zip(a, b))
def _norm(a): return math.sqrt(_dot(a, a))


def _unit(v, fallback=(1.0, 0.0, 0.0)):
    n = _norm(v)
    if n < 1e-10:
        return list(fallback)
    return [x/n for x in v]


def _cross(a, b):
    return [a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0]]


def _rotate(v, axis, angle):
    c, s = math.cos(angle), math.sin(angle)
    return _add(_add(_mul(v, c), _mul(_cross(axis, v), s)), _mul(axis, _dot(axis, v)*(1-c)))


def segment_lengths():
    """Return the authored constant lengths in renderer segment order."""
    return tuple(x for _ in range(4) for x in FINGER_LENGTHS) + THUMB_LENGTHS


def _fabrik(base, target, lengths, pole):
    """Solve a fixed-link chain with a stable bend-plane preference."""
    total = sum(lengths)
    delta = _sub(target, base)
    distance = _norm(delta)
    if distance > total - 1e-7:
        # A nearly straight chain is still valid at its exact authored lengths.
        direction = _unit(delta)
        points = [list(base)]
        for length in lengths:
            points.append(_add(points[-1], _mul(direction, length)))
        return points
    direction = _unit(delta)
    bend = _sub(pole, _mul(direction, _dot(pole, direction)))
    if _norm(bend) < 1e-7:
        candidate = min(([1.,0.,0.], [0.,1.,0.], [0.,0.,1.]), key=lambda x: abs(_dot(x, direction)))
        bend = _sub(candidate, _mul(direction, _dot(candidate, direction)))
    bend = _unit(bend)
    # An arced deterministic seed keeps the fingers curled around the tube.
    points = [list(base)]
    for i, length in enumerate(lengths):
        phase = (i + 0.5) / len(lengths)
        side = math.sin(math.pi * phase) * 0.38
        points.append(_add(points[-1], _mul(_unit(_add(direction, _mul(bend, side))), length)))
    # FABRIK preserves each link exactly and converges to the ring-side contact.
    for _ in range(36):
        points[-1] = list(target)
        for i in range(len(lengths)-1, -1, -1):
            v = _unit(_sub(points[i], points[i+1]), _mul(direction, -1.0))
            points[i] = _add(points[i+1], _mul(v, lengths[i]))
        points[0] = list(base)
        for i, length in enumerate(lengths):
            v = _unit(_sub(points[i+1], points[i]), direction)
            points[i+1] = _add(points[i], _mul(v, length))
        if _norm(_sub(points[-1], target)) < 1e-7:
            break
    # One final backward/forward pair improves contact while maintaining lengths.
    points[-1] = list(target)
    for i in range(len(lengths)-1, -1, -1):
        v = _unit(_sub(points[i], points[i+1]), _mul(direction, -1.0))
        points[i] = _add(points[i+1], _mul(v, lengths[i]))
    points[0] = list(base)
    for i, length in enumerate(lengths):
        v = _unit(_sub(points[i+1], points[i]), direction)
        points[i+1] = _add(points[i], _mul(v, length))
    return points


def _wrap_chain(base, center, ring_normal, ring_tangent, ring_radius, lengths, skin_radius):
    """Construct exact-length links around the clearance circle of the hoop."""
    n = _unit(ring_normal)
    tangent = _unit(ring_tangent)
    from_center = _sub(base, center)
    plane = _sub(from_center, _mul(n, _dot(from_center, n)))
    radial = _unit(plane, (1., 0., 0.))
    q = _add(center, _mul(radial, ring_radius))
    # Each finger contacts a different point around the hoop, so use the
    # local tangent there rather than the prop's single reference tangent.
    local_tangent = _unit(_cross(radial, n), tangent)
    b = _sub(base, q)
    b = _sub(b, _mul(local_tangent, _dot(b, local_tangent)))
    d = _norm(b)
    clearance_radius = skin_radius + .0042
    l0 = lengths[0]
    if d < 1e-8 or not abs(d-clearance_radius) < l0 < d+clearance_radius:
        raise ValueError('MCP cannot reach fixed circle: d=%.6f R=%.6f L=%.6f' % (d, clearance_radius, l0))
    cosine = (d*d + clearance_radius*clearance_radius - l0*l0) / (2*d*clearance_radius)
    if cosine < -1-1e-8 or cosine > 1+1e-8:
        raise ValueError('invalid MCP/circle intersection')
    angle = math.acos(max(-1., min(1., cosine)))
    b_hat = _unit(b)
    perpendicular = _unit(_cross(local_tangent, b_hat))
    # The palm side is opposite ring_normal. Select the intersection and curl
    # direction that carry the fingertip around the near/lower tube surface.
    candidates = []
    for first_sign in (-1., 1.):
        e = _add(_mul(b_hat, math.cos(angle)), _mul(perpendicular, first_sign*math.sin(angle)))
        points = [list(base), _add(q, _mul(e, clearance_radius))]
        for length in lengths[1:]:
            step = 2*math.asin(min(.999999, length/(2*clearance_radius)))
            e = _rotate(e, local_tangent, -step)
            points.append(_add(q, _mul(e, clearance_radius)))
        # Reject any chain whose straight phalanx capsule cuts through the
        # same torus cross-section; prefer tips on the palm-facing side.
        toward_hand = _dot(_sub(points[-1], q), _mul(n, -1.))
        candidates.append((toward_hand, points))
    return max(candidates, key=lambda item: item[0])[1]


def _wrap_thumb(base, center, ring_normal, ring_tangent, ring_radius, lengths, skin_radius):
    """Oppose the thumb with one palm-side link and one tube-wrapping link."""
    n = _unit(ring_normal)
    tangent = _unit(ring_tangent)
    from_center = _sub(base, center)
    plane = _sub(from_center, _mul(n, _dot(from_center, n)))
    radial = _unit(plane, (1., 0., 0.))
    q = _add(center, _mul(radial, ring_radius))
    local_tangent = _unit(_cross(radial, n), tangent)
    b = _sub(base, q)
    b = _sub(b, _mul(local_tangent, _dot(b, local_tangent)))
    d = _norm(b)
    r1 = .040
    r2 = skin_radius + .0042
    l0, l1 = lengths
    if d < 1e-8 or not abs(d-r1) < l0 < d+r1:
        raise ValueError('thumb MCP cannot reach palm-side arc: d=%.6f R=%.6f L=%.6f' % (d, r1, l0))
    if not abs(r1-r2) < l1 < r1+r2:
        raise ValueError('thumb tip cannot reach tube arc: R0=%.6f R1=%.6f L=%.6f' % (r1, r2, l1))
    b_hat = _unit(b)
    perp = _unit(_cross(local_tangent, b_hat))
    a0 = math.acos(max(-1., min(1., (d*d+r1*r1-l0*l0)/(2*d*r1))))
    a1 = math.acos(max(-1., min(1., (r1*r1+r2*r2-l1*l1)/(2*r1*r2))))
    candidates=[]
    for sign0 in (-1., 1.):
        e1 = _add(_mul(b_hat, math.cos(a0)), _mul(perp, sign0*math.sin(a0)))
        p1 = _add(q, _mul(e1, r1))
        for sign1 in (-1., 1.):
            e2 = _rotate(e1, local_tangent, sign1*a1)
            p2 = _add(q, _mul(e2, r2))
            toward_palm = _dot(_sub(p2, q), _mul(n, -1.))
            # Prefer the branch whose first-link centerline stays furthest
            # from the tube. Both links keep their authored exact lengths.
            nearest = min(_norm(_sub(q, _add(base, _mul(_sub(p1, base),
                         max(0., min(1., _dot(_sub(q, base), _sub(p1, base)) /
                         max(_dot(_sub(p1, base), _sub(p1, base)),1e-12))))))),
                          _norm(_sub(q, _add(p1, _mul(_sub(p2, p1),
                         max(0., min(1., _dot(_sub(q, p1), _sub(p2, p1)) /
                         max(_dot(_sub(p2, p1), _sub(p2, p1)),1e-12))))))))
            candidates.append((toward_palm, nearest, [list(base),p1,p2]))
    return max(candidates, key=lambda item: (item[0], item[1]))[2]


def hand_segments(frame, side, spec):
    """Return 12 finger and 2 thumb capsules with constant bone lengths."""
    from hand_axes import axes
    from ring_geometry import ring_frame

    wrist = frame['wrist_' + side]
    u, palm_n, across = axes(wrist, frame['hand_' + side], frame['palmn_' + side], side)
    center, ring_n, tangent, _ = ring_frame(frame, spec)
    radial = _unit(_cross(ring_n, tangent))
    radius = float(spec['radius'])
    tube = float(spec['tube'])
    fpad = float(spec.get('finger_radius', .0088))
    tpad = float(spec.get('thumb_radius', .0105))
    # With the source's grip-first frame, across is the ring tangent. Bases
    # remain attached to the palm and track the apparatus without a scaffold.
    segments = []
    for offset in FINGER_BASES:
        base = _add(_add(wrist, _mul(u, .085)), _mul(across, offset))
        # Follow the ring circumference across the knuckle row.  The endpoint
        # lies just outside the actual tube on the hand-facing side.
        points = _wrap_chain(base, center, ring_n, tangent, radius,
                             FINGER_LENGTHS, tube + fpad)
        segments.extend((points[i], points[i+1]) for i in range(3))

    # Thumb base remains on the radial side of the palm; its tip reaches the
    # adjacent lower hoop arc so the opposing digit closes around the grip.
    thumb_base = _add(_add(wrist, _mul(u, .047)), _mul(across, .034 if side == 'l' else -.034))
    thumb = _wrap_thumb(thumb_base, center, ring_n, tangent, radius,
                        THUMB_LENGTHS, tube + tpad)
    segments.extend(((thumb[0], thumb[1]), (thumb[1], thumb[2])))
    return segments
