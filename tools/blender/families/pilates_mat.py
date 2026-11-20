"""Pilates mat work with continuous controlled arcs and explicit floor contacts."""
import math
from poses import base_pose, sample, solve, trunk_frame

MAT = [dict(kind='box', center=[0, 0, -0.025], size=[2.4, 1.5, .05])]
# Keep the repaired mat surface clear of the floor to prevent coplanar shading flicker.
REPAIR_MAT = [dict(kind='box', center=[0, 0, -0.020], size=[2.4, 1.5, .05])]

def _key(P, p1, p2, feet, wrists=None, *, yaw=0, hp=0, knees=None, arms=None, hands=None, feetdir=None, roll=0,
         thoracic_twist=0, head_turn=None, head_side_tilt=None, head_retract=None, head_detail=None):
    if wrists is None: wrists = [[-.12,-.22,.035],[-.12,.22,.035]]
    if knees is None: knees=[[1,0,0],[1,0,0]]
    if arms is None: arms=[[0,-1,0],[0,1,0]]
    if hands is None: hands=[[1,0,0],[1,0,0]]
    if feetdir is None: feetdir=[[1,0,0],[1,0,0]]
    pose=base_pose(pelvis=P,p1=p1,p2=p2,hp=hp,yaw=yaw,roll=roll,
      ankle_r=feet[0],ankle_l=feet[1],knee_r=knees[0],knee_l=knees[1],foot_r=feetdir[0],foot_l=feetdir[1],
      wrist_r=wrists[0],wrist_l=wrists[1],elbow_r=arms[0],elbow_l=arms[1],hand_r=hands[0],hand_l=hands[1],
      palm_r=[0,0,-1],palm_l=[0,0,-1],grip_r=.1,grip_l=.1)
    pose['thoracic_twist']=thoracic_twist
    for name, value in [('head_turn',head_turn),('head_side_tilt',head_side_tilt),('head_retract',head_retract),('head_detail',head_detail)]:
        if value is not None:
            pose[name]=value
    return pose

def _lying(ankles, wrists=None, p1=-90, p2=-90, hp=0, P=[0,0,.11], **kw):
    return _key(P,p1,p2,ankles,wrists,hp=hp,**kw)

def _roll(s, rocker=False, openlegs=False):
    # Rocking ball: tucked legs stay short and off-mat; hands follow ankle contact.
    # Open rocker: each ankle remains .866 m from its hip, at fixed 0.80 m foot width.
    p1_end,p2_end,hp_end=(-120.0,-110.0,45.0) if openlegs else (-145.0,-115.0,50.0)
    P,p1,p2,hp=_pelvis_contact_pose(s,p1_end,p2_end,hp_end)
    shoulders=_shoulders(P,p1,p2)
    u1,l1=trunk_frame(p1)
    ankles=[]; wrists=[]; hands=[]; knees=[]
    for idx,sgn in enumerate((-1,1)): # right then left
        hip=[P[i]-.03*u1[i]+sgn*.09*l1[i] for i in range(3)]
        if openlegs:
            # Constant-radius V vectors: .866 m total, .31 m lateral from each hip.
            # Start has legs forward/up; end points past the head, slightly down, to land near the mat.
            q=s*s*(3-2*s)
            # Rotate the sagittal leg vector from forward/up to overhead/back early enough
            # that the hand-to-ankle grip stays within arm reach during the whole arc.
            theta=math.radians(53.5+126.5*(q**.8))
            sagittal=math.sqrt(.866**2-.31**2)
            lateral=sgn*.31
            vec=[sagittal*math.cos(theta),lateral,sagittal*math.sin(theta)]
        else:
            # Tucked shin radius (0.46 m): feet start in front of the chest and arc above it.
            q=s*s*(3-2*s)
            dx=.30*(1-q)-.40*q
            dz=.34*(1-q)+.25*q
            lat=sgn*.015
            r=.46; scale=math.sqrt(r*r-lat*lat)/math.sqrt(dx*dx+dz*dz)
            vec=[dx*scale,lat,dz*scale]
        ankle=[hip[i]+vec[i] for i in range(3)]
        wrist,hand=_contact_wrist(shoulders[idx],ankle)
        ankles.append(ankle); wrists.append(wrist); hands.append(hand)
        knees.append([1.0,sgn*.35,0.0] if openlegs else [trunk_frame(p2)[0][0],sgn*.20,trunk_frame(p2)[0][2]])
    feetdirs=([[1,0,.1],[1,0,.1]] if openlegs else [[1,0,0],[1,0,0]])
    k=_key(P,p1,p2,[ankles[0],ankles[1]],[wrists[0],wrists[1]],hp=hp,
           knees=[knees[0],knees[1]],arms=[[0,-1,1],[0,1,1]],hands=[hands[0],hands[1]],feetdir=feetdirs)
    k['grip_r']=k['grip_l']=.95
    k['palm_r']=[0,0,1]; k['palm_l']=[0,0,1]
    return k

