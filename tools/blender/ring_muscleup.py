"""Controlled false-grip ring muscle-up with coupled hand and apparatus motion."""
import math
from poses import base_pose,sample_continuous,steady_progress,solve
from ring_geometry import flexed_hand_axes

DEFAULTS = {'bottom_z': 1.21, 'top_z': 2.32, 'along': 0.05, 'palm': 0.015, 'lateral': 0.08, 'elbow_y': 0.0, 'grip_start': 40.0, 'grip_end': 65.0, 'turn': 30.0, 'width': 0.09, 'elbow_turn_z': -2.0, 'center_travel': -0.04, 'hang_flex': 90.0, 'support_flex': 18.0}


def specification(parameters=None,_endpoints_only=False):
    p={**DEFAULTS,**(parameters or {})}
    def body(z,x,pitch,elbow_z):
        return base_pose(pelvis=[x-.5*math.sin(math.radians(pitch)),0,z],p1=pitch,p2=pitch,hp=-2,
            wrist_l=[.04,.31,2.25],wrist_r=[.04,-.31,2.25],
            elbow_l=[-1,p['elbow_y'],elbow_z],elbow_r=[-1,-p['elbow_y'],elbow_z],
            ankle_l_rel=[0,.02,-.869],ankle_r_rel=[0,-.02,-.869],
            knee_l=[1,0,0],knee_r=[1,0,0],foot_l=[1,0,0],foot_r=[1,0,0],
            hand_l=[1,0,0],hand_r=[1,0,0],palm_l=[0,0,-1],palm_r=[0,0,-1],
            grip_l=.95,grip_r=.95)
    if not _endpoints_only:
        # Derive the body endpoints from the actual gripped wrist targets.
        # Changing a false grip must not silently shorten the hang or support.
        span=.565
        for _ in range(8):
            endpoints=specification(p,_endpoints_only=True)['keys']
            adjustments=[]
            for index,(_,key) in enumerate(endpoints):
                joints=solve(key,{})
                shoulder=joints['shoulder_l'];wrist=key['wrist_l']
                horizontal=(wrist[0]-shoulder[0])**2+(wrist[1]-shoulder[1])**2
                if horizontal>=span**2:break
                vertical=math.sqrt(span**2-horizontal)
                desired=wrist[2]+(-vertical if index==0 else vertical)
                adjustments.append(desired-shoulder[2])
            if len(adjustments)!=2:break
            p['bottom_z']+=adjustments[0];p['top_z']+=adjustments[1]
            if max(abs(v) for v in adjustments)<.000001:break
    shape=[(0,body(p['bottom_z'],-.06,-2,-1)),(.38,body(1.67,-.12,-2,-1)),
           (.62,body(1.84,.10,14,p['elbow_turn_z'])),(.76,body(1.97,.08,10,.4)),
           (1,body(p['top_z'],0,0,.6))]
    radius=.105;anchor_x=.12;anchor_z=3.05;strap=.65
    anchors={side:[anchor_x,sign*.26,anchor_z] for side,sign in (('l',1),('r',-1))}
    keys=[]
    for i in ([0,96] if _endpoints_only else range(193)):
        t=i/192
        if t<.05:s=0.
        elif t<.45:s=steady_progress((t-.05)/.40)
        elif t<.55:s=1.
        elif t<.95:s=1-steady_progress((t-.55)/.40)
        else:s=0.
        k=sample_continuous(shape,s)
        yaw=math.radians(p['turn']*s)
        beta=math.radians(p['grip_start']+(p['grip_end']-p['grip_start'])*s)
        flex=sample_continuous([(0,{'v':p['hang_flex']}),(.40,{'v':p['hang_flex']}),
                                (.78,{'v':25.}),(1,{'v':p['support_flex']})],s)['v']
        for side,sign in (('l',1),('r',-1)):
            normal=[math.sin(yaw),-sign*math.cos(yaw),0.]
            radial=[math.sin(beta)*math.cos(yaw),sign*math.sin(beta)*math.sin(yaw),math.cos(beta)]
            x=anchor_x+p['center_travel']*math.sin(math.pi*s)
            y=sign*(.24+p['width']*s)
            top_z=anchor_z-math.sqrt(strap**2-(x-anchor_x)**2-(y-sign*.26)**2)
            center=[x,y,top_z-radius]
            target=[a-radius*b for a,b in zip(center,radial)]
            k['ring_center_'+side]=center
            k['ring_plane_'+side+'_up']=normal
            k['ring_radial_'+side+'_up']=radial
            # Besides heel/palm offsets, the real heel contacts the side of
            # the wood. A separate hoop-normal coefficient lets the forearm
            # remain beside the hoop rather than passing through its plane.
            offset=[p['along'],p['palm'],p['lateral']]
            k['grip_offset_'+side]=offset
            k['wrist_'+side]=[target[0],target[1]-p['lateral']*normal[1],target[2]+.03]
            for _ in range(30):
                joints=solve(k,{})
                fore=[joints['wrist_'+side][j]-joints['elbow_'+side][j] for j in range(3)]
                hand,palm=flexed_hand_axes(fore,flex)
                k['hand_'+side]=hand;k['palm_'+side]=palm
                wrist=[target[j]-offset[0]*hand[j]-offset[1]*palm[j]-offset[2]*normal[j] for j in range(3)]
                k['wrist_'+side]=wrist
        keys.append((t,k))
    rings=[dict(kind='ring',joint='wrist_'+side,radius=radius,tube=.014,anchor=anchors[side],
                color=[.55,.29,.10],frame_center='ring_center_'+side,
                frame_normal='ring_plane_'+side+'_up',frame_radial='ring_radial_'+side+'_up',
                articulated_contact=True) for side in ('l','r')]
    return dict(seconds=8,keys=keys,interpolation='continuous',cam_az=42,cam_el=10,
                props=rings,ring_grip=True,grip_detail='RING GRIP DETAIL',anatomy_detail=True,
                motion_profile='strict-false-grip-moving-rings-v3')
