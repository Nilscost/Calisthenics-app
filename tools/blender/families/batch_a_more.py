"""Batch A, part 2 (doc 43, lane L6): the other seven clips. Same method as batch_a.py."""
import math

from motion_constraints import bisect_target, planted_ankle
from poses import (L, add, base_pose, both, cross, mul, norm, sample_continuous, sub, unit)
from families.batch_a import FPS, _c, _dense, _joints, _s


# ---------------------------------------------------------------- 3. side plank (forearm, hold)
def side_plank():
    """Side plank on the forearm: straight line ankle-hip-shoulder, feet stacked, top hand on the
    hip. References: Runna (front oblique), NASM (front). A hold with a small breath."""
    from families.core import _side_forearm_base

    def pose(pz):
        # pz: pelvis height; the bottom shoulder sits one upper-arm length above the elbow on the floor.
        k = _side_forearm_base(pz, (-0.80, 0.0, 0.25), bottom_ankle=(-0.80, 0.0, 0.08))
        J = _joints(k)
        sh, hip = J['shoulder_l'], J['hip_r']
        elbow = [sh[0], sh[1], sh[2] - L['upper'] * .995]
        k.update(wrist_l=[sh[0], sh[1] + .26, .04], elbow_l=[0, -.2, -1], hand_l=[0, 1, 0],
                 wrist_r=[hip[0] + .02, hip[1], hip[2] + .05], elbow_r=[-1, 0, 1], hand_r=[1, 0, 0], palm_r=[0, 1, 0])
        k['head_detail'] = 1
        return k
    a, b = pose(.565), pose(.561)
    keys = [(0, a), (.3, a), (.5, b), (.8, a)]
    return dict(seconds=3.0, keys=keys, cam_az=50, cam_el=8, props=[], anatomy_detail=True,
                motion_profile='side-plank')


def _toe_planted(toe, theta, y):
    """Ankle and foot direction of a foot whose toe stays at `toe` while the heel rises by theta deg."""
    ankle = [toe[0] - .2 * _c(theta), y, toe[2] + .2 * _s(theta)]
    return ankle, [_c(theta), 0, -_s(theta)]


# ---------------------------------------------------------------- 4. walking lunge (alternating)
def walking_lunge():
    """Forward lunge with each leg in turn, back to standing between (in-place loop, left then right).
    The leading foot steps out and plants, the other heel rises on its toes, knees reach about 90 deg."""
    stride = .80
    y = {'l': .10, 'r': -.10}

    def pose(lead, step, depth):
        rear = 'l' if lead == 'r' else 'r'
        k = base_pose(pelvis=[.35 * step, 0, .955 - .395 * depth], p1=4 * step, p2=6 * step, hp=-6,
                      **both(wrist_rel=[.02, .06, -.5]))
        # rear foot stays where it stood; its heel rises as the body moves forward and down
        toe = [.20, y[rear], .08 + .045 * step]
        ankle, foot = _toe_planted(toe, 55 * step, y[rear])
        k.update({'ankle_' + rear: ankle, 'foot_' + rear: foot, 'knee_' + rear: [0, 0, -1 if depth > .5 else 1]})
        lift = .16 * math.sin(math.pi * step)
        k.update({'ankle_' + lead: [stride * step, y[lead], .08 + lift], 'foot_' + lead: [1, 0, -.15 * math.sin(math.pi * step)],
                  'knee_' + lead: [1, 0, 0]})
        k['head_detail'] = 1
        return k
    seq = [(0, dict(step=0., depth=0.)), (.05, dict(step=0., depth=0.)), (.22, dict(step=1., depth=.7)),
           (.32, dict(step=1., depth=1.)), (.38, dict(step=1., depth=1.)), (.50, dict(step=1., depth=.7)),
           (.64, dict(step=0., depth=0.)), (.72, dict(step=0., depth=0.)), (1.0, dict(step=0., depth=0.))]
    n = int(6.5 * FPS)

    def frame(i):
        t = i / n
        lead = 'r' if t < .5 else 'l'
        u = (t % .5) * 2
        return pose(lead, **sample_continuous(seq, u))
    # first half right, second half left; each half is one pass through `seq` up to its rest
    keys = [(i / n, frame(i)) for i in range(n)]
    return dict(seconds=6.5, keys=keys, cam_az=30, cam_el=8, props=[], anatomy_detail=True, motion_profile='walking-lunge')


