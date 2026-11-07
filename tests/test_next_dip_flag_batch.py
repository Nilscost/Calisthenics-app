"""Source-level regression screens for the Russian dip and human-flag kick press."""
import math
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools/blender"))
import poses
from motion_quality import angle
from generation_checks.next_dip_flag import audit as dip_flag_audit


def _point_to_bar(point, side):
    """Distance from a joint center to its finite parallel bar centerline."""
    x = max(-.25, min(.25, point[0]))
    y = .25 if side == "l" else -.25
    return math.sqrt((point[0]-x)**2 + (point[1]-y)**2 + (point[2]-1.0)**2)


class NextDipFlagBatchTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        poses.load_families()
        cls.dip_spec = poses.EXERCISES["dip-russian"]()
        cls.flag_spec = poses.EXERCISES["flag-kick-press"]()
        cls.dip, cls.dip_warnings = poses.build("dip-russian")
        cls.flag, cls.flag_warnings = poses.build("flag-kick-press")

    def test_dip_russian_has_smooth_articulated_bar_supported_bottom(self):
        self.assertEqual("continuous", self.dip_spec.get("interpolation"))
        self.assertEqual([], self.dip_warnings)
        dip_report = dip_flag_audit(self.dip)
        self.assertEqual([], dip_report["failures"])
        self.assertTrue(dip_report["requires_visual_review"])
        frames = self.dip["frames"]
        # The forearm-rest interval is the low pelvis plateau. Both elbows
        # remain close to their actual bar axis, while the wrists stay seated.
        supported = []
        for i, frame in enumerate(frames):
            if .645 <= frame["pelvis"][2] <= .655:
                supported.append(i)
                for side in ("l", "r"):
                    # The renderer's bar-axis centerline is an authored support
                    # proxy; allow a 1 mm margin for the dense interpolated keys.
                    self.assertLessEqual(_point_to_bar(frame["elbow_"+side], side), .067)
                    self.assertLessEqual(_point_to_bar(frame["wrist_"+side], side), .045)
                    self.assertGreater(angle(frame["shoulder_"+side], frame["elbow_"+side],
                                             frame["wrist_"+side]), 125)
        self.assertGreaterEqual(len(supported), 6)
        self.assertGreater(frames[0]["pelvis"][2], .9)
        self.assertLess(min(f["pelvis"][2] for f in frames), .67)
        self.assertLess(frames[-3]["pelvis"][2], frames[0]["pelvis"][2] + .02)

    def test_flag_kick_is_continuous_unheld_and_lands_with_absorption(self):
        self.assertEqual("continuous", self.flag_spec.get("interpolation"))
        self.assertAlmostEqual(3.2, self.flag_spec["seconds"])
        self.assertEqual([], self.flag_warnings)
        flag_report = dip_flag_audit(self.flag)
        self.assertEqual([], flag_report["failures"])
        self.assertTrue(flag_report["requires_visual_review"])
        frames = self.flag["frames"]
        peak = max(range(len(frames)), key=lambda i: frames[i]["pelvis"][2])
        peak_z = frames[peak]["pelvis"][2]
        self.assertGreater(frames[peak]["hip_l"][2], 1.65)
        self.assertGreater(max(f["ankle_l"][2] for f in frames), 1.6)
        # There is a kick-through peak, not an authored held position.
        apex = [i for i, f in enumerate(frames) if f["pelvis"][2] >= peak_z-.005]
        self.assertLessEqual(len(apex), 2)
        # The return has bilateral floor contact and visible knee absorption.
        landing_index = round(self.flag_spec["seconds"] * .78 * self.flag["fps"])
        landing_window = frames[landing_index-1:landing_index+4]
        landed = [f for f in landing_window if .85 <= f["pelvis"][2] <= 1.05 and
                  f["ankle_l"][2] <= .09 and f["ankle_r"][2] <= .09]
        self.assertGreaterEqual(len(landed), 3)
        self.assertTrue(all(angle(f["hip_l"], f["knee_l"], f["ankle_l"]) < 165
                            for f in landed))
        self.assertLess(max(f["ankle_l"][2] for f in landed), .09)

    def test_scoped_audit_rejects_speed_spike_and_open_loop_seam(self):
        bad = {**self.dip, "frames": [dict(frame) for frame in self.dip["frames"]]}
        bad["frames"][30]["pelvis"][0] += .5
        names = {failure["check"] for failure in dip_flag_audit(bad)["failures"]}
        self.assertIn("max_joint_speed_m_s", names)
        bad = {**self.flag, "frames": [dict(frame) for frame in self.flag["frames"]]}
        bad["frames"][-1]["chest"][0] += .1
        names = {failure["check"] for failure in dip_flag_audit(bad)["failures"]}
        self.assertIn("loop_seam_max_m", names)

    def test_sibling_exercise_definitions_are_still_registered(self):
        for ex_id in ("dip-korean", "dip-l-sit", "flag-press", "flag-clutch"):
            with self.subTest(ex_id=ex_id):
                self.assertIn(ex_id, poses.EXERCISES)
                data, warnings = poses.build(ex_id)
                self.assertGreaterEqual(len(data["frames"]), 4)
                self.assertEqual([], warnings)


if __name__ == "__main__":
    unittest.main()