def _dense(fn, n=96): return [(i/n,fn(i/n)) for i in range(n+1)]

def roll_up():
    def k(t):
        # The upper segment leads the lower one on the curl; this staggers the
        # available two hinges without claiming true vertebra-by-vertebra roll.
        s=(1-math.cos(2*math.pi*t))/2
        p1=-90+118*s;p2=-90+138*(s**.9 if s else 0.0)
        P=[.03*math.sin(math.radians(p1))+.03,0,.11];k=_key(P,p1,p2,[[.8995,-.10,.08],[.8995,.10,.08]],knees=[[0,0,1],[0,0,1]])
        j=solve(k,{});a=math.radians(180*(1-s))
        for side,sg in [('r',-1),('l',1)]:
            sh=j['shoulder_'+side];d=[math.cos(a),0,math.sin(a)]
            k['wrist_'+side]=[sh[i]+.568*d[i] for i in range(3)];k['hand_'+side]=d;k['elbow_'+side]=[0,sg,0]
        return k
    return dict(seconds=5,keys=_dense(k),cam_az=24,cam_el=20,props=REPAIR_MAT)

def rolling_like_a_ball():
    keys=_dense(lambda t:_roll(math.sin(math.pi*t)**2))
    return dict(seconds=3.6,keys=keys,cam_az=25,cam_el=22,props=MAT)

def single_leg_circle():
    def k(t):
        a=4*math.pi*(t if t<=.5 else 1-t);r=.8695;th=math.radians(16)
        raised=[.03+r*math.sin(th)*math.cos(a),-.09+r*math.sin(th)*math.sin(a),.11+r*math.cos(th)]
        return _pm_floor_arms(_lying([raised,[.899,.10,.08]],knees=[[1,0,0],[0,0,1]],feetdir=[[0,0,1],[1,0,0]]))
    return dict(seconds=5,keys=_dense(k),cam_az=28,cam_el=24,props=MAT)

def single_leg_stretch():
    def k(t):
        q=(1-math.cos(4*math.pi*t))/2;long=[.03+.8695*math.cos(math.radians(18)),.11+.8695*math.sin(math.radians(18))]
        feet=[]
        for sg,v in [(-1,q),(1,1-q)]:
            feet.append([.10*(1-v)+long[0]*v,sg*.09,.42*(1-v)+long[1]*v])
        k=_lying(feet,p2=-60,hp=0,knees=[[-1,0,1],[-1,0,1]])
        j=solve(k,{});kr=j['knee_r'];kl=j['knee_l'];c=[kr[i]*(1-q)+kl[i]*q for i in range(3)];c[0]-=.025
        return _pm_grip(k,c)
    return dict(seconds=5,keys=_dense(k,160),cam_az=26,cam_el=24,props=MAT)

