"""Agility drills, crawling patterns and controlled mat rolls."""

import math
from poses import base_pose, trunk_frame, sample, solve


def _stand(pelvis, ar, al, kr=(1, 0, 0), kl=(1, 0, 0), p1=8, p2=10,
           wr=(0.2, -0.45, 0.0), wl=(0.2, 0.45, 0.0), yaw=0):
    """A low athletic stance. Foot targets are explicit so each step can lift."""
    return base_pose(pelvis=pelvis, p1=p1, p2=p2, yaw=yaw, hp=-5,
                     ankle_r=ar, ankle_l=al, foot_r=[1, 0, 0], foot_l=[1, 0, 0],
                     knee_r=kr, knee_l=kl,
                     wrist_r_rel=wr, wrist_l_rel=wl,
                     elbow_r=[-1, 0, -0.2], elbow_l=[-1, 0, -0.2],
                     hand_r=[1, 0, 0], hand_l=[1, 0, 0])


def lateral_shuffle():
    def k(py,r,l,rz=.08,lz=.08):
        return _stand([0,py,.80],[0,r,rz],[0,l,lz],kr=(1,0,0),kl=(1,0,0),p1=10,p2=12)
    a=k(0,-.10,.10)
    keys=[(0,a),(.12,k(-.10,-.35,.10,.22,.08)),(.24,k(-.15,-.40,.10)),
      (.36,k(-.25,-.40,-.20,.08,.22)),(.46,k(-.30,-.40,-.20)),
      (.60,k(-.20,-.40,.10,.08,.22)),(.72,k(-.15,-.40,.10)),
      (.84,k(-.05,-.10,.10,.22,.08)),(.96,a)]
    return dict(seconds=3.5,keys=keys,cam_az=0,cam_el=12,props=[])


def carioca():
    # A crossover in front and behind, opening with the other foot between crosses.
    R=[0,-.12,.08];L=[0,.12,.08];keys=[]
    def pose(r,l,yaw=0):
        return _stand([0,(r[1]+l[1])/2,.80],r,l,p1=3,p2=4,yaw=yaw,
          wr=(.15,-.42,-.02),wl=(.15,.42,-.02))
    keys.append((0,pose(R,L)))
    steps=[(1,[.15,-.32,.08],8),(0,[0,-.55,.08],0),
      (1,[-.15,-.72,.08],-8),(0,[0,-.95,.08],0),
      (0,[.15,-.50,.08],-8),(1,[0,-.20,.08],0),
      (0,[-.15,0,.08],8),(1,[0,.12,.08],0),
      (0,[0,-.12,.08],0)]
    for n,(side,target,yaw) in enumerate(steps):
        feet=[R[:],L[:]];lift=target[:];lift[2]=.22;feet[side]=lift
        t=.025+n*.105;keys.append((t,pose(feet[0],feet[1],yaw)))
        if side==0:R=target
        else:L=target
        keys.append((t+.055,pose(R,L,yaw)))
    return dict(seconds=5.5,keys=keys,cam_az=0,cam_el=15,props=[])


def shuttle_sprint():
    # A compact indoor shuttle with two actual hand touches and a third acceleration.
    def run(x,side,sign=1):
        support=[x+sign*.05,-.10 if side==0 else .10,.08]
        free=[x-sign*.25,.10 if side==0 else -.10,.28]
        feet=[support,free] if side==0 else [free,support]
        return _agility_pose([x,0,.90],18,feet,yaw=0 if sign==1 else 180,lead=1 if side==0 else -1)
    def touch(x,sign):
        return _agility_pose([x-sign*.30,0,.35],80,[[x,-.10,.08],[x,.10,.08]],yaw=0 if sign==1 else 180,touch=True)
    keys=[(0,run(-.60,0)),(.07,run(-.30,1)),(.14,run(0,0)),(.21,run(.30,1)),
      (.28,touch(.40,1)),(.32,_agility_pose([.15,0,.60],35,[[.25,-.1,.08],[.40,.1,.08]],yaw=90)),
      (.38,run(.20,0,-1)),(.45,run(-.10,1,-1)),(.52,run(-.40,0,-1)),(.59,touch(-.60,-1)),
      (.605,_agility_pose([-.30,0,.35],80,[[-.60,-.1,.08],[-.60,.1,.08]],yaw=180)),
      (.64,_agility_pose([-.30,0,.60],35,[[-.50,-.1,.08],[-.35,.1,.08]],yaw=90)),
      (.70,run(-.30,0)),(.77,run(0,1)),(.84,run(.30,0)),
      (.89,run(.10,1)),(.94,run(-.25,0)),(1,run(-.60,0))]
    lines=[dict(kind='rod',a=[x,-.55,.01],b=[x,.55,.01],r=.008) for x in (-.80,.60)]
    return dict(seconds=6,keys=keys,cam_az=25,cam_el=14,props=lines)


