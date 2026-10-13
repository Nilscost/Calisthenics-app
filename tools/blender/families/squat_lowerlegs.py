"""Squat and lower-leg exercises (tools/blender/POSES.md, HAIKU-TASK.md). squat-air is the worked example."""
import math

from poses import base_pose, both, rep


def squat_air():
    # Feet flat shoulder-width, arms forward for balance; full depth with heels down.
    top = base_pose(pelvis=[0.0, 0.0, 0.975], **both(ankle=[0.0, 0.14, 0.08], foot=[1, 0.25, 0], knee=[1, 0.25, 0],
                           wrist_rel=[0.15, 0.0, -0.53], elbow=[-1, 0.2, 0], hand=[0, 0, -1]))
    bottom = base_pose(pelvis=[-0.24, 0.0, 0.40], p1=35, p2=40, hp=-15,
                       **both(ankle=[0.0, 0.14, 0.08], foot=[1, 0.25, 0], knee=[1, 0.25, 0],
                              wrist_rel=[0.56, 0.0, -0.05], elbow=[0, 0.3, -1], hand=[1, 0, 0]))
    return dict(seconds=3.0, keys=rep(top, bottom), cam_az=35, cam_el=8, props=[])


def split_squat():
    # Stance fixed (right foot 0.9 m in front). Top: standing in the stride; bottom: back knee on the floor.
    top = base_pose(pelvis=[0.0, 0.0, 0.85], p1=3, p2=4, hp=-5,
                    ankle_r=[0.45, -0.10, 0.08], foot_r=[1, 0, 0], knee_r=[1, 0, 0],
                    ankle_l=[-0.45, 0.10, 0.07], foot_l=[1, 0, -0.3], knee_l=[1, 0, -1])
    bottom = base_pose(pelvis=[0.0, 0.0, 0.50], p1=6, p2=8, hp=-5,
                       ankle_r=[0.45, -0.10, 0.08], foot_r=[1, 0, 0], knee_r=[1, 0, 0],
                       ankle_l=[-0.45, 0.10, 0.07], foot_l=[1, 0, -0.35], knee_l=[1, 0, -1])
    return dict(seconds=3.0, keys=rep(top, bottom), cam_az=30, cam_el=10, props=[])


def split_squat_bulgarian():
    # Rear foot on a chair seat behind (chair back at x = -0.75); front foot 0.85 m ahead of the chair back.
    chair = dict(kind="chair", x=-0.75, seat=0.45)
    top = base_pose(pelvis=[0.0, 0.0, 0.91], p1=2, p2=3, hp=-3,
                    ankle_r=[0.30, -0.10, 0.08], foot_r=[1, 0, 0], knee_r=[1, 0, 0],
                    ankle_l=[-0.55, 0.10, 0.53], foot_l=[1, 0, 0], knee_l=[0, 0, -1])
    bottom = base_pose(pelvis=[0.0, 0.0, 0.65], p1=10, p2=12, hp=-5,
                       ankle_r=[0.30, -0.10, 0.08], foot_r=[1, 0, 0], knee_r=[1, 0, 0],
                       ankle_l=[-0.55, 0.10, 0.53], foot_l=[1, 0, 0], knee_l=[0, 0, -1])
    return dict(seconds=3.0, keys=rep(top, bottom), cam_az=30, cam_el=10, props=[chair])


def _shrimp(lean, right_arm):
    """Stands on the right leg; the left foot is held behind, the left knee drops to the floor at the bottom."""
    def key(pz, px, ankle_l, foot_l, knee_l, wrist_l, hand_l):
        return base_pose(pelvis=[px, 0.0, pz], p1=lean, p2=lean, hp=-5,
                         ankle_r=[0.0, -0.10, 0.08], foot_r=[1, 0, 0], knee_r=[1, -0.15, 0],
                         ankle_l=ankle_l, foot_l=foot_l, knee_l=knee_l, wrist_l=wrist_l, hand_l=hand_l,
                         **right_arm)
    top = key(0.975, 0.0, [-0.45, 0.10, 0.70], [-0.6, 0, -0.8], [0, 0, -1], [-0.22, 0.14, 1.00], [-0.6, 0, -0.8])
    bottom = key(0.52, -0.05, [-0.25, 0.12, 0.42], [-0.6, 0, -0.8], [1, 0, -1], [-0.20, 0.15, 0.60],
                 [-0.2, 0, -0.98])
    return dict(seconds=3.5, keys=rep(top, bottom), cam_az=35, cam_el=12, props=[])


def squat_shrimp():
    # Beginner: right arm forward for balance, slight forward lean.
    return _shrimp(8, dict(wrist_r_rel=[0.5, -0.1, 0.0], hand_r=[1, 0, 0]))


def squat_shrimp_intermediate():
    # Upright chest, free arm hangs; the rear foot is held by the same-side hand.
    return _shrimp(3, dict())


def squat_shrimp_advanced():
    # Torso upright, free arm out to the side (T balance) for the deepest, most controlled version.
    return _shrimp(0, dict(wrist_r_rel=[0.0, -0.55, 0.0], hand_r=[0, -1, 0]))


