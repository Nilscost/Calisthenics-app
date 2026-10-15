"""Conditioning and locomotor drills added in R8."""
import math

from poses import base_pose, both


def _pushup_line(angle, knee_hint=(0, 0, -1)):
    """A genuine press line with the same planted feet and hand contacts throughout."""
    a = math.radians(angle)
    pelvis = [-.80 + .8998*math.cos(a), 0, .08 + .8998*math.sin(a)]
    return base_pose(pelvis=pelvis, p1=90-angle, p2=90-angle, hp=-8,
        ankle_l=[-.80,.10,.08], ankle_r=[-.80,-.10,.08],
        foot_l=[.95,0,-.3], foot_r=[.95,0,-.3], knee_l=list(knee_hint), knee_r=list(knee_hint),
        wrist_l=[.25,.23,.035], wrist_r=[.25,-.23,.035],
        elbow_l=[-.6,.7,1], elbow_r=[-.6,-.7,1],
        hand_l=[1,0,0], hand_r=[1,0,0], palm_l=[0,0,-1], palm_r=[0,0,-1],
        grip_l=0, grip_r=0)


def _running_cycle(t, mode):
    """A full airborne swing and landing for each alternating lead leg."""
    lead = 'r' if t < .5 else 'l'
    q = (t * 2) % 1.0
    swing = math.sin(math.pi*q)
    support_z = .08 + (.10*swing if mode in ('skip', 'power') else 0)
    lead_z = .08 + swing * ({'butt': .74, 'high': .50, 'skip': .20, 'power': .58}[mode])
    pelvis_z = .945 + swing * ({'butt': .015, 'high': .025, 'skip': .07, 'power': .12}[mode])
    forward = .08*swing if mode == 'power' else 0.0
    al = [forward, .10, support_z]
    ar = [forward, -.10, support_z]
    kl = kr = [1,0,0]
    fl = fr = [1,0,0]
    if lead == 'r':
        ar = [forward-.25 if mode=='butt' else forward+.10*swing, -.10, lead_z]
        fr = [1,0,-.5*swing] if mode in ('butt','high','power') else [1,0,0]
        kr = [1,0,0]
        arm_l = [.38,.12,.24] if mode == 'power' else [.28,.12,.22 if mode == 'high' else -.10]
        arm_r = [-.20,-.12,-.16] if mode == 'power' else [.25,-.12,-.24]
    else:
        al = [forward-.25 if mode=='butt' else forward+.10*swing, .10, lead_z]
        fl = [1,0,-.5*swing] if mode in ('butt','high','power') else [1,0,0]
        kl = [1,0,0]
        arm_l = [-.20,.12,-.16] if mode == 'power' else [.25,.12,-.24]
        arm_r = [.38,-.12,.24] if mode == 'power' else [.28,-.12,.22 if mode == 'high' else -.10]
    return base_pose(pelvis=[0,0,pelvis_z],p1=3,p2=4,hp=-2,
        ankle_l=al,ankle_r=ar,foot_l=fl,foot_r=fr,knee_l=kl,knee_r=kr,
        wrist_l_rel=arm_l,wrist_r_rel=arm_r,
        elbow_l=[-1,.3,0],elbow_r=[-1,-.3,0],hand_l=[1,0,0],hand_r=[1,0,0])


def squat_thrust_step_back():
    stand,crouch,air,top=_thrust_stages()
    lower=_conditioning_stage([-.15,0,.72],40,[[0,-.10,.08],[0,.10,.08]],[[.25,-.23,.75],[.25,.23,.75]],hp=-4)
    hands=[[.25,-.23,.035],[.25,.23,.035]]
    r_swing=_conditioning_stage([-.25,0,.50],80,[[-.40,-.10,.24],[0,.10,.08]],hands,hp=-8)
    r_plant=_conditioning_stage([-.20,0,.45],75,[[-.80,-.10,.08],[0,.10,.08]],hands,hp=-8)
    l_swing=_conditioning_stage([-.05,0,.42],72,[[-.80,-.10,.08],[-.40,.10,.24]],hands,hp=-8)
    return dict(seconds=5.5,keys=[(0,stand),(.05,lower),(.10,crouch),(.18,r_swing),(.26,r_plant),(.34,l_swing),(.42,top),
      (.50,top),(.58,l_swing),(.66,r_plant),(.74,r_swing),(.82,crouch),(.89,lower),(.96,stand)],cam_az=30,cam_el=20,props=[])