def crouch_run():
    def plank(drive=False):
        a=math.radians(20);P=[-.80+.8998*math.cos(a),0,.08+.8998*math.sin(a)]
        feet=[[-.80,-.1,.08],[-.80,.1,.08]]
        if drive:feet[0]=[.05,-.1,.18]
        k=_agility_pose(P,70,feet,hands=[[.25,-.23,.035],[.25,.23,.035]])
        k['knee_l']=[0,0,1]
        k['knee_r']=[0,0,1]
        k['foot_l']=[.95,0,-.3];k['foot_r']=[1,0,0] if drive else [.95,0,-.3]
        return k
    def run(x,side,sign=1,low=True):
        feet=[[x-sign*.12,-.1,.08],[x+sign*.18,.1,.25]] if side==0 else [[x+sign*.18,-.1,.25],[x-sign*.12,.1,.08]]
        return _agility_pose([x,0,.68 if low else .90],35 if low else 15,feet,yaw=0 if sign==1 else 180,lead=1 if side==0 else -1)
    start=plank();drive=plank(True)
    keys=[(0,start),(.05,drive),(.10,start),(.18,run(0,0)),(.26,run(.25,1)),(.34,run(.50,0)),
      (.40,_agility_pose([.50,0,.65],40,[[.50,-.1,.08],[.65,.1,.08]],yaw=90)),
      (.46,run(.40,1,-1)),(.54,run(.15,0,-1)),(.62,run(-.10,1,-1)),
      (.70,run(-.35,0,-1,False)),(.76,run(-.60,1,-1,False)),
      (.82,_agility_pose([-.45,0,.65],40,[[-.55,-.1,.08],[-.35,.1,.08]],yaw=90)),
      (.89,_agility_pose([-.25,0,.45],80,[[0,-.1,.08],[0,.1,.08]],hands=[[.25,-.23,.035],[.25,.23,.035]])),
      (.94,dict(start,ankle_r=[-.40,-.1,.25],ankle_l=[-.40,.1,.25])),(1,start)]
    for t,k in keys:
        if t>=.89:k['knee_r']=[0,0,1];k['knee_l']=[0,0,1]
    return dict(seconds=6,keys=keys,cam_az=25,cam_el=18,props=[])


def four_square_hops():
    # Clockwise through four actual quadrants, landing wholly inside each square.
    def k(x,y,air=False):
        return _stand([x,y,1.10 if air else .86],[x,y-.10,.28 if air else .08],
          [x,y+.10,.28 if air else .08],p1=4,p2=5)
    points=[(.25,-.25),(.25,.25),(-.25,.25),(-.25,-.25),(.25,-.25)]
    keys=[(0,k(*points[0]))]
    for n in range(4):
        a,b=points[n:n+2];t=n*.24
        keys.extend([(t+.10,k((a[0]+b[0])/2,(a[1]+b[1])/2,True)),(t+.21,k(*b))])
    cross=[dict(kind='rod',a=[-.6,0,.01],b=[.6,0,.01],r=.007),dict(kind='rod',a=[0,-.6,.01],b=[0,.6,.01],r=.007)]
    return dict(seconds=3.8,keys=keys,cam_az=30,cam_el=24,props=cross)