def double_leg_stretch():
    def k(t):
        if t<.12:q=0
        elif t<.40:q=(1-math.cos(math.pi*(t-.12)/.28))/2
        elif t<.53:q=1
        elif t<.85:q=(1+math.cos(math.pi*(t-.53)/.32))/2
        else:q=0
        long=[.03+.8695*math.cos(math.radians(20)),.11+.8695*math.sin(math.radians(20))]
        feet=[[.10*(1-q)+long[0]*q,sg*.09,.42*(1-q)+long[1]*q] for sg in (-1,1)]
        k=_lying(feet,p2=-60,hp=0,knees=[[-1,0,1],[-1,0,1]]);j=solve(k,{})
        c=[(j['knee_r'][i]+j['knee_l'][i])*.5 for i in range(3)];grip=_pm_grip(dict(k),c)
        for side,sg in [('r',-1),('l',1)]:
            sh=j['shoulder_'+side]
            if t<=.53:d=[-.94,0,.342]
            else:
                a=math.pi*min(1,(t-.53)/.32);d=[-math.cos(a),sg*math.sin(a),.15];n=math.sqrt(sum(v*v for v in d));d=[v/n for v in d]
            far=[sh[i]+.568*d[i] for i in range(3)]
            k['wrist_'+side]=[grip['wrist_'+side][i]*(1-q)+far[i]*q for i in range(3)]
            # Direction follows the same hemisphere during the reach/circle.
            v=[grip['hand_'+side][i]*(1-q)+d[i]*q for i in range(3)]
            if sum(x*x for x in v)<.01:v=[0,0,1]
            k['hand_'+side]=v;k['elbow_'+side]=[0,sg,0];k['palm_'+side]=[0,0,-1]
        return k
    return dict(seconds=5,keys=_dense(k,160),cam_az=26,cam_el=24,props=MAT)

def scissors():
    def smooth(a):return a*a*(3-2*a)
    def k(t):
        if t<.18:q=0
        elif t<.32:q=smooth((t-.18)/.14)
        elif t<.68:q=1
        elif t<.82:q=1-smooth((t-.68)/.14)
        else:q=0
        pulse=3*math.sin(10*math.pi*t)**2 if t<.18 or .32<t<.68 or t>.82 else 0
        ar=math.radians(95*(1-q)+20*q+pulse*(1-q));al=math.radians(20*(1-q)+95*q+pulse*q)
        feet=[[.03+.8695*math.cos(a),sg*.09,.11+.8695*math.sin(a)] for sg,a in [(-1,ar),(1,al)]]
        k=_lying(feet,p2=-60,hp=0,knees=[[1,0,0],[1,0,0]],feetdir=[[math.cos(ar),0,math.sin(ar)],[math.cos(al),0,math.sin(al)]])
        c1=[.03+.50*math.cos(ar),-.09,.11+.50*math.sin(ar)];c2=[.03+.50*math.cos(al),.09,.11+.50*math.sin(al)]
        c=[c1[i]*(1-q)+c2[i]*q for i in range(3)];w=abs(2*q-1)**2
        c=[c[i]*w+[-.20,0,.45][i]*(1-w) for i in range(3)]
        return _pm_grip(k,c)
    return dict(seconds=5,keys=_dense(k,192),cam_az=26,cam_el=24,props=MAT)

def spine_stretch_forward():
    from poses import solve
    def k(t):
        s=(1-math.cos(2*math.pi*t))/2;p1=12*s;p2=48*s
        P=[.03*math.sin(math.radians(p1)),0,.11]
        k=_key(P,p1,p2,[[.831,-.34,.08],[.831,.34,.08]],hp=15*s,knees=[[0,0,1],[0,0,1]],feetdir=[[0,0,1],[0,0,1]])
        j=solve(k,{});a=math.radians(-8-34*s)
        for side,sg in [('r',-1),('l',1)]:
            sh=j['shoulder_'+side];k['wrist_'+side]=[sh[0]+.568*math.cos(a),sh[1],sh[2]+.568*math.sin(a)]
            k['elbow_'+side]=[0,sg,0];k['hand_'+side]=[math.cos(a),0,math.sin(a)]
        return k
    return dict(seconds=5,keys=_dense(k),cam_az=25,cam_el=20,props=MAT)


