"""Push and shoulder exercises (tools/blender/POSES.md, HAIKU-TASK.md).

Floor push-ups use a body line from the toes (or knees) to the hands; _line() builds one key of it."""
import math
from poses import base_pose, both, rep, add, mul


def _line(ankle_x, a, wrist_x, wy=0.27, wz=0.035, elbow=(-0.6, 0.7, 1.0), knee=(0, 0, -1), foot=(0.95, 0, -0.3)):
    """Straight body at angle a above the floor (deg), toes at ankle_x, hands at (wrist_x, +-wy, wz)."""
    ar = math.radians(a)
    d = [math.cos(ar), 0.0, math.sin(ar)]
    pelvis = add([ankle_x, 0.0, 0.08], mul(d, 0.898))
    pitch = 90 - a
    return base_pose(pelvis=pelvis, p1=pitch, p2=pitch, hp=-8,
                     **both(ankle=[ankle_x, 0.08, 0.08], foot=list(foot), knee=list(knee),
                            wrist=[wrist_x, wy, wz], elbow=list(elbow), hand=[1, 0, 0],
                            palm=[0, 0, -1], grip=0.0))


def _knee_line(kx, a, wrist_x, wy=0.27):
    """Knee push-up: knees on the floor at x=kx, shins flat behind, body line at angle a."""
    ar = math.radians(a)
    d = [math.cos(ar), 0.0, math.sin(ar)]
    pelvis = add([kx, 0.0, 0.06], mul(d, 0.47))
    pitch = 90 - a
    return base_pose(pelvis=pelvis, p1=pitch, p2=pitch, hp=-8,
                     **both(ankle=[kx - 0.43, 0.08, 0.07], foot=[-1, 0, 0], knee=[0, 0, -1],
                            wrist=[wrist_x, wy, 0.035], elbow=[-0.6, 0.7, 1.0], hand=[1, 0, 0],
                            palm=[0, 0, -1], grip=0.0))


def _stand_line(ankle_x, a, wrist_x, wz, wy=0.27):
    """Standing push against a raised surface at height wz (wall, box or counter top)."""
    ar = math.radians(a)
    d = [math.cos(ar), 0.0, math.sin(ar)]
    pelvis = add([ankle_x, 0.0, 0.08], mul(d, 0.898))
    pitch = 90 - a
    return base_pose(pelvis=pelvis, p1=pitch, p2=pitch, hp=-8,
                     **both(ankle=[ankle_x, 0.08, 0.08], foot=[0.95, 0, -0.3], knee=[0, 0, -1],
                            wrist=[wrist_x, wy, wz], elbow=[-0.6, 0.7, 1.0], hand=[1, 0, 0],
                            palm=[0, 0, -1], grip=0.0))


def pushup_standard():
    # Plank on the toes, hands on the floor under the shoulders; the chest drops to just above the floor.
    top = _line(-1.1, 17, -0.14)
    bottom = _line(-1.1, 4, -0.12)
    return dict(seconds=2.5, keys=rep(top, bottom), cam_az=30, cam_el=10, props=[])


def pushup_knee():
    # Knees on the mat; hips and shoulders in one line, the chest drops toward the floor.
    top = _knee_line(-0.75, 30, -0.20)
    bottom = _knee_line(-0.75, 6, -0.20)
    return dict(seconds=2.5, keys=rep(top, bottom), cam_az=30, cam_el=10, props=[])


def pushup_incline():
    # Hands on a stable raised surface (box top at 0.97 m, front edge at x=0.10); the body is straight from the feet.
    box = [dict(kind="box", center=[0.35, 0, 0.485], size=[0.50, 1.2, 0.97])]
    top = _stand_line(-1.1, 53, 0.25, 0.97)
    bottom = _stand_line(-1.1, 25, 0.25, 0.97)
    return dict(seconds=2.5, keys=rep(top, bottom), cam_az=30, cam_el=10, props=box)


