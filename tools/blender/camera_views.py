"""Reviewed camera-only changes, shared by rendering and installation checks."""

CAMERA_OVERRIDES = {
    "handstand-wall-one-foot": (-90, 12),
    "handstand-toe-pulls": (-90, 12),
    "headstand-wall": (-90, 18),
    "hspu-wall-deficit": (90, 15),
    "handstand-press-wall-negative": (90, 18),
    "flag-support-press": (-25, 12),
}


def camera_stamp(ex_id):
    angles = CAMERA_OVERRIDES.get(ex_id)
    return {"cam_az": angles[0], "cam_el": angles[1]} if angles else None
