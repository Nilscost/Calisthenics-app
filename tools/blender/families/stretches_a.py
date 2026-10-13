"""Stretches, group A (tools/blender/POSES.md, HAIKU-TASK.md). Structure follows families/squat_lowerlegs.py.

Stretches: key a = start position, key b = deeper into the stretch, seconds=4. Per-side stretches stretch the
right side (camera side). Lying face up p = -90; face down (cobra, frog) p = +90.
Seated: pelvis z 0.11 on the floor (hip 0.08 with the trunk upright; straight leg ankle 0.868 ahead of the hip).
Kneeling: knee on the floor under the hip (hip z 0.50), shin on the floor (ankle 0.43 behind the knee).
Feet planted stay at ankle z 0.08 in every key; hands on a wall are world targets on the wall face.
"""
from poses import base_pose, both, rep

FLOOR_WRIST_Z = 0.035
WALL_CENTER_X = 0.60  # wall in front of the figure: thin box, face at x = 0.575
WALL = dict(kind="box", center=[WALL_CENTER_X, 0.0, 1.2], size=[0.05, 1.6, 2.4])


# --- seated -----------------------------------------------------------------

def _seat_feet_forward(ax, ay=0.10, foot=(0.3, 0, 1)):
    """Both legs straight forward on the floor, ankles world targets (reach 0.868 keeps the knees >= 165)."""
    return dict(ankle_l=[ax, ay, 0.08], ankle_r=[ax, -ay, 0.08], knee_l=[1, 0, 0], knee_r=[1, 0, 0],
                foot_l=list(foot), foot_r=list(foot))


def stretch_90_90():
    # Seated, right (front) knee bent 90 with the shin pointing out to the right; left (back) knee bent 90 to the left,
    # shin pointing back. Key a: sit tall. Key b: chest leans forward over the front leg.
    legs = dict(ankle_r=[0.42, -0.52, 0.08], ankle_l=[-0.42, 0.52, 0.08],
                knee_r=[0.7, 0.7, 0.2], knee_l=[0.7, 0.7, 0.2], foot_r=[0.7, -0.7, 0], foot_l=[-0.7, 0.7, 0])
    arms = dict(wrist_r_rel=[0.40, -0.12, -0.33], wrist_l_rel=[0.20, 0.15, -0.40],
                elbow_r=[0.5, -0.5, 0.5], elbow_l=[0.5, 0.5, 0.5], hand_r=[1, 0, 0], hand_l=[1, 0, 0])
    a = base_pose(pelvis=[0.0, 0.0, 0.11], p1=0, p2=0, hp=0, **legs, **arms)
    b = base_pose(pelvis=[0.0, 0.0, 0.11], p1=30, p2=45, hp=0, **legs, **arms)
    return dict(seconds=4.0, keys=rep(a, b), cam_az=30, cam_el=15, props=[])


def stretch_butterfly():
    # Seated, soles together (feet in front of the pelvis), knees dropped out to the sides, hands hold the feet.
    feet = dict(ankle_r=[0.12, -0.05, 0.08], ankle_l=[0.12, 0.05, 0.08], knee_r=[0, -1, 0.5], knee_l=[0, 1, 0.5],
                foot_r=[1, 0, 0], foot_l=[1, 0, 0])
    hold = dict(wrist_r=[0.12, -0.05, 0.12], wrist_l=[0.12, 0.05, 0.12], hand_r=[0, 0, 1], hand_l=[0, 0, 1],
                elbow_r=[0, -1, 0.4], elbow_l=[0, 1, 0.4])
    # The knees are not on the floor; the pelvis is.
    a = base_pose(pelvis=[0.0, 0.0, 0.11], p1=0, p2=0, hp=0, **feet, **hold)
    b = base_pose(pelvis=[0.0, 0.0, 0.11], p1=45, p2=50, hp=0, **feet, **hold)
    return dict(seconds=4.0, keys=rep(a, b), cam_az=35, cam_el=15, props=[])


def stretch_hamstring():
    # Seated, both legs straight forward (knees >= 165). Key a: sit tall, hands hanging near the thighs.
    # Key b: hinge forward from the hips with a long back; hands reach toward the shins.
    feet = _seat_feet_forward(0.868, foot=(0.3, 0, 1))
    arms = both(wrist_rel=[0.30, 0.10, -0.30])
    arms.update(hand_l=[1, 0, 0], hand_r=[1, 0, 0])
    a = base_pose(pelvis=[0.0, 0.0, 0.11], p1=0, p2=0, hp=0, **feet, **arms)
    b = base_pose(pelvis=[0.025, 0.0, 0.097], p1=55, p2=55, hp=0, **feet, **arms)
    return dict(seconds=4.0, keys=rep(a, b), cam_az=35, cam_el=15, props=[])


