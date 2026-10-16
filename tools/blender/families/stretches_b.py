"""Stretches, group stretches_b (tools/blender/POSES.md, HAIKU-TASK.md). Key a = start, key b = deeper; 4 s.
Per-side stretches stretch the right side (camera side). Lying face up: p = -90 (head toward -X).
Face down / all fours: p = +90 (head toward +X). Side-lying on the right: p1 = p2 = 90, roll = +90.
Floor contacts: ankle 0.08, wrist 0.035, knee 0.06, lying pelvis 0.11."""

import math

from poses import base_pose, pt, rep, solve

FLOOR_WRIST_Z = 0.035


def _c(deg):
    return math.cos(math.radians(deg))


def _s(deg):
    return math.sin(math.radians(deg))


# --- kneeling on all fours: trunk horizontal (p = 90), knees and toes on the floor, hands flat ----------
def _fours(px, ankle_x, roll=0.0, palm=(0, 0, -1), hand=(-1, 0, 0), pz=0.50, **over):
    """Hands under the shoulders (world x = 0.47, fixed), knees under the hips (x = px - 0.03), toes back on the floor.
    roll tilts the trunk about its long axis (+ = right side down). Wrists and ankles are world targets."""
    k = dict(pelvis=[px, 0.0, pz], p1=90, p2=90, hp=0, roll=roll,
             ankle_l=[ankle_x, 0.10, 0.08], ankle_r=[ankle_x, -0.10, 0.08], foot_l=[-1, 0, 0], foot_r=[-1, 0, 0],
             knee_l=[1, 0, -1], knee_r=[1, 0, -1],
             wrist_l=[0.47, 0.18, FLOOR_WRIST_Z], wrist_r=[0.47, -0.18, FLOOR_WRIST_Z],
             hand_l=list(hand), hand_r=list(hand), palm_l=list(palm), palm_r=list(palm),
             elbow_l=[-1, 0, 0], elbow_r=[-1, 0, 0], grip_l=0.0, grip_r=0.0)
    k.update(over)
    return base_pose(**k)


# --- 1. standing, one heel on a chair, hinge forward ------------------------------------------------------
def pike_one_leg():
    chair = dict(kind="chair", x=0.46, seat=0.45)

    def key(px, pz, p):
        return base_pose(pelvis=[px, 0.0, pz], p1=p, p2=p, hp=0,
                         ankle_l=[0.0, 0.10, 0.08], foot_l=[1, 0, 0], knee_l=[1, 0, 0],
                         ankle_r=[0.70, -0.10, 0.52], foot_r=[0, 0, 1], knee_r=[1, 0, -0.3],
                         wrist_l_rel=[0.40, 0.10, -0.35], wrist_r_rel=[0.40, -0.10, -0.35],
                         elbow_l=[0, 0.5, -1], elbow_r=[0, -0.5, -1])
    a = key(-0.06, 0.965, 0)
    b = key(-0.03, 0.965, 55)
    return dict(seconds=4.0, keys=rep(a, b), cam_az=35, cam_el=10, props=[chair])


# --- 2. kneeling lunge, right knee on the floor, right shin up a wall behind ---------------------------------
def quad_couch():
    wall = dict(kind="box", center=[-0.40, 0.0, 1.2], size=[0.05, 1.6, 2.4])

    def key(px, pz, p, rear_ankle_z):
        return base_pose(pelvis=[px, 0.0, pz], p1=p, p2=p, hp=0,
                         ankle_r=[-0.36, -0.10, rear_ankle_z], foot_r=[0, 0, 1], knee_r=[1, 0, 0],
                         ankle_l=[0.45, 0.10, 0.08], foot_l=[1, 0, 0], knee_l=[1, 0, 0],
                         wrist_l_rel=[0.18, 0.08, -0.50], wrist_r_rel=[0.18, -0.08, -0.50],
                         hand_l=[0, 0, -1], hand_r=[0, 0, -1], elbow_l=[-1, 0, 0], elbow_r=[-1, 0, 0])
    a = key(-0.06, 0.50, 0, 0.44)
    b = key(0.02, 0.52, -4, 0.44)
    return dict(seconds=4.0, keys=rep(a, b), cam_az=35, cam_el=12, props=[wall])


# --- 3. standing, right heel pulled to the seat, hand holds the right ankle ---------------------------------
def quad_standing():
    def key(px, pz, p, ankle_z, ankle_x, wrist_z, wrist_x):
        return base_pose(pelvis=[px, 0.0, pz], p1=p, p2=p, hp=0,
                         ankle_l=[0.0, 0.10, 0.08], foot_l=[1, 0, 0], knee_l=[1, 0, 0],
                         ankle_r=[ankle_x, -0.10, ankle_z], foot_r=[-1, 0, 0.3], knee_r=[1, 0, -0.3],
                         wrist_r=[wrist_x, -0.12, wrist_z], hand_r=[0, 0, 1],
                         wrist_l_rel=[0.02, 0.04, -0.565], elbow_r=[1, -0.3, 0])
    a = key(-0.02, 0.975, 5, 0.92, -0.26, 0.94, -0.24)
    b = key(-0.02, 0.975, 5, 0.94, -0.32, 0.98, -0.30)
    return dict(seconds=4.0, keys=rep(a, b), cam_az=60, cam_el=10, props=[])


