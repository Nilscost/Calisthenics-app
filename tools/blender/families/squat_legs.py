"""Squat, lunge and knee-extension poses for the R8 exercise set."""

from poses import base_pose, both, rep


def _arms_forward(wrist=0.50):
    return dict(wrist_l_rel=[wrist, 0.04, -0.06], wrist_r_rel=[wrist, -0.04, -0.06],
                elbow_l=[0, 0.3, -1], elbow_r=[0, -0.3, -1],
                hand_l=[1, 0, 0], hand_r=[1, 0, 0])


def _squat(top_z=0.975, bottom_z=0.39, stance=0.13, lean=34, foot_x=0.0,
           foot_z=0.08, bottom_x=-0.22, arm_reach=0.50):
    arms = _arms_forward(arm_reach)
    top = base_pose(pelvis=[0, 0, top_z], p1=2, p2=3,
                    ankle_l=[foot_x, stance, foot_z], ankle_r=[foot_x, -stance, foot_z],
                    foot_l=[1, 0, 0], foot_r=[1, 0, 0], knee_l=[1, 0, 0], knee_r=[1, 0, 0], **arms)
    bottom = base_pose(pelvis=[bottom_x, 0, bottom_z], p1=lean, p2=lean + 4, hp=-10,
                       ankle_l=[foot_x, stance, foot_z], ankle_r=[foot_x, -stance, foot_z],
                       foot_l=[1, 0, 0], foot_r=[1, 0, 0], knee_l=[1, 0, 0], knee_r=[1, 0, 0], **arms)
    return top, bottom


def squat_assisted():
    post = dict(kind="box", center=[0.42, -0.30, 1.15], size=[0.05, 0.05, 2.3])
    top = base_pose(pelvis=[0, 0, 0.975], ankle_l=[0, 0.13, 0.08], ankle_r=[0, -0.13, 0.08],
                    wrist_r=[0.42, -0.30, 1.15], hand_r=[0, 0, 1], palm_r=[-1, 0, 0], grip_r=0.35)
    bottom = base_pose(pelvis=[-0.20, 0, 0.42], p1=33, p2=37, hp=-8,
                       ankle_l=[0, 0.13, 0.08], ankle_r=[0, -0.13, 0.08],
                       wrist_r=[0.42, -0.30, 1.15], hand_r=[0, 0, 1], palm_r=[-1, 0, 0], grip_r=0.35)
    return dict(seconds=3.5, keys=rep(top, bottom), cam_az=28, cam_el=10, props=[post])


def squat_box():
    chair = dict(kind="chair", x=-0.38, seat=0.45)
    top, _ = _squat(bottom_z=0.52, bottom_x=-0.12)
    bottom = base_pose(pelvis=[-0.18, 0, 0.53], p1=27, p2=30, hp=-7,
                       ankle_l=[0, 0.13, 0.08], ankle_r=[0, -0.13, 0.08],
                       foot_l=[1, 0, 0], foot_r=[1, 0, 0], knee_l=[1, 0, 0], knee_r=[1, 0, 0], **_arms_forward())
    return dict(seconds=3.5, keys=rep(top, bottom), cam_az=30, cam_el=10, props=[chair])


def squat_half():
    top, bottom = _squat(bottom_z=0.69, bottom_x=-0.10, lean=24)
    return dict(seconds=3.0, keys=rep(top, bottom), cam_az=32, cam_el=10, props=[])


def squat_close():
    top, bottom = _squat(stance=0.045, bottom_z=0.39, bottom_x=-0.20, lean=37, arm_reach=0.54)
    return dict(seconds=3.5, keys=rep(top, bottom), cam_az=28, cam_el=12, props=[])


def squat_uneven():
    block = dict(kind="box", center=[0.0, 0.18, 0.075], size=[0.34, 0.32, 0.15])
    def key(pz, px):
        return base_pose(pelvis=[px, -0.035, pz], p1=27 if pz < 0.7 else 2, p2=31 if pz < 0.7 else 3,
                         ankle_r=[0, -0.10, 0.08], ankle_l=[0, 0.18, 0.23],
                         foot_r=[1, 0, 0], foot_l=[1, 0, 0], knee_r=[1, -0.1, 0], knee_l=[1, 0.1, 0],
                         **_arms_forward())
    return dict(seconds=3.5, keys=rep(key(0.975, 0), key(0.38, -0.20)), cam_az=28, cam_el=12, props=[block])


