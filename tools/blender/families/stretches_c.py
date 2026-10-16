"""R8 stretch poses: grounded holds and clearly differentiated seated twists."""
from poses import base_pose, pt, rep, solve


def _standing(**kw):
    fields=dict(pelvis=[0, 0, .975], p1=0, p2=0, hp=0,
        ankle_l=[0,.10,.08], ankle_r=[0,-.10,.08], foot_l=[1,0,0], foot_r=[1,0,0])
    fields.update(kw)
    return base_pose(**fields)


def overhead_arm_pull():
    # Right elbow is high overhead; compute the left hand target from the solved right upper arm.
    def k(p1, p2, yaw, grip_z):
        pose = _standing(p1=p1, p2=p2, yaw=yaw,
            wrist_r=[-.38,-.18,1.39], elbow_r=[0,0,1], hand_r=[0,0,-1],
            wrist_l=[-.08,-.20,grip_z], elbow_l=[0,0,1], hand_l=[0,-1,0])
        joints = solve(pose, {})
        shoulder, elbow = joints["shoulder_r"], joints["elbow_r"]
        upper = [shoulder[i] - elbow[i] for i in range(3)]
        length = sum(v*v for v in upper) ** 0.5
        contact = [elbow[i] + .04 * upper[i] / length for i in range(3)]
        # Hand center lands exactly on the upper arm contact point.
        pose["wrist_l"] = [contact[i] - .085 * pose["hand_l"][i] for i in range(3)]
        return pose
    return dict(seconds=4, keys=rep(k(0,0,0,1.69),k(4,10,-8,1.68)), cam_az=-35, cam_el=16, props=[])


def downward_dog():
    from families.neck_knee_warmup import _hip_raise_supported
    k=_hip_raise_supported(48)
    return dict(seconds=3,keys=rep(k,k),cam_az=30,cam_el=20,props=[])


def groin():
    # Lateral lunge: pelvis shifts toward the camera-side/right foot while the left leg stays long.
    def k(py,pz,left_y):
        return base_pose(pelvis=[0,py,pz],p1=0,p2=0,hp=0,
          ankle_r=[0,-.50,.08],ankle_l=[0,left_y,.08], foot_r=[1,0,0],foot_l=[1,0,0],
          knee_r=[0,0,1],knee_l=[1,0,0], wrist_r_rel=[.18,-.18,-.40],wrist_l_rel=[.18,.18,-.40],
          elbow_r=[0,0,-1],elbow_l=[0,0,-1])
    return dict(seconds=3,keys=rep(k(-.15,.55,.69),k(-.15,.55,.69)),cam_az=35,cam_el=12,props=[])


def shoulder_er():
    frame=dict(kind="box",center=[0,-.545,1.2],size=[1.6,.05,2.4])
    def k(yaw):
        return _standing(yaw=yaw,pelvis=[0,0,.975],wrist_r=[0,-.50,1.715],elbow_r=[0,-1,0],hand_r=[0,0,1],palm_r=[0,-1,0],
                         wrist_l_rel=[.15,.1,-.35],elbow_l=[1,0,-1])
    return dict(seconds=3,keys=rep(k(0),k(8)),cam_az=65,cam_el=12,props=[frame])


def wrist_prayer():
    def k(z):
        y=-.018
        return _standing(wrist_r=[.19,y,z],wrist_l=[.19,-y,z],hand_r=[0,0,1],hand_l=[0,0,1],
          palm_r=[0,1,0],palm_l=[0,-1,0],elbow_r=[0,-1,0],elbow_l=[0,1,0])
    return dict(seconds=3,keys=rep(k(1.34),k(1.20)),cam_az=35,cam_el=12,props=[])


def wrist_reverse_prayer():
    def k(z):
        y=-.018
        return _standing(wrist_r=[.19,y,z],wrist_l=[.19,-y,z],hand_r=[0,0,-1],hand_l=[0,0,-1],
          palm_r=[0,-1,0],palm_l=[0,1,0],elbow_r=[0,-1,0],elbow_l=[0,1,0])
    return dict(seconds=3,keys=rep(k(1.20),k(1.37)),cam_az=-35,cam_el=12,props=[])


def spiderman():
    import math
    def k(z):
        # Long rear leg; lead foot lies outside the same-side palm.
        rearx=-.11-math.sqrt(.8695**2-(z-.08)**2-.01**2)
        return base_pose(pelvis=[-.08,0,z],p1=90,p2=90,hp=0,
          ankle_r=[.58,-.42,.08],ankle_l=[rearx,.10,.08],foot_r=[1,0,0],foot_l=[-1,0,0],
          knee_r=[1,0,0],knee_l=[0,0,-1],wrist_r=[.42,-.25,.035],wrist_l=[.42,.22,.035],
          hand_r=[1,0,0],hand_l=[1,0,0],palm_r=[0,0,-1],palm_l=[0,0,-1],elbow_r=[-1,0,0],elbow_l=[-1,0,0])
    return dict(seconds=3,keys=rep(k(.48),k(.45)),cam_az=35,cam_el=22,props=[])


