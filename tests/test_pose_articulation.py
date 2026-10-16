"""Contract tests for the optional head and upper-trunk pose controls."""

import pathlib
import sys
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools" / "blender"))

import poses  # noqa: E402


class PoseArticulationTests(unittest.TestCase):
    def setUp(self):
        self.base = poses.base_pose(pelvis=[0.2, -0.1, 0.9], p1=8, p2=0, yaw=7)

    @staticmethod
    def solve(k):
        return poses.solve(k, {})

    def test_thoracic_twist_keeps_pelvis_and_lower_trunk_and_rotates_upper_trunk(self):
        baseline = self.solve(dict(self.base))
        twisted = self.solve({**self.base, "thoracic_twist": 35})

        for name in ("pelvis", "spine", "pelvis_up", "pelvis_left"):
            self.assertEqual(baseline[name], twisted[name], name)
        self.assertEqual(baseline["chest"], twisted["chest"])
        self.assertNotEqual(baseline["chest_left"], twisted["chest_left"])
        self.assertNotEqual(baseline["shoulder_l"], twisted["shoulder_l"])
        self.assertNotEqual(baseline["shoulder_r"], twisted["shoulder_r"])

    def test_head_retraction_translates_head_without_changing_tilt_or_face_direction(self):
        baseline = self.solve({**self.base, "head_detail": 1})
        retracted = self.solve({**self.base, "head_detail": 1, "head_retract": 0.04})

        self.assertEqual(baseline["chest"], retracted["chest"])
        delta = [retracted["head"][i] - baseline["head"][i] for i in range(3)]
        back = poses.mul(poses.unit(poses.cross(baseline["chest_left"], baseline["chest_up"])), -1)
        self.assertAlmostEqual(0.04, poses.norm(delta), places=6)
        self.assertAlmostEqual(1.0, poses.dot(poses.unit(delta), back), places=6)
        self.assertEqual(baseline["head_left"], retracted["head_left"])
        self.assertEqual(baseline["head"][2] - baseline["neck"][2],
                         retracted["head"][2] - retracted["neck"][2])

    def test_head_turn_changes_face_orientation_without_moving_head(self):
        baseline = self.solve({**self.base, "head_detail": 1})
        turned = self.solve({**self.base, "head_detail": 1, "head_turn": 30})

        self.assertEqual(baseline["head"], turned["head"])
        self.assertEqual(baseline["neck"], turned["neck"])
        self.assertNotEqual(baseline["head_left"], turned["head_left"])
        self.assertAlmostEqual(1.0, poses.norm(turned["head_left"]), places=6)

    def test_positive_head_side_tilt_moves_head_toward_right_shoulder(self):
        upright = poses.base_pose(pelvis=[0, 0, 0.9], p1=0, p2=0, yaw=0)
        baseline = self.solve({**upright, "head_detail": 1})
        tilted = self.solve({**upright, "head_detail": 1, "head_side_tilt": 18})

        self.assertEqual(baseline["chest"], tilted["chest"])
        self.assertLess(tilted["head"][1], baseline["head"][1])
        self.assertNotEqual(baseline["head_left"], tilted["head_left"])

    def test_per_finger_values_are_preserved_as_lists(self):
        left = [0.1, 0.2, 0.3, 0.4, 0.5]
        right = [0.8, 0.7, 0.6, 0.5, 0.4]
        joints = self.solve({**self.base, "finger_lift_l": left, "finger_lift_r": right})

        self.assertEqual(left, joints["finger_lift_l"])
        self.assertEqual(right, joints["finger_lift_r"])
        self.assertIsNot(left, joints["finger_lift_l"])
        self.assertIsNot(right, joints["finger_lift_r"])

    def test_zero_or_absent_optional_controls_preserve_existing_geometry(self):
        baseline = self.solve(dict(self.base))
        explicit_zero = self.solve({
            **self.base,
            "thoracic_twist": 0,
            "head_retract": 0,
            "head_turn": 0,
            "head_side_tilt": 0,
            "head_detail": 1,
            "finger_lift_l": [0, 0, 0, 0, 0],
            "finger_lift_r": [0, 0, 0, 0, 0],
        })
        metadata = {"head_left", "head_detail", "finger_lift_l", "finger_lift_r"}

        self.assertEqual(set(baseline) - metadata, set(explicit_zero) - metadata)
        for name in set(baseline) - metadata:
            self.assertEqual(baseline[name], explicit_zero[name], name)


if __name__ == "__main__":
    unittest.main()
