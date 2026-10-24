"""Hanging leg raises and anchored dragon-flag progressions."""
import math
from poses import base_pose, pt, solve, trunk_frame

BAR_Z = 2.30
WRIST_Z = BAR_Z - 0.065


def _bar():
    return dict(kind="bar", z=BAR_Z, half=0.72, r=0.017)


def _hands(y=0.24, z=WRIST_Z, x=-0.045, grips=(0.95, 0.95), palms=(1, 0, 0), hand=(0, 0, 1), elbows=None):
    if elbows is None:
        elbows=((0.25,1,-0.2),(0.25,-1,-0.2))
    return dict(wrist_l=[x,y,z], wrist_r=[x,-y,z], grip_l=grips[0], grip_r=grips[1],
                palm_l=list(palms),palm_r=list(palms),hand_l=list(hand),hand_r=list(hand),
                elbow_l=list(elbows[0]),elbow_r=list(elbows[1]))


def _legs(ankles, knees=((1,0,0),(1,0,0)), feet=((0,0,-1),(0,0,-1))):
    return dict(ankle_l=list(ankles[0]),ankle_r=list(ankles[1]),
                knee_l=list(knees[0]),knee_r=list(knees[1]),
                foot_l=list(feet[0]),foot_r=list(feet[1]))


def _hang_pose(pz=1.205, px=-0.04, p1=-3, p2=-6, ankles=None, knees=((1,0,0),(1,0,0)),
               feet=((0,0,1),(0,0,1)), hands=None, **kw):
    if ankles is None:
        ankles=([0.02,0.10,0.31],[0.02,-0.10,0.31])
    f={}
    f.update(hands or _hands())
    f.update(_legs(ankles,knees,feet))
    f.update(kw)
    return base_pose(pelvis=[px,0,pz],p1=p1,p2=p2,hp=-4,**f)


def _keys_rep(bottom, top, seconds=3.8):
    return dict(seconds=seconds,keys=[(0,bottom),(.14,bottom),(.48,top),(.60,top),(.88,bottom),(.98,bottom)],
                cam_az=35,cam_el=14)


def _dip_bars():
    return [dict(kind="rod",a=[-.52,.25,1.0],b=[.50,.25,1.0],r=.022),
            dict(kind="rod",a=[-.52,-.25,1.0],b=[.50,-.25,1.0],r=.022)]


def _support_pose(pz=1.08, ankles=None, hands=None):
    if ankles is None:
        ankles=([.02,.10,.20],[.02,-.10,.20])
    if hands is None:
        hands=_hands(y=.25,z=1.0,x=0.0,elbows=((0,1,0),(0,-1,0)))
    f={};f.update(hands);f.update(_legs(ankles,((1,0,0),(1,0,0))) )
    return base_pose(pelvis=[-.04,0,pz],p1=-2,p2=-4,hp=-4,**f)


def knee_raise_supported():
    # Keep the shoulders low enough to reach the parallel bars while the pelvis
    # curls the knees up; the hands remain fixed on the bars throughout.
    down=_support_pose(1.09,([.02,.10,.25],[.02,-.10,.25]))
    up=_support_pose(1.09,([.14,.12,.77],[.14,-.12,.77]))
    return dict(seconds=3.8,keys=[(0,down),(.15,down),(.46,up),(.58,up),(.88,down),(.98,down)],
                cam_az=35,cam_el=15,props=_dip_bars())


def hanging_knee_raise():
    down=_hang_pose()
    up=_hang_pose(1.30,-.04,-14,-19,([.17,.13,.91],[.17,-.13,.91]),
                  ((1,0,-1),(1,0,-1)))
    return dict(seconds=3.8,keys=[(0,down),(.15,down),(.46,up),(.58,up),(.88,down),(.98,down)],
                cam_az=35,cam_el=14,props=[_bar()])


def hanging_knee_raise_twisting():
    hang=_hang_pose()
    right=_hang_pose(1.30,-.05,-15,-20,([.15,-.22,.93],[.15,-.08,.93]),
                     ((1,0,-1),(1,0,-1)),yaw=-12)
    left=_hang_pose(1.30,-.05,-15,-20,([.15,.08,.93],[.15,.22,.93]),
                    ((1,0,-1),(1,0,-1)),yaw=12)
    return dict(seconds=4.4,keys=[(0,hang),(.12,hang),(.30,right),(.43,left),(.56,right),(.70,left),(.86,hang),(.98,hang)],
                cam_az=35,cam_el=14,props=[_bar()])


