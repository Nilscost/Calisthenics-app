"""R8 posterior-chain, bridge and hip-thrust poses."""
import math
from poses import base_pose, rep, pt

FLAT = dict(palm_l=[0,0,-1], palm_r=[0,0,-1], grip_l=0.0, grip_r=0.0)

def _keys(fn, steps=72):
    # Dense uniform trajectory samples prevent the backbend/dropback from cutting corners.
    return [(i/steps, fn(i/steps)) for i in range(steps)]

def _pose(pelvis, p1, p2, ankles, wrists=None, **kw):
    d=dict(pelvis=list(pelvis),p1=p1,p2=p2,hp=kw.pop('hp',0),
           ankle_l=list(ankles[0]),ankle_r=list(ankles[1]),
           knee_l=kw.pop('knee_l',[1,0,0]),knee_r=kw.pop('knee_r',[1,0,0]),
           foot_l=kw.pop('foot_l',[1,0,0]),foot_r=kw.pop('foot_r',[1,0,0]),
           elbow_l=kw.pop('elbow_l',[0,1,0]),elbow_r=kw.pop('elbow_r',[0,-1,0]),
           hand_l=kw.pop('hand_l',[1,0,0]),hand_r=kw.pop('hand_r',[1,0,0]),**FLAT)
    if wrists is None:
        d.update(wrist_l_rel=[0.02,0.22,-0.55],wrist_r_rel=[0.02,-0.22,-0.55])
    else:
        d.update(wrist_l=list(wrists[0]),wrist_r=list(wrists[1]))
    d.update(kw)
    return base_pose(**d)

def _ease(x): return x*x*(3-2*x)
def _cycle(fn):
    # Top-bottom-top with a smooth return and no endpoint discontinuity.
    return _keys(lambda t: fn(0.5-0.5*math.cos(2*math.pi*t)))

def bridge_reverse_plank():
    # Straight reverse-plank line; hands fixed behind the hips, feet fixed on the floor.
    ankles=([.90,.10,.08],[.90,-.10,.08])
    wrists=([-.62,.22,.035],[-.62,-.22,.035])
    def pose(pitch):
        up=[math.sin(math.radians(pitch)),0,math.cos(math.radians(pitch))]
        pelvis=[.90+.899*up[0],0,.08+.899*up[2]]
        return _pose(pelvis,pitch,pitch,ankles,wrists,hp=-5,
                     elbow_l=[0,1,0],elbow_r=[0,-1,0],hand_l=[1,0,0],hand_r=[1,0,0])
    return dict(seconds=3,keys=[(0,pose(-70)),(.5,pose(-69.9))],cam_az=30,cam_el=18,props=[])

def bridge_half():
    # Partial back bridge, hands beside the head and feet flat; elbows remain bent.
    ankles=([0.62,0.10,0.08],[0.62,-0.10,0.08])
    wrists=([-.20,.23,.035],[-.20,-.23,.035])
    def pose(q):
        z=.13+.39*q
        pitch=-90-18*q
        return _pose([.05,0,z],pitch,pitch,ankles,wrists,hp=-6,
                     elbow_l=[0,1,.2],elbow_r=[0,-1,.2])
    return dict(seconds=3,keys=_cycle(pose),cam_az=32,cam_el=19,props=[])

def bridge_wall_walkup():
    # Alternate left/right hand steps down and up a forward wall; feet stay planted.
    wall=dict(kind="box",center=[-.60,0,1.20],size=[.05,1.6,2.4])
    ankles=([0,.10,.08],[0,-.10,.08])
    def pose(t):
        q=2*t if t<.5 else 2*(1-t)
        q=_ease(q)
        # Forty alternating half-steps preserve wall contact while each hand descends in turn.
        n=min(39,int(q*40)); r=_ease(q*40-n); step=1.665/20
        left=n//2; right=n//2
        if n%2==0:
            zl=1.70-left*step-r*step; zr=1.70-right*step
        else:
            zl=1.70-(left+1)*step; zr=1.70-right*step-r*step
        if q>=1: zl=zr=.035
        pz=.955-.355*q; px=.05*q
        p1=-10-55*q; p2=-15-125*q
        return _pose([px,0,pz],p1,p2,ankles,[[-.565,.20,zl],[-.565,-.20,zr]],hp=-.90*p2,
                     palm_l=[-1,0,0],palm_r=[-1,0,0],hand_l=[0,0,1],hand_r=[0,0,1],
                     elbow_l=[.3,1,0],elbow_r=[.3,-1,0])
    return dict(seconds=5,keys=_keys(pose,120),cam_az=32,cam_el=14,props=[wall])

