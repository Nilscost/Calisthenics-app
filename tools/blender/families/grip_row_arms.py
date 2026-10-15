"""R8 grip, arm strength and unilateral low-row poses."""
import math

from poses import base_pose, pt, rep

BAR_Z = 2.30
BAR_W = BAR_Z - 0.065
LOW_Z = 1.00
LOW_W = LOW_Z - 0.065
HIGH_BAR = dict(kind="bar", z=BAR_Z, half=0.72, r=0.017)
LOW_BAR = dict(kind="bar", z=LOW_Z, half=0.72, r=0.017)


def _hands(z=BAR_W, left_y=0.24, right_y=-0.24, left_x=-0.045, right_x=-0.045,
           grip=(0.95,0.95), palms=(1,0,0), elbows=None, hand=(0,0,1)):
    e=elbows or ((0.25,1,-0.2),(0.25,-1,-0.2))
    return dict(wrist_l=[left_x,left_y,z if isinstance(z,(int,float)) else z[0]],
                wrist_r=[right_x,right_y,z if isinstance(z,(int,float)) else z[1]],
                palm_l=list(palms),palm_r=list(palms),hand_l=list(hand),hand_r=list(hand),
                grip_l=grip[0],grip_r=grip[1],elbow_l=list(e[0]),elbow_r=list(e[1]))


def _hang_legs(fwd=0.04, wide=0.01):
    return dict(ankle_l_rel=[fwd,wide,-0.8695],ankle_r_rel=[fwd,-wide,-0.8695],
                knee_l=[1,0,0],knee_r=[1,0,0],foot_l=[0.4,0,-1],foot_r=[0.4,0,-1])


def _hang_pose(hands=None, legs=None, pelvis=( -0.04,0,1.205), p1=-3,p2=-6, **kw):
    return base_pose(pelvis=list(pelvis),p1=p1,p2=p2,hp=-4,
                     **(hands or _hands()),**(legs or _hang_legs()),**kw)


def _lowrow(ankles, angle, wrists=None, sy=0.0, elbows=None, legs_overrides=None):
    """Inverted-row geometry with exact heel and bar contacts."""
    ax,az=ankles
    rad=math.radians(angle)
    # Body line from ankle to shoulder; angle is measured down from the -X axis.
    sx=ax-1.365*math.cos(rad)
    sz=az-1.365*math.sin(rad)
    pitch=-90.0-angle
    ux=math.sin(math.radians(pitch)); uz=math.cos(math.radians(pitch))
    pelvis=[sx-0.47*ux,sy,sz-0.47*uz]
    w=wrists or ([[-0.045,0.22,LOW_W],[-0.045,-0.22,LOW_W]])
    e=elbows or ((0,1,-0.6),(0,-1,-0.6))
    legs=dict(ankle_l=[ax,0.10,az],ankle_r=[ax,-0.10,az],
              knee_l=[0,0,1],knee_r=[0,0,1],foot_l=[1,0,0],foot_r=[1,0,0])
    if legs_overrides: legs.update(legs_overrides)
    return base_pose(pelvis=pelvis,p1=pitch,p2=pitch,hp=0,
                     wrist_l=w[0],wrist_r=w[1],palm_l=[1,0,0],palm_r=[1,0,0],
                     hand_l=[0,0,1],hand_r=[0,0,1],grip_l=.95,grip_r=.95,
                     elbow_l=list(e[0]),elbow_r=list(e[1]),**legs)


def _cyclic_pose(pose_at, steps=96):
    """Cosine-paced full rep with a freshly solved body line at every sample."""
    return [(i/steps, pose_at(0.5-0.5*math.cos(2*math.pi*i/steps))) for i in range(steps)]


def row_archer_arm_in():
    ankles=(1.287,0.08)
    # Right hand rows at the bar; left arm stays straight and sweeps out sideways.
    right=[-0.045,-0.22,LOW_W]
    def pose(angle, sy, sweep):
        p=_lowrow(ankles,angle,wrists=[right,right],sy=sy)
        p.pop("wrist_l")
        a=math.pi*0.5*sweep
        p["wrist_l_rel"]=[0.569*math.cos(a),0.569*math.sin(a),0.0]
        p["elbow_l"]=[0.0,1.0,0.0]
        # Keep left hand open and straight as it moves from beside the torso to wide.
        p["grip_l"]=0.1
        return p
    keys=_cyclic_pose(lambda q: pose(-34.0+21.9*q,-0.08*q,q))
    return dict(seconds=3.8,keys=keys,cam_az=28,cam_el=18,props=[LOW_BAR])


