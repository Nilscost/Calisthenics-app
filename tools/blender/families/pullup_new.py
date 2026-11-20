"""Nineteen R8 pull-up, hang, transition and pullover poses."""
import math
from poses import base_pose, pt, rep, dense

BAR_Z = 2.30
HANG_Z = BAR_Z - 0.065


def _bar(z=BAR_Z):
    return dict(kind="bar", z=z, half=0.72, r=0.017)


def _hands(grip=0.24, x=-0.045, z=HANG_Z, wide=None, palms=(1,0,0), hand=(0,0,1), grips=(0.95,0.95),
           left=None, right=None, elbows=((0.25,1,-0.2),(0.25,-1,-0.2))):
    yl = grip if wide is None else wide
    yr = -grip if wide is None else -wide
    l = left or [x,yl,z]
    r = right or [x,yr,z]
    return dict(wrist_l=list(l),wrist_r=list(r),palm_l=list(palms),palm_r=list(palms),
                hand_l=list(hand),hand_r=list(hand),grip_l=grips[0],grip_r=grips[1],
                elbow_l=list(elbows[0]),elbow_r=list(elbows[1]))


def _legs(fwd=0.04, tuck=0.0, wide=0.01):
    # Free hanging legs; tuck shortens the knees and raises the ankles.
    z = -0.865 + tuck
    return dict(ankle_l_rel=[fwd,-wide,z],ankle_r_rel=[fwd,wide,z],
                knee_l=[1,0,0.25 if tuck else 0],knee_r=[1,0,-0.25 if tuck else 0],
                foot_l=[0.4,0,-1],foot_r=[0.4,0,-1])


def _pose(pz=1.205, px=-0.04, py=0, p1=-3, p2=-6, hands=None, legs=None, **kw):
    f={}
    f.update(hands or _hands())
    f.update(legs or _legs())
    f.update(kw)
    return base_pose(pelvis=[px,py,pz],p1=p1,p2=p2,hp=-4,**f)


def _hang(**kw): return _pose(**kw)


def _top(pz=1.62, px=-0.04, py=0, p1=-2, p2=-8, hands=None, legs=None, **kw):
    return _pose(pz,px,py,p1,p2,hands or _hands(),legs or _legs(),**kw)


def _rep(bottom, top, seconds=3.0, props=None, **kw):
    return dict(seconds=seconds,keys=rep(bottom,top),cam_az=35,cam_el=10,props=props or [_bar()],**kw)


def pullup_jumping():
    # Start in a shallow floor squat below the bar, then jump to latch and pull; descend slowly.
    prelegs=dict(ankle_l=[-0.02,0.10,0.08],ankle_r=[-0.02,-0.10,0.08],knee_l=[1,0,0],knee_r=[1,0,0],foot_l=[1,0,0],foot_r=[1,0,0])
    midlegs=dict(ankle_l=[0.02,0.10,0.30],ankle_r=[0.02,-0.10,0.30],knee_l=[1,0,0],knee_r=[1,0,0],foot_l=[1,0,0],foot_r=[1,0,0])
    top_legs=dict(ankle_l=[0.02,0.10,0.88],ankle_r=[0.02,-0.10,0.88],knee_l=[1,0,0],knee_r=[1,0,0],foot_l=[0.4,0,-1],foot_r=[0.4,0,-1])
    pre=_pose(0.93,-0.04,0,10,12,_hands(z=1.92,grips=(0,0)),prelegs)
    jump=_pose(1.20,-0.04,0,4,6,_hands(z=2.05,grips=(0.35,0.35)),midlegs)
    top=_pose(1.74,0.16,0,-2,-8,_hands(),top_legs)
    keys=[(0,pre),(0.14,pre),(0.32,jump),(0.48,top),(0.57,top),(0.78,jump),(0.96,pre)]
    return dict(seconds=3.8,keys=keys,cam_az=35,cam_el=10,props=[_bar()])


def pullup_partial():
    # Top-half only: never drops below the 90-degree elbow position.
    top=_top(pz=1.74,px=0.16)
    mid=_top(pz=1.40,p1=-4,p2=-8)
    return dict(seconds=3.0,keys=[(0,top),(0.16,top),(0.50,mid),(0.84,top),(0.98,top)],cam_az=35,cam_el=10,props=[_bar()])


def pullup_false_grip_hang():
    # Bent wrists roll over the bar into the heel of each palm; active straight-arm hang.
    a=_hang(p1=-5,p2=-8,hands=_hands(palms=(0,0,1),hand=(1,0,0)))
    b=_hang(pz=1.21,p1=-5,p2=-7,hands=_hands(palms=(0,0.25,0.97),hand=(1,0,0)))
    return dict(seconds=3.0,keys=rep(a,b),cam_az=35,cam_el=12,props=[_bar()])