def open_leg_rocker():
    keys=_dense(lambda t:_roll(math.sin(math.pi*t)**2,rocker=True,openlegs=True))
    return dict(seconds=3.8,keys=keys,cam_az=24,cam_el=23,props=MAT)

def corkscrew():
    def k(t):
        a=4*math.pi*(t if t<=.5 else 1-t);r=.8695;th=math.radians(23)
        x=.03+r*math.sin(th)*math.cos(a);dy=r*math.sin(th)*math.sin(a);z=.11+r*math.cos(th)
        return _pm_floor_arms(_lying([[x,-.09+dy,z],[x,.09+dy,z]],knees=[[1,0,0],[1,0,0]],feetdir=[[0,0,1],[0,0,1]]))
    return dict(seconds=5,keys=_dense(k),cam_az=28,cam_el=24,props=MAT)

def saw():
    def k(t):
        # Each half-cycle: sit tall with the arms level in a T, rotate the whole
        # arm line with the chest, fold and reach the front hand past the opposite
        # little toe, sit up (the hand lifts off first, then returns), then untwist.
        u=(2*t)%1.0; side=1.0 if t<.5 else -1.0
        def ease(x):
            x=max(0.0,min(1.0,x)); return x*x*(3-2*x)
        turn=ease((u-.03)/.17)*(1.0-ease((u-.79)/.16))
        bend=ease((u-.19)/.31)*(1.0-ease((u-.52)/.29))
        twist=side*40.0*turn
        k=_key([.03*math.sin(math.radians(60.0*bend)),0,.11],60.0*bend,80.0*bend,
               [[.846,-.30,.08],[.846,.30,.08]],yaw=0,
               thoracic_twist=twist,head_turn=side*12.0*turn,
               knees=[[0,0,1],[0,0,1]],feetdir=[[1,0,0],[1,0,0]])
        j=solve(k,{})
        # Keep the legs long and the heels planted in a wide V while the chest
        # twists; derive the forward offset from each hip to avoid IK stretch.
        for name,sgn in [('r',-1),('l',1)]:
            hip=j['hip_'+name];dy=sgn*.30-hip[1]
            dx=math.sqrt(.8695**2-dy**2-.015**2)
            k['ankle_'+name]=[dx,sgn*.30,.08]
        j=solve(k,{})
        # Both arms stay level (shoulder height) and in one line through the
        # shoulders, following the chest rotation: the rear arm reaches behind,
        # the front arm across. The line is taken from the upright twisted chest;
        # the pitched chest's own lateral axis would tilt the arms up or down.
        upright_left=trunk_frame(0,0,twist)[1]
        arm=.568
        for name,sgn in [('r',-1),('l',1)]:
            d=[sgn*x for x in upright_left];sh=j['shoulder_'+name]
            k['wrist_'+name]=[sh[i]+arm*d[i] for i in range(3)]
            k['hand_'+name]=d;k['elbow_'+name]=[0,0,1]
        if bend>0:
            reaching='r' if side>0 else 'l'
            opposite='l' if reaching=='r' else 'r'
            sh=j['shoulder_'+reaching];ank=k['ankle_'+opposite]
            # Little-toe edge of the opposite foot, a little beyond the toes.
            target=[ank[0]+.24,ank[1]+(.05 if opposite=='l' else -.05),ank[2]+.01]
            v=[target[i]-sh[i] for i in range(3)];n=math.sqrt(sum(x*x for x in v))
            d=[x/n for x in v] if n else [1,0,0]
            reach=min(arm,max(0.0,n-.17))
            start=k['wrist_'+reaching]; end=[sh[i]+reach*d[i] for i in range(3)]
            # Lift before travelling: the height follows bend**1.6 (it leaves the
            # leg first on the way up and lands last on the way down), the
            # horizontal travel follows bend**.75, so the hand never drags along the leg.
            wz=bend**1.6; wxy=bend**.75
            w=[start[0]+(end[0]-start[0])*wxy,start[1]+(end[1]-start[1])*wxy,start[2]+(end[2]-start[2])*wz]
            off=[w[i]-sh[i] for i in range(3)];m=math.sqrt(sum(x*x for x in off))
            if m>arm: w=[sh[i]+off[i]*arm/m for i in range(3)]
            k['wrist_'+reaching]=w
            initial=k['hand_'+reaching]
            hd=[initial[i]+wxy*(d[i]-initial[i]) for i in range(3)];hn=math.sqrt(sum(x*x for x in hd))
            k['hand_'+reaching]=[x/hn for x in hd]
            k['elbow_'+reaching]=[0,0,1]
        return k
    return dict(seconds=5.6,keys=_dense(k,240),interpolation='continuous',cam_az=34,cam_el=24,props=REPAIR_MAT)