def hanging_leg_raise_bent():
    hang=_hang_pose()
    top=_hang_pose(1.32,-.10,-12,-16,([.70,.10,1.16],[.70,-.10,1.16]),
                   ((1,0,-.2),(1,0,-.2)),feet=((1,0,0),(1,0,0)))
    return dict(seconds=4.0,keys=[(0,hang),(.15,hang),(.48,top),(.60,top),(.90,hang),(.98,hang)],
                cam_az=35,cam_el=14,props=[_bar()])


def hanging_leg_raise():
    hang=_hang_pose()
    top=_hang_pose(1.32,-.22,-8,-12,([.58,.10,1.18],[.58,-.10,1.18]),
                   ((1,0,0),(1,0,0)),feet=((1,0,0),(1,0,0)))
    return dict(seconds=4.0,keys=[(0,hang),(.15,hang),(.48,top),(.62,top),(.90,hang),(.98,hang)],
                cam_az=35,cam_el=14,props=[_bar()])


def toes_to_bar():
    hang=_hang_pose()
    # The feet approach the center of the bar while the pelvis stays back,
    # preserving a long, nearly straight leg line at contact.
    top=_hang_pose(1.45,-.57,50,55,([-.074,.022,2.126],[-.074,-.022,2.126]),
                   ((1,0,0),(1,0,0)),feet=((.4,0,1),(.4,0,1)))
    keys=[(0,hang),(.12,hang),(.24,_hang_pose(1.27,-.16,15,18,([.12,.10,.88],[.12,-.10,.88]),
                   ((1,0,-.3),(1,0,-.3)))),(.36,_hang_pose(1.39,-.40,35,40,([.02,.10,1.55],[.02,-.10,1.55]),
                   ((1,0,0),(1,0,0)),feet=((.4,0,1),(.4,0,1)))),(.50,top),(.62,top),(.88,hang),(.98,hang)]
    # Keep the knee poles on a stable lateral plane. The old forward pole
    # became singular as the ankles passed the hips and flipped the knees
    # across the body during the controlled lower.
    for _, pose in keys:
        pose["knee_l"]=[0,1,0]
        pose["knee_r"]=[0,-1,0]
    from strict_toes import with_straight_legs
    return with_straight_legs(dict(seconds=5.6,keys=keys,cam_az=35,cam_el=15,props=[_bar()]))


def heel_hook():
    hang=_hang_pose()
    # Raise the hips enough to pass the feet over the bar, then cross the
    # ankles while both feet are still above the bar plane.
    raised=_hang_pose(1.60,-.02,8,14,([.04715,.08,2.35],[.04715,-.08,2.35]),
                  ((1,0,-1),(1,0,-1)),feet=((0,0,1),(0,0,1)),
                  hands=_hands(y=.24,z=WRIST_Z,x=-.045,elbows=((1,1,0),(1,-1,0))))
    up=_hang_pose(1.60,-.02,8,14,([.04715,-.08,2.35],[.04715,.08,2.35]),
                  ((1,0,-1),(1,0,-1)),feet=((0,0,1),(0,0,1)),
                  hands=_hands(y=.24,z=WRIST_Z,x=-.045,elbows=((1,1,0),(1,-1,0))))
    keys=[(0,hang),(.12,hang),(.34,raised),(.42,raised),(.52,up),(.60,up),
          (.70,raised),(.88,hang),(.98,hang)]
    for _, pose in keys:
        # This inward-facing lateral pole keeps both knees from popping across
        # the trunk as the ankles reverse their order.
        pose["knee_l"]=[0,-1,0]
        pose["knee_r"]=[0,1,0]
    return dict(seconds=4.8,keys=keys,
                cam_az=35,cam_el=14,props=[_bar()])


