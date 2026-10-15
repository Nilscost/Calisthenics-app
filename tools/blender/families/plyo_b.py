"""Plyometric bounds, hops and box drills for the R8 pose batch."""

from poses import base_pose


def _pose(pelvis, right, left, *, p1=0, p2=0, arms=None):
    """A key with explicit ankle targets; all exercises keep the same fields per key."""
    opts = arms or {}
    return base_pose(pelvis=pelvis, p1=p1, p2=p2, hp=-4,
                     ankle_r=right[0], foot_r=right[1], knee_r=right[2],
                     ankle_l=left[0], foot_l=left[1], knee_l=left[2], **opts)


def _arms(swing=0.0, lateral=0.0):
    # Arms swing freely; symmetric reach makes the takeoff and landing legible.
    swing = max(-0.40, min(0.40, swing))
    lateral = max(-0.08, min(0.08, lateral))
    return dict(wrist_r_rel=[swing, -0.16 + lateral, -0.04],
                wrist_l_rel=[swing, 0.16 - lateral, -0.04],
                elbow_r=[0, -0.2, -1], elbow_l=[0, 0.2, -1],
                hand_r=[1, 0, 0], hand_l=[1, 0, 0])


def _two(pelvis_x, z, bend, *, y=0.0, p=0, swing=0.0):
    # Bent landings use knees pointed forward and toes slightly out.
    return _pose([pelvis_x, y, z],
                 ([pelvis_x, y - 0.10, 0.08], [1, 0, 0], [1, -0.15, 0]),
                 ([pelvis_x, y + 0.10, 0.08], [1, 0, 0], [1, 0.15, 0]),
                 p1=p, p2=p, arms=_arms(swing))


def _air2(x, z=0.30, *, y=0.0, tuck=0.0, swing=0.0):
    return _pose([x, y, 0.83 + z],
                 ([x + 0.04, y - 0.10, z], [1, 0, -0.25], [1, -0.15, 0]),
                 ([x - 0.04, y + 0.10, z], [1, 0, -0.25], [1, 0.15, 0]),
                 p1=8 + tuck, p2=10 + tuck, arms=_arms(swing))


def _single(x, *, y=0.0, z=0.08, knee=0.0, lifted=(0.15, 0.12, 0.38), p=5, swing=0.0):
    # Right foot is the working/near leg. The free left knee cycles forward or behind.
    return _pose([x, y, 0.88 if z <= 0.08 else 0.92],
                 ([x, y - 0.10, z], [1, 0, -0.22], [1, -0.15, 0]),
                 ([x + lifted[0], y + 0.10, lifted[2]], [1, 0, -0.4], [1, 0, 0]),
                 p1=p, p2=p, arms=_arms(swing))


def _reset(x0, x1, *, y=0.0, axis="x"):
    """Walk back in short alternating steps, keeping the stance foot planted per key."""
    distance = x1 - x0
    steps = max(1, int(abs(distance) / .50 + .999))
    direction = 1 if distance >= 0 else -1
    stride = distance / steps

    def target(v, side, z):
        if axis == "x":
            return [v, y + side * .10, z]
        return [y + side * .10, v, z]

    def pelvis(v):
        return [v, y, .77] if axis == "x" else [y, v, .77]

    # The planted foot stays at its exact world target while the other advances.
    center = x0
    right_v = left_v = x0
    keys = [_pose(pelvis(center), (target(right_v, -1, .08), [1, 0, 0], [1, 0, 0]),
                  (target(left_v, 1, .08), [1, 0, 0], [1, 0, 0]), arms=_arms(-.12))]
    for i in range(steps):
        next_v = x0 + stride * (i + 1)
        half_a = center + stride * .20
        half_b = center + stride * .50
        half_c = center + stride * .80
        if i % 2 == 0:
            keys.append(_pose(pelvis(half_a), (target(next_v, -1, .22), [1, 0, -.7], [1, 0, 0]),
                              (target(left_v, 1, .08), [1, 0, 0], [1, 0, 0]), arms=_arms(.12 * direction)))
            keys.append(_pose(pelvis(half_b), (target(next_v, -1, .08), [1, 0, 0], [1, 0, 0]),
                              (target(left_v, 1, .08), [1, 0, 0], [1, 0, 0]), arms=_arms(-.08 * direction)))
            keys.append(_pose(pelvis(half_c), (target(right_v, -1, .08), [1, 0, 0], [1, 0, 0]),
                              (target(next_v, 1, .22), [1, 0, -.7], [1, 0, 0]), arms=_arms(-.12 * direction)))
            right_v = left_v = next_v
        else:
            keys.append(_pose(pelvis(half_a), (target(right_v, -1, .08), [1, 0, 0], [1, 0, 0]),
                              (target(next_v, 1, .22), [1, 0, -.7], [1, 0, 0]), arms=_arms(-.12 * direction)))
            keys.append(_pose(pelvis(half_b), (target(right_v, -1, .08), [1, 0, 0], [1, 0, 0]),
                              (target(next_v, 1, .08), [1, 0, 0], [1, 0, 0]), arms=_arms(.08 * direction)))
            keys.append(_pose(pelvis(half_c), (target(next_v, -1, .22), [1, 0, -.7], [1, 0, 0]),
                              (target(left_v, 1, .08), [1, 0, 0], [1, 0, 0]), arms=_arms(.12 * direction)))
            right_v = left_v = next_v
        center = next_v
        keys.append(_pose(pelvis(center), (target(right_v, -1, .08), [1, 0, 0], [1, 0, 0]),
                          (target(left_v, 1, .08), [1, 0, 0], [1, 0, 0]), arms=_arms(.08 * direction)))
    return keys


