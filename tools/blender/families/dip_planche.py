"""Dip and planche progressions for the R8 pose set."""
import math
from poses import base_pose, both, rep, dense, add, mul


def _bars(z=1.0):
    return [dict(kind='rod', a=[-.25,.25,z], b=[.25,.25,z], r=.017),
            dict(kind='rod', a=[-.25,-.25,z], b=[.25,-.25,z], r=.017)]


def _bar_dip(pz, px=0.0, p1=5, p2=5, wx=-.12, wz=1.05,
             ankle_rel=(.10,.02,-.80), knee=(1,0,0), elbow=(-1,.2,0)):
    return base_pose(pelvis=[px,0,pz],p1=p1,p2=p2,hp=0,
        **both(wrist=[wx,.25,wz],elbow=list(elbow),hand=[1,0,0],palm=[0,0,-1],grip=.9,
               knee=list(knee),ankle_rel=list(ankle_rel),foot=[1,0,0]))


def _bench_pose(pz, ankle_x, knee=(1,0,0), foot=(1,0,0), ankle_z=.08, px=-.08, wx=-.36):
    # Torso and hips stay just in front of the chair's front edge; both hands stay on its seat.
    return base_pose(pelvis=[px,0,pz],p1=0,p2=0,hp=0,
        **both(wrist=[wx,.18,.485],elbow=[-1,.2,0],hand=[1,0,0],palm=[0,0,-1],grip=0,
               ankle=[ankle_x,.12,ankle_z],knee=list(knee),foot=list(foot)))


def _bench_dip(bent=False, elevated=False):
    chair=[dict(kind='chair',x=-.50,seat=.45)]
    props=list(chair)
    if elevated:
        props.append(dict(kind='chair',x=.88,seat=.52))
    if bent:
        top=_bench_pose(.50,.22,knee=(1,0,0))
        bottom=_bench_pose(.32,.22,knee=(1,0,0))
    elif elevated:
        # Arc the pelvis around fixed elevated heels to maintain straight legs.
        def key(t):
            pz=.50-.23*t
            px=.64-math.sqrt(.8698**2-(pz-.03-.60)**2-.03**2)+.009
            pitch=24-20*t
            p=base_pose(pelvis=[px,0,pz],p1=pitch,p2=pitch,hp=0,
                **both(wrist=[-.38,.18,.485],elbow=[-1,.2,0],hand=[1,0,0],palm=[0,0,-1],grip=0,
                       ankle=[.64,.12,.60],knee=[0,0,1],foot=[1,0,0]))
            return p
        return dict(seconds=3,keys=dense(key,rep({'t':0},{'t':1}),48),cam_az=28,cam_el=12,props=props)
    else:
        # The pelvis follows an arc around planted heels to keep straight knees.
        def key(t):
            pz=.48-.21*t
            px=.72-math.sqrt(.8698**2-(pz-.03-.08)**2-.03**2)
            return _bench_pose(pz,.72,knee=(0,0,1),foot=[1,0,0],px=px,wx=-.40)
        top=key(0);bottom=key(1)
        return dict(seconds=3,keys=dense(key,rep({'t':0},{'t':1}),48),cam_az=28,cam_el=12,props=props)
    return dict(seconds=3,keys=rep(top,bottom),cam_az=28,cam_el=12,props=props)


def dip_bench_bent_knee(): return _bench_dip(bent=True)
def dip_bench_straight_leg(): return _bench_dip()
def dip_bench_feet_elevated(): return _bench_dip(elevated=True)


def dip_feet_supported():
    bars=_bars()
    box=dict(kind='box',center=[.70,0,.27],size=[.55,.9,.54])
    def key(t):
        pz=1.14-.27*t;lean=3+9*t
        k=_bar_dip(pz,px=-.02*t,p1=lean,p2=lean,wx=0.0,
                   ankle_rel=[.78,.02,-.90],knee=(1,0,0))
        # Feet rest 8 cm above the box surface while the arms still carry the load.
        hipx=k['pelvis'][0]
        k['ankle_l']=[.55,.10,.62];k['ankle_r']=[.55,-.10,.62]
        k.pop('ankle_l_rel',None);k.pop('ankle_r_rel',None)
        return k
    keys=dense(key,[(0,{'t':0}),(1,{'t':1})],48)
    return dict(seconds=3.2,keys=keys,cam_az=28,cam_el=12,props=bars+[box])