def _wiper_pose(elevation, side_angle):
    """Straight-leg pose on a .869 m sphere around a steady, bar-supported hip."""
    pz, px, reach = 1.21, -.04, .869
    e, b = math.radians(elevation), math.radians(side_angle)
    dx = reach * math.cos(e) * math.cos(b)
    dy = reach * math.cos(e) * math.sin(b)
    dz = reach * math.sin(e)
    hipx, hipz = px + .004, pz - .03 * math.cos(math.radians(8))
    ankles=([hipx+dx,.09+dy,hipz+dz],[hipx+dx,-.09+dy,hipz+dz])
    return _hang_pose(pz,px,-8,-12,ankles,((1,0,0),(1,0,0)),
                      feet=((0,0,1),(0,0,1)))


def _wiper_keys(full):
    # Densely sample the elevation and lateral sweep so linear key interpolation
    # does not shorten the working legs at the middle of the arc.
    top_elevation = 71 if full else 0
    side_limit = 43 if full else 23
    keys=[(0,_wiper_pose(-90,0)),(.12,_wiper_pose(-90,0))]

    def path(t0,t1,e0,b0,e1,b1):
        span=max(abs(e1-e0),abs(b1-b0))
        steps=max(1,int(math.ceil(span/3.0)))
        for i in range(1,steps+1):
            q=i/steps
            e=e0+(e1-e0)*q
            b=b0+(b1-b0)*q
            if full and abs(b)>0:
                e=71-17*abs(b)/side_limit
            t=t0+(t1-t0)*q
            keys.append((t,_wiper_pose(e,b)))

    path(.12,.30,-90,0,top_elevation,0)
    keys.append((.30,_wiper_pose(top_elevation,0)))
    path(.30,.43,top_elevation,0,top_elevation,-side_limit)
    path(.43,.56,top_elevation,-side_limit,top_elevation,0)
    path(.56,.69,top_elevation,0,top_elevation,side_limit)
    path(.69,.82,top_elevation,side_limit,top_elevation,0)
    path(.82,.98,top_elevation,0,-90,0)
    return dict(seconds=5.0 if full else 4.8,keys=keys,cam_az=35,cam_el=16,props=[_bar()])


def hanging_windshield_wiper_half():
    return _wiper_keys(False)


def _wiper_full_pose(elevation, side_angle):
    """Full-range wiper pose with active trunk counter-roll under fixed hands."""
    pz, px, reach = 1.21, -.04, .869
    trunk_roll = -0.30 * side_angle
    e, b = math.radians(elevation), math.radians(side_angle)
    dx = reach * math.cos(e) * math.cos(b)
    dy = reach * math.cos(e) * math.sin(b)
    dz = reach * math.sin(e)
    up, left = trunk_frame(-8, 0, trunk_roll)
    origin = [px, 0, pz]
    ankles = []
    for sign in (1, -1):
        hip = [origin[i] + sign*.09*left[i] - .03*up[i] for i in range(3)]
        ankles.append([hip[i] + (dx, dy, dz)[i] for i in range(3)])
    pose = _hang_pose(pz, px, -8, -12, ankles, ((1,0,0),(1,0,0)),
                      feet=((0,0,1),(0,0,1)), roll=trunk_roll)
    # Small active shoulder protraction lets the arms stay within reach of the
    # fixed bar grip as the chest and pelvis counter-roll beneath it.
    reach_offset = abs(trunk_roll) * .0026
    pose['shoulder_offset_l'] = [reach_offset, 0, 0]
    pose['shoulder_offset_r'] = [reach_offset, 0, 0]
    # Point each toe mostly along its solved shin, with a small forward bias.
    # This keeps the render heel-frame normal away from its collinear fallback
    # throughout the hang, raise, lateral sweeps, and lower.
    joints = solve(pose, {})
    for side in ('l', 'r'):
        shin = [joints['ankle_'+side][i]-joints['knee_'+side][i] for i in range(3)]
        shin_len = math.sqrt(sum(x*x for x in shin))
        shin = [x/shin_len for x in shin]
        # Cross with the global lateral axis to get a stable sagittal-plane
        # toe offset, independent of whether the shin points up or down.
        offset = [-shin[2], 0., shin[0]]
        offset_len = math.sqrt(sum(x*x for x in offset))
        offset = [x/offset_len for x in offset]
        pointed = [shin[i] + .28*offset[i] for i in range(3)]
        pointed_len = math.sqrt(sum(x*x for x in pointed))
        pose['foot_'+side] = [x/pointed_len for x in pointed]
    return pose


