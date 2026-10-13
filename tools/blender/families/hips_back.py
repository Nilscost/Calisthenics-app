"""Hip, back and posterior-chain exercises (tools/blender/POSES.md, HAIKU-TASK.md).

Lying face up (bridges, slides): p = -90, head toward -X. Lying face down (superman, arch,
reverse hyperextension): p = +90, head toward +X. Floor contacts: ankle 0.08, wrist 0.035, lying pelvis 0.11.
"""

import math

from poses import base_pose, dense, pt, rep

FLOOR_WRIST_Z = 0.035
FLAT_PALM = dict(palm_l=[0, 0, -1], palm_r=[0, 0, -1], grip_l=0.0, grip_r=0.0)


def _sides(x, y=0.25):
    """Both hands flat on the floor beside the body, fingers toward the feet (world targets)."""
    return dict(wrist_l=[x, y, FLOOR_WRIST_Z], wrist_r=[x, -y, FLOOR_WRIST_Z], hand_l=[1, 0, 0], hand_r=[1, 0, 0],
                elbow_l=[0, 1, 0.4], elbow_r=[0, -1, 0.4], **FLAT_PALM)


def _feet(ax, ay=0.10, knee=(1.0, 0.0, 0.3)):
    """Both feet flat on the floor in front of the body (world targets)."""
    return dict(ankle_l=[ax, ay, 0.08], ankle_r=[ax, -ay, 0.08], knee_l=list(knee), knee_r=list(knee),
                foot_l=[1, 0, 0], foot_r=[1, 0, 0])


def _right_foot(ax):
    """Only the right foot on the floor (working side); the left leg is set separately."""
    return dict(ankle_r=[ax, -0.10, 0.08], knee_r=[1, 0, 0.3], foot_r=[1, 0, 0])


def _left_leg_up():
    """Left leg straight up toward the ceiling, relative to the hip."""
    return dict(ankle_l_rel=[0.0, 0.01, 0.84], knee_l=[1, 0, 0], foot_l=[1, 0, 0])


def _prone_legs(z, x=-0.88):
    """Legs straight out behind (toward -X), ankles lifted to height z."""
    return dict(ankle_l=[x, 0.10, z], ankle_r=[x, -0.10, z], knee_l=[0, 0, 1], knee_r=[0, 0, 1],
                foot_l=[-1, 0, 0], foot_r=[-1, 0, 0])


def _overhead(x, y, z):
    """Arms overhead on the floor, palms down, fingers pointing forward (+X)."""
    return dict(wrist_l=[x, y, z], wrist_r=[x, -y, z], palm_l=[0, 0, -1], palm_r=[0, 0, -1],
                hand_l=[1, 0, 0], hand_r=[1, 0, 0], grip_l=0.0, grip_r=0.0,
                elbow_l=[0, 1, 0.3], elbow_r=[0, -1, 0.3])


# --- lying face up: bridges ---------------------------------------------------

def glute_bridge():
    # Lying with feet flat and knees bent; top: hips lifted, shoulders on the floor, shoulder-hip-knee in a line.
    arms = _sides(-0.02, 0.22)
    legs = _feet(0.60)
    lying = base_pose(pelvis=[0.10, 0, 0.11], p1=-90, p2=-90, **arms, **legs)
    top = base_pose(pelvis=[0.05, 0, 0.29], p1=-105, p2=-105, **arms, **legs)
    return dict(seconds=3.0, keys=rep(lying, top), cam_az=35, cam_el=15, props=[])


def bridge_single_leg():
    # Right (working) foot on the floor; left leg straight up. Lying, then the right hip lifts level.
    arms = _sides(-0.02, 0.22)
    lying = base_pose(pelvis=[0.10, 0, 0.11], p1=-90, p2=-90, **arms, **_right_foot(0.60), **_left_leg_up())
    top = base_pose(pelvis=[0.05, 0, 0.29], p1=-105, p2=-105, **arms, **_right_foot(0.60), **_left_leg_up())
    return dict(seconds=3.0, keys=rep(lying, top), cam_az=35, cam_el=15, props=[])


def bridge_table():
    # Reverse tabletop: hands on the floor behind the shoulders (fingers toward the feet), arms straight at the top,
    # belly up, trunk level, knees about 90. Lowers to a lower table.
    arms = _sides(-0.80, 0.20)
    legs = _feet(0.44, knee=(1.0, 0.0, 0.0))
    table = base_pose(pelvis=[0.0, 0, 0.50], p1=-90, p2=-90, **arms, **legs)
    low = base_pose(pelvis=[0.0, 0, 0.30], p1=-90, p2=-90, **arms, **legs)
    return dict(seconds=3.0, keys=rep(table, low), cam_az=35, cam_el=20, props=[])


