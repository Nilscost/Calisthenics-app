"""Army physical-readiness drill movements, authored from docs/23-new-exercises.json."""
import math

from poses import sample, solve, dense, base_pose, trunk_frame


def _key(pelvis=(0, 0, .955), p1=0, p2=0, hp=0, yaw=0, roll=0,
         feet=None, knees=None, arms=None, wrists=None, elbows=None, hands=None, footdirs=None):
    feet = feet or ([0, .12, .08], [0, -.12, .08])
    knees = knees or ([1, 0, 0], [1, 0, 0])
    arms = arms or ([.02, .04, -.565], [.02, -.04, -.565])
    wrists = wrists or (None, None)
    elbows = elbows or ([-1, .2, 0], [-1, -.2, 0])
    hands = hands or ([0, 0, -1], [0, 0, -1])
    footdirs = footdirs or ([1, 0, 0], [1, 0, 0])
    # Convert arm offsets to world targets so every key uses one wrist field type.
    def shoulder(side):
        u1, _ = trunk_frame(p1, yaw, roll)
        u2, left = trunk_frame(p2, yaw, roll)
        chest=[pelvis[i]+.24*u1[i]+.26*u2[i] for i in range(3)]
        return [chest[i]+side*.18*left[i]-.03*u2[i] for i in range(3)]
    wr_l = list(wrists[0]) if wrists[0] is not None else [shoulder(1)[i]+arms[0][i] for i in range(3)]
    wr_r = list(wrists[1]) if wrists[1] is not None else [shoulder(-1)[i]+arms[1][i] for i in range(3)]
    d = dict(pelvis=list(pelvis), p1=p1, p2=p2, hp=hp, yaw=yaw, roll=roll,
             ankle_l=list(feet[0]), ankle_r=list(feet[1]),
             knee_l=list(knees[0]), knee_r=list(knees[1]),
             foot_l=list(footdirs[0]), foot_r=list(footdirs[1]),
             elbow_l=list(elbows[0]), elbow_r=list(elbows[1]),
             hand_l=list(hands[0]), hand_r=list(hands[1]), wrist_l=wr_l, wrist_r=wr_r)
    return base_pose(**d)


def _keys(seq):
    """Chronological choreographic beats; loop closures use a deliberate recovery step."""
    return [(float(t), p) for t, p in seq]


def bend_and_reach():
    def key(q):
        p=_key((-.18*q,0,.955-.195*q),60*q,90*q,18*q)
        sh=_army_shoulders(p)
        for side,sg in [('l',1),('r',-1)]:
            # Rotate from overhead to a straight backward reach between thighs.
            a=math.radians(233.13*q)
            dx=math.sin(a);dz=math.cos(a)
            dy=-sg*.13*q
            r=math.sqrt(.568**2-dy**2)
            p['wrist_'+side]=[sh[side][0]+r*dx,sh[side][1]+dy,sh[side][2]+r*dz]
            p['hand_'+side]=[dx,0,dz]
            p['elbow_'+side]=[0,sg,0]
        return p
    return dict(seconds=4,keys=dense(key,[(0,{'q':0}),(.12,{'q':0}),(.42,{'q':1}),(.55,{'q':1}),(.94,{'q':0})],steps=64),cam_az=30,cam_el=12,props=[])


def high_jumper():
    feet=([0,.12,.08],[0,-.12,.08])
    back_z=-math.sqrt(.568**2-.50**2-.07**2)
    back=([-.50,.07,back_z],[-.50,-.07,back_z]); drive=([.34,.20,.37],[.34,-.20,.37]); over_z=math.sqrt(.568**2-.12**2); overhead=([0,.12,over_z],[0,-.12,over_z])
    crouch=_army_hop((0,0,.82),feet,p1=20,p2=23,arm_dirs=back)
    small_push=_army_hop((0,0,.955),feet,p1=8,p2=10,arm_dirs=drive)
    small_air=_army_hop((0,0,1.055),([0,.12,.18],[0,-.12,.18]),p1=2,p2=3,arm_dirs=drive)
    soft=_army_hop((0,0,.91),([0,.12,.10],[0,-.12,.10]),p1=15,p2=18,arm_dirs=drive)
    big_push=_army_hop((0,0,.955),feet,p1=7,p2=8,arm_dirs=overhead)
    big_air=_army_hop((0,0,1.13),([0,.12,.255],[0,-.12,.255]),p1=0,p2=1,arm_dirs=overhead)
    big_land=_army_hop((0,0,.89),([0,.12,.09],[0,-.12,.09]),p1=18,p2=20,arm_dirs=overhead)
    recover=_army_hop((0,0,.955),feet)
    return dict(seconds=3.8,keys=_army_hop_keys([(0,crouch),(.10,crouch),(.20,small_push),(.25,small_air),(.31,soft),(.40,crouch),(.52,big_push),(.60,big_air),(.68,big_land),(.78,recover),(.92,crouch)]),cam_az=30,cam_el=12,props=[])


