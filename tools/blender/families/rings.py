"""Ring support, dip, push-up and muscle-up poses for the R8 set."""
import math
from poses import base_pose, pt, dense, rep, solve

BAR_Z = 2.30


def _legs(tuck=0.0, lift=0.0):
    # Free legs hang from the pelvis; tuck bends the knees forward.
    return dict(ankle_l_rel=[0, .02, -.869 + lift + tuck],
                ankle_r_rel=[0, -.02, -.869 + lift + tuck],
                knee_l=[1, 0, .45 if tuck else 0], knee_r=[1, 0, -.45 if tuck else 0],
                foot_l=[1, 0, 0], foot_r=[1, 0, 0])


def _ring_props(anchor_z=BAR_Z):
    # Rod endpoints are attached directly to the wrist targets in every frame.
    return [dict(kind="rod", a=[0, .25, anchor_z], b=pt("wrist_l"), r=.015),
            dict(kind="rod", a=[0, -.25, anchor_z], b=pt("wrist_r"), r=.015)]


def _support(pz=1.25, rto=False, l_sit=False):
    # Hip-height handles, shoulders depressed, arms near vertical and locked.
    z = 1.157
    legs = _legs() if not l_sit else dict(ankle_l_rel=[.869, .02, .005], ankle_r_rel=[.869, -.02, .005],
                                           knee_l=[0,0,1], knee_r=[0,0,1], foot_l=[1,0,0], foot_r=[1,0,0])
    return base_pose(pelvis=[-.04,0,pz],p1=5,p2=5,hp=0,
        wrist_l=[-.04,.25,z],wrist_r=[-.04,-.25,z],
        elbow_l=[-.5,.7,1],elbow_r=[-.5,-.7,1],
        hand_l=[1,0,0],hand_r=[1,0,0],palm_l=[0,0,-1] if not rto else [1,0,0],palm_r=[0,0,-1] if not rto else [1,0,0],
        grip_l=.9,grip_r=.9,**legs)


def ring_support_hold():
    a=_support(1.25); b=_support(1.252)
    return dict(seconds=3,keys=rep(a,b),cam_az=30,cam_el=10,props=_ring_props())


def ring_support_rto():
    a=_support(1.25,True); b=_support(1.252,True)
    return dict(seconds=3,keys=rep(a,b),cam_az=30,cam_el=10,props=_ring_props())


def ring_l_sit():
    a=_support(1.25,l_sit=True); b=_support(1.252,l_sit=True)
    return dict(seconds=3,keys=rep(a,b),cam_az=30,cam_el=12,props=_ring_props())


def _dip_pose(t, l_sit=False, rto=False):
    # Ring handles remain at hip height, and the body lowers between them.
    pz=1.25-.26*t
    legs = (dict(ankle_l_rel=[.869,.02,.005],ankle_r_rel=[.869,-.02,.005],knee_l=[0,0,1],knee_r=[0,0,1],foot_l=[1,0,0],foot_r=[1,0,0])
            if l_sit else _legs())
    # A forward chest lean helps the shoulders pass below the elbows at depth.
    pitch=5+20*t
    r=float(rto)
    return base_pose(pelvis=[-.04-.06*t,0,pz],p1=pitch,p2=pitch,hp=0,
        wrist_l=[-.04,.25,1.157],wrist_r=[-.04,-.25,1.157],
        elbow_l=[-1+r,.3,r],elbow_r=[-1+r,-.3,r],
        hand_l=[1,0,0],hand_r=[1,0,0],palm_l=[r,0,-(1-r)],palm_r=[r,0,-(1-r)],
        grip_l=.9,grip_r=.9,**legs)


def ring_dip_negative():
    # Emphasize the controlled eccentric; the brief return represents the step-up reset.
    def pose(t): return _dip_pose(t)
    keys=dense(pose,[(0,{'t':0}),(.08,{'t':0}),(.78,{'t':1}),(.90,{'t':1}),(.99,{'t':0})],64)
    return dict(seconds=6,keys=keys,cam_az=30,cam_el=12,props=_ring_props())


def ring_dip():
    return dict(seconds=3.2,keys=dense(_dip_pose,rep({'t':0},{'t':1}),48),cam_az=30,cam_el=12,props=_ring_props())


def ring_dip_l():
    return dict(seconds=3.4,keys=dense(lambda t:_dip_pose(t,l_sit=True),rep({'t':0},{'t':1}),48),cam_az=30,cam_el=12,props=_ring_props())