def roll_over():
    # Start supine, curl from the pelvis through the upper trunk, then lower in
    # reverse order. The contact formula keeps the trunk supported by the mat.
    def pose(s, opened=0):
        # Let the pelvis/lumbar hinge start the roll, then progressively curl
        # the upper trunk as the legs pass overhead. On the return this reverses
        # the order, keeping the shoulder-blade support phase visible.
        q1=s*s*(3-2*s)
        x=max(0.0,min(1.0,(s-.14)/.86)); q2=x*x*(3-2*x)
        p1=-90.0-62.0*q1; p2=-90.0-8.0*q2; hp=10.0+28.0*q2
        u1,_=trunk_frame(p1);u2,_=trunk_frame(p2)
        sp=.24*u1[2]; ch=sp+.26*u2[2]
        pz=.01-min(-.09,sp-.09,ch-.10)
        # The rendered chest ellipsoid is centered at chest - .11*u2 and has
        # radii (.115, .175, .17). In this sagittal plane, its world-vertical
        # support radius is set by the local-X and local-Z axes. Raise only
        # enough to maintain 1 mm of chest-surface clearance; wrist anchors
        # stay at their authored floor targets.
        chest_surface_radius=math.sqrt((.115*u2[0])**2+(.17*u2[2])**2)
        # 0.1 mm extra margin covers rounded joint/orientation serialization.
        pz=max(pz,chest_surface_radius+.11*u2[2]-ch+.0011)
        P=[-.16*q1,0.0,pz]
        # Constant leg radius, with feet rotating from vertical to parallel beyond the head.
        ang=math.pi/2*q1
        dx=-math.sin(ang); dz=math.cos(ang)
        spread=.03+.06*opened
        ankles=[]; feet=[]
        for sgn in (-1,1):
            hip=[P[i]-.03*u1[i]+sgn*.09*trunk_frame(p1)[1][i] for i in range(3)]
            dy=sgn*spread
            r=math.sqrt(.8695**2-dy**2)
            ank=[hip[0]+r*dx,hip[1]+dy,hip[2]+r*dz]
            ankles.append(ank); feet.append([-math.sin(ang),0,math.cos(ang)])
        # Keep palms at fixed mat targets as the pelvis rolls. The shared
        # helper deliberately follows the shoulders for other Pilates arcs;
        # this exercise instead uses stable floor support beside the torso.
        k=_key(P,p1,p2,[ankles[0],ankles[1]],[[.03,-.18,.04],[.03,.18,.04]],hp=hp,
               knees=[[1,0,0],[1,0,0]],arms=[[0,-1,0],[0,1,0]],hands=[[1,0,0],[1,0,0]],
               feetdir=[feet[0],feet[1]])
        # Follow each solved shin, with a small side-specific toe splay. The
        # offset avoids the nearly collinear heel-frame fallback used by render.
        j=solve(k,{})
        left=trunk_frame(p1)[1]
        for side,sgn in [('r',-1),('l',1)]:
            shin=[j['ankle_'+side][i]-j['knee_'+side][i] for i in range(3)]
            length=math.sqrt(sum(x*x for x in shin))
            pointed=[shin[i]/length+.22*sgn*left[i] for i in range(3)]
            mag=math.sqrt(sum(x*x for x in pointed))
            k['foot_'+side]=[x/mag for x in pointed]
        return k
    keys=[]
    for i in range(97):
        t=i/96
        if t < .45:
            x=t/.45; s=x*x*(3-2*x)
        elif t <= .55:
            s=1.0
        else:
            x=(1.0-t)/.45; x=max(0.0,min(1.0,x)); s=x*x*(3-2*x)
        opened=max(0.0,min(1.0,(s-.76)/.24))
        keys.append((t,pose(s,opened)))
    return dict(seconds=4,keys=keys,cam_az=24,cam_el=23,props=REPAIR_MAT)

