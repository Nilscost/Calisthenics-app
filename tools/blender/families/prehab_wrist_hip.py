"""Wrist and hip prehab movements (careful, low-range mobility work)."""

import math

from poses import base_pose, dense, rep


def _floorhands(x=0.36, y=0.24, hand=(1, 0, 0), palm=(0, 0, -1)):
    return dict(wrist_l=[x, y, 0.035], wrist_r=[x, -y, 0.035],
                hand_l=list(hand), hand_r=list(hand), palm_l=list(palm), palm_r=list(palm),
                grip_l=0.0, grip_r=0.0,
                elbow_l=[0, 1, -0.3], elbow_r=[0, -1, -0.3])


def _all_fours(wrist_z=0.035, hands=(1, 0, 0), palms=(0, 0, -1), pelvis=(-0.15, 0, 0.58)):
    P = [0, 0, 0.52]
    hip_l, hip_r = [-0.02954, 0.09, 0.51479], [-0.02954, -0.09, 0.51479]
    knee_l, knee_r = [hip_l[0], hip_l[1], hip_l[2] - 0.44], [hip_r[0], hip_r[1], hip_r[2] - 0.44]
    ankle_l, ankle_r = [knee_l[0] - 0.43, knee_l[1], knee_l[2]], [knee_r[0] - 0.43, knee_r[1], knee_r[2]]
    return base_pose(pelvis=P, p1=80, p2=80, hp=0,
                     wrist_l=[0.48, 0.18, wrist_z], wrist_r=[0.48, -0.18, wrist_z],
                     elbow_l=[0, 0, -1], elbow_r=[0, 0, -1],
                     hand_l=list(hands), hand_r=list(hands), palm_l=list(palms), palm_r=list(palms),
                     grip_l=0, grip_r=0, ankle_l=ankle_l, ankle_r=ankle_r,
                     knee_l=[0, 0, -1], knee_r=[0, 0, -1], foot_l=[-1, 0, 0], foot_r=[-1, 0, 0])


def wrist_palm_pulses():
    # The single rigid hand pivots about planted fingertips. Individual finger
    # lifts cannot be represented by this rig.
    def key(q):
        wz=.035+.07*q
        k=_all_fours(wrist_z=wz)
        dx=math.sqrt(.17**2-(wz-.035)**2)
        for side in ('l','r'):
            k['wrist_'+side][0]=.65-dx
            k['hand_'+side]=[dx/.17,0,(.035-wz)/.17]
        return k
    keys=[(i/80,key(.5-.5*math.cos(4*math.pi*i/80))) for i in range(80)]
    return dict(seconds=4,keys=keys,cam_az=30,cam_el=22,props=[])


def wrist_seated_mobility():
    def key(t):
        stage=min(3,int(4*t));u=4*t-stage
        p=base_pose(pelvis=[0,0,.11],p1=20,p2=20,hp=0,
            ankle_l=[.70,.18,.08],ankle_r=[.70,-.18,.08],
            knee_l=[0,0,1],knee_r=[0,0,1],foot_l=[1,0,0],foot_r=[1,0,0],
            wrist_l=[.415,.18,.344],wrist_r=[.415,-.18,.344],
            elbow_l=[0,0,-1],elbow_r=[0,0,-1],grip_l=0,grip_r=0)
        a=math.radians(20)
        v=[math.cos(a),0,math.sin(a)];n=[-math.sin(a),0,math.cos(a)]
        for side,sign in [('l',1),('r',-1)]:
            # Begin and end each phase at neutral to avoid a reset jump.
            if stage<2:
                angle=(1 if stage==0 else -1)*2*math.pi*u
                amp=.42*math.sin(math.pi*u)
                y=sign*amp*math.cos(angle);z=amp*math.sin(angle)
            elif stage==2:y=0;z=.65*math.sin(2*math.pi*u)
            else:y=sign*.45*math.sin(2*math.pi*u);z=0
            d=[v[0]+z*n[0],y,v[2]+z*n[2]]
            p['hand_'+side]=d;p['palm_'+side]=[d[2],0,-d[0]]
        return p
    return dict(seconds=8,keys=[(i/160,key(i/160)) for i in range(160)],cam_az=35,cam_el=18,props=[])


