"""R8 front/back lever progressions and lever rows."""
import math

from poses import base_pose, rep, dense

BAR_Z = 2.30
WRIST_Z = BAR_Z - 0.065
BAR = dict(kind="bar", z=BAR_Z, half=0.72, r=0.017)


def _hands(x=-0.045, z=WRIST_Z, width=0.24, elbows=None):
    el = elbows or ((0.25, 1.0, -0.2), (0.25, -1.0, -0.2))
    return dict(wrist_l=[x, width, z], wrist_r=[x, -width, z],
                palm_l=[1, 0, 0], palm_r=[1, 0, 0],
                hand_l=[0, 0, 1], hand_r=[0, 0, 1], grip_l=0.95, grip_r=0.95,
                elbow_l=list(el[0]), elbow_r=list(el[1]))


def _legs(x=0.8695, z=0.0, tuck=0.0, spread=0.015):
    # Relative targets are measured from each hip; elbows/poles keep the knee bend
    # visible and symmetric. x is toward the feet in a horizontal lever.
    if tuck:
        x, z = 0.10, 0.43
        pole = [-1, 0, 1]
        foot = [1, 0, 0.2]
    else:
        pole = [-1, 0, 0]
        foot = [1, 0, 0]
    return dict(ankle_l_rel=[x, spread, z], ankle_r_rel=[x, -spread, z],
                knee_l=list(pole), knee_r=list(pole), foot_l=list(foot), foot_r=list(foot))


def _front(legs=None, pz=1.67, px=0.425, elbow=None):
    return base_pose(pelvis=[px, 0, pz], p1=-90, p2=-90, hp=0,
                     **_hands(elbows=elbow), **(legs or _legs()))


def _back(legs=None, pz=1.67, px=-0.545, elbow=None):
    # Face down, head toward +X; shoulder at x=-0.045 under fixed bar grip.
    return base_pose(pelvis=[px, 0, pz], p1=90, p2=90, hp=0,
                     **_hands(elbows=elbow), **(legs or _legs(x=-0.8695)))


def _hold(a, b=None, seconds=3.0):
    return dict(seconds=seconds, keys=rep(a, b or a), cam_az=32, cam_el=15, props=[BAR])


def front_lever_one_leg():
    bent = dict(ankle_l_rel=[0.8695, 0.025, 0.0], ankle_r_rel=[0.12, -0.025, 0.43],
                knee_l=[-1, 0, 0], knee_r=[-1, 0, 1], foot_l=[1, 0, 0], foot_r=[1, 0, 0.2])
    return _hold(_front(bent), _front({**bent, "ankle_r_rel":[0.10,-0.025,0.44]}))


def front_lever():
    return _hold(_front(_legs(x=0.8695, spread=0.004)), _front(_legs(x=0.8685, spread=0.004)))


def _skin_pose(angle, shoulder_x=-0.045, shoulder_z=WRIST_Z - 0.565, straighten=0.0):
    # Rotate about the straight-arm shoulder/grip axis. The shoulder stays 0.565 m
    # below the bar, while the trunk and tucked legs pass through a full inversion.
    a = math.radians(angle)
    up = [math.sin(a), 0.0, math.cos(a)]
    shoulder = [shoulder_x, 0.0, shoulder_z]
    pelvis = [shoulder[0] - 0.47*up[0], 0.0, shoulder[2] - 0.47*up[2]]
    tuck_vec = [0.10*math.cos(a)+0.43*math.sin(a), 0.02,
                -0.10*math.sin(a)+0.43*math.cos(a)]
    straight_vec = [-0.8695*math.sin(a), 0.02, -0.8695*math.cos(a)]
    ankle = [tuck_vec[j]*(1-straighten)+straight_vec[j]*straighten for j in range(3)]
    legs = dict(ankle_l_rel=list(ankle),
                ankle_r_rel=[ankle[0],-ankle[1],ankle[2]],
                knee_l=[-1,0,1] if straighten < 0.5 else [-1,0,0], knee_r=[-1,0,1] if straighten < 0.5 else [-1,0,0],
                foot_l=[1,0,0], foot_r=[1,0,0])
    return base_pose(pelvis=pelvis, p1=angle, p2=angle, hp=0,
                     **_hands(), **legs)


