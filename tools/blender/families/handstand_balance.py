"""Handstand, headstand, and elbow-lever progressions for R8."""
import math
from poses import base_pose, both, rep, dense

MAT = [dict(kind="box", center=[-0.30, 0, 0.02], size=[0.75, 0.85, 0.04])]
WALL = [dict(kind="box", center=[0.025, 0, 1.2], size=[0.05, 1.6, 2.4])]


def _arms(wx=-0.25, wy=0.24, wz=0.035, elbow=(0, 1, 0), hand=(1, 0, 0), palm=(0, 0, -1)):
    return both(wrist=[wx, wy, wz], elbow=list(elbow), hand=list(hand), palm=list(palm), grip=0.0)


def _inv(pz=1.05, px=-0.22, p=180, ankles=None, wx=-0.22, wy=0.24, elbow=(0, 1, 0), wz=0.035):
    if ankles is None:
        ankles = [[px, 0.02, pz + 0.895], [px, -0.02, pz + 0.895]]
    return base_pose(pelvis=[px, 0, pz], p1=p, p2=p, hp=0,
        ankle_l=ankles[0], ankle_r=ankles[1], knee_l=[1,0,0], knee_r=[1,0,0],
        foot_l=[1,0,0], foot_r=[1,0,0], **_arms(wx, wy, wz, elbow))


def _pike(pelvis, p, wristx, wristz, anklex=-0.95, anklez=0.08, elbow=(-0.5,0.7,1)):
    return base_pose(pelvis=[pelvis,0,p],p1=140,p2=140,hp=0,
      **both(ankle=[anklex,0.10,anklez],foot=[1,0,-0.3],knee=[0,0,1],
             wrist=[wristx,0.22,wristz],elbow=list(elbow),hand=[1,0,0],palm=[0,0,-1],grip=0))


def pike_pushup_hands_elevated():
    seat=.48
    chair=[dict(kind="chair",x=.28,seat=seat)]
    def k(pz,p2,wx):
        return base_pose(pelvis=[-.35,0,pz],p1=18,p2=p2,hp=0,
          **both(ankle=[-.82,.10,.08],foot=[1,0,-.3],knee=[0,0,1],wrist=[wx,.22,seat+.04],elbow=[-.4,.6,1],hand=[1,0,0],palm=[0,0,-1],grip=0))
    a=k(.84,125,.28); b=k(.75,135,.28)
    return dict(seconds=3,keys=rep(a,b),cam_az=30,cam_el=18,props=chair)


def hspu_wall_deficit():
    wx=0.0; wz=.38
    def key(a):
        px=.30*a; pz=1.415-.565*a
        ankle_z=pz+.03+math.sqrt(.8697*.8697-px*px-.085*.085)
        return base_pose(pelvis=[px,0,pz],p1=180,p2=180,hp=0,
          ankle_l=[0,.005,ankle_z],ankle_r=[0,-.005,ankle_z],knee_l=[1,0,0],knee_r=[1,0,0],foot_l=[1,0,0],foot_r=[1,0,0],
          wrist_l=[wx,.18,wz],wrist_r=[wx,-.18,wz],elbow_l=[1,.2,0],elbow_r=[1,-.2,0],hand_l=[1,0,0],hand_r=[1,0,0],palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=0,grip_r=0)
    keyed=rep({"a":0},{"a":1})
    rods=[dict(kind="rod",a=[wx-.16,.18,.345],b=[wx+.16,.18,.345],r=.02),dict(kind="rod",a=[wx-.16,-.18,.345],b=[wx+.16,-.18,.345],r=.02)]
    wall=[dict(kind="box",center=[.025,0,1.2],size=[.05,1.6,2.4])]
    return dict(seconds=3,keys=dense(key,keyed,48),cam_az=-30,cam_el=15,props=wall+rods)


def hspu_free_head():
    def key(pz,p):
        return _inv(pz,0,p,[[0,.005,pz+.895],[0,-.005,pz+.895]],0,.18,(1,.2,0))
    return dict(seconds=3,keys=rep(key(1.07,180),key(.84,180)),cam_az=30,cam_el=18,props=MAT)