def dip_jumping():
    bars=_bars()
    def key(t):
        # Begin in a floor-supported crouch, push up between the bars, then land softly.
        if t<.30:
            u=t/.30;pz=.88+.27*u;az=.08+.24*u;ax=-.20+.30*u;pitch=12-7*u
        elif t<.68:
            u=(t-.30)/.38;pz=1.15-.28*u;az=.32-.08*u;ax=.10+.08*u;pitch=5+8*u
        else:
            u=(1-t)/.32;pz=.88+.27*u;az=.08+.24*u;ax=-.20+.30*u;pitch=12-7*u
        p=_bar_dip(pz,p1=pitch,p2=pitch,wx=0.0,ankle_rel=[.1,.02,-.7],knee=(1,0,0))
        p['ankle_l']=[ax,.10,az];p['ankle_r']=[ax,-.10,az]
        p.pop('ankle_l_rel',None);p.pop('ankle_r_rel',None)
        return p
    keys=dense(key,[(0,{'t':0}),(1,{'t':1})],48)
    return dict(seconds=3.0,keys=keys,cam_az=28,cam_el=12,props=bars)


def dip_straight_bar():
    bar=[dict(kind='bar',z=1.05,half=.65,r=.017,x=0)]
    def k(pz,p1,legs):
        return base_pose(pelvis=[0,0,pz],p1=p1,p2=p1,hp=0,
          **both(wrist=[-.08,.25,1.05],elbow=[-1,.2,0],hand=[1,0,0],palm=[0,0,-1],grip=.9,
                 ankle_rel=list(legs),knee=[1,0,0],foot=[1,0,0]))
    top=k(1.10,43,[.40,.02,-.72]);bottom=k(.72,25,[.40,.02,-.45])
    return dict(seconds=3,keys=rep(top,bottom),cam_az=28,cam_el=12,props=bar)


def dip_l_sit():
    bars=_bars()
    def k(t):
        pz=1.14-.27*t;p1=4+8*t
        p=_bar_dip(pz,p1=p1,p2=p1,wx=-.03,ankle_rel=[.87,.02,0],knee=(1,0,0))
        return p
    return dict(seconds=3,keys=dense(k,rep({'t':0},{'t':1}),48),cam_az=28,cam_el=12,props=bars)


def dip_forward_lean():
    bars=_bars()
    def k(t):
        pz=1.12-.27*t;lean=42
        return _bar_dip(pz,p1=lean,p2=lean,wx=-.06,ankle_rel=[-.02,.02,-.60],knee=(0,0,1))
    return dict(seconds=3,keys=dense(k,rep({'t':0},{'t':1}),48),cam_az=28,cam_el=12,props=bars)


def dip_russian():
    bars=_bars()
    # The elbows settle onto the bars, then the athlete rolls forward and presses back to straight-arm support.
    def k(t):
        def blend(a,b,u): return a+(b-a)*u
        def smooth(u): return u*u*(3-2*u)
        top=(1.136,14,-.025,[-1,.2,0],[.03,.07,-.84])
        forearm=(.65,45,-.20,[.5,.2,0],[.20,.02,-.40])
        press=(.94,55,.01,[-1,.2,0],[.10,.02,-.60])
        if t<.18: vals=top
        elif t<.48:
            u=smooth((t-.18)/.30)
            vals=(blend(top[0],forearm[0],u),blend(top[1],forearm[1],u),blend(top[2],forearm[2],u),
                  [blend(top[3][i],forearm[3][i],u) for i in range(3)],
                  [blend(top[4][i],forearm[4][i],u) for i in range(3)])
        elif t<.56: vals=forearm
        elif t<.82:
            u=smooth((t-.56)/.26)
            vals=(blend(forearm[0],press[0],u),blend(forearm[1],press[1],u),blend(forearm[2],press[2],u),
                  [blend(forearm[3][i],press[3][i],u) for i in range(3)],
                  [blend(forearm[4][i],press[4][i],u) for i in range(3)])
        else:
            u=smooth((t-.82)/.18)
            vals=(blend(press[0],top[0],u),blend(press[1],top[1],u),blend(press[2],top[2],u),
                  [blend(press[3][i],top[3][i],u) for i in range(3)],
                  [blend(press[4][i],top[4][i],u) for i in range(3)])
        pz,pitch,wx,ep,legs=vals
        return _bar_dip(pz,p1=pitch,p2=pitch,wx=wx,wz=1.04,ankle_rel=legs,elbow=ep)
    return dict(seconds=4,keys=dense(k,[(0,{'t':0}),(1,{'t':1})],64),
                interpolation='continuous',cam_az=28,cam_el=12,props=bars)


