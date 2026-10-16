"""Shoulder prehab and scapular-control poses."""
import math

from poses import solve, base_pose, dense, pt, rep, rot, trunk_frame


def _wave(fn, steps=48):
    """Dense cosine-paced out-and-back rep; endpoint contacts stay exact."""
    keyed = [(0.0, {"q": 0.0}), (0.12, {"q": 0.0}),
             (0.48, {"q": 1.0}), (0.58, {"q": 1.0}),
             (0.98, {"q": 0.0})]
    return dense(fn, keyed, steps=steps)


def _shoulder(pelvis, p1=0.0, p2=0.0, roll=0.0, side="r"):
    up1,left1=trunk_frame(p1,0.0,roll)
    up2,left2=trunk_frame(p2,0.0,roll)
    chest=[pelvis[i]+0.24*up1[i]+0.26*up2[i] for i in range(3)]
    sign=1.0 if side=="l" else -1.0
    return [chest[i]+sign*0.18*left2[i]-0.03*up2[i] for i in range(3)]


def _prone(arm_shape, lift=0.0):
    """Prone arm geometry with 30cm upper arms and 27cm forearms.

    Elbows and wrists start at floor height, and rise about 9cm and 10.5cm
    respectively while the two segment lengths stay exact. The mild elbow
    bend is explicit for I/T/Y; L and W retain their bent shoulder patterns.
    """
    q = max(0.0, min(1.0, lift / .105))
    pelvis = [0, 0, .11]
    # Joint positions are specified shoulder-relative. For the straight-arm
    # patterns use a shallow 0.5cm elbow bend so both dimensions are exact.
    z_e = -.075 + .090*q
    z_w = -.075 + .105*q
    ue_h = math.sqrt(.30*.30-z_e*z_e)
    fw_z = z_w-z_e
    fw_h = math.sqrt(.27*.27-fw_z*fw_z)
    if arm_shape == 'I':
        directions = [(1, 0), (1, 0)]
    elif arm_shape == 'T':
        directions = [(0, 1), (0, 1)]
    elif arm_shape == 'Y':
        directions = [(2**-.5, 2**-.5), (2**-.5, 2**-.5)]
    elif arm_shape == 'L':
        directions = [None, None]
    else:  # W: upper arm back/out, forearm bends 45 degrees toward the head.
        directions = [None, None]

    side = {}
    for label, sign in (('l', 1), ('r', -1)):
        if arm_shape == 'L':
            upper_xy = [0.0, sign*ue_h]
            fore_xy = [fw_h, 0.0]
        elif arm_shape == 'W':
            upper_xy = [-ue_h/math.sqrt(2), sign*ue_h/math.sqrt(2)]
            fore_xy = [fw_h, 0.0]
        else:
            a, b = directions[0]
            upper_xy = [ue_h*a, ue_h*b*sign]
            # I/T/Y arms continue forward along their same ray.
            fore_xy = [fw_h*a, fw_h*b*sign]
        elbow = [upper_xy[0], upper_xy[1], z_e]
        wrist = [elbow[0]+fore_xy[0], elbow[1]+fore_xy[1], z_w]
        side['wrist_'+label+'_rel'] = wrist
        side['elbow_'+label] = _pole_for([0, 0, 0], elbow, wrist)
        side['hand_'+label] = [fore_xy[0],fore_xy[1],fw_z]
    return base_pose(pelvis=pelvis, p1=90, p2=90, hp=-8,
        ankle_l=[-.8994,.09,.11], ankle_r=[-.8994,-.09,.11],
        knee_l=[0,0,1], knee_r=[0,0,1], foot_l=[-1,0,0], foot_r=[-1,0,0],
        palm_l=[0,1,0], palm_r=[0,-1,0], grip_l=0, grip_r=0, **side)


def _prone_raise(shape):
    keys = dense(lambda q: _prone(shape, .105*q),
                 [(0.0, {'q':0.0}), (.12, {'q':0.0}), (.48, {'q':1.0}),
                  (.58, {'q':1.0}), (.98, {'q':0.0})], steps=48)
    return dict(seconds=4.0, keys=keys, cam_az=28, cam_el=24, props=[])