def bridge_incline():
    # Hands on the front edge of a chair seat behind the shoulders. Top: hips pressed up, body arched, arms straight.
    # Bottom: seated in front of the chair, legs out, arms bent.
    chair = dict(kind="chair", x=-0.87, seat=0.45)
    arms = dict(wrist_l=[-0.50, 0.20, 0.47], wrist_r=[-0.50, -0.20, 0.47], palm_l=[0, 0, -1], palm_r=[0, 0, -1],
                hand_l=[1, 0, 0], hand_r=[1, 0, 0], grip_l=0.0, grip_r=0.0,
                elbow_l=[0, 1, 0.3], elbow_r=[0, -1, 0.3])
    legs = _feet(0.50)
    up = base_pose(pelvis=[0.30, 0, 0.60], p1=-143, p2=-143, hp=40, **arms, **legs)
    low = base_pose(pelvis=[-0.30, 0, 0.12], p1=0, p2=0, hp=0, **arms, **legs)
    return dict(seconds=3.0, keys=rep(up, low), cam_az=35, cam_el=15, props=[chair])


def bridge_head():
    # Full bridge resting on the top of the head: hands by the head, feet near the hips, weight on hands and feet.
    arms = _sides(-0.52, 0.20)
    legs = _feet(0.60)
    a = base_pose(pelvis=[0.12, 0, 0.56], p1=-115, p2=-130, hp=-20, **arms, **legs)
    b = base_pose(pelvis=[0.12, 0, 0.58], p1=-115, p2=-130, hp=-20, **arms, **legs)
    return dict(seconds=3.0, keys=rep(a, b), cam_az=35, cam_el=15, props=[])


def bridge_back():
    # Full wheel bridge: hands and feet on the floor, arms and legs straight, hips high, belly up.
    arms = _sides(-0.55, 0.22)
    legs = _feet(0.70)
    a = base_pose(pelvis=[0.10, 0, 0.70], p1=-100, p2=-120, hp=-10, **arms, **legs)
    b = base_pose(pelvis=[0.10, 0, 0.72], p1=-100, p2=-120, hp=-10, **arms, **legs)
    return dict(seconds=3.0, keys=rep(a, b), cam_az=35, cam_el=15, props=[])


def bridge_wall_walkdown():
    # Standing, back about a foot from a wall, hands on the wall. Hands walk down as the back arches.
    wall = dict(kind="box", center=[-0.30, 0, 1.2], size=[0.05, 1.6, 2.4])

    def key(pz, px, p1, p2, wz, hands_x):
        return base_pose(pelvis=[px, 0, pz], p1=p1, p2=p2, hp=-5,
                         wrist_l=[hands_x, 0.20, wz], wrist_r=[hands_x, -0.20, wz],
                         palm_l=[-1, 0, 0], palm_r=[-1, 0, 0], hand_l=[0, 0, 1], hand_r=[0, 0, 1],
                         grip_l=0.0, grip_r=0.0, elbow_l=[0.3, 1, 0], elbow_r=[0.3, -1, 0],
                         ankle_l=[0.0, 0.10, 0.08], ankle_r=[0.0, -0.10, 0.08],
                         knee_l=[1, 0, 0], knee_r=[1, 0, 0], foot_l=[1, 0, 0], foot_r=[1, 0, 0])
    high = key(0.955, 0.0, -3, -8, 1.95, -0.26)
    low = key(0.85, 0.15, -35, -55, 0.78, -0.26)
    return dict(seconds=4.0, keys=rep(high, low), cam_az=35, cam_el=12, props=[wall])


# --- lying face down: superman, arch, reverse hyperextension ------------------

def superman_hold():
    # Face down, arms straight forward together, legs straight; a small lift of chest and legs.
    arms = _overhead(1.00, 0.08, 0.12)
    legs = _prone_legs(0.14, x=-0.895)
    a = base_pose(pelvis=[0.0, 0, 0.13], p1=90, p2=90, hp=-10, **arms, **legs)
    b = base_pose(pelvis=[0.0, 0, 0.16], p1=90, p2=90, hp=-10, **arms, **legs)
    return dict(seconds=3.0, keys=rep(a, b), cam_az=35, cam_el=20, props=[])