def hspu_free():
    def key(a):
        px=.30*a; pz=1.415-.565*a
        return base_pose(pelvis=[px,0,pz],p1=180,p2=180,hp=0,
          ankle_l_rel=[0,0,.8697],ankle_r_rel=[0,0,.8697],knee_l=[1,0,0],knee_r=[1,0,0],foot_l=[1,0,0],foot_r=[1,0,0],
          wrist_l=[0,.18,.38],wrist_r=[0,-.18,.38],elbow_l=[1,.2,0],elbow_r=[1,-.2,0],hand_l=[1,0,0],hand_r=[1,0,0],palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=0,grip_r=0)
    rods=[dict(kind="rod",a=[-.16,.18,.345],b=[.16,.18,.345],r=.02),dict(kind="rod",a=[-.16,-.18,.345],b=[.16,-.18,.345],r=.02)]
    return dict(seconds=3,keys=dense(key,rep({"a":0},{"a":1}),48),cam_az=30,cam_el=18,props=rods)


def handstand_wall_plank():
    # Straight body line from feet at hip-height on the wall to hands on the floor.
    p=-120; ux=math.sin(math.radians(p)); uz=math.cos(math.radians(p))
    pelvis=[.90*ux,0,1.02+.90*uz]
    shoulder_x=pelvis[0]+.47*ux; shoulder_z=pelvis[2]+.47*uz
    wx=shoulder_x-math.sqrt(.565**2-(shoulder_z-.035)**2)
    def key(dz):
        return base_pose(pelvis=[pelvis[0],0,pelvis[2]+dz],p1=p,p2=p,hp=0,
          ankle_l=[0,.09,1.02],ankle_r=[0,-.09,1.02],knee_l=[0,1,0],knee_r=[0,-1,0],foot_l=[1,0,0],foot_r=[1,0,0],
          wrist_l=[wx,.18,.035],wrist_r=[wx,-.18,.035],elbow_l=[0,1,0],elbow_r=[0,-1,0],hand_l=[-1,0,0],hand_r=[-1,0,0],palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=0,grip_r=0)
    wall=[dict(kind="box",center=[.04,0,1.2],size=[.05,1.6,2.4])]
    return dict(seconds=3,keys=rep(key(0),key(.005)),cam_az=-30,cam_el=12,props=wall)


def handstand_wall_walk_up():
    wall=[dict(kind="box",center=[.025,0,1.2],size=[.05,1.6,2.4])]
    def k(t):
        p=-120-60*t; r=math.radians(p); ux=math.sin(r); uz=math.cos(r)
        az=1.02+.945*t
        px=.90*ux; pz=az+.90*uz
        sx=px+.47*ux; sz=pz+.47*uz
        dz=sz-.035; dx=math.sqrt(max(.565**2-dz**2,.01))
        wx=sx-dx
        return base_pose(pelvis=[px,0,pz],p1=p,p2=p,hp=0,
          ankle_l=[0,.09,az],ankle_r=[0,-.09,az],knee_l=[0,1,0],knee_r=[0,-1,0],foot_l=[1,0,0],foot_r=[1,0,0],
          wrist_l=[wx,.18,.035],wrist_r=[wx,-.18,.035],elbow_l=[0,1,0],elbow_r=[0,-1,0],hand_l=[-1,0,0],hand_r=[-1,0,0],palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=0,grip_r=0)
    keys=[(i/48,k(i/48)) for i in range(48)]
    return dict(seconds=5,keys=keys,cam_az=-30,cam_el=12,props=wall)


def handstand_toe_pulls():
    a=_inv(1.07,-.22,180,[[-.22,.095,1.965],[-.22,-.095,1.965]],-.22,.18)
    b=_inv(1.07,-.22,180,[[-.33,.005,1.94],[-.33,-.005,1.94]],-.22,.18)
    c=_inv(1.07,-.22,180,[[-.22,.095,1.965],[-.22,-.095,1.965]],-.22,.18)
    return dict(seconds=3,keys=[(0,a),(.4,b),(.55,b),(.95,c)],cam_az=-30,cam_el=12,props=WALL)


def handstand_wall_one_foot():
    a=_inv(1.07,-.22,180,[[-.22,.095,1.965],[-.22,-.095,1.965]],-.22,.18)
    b=_inv(1.07,-.22,180,[[-.22,.095,1.97],[-.245,-.095,1.965]],-.22,.18)
    c=_inv(1.07,-.22,180,[[-.27,.095,1.965],[-.22,-.095,1.965]],-.22,.18)
    d=_inv(1.07,-.22,180,[[-.22,.095,1.965],[-.22,-.095,1.965]],-.22,.18)
    return dict(seconds=3,keys=[(0,a),(.25,b),(.5,c),(.75,d)],cam_az=-30,cam_el=12,props=WALL)