def hexagon_drill():
    def pose(x,y,lift=0):
        return _stand([x,y,.90+lift],[x,y-.10,.08+lift],[x,y+.10,.08+lift],p1=2,p2=4)
    keys=[(0,pose(0,0))]
    for n in range(6):
        a=math.radians(30+60*n);x=.70*math.cos(a);y=.70*math.sin(a);t=n*.16
        keys.extend([(t+.035,pose(x/2,y/2,.14)),(t+.07,pose(x,y)),
          (t+.105,pose(x/2,y/2,.14)),(t+.14,pose(0,0))])
    vertices=[[.60*math.cos(math.radians(60*n)),.60*math.sin(math.radians(60*n)),.01] for n in range(6)]
    edges=[dict(kind='rod',a=vertices[n],b=vertices[(n+1)%6],r=.007) for n in range(6)]
    return dict(seconds=6.2,keys=keys,cam_az=30,cam_el=24,props=edges)


def bear_plank():
    def k(pz):
        return base_pose(pelvis=[-.12, 0, pz], p1=80, p2=80, hp=-5,
                         ankle_r=[-.43, -.10, .08], ankle_l=[-.43, .10, .08],
                         foot_r=[1, 0, 0], foot_l=[1, 0, 0],
                         # Pole points down: knees tuck beneath the hips, with shins trailing.
                         knee_r=[0, 0, -1], knee_l=[0, 0, -1],
                         wrist_r=[.36, -.18, .035], wrist_l=[.36, .18, .035],
                         elbow_r=[0, 0, -1], elbow_l=[0, 0, -1],
                         hand_r=[1, 0, 0], hand_l=[1, 0, 0],
                         palm_r=[0, 0, -1], palm_l=[0, 0, -1])
    return dict(seconds=3.0, keys=[(0, k(.52)), (.5, k(.525))], cam_az=25, cam_el=24, props=[])


def bear_crawl():
    def k(px,r,l,wr,wl):
        return base_pose(pelvis=[px,0,.515],p1=80,p2=80,hp=-5,
          ankle_r=r,ankle_l=l,knee_r=[0,0,-1],knee_l=[0,0,-1],foot_r=[1,0,0],foot_l=[1,0,0],
          wrist_r=wr,wrist_l=wl,elbow_r=[0,0,-1],elbow_l=[0,0,-1],hand_r=[1,0,0],hand_l=[1,0,0],palm_r=[0,0,-1],palm_l=[0,0,-1])
    a=k(0,[-.46,-.1,.08],[-.46,.1,.08],[.48,-.18,.035],[.48,.18,.035])
    s1=k(.03,[-.46,-.1,.08],[-.34,.1,.16],[.60,-.18,.15],[.48,.18,.035])
    b=k(.06,[-.46,-.1,.08],[-.34,.1,.08],[.60,-.18,.035],[.48,.18,.035])
    s2=k(.09,[-.34,-.1,.16],[-.34,.1,.08],[.60,-.18,.035],[.60,.18,.15])
    c=k(.12,[-.34,-.1,.08],[-.34,.1,.08],[.60,-.18,.035],[.60,.18,.035])
    s3=k(.09,[-.46,-.1,.16],[-.34,.1,.08],[.60,-.18,.035],[.48,.18,.15])
    s4=k(.03,[-.46,-.1,.08],[-.46,.1,.16],[.48,-.18,.15],[.48,.18,.035])
    return dict(seconds=4,keys=[(0,a),(.12,s1),(.24,b),(.36,s2),(.48,c),(.60,s3),(.72,b),(.84,s4),(.96,a)],cam_az=25,cam_el=24,props=[])


