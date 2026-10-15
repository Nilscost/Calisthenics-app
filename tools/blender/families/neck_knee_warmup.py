"""Neck, knee and general warm-up motions for the R8 catalog."""
import math

from poses import dense, pt, base_pose, both, rep

FLOOR = 0.035


def _hands_floor(x=0.40, y=0.22):
    return dict(wrist_l=[x, y, FLOOR], wrist_r=[x, -y, FLOOR],
                elbow_l=[0, 0, -1], elbow_r=[0, 0, -1],
                hand_l=[1, 0, 0], hand_r=[1, 0, 0],
                palm_l=[0, 0, -1], palm_r=[0, 0, -1], grip_l=0, grip_r=0)


def _feet_forward(x=0.69, y=0.18):
    # Real thigh support: seated pelvis on floor, knees raised, feet planted.
    return dict(ankle_l=[x, y, 0.08], ankle_r=[x, -y, 0.08],
                knee_l=[0.3, 0, 1], knee_r=[0.3, 0, 1],
                foot_l=[1, 0, 0], foot_r=[1, 0, 0])


def _seated(pelvis=(0.0, 0.0, 0.11), p1=0, p2=0, **over):
    fields = dict(pelvis=list(pelvis), p1=p1, p2=p2, hp=0,
                  **_feet_forward(),
                  wrist_l_rel=[-0.05, 0.24, 0.16], wrist_r_rel=[-0.05, -0.24, 0.16],
                  hand_l=[0, 0, 1], hand_r=[0, 0, 1],
                  elbow_l=[-1, 0.2, 0], elbow_r=[-1, -0.2, 0],
                  palm_l=[0, -1, 0], palm_r=[0, 1, 0], grip_l=0, grip_r=0)
    fields.update(over)
    return base_pose(**fields)


def _standing_arms(wrist_l_rel=(0, 0.35, 0.0), wrist_r_rel=(0, -0.35, 0.0)):
    return dict(wrist_l_rel=list(wrist_l_rel), wrist_r_rel=list(wrist_r_rel),
                elbow_l=[1, 1, 0], elbow_r=[1, -1, 0],
                hand_l=[0, 0, 1], hand_r=[0, 0, 1],
                palm_l=[0, -1, 0], palm_r=[0, 1, 0], grip_l=0, grip_r=0)


def spanish_squat():
    # Fixed, vertical shins; band passes behind both knees to the front anchor.
    dx=math.sqrt(.44**2-.04**2)
    p=base_pose(pelvis=[-dx,0,.54],p1=0,p2=0,hp=0,
        ankle_l=[0,.13,.08],ankle_r=[0,-.13,.08],
        knee_l=[dx,.04,0],knee_r=[dx,-.04,0],foot_l=[1,0,0],foot_r=[1,0,0],
        wrist_l_rel=[.50,.04,-.10],wrist_r_rel=[.50,-.04,-.10],
        elbow_l=[0,1,-1],elbow_r=[0,-1,-1],hand_l=[1,0,0],hand_r=[1,0,0])
    props=[dict(kind='rod',a=[.95,0,.05],b=[.95,0,1],r=.025),
        dict(kind='rod',a=[.95,0,.51],b=[-.025,.13,.51],r=.008),
        dict(kind='rod',a=[.95,0,.51],b=[-.025,-.13,.51],r=.008),
        dict(kind='rod',a=[-.025,-.17,.51],b=[-.025,.17,.51],r=.008)]
    return dict(seconds=3,keys=[(0,p),(.99,p)],cam_az=28,cam_el=10,props=props)


def terminal_knee_extension():
    def key(q):
        return base_pose(pelvis=[0,0,.95+.0294*q],p1=0,p2=0,hp=0,
            ankle_l=[0,.12,.08],ankle_r=[0,-.12,.08],
            knee_l=[1,0,0],knee_r=[1,0,0],foot_l=[1,0,0],foot_r=[1,0,0],
            **_standing_arms())
    props=[dict(kind='rod',a=[.8,-.12,.52],b=pt('knee_r',[-.035,0,0]),r=.008)]
    return dict(seconds=3.2,keys=dense(key,[(0,{'q':0}),(.12,{'q':0}),(.45,{'q':1}),(.60,{'q':1}),(.96,{'q':0})],steps=48),cam_az=25,cam_el=8,props=props)


