"""Supine, seated, rotational and side-supported core exercises."""
import math

from poses import base_pose


def _lie(pz=.11, p1=-90, p2=-90, hp=0, **fields):
    f=dict(hand_l=[-1,0,0],hand_r=[-1,0,0]); f.update(fields)
    return base_pose(pelvis=[0, 0, pz], p1=p1, p2=p2, hp=hp, **f)


def _rep(a, b, end=.94):
    return [(0,a),(.12,a),(.50,b),(.62,b),(end,a),(.98,a)]


def _series(fn, t0, t1, n=18):
    return [(t0+(t1-t0)*i/n, fn(i/n)) for i in range(1,n+1)]


def _floor_arms():
    # Arms relaxed beside the ribs, clear of the head and body.
    return dict(wrist_l=[-.05,.20,.035],wrist_r=[-.05,-.20,.035],
                elbow_l=[0,1,0],elbow_r=[0,-1,0],hand_l=[1,0,0],hand_r=[1,0,0],
                palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=0,grip_r=0)


def _overhead_arms():
    return dict(wrist_l_rel=[-.55,.12,-.075],wrist_r_rel=[-.55,-.12,-.075],
                elbow_l=[-1,.2,0],elbow_r=[-1,-.2,0],
                palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=0,grip_r=0)


def _legs(rel_l, rel_r=None, knee_l=(0,1,0), knee_r=(0,1,0), feet=(1,0,0)):
    if rel_r is None: rel_r=rel_l
    return dict(ankle_l_rel=list(rel_l),ankle_r_rel=list(rel_r),
                knee_l=list(knee_l),knee_r=list(knee_r),foot_l=list(feet),foot_r=list(feet))


def lying_knee_tuck():
    arms=_floor_arms()
    a=_lie(**arms,**_legs((.85,0,.04),knee_l=(0,1,0),knee_r=(0,1,0)))
    b=_lie(.14,**arms,**_legs((.20,0,.43),knee_l=(0,1,0),knee_r=(0,1,0)))
    return dict(seconds=3.4,keys=_rep(a,b),cam_az=32,cam_el=18,props=[])


def lying_bent_knee_raise():
    arms=_floor_arms()
    # Fixed 90-degree knee bend: hip-to-ankle distance about .61 m.
    low=_lie(**arms,**_legs((.60,0,.10),knee_l=(0,1,0),knee_r=(0,1,0)))
    high=_lie(**arms,**_legs((.02,0,.61),knee_l=(0,1,0),knee_r=(0,1,0)))
    return dict(seconds=3.6,keys=_rep(low,high),cam_az=32,cam_el=18,props=[])


def seated_knee_raise():
    arms=dict(wrist_l=[-.04,.24,.035],wrist_r=[-.04,-.24,.035],elbow_l=[-1,.2,0],elbow_r=[-1,-.2,0],
              hand_l=[1,0,0],hand_r=[1,0,0],palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=0,grip_r=0)
    def pose(rel, pz=.12, p=-8):
        return base_pose(pelvis=[0,0,pz],p1=p,p2=p,hp=0,**arms,
                         **_legs(rel,knee_l=(0,1,0),knee_r=(0,1,0)))
    a=pose((.868,0,.03)); b=pose((.16,0,.42),.12,-8)
    return dict(seconds=3.4,keys=_rep(a,b),cam_az=32,cam_el=18,props=[])


def frog_raise():
    arms=_floor_arms()
    bent=_lie(**arms,**_legs((.61,0,.01),knee_l=(0,1,0),knee_r=(0,1,0)))
    straight=_lie(**arms,**_legs((0,0,.868),knee_l=(0,1,0),knee_r=(0,1,0)))
    return dict(seconds=3.8,keys=_rep(bent,straight),cam_az=32,cam_el=18,props=[])


def reverse_crunch():
    arms=_floor_arms()
    a=_lie(**arms,**_legs((.02,0,.61),knee_l=(0,1,0),knee_r=(0,1,0)))
    b=_lie(.19,-98,-98,**arms,**_legs((-.10,0,.60),knee_l=(0,1,0),knee_r=(0,1,0)))
    return dict(seconds=3.4,keys=_rep(a,b),cam_az=32,cam_el=18,props=[])