# ---------------------------------------------------------------- 5. split squat
def split_squat():
    """Static stance, down and up: front foot flat and planted, rear heel raised on the toes."""
    ra, rf = _toe_planted([-.50, .10, .05], 60, .10)

    def pose(d):
        k = base_pose(pelvis=[.04 * d, 0, .850 - .350 * d], p1=2 + 4 * d, p2=3 + 6 * d, hp=-5,
                      ankle_r=[.45, -.10, .08], foot_r=[1, 0, 0], knee_r=[1, 0, 0],
                      ankle_l=ra, foot_l=rf, knee_l=[0, 0, -1], **both(wrist_rel=[.02, .06, -.5]))
        k['head_detail'] = 1
        return k
    phases = [(0, dict(d=0.)), (.12, dict(d=0.)), (.45, dict(d=1.)), (.58, dict(d=1.)), (.92, dict(d=0.))]
    return dict(seconds=3.6, keys=_dense(lambda d: pose(d), phases, 3.6), cam_az=28, cam_el=8, props=[],
                anatomy_detail=True, motion_profile='split-squat')


# ---------------------------------------------------------------- 6. parallel-bar dip
def dip_parallel():
    """Support on parallel bars, lower until the elbows are near 90 deg with a forward lean, press up.
    Knees bent behind. References: Live Lean TV (side), One Minute Tutorial."""
    bars = [dict(kind='rod', a=[-.20, .25, 1.0], b=[.20, .25, 1.0], r=.017),
            dict(kind='rod', a=[-.20, -.25, 1.0], b=[.20, -.25, 1.0], r=.017)]
    wx, wz = 0.0, 1.05

    def pose(d):
        lean = 10 + 14 * d
        drop = .565 - (.565 - .26) * d
        dx = .02 + .07 * d
        sh_mid = [wx + dx, 0, wz + math.sqrt(max(drop ** 2 - dx ** 2, 0))]
        up = [_s(lean), 0, _c(lean)]
        pelvis = sub(sh_mid, mul(up, .47))
        k = base_pose(pelvis=pelvis, p1=lean, p2=lean, hp=-4 - 6 * d,
                      **both(wrist=[wx, .25, wz], elbow=[-1, .25, 0], hand=[1, 0, 0], palm=[0, 0, -1], grip=.9,
                             knee=[1, 0, -.2], ankle_rel=[-.30, .07, -.60], foot=[.2, 0, -1]))
        k['head_detail'] = 1
        return k
    phases = [(0, dict(d=0.)), (.12, dict(d=0.)), (.50, dict(d=1.)), (.60, dict(d=1.)), (.92, dict(d=0.))]
    return dict(seconds=3.6, keys=_dense(lambda d: pose(d), phases, 3.6), cam_az=32, cam_el=8, props=bars,
                anatomy_detail=True, motion_profile='dip-parallel')


# ---------------------------------------------------------------- 7. inverted row
def inverted_row():
    """Straight body under a low bar, heels planted, chest to the bar. Uses the family's row solver
    (shoulders on the body line from the heels), now with pauses and a continuous path."""
    from families.pull_row import LOW_Z, _ankles, _row_pose, _S_from_ankle
    A = (1.287, .08)

    def pose(ang):
        k = _row_pose(_S_from_ankle(A, ang), ang, _ankles(*A))
        k['head_detail'] = 1
        return k
    phases = [(0, dict(ang=-34.0)), (.10, dict(ang=-34.0)), (.46, dict(ang=-12.1)), (.58, dict(ang=-12.1)), (.92, dict(ang=-34.0))]
    return dict(seconds=3.6, keys=_dense(lambda ang: pose(ang), phases, 3.6), cam_az=30, cam_el=14,
                props=[dict(kind='bar', z=LOW_Z, half=.65, r=.017)], anatomy_detail=True, motion_profile='inverted-row')