def stretch_pancake():
    # Seated straddle, both legs straight out to the sides (ankles 0.868 from the hips, knees >= 165).
    # Key a: sit tall, hands on the floor in front. Key b: hinge from the hips and walk the hands forward.
    ax, ay = 0.367, 0.877
    legs = dict(ankle_r=[ax, -ay, 0.08], ankle_l=[ax, ay, 0.08], knee_r=[0, 0, 1], knee_l=[0, 0, 1],
                foot_r=[0, 0, 1], foot_l=[0, 0, 1])
    a = base_pose(pelvis=[0.0, 0.0, 0.11], p1=0, p2=0, hp=0, **legs,
                  wrist_l=[0.20, 0.20, 0.20], wrist_r=[0.20, -0.20, 0.20],
                  hand_l=[1, 0, 0], hand_r=[1, 0, 0], palm_l=[0, 0, -1], palm_r=[0, 0, -1],
                  grip_l=0.0, grip_r=0.0, elbow_l=[-0.5, 0.3, 1], elbow_r=[-0.5, -0.3, 1])
    b = base_pose(pelvis=[0.027, 0.0, 0.093], p1=65, p2=65, hp=0, **legs,
                  wrist_l=[0.80, 0.08, FLOOR_WRIST_Z], wrist_r=[0.80, -0.08, FLOOR_WRIST_Z],
                  hand_l=[1, 0, 0], hand_r=[1, 0, 0], palm_l=[0, 0, -1], palm_r=[0, 0, -1],
                  grip_l=0.0, grip_r=0.0, elbow_l=[-0.5, 0.3, 1], elbow_r=[-0.5, -0.3, 1])
    return dict(seconds=4.0, keys=rep(a, b), cam_az=35, cam_el=20, props=[])


# --- kneeling ---------------------------------------------------------------

def stretch_back():
    # Key a: hands and knees (table), back flat. Key b: child's pose, hips sit back onto the heels, arms forward.
    kneel = dict(ankle_l=[-0.46, 0.10, 0.08], ankle_r=[-0.46, -0.10, 0.08], knee_l=[1, 0, 0], knee_r=[1, 0, 0],
                 foot_l=[-0.95, 0, -0.3], foot_r=[-0.95, 0, -0.3])
    hands = dict(wrist_l=[0.40, 0.22, FLOOR_WRIST_Z], wrist_r=[0.40, -0.22, FLOOR_WRIST_Z],
                 hand_l=[1, 0, 0], hand_r=[1, 0, 0], palm_l=[0, 0, -1], palm_r=[0, 0, -1], grip_l=0.0, grip_r=0.0,
                 elbow_l=[0, 1, 0.4], elbow_r=[0, -1, 0.4])
    table = base_pose(pelvis=[0.0, 0.0, 0.50], p1=90, p2=90, hp=0, **kneel, **hands)
    # In-between keys keep the knees on the floor while the hips travel back to the heels and return.
    mid1 = base_pose(pelvis=[-0.20, 0.0, 0.46], p1=85, p2=85, hp=0, **kneel, **hands)
    mid2 = base_pose(pelvis=[-0.30, 0.0, 0.40], p1=80, p2=80, hp=0, **kneel, **hands)
    child = base_pose(pelvis=[-0.40, 0.0, 0.26], p1=70, p2=70, hp=-5, **kneel, **hands)
    keys = [(0.0, table), (0.1, table), (0.22, mid1), (0.36, mid2), (0.48, child), (0.58, child),
            (0.70, mid2), (0.84, mid1)]
    return dict(seconds=4.0, keys=keys, cam_az=35, cam_el=25, props=[])


def stretch_hip_flexor():
    # Right knee on the floor (mat under the knee), right shin flat behind; left foot flat in front, knee bent.
    # Key a: upright, pelvis under the hip. Key b: pelvis tucked and shifted forward, back hip opens.
    back = dict(ankle_r=[-0.55, -0.10, 0.08], foot_r=[-0.95, 0, -0.3], knee_r=[1, 0, -0.5])
    front = dict(ankle_l=[0.40, 0.10, 0.08], foot_l=[1, 0, 0], knee_l=[1, 0, 0])
    arms = dict(wrist_l_rel=[0.02, 0.04, -0.565], wrist_r_rel=[0.02, -0.04, -0.565])
    a = base_pose(pelvis=[-0.12, 0.0, 0.53], p1=0, p2=0, hp=0, **back, **front, **arms)
    b = base_pose(pelvis=[-0.06, 0.0, 0.52], p1=-5, p2=-5, hp=0, **back, **front, **arms)
    return dict(seconds=4.0, keys=rep(a, b), cam_az=35, cam_el=15, props=[])


# --- face down (cobra) and the frog ----------------------------------------