def prone_i_raise(): return _prone_raise("I")
def prone_t_raise(): return _prone_raise("T")
def prone_y_raise(): return _prone_raise("Y")
def prone_l_raise(): return _prone_raise("L")
def prone_w_raise(): return _prone_raise("W")


def _wall_push(q):
    # Translate the trunk toward the wall while holding shoulder roots and
    # wrists still. The offset isolates scapular glide from arm movement.
    glide = 0.035 * q
    return base_pose(pelvis=[glide,0,0.955],p1=0,p2=0,hp=0,
        ankle_l=[0,0.10,0.08],ankle_r=[0,-0.10,0.08],
        knee_l=[1,0,0],knee_r=[1,0,0],foot_l=[1,0,0],foot_r=[1,0,0],
        shoulder_offset_l=[-glide,0,0],shoulder_offset_r=[-glide,0,0],
        wrist_l=[0.568,0.18,1.425],wrist_r=[0.568,-0.18,1.425],
        elbow_l=[1,0,0],elbow_r=[1,0,0],hand_l=[0,0,1],hand_r=[0,0,1],
        palm_l=[1,0,0],palm_r=[1,0,0],grip_l=0,grip_r=0)


def scapular_pushup_wall():
    # The near face is x=.588; the rendered palm outer surface reaches x=.588.
    wall=dict(kind="box",center=[0.618,0,1.2],size=[0.06,1.6,2.4])
    return dict(seconds=3.5,keys=_wave(_wall_push),cam_az=0,cam_el=12,props=[wall],
                camera_focus=['head','chest','shoulder_l','shoulder_r','elbow_l','elbow_r','wrist_l','wrist_r'],camera_padding=.10)


def _rotate_y(point, pivot, degrees):
    v=[point[i]-pivot[i] for i in range(3)]
    v=rot(v,[0,1,0],degrees)
    return [pivot[i]+v[i] for i in range(3)]


def _fix_shoulder_roots(k, targets):
    """Offset moved torso shoulders back to fixed world-space arm roots."""
    joints=solve(k,{})
    _,left=trunk_frame(k['p2'],k.get('yaw',0),k.get('roll',0))
    up,_=trunk_frame(k['p2'],k.get('yaw',0),k.get('roll',0))
    forward=[left[1]*up[2]-left[2]*up[1],
             left[2]*up[0]-left[0]*up[2],
             left[0]*up[1]-left[1]*up[0]]
    for side in ('l','r'):
        delta=[targets[side][i]-joints['shoulder_'+side][i] for i in range(3)]
        k['shoulder_offset_'+side]=[
            sum(delta[i]*forward[i] for i in range(3)),
            sum(delta[i]*left[i] for i in range(3)),
            sum(delta[i]*up[i] for i in range(3)),
        ]
    return k


