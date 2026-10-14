"""New push-up progressions, using the proven body-line construction."""
import math
from poses import base_pose, both, rep, add, mul, dense, sample


def line(a, ankle_x=-1.1, ankle_y=0.08, ankle_z=0.08, wrist_x=-0.14, wrist_z=0.035,
         wy=0.27, knee=(0,0,-1), foot=(0.95,0,-0.3), elbows=(-0.6,0.7,1)):
    ar=math.radians(a); d=[math.cos(ar),0,math.sin(ar)]
    pelvis=add([ankle_x,0,ankle_z],mul(d,0.8998)); pitch=90-a
    return base_pose(pelvis=pelvis,p1=pitch,p2=pitch,hp=-8,
        **both(ankle=[ankle_x,ankle_y,ankle_z],foot=list(foot),knee=list(knee),
               wrist=[wrist_x,wy,wrist_z],elbow=list(elbows),hand=[1,0,0],palm=[0,0,-1],grip=0))

def line_rep(top, bottom, **opts):
    return dense(lambda a: line(a, **opts), rep({'a': top}, {'a': bottom}), 48)

def one_arm(a, wx=-.03, ankle_x=-.99, wide=True):
    ar=math.radians(a); d=[math.cos(ar),0,math.sin(ar)]; pitch=90-a
    pelvis=add([ankle_x,0,.08],mul(d,.8998)); yf=.11 if wide else .035
    return base_pose(pelvis=pelvis,p1=pitch,p2=pitch,hp=-8,
      ankle_l=[ankle_x,yf,.08],ankle_r=[ankle_x,-yf,.08],foot_l=[.95,0,-.3],foot_r=[.95,0,-.3],
      knee_l=[0,0,-1],knee_r=[0,0,-1],wrist_r=[wx,-.10,.035],elbow_r=[-.6,-.7,1],
      hand_r=[1,0,0],palm_r=[0,0,-1],wrist_l_rel=[-.25,.08,.04],elbow_l=[-.6,.7,1],
      hand_l=[0,0,-1],palm_l=[0,0,1],grip_l=0,grip_r=0)

def pushup_uneven():
    box=dict(kind='box',center=[-.13,.27,.095],size=[.28,.24,.12])
    def k(a):
        p=line(a,wrist_x=-.13)
        p['wrist_l']=[-.14,.27,.155]
        return p
    keys=dense(lambda a:k(a),rep({'a':17},{'a':4}),48)
    return dict(seconds=3,keys=keys,cam_az=28,cam_el=18,props=[box])

def pushup_half_one_arm():
    keys=dense(lambda a:one_arm(a,wx=.02),rep({'a':17},{'a':8}),48)
    return dict(seconds=3,keys=keys,cam_az=28,cam_el=18,props=[])

def pushup_one_arm_elevated():
    # A narrow counter edge: the hand stays at its front edge throughout the rep.
    box=dict(kind='box',center=[.60,-.1,.46],size=[.40,.72,.92])
    def k(a):
        p=one_arm(a,wx=.40,ankle_x=-1.0)
        p['wrist_r']=[.40,-.10,.92]
        return p
    keys=dense(lambda a:k(a),rep({'a':50.3},{'a':40}),48)
    return dict(seconds=3,keys=keys,cam_az=28,cam_el=18,props=[box])

def pushup_one_arm_straight_body():
    keys=dense(lambda a: one_arm(a,wide=False),rep({'a':17},{'a':4}),48)
    return dict(seconds=3,keys=keys,cam_az=28,cam_el=18,props=[])