def arch_hold_y():
    # Face down, arms wide in a Y overhead, chest and legs lifted higher than the superman; long neck.
    arms = _overhead(0.90, 0.38, 0.12)
    legs = _prone_legs(0.20)
    a = base_pose(pelvis=[0.0, 0, 0.15], p1=90, p2=84, hp=-30, **arms, **legs)
    b = base_pose(pelvis=[0.0, 0, 0.17], p1=90, p2=84, hp=-30, **arms, **legs)
    return dict(seconds=3.0, keys=rep(a, b), cam_az=35, cam_el=20, props=[])


def arch_rocks():
    # Arch position with arms together overhead; the body rocks forward and back a few centimetres.
    arms = _overhead(0.95, 0.10, 0.12)
    legs = _prone_legs(0.18, x=-0.86)
    a = base_pose(pelvis=[0.04, 0, 0.15], p1=90, p2=80, hp=-15, **arms, **legs)
    b = base_pose(pelvis=[-0.05, 0, 0.15], p1=90, p2=80, hp=-15, **arms, **legs)
    return dict(seconds=4.0, keys=rep(a, b), cam_az=35, cam_el=20, props=[])


def reverse_hyperextension():
    # Face down on a bench, hips at the edge, legs hanging; the glutes lift the legs to body height.
    bench = dict(kind="box", center=[0.48, 0.0, 0.40], size=[0.90, 0.50, 0.80])
    arms = dict(wrist_l=[0.60, 0.22, 0.835], wrist_r=[0.60, -0.22, 0.835], palm_l=[0, 0, -1], palm_r=[0, 0, -1],
                hand_l=[1, 0, 0], hand_r=[1, 0, 0], grip_l=0.0, grip_r=0.0,
                elbow_l=[0, 1, 0.3], elbow_r=[0, -1, 0.3])
    hang = base_pose(pelvis=[0.05, 0, 0.91], p1=90, p2=90, **arms,
                     ankle_l_rel=[-0.10, 0.01, -0.80], ankle_r_rel=[-0.10, -0.01, -0.80],
                     knee_l=[1, 0, 0], knee_r=[1, 0, 0], foot_l=[1, 0, 0], foot_r=[1, 0, 0])
    lifted = base_pose(pelvis=[0.05, 0, 0.91], p1=90, p2=90, **arms,
                       ankle_l_rel=[-0.86, 0.01, -0.03], ankle_r_rel=[-0.86, -0.01, -0.03],
                       knee_l=[0, 0, 1], knee_r=[0, 0, 1], foot_l=[-1, 0, 0], foot_r=[-1, 0, 0])
    return dict(seconds=3.0, keys=rep(hang, lifted), cam_az=35, cam_el=20, props=[bench])


# --- hinges: deadlift, swing, RDL ---------------------------------------------

def _hinge_legs(right_ax=0.0):
    """Both feet flat under the hips; knees soft (about 160 at the top)."""
    return dict(ankle_l=[right_ax, 0.10, 0.08], ankle_r=[right_ax, -0.10, 0.08],
                knee_l=[1, 0, 0], knee_r=[1, 0, 0], foot_l=[1, 0, 0], foot_r=[1, 0, 0])


def kettlebell_deadlift():
    # Kettlebell between the feet; hinge with a flat back, hands at the bell. Bottom: bell at knee height.
    weight = dict(kind="weight", type="kettlebell", joint="wrist_r", off=[0.0, 0.07, -0.05])

    def key(pz, px, pitch, wz, hx):
        return base_pose(pelvis=[px, 0, pz], p1=pitch, p2=pitch, hp=0,
                         wrist_l=[hx, 0.07, wz], wrist_r=[hx, -0.07, wz],
                         palm_l=[0, 0, -1], palm_r=[0, 0, -1], hand_l=[0, 0, -1], hand_r=[0, 0, -1],
                         grip_l=0.9, grip_r=0.9, elbow_l=[0, 1, -0.2], elbow_r=[0, -1, -0.2],
                         **_hinge_legs())
    top = key(0.955, 0.0, 0, 0.92, 0.02)
    bottom = key(0.93, -0.12, 70, 0.66, 0.30)
    return dict(seconds=3.5, keys=rep(top, bottom), cam_az=35, cam_el=10, props=[weight])