def _dropback_pose(s):
    # Keep the arms long beside the head throughout the dropback, rather than
    # interpolating the wrists through the shoulders mid-repetition.
    p1=-5-60*s; p2=-8-132*s
    px=.58-.53*s; pz=.975-.375*s
    a1=math.radians(p1); a2=math.radians(p2)
    sx=px+.24*math.sin(a1)+.23*math.sin(a2)
    sz=pz+.24*math.cos(a1)+.23*math.cos(a2)
    arm=math.radians(p2-10*s)
    wx=sx+.565*math.sin(arm); wz=sz+.565*math.cos(arm)
    return _pose([px,0,pz],p1,p2,([.58,.10,.08],[.58,-.10,.08]),
                 [[wx,.20,wz],[wx,-.20,wz]],hp=0,
                 elbow_l=[0,1,0],elbow_r=[0,-1,0])


def bridge_closing():
    mat=dict(kind="box",center=[0,0,.005],size=[2.8,1.5,.01])
    bridge=_dropback_pose(1)
    def pose(t):
        u=2*t if t<.5 else 2*(1-t)
        if u<.68:return _dropback_pose(_ease(u/.68))
        q=_ease((u-.68)/.32)
        return _pose([.05+.01*q,0,.60-.47*q],-65-25*q,-140+50*q,
                     ([.58,.10,.08],[.58,-.10,.08]),
                     [bridge['wrist_l'],bridge['wrist_r']],hp=0,
                     elbow_l=[0,1,0],elbow_r=[0,-1,0])
    return dict(seconds=6,keys=_keys(pose,144),cam_az=32,cam_el=18,props=[mat])


def bridge_stand_to_stand():
    mat=dict(kind="box",center=[0,0,.005],size=[2.8,1.5,.01])
    def pose(t):
        q=2*t if t<.5 else 2*(1-t)
        return _dropback_pose(_ease(q))
    return dict(seconds=5,keys=_keys(pose,120),cam_az=32,cam_el=18,props=[mat])


def bridge_wall_push_off():
    wall=dict(kind="box",center=[-.60,0,1.20],size=[.05,1.6,2.4])
    ankles=([0,.10,.08],[0,-.10,.08])
    def pose(t):
        q=2*t if t<.5 else 2*(1-t); q=_ease(q)
        # Lean back to the wall, load bent arms briefly, then spring away to stand.
        px=.20*q; pz=.955-.08*q; p1=-8-35*q; p2=-12-58*q
        wx=-.565 if q>=.30 else .02-(.585*q/.30)
        wz=1.62-.32*q
        return _pose([px,0,pz],p1,p2,ankles,[[wx,.20,wz],[wx,-.20,wz]],hp=0,
                     palm_l=[-1,0,0],palm_r=[-1,0,0],hand_l=[0,0,1],hand_r=[0,0,1],
                     elbow_l=[.3,1,0],elbow_r=[.3,-1,0])
    return dict(seconds=3.5,keys=_keys(pose),cam_az=32,cam_el=14,props=[wall])

