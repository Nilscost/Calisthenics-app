"""Shared, pure-Python ring/hand contact geometry for the strict muscle-up pilot."""
import math
from bar_grip import wrapped_segments


def add(a,b):return [x+y for x,y in zip(a,b)]
def mul(v,s):return [x*s for x in v]
def cross(a,b):return [a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]]


def radial_axis(spec, frame=None):
    if frame is not None and spec.get('frame_radial'):
        return frame[spec['frame_radial']]
    angle=math.radians(spec.get('grip_angle_deg',0))
    return [math.sin(angle),0,math.cos(angle)]


def ring_frame(frame,spec):
    if spec.get('frame_center'):
        centre=frame[spec['frame_center']]
        normal=frame[spec['frame_normal']]
        radial=radial_axis(spec,frame)
        return centre,normal,cross(radial,normal),add(centre,mul(radial,-spec['radius']))
    if 'fixed_center' in spec:
        centre=spec['fixed_center'];n=spec['plane_normal'];radial=radial_axis(spec,frame)
        return centre,n,cross(radial,n),add(centre,mul(radial,-spec['radius']))
    side=spec['joint'][-1];w=frame[spec['joint']]
    n=frame['palmn_'+side];radial=radial_axis(spec,frame);tangent=cross(radial,n)
    grip=add(add(w,mul(n,.045)),mul(radial,.065))
    return add(grip,mul(radial,spec['radius'])),n,tangent,grip


def ring_outline(frame,spec,steps=48):
    centre,n,tangent,_=ring_frame(frame,spec)
    radius=spec['radius']+spec['tube']
    return [add(centre,add(mul(tangent,radius*math.sin(i*2*math.pi/steps)),mul(radial_axis(spec,frame),radius*math.cos(i*2*math.pi/steps)))) for i in range(steps)]


def articulated_segments(frame,side,spec):
    """Attach knuckles to the palm; wrap each finger around the actual hoop.

    The ring has an independent per-frame or legacy fixed plane. A hand may change pitch through
    turnover without taking the entire ring plane or its strap with it.
    """
    from hand_axes import axes
    wrist=frame['wrist_'+side]
    u,n,across=axes(wrist,frame['hand_'+side],frame['palmn_'+side],side)
    centre,ring_n,_,_=ring_frame(frame,spec);segments=[]
    def nearest(point):
        v=[a-b for a,b in zip(point,centre)]
        projection=sum(a*b for a,b in zip(v,ring_n))
        v=[a-projection*b for a,b in zip(v,ring_n)]
        length=math.sqrt(sum(x*x for x in v))
        if length<1e-8:raise ValueError("finger knuckle lies on the ring axis")
        v=[x/length for x in v]
        return add(centre,mul(v,spec['radius'])),mul(v,-1)
    for offset,end in ((.030,-20),(.010,-30),(-.010,-25),(-.029,0)):
        base=add(wrist,add(mul(u,.085),mul(across,offset)))
        contact,inward=nearest(base);points=[base]
        if spec.get('frame_center'):
            # Approach the tube from the actual knuckle side. A fixed initial
            # curl angle can send the first phalanx through the wood when the
            # palm turns independently of the ring during a false-grip roll.
            delta=[a-b for a,b in zip(base,contact)]
            start=math.atan2(sum(a*b for a,b in zip(delta,inward)),
                             sum(a*b for a,b in zip(delta,ring_n)))
            angles=[start,start-math.radians(60),start-math.radians(120)]
        else:
            angles=[math.radians(degrees) for degrees in (110,45,end)]
        for a in angles:
            radius=spec['tube']+.0088+.0082
            points.append(add(contact,mul(add(mul(ring_n,math.cos(a)),mul(inward,math.sin(a))),radius)))
        segments.extend(zip(points,points[1:]))
    base=add(wrist,add(mul(u,.045),mul(across,.032)))
    contact,inward=nearest(base)
    if spec.get('frame_center'):
        delta=[a-b for a,b in zip(base,contact)]
        start=math.atan2(sum(a*b for a,b in zip(delta,inward)),
                         sum(a*b for a,b in zip(delta,ring_n)))
        radius=spec['tube']+.0105+.0082
        points=[base]+[add(contact,mul(add(mul(ring_n,math.cos(a)),mul(inward,math.sin(a))),radius))
                       for a in (start,start+math.radians(65))]
    else:
        points=[base,add(contact,add(mul(ring_n,-.009),mul(inward,-.037))),
                add(contact,add(mul(ring_n,.027),mul(inward,-.028)))]
    segments.extend(zip(points,points[1:]))
    return segments


def hand_segments(frame,side,spec):
    if spec.get('hand_model') == 'rigid-phalanx-v1':
        from ring_hand_rig import hand_segments as rigid_segments
        return rigid_segments(frame,side,spec)
    if spec.get('articulated_contact'):
        return articulated_segments(frame,side,spec)
    centre,n,tangent,grip=ring_frame(frame,spec)
    # Reuse the collision-aware curl, in a local frame normal to the ring.
    pseudo={'wrist_'+side:[-.045,0,-.065],'hand_'+side:[-.045,0,.105],
            'palmn_'+side:[1,0,0]}
    segments=wrapped_segments(pseudo,side,{'z':0,'r':spec['tube']})
    def world(p):
        # Follow the ring's curvature across each finger's width.
        rise=spec['radius']-math.sqrt(max(0,spec['radius']**2-p[1]**2))
        if 'fixed_center' in spec:
            wrist=frame['wrist_'+side];hand=frame['hand_'+side]
            u=[b-a for a,b in zip(wrist,hand)];length=math.sqrt(sum(x*x for x in u));u=[x/length for x in u]
            return add(grip,add(mul(frame['palmn_'+side],p[0]),add(mul(tangent,p[1]),add(mul(u,p[2]),mul(radial_axis(spec,frame),rise)))))
        return add(grip,add(mul(n,p[0]),add(mul(tangent,p[1]),mul(radial_axis(spec,frame),p[2]+rise))))
    return [(world(a),world(b)) for a,b in segments]