def squat_bender():
    def key(q,mode):
        if mode=='squat':p=_key((-.10*q,0,.955-.405*q),30*q,32*q)
        else:p=_key((-.20*q,0,.955-.03*q),75*q,75*q)
        sh=_army_shoulders(p)
        for side,sg in [('l',1),('r',-1)]:
            hiphand=[0,sg*.18,1.04]
            target=[sh[side][0]+(.568 if mode=='squat' else 0),sh[side][1],sh[side][2]-(0 if mode=='squat' else .568)]
            p['wrist_'+side]=[hiphand[i]*(1-q)+target[i]*q for i in range(3)]
            p['hand_'+side]=[q if mode=='squat' else 0,0,-1 if mode!='squat' else -(1-q)]
        return p
    def at(t):
        if t<.10:q=0;mode='squat'
        elif t<.30:q=(t-.10)/.20;mode='squat'
        elif t<.40:q=1;mode='squat'
        elif t<.52:q=1-(t-.40)/.12;mode='squat'
        elif t<.73:q=(t-.52)/.21;mode='hinge'
        elif t<.82:q=1;mode='hinge'
        elif t<.96:q=1-(t-.82)/.14;mode='hinge'
        else:q=0;mode='hinge'
        q=.5-.5*math.cos(math.pi*q)
        p=key(q,mode);sh=_army_shoulders(p)
        for side in ('l','r'):
            d=[p['wrist_'+side][i]-sh[side][i] for i in range(3)]
            norm=math.sqrt(sum(x*x for x in d))
            if norm>.568:p['wrist_'+side]=[sh[side][i]+.568*d[i]/norm for i in range(3)]
        return p
    return dict(seconds=5,keys=[(i/160,at(i/160)) for i in range(160)],cam_az=30,cam_el=10,props=[])


def windmill_army():
    def key(q,s):
        p=_key((-.24*q,0,.925-.29*q),70*q,90*q,0,roll=s*45*q,
            feet=([0,.30,.08],[0,-.30,.08]))
        sh=_army_shoulders(p);low='r' if s>0 else 'l';high='l' if s>0 else 'r'
        # At the end, the low fingertips meet the outside of the opposite foot.
        C=[0,s*.36,.08];v=[C[i]-sh[low][i] for i in range(3)]
        length=math.sqrt(sum(x*x for x in v));v=[x/length for x in v]
        for side,sg in [('l',1),('r',-1)]:
            d=[(v[i] if side==low else -v[i])*q + (sg if i==1 else 0)*(1-q) for i in range(3)]
            norm=math.sqrt(sum(x*x for x in d));d=[x/norm for x in d]
            p['wrist_'+side]=[sh[side][i]+.568*d[i] for i in range(3)]
            p['hand_'+side]=d;p['elbow_'+side]=[1,0,0]
        return p
    keys=[]
    for i in range(161):
        t=i/160;s=1 if t<.5 else -1;u=(t if t<.5 else t-.5)*2
        q=math.sin(math.pi*u)**2
        keys.append((t,key(q,s)))
    return dict(seconds=6,keys=keys,cam_az=35,cam_el=18,props=[])