def ring_dip_rto():
    return dict(seconds=3.3,keys=dense(lambda t:_dip_pose(t,rto=1-t),rep({'t':0},{'t':1}),48),cam_az=30,cam_el=12,props=_ring_props())


def _push(a, wide=0, rto=False, archer=0, lean=0):
    # Maintain a straight plank around planted toes. Ring wrist targets are fixed in height.
    ar=math.radians(a); d=[math.cos(ar),0,math.sin(ar)]
    ankle_x=-1.08; pelvis=[ankle_x+d[0]*.90,0,.08+d[2]*.90]
    wy=.25+wide
    # Both wrist contacts are exact. Archer's left arm opens straight while right side works.
    wx=-.08-.115*max(0,min(1,(a-4)/13))
    wl=[wx,wy,.11]; wr=[wx,-wy,.11]
    el_l=[-.6,.7,1];el_r=[-.6,-.7,1]
    if archer:
        wl=[wx,.25+archer,.11]; wr=[wx,-.25,.11]
        el_l=[-1,.2,0]
    palm=[rto,0,-(1-rto)] if isinstance(rto,(int,float)) else [0,0,-1]
    p=base_pose(pelvis=pelvis,p1=90-a+lean,p2=90-a+lean,hp=-8,
        ankle_l=[ankle_x,.08,.08],ankle_r=[ankle_x,-.08,.08],
        foot_l=[.95,0,-.3],foot_r=[.95,0,-.3],knee_l=[0,0,-1],knee_r=[0,0,-1],
        wrist_l=wl,wrist_r=wr,elbow_l=el_l,elbow_r=el_r,
        hand_l=[1,0,0],hand_r=[1,0,0],palm_l=palm,palm_r=palm,grip_l=.8,grip_r=.8)
    if archer:
        shoulder=solve(p,{})['shoulder_l']
        dx=shoulder[0]-wl[0]; dz=shoulder[2]-wl[2]
        lateral=math.sqrt(max(0,.569**2-dx*dx-dz*dz))
        p['wrist_l']=[wl[0],shoulder[1]+lateral,wl[2]]
    return p


def _push_props():
    return [dict(kind="rod",a=[0,.25,BAR_Z],b=pt("wrist_l"),r=.015),dict(kind="rod",a=[0,-.25,BAR_Z],b=pt("wrist_r"),r=.015)]


def ring_pushup_wide():
    keys=dense(lambda a,w:_push(a,wide=.10*w),rep({'a':17,'w':0},{'a':4,'w':1}),48)
    return dict(seconds=3.2,keys=keys,cam_az=28,cam_el=18,props=_push_props())


def ring_pushup():
    keys=dense(lambda a:_push(a),rep({'a':17},{'a':4}),48)
    return dict(seconds=3.2,keys=keys,cam_az=28,cam_el=18,props=_push_props())


def ring_pushup_rto():
    def pose(a,turn): return _push(a,rto=turn)
    events=[(0,{'a':17,'turn':1}),(.18,{'a':17,'turn':1}),(.48,{'a':4,'turn':0}),(.72,{'a':4,'turn':0}),(.90,{'a':17,'turn':1}),(1,{'a':17,'turn':1})]
    return dict(seconds=3.2,keys=dense(pose,events,48),cam_az=28,cam_el=18,props=_push_props())


def ring_pushup_rto_archer():
    def pose(a,s): return _push(a,rto=True,archer=s*.16)
    events=[(0,{'a':17,'s':0}),(.20,{'a':17,'s':0}),(.46,{'a':5,'s':1}),(.58,{'a':5,'s':1}),(.82,{'a':17,'s':0}),(1,{'a':17,'s':0})]
    return dict(seconds=3.4,keys=dense(pose,events,48),cam_az=28,cam_el=18,props=_push_props())


def _pppu(a,lean):
    # Place the rings behind each shoulder by the angle-specific horizontal arm projection.
    # The hands drift slightly forward during the deepest part to preserve elbow bend.
    fraction=max(0,min(1,(a-4)/11))
    body_angle=max(4,a+(4.6 if lean==40 else -2))
    p=_push(body_angle,rto=True)
    shoulder=solve(p,{})['shoulder_r']
    dx=(.30+.066*fraction) if lean==40 else (.32+.167*fraction)
    z=.11
    p['wrist_l']=[shoulder[0]-dx,.25,z]
    p['wrist_r']=[shoulder[0]-dx,-.25,z]
    return p