# --- 4. standing, towel behind the back (right upper hand, left hand low) ----------------------------------
def rear_hand_clasp():
    towel = dict(kind="rod", a=pt("wrist_r"), b=pt("wrist_l"), r=0.02)

    def key(wr, wl):
        return base_pose(pelvis=[0.0, 0.0, 0.975], p1=0, p2=0, hp=0,
                         wrist_r=wr, elbow_r=[0, 0, 1], hand_r=[0, 0, -1], palm_r=[-1, 0, 0],
                         wrist_l=wl, elbow_l=[-0.3, 1, -0.2], hand_l=[0, 0, 1], palm_l=[-1, 0, 0])
    # The right elbow bends high over the shoulder; the wrist descends to the upper back.
    # The left hand stays low on the opposite end of the towel.
    a = key([-0.25, -0.14, 1.30], [-0.15, 0.14, 0.90])
    b = key([-0.25, -0.14, 1.34], [-0.15, 0.14, 0.94])
    # Rear three-quarter view makes the towel and both hands visible behind the torso.
    return dict(seconds=4.0, keys=rep(a, b), cam_az=-45, cam_el=12, props=[towel])


# --- 5. standing, right arm across the chest, left hand holds the right upper arm --------------------------
def shoulder_cross():
    def key(wr, wl):
        return base_pose(pelvis=[0.0, 0.0, 0.975], p1=0, p2=0, hp=0,
                         wrist_r=wr, elbow_r=[1, 0, -1], hand_r=[0, 1, 0], palm_r=[0, 0, -1],
                         wrist_l=wl, elbow_l=[1, 0, -1], hand_l=[0, -1, 0], palm_l=[0, 0, -1])
    a = key([0.02, 0.28, 1.34], [0.08, -0.02, 1.33])
    b = key([0.02, 0.34, 1.38], [0.10, 0.00, 1.36])
    return dict(seconds=4.0, keys=rep(a, b), cam_az=60, cam_el=8, props=[])


# --- 6. standing, both arms straight up, chest opens with a slight backbend ----------------------------------
def shoulder_backbend():
    def key(p1, p2, pz, wr_x):
        return base_pose(pelvis=[0.0, 0.0, pz], p1=p1, p2=p2, hp=0,
                         wrist_l_rel=[wr_x, 0.07, 0.56], wrist_r_rel=[wr_x, -0.07, 0.56],
                         hand_l=[0, 0, 1], hand_r=[0, 0, 1], palm_l=[0, -1, 0], palm_r=[0, 1, 0],
                         elbow_l=[0, 1, 0], elbow_r=[0, -1, 0], grip_l=0.0, grip_r=0.0)
    a = key(0, 0, 0.975, -0.03)
    b = key(-8, -22, 0.98, -0.10)
    return dict(seconds=4.0, keys=rep(a, b), cam_az=60, cam_el=8, props=[])


# --- 7. side-lying on the RIGHT side (right shoulder under the body), left arm presses the right forearm -----
def sleeper():
    def key(wl, wr_hand_dir, wrist_r, elbow_pole):
        return base_pose(pelvis=[0.0, 0.0, 0.26], p1=90, p2=90, roll=90, hp=0,
                         ankle_r=[-0.55, 0.0, 0.08], foot_r=[-1, 0, 0], knee_r=[0, 1, 0],
                         ankle_l=[-0.60, 0.22, 0.08], foot_l=[-1, 0, 0], knee_l=[1, 0.3, 1],
                         wrist_r=wrist_r, elbow_r=elbow_pole, hand_r=wr_hand_dir, palm_r=[1, 0, 0],
                         wrist_l=wl, elbow_l=[0, 0.5, 1], hand_l=[0, 1, -0.5], palm_l=[0, 0, -1])
    a = key([0.47, 0.30, 0.25], [0, 0, 1], [0.47, 0.30, 0.32], [0, 1, 0.6])
    b = key([0.47, 0.40, 0.14], [0, 1, -0.2], [0.47, 0.44, 0.20], [0, 1, 0.6])
    return dict(seconds=4.0, keys=rep(a, b), cam_az=20, cam_el=25, props=[])