def skin_the_cat():
    # Dense complete backward circle: hang -> inverted tuck -> German hang -> hang.
    # Brief pause in the stretched German-hang orientation before the return arc.
    def pose(t):
        if t < 0.45:
            q = t / 0.45
        elif t < 0.55:
            q = 1.0
        else:
            q = 1.0 - (t - 0.55) / 0.45
        a = 360.0*q
        shift = max(0.0, min(1.0, (q-0.5)*2.0))
        shift = shift*shift*(3-2*shift)
        sx = -0.045 + (0.445+0.045)*shift
        sz = WRIST_Z - 0.565 + (1.955-(WRIST_Z-0.565))*shift
        if q < 0.22:
            straight = 1.0 - q/0.22
        else:
            straight = max(0.0, min(1.0, (q-0.82)/0.18))
        return _skin_pose(a, sx, sz, straight)
    keys = [(i/96, pose(i/96)) for i in range(96)]
    return dict(seconds=6.0, keys=keys, cam_az=32, cam_el=15, props=[BAR])


def back_lever_tuck():
    tuck = dict(ankle_l_rel=[-0.10,0.02,-0.43], ankle_r_rel=[-0.10,-0.02,-0.43],
                knee_l=[1,0,1], knee_r=[1,0,1], foot_l=[1,0,0.2], foot_r=[1,0,0.2])
    return _hold(_back(tuck), _back({**tuck,"ankle_l_rel":[-0.08,0.02,-0.44],"ankle_r_rel":[-0.08,-0.02,-0.44]}))


def back_lever_adv_tuck():
    adv = dict(ankle_l_rel=[-0.35,0.02,-0.43], ankle_r_rel=[-0.35,-0.02,-0.43],
               knee_l=[1,0,1], knee_r=[1,0,1], foot_l=[1,0,0], foot_r=[1,0,0])
    return _hold(_back(adv), _back({**adv,"ankle_l_rel":[-0.37,0.02,-0.42],"ankle_r_rel":[-0.37,-0.02,-0.42]}))


def back_lever_straddle():
    sx = math.sqrt(0.8695**2 - 0.32**2)
    legs = dict(ankle_l_rel=[-sx,0.32,0], ankle_r_rel=[-sx,-0.32,0],
                knee_l=[1,0,0], knee_r=[1,0,0], foot_l=[1,0.2,0], foot_r=[1,-0.2,0])
    sx2 = math.sqrt(0.8695**2 - 0.30**2)
    return _hold(_back(legs), _back({**legs,"ankle_l_rel":[-sx2,0.30,0],"ankle_r_rel":[-sx2,-0.30,0]}))


def back_lever_one_leg():
    # Keep the working thigh along the trunk while folding the shin upward
    # behind the knee for a clear half-lay shape.
    legs=dict(ankle_l_rel=[-0.8695,0.02,0], ankle_r_rel=[-0.44,-0.02,0.43],
              knee_l=[1,0,0], knee_r=[-1,0,0], foot_l=[1,0,0], foot_r=[1,0,0.2])
    return _hold(_back(legs), _back({**legs,"ankle_r_rel":[-0.44,-0.02,0.42]}))


def back_lever():
    return _hold(_back(_legs(x=-0.8695, spread=0.004)), _back(_legs(x=-0.8685, spread=0.004)))


def _front_row(legs, pz, bent=False):
    # Straight-arm lever shoulders sit at x=-.045,z=pz. Bent-arm row raises
    # shoulders toward the fixed bar; elbow pole curls arms toward torso.
    elbows = ((-0.7,1,-0.2),(-0.7,-1,-0.2)) if bent else ((0.25,1,-0.2),(0.25,-1,-0.2))
    return _front(legs, pz=pz, elbow=elbows)


def front_lever_row_tuck():
    l=_legs(tuck=1)
    a=_front_row(l,1.67,False); b=_front_row(l,1.82,True)
    return dict(seconds=3.6,keys=rep(a,b),cam_az=32,cam_el=15,props=[BAR])


def front_lever_row_adv_tuck():
    l=dict(ankle_l_rel=[0.43,0.02,0.44],ankle_r_rel=[0.43,-0.02,0.44],knee_l=[-1,0,1],knee_r=[-1,0,1],foot_l=[1,0,0],foot_r=[1,0,0])
    return dict(seconds=3.6,keys=rep(_front_row(l,1.67),_front_row(l,1.82,True)),cam_az=32,cam_el=15,props=[BAR])


