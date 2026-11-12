"""Batch A (doc 43, lane L6): ten clips rebuilt from filmed references.

Each exercise is a continuous trajectory (sample_continuous with pauses where the movement
rests), built with the small constraint solvers in motion_constraints.py instead of typed
joint positions, with the anatomy_detail torso. References: motion_references.json,
`batch_references`; contracts: motion_contracts.json (movement_class + identity checks).

World axes as everywhere: Z up, figure faces +X, +Y is the figure's left.
"""
import math

from motion_constraints import bisect_target, planted_ankle
from poses import (L, add, base_pose, both, cross, mul, norm, sample_continuous, solve, sub, trunk_frame, unit)

FPS = 24
L_SH = .47


def _c(d): return math.cos(math.radians(d))
def _s(d): return math.sin(math.radians(d))


def _joints(key):
    return solve(key, {})


def _dense(fn, phases, seconds):
    """One pose per frame from `fn(**params)`, params following monotone-Hermite phases.

    Pauses are consecutive phases with equal values (zero tangent), so the movement
    eases in and out of them. The last frame is the one before the loop closes.
    """
    n = int(round(seconds * FPS))
    return [(i / n, fn(**sample_continuous(phases, i / n))) for i in range(n)]


# ---------------------------------------------------------------- 1. long-lever (RKC) plank
def plank_long_lever():
    """RKC plank: elbows ahead of the shoulders (under the eyes), forearms converging to fists
    that touch, pelvis tucked, feet together. References: Nichols, Exercise Encyclopedia, Baird."""
    ahead = .20           # elbow ahead of the shoulder along the body (references: 0.15 to 0.25 m)
    elbow_y = .17         # elbows about shoulder width
    fist_y = .045         # fists touch: forearms converge
    az, toe_x, toe_h = .111, -1.40, .051
    elbow_z = .052

    def key(a, breath=0.0):
        k = base_pose(pelvis=[0, 0, 1], p1=90 - a - 5, p2=90 - a, hp=-12 + breath)
        # Pelvis at 0.898 from the planted ankles along the body line, as in the archer push-up.
        ar = math.radians(a)
        k['pelvis'] = [toe_x - .18 + math.cos(ar) * .898, 0, az + math.sin(ar) * .898]
        J = _joints(k)
        for sg, name in ((1, 'l'), (-1, 'r')):
            sh = J['shoulder_' + name]
            elbow = [sh[0] + ahead, sg * elbow_y, elbow_z]
            dy = elbow[1] - sg * fist_y
            wrist = [elbow[0] + math.sqrt(L['fore'] ** 2 - dy ** 2 - .0004), sg * fist_y, elbow_z - .002]
            line = sub(wrist, sh)
            along = mul(line, sum(x * y for x, y in zip(sub(elbow, sh), line)) / norm(line) ** 2)
            pole = sub(sub(elbow, sh), along)
            k.update({'wrist_' + name: wrist, 'elbow_' + name: pole, 'hand_' + name: [1, 0, 0],
                      'palm_' + name: [0, -sg, 0], 'grip_' + name: .9})
            hip = J['hip_' + name]
            toe = [toe_x, sg * .045, az - .06]
            k['ankle_' + name] = planted_ankle(hip, toe, [toe_x - .18, sg * .05, az], side=-sg)
            k['foot_' + name] = sub(toe, k['ankle_' + name])
            k['knee_' + name] = [0, 0, 1]
        k['head_detail'] = 1
        return k

    # Shoulder height follows from the upper arm resting the elbow on the floor.
    def build(breath):
        def shoulder_z(a):
            return _joints(key_stub(a))['shoulder_l'][2]
        def key_stub(a):
            k = base_pose(pelvis=[0, 0, 1], p1=90 - a - 5, p2=90 - a, hp=-12)
            ar = math.radians(a)
            k['pelvis'] = [toe_x - .18 + math.cos(ar) * .898, 0, az + math.sin(ar) * .898]
            return k
        target = elbow_z + math.sqrt(L['upper'] ** 2 - ahead ** 2)
        a = bisect_target(shoulder_z, 2, 14, target)
        return key(a, breath)

    a_key = build(0.0)
    b_key = build(2.0)
    keys = [(0, a_key), (.25, a_key), (.5, b_key), (.75, a_key)]
    return dict(seconds=3.0, keys=keys, cam_az=32, cam_el=14, props=[], anatomy_detail=True,
                motion_profile='plank-long-lever')


