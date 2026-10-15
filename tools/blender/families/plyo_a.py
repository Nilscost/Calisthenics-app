"""Plyometric jumps and landings for the R8 pose set."""

from poses import base_pose, both, solve


def _key(t, pz=0.975, px=0.0, pitch=0, knee_bend=False, ankle_z=0.08,
         ankle_dx=0.0, ankle_dy=0.14, wrist=None, yaw=0, left_dx=None,
         left_z=None, foot_x=1.0, foot_y=0.0, heel_kick=0.0,
         cycle_left=False, pelvis_y=0.0, knee_r=None, knee_l=None):
    """A symmetric airborne/grounded key. Feet are explicit world targets."""
    if wrist is None:
        wrist = [0.05, 0.0, -0.20]
    ar = [px + ankle_dx, pelvis_y - ankle_dy, ankle_z]
    al = [px + (ankle_dx if left_dx is None else left_dx), pelvis_y + ankle_dy,
          ankle_z if left_z is None else left_z]
    knee = knee_r or ([1, 0.15, 0] if knee_bend else [1, 0, 0])
    kl = knee_l or ([1, -0.15, 0] if knee_bend else [1, 0, 0])
    foot_r = [foot_x, foot_y, -heel_kick]
    foot_l = [foot_x, foot_y, -heel_kick]
    if cycle_left:
        # Forward-swinging back leg, with a visibly bent knee.
        al = [px + 0.24, 0.14, max(ankle_z, 0.29)]
        kl = [1, 0.2, 0]
    return (t, base_pose(pelvis=[px, pelvis_y, pz], p1=pitch, p2=pitch, hp=0, yaw=yaw,
                         ankle_r=ar, ankle_l=al, foot_r=foot_r, foot_l=foot_l,
                         knee_r=knee, knee_l=kl,
                         **both(wrist_rel=wrist, elbow=[0, 0.2, -1], hand=[1, 0, 0])))


def _jump(bottom, takeoff, peak, landing, cam_az=30):
    """Reusable low impact loop: set, compress, flight, soft landing, reset."""
    return dict(seconds=3.2, cam_az=cam_az, cam_el=12, props=[], keys=[
        bottom(0.0), bottom(0.12), takeoff(0.24), peak(0.40),
        landing(0.62), landing(0.70), bottom(0.86),
    ])


def landing_stick():
    def ready(t): return _key(t, wrist=[0.15, 0, -0.38])
    def airborne(t): return _key(t, pz=1.13, ankle_z=0.23, wrist=[0.1, 0, 0.28])
    def stick(t): return _key(t, pz=0.70, px=-0.12, ankle_dx=.12, pitch=8, knee_bend=True,
                             wrist=[0.25, 0, 0.05])
    # A deeper, sustained quarter-squat marks the two-second landing pause.
    keys = [ready(0), ready(0.10), airborne(0.22), airborne(0.34), stick(0.48),
            stick(0.68), stick(0.76), ready(0.90)]
    return dict(seconds=4.0, keys=keys, cam_az=30, cam_el=12, props=[])


def pogo_hop():
    def stance(t): return _key(t, pz=0.98, wrist=[0.05, 0, -0.20])
    def spring(t): return _key(t, pz=1.005, ankle_z=0.13, wrist=[0.05, 0, -0.16])
    def flight(t): return _key(t, pz=1.08, ankle_z=0.22, wrist=[0.05, 0, -0.12])
    keys = [stance(0), spring(.12), flight(.24), spring(.36), stance(.46),
            spring(.58), flight(.70), spring(.82)]
    return dict(seconds=1.8, keys=keys, cam_az=28, cam_el=8, props=[])


def ankle_hop_side_to_side():
    def side(t, y, z):
        return _key(t, pz=z, pelvis_y=y, ankle_z=0.08 if z < 1 else 0.26,
                    ankle_dy=.045, left_z=0.08 if z < 1 else 0.26,
                    wrist=[0.05, 0, -.20])
    # Small travel across the line, feet together and knees nearly straight.
    return dict(seconds=1.9, keys=[side(0, -.10, .98), side(.13, -.05, 1.05),
        side(.25, .10, 1.13), side(.38, .10, 1.04), side(.50, .10, .98),
        side(.63, .05, 1.05), side(.75, -.10, 1.13), side(.88, -.10, 1.04)],
        cam_az=30, cam_el=10, props=[])