def _jump_thrust(sprawl=False,pushup=False,clap=False):
    stand,crouch,air,top=_thrust_stages()
    lower=_conditioning_stage([-.15,0,.72],40,[[0,-.10,.08],[0,.10,.08]],[[.25,-.23,.75],[.25,.23,.75]],hp=-4)
    keys=[(0,stand),(.07,lower),(.14,crouch),(.20,air),(.26,top)]
    if pushup:
        bottom=_pushup_line(8,(0,0,1))
        keys.extend([(.34,bottom),(.40,bottom),(.48,top)])
    elif sprawl:
        sunk=_conditioning_stage([.09,0,.18],90,[[-.80,-.10,.08],[-.80,.10,.08]],[[.25,-.23,.035],[.25,.23,.035]])
        sunk['p2']=55
        keys.extend([(.34,sunk),(.42,sunk),(.48,top)])
    else:
        keys.append((.48,top))
    keys.extend([(.54,air),(.60,crouch),(.67,lower),(.74,stand)])
    if clap or pushup:
        launch=dict(stand)
        apex=_conditioning_stage([0,0,1.155],0,[[0,-.10,.30],[0,.10,.30]],[[.15,-.20,1.25],[.15,.20,1.25]],knee=(1,0,0))
        landing=_conditioning_stage([0,0,.82],8,[[0,-.10,.08],[0,.10,.08]],[[.18,-.20,1.05],[.18,.20,1.05]],knee=(1,0,0))
        if clap:
            launch=dict(stand,wrist_r=[0,-.02,1.985],wrist_l=[0,.02,1.985])
            apex.update(wrist_r=[0,-.02,2.165],wrist_l=[0,.02,2.165])
        keys.extend([(.80,launch),(.86,apex),(.92,landing)])
    keys.append((.98,stand))
    return dict(seconds=6 if pushup else 5,keys=keys,cam_az=30,cam_el=20,props=[])


def squat_thrust(): return _jump_thrust()
def sprawl(): return _jump_thrust(sprawl=True)
def burpee(): return _jump_thrust(clap=True)
def burpee_pushup(): return _jump_thrust(pushup=True)


def eight_count_t_pushup():
    stand,crouch,air,top=_thrust_stages()
    lower=_conditioning_stage([-.15,0,.72],40,[[0,-.10,.08],[0,.10,.08]],[[.25,-.23,.75],[.25,.23,.75]],hp=-4)
    prone=_conditioning_stage([.10,0,.11],90,[[-.80,-.10,.08],[-.80,.10,.08]],[[.25,-.23,.035],[.25,.23,.035]],knee=(0,0,-1))
    lifted=dict(prone,wrist_r=[.57,-.746,.16],wrist_l=[.57,.746,.16],hand_r=[0,-1,0],hand_l=[0,1,0])
    return dict(seconds=8,keys=[(0,stand),(.05,lower),(.10,crouch),(.16,air),(.22,top),(.34,prone),
      (.42,lifted),(.52,lifted),(.60,prone),(.72,top),(.80,air),(.86,crouch),(.915,lower),(.97,stand)],cam_az=30,cam_el=22,props=[])