def squat_cossack():
    def key(pz, py, bent):
        return base_pose(pelvis=[-0.08 if bent else 0, py, pz], p1=8 if bent else 2, p2=10 if bent else 3,
                         ankle_r=[0, -0.32 if bent else -0.20, 0.08], ankle_l=[0, 0.77 if bent else 0.20, 0.08],
                         foot_r=[1, -0.35, 0], foot_l=[1, 0.35, 0.45 if bent else 0],
                         knee_r=[1, -0.6, 0], knee_l=[1, 0.5, 0], **_arms_forward(0.42))
    top = key(0.975, 0, False)
    bottom = key(0.44, -0.12, True)
    return dict(seconds=4.0, keys=rep(top, bottom), cam_az=35, cam_el=15, props=[])


def _pistol(elevated=False):
    step_z = 0.20 if elevated else 0.0
    props = [dict(kind="box", center=[0, 0, step_z / 2], size=[0.75, 0.70, step_z])] if elevated else []
    def key(pz, px, p1, p2, free_ankle):
        return base_pose(pelvis=[px, 0, pz], p1=p1, p2=p2, hp=-8,
                         ankle_r=[0, -0.10, 0.08 + step_z], foot_r=[1, 0, 0], knee_r=[1, -0.2, 0],
                         ankle_l=free_ankle, foot_l=[1, 0, 0.25], knee_l=[1, 0, 0], **_arms_forward(0.56))
    top = key(0.975 + step_z, 0, 2, 4, [0.45, 0.10, 0.58 if elevated else 0.48])
    bottom = key(0.40 + step_z, -0.22, 37, 43, [0.45, 0.10, 0.10 if elevated else 0.22])
    return dict(seconds=4.0, keys=rep(top, bottom, hold_top=0.08, down_end=0.60, hold_bottom=0.08),
                cam_az=24, cam_el=14, props=props)


def squat_pistol_negative():
    def key(pz, px, pitch, free):
        return base_pose(pelvis=[px, 0, pz], p1=pitch, p2=pitch + 3, hp=-8,
                         ankle_r=[0, -0.10, 0.08], foot_r=[1, 0, 0], knee_r=[1, -0.2, 0],
                         ankle_l=free, foot_l=[1, 0, 0.2], knee_l=[1, 0, 0], **_arms_forward(0.55))
    both_up = key(0.975, 0, 2, [0, 0.10, 0.08])
    high = key(0.88, -0.03, 8, [0.38, 0.10, 0.42])
    bottom = key(0.40, -0.22, 38, [0.58, 0.10, 0.23])
    return dict(seconds=4.5, keys=[(0.0, both_up), (0.10, both_up), (0.48, bottom), (0.57, bottom),
                                   (0.76, high), (0.88, both_up)], cam_az=24, cam_el=12, props=[])


def squat_pistol_elevated():
    return _pistol(elevated=True)


def squat_shrimp_two_hand():
    def key(pz, px, back_ankle, back_knee, pitch):
        # Both wrists track the held ankle, with each hand cupping one side of the heel.
        wr_l = [back_ankle[0], back_ankle[1] + 0.025, back_ankle[2]]
        wr_r = [back_ankle[0], back_ankle[1] - 0.025, back_ankle[2]]
        return base_pose(pelvis=[px, 0, pz], p1=pitch, p2=pitch + 2, hp=-5,
                         ankle_r=[0, -0.10, 0.08], foot_r=[1, 0, 0], knee_r=[1, -0.15, 0],
                         ankle_l=back_ankle, foot_l=[-0.6, 0, -0.8], knee_l=back_knee,
                         wrist_l=wr_l, wrist_r=wr_r, hand_l=[0, -1, 0], hand_r=[0, 1, 0],
                         palm_l=[0, 0, 1], palm_r=[0, 0, 1], grip_l=0.8, grip_r=0.8,
                         elbow_l=[-1, 0.3, -0.2], elbow_r=[-1, -0.3, -0.2])
    top = key(0.68, -0.08, [-0.45, 0.0, 0.76], [0, 0, -1], -10)
    bottom = key(0.51, -0.10, [-0.23, 0.0, 0.45], [0, 0, -1], -10)
    return dict(seconds=4.0, keys=rep(top, bottom), cam_az=32, cam_el=14, props=[])


def squat_shrimp_deficit():
    # Compact top under the working foot; the rear knee is kept behind its x footprint.
    step = dict(kind="box", center=[0, -0.25, 0.10], size=[0.50, 0.42, 0.20])
    def key(pz, px, knee_l, ankle_l, pitch):
        return base_pose(pelvis=[px, 0, pz], p1=pitch, p2=pitch + 2, hp=-5,
                         ankle_r=[0, -0.25, 0.28], foot_r=[1, 0, 0], knee_r=[1, -0.15, 0],
                         ankle_l=ankle_l, foot_l=[-0.6, 0, -0.8], knee_l=knee_l,
                         wrist_l=[ankle_l[0], ankle_l[1] + 0.02, ankle_l[2]],
                         wrist_r_rel=[0.42, -0.12, -0.04],
                         hand_l=[0, -1, 0], hand_r=[1, 0, 0],
                         grip_l=0.8, palm_l=[0, 0, 1], elbow_l=[-1, 0.3, -0.2])
    top = key(0.86, 0, [0, 0, -1], [-0.38, 0.12, 0.84], -25)
    bottom = key(0.50, -0.02, [1, 0, -1], [-0.28, 0.12, 0.30], -42)
    return dict(seconds=4.0, keys=rep(top, bottom), cam_az=32, cam_el=14, props=[step])