def forearm_rotation_fist():
    def key(t):
        a=math.radians(65)*math.sin(2*math.pi*t)
        k=_all_fours(hands=(1,0,0))
        for side,sign in [('l',1),('r',-1)]:
            k['palm_'+side]=[0,-sign*math.cos(a),math.sin(a)]
            k['grip_'+side]=.95
        return k
    return dict(seconds=4,keys=[(i/64,key(i/64)) for i in range(64)],cam_az=35,cam_el=18,props=[])


def _back_hand_pushup(knees):
    arms = dict(wrist_l=[0.25, 0.23, 0.035], wrist_r=[0.25, -0.23, 0.035],
                hand_l=[-1, 0, 0], hand_r=[-1, 0, 0],
                palm_l=[0, 0, 1], palm_r=[0, 0, 1], grip_l=0.1, grip_r=0.1,
                elbow_l=[0, 0, -1], elbow_r=[0, 0, -1])
    if knees:
        def key(pitch):
            a=math.radians(pitch)
            return base_pose(pelvis=[-.13+.03*math.sin(a), 0, .49+.03*math.cos(a)], p1=pitch, p2=pitch, hp=-5,
                             ankle_l=[-.56,.09,.05], ankle_r=[-.56,-.09,.05],
                             knee_l=[0, 0, -1], knee_r=[0, 0, -1],
                             foot_l=[-1, 0, 0], foot_r=[-1, 0, 0], **arms)
        top, bottom = key(80), key(86)
        keys = [(0, top), (0.12, top), (0.48, bottom), (0.60, bottom), (0.96, top)]
        return dict(seconds=3.0, keys=keys, cam_az=32, cam_el=18, props=[])
    def key(a):
        rad = math.radians(a)
        pelvis = [-0.8 + 0.8998 * math.cos(rad), 0, 0.08 + 0.8998 * math.sin(rad)]
        return base_pose(pelvis=pelvis, p1=90 - a, p2=90 - a, hp=-5,
                         ankle_l=[-0.8, 0.10, 0.08], ankle_r=[-0.8, -0.10, 0.08],
                         knee_l=[0, 0, -1], knee_r=[0, 0, -1],
                         foot_l=[-1, 0, 0], foot_r=[-1, 0, 0], **arms)
    keys = [(0, key(20)), (0.12, key(20)), (0.48, key(18)), (0.60, key(18)), (0.96, key(20))]
    return dict(seconds=3.0, keys=keys, cam_az=32, cam_el=18, props=[])


def wrist_pushup_knees():
    return _back_hand_pushup(True)


def wrist_pushup():
    return _back_hand_pushup(False)


def wrist_curl_eccentric():
    w = [0.415, -0.18, 0.344]
    assist = [w[0], w[1] + 0.17, w[2] - 0.02]
    lap = [0.28, 0.20, 0.18]
    def key(deg, assisting):
        a = math.radians(20 + deg)
        return base_pose(pelvis=[0, 0, 0.11], p1=20, p2=20, hp=0,
                         ankle_l=[.70,.18,.08], ankle_r=[.70,-.18,.08],
                         knee_l=[0,0,1], knee_r=[0,0,1], foot_l=[1, 0, 0], foot_r=[1, 0, 0],
                         wrist_r=w, elbow_r=[0, 0, -1], hand_r=[math.cos(a), 0, math.sin(a)],
                         palm_r=[math.sin(a),0,-math.cos(a)], grip_r=0.45,
                         wrist_l=[w[0]+.0765*math.cos(a)-.05,w[1]+.16,w[2]+.0765*math.sin(a)-.075] if assisting else lap, elbow_l=[0, 0, -1],
                         hand_l=[0.3, -0.95, 0], palm_l=[0,0,1], grip_l=0.2)
    raised, raised_free = key(30, True), key(30, False)
    lower, lower_assist = key(-30, False), key(-30, True)
    return dict(seconds=7.0, keys=[(0, raised), (0.10, raised), (0.13, raised_free),
                                   (0.84, lower), (0.86, lower_assist), (0.99, raised)],
                cam_az=35, cam_el=18,
                props=[dict(kind='weight', type='dumbbell', joint='wrist_r', off=[0, 0, -0.03])])