def petersen_step_up():
    def key(q):
        return base_pose(pelvis=[.05*(1-q),0,.95+.2465*q],p1=0,p2=0,hp=0,
            ankle_r=[0,-.16,.30],foot_r=[math.sqrt(1-(.08/.22)**2),0,-.08/.22],knee_r=[1,0,0],
            ankle_l=[-.10*(1-q),.10,.08+.22*q],foot_l=[1,0,0],knee_l=[1,0,0],
            **_standing_arms())
    step=dict(kind='box',center=[.05,-.16,.10],size=[.60,.30,.20])
    return dict(seconds=3.6,keys=dense(key,[(0,{'q':1}),(.15,{'q':1}),(.48,{'q':0}),(.60,{'q':0}),(.96,{'q':1})],steps=56),cam_az=24,cam_el=12,props=[step])


def stair_descent_slow():
    def key(q):
        return base_pose(pelvis=[.10*q,0,1.1765-.2265*q],p1=0,p2=0,hp=0,
            ankle_r=[0,-.16,.28],foot_r=[1,0,0],knee_r=[1,0,0],
            ankle_l=[.25*q,.12,.28-.20*q],foot_l=[.96,0,.28*q],knee_l=[1,0,0],
            **_standing_arms())
    step=dict(kind='box',center=[.05,-.16,.10],size=[.60,.30,.20])
    # .10→.6833 of a six-second cycle = 3.5 seconds lowering.
    return dict(seconds=6,keys=dense(key,[(0,{'q':0}),(.10,{'q':0}),(.683333,{'q':1}),(.75,{'q':1}),(.96,{'q':0})],steps=96),cam_az=26,cam_el=12,props=[step])


def _supine_head_pose(hp, hands=None):
    """Supine with knees bent and the lower spine/head supported on the floor."""
    fields = dict(pelvis=[0,0,.11],p1=-90,p2=-90,hp=hp,
        ankle_l=[.42,.18,.08],ankle_r=[.42,-.18,.08],
        knee_l=[0,0,1],knee_r=[0,0,1],foot_l=[1,0,0],foot_r=[1,0,0],
        wrist_l=[.09165,.18,.035],wrist_r=[.09165,-.18,.035],
        elbow_l=[math.sqrt(.30**2-.07**2),0,-.07],elbow_r=[math.sqrt(.30**2-.07**2),0,-.07],
        hand_l=[1,0,0],hand_r=[1,0,0],palm_l=[0,0,1],palm_r=[0,0,1],grip_l=0,grip_r=0)
    if hands:
        fields.update(hands)
    return base_pose(**fields)


def chin_nod_supine():
    # A tiny available head pitch (3 degrees) approximates a supported nod; no head lift is intended.
    neutral=_supine_head_pose(0)
    nod=_supine_head_pose(3)
    return dict(seconds=4.0,keys=rep(neutral,nod,hold_top=.18,down_end=.46,hold_bottom=.16),cam_az=28,cam_el=16,props=[])


def chin_tuck_seated():
    # Head retraction/translation is absent on this rig. Keep the prescribed floor-seated setup still.
    seated=_seated(p1=0,p2=0,hp=0)
    return dict(seconds=4.0,keys=[(0,seated),(.18,seated),(.50,seated),(.68,seated)],cam_az=28,cam_el=12,props=[])


def chin_tuck_head_lift():
    # Seven degrees of head pitch raises the head center about 2.4 cm while the trunk stays supine.
    down=_supine_head_pose(0)
    lift=_supine_head_pose(7)
    return dict(seconds=4.0,keys=rep(down,lift,hold_top=.16,down_end=.46,hold_bottom=.14),cam_az=28,cam_el=18,props=[])