def _vup_pose(tuck=False, phase=0):
    # Smooth supine-to-seated spinal roll; the torso and legs meet at the top.
    p=-90+45*phase
    pz=.11+.02*phase
    if tuck:
        ankles=(.32,0,.28) if phase else (.868,0,.02)
        knees=(1,0,1) if phase else (1,0,0)
        arms=(dict(wrist_l_rel=[.45,.12,-.05],wrist_r_rel=[.45,-.12,-.05],
                   elbow_l=[-1,.2,0],elbow_r=[-1,-.2,0],palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=0,grip_r=0)
               if phase else _overhead_arms())
    else:
        ankles=(.60,0,.56) if phase else (.85,0,.02)
        knees=(0,1,0)
        arms=(dict(wrist_l_rel=[.60,.12,.00],wrist_r_rel=[.60,-.12,.00],
                   elbow_l=[-1,.2,0],elbow_r=[-1,-.2,0],palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=0,grip_r=0)
               if phase else _overhead_arms())
    return _lie(pz,p,p,0,**arms,**_legs(ankles,knee_l=knees,knee_r=knees))


def _vup(tuck=False):
    keys=[(0,_vup_pose(tuck,0)),(.12,_vup_pose(tuck,0))]
    keys += _series(lambda s:_vup_pose(tuck,s),.12,.50,18)
    keys += [(.62,_vup_pose(tuck,1))]
    keys += _series(lambda s:_vup_pose(tuck,1-s),.62,.94,18)
    keys += [(.98,_vup_pose(tuck,0))]
    return dict(seconds=3.8,keys=keys,cam_az=32,cam_el=18,props=[])


def v_up_tuck(): return _vup(True)
def v_up_straddle():
    # Wide straight legs, with both hands reaching between the thighs at the top.
    keys=[(0,_vup_pose(False,0)),(.12,_vup_pose(False,0))]
    def top(s):
        q=_vup_pose(False,s)
        # Rotate each .8695 m leg vector smoothly from floorward to raised;
        # maintain its length and grow the straddle continuously with the lift.
        spread=.08+.04*s
        xz=math.sqrt(.8695**2-spread**2)
        angle=math.radians(40*s)
        x,z=xz*math.cos(angle),xz*math.sin(angle)
        q['ankle_l_rel']=[x,spread,z]; q['ankle_r_rel']=[x,-spread,z]
        arm_angle=math.radians(168*(1-s)+4.1*s)
        arm_x=.56*math.cos(arm_angle)
        arm_y=.56*math.sin(arm_angle)
        arm_z=-.075*(1-s)
        q['wrist_l_rel']=[arm_x,arm_y,arm_z]
        q['wrist_r_rel']=[arm_x,-arm_y,arm_z]
        return q
    keys[0]=(0,top(0)); keys[1]=(.12,top(0))
    keys += _series(top,.12,.50,72)+[(.62,top(1))]
    keys += _series(lambda s:top(1-s),.62,.94,72)+[(.98,top(0))]
    return dict(seconds=3.8,keys=keys,cam_az=32,cam_el=18,props=[])


def _floor_wiper(bent=True, side=0):
    pz=.11
    arms=dict(wrist_l=[-.48,.68,.035],wrist_r=[-.48,-.68,.035],
              elbow_l=[-1,1,-.2],elbow_r=[-1,-1,-.2],hand_l=[1,0,0],hand_r=[1,0,0],
              palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=0,grip_r=0)
    if bent:
        length=.61
        angle=math.radians(side*50)
    else:
        length=.868
        angle=math.radians(side*45)
    dy=length*math.sin(angle); dz=length*math.cos(angle)
    rel=(0,dy,dz)
    if bent: knees=(0,1,0)
    else: knees=(0,1,0)
    return _lie(pz,**arms,**_legs(rel,knee_l=knees,knee_r=knees))


def _wiper(bent):
    keys=[(0,_floor_wiper(bent,0)),(.12,_floor_wiper(bent,0))]
    keys+=_series(lambda s:_floor_wiper(bent,-s*1),.12,.35,12)
    keys+=_series(lambda s:_floor_wiper(bent,-1+2*s),.35,.62,14)
    keys+=_series(lambda s:_floor_wiper(bent,1-2*s),.62,.89,14)
    keys += [(.98,_floor_wiper(bent,0))]
    return dict(seconds=4.0,keys=keys,cam_az=32,cam_el=22,props=[])