def crab_walk():
    # Reverse tabletop: hands under the shoulders, feet ahead of the hips.
    def k(px, wr, wl, ar, al):
        return base_pose(pelvis=[px, 0, .50], p1=-85, p2=-85, hp=0,
                         ankle_r=ar, ankle_l=al, foot_r=[1, 0, 0], foot_l=[1, 0, 0],
                         knee_r=[1, 0, 0], knee_l=[1, 0, 0], wrist_r=wr, wrist_l=wl,
                         elbow_r=[0, 0, -1], elbow_l=[0, 0, -1], hand_r=[1, 0, 0], hand_l=[1, 0, 0],
                         palm_r=[0, 0, -1], palm_l=[0, 0, -1])
    a = k(0, [-.50, -.20, .035], [-.50, .20, .035], [.40, -.1, .08], [.40, .1, .08])
    # Opposite hand/foot pairs take lifted short steps; stance targets remain fixed.
    b = k(0, [-.62, -.20, .14], [-.50, .20, .035], [.40, -.1, .08], [.25, .1, .17])
    bl = k(0, [-.62, -.20, .035], [-.50, .20, .035], [.40, -.1, .08], [.25, .1, .08])
    advance1 = k(-.08, [-.62, -.20, .035], [-.50, .20, .035], [.40, -.1, .08], [.25, .1, .08])
    c = k(-.08, [-.62, -.20, .035], [-.62, .20, .14], [.25, -.1, .17], [.25, .1, .08])
    cl = k(-.08, [-.62, -.20, .035], [-.62, .20, .035], [.25, -.1, .08], [.25, .1, .08])
    advance2 = k(-.16, [-.62, -.20, .035], [-.62, .20, .035], [.25, -.1, .08], [.25, .1, .08])
    back_c = k(-.16, [-.62, -.20, .035], [-.50, .20, .14], [.40, -.1, .17], [.25, .1, .08])
    back_cl = k(-.16, [-.62, -.20, .035], [-.50, .20, .035], [.40, -.1, .08], [.25, .1, .08])
    back_b = k(-.08, [-.50, -.20, .14], [-.50, .20, .035], [.40, -.1, .08], [.40, .1, .17])
    back_bl = k(-.08, [-.50, -.20, .035], [-.50, .20, .035], [.40, -.1, .08], [.40, .1, .08])
    return dict(seconds=4.0, keys=[(0, a), (.10, b), (.17, bl), (.25, advance1),
                                  (.34, c), (.41, cl), (.49, advance2), (.58, back_c),
                                  (.65, back_cl), (.73, back_b), (.80, back_bl), (.88, a)],
                cam_az=25, cam_el=24, props=[])


def inchworm():
    # Contacts walk in separate phases. The legs follow a radius-preserving arc;
    # the trunk hinge is constrained by the planted hands throughout the fold.
    phases=[('hand',[.40,.40,.60,.60,.80,.80,1.05,1.05],0,.30),
      ('foot',[.20,.20,.40,.40,.60,.60,.85,.85],.30,.55),
      ('foot',[.60,.60,.40,.40,.20,.20,0,0],.55,.80),
      ('hand',[.80,.80,.60,.60,.40,.40,.20,.20],.80,1)]
    feet=[0.,0.];hands=[.20,.20];keys=[]
    def pose(f,w,fz,wz):
        origin=(f[0]+f[1])/2
        gap=((w[0]+w[1])/2-origin-.20)/.85
        s=max(0,min(1,gap));alpha=math.radians(90-70*s)
        def leg_ok(r):
            hx=origin+r*math.cos(alpha);hz=.08+r*math.sin(alpha)
            return all(math.hypot(hx-f[i],hz-fz[i])<=.8695 for i in range(2))
        lo,hi=.5,.8695
        for _ in range(24):
            mid=(lo+hi)/2
            if leg_ok(mid):lo=mid
            else:hi=mid
        hip=[origin+lo*math.cos(alpha),0,.08+lo*math.sin(alpha)]
        target=160-90*s**1.8
        candidates=[]
        for j in range(191):
            p=70+j*.5;a=math.radians(p)
            sx=hip[0]+.50*math.sin(a);sz=hip[2]+.50*math.cos(a)
            distances=[math.hypot(sx-w[i],sz-wz[i]) for i in range(2)]
            head_z=hip[2]+.53*math.cos(a)+.199*math.cos(math.radians(p-5))
            if max(distances)<=.569 and head_z>=.145:
                candidates.append((abs(p-target),p))
        assert candidates,'No anatomical inchworm hinge for contact pair'
        pitch=min(candidates)[1];a=math.radians(pitch)
        P=[hip[0]+.03*math.sin(a),0,hip[2]+.03*math.cos(a)]
        return base_pose(pelvis=P,p1=pitch,p2=pitch,hp=-5,
          ankle_r=[f[0],-.10,fz[0]],ankle_l=[f[1],.10,fz[1]],knee_r=[0,0,1],knee_l=[0,0,1],foot_r=[1,0,0],foot_l=[1,0,0],
          wrist_r=[w[0],-.18,wz[0]],wrist_l=[w[1],.18,wz[1]],elbow_r=[0,0,1],elbow_l=[0,0,1],
          hand_r=[1,0,0],hand_l=[1,0,0],palm_r=[0,0,-1],palm_l=[0,0,-1],grip_r=0,grip_l=0)
    keys.append((0,pose(feet,hands,[.08,.08],[.035,.035])))
    for kind,targets,t0,t1 in phases:
        for n,target in enumerate(targets):
            side=n%2;old=(hands if kind=='hand' else feet)[side]
            for j in range(1,9):
                q=j/8;u=q*q*(3-2*q);f=feet[:];w=hands[:];fz=[.08,.08];wz=[.035,.035]
                if kind=='hand':w[side]=old+(target-old)*u;wz[side]+=.11*math.sin(math.pi*q)
                else:f[side]=old+(target-old)*u;fz[side]+=.12*math.sin(math.pi*q)
                time=t0+(t1-t0)*(n+q)/len(targets)
                keys.append((time,pose(f,w,fz,wz)))
            if kind=='hand':hands[side]=target
            else:feet[side]=target
    return dict(seconds=12,keys=keys,cam_az=25,cam_el=20,props=[])