def _head_center_seated():
    # p1=p2=0: seated pelvis plus the lumbar, thoracic and neck/head segments.
    return [0.0,0.0,.11+.24+.26+.199]


def _isometric_pose(target_contact=None, working="l", palm=None, overhead=False):
    H=_head_center_seated()
    # The touching hand is vertical (+Z), so its midpoint lands on the surface with this wrist offset.
    chest_l=[.30,.22,.70]; chest_r=[.30,-.22,.70]
    overhead_l=[0,.06,H[2]+.30]; overhead_r=[0,-.06,H[2]+.30]
    wrist_l=list(overhead_l if overhead else chest_l)
    wrist_r=list(overhead_r if overhead else chest_r)
    hand_l=[0,0,1]; hand_r=[0,0,1]
    palm_l=[0,-1,0]; palm_r=[0,1,0]
    if target_contact is not None:
        target=[target_contact[i] for i in range(3)]
        target[2]-=.085
        if working=="l":
            wrist_l=target; palm_l=list(palm)
        else:
            wrist_r=target; palm_r=list(palm)
    return _seated(p1=0,p2=0,hp=0,
        wrist_l=wrist_l,wrist_r=wrist_r,
        hand_l=hand_l,hand_r=hand_r,palm_l=palm_l,palm_r=palm_r,
        elbow_l=[1,1,0],elbow_r=[1,-1,0],grip_l=.2,grip_r=.2)


def neck_isometric_4_way():
    # Press the forehead, occiput and each temple. Between presses, wrists rise above the head before crossing.
    H=_head_center_seated()
    # Surface centers; palms face inward toward the skull while hand direction stays vertical +Z.
    contacts=[([H[0]+.10,H[1],H[2]+.02],"l",[-1,0,0]),
              ([H[0]-.10,H[1],H[2]+.02],"l",[1,0,0]),
              ([H[0],H[1]+.10,H[2]+.02],"l",[0,-1,0]),
              ([H[0],H[1]-.10,H[2]+.02],"r",[0,1,0])]
    keys=[]
    t=0.0
    overhead=_isometric_pose(overhead=True)
    keys.append((t,overhead))
    for contact,working,palm in contacts:
        above=_isometric_pose(target_contact=[contact[0],contact[1],H[2]+.365],working=working,palm=palm)
        touch=_isometric_pose(target_contact=contact,working=working,palm=palm)
        t+=.035;keys.append((t,above))
        t+=.035;keys.append((t,touch))
        t+=.10;keys.append((t,touch))
        t+=.035;keys.append((t,above))
        t+=.035;keys.append((t,overhead))
    # End on the exact opening posture to close the loop without a snap.
    if t<.99: keys.append((.99,overhead))
    return dict(seconds=8.0,keys=keys,cam_az=28,cam_el=12,props=[])

def neck_head_raise_manual():
    # Keep the thorax and pelvis down; pitch the head 7 degrees, with the hand midpoint following the forehead.
    def key(hp):
        p=_supine_head_pose(hp)
        a=math.radians(hp)
        face=[math.sin(a),0,math.cos(a)]
        headward=[-math.cos(a),0,math.sin(a)]
        center=[-.50-.199*math.cos(a)+.08*face[0]+.04*headward[0],
                0,
                .11+.199*math.sin(a)+.08*face[2]+.04*headward[2]]
        p.update(wrist_l=[center[i]-.085*headward[i] for i in range(3)],
                 hand_l=headward,palm_l=[-face[0],-face[1],-face[2]],grip_l=.2,
                 elbow_l=[0,1,1])
        return p
    down,up=key(0),key(7)
    return dict(seconds=3.6,keys=rep(down,up,hold_top=.14,down_end=.43,hold_bottom=.14),cam_az=28,cam_el=20,props=[])


