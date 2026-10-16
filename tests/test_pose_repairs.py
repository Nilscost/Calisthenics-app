"""Semantic pose checks for the held R8 demonstrations."""

import math
import pathlib
import sys
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools" / "blender"))

import poses  # noqa: E402


HELD_REPAIR_IDS = {
    "stretch-thoracic", "short-foot", "rower-situp", "scapular-pushup-wall",
    "scapular-pushup-knee", "scapular-pushup", "support-scapular-shrug",
    "wrist-palm-pulses", "chin-nod-supine", "chin-tuck-seated",
    "chin-tuck-head-lift", "neck-head-raise-manual", "turn-and-lunge",
    "stretch-neck-side", "stretch-twist-straight-leg", "stretch-twist-easy",
    "stretch-twist-half", "stretch-twist-three-quarter", "stretch-twist-full",
    "pilates-roll-up", "pilates-saw", "pilates-roll-over", "pilates-seal",
    "pilates-spine-twist", "pilates-double-leg-kick",
}


def _sub(a, b):
    return [a[i] - b[i] for i in range(3)]


def _dot(a, b):
    return sum(a[i] * b[i] for i in range(3))


def _cross(a, b):
    return [a[1] * b[2] - a[2] * b[1],
            a[2] * b[0] - a[0] * b[2],
            a[0] * b[1] - a[1] * b[0]]


def _unit(v):
    n = math.sqrt(_dot(v, v))
    return [x / n for x in v]


def _elbow_angle(shoulder, elbow, wrist):
    upper = _sub(shoulder, elbow)
    forearm = _sub(wrist, elbow)
    return math.degrees(math.acos(max(-1.0, min(1.0,
        _dot(upper, forearm) / math.sqrt(_dot(upper, upper) * _dot(forearm, forearm))))))


def _palm_axes(frame, side):
    wrist = frame["wrist_" + side]
    u = _unit(_sub(frame["hand_" + side], wrist))
    n0 = frame["palmn_" + side]
    n = _unit([n0[i] - u[i] * _dot(n0, u) for i in range(3)])
    s = _unit(_cross(u, n))
    inward = [0, -1 if side == "l" else 1, 0]
    if _dot(s, inward) < 0:
        s = [-x for x in s]
    return wrist, u, n, s


def _palm_center(frame, side):
    wrist, u, n, _ = _palm_axes(frame, side)
    return [wrist[i] + u[i] * 0.04675 + n[i] * 0.004 for i in range(3)]


class PoseRepairTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        poses.load_families()

    def build(self, exercise_id):
        data, reach = poses.build(exercise_id)
        self.assertEqual([], reach, f"{exercise_id}: unreachable targets")
        self.assertEqual([], poses.checks(data), f"{exercise_id}: floor penetration")
        return data

    def test_all_25_held_repair_poses_build_without_reach_or_floor_errors(self):
        self.assertTrue(HELD_REPAIR_IDS.issubset(poses.EXERCISES))
        self.assertEqual(25, len(HELD_REPAIR_IDS))
        for exercise_id in sorted(HELD_REPAIR_IDS):
            with self.subTest(exercise_id=exercise_id):
                self.build(exercise_id)

    def test_rower_pelvis_ellipsoid_stays_supported_through_the_loop(self):
        data = self.build("rower-situp")
        bottoms = []
        for frame in data["frames"]:
            up = _unit(frame["pelvis_up"])
            left = _unit(frame["pelvis_left"])
            x_axis = _unit(_cross(left, up))
            center = [frame["pelvis"][i] + 0.03 * up[i] for i in range(3)]
            vertical_radius = math.sqrt(
                (0.105 * x_axis[2]) ** 2
                + (0.165 * left[2]) ** 2
                + (0.11 * up[2]) ** 2
            )
            bottoms.append(center[2] - vertical_radius)

        # The rendered ellipsoid should skim the floor throughout the loop,
        # with only the authored 3 mm clearance rather than visibly floating.
        self.assertLessEqual(max(abs(z - 0.003) for z in bottoms), 0.0001)

    def test_saw_reaches_across_to_the_opposite_toe_with_a_horizontal_rear_arm(self):
        data = self.build("pilates-saw")
        frames = data["frames"]
        # These are the held-turn peaks in the two alternating half-cycles.
        for index, reaching, opposite, rear in ((48, "r", "l", "l"),
                                                 (144, "l", "r", "r")):
            with self.subTest(peak_frame=index):
                frame = frames[index]
                toe = frame["toe_" + opposite]

                # The hand tip, rather than the wrist joint, reaches the
                # little-toe edge on the far side of the opposite foot.
                hand = frame["hand_" + reaching]
                self.assertLess(math.dist(hand, toe), 0.06)
                self.assertGreater(hand[1] * toe[1], 0.0)
                self.assertGreater(abs(hand[1]), abs(toe[1]))

                # The non-reaching arm remains behind the shoulder at shoulder
                # height; it must not follow the forward-folded chest axis and
                # lift overhead.
                rear_wrist = frame["wrist_" + rear]
                rear_shoulder = frame["shoulder_" + rear]
                self.assertAlmostEqual(rear_wrist[2], rear_shoulder[2], delta=0.02)
                self.assertLess(rear_wrist[0], rear_shoulder[0] - 0.25)

                # Preserve the seated base and long legs during the turn.
                self.assertAlmostEqual(frame["pelvis"][2], 0.11, delta=0.005)
                for side in ("l", "r"):
                    self.assertAlmostEqual(frame["ankle_" + side][2], 0.08, delta=0.005)
                    angle = _elbow_angle(frame["hip_" + side], frame["knee_" + side],
                                         frame["ankle_" + side])
                    self.assertGreaterEqual(angle, 170.0)

    def test_wall_scapular_pushup_keeps_straight_arms_and_palms_on_wall(self):
        data = self.build("scapular-pushup-wall")
        wall = next(prop for prop in data["props"] if prop["kind"] == "box")
        wall_face = wall["center"][0] - wall["size"][0] / 2
        wrist_tracks = {side: [] for side in ("l", "r")}
        minimum_angles = {side: 180.0 for side in ("l", "r")}
        maximum_contact_error = 0.0

        for frame in data["frames"]:
            for side in ("l", "r"):
                wrist_tracks[side].append(tuple(frame["wrist_" + side]))
                angle = _elbow_angle(frame["shoulder_" + side], frame["elbow_" + side],
                                     frame["wrist_" + side])
                minimum_angles[side] = min(minimum_angles[side], angle)
                wrist, u, n, s = _palm_axes(frame, side)
                center = _palm_center(frame, side)
                radius_x = math.sqrt((0.042 * s[0]) ** 2 + (0.016 * n[0]) ** 2 + (0.052 * u[0]) ** 2)
                outer_face = center[0] + radius_x
                maximum_contact_error = max(maximum_contact_error, abs(outer_face - wall_face))

        for side in ("l", "r"):
            self.assertEqual(1, len(set(wrist_tracks[side])), f"{side} wrist moved off wall contact")
            self.assertGreaterEqual(minimum_angles[side], 170.0, f"{side} arm bent during scapular glide")
        self.assertLess(maximum_contact_error, 0.0001)

    def test_thoracic_hand_tracks_rendered_occiput_while_elbow_opens(self):
        data = self.build("stretch-thoracic")
        frames = data["frames"]
        for name in ("pelvis", "knee_l", "knee_r", "wrist_l"):
            self.assertTrue(all(frame[name] == frames[0][name] for frame in frames),
                            f"support point {name} moved")

        elbow_heights = [frame["elbow_r"][2] for frame in frames]
        self.assertGreater(max(elbow_heights) - min(elbow_heights), 0.30)
        self.assertGreater(elbow_heights[len(frames) // 2], elbow_heights[0] + 0.30)

        # Match the palm's inward surface to the actual rendered head ellipsoid:
        # render.py offsets its center 2 cm along head-up and uses a 9.8 cm
        # radius along face-forward; the palm thickness is 1.6 cm.
        for frame in frames[::4]:
            head_up = _unit(_sub(frame["head"], frame["neck"]))
            head_left = _unit(frame["head_left"])
            forward = _unit(_cross(head_left, head_up))
            head_center = [frame["head"][i] + 0.02 * head_up[i] for i in range(3)]
            occiput = [head_center[i] - 0.098 * forward[i] for i in range(3)]
            center = _palm_center(frame, "r")
            _, _, normal, _ = _palm_axes(frame, "r")
            palm_inner_surface = [center[i] + 0.016 * normal[i] for i in range(3)]
            gap = math.sqrt(_dot(_sub(occiput, palm_inner_surface),
                                 _sub(occiput, palm_inner_surface)))
            self.assertLess(gap, 0.003, f"palm lost occiput contact at sampled frame: {gap:.4f} m")

    def test_turn_and_lunge_uses_long_split_stance_forward_rear_knee_and_floor_hand(self):
        data = self.build("turn-and-lunge")
        checked_sides = set()
        reached_frames = 0
        for frame in data["frames"]:
            ankles = {side: frame["ankle_" + side] for side in ("l", "r")}
            stance = math.dist(ankles["l"], ankles["r"])
            if stance < 1.0:
                continue
            # The reaching wrist is the one nearest the floor; the other stays
            # above the floor while the trunk hinges over the split stance.
            reach_side = min(("l", "r"), key=lambda side: frame["wrist_" + side][2])
            wrist = frame["wrist_" + reach_side]
            if wrist[2] > 0.04:
                continue
            reached_frames += 1
            # The opposite hand reaches down between the front and rear feet,
            # so the reaching-hand side is the rear-leg side in this pattern.
            rear_side = reach_side
            lead_side = "r" if rear_side == "l" else "l"
            rear_ankle = ankles[rear_side]
            lead_ankle = ankles[lead_side]
            axis = _unit([lead_ankle[i] - rear_ankle[i] for i in (0, 1)] + [0])
            stance_length = _dot(_sub(lead_ankle, rear_ankle), axis)
            hand_progress = _dot(_sub(wrist, rear_ankle), axis)
            self.assertGreaterEqual(stance_length, 1.0)
            self.assertGreater(hand_progress, 0.15)
            self.assertLess(hand_progress, stance_length - 0.05)

            # A rear knee must move toward the lead foot from its ankle, rather
            # than fold backward behind the rear foot.
            rear_knee = frame["knee_" + rear_side]
            rear_knee_progress = _dot(_sub(rear_knee, rear_ankle), axis)
            self.assertGreater(rear_knee_progress, 0.25)
            checked_sides.add(lead_side)

            # Verify the rendered palm's lowest point stays close to the floor.
            center = _palm_center(frame, reach_side)
            _, u, normal, across = _palm_axes(frame, reach_side)
            palm_bottom = center[2] - math.sqrt(
                (0.042 * across[2]) ** 2 + (0.016 * normal[2]) ** 2 + (0.052 * u[2]) ** 2
            )
            self.assertGreaterEqual(palm_bottom, -0.002)
            self.assertLessEqual(palm_bottom, 0.020)

        self.assertGreater(reached_frames, 5, "lunge never holds the floor reach in its full stance")
        self.assertEqual({"l", "r"}, checked_sides, "the alternating lunge must lead with both legs")

    def test_seated_twists_cross_the_leg_and_anchor_the_authored_bind(self):
        variants = {
            "stretch-twist-straight-leg": "straight",
            "stretch-twist-easy": "easy",
            "stretch-twist-half": "half",
            "stretch-twist-three-quarter": "threequarter",
            "stretch-twist-full": "full",
        }
        for exercise_id, variant in variants.items():
            with self.subTest(exercise_id=exercise_id):
                data = self.build(exercise_id)
                frames = data["frames"]
                peak = frames[len(frames) // 2]
                # The right foot must visibly pass the left knee/thigh; the
                # original poses left both feet on their own sides.
                self.assertGreater(peak["ankle_r"][1], peak["knee_l"][1] + 0.08)
                self.assertGreater(peak["knee_r"][2], 0.30)
                self.assertAlmostEqual(peak["ankle_r"][2], 0.08, places=3)

                # The pelvis stays rooted while the chest turns toward the
                # crossed right knee (negative thoracic twist in this rig).
                self.assertTrue(all(frame["pelvis"] == frames[0]["pelvis"] for frame in frames))
                pelvis_left = peak["pelvis_left"]
                chest_left = peak["chest_left"]
                signed_turn = chest_left[0] * pelvis_left[1] - chest_left[1] * pelvis_left[0]
                self.assertGreater(signed_turn, 0.45)

                elbow_knee = math.dist(peak["elbow_l"], peak["knee_r"])
                if variant in ("straight", "easy"):
                    self.assertLess(elbow_knee, 0.06,
                                    "opposite elbow must brace the outside of the bent knee")
                elif variant == "half":
                    # Hand lies on the outside calf, near the ankle/instep.
                    calf = _sub(peak["ankle_r"], peak["knee_r"])
                    hand = _sub(peak["wrist_l"], peak["knee_r"])
                    ratio = _dot(hand, calf) / _dot(calf, calf)
                    projection = [peak["knee_r"][i] + ratio * calf[i] for i in range(3)]
                    self.assertGreater(ratio, 0.65)
                    self.assertLess(math.dist(peak["wrist_l"], projection), 0.04)
                elif variant == "threequarter":
                    self.assertTrue(any(prop["kind"] == "rod" for prop in data["props"]))
                    self.assertLess(elbow_knee, 0.06,
                                    "towel bind must retain the front-arm wrap around the knee")
                    self.assertLess(math.dist(peak["wrist_l"], peak["wrist_r"]), 0.41)
                else:
                    self.assertLess(elbow_knee, 0.06,
                                    "full bind must wrap the front arm around the knee")
                    self.assertLess(peak["wrist_l"][0], peak["pelvis"][0])
                    self.assertLess(peak["wrist_r"][0], peak["pelvis"][0])
                    self.assertLess(math.dist(peak["wrist_l"], peak["wrist_r"]), 0.18)
                if variant != "straight":
                    # The folded support leg rests at the mat, rather than
                    # leaving both knees floating in the seated pose.
                    self.assertAlmostEqual(peak["knee_l"][2], 0.05, delta=0.01)
                    self.assertGreater(peak["knee_l"][1], 0.10,
                                       "folded left knee must stay on the figure-left side")

    def test_wrist_palm_pulses_lifts_each_finger_independently_with_palm_planted(self):
        data = self.build("wrist-palm-pulses")
        frames = data["frames"]
        for side in ("l", "r"):
            for finger in range(5):
                candidates = [frame for frame in frames
                              if frame["finger_lift_" + side][finger] >= 10.0
                              and all(frame["finger_lift_" + side][i] <= 0.5
                                      for i in range(5) if i != finger)]
                self.assertTrue(candidates, f"{side} finger {finger} never lifts independently by 10 degrees")
                for frame in candidates:
                    center = _palm_center(frame, side)
                    wrist, u, normal, across = _palm_axes(frame, side)
                    lower_surface = center[2] - math.sqrt(
                        (0.042 * across[2]) ** 2 + (0.016 * normal[2]) ** 2 + (0.052 * u[2]) ** 2
                    )
                    self.assertGreaterEqual(lower_surface, -0.002)
                    self.assertLessEqual(lower_surface, 0.020)
                    self.assertLessEqual(abs(wrist[2] - 0.035), 0.001)


if __name__ == "__main__":
    unittest.main()