def pushup_incline_high():
    # Hands on a sturdy table or counter top at 0.62 m.
    counter = [dict(kind="box", center=[0.45, 0, 0.31], size=[0.30, 1.2, 0.62])]
    top = _stand_line(-1.1, 42, 0.33, 0.62 + 0.0)
    bottom = _stand_line(-1.1, 30, 0.33, 0.62 + 0.0)
    return dict(seconds=2.5, keys=rep(top, bottom), cam_az=30, cam_el=10, props=counter)


def pushup_wall():
    # Standing, hands on a wall at shoulder height (wall face at x=0.10), feet about an arm's length away.
    wall = [dict(kind="box", center=[0.125, 0, 1.2], size=[0.05, 1.6, 2.4])]
    top = _stand_line(-1.15, 60, 0.10, 1.25)
    bottom = _stand_line(-1.15, 44, 0.10, 1.25)
    return dict(seconds=2.5, keys=rep(top, bottom), cam_az=-30, cam_el=10, props=wall)


def pushup_diamond():
    # Hands together under the chest, elbows close to the body.
    top = _line(-1.1, 18, -0.10, wy=0.06, elbow=(-0.3, 0.4, 1.0))
    bottom = _line(-1.1, 4, -0.05, wy=0.06, elbow=(-0.3, 0.4, 1.0))
    return dict(seconds=2.5, keys=rep(top, bottom), cam_az=30, cam_el=10, props=[])


def pushup_archer():
    # Fixed wide palms. Rotate the whole plank about the feet, keeping the
    # assisting elbow extended through descent; mirror for the second rep.
    from motion_constraints import bisect_target, planted_ankle
    from poses import norm, sub, sample, trunk_frame
    half_width, wrist_x, ankle_x = 0.47, 0.0, -1.1
    # Ankle height: the rendered forefoot hangs ~3 cm below the toe joint, so lift the
    # planted feet 3.1 cm to keep the sole on, not in, the floor (native geometry gate).
    az=0.08+0.031
    reach = 0.568  # 2 mm short of arm length: straight, without IK clamping.

    def key(a, yaw):
        ar, yr = math.radians(a), math.radians(yaw)
        k = _line(ankle_x, a, wrist_x)
        k.update(yaw=yaw, wrist_l=[wrist_x, half_width, .035],
                 wrist_r=[wrist_x, -half_width, .035],
                 hand_l=[.7,.7,0], hand_r=[.7,-.7,0], head_detail=1)
        k['pelvis'] = [ankle_x + math.cos(ar)*math.cos(yr)*.898,
                       math.cos(ar)*math.sin(yr)*.898, az + math.sin(ar)*.898]
        for side, sign in (('l',1),('r',-1)):
            # Feet pivot a few mm at the ankles; the toe tips stay planted.
            ankle=[ankle_x-math.sin(yr)*sign*.08, math.cos(yr)*sign*.08, az]
            toe=[ankle_x+.18,sign*.08,az-.06]
            # Hip positions follow the same rotated rigid plank as the torso.
            up,left=trunk_frame(90-a,yaw,0)
            hip=add(add(k['pelvis'],mul(left,sign*.09)),mul(up,-.03))
            ankle=planted_ankle(hip,toe,ankle,side=sign)
            k['ankle_'+side]=ankle
            k['foot_'+side]=sub(toe,ankle)
        return k

    def support_distance(a, yaw):
        up,left=trunk_frame(90-a,yaw,0)
        pelvis=add([ankle_x,0,az],mul(up,.898))
        shoulder=add(add(pelvis,mul(up,.47)),mul(left,.18))
        return norm(sub(shoulder,[wrist_x,half_width,.035]))

    top=bisect_target(lambda a:support_distance(a,0), 12,18,reach)
    def pose(a, side):
        yaw=bisect_target(lambda y:support_distance(a,y),-15,0,reach) if a<top-1e-7 else 0
        return key(a,yaw*side)
    # One right and one left repetition; reset at the centre between sides.
    phases=[(0,dict(a=top,side=1)),(.07,dict(a=top,side=1)),
            (.21,dict(a=4,side=1)),(.27,dict(a=4,side=1)),
            (.43,dict(a=top,side=1)),(.5,dict(a=top,side=-1)),
            (.57,dict(a=top,side=-1)),(.71,dict(a=4,side=-1)),
            (.77,dict(a=4,side=-1)),(.93,dict(a=top,side=-1)),
            (1,dict(a=top,side=-1))]
    # Explicit half-cycle side avoids interpolating a sign through zero.
    def frame(t):
        values=sample(phases,t)
        return pose(values['a'],1 if t<.5 else -1)
    return dict(seconds=6, keys=[(i/144,frame(i/144)) for i in range(145)],
                cam_az=65,cam_el=28,props=[],motion_profile='archer-alternating',
                anatomy_detail=True)