def stretch_cobra():
    # Face down: pelvis and legs on the floor, hands under the shoulders. Key a: lying flat.
    # Key b: chest pressed up with the hips down, long neck (head pitch -10 relative to the upper trunk).
    legs = dict(ankle_l=[-0.90, 0.10, 0.08], ankle_r=[-0.90, -0.10, 0.08], knee_l=[0, 0, 1], knee_r=[0, 0, 1],
                foot_l=[-1, 0, 0], foot_r=[-1, 0, 0])
    hands = dict(wrist_l=[0.66, 0.22, FLOOR_WRIST_Z], wrist_r=[0.66, -0.22, FLOOR_WRIST_Z],
                 hand_l=[1, 0, 0], hand_r=[1, 0, 0], palm_l=[0, 0, -1], palm_r=[0, 0, -1], grip_l=0.0, grip_r=0.0,
                 elbow_l=[0, 1, 0.5], elbow_r=[0, -1, 0.5])
    a = base_pose(pelvis=[0.0, 0.0, 0.11], p1=90, p2=90, hp=0, **legs, **hands)
    b = base_pose(pelvis=[0.0, 0.0, 0.11], p1=88, p2=70, hp=-10, **legs, **hands)
    return dict(seconds=4.0, keys=rep(a, b), cam_az=35, cam_el=20, props=[])


def stretch_frog():
    # Kneeling with the knees wide and the feet turned out; forearms on the floor. Key a: hips over the knees.
    # Key b: hips rocked back toward the heels and lower.
    legs = dict(ankle_r=[-0.52, -0.72, 0.08], ankle_l=[-0.52, 0.72, 0.08], foot_r=[-0.2, -1, 0], foot_l=[-0.2, 1, 0])
    # Knee poles keep the knee on the floor: pointing down and out (the solver puts the knee below the hip-ankle line).
    legs_a = dict(legs, knee_r=[-0.5, -1, -0.7], knee_l=[-0.5, 1, -0.7])
    legs_b = dict(legs, knee_r=[0, -1, -0.6], knee_l=[0, 1, -0.6])
    arms = dict(wrist_l=[0.62, 0.22, FLOOR_WRIST_Z], wrist_r=[0.62, -0.22, FLOOR_WRIST_Z],
                hand_l=[1, 0, 0], hand_r=[1, 0, 0], palm_l=[0, 0, -1], palm_r=[0, 0, -1], grip_l=0.0, grip_r=0.0,
                elbow_l=[-1, 1, -1], elbow_r=[-1, -1, -1])
    a = base_pose(pelvis=[-0.03, 0.0, 0.30], p1=90, p2=90, hp=0, **legs_a, **arms)
    b = base_pose(pelvis=[-0.22, 0.0, 0.30], p1=90, p2=90, hp=0, **legs_b, **arms)
    return dict(seconds=4.0, keys=rep(a, b), cam_az=35, cam_el=25, props=[])


# --- standing -------------------------------------------------------------

def stretch_ankle_mobility():
    # Right foot a hand-width (toes 0.085 m) from the wall, right knee drives forward toward the wall.
    # Key a: knee short of the wall. Key b: knee over the toes, touching the wall. Left foot stays planted behind.
    feet = dict(ankle_r=[0.31, -0.10, 0.08], foot_r=[1, 0, 0], ankle_l=[-0.20, 0.10, 0.08], foot_l=[1, 0, 0],
                knee_r=[1, 0, 0], knee_l=[1, 0, 0])
    a = base_pose(pelvis=[-0.05, 0.0, 0.86], p1=0, p2=0, hp=0, **feet)
    b = base_pose(pelvis=[0.20, 0.0, 0.80], p1=0, p2=0, hp=0, **feet)
    return dict(seconds=4.0, keys=rep(a, b), cam_az=30, cam_el=10, props=[WALL])


def stretch_calf_wall():
    # Hands on the wall at chest height (arms nearly straight). Right leg back, heel flat, straight.
    # Key a: upright. Key b: hips move forward toward the wall, right calf stretched (heel stays down).
    hands = dict(wrist_l=[0.545, 0.20, 1.30], wrist_r=[0.545, -0.20, 1.30], hand_l=[0, 0, 1], hand_r=[0, 0, 1],
                 palm_l=[1, 0, 0], palm_r=[1, 0, 0], grip_l=0.0, grip_r=0.0)
    feet = dict(ankle_r=[-0.41, -0.10, 0.08], foot_r=[1, 0, 0], knee_r=[1, 0, 0],
                ankle_l=[0.30, 0.10, 0.08], foot_l=[1, 0, 0], knee_l=[1, 0, 0])
    a = base_pose(pelvis=[-0.03, 0.0, 0.89], p1=2, p2=2, hp=0, **feet, **hands)
    b = base_pose(pelvis=[0.08, 0.0, 0.82], p1=10, p2=10, hp=0, **feet, **hands)
    return dict(seconds=4.0, keys=rep(a, b), cam_az=35, cam_el=10, props=[WALL])