def squat_pistol_assisted():
    # Stands on the right (near) leg, left leg straight out in front, right hand on a door post.
    post = dict(kind="box", center=[0.40, -0.30, 1.05], size=[0.05, 0.05, 2.0])
    r = math.radians(35)

    def key(pz, px, p1, p2, ankle_l):
        return base_pose(pelvis=[px, 0.0, pz], p1=p1, p2=p2, hp=-10,
                         ankle_r=[0.0, -0.10, 0.08], foot_r=[1, 0, 0], knee_r=[1, -0.15, 0],
                         ankle_l=ankle_l, foot_l=[1, 0, 0.3], knee_l=[1, 0, 0],
                         wrist_r=[0.36, -0.30, 1.05], hand_r=[1, 0, 0], palm_r=[0, 1, 0], grip_r=0.8)
    top = key(0.975, 0.0, 2, 4, [0.60, 0.10, 0.36])
    bottom = key(0.50, -0.22, 24, 26, [0.60, 0.10, 0.25])
    return dict(seconds=4.0, keys=rep(top, bottom), cam_az=22, cam_el=10, props=[post])


def squat_pistol_box():
    # Sits down onto a chair behind on the right leg; the left leg stays straight out in front.
    chair = dict(kind="chair", x=-0.30, seat=0.45)
    top = base_pose(pelvis=[0.0, 0.0, 0.975], p1=2, p2=4, hp=-5,
                    ankle_r=[0.12, -0.10, 0.08], foot_r=[1, 0, 0], knee_r=[1, -0.15, 0],
                    ankle_l=[0.75, 0.10, 0.50], foot_l=[1, 0, 0.4], knee_l=[1, 0, 0])
    bottom = base_pose(pelvis=[-0.08, 0.0, 0.56], p1=28, p2=30, hp=-5,
                       ankle_r=[0.12, -0.10, 0.08], foot_r=[1, 0, 0], knee_r=[1, -0.15, 0],
                       ankle_l=[0.70, 0.10, 0.22], foot_l=[1, 0, 0.2], knee_l=[1, 0, 0])
    return dict(seconds=4.0, keys=rep(top, bottom), cam_az=22, cam_el=10, props=[chair])


def squat_pistol_counterbalance():
    # Both arms straight out in front holding a light dumbbell each; no support; torso leans forward at the bottom.
    weights = [dict(kind="weight", type="dumbbell", joint="wrist_l", off=[0, 0, 0]),
               dict(kind="weight", type="dumbbell", joint="wrist_r", off=[0, 0, 0])]

    def key(pz, px, p1, p2, ankle_l):
        return base_pose(pelvis=[px, 0.0, pz], p1=p1, p2=p2, hp=-10,
                         ankle_r=[0.0, -0.10, 0.08], foot_r=[1, 0, 0], knee_r=[1, -0.15, 0],
                         ankle_l=ankle_l, foot_l=[1, 0, 0.3], knee_l=[1, 0, 0],
                         wrist_l_rel=[0.565, 0.04, -0.02], wrist_r_rel=[0.565, -0.04, -0.02],
                         elbow_l=[0, 0.3, -1], elbow_r=[0, -0.3, -1], hand_l=[1, 0, 0], hand_r=[1, 0, 0],
                         grip_l=0.9, grip_r=0.9)
    top = key(0.975, 0.0, 2, 4, [0.60, 0.10, 0.36])
    bottom = key(0.40, -0.25, 40, 48, [0.60, 0.10, 0.45])
    return dict(seconds=4.0, keys=rep(top, bottom), cam_az=22, cam_el=10, props=weights)


def walking_lunge():
    # Four keys: right-front lunge, stand, left-front lunge, stand. Feet slide between the stances.
    lunge_r = base_pose(pelvis=[0.0, 0.0, 0.50], p1=6, p2=8, hp=-5,
                        ankle_r=[0.45, -0.10, 0.08], foot_r=[1, 0, 0], knee_r=[1, 0, 0],
                        ankle_l=[-0.45, 0.10, 0.07], foot_l=[1, 0, -0.35], knee_l=[1, 0, -1])
    lunge_l = base_pose(pelvis=[0.0, 0.0, 0.50], p1=6, p2=8, hp=-5,
                        ankle_r=[-0.45, -0.10, 0.07], foot_r=[1, 0, -0.35], knee_r=[1, 0, -1],
                        ankle_l=[0.45, 0.10, 0.08], foot_l=[1, 0, 0], knee_l=[1, 0, 0])
    stand = base_pose(pelvis=[0.0, 0.0, 0.975], ankle_r=[0.0, -0.10, 0.08], ankle_l=[0.0, 0.10, 0.08],
                      knee_r=[1, 0, 0], knee_l=[1, 0, 0])
    keys = [(0.0, lunge_r), (0.25, stand), (0.5, lunge_l), (0.75, stand)]
    return dict(seconds=4.0, keys=keys, cam_az=35, cam_el=10, props=[])