def _knee_plank(q, full=False):
    """Small rigid trunk pivot with fixed arm roots and hand/knee/toe contacts."""
    if full:
        ang=22.5;ax=-1.248
        p=[ax+.898*math.cos(math.radians(ang)),0,.08+.898*math.sin(math.radians(ang))]
        pitch=90-ang
        ankles=([-1.25,.10,.08],[-1.25,-.10,.08])
        feet=([-1,0,0],[-1,0,0])
        poles=([0,0,-1],[0,0,-1])
    else:
        # Hip, trunk and thigh form one line from the planted knees.
        lo,hi=30.,40.
        for _ in range(40):
            a=(lo+hi)/2;r=math.radians(a)
            sx=-.45+.94*math.cos(r);sz=.05+.94*math.sin(r)
            if math.hypot(sx-.25,sz-.035)<math.sqrt(.568**2-.05**2):lo=a
            else:hi=a
        a=(lo+hi)/2;r=math.radians(a);pitch=90-a
        p=[-.45+.47*math.cos(r),0,.05+.47*math.sin(r)]
        ankles=([-.88,.09,.05],[-.88,-.09,.05])
        feet=([-1,0,0],[-1,0,0])
        poles=([-math.cos(r),0,-math.sin(r)],[-math.cos(r),0,-math.sin(r)])

    base=base_pose(pelvis=p,p1=pitch,p2=pitch,hp=-3,
        ankle_l=ankles[0],ankle_r=ankles[1],knee_l=poles[0],knee_r=poles[1],
        foot_l=feet[0],foot_r=feet[1],
        wrist_l=[0,0,0],wrist_r=[0,0,0],
        elbow_l=[0,1,0],elbow_r=[0,-1,0],hand_l=[1,0,0],hand_r=[1,0,0],
        palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=0,grip_r=0)
    base_j=solve(base,{})
    roots={s:base_j['shoulder_'+s] for s in ('l','r')}
    wrists={s:base_j['shoulder_'+s] for s in ('l','r')}
    # Keep the arms slightly flexed but nearly straight, with the same wrist
    # points throughout the cycle. The baseline reach is 0.564 m (about 175°).
    for side in ('l','r'):
        wrists[side]=[roots[side][0],roots[side][1],roots[side][2]-.568]

    def pose():
        delta=(1.3 if full else 1.8)*q
        if full:
            toe=[-1.45,0,.08]
            pelvis=_rotate_y(p,toe,delta)
            ankle_targets=tuple(_rotate_y(a,toe,delta) for a in ankles)
            foot_dirs=tuple(rot(f,[0,1,0],delta) for f in feet)
            knee_poles=tuple(rot(v,[0,1,0],delta) for v in poles)
        else:
            pivot=[-.45,0,.05]
            pelvis=_rotate_y(p,pivot,delta)
            ankle_targets=ankles
            foot_dirs=feet
            knee_poles=poles
        k=base_pose(pelvis=pelvis,p1=pitch+delta,p2=pitch+delta,hp=-3,
            ankle_l=ankle_targets[0],ankle_r=ankle_targets[1],
            knee_l=knee_poles[0],knee_r=knee_poles[1],
            foot_l=foot_dirs[0],foot_r=foot_dirs[1],
            wrist_l=wrists['l'],wrist_r=wrists['r'],
            elbow_l=[0,1,0],elbow_r=[0,-1,0],hand_l=[1,0,0],hand_r=[1,0,0],
            palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=0,grip_r=0)
        if not full:
            # Aim the IK pole through the fixed knee contact as the thigh turns.
            raw=solve(k,{})
            for side,sign in (('l',1),('r',-1)):
                fixed=[-.45,sign*.09,.05]
                k['knee_'+side]=[fixed[i]-raw['hip_'+side][i] for i in range(3)]
        return _fix_shoulder_roots(k,roots)
    return pose()


def scapular_pushup_knee():
    return dict(seconds=3.5,keys=_wave(lambda q:_knee_plank(q)),cam_az=30,cam_el=20,props=[],
                camera_focus=['head','chest','shoulder_l','shoulder_r','wrist_l','wrist_r','knee_l','knee_r'],camera_padding=.10)


def scapular_pushup():
    return dict(seconds=3.5,keys=_wave(lambda q:_knee_plank(q,True)),cam_az=30,cam_el=20,props=[],
                camera_focus=['head','chest','shoulder_l','shoulder_r','elbow_l','elbow_r','wrist_l','wrist_r'],camera_padding=.12)


def support_scapular_shrug():
    def pose(q):
        # Scapulae elevate relative to the torso. The torso sinks by the same
        # small amount, keeping the straight-arm bar support at a fixed length.
        rise = 0.05 * q
        return base_pose(pelvis=[0,0,1.244-rise],p1=0,p2=0,hp=0,
          shoulder_offset_l=[0,0,rise],shoulder_offset_r=[0,0,rise],
          wrist_l=[0,0.25,1.15],wrist_r=[0,-0.25,1.15],
          elbow_l=[0,1,0],elbow_r=[0,-1,0],hand_l=[1,0,0],hand_r=[1,0,0],
          palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=.8,grip_r=.8,
          ankle_l_rel=[0,0.10,-0.86],ankle_r_rel=[0,-0.10,-0.86],
          knee_l=[1,0,0],knee_r=[1,0,0],foot_l=[0,0,-1],foot_r=[0,0,-1])
    bars=[dict(kind="rod",a=[-0.8,0.25,1.15],b=[0.8,0.25,1.15],r=.025),
          dict(kind="rod",a=[-0.8,-0.25,1.15],b=[0.8,-0.25,1.15],r=.025)]
    return dict(seconds=3.5,keys=_wave(pose),cam_az=30,cam_el=15,props=bars,
                camera_focus=['head','neck','chest','shoulder_l','shoulder_r','elbow_l','elbow_r','wrist_l','wrist_r'])