def _timed_walk(start, end, *, y=0.0, axis="x", t0=.6, t1=.97):
    poses = _reset(start, end, y=y, axis=axis)
    return [(t0 + (t1 - t0) * i / (len(poses) - 1), pose) for i, pose in enumerate(poses)]


def jump_lateral():
    # Side-to-side broad two-foot jumps with a visible stick at each end.
    keys = [(0.00, _two(0, .94, 0, swing=-.35)), (0.10, _two(0, .56, 28, swing=-.55)),
            (0.24, _air2(0, .31, y=-.06, swing=.55)), (0.36, _two(0, .52, 30, y=-.46, swing=.48)),
            (0.46, _two(0, .94, 0, y=-.46, swing=.10)), (0.58, _two(0, .56, 28, y=-.46, swing=-.5)),
            (0.72, _air2(0, .31, y=-.40, swing=.55)), (0.84, _two(0, .52, 30, y=.0, swing=.48)),
            (0.94, _two(0, .94, 0, swing=.1))]
    return dict(seconds=4.0, keys=keys, cam_az=35, cam_el=14, props=[])


def jump_broad():
    # One long forward jump, held, then a stepping walk-back reset.
    keys = [(0.00, _two(0, .94, 0, swing=-.35)), (0.12, _two(0, .56, 28, p=12, swing=-.60)),
            (0.25, _air2(.48, .42, swing=.58)), (0.38, _air2(1.02, .32, swing=.42)),
            (0.49, _two(1.12, .56, 28, p=12, swing=.35)), (0.59, _two(1.12, .56, 26, p=10, swing=.2))]
    keys += _timed_walk(1.12, 0.0, t0=.64, t1=.98)
    return dict(seconds=5.0, keys=keys, cam_az=25, cam_el=12, props=[])


def jump_broad_consecutive():
    # Three forward contacts are brief and immediately reload into the next bound.
    keys = [(0.00, _two(0, .56, 26, swing=-.5)), (0.10, _air2(.42, .34, swing=.55)),
            (0.20, _two(.78, .61, 23, swing=.42)), (0.26, _two(.78, .53, 30, swing=-.38)),
            (0.37, _air2(1.16, .32, swing=.56)), (0.48, _two(1.53, .61, 23, swing=.42)),
            (0.54, _two(1.53, .53, 30, swing=-.38)), (0.65, _air2(1.91, .32, swing=.56)),
            (0.76, _two(2.28, .61, 23, swing=.35)), (0.84, _two(2.28, .61, 23, swing=.22))]
    keys += _timed_walk(2.28, 0.0, t0=.85, t1=.98)
    keys.append((.99, _two(0, .56, 26, swing=-.5)))
    return dict(seconds=7.0, keys=keys, cam_az=24, cam_el=12, props=[])


def cone_hops_front():
    # Small vertical forward hops over five low markers; each contact is quick.
    markers = [dict(kind="box", center=[x, 0, .025], size=[.07, .20, .05]) for x in (.32, .72, 1.12, 1.52)]
    keys = [(0.00, _two(0, .94, 0, swing=-.25)), (0.10, _two(0, .72, 18, swing=-.35)),
            (0.19, _air2(.18, .18, tuck=8, swing=.38)), (0.28, _two(.38, .72, 18, swing=.35)),
            (0.36, _air2(.58, .18, tuck=8, swing=-.3)), (0.45, _two(.78, .72, 18, swing=.35)),
            (0.53, _air2(.98, .18, tuck=8, swing=-.3)), (0.62, _two(1.18, .72, 18, swing=.35)),
            (0.70, _air2(1.38, .18, tuck=8, swing=-.3)), (0.79, _two(1.58, .72, 18, swing=.35))]
    keys += _timed_walk(1.58, 0.0, t0=.82, t1=.98)
    return dict(seconds=5.0, keys=keys, cam_az=22, cam_el=17, props=markers)


