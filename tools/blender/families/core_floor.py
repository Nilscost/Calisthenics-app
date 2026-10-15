"""Floor-based core exercises for R8."""

import math

from poses import base_pose, both, rep, dense, pt

L_SH = 0.47


def _c(a):
    return math.cos(math.radians(a))


def _s(a):
    return math.sin(math.radians(a))


def _forearm_pose(ax=-1.35, angle=11, shift=0.0, bump=0.0, lever=0.27):
    """Straight forearm plank, with toes as the rear pivot and forearms on floor."""
    a = angle
    p0 = [ax + 0.898 * _c(a) + shift, 0.0, 0.08 + 0.898 * _s(a) + bump]
    sx = p0[0] + L_SH * _c(a)
    return base_pose(pelvis=p0, p1=90-a, p2=90-a, hp=-8,
                     wrist_l=[sx+lever, 0.20, 0.05], wrist_r=[sx+lever, -0.20, 0.05],
                     elbow_l=[-0.2, 0.3, -1], elbow_r=[-0.2, -0.3, -1],
                     hand_l=[1, 0, 0], hand_r=[1, 0, 0], palm_l=[0,0,-1], palm_r=[0,0,-1],
                     ankle_l=[ax+shift, 0.10, 0.08], ankle_r=[ax+shift, -0.10, 0.08],
                     knee_l=[0,0,1], knee_r=[0,0,1], foot_l=[0.95,0,-0.3], foot_r=[0.95,0,-0.3])


def plank_long_lever():
    # Elbows are conspicuously ahead of the shoulder line; squeeze into a short hold.
    a = _forearm_pose(lever=0.34, bump=0.005)
    b = _forearm_pose(lever=0.34, bump=-0.005)
    return dict(seconds=3.0, keys=rep(a,b), cam_az=30, cam_el=18, props=[])


def _high_plank(angle=22.5, ax=-1.245, reach=None):
    p0 = [ax + 0.898*_c(angle), 0.0, 0.08 + 0.898*_s(angle)]
    sx = p0[0] + L_SH*_c(angle)
    sh_z = p0[2] + L_SH*_s(angle)
    hip_x = p0[0] - 0.03*_c(angle)
    hip_z = p0[2] - 0.03*_s(angle)
    wrists = dict(wrist_l=[sx, 0.18, 0.035], wrist_r=[sx, -0.18, 0.035])
    ankles = dict(ankle_l=[ax,0.09,0.08], ankle_r=[ax,-0.09,0.08])
    if reach == "right-arm-left-leg":
        wrists["wrist_r"]=[sx+0.56,-0.18,sh_z]
        ankles["ankle_l"]=[hip_x-0.868,0.09,hip_z]
    elif reach == "left-arm-right-leg":
        wrists["wrist_l"]=[sx+0.56,0.18,sh_z]
        ankles["ankle_r"]=[hip_x-0.868,-0.09,hip_z]
    return base_pose(pelvis=p0, p1=90-angle, p2=90-angle, hp=-8,
                     **wrists, **ankles,
                     elbow_l=[-0.6,0.7,1], elbow_r=[-0.6,-0.7,1],
                     hand_l=[1,0,0], hand_r=[1,0,0], palm_l=[0,0,-1], palm_r=[0,0,-1],
                     grip_l=0, grip_r=0, foot_l=[0.95,0,-0.3], foot_r=[0.95,0,-0.3],
                     knee_l=[0,0,-1], knee_r=[0,0,-1])


def plank_contralateral():
    a = _high_plank(reach="right-arm-left-leg")
    b = _high_plank(reach="right-arm-left-leg")
    # Small breath only: it is a hold, not an alternating rep.
    b["pelvis"][2] -= 0.008
    return dict(seconds=3.0, keys=rep(a,b), cam_az=30, cam_el=18, props=[])


def plank_arm_leg_reach():
    neutral = _high_plank()
    right = _high_plank(reach="right-arm-left-leg")
    left = _high_plank(reach="left-arm-right-leg")
    keys = [(0.0,neutral),(0.12,neutral),(0.31,right),(0.42,neutral),
            (0.62,left),(0.73,neutral),(0.90,neutral)]
    return dict(seconds=4.0, keys=keys, cam_az=30, cam_el=18, props=[])