def kettlebell_swing():
    # Two hands on one bell: top, upright body with straight arms and the bell at chest height; bottom, hips back.
    weight = dict(kind="weight", type="kettlebell", joint="wrist_r", off=[0.0, 0.07, -0.05])

    def key(pz, px, pitch, wx, wz):
        return base_pose(pelvis=[px, 0, pz], p1=pitch, p2=pitch, hp=0,
                         wrist_l=[wx, 0.07, wz], wrist_r=[wx, -0.07, wz],
                         palm_l=[0, -1, 0], palm_r=[0, 1, 0], hand_l=[1, 0, 0], hand_r=[1, 0, 0],
                         grip_l=0.9, grip_r=0.9, elbow_l=[0, 1, -0.2], elbow_r=[0, -1, -0.2],
                         **_hinge_legs())
    top = key(0.965, 0.0, 0, 0.50, 1.20)
    bottom = key(0.93, -0.15, 65, 0.25, 0.68)
    return dict(seconds=2.5, keys=rep(top, bottom), cam_az=35, cam_el=10, props=[weight])


def kettlebell_swing_one_arm():
    # One hand on the bell (right, working side); top: upright, right arm straight, bell at chest height;
    # the left arm hangs loosely.
    weight = dict(kind="weight", type="kettlebell", joint="wrist_r", off=[0.0, 0.0, -0.05])

    def key(pz, px, pitch, wx, wz):
        return base_pose(pelvis=[px, 0, pz], p1=pitch, p2=pitch, hp=0,
                         wrist_r=[wx, -0.06, wz], palm_r=[0, 1, 0], hand_r=[1, 0, 0], grip_r=0.9,
                         elbow_r=[0, -1, -0.2],
                         wrist_l_rel=[0.02, 0.14, -0.52], palm_l=[0, -1, 0], hand_l=[0, 0, -1], grip_l=0.3,
                         elbow_l=[0, 1, -0.3], **_hinge_legs())
    top = key(0.965, 0.0, 0, 0.50, 1.20)
    bottom = key(0.93, -0.15, 65, 0.25, 0.68)
    return dict(seconds=2.5, keys=rep(top, bottom), cam_az=35, cam_el=10, props=[weight])


def rdl_bodyweight():
    # Standing with soft knees, hips back and a flat back to a forward lean of about 60; arms hang down the legs.
    def key(pz, px, pitch, wx, wz):
        return base_pose(pelvis=[px, 0, pz], p1=pitch, p2=pitch + 5, hp=0,
                         wrist_l=[wx, 0.10, wz], wrist_r=[wx, -0.10, wz],
                         palm_l=[0, -1, 0], palm_r=[0, 1, 0], hand_l=[0, 0, -1], hand_r=[0, 0, -1],
                         grip_l=0.0, grip_r=0.0, elbow_l=[0, 1, -0.2], elbow_r=[0, -1, -0.2],
                         **_hinge_legs())
    top = key(0.955, 0.0, 0, 0.02, 0.88)
    bottom = key(0.92, -0.20, 60, 0.24, 0.70)
    return dict(seconds=3.5, keys=rep(top, bottom), cam_az=35, cam_el=12, props=[])


def _single_leg_hinge(pz_top, px_bot, pz_bot, p_bot, arms_top, arms_bot, props):
    """Right leg stands; the left leg hangs behind and lifts in line with the body at the bottom."""
    def key(pz, px, p, free_leg, arms):
        fields = dict(pelvis=[px, 0, pz], p1=p, p2=p + 4, hp=0,
                      ankle_r=[0.0, -0.10, 0.08], knee_r=[1, 0, 0], foot_r=[1, 0, 0],
                      ankle_l_rel=free_leg, knee_l=[1, 0, 0], foot_l=[-1, 0, 0])
        fields.update(arms)
        return base_pose(**fields)
    top = key(pz_top, 0.0, 0, [-0.04, 0.01, -0.85], arms_top)
    bottom = key(pz_bot, px_bot, p_bot, [-0.80, 0.01, -0.35], arms_bot)
    return dict(seconds=3.5, keys=rep(top, bottom), cam_az=35, cam_el=10, props=props)