def prone_row_army():
    pelvis = [0.0, 0.0, .12]

    def key(pull):
        p1, p2 = (90.0, 80.0) if pull else (90.0, 90.0)
        sh_l = _army_ref_shoulder(pelvis, p1, p2, 1)
        sh_r = _army_ref_shoulder(pelvis, p1, p2, -1)
        if pull:
            # Short pull to the shoulders, with the fists raised and elbows sweeping back.
            ez=math.sqrt(.30**2-.24**2-.17**2)
            fz=math.sqrt(.27**2-.24**2-.10**2)
            wrists=([sh_l[0],sh_l[1]+.07,sh_l[2]+ez+fz],
                    [sh_r[0],sh_r[1]-.07,sh_r[2]+ez+fz])
            elbows=([-.24,.17,ez],[-.24,-.17,ez])
            grips = (.95, .95)
        else:
            # Reach along the full .568 m arm radius; hands stay 20 cm above the floor.
            dz = .20 - sh_l[2]
            dx = math.sqrt(.568 ** 2 - dz ** 2)
            wrists = ([sh_l[0] + dx, sh_l[1], .20],
                      [sh_r[0] + dx, sh_r[1], .20])
            elbows = ([0, .2, 1], [0, -.2, 1])
            grips = (.15, .15)
        return _army_ref_pose(pelvis, p1, p2,
                              ([-.8993, .09, .12], [-.8993, -.09, .12]),
                              ([1, 0, 0], [1, 0, 0]), wrists,
                              ([1, 0, 0], [1, 0, 0]), elbows,
                              knees=([0, 1, 0], [0, -1, 0]), grips=grips)

    reach, pull = key(False), key(True)
    return dict(seconds=3.8,
                keys=[(0.0, reach), (.12, reach), (.43, pull), (.55, pull), (.88, reach)],
                cam_az=30, cam_el=24, props=[])


def turn_and_lunge():
    def ease(u): return .5 - .5 * math.cos(math.pi * max(0, min(1, u)))

    def key(t):
        side = 1 if t < .5 else -1
        u = 2 * (t if t < .5 else t - .5)
        if u < .30:
            # Widen the stance as the hips lower so neither leg has to stretch
            # past its reach during the turn-in transition.
            step = ease(u/.30)
            sink = step
            reach = 0.0
        elif u < .40:
            step, sink, reach = 1.0, 1.0, ease((u-.30)/.10)
        elif u < .52:
            step, sink, reach = 1.0, 1.0, 1.0
        elif u < .63:
            step, sink, reach = 1.0, 1.0, 1.0-ease((u-.52)/.11)
        elif u < .82:
            step, sink, reach = 1.0, 1.0, 0.0
        else:
            step, sink, reach = 1.0-ease((u-.82)/.18), 1.0-ease((u-.82)/.18), 0.0

        # Turn toward the working side, step into a stable split stance, then
        # hinge/reach from that stance. Alternate the lead leg for side two.
        yaw = -side * 90.0 * step
        a = math.radians(yaw)
        d = [math.cos(a), math.sin(a), 0.0]
        left = [-math.sin(a), math.cos(a), 0.0]
        lead_side = 'r' if side > 0 else 'l'
        rear_side = 'l' if lead_side == 'r' else 'r'
        lateral = -1.0 if lead_side == 'r' else 1.0
        front = [.54*d[i] + lateral*.08*left[i] for i in range(3)]
        rear = [-.54*d[i] - lateral*.08*left[i] for i in range(3)]
        neutral = {'l':[0,.12,.08], 'r':[0,-.12,.08]}
        targets = {lead_side:[front[0],front[1],.08], rear_side:[rear[0],rear[1],.08]}
        ankles = {s:[neutral[s][i]*(1-step)+targets[s][i]*step for i in range(3)] for s in ('l','r')}
        flat = [1.0,0.0,0.0]
        turnfoot = [d[0],d[1],0.0]
        footdirs = {s:[flat[i]*(1-step)+turnfoot[i]*step for i in range(3)] for s in ('l','r')}
        knee_dirs = {lead_side:d, rear_side:d}

        # Hinge over a stable one-metre split stance. The torso stays above
        # the hips while the opposite hand reaches down between the feet.
        pz = .955 - .435*sink
        p1 = 90.0*sink
        p2 = 90.0*sink
        pelvis = [.14*d[0]*step, .14*d[1]*step, pz]
        p = _key(pelvis, p1, p2, 0, yaw=yaw,
                 feet=(ankles['l'],ankles['r']), footdirs=(footdirs['l'],footdirs['r']),
                 knees=(knee_dirs['l'],knee_dirs['r']))
        sh = _army_shoulders(p)
        reach_side = rear_side
        for s in ('l','r'):
            # One arm hangs naturally while the opposite palm moves to the
            # floor in the middle of the front/rear foot span.
            sign = 1 if s=='l' else -1
            rest = [sh[s][0]+.02*d[0], sh[s][1]+sign*.18*left[1], sh[s][2]-.28]
            target = [.38*d[0], .38*d[1], .035]
            p['wrist_'+s] = [rest[i]*(1-reach)+target[i]*reach if s==reach_side else rest[i] for i in range(3)]
            p['hand_'+s] = d
            p['palm_'+s] = [0,0,-1]
            p['elbow_'+s] = [-d[1],d[0],0]
        # The first key remains the clearly readable standing start; there is
        # no kneeling/all-fours transition in either half of the loop.
        return p

    return dict(seconds=8,keys=[(i/256,key(i/256)) for i in range(256)],cam_az=90,cam_el=15,props=[])