def _wall_slide(q):
    # Upper arms and forearms stay against the wall, from goalpost W to Y.
    a=math.radians(55*q)
    b=math.radians(90-35*q)
    r=math.sqrt(.30**2-.07**2)
    p=base_pose(pelvis=[0,0,.955],p1=0,p2=0,hp=0,
        ankle_l=[0,.10,.08],ankle_r=[0,-.10,.08],
        knee_l=[1,0,0],knee_r=[1,0,0],foot_l=[1,0,0],foot_r=[1,0,0],
        palm_l=[-1,0,0],palm_r=[-1,0,0],grip_l=0,grip_r=0)
    for side,sign in [('l',1),('r',-1)]:
        e=[-.07,sign*(.18+r*math.cos(a)),1.425+r*math.sin(a)]
        p['wrist_'+side]=[e[0],e[1]+sign*.27*math.cos(b),e[2]+.27*math.sin(b)]
        p['elbow_'+side]=[-.07,sign*r*math.cos(a),r*math.sin(a)]
        p['hand_'+side]=[0,sign*math.cos(b),math.sin(b)]
    return p


def wall_slide():
    wall=dict(kind="box",center=[-0.13,0,1.2],size=[0.06,1.2,2.4])
    return dict(seconds=4.0,keys=_wave(_wall_slide),cam_az=20,cam_el=12,props=[wall])


def _floor_angel(q):
    # Supine goalpost W to overhead: both elbows and wrists remain on floor.
    a=math.pi/2*q
    r=math.sqrt(.30**2-.07**2)
    f=math.sqrt(.27**2-.005**2)
    p=base_pose(pelvis=[0,0,.11],p1=-90,p2=-90,hp=0,
        ankle_l=[.30,.10,.08],ankle_r=[.30,-.10,.08],
        knee_l=[1,0,1],knee_r=[1,0,1],foot_l=[1,0,0],foot_r=[1,0,0],
        palm_l=[0,0,-1],palm_r=[0,0,-1],grip_l=0,grip_r=0)
    for side,sign in [('l',1),('r',-1)]:
        e=[-.47-r*math.sin(a),sign*(.18+r*math.cos(a)),.04]
        p['wrist_'+side]=[e[0]-f,e[1],.035]
        p['elbow_'+side]=[-r*math.sin(a),sign*r*math.cos(a),-.07]
        p['hand_'+side]=[-1,0,0]
    return p


def floor_angel():
    keys = dense(lambda q: _floor_angel(q),
                 [(0.0, {'q':0.0}), (.12, {'q':0.0}), (.48, {'q':1.0}),
                  (.58, {'q':1.0}), (.98, {'q':0.0})], steps=48)
    return dict(seconds=4.0, keys=keys, cam_az=28, cam_el=24, props=[])


def _side_er(q):
    # Right side is upper and faces camera. Elbow stays against ribs; the
    # forearm endpoint traces a dense 0.27 m arc about that fixed elbow.
    angle=math.radians(90*q)
    pelvis=[0,0,0.176]
    p1=p2=50.0
    sh=_shoulder(pelvis,p1,p2,-90,"r")
    up,_=trunk_frame(p2,0,-90)
    # The upper arm follows the torso down to the ribs; the forearm rotates
    # from across the abdomen toward the ceiling around that fixed elbow.
    e=[sh[i]-0.30*up[i] for i in range(3)]
    v=rot([0,-0.27,0],[-up[0],-up[1],-up[2]],90*q)
    w=[e[i]+v[i] for i in range(3)]
    return base_pose(pelvis=pelvis,p1=p1,p2=p2,roll=-90,hp=0,
      ankle_l=[-0.82,0.10,0.08],ankle_r=[-0.834,-0.10,0.08],
      knee_l=[0,0,1],knee_r=[0,0,1],foot_l=[-1,0,0],foot_r=[-1,0,0],
      wrist_l=[0.206,0,0.040],wrist_r=w,
      elbow_l=[0,0,-1],elbow_r=[-up[0],-up[1],-up[2]],
      hand_r=v,hand_l=[-1,0,0],palm_r=up,palm_l=[0,0,-1],grip_r=0,grip_l=0)