def ankle_hop_hip_twist():
    def twist(t, turn, z, az):
        return _key(t, pz=z, ankle_z=.08 if z < 1 else .26, ankle_dy=.045,
                    left_z=.08 if z < 1 else .26, foot_x=1, foot_y=turn,
                    wrist=[.05, 0, -.20], yaw=az)
    # The feet rotate under a forward-facing chest; the tiny yaw is confined to the lower stance.
    return dict(seconds=2.0, keys=[twist(0, 0, .98, 0), twist(.13, .12, 1.06, 0),
        twist(.25, .50, 1.13, 0), twist(.38, .50, 1.05, 0), twist(.50, .50, .98, 0),
        twist(.63, -.12, 1.06, 0), twist(.75, -.50, 1.13, 0), twist(.88, -.50, 1.05, 0)],
        cam_az=30, cam_el=10, props=[])


def jump_squat():
    def pose(t, pz, pitch, hands):
        return (t, base_pose(pelvis=[-.30 if pz < .5 else 0, 0, pz], p1=pitch,
            p2=pitch, hp=0, ankle_r=[0, -.14, .08 if pz < 1 else .31],
            ankle_l=[0, .14, .08 if pz < 1 else .31], foot_r=[1, 0, 0],
            foot_l=[1, 0, 0], knee_r=[1, 0, 0], knee_l=[1, 0, 0],
            wrist_r=hands[0], wrist_l=hands[1], elbow_r=[0, .2, -1],
            elbow_l=[0, -.2, -1], hand_r=[1, 0, 0], hand_l=[1, 0, 0]))
    squat = lambda t: pose(t, .52, 52, ([.15, -.14, .34], [.15, .14, .34]))
    rise = lambda t: pose(t, .94, 10, ([.20, -.20, 1.35], [.20, .20, 1.35]))
    peak = lambda t: pose(t, 1.20, 0, ([.10, -.20, 2.10], [.10, .20, 2.10]))
    land = lambda t: pose(t, .45, 35, ([.15, -.14, .34], [.15, .14, .34]))
    return dict(seconds=3.2, keys=[squat(0), squat(.12), rise(.24), peak(.38),
        peak(.48), land(.62), land(.70), squat(.86)], cam_az=30, cam_el=12, props=[])


def jump_vertical():
    def dip(t): return _key(t, pz=.76, pitch=18, knee_bend=True, wrist=[-.10, 0, -.43])
    def drive(t): return _key(t, pz=1.01, ankle_z=.12, wrist=[.12, 0, .48])
    def reach(t): return _key(t, pz=1.24, ankle_z=.36, wrist=[.05, 0, .56])
    def reset(t): return _key(t, pz=.78, pitch=12, knee_bend=True, wrist=[.10, 0, .36])
    return _jump(dip, drive, reach, reset)


def jump_butt_kick():
    def dip(t): return _key(t, pz=.78, pitch=12, knee_bend=True, wrist=[-.10, 0, -.40])
    def drive(t): return _key(t, pz=1.02, ankle_z=.12, wrist=[.05, 0, -.08])
    def kick(t): return _key(t, pz=1.17, ankle_z=.96, ankle_dx=-.24,
                             knee_bend=True, wrist=[.05, 0, -.08])
    def land(t): return _key(t, pz=.78, pitch=10, knee_bend=True, wrist=[.05, 0, -.12])
    return _jump(dip, drive, kick, land)


def _tuck_key(t, ankle_dx, ankle_z, hand_x, hand_z, pelvis_z=1.17,
              bent=True):
    # Fold both thighs toward the chest; knee poles point upward to create a tight tuck.
    return (t, base_pose(pelvis=[0, 0, pelvis_z], p1=0, p2=0, hp=0,
        ankle_r=[ankle_dx, -.10, ankle_z], ankle_l=[ankle_dx, .10, ankle_z],
        knee_r=[1, 0, 1] if bent else [1, 0, 0],
        knee_l=[1, 0, 1] if bent else [1, 0, 0],
        foot_r=[1, 0, 0], foot_l=[1, 0, 0],
        wrist_r=[hand_x, -.10, hand_z], wrist_l=[hand_x, .10, hand_z],
        elbow_r=[0, 0, -1], elbow_l=[0, 0, -1], hand_r=[1, 0, 0], hand_l=[1, 0, 0]))