def row_one_arm_straddle():
    ankles=(1.287,0.08)
    wrists=[[-0.045,0.30,LOW_W],[-0.045,-0.20,LOW_W]]
    def key(angle):
        p=_lowrow(ankles,angle,wrists=wrists)
        # Non-working left arm reaches out for balance; the right arm stays at the bar.
        p.pop("wrist_l")
        p["wrist_l_rel"]=[0.12,0.48,-0.02]
        p["elbow_l"]=[0,1,0]
        p["ankle_l"]=[ankles[0],0.16,ankles[1]]
        p["ankle_r"]=[ankles[0],-0.16,ankles[1]]
        p["knee_l"]=[0,0,1];p["knee_r"]=[0,0,1]
        return p
    return dict(seconds=3.8,keys=_cyclic_pose(lambda q:key(-34+21.9*q)),cam_az=28,cam_el=18,props=[LOW_BAR])


def row_one_arm():
    ankles=(1.287,0.08)
    wrists=[[-0.045,0.34,LOW_W],[-0.045,-0.20,LOW_W]]
    def key(angle):
        p=_lowrow(ankles,angle,wrists=wrists)
        p.pop("wrist_l")
        p["wrist_l_rel"]=[0.42,0.17,-0.16]
        p["elbow_l"]=[1,1,0]
        return p
    return dict(seconds=3.8,keys=_cyclic_pose(lambda q:key(-34+21.9*q)),cam_az=28,cam_el=18,props=[LOW_BAR])


def _bodyweight_arm_pose(angle, wrist_x=0.10, bar_z=0.95, forward=False, ankle_x=None):
    """Straight-body lean about planted heels, hands held at a low fixed bar."""
    # For curls angle<0 means leaning away from the bar; triceps uses a forward lean.
    theta=math.radians(angle)
    ankle_x=(1.56 if not forward else -1.30) if ankle_x is None else ankle_x
    ankle_z=0.08
    if not forward:
        shoulder=[ankle_x+1.365*math.sin(theta),0,ankle_z+1.365*math.cos(theta)]
        pitch=angle
    else:
        # Lean toward +X over the bar.
        shoulder=[ankle_x+1.365*math.sin(theta),0,ankle_z+1.365*math.cos(theta)]
        pitch=angle
    up=[math.sin(theta),0,math.cos(theta)]
    pelvis=[shoulder[0]-0.47*up[0],0,shoulder[2]-0.47*up[2]]
    # The bar hands stay fixed; elbow poles keep the bend in front of the torso.
    e=((0,1,0), (0,-1,0))
    ankle=[ankle_x,0.10,ankle_z]
    ankle_r=[ankle_x,-0.10,ankle_z]
    return base_pose(pelvis=pelvis,p1=pitch,p2=pitch,hp=0,
                     wrist_l=[wrist_x,0.20,bar_z],wrist_r=[wrist_x,-0.20,bar_z],
                     palm_l=[-1,0,0] if not forward else [0,0,-1],
                     palm_r=[-1,0,0] if not forward else [0,0,-1],
                     hand_l=[0,0,1],hand_r=[0,0,1],grip_l=.95,grip_r=.95,
                     elbow_l=list(e[0]),elbow_r=list(e[1]),
                     ankle_l=ankle,ankle_r=ankle_r,knee_l=[1,0,0],knee_r=[1,0,0],
                     foot_l=[1,0,0],foot_r=[1,0,0])


def arms_biceps_curl():
    # The body pivots as a straight unit against fixed underhand hands; elbows flex.
    extended=_bodyweight_arm_pose(-42,wrist_x=0.10,bar_z=0.95)
    curled=_bodyweight_arm_pose(-57,wrist_x=0.10,bar_z=0.95)
    # Underhand grip, palms face the body.
    for p in (extended,curled):
        p["palm_l"]=[-1,0,0];p["palm_r"]=[-1,0,0]
    keys=_cyclic_pose(lambda q:_bodyweight_arm_pose(-42-15*q,wrist_x=0.10,bar_z=0.95))
    return dict(seconds=3.8,keys=keys,cam_az=30,cam_el=12,
                props=[dict(kind="bar",x=0.10,z=1.0,half=0.65,r=0.017)])