def cone_hops_lateral():
    # Feet stay paired as the body passes marker to marker sideways.
    markers = [dict(kind="box", center=[0, y, .025], size=[.20, .07, .05]) for y in (-.48, -.24, 0, .24, .48)]
    keys = [(0.00, _two(0, .94, 0, y=-.50, swing=-.25)), (0.10, _two(0, .70, 18, y=-.50, swing=-.35)),
            (0.20, _air2(0, .18, y=-.34, swing=.38)), (0.30, _two(0, .70, 18, y=-.24, swing=.3)),
            (0.40, _air2(0, .18, y=-.10, swing=-.3)), (0.50, _two(0, .70, 18, y=0, swing=.3)),
            (0.60, _air2(0, .18, y=.14, swing=-.3)), (0.70, _two(0, .70, 18, y=.24, swing=.3)),
            (0.78, _air2(0, .18, y=.38, swing=-.3)), (0.85, _two(0, .70, 18, y=.50, swing=.25)),
            (0.87, _two(0, .94, 0, y=.50, swing=0))]
    keys += _timed_walk(.50, -.50, axis="y", y=0.0, t0=.88, t1=.99)
    return dict(seconds=6.0, keys=keys, cam_az=40, cam_el=17, props=markers)


def skater_bound():
    # Bounds from one foot to the other; the free leg sweeps behind the landing leg.
    def landing(x, y, support_right, free_x, free_y, free_z, p):
        z = .91 if p < 15 else .70
        if support_right:
            right = ([x, y - .10, .08], [1, 0, -.2], [1, -.25, 0])
            left = ([free_x, free_y, free_z], [1, 0, -.45], [1, 0, -1])
        else:
            left = ([x, y + .10, .08], [1, 0, -.2], [1, .25, 0])
            right = ([free_x, free_y, free_z], [1, 0, -.45], [1, 0, -1])
        return _pose([x, y, z], right, left, p1=p, p2=p, arms=_arms(.36, .12))
    keys = [(0.00, landing(0, -.32, True, .08, .32, .45, 5)),
            (0.13, landing(0, -.32, True, .08, .32, .45, 5)),
            (0.25, _pose([0, 0, 1.18], ([0, -.10, .35], [1, 0, -.35], [1, 0, 0]),
                         ([.10, .10, .42], [1, 0, -.4], [1, 0, 0]), p1=4, p2=4, arms=_arms(.5, .25))),
            (0.38, landing(0, .32, False, -.08, -.32, .45, 26)),
            (0.52, landing(0, .32, False, -.08, -.32, .45, 5)),
            (0.64, _pose([0, 0, 1.18], ([0, -.10, .35], [1, 0, -.35], [1, 0, 0]),
                         ([.10, .10, .42], [1, 0, -.4], [1, 0, 0]), p1=4, p2=4, arms=_arms(-.5, -.25))),
            (0.77, landing(0, -.32, True, .08, .32, .45, 26)),
            (0.91, landing(0, -.32, True, .08, .32, .45, 5))]
    return dict(seconds=3.6, keys=keys, cam_az=35, cam_el=14, props=[])


def hop_single_leg_stick():
    # Same right leg takes off and lands; the two-second stick is the long landing hold.
    keys = [(0.00, _single(0, knee=0, lifted=(.15, .12, .38), swing=-.1)),
            (0.14, _single(0, z=.08, p=24, lifted=(.42, .12, .34), swing=-.4)),
            (0.30, _pose([.36, 0, 1.15], ([.36, -.10, .34], [1, 0, -.3], [1, 0, 0]),
                         ([.62, .10, .46], [1, 0, -.4], [1, 0, 0]), p1=8, p2=8, arms=_arms(.48))),
            (0.44, _single(.72, p=28, lifted=(.42, .12, .32), swing=.35)),
            (0.56, _single(.72, p=28, lifted=(.42, .12, .32), swing=.22)),
            (0.67, _single(.72, p=28, lifted=(.42, .12, .32), swing=.12)),
            (0.78, _single(.72, p=28, lifted=(.42, .12, .32), swing=.06)),
            (.80, _pose([.72, 0, .94], ([.72, -.10, .08], [1, 0, 0], [1, 0, 0]),
                        ([.72, .10, .08], [1, 0, 0], [1, 0, 0]), arms=_arms(-.12)))]
    keys += _timed_walk(.72, 0.0, t0=.82, t1=.98)
    keys.append((.99, _single(0, p=5, lifted=(.15, .12, .38), swing=-.1)))
    return dict(seconds=5.5, keys=keys, cam_az=25, cam_el=13, props=[])