def handstand_kick_up():
    mat=MAT
    def k(pel,pitch,ankles,wx=-.20):
        return base_pose(pelvis=list(pel),p1=pitch,p2=pitch,hp=0,
          ankle_l=list(ankles[0]),ankle_r=list(ankles[1]),knee_l=[1,0,0],knee_r=[1,0,0],foot_l=[1,0,0],foot_r=[1,0,0],**_arms(wx,.18,.035,(0,1,0)))
    start=k([-.42,0,.70],130,[[-.86,.08,.08],[-.16,-.08,.08]])
    rise=k([-.34,0,.82],150,[[-.52,.08,.60],[-.18,-.08,1.10]])
    top=k([-.22,0,1.07],180,[[-.22,.095,1.965],[-.22,-.095,1.965]])
    keys=[(0,start),(.16,start),(.32,rise),(.55,top),(.72,rise),(.92,start)]
    return dict(seconds=4,keys=keys,cam_az=30,cam_el=16,props=mat)


def handstand_free():
    def key(pz):
        return base_pose(pelvis=[0,0,pz],p1=180,p2=180,hp=0,
          ankle_l_rel=[0,.005,.8697],ankle_r_rel=[0,-.005,.8697],knee_l=[1,0,0],knee_r=[1,0,0],foot_l=[1,0,0],foot_r=[1,0,0],
          wrist_l=[0,.18,.035],wrist_r=[0,-.18,.035],elbow_l=[0,1,0],elbow_r=[0,-1,0],hand_l=[1,0,0],hand_r=[1,0,0],palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=0,grip_r=0)
    a=key(1.07); b=key(1.075); c=key(1.07)
    return dict(seconds=3,keys=[(0,a),(.45,b),(.55,b),(.95,c)],cam_az=30,cam_el=18,props=MAT)


def handstand_press_wall_negative():
    wall=[dict(kind="box",center=[-.85,0,1.2],size=[.05,1.6,2.4])]
    def k(t):
        p=180-112*t; r=math.radians(p); ux=math.sin(r); uz=math.cos(r)
        wx=-.22; wz=.035; dz=.565
        px=wx-.47*ux; pz=wz+dz-.47*uz
        ax=px-.90*ux; az=pz-.90*uz
        return base_pose(pelvis=[px,0,pz],p1=p,p2=p,hp=0,
          ankle_l=[ax,.15,az],ankle_r=[ax,-.15,az],knee_l=[0,1,0],knee_r=[0,-1,0],foot_l=[1,0,0],foot_r=[1,0,0],
          wrist_l=[wx,.18,wz],wrist_r=[wx,-.18,wz],elbow_l=[0,1,0],elbow_r=[0,-1,0],hand_l=[-1,0,0],hand_r=[-1,0,0],palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=0,grip_r=0)
    keys=[(i/48,k(i/48)) for i in range(48)]
    return dict(seconds=4,keys=keys,cam_az=30,cam_el=18,props=wall)


def handstand_press_elevated():
    box=[dict(kind="box",center=[-1.30,0,.22],size=[.80,.9,.44])]
    wrist_x=-.25
    def k(t):
        p2=180
        spread=.30*(1-t)+.005*t
        leg_xz=math.sqrt(.899*.899-spread*spread)
        dz=.53+.035*t; reach=.565
        # Keep the elbows nearly straight while the shoulders lead the press.
        p0=math.degrees(math.acos(max(-1,min(1,(.565-.52+.23)/(math.sqrt(.899*.899-.30*.30)+.24)))))
        p1=p0+(180-p0)*t
        dx=math.sqrt(max(reach*reach-dz*dz,0))
        shoulder=[wrist_x+dx,0,.035+dz]
        u1=[math.sin(math.radians(p1)),0,math.cos(math.radians(p1))]
        u2=[0,0,-1]
        pelvis=[shoulder[0]-.24*u1[0]-.23*u2[0],0,shoulder[2]-.24*u1[2]-.23*u2[2]]
        ax=pelvis[0]-leg_xz*u1[0] # straight legs, with lateral straddle included in reach
        az=pelvis[2]-leg_xz*u1[2]
        return base_pose(pelvis=pelvis,p1=p1,p2=p2,hp=0,
          ankle_l=[ax, .09+spread, az],ankle_r=[ax,-.09-spread,az],
          knee_l=[0,0,1],knee_r=[0,0,1],foot_l=[1,0,0],foot_r=[1,0,0],
          wrist_l=[wrist_x,.18,.035],wrist_r=[wrist_x,-.18,.035],elbow_l=[0,1,0],elbow_r=[0,-1,0],hand_l=[1,0,0],hand_r=[1,0,0],palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=0,grip_r=0)
    keys=[(i/48,k(i/48)) for i in range(49)]
    return dict(seconds=4,keys=keys,cam_az=30,cam_el=18,props=box)