def _tuck_contact(key, mode):
    t, pose = key
    joints = solve(pose, [])
    for side in ('l', 'r'):
        knee, ankle = joints['knee_'+side], joints['ankle_'+side]
        point = knee if mode != 'catch' else [(knee[i]+ankle[i])/2 for i in range(3)]
        pose['wrist_'+side] = [point[0]-.065, point[1]+(.02 if side=='l' else -.02), point[2]+(.04 if mode=='slap' else -.02)]
        pose['hand_'+side] = [1,0,0]
        pose['palm_'+side] = [0,0,-1] if mode=='slap' else [0,0,1]
        pose['grip_'+side] = 0 if mode=='slap' else .45
    return t, pose


def jump_tuck_slap():
    def dip(t): return _tuck_key(t, 0, .08, -.15, .80, pelvis_z=.78, bent=False)
    def drive(t): return _tuck_key(t, 0, .15, .10, 1.35, pelvis_z=1.02, bent=False)
    def tuck(t): return _tuck_contact(_tuck_key(t, .30, 1.04, .25, 1.37), "slap")
    def land(t): return _tuck_key(t, 0, .08, .10, .82, pelvis_z=.78, bent=False)
    return dict(seconds=3.2, keys=[dip(0), dip(.12), drive(.24), tuck(.40),
        land(.62), land(.70), dip(.86)], cam_az=30, cam_el=12, props=[])


def jump_tuck():
    def dip(t): return _tuck_key(t, 0, .08, -.15, .80, pelvis_z=.78, bent=False)
    def drive(t): return _tuck_key(t, 0, .14, .10, 1.35, pelvis_z=1.02, bent=False)
    def hug(t): return _tuck_contact(_tuck_key(t, .21, 1.14, .24, 1.33), "hug")
    def land(t): return _tuck_key(t, 0, .08, .10, .82, pelvis_z=.78, bent=False)
    return dict(seconds=3.2, keys=[dip(0), dip(.12), drive(.24), hug(.40),
        land(.62), land(.70), dip(.86)], cam_az=30, cam_el=12, props=[])


def jump_tuck_catch():
    def dip(t): return _tuck_key(t, 0, .08, -.15, .80, pelvis_z=.78, bent=False)
    def drive(t): return _tuck_key(t, 0, .14, .10, 1.35, pelvis_z=1.02, bent=False)
    def catch(t): return _tuck_contact(_tuck_key(t, .16, 1.18, .07, 1.16), "catch")
    def land(t): return _tuck_key(t, 0, .08, .10, .82, pelvis_z=.78, bent=False)
    return dict(seconds=3.2, keys=[dip(0), dip(.12), drive(.24), catch(.40),
        land(.62), land(.70), dip(.86)], cam_az=30, cam_el=12, props=[])


def _split_key(t, swap=False, cycling=False, z=.82, airborne=False):
    # X-separated feet make the scissor switch legible from the side camera.
    if not swap:
        right = [.32, -.10, .08 if not airborne else .30]
        left = [-.32, .10, .08 if not airborne else .30]
    else:
        right = [-.32, -.10, .08 if not airborne else .30]
        left = [.32, .10, .08 if not airborne else .30]
    kr = [1, 0, 0]
    kl = [1, 0, 0]
    if cycling:
        # On the switch, the trailing leg folds as the back leg swings through.
        if swap:
            right = [-.08, -.10, .37]
            kr = [1, 0, 0]
        else:
            left = [-.08, .10, .37]
            kl = [1, 0, 0]
    return (t, base_pose(pelvis=[0, 0, z], p1=8, p2=8, hp=0,
        ankle_r=right, ankle_l=left, foot_r=[1, 0, 0], foot_l=[1, 0, 0],
        knee_r=kr, knee_l=kl,
        **both(wrist_rel=[.18, 0, .10], elbow=[0, .2, -1], hand=[1, 0, 0])))