# ---------------------------------------------------------------- 8. plank shoulder taps (alternating)
def plank_shoulder_tap():
    """High plank, straight support arms, hips stable; each hand lifts straight up, crosses to touch the
    opposite shoulder and returns. Right hand then left hand in one loop."""
    toe_x, az, wy, fy = -1.40, .17, .19, .08

    def base(a):
        ar = math.radians(a)
        return base_pose(pelvis=[toe_x - .16 + math.cos(ar) * .898, 0, az + math.sin(ar) * .898], p1=90 - a, p2=90 - a, hp=-14)

    def shoulder_dist(a):
        J = _joints(base(a))
        return norm(sub(J['shoulder_l'], [J['shoulder_l'][0], wy, .035]))
    a = bisect_target(shoulder_dist, 6, 40, .565)
    k0 = base(a)
    J0 = _joints(k0)
    wx = J0['shoulder_l'][0]

    def pose(rl, rc, ll, lc):
        k = dict(k0)
        for sg, name, lift, cross_ in ((-1, 'r', rl, rc), (1, 'l', ll, lc)):
            floor = [wx, sg * wy, .035]
            opp = J0['shoulder_' + ('r' if name == 'l' else 'l')]
            tap = [opp[0] + .10, opp[1] * .75, opp[2] + .02]
            w = [floor[i] * (1 - cross_) + tap[i] * cross_ for i in range(3)]
            w[2] += .26 * lift * (1 - cross_) + .10 * lift * cross_
            k.update({'wrist_' + name: w, 'elbow_' + name: [-1, sg * .6, .4], 'hand_' + name: [1, -sg * .6 * cross_, 0],
                      'palm_' + name: [0, 0, -1], 'grip_' + name: 0.0})
        for sg, name in ((1, 'l'), (-1, 'r')):
            toe = [toe_x, sg * fy, az - .12]
            k['ankle_' + name] = planted_ankle(J0['hip_' + name], toe, [toe_x - .16, sg * fy, az], side=-sg)
            k['foot_' + name] = sub(toe, k['ankle_' + name])
            k['knee_' + name] = [0, 0, 1]
        k['head_detail'] = 1
        return k
    r0 = dict(rl=0., rc=0., ll=0., lc=0.)
    def P(t, **o): return (t, {**r0, **o})
    ph = [P(0), P(.05), P(.12, rl=1.), P(.21, rl=1., rc=1.), P(.26, rl=1., rc=1.), P(.35, rl=1.), P(.42), P(.52),
          P(.57, ll=1.), P(.66, ll=1., lc=1.), P(.71, ll=1., lc=1.), P(.80, ll=1.), P(.87), P(.93)]
    return dict(seconds=4.8, keys=_dense(pose, ph, 4.8), cam_az=42, cam_el=10, props=[],
                anatomy_detail=True, camera_padding=.06, motion_profile='plank-shoulder-tap')


# ---------------------------------------------------------------- 9. Russian twist (alternating)
def russian_twist():
    """Seated V: trunk leaned back about 40 deg, feet lifted, hands together; the chest turns to each
    side in turn and back to centre. References: Leap Fitness, Howcast, Runna."""
    def pose(tw):
        k = base_pose(pelvis=[0, 0, .145], p1=-48, p2=-48, hp=16, thoracic_twist=tw,
                      ankle_l=[.46, .07, .22], ankle_r=[.46, -.07, .22], foot_l=[1, 0, -.1], foot_r=[1, 0, -.1],
                      knee_l=[0, 0, 1], knee_r=[0, 0, 1])
        J = _joints(k)
        fwd = unit(cross(J['chest_left'], J['chest_up']))
        swing = tw / 62.0
        mid = add(add(add(J['chest'], mul(fwd, .30 - .06 * abs(swing))), mul(J['chest_up'], -.06 - .10 * abs(swing))), mul(J['chest_left'], -.30 * swing))
        for sg, name in ((1, 'l'), (-1, 'r')):
            k.update({'wrist_' + name: add(mid, mul(J['chest_left'], sg * .03)),
                      'elbow_' + name: add(mul(J['chest_left'], sg), [0, 0, -.4]),
                      'hand_' + name: fwd, 'palm_' + name: mul(J['chest_left'], -sg), 'grip_' + name: .6})
        k['head_detail'] = 1
        return k
    ph = [(0, dict(tw=0.)), (.06, dict(tw=0.)), (.20, dict(tw=62.)), (.30, dict(tw=62.)), (.46, dict(tw=0.)), (.54, dict(tw=0.)),
          (.68, dict(tw=-62.)), (.78, dict(tw=-62.)), (.94, dict(tw=0.))]
    return dict(seconds=4.0, keys=_dense(lambda tw: pose(tw), ph, 4.0), cam_az=25, cam_el=14, props=[], anatomy_detail=True,
                motion_profile='russian-twist')


EXERCISES = {
    'side-plank': side_plank,
    'walking-lunge': walking_lunge,
    'split-squat': split_squat,
    'dip-parallel': dip_parallel,
    'inverted-row': inverted_row,
    'plank-shoulder-tap': plank_shoulder_tap,
    'russian-twist': russian_twist,
}