def hop_single_leg_lateral():
    # Repeated small side-to-side contacts on the same right leg, with level hips.
    keys = [(0.00, _single(0, y=0, p=12, lifted=(.10, .12, .34), swing=-.2)),
            (0.12, _single(0, y=0, z=.20, p=4, lifted=(.1, .12, .34), swing=.2)),
            (0.23, _single(0, y=-.23, p=18, lifted=(.12, .12, .35), swing=.35)),
            (0.34, _single(0, y=-.23, p=8, lifted=(.1, .12, .34), swing=-.2)),
            (0.45, _single(0, y=0, z=.20, p=4, lifted=(.1, .12, .34), swing=.2)),
            (0.56, _single(0, y=.23, p=18, lifted=(.12, .12, .35), swing=.35)),
            (0.67, _single(0, y=.23, p=8, lifted=(.1, .12, .34), swing=-.2)),
            (0.78, _single(0, y=0, z=.20, p=4, lifted=(.1, .12, .34), swing=.2)),
            (0.89, _single(0, y=0, p=12, lifted=(.10, .12, .34), swing=-.2))]
    return dict(seconds=3.5, keys=keys, cam_az=40, cam_el=14, props=[])


def zigzag_hop():
    # Forward diagonal hops cross between two visible marker lanes.
    markers = [dict(kind="box", center=[x, y, .025], size=[.12, .08, .05])
               for x, y in ((.30, -.25), (.65, .25), (1.0, -.25), (1.35, .25), (1.70, -.25))]
    keys = [(0.00, _single(0, y=-.22, p=8, lifted=(.12, .12, .34), swing=-.1)),
            (0.12, _pose([.18, 0, 1.13], ([.18, -.10, .30], [1, 0, -.3], [1, 0, 0]),
                         ([.40, .10, .40], [1, 0, -.4], [1, 0, 0]), p1=8, p2=8, arms=_arms(.4))),
            (0.24, _single(.36, y=.22, p=20, lifted=(.40, .12, .34), swing=.3)),
            (0.36, _pose([.54, 0, 1.13], ([.54, -.10, .30], [1, 0, -.3], [1, 0, 0]),
                         ([.76, .10, .40], [1, 0, -.4], [1, 0, 0]), p1=8, p2=8, arms=_arms(-.4))),
            (0.48, _single(.72, y=-.22, p=20, lifted=(.40, .12, .34), swing=.3)),
            (0.60, _pose([.90, 0, 1.13], ([.90, -.10, .30], [1, 0, -.3], [1, 0, 0]),
                         ([1.12, .10, .40], [1, 0, -.4], [1, 0, 0]), p1=8, p2=8, arms=_arms(-.4))),
            (0.70, _single(1.08, y=.22, p=20, lifted=(.40, .12, .34), swing=.3)),
            (0.72, _pose([1.26, 0, 1.13], ([1.26, -.10, .30], [1, 0, -.3], [1, 0, 0]),
                         ([1.48, .10, .40], [1, 0, -.4], [1, 0, 0]), p1=8, p2=8, arms=_arms(-.4))),
            (0.78, _single(1.44, y=-.22, p=14, lifted=(.20, .12, .36), swing=.2))]
    keys.append((.80, _pose([1.44, 0, .94], ([1.44, -.10, .08], [1, 0, 0], [1, 0, 0]),
                            ([1.44, .10, .08], [1, 0, 0], [1, 0, 0]), arms=_arms(-.12))))
    keys += _timed_walk(1.44, 0.0, t0=.82, t1=.98)
    keys.append((.99, _single(0, y=-.22, p=8, lifted=(.12, .12, .34), swing=-.1)))
    return dict(seconds=5.5, keys=keys, cam_az=28, cam_el=16, props=markers)