def frogger():
    # Fixed planted hands. Both feet leave the plank, land wide beyond the hands, and return.
    wrists = ([1.05, -.23, .035], [1.05, .23, .035])
    common = dict(wrist_r=wrists[0], wrist_l=wrists[1],
                  elbow_r=[0, 0, -1], elbow_l=[0, 0, -1],
                  hand_r=[1, 0, 0], hand_l=[1, 0, 0],
                  palm_r=[0, 0, -1], palm_l=[0, 0, -1])
    plank_hip = [.8698 * __import__("math").cos(__import__("math").radians(20)), 0,
                 .08 + .8698 * __import__("math").sin(__import__("math").radians(20))]
    plank = base_pose(pelvis=[plank_hip[0] + .03 * __import__("math").sin(__import__("math").radians(70)),
                              0, plank_hip[2] + .03 * __import__("math").cos(__import__("math").radians(70))],
                      p1=70, p2=70, hp=-5,
                      ankle_r=[0, -.1, .08], ankle_l=[0, .1, .08],
                      foot_r=[1, 0, 0], foot_l=[1, 0, 0], knee_r=[1, 0, 0], knee_l=[1, 0, 0], **common)
    flight1 = base_pose(pelvis=plank["pelvis"], p1=70, p2=70, hp=-5,
                        ankle_r=[.18, -.30, .24], ankle_l=[.18, .30, .24],
                        foot_r=[1, 0, -.2], foot_l=[1, 0, -.2],
                        knee_r=[1, 0, 0], knee_l=[1, 0, 0], **common)
    squat = base_pose(pelvis=[.86, 0, .20], p1=90, p2=20, hp=-5,
                      ankle_r=[1.32, -.30, .08], ankle_l=[1.32, .30, .08],
                      foot_r=[1, 0, 0], foot_l=[1, 0, 0],
                      knee_r=[1, 0, 0], knee_l=[1, 0, 0], **common)
    flight2 = base_pose(pelvis=plank["pelvis"], p1=70, p2=70, hp=-5,
                        ankle_r=[.10, -.22, .24], ankle_l=[.10, .22, .24],
                        foot_r=[1, 0, -.2], foot_l=[1, 0, -.2],
                        knee_r=[1, 0, 0], knee_l=[1, 0, 0], **common)
    return dict(seconds=3.0, keys=[(0, plank), (.22, flight1), (.38, squat),
                                  (.52, flight2), (.68, plank)], cam_az=25, cam_el=24, props=[])