def dip_korean():
    bar=[dict(kind='bar',z=1.02,half=.65,r=.017,x=-.12)]
    def k(pz,pitch):
        return base_pose(pelvis=[.04,0,pz],p1=pitch,p2=pitch,hp=0,
            **both(wrist=[-.12,.25,1.02],elbow=[1,.2,0],hand=[1,0,0],palm=[0,0,1],grip=.9,
                   ankle_rel=[.40,.02,-.58],knee=[1,0,0],foot=[1,0,0]))
    return dict(seconds=3.5,keys=rep(k(1.08,2),k(.90,8)),cam_az=28,cam_el=12,props=bar)


def _frog(breath):
    # Knees rest on the upper arms; the bent-arm version holds a compact tuck.
    pz=.35+breath
    return base_pose(pelvis=[-.30,0,pz],p1=90,p2=90,hp=-8,
        **both(wrist=[.05,.20,.035],elbow=[-.3,.7,1],hand=[1,0,0],palm=[0,0,-1],grip=0,
               ankle_rel=[0,.02,-.20],knee=[1,1,-1],foot=[1,0,0]))


def planche_frog_stand():
    a=_frog(0);b=_frog(.001)
    # Deeply bent arms; hips high and knees folded around the upper arms.
    return dict(seconds=3,keys=rep(a,b),cam_az=28,cam_el=18,props=[])


def planche_frog_stand_straight_arm():
    def k(pz):
        return base_pose(pelvis=[-.173985,0,pz],p1=105,p2=105,hp=-15,
            **both(wrist=[0,.20,.035],elbow=[-1,.2,0],hand=[1,0,0],palm=[0,0,-1],grip=0,
                   ankle_rel=[0,.08,-.40],knee=[1,0,1],foot=[-1,0,0]))
    return dict(seconds=3,keys=rep(k(.648128),k(.649128)),cam_az=28,cam_el=18,props=[])


def planche_tuck_pushup():
    def k(t):
        pz=.48-.16*t
        return base_pose(pelvis=[-.25,0,pz],p1=90,p2=90,hp=0,
            **both(wrist=[-.14,.20,.035],elbow=[-.2,.7,1],hand=[1,0,0],palm=[0,0,-1],grip=0,
                   ankle_rel=[.18,.02,-.12],knee=[1,0,0],foot=[1,0,0]))
    return dict(seconds=3,keys=dense(k,rep({'t':0},{'t':1}),48),cam_az=28,cam_el=18,props=[])


def _planche_hold(straddle):
    def k(breath):
        return base_pose(pelvis=[-.035,0,.40+breath],p1=90,p2=90,hp=0,
            **both(wrist=[0,.20,.035],elbow=[-.2,.7,1],hand=[1,0,0],palm=[0,0,-1],grip=0,
                   ankle_rel=[-.8164 if straddle else -.8697,.30 if straddle else .005,0],knee=[0,0,1],foot=[1,0,0]))
    return dict(seconds=3,keys=rep(k(0),k(.002)),cam_az=28,cam_el=18,props=[])


def planche_straddle(): return _planche_hold(True)
def planche_full(): return _planche_hold(False)


EXERCISES={
 'dip-bench-bent-knee':dip_bench_bent_knee,
 'dip-bench-straight-leg':dip_bench_straight_leg,
 'dip-bench-feet-elevated':dip_bench_feet_elevated,
 'dip-feet-supported':dip_feet_supported,
 'dip-jumping':dip_jumping,
 'dip-straight-bar':dip_straight_bar,
 'dip-l-sit':dip_l_sit,
 'dip-forward-lean':dip_forward_lean,
 'dip-russian':dip_russian,
 'dip-korean':dip_korean,
 'planche-frog-stand':planche_frog_stand,
 'planche-frog-stand-straight-arm':planche_frog_stand_straight_arm,
 'planche-tuck-pushup':planche_tuck_pushup,
 'planche-straddle':planche_straddle,
 'planche-full':planche_full,
}