def pushup_pseudo_planche():
    chair=dict(kind='chair',x=-1.10,seat=.90)
    def k(a,wx):
        p=line(a,ankle_x=-1.20,ankle_z=.98,wrist_x=wx,wy=.27,elbows=(-.4,.7,1))
        # feet elevated, hands shifted behind shoulder line, fingers turned outward
        p['ankle_l']=[-1.20,.08,.98];p['ankle_r']=[-1.20,-.08,.98]
        p['foot_l']=[1,0,0];p['foot_r']=[1,0,0]
        p['wrist_l']=[wx,.27,.035];p['wrist_r']=[wx,-.27,.035]
        p['hand_l']=[0,1,0];p['hand_r']=[0,-1,0]
        return p
    return dict(seconds=3.3,keys=rep(k(-19,-.16),k(-25,-.16)),cam_az=28,cam_el=18,props=[chair])

def pushup_single_leg():
    def k(a,u):
        p=line(a,wrist_x=-.13)
        p['ankle_r']=[-1.12+.02*u,-.08,.14+.04*u]
        p['wrist_l']=[-.13,.27,.035];p['wrist_r']=[-.13,-.27,.035]
        return p
    keys=dense(k,rep({'a':17,'u':0},{'a':4,'u':1}),48)
    return dict(seconds=3,keys=keys,cam_az=28,cam_el=18,props=[])

def pushup_hindu():
    def dog():
        return base_pose(pelvis=[-.60,0,.755],p1=160,p2=160,hp=-60,
            **both(ankle=[-1.1,.12,.08],foot=[1,0,-.3],knee=[0,0,1],wrist=[.05,.24,.035],elbow=[-.5,.7,1],hand=[1,0,0],palm=[0,0,-1],grip=0))
    def dive(u):
        # sweep pelvis toward the hands while the spine lowers from an inverted V to horizontal
        return base_pose(pelvis=[-.60+.28*u,0,.755-.42*u],p1=160-70*u,p2=160-70*u,hp=-60*(1-u),
            **both(ankle=[-1.1,.12,.08],foot=[1,0,-.3],knee=[0,0,1],wrist=[.05,.24,.035],elbow=[-.5,.7,1],hand=[1,0,0],palm=[0,0,-1],grip=0))
    def cobra(u):
        return base_pose(pelvis=[-.32+.04*u,0,.34],p1=90,p2=30+5*u,hp=0,
            **both(ankle=[-1.1,.12,.08],foot=[1,0,-.3],knee=[0,0,1],wrist=[.05,.24,.035],elbow=[-.5,.7,1],hand=[1,0,0],palm=[0,0,-1],grip=0))
    def key(t):
        if t<.25: return dog()
        if t<.50: return dive((t-.25)/.25)
        if t<.68: return cobra((t-.50)/.18)
        return dive(1-(t-.68)/.32)
    return dict(seconds=3.6,keys=dense(key,[(0,{'t':0}),(1,{'t':1})],48),cam_az=28,cam_el=18,props=[])

def pushup_pop_up_incline():
    box=dict(kind='box',center=[.35,0,.485],size=[.5,1.2,.97])
    def k(a,release=False):
        p=line(a,ankle_x=-1.1,wrist_x=.21,wrist_z=.97)
        if release: p['wrist_l']=[.28,.27,1.07];p['wrist_r']=[.28,-.27,1.07]
        return p
    return dict(seconds=2.4,keys=[(0,k(53)),(.18,k(33)),(.36,k(53,True)),(.46,k(53)),(.63,k(53)),(.80,k(53,True)),(.9,k(53)),(1,k(53))],cam_az=28,cam_el=18,props=[box])

def pushup_kneeling_push_off():
    mat=dict(kind='box',center=[-.1,0,.015],size=[1.4,.8,.03])
    def pose(a):
        ar=math.radians(a);d=[math.cos(ar),0,math.sin(ar)]
        pelvis=add([-.75,0,.06],mul(d,.47)); pitch=90-a
        wz=.035+.585*max(0,min(1,(a-20)/70))
        wx=-.20 if a<65 else -.48
        return base_pose(pelvis=pelvis,p1=pitch,p2=pitch,hp=-8,
          **both(ankle=[-1.18,.08,.07],foot=[-1,0,0],knee=[0,0,-1],wrist=[wx,.27,wz],elbow=[-.6,.7,1],hand=[1,0,0],palm=[0,0,-1],grip=0))
    def key(t):
        if t<.18: a=90
        elif t<.42: a=90-(t-.18)/.24*70
        elif t<.58: a=20
        elif t<.84: a=20+(t-.58)/.26*70
        else: a=90
        return pose(a)
    keyed=[(0,{'t':0}),(.18,{'t':.18}),(.42,{'t':.42}),(.58,{'t':.58}),(.84,{'t':.84}),(1,{'t':1})]
    return dict(seconds=2.4,keys=dense(key,keyed,48),cam_az=28,cam_el=18,props=[mat])