def handstand_press_straddle():
    wrist_x=-.55
    def k(t):
        p2=180
        spread=.30*(1-t)+.005*t
        leg_xz=math.sqrt(.899*.899-spread*spread)
        dz=.53+.035*t; reach=.565
        p0=math.degrees(math.acos(max(-1,min(1,(.565-.08+.23)/(math.sqrt(.899*.899-.30*.30)+.24)))))
        p1=p0+(180-p0)*t
        dx=math.sqrt(max(reach*reach-dz*dz,0))
        shoulder=[wrist_x+dx,0,.035+dz]
        u1=[math.sin(math.radians(p1)),0,math.cos(math.radians(p1))]
        u2=[0,0,-1]
        pelvis=[shoulder[0]-.24*u1[0]-.23*u2[0],0,shoulder[2]-.24*u1[2]-.23*u2[2]]
        ax=pelvis[0]-leg_xz*u1[0]; az=pelvis[2]-leg_xz*u1[2]
        return base_pose(pelvis=pelvis,p1=p1,p2=p2,hp=0,
          ankle_l=[ax,.09+spread,az],ankle_r=[ax,-.09-spread,az],
          knee_l=[0,0,1],knee_r=[0,0,1],foot_l=[1,0,0],foot_r=[1,0,0],
          wrist_l=[wrist_x,.18,.035],wrist_r=[wrist_x,-.18,.035],elbow_l=[0,1,0],elbow_r=[0,-1,0],hand_l=[1,0,0],hand_r=[1,0,0],palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=0,grip_r=0)
    keys=[(i/48,k(i/48)) for i in range(49)]
    return dict(seconds=4,keys=keys,cam_az=30,cam_el=18,props=[])


def handstand_press_bent_arm():
    def k(t):
        pz=.43+.64*t; pitch=90+90*t; px=-.40+.20*t
        return base_pose(pelvis=[px,0,pz],p1=pitch,p2=pitch,hp=0,
          ankle_l_rel=[-.22,.10,-.38*(1-t)+.87*t],ankle_r_rel=[-.22,-.10,-.38*(1-t)+.87*t],knee_l=[1,0,0],knee_r=[1,0,0],foot_l=[1,0,0],foot_r=[1,0,0],**_arms(-.20,.18,.035,(1,0,0)))
    keys=[(i/32,k(i/32)) for i in range(32)]
    return dict(seconds=4,keys=keys,cam_az=30,cam_el=18,props=[])


def _headstand(pz=.94,pitch=180,ankle_rel=None,wrists=None,elbows=(1,.5,0)):
    if ankle_rel is None: ankle_rel=[[0,.06,.35],[0,-.06,.35]]
    if wrists is None: wrists=[[-.36,.23,.035],[-.36,-.23,.035]]
    return base_pose(pelvis=[-.28,0,pz-.19],p1=pitch,p2=pitch,hp=0,
      ankle_l_rel=list(ankle_rel[0]),ankle_r_rel=list(ankle_rel[1]),knee_l=[1,0,0],knee_r=[1,0,0],foot_l=[1,0,0],foot_r=[1,0,0],
      wrist_l=list(wrists[0]),wrist_r=list(wrists[1]),elbow_l=list(elbows),elbow_r=[elbows[0],-elbows[1],elbows[2]],hand_l=[1,0,0],hand_r=[1,0,0],palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=0,grip_r=0)


def headstand_tripod_tuck():
    def key(pz):
        return base_pose(pelvis=[-.28,0,pz],p1=130,p2=180,hp=0,
          ankle_l_rel=[-.05,.14,.02],ankle_r_rel=[-.05,-.14,.02],knee_l=[-.1,0,-1],knee_r=[-.1,0,-1],foot_l=[1,0,0],foot_r=[1,0,0],
          wrist_l=[-.36,.23,.035],wrist_r=[-.36,-.23,.035],elbow_l=[0,0,1],elbow_r=[0,0,1],hand_l=[1,0,0],hand_r=[1,0,0],palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=0,grip_r=0)
    return dict(seconds=3,keys=rep(key(.723269),key(.729269)),cam_az=30,cam_el=18,props=MAT)