def bounding_alternate():
    # Long airborne running strides alternate working leg and opposite arm drive.
    def stride(x, right_forward=True, air=False):
        lead_x = x + (.34 if air else .25)
        trail_x = x - (.16 if air else .35)
        if right_forward:
            r = ([lead_x, -.10, .33 if air else .08], [1, 0, -.35], [1, 0, 0])
            l = ([trail_x, .10, .27 if air else .18], [1, 0, -.6], [1, 0, -1])
            arm = dict(wrist_r_rel=[-.40, -.16, .10], wrist_l_rel=[.40, .16, -.02],
                       elbow_r=[0, -.2, 1], elbow_l=[0, .2, -1], hand_r=[1, 0, 0], hand_l=[1, 0, 0])
        else:
            r = ([trail_x, -.10, .27 if air else .18], [1, 0, -.6], [1, 0, -1])
            l = ([lead_x, .10, .33 if air else .08], [1, 0, -.35], [1, 0, 0])
            arm = dict(wrist_r_rel=[.40, -.16, -.02], wrist_l_rel=[-.40, .16, .10],
                       elbow_r=[0, -.2, -1], elbow_l=[0, .2, 1], hand_r=[1, 0, 0], hand_l=[1, 0, 0])
        return _pose([x, 0, 1.15 if air else .91], r, l, p1=10, p2=13, arms=arm)
    keys = [(0.00, _two(0, .94, 0)), (.03, stride(0, True)), (0.08, stride(.25, True, True)),
            (0.16, stride(.55, False)), (0.24, stride(.80, False, True)),
            (0.32, stride(1.10, True)), (0.40, stride(1.35, True, True)),
            (0.48, stride(1.65, False)), (0.56, stride(1.90, False, True)),
            (0.64, stride(2.20, True)),
            (.66, _two(2.20, .91, 0, swing=-.12))]
    keys += _timed_walk(2.20, 0.0, t0=.68, t1=.98)
    return dict(seconds=6.5, keys=keys, cam_az=25, cam_el=10, props=[])


def bounding_single_leg():
    # Repeated forward bounds all land on the right leg; the left leg cycles through.
    keys = [(0.00, _single(0, p=8, lifted=(.18, .12, .38), swing=-.15)),
            (0.13, _pose([.20, 0, 1.20], ([.20, -.10, .34], [1, 0, -.3], [1, 0, 0]),
                         ([.50, .10, .48], [1, 0, -.5], [1, 0, 0]), p1=8, p2=10, arms=_arms(.42))),
            (0.26, _single(.42, p=22, lifted=(.42, .12, .34), swing=.35)),
            (0.39, _pose([.62, 0, 1.20], ([.62, -.10, .34], [1, 0, -.3], [1, 0, 0]),
                         ([.92, .10, .48], [1, 0, -.5], [1, 0, 0]), p1=8, p2=10, arms=_arms(.42))),
            (0.52, _single(.84, p=22, lifted=(.42, .12, .34), swing=.35)),
            (0.58, _pose([1.04, 0, 1.20], ([1.04, -.10, .34], [1, 0, -.3], [1, 0, 0]),
                         ([1.34, .10, .48], [1, 0, -.5], [1, 0, 0]), p1=8, p2=10, arms=_arms(.42))),
            (0.68, _single(1.26, p=22, lifted=(.42, .12, .34), swing=.35)),
            (.69, _pose([1.26, 0, .94], ([1.26, -.10, .08], [1, 0, 0], [1, 0, 0]),
                        ([1.26, .10, .08], [1, 0, 0], [1, 0, 0]), arms=_arms(-.12)))]
    keys += _timed_walk(1.26, 0.0, t0=.70, t1=.98)
    keys.append((.99, _single(0, p=8, lifted=(.18, .12, .38), swing=-.15)))
    return dict(seconds=5.5, keys=keys, cam_az=24, cam_el=12, props=[])


def box_step_off_landing():
    # 30 cm box: step one foot down, then the other; settle and step back up.
    h = .30
    box = dict(kind="box", center=[-.42, 0, h / 2], size=[.52, .60, h])
    def key(px, pz, ar, al, bend=0, arms=None):
        return _pose([px, 0, pz], ar, al, p1=3, p2=4, arms=arms or _arms(-.1))
    on = ([ -.42, -.10, h + .08], [1, 0, 0], [1, 0, 0])
    ground_r = ([.24, -.10, .08], [1, 0, 0], [1, 0, 0])
    ground_l = ([.24, .10, .08], [1, 0, 0], [1, 0, 0])
    up = ([ -.42, -.10, h + .08], [1, 0, 0], [1, 0, 0])
    keys = [(0, key(-.42, 1.20, on, ([-.42, .10, h + .08], [1, 0, 0], [1, 0, 0]))),
            (.16, key(-.20, 1.00, on, ([ -.12, .10, .25], [1, 0, -.7], [1, 0, 0]))),
            (.28, key(.15, .91, ground_r, ([ -.42, .10, h + .08], [1, 0, 0], [1, 0, 0]))),
            (.38, key(.24, .68, ground_r, ground_l, arms=_arms(.12))),
            (.52, key(.24, .68, ground_r, ground_l, arms=_arms(.08))),
            (.67, key(.05, .91, ([ -.42, -.10, h + .08], [1, 0, 0], [1, 0, 0]), ground_l)),
            (.78, key(-.20, 1.00, on, ([ -.12, .10, .25], [1, 0, -.7], [1, 0, 0]))),
            (.90, key(-.42, 1.20, on, ([-.42, .10, h + .08], [1, 0, 0], [1, 0, 0])))]
    return dict(seconds=4.0, keys=keys, cam_az=28, cam_el=12, props=[box])