def squat_wall_sit():
    wall = dict(kind="box", center=[-0.375, 0, 1.2], size=[0.05, 1.6, 2.4])
    def key(z):
        return base_pose(pelvis=[-0.25, 0, z], p1=0, p2=0, hp=0,
                         ankle_l=[0.16, 0.12, 0.08], ankle_r=[0.16, -0.12, 0.08],
                         foot_l=[1, 0, 0], foot_r=[1, 0, 0], knee_l=[1, 0, 0], knee_r=[1, 0, 0],
                         wrist_l_rel=[0.20, 0.04, -0.34], wrist_r_rel=[0.20, -0.04, -0.34],
                         elbow_l=[1, 0.3, 0], elbow_r=[1, -0.3, 0])
    return dict(seconds=3.0, keys=[(0.0, key(0.57)), (0.5, key(0.59))], cam_az=15, cam_el=8, props=[wall])


def squat_horse_stance():
    def key(z):
        return base_pose(pelvis=[-0.10, 0, z], p1=3, p2=4,
                         ankle_r=[0, -0.40, 0.08], ankle_l=[0, 0.40, 0.08],
                         foot_r=[1, -0.2, 0], foot_l=[1, 0.2, 0],
                         knee_r=[1, -0.7, 0], knee_l=[1, 0.7, 0], **_arms_forward(0.46))
    return dict(seconds=3.0, keys=[(0.0, key(0.54)), (0.5, key(0.56))], cam_az=30, cam_el=13, props=[])


def _step_up(height, deep=False):
    # Keep the step under the near-side working foot only; the trailing ankle
    # remains outside the box throughout the lift.
    step = dict(kind="box", center=[0, -0.16, height / 2], size=[0.62, 0.28, height])
    def key(pz, px, ankle_l):
        return base_pose(pelvis=[px, 0, pz], p1=13, p2=15, hp=-7,
                         ankle_r=[0, -0.10, height + 0.08], foot_r=[1, 0, 0], knee_r=[1, -0.1, 0],
                         ankle_l=ankle_l, foot_l=[1, 0, 0], knee_l=[1, 0, 0],
                         **_arms_forward(0.45))
    # Free foot begins on the floor, lifts clear as the right leg stands on the step,
    # then returns to the floor on the controlled lowering phase.
    top = key(0.975 + height, 0.02, [0.20, 0.10, 0.42 if not deep else 0.82])
    bottom = key(0.78 if not deep else 0.82, -0.04, [-0.34, 0.10, 0.08])
    if deep:
        middle = key(1.08, -0.01, [0.04, 0.10, 0.40])
        keys = [(0.0, bottom), (0.16, bottom), (0.43, middle), (0.70, top),
                (0.79, top), (0.88, middle), (0.98, bottom)]
    else:
        keys = [(0.0, bottom), (0.18, bottom), (0.68, top), (0.80, top), (0.98, bottom)]
    return dict(seconds=4.0, keys=keys, cam_az=27, cam_el=12, props=[step])


def step_up():
    return _step_up(0.20)


def step_up_deep():
    return _step_up(0.46, deep=True)


def lunge_reverse():
    def pose(pz, px, rear_x):
        return base_pose(pelvis=[px, 0, pz], p1=4, p2=5, hp=-4,
                         ankle_r=[0.35, -0.10, 0.08], foot_r=[1, 0, 0], knee_r=[1, -0.1, 0],
                         ankle_l=[rear_x, 0.10, 0.08], foot_l=[1, 0, 0], knee_l=[1, 0, -1],
                         wrist_l_rel=[0, 0.04, -0.36], wrist_r_rel=[0, -0.04, -0.36])
    stand = base_pose(pelvis=[0, 0, 0.975], ankle_l=[0, 0.10, 0.08], ankle_r=[0, -0.10, 0.08])
    bottom = pose(0.51, -0.02, -0.40)
    return dict(seconds=4.0, keys=[(0.0, stand), (0.15, stand), (0.48, bottom), (0.58, bottom), (0.88, stand)],
                cam_az=32, cam_el=12, props=[])