def external_rotation_side_lying():
    return dict(seconds=4.0,keys=_wave(_side_er),cam_az=30,cam_el=22,props=[])


def _standing(side_y=0):
    return base_pose(pelvis=[0,0,0.955],p1=0,p2=0,hp=0,
      ankle_l=[0,0.10,0.08],ankle_r=[0,-0.10,0.08],
      knee_l=[1,0,0],knee_r=[1,0,0],foot_l=[1,0,0],foot_r=[1,0,0])


def external_rotation_band():
    def pose(q):
        a=math.radians(80*q)
        p=_standing()
        sh=_shoulder([0,0,0.955],side="r")
        e=[sh[0],sh[1],sh[2]-0.30]
        w=[e[0]+0.27*math.cos(a),e[1]-0.27*math.sin(a),e[2]]
        p.update(wrist_r=w,elbow_r=[0,0,-1],hand_r=[math.cos(a),-math.sin(a),0],palm_r=[0,0,-1],grip_r=.55)
        return p
    band=dict(kind="rod",a=[0,.85,1.125],b=pt("wrist_r"),r=.008)
    return dict(seconds=4.0,keys=_wave(pose),cam_az=30,cam_el=15,props=[band])


def external_rotation_band_90():
    def pose(q):
        a=math.radians(90*q)
        p=_standing()
        sh=_shoulder([0,0,0.955],side="r")
        e=[sh[0],sh[1]-0.30,sh[2]]
        w=[e[0]+0.27*math.cos(a),e[1],e[2]+0.27*math.sin(a)]
        p.update(wrist_r=w,elbow_r=[0,-1,0],hand_r=[math.cos(a),0,math.sin(a)],palm_r=[-math.sin(a),0,math.cos(a)],grip_r=.55)
        return p
    band=dict(kind="rod",a=[0.72,-0.48,1.425],b=pt("wrist_r"),r=.008)
    return dict(seconds=4.0,keys=_wave(pose),cam_az=30,cam_el=15,props=[band])


def band_pull_apart():
    def pose(q):
        # Straight wrists follow a shoulder-radius arc from in front to wide;
        # the band crosses the chest at the open end of the rep.
        a=math.radians(7+68*q)
        x=0.568*math.cos(a)
        span=0.568*math.sin(a)
        return base_pose(pelvis=[0,0,0.955],p1=0,p2=0,hp=0,
          ankle_l=[0,0.10,0.08],ankle_r=[0,-0.10,0.08],
          knee_l=[1,0,0],knee_r=[1,0,0],foot_l=[1,0,0],foot_r=[1,0,0],
          wrist_l_rel=[x,span,0.0],wrist_r_rel=[x,-span,0.0],
          elbow_l=[0,1,0],elbow_r=[0,-1,0],hand_l=[math.cos(a),math.sin(a),0],hand_r=[math.cos(a),-math.sin(a),0],
          palm_l=[0,0,1],palm_r=[0,0,1],grip_l=.3,grip_r=.3)
    band=dict(kind="rod",a=pt("wrist_l"),b=pt("wrist_r"),r=.008)
    return dict(seconds=4.0,keys=_wave(pose),cam_az=30,cam_el=15,props=[band])


