"""Calf strengthening and one-leg balance poses."""
import math

from poses import base_pose


def _dense(fn, t0, t1, n=36):
    return [(t0 + (t1 - t0) * i / n, fn(i / n)) for i in range(1, n + 1)]


def _cycle(fn):
    """High start, controlled lower, pause at stretch, then return high."""
    high, low = fn(1.0), fn(0.0)
    keys = [(0.0, high), (.12, high)]
    keys += _dense(lambda s: fn(1 - s), .12, .49)
    keys += [(.58, low)]
    keys += _dense(fn, .58, .94)
    return keys


def _wall_hand():
    return dict(wrist_l=[.43, .20, 1.45], elbow_l=[-1, .2, 0], hand_l=[1, 0, 0],
                palm_l=[1, 0, 0], grip_l=.25)


def _wall():
    return dict(kind="box", center=[.46, 0, 1.2], size=[.05, .80, 2.4])


def _toe_pivot(x, y, z, theta):
    """Ankle/orientation that keeps the rendered foot-tip cap on its surface."""
    a = math.radians(theta)
    foot = [math.cos(a), 0, math.sin(a)]
    ankle = [x - .165 * math.cos(a) + .045 * math.sin(a), y,
             z + .026 - .165 * math.sin(a) + .045 * math.cos(a)]
    return ankle, foot


def _standing_key(phase, bent=False, single=False, step=False):
    # phase 0 = heel below step / flat floor, phase 1 = high plantar flexion.
    step_z = .20 if step else 0.0
    theta = (34 - 69 * phase) if step else (-34 * phase)
    toe_x = .20
    if single:
        toe_r = _toe_pivot(toe_x, -.10, step_z, theta)
        reach = .84 if bent else .868
        dx = toe_r[0][0]
        dy = -.10 - (-.09)
        dz = math.sqrt(max(.01, reach * reach - dx * dx - dy * dy))
        pz = toe_r[0][2] + dz + .03
        free = [0.25, .10, .55]
        return base_pose(pelvis=[0, 0, pz], p1=0, p2=0,
                         ankle_r=toe_r[0], foot_r=toe_r[1], knee_r=[1, 0, 0],
                         ankle_l_rel=free, foot_l=[1, 0, -.15], knee_l=[1, 0, 0],
                         **_wall_hand())
    left = _toe_pivot(toe_x, .10, step_z, theta)
    right = _toe_pivot(toe_x, -.10, step_z, theta)
    reach = .84 if bent else .868
    # Set pelvis height from the actual hip-to-ankle reach so knee flex is
    # constant even as the foot rotates around its planted ball.
    vertical = math.sqrt(max(.01, reach * reach - (right[0][0]) ** 2 - .01 ** 2))
    pz = right[0][2] + vertical + .03
    knee = [1, 0, 0]
    return base_pose(pelvis=[0, 0, pz], p1=0, p2=0,
                     ankle_r=right[0], ankle_l=left[0],
                     foot_r=right[1], foot_l=left[1], knee_r=knee, knee_l=knee,
                     **_wall_hand())


def _calf(bent=False, single=False, step=False):
    props = [_wall()]
    if step:
        # Compact support only under the ball/toe contact; the heel/ankle stays
        # beyond its rear footprint so it can descend freely below step height.
        props.append(dict(kind="box", center=[.155, 0, .10], size=[.07, .60, .20]))
    keys = _cycle(lambda s: _standing_key(s, bent=bent, single=single, step=step))
    return dict(seconds=4.0, keys=keys, cam_az=28, cam_el=12, props=props)


def calf_raise_bent_knee(): return _calf(bent=True)
def calf_raise_single_bent_knee(): return _calf(bent=True, single=True)
def calf_raise_step_bent_knee(): return _calf(bent=True, step=True)
def calf_raise_step(): return _calf(step=True)
def calf_raise_step_single_bent_knee(): return _calf(bent=True, single=True, step=True)
def calf_raise_step_single(): return _calf(single=True, step=True)


def _balance(eyes_closed=False, cushion=False):
    pz = .89 if not cushion else .97
    support_z = .08 if not cushion else .16
    # Right leg is the stance leg; left foot is lifted with a comfortable bend.
    dx = -.20
    reach = .85
    pz = support_z + math.sqrt(reach * reach - dx * dx - .01 ** 2) + .03
    arm_span = .43 if not eyes_closed else .52
    def pose(s):
        return base_pose(pelvis=[.01 * math.sin(s * math.pi), 0, pz + .008 * math.sin(s * math.pi)],
                         p1=0, p2=0,
                         ankle_r=[0, -.10, support_z], foot_r=[1, 0, 0], knee_r=[1, 0, 0],
                         ankle_l_rel=[.25, 0, .55], foot_l=[1, 0, -.15], knee_l=[1, 0, 0],
                         wrist_l_rel=[0, arm_span, -.04], wrist_r_rel=[0, -arm_span, -.04],
                         elbow_l=[-1, 0, 0], elbow_r=[-1, 0, 0],
                         hand_l=[0, 1, 0], hand_r=[0, -1, 0], grip_l=0, grip_r=0)
    keys=[(0,pose(0)),(.12,pose(0)),(.5,pose(1)),(.62,pose(1)),(.94,pose(0))]
    props=[]
    if cushion:
        props=[dict(kind="box",center=[0,-.10,.04],size=[.46,.46,.08])]
    return dict(seconds=3.5,keys=keys,cam_az=28,cam_el=12,props=props)


def balance_single_leg(): return _balance()
def balance_single_leg_eyes_closed(): return _balance(eyes_closed=True)
def balance_single_leg_cushion(): return _balance(cushion=True)


def short_foot():
    # Seated, knees bent, both feet planted and toes long. The current rig has
    # no intrinsic arch/toe deformation, so this keeps the prescribed contact
    # and avoids falsely curling or lifting the toes to imply a short-foot.
    def pose(shift):
        return base_pose(pelvis=[-.02 * shift, 0, .53 + .006 * shift], p1=-5, p2=-5,
                         ankle_r=[.34, -.10, .08], ankle_l=[.34, .10, .08],
                         foot_r=[1, 0, 0], foot_l=[1, 0, 0],
                         knee_r=[1, 0, 0], knee_l=[1, 0, 0],
                         wrist_l=[.05, .20, .72], wrist_r=[.05, -.20, .72],
                         elbow_l=[0, 1, 0], elbow_r=[0, -1, 0],
                         hand_l=[0, 0, -1], hand_r=[0, 0, -1])
    chair=dict(kind="chair",x=0,seat=.45)
    return dict(seconds=5.0,keys=[(0,pose(0)),(.12,pose(0)),(.35,pose(1)),(.55,pose(1)),(.78,pose(0)),(.94,pose(0))],
                cam_az=30,cam_el=15,props=[chair])


EXERCISES={
    "calf-raise-bent-knee":calf_raise_bent_knee,
    "calf-raise-single-bent-knee":calf_raise_single_bent_knee,
    "calf-raise-step-bent-knee":calf_raise_step_bent_knee,
    "calf-raise-step":calf_raise_step,
    "calf-raise-step-single-bent-knee":calf_raise_step_single_bent_knee,
    "calf-raise-step-single":calf_raise_step_single,
    "balance-single-leg":balance_single_leg,
    "balance-single-leg-eyes-closed":balance_single_leg_eyes_closed,
    "balance-single-leg-cushion":balance_single_leg_cushion,
    "short-foot":short_foot,
}