# --- 8. lying face up, arms out, right knee falls across the body to the left -------------------------------
def supine_twist():
    arms = dict(wrist_l=[-0.47, 0.73, FLOOR_WRIST_Z], wrist_r=[-0.47, -0.73, FLOOR_WRIST_Z],
                hand_l=[0, 1, 0], hand_r=[0, -1, 0], palm_l=[0, 0, -1], palm_r=[0, 0, -1],
                elbow_l=[0, 0, 1], elbow_r=[0, 0, 1], grip_l=0.0, grip_r=0.0)

    def key(ankle_r, knee_r):
        return base_pose(pelvis=[0.0, 0.0, 0.11], p1=-90, p2=-90, hp=0,
                         ankle_l=[0.90, 0.10, 0.08], foot_l=[1, 0, 0], knee_l=[0, 0, 1],
                         ankle_r=ankle_r, foot_r=[1, 0, 0], knee_r=knee_r, **arms)
    a = key([0.90, -0.10, 0.08], [0, 0, 1])
    b = key([-0.40, 0.34, 0.08], [1, 1, 0])
    return dict(seconds=4.0, keys=rep(a, b), cam_az=20, cam_el=25, props=[])


# --- 9. all fours, thread the right arm under and then open toward the ceiling -------------------------------
def thoracic_rotation():
    # Hand-on-occiput quadruped rotation, matching the app's elbow-led cue.
    def key(t):
        q=(1-math.cos(2*math.pi*t))/2
        phi=25-55*q
        pose=_fours(0.0,ankle_x=-.46,pz=.50,wrist_l=[.47,.14,.04],elbow_l=[0,0,1],
                    elbow_r=[0,-.6,-1+2*q],grip_r=.05,
                    thoracic_twist=phi,head_turn=-.3*phi,head_detail=1)
        joints=solve(pose,{})
        up=[(joints['head'][i]-joints['neck'][i])/.099 for i in range(3)]
        left=joints['head_left']
        forward=[left[1]*up[2]-left[2]*up[1],left[2]*up[0]-left[0]*up[2],left[0]*up[1]-left[1]*up[0]]
        pose['hand_r']=up;pose['palm_r']=forward
        # Head center includes a 2 cm renderer offset; palm outer surface
        # (4 mm center offset + 16 mm thickness) meets the occiput exactly.
        pose['wrist_r']=[joints['head'][i]-.02675*up[i]-.118*forward[i] for i in range(3)]
        return pose
    return dict(seconds=4,keys=[(i/128,key(i/128)) for i in range(129)],cam_az=50,cam_el=22,props=[])


# --- 10. standing, right palm flat on a wall beside the body, fingers back; the body turns away -------------
def wrist_biceps():
    wall = dict(kind="box", center=[0.0, -0.77, 1.2], size=[1.6, 0.05, 2.4])

    def key(yaw):
        return base_pose(pelvis=[0.0, 0.0, 0.975], p1=0, p2=0, hp=0, yaw=yaw,
                         wrist_r=[0.0, -0.745, 1.42], hand_r=[-1, 0, 0], palm_r=[0, -1, 0], grip_r=0.0,
                         elbow_r=[1, 0, 0], wrist_l_rel=[0.02, 0.04, -0.565])
    a = key(0)
    b = key(15)
    return dict(seconds=4.0, keys=rep(a, b), cam_az=75, cam_el=10, props=[wall])


# --- 11. all fours, backs of the hands on the floor, fingers toward the knees; hips rock back ----------------
def wrist_extensor():
    a = _fours(0.0, ankle_x=-0.46, palm=(0, 0, 1))
    b = _fours(-0.14, ankle_x=-0.60, palm=(0, 0, 1))
    return dict(seconds=4.0, keys=rep(a, b), cam_az=35, cam_el=20, props=[])


# --- 12. all fours, palms down on the floor, fingers toward the knees; hips rock back a little --------------
def wrist_flexor():
    a = _fours(0.0, ankle_x=-0.46, palm=(0, 0, -1))
    b = _fours(-0.08, ankle_x=-0.54, palm=(0, 0, -1))
    return dict(seconds=4.0, keys=rep(a, b), cam_az=35, cam_el=20, props=[])


EXERCISES = {
    "stretch-pike-one-leg": pike_one_leg,
    "stretch-quad-couch": quad_couch,
    "stretch-quad-standing": quad_standing,
    "stretch-rear-clasp": rear_hand_clasp,
    "stretch-shoulder": shoulder_cross,
    "stretch-shoulder-backbend": shoulder_backbend,
    "stretch-sleeper": sleeper,
    "stretch-supine-twist": supine_twist,
    "stretch-thoracic": thoracic_rotation,
    "stretch-wrist-biceps": wrist_biceps,
    "stretch-wrist-extensor": wrist_extensor,
    "stretch-wrist-flexor": wrist_flexor,
}