def pullup_sternum():
    legs=_legs()
    hang=_hang(legs=legs)
    # Raise the lower chest to the bar and recline the torso through the finish.
    top=_top(pz=1.88,px=0.19,p1=-24,p2=-38,legs=legs)
    return _rep(hang,top,3.8)


def muscle_up_negative():
    # Slow reverse sequence: straight-arm support, dip, turnover, pull-up, then dead hang.
    def support():
        h=_hands(z=BAR_Z+0.06,palms=(0,0,-1),hand=(1,0,0),elbows=((0,1,0),(0,-1,0)))
        return _pose(2.45,-0.10,0,0,0,h,_legs(tuck=0.12))
    def transition(t):
        # Arms move from above-bar support toward a close chest-to-bar turnover.
        z=(BAR_Z+0.06)*(1-t)+(HANG_Z*t)
        h=_hands(z=z,x=-0.02,palms=(1,0,0),hand=(0,0,1),elbows=((0.15,1,-0.2),(0.15,-1,-0.2)))
        return _pose(2.45-1.24*t,-0.10+0.05*t,0,30*t,25*t,h,_legs(tuck=0.12*(1-t)))
    s=support(); low=_hang()
    keys=[(0,s),(0.10,s),(0.30,transition(.30)),(0.48,transition(.62)),(0.68,transition(.83)),(0.88,low),(0.98,low)]
    return dict(seconds=7.0,keys=dense(lambda t: transition(t),[(0.30,{"t":0.30}),(0.48,{"t":0.62}),(0.68,{"t":0.83})],steps=28) if False else keys,
                cam_az=35,cam_el=10,props=[_bar()])


def muscle_up_kipping():
    # A broad hollow-to-arch kip visibly swings hips and legs before the pull.
    def swing(phase):
        a=2*math.pi*phase
        p2=24.0*math.cos(a); p1=.78*p2
        px=-.045-.24*math.sin(math.radians(p1))-.23*math.sin(math.radians(p2))
        return _hang(pz=1.255+.045*math.sin(a),px=px,p1=p1,p2=p2,
                     hands=_hands(grip=.18,z=BAR_Z-.060),legs=_legs(fwd=.30*math.cos(a),tuck=.14+.10*math.sin(a)))
    arch=swing(.5); hollow=swing(0)
    pull=_top(pz=1.56,p1=-8,p2=-12,legs=_legs(fwd=.10,tuck=0.16))
    turn=_pose(1.82,-0.03,0,22,20,_hands(z=BAR_Z+0.01,palms=(0,0,-1),hand=(1,0,0)),_legs(tuck=0.20))
    support=_pose(2.45,-0.10,0,0,0,_hands(grip=0.18,z=BAR_Z+0.06,palms=(0,0,-1),hand=(1,0,0),elbows=((0,1,0),(0,-1,0))),_legs(tuck=0.10))
    keys=[(0,arch),(.10,hollow),(.19,arch),(.28,hollow),(.36,pull),(.47,turn),(.60,support),(.70,support),(.80,turn),(.90,arch),(.98,hollow)]
    return dict(seconds=4.5,keys=keys,cam_az=35,cam_el=10,props=[_bar()])


def pullup_l_sit():
    def legs(zlift=0):
        return dict(ankle_l_rel=[0.868,0.005,-0.03+zlift],ankle_r_rel=[0.868,-0.005,-0.03+zlift],
                    knee_l=[0,0,1],knee_r=[0,0,1],foot_l=[1,0,0],foot_r=[1,0,0])
    bottom=_hang(legs=legs())
    top=_top(pz=1.74,px=0.16,p1=-2,p2=-8,legs=legs())
    return _rep(bottom,top,3.8)


def pullup_archer():
    def pose(pz,py=0,work=True):
        # Right hand is the near-side working arm; left arm stays long on the bar.
        h=_hands(wide=0.43,elbows=((0.2,1,-0.2),(0.2,-1,-0.2)))
        return _pose(pz,-0.04,py,-2,-8,h,_legs())
    bottom=pose(1.26); top=pose(1.74,-0.08)
    return _rep(bottom,top,3.8)


def pullup_typewriter():
    def at(py,pz):
        return _pose(pz,-0.04,py,-2,-8,_hands(wide=0.43),_legs())
    hang=at(0,1.26)
    right=at(-0.12,1.74); left=at(0.12,1.74)
    keys=[(0,hang),(0.15,hang),(0.34,right),(0.48,left),(0.62,right),(0.78,hang),(0.98,hang)]
    return dict(seconds=4.5,keys=keys,cam_az=35,cam_el=10,props=[_bar()])