def seal():
    # Deliberate top and shoulder-blade holds make three ankle-clap pulses visible.
    def phase(t):
        if .22 <= t < .48:
            x=(t-.22)/.26; return x*x*(3-2*x)
        if .48 <= t <= .72: return 1.0
        if .72 < t <= .98:
            x=(.98-t)/.26; return x*x*(3-2*x)
        return 0.0
    def pulse(t, centers):
        a=0.0
        for c in centers:
            # A cosine pulse spans multiple movie frames at 24 fps, rather than
            # being a one-frame triangular flick that disappears in playback.
            x=abs(t-c)/.025
            if x<1.0: a=max(a,.5+.5*math.cos(math.pi*x))
        return a
    keys=[]
    for i in range(193):
        t=i/192
        s=phase(t)
        k=_roll(s)
        q=pulse(t,[.045,.115,.185,.515,.615,.715])
        for side,sgn in (('r',-1),('l',1)):
            # Feet touch together at rest; each clap briefly opens then returns the soles together.
            ank=k['ankle_'+side]
            ank[1]=sgn*(.035+.065*q)
            # Rebuild wrist to preserve the inside-leg ankle grip as the feet clap.
            P=k['pelvis']; p1=k['p1'];p2=k['p2'];sh=_shoulders(P,p1,p2)[0 if side=='r' else 1]
            wr,hd=_contact_wrist(sh,ank)
            k['wrist_'+side]=wr;k['hand_'+side]=hd
        k['grip_r']=k['grip_l']=.98
        keys.append((t,k))
    return dict(seconds=6.0,keys=keys,cam_az=25,cam_el=22,props=REPAIR_MAT)

def spine_twist():
    from poses import solve
    def ease(x):
        x=max(0.0,min(1.0,x)); return x*x*(3-2*x)
    def k(t):
        # Two short pulses per side, with pelvis and straight legs facing forward.
        side=1.0 if t<.5 else -1.0
        u=(2*t)%1.0
        turn=ease((u-.16)/.18)*(1.0-ease((u-.72)/.16))
        twist=side*32.0*turn
        k=_key([0,0,.11],0,0,[[.8695,-.30,.08],[.8695,.30,.08]],yaw=0,
               thoracic_twist=twist,head_turn=side*10.0*turn,
               knees=[[0,0,1],[0,0,1]],feetdir=[[1,0,0],[1,0,0]])
        j=solve(k,{})
        for name,sgn in [('r',-1),('l',1)]:
            hip=j['hip_'+name];dy=sgn*.30-hip[1]
            dx=math.sqrt(max(0.0,.8695**2-dy**2-(hip[2]-.08)**2))
            k['ankle_'+name]=[hip[0]+dx,sgn*.30,.08]
        j=solve(k,{})
        for name,sgn in [('r',-1),('l',1)]:
            d=[sgn*x for x in j['chest_left']];sh=j['shoulder_'+name]
            k['wrist_'+name]=[sh[i]+.568*d[i] for i in range(3)]
            k['elbow_'+name]=[0,0,-1];k['hand_'+name]=d
        return k
    return dict(seconds=6,keys=_dense(k,240),cam_az=34,cam_el=20,props=REPAIR_MAT)