def jump_split():
    return dict(seconds=3.0, keys=[_split_key(0, False, z=.67), _split_key(.15, False, z=.85, airborne=True),
        _split_key(.30, False, z=1.02, airborne=True), _split_key(.42, True, z=1.02, airborne=True),
        _split_key(.56, True, z=.67), _split_key(.69, True, z=.84, airborne=True),
        _split_key(.82, False, z=1.02, airborne=True), _split_key(.93, False, z=.67)],
        cam_az=8, cam_el=12, props=[])


def jump_split_cycle():
    return dict(seconds=3.3, keys=[_split_key(0, False, True, z=.67),
        _split_key(.13, False, True, z=.88, airborne=True),
        _split_key(.28, False, True, z=1.12, airborne=True),
        _split_key(.42, True, True, z=1.08, airborne=True),
        _split_key(.56, True, True, z=.67),
        _split_key(.69, True, True, z=.88, airborne=True),
        _split_key(.83, True, True, z=1.12, airborne=True),
        _split_key(.94, False, True, z=.67)], cam_az=8, cam_el=12, props=[])


def jump_block():
    # Alternating approach steps, then both feet plant together before takeoff.
    def run(t, x, xr, xl, zr=.08, zl=.08, wrist=[.25, 0, .04], pz=.975):
        return (t, base_pose(pelvis=[x, 0, pz], p1=12, p2=12, hp=0,
            ankle_r=[xr, -.12, zr], ankle_l=[xl, .12, zl],
            knee_r=[1, 0, 0], knee_l=[1, 0, 0], foot_r=[1, 0, 0], foot_l=[1, 0, 0],
            **both(wrist_rel=wrist, elbow=[0, .2, -1], hand=[1, 0, 0])))
    def plant(t): return _key(t, pz=.80, px=.50, knee_bend=True, wrist=[-.10, 0, -.34])
    def launch(t): return _key(t, pz=1.04, px=.50, ankle_z=.14, wrist=[.10, 0, .36])
    def peak(t): return _key(t, pz=1.24, px=.50, ankle_z=.36, wrist=[.10, 0, .55])
    def land(t): return _key(t, pz=.78, px=.50, pitch=8, knee_bend=True, wrist=[.15, 0, .08])
    keys = [run(0, -.55, -.55, -.25, zl=.24, wrist=[.18, 0, .08]),
            run(.10, -.30, -.05, -.30, zr=.24),
            run(.19, -.05, -.05, .25, zl=.24),
            run(.27, .20, .48, .20, zr=.24), plant(.34), plant(.40), launch(.48),
            peak(.57), land(.70), land(.78)]
    keys=[(t*.60/.78,p) for t,p in keys]
    keys += [run(.64,.40,.30,.50,zr=.18,pz=.925),
             run(.69,.25,.10,.50,pz=.925),
             run(.74,.05,.10,-.10,zl=.18,pz=.925),
             run(.79,-.15,.10,-.30,pz=.925),
             run(.85,-.35,-.50,-.30,zr=.18,pz=.925),
             run(.91,-.55,-.55,-.30,pz=.925),
             run(.98,-.55,-.55,-.25,zl=.24)]
    return dict(seconds=5.5, keys=keys, cam_az=30, cam_el=10, props=[])


EXERCISES = {
    "landing-stick": landing_stick,
    "pogo-hop": pogo_hop,
    "ankle-hop-side-to-side": ankle_hop_side_to_side,
    "ankle-hop-hip-twist": ankle_hop_hip_twist,
    "jump-squat": jump_squat,
    "jump-vertical": jump_vertical,
    "jump-butt-kick": jump_butt_kick,
    "jump-tuck-slap": jump_tuck_slap,
    "jump-tuck": jump_tuck,
    "jump-tuck-catch": jump_tuck_catch,
    "jump-split": jump_split,
    "jump-split-cycle": jump_split_cycle,
    "jump-block": jump_block,
}