def body_saw():
    # Forearms stay planted while the toe sliders and straight body move as a unit.
    anchor = _forearm_pose(shift=0.0)
    wrist_x = anchor["wrist_l"][0]
    def pose(shift):
        p = _forearm_pose(shift=shift)
        return p
    keyed = [(0.0,{"shift":0.0}),(0.15,{"shift":0.0}),
             (0.48,{"shift":-0.16}),(0.60,{"shift":-0.16}),
             (0.86,{"shift":0.035})]
    keys = dense(pose,keyed,steps=48)
    props=[dict(kind="plate",center=[-1.27,0.10,0.04],r=0.13,normal=[0,0,1]),
           dict(kind="plate",center=[-1.27,-0.10,0.04],r=0.13,normal=[0,0,1])]
    return dict(seconds=4.0, keys=keys, cam_az=30, cam_el=18, props=props)


def _sit_pose(pitch, ankle_l, ankle_r, bump=0.0, wrist_y=0.18):
    # Hands press directly into the floor at each hip; straight torso sits just off floor.
    pz=0.60 - L_SH*_c(pitch) + bump
    sx=L_SH*_s(pitch)
    k=dict(pelvis=[0,0,pz],p1=pitch,p2=pitch,hp=0,
           wrist_l=[sx,wrist_y,0.035],wrist_r=[sx,-wrist_y,0.035],
           elbow_l=[-1,0.2,0],elbow_r=[-1,-0.2,0],hand_l=[1,0,0],hand_r=[1,0,0],
           palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=0,grip_r=0)
    for side,ankle in (("l",ankle_l),("r",ankle_r)):
        kind,v,pole=ankle
        k["ankle_"+side+("_rel" if kind=="rel" else "")]=list(v)
        k["knee_"+side]=list(pole)
        k["foot_"+side]=[1,0,0]
    return base_pose(**k)


def _straight_legs(y_l=0.0,y_r=0.0,z=0.0):
    return (("rel",[0.84,y_l,z],[0,0,1]),("rel",[0.84,y_r,z],[0,0,1]))


def l_sit_compression_lift():
    def seated(heel_z):
        # Seated pike: hips stay close to the floor while hands press beside knees.
        pz=0.13
        hip_z=pz-0.03*math.cos(math.radians(35))
        relz=heel_z-hip_z
        return base_pose(pelvis=[0,0,pz],p1=35,p2=35,hp=0,
                         wrist_l=[0.46,0.18,0.035],wrist_r=[0.46,-0.18,0.035],
                         elbow_l=[-1,0.2,0],elbow_r=[-1,-0.2,0],
                         hand_l=[1,0,0],hand_r=[1,0,0],palm_l=[0,0,-1],palm_r=[0,0,-1],
                         ankle_l_rel=[math.sqrt(max(0.0,0.864**2-relz**2)),0.12,relz],
                         ankle_r_rel=[math.sqrt(max(0.0,0.864**2-relz**2)), -0.12,relz],
                         knee_l=[0,0,1],knee_r=[0,0,1],foot_l=[1,0,0],foot_r=[1,0,0])
    down=seated(0.08)
    up=seated(0.28)
    return dict(seconds=3.5,keys=rep(down,up,hold_top=0.12,down_end=0.50,hold_bottom=0.10),cam_az=30,cam_el=18,props=[])


def l_sit_straddle():
    # Locked arms and straight, clearly wide legs; small breath while the hips stay lifted.
    a=_sit_pose(15,("rel",[0.770,0.40,0],[0,0,1]),("rel",[0.770,-0.40,0],[0,0,1]))
    b=_sit_pose(15,("rel",[0.770,0.40,0],[0,0,1]),("rel",[0.770,-0.40,0],[0,0,1]),bump=-0.003)
    return dict(seconds=3.0,keys=rep(a,b),cam_az=35,cam_el=20,props=[])


def _wheel_prop(handle=True):
    # The renderer has no animated plate. Ten joint-attached rim rods make a
    # segmented wheel whose centre and handle follow the working hands.
    ring=[]
    n=10
    for i in range(n):
        a=2*math.pi*i/n
        b=2*math.pi*(i+1)/n
        pa=(0.09*math.cos(a),0.18, -0.03+0.09*math.sin(a))
        pb=(0.09*math.cos(b),0.18, -0.03+0.09*math.sin(b))
        ring.append(dict(kind="rod",a=pt("wrist_r",pa),b=pt("wrist_r",pb),r=0.012))
    if handle:
        ring.append(dict(kind="rod",a=pt("wrist_l"),b=pt("wrist_r"),r=0.017))
    return ring