def box_push_off_alternating():
    # A 20 cm step supports alternating feet while the opposite leg lands on the floor.
    h = .20
    box = dict(kind="box", center=[0, 0, h / 2], size=[.52, .62, h])
    def state(box_leg, flight=False):
        bx = 0.0
        floor_x = .30
        if box_leg == "r":
            r = ([bx if not flight else .08, -.10, h + .08 if not flight else .38], [1, 0, -.15 if flight else 0], [1, 0, 0])
            l = ([floor_x if not flight else .34, .10, .08 if not flight else .29], [1, 0, -.3], [1, 0, 0])
        else:
            l = ([bx if not flight else .08, .10, h + .08 if not flight else .38], [1, 0, -.15 if flight else 0], [1, 0, 0])
            r = ([floor_x if not flight else .34, -.10, .08 if not flight else .29], [1, 0, -.3], [1, 0, 0])
        return _pose([.12 if flight else .04, 0, 1.12 if flight else .94], r, l, p1=2, p2=4,
                     arms=_arms(.45 if flight else -.25))
    keys = [(0.00, state("r")), (.18, state("r", True)), (.34, state("l")),
            (.50, state("l", True)), (.66, state("r")), (.82, state("r", True)), (.98, state("r"))]
    return dict(seconds=3.6, keys=keys, cam_az=30, cam_el=12, props=[box])


def box_push_off_single():
    # Right foot repeatedly pushes vertically from and returns to a 20 cm step.
    h = .20
    box = dict(kind="box", center=[0, 0, h / 2], size=[.52, .62, h])
    def key(stage):
        z, pelvis_z, left_z = {
            "stick": (h + .08, 1.04, .35),
            "takeoff": (.43, 1.20, .43),
            "apex": (.62, 1.38, .60),
        }[stage]
        return _pose([.04 if stage == "stick" else .08, 0, pelvis_z],
                     ([.04, -.10, z], [1, 0, -.25 if stage != "stick" else 0], [1, -.1, 0]),
                     ([.20, .10, left_z], [1, 0, -.45], [1, 0, 0]),
                     p1=5, p2=5, arms=_arms(.35 if stage != "stick" else -.15))
    keys = [(0.00, key("stick")), (.12, key("takeoff")), (.22, key("apex")),
            (.31, key("takeoff")), (.39, key("stick")), (.48, key("takeoff")),
            (.58, key("apex")), (.67, key("takeoff")), (.75, key("stick")),
            (.84, key("takeoff")), (.93, key("apex")), (.99, key("stick"))]
    return dict(seconds=4.0, keys=keys, cam_az=25, cam_el=12, props=[box])