def arms_triceps_extension():
    # Overhand hands on a hip-height bar; lean toward it and bend/extend at elbows.
    extended=_bodyweight_arm_pose(39.5,wrist_x=0.10,bar_z=0.95,forward=True,ankle_x=-1.30)
    bent=_bodyweight_arm_pose(53,wrist_x=0.10,bar_z=0.95,forward=True,ankle_x=-1.30)
    keys=_cyclic_pose(lambda q:_bodyweight_arm_pose(39.5+13.5*q,wrist_x=0.10,bar_z=0.95,forward=True,ankle_x=-1.30))
    return dict(seconds=3.8,keys=keys,cam_az=30,cam_el=12,
                props=[dict(kind="bar",x=0.10,z=1.0,half=0.65,r=0.017)])


def grip_eagle_claw():
    def key(grip):
        return base_pose(pelvis=[0,0,0.955],p1=0,p2=0,hp=0,
                         wrist_l_rel=[0.43,0.02,0.02],wrist_r_rel=[0.43,-0.02,0.02],
                         palm_l=[1,0,0],palm_r=[1,0,0],hand_l=[1,0,0],hand_r=[1,0,0],
                         grip_l=grip,grip_r=grip,elbow_l=[0,1,0],elbow_r=[0,-1,0],
                         ankle_l=[0,0.1,0.08],ankle_r=[0,-0.1,0.08],
                         knee_l=[1,0,0],knee_r=[1,0,0],foot_l=[1,0,0],foot_r=[1,0,0])
    return dict(seconds=4.0,keys=rep(key(0.0),key(1.0)),cam_az=30,cam_el=15,props=[])


def grip_horizontal_hang():
    # Straight body beneath a low bar, with heels planted and arms fully extended.
    bottom=_lowrow((1.287,0.08),-12.1)
    return dict(seconds=3.0,keys=rep(bottom,bottom),cam_az=30,cam_el=15,props=[LOW_BAR])


def grip_uneven_hang():
    # Right hand grips the bar; left hand grips a lower towel end, unloading that side.
    hands=_hands(z=BAR_W,left_y=0.39,right_y=-0.24,left_x=-0.045,right_x=-0.045,
                 elbows=((0.1,1,-0.2),(0.25,-1,-0.2)))
    hands["wrist_l"][2]=2.00
    towel=dict(kind="rod",a=[0,0.39,BAR_Z],b=pt("wrist_l"),r=0.02)
    a=_hang_pose(hands,_hang_legs(),pelvis=(-0.04,0,1.205),p1=-4,p2=-8)
    b=_hang_pose(hands,_hang_legs(fwd=0.06),pelvis=(-0.03,0,1.215),p1=-5,p2=-9)
    return dict(seconds=3.0,keys=rep(a,b),cam_az=30,cam_el=12,props=[HIGH_BAR,towel])


def grip_one_arm_hang():
    # Right hand carries the hang; left arm is released and poised beside the torso.
    h=_hands(z=BAR_W,left_y=0.31,right_y=-0.24,grip=(0.0,0.95))
    h["wrist_l"]=[-0.16,0.31,1.48]
    h["elbow_l"]=[-0.2,1,0]
    def pose(pz):
        return _hang_pose(h,_hang_legs(),pelvis=(-0.04,0,pz),p1=-4,p2=-8)
    return dict(seconds=3.0,keys=rep(pose(1.205),pose(1.22)),cam_az=30,cam_el=12,props=[HIGH_BAR])


def grip_towel_hang():
    z=2.13; y=0.22
    h=_hands(z=z,left_y=y,right_y=-y)
    # One narrow shared towel loop: two strands descend from the same anchor.
    anchor=[0,0,BAR_Z]
    towel=[dict(kind="rod",a=anchor,b=pt("wrist_l"),r=0.02),
           dict(kind="rod",a=anchor,b=pt("wrist_r"),r=0.02)]
    tucked=dict(ankle_l_rel=[0.08,0.02,-0.55],ankle_r_rel=[0.08,-0.02,-0.55],
                knee_l=[1,0,1],knee_r=[1,0,1],foot_l=[0.4,0,-1],foot_r=[0.4,0,-1])
    p1=_hang_pose(h,tucked,pelvis=(-0.04,0,1.10),p1=-4,p2=-8)
    p2=_hang_pose(h,tucked,pelvis=(-0.03,0,1.11),p1=-5,p2=-9)
    return dict(seconds=3.0,keys=rep(p1,p2),cam_az=30,cam_el=12,props=[HIGH_BAR]+towel)