def y_squat():
    feet = ([0.0, .20, .08], [0.0, -.20, .08])
    footdirs = ([1, 0, 0], [1, 0, 0])

    def key(pz):
        pelvis = [0.0, 0.0, pz]
        sh_l = _army_ref_shoulder(pelvis, 0, 0, 1)
        sh_r = _army_ref_shoulder(pelvis, 0, 0, -1)
        # Straight arms form a Y, each exactly .568 m from shoulder to wrist.
        angle = math.radians(35)
        dy = .568 * math.sin(angle)
        dz = .568 * math.cos(angle)
        wrists = ([sh_l[0], sh_l[1] + dy, sh_l[2] + dz],
                  [sh_r[0], sh_r[1] - dy, sh_r[2] + dz])
        arms = ([0, math.sin(angle), math.cos(angle)],
                [0, -math.sin(angle), math.cos(angle)])
        return _army_ref_pose(pelvis, 0, 0, feet, footdirs, wrists, arms,
                              ([0, 0, 1], [0, 0, 1]),
                              knees=([1, 0, 0], [1, 0, 0]))

    top, bottom = key(.9715), key(.57)
    return dict(seconds=3.6,
                keys=[(0.0, top), (.12, top), (.44, bottom), (.55, bottom), (.90, top)],
                cam_az=30, cam_el=10, props=[])


def front_kick_toe_touch():
    pelvis_x = 0.0
    ankle_radius = .8695
    q_steps = 24

    def key(sign, q):
        # The lower and upper spine segment turns together into a forward hip hinge.
        p1, p2 = 20.0 * q, 45.0 * q
        pelvis = [pelvis_x, 0.0, .973]
        up1, left1 = trunk_frame(p1, 0, 0)
        hip_l = [pelvis[i] - .03 * up1[i] + .09 * left1[i] for i in range(3)]
        hip_r = [pelvis[i] - .03 * up1[i] - .09 * left1[i] for i in range(3)]
        theta = math.radians(90.0 * q)
        legvec = [math.sin(theta), 0.0, -math.cos(theta)]
        footdir = [math.cos(theta), 0.0, math.sin(theta)]
        if sign > 0:
            working_hip, working_y = hip_l, .09
            support = [0.0, -.09, .0755]
            ankle = [working_hip[i] + (.8675+.002*q) * legvec[i] for i in range(3)]
            ankle[1] = working_y
            toe = [ankle[i] + .20 * footdir[i] for i in range(3)]
            sh_l = _army_ref_shoulder(pelvis, p1, p2, 1)
            sh_r = _army_ref_shoulder(pelvis, p1, p2, -1)
            contact_wrist = [toe[0] - .17, toe[1], toe[2]]
            free_wrist = [sh_l[0] + .50, sh_l[1], sh_l[2]]
            blend = q ** 3
            start_wrist = [sh_r[0] + .50, sh_r[1], sh_r[2]]
            wrist_r = [(1 - blend) * start_wrist[i] + blend * contact_wrist[i]
                       for i in range(3)]
            ankles = (ankle, support)
            feet = (footdir, [1, 0, 0])
            wrists = (free_wrist, wrist_r)
            knees = ([0, 1, 0], [0, 1, 0])
        else:
            working_hip, working_y = hip_r, -.09
            support = [0.0, .09, .0755]
            ankle = [working_hip[i] + (.8675+.002*q) * legvec[i] for i in range(3)]
            ankle[1] = working_y
            toe = [ankle[i] + .20 * footdir[i] for i in range(3)]
            sh_l = _army_ref_shoulder(pelvis, p1, p2, 1)
            sh_r = _army_ref_shoulder(pelvis, p1, p2, -1)
            contact_wrist = [toe[0] - .17, toe[1], toe[2]]
            free_wrist = [sh_r[0] + .50, sh_r[1], sh_r[2]]
            blend = q ** 3
            start_wrist = [sh_l[0] + .50, sh_l[1], sh_l[2]]
            wrist_l = [(1 - blend) * start_wrist[i] + blend * contact_wrist[i]
                       for i in range(3)]
            ankles = (support, ankle)
            feet = ([1, 0, 0], footdir)
            wrists = (wrist_l, free_wrist)
            knees = ([0, 1, 0], [0, 1, 0])
        # The toe is touched at full kick; the approaching hand stays inside
        # the arm radius while the leg is still rising.
        adjusted=[]
        for wr,sh in zip(wrists,[sh_l,sh_r]):
            v=[wr[i]-sh[i] for i in range(3)];r=math.sqrt(sum(z*z for z in v))
            adjusted.append([sh[i]+min(1,.568/r)*v[i] for i in range(3)])
        wrists=adjusted
        return _army_ref_pose(pelvis, p1, p2, ankles, feet, wrists,
                              ([1, 0, 0], [1, 0, 0]),
                              ([0, 1, 0], [0, -1, 0]), knees=knees,
                              grips=(.15, .15))

    keys=[]
    for i in range(257):
        t=i/256;sign=1 if t<.5 else -1;u=t if t<.5 else t-.5
        if u<.18:q=.5-.5*math.cos(math.pi*u/.18)
        elif u<.25:q=1
        elif u<.43:q=.5+.5*math.cos(math.pi*(u-.25)/.18)
        else:q=0
        keys.append((t,key(sign,q)))
    return dict(seconds=5.2,keys=keys,cam_az=28,cam_el=14,props=[])