def _mat():
    return dict(kind="box", center=[0, 0, -.025], size=[2.4, 1.5, .05])


def shoulder_roll():
    def key(P,p1,p2,feet,hands,*,hp=0,yaw=0,roll=0,knee=(0,0,1)):
        return base_pose(pelvis=P,p1=p1,p2=p2,hp=hp,yaw=yaw,roll=roll,
          ankle_r=feet[0],ankle_l=feet[1],knee_r=list(knee),knee_l=list(knee),
          foot_r=[1,0,0],foot_l=[1,0,0],wrist_r=hands[0],wrist_l=hands[1],
          elbow_r=[0,-1,4],elbow_l=[0,1,4],hand_r=[-1,0,0],hand_l=[1,0,0],
          palm_r=[0,0,-1],palm_l=[0,0,-1],grip_r=0,grip_l=0)
    def anchored(C,p1,p2,feet,hands,*,hp=65,yaw=20,roll=0):
        u1,_=trunk_frame(p1,yaw,roll);u2,_=trunk_frame(p2,yaw,roll)
        P=[C[i]-.24*u1[i]-.26*u2[i] for i in range(3)]
        return key(P,p1,p2,feet,hands,hp=hp,yaw=yaw,roll=roll)
    start=key([-.70,0,.975],0,0,[[-.70,-.10,.08],[-.70,.10,.08]], [[-.55,-.20,1.02],[-.55,.20,1.02]],knee=(1,0,0))
    crouch=anchored([-.15,0,.40],70,85,[[-.40,-.10,.08],[-.40,.10,.08]], [[0,-.22,.035],[0,.22,.035]],hp=10,yaw=0)
    shoulder=anchored([.02,-.10,.31],115,135,[[-.32,-.15,.48],[-.24,.08,.50]], [[.02,-.30,.035],[.15,.10,.035]],roll=35)
    inverted=anchored([.18,0,.29],165,185,[[.10,-.05,.92],[.12,.15,.90]], [[.02,-.30,.035],[.15,.10,.035]],roll=22)
    back=anchored([.35,.10,.11],225,245,[[.53,.10,.67],[.55,.30,.65]], [[.10,-.35,.25],[.05,.55,.25]],roll=0)
    seat=anchored([.60,.20,.48],300,315,[[.75,.10,.08],[.75,.30,.08]], [[.82,.02,.38],[.82,.38,.38]],hp=35,roll=0)
    stand=key([1.05,.30,.975],360,360,[[1.05,.20,.08],[1.05,.40,.08]], [[1.20,.10,1.02],[1.20,.50,1.02]],knee=(1,0,0))
    keys=[(0,start),(.08,crouch),(.17,shoulder),(.25,inverted),(.35,back),(.43,seat),(.54,stand)]
    # The full turn has ended. Reset the angle without changing its orientation.
    reset=dict(stand,p1=0,p2=0)
    # Lift each foot in turn while walking backward around the short diagonal path.
    right=[1.05,.20,.08];left=[1.05,.40,.08]
    keys.append((.56,reset))
    steps=6
    for n in range(steps):
        for side in (0,1):
            progress=(n+(side+1)/2)/steps
            x=1.05-1.75*progress;y=.30*(1-progress)
            target=[x,y+(-.10 if side==0 else .10),.08]
            before=[right[:],left[:]];after=[right[:],left[:]];after[side]=target
            lifted=[before[0][:],before[1][:]];lifted[side]=[target[0],target[1],.22]
            time=.57+.40*(n*2+side)/(steps*2)
            P=[(before[0][0]+before[1][0]+target[0])/3,(before[0][1]+before[1][1]+target[1])/3,.78]
            hands=[[P[0]+.15,P[1]-.20,1.0],[P[0]+.15,P[1]+.20,1.0]]
            keys.append((time,key(P,0,0,lifted,hands,knee=(1,0,0))))
            P=[(after[0][0]+after[1][0])/2,(after[0][1]+after[1][1])/2,.78]
            hands=[[P[0]+.15,P[1]-.20,1.0],[P[0]+.15,P[1]+.20,1.0]]
            keys.append((time+.015,key(P,0,0,after,hands,knee=(1,0,0))))
            right,left=after
    keys.append((.99,start))
    # Keep the completed turn through the reset. An explicit t=1 key avoids
    # interpolating 360 back through 0 before the playback loop wraps.
    keys=[(t,dict(k,p1=k['p1']+360,p2=k['p2']+360) if .56<=t<.99 else k) for t,k in keys]
    keys[-1]=(.99,dict(start,p1=360,p2=360))
    keys.append((1.0,dict(start,p1=360,p2=360)))
    for t,k in keys:
        if .08 <= t <= .17:
            k['hand_r']=[-1,0,0]
    # Preserve head clearance between the keyed shoulder/back contacts too.
    arc=[]
    for i in range(136):
        t=.54*i/135
        k=sample(keys,t)
        joints=solve(k,{})
        k['pelvis'][2]+=max(0,.145-joints['head'][2])
        arc.append((t,k))
    keys=arc+[(t,k) for t,k in keys if t>.54]
    return dict(seconds=9,keys=keys,cam_az=30,cam_el=22,props=[dict(kind='box',center=[.2,.1,-.025],size=[3,1.5,.05])])


