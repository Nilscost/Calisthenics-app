"""Pistol regressions; depth phase/physics regressions live in test_depth_jump_physics."""
import copy
import gzip
import json
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools/blender"))
import poses
from generation_checks.lower import audit
from pistol_refinement import specification


class LowerGenerationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        poses.load_families()
        # Root owns poses.py integration. Exercise the proposed function only
        # within this test process until that integration lands.
        poses.EXERCISES["squat-pistol"] = specification
        cls.data = {ex: poses.build(ex)[0] for ex in ("squat-pistol",)}

    def test_candidates_pass_build_reach_floor_and_phase_audits(self):
        for ex, data in self.data.items():
            with self.subTest(exercise=ex):
                self.assertEqual([], poses.build(ex)[1])
                self.assertEqual([], poses.checks(data))
                report = audit(data)
                self.assertEqual([], report["failures"], report["metrics"])
                self.assertLessEqual(report["metrics"]["loop_seam_joint_gap_m"]["value"], .012)
        self.assertLessEqual(audit(self.data["squat-pistol"])["metrics"]["max_joint_speed_m_s"]["value"], 2.5)
        self.assertLessEqual(audit(self.data["squat-pistol"])["metrics"]["support_ankle_drift_m"]["value"], .003)

    def test_contract_uses_exercises_schema_and_strict_finite_bounds(self):
        path = ROOT / "tools/blender/motion_contracts.json"
        def reject_constant(value):
            raise ValueError("nonstandard JSON constant: " + value)
        contract = json.loads(path.read_text(), parse_constant=reject_constant)
        self.assertTrue({"depth-jump", "squat-pistol"}.issubset(contract["exercises"]))
        self.assertEqual(2.5, contract["exercises"]["squat-pistol"]["bounds"]["max_joint_speed_max_m_s"])
        self.assertEqual(.003, contract["exercises"]["squat-pistol"]["bounds"]["support_ankle_drift_max_m"])


    def test_actual_old_pistol_remains_a_valid_deep_cycle_negative_control(self):
        path = ROOT / "tests/fixtures/motion/squat-pistol-pre-generation.json.gz"
        with gzip.open(path, "rt", encoding="utf-8") as source:
            old = json.load(source)
        # Screening frames showed the old cycle rises again; do not label that
        # animation faulty merely because its source lacked a final keyed pose.
        self.assertEqual([], audit(old)["failures"])

    def test_wrong_variant_is_not_approved_by_pistol_bounds(self):
        bad = copy.deepcopy(self.data["squat-pistol"])
        bad["id"] = "squat-air"
        self.assertIn("known_variant", {item["check"] for item in audit(bad)["failures"]})

    def test_discontinuous_joint_is_rejected(self):
        bad = copy.deepcopy(self.data["squat-pistol"])
        bad["frames"][40]["pelvis"][0] += 1.0
        self.assertIn("max_joint_speed_m_s", {item["check"] for item in audit(bad)["failures"]})

    def test_pistol_support_slip_is_rejected_independently(self):
        bad = copy.deepcopy(self.data["squat-pistol"])
        for frame in bad["frames"][30:60]:
            frame["ankle_l"][0] += .06
        self.assertIn("support_ankle_drift_m", {item["check"] for item in audit(bad)["failures"]})

    def test_bent_forward_leg_is_rejected_independently(self):
        bad = copy.deepcopy(self.data["squat-pistol"])
        n = len(bad["frames"])
        for i, frame in enumerate(bad["frames"]):
            if .42 <= i / n < .60:
                frame["knee_r"][2] += .20
        self.assertIn("raised_leg_knee_angle_deg", {item["check"] for item in audit(bad)["failures"]})

    def test_pistol_missing_return_is_rejected(self):
        bad = copy.deepcopy(self.data["squat-pistol"])
        n = len(bad["frames"])
        for i, frame in enumerate(bad["frames"]):
            if i / n >= .55:
                frame["pelvis"][2] = .36
        self.assertIn("cycle_return_error_m", {item["check"] for item in audit(bad)["failures"]})

    def test_nonfinite_geometry_fails_explicitly(self):
        bad = copy.deepcopy(self.data["squat-pistol"])
        bad["frames"][8]["ankle_l"][0] = float("nan")
        self.assertIn("finite_geometry", {item["check"] for item in audit(bad)["failures"]})


if __name__ == "__main__":
    unittest.main()