def _one_arm(a, wx, wy_r=-0.14, elbow_r=(-0.6, -0.7, 1.0), ankle_x=-0.99):
    """One hand under the chest (right, near side), left hand behind the back (not on the floor), feet wide."""
    ar = math.radians(a)
    d = [math.cos(ar), 0.0, math.sin(ar)]
    pelvis = add([ankle_x, 0.0, 0.08], mul(d, 0.898))
    pitch = 90 - a
    return base_pose(pelvis=pelvis, p1=pitch, p2=pitch, hp=-8,
                     ankle_l=[ankle_x, 0.18, 0.08], ankle_r=[ankle_x, -0.18, 0.08],
                     foot_l=[0.95, 0, -0.3], foot_r=[0.95, 0, -0.3], knee_l=[0, 0, -1], knee_r=[0, 0, -1],
                     wrist_r=[wx, wy_r, 0.035], elbow_r=list(elbow_r), hand_r=[1, 0, 0], palm_r=[0, 0, -1],
                     grip_r=0.0,
                     wrist_l_rel=[-0.25, 0.08, 0.04], elbow_l=[-0.6, 0.7, 1.0], hand_l=[0, 0, -1],
                     palm_l=[0, 0, 1], grip_l=0.0)


def pushup_one_arm_negative():
    # Slow lowering on one arm (about 4 s); the chest stops just above the floor, then both hands reset.
    top = _one_arm(16, -0.06)
    bottom = _one_arm(4, -0.05)
    return dict(seconds=4.0, keys=rep(top, bottom, down_end=0.80), cam_az=30, cam_el=10, props=[])


def pushup_one_arm():
    # Full one-arm push-up: chest lowers to the floor with the body square; the near arm bends deeper.
    top = _one_arm(16, -0.06)
    bottom = _one_arm(2, -0.05, elbow_r=(-0.6, -0.7, 1.0))
    return dict(seconds=3.0, keys=rep(top, bottom), cam_az=30, cam_el=10, props=[])


def _planche(pelvis, p, ankle, knee_pole=(0, 0, 1), foot=(1, 0, -0.3), wrist_x=-0.30):
    """Arms on the floor (wrists fixed at wrist_x), shoulders about 0.5 m up; legs per ankle target."""
    return base_pose(pelvis=list(pelvis), p1=p, p2=p, hp=-8,
                     **both(wrist=[wrist_x, 0.20, 0.035], elbow=[-0.2, 0.8, 0.5], hand=[1, 0, 0],
                            palm=[0, 0, -1], grip=0.0, knee=list(knee_pole), ankle=list(ankle), foot=list(foot)))


def planche_lean():
    # Push-up position, shoulders forward past the hands, legs straight, toes on the floor.
    a = _planche([-0.42, 0.0, 0.41], 80, [-1.25, 0.08, 0.08])
    b = _planche([-0.42, 0.0, 0.405], 80, [-1.25, 0.08, 0.08])
    return dict(seconds=3.0, keys=rep(a, b), cam_az=30, cam_el=10, props=[])