def _wiper_full_keys():
    """Complete raise, bilateral sweep, centered return and lower cycle."""
    top_elevation, side_limit = 71, 43
    keys = [(0, _wiper_full_pose(-90, 0)), (.12, _wiper_full_pose(-90, 0))]

    def path(t0, t1, e0, b0, e1, b1):
        span = max(abs(e1-e0), abs(b1-b0))
        steps = max(1, int(math.ceil(span/3.0)))
        for i in range(1, steps+1):
            q = i/steps
            e = e0+(e1-e0)*q
            b = b0+(b1-b0)*q
            if abs(b) > 0:
                e = top_elevation-17*abs(b)/side_limit
            t = t0+(t1-t0)*q
            keys.append((t, _wiper_full_pose(e, b)))

    path(.12, .30, -90, 0, top_elevation, 0)
    keys.append((.30, _wiper_full_pose(top_elevation, 0)))
    path(.30, .43, top_elevation, 0, top_elevation, -side_limit)
    path(.43, .56, top_elevation, -side_limit, top_elevation, 0)
    path(.56, .69, top_elevation, 0, top_elevation, side_limit)
    path(.69, .82, top_elevation, side_limit, top_elevation, 0)
    path(.82, .98, top_elevation, 0, -90, 0)
    return dict(seconds=6.5, keys=keys, cam_az=35, cam_el=16, props=[_bar()])


def hanging_windshield_wiper():
    return _wiper_full_keys()


def _bench():
    return dict(kind="box",center=[-.075,0,.275],size=[.95,.65,.55])


def _dragon_pose(angle, leg_lengths=(.869,.869), chest_z=.55):
    a=math.radians(angle)
    up=[math.sin(a),0,math.cos(a)]
    # Chest/upper back stays at one fixed point on the bench while the pelvis arcs.
    chest=[-.45,0,chest_z]
    pelvis=[chest[0]-.50*up[0],0,chest[2]-.50*up[2]]
    ankles=[]
    for s,reach in enumerate(leg_lengths):
        y=.09 if s==0 else -.09
        ankles.append([pelvis[0]-(.03+reach)*up[0],y,pelvis[2]-(.03+reach)*up[2]])
    wrists=dict(wrist_l=[-.86,.25,.56],wrist_r=[-.86,-.25,.56],
                grip_l=.9,grip_r=.9,palm_l=[0,0,-1],palm_r=[0,0,-1],
                hand_l=[-1,0,0],hand_r=[-1,0,0],elbow_l=[-1,1,-.2],elbow_r=[-1,-1,-.2])
    return base_pose(pelvis=pelvis,p1=angle,p2=angle,hp=270-angle,
                     ankle_l=ankles[0],ankle_r=ankles[1],
                     knee_l=[0,0,-1],knee_r=[0,0,-1],foot_l=[-up[0],0,-up[2]],foot_r=[-up[0],0,-up[2]],
                     **wrists)


def _negative_bench_lift(pose):
    """Approximate the torso's lowest surface over the bench footprint.

    Mirrors the renderer's torso section dimensions so the negative stays on,
    rather than sinking through, the bench during the shoulder-supported arc.
    This source-only envelope is a screening guide; native mesh checks remain
    authoritative.
    """
    sections=((-0.10,.12,.095),(0,.155,.10),(.09,.145,.095),(.20,.135,.09),
              (.29,.15,.10),(.39,.175,.112),(.45,.185,.12),(.48,.185,.11),
              (.51,.16,.095),(.57,.055,.055))
    frame=solve(pose,{})
    def unit(v):
        length=math.sqrt(sum(x*x for x in v))
        return [x/length for x in v]
    def cross(a,b):
        return [a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]]
    p,s=frame['pelvis'],frame['spine']
    u1,u2=unit(frame['pelvis_up']),unit(frame['chest_up'])
    l1,l2=unit(frame['pelvis_left']),unit(frame['chest_left'])
    lowest=math.inf
    for length,width,depth in sections:
        if length<=.24:
            center=[p[i]+u1[i]*length for i in range(3)];up,left=u1,l1
        else:
            center=[s[i]+u2[i]*(length-.24) for i in range(3)];up,left=u2,l2
        forward=unit(cross(left,up))
        for j in range(32):
            theta=2*math.pi*j/32
            point=[center[k]+left[k]*width*math.cos(theta)+forward[k]*depth*math.sin(theta)
                   for k in range(3)]
            if -.55<=point[0]<=.4 and -.325<=point[1]<=.325:
                lowest=min(lowest,point[2])
    if not math.isfinite(lowest):
        return 0.
    # Leave a small positive margin for the torso's smoothed surface rather
    # than merely lifting the sampled ring vertices to the bench plane.
    return max(0.,.560-lowest)