def box_jump():
    # Jump up onto a 30 cm box, stand, then step down to the floor.
    h = .30
    box = dict(kind="box", center=[.52, 0, h / 2], size=[.62, .72, h])
    # Keep the approach back from the box's near edge (x=.21). Lift first, then
    # travel forward while airborne so the knees clear the solid box face.
    start = _two(-.42, .94, 0, swing=-.25)
    crouch = _two(-.42, .58, 27, swing=-.55)
    takeoff = _pose([-.42, 0, 1.12], ([-.42, -.10, .30], [1, 0, -.3], [1, 0, 0]),
                    ([-.42, .10, .30], [1, 0, -.3], [1, 0, 0]), p1=7, p2=8, arms=_arms(.4))
    air = _pose([.08, 0, 1.28], ([.08, -.10, .53], [1, 0, -.25], [1, 0, 0]),
                ([.08, .10, .53], [1, 0, -.25], [1, 0, 0]), p1=6, p2=8, arms=_arms(.4))
    land = _pose([.52, 0, .92], ([.52, -.10, h + .08], [1, 0, 0], [1, -.15, 0]),
                 ([.52, .10, h + .08], [1, 0, 0], [1, .15, 0]), p1=10, p2=12, arms=_arms(.28))
    stand = _pose([.52, 0, h + .975], ([.52, -.10, h + .08], [1, 0, 0], [1, 0, 0]),
                  ([.52, .10, h + .08], [1, 0, 0], [1, 0, 0]), arms=_arms(-.10))
    step = _pose([1.02, 0, .96], ([1.20, -.10, .18], [1, 0, -.7], [1, 0, 0]),
                 ([.52, .10, h + .08], [1, 0, 0], [1, 0, 0]), arms=_arms(.12))
    clear = _pose([1.20, 0, .94], ([1.20, -.10, .08], [1, 0, 0], [1, 0, 0]),
                  ([.98, .10, .55], [1, 0, -.7], [1, 0, 0]), arms=_arms(.06))
    down = _pose([1.20, 0, .94], ([1.20, -.10, .08], [1, 0, 0], [1, 0, 0]),
                 ([1.20, .10, .08], [1, 0, 0], [1, 0, 0]), arms=_arms(.06))
    sidewalk = _timed_walk(0.0, -.55, axis="y", y=1.20, t0=.72, t1=.765)
    backwalk = _timed_walk(1.20, -.42, y=-.55, t0=.77, t1=.88)
    close = _timed_walk(-.55, 0.0, axis="y", y=-.42, t0=.885, t1=.93)
    keys = [(0, start), (.10, crouch), (.17, takeoff), (.25, air), (.38, land), (.48, stand),
            (.58, step), (.64, clear), (.70, down)]
    keys += sidewalk + backwalk + close
    keys.append((.99, _two(-.42, .94, 0, swing=-.25)))
    return dict(seconds=7.0, keys=keys, cam_az=25, cam_el=12, props=[box])


def box_jump_rebound():
    # Jump up; hop down to the floor; immediate floor contact rebounds back onto the box.
    h = .30
    box = dict(kind="box", center=[.48, 0, h / 2], size=[.62, .72, h])
    def onbox(bend=0, x=.48):
        return _pose([x, 0, h + (.66 if bend else .975)],
                     ([x, -.10, h + .08], [1, 0, 0], [1, -.15 if bend else 0, 0]),
                     ([x, .10, h + .08], [1, 0, 0], [1, .15 if bend else 0, 0]),
                     p1=12 if bend else 2, p2=14 if bend else 4, arms=_arms(.25 if bend else -.15))
    crouch = _two(-.42, .57, 28, swing=-.5)
    flight_clear = _pose([-.16, 0, 1.00], ([-.16, -.10, .34], [1, 0, -.3], [1, 0, 0]),
                         ([-.16, .10, .34], [1, 0, -.3], [1, 0, 0]), p1=8, p2=9, arms=_arms(.45))
    air_up = _pose([.10, 0, 1.22], ([.10, -.10, .43], [1, 0, -.3], [1, 0, 0]),
                   ([.10, .10, .43], [1, 0, -.3], [1, 0, 0]), p1=8, p2=9, arms=_arms(.52))
    boxland = onbox(12)
    down_air = _pose([1.15, 0, 1.23], ([1.15, -.10, .39], [1, 0, -.45], [1, 0, 0]),
                     ([1.15, .10, .39], [1, 0, -.45], [1, 0, 0]), p1=6, p2=7, arms=_arms(.38))
    floorland = _two(1.15, .55, 29, swing=-.32)
    rebound = _pose([1.15, 0, 1.38], ([1.15, -.10, .53], [1, 0, -.3], [1, 0, 0]),
                    ([1.15, .10, .53], [1, 0, -.3], [1, 0, 0]), p1=5, p2=6, arms=_arms(.55))
    rebound_travel = _pose([.78, 0, 1.38], ([.78, -.10, .53], [1, 0, -.3], [1, 0, 0]),
                           ([.78, .10, .53], [1, 0, -.3], [1, 0, 0]), p1=5, p2=6, arms=_arms(.45))
    movement = [(0, crouch), (.065, flight_clear), (.13, air_up), (.26, boxland), (.34, onbox()),
            (.68, down_air), (.73, floorland), (.77, rebound), (.81, rebound_travel), (.86, boxland),
            (.88, onbox()), (.90, _pose([.85, 0, 1.06], ([.90, -.10, .28], [1, 0, -.7], [1, 0, 0]),
                                           ([.48, .10, h + .08], [1, 0, 0], [1, 0, 0]), arms=_arms(.12))),
            (.915, _pose([1.00, 0, .99], ([1.00, -.10, .08], [1, 0, 0], [1, 0, 0]),
                                            ([.96, .10, .48], [1, 0, -.7], [1, 0, 0]), arms=_arms(.06))),
            (.94, floorland)]
    scale = .68 / .94
    keys = [(t * scale, pose) for t, pose in movement]
    keys += _timed_walk(0.0, -.55, axis="y", y=1.15, t0=.681, t1=.73)
    keys += _timed_walk(1.15, -.42, y=-.55, t0=.74, t1=.91)
    keys += _timed_walk(-.55, 0.0, axis="y", y=-.42, t0=.92, t1=.98)
    keys.append((.99, _two(-.42, .94, 0, swing=-.25)))
    return dict(seconds=8.3, keys=keys, cam_az=27, cam_el=13, props=[box])