def torus_clearance(point,frame,spec):
    centre,n,_,_=ring_frame(frame,spec);v=[a-b for a,b in zip(point,centre)]
    normal=sum(a*b for a,b in zip(v,n))
    radial=math.sqrt(max(0,sum(a*a for a in v)-normal*normal))
    return math.hypot(radial-spec['radius'],normal)-spec['tube']


def capsule_ring_clearance(a,b,r1,r2,frame,spec,steps=64):
    """Conservative whole tapered-capsule clearance, including gaps in sampling.

    Torus distance is 1-Lipschitz; radius also changes linearly. A positive bound
    establishes separation even between samples and for the capsule end caps.
    """
    values=[]
    for i in range(steps+1):
        t=i/steps;point=[x+(y-x)*t for x,y in zip(a,b)]
        values.append(torus_clearance(point,frame,spec)-(r1*(1-t)+r2*t))
    return min(values)-(math.dist(a,b)+abs(r1-r2))/(2*steps)


def strap_attachment(frame,spec):
    """Highest point of the actual hoop, including a tilted ring plane."""
    centre,normal,_,_=ring_frame(frame,spec)
    vertical=[-normal[2]*normal[0],-normal[2]*normal[1],1-normal[2]**2]
    length=math.sqrt(sum(v*v for v in vertical))
    if length<1e-8:
        vertical=radial_axis(spec,frame);length=math.sqrt(sum(v*v for v in vertical))
    return add(centre,mul(vertical,spec['radius']/length))


def grip_offset(frame,side):
    """Wrist-to-contact coefficients along fingertip and palm-normal axes."""
    return frame.get('grip_offset_'+side,[.065,.045])


def palm_surface_clearance(frame,spec,rings=16,segments=32):
    """Sample the renderer's palm, pad and dorsum ellipsoids against the hoop.

    This supplements, rather than replaces, actual native mesh verification.
    Coarser grids are useful for parameter search; acceptance uses defaults.
    """
    from hand_axes import axes
    side=spec['joint'][-1];wrist=frame['wrist_'+side]
    u,n,_=axes(wrist,frame['hand_'+side],frame['palmn_'+side],side)
    across=cross(n,u)
    center=add(wrist,add(mul(u,.085*.55),mul(n,.004)))
    minimum=math.inf
    for rx,ry,rz,shift in ((.042,.016,.052,0),(.034,.004,.037,.016),(.028,.004,.032,-.016)):
        c=add(center,mul(n,shift))
        for i in range(rings+1):
            latitude=math.pi*i/rings;z=rz*math.cos(latitude)
            for j in range(segments):
                longitude=2*math.pi*j/segments
                x=rx*math.sin(latitude)*math.cos(longitude)
                y=ry*math.sin(latitude)*math.sin(longitude)
                point=[c[k]+across[k]*x+n[k]*y+u[k]*z for k in range(3)]
                minimum=min(minimum,torus_clearance(point,frame,spec))
    return minimum


def finger_volume_clearance(frame,spec,steps=64):
    """Conservative full finger capsules, including between-sample coverage."""
    minimum=math.inf
    for i,(a,b) in enumerate(hand_segments(frame,spec['joint'][-1],spec)):
        radius=.0105 if i>=12 else .0088
        sampled=min(torus_clearance([x+(y-x)*j/steps for x,y in zip(a,b)],frame,spec)-radius
                    for j in range(steps+1))
        minimum=min(minimum,sampled-math.dist(a,b)/(2*steps))
    return minimum


def flexed_hand_axes(forearm,flexion_deg,lateral=(0,1,0)):
    """Continuous forearm-relative palm frame for a prescribed palmar flexion.

    The lateral axis defines the bend plane. Keeping its sign avoids a spurious
    180-degree palm flip when the forearm passes through horizontal.
    """
    def unit(v):
        length=math.sqrt(sum(x*x for x in v))
        if length<1e-8:raise ValueError('degenerate forearm flexion frame')
        return [x/length for x in v]
    fore=unit(forearm)
    axis=unit([x-y*sum(a*b for a,b in zip(lateral,fore)) for x,y in zip(lateral,fore)])
    bend=cross(axis,fore);angle=math.radians(flexion_deg)
    hand=add(mul(fore,math.cos(angle)),mul(bend,math.sin(angle)))
    palm=add(mul(fore,-math.sin(angle)),mul(bend,math.cos(angle)))
    return hand,palm


def capsule_vertex_z(z, initial_length, current_length):
    """Move a capsule endpoint without stretching its hemispherical caps."""
    if initial_length <= 0 or current_length <= 0:
        raise ValueError('capsule lengths must be positive')
    if z < 0:return z
    if z > initial_length:return z + current_length-initial_length
    return z*current_length/initial_length