def _reach_wrist(pelvis,pitch,side,pitch2=None):
    # Place the hands close to full arm reach while keeping them on the wheel.
    p1=math.radians(pitch)
    p2=math.radians(pitch if pitch2 is None else pitch2)
    shoulder_x=pelvis[0]+0.24*math.sin(p1)+0.23*math.sin(p2)
    shoulder_z=pelvis[2]+0.24*math.cos(p1)+0.23*math.cos(p2)
    dz=shoulder_z-0.12
    dx=math.sqrt(max(0.0,0.566**2-dz**2-0.01**2))
    return [shoulder_x+dx,side*0.17,0.12]


def _kneeling_rollout(s):
    # Knees and feet stay anchored while the hip traces the full controlled arc.
    q=math.radians(80-72*s)
    p=75+7*s
    pr=math.radians(p)
    hip=[0.44*math.cos(q),0,0.05+0.44*math.sin(q)]
    pelvis=[hip[0]+0.03*math.sin(pr),0,hip[2]+0.03*math.cos(pr)]
    wl=_reach_wrist(pelvis,p,1)
    wr=_reach_wrist(pelvis,p,-1)
    return base_pose(pelvis=pelvis,p1=p,p2=p,hp=-8,
                     ankle_l=[-0.429,0.09,0.08],ankle_r=[-0.429,-0.09,0.08],
                     knee_l=[1,0,-1],knee_r=[1,0,-1],foot_l=[-1,0,0],foot_r=[-1,0,0],
                     wrist_l=wl,wrist_r=wr,
                     elbow_l=[0,0.5,-1],elbow_r=[0,-0.5,-1],hand_l=[1,0,0],hand_r=[1,0,0],
                     palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=0.8,grip_r=0.8)


def _rollout_kneeling(stop=False):
    limit=0.55 if stop else 1.0
    def pose(s): return _kneeling_rollout(limit*s)
    keyed=[(0.0,{"s":0.0}),(0.14,{"s":0.0}),(0.55,{"s":1.0}),
           (0.63,{"s":1.0}),(0.98,{"s":0.0})]
    keys=dense(pose,keyed,steps=48)
    end=pose(1.0)["wrist_r"][0]
    wall=[dict(kind="box",center=[end+0.115,0,0.55],size=[0.05,0.8,1.1])] if stop else []
    return dict(seconds=4.0,keys=keys,cam_az=28,cam_el=18,
                props=_wheel_prop()+wall)


def rollout_knees():
    return _rollout_kneeling(False)


def rollout_ramp():
    return _rollout_kneeling(True)


def _standing_rollout_pose(phase):
    # High-hip pike folds at the hips, then unfolds to a straight plank;
    # the toes remain planted while the wheel follows the hands.
    leg_pitch=50+30*phase
    pitch1=80
    pitch2=120-40*phase
    leg=math.radians(leg_pitch); lower=math.radians(pitch1)
    hip=[-0.80+0.868*math.sin(leg),0,0.08+0.868*math.cos(leg)]
    pelvis=[hip[0]+0.03*math.sin(lower),0,hip[2]+0.03*math.cos(lower)]
    wl=_reach_wrist(pelvis,pitch1,1,pitch2)
    wr=_reach_wrist(pelvis,pitch1,-1,pitch2)
    return base_pose(pelvis=pelvis,p1=pitch1,p2=pitch2,hp=-8,
                     ankle_l=[-0.80,0.10,0.08],ankle_r=[-0.80,-0.10,0.08],
                     knee_l=[1,0,0],knee_r=[1,0,0],foot_l=[-1,0,0],foot_r=[-1,0,0],
                     wrist_l=wl,wrist_r=wr,
                     elbow_l=[0,0.5,-1],elbow_r=[0,-0.5,-1],hand_l=[1,0,0],hand_r=[1,0,0],
                     palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=0.8,grip_r=0.8)


def _blend_pose(a,b,t):
    out={}
    for k,v in a.items():
        w=b[k]
        out[k]=[x+(y-x)*t for x,y in zip(v,w)] if isinstance(v,list) else v+(w-v)*t
    return out