def neck_side():
    chair=dict(kind="chair",x=-.25,seat=.45)
    # Static seated setup only: independent head sidebend and yaw are unavailable.
    def k(y):
        return base_pose(pelvis=[0,0,.48],p1=0,p2=0,hp=0,
          ankle_l=[.35,.10,.08],ankle_r=[.35,-.10,.08],foot_l=[1,0,0],foot_r=[1,0,0],
          wrist_r=[-.25,-.20,.47],elbow_r=[0,-1,0],wrist_l_rel=[.15,.15,.15],elbow_l=[0,1,0],yaw=y)
    return dict(seconds=3,keys=rep(k(0),k(0)),cam_az=35,cam_el=12,props=[chair])


def sphinx():
    import math
    def k(chestz):
        p1=82
        p2=math.degrees(math.acos((chestz-.11-.24*math.cos(math.radians(p1)))/.23))
        k=base_pose(pelvis=[0,0,.11],p1=p1,p2=p2,hp=0,
          ankle_l=[-.899,.10,.08],ankle_r=[-.899,-.10,.08],knee_l=[0,0,1],knee_r=[0,0,1],foot_l=[-1,0,0],foot_r=[-1,0,0])
        from poses import solve
        j=solve(k,[])
        for s,sg in [('r',-1),('l',1)]:
            sh=j['shoulder_'+s]; dz=sh[2]-.04
            dx=math.sqrt(.30**2-dz**2);e=[sh[0]+dx,sh[1],.04]
            k['wrist_'+s]=[e[0]+.27,e[1],.04];k['elbow_'+s]=[e[i]-sh[i] for i in range(3)]
            k['hand_'+s]=[1,0,0];k['palm_'+s]=[0,0,-1]
        return k
    return dict(seconds=3,keys=[(i/96,k(.30+.035*(1-math.cos(2*math.pi*i/96))/2)) for i in range(97)],cam_az=25,cam_el=18,props=[])


def knee_to_chest():
    from poses import solve
    def k(ax):
        k=base_pose(pelvis=[0,0,.11],p1=-90,p2=-90,hp=0,
          ankle_l=[.899,.10,.08],ankle_r=[ax,-.10,.42],knee_l=[0,0,1],knee_r=[-1,0,1],foot_l=[1,0,0],foot_r=[1,0,0])
        j=solve(k,[]);q=j['knee_r']
        for s,sg in [('r',-1),('l',1)]:
            k['wrist_'+s]=[q[0]-.06,q[1]+sg*.035,q[2]-.085]
            k['hand_'+s]=[0,0,1];k['palm_'+s]=[1,0,0];k['elbow_'+s]=[0,sg,0];k['grip_'+s]=.6
        return k
    return dict(seconds=3,keys=rep(k(.12),k(.08)),cam_az=25,cam_el=24,props=[])


def figure_four():
    from poses import solve
    def k(pull):
        k=base_pose(pelvis=[0,0,.11],p1=-90,p2=-90,hp=0,
          ankle_l=[.38-pull,.10,.30+pull],ankle_r=[.25,.18,.47],knee_l=[0,0,1],knee_r=[0,-1,0],foot_l=[1,0,0],foot_r=[0,1,0])
        j=solve(k,[]);q=j['knee_l'];k['ankle_r']=[q[0]+.03,q[1]-.06,q[2]+.045]
        q=[(q[i]+j['hip_l'][i])*.5 for i in range(3)]
        for s,sg in [('r',-1),('l',1)]:
            k['wrist_'+s]=[q[0]-.07,q[1]+sg*.045,q[2]-.085]
            k['hand_'+s]=[0,0,1];k['palm_'+s]=[1,0,0];k['elbow_'+s]=[0,sg,0];k['grip_'+s]=.6
        return k
    return dict(seconds=3,keys=rep(k(0),k(.025)),cam_az=35,cam_el=28,props=[])


def split_front():
    import math
    blocks=[dict(kind='box',center=[.10,y,.06],size=[.24,.20,.12]) for y in (-.20,.20)]
    def k(z):
        x=math.sqrt(.8695**2-(z-.03-.08)**2)
        return base_pose(pelvis=[0,0,z],p1=0,p2=0,hp=0,
          ankle_r=[x,-.10,.08],ankle_l=[-x,.10,.08],foot_r=[1,0,0],foot_l=[-1,0,0],knee_r=[0,0,1],knee_l=[0,0,1],
          wrist_r=[.10,-.20,.12],wrist_l=[.10,.20,.12],hand_r=[1,0,0],hand_l=[1,0,0],palm_r=[0,0,-1],palm_l=[0,0,-1],elbow_r=[-1,0,0],elbow_l=[-1,0,0])
    return dict(seconds=3,keys=rep(k(.20),k(.18)),cam_az=30,cam_el=18,props=blocks)