def grip_twin_towel_hang():
    # Each doubled towel supports one hand at shoulder width; both hands share load.
    z=2.13;y=0.24
    h=_hands(z=z,left_y=y,right_y=-y)
    # Two separated doubled loops, each represented by a paired strand.
    props=[HIGH_BAR]
    for side, hand in ((1,"wrist_l"),(-1,"wrist_r")):
        for offset in (-0.035,0.035):
            props.append(dict(kind="rod",a=[-0.045+offset,side*y,BAR_Z],
                              b=pt(hand),r=0.012))
    tucked=dict(ankle_l_rel=[0.08,0.02,-0.55],ankle_r_rel=[0.08,-0.02,-0.55],
                knee_l=[1,0,1],knee_r=[1,0,1],foot_l=[0.4,0,-1],foot_r=[0.4,0,-1])
    a=_hang_pose(h,tucked,pelvis=(-0.04,0,1.10),p1=-4,p2=-8)
    b=_hang_pose(h,tucked,pelvis=(-0.03,0,1.11),p1=-5,p2=-9)
    return dict(seconds=3.0,keys=rep(a,b),cam_az=30,cam_el=12,props=props)


def grip_uneven_towel_hang():
    # The higher right hand grips the doubled towel above the lower left hand.
    h=_hands(z=[1.91,2.20],left_y=0.0,right_y=0.0,
             grip=(0.85,0.95),elbows=((0.2,1,-0.2),(0.25,-1,-0.2)))
    h["wrist_l"][0]=h["wrist_r"][0]
    towel=dict(kind="rod",a=[0,0,BAR_Z],b=pt("wrist_r"),r=0.02)
    tucked=dict(ankle_l_rel=[0.08,0.02,-0.55],ankle_r_rel=[0.08,-0.02,-0.55],
                knee_l=[1,0,1],knee_r=[1,0,1],foot_l=[0.4,0,-1],foot_r=[0.4,0,-1])
    a=_hang_pose(h,tucked,pelvis=(-0.04,0,1.20),p1=-4,p2=-8)
    b=_hang_pose(h,tucked,pelvis=(-0.03,0,1.21),p1=-5,p2=-9)
    return dict(seconds=3.0,keys=rep(a,b),cam_az=30,cam_el=12,props=[HIGH_BAR,towel])


def grip_one_arm_towel_hang():
    # Right hand alone grips one doubled towel; the left hand is released.
    h=_hands(z=2.13,left_y=0.32,right_y=-0.20,grip=(0.0,0.95))
    h["wrist_l"]=[-0.16,0.32,1.48];h["elbow_l"]=[-0.2,1,0]
    towel=dict(kind="rod",a=[0,-0.20,BAR_Z],b=pt("wrist_r"),r=0.02)
    tucked=dict(ankle_l_rel=[0.08,0.02,-0.55],ankle_r_rel=[0.08,-0.02,-0.55],
                knee_l=[1,0,1],knee_r=[1,0,1],foot_l=[0.4,0,-1],foot_r=[0.4,0,-1])
    a=_hang_pose(h,tucked,pelvis=(-0.04,0,1.10),p1=-4,p2=-8)
    b=_hang_pose(h,tucked,pelvis=(-0.03,0,1.11),p1=-5,p2=-9)
    return dict(seconds=3.0,keys=rep(a,b),cam_az=30,cam_el=12,props=[HIGH_BAR,towel])


EXERCISES={
    "row-archer-arm-in":row_archer_arm_in,
    "row-one-arm-straddle":row_one_arm_straddle,
    "row-one-arm":row_one_arm,
    "arms-biceps-curl":arms_biceps_curl,
    "arms-triceps-extension":arms_triceps_extension,
    "grip-eagle-claw":grip_eagle_claw,
    "grip-horizontal-hang":grip_horizontal_hang,
    "grip-uneven-hang":grip_uneven_hang,
    "grip-one-arm-hang":grip_one_arm_hang,
    "grip-towel-hang":grip_towel_hang,
    "grip-twin-towel-hang":grip_twin_towel_hang,
    "grip-uneven-towel-hang":grip_uneven_towel_hang,
    "grip-one-arm-towel-hang":grip_one_arm_towel_hang,
}