def leg_tuck_and_twist():
    def key(t):
        s=1 if t<.5 else -1;u=2*(t if t<.5 else t-.5)
        q=math.sin(math.pi*u)**2
        p=_key((0,0,.11),-38,-46,0,yaw=s*12*q,roll=s*10*q,
            wrists=([-.18,.28,.035],[-.18,-.28,.035]),knees=([0,0,1],[0,0,1]),hands=([1,0,0],[1,0,0]))
        up,left=trunk_frame(-38,s*12*q,s*10*q)
        for side,sg in [('l',1),('r',-1)]:
            hip=[p['pelvis'][i]-.03*up[i]+sg*.09*left[i] for i in range(3)]
            dz=.28-hip[2];start=[hip[0]+math.sqrt(.8695**2-dz**2),hip[1],.28]
            target=[.30,sg*.10+s*.20,.25]
            p['ankle_'+side]=[start[i]*(1-q)+target[i]*q for i in range(3)]
        return p
    return dict(seconds=5,keys=[(i/160,key(i/160)) for i in range(160)],cam_az=28,cam_el=25,props=[])


def side_to_side_knee_lifts():
    base=([0,.11,.08],[0,-.11,.08])
    def centered(x=0):
        return _army_hop((x,0,.90),([x,.11,.08],[x,-.11,.08]),p1=3,p2=4,
                         arm_dirs=((.10,.10,-.43),(.10,-.10,-.43)))
    def knee_lift(left_active, x):
        raised=([x+.35,.11,.72],[x,-.11,.08]) if left_active else ([x,.11,.08],[x+.35,-.11,.72])
        p=_army_hop((x,0,.95),raised,p1=8,p2=12,
                    arm_dirs=((.03,-.18,-.47),(.03,.18,-.47)),
                    knees=([1,0,0],[1,0,0]))
        # Solve provisional leg, then put the opposite hand midpoint on the actual knee.
        J=__import__('poses').solve(p,{})
        K=J['knee_l' if left_active else 'knee_r']
        target=[K[0]+.05,K[1],K[2]+.085]
        if left_active: p['wrist_r']=target; p['hand_r']=[0,0,-1]; p['palm_r']=[-1,0,0]; p['contact_r']=1
        else: p['wrist_l']=target; p['hand_l']=[0,0,-1]; p['palm_l']=[-1,0,0]; p['contact_l']=1
        return p
    center=centered(); r=knee_lift(False,.05); l=knee_lift(True,-.05)
    # The brief two-foot flight shifts the next support by only 5 cm.
    flight_r=_army_hop((.05,0,1.00),([.05,.11,.18],[.05,-.11,.18]),p1=2,p2=3)
    flight_l=_army_hop((-.05,0,1.00),([-.05,.11,.18],[-.05,-.11,.18]),p1=2,p2=3)
    return dict(seconds=4.0,keys=_army_hop_keys([(0,center),(.10,center),(.16,flight_r),(.20,r),(.26,r),(.34,flight_l),(.38,l),(.44,l),(.52,flight_r),(.56,r),(.62,r),(.70,flight_l),(.74,l),(.80,l),(.85,_army_hop((0,0,1.0),([0,.11,.18],[0,-.11,.18]))),(.90,center),(.97,center)]),cam_az=28,cam_el=12,props=[])