def planche_tuck():
    # Feet lifted, knees tucked to the chest (knees about 40, hips about 30), arms straight.
    a = _planche([-0.42, 0.0, 0.41], 80, [-0.20, 0.08, 0.25], knee_pole=(1, 0, 0.3), foot=(1, 0, 0))
    b = _planche([-0.42, 0.0, 0.41], 80, [-0.20, 0.08, 0.26], knee_pole=(1, 0, 0.3), foot=(1, 0, 0))
    return dict(seconds=3.0, keys=rep(a, b), cam_az=30, cam_el=10, props=[])


def planche_adv_tuck():
    # Flat back; hips open (about 125) so the thighs point back and the feet sit behind and above the knees.
    # Knees about 65, off the floor. Arms straight. The hold is static (a 1 cm breath moved the arms out of reach).
    a = _planche([-0.42, 0.0, 0.52], 94, [-0.92, 0.08, 0.54], knee_pole=(0.3, 0, -1), foot=(-1, 0, 0))
    b = a
    return dict(seconds=3.0, keys=rep(a, b), cam_az=30, cam_el=10, props=[])


def _dip(pz, px, p1, legs, wz=1.05, wx=0.0, lean_p=3):
    bars = [dict(kind="rod", a=[-0.20, 0.25, 1.0], b=[0.20, 0.25, 1.0], r=0.017),
            dict(kind="rod", a=[-0.20, -0.25, 1.0], b=[0.20, -0.25, 1.0], r=0.017)]
    return base_pose(pelvis=[px, 0.0, pz], p1=p1, p2=p1 + lean_p, hp=0,
                     **both(wrist=[wx, 0.25, wz], elbow=[-1, 0.2, 0.0], hand=[1, 0, 0], palm=[0, 0, -1],
                            grip=0.9, knee=[1, 0, 0], ankle_rel=legs, foot=[1, 0, 0])), bars


def dip_support_hold():
    a, bars = _dip(1.15, 0.0, 2, [0.03, 0.07, -0.84])
    b, _ = _dip(1.13, 0.0, 2, [0.03, 0.07, -0.84])
    return dict(seconds=3.0, keys=rep(a, b), cam_az=30, cam_el=10, props=bars)


def dip_negative():
    a, bars = _dip(1.15, 0.0, 2, [0.03, 0.07, -0.84])
    b, _ = _dip(0.85, 0.0, 12, [0.30, 0.07, -0.60])
    return dict(seconds=4.0, keys=rep(a, b, down_end=0.80), cam_az=30, cam_el=10, props=bars)


def dip_parallel():
    a, bars = _dip(1.15, 0.0, 2, [0.03, 0.07, -0.84])
    b, _ = _dip(0.85, 0.0, 12, [0.30, 0.07, -0.60])
    return dict(seconds=3.0, keys=rep(a, b), cam_az=30, cam_el=10, props=bars)


def _pike(px, pz, p, ankle=(-0.9, 0.10, 0.08), wx=0.05, wy=0.20):
    """Hips high, trunk folded forward; the feet and hands are the fixed contacts."""
    return base_pose(pelvis=[px, 0.0, pz], p1=p, p2=p, hp=0,
                     **both(ankle=list(ankle), foot=[1, 0, -0.3], knee=[0, 0, 1], wrist=[wx, wy, 0.035],
                            elbow=[-0.3, 0.5, 1.0], hand=[1, 0, 0], palm=[0, 0, -1], grip=0.0))


def pike_pushup():
    # Toes on the floor, legs straight; the head lowers between the hands and the upper arms bend.
    top = _pike(-0.6, 0.85, 140, ankle=(-0.95, 0.10, 0.08))
    bottom = _pike(-0.35, 0.70, 145, ankle=(-0.95, 0.10, 0.08))
    return dict(seconds=3.0, keys=rep(top, bottom), cam_az=30, cam_el=10, props=[])