def rolling_sit_up():
    keys=[]
    for i in range(121):
        t=i/120;s=math.sin(math.pi*t)**2
        keys.append((t,_contact_rock(s)))
    return dict(seconds=3.6,keys=keys,cam_az=30,cam_el=22,props=[dict(kind='box',center=[0,0,-.025],size=[2.4,1.5,.05])])


def rolling_squat():
    stand=_contact_rock(0,0);stand.update(pelvis=[0,0,.975],wrist_r=[.25,-.16,1.05],wrist_l=[.25,.16,1.05],knee_r=[1,0,0],knee_l=[1,0,0])
    squat=_contact_rock(0,0);squat.update(pelvis=[-.18,0,.34],p1=65,p2=65,hp=10,wrist_r=[.28,-.16,.50],wrist_l=[.28,.16,.50])
    seat=_contact_rock(0,0)
    setup=[(0,stand),(.10,squat),(.20,seat)]
    keys=[(i/120,sample(setup,i/120)) for i in range(25)]
    for i in range(1,73):
        t=.20+.60*i/72;s=math.sin(math.pi*(t-.20)/.60)**2
        keys.append((t,_contact_rock(s,0)))
    close=[(0,seat),(.5,squat),(1,stand)]
    for i in range(1,25):
        t=.8+.2*i/24;keys.append((t,sample(close,(t-.8)/.2)))
    return dict(seconds=4.5,keys=keys,cam_az=30,cam_el=22,props=[dict(kind='box',center=[0,0,-.025],size=[2.4,1.5,.05])])


def shoulder_pop():
    def key(P,p1,p2,feet,hands,hp):
        return base_pose(pelvis=P,p1=p1,p2=p2,hp=hp,
          ankle_r=feet[0],ankle_l=feet[1],knee_r=[0,0,1],knee_l=[0,0,1],foot_r=[1,0,0],foot_l=[1,0,0],
          wrist_r=hands[0],wrist_l=hands[1],elbow_r=[0,-1,1],elbow_l=[0,1,1],
          hand_r=[1,0,0],hand_l=[1,0,0],palm_r=[0,0,-1],palm_l=[0,0,-1],grip_r=0,grip_l=0)
    seat=key([-.10,0,.12],0,0,[[.20,-.1,.08],[.20,.1,.08]],[[.15,-.2,.45],[.15,.2,.45]],0)
    rock=key([-.10,0,.25],-65,-82,[[.18,-.1,.42],[.18,.1,.42]],[[-.55,-.2,.035],[-.55,.2,.035]],40)
    shoulders=key([0,0,.40],-145,-115,[[.32,-.1,.68],[.32,.1,.68]],[[-.55,-.2,.035],[-.55,.2,.035]],50)
    pop=key([0,0,.435],-145,-115,[[.0172,-.09,1.3446],[.0172,.09,1.3446]],[[-.55,-.2,.035],[-.55,.2,.035]],50)
    for k in (seat,rock,shoulders,pop): k['lift']=.035 if k is pop else 0
    keys=[(0,seat),(.15,rock),(.32,shoulders),(.43,pop),(.51,shoulders),(.70,rock),(.90,seat)]
    dense_keys=[]
    for i in range(121):
        t=i/120
        k=sample(keys,t)
        J=solve(k,{})
        low=min(J['pelvis'][2]-.09,J['spine'][2]-.09,J['chest'][2]-.10)
        k['pelvis'][2]+=.01+k.pop('lift')-low
        dense_keys.append((t,k))
    return dict(seconds=4,keys=dense_keys,cam_az=30,cam_el=22,props=[dict(kind='box',center=[0,0,-.025],size=[2.4,1.5,.05])])