def _mountain(kind):
    keys=[]
    for n in range(121):
        t=n/120;w=math.sin(2*math.pi*t)
        drives=(max(0,w),max(0,-w)) if kind=='slow' else ((1+w)/2,(1-w)/2)
        p=_pushup_line(20,(0,0,1))
        for side,sgn,u in (('r',-1,drives[0]),('l',1,drives[1])):
            y=sgn*.10-sgn*.33*u if kind=='cross' else sgn*.10
            p['ankle_'+side]=[-.80+.90*u,y,.08+.10*math.sin(math.pi*u)]
            p['foot_'+side]=[1,0,0]
            p['knee_'+side]=[1,-sgn*.5 if kind=='cross' else sgn*.15,1]
        keys.append((t,p))
    return dict(seconds=4 if kind=='slow' else 2.4,keys=keys,cam_az=35 if kind=='cross' else 30,cam_el=22,props=[])


def mountain_climber_slow(): return _mountain('slow')
def mountain_climber(): return _mountain('run')
def mountain_climber_cross_body(): return _mountain('cross')


def _jack(style):
    def key(t):
        u=(1-math.cos(2*math.pi*t))/2
        wide=.10+.10*u
        z=.08+.10*math.sin(2*math.pi*t)**2
        p=base_pose(pelvis=[0,0,.975+.03*math.sin(2*math.pi*t)],
            ankle_l=[0,wide,z],ankle_r=[0,-wide,z],foot_l=[1,0,0],foot_r=[1,0,0],
            knee_l=[1,0,0],knee_r=[1,0,0])
        if style=='half':
            th=math.radians(90*u)
            p['wrist_l_rel']=[0,.568*math.sin(th),-.568*math.cos(th)]
            p['wrist_r_rel']=[0,-.568*math.sin(th),-.568*math.cos(th)]
            p['elbow_l']=[-1,.3,0];p['elbow_r']=[-1,-.3,0]
        elif style=='overhead':
            th=math.radians(170*u)
            p['wrist_l_rel']=[0,.568*math.sin(th),-.568*math.cos(th)]
            p['wrist_r_rel']=[0,-.568*math.sin(th),-.568*math.cos(th)]
            p['elbow_l']=[-1,.25,0];p['elbow_r']=[-1,-.25,0]
        elif style=='seal':
            phi=math.radians(-16.4+106.4*u)
            p['wrist_l_rel']=[.568*math.cos(phi),.568*math.sin(phi),0]
            p['wrist_r_rel']=[.568*math.cos(phi),-.568*math.sin(phi),0]
            p['elbow_l']=[-1,.3,0];p['elbow_r']=[-1,-.3,0]
        else:
            spread=u
            p['wrist_l_rel']=[.02+spread*.23,.04+spread*.22,-.02+spread*.20]
            p['wrist_r_rel']=[.02+spread*.23,-.04-spread*.22,-.02+spread*.20]
            p['elbow_l']=[-1,.3,0];p['elbow_r']=[-1,-.3,0]
        return p
    return dict(seconds=2.6,keys=[(i/48,key(i/48)) for i in range(48)],cam_az=30,cam_el=12,props=[])


def half_jack(): return _jack('half')
def jumping_jack(): return _jack('overhead')
def seal_jack(): return _jack('seal')
def star_jack():
    def key(t):
        open_=math.sin(math.pi*t)**2
        th=math.radians(135*open_)
        p=base_pose(pelvis=[0,0,.975+.10*open_],p1=4,p2=4,
            ankle_l=[0,.10+.20*open_,.08+.15*open_],ankle_r=[0,-.10-.20*open_,.08+.15*open_],
            foot_l=[1,0,0],foot_r=[1,0,0],knee_l=[1,.2,0],knee_r=[1,-.2,0],
            wrist_l_rel=[0,.568*math.sin(th),-.568*math.cos(th)],
            wrist_r_rel=[0,-.568*math.sin(th),-.568*math.cos(th)],
            elbow_l=[-1,.3,0],elbow_r=[-1,-.3,0])
        return p
    return dict(seconds=2.7,keys=[(i/48,key(i/48)) for i in range(48)],cam_az=30,cam_el=12,props=[])


