"""Authored full-cycle key specification for the standard pistol squat.

This module provides pose inputs only. It makes no claim about exercise form.
"""
import math


def specification():
    """Return a controlled, deep descent and explicit full return to standing."""
    # Imported lazily so poses.py can delegate its pistol() function here.
    from poses import dense

    st_ankle = [0.0, 0.09, 0.08]

    def key(pz, px, p1, p2, lift):
        r = math.radians(lift)
        return dict(pelvis=[px, 0.0, pz], p1=p1, p2=p2, hp=-10,
                    ankle_l=st_ankle, knee_l=[1, 0.15, 0], foot_l=[1, 0, 0],
                    ankle_r_rel=[0.862 * math.sin(r), -0.03, -0.862 * math.cos(r)],
                    knee_r=[0.3, 0, 1], foot_r=[math.cos(r) * 0.6, 0, math.sin(r) + 0.3],
                    wrist_l_rel=[0.565, 0.04, -0.02], wrist_r_rel=[0.565, -0.04, -0.02],
                    elbow_l=[0, 0.3, -1], elbow_r=[0, -0.3, -1],
                    hand_l=[1, 0, 0], hand_r=[1, 0, 0])

    # pz 0.98 puts the hip exactly one straight leg length above the planted ankle
    # (knee ~179 deg); the free leg stays forward and off the floor in every phase.
    top = {"pz": 0.98, "px": 0.0, "p1": 2, "p2": 4, "lift": 55}
    bottom = {"pz": 0.36, "px": -0.27, "p1": 38, "p2": 48, "lift": 86}
    # Same full-depth target as the prior movement, now with an explicit rise
    # and a quiet top phase to make the cycle and loop seam legible.
    keyed = [(0.00, top), (0.08, top), (0.48, bottom), (0.54, bottom),
             (0.94, top), (1.00, top)]
    return dict(seconds=4.0, keys=dense(key, keyed, 96), cam_az=22, cam_el=10, props=[])