def pullup_uneven():
    # Right bar hand bears most of the load; the left hand holds a lower towel loop.
    towel_z=1.84
    hands=_hands(grip=0.24,left=[-0.045,0.43,towel_z],right=[-0.045,-0.24,HANG_Z],elbows=((0.2,1,-0.2),(0.2,-1,-0.2)))
    top_hands=_hands(grip=0.24,left=[-0.045,0.43,towel_z],right=[-0.045,-0.24,HANG_Z],elbows=((0.2,1,-0.2),(0.2,-1,-0.2)))
    hang=_pose(1.205,-0.04,0,-3,-6,hands,_legs())
    top=_pose(1.74,0.17,-0.04,-2,-8,top_hands,_legs())
    towel=dict(kind="rod",a=[0,0.43,BAR_Z],b=pt("wrist_l"),r=0.02)
    return _rep(hang,top,3.8,[_bar(),towel])


def pullup_one_arm_flex_hang():
    # Hold at the top on the working right arm; released left arm hangs beside the body.
    h=_hands()
    def key(pz,px=0.16):
        released=_hands(left=[-0.15,0.30,1.84],elbows=((-1,0.4,0),(0.25,-1,-0.2)))
        released["grip_l"]=0
        released["wrist_l"][0]=px-0.11
        return _pose(pz,px,0,-3,-8,released,_legs())
    return dict(seconds=3.0,keys=rep(key(1.74),key(1.76)),cam_az=35,cam_el=10,props=[_bar()])


def pullup_one_arm_assisted():
    def key(pz):
        # Left helper remains cupped around the right working wrist for the full rep.
        lh=[-0.02,-0.06,HANG_Z-0.07]
        h=_hands(left=lh,right=[-0.02,-0.04,HANG_Z],hand=(0,0,1),elbows=((0.2,1,-0.2),(0.2,-1,-0.2)))
        return _pose(pz,-0.04,0,-3,-7,h,_legs(),yaw=90,grip_l=0.35)
    return _rep(key(1.22),key(1.74),3.8)


def pullup_one_arm_negative():
    # Begin at the top; only the right hand remains on the bar during the slow eccentric.
    def key(pz,progress,px=-0.04):
        free=[px-0.14,0.33,1.86+0.34*progress]
        h=_hands(left=free,right=[-0.045,-0.24,HANG_Z],elbows=((0.2,1,-0.2),(0.2,-1,-0.2)))
        return _pose(pz,px,0,-3,-7,h,_legs(),grip_l=0)
    top=key(1.74,0,0.16); bottom=key(1.24,1)
    keys=[(0,top),(0.12,top),(0.84,bottom),(0.98,bottom)]
    return dict(seconds=7.0,keys=dense(lambda s:key(1.62-0.415*s,s),[(0.12,{"s":0}),(0.84,{"s":1})],steps=32) if False else keys,
                cam_az=35,cam_el=10,props=[_bar()])


def pullup_one_arm():
    # Strict single right-arm pull-up with the left arm free and torso rotation controlled.
    def key(pz,yaw,px=-0.04):
        h=_hands(left=[px-0.12,0.24,1.78],right=[-0.045,-0.18,HANG_Z],elbows=((0,1,0),(0,-1,-0.2)))
        return _pose(pz,px,0,-3,-7,h,_legs(),yaw=yaw,grip_l=0)
    return _rep(key(1.205,0),key(1.74,5,0.16),4.0)


def pullup_commando():
    # Alternating staggered hands, with the head passing to either side of the bar.
    def pose(pz,py,xoff):
        h=_hands(left=[xoff+0.04,0.18,HANG_Z],right=[xoff-0.04,-0.18,HANG_Z],elbows=((0.2,1,-0.2),(0.2,-1,-0.2)))
        return _pose(pz,-0.04,py,-3,-8,h,_legs(),yaw=12)
    hang=pose(1.25,0,0); side1=pose(1.74,-0.12,-0.02); side2=pose(1.74,0.12,0.02)
    return dict(seconds=4.2,keys=[(0,hang),(0.15,hang),(0.38,side1),(0.56,side2),(0.75,hang),(0.98,hang)],cam_az=24,cam_el=10,props=[_bar()])


def _swing_pose(phase):
    # Keep both hands fixed to the bar while shoulder/body angle drives a compact hollow-to-arch.
    theta=12.0*math.cos(2*math.pi*phase)
    p1=0.75*theta
    px=-0.045-0.24*math.sin(math.radians(p1))-0.23*math.sin(math.radians(theta))
    legs=_legs(fwd=0.12*math.cos(2*math.pi*phase),tuck=0.06)
    return _hang(pz=1.205,px=px,p1=p1,p2=theta,hands=_hands(grip=0.18,z=BAR_Z-0.060),legs=legs)


