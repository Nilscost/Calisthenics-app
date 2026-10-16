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
    chair=dict(kind="chair",x=0,seat=.45)
    # Hold the right side of the seat so that shoulder stays low; tilt the
    # head toward the free shoulder, with a small nose turn and chin pitch.
    def k(tilt, turn, pitch):
        return base_pose(pelvis=[0,0,.53],p1=0,p2=0,hp=pitch,
          ankle_l=[.35,.10,.08],ankle_r=[.35,-.10,.08],foot_l=[1,0,0],foot_r=[1,0,0],
          knee_l=[1,0,0],knee_r=[1,0,0],
          wrist_r=[-.18,-.25,.55],hand_r=[0,0,-1],palm_r=[0,1,0],grip_r=.8,elbow_r=[1,-1,0],
          wrist_l=[.14,.10,.56],hand_l=[0,0,-1],elbow_l=[1,1,0],
          head_side_tilt=tilt,head_turn=turn)
    a=k(0,0,0); b=k(-18,-6,5)
    return dict(seconds=4,keys=rep(a,b,hold_top=.12,down_end=.50,hold_bottom=.12),
                cam_az=35,cam_el=12,props=[chair],
                camera_focus=["head","neck","shoulder_l","shoulder_r","wrist_r","chest"])


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
    # The shoulder girdle turns over a still pelvis. The angle increases with
    # each named level; arm positions then distinguish elbow anchor, calf reach,
    # towel bind, and full clasp. All feet and knees remain in their authored
    # support positions while the twist returns to neutral on each loop.
    import math

    # Show a right-knee cross-leg twist. In this rig positive thoracic_twist
    # turns the chest toward figure-left, so the rightward twist is negative.
    angles = {"straight": -48, "easy": -50, "half": -52, "threequarter": -54, "full": -56}
    def key(twist):
        if variant == "straight":
            # One leg long; the other foot is planted beside the opposite knee.
            ankle_l=[.86,.10,.08]; ankle_r=[.38,.38,.08]
            knee_l=[0,0,1]; knee_r=[-1,1,2]
        else:
            # Fold the left foot beside the opposite buttock, knee on the mat,
            # then cross the right foot beyond that leg.
            ankle_l=[.18,-.18,.08]; ankle_r=[.38,.38,.08]
            knee_l=[2,.5,0]; knee_r=[-1,1,2]
        k=base_pose(pelvis=[0,0,.083],p1=0,p2=0,yaw=0,hp=0,
          ankle_l=ankle_l,ankle_r=ankle_r,knee_l=knee_l,knee_r=knee_r,
          foot_l=[1,0,0],foot_r=[1,0,0],thoracic_twist=twist,head_turn=twist*.35)
        j=solve(k,{})
        props=[]
        # The opposite elbow braces outside the bent knee; half twist slides
        # that hand down the outside of the crossed calf toward the instep.
        if variant in ("straight", "easy", "half"):
            kn=j["knee_r"]
            if variant == "half":
                ank=j["ankle_r"]
                target=[kn[i]*.34+ank[i]*.66 for i in range(3)]
                target[1]+=.01
                k["wrist_l"]=target
                k["hand_l"]=[.15,.35,-.92]; k["palm_l"]=[1,0,0]
            else:
                # Keep the hand just beyond the knee so the solved elbow,
                # rather than the wrist, lands against its outside edge.
                target=[kn[0]+.20,kn[1]+.20,kn[2]]
                k["wrist_l"]=[target[i] for i in range(3)]
                k["hand_l"]=[0,0,1]; k["palm_l"]=[1,0,0]
                k["elbow_l"]=[-1,-1,0]
            k["wrist_r"]=[-.04,-.20,.035]
            k["hand_r"]=[-1,0,0]; k["palm_r"]=[0,0,-1]
            k["elbow_r"]=[-1,-1,0]
        else:
            # The left arm wraps around the outside of the bent right knee;
            # the right arm reaches behind to meet it. The towel bridges the
            # gap at 3/4 level, then the wrists clasp at the full level.
            k["wrist_l"]=[-.20,.23,.22]; k["hand_l"]=[0,-1,0]; k["palm_l"]=[1,0,0]
            k["wrist_r"]=[-.20,-.17,.22]; k["hand_r"]=[0,1,0]; k["palm_r"]=[-1,0,0]
            k["elbow_l"]=[1,0,.5]; k["elbow_r"]=[-1,-1,0]
            k["grip_l"]=k["grip_r"]=.7
            if variant == "threequarter":
                props=[dict(kind="rod",a=pt("wrist_r"),b=pt("wrist_l"),r=.012)]
            else:
                # Close the wrapped front arm to the back arm for a visible clasp.
                k["wrist_r"]=[-.20,.17,.22]
        return k,props

    neutral, _ = key(0)
    turned, props = key(angles[variant])
    return dict(seconds=4,keys=rep(neutral,turned,hold_top=.12,down_end=.50,hold_bottom=.12),
                cam_az=140 if variant in ('straight','easy','half') else -140,cam_el=22,props=props)


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