def butt_kicks():
    def key(t): return _running_cycle(t,'butt')
    return dict(seconds=2.4,keys=[(i/48,key(i/48)) for i in range(48)],cam_az=25,cam_el=10,props=[])


def high_knees():
    def key(t): return _running_cycle(t,'high')
    return dict(seconds=2.6,keys=[(i/48,key(i/48)) for i in range(48)],cam_az=25,cam_el=10,props=[])


def skipping(): return _forward_skip('skip')


def side_skip():
    # Two hop steps out and two back, with the upper body facing forward throughout.
    keys=[]
    for i in range(161):
        t=i/160;phase=2*t
        p=_running_cycle(phase%1,'skip')
        hop=min(3,int(t*4));q=min(1,t*4-hop);s=q*q*(3-2*q)
        y=.40*(hop+s) if hop<2 else .80-.40*(hop-2+s)
        p['pelvis'][0]=0;p['pelvis'][1]=y
        for side,sgn in (('r',-1),('l',1)):
            p['ankle_'+side][0]=0;p['ankle_'+side][1]=y+sgn*.10
            p['wrist_'+side+'_rel']=[.15,sgn*.10+.22*math.sin(4*math.pi*t),-.22+.20*math.cos(4*math.pi*t)]
        keys.append((t,p))
    return dict(seconds=5,keys=keys,cam_az=35,cam_el=14,props=[])


def power_skip(): return _forward_skip('power')


def _rope(double=False):
    # The curve itself is not a supported prop; wrist rotation and a raised hop convey the drill.
    def key(t):
        jump=math.sin(2*math.pi*t)**2
        phase=4*math.pi*t*(2 if double else 1)
        h=.26 if double else .12
        return base_pose(pelvis=[0,0,.975+h*jump],
            ankle_l=[0,.10,.08+h*jump],ankle_r=[0,-.10,.08+h*jump],
            foot_l=[1,0,-.15],foot_r=[1,0,-.15],knee_l=[1,0,0],knee_r=[1,0,0],
            wrist_l_rel=[.16+.025*math.cos(phase),.25,-.30+.025*math.sin(phase)],wrist_r_rel=[.16+.025*math.cos(phase),-.25,-.30+.025*math.sin(phase)],
            elbow_l=[-1,.3,0],elbow_r=[-1,-.3,0],hand_l=[1,0,0],hand_r=[1,0,0],grip_l=.9,grip_r=.9)
    return dict(seconds=2.4 if not double else 2.2,keys=[(i/48,key(i/48)) for i in range(48)],cam_az=30,cam_el=12,props=[])


def jump_rope(): return _rope(False)
def jump_rope_double_under(): return _rope(True)


def _world_wrist_keys(exercise):
    """Keep one wrist representation through the whole clip; convert offsets to world points."""
    spec = exercise()
    out = []
    for t, original in spec['keys']:
        p = dict(original)
        pitch = math.radians((p.get('p1', 0) + p.get('p2', 0)) * .5)
        for s, sign in (('l', 1), ('r', -1)):
            rel_name, world_name = 'wrist_' + s + '_rel', 'wrist_' + s
            if rel_name in p:
                rel = p.pop(rel_name)
                p1 = math.radians(p.get('p1', 0))
                p2 = math.radians(p.get('p2', 0))
                yaw = math.radians(p.get('yaw', 0))
                shoulder = [p['pelvis'][0] + .24*math.sin(p1) + .23*math.sin(p2) - sign*.18*math.sin(yaw),
                            p['pelvis'][1] + sign*.18*math.cos(yaw),
                            p['pelvis'][2] + .24*math.cos(p1) + .23*math.cos(p2)]
                p[world_name] = [shoulder[i] + rel[i] for i in range(3)]
        out.append((t, p))
    spec['keys'] = out
    return spec