def _side_base(roll=-90, pelvis=[0, 0, 0.176]):
    # Near-side upper body is propped by a real forearm contact; the free hand rests on the belly.
    arms = (dict(wrist_l=[0.206, 0, 0.040], wrist_r=[0.10, -0.05, 0.30],
                 elbow_l=[0, 0, -1], elbow_r=[0, -1, 0]) if roll < 0 else
            dict(wrist_r=[0.206, 0, 0.040], wrist_l=[0.10, 0.05, 0.30],
                 elbow_r=[0, 0, -1], elbow_l=[0, 1, 0]))
    return dict(pelvis=list(pelvis), p1=50, p2=50, roll=roll, hp=0, **arms,
                hand_l=[-1, 0, 0], hand_r=[-1, 0, 0], palm_l=[0, 0, -1], palm_r=[0, 0, -1],
                grip_l=0, grip_r=0)


def _side_pose(roll=-90, pelvis=None, **over):
    pose = _side_base(roll=roll, pelvis=pelvis or [0, 0, 0.176])
    pose.update(over)
    return base_pose(**pose)


def hip_lateral_leg_raise():
    # Working right/top ankle follows a dense .869 m arc about its anatomical hip.
    def key(t):
        lift = 0.14 * (0.5 - 0.5 * math.cos(2 * math.pi * t))
        hip_x, hip_z = -0.081, 0.226
        ankle_z = 0.12 + lift
        dx = math.sqrt(0.869**2 - (ankle_z - hip_z)**2)
        return _side_pose(ankle_r=[hip_x - dx, 0, ankle_z], ankle_l=[0.035 - 0.869, 0, 0.08],
                          knee_r=[0, 0, 1], knee_l=[0, 0, 1],
                          foot_r=[-1, 0, 0], foot_l=[-1, 0, 0])
    keys = [(i / 48, key(i / 48)) for i in range(48)]
    return dict(seconds=3.2, keys=keys, cam_az=25, cam_el=20, props=[])


def hip_medial_leg_raise():
    # Lie on the right side so the right working leg is the lower leg; left/top foot plants
    # behind the working knee. Pelvis and trunk stay lifted on the right forearm.
    def key(t):
        lift = 0.18 * (0.5 - 0.5 * math.cos(2 * math.pi * t))
        hip_x, hip_z = 0.035, 0.088
        ankle_z = 0.08 + lift
        dx = math.sqrt(0.869**2 - (ankle_z - hip_z)**2)
        return _side_pose(roll=90, pelvis=[0, 0, 0.176],
                          ankle_r=[hip_x - dx, 0, ankle_z], ankle_l=[-0.35, 0.20, 0.08],
                          knee_r=[0, 0, 1], knee_l=[0, 0, 1],
                          foot_r=[-1, 0, 0], foot_l=[1, 0, 0])
    keys = [(i / 48, key(i / 48)) for i in range(48)]
    return dict(seconds=3.2, keys=keys, cam_az=28, cam_el=20, props=[])


def clamshell():
    # Horizontal pelvis with an elbow-propped upper trunk. Feet stay stacked;
    # the top knee opens upward without rolling the pelvis.
    def key(t):
        a=math.radians(40)*(.5-.5*math.cos(2*math.pi*t))
        p=_side_pose(pelvis=[0,0,.176],p1=90,p2=40,
            ankle_l=[-.47,.43,.08],ankle_r=[-.47,.43,.12],
            knee_l=[-1,0,0],knee_r=[-math.cos(a),0,math.sin(a)],
            foot_l=[0,1,0],foot_r=[0,1,0])
        # Exact floor elbow and forearm contact for the inclined upper trunk.
        sx=.24+.23*math.sin(math.radians(40))+.18*math.cos(math.radians(40))
        sz=.176+.23*math.cos(math.radians(40))-.18*math.sin(math.radians(40))
        dx=math.sqrt(.30**2-(sz-.04)**2)
        p['wrist_l']=[sx-dx-.27,0,.04]
        p['elbow_l']=[-dx,0,.04-sz]
        p['hand_l']=[-1,0,0]
        return p
    return dict(seconds=3.2,keys=[(i/64,key(i/64)) for i in range(64)],cam_az=28,cam_el=20,props=[])