def lunge_lateral():
    def key(pz, py, wide):
        return base_pose(pelvis=[0, py, pz], p1=6, p2=7, hp=-5,
                         ankle_r=[0, -0.52 if wide else -0.10, 0.08],
                         ankle_l=[0, 0.64 if wide else 0.10, 0.08],
                         foot_r=[1, -0.25, 0], foot_l=[1, 0.25, 0.45 if wide else 0],
                         knee_r=[1, -0.8, 0], knee_l=[1, 0.6, 0], **_arms_forward(0.45))
    return dict(seconds=3.5, keys=rep(key(0.975, 0, False), key(0.54, -0.20, True)),
                cam_az=50, cam_el=15, props=[])


def reverse_nordic():
    mat = dict(kind="box", center=[-0.10, 0, 0.015], size=[1.2, 0.8, 0.03])
    def key(px, pz, pitch):
        return base_pose(pelvis=[px, 0, pz], p1=pitch, p2=pitch, hp=-5,
                         ankle_l=[-0.35, 0.10, 0.08], ankle_r=[-0.35, -0.10, 0.08],
                         foot_l=[-1, 0, 0], foot_r=[-1, 0, 0], knee_l=[1, 0, -1], knee_r=[1, 0, -1],
                         **_arms_forward(0.42))
    upright = key(0, 0.52, 0)
    lean = key(-0.14, 0.50, -25)
    return dict(seconds=4.0, keys=rep(upright, lean), cam_az=20, cam_el=12, props=[mat])


def _sissy(assisted=False):
    post = dict(kind="box", center=[-0.23, -0.30, 1.1], size=[0.05, 0.05, 2.2])
    def key(pz, px, lean):
        over = dict(wrist_r=[-0.23, -0.30, 1.0], hand_r=[0, 0, 1], palm_r=[-1, 0, 0], grip_r=0.35) if assisted else _arms_forward(0.50)
        return base_pose(pelvis=[px, 0, pz], p1=lean, p2=lean, hp=-3,
                         ankle_l=[0, 0.10, 0.12], ankle_r=[0, -0.10, 0.12],
                         foot_l=[0.8, 0, -0.6], foot_r=[0.8, 0, -0.6],
                         knee_l=[0.35, 0, -1], knee_r=[0.35, 0, -1], **over)
    top = key(0.995, 0.05, 0)
    bottom = key(0.64, 0.15, -30)
    return dict(seconds=3.5, keys=rep(top, bottom), cam_az=26, cam_el=12, props=[post] if assisted else [])


def sissy_squat_assisted():
    return _sissy(True)


def sissy_squat():
    return _sissy(False)


def natural_leg_extension():
    mat = dict(kind="box", center=[-0.10, 0, 0.015], size=[1.2, 0.8, 0.03])
    anchor = dict(kind="box", center=[-0.75, 0, 0.35], size=[0.20, 0.70, 0.70])
    def key(px, pz, pitch):
        return base_pose(pelvis=[px, 0, pz], p1=pitch, p2=pitch, hp=-5,
                         ankle_l=[-0.50, 0.10, 0.08], ankle_r=[-0.50, -0.10, 0.08],
                         foot_l=[-1, 0, 0], foot_r=[-1, 0, 0], knee_l=[1, 0, -1], knee_r=[1, 0, -1],
                         **_arms_forward(0.45))
    upright = key(0, 0.55, 0)
    sit_back = key(-0.30, 0.49, -38)
    drive_forward = key(0.10, 0.62, -5)
    keys = [(0.0, upright), (0.18, upright), (0.48, sit_back),
            (0.60, sit_back), (0.82, drive_forward), (0.98, upright)]
    return dict(seconds=4.0, keys=keys, cam_az=24, cam_el=14, props=[mat, anchor])


EXERCISES = {
    "squat-assisted": squat_assisted,
    "squat-box": squat_box,
    "squat-half": squat_half,
    "squat-close": squat_close,
    "squat-uneven": squat_uneven,
    "squat-cossack": squat_cossack,
    "squat-pistol-negative": squat_pistol_negative,
    "squat-pistol-elevated": squat_pistol_elevated,
    "squat-shrimp-two-hand": squat_shrimp_two_hand,
    "squat-shrimp-deficit": squat_shrimp_deficit,
    "squat-wall-sit": squat_wall_sit,
    "squat-horse-stance": squat_horse_stance,
    "step-up": step_up,
    "step-up-deep": step_up_deep,
    "lunge-reverse": lunge_reverse,
    "lunge-lateral": lunge_lateral,
    "reverse-nordic": reverse_nordic,
    "sissy-squat-assisted": sissy_squat_assisted,
    "sissy-squat": sissy_squat,
    "natural-leg-extension": natural_leg_extension,
}