def pike_pushup_elevated():
    # Toes on a chair seat (top z 0.53), legs straight; the head lowers between the hands.
    chair = [dict(kind="chair", x=-0.95, seat=0.45)]
    top = _pike(-0.38, 0.92, 140, ankle=(-1.15, 0.10, 0.53), wx=0.15)
    bottom = _pike(-0.30, 0.72, 150, ankle=(-1.15, 0.10, 0.53), wx=0.15)
    return dict(seconds=3.0, keys=rep(top, bottom), cam_az=30, cam_el=10, props=chair)


def _inv(px, pz, p, ax=-0.20, az=1.97, wx=-0.20, wy=0.20):
    """Inverted body: hands on the floor, feet on the wall, belly to the wall (wall face at x = 0)."""
    return base_pose(pelvis=[px, 0.0, pz], p1=p, p2=p, hp=0,
                     **both(ankle=[ax, 0.08, az], foot=[1, 0, 0], knee=[1, 0, 0], wrist=[wx, wy, 0.035],
                            elbow=[0, 1, 0], hand=[1, 0, 0], palm=[0, 0, -1], grip=0.0))


def _wall_props():
    return [dict(kind="box", center=[0.025, 0, 1.2], size=[0.05, 1.6, 2.4])]


def wall_handstand_hold():
    # Chest to the wall, arms straight, body tight; the breath moves the hips about 1 cm.
    a = _inv(-0.20, 1.07, 180, ax=-0.20, az=1.965)
    b = _inv(-0.20, 1.07, 180, ax=-0.20, az=1.965)
    return dict(seconds=3.0, keys=rep(a, b), cam_az=-30, cam_el=10, props=_wall_props())


def hspu_wall_negative():
    # Kick up to the wall, then lower slowly (about 4 s) until the head touches the folded mat.
    top = _inv(-0.20, 1.07, 180, ax=-0.20, az=1.965)
    bottom = _inv(-0.20, 0.84, 180, ax=-0.20, az=1.735)
    mat = [dict(kind="box", center=[-0.25, 0, 0.02], size=[0.45, 0.60, 0.04])]
    return dict(seconds=4.0, keys=rep(top, bottom, down_end=0.80), cam_az=-30, cam_el=10,
                props=_wall_props() + mat)


def hspu_wall():
    # Same wall handstand: lower to the mat, then press to full lockout.
    top = _inv(-0.20, 1.07, 180, ax=-0.20, az=1.965)
    bottom = _inv(-0.20, 0.85, 180, ax=-0.20, az=1.745)
    mat = [dict(kind="box", center=[-0.25, 0, 0.02], size=[0.45, 0.60, 0.04])]
    return dict(seconds=3.0, keys=rep(top, bottom), cam_az=-30, cam_el=10, props=_wall_props() + mat)


EXERCISES = {
    "pushup-standard": pushup_standard,
    "pushup-knee": pushup_knee,
    "pushup-incline": pushup_incline,
    "pushup-incline-high": pushup_incline_high,
    "pushup-wall": pushup_wall,
    "pushup-diamond": pushup_diamond,
    "pushup-archer": pushup_archer,
    "pushup-one-arm-negative": pushup_one_arm_negative,
    "pushup-one-arm": pushup_one_arm,
    "planche-lean": planche_lean,
    "planche-tuck": planche_tuck,
    "planche-adv-tuck": planche_adv_tuck,
    "dip-support-hold": dip_support_hold,
    "dip-negative": dip_negative,
    "dip-parallel": dip_parallel,
    "pike-pushup": pike_pushup,
    "pike-pushup-elevated": pike_pushup_elevated,
    "wall-handstand-hold": wall_handstand_hold,
    "hspu-wall-negative": hspu_wall_negative,
    "hspu-wall": hspu_wall,
}