def kettlebell_single_leg_rdl():
    # Right leg stands; kettlebell in the left hand (opposite the standing leg); left leg reaches behind in line.
    weight = dict(kind="weight", type="kettlebell", joint="wrist_l", off=[0.0, 0.0, -0.05])
    arms_top = dict(wrist_l_rel=[0.02, 0.10, -0.55], wrist_r_rel=[0.02, -0.10, -0.55], palm_l=[0, -1, 0],
                    palm_r=[0, 1, 0], hand_l=[0, 0, -1], hand_r=[0, 0, -1], grip_l=0.9, grip_r=0.0,
                    elbow_l=[0, 1, -0.2], elbow_r=[0, -1, -0.2])
    arms_bot = dict(arms_top, wrist_l_rel=[0.20, 0.10, -0.50])
    return _single_leg_hinge(0.955, -0.12, 0.92, 60, arms_top, arms_bot, [weight])


def single_leg_deadlift():
    # Right leg stands; arms out to the sides to balance at the top, reaching forward at the bottom.
    arms_top = dict(wrist_l_rel=[0.0, 0.50, -0.02], wrist_r_rel=[0.0, -0.50, -0.02], palm_l=[0, 0, -1],
                    palm_r=[0, 0, -1], hand_l=[0, 1, 0], hand_r=[0, -1, 0], grip_l=0.0, grip_r=0.0,
                    elbow_l=[0, 1, 0], elbow_r=[0, -1, 0])
    arms_bot = dict(arms_top, wrist_l_rel=[0.52, 0.10, 0.0], wrist_r_rel=[0.52, -0.10, 0.0],
                    palm_l=[0, 0, -1], palm_r=[0, 0, -1], hand_l=[1, 0, 0], hand_r=[1, 0, 0])
    return _single_leg_hinge(0.955, -0.12, 0.92, 60, arms_top, arms_bot, [])


# --- nordic curls (kneeling, heels anchored) ----------------------------------

ANKLE_X = -0.43  # ankles anchored on the floor behind the knees


def _kneel_legs():
    """Knees on the floor (solved from the hip and the ankles), shins flat behind the knees, ankles anchored."""
    return dict(ankle_l=[ANKLE_X, 0.10, 0.08], ankle_r=[ANKLE_X, -0.10, 0.08],
                knee_l=[1, 0, -1], knee_r=[1, 0, -1], foot_l=[-1, 0, 0], foot_r=[-1, 0, 0])


def _nordic_pose(phi, arms):
    """One straight line from the knee: trunk and thigh both at phi degrees from vertical (0 = upright kneel,
    larger = the whole body tips forward about the knees, like a plank)."""
    s, c = math.sin(math.radians(phi)), math.cos(math.radians(phi))
    pelvis = [0.47 * s, 0.0, 0.06 + 0.47 * c]
    return base_pose(pelvis=pelvis, p1=phi, p2=phi, hp=0, **arms, **_kneel_legs())


_NORDIC_CROSS = dict(wrist_l_rel=[0.06, 0.12, -0.12], wrist_r_rel=[0.06, -0.12, -0.12], elbow_l=[0, 1, 0],
                     elbow_r=[0, -1, 0], palm_l=[0, -1, 0], palm_r=[0, 1, 0], hand_l=[0, 0, -1], hand_r=[0, 0, -1],
                     grip_l=0.0, grip_r=0.0)


def _nordic_seq(phi_bot, arms_fn):
    """Keys along the arc: the body tips forward about the knees from phi 0 to phi_bot (dense, so the knees stay down).
    arms_fn(w) gives the arm fields for w in 0 (start) .. 1 (bottom)."""
    keyed = [(0.0, dict(phi=0.0, w=0.0)), (0.1, dict(phi=0.0, w=0.0)),
             (0.48, dict(phi=phi_bot, w=1.0)), (0.58, dict(phi=phi_bot, w=1.0))]
    return dense(lambda phi, w: _nordic_pose(phi, arms_fn(w)), keyed, steps=48)


def nordic_curl():
    # Kneeling, upright line at the start (knees and shins on the floor); lowers forward as one line, arms crossed.
    return dict(seconds=4.0, keys=_nordic_seq(72, lambda w: _NORDIC_CROSS), cam_az=35, cam_el=20, props=[])


def _band_from_above():
    """Band from an anchor above and behind to the chest."""
    return dict(kind="rod", a=[-0.30, 0.0, 2.20], b=pt("chest"), r=0.008)


def nordic_banded():
    # Same kneeling start; a band from above and behind takes part of the weight on a partial lowering.
    return dict(seconds=4.0, keys=_nordic_seq(60, lambda w: _NORDIC_CROSS), cam_az=35, cam_el=20,
                props=[_band_from_above()])