# ---------------------------------------------------------------- 2. single-leg glute bridge
def _bridge(single):
    """Working foot planted, shoulders on the floor, hips level, free leg straight and parallel to
    (in line with) the working thigh. References: MedStar Health (side), Runna (front-right)."""
    chest = [0.0, 0.0, .13]
    top_deg = 22.0
    ankle_x = {}

    def pelvis_for(phi):
        d = [_c(phi), 0, _s(phi)]
        return add(chest, mul(d, .5)), d

    # Planted heel: fixed at the x where the top shin stands almost upright with the thigh collinear
    # with the trunk (shoulder, hip, knee on one line at the top).
    p_top, d_top = pelvis_for(top_deg)
    hip_top = add(p_top, mul(d_top, .03))
    knee_top = add(hip_top, mul(d_top, L['thigh']))
    dz = knee_top[2] - .08
    ax = knee_top[0] - math.sqrt(L['shin'] ** 2 - dz ** 2)
    heel = [ax, -.10, .08]

    def pose(phi):
        p, d = pelvis_for(phi)
        k = base_pose(pelvis=p, p1=-90 - phi, p2=-90 - phi, hp=phi)
        J = _joints(k)
        hip_r, hip_l = J['hip_r'], J['hip_l']
        # working (right) leg: planted heel, knee towards the ceiling/forward
        to_heel = sub(heel, hip_r)
        mid_pole = [0, 0, 1]
        k.update(ankle_r=heel, knee_r=[1, 0, .8], foot_r=[1, 0, 0])
        if not single:
            k.update(ankle_l=[heel[0], .10, .08], knee_l=[1, 0, .8], foot_l=[1, 0, 0])
            k.update(both(wrist=[J['shoulder_l'][0] + .5, .26, .035], hand=[1, 0, 0], palm=[0, 0, -1], grip=0.0, elbow=[0, 1, .4]))
            k['head_detail'] = 1
            return k
        # free (left) leg: straight, parallel to the working thigh
        J2 = _joints({**k, 'knee_l': [1, 0, .8], 'ankle_l': add(hip_l, [0, 0, -.8]), 'foot_l': [1, 0, 0]})
        thigh = unit(sub(J2['knee_r'], J2['hip_r']))
        k.update(ankle_l=add(hip_l, mul(thigh, (L['thigh'] + L['shin']) * .9998)), knee_l=[0, 0, 1], foot_l=[0, 0, 1])
        sh = J['shoulder_l']
        k.update(both(wrist=[sh[0] + .5, .26, .035], hand=[1, 0, 0], palm=[0, 0, -1], grip=0.0,
                      elbow=[0, 1, .4]))
        k['head_detail'] = 1
        return k

    phases = [(0, dict(phi=0.0)), (.08, dict(phi=0.0)), (.38, dict(phi=top_deg)), (.60, dict(phi=top_deg)),
              (.92, dict(phi=0.0))]
    keys = _dense(lambda phi: pose(phi), phases, 4.0)
    return dict(seconds=4.0, keys=keys, cam_az=22, cam_el=12, props=[], anatomy_detail=True,
                motion_profile='bridge-single-leg' if single else 'glute-bridge')


def bridge_single_leg():
    return _bridge(True)


def glute_bridge():
    return _bridge(False)


EXERCISES = {
    'plank-long-lever': plank_long_lever,
    'bridge-single-leg': bridge_single_leg,
    'glute-bridge': glute_bridge,
}