def _conditioning_stage(P,p,feet,hands,*,hp=0,knee=(0,0,1)):
    return base_pose(pelvis=P,p1=p,p2=p,hp=hp,
      ankle_r=feet[0],ankle_l=feet[1],foot_r=[1,0,0],foot_l=[1,0,0],knee_r=list(knee),knee_l=list(knee),
      wrist_r=hands[0],wrist_l=hands[1],elbow_r=[0,-1,1],elbow_l=[0,1,1],hand_r=[1,0,0],hand_l=[1,0,0],
      palm_r=[0,0,-1],palm_l=[0,0,-1],grip_r=0,grip_l=0)

def _thrust_stages():
    stand=_conditioning_stage([0,0,.975],0,[[0,-.10,.08],[0,.10,.08]],[[.20,-.23,1.02],[.20,.23,1.02]],knee=(1,0,0))
    crouch=_conditioning_stage([-.30,0,.50],80,[[0,-.10,.08],[0,.10,.08]],[[.25,-.23,.035],[.25,.23,.035]],hp=-8)
    air=_conditioning_stage([-.10,0,.45],75,[[-.40,-.10,.22],[-.40,.10,.22]],[[.25,-.23,.035],[.25,.23,.035]],hp=-8)
    top=_pushup_line(20,(0,0,1))
    return stand,crouch,air,top


def _forward_skip(mode):
    keys=[]
    for i in range(81):
        phase=i/80
        p=_running_cycle(min(phase,1-1e-8),mode)
        hop=min(1,int(phase*2));q=min(1,phase*2-hop)
        x=.40*(hop+q*q*(3-2*q))
        p['pelvis'][0]+=x;p['ankle_r'][0]+=x;p['ankle_l'][0]+=x
        keys.append((.65*phase,p))
    r=l=.80
    for n in range(4):
        side=n%2;target=.40 if n<2 else 0
        before=[r,l];after=before[:];after[side]=target
        P=[sum(before+after)/4,0,.80]
        feet=[[r,-.1,.08],[l,.1,.08]];feet[side]=[target,(-.1 if side==0 else .1),.22]
        def walk(P,feet):
            return base_pose(pelvis=P,ankle_r=feet[0],ankle_l=feet[1],foot_r=[1,0,0],foot_l=[1,0,0],
              knee_r=[1,0,0],knee_l=[1,0,0],wrist_r_rel=[.15,-.12,-.30],wrist_l_rel=[.15,.12,-.30],
              elbow_r=[0,-1,0],elbow_l=[0,1,0],hand_r=[1,0,0],hand_l=[1,0,0])
        t=.68+n*.07;keys.append((t,walk(P,feet)))
        r,l=after;feet=[[r,-.1,.08],[l,.1,.08]];keys.append((t+.04,walk([(r+l)/2,0,.80],feet)))
    keys.append((.98,_running_cycle(0,mode)))
    return dict(seconds=5,keys=keys,cam_az=28,cam_el=14,props=[])

EXERCISES = {
    'squat-thrust-step-back': squat_thrust_step_back,
    'squat-thrust': squat_thrust,
    'sprawl': sprawl,
    'burpee': burpee,
    'burpee-pushup': burpee_pushup,
    'eight-count-t-pushup': eight_count_t_pushup,
    'mountain-climber-slow': mountain_climber_slow,
    'mountain-climber': mountain_climber,
    'mountain-climber-cross-body': mountain_climber_cross_body,
    'half-jack': half_jack,
    'jumping-jack': jumping_jack,
    'seal-jack': seal_jack,
    'star-jack': star_jack,
    'butt-kicks': butt_kicks,
    'high-knees': high_knees,
    'skipping': skipping,
    'side-skip': side_skip,
    'power-skip': power_skip,
    'jump-rope': jump_rope,
    'jump-rope-double-under': jump_rope_double_under,
}

EXERCISES = {exercise_id: (lambda fn=fn: _world_wrist_keys(fn))
             for exercise_id, fn in EXERCISES.items()}