def split_middle():
    import math
    def k(z):
        p=55;hipz=z-.03*math.cos(math.radians(p));hy=math.sqrt(.8695**2-(hipz-.08)**2)+.09
        return base_pose(pelvis=[0,0,z],p1=p,p2=p,hp=-15,
          ankle_r=[-.03*math.sin(math.radians(p)),-hy,.08],ankle_l=[-.03*math.sin(math.radians(p)),hy,.08],foot_r=[0,-1,0],foot_l=[0,1,0],knee_r=[0,0,1],knee_l=[0,0,1],
          wrist_r=[.63,-.20,.035],wrist_l=[.63,.20,.035],hand_r=[1,0,0],hand_l=[1,0,0],palm_r=[0,0,-1],palm_l=[0,0,-1],elbow_r=[-1,0,0],elbow_l=[-1,0,0])
    return dict(seconds=3,keys=rep(k(.20),k(.18)),cam_az=35,cam_el=24,props=[])


def _twist(variant):
    # The fixed rig shares pelvic and thoracic yaw; show distinct leg/arm setups
    # without animating a whole-body turn as if it were isolated spinal rotation.
    k=base_pose(pelvis=[0,0,.11],p1=0,p2=0,yaw=-25,hp=0,
      ankle_r=[.44,.44,.08],ankle_l=[.12,.03,.08],knee_r=[0,0,1],knee_l=[1,1,0],
      foot_r=[1,0,0],foot_l=[-1,0,0])
    if variant=='straight':
        import math
        j=solve(k,{});h=j['hip_l'];k['ankle_l']=[h[0]+math.sqrt(.8695**2-(h[1]-.10)**2),.10,.08]
        k['ankle_r']=[.46,.22,.08];k['knee_l']=[0,0,1]
    j=solve(k,{});props=[]
    k['wrist_r']=[-.16,-.24,.035];k['hand_r']=[-1,0,0];k['palm_r']=[0,0,-1];k['elbow_r']=[-1,-1,0]
    if variant in ('straight','easy'):
        sh=j['shoulder_l'];kn=j['knee_r'];goal=[kn[0]+.025,kn[1]-.045,kn[2]]
        v=[goal[i]-sh[i] for i in range(3)];n=sum(x*x for x in v)**.5;e=[sh[i]+.30*v[i]/n for i in range(3)]
        k['wrist_l']=[e[0],e[1],e[2]+.27];k['elbow_l']=[e[i]-sh[i] for i in range(3)];k['hand_l']=[0,0,1];k['palm_l']=[0,-1,0]
    elif variant=='half':
        sh=j['shoulder_l'];a=k['ankle_r'];c=[a[0]+.08,a[1]-.035,a[2]+.025];v=[c[i]-sh[i] for i in range(3)];n=sum(x*x for x in v)**.5;hd=[x/n for x in v]
        k['wrist_l']=[c[i]-.17*hd[i] for i in range(3)];k['hand_l']=hd;k['elbow_l']=[1,1,0];k['palm_l']=[0,-1,0]
    else:
        # The near arm passes outside the crossed calf; the other reaches around the back.
        k['elbow_l']=[1,1,0];k['elbow_r']=[-1,-1,0]
        k['hand_r']=[0,1,0];k['hand_l']=[0,-1,0];k['palm_r']=[1,0,0];k['palm_l']=[-1,0,0]
        if variant=='full':
            c=[-.14,.03,.28];k['wrist_r']=[c[0],c[1]-.085,c[2]];k['wrist_l']=[c[0],c[1]+.085,c[2]]
        else:
            k['wrist_r']=[-.14,-.185,.28];k['wrist_l']=[-.14,.245,.28]
            props=[dict(kind='rod',a=pt('wrist_r',[0,.085,0]),b=pt('wrist_l',[0,-.085,0]),r=.012)]
        k['grip_l']=k['grip_r']=.7
    return dict(seconds=3,keys=rep(k,k),cam_az=-65,cam_el=25,props=props)


def twist_straight(): return _twist("straight")
def twist_easy(): return _twist("easy")
def twist_half(): return _twist("half")
def twist_threequarter(): return _twist("threequarter")
def twist_full(): return _twist("full")


EXERCISES = {
 "stretch-overhead-arm-pull":overhead_arm_pull,"stretch-downward-dog":downward_dog,
 "stretch-groin":groin,"stretch-shoulder-er":shoulder_er,"stretch-wrist-prayer":wrist_prayer,
 "stretch-wrist-reverse-prayer":wrist_reverse_prayer,"stretch-spiderman":spiderman,
 "stretch-neck-side":neck_side,"stretch-sphinx":sphinx,"stretch-knee-to-chest":knee_to_chest,
 "stretch-figure-four":figure_four,"stretch-split-front":split_front,"stretch-split-middle":split_middle,
 "stretch-twist-straight-leg":twist_straight,"stretch-twist-easy":twist_easy,
 "stretch-twist-half":twist_half,"stretch-twist-three-quarter":twist_threequarter,"stretch-twist-full":twist_full,
}