def windshield_wiper_bent_knee(): return _wiper(True)
def windshield_wiper_floor(): return _wiper(False)


def curl_up_mcgill():
    arms=dict(wrist_l=[.05,.08,.035],wrist_r=[.05,-.08,.035],
              elbow_l=[0,1,0],elbow_r=[0,-1,0],hand_l=[1,0,0],hand_r=[1,0,0],
              palm_l=[0,0,1],palm_r=[0,0,1],grip_l=0,grip_r=0)
    def pose(lift, left_bent=True):
        bent=(.02,0,.61); straight=(.868,0,.02)
        legs=(bent,straight) if left_bent else (straight,bent)
        return _lie(.11,-90,-90+5*lift,0,**arms,
                    **_legs(*legs,knee_l=(0,1,0),knee_r=(0,1,0)))
    a=pose(0,True); b=pose(1,True); c=pose(0,False); d=pose(1,False)
    return dict(seconds=4.0,keys=[(0,a),(.12,a),(.22,b),(.38,b),(.48,a),(.52,c),(.62,c),(.72,d),(.88,d),(.96,c),(.98,a)],cam_az=32,cam_el=18,props=[])


def crunch():
    arms=dict(wrist_l_rel=[-.20,.28,.00],wrist_r_rel=[-.20,-.28,.00],
              elbow_l=[-1,1,0],elbow_r=[-1,-1,0],hand_l=[-1,0,0],hand_r=[-1,0,0],
              palm_l=[0,0,1],palm_r=[0,0,1],grip_l=0,grip_r=0)
    legs=_legs((.02,0,.61),knee_l=(0,1,0),knee_r=(0,1,0))
    a=_lie(**arms,**legs); b=_lie(.13,-90,-68,**arms,**legs)
    return dict(seconds=3.2,keys=_rep(a,b),cam_az=32,cam_el=18,props=[])


def bicycle_crunch():
    arms=dict(wrist_l_rel=[-.12,.20,.03],wrist_r_rel=[-.12,-.20,.03],
              elbow_l=[-1,1,0],elbow_r=[-1,-1,0],hand_l=[-1,0,0],hand_r=[-1,0,0],
              palm_l=[0,0,1],palm_r=[0,0,1],grip_l=0,grip_r=0)
    def pose(right=True, center=False):
        yaw=0 if center else (-22 if right else 22)
        legs=(_legs((.13,0,.58),(.85,0,.02),knee_l=(0,1,0),knee_r=(0,1,0)) if right else
              _legs((.85,0,.02),(.13,0,.58),knee_l=(0,1,0),knee_r=(0,1,0)))
        return _lie(.12,-90,-76 if not center else -90,0,**arms,**legs,yaw=yaw)
    keys=[(0,pose(True)),(.15,pose(True)),(.32,pose(True,True)),(.42,pose(False)),(.57,pose(False,True)),(.70,pose(True)),(.85,pose(True,True)),(.98,pose(True))]
    return dict(seconds=4.0,keys=keys,cam_az=32,cam_el=20,props=[])


def _situp_pose(phase, rower=False):
    p1=-90+70*phase
    p2=-90+87*phase
    pz=.11+.37*phase
    arms=(_overhead_arms() if phase<.5 else
          dict(wrist_l_rel=[.45,.12,-.05],wrist_r_rel=[.45,-.12,-.05],elbow_l=[-1,.2,0],elbow_r=[-1,-.2,0],palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=0,grip_r=0))
    if rower:
        legs=_legs((.30,0,.30) if phase else (.62,0,.05),knee_l=(0,1,0),knee_r=(0,1,0))
        if phase:
            arms=dict(wrist_l_rel=[.50,.12,.00],wrist_r_rel=[.50,-.12,.00],elbow_l=[-1,.2,0],elbow_r=[-1,-.2,0],palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=0,grip_r=0)
    else:
        legs=_legs((.62,0,.02),knee_l=(0,1,0),knee_r=(0,1,0))
    arms.update(hand_l=[-1,0,0],hand_r=[-1,0,0])
    ankles={} if rower else dict(ankle_l=[.75,.10,.08],ankle_r=[.75,-.10,.08])
    return base_pose(pelvis=[.13*phase,0,pz],p1=p1,p2=p2,hp=0,**arms,**legs,**ankles)


