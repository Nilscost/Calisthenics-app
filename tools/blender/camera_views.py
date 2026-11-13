"""Reviewed camera-only changes, shared by rendering and installation checks."""

CAMERA_OVERRIDES = {
    "handstand-wall-one-foot": (-90, 12),
    "handstand-toe-pulls": (-90, 12),
    "headstand-wall": (-90, 18),
    "hspu-wall-deficit": (90, 15),
    "handstand-press-wall-negative": (90, 18),
    "handstand-press-straddle": (75, 16),
    "flag-support-press": (-25, 12),
    # Archer: lower 3/4 view keeps feet, head and the extended arm inside the frame at normal scale.
    "pushup-archer": (50, 20),
    # Saw: view from the feet end so both rotation sides and the level arm line read symmetrically.
    "pilates-saw": (90, 26),
    "ring-muscle-up": (12, 8),
    # Side-oblique, slightly from below: heels/shins over the bar and the body below.
    "heel-hook": (25, -6),
    # Near-frontal, slightly from below: the left/right leg sweep reads against the torso.
    "hanging-windshield-wiper": (82, -8),
}


def camera_stamp(ex_id):
    angles = CAMERA_OVERRIDES.get(ex_id)
    return {"cam_az": angles[0], "cam_el": angles[1]} if angles else None