def depth_jump():
    # Step off a 30 cm box (never jump down), absorb the floor landing, then jump vertically.
    h = .30
    box = dict(kind="box", center=[-.42, 0, h / 2], size=[.52, .62, h])
    top = _pose([-.42, 0, h + .975], ([-.42, -.10, h + .08], [1, 0, 0], [1, 0, 0]),
                ([-.42, .10, h + .08], [1, 0, 0], [1, 0, 0]), arms=_arms(-.18))
    step = _pose([-.12, 0, 1.12], ([-.42, -.10, h + .08], [1, 0, 0], [1, 0, 0]),
                 ([.10, .10, .25], [1, 0, -.7], [1, 0, 0]), arms=_arms(.12))
    fall = _pose([.30, 0, 1.10], ([.30, -.10, .27], [1, 0, -.45], [1, 0, 0]),
                 ([.30, .10, .27], [1, 0, -.45], [1, 0, 0]), p1=4, p2=6, arms=_arms(.32))
    land = _two(.30, .56, 28, swing=-.3)
    rebound = _pose([.30, 0, 1.22], ([.30, -.10, .40], [1, 0, -.25], [1, 0, 0]),
                    ([.30, .10, .49], [1, 0, -.25], [1, 0, 0]), p1=4, p2=5, arms=_arms(.55))
    apex = _pose([.30, 0, 1.43], ([.30, -.10, .57], [1, 0, -.18], [1, 0, 0]),
                 ([.30, .10, .57], [1, 0, -.18], [1, 0, 0]), p1=3, p2=4, arms=_arms(.32))
    jump_land = _two(.30, .57, 27, swing=.1)
    keys = [(0, top), (.12, step), (.22, fall), (.30, land), (.34, land),
            (.45, rebound), (.54, apex), (.63, jump_land), (.71, jump_land)]
    keys += _timed_walk(0.0, -.55, axis="y", y=.30, t0=.72, t1=.765)
    keys += _timed_walk(.30, -.90, y=-.55, t0=.77, t1=.88)
    keys += _timed_walk(-.55, 0.0, axis="y", y=-.90, t0=.885, t1=.93)
    step_up_r = _pose([-.82, 0, .94], ([-.42, -.10, .47], [1, 0, -.2], [1, 0, 0]),
                      ([-.90, .10, .08], [1, 0, 0], [1, 0, 0]), arms=_arms(.1))
    step_up_l = _pose([-.55, 0, 1.10], ([-.42, -.10, h + .08], [1, 0, 0], [1, 0, 0]),
                      ([-.42, .10, .50], [1, 0, -.4], [1, 0, 0]), arms=_arms(.1))
    keys += [(.945, step_up_r), (.97, step_up_l), (.99, top)]
    return dict(seconds=8.0, keys=keys, cam_az=28, cam_el=12, props=[box])


EXERCISES = {
    "jump-lateral": jump_lateral,
    "jump-broad": jump_broad,
    "jump-broad-consecutive": jump_broad_consecutive,
    "cone-hops-front": cone_hops_front,
    "cone-hops-lateral": cone_hops_lateral,
    "skater-bound": skater_bound,
    "hop-single-leg-stick": hop_single_leg_stick,
    "hop-single-leg-lateral": hop_single_leg_lateral,
    "zigzag-hop": zigzag_hop,
    "bounding-alternate": bounding_alternate,
    "bounding-single-leg": bounding_single_leg,
    "box-step-off-landing": box_step_off_landing,
    "box-push-off-alternating": box_push_off_alternating,
    "box-push-off-single": box_push_off_single,
    "box-jump": box_jump,
    "box-jump-rebound": box_jump_rebound,
    "depth-jump": depth_jump,
}