def calf_raise():
    # Both feet, heels down, then rise onto the balls of the feet with straight knees.
    flat = base_pose(pelvis=[0.0, 0.0, 0.975], ankle_r=[0.0, -0.10, 0.08], ankle_l=[0.0, 0.10, 0.08],
                     foot_r=[1, 0, 0], foot_l=[1, 0, 0], knee_r=[1, 0, 0], knee_l=[1, 0, 0])
    rise = base_pose(pelvis=[0.0, 0.0, 1.015], ankle_r=[0.0, -0.10, 0.12], ankle_l=[0.0, 0.10, 0.12],
                     foot_r=[0.82, 0, -0.57], foot_l=[0.82, 0, -0.57], knee_r=[1, 0, 0], knee_l=[1, 0, 0])
    return dict(seconds=3.0, keys=rep(rise, flat), cam_az=35, cam_el=8, props=[])


def _single_calf(lean_pelvis_y=0.0):
    # Stands on the right foot; the left leg is bent and lifted in front. Right foot rises onto its ball.
    def key(pz, ankle_r, foot_r):
        return base_pose(pelvis=[0.0, lean_pelvis_y, pz], ankle_r=ankle_r, foot_r=foot_r, knee_r=[1, 0, 0],
                         ankle_l=[0.12, 0.12, 0.35], foot_l=[1, 0, -0.5], knee_l=[1, 0, 0])
    flat = key(0.975, [0.0, -0.10, 0.08], [1, 0, 0])
    rise = key(1.015, [0.0, -0.10, 0.12], [0.82, 0, -0.57])
    return rep(rise, flat)


def calf_raise_single():
    return dict(seconds=3.0, keys=_single_calf(), cam_az=35, cam_el=8, props=[])


def calf_raise_single_loaded():
    # Dumbbell in the right (working) hand at the side; the left hand rests lightly on a wall in front.
    wall = dict(kind="box", center=[0.46, 0.0, 1.2], size=[0.05, 1.6, 2.4])
    weight = dict(kind="weight", type="dumbbell", joint="wrist_r", off=[0, 0, 0])

    def key(pz, ankle_r, foot_r):
        return base_pose(pelvis=[0.0, 0.0, pz], ankle_r=ankle_r, foot_r=foot_r, knee_r=[1, 0, 0],
                         ankle_l=[0.12, 0.12, 0.35], foot_l=[1, 0, -0.5], knee_l=[1, 0, 0],
                         wrist_l=[0.42, 0.18, 1.15], hand_l=[0, 0, 1], palm_l=[1, 0, 0])
    flat = key(0.975, [0.0, -0.10, 0.08], [1, 0, 0])
    rise = key(1.015, [0.0, -0.10, 0.12], [0.82, 0, -0.57])
    return dict(seconds=3.0, keys=rep(rise, flat), cam_az=-30, cam_el=8, props=[wall, weight])


def _tibialis(single):
    # Back against a wall (wall face at x = -0.325), feet about a foot in front; toes lift toward the shins.
    wall = dict(kind="box", center=[-0.35, 0.0, 1.2], size=[0.05, 1.6, 2.4])

    def key(toes_up):
        r_ankle = [0.0, -0.10, 0.075 if toes_up else 0.08]
        r_foot = [0.9, 0, 0.45] if toes_up else [1, 0, 0]
        extra = dict(ankle_l=[0.05, 0.10, 0.25], foot_l=[1, 0, -0.3], knee_l=[1, 0, 0]) if single else \
            dict(ankle_l=[0.0, 0.10, 0.08], foot_l=[0.9, 0, 0.45] if toes_up else [1, 0, 0], knee_l=[1, 0, 0])
        return base_pose(pelvis=[-0.23, 0.0, 0.94], p1=0, p2=0, hp=0,
                         ankle_r=r_ankle, foot_r=r_foot, knee_r=[1, 0, 0], **extra)
    return dict(seconds=3.0, keys=rep(key(True), key(False)), cam_az=35, cam_el=8, props=[wall])


def tibialis_raise():
    return _tibialis(single=False)


def tibialis_raise_single():
    return _tibialis(single=True)


EXERCISES = {
    "squat-air": squat_air,
    "split-squat": split_squat,
    "split-squat-bulgarian": split_squat_bulgarian,
    "squat-shrimp": squat_shrimp,
    "squat-shrimp-intermediate": squat_shrimp_intermediate,
    "squat-shrimp-advanced": squat_shrimp_advanced,
    "squat-pistol-assisted": squat_pistol_assisted,
    "squat-pistol-box": squat_pistol_box,
    "squat-pistol-counterbalance": squat_pistol_counterbalance,
    "walking-lunge": walking_lunge,
    "calf-raise": calf_raise,
    "calf-raise-single": calf_raise_single,
    "calf-raise-single-loaded": calf_raise_single_loaded,
    "tibialis-raise": tibialis_raise,
    "tibialis-raise-single": tibialis_raise_single,
}