def _kneeling_transition(a,b,t):
    pose=_blend_pose(a,b,t)
    # Clear the knees during the standing-to-kneeling fold, then settle onto
    # the mat at the kneeling endpoint instead of interpolating through it.
    pose["pelvis"][2]+=0.06*math.sin(math.pi*t)
    pose["wrist_l"]=_reach_wrist(pose["pelvis"],pose["p1"],1)
    pose["wrist_r"]=_reach_wrist(pose["pelvis"],pose["p1"],-1)
    p=math.radians(pose["p1"])
    hx=pose["pelvis"][0]-0.03*math.sin(p)
    hz=pose["pelvis"][2]-0.03*math.cos(p)
    dx=math.sqrt(max(0.0,0.862**2-(hz-0.08)**2))
    pose["ankle_l"]=[hx-dx,0.10,0.08]
    pose["ankle_r"]=[hx-dx,-0.10,0.08]
    return pose


def _negative_kneel(s):
    pose=_kneeling_rollout(s)
    pose["pelvis"][2]+=0.04*math.sin(math.pi*s)
    pose["wrist_l"]=_reach_wrist(pose["pelvis"],pose["p1"],1)
    pose["wrist_r"]=_reach_wrist(pose["pelvis"],pose["p1"],-1)
    return pose


def rollout_negative():
    mat=dict(kind="box",center=[-0.45,0,0.015],size=[1.5,0.9,0.03])
    # Roll out from a fixed-foot pike, lower the knees, return from the knees,
    # then restore the pike for a seamless loop.
    stand0=_standing_rollout_pose(0.0)
    stand1=_standing_rollout_pose(1.0)
    kneel0=_kneeling_rollout(0.0)
    kneel1=_kneeling_rollout(1.0)
    def pose(s):
        if s<0.32: return _standing_rollout_pose(s/0.32)
        if s<0.42: return _kneeling_transition(stand1,kneel1,(s-0.32)/0.10)
        if s<0.62: return _negative_kneel(1-(s-0.42)/0.20)
        if s<0.72: return _kneeling_transition(kneel0,stand0,(s-0.62)/0.10)
        return stand0
    keyed=[(0.0,{"s":0.0}),(0.14,{"s":0.0}),(0.62,{"s":1.0}),
           (0.72,{"s":1.0}),(0.98,{"s":0.0})]
    return dict(seconds=4.0,keys=dense(pose,keyed,steps=48),cam_az=28,cam_el=18,
                props=_wheel_prop()+[mat])


def rollout_standing():
    # Same full straight-body reach, but return all the way to a straight-knee pike.
    keyed=[(0.0,{"phase":0.0}),(0.14,{"phase":0.0}),(0.52,{"phase":1.0}),
           (0.62,{"phase":1.0}),(0.98,{"phase":0.0})]
    keys=dense(_standing_rollout_pose,keyed,steps=48)
    return dict(seconds=4.0,keys=keys,cam_az=25,cam_el=18,props=_wheel_prop())


def rollout_one_arm():
    def pose(s):
        k=_kneeling_rollout(0.55*s)
        k["wrist_l"]=[k["pelvis"][0]-0.02,0.20,k["pelvis"][2]-0.06]
        k["elbow_l"]=[-1,0.5,0]
        k["hand_l"]=[-1,0,0]
        k["grip_l"]=0
        return k
    keyed=[(0.0,{"s":0.0}),(0.14,{"s":0.0}),(0.55,{"s":1.0}),
           (0.64,{"s":1.0}),(0.98,{"s":0.0})]
    axle=dict(kind="rod",a=pt("wrist_r",(0,0.13,0)),b=pt("wrist_r",(0,-0.13,0)),r=0.017)
    return dict(seconds=4.0,keys=dense(pose,keyed,steps=48),cam_az=27,cam_el=18,
                props=_wheel_prop(handle=False)+[axle])


# --- dead-bug positions, all supine with pelvis and lower back at floor level --------------
def _supine(pelvis_z=0.11, head=0, **over):
    return base_pose(pelvis=[0,0,pelvis_z],p1=-90,p2=-90,hp=head,
                     hand_l=[-1,0,0],hand_r=[-1,0,0],**over)


def _tabletop():
    return dict(ankle_l_rel=[0.43,0,0.44],ankle_r_rel=[0.43,0,0.44],
                knee_l=[0.3,0,1],knee_r=[0.3,0,1],
                wrist_l_rel=[0,0.03,0.56],wrist_r_rel=[0,-0.03,0.56])