def _situp(rower=False):
    keys=[(0,_situp_pose(0,rower)),(.12,_situp_pose(0,rower))]
    keys+=_series(lambda s:_situp_pose(s,rower),.12,.50,18)
    keys += [(.62,_situp_pose(1,rower))]
    keys += _series(lambda s:_situp_pose(1-s,rower),.62,.94,18)+[(.98,_situp_pose(0,rower))]
    return dict(seconds=4.0,keys=keys,cam_az=32,cam_el=18,props=[])


def sit_up(): return _situp(False)
def rower_situp():
    # Deliberately keep the glutes low: this is a seated rower tuck, not a
    # sit-up with the pelvis lifted into the air. The knees tuck as the trunk
    # rises and both arms reach toward the shins, then the body returns supine.
    def pose(q):
        ease = .5 - .5 * math.cos(math.pi * q)
        p1 = -90 + 60 * ease
        p2 = -90 + 78 * ease
        radius = math.sqrt(.105**2*math.sin(math.radians(p1))**2 + .11**2*math.cos(math.radians(p1))**2)
        pz = radius - .03*math.cos(math.radians(p1)) + .003
        ankle = (.62 * (1-ease) + .22 * ease, 0, .05 * (1-ease) + .36 * ease)
        wr_x = -.55 * (1-ease) + .55 * ease
        wr_y = .12 * (1-ease) + .10 * ease
        wr_z = -.075 * (1-ease)
        hand_angle = math.pi * ease
        arms = dict(
            wrist_l_rel=[wr_x, wr_y, wr_z], wrist_r_rel=[wr_x, -wr_y, wr_z],
            elbow_l=[-1,.2,0], elbow_r=[-1,-.2,0],
            hand_l=[-math.cos(hand_angle),0,math.sin(hand_angle)],
            hand_r=[-math.cos(hand_angle),0,math.sin(hand_angle)],
            palm_l=[0,1,0], palm_r=[0,-1,0], grip_l=0, grip_r=0,
        )
        px=.13*ease
        ax=px+.898*(1-ease)+.50*ease
        return base_pose(pelvis=[px,0,pz],p1=p1,p2=p2,hp=4*ease,
                         ankle_l=[ax,.10,.08+.12*ease],ankle_r=[ax,-.10,.08+.12*ease],
                         knee_l=[0,0,1],knee_r=[0,0,1],foot_l=[1,0,0],foot_r=[1,0,0],**arms)

    keys=[(0.0,pose(0)),(.12,pose(0))]
    keys += _series(pose,.12,.50,36)+[(.62,pose(1))]
    keys += _series(lambda s:pose(1-s),.62,.94,36)+[(.98,pose(0))]
    return dict(seconds=4.0,keys=keys,cam_az=32,cam_el=22,props=[])


def russian_twist():
    arms=dict(wrist_l_rel=[.18,.10,-.06],wrist_r_rel=[.18,-.10,-.06],
              elbow_l=[-1,.2,0],elbow_r=[-1,-.2,0],hand_l=[1,0,0],hand_r=[1,0,0],
              palm_l=[0,1,0],palm_r=[0,1,0],grip_l=.5,grip_r=.5)
    def pose(yaw):
        return base_pose(pelvis=[0,0,.30],p1=-18,p2=-18,hp=0,yaw=yaw,**arms,
                         ankle_l=[.45,.10,.08],ankle_r=[.45,-.10,.08],
                         foot_l=[1,0,0],foot_r=[1,0,0],knee_l=[1,0,0],knee_r=[1,0,0])
    keys=[(0,pose(0)),(.12,pose(0)),(.30,pose(-32)),(.42,pose(0)),(.55,pose(32)),(.68,pose(0)),(.82,pose(-32)),(.94,pose(0)),(.98,pose(0))]
    return dict(seconds=3.8,keys=keys,cam_az=32,cam_el=18,props=[])