def ice_cream_maker():
    # Pull-up top, rotate backward into a horizontal front lever, then pull back.
    legs_tuck=_legs(tuck=1)
    def top():
        return base_pose(pelvis=[0.16,0,1.72],p1=-2,p2=-6,hp=-5,**_hands(elbows=((-0.5,1,-0.2),(-0.5,-1,-0.2))),**legs_tuck)
    def lever(t):
        angle=-90*t
        rad=math.radians(angle)
        shoulder=[0.16-0.205*t,0,2.20-0.53*t]
        up=[math.sin(rad),0,math.cos(rad)]
        pel=[shoulder[0]-.47*up[0],0,shoulder[2]-.47*up[2]]
        return base_pose(pelvis=pel,p1=angle,p2=angle,hp=0,**_hands(),**legs_tuck)
    keys=[(0,top()),(.13,top()),(.30,lever(.35)),(.47,lever(1.0)),(.60,lever(1.0)),(.78,lever(.35)),(.94,top()),(.99,top())]
    return dict(seconds=4.2,keys=keys,cam_az=32,cam_el=15,props=[BAR])


def front_lever_row_straddle():
    sx=math.sqrt(0.8695**2-0.32**2)
    l=dict(ankle_l_rel=[sx,0.32,0],ankle_r_rel=[sx,-0.32,0],knee_l=[-1,0,0],knee_r=[-1,0,0],foot_l=[1,0.2,0],foot_r=[1,-0.2,0])
    return dict(seconds=3.6,keys=rep(_front_row(l,1.67),_front_row(l,1.82,True)),cam_az=32,cam_el=15,props=[BAR])


def front_lever_row_hang():
    def at(q):
        # Rotate a straight body from dead hang to a level lever while the hands
        # remain fixed; shoulders rise slightly for a deep row at the finish.
        angle=-90*q
        rad=math.radians(angle)
        up=[math.sin(rad),0,math.cos(rad)]
        shoulder=[-0.045+0.08*q,0,1.67+0.18*q]
        pelvis=[shoulder[0]-.47*up[0],0,shoulder[2]-.47*up[2]]
        legs=dict(ankle_l_rel=[-0.8695*math.sin(rad),0.015,-0.8695*math.cos(rad)],
                  ankle_r_rel=[-0.8695*math.sin(rad),-0.015,-0.8695*math.cos(rad)],
                  knee_l=[1,0,0],knee_r=[1,0,0],foot_l=[0.4,0,-1],foot_r=[0.4,0,-1])
        elbows=((0.25,1,-0.2), (0.25,-1,-0.2)) if q < 0.08 else ((-0.7,1,-0.2),(-0.7,-1,-0.2))
        return base_pose(pelvis=pelvis,p1=angle,p2=angle,hp=0,**_hands(elbows=elbows),**legs)
    keys=[]
    for i in range(96):
        t=i/96
        q=0.5-0.5*math.cos(2*math.pi*t)
        keys.append((t,at(q)))
    return dict(seconds=4.0,keys=keys,cam_az=32,cam_el=15,props=[BAR])


def front_lever_row():
    l=_legs(x=0.8695,spread=0.004)
    return dict(seconds=3.6,keys=rep(_front_row(l,1.67),_front_row(l,1.82,True)),cam_az=32,cam_el=15,props=[BAR])


EXERCISES = {
    "front-lever-one-leg": front_lever_one_leg,
    "front-lever": front_lever,
    "skin-the-cat": skin_the_cat,
    "back-lever-tuck": back_lever_tuck,
    "back-lever-adv-tuck": back_lever_adv_tuck,
    "back-lever-straddle": back_lever_straddle,
    "back-lever-one-leg": back_lever_one_leg,
    "back-lever": back_lever,
    "front-lever-row-tuck": front_lever_row_tuck,
    "front-lever-row-adv-tuck": front_lever_row_adv_tuck,
    "ice-cream-maker": ice_cream_maker,
    "front-lever-row-straddle": front_lever_row_straddle,
    "front-lever-row-hang": front_lever_row_hang,
    "front-lever-row": front_lever_row,
}