def side_lying_leg_tuck():
    def key(t):
        a=math.pi/2*(.5-.5*math.cos(2*math.pi*t))
        # Exact anatomical hip from this fixed side-prop setup.
        h=[-.03*math.sin(math.radians(50))-.09*math.cos(math.radians(50)),0,
           .176-.03*math.cos(math.radians(50))+.09*math.sin(math.radians(50))]
        u=[-math.cos(a),math.sin(a),0]
        # A millimetre of slack avoids a fully locked IK singularity.
        ankle=[h[0]+.44*u[0]-.429,h[1]+.44*u[1],h[2]]
        return _side_pose(ankle_r=ankle,ankle_l=[-.834,0,.08],
            knee_r=u,knee_l=[0,0,1],foot_r=[-1,0,0],foot_l=[-1,0,0])
    return dict(seconds=3.4,keys=[(i/64,key(i/64)) for i in range(64)],cam_az=28,cam_el=20,props=[])


def _quadruped(working='r', theta=0.0, donkey=False):
    """Fixed all-fours support with an explicitly constructed 90-degree working knee."""
    pelvis = [0, 0, 0.52]
    hip_l = [-0.02954, 0.09, 0.51479]
    hip_r = [-0.02954, -0.09, 0.51479]
    # The left knee rests beneath the hip; its ankle extends the shin back to the floor.
    knee_l = [hip_l[0], hip_l[1], hip_l[2] - 0.44]
    ankle_l = [knee_l[0] - 0.43, knee_l[1], knee_l[2]]
    if donkey:
        u = [-math.sin(theta), 0, -math.cos(theta)]
        v = [-math.cos(theta), 0, math.sin(theta)]
    else:
        u = [0, -math.sin(theta), -math.cos(theta)]
        v = [-1, 0, 0]
    knee_r = [hip_r[i] + 0.44 * u[i] for i in range(3)]
    ankle_r = [knee_r[i] + 0.43 * v[i] for i in range(3)]
    return base_pose(
        pelvis=pelvis, p1=80, p2=80, hp=0,
        wrist_l=[0.48, 0.18, 0.035], wrist_r=[0.48, -0.18, 0.035],
        elbow_l=[0, 0, -1], elbow_r=[0, 0, -1],
        hand_l=[1, 0, 0], hand_r=[1, 0, 0], palm_l=[0, 0, -1], palm_r=[0, 0, -1],
        grip_l=0, grip_r=0,
        ankle_l=ankle_l, ankle_r=ankle_r,
        knee_l=[0, 0, -1], knee_r=u,
        foot_l=[-1, 0, 0], foot_r=[-1, 0, 0])


def fire_hydrant():
    # Bent right knee sweeps laterally while the torso, left leg and both hands stay planted.
    keys = [(i / 48, _quadruped(theta=math.radians(70 * (0.5 - 0.5 * math.cos(2 * math.pi * i / 48)))))
            for i in range(48)]
    return dict(seconds=3.0, keys=keys, cam_az=28, cam_el=22, props=[])


def donkey_kick():
    # Knee stays bent as the thigh moves back and the heel arcs upward toward the ceiling.
    keys = [(i / 48, _quadruped(theta=math.radians(90 * (0.5 - 0.5 * math.cos(2 * math.pi * i / 48))), donkey=True))
            for i in range(48)]
    return dict(seconds=3.0, keys=keys, cam_az=28, cam_el=22, props=[])


EXERCISES = {
    'wrist-palm-pulses': wrist_palm_pulses,
    'wrist-seated-mobility': wrist_seated_mobility,
    'forearm-rotation-fist': forearm_rotation_fist,
    'wrist-pushup-knees': wrist_pushup_knees,
    'wrist-pushup': wrist_pushup,
    'wrist-curl-eccentric': wrist_curl_eccentric,
    'hip-lateral-leg-raise': hip_lateral_leg_raise,
    'hip-medial-leg-raise': hip_medial_leg_raise,
    'clamshell': clamshell,
    'side-lying-leg-tuck': side_lying_leg_tuck,
    'fire-hydrant': fire_hydrant,
    'donkey-kick': donkey_kick,
}