def face_pull():
    def pose(q):
        p=_standing()
        for side,sign in [('l',1),('r',-1)]:
            p['wrist_'+side]=[.568*(1-q)+.08*q,sign*(.18+.22*q),1.425+.225*q]
            p['elbow_'+side]=[-.15*q,sign, .25*q]
            p['hand_'+side]=[1,0,0]
            p['palm_'+side]=[0,0,-1]
            p['grip_'+side]=.4
        joints=solve(p,[])
        for side in ('l','r'):
            d=[joints['wrist_'+side][i]-joints['elbow_'+side][i] for i in range(3)]
            p['hand_'+side]=d
            p['palm_'+side]=[d[2],0,-d[0]]
        return p
    props=[dict(kind='rod',a=[1,0,1.64],b=pt('wrist_'+s),r=.008) for s in ('l','r')]
    return dict(seconds=4,keys=_wave(pose),cam_az=30,cam_el=15,props=props)


def cuban_press():
    def pose(q):
        # Forearms rotate from down through forward to up while elbows stay at
        # shoulder level; the straight arms then press overhead. _wave reverses
        # this same controlled path for the return.
        sh_l=_shoulder([0,0,.955],side="l")
        sh_r=_shoulder([0,0,.955],side="r")
        def endpoint(sh,sgn):
            e=[sh[0],sh[1]+sgn*.30,sh[2]]
            if q <= .58:
                theta=math.pi*min(1.0,q/.58)
                w=[e[0]+.27*math.sin(theta),e[1],e[2]-.27*math.cos(theta)]
            else:
                u=(q-.58)/.42
                start=[e[0],e[1],e[2]+.27]
                target=[sh[0],sh[1],sh[2]+.568]
                w=[start[i]+(target[i]-start[i])*u for i in range(3)]
            return e,w
        el_l,wl=endpoint(sh_l,1)
        el_r,wr=endpoint(sh_r,-1)
        p=base_pose(pelvis=[0,0,.955],p1=0,p2=0,hp=0,
          ankle_l=[0,0.10,.08],ankle_r=[0,-.10,.08],
          knee_l=[1,0,0],knee_r=[1,0,0],foot_l=[1,0,0],foot_r=[1,0,0],
          wrist_l=wl,wrist_r=wr,
          elbow_l=[0,1,0],elbow_r=[0,-1,0],hand_l=[0,0,1],hand_r=[0,0,1],
          palm_l=[1,0,0],palm_r=[1,0,0],grip_l=.7,grip_r=.7)
        joints=solve(p,[])
        for side in ('l','r'):
            d=[joints['wrist_'+side][i]-joints['elbow_'+side][i] for i in range(3)]
            p['hand_'+side]=d
            p['palm_'+side]=[-d[2],0,d[0]]
        return p
    props=[dict(kind="weight",type="dumbbell",joint="wrist_l",off=[0,0,0]),
           dict(kind="weight",type="dumbbell",joint="wrist_r",off=[0,0,0])]
    return dict(seconds=4.5,keys=_wave(pose),cam_az=30,cam_el=15,props=props)


def _unit(v):
    n = math.sqrt(sum(x*x for x in v))
    return [x/n for x in v]

def _sub(a, b):
    return [a[i]-b[i] for i in range(3)]

def _dot(a, b):
    return sum(a[i]*b[i] for i in range(3))

def _pole_for(shoulder, elbow, wrist):
    """World-space elbow pole perpendicular to the shoulder-wrist line."""
    axis = _unit(_sub(wrist, shoulder))
    radial = _sub(elbow, shoulder)
    radial = [radial[i] - _dot(radial, axis)*axis[i] for i in range(3)]
    return _unit(radial)

EXERCISES = {
    "prone-i-raise": prone_i_raise,
    "prone-t-raise": prone_t_raise,
    "prone-y-raise": prone_y_raise,
    "prone-l-raise": prone_l_raise,
    "prone-w-raise": prone_w_raise,
    "scapular-pushup-wall": scapular_pushup_wall,
    "scapular-pushup-knee": scapular_pushup_knee,
    "scapular-pushup": scapular_pushup,
    "support-scapular-shrug": support_scapular_shrug,
    "wall-slide": wall_slide,
    "floor-angel": floor_angel,
    "external-rotation-side-lying": external_rotation_side_lying,
    "external-rotation-band": external_rotation_band,
    "external-rotation-band-90": external_rotation_band_90,
    "band-pull-apart": band_pull_apart,
    "face-pull": face_pull,
    "cuban-press": cuban_press,
}
