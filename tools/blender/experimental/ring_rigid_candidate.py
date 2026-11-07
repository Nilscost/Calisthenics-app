"""Controlled false-grip ring muscle-up with coupled hand and apparatus motion."""
import math
from poses import base_pose,sample_continuous,steady_progress,solve

DEFAULTS = {'bottom_z': 1.21, 'top_z': 2.32, 'elbow_y': 0.0, 'grip_start': 40.0, 'grip_end': -55.0, 'turn': 30.0, 'width': 0.09, 'elbow_turn_x': 1.0, 'elbow_turn_y': -1.0, 'elbow_turn_z': -2.0, 'elbow_ring_bias': 0.0, 'shoulder_splay': 0.08, 'center_travel': -0.04, 'grip_roll_deg': 90.0, 'grip_along': 0.050, 'palm_inset': 0.0415, 'roll_start': 0.74, 'roll_end': 0.96}


def specification(parameters=None,_endpoints_only=False):
    p={**DEFAULTS,**(parameters or {})}
    def body(z,x,pitch,elbow_x,elbow_y,elbow_z):
        return base_pose(pelvis=[x-.5*math.sin(math.radians(pitch)),0,z],p1=pitch,p2=pitch,hp=-2,
            wrist_l=[.04,.31,2.25],wrist_r=[.04,-.31,2.25],
            elbow_l=[elbow_x,elbow_y,elbow_z],elbow_r=[elbow_x,-elbow_y,elbow_z],
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
    shape=[(0,body(p['bottom_z'],-.06,-2,-1,p['elbow_y'],-1)),(.38,body(1.67,-.12,-2,-1,p['elbow_y'],-1)),
           (.62,body(1.84,.10,14,p['elbow_turn_x'],p['elbow_turn_y'],p['elbow_turn_z'])),(.76,body(1.97,.08,10,p['elbow_turn_x'],p['elbow_turn_y'],.4)),
           (1,body(p['top_z'],0,0,p['elbow_turn_x'],p['elbow_turn_y'],.6))]
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
        roll_fraction=max(0.,min(1.,(s-p['roll_start'])/(p['roll_end']-p['roll_start'])))
        roll=math.radians(p['grip_roll_deg']*steady_progress(roll_fraction))
        for side,sign in (('l',1),('r',-1)):
            normal=[math.sin(yaw),-sign*math.cos(yaw),0.]
            guide=k['elbow_'+side]
            k['elbow_'+side]=[guide[j]+p['elbow_ring_bias']*s*normal[j] for j in range(3)]
            radial=[math.sin(beta)*math.cos(yaw),sign*math.sin(beta)*math.sin(yaw),math.cos(beta)]
            x=anchor_x+p['center_travel']*math.sin(math.pi*s)
            y=sign*(.24+p['width']*s)
            top_z=anchor_z-math.sqrt(strap**2-(x-anchor_x)**2-(y-sign*.26)**2)
            center=[x,y,top_z-radius]
            target=[a-radius*b for a,b in zip(center,radial)]
            k['ring_center_'+side]=center
            k['ring_plane_'+side+'_up']=normal
            k['ring_radial_'+side+'_up']=radial
            k['shoulder_offset_'+side]=[0., sign*p['shoulder_splay']*s, 0.]
            tangent=[radial[1]*normal[2]-radial[2]*normal[1],
                     radial[2]*normal[0]-radial[0]*normal[2],
                     radial[0]*normal[1]-radial[1]*normal[0]]
            # Roll the whole palm/forearm presentation in the hoop cross
            # section. The knuckle row remains tangent to the ring, while the
            # wrist travels smoothly from the outside of the hoop into support.
            hand=[math.cos(roll)*normal[j]-math.sin(roll)*radial[j] for j in range(3)]
            palm=[-math.sin(roll)*normal[j]-math.cos(roll)*radial[j] for j in range(3)]
            k['wrist_'+side]=[target[j]-p['grip_along']*hand[j]-p['palm_inset']*palm[j] for j in range(3)]
            k['grip_offset_'+side]=[p['grip_along'],p['palm_inset'],0.]
            k['hand_'+side]=hand
            k['palm_'+side]=palm
        keys.append((t,k))
    rings=[dict(kind='ring',joint='wrist_'+side,radius=radius,tube=.014,anchor=anchors[side],
                color=[.55,.29,.10],frame_center='ring_center_'+side,
                frame_normal='ring_plane_'+side+'_up',frame_radial='ring_radial_'+side+'_up',
                articulated_contact=True,hand_model='rigid-phalanx-v1') for side in ('l','r')]
    return dict(seconds=8,keys=keys,interpolation='continuous',cam_az=42,cam_el=10,
                props=rings,ring_grip=True,grip_detail='RING GRIP DETAIL',anatomy_detail=True,
                motion_profile='strict-false-grip-moving-rings-v3')