def double_leg_kick():
    def lift_progress(t):
        if t < .55: return 0.0
        if t < .72:
            x=(t-.55)/.17; return x*x*(3-2*x)
        if t <= .87: return 1.0
        x=(1.0-t)/.13; x=max(0.0,min(1.0,x)); return x*x*(3-2*x)

    def kick_pulse(t):
        # Three distinct heel-to-butt flexions; knees remain on the mat.
        q=0.0
        for center in (.075,.205,.335):
            x=abs(t-center)/.040
            if x < 1.0: q=max(q,.5+.5*math.cos(math.pi*x))
        return q

    def pose(t):
        # Show both head sides in one seamless loop: three kicks, chest lift,
        # lower and turn, then repeat on the other cheek.
        cycle=(2.0*t)%1.0
        side=1.0 if t<.5 else -1.0
        lift=lift_progress(cycle)
        x=max(0.0,min(1.0,(cycle-.86)/.14));head_swap=x*x*(3-2*x)
        head_turn=side*75.0*(1.0-2.0*head_swap)
        p=90.0-20.0*lift
        P=[0.0,0.0,.11] # sacrum/pelvis remains planted throughout
        theta=math.radians(180.0-90.0*kick_pulse(cycle)) if lift < .5 else math.pi
        u1,l1=trunk_frame(p)
        ankles=[]; knees=[]; feet=[]
        for sgn in (-1,1):
            hip=[P[i]-.03*u1[i]+sgn*.09*l1[i] for i in range(3)]
            if lift < .5:
                # Pick the floor kneecap point on the .44 m thigh circle. Rotate the .43 m
                # shin about that fixed point, so only the lower leg moves during each kick.
                kz=.08
                dz=kz-hip[2]
                kx=hip[0]-math.sqrt(max(.44*.44-dz*dz,0.0))
                knee=[kx,hip[1],kz]
                ankle=[kx+.43*math.cos(theta),hip[1],kz+.43*math.sin(theta)]
                bend=180.0-math.degrees(theta)
                footang=math.radians(180.0-2.0*bend)
                foot=[math.cos(footang),0.0,math.sin(footang)]
            else:
                # Chest-lift section: both legs extend to a constant 0.8695 m hip radius.
                knee=[0.0,0.0,1.0]
                ankle=[hip[0]-.8695,hip[1],hip[2]]
                foot=[-1.0,0.0,0.0]
            ankles.append(ankle); knees.append(knee); feet.append(foot)

        # During the kicks, clasp the hands together over the upper back. As the chest rises,
        # keep the wrists behind the torso and extend both arms toward the feet.
        wrists=[[
            .34+(-.07-.34)*lift,
            sgn*(.035+(.12-.035)*lift),
            .20+(.12-.20)*lift
        ] for sgn in (-1,1)]
        hands=[]
        for sgn in (-1.0,1.0):
            # Blend from clasping inward behind the back to pointing toward the feet.
            d=[-lift,-sgn*(1.0-lift),0.0]
            n=math.sqrt(sum(x*x for x in d))
            hands.append([x/n for x in d])
        arms=[[0.0,-1.0,-.25],[0.0,1.0,-.25]]
        k=_key(P,p,p,[ankles[0],ankles[1]],[wrists[0],wrists[1]],hp=0,
               thoracic_twist=0,head_turn=head_turn,head_detail=1,
               knees=[[0,0,-1],[0,0,-1]],arms=arms,hands=hands,feetdir=feet)
        k['grip_r']=k['grip_l']=.98*(1.0-lift)+.1*lift
        k['palm_r']=[0,0,1]; k['palm_l']=[0,0,1]
        return k

    keys=[(i/288,pose(i/288)) for i in range(289)]
    return dict(seconds=8.0,keys=keys,cam_az=25,cam_el=22,props=REPAIR_MAT)