def _explosive(kind):
    def k(a,lift,width):
        p=line(a,ankle_x=-1.1,wrist_x=-.13,wy=width)
        for side,sgn in (('l',1),('r',-1)):
            start=[-.13,sgn*width,.035]
            if kind=='clap': target=[.03,sgn*.04,.32]
            elif kind=='chest': target=[-.12,sgn*.18,.38]
            else: target=[-.13,sgn*width,.18]
            p['wrist_'+side]=[start[i]+(target[i]-start[i])*lift for i in range(3)]
        return p
    close_width=.18 if kind=='wideclose' else .27
    events=[(0,{'a':17,'lift':0,'width':.27}),(.2,{'a':4,'lift':0,'width':close_width}),
            (.4,{'a':17,'lift':1,'width':.27}),(.52,{'a':17,'lift':0,'width':.27}),
            (.72,{'a':4,'lift':0,'width':close_width}),(.88,{'a':17,'lift':1,'width':.27}),
            (1,{'a':17,'lift':0,'width':.27})]
    keys=dense(k,events,48)
    return dict(seconds=2.4,keys=keys,cam_az=28,cam_el=18,props=[])

def pushup_pop_up(): return _explosive('pop')
def pushup_pop_up_wide_close(): return _explosive('wideclose')
def pushup_clap(): return _explosive('clap')
def pushup_chest_strike(): return _explosive('chest')

def pushup_wall_push_away():
    wall=dict(kind='box',center=[.125,0,1.2],size=[.05,1.6,2.4])
    def k(a):
        ar=math.radians(a);d=[math.cos(ar),0,math.sin(ar)];pelvis=add([-1.15,0,.08],mul(d,.898));pitch=90-a
        return base_pose(pelvis=pelvis,p1=pitch,p2=pitch,hp=-8,
          ankle_l=[-1.15,.10,.08],ankle_r=[-1.15,-.10,.08],foot_l=[.95,0,-.3],foot_r=[.95,0,-.3],knee_l=[0,0,-1],knee_r=[0,0,-1],
          wrist_r=[.10,-.27,1.25],elbow_r=[-.6,-.7,1],hand_r=[1,0,0],palm_r=[0,0,-1],grip_r=0,
          wrist_l_rel=[.12,.28,-.20],elbow_l=[-.6,.7,1],hand_l=[1,0,0],palm_l=[0,0,1],grip_l=0)
    top=k(60);bottom=k(44)
    return dict(seconds=2.7,keys=rep(top,bottom),cam_az=-25,cam_el=10,props=[wall])

EXERCISES={
 'pushup-uneven':pushup_uneven,'pushup-half-one-arm':pushup_half_one_arm,
 'pushup-one-arm-elevated':pushup_one_arm_elevated,'pushup-one-arm-straight-body':pushup_one_arm_straight_body,
 'pushup-pseudo-planche':pushup_pseudo_planche,'pushup-single-leg':pushup_single_leg,'pushup-hindu':pushup_hindu,
 'pushup-pop-up-incline':pushup_pop_up_incline,'pushup-kneeling-push-off':pushup_kneeling_push_off,
 'pushup-pop-up':pushup_pop_up,'pushup-pop-up-wide-close':pushup_pop_up_wide_close,
 'pushup-clap':pushup_clap,'pushup-chest-strike':pushup_chest_strike,'pushup-wall-push-away':pushup_wall_push_away,
}