def dead_bug_brace():
    a=_supine(ankle_l=[0.38,0.10,0.08],ankle_r=[0.38,-0.10,0.08],
              knee_l=[0.3,0,1],knee_r=[0.3,0,1],
              wrist_l_rel=[0,0.03,0.20],wrist_r_rel=[0,-0.03,0.20])
    b=_supine(pelvis_z=0.115,ankle_l=[0.38,0.10,0.08],ankle_r=[0.38,-0.10,0.08],
              knee_l=[0.3,0,1],knee_r=[0.3,0,1],
              wrist_l_rel=[0,0.03,0.20],wrist_r_rel=[0,-0.03,0.20])
    return dict(seconds=3.0,keys=rep(a,b),cam_az=30,cam_el=18,props=[])


def dead_bug_90_90():
    a=_supine(**_tabletop())
    b=_supine(pelvis_z=0.105,**_tabletop())
    return dict(seconds=3.0,keys=rep(a,b),cam_az=30,cam_el=18,props=[])


def bent_leg_raise_army():
    def key(extension):
        # Hands support the lumbar curve; the head is lifted slightly throughout.
        if extension < 0.5:
            ankle=[0.43,0,0.44]
        else:
            q=(extension-0.5)*2
            ankle=[0.43+0.437*q,0,0.44-0.46*q]
        return _supine(pelvis_z=0.11,head=18,
                       wrist_l=[-0.02,0.10,0.04],wrist_r=[-0.02,-0.10,0.04],
                       elbow_l=[-1,0.3,1],elbow_r=[-1,-0.3,1],
                       ankle_l_rel=ankle,ankle_r_rel=ankle,
                       knee_l=[0.3,0,1],knee_r=[0.3,0,1])
    keys=[(0.0,key(0)),(0.20,key(0)),(0.50,key(1.0)),(0.63,key(1.0)),(0.83,key(0.0))]
    return dict(seconds=4.0,keys=keys,cam_az=30,cam_el=20,props=[])


def dead_bug_single_leg():
    table=_supine(**_tabletop())
    def extended(side):
        kwargs=_tabletop()
        kwargs["ankle_"+side+"_rel"]=[0.866,0,-0.03]
        kwargs["knee_"+side]=[0,0,1]
        return _supine(**kwargs)
    keys=[(0.0,table),(0.12,table),(0.32,extended("l")),(0.43,table),
          (0.63,extended("r")),(0.74,table),(0.91,table)]
    return dict(seconds=4.0,keys=keys,cam_az=30,cam_el=20,props=[])


# --- quadruped / bird-dog family ------------------------------------------------------------
def _quadruped(arm=None,leg=None,breath=0.0):
    px=-0.10
    shoulder_x=px+L_SH*_s(88)
    shoulder_z=0.47+L_SH*_c(88)
    hip_x=px-0.03*_s(88)
    hip_z=0.47-0.03*_c(88)
    wrists=dict(wrist_l=[shoulder_x,0.18,0.035],wrist_r=[shoulder_x,-0.18,0.035])
    ankles=dict(ankle_l=[px-0.58,0.09,0.08],ankle_r=[px-0.58,-0.09,0.08])
    if arm=="r":
        wrists["wrist_r"]=[shoulder_x+0.565,-0.18,shoulder_z]
    if arm=="l":
        wrists["wrist_l"]=[shoulder_x+0.565,0.18,shoulder_z]
    if leg=="l":
        ankles["ankle_l"]=[hip_x-0.868,0.12,hip_z]
    if leg=="r":
        ankles["ankle_r"]=[hip_x-0.868,-0.12,hip_z]
    return base_pose(pelvis=[px,0,0.47+breath],p1=88,p2=88,hp=-5,
                     **wrists,**ankles,
                     elbow_l=[0,0.4,-1],elbow_r=[0,-0.4,-1],
                     hand_l=[1,0,0],hand_r=[1,0,0],palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=0,grip_r=0,
                     knee_l=[1,0,-3],knee_r=[1,0,-3],foot_l=[-1,0,0],foot_r=[-1,0,0])


def bird_dog_quadruped():
    a=_quadruped(breath=0.005); b=_quadruped(breath=-0.005)
    return dict(seconds=3.0,keys=rep(a,b),cam_az=30,cam_el=20,props=[])