def _flag_keys(lengths=(.866,.866), bottom_angle=260, seconds=4.0, negative=False):
    def pose(angle, legs):
        p=_dragon_pose(angle,legs)
        if negative:
            # A side-facing pole avoids the solver's knee branch singularity
            # at the tuck-and-roll transition.
            p["knee_l"]=[0,1,0]
            p["knee_r"]=[0,-1,0]
            lift=_negative_bench_lift(p)
            p["pelvis"][2]+=lift
            p["ankle_l"][2]+=lift
            p["ankle_r"][2]+=lift
        return p
    top=pose(180,lengths)
    keys=[(0,top),(.12,top)]
    # Sample the shoulder-anchored body arc densely; wrist and chest contacts stay fixed.
    n=22
    for i in range(1,n+1):
        angle=180+(bottom_angle-180)*i/n
        keys.append((.12+.60*i/n,pose(angle,lengths)))
    if negative:
        tucked=(.43,.43)
        keys.extend([(.75,pose(bottom_angle,tucked)),(.81,pose(245,tucked)),
                     (.87,pose(220,tucked)),(.90,pose(207,tucked)),
                     (.93,pose(195,(.54,.54))),(.96,pose(185,(.68,.68))),
                     (.98,pose(180,(.81,.81))),(.99,top)])
    else:
        for i in range(1,n+1):
            angle=bottom_angle-(bottom_angle-180)*i/n
            keys.append((.72+.25*i/n,pose(angle,lengths)))
    return dict(seconds=seconds,keys=keys,cam_az=35,cam_el=15,props=[_bench()])


def dragon_flag_candlestick():
    a=_dragon_pose(180,(.869,.869),.55)
    b=_dragon_pose(179,(.869,.869),.55)
    return dict(seconds=3.0,keys=[(0,a),(.45,b),(.92,a)],cam_az=35,cam_el=15,props=[_bench()])


def dragon_flag_tuck():
    return _flag_keys((.43,.43),bottom_angle=258,seconds=4.6)


def dragon_flag_adv_tuck():
    return _flag_keys((.615,.615),bottom_angle=258,seconds=4.6)


def dragon_flag_one_leg():
    return _flag_keys((.866,.43),bottom_angle=260,seconds=4.6)


def dragon_flag_negative():
    return _flag_keys((.869,.869),bottom_angle=260,seconds=7.0,negative=True)


def dragon_flag():
    return _flag_keys((.869,.869),bottom_angle=260,seconds=5.0)


EXERCISES={
    "knee-raise-supported":knee_raise_supported,
    "hanging-knee-raise":hanging_knee_raise,
    "hanging-knee-raise-twisting":hanging_knee_raise_twisting,
    "hanging-leg-raise-bent":hanging_leg_raise_bent,
    "hanging-leg-raise":hanging_leg_raise,
    "toes-to-bar":toes_to_bar,
    "heel-hook":heel_hook,
    "hanging-windshield-wiper-half":hanging_windshield_wiper_half,
    "hanging-windshield-wiper":hanging_windshield_wiper,
    "dragon-flag-candlestick":dragon_flag_candlestick,
    "dragon-flag-tuck":dragon_flag_tuck,
    "dragon-flag-adv-tuck":dragon_flag_adv_tuck,
    "dragon-flag-one-leg":dragon_flag_one_leg,
    "dragon-flag-negative":dragon_flag_negative,
    "dragon-flag":dragon_flag,
}