def straddle_run():
    # Eight diagonal single-leg bounds. Local left/right foot identities stay fixed;
    # pelvis follows the alternating support side while only airborne feet travel.
    beats=[]; t=0.0; x0=0.0; yc0=-.22
    def shape(x,yc,support,air=False,pz=None):
        if pz is None: pz=1.04 if air else .94
        zsup=.20 if air else .08; zfree=.27 if air else .18
        al=[x,yc+.12,zsup if support=='L' or air else zfree]
        ar=[x,yc-.12,zsup if support=='R' or air else zfree]
        return _army_hop((x,yc,pz),(al,ar),p1=5 if air else 11,p2=7 if air else 13,
                         knees=([1,0,0],[1,0,0]),arm_dirs=((.32,.24,.10),(.32,-.24,.10)))
    current=shape(x0,yc0,'L',False); beats.extend([(0,current),(.06,current)])
    dests=[(.15,.22),(.30,-.22),(.45,.22),(.60,-.22),(.45,.22),(.30,-.22),(.15,.22),(0,-.22)]
    t=.12; prevx=x0; prevy=yc0; support='L'
    for x,yc in dests:
        # push retains the exact previous planted support target
        push=shape(prevx,prevy,support,False,.94)
        flight=shape(x,yc,support,True,1.04)
        next_support='R' if support=='L' else 'L'
        land=shape(x,yc,next_support,False,.91)
        beats.extend([(t,push),(t+.035,flight),(t+.075,land)])
        t+=.105; prevx=x; prevy=yc; support=next_support
    beats.append((.96,beats[0][1]))
    return dict(seconds=4.8,keys=_army_hop_keys(beats),cam_az=32,cam_el=12,props=[])


def half_squat_laterals():
    # Left/right/right/left step-hops: low pelvis throughout; travel occurs in flight.
    def pose(y,z=.08,low=True):
        pz=(.60 if low else .94)+(z-.08)
        return _army_hop((0,y,pz),([0,y+.13,z],[0,y-.13,z]),p1=19 if low else 3,p2=22 if low else 4,
            arm_dirs=((.36,.08,-.02),(.36,-.08,-.02)))
    start=pose(0); beats=[(0,start),(.07,start)]; t=.12
    current=0.0
    for dest in (.20,0,-.20,0):
        push=pose(current)
        flight=pose(dest,.16)
        landing=pose(dest,.08)
        beats.extend([(t,push),(t+.035,flight),(t+.075,landing)])
        t+=.18; current=dest
    beats.extend([(.91,pose(0)),(.97,start)])
    return dict(seconds=4.0,keys=_army_hop_keys(beats),cam_az=30,cam_el=12,props=[])


def frog_jumps():
    # Forward, back, back, forward; squat loading, extension, two-foot flight and soft squat.
    def squat(x,z=.08,pz=.60):
        return _army_hop((x,0,pz),([x,.12,z],[x,-.12,z]),p1=20,p2=23,
                         arm_dirs=((.44,.10,-.10),(.44,-.10,-.10)))
    def extend(x):
        return _army_hop((x,0,.955),([x,.12,.08],[x,-.12,.08]),p1=5,p2=7,
                         arm_dirs=((.38,.10,.20),(.38,-.10,.20)))
    beats=[(0,squat(0)),(.08,squat(0))]; t=.12
    current=0.0
    for x in (.24,0,-.24,0):
        push=extend(current)
        flight=_army_hop((x,0,1.02),([x,.12,.16],[x,-.12,.16]),p1=3,p2=5,
                         arm_dirs=((.40,.10,.16),(.40,-.10,.16)))
        land=squat(x,.08,.60)
        beats.extend([(t,push),(t+.035,flight),(t+.075,land)])
        t+=.18; current=x
    beats.append((.97,beats[0][1]))
    return dict(seconds=4.0,keys=_army_hop_keys(beats),cam_az=30,cam_el=13,props=[])