def ring_pppu_40():
    return dict(seconds=3.4,keys=dense(lambda a:_pppu(a,40),rep({'a':15},{'a':5}),48),cam_az=28,cam_el=18,props=_push_props())


def ring_pppu_60():
    return dict(seconds=3.4,keys=dense(lambda a:_pppu(a,60),rep({'a':15},{'a':5}),48),cam_az=28,cam_el=18,props=_push_props())


def ring_false_grip_hang():
    def pose(b):
        return base_pose(pelvis=[-.04,0,1.18+.002*b],p1=-3,p2=-6,hp=0,
            wrist_l=[-.045,.25,2.217],wrist_r=[-.045,-.25,2.217],
            elbow_l=[.2,.7,-.2],elbow_r=[.2,-.7,-.2],hand_l=[0,0,1],hand_r=[0,0,1],
            palm_l=[0,0,1],palm_r=[0,0,1],grip_l=.95,grip_r=.95,**_legs())
    return dict(seconds=3,keys=rep(pose(0),pose(1)),cam_az=32,cam_el=10,props=_ring_props())


def _mu_pose(stage):
    # Fixed-height handles; the athlete rises from a hang through a close turnover to support.
    z=2.21
    pz=[1.18,1.68,1.95,2.304][stage]
    px=[-.04,.02,.13,-.04][stage]
    pitch=[-4,12,25,5][stage]
    legs=[_legs(),_legs(tuck=.24),_legs(tuck=.30),_legs()][stage]
    return base_pose(pelvis=[px,0,pz],p1=pitch,p2=pitch,hp=-3,
        wrist_l=[-.045,.25,z],wrist_r=[-.045,-.25,z],elbow_l=[.2,.7,-.2],elbow_r=[.2,-.7,-.2],
        hand_l=[0,0,1] if stage<3 else [1,0,0],hand_r=[0,0,1] if stage<3 else [1,0,0],
        palm_l=[1,0,0] if stage<3 else [0,0,-1],palm_r=[1,0,0] if stage<3 else [0,0,-1],
        grip_l=.95,grip_r=.95,**legs)


def _muscle_up(negative=False):
    # Ring contacts move in lockstep with wrist targets throughout a slow transition.
    if negative:
        events=[(0,{'s':3}),(.10,{'s':3}),(.30,{'s':2}),(.46,{'s':1}),(.72,{'s':0}),(.80,{'s':0}),(1,{'s':3})]
        duration=7
    else:
        events=[(0,{'s':0}),(.12,{'s':0}),(.32,{'s':1}),(.48,{'s':2}),(.64,{'s':3}),(.76,{'s':3}),(.90,{'s':2}),(1,{'s':0})]
        duration=4.5
    def pose(s):
        lo=int(math.floor(s)); hi=min(3,lo+1); f=s-lo
        a=_mu_pose(lo); b=_mu_pose(hi)
        # Interpolate all numeric fields, with smooth dense timing between phase markers.
        return {k:(a[k]+(b[k]-a[k])*f if isinstance(a[k],(int,float)) else [x+(y-x)*f for x,y in zip(a[k],b[k])]) for k in a}
    return dict(seconds=duration,keys=dense(pose,events,64),cam_az=32,cam_el=12,props=_ring_props(3.4))


def ring_muscle_up_negative(): return _muscle_up(True)
def ring_muscle_up():
    from ring_muscleup import specification
    return specification()


EXERCISES={
 'ring-support-hold':ring_support_hold,'ring-support-rto':ring_support_rto,'ring-l-sit':ring_l_sit,
 'ring-dip-negative':ring_dip_negative,'ring-dip':ring_dip,'ring-dip-l':ring_dip_l,'ring-dip-rto':ring_dip_rto,
 'ring-pushup-wide':ring_pushup_wide,'ring-pushup':ring_pushup,'ring-pushup-rto':ring_pushup_rto,
 'ring-pushup-rto-archer':ring_pushup_rto_archer,'ring-pppu-40':ring_pppu_40,'ring-pppu-60':ring_pppu_60,
 'ring-false-grip-hang':ring_false_grip_hang,'ring-muscle-up-negative':ring_muscle_up_negative,'ring-muscle-up':ring_muscle_up,
}
