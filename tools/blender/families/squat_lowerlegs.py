"""Squat and lower-leg exercises (tools/blender/POSES.md, HAIKU-TASK.md). squat-air is the worked example."""
from poses import base_pose, both, rep


def squat_air():
    # Feet flat shoulder-width, arms forward for balance; full depth with heels down.
    top = base_pose(pelvis=[0.0, 0.0, 0.975], **both(ankle=[0.0, 0.14, 0.08], foot=[1, 0.25, 0], knee=[1, 0.25, 0],
                           wrist_rel=[0.15, 0.0, -0.53], elbow=[-1, 0.2, 0], hand=[0, 0, -1]))
    bottom = base_pose(pelvis=[-0.24, 0.0, 0.40], p1=35, p2=40, hp=-15,
                       **both(ankle=[0.0, 0.14, 0.08], foot=[1, 0.25, 0], knee=[1, 0.25, 0],
                              wrist_rel=[0.56, 0.0, -0.05], elbow=[0, 0.3, -1], hand=[1, 0, 0]))
    return dict(seconds=3.0, keys=rep(top, bottom), cam_az=35, cam_el=8, props=[])


EXERCISES = {
    "squat-air": squat_air,
}