def stretch_chest_door():
    # Right forearm vertical on the frame (thin box, inner face at y = -0.55), elbow at shoulder height, out to the side.
    # Key a: facing forward. Key b: body turned away from the frame (yaw +30), chest opens.
    frame = dict(kind="box", center=[0.0, -0.575, 1.2], size=[1.6, 0.05, 2.4])
    feet = dict(ankle_r=[0.0, -0.10, 0.08], ankle_l=[0.0, 0.10, 0.08], foot_r=[1, 0, 0], foot_l=[1, 0, 0],
                knee_r=[1, 0, 0], knee_l=[1, 0, 0])
    arm = dict(wrist_r=[0.0, -0.52, 1.58], palm_r=[0, -1, 0], hand_r=[0, 0, 1], elbow_r=[0.2, -1, 0],
               grip_r=0.0, wrist_l_rel=[0.02, 0.04, -0.565], elbow_l=[-1, 0.2, 0], hand_l=[0, 0, -1],
               palm_l=[0, -1, 0], grip_l=0.25)
    a = base_pose(pelvis=[0.0, 0.0, 0.975], p1=0, p2=0, hp=0, yaw=0, **feet, **arm)
    b = base_pose(pelvis=[0.0, 0.0, 0.975], p1=0, p2=0, hp=0, yaw=30, **feet, **arm)
    return dict(seconds=4.0, keys=rep(a, b), cam_az=35, cam_el=10, props=[frame])


def stretch_hamstring_standing():
    # Right heel a step ahead on the floor, toes up, leg straight (knee >= 165). Left leg plants the stance.
    # Key a: upright, hands hanging. Key b: hinge forward from the hips with a flat back.
    front = dict(ankle_r=[0.45, -0.10, 0.08], foot_r=[0.3, 0, 1], knee_r=[1, 0, 0])
    stance = dict(ankle_l=[0.0, 0.10, 0.08], foot_l=[1, 0, 0], knee_l=[1, 0, 0])
    arms = dict(wrist_r_rel=[0.10, -0.04, -0.55], wrist_l_rel=[0.10, 0.04, -0.55])
    a = base_pose(pelvis=[0.0, 0.0, 0.85], p1=0, p2=0, hp=0, **front, **stance, **arms)
    b = base_pose(pelvis=[0.021, 0.0, 0.841], p1=45, p2=50, hp=0, **front, **stance, **arms)
    return dict(seconds=4.0, keys=rep(a, b), cam_az=35, cam_el=10, props=[])


def stretch_lat_wall():
    # Hands on the wall at hip height, arms straight (wrists at z 1.00). Feet well back, planted.
    # Key a: hips back, trunk 78. Key b: chest sinks between the arms (shoulders lower), trunk 88 (near horizontal),
    # legs nearly straight (knees 160-175).
    hands = dict(wrist_l=[0.545, 0.20, 1.00], wrist_r=[0.545, -0.20, 1.00], hand_l=[0, 0, 1], hand_r=[0, 0, 1],
                 palm_l=[1, 0, 0], palm_r=[1, 0, 0], grip_l=0.0, grip_r=0.0)
    feet = dict(ankle_l=[-1.05, 0.10, 0.08], ankle_r=[-1.05, -0.10, 0.08], foot_l=[1, 0, 0], foot_r=[1, 0, 0],
                knee_l=[1, 0, 0], knee_r=[1, 0, 0])
    a = base_pose(pelvis=[-0.46, 0.0, 0.75], p1=78, p2=78, hp=0, **hands, **feet)
    b = base_pose(pelvis=[-0.41, 0.0, 0.70], p1=88, p2=88, hp=0, **hands, **feet)
    return dict(seconds=4.0, keys=rep(a, b), cam_az=30, cam_el=10, props=[WALL])


EXERCISES = {
    "stretch-90-90": stretch_90_90,
    "stretch-ankle-mobility": stretch_ankle_mobility,
    "stretch-back": stretch_back,
    "stretch-butterfly": stretch_butterfly,
    "stretch-calf-wall": stretch_calf_wall,
    "stretch-chest-door": stretch_chest_door,
    "stretch-cobra": stretch_cobra,
    "stretch-frog": stretch_frog,
    "stretch-hamstring": stretch_hamstring,
    "stretch-hamstring-standing": stretch_hamstring_standing,
    "stretch-hip-flexor": stretch_hip_flexor,
    "stretch-lat-wall": stretch_lat_wall,
    "stretch-pancake": stretch_pancake,
}