def _side_base(pz, bottom_ankle, top_ankle, chair=None):
    k=base_pose(pelvis=[0,0,pz],p1=90,p2=90,roll=-90,hp=0,
                wrist_l=[.22,.24,.035],elbow_l=[-1,0,-.25],hand_l=[0,1,0],palm_l=[0,0,-1],grip_l=0,
                wrist_r_rel=[-.20,0,.40],elbow_r=[0,0,1],hand_r=[-1,0,0],palm_r=[0,0,1],grip_r=0,
                ankle_l=list(bottom_ankle),ankle_r=list(top_ankle),
                knee_l=[0,0,1],knee_r=[0,0,1],foot_l=[0,1,0],foot_r=[0,1,0])
    return k


def side_plank_knee():
    def pose(pz):
        p=[0,0,pz]; up=[.94,0,.342]; left=[.342,0,-.94]
        hip_l=[p[0]+.09*left[0]-.03*up[0],0,pz+.09*left[2]-.03*up[2]]
        hip_r=[p[0]-.09*left[0]-.03*up[0],0,pz-.09*left[2]-.03*up[2]]
        knee_l=[hip_l[0]-.44*up[0],hip_l[1],hip_l[2]-.44*up[2]]
        knee_r=[hip_r[0]-.44*up[0],hip_r[1],hip_r[2]-.44*up[2]]
        ank_l=[knee_l[0],knee_l[1]+.43,knee_l[2]]
        ank_r=[knee_r[0],knee_r[1]+.43,knee_r[2]]
        return base_pose(pelvis=p,p1=70,p2=70,roll=-90,hp=0,
                         wrist_l=[.22,.24,.035],elbow_l=[-1,0,-.25],hand_l=[0,1,0],palm_l=[0,0,-1],grip_l=0,
                         wrist_r_rel=[-.20,0,.40],elbow_r=[0,0,1],hand_r=[-1,0,0],palm_r=[0,0,1],grip_r=0,
                         ankle_l=ank_l,ankle_r=ank_r,knee_l=[-.94,0,-.342],knee_r=[-.94,0,-.342],
                         foot_l=[0,1,0],foot_r=[0,1,0])
    a=pose(.2955); b=pose(.2905)
    return dict(seconds=3.0,keys=[(0,a),(.12,a),(.48,b),(.62,b),(.90,a),(.98,a)],cam_az=32,cam_el=18,props=[])


def side_plank_hip_raise():
    low=_side_base(.30,(-.75,.10,.08),(-.75,-.10,.08))
    high=_side_base(.44,(-.75,.10,.08),(-.75,-.10,.08))
    return dict(seconds=3.5,keys=_rep(low,high),cam_az=32,cam_el=18,props=[])


def copenhagen_adduction():
    chair=dict(kind="chair",x=-.90,seat=.45)
    # Fixed top-ankle support; the lower ankle swings on a constant-radius arc
    # from the floor to meet it, keeping the adducting leg straight in flight.
    hip=[-.03,0,.36]
    radius=.8695
    def pose(s):
        z=.08+.42*s
        dy=.10
        dx=math.sqrt(radius**2-(z-hip[2])**2-dy**2)
        ankle_l=[hip[0]-dx,dy,z]
        return _side_base(.45,ankle_l,(-.89,-.10,.50))
    keys=[(0,pose(0)),(.12,pose(0))]
    keys += _series(pose,.12,.50,72)+[(.62,pose(1))]
    keys += _series(lambda s:pose(1-s),.62,.94,72)+[(.98,pose(0))]
    return dict(seconds=3.8,keys=keys,cam_az=32,cam_el=18,props=[chair])


EXERCISES={
    "lying-knee-tuck":lying_knee_tuck,
    "lying-bent-knee-raise":lying_bent_knee_raise,
    "seated-knee-raise":seated_knee_raise,
    "frog-raise":frog_raise,
    "reverse-crunch":reverse_crunch,
    "v-up-tuck":v_up_tuck,
    "v-up-straddle":v_up_straddle,
    "windshield-wiper-bent-knee":windshield_wiper_bent_knee,
    "windshield-wiper-floor":windshield_wiper_floor,
    "curl-up-mcgill":curl_up_mcgill,
    "crunch":crunch,
    "bicycle-crunch":bicycle_crunch,
    "sit-up":sit_up,
    "rower-situp":rower_situp,
    "russian-twist":russian_twist,
    "side-plank-knee":side_plank_knee,
    "side-plank-hip-raise":side_plank_hip_raise,
    "copenhagen-adduction":copenhagen_adduction,
}