def quarter_turn_jump():
    # Chest square; ankles, toe vectors and knee poles rotate through each 90 degree turn.
    def pose(deg, airborne=False, low=True):
        a=math.radians(deg); z=.18 if airborne else .08
        ankles=([-.13*math.sin(a),.13*math.cos(a),z],[.13*math.sin(a),-.13*math.cos(a),z])
        direction=[math.cos(a),math.sin(a),0]
        pz=.70 if airborne else (.60 if low else .94)
        return _army_hop((0,0,pz),ankles,p1=18 if low else 3,p2=21 if low else 4,
            knees=(direction,direction),foot_dirs=(direction,direction),arm_dirs=((.35,.15,.12),(.35,-.15,.12)))
    center=pose(0); beats=[(0,center),(.07,center)]; t=.10; current=0
    for angle in (-90,0,90,0):
        source=pose(current)
        beats.extend([(t,source),(t+.035,pose(angle,True)),(t+.075,pose(angle,False))])
        t+=.18; current=angle
    beats.append((.97,center))
    return dict(seconds=4.2,keys=_army_hop_keys(beats),cam_az=30,cam_el=14,props=[])


def criss_cross_jump():
    def pose(which,air=False,load=False):
        z=.16 if air else .08
        if which=='L': ankles=([.07,-.055,z],[-.07,.055,z])
        else: ankles=([-.07,-.055,z],[.07,.055,z])
        return _army_hop((0,0,.90 if load else (.99 if air else .94)),ankles,p1=2 if not load else 8,p2=3 if not load else 10,
                         arm_dirs=((.06,.04,-.55),(.06,-.04,-.55)))
    start=pose('L'); beats=[(0,start),(.08,start)]; t=.12; current='L'
    for which in ('R','L','R','L'):
        push=pose(current,load=True)  # exact source landing coordinates
        flight=pose(which,air=True)
        land=pose(which)
        beats.extend([(t,push),(t+.035,flight),(t+.075,land)])
        t+=.18; current=which
    beats.append((.97,start))
    return dict(seconds=3.4,keys=_army_hop_keys(beats),cam_az=28,cam_el=14,props=[])


def alternate_twist_jump():
    # Short flight between staggered landings; modest shared yaw shows hip rotation.
    def stance(front,air=False,load=False):
        z=.15 if air else .08
        if front=='L': ankles=([.17,.12,z],[-.17,-.12,z]); yaw=-12
        else: ankles=([-.17,.12,z],[.17,-.12,z]); yaw=12
        return _army_hop((0,0,.90 if load else (.99 if air else .94)),ankles,p1=8 if load else 2,p2=10 if load else 4,yaw=yaw,
                         arm_dirs=((.20,.20,.10),(.20,-.20,.10)))
    first=stance('L'); beats=[(0,first),(.08,first)]; t=.12; current='L'
    for side in ('R','L','R','L'):
        push=stance(current,load=True)
        air=stance(side,air=True)
        land=stance(side)
        beats.extend([(t,push),(t+.035,air),(t+.075,land)])
        t+=.18; current=side
    beats.append((.97,first))
    return dict(seconds=3.8,keys=_army_hop_keys(beats),cam_az=30,cam_el=12,props=[])


def _army_shoulders(p):
    u1,_=trunk_frame(p['p1'],p.get('yaw',0),p.get('roll',0))
    u2,l=trunk_frame(p['p2'],p.get('yaw',0),p.get('roll',0))
    return {s:[p['pelvis'][i]+.24*u1[i]+.23*u2[i]+sg*.18*l[i] for i in range(3)] for s,sg in [('l',1),('r',-1)]}

def _army_ref_shoulder(pelvis, p1, p2, side, yaw=0.0, roll=0.0):
    up1, _ = trunk_frame(p1, yaw, roll)
    up2, left2 = trunk_frame(p2, yaw, roll)
    chest = [pelvis[i] + .24 * up1[i] + .26 * up2[i] for i in range(3)]
    return [chest[i] + side * .18 * left2[i] - .03 * up2[i] for i in range(3)]

def _army_ref_pose(pelvis, p1, p2, feet, footdirs, wrists, hands, elbows,
                   knees=None, hp=0.0, grips=(.25, .25)):
    knees = knees or ([1, 0, 0], [1, 0, 0])
    pose = base_pose(pelvis=list(pelvis), p1=p1, p2=p2, hp=hp, yaw=0, roll=0,
                     ankle_l=list(feet[0]), ankle_r=list(feet[1]),
                     foot_l=list(footdirs[0]), foot_r=list(footdirs[1]),
                     knee_l=list(knees[0]), knee_r=list(knees[1]),
                     wrist_l=list(wrists[0]), wrist_r=list(wrists[1]),
                     hand_l=list(hands[0]), hand_r=list(hands[1]),
                     elbow_l=list(elbows[0]), elbow_r=list(elbows[1]))
    pose['grip_l'], pose['grip_r'] = grips
    return pose