def nordic_negative_banded():
    # Band from above; the deepest lowering: the body tips almost flat and the hands catch the floor.
    def arms(w):
        x = 0.10 + 1.15 * w
        z = 0.95 - 0.915 * w
        return dict(wrist_l=[x, 0.12 + 0.03 * w, z], wrist_r=[x, -(0.12 + 0.03 * w), z],
                    palm_l=[0, -1, 0], palm_r=[0, 1, 0], hand_l=[1, 0, 0], hand_r=[1, 0, 0],
                    grip_l=0.0, grip_r=0.0, elbow_l=[0, 1, 0.2], elbow_r=[0, -1, 0.2])
    return dict(seconds=4.5, keys=_nordic_seq(84, arms), cam_az=35, cam_el=20, props=[_band_from_above()])


# --- hamstring slides (bridge, heels on towels) -------------------------------

def slide_hamstring():
    # Bridge with the hips high; heels slide out to straight legs and are pulled back in by the hamstrings.
    arms = _sides(-0.02, 0.22)
    bent = base_pose(pelvis=[0.05, 0, 0.29], p1=-105, p2=-105, **arms, **_feet(0.42, knee=(1, 0, 0.6)))
    straight = base_pose(pelvis=[0.05, 0, 0.29], p1=-105, p2=-105, **arms, **_feet(0.88, knee=(1, 0, 0.3)))
    return dict(seconds=3.0, keys=rep(bent, straight), cam_az=35, cam_el=15, props=[])


def slide_negative():
    # Same bridge; the slow part: the heels slide out from the bent position toward straight legs, hips level.
    arms = _sides(-0.02, 0.22)
    bent = base_pose(pelvis=[0.05, 0, 0.29], p1=-105, p2=-105, **arms, **_feet(0.55, knee=(1, 0, 0.6)))
    straight = base_pose(pelvis=[0.05, 0, 0.27], p1=-105, p2=-105, **arms, **_feet(0.88, knee=(1, 0, 0.3)))
    return dict(seconds=4.0, keys=rep(bent, straight), cam_az=35, cam_el=15, props=[])


def slide_single_leg():
    # Bridge on the right heel; the left leg lifted; the right heel slides out and back in with the hips level.
    arms = _sides(-0.02, 0.22)
    bent = base_pose(pelvis=[0.05, 0, 0.29], p1=-105, p2=-105, **arms, **_right_foot(0.42), **_left_leg_up())
    straight = base_pose(pelvis=[0.05, 0, 0.29], p1=-105, p2=-105, **arms, **_right_foot(0.88), **_left_leg_up())
    return dict(seconds=3.0, keys=rep(bent, straight), cam_az=35, cam_el=15, props=[])


def slide_negative_single():
    # Single-leg bridge with the left leg lifted; the working heel slides slowly out, hips level and high.
    arms = _sides(-0.02, 0.22)
    bent = base_pose(pelvis=[0.05, 0, 0.29], p1=-105, p2=-105, **arms, **_right_foot(0.55), **_left_leg_up())
    straight = base_pose(pelvis=[0.05, 0, 0.29], p1=-105, p2=-105, **arms, **_right_foot(0.88), **_left_leg_up())
    return dict(seconds=4.5, keys=rep(straight, bent), cam_az=35, cam_el=15, props=[])


EXERCISES = {
    "glute-bridge": glute_bridge,
    "superman-hold": superman_hold,
    "kettlebell-deadlift": kettlebell_deadlift,
    "bridge-table": bridge_table,
    "bridge-single-leg": bridge_single_leg,
    "bridge-incline": bridge_incline,
    "bridge-head": bridge_head,
    "bridge-wall-walkdown": bridge_wall_walkdown,
    "bridge-back": bridge_back,
    "arch-hold-y": arch_hold_y,
    "arch-rocks": arch_rocks,
    "kettlebell-single-leg-rdl": kettlebell_single_leg_rdl,
    "kettlebell-swing": kettlebell_swing,
    "kettlebell-swing-one-arm": kettlebell_swing_one_arm,
    "rdl-bodyweight": rdl_bodyweight,
    "single-leg-deadlift": single_leg_deadlift,
    "nordic-negative-banded": nordic_negative_banded,
    "nordic-banded": nordic_banded,
    "nordic-curl": nordic_curl,
    "slide-negative": slide_negative,
    "slide-hamstring": slide_hamstring,
    "slide-negative-single": slide_negative_single,
    "slide-single-leg": slide_single_leg,
    "reverse-hyperextension": reverse_hyperextension,
}
