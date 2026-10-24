"""Controlled bar pullover: continuous turnover, contact-preserving wrist roll.

Shape parameters use unwrapped angles. Derive world positions after sampling so
interpolation follows the bar's circular surface instead of cutting through it.
Time is assigned by the solved body's arc length, with smooth endpoint ramps.
"""
import bisect
import math
from poses import base_pose, sample_continuous, solve, steady_progress

BAR_Z = 2.30
SECONDS = 11
SHAPE = [
    (0, dict(pitch=0, along=-1.096, offset=.06, leg=0, roll=0)),
    (.16, dict(pitch=-15, along=-.58, offset=.145, leg=-75, roll=20)),
    (.34, dict(pitch=-80, along=-.30, offset=.145, leg=-145, roll=15)),
    (.50, dict(pitch=-170, along=-.20, offset=.145, leg=-190, roll=-60)),
    (.66, dict(pitch=-250, along=-.12, offset=.145, leg=-270, roll=-125)),
    (.84, dict(pitch=-320, along=.05, offset=.145, leg=-335, roll=-180)),
    (1, dict(pitch=-360, along=.157, offset=.145, leg=-360, roll=-225)),
]
TRACKED = ('pelvis', 'chest', 'head', 'elbow_l', 'elbow_r',
           'knee_l', 'knee_r', 'ankle_l', 'ankle_r')


def pose(progress):
    q = sample_continuous(SHAPE, progress)
    a, leg, roll = (math.radians(q[k]) for k in ('pitch', 'leg', 'roll'))
    # Keep the trunk tangent to the bar through inversion and support.
    pelvis = [-q['offset']*math.cos(a) + q['along']*math.sin(a), 0,
              BAR_Z + q['offset']*math.sin(a) + q['along']*math.cos(a)]
    fields = dict(pelvis=pelvis, p1=q['pitch'], p2=q['pitch'], hp=0)
    for side, sign in (('l', 1), ('r', -1)):
        wrist = [-.045*math.cos(roll)-.065*math.sin(roll), sign*.24,
                 BAR_Z+.045*math.sin(roll)-.065*math.cos(roll)]
        shoulder = [pelvis[0]+.47*math.sin(a), sign*.18,
                    pelvis[2]+.47*math.cos(a)]
        fields.update({
            'wrist_'+side: wrist,
            'hand_'+side: [math.sin(roll), 0, math.cos(roll)],
            'palm_'+side: [math.cos(roll), 0, -math.sin(roll)],
            'grip_'+side: .95,
            # Radial elbow poles keep the whole forearm outside the bar.
            'elbow_'+side: [shoulder[0]+wrist[0], sign*.08,
                           shoulder[2]+wrist[2]-2*BAR_Z],
            'ankle_'+side+'_rel': [-.86*math.sin(leg), 0, -.86*math.cos(leg)],
            'knee_'+side: [math.cos(leg), 0, -math.sin(leg)],
            'foot_'+side: [math.cos(leg), 0, -math.sin(leg)],
        })
    return base_pose(**fields)


def arc_table(steps=1000):
    """Average distance of tracked body joints; no dense-phase timing bias."""
    lengths = [0.]
    previous = solve(pose(0), {})
    for i in range(1, steps+1):
        current = solve(pose(i/steps), {})
        lengths.append(lengths[-1] + sum(math.dist(current[n], previous[n])
                                        for n in TRACKED)/len(TRACKED))
        previous = current
    return lengths


def specification():
    lengths = arc_table()
    def shape_progress(distance):
        target = distance*lengths[-1]
        i = min(len(lengths)-1, max(1, bisect.bisect_left(lengths, target)))
        fraction = (target-lengths[i-1])/(lengths[i]-lengths[i-1])
        return (i-1+fraction)/(len(lengths)-1)
    keys = []
    for i in range(SECONDS*24+1):
        t = i/(SECONDS*24)
        if t < .04: distance = 0
        elif t < .44: distance = steady_progress((t-.04)/.40)
        elif t < .50: distance = 1
        elif t < .96: distance = 1-steady_progress((t-.50)/.46)
        else: distance = 0
        keys.append((t, pose(shape_progress(distance))))
    return dict(seconds=SECONDS, keys=keys, interpolation='continuous',
                cam_az=25, cam_el=10, props=[dict(kind='bar', z=BAR_Z, half=.72, r=.017)],
                anatomy_detail=True, motion_profile='controlled-bar-pullover',
                bar_grip=dict(z=BAR_Z, r=.017, rotate_with_hand=True))