def mermaid():
    # Side-sit with both feet on +Y, knees folded toward -Y. The right (camera-near)
    # knee-side arm arcs over the head; the left hand bears on the mat opposite the knees.
    # Fixed yaw=90 turns pitch into a broad side bend; the rig cannot isolate lateral spine motion.
    def key(bend):
        p2=30+25*bend
        ankles=[[-.15,.30,.08],[-.40,.30,.08]]
        # right arm reaches overhead and across the head; left wrist stays planted on floor.
        wrists=[[0,.62,.75],[-.04,.25,.035]]
        return _key([0,0,.11],0,p2,ankles,wrists,yaw=90,
            knees=[[0,-1,0],[0,-1,0]],
            feetdir=[[0,1,0],[0,1,0]],
            arms=[[0,0,1],[0,0,-1]],
            hands=[[0,1,0],[1,0,0]])
    def bend_at(t):
        # Ease from a held side arc into a deeper reach, hold, then return.
        if t < .18: return 0.0
        if t < .48:
            u=(t-.18)/.30
            return u*u*(3-2*u)
        if t < .70: return 1.0
        if t < .94:
            u=(t-.70)/.24
            return 1-u*u*(3-2*u)
        return 0.0
    keys=[(i/96,key(bend_at(i/96))) for i in range(97)]
    return dict(seconds=4,keys=keys,cam_az=35,cam_el=22,props=MAT)

def _pm_floor_arms(k):
    from poses import solve
    j=solve(k,{})
    for side,sg in [('r',-1),('l',1)]:
        sh=j['shoulder_'+side]; dz=sh[2]-.04;dx=math.sqrt(.30**2-dz**2)
        e=[sh[0]+dx,sh[1],.04]
        k['wrist_'+side]=[e[0]+.27,e[1],.04]; k['elbow_'+side]=[e[i]-sh[i] for i in range(3)]
        k['hand_'+side]=[1,0,0];k['palm_'+side]=[0,0,-1]
    return k

def _pelvis_contact_pose(s, p1_end=-145.0, p2_end=-115.0, hp_end=50.0):
    p1, p2 = 20.0+(p1_end-20.0)*s, 25.0+(p2_end-25.0)*s
    u1, _ = trunk_frame(p1); u2, _ = trunk_frame(p2)
    spine_z = .24*u1[2]
    chest_z = spine_z + .26*u2[2]
    pz = .01 - min(-.09, spine_z-.09, chest_z-.10)
    P = [-.04*math.sin(math.pi*s), 0.0, pz]
    return P, p1, p2, hp_end*s

def _shoulders(P, p1, p2):
    u1,l1 = trunk_frame(p1); u2,l2 = trunk_frame(p2)
    chest=[P[i]+.24*u1[i]+.26*u2[i] for i in range(3)]
    return [[chest[i]-.03*u2[i]+sgn*.18*l2[i] for i in range(3)] for sgn in (-1,1)]

def _contact_wrist(shoulder, ankle):
    v=[ankle[i]-shoulder[i] for i in range(3)]; n=math.sqrt(sum(x*x for x in v))
    d=[x/n for x in v] if n else [1,0,0]
    return [ankle[i]-.085*d[i] for i in range(3)], d

def _pm_grip(k, target):
    j=solve(k,{})
    for side,sg in [('r',-1),('l',1)]:
        c=[target[0],target[1]+sg*.04,target[2]];sh=j['shoulder_'+side]
        v=[c[i]-sh[i] for i in range(3)];n=math.sqrt(sum(x*x for x in v));d=[x/n for x in v]
        k['wrist_'+side]=[c[i]-.085*d[i] for i in range(3)];k['hand_'+side]=d;k['palm_'+side]=[0,sg,0];k['elbow_'+side]=[0,sg,0];k['grip_'+side]=.7
    return k

EXERCISES={
 'pilates-roll-up':roll_up,'pilates-rolling-like-a-ball':rolling_like_a_ball,
 'pilates-single-leg-circle':single_leg_circle,'pilates-single-leg-stretch':single_leg_stretch,
 'pilates-double-leg-stretch':double_leg_stretch,'pilates-scissors':scissors,
 'pilates-spine-stretch-forward':spine_stretch_forward,'pilates-open-leg-rocker':open_leg_rocker,
 'pilates-corkscrew':corkscrew,'pilates-saw':saw,'pilates-roll-over':roll_over,
 'pilates-seal':seal,'pilates-spine-twist':spine_twist,'pilates-double-leg-kick':double_leg_kick,
 'pilates-mermaid':mermaid,
}