def _agility_pose(P,pitch,feet,*,yaw=0,lead=1,touch=False,hands=None):
    u,l=trunk_frame(pitch,yaw);d=[math.cos(math.radians(yaw)),math.sin(math.radians(yaw)),0]
    shoulders=[[P[i]+.47*u[i]+sgn*.18*l[i] for i in range(3)] for sgn in (-1,1)]
    if hands is None:
        hands=[]
        for n,sgn in enumerate((-1,1)):
            swing=.22*lead*(1 if n==1 else -1)
            hands.append([shoulders[n][i]+swing*d[i]+sgn*.06*l[i]+(-.27 if i==2 else 0) for i in range(3)])
    if touch:
        hands[0]=[P[0]+.50*d[0],P[1]+.50*d[1]-.20,.035]
    return base_pose(pelvis=P,p1=pitch,p2=pitch,hp=0,yaw=yaw,
      ankle_r=feet[0],ankle_l=feet[1],knee_r=[d[0],d[1],.4],knee_l=[d[0],d[1],.4],
      foot_r=d,foot_l=d,wrist_r=hands[0],wrist_l=hands[1],elbow_r=[0,-1,1],elbow_l=[0,1,1],
      hand_r=d,hand_l=d,palm_r=[0,0,-1],palm_l=[0,0,-1],grip_r=0,grip_l=0)

def _contact_rock(s, feet_start=.20):
    p1=-145*s;p2=-115*s
    u1,_=trunk_frame(p1);u2,_=trunk_frame(p2)
    spine=.24*u1[2];chest=spine+.26*u2[2]
    z=.01-min(-.09,spine-.09,chest-.10)
    P=[-.10+.10*s,0,z]
    ax=feet_start*(1-s)-.35*s;az=.08+.37*s
    return base_pose(pelvis=P,p1=p1,p2=p2,hp=50*s,
      ankle_r=[ax,-.10,az],ankle_l=[ax,.10,az],knee_r=[0,0,1],knee_l=[0,0,1],foot_r=[1,0,0],foot_l=[1,0,0],
      wrist_r=[ax,-.13,az+.07],wrist_l=[ax,.13,az+.07],elbow_r=[0,-1,1],elbow_l=[0,1,1],
      hand_r=[1,0,0],hand_l=[1,0,0],palm_r=[0,0,-1],palm_l=[0,0,-1],grip_r=0,grip_l=0)


EXERCISES = {
    "lateral-shuffle": lateral_shuffle,
    "carioca": carioca,
    "shuttle-sprint": shuttle_sprint,
    "crouch-run": crouch_run,
    "four-square-hops": four_square_hops,
    "hexagon-drill": hexagon_drill,
    "bear-plank": bear_plank,
    "bear-crawl": bear_crawl,
    "crab-walk": crab_walk,
    "inchworm": inchworm,
    "frogger": frogger,
    "shoulder-roll": shoulder_roll,
    "rolling-sit-up": rolling_sit_up,
    "rolling-squat": rolling_squat,
    "shoulder-pop": shoulder_pop,
}