def joint_circles():
    # Seven bounded circles. Every region blends out to a shared neutral posture before the next begins.
    # The rig offers head pitch only; it has no neck yaw or side-bend.
    def key(t):
        segment=min(6,int(t*7));u=t*7-segment;ang=2*math.pi*u
        envelope=math.sin(math.pi*u)**2
        base=dict(pelvis=[0,0,.975],p1=0,p2=0,hp=0,
            ankle_l=[0,.12,.08],ankle_r=[0,-.12,.08],
            knee_l=[1,0,0],knee_r=[1,0,0],foot_l=[1,0,0],foot_r=[1,0,0],
            wrist_l_rel=[.02,.04,-.52],wrist_r_rel=[.02,-.04,-.52],
            elbow_l=[1,1,0],elbow_r=[1,-1,0],hand_l=[0,0,-1],hand_r=[0,0,-1])
        if segment==0: # gentle head nod only
            base['hp']=3*envelope*math.sin(ang)
        elif segment==1: # shoulder circumduction, returning wrists to neutral at both ends
            base['wrist_l_rel']=[.02+.05*envelope*math.sin(ang),.04,-.52+.02*envelope*(math.cos(ang)-1)]
            base['wrist_r_rel']=[.02+.05*envelope*math.sin(ang),-.04,-.52+.02*envelope*(math.cos(ang)-1)]
        elif segment==2: # small elbow circles from compact bent-arm setup
            base['wrist_l_rel']=[.02+.04*envelope*math.cos(ang),.04,-.52+.04*envelope*math.sin(ang)]
            base['wrist_r_rel']=[.02+.04*envelope*math.cos(ang),-.04,-.52+.04*envelope*math.sin(ang)]
        elif segment==3: # hands circle within a 25-degree cone about the neutral downward direction
            theta=math.radians(25)*envelope*math.sin(ang)
            side=math.radians(25)*envelope*math.cos(ang)
            d=[.35*math.sin(theta),.25*math.sin(side),-1.0]
            n=math.sqrt(sum(v*v for v in d));d=[v/n for v in d]
            base['hand_l']=d;base['hand_r']=[d[0],-d[1],d[2]]
        elif segment in (4,5): # hip/knee circles: the right foot lifts into a smooth, bounded arc and returns
            radius=.035 if segment==4 else .045
            base['ankle_r']=[radius*envelope*math.cos(ang),-.12,.08+.25*envelope+.025*envelope*math.sin(ang)]
            if segment==5:base['knee_r']=[1,0,envelope]
        else: # ankle circles: lift smoothly, rotate the foot with ankle fixed, then lower it
            if u<.25:
                q=u/.25;lift=.5-.5*math.cos(math.pi*q)
                base['ankle_r']=[.12*lift,-.12,.08+.19*lift]
            elif u<=.75:
                base['ankle_r']=[.12,-.12,.27]
                q=(u-.25)/.5;phase=2*math.pi*q
                pitch=math.radians(25)*math.sin(phase)
                side=math.radians(25)*(1-math.cos(phase))*.5
                base['foot_r']=[math.cos(pitch)*math.cos(side),math.sin(side),math.sin(pitch)]
            else:
                q=(u-.75)/.25;lift=.5+.5*math.cos(math.pi*q)
                base['ankle_r']=[.12*lift,-.12,.08+.19*lift]
        return base_pose(**base)
    return dict(seconds=10.0,keys=[(i/140,key(i/140)) for i in range(140)],cam_az=30,cam_el=12,props=[])

def deep_squat_hold():
    def key(t):
        shift=.02*math.sin(2*math.pi*t)
        return base_pose(pelvis=[-.18,shift,.34],p1=35,p2=25,hp=-5,
            ankle_l=[0,.20,.08],ankle_r=[0,-.20,.08],
            knee_l=[1,.15,0],knee_r=[1,-.15,0],foot_l=[1,0,0],foot_r=[1,0,0],
            wrist_l_rel=[.48,.04,-.03],wrist_r_rel=[.48,-.04,-.03],
            elbow_l=[0,1,-1],elbow_r=[0,-1,-1],hand_l=[1,0,0],hand_r=[1,0,0])
    return dict(seconds=4,keys=[(i/64,key(i/64)) for i in range(64)],cam_az=30,cam_el=10,props=[])