def _thrust_pose(z, one_leg=False):
    # Upper back/chest stays just above the bench while the pelvis rotates at the hip.
    q=max(0.0,min(1.0,(z-.45)/.25))
    p1=-30-60*q
    p2=-90
    pelvis=[-.55-.24*math.sin(math.radians(p1))-.26*math.sin(math.radians(p2)),0,
            .56-.24*math.cos(math.radians(p1))-.26*math.cos(math.radians(p2))]
    a=dict(ankle_l=[.43,.11,.08],ankle_r=[.43,-.11,.08],knee_l=[1,0,0],knee_r=[1,0,0],
           foot_l=[1,0,0],foot_r=[1,0,0],wrist_l_rel=[.10,.10,-.45],wrist_r_rel=[.10,-.10,-.45],
           hand_l=[1,0,0],hand_r=[1,0,0],
           elbow_l=[0,1,0],elbow_r=[0,-1,0])
    if one_leg:
        a.pop('ankle_l'); a['ankle_l_rel']=[.02,.02,.72]; a['knee_l']=[1,0,0]
    return base_pose(pelvis=pelvis,p1=p1,p2=p2,hp=0,**a)

def hip_thrust():
    bench=dict(kind="box",center=[-.55,0,.225],size=[.45,1.0,.45])
    top=_thrust_pose(.70); bottom=_thrust_pose(.45)
    return dict(seconds=3.5,keys=rep(top,bottom),cam_az=32,cam_el=15,props=[bench])

def hip_thrust_single_leg():
    bench=dict(kind="box",center=[-.55,0,.225],size=[.45,1.0,.45])
    top=_thrust_pose(.70,True); bottom=_thrust_pose(.45,True)
    return dict(seconds=3.5,keys=rep(top,bottom),cam_az=32,cam_el=15,props=[bench])

def back_extension_floor():
    # Small prone chest lift while pelvis and straight legs remain on the floor.
    def pose(q):
        return base_pose(pelvis=[0,0,.11],p1=90,p2=90-15*q,hp=-12,
                         ankle_l=[-.88,.10,.08],ankle_r=[-.88,-.10,.08],knee_l=[0,0,1],knee_r=[0,0,1],
                         foot_l=[-1,0,0],foot_r=[-1,0,0],hand_l=[1,0,0],hand_r=[1,0,0],
                         wrist_l_rel=[.02,.10,.00],wrist_r_rel=[.02,-.10,.00],
                         elbow_l=[0,1,0],elbow_r=[0,-1,0])
    return dict(seconds=3.5,keys=_cycle(pose),cam_az=32,cam_el=20,props=[])

def _harop_pose(q, beginner=False):
    angle=(60 if beginner else 75)*_ease(q)
    # Keep hips at z=.49 while the trunk rotates over the fixed kneeling base.
    pelvis=[.03*math.sin(math.radians(angle)),0,.49+.03*math.cos(math.radians(angle))]
    return base_pose(pelvis=pelvis,p1=angle,p2=angle,hp=0,
                     ankle_l=[-.43,.10,.08],ankle_r=[-.43,-.10,.08],
                     knee_l=[0,0,-1],knee_r=[0,0,-1],foot_l=[-1,0,0],foot_r=[-1,0,0],
                     wrist_l_rel=[.10,.10,-.38],wrist_r_rel=[.10,-.10,-.38],
                     elbow_l=[0,1,0],elbow_r=[0,-1,0])

def _harop(beginner):
    anchor=dict(kind="box",center=[-.43,0,.18],size=[.22,.60,.36])
    def pose(t):
        q=.5-.5*math.cos(2*math.pi*t)
        return _harop_pose(q,beginner)
    return dict(seconds=4,keys=_keys(pose),cam_az=32,cam_el=18,props=[anchor])

def harop_curl_beginner(): return _harop(True)
def harop_curl(): return _harop(False)

EXERCISES={
 "bridge-reverse-plank":bridge_reverse_plank,
 "bridge-half":bridge_half,
 "bridge-wall-walkup":bridge_wall_walkup,
 "bridge-closing":bridge_closing,
 "bridge-stand-to-stand":bridge_stand_to_stand,
 "bridge-wall-push-off":bridge_wall_push_off,
 "hip-thrust":hip_thrust,
 "hip-thrust-single-leg":hip_thrust_single_leg,
 "back-extension-floor":back_extension_floor,
 "harop-curl-beginner":harop_curl_beginner,
 "harop-curl":harop_curl,
}