def headstand_wall():
    wall=[dict(kind="box",center=[.04,0,1.2],size=[.05,1.6,2.4])]
    a=_headstand(1.00,180,[[.08,.08,.85],[.08,-.08,.85]],[[-.36,.23,.035],[-.36,-.23,.035]])
    b=_headstand(1.04,180,[[.08,.08,.85],[.08,-.08,.85]],[[-.36,.23,.035],[-.36,-.23,.035]])
    return dict(seconds=3,keys=rep(a,b),cam_az=-30,cam_el=18,props=wall+MAT)


def headstand_tripod():
    a=_headstand(.99,180,[[0,.07,.35],[0,-.07,.35]],[[-.36,.23,.035],[-.36,-.23,.035]])
    b=_headstand(1.04,180,[[0,.07,.80],[0,-.07,.80]],[[-.36,.23,.035],[-.36,-.23,.035]])
    return dict(seconds=4,keys=rep(b,a),cam_az=30,cam_el=18,props=MAT)


def headstand_forearm():
    legz=math.sqrt(.869*.869-.08*.08)
    a=_headstand(1.00,180,[[0,.08,legz],[0,-.08,legz]],[[-.50,.18,.035],[-.50,-.18,.035]],(1,.2,0))
    b=_headstand(1.01,180,[[0,.08,legz],[0,-.08,legz]],[[-.50,.18,.035],[-.50,-.18,.035]],(1,.2,0))
    return dict(seconds=3,keys=rep(a,b),cam_az=30,cam_el=18,props=MAT)


def elbow_lever_umbrella():
    def k(pz):
        return base_pose(pelvis=[-.02,0,pz],p1=65,p2=65,hp=0,
          ankle_l=[-.48,.08,.08],ankle_r=[-.48,-.08,.08],knee_l=[0,0,1],knee_r=[0,0,1],foot_l=[1,0,0],foot_r=[1,0,0],
          wrist_l=[.40,.10,.035],wrist_r=[.40,-.10,.035],elbow_l=[0,.4,1],elbow_r=[0,-.4,1],hand_l=[-1,0,0],hand_r=[-1,0,0],palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=0,grip_r=0)
    return dict(seconds=3,keys=rep(k(.28),k(.30)),cam_az=30,cam_el=18,props=[])


def _lever(pz,ankles,hippitch=90,wrx=.23,wy=.18):
    return base_pose(pelvis=[0,0,pz-.12],p1=hippitch,p2=hippitch,hp=0,
      ankle_l_rel=list(ankles[0]),ankle_r_rel=list(ankles[1]),knee_l=[0,0,1],knee_r=[0,0,1],foot_l=[1,0,0],foot_r=[1,0,0],
      wrist_l=[wrx,wy,.035],wrist_r=[wrx,-wy,.035],elbow_l=[-1,0,0],elbow_r=[-1,0,0],hand_l=[1,0,0],hand_r=[1,0,0],palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=0,grip_r=0)


def elbow_lever_straddle():
    legx=math.sqrt(.8697*.8697-.30*.30)
    a=_lever(.42,[[-legx,.30,0],[-legx,-.30,0]],90)
    b=_lever(.43,[[-legx,.30,0],[-legx,-.30,0]],90)
    return dict(seconds=3,keys=rep(a,b),cam_az=30,cam_el=18,props=[])


def elbow_lever():
    a=_lever(.42,[[-.8697,.005,0],[-.8697,-.005,0]],90)
    b=_lever(.43,[[-.8697,.005,0],[-.8697,-.005,0]],90)
    return dict(seconds=3,keys=rep(a,b),cam_az=30,cam_el=18,props=[])


EXERCISES={
 'pike-pushup-hands-elevated':pike_pushup_hands_elevated,'hspu-wall-deficit':hspu_wall_deficit,'hspu-free-head':hspu_free_head,'hspu-free':hspu_free,
 'handstand-wall-plank':handstand_wall_plank,'handstand-wall-walk-up':handstand_wall_walk_up,'handstand-toe-pulls':handstand_toe_pulls,'handstand-wall-one-foot':handstand_wall_one_foot,
 'handstand-kick-up':handstand_kick_up,'handstand-free':handstand_free,'handstand-press-wall-negative':handstand_press_wall_negative,'handstand-press-elevated':handstand_press_elevated,'handstand-press-straddle':handstand_press_straddle,'handstand-press-bent-arm':handstand_press_bent_arm,
 'headstand-tripod-tuck':headstand_tripod_tuck,'headstand-wall':headstand_wall,'headstand-tripod':headstand_tripod,'headstand-forearm':headstand_forearm,
 'elbow-lever-umbrella':elbow_lever_umbrella,'elbow-lever-straddle':elbow_lever_straddle,'elbow-lever':elbow_lever}