def pullup_kip_swing():
    keys=[(i/48,_swing_pose(i/48)) for i in range(48)]
    return dict(seconds=3.5,keys=keys,cam_az=35,cam_el=10,props=[_bar()])


def pullup_kipping():
    top=_top(pz=1.74,px=0.14,p1=-6,p2=-10,legs=_legs(tuck=0.15))
    keys=[(0.0,_swing_pose(0.0)),(0.12,_swing_pose(0.25)),(0.24,_swing_pose(0.50)),
          (0.34,_swing_pose(0.75)),(0.44,top),(0.53,top),(0.62,top),
          (0.72,_swing_pose(0.50)),(0.82,_swing_pose(0.75)),(0.92,_swing_pose(0.0)),(0.98,_swing_pose(0.0))]
    return dict(seconds=4.0,keys=keys,cam_az=35,cam_el=10,props=[_bar()])


def pullup_pullover_jumping():
    # Low bar (about chest height): jump from bent knees, bring hips over, and finish in support.
    z=1.0; wh=z+0.06
    def key(pelvis,p1,p2,ankles,wrz,palm=(1,0,0),elbows=((0.25,1,-0.2),(0.25,-1,-0.2)),hands=(0,0,1),px=-0.08,handx=0.0):
        h=_hands(grip=0.18,x=handx,z=wrz,palms=palm,hand=hands,elbows=elbows)
        return _pose(pelvis, px,0,p1,p2,h,ankles)
    prelegs=dict(ankle_l=[-0.12,0.10,0.08],ankle_r=[-0.12,-0.10,0.08],knee_l=[1,0,0],knee_r=[1,0,0],foot_l=[1,0,0],foot_r=[1,0,0])
    start=key(0.90,0,0,dict(ankle_l=[-0.12,0.10,0.08],ankle_r=[-0.12,-0.10,0.08],knee_l=[1,0,0],knee_r=[1,0,0],foot_l=[1,0,0],foot_r=[1,0,0]),wh)
    def orbit(angle):
        a=math.radians(angle); x=-0.08-0.36*math.sin(a); pz=1.0-0.36*math.cos(a)
        ankles=dict(ankle_l=[x-0.50*math.sin(a),0.10,pz-0.50*math.cos(a)],
                    ankle_r=[x-0.50*math.sin(a),-0.10,pz-0.50*math.cos(a)],
                    knee_l=[1,0,0],knee_r=[1,0,0],foot_l=[1,0,0],foot_r=[1,0,0])
        return key(pz,angle,angle,ankles,wh,(1,0,0),((0.1,1,0),(0.1,-1,0)),(0,0,1),px=x)
    gathered=orbit(0)
    a90=orbit(90); a180=orbit(180); a270=orbit(270); a360=orbit(360)
    support=key(1.16,360,360,dict(ankle_l=[0.12,0.10,0.35],ankle_r=[0.12,-0.10,0.35],knee_l=[1,0,0],knee_r=[1,0,0],foot_l=[1,0,0],foot_r=[1,0,0]),wh,(0,0,-1),((0,1,0),(0,-1,0)),(1,0,0),px=0.0)
    keys=[(0,start),(.08,start),(.18,gathered),(.30,a90),(.40,a180),(.48,a270),(.58,a360),(.66,support),(.74,support),(.80,a360),(.86,a270),(.92,a180),(.96,a90),(.98,gathered),(.995,start)]
    return dict(seconds=4.2,keys=keys,cam_az=35,cam_el=12,props=[_bar(z)])


def pullup_pullover():
    from bar_pullover import specification
    return specification()


EXERCISES={
    "pullup-jumping":pullup_jumping,
    "pullup-partial":pullup_partial,
    "pullup-false-grip-hang":pullup_false_grip_hang,
    "pullup-sternum":pullup_sternum,
    "muscle-up-negative":muscle_up_negative,
    "muscle-up-kipping":muscle_up_kipping,
    "pullup-l-sit":pullup_l_sit,
    "pullup-archer":pullup_archer,
    "pullup-typewriter":pullup_typewriter,
    "pullup-uneven":pullup_uneven,
    "pullup-one-arm-flex-hang":pullup_one_arm_flex_hang,
    "pullup-one-arm-assisted":pullup_one_arm_assisted,
    "pullup-one-arm-negative":pullup_one_arm_negative,
    "pullup-one-arm":pullup_one_arm,
    "pullup-commando":pullup_commando,
    "pullup-kip-swing":pullup_kip_swing,
    "pullup-kipping":pullup_kipping,
    "pullup-pullover-jumping":pullup_pullover_jumping,
    "pullup-pullover":pullup_pullover,
}