def _army_hop(pelvis, ankles, *, p1=2, p2=3, yaw=0, knees=None,
              arm_dirs=None, wrist_targets=None, hands=None, foot_dirs=None):
    """Build an Army hop beat; torso shoulder origins use the solver's trunk frame."""
    knees = knees or ([1, 0, 0], [1, 0, 0])
    arm_dirs = arm_dirs or ((.02, .04, -.565), (.02, -.04, -.565))
    align_hands=hands is None
    hands = hands or ([0, 0, -1], [0, 0, -1])
    foot_dirs = foot_dirs or ([1, 0, 0], [1, 0, 0])
    u1, left1 = trunk_frame(p1, yaw, 0)
    u2, left2 = trunk_frame(p2, yaw, 0)
    spine = [pelvis[i] + .24 * u1[i] for i in range(3)]
    chest = [spine[i] + .26 * u2[i] for i in range(3)]
    shoulders = []
    for sgn in (1, -1):
        shoulders.append([chest[i] + sgn * .18 * left2[i] - .03 * u2[i] for i in range(3)])
    wrists = wrist_targets or tuple([shoulders[s][i] + arm_dirs[s][i] for i in range(3)] for s in (0, 1))
    p=base_pose(pelvis=list(pelvis), p1=p1, p2=p2, yaw=yaw,
        ankle_l=list(ankles[0]), ankle_r=list(ankles[1]),
        knee_l=list(knees[0]), knee_r=list(knees[1]),
        foot_l=list(foot_dirs[0]), foot_r=list(foot_dirs[1]),
        wrist_l=list(wrists[0]), wrist_r=list(wrists[1]),
        elbow_l=[-1, .2, 0], elbow_r=[-1, -.2, 0],
        hand_l=list(hands[0]), hand_r=list(hands[1]))

    p['contact_l']=p['contact_r']=0
    if align_hands:
        joints=solve(p,[])
        for side in ('l','r'):
            d=[joints['wrist_'+side][i]-joints['elbow_'+side][i] for i in range(3)]
            p['hand_'+side]=d
            n=[-d[2],0,d[0]]
            p['palm_'+side]=n if math.hypot(n[0],n[2])>1e-7 else [1,0,0]
    return p

def _army_hop_keys(beats):
    # Sample the support choreography densely, then align the rigid hands to
    # the actual forearms. This avoids a backwards wrist between arm-swing keys.
    keys=[]
    for i in range(257):
        t=i/256;p=sample(beats,t);joints=solve(p,[])
        for side in ('l','r'):
            d=[joints['wrist_'+side][j]-joints['elbow_'+side][j] for j in range(3)]
            norm=math.sqrt(sum(v*v for v in d));d=[v/norm for v in d]
            weight=p['contact_'+side]
            d=[(1-weight)*d[j]+weight*(0 if j<2 else -1) for j in range(3)]
            norm=math.sqrt(sum(v*v for v in d));d=[v/norm for v in d]
            p['hand_'+side]=d
            n=[d[2],0,-d[0]]
            p['palm_'+side]=n if math.hypot(n[0],n[2])>1e-7 else [1,0,0]
        keys.append((t,p))
    return keys

EXERCISES = {
    "bend-and-reach": bend_and_reach,
    "high-jumper": high_jumper,
    "squat-bender": squat_bender,
    "windmill-army": windmill_army,
    "prone-row-army": prone_row_army,
    "turn-and-lunge": turn_and_lunge,
    "y-squat": y_squat,
    "front-kick-toe-touch": front_kick_toe_touch,
    "leg-tuck-and-twist": leg_tuck_and_twist,
    "side-to-side-knee-lifts": side_to_side_knee_lifts,
    "straddle-run": straddle_run,
    "half-squat-laterals": half_squat_laterals,
    "frog-jumps": frog_jumps,
    "quarter-turn-jump": quarter_turn_jump,
    "criss-cross-jump": criss_cross_jump,
    "alternate-twist-jump": alternate_twist_jump,
}