def bird_dog_arm_raise():
    neutral=_quadruped()
    reach=_quadruped(arm="r")
    left=_quadruped(arm="l")
    keys=[(0.0,neutral),(0.12,neutral),(0.30,reach),(0.42,reach),(0.52,neutral),
          (0.70,left),(0.78,left),(0.88,neutral)]
    return dict(seconds=4.0,keys=keys,cam_az=30,cam_el=20,props=[])


def bird_dog_leg_raise():
    neutral=_quadruped()
    right=_quadruped(leg="r"); left=_quadruped(leg="l")
    keys=[(0.0,neutral),(0.12,neutral),(0.34,right),(0.42,right),
          (0.52,neutral),(0.68,left),(0.76,left),(0.86,neutral)]
    return dict(seconds=4.0,keys=keys,cam_az=30,cam_el=20,props=[])


def bird_dog():
    neutral=_quadruped()
    right=_quadruped(arm="r",leg="l"); left=_quadruped(arm="l",leg="r")
    keys=[(0.0,neutral),(0.12,neutral),(0.32,right),(0.42,right),
          (0.54,neutral),(0.68,left),(0.78,left),(0.90,neutral)]
    return dict(seconds=4.0,keys=keys,cam_az=30,cam_el=20,props=[])


def quadraplex():
    a=_quadruped(arm="r",leg="l",breath=0.004)
    b=_quadruped(arm="r",leg="l",breath=-0.004)
    return dict(seconds=3.0,keys=rep(a,b),cam_az=30,cam_el=20,props=[])


def swimmer():
    def pose(side,bump=0.0):
        pelvis=[0,0,0.11+bump]
        # Arms point forward past the head; feet extend back; alternate diagonal lifts.
        if side=="r-arm-l-leg":
            wl=[1.035,0.18,0.17]; wr=[1.038,-0.18,0.11]
            al=[-0.892,0.12,0.18]; ar=[-0.895,-0.12,0.08]
        elif side=="l-arm-r-leg":
            wl=[1.038,0.18,0.11]; wr=[1.035,-0.18,0.17]
            al=[-0.895,0.12,0.08]; ar=[-0.892,-0.12,0.18]
        else:
            wl=[1.038,0.18,0.11]; wr=[1.038,-0.18,0.11]
            al=[-0.895,0.12,0.08]; ar=[-0.895,-0.12,0.08]
        return base_pose(pelvis=pelvis,p1=90,p2=90,hp=-8,
                         wrist_l=wl,wrist_r=wr,elbow_l=[1,0.3,0],elbow_r=[1,-0.3,0],
                         hand_l=[1,0,0],hand_r=[1,0,0],palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=0,grip_r=0,
                         ankle_l=al,ankle_r=ar,knee_l=[0,0,1],knee_r=[0,0,1],
                         foot_l=[-1,0,0],foot_r=[-1,0,0])
    keys=[(0.0,pose("r-arm-l-leg")),(0.18,pose("r-arm-l-leg")),
          (0.48,pose("l-arm-r-leg")),(0.58,pose("l-arm-r-leg")),
          (0.86,pose("r-arm-l-leg"))]
    return dict(seconds=4.0,keys=keys,cam_az=28,cam_el=20,props=[])


EXERCISES = {
    "plank-long-lever": plank_long_lever,
    "plank-contralateral": plank_contralateral,
    "plank-arm-leg-reach": plank_arm_leg_reach,
    "body-saw": body_saw,
    "l-sit-compression-lift": l_sit_compression_lift,
    "l-sit-straddle": l_sit_straddle,
    "rollout-knees": rollout_knees,
    "rollout-ramp": rollout_ramp,
    "rollout-negative": rollout_negative,
    "rollout-standing": rollout_standing,
    "rollout-one-arm": rollout_one_arm,
    "dead-bug-brace": dead_bug_brace,
    "dead-bug-90-90": dead_bug_90_90,
    "bent-leg-raise-army": bent_leg_raise_army,
    "dead-bug-single-leg": dead_bug_single_leg,
    "bird-dog-quadruped": bird_dog_quadruped,
    "bird-dog-arm-raise": bird_dog_arm_raise,
    "bird-dog-leg-raise": bird_dog_leg_raise,
    "bird-dog": bird_dog,
    "quadraplex": quadraplex,
    "swimmer": swimmer,
}