def hip_raise_pushup():
    def key(t):
        if t<.15:return _hip_raise_supported(22)
        if t<.40:
            s=math.sin(math.pi*(t-.15)/.25)**2
            return _hip_raise_supported(22-4*s,72-4*(1-s))
        if t<.88:
            s=math.sin(math.pi*(t-.40)/.48)**2
            return _hip_raise_supported(22+26*s)
        return _hip_raise_supported(22)
    return dict(seconds=5,keys=[(i/160,key(i/160)) for i in range(160)],cam_az=30,cam_el=20,props=[])


def leg_swings():
    # Dense constant-radius front/back swings followed by side-to-side swings.
    # No balance prop is specified by the contract, so the arms balance freely.
    def key(t):
        stage=min(1,int(2*t));u=2*t-stage
        a=math.radians(35)*math.sin(2*math.pi*u)
        h=[0,-.09,.945];r=.8695
        ankle=[h[0]+(r*math.sin(a) if stage==0 else 0),
               h[1]+(-r*math.sin(a) if stage==1 else 0),h[2]-r*math.cos(a)]
        return base_pose(pelvis=[0,0,.975],p1=0,p2=0,hp=0,
            ankle_l=[0,.10,.08],ankle_r=ankle,knee_l=[1,0,0],knee_r=[1,0,0],
            foot_l=[1,0,0],foot_r=[1,0,0],**_standing_arms())
    return dict(seconds=6,keys=[(i/144,key(i/144)) for i in range(144)],cam_az=25,cam_el=10,props=[])

def _hip_raise_supported(a,p1=None):
    # Both feet and hands stay fixed. Construct the hips from the leg radius,
    # then solve the trunk direction on the straight-arm support circle.
    rad=math.radians(a);lr=math.sqrt(.8695**2-.01**2)
    H=[-.8+lr*math.cos(rad),0,.08+lr*math.sin(rad)]
    armr=math.sqrt(.568**2-.05**2)
    if p1 is None:
        center=H;length=.50
    else:
        u=[math.sin(math.radians(p1)),0,math.cos(math.radians(p1))]
        center=[H[i]+.27*u[i] for i in range(3)];length=.23
    D=[.55-center[0],.035-center[2]];dist=math.hypot(*D)
    c=(dist**2+length**2-armr**2)/(2*length*dist)
    angle=math.degrees(math.atan2(D[0],D[1])-math.acos(max(-1,min(1,c))))
    if p1 is None:p1=angle
    p2=angle
    P=[H[0]+.03*math.sin(math.radians(p1)),0,H[2]+.03*math.cos(math.radians(p1))]
    return base_pose(pelvis=P,p1=p1,p2=p2,hp=0,
        ankle_l=[-.8,.10,.08],ankle_r=[-.8,-.10,.08],
        knee_l=[0,0,-1],knee_r=[1,0,0],foot_l=[-1,0,0],foot_r=[-1,0,0],
        wrist_l=[.55,.23,.035],wrist_r=[.55,-.23,.035],
        elbow_l=[0,1,0],elbow_r=[0,-1,0],hand_l=[1,0,0],hand_r=[1,0,0],
        palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=0,grip_r=0)

EXERCISES = {
    "spanish-squat": spanish_squat,
    "terminal-knee-extension": terminal_knee_extension,
    "petersen-step-up": petersen_step_up,
    "stair-descent-slow": stair_descent_slow,
    "chin-nod-supine": chin_nod_supine,
    "chin-tuck-seated": chin_tuck_seated,
    "chin-tuck-head-lift": chin_tuck_head_lift,
    "neck-isometric-4-way": neck_isometric_4_way,
    "neck-head-raise-manual": neck_head_raise_manual,
    "joint-circles": joint_circles,
    "leg-swings": leg_swings,
    "deep-squat-hold": deep_squat_hold,
    "hip-raise-pushup": hip_raise_pushup,
}
