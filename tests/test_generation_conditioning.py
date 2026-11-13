"""Burpee-pushup generation checks and independent negative controls."""
import copy
import gzip
import json
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools/blender"))
import poses
from motion_quality import digest
from families.conditioning import EXERCISES
from generation_checks.conditioning import audit, _plank_indices


class GenerationConditioningTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        poses.load_families()
        cls.data, cls.reach = poses.build("burpee-pushup")

    def failed_checks(self, data):
        return {failure["check"] for failure in audit(data)["failures"]}

    def test_complete_burpee_pushup_passes_reach_floor_and_scoped_audit(self):
        self.assertEqual([], self.reach)
        self.assertEqual([], poses.checks(self.data))
        report = audit(self.data)
        self.assertEqual([], report["failures"])
        self.assertTrue(all(m["ok"] for m in report["metrics"].values()))
        self.assertGreater(report["metrics"]["pushup_bottom_elbow_bend_deg"]["value"], 30)
        self.assertGreater(report["metrics"]["pushup_top_elbow_deg"]["value"], 145)

    def test_frozen_actual_old_pose_has_pushup_and_landing_but_lacks_load_and_timing(self):
        fixture = ROOT / "tests/fixtures/generation-conditioning-burpee-pushup-before.json.gz"
        baseline = ROOT / "work/video-generation-2026-10-21/baseline/burpee-pushup.json"
        with gzip.open(fixture, "rt") as f:
            old = json.load(f)
        self.assertEqual(old, json.loads(baseline.read_text()))
        failures = self.failed_checks(old)
        self.assertTrue({"duration_s", "max_joint_speed_m_s", "jump_preload_depth_m"} <= failures)
        # The screening hypothesis that the actual source lacked a complete
        # high-low-high push-up or any landing is contradicted by this pose data.
        self.assertNotIn("pushup_phase_order", failures)
        self.assertNotIn("landing_contact", failures)

    def test_renderer_equivalent_foot_capsule_floor_check_catches_old_and_mutation(self):
        fixture = ROOT / "tests/fixtures/generation-conditioning-burpee-pushup-before.json.gz"
        with gzip.open(fixture, "rt") as f:
            old = json.load(f)
        old_report = audit(old)
        self.assertIn("foot_mesh_floor_min_z_m", self.failed_checks(old))
        self.assertAlmostEqual(old_report["metrics"]["foot_mesh_floor_min_z_m"]["value"], -.038453, places=5)

        candidate = audit(self.data)
        self.assertTrue(candidate["metrics"]["foot_mesh_floor_min_z_m"]["ok"])
        bad = copy.deepcopy(self.data)
        # Move a planted toe downward without changing the body cycle; the
        # renderer-equivalent capsule bound must see the resulting floor hit.
        plank = _plank_indices(bad["frames"])
        bad["frames"][plank[len(plank)//2]]["toe_l"][2] -= .05
        self.assertIn("foot_mesh_floor_min_z_m", self.failed_checks(bad))

    def test_missing_press_phase_is_rejected(self):
        bad = copy.deepcopy(self.data)
        plank = _plank_indices(bad["frames"])
        low = min(plank, key=lambda i: bad["frames"][i]["chest"][2])
        low_pose = copy.deepcopy(bad["frames"][low])
        for i in plank:
            if i > low:
                bad["frames"][i] = copy.deepcopy(low_pose)
        self.assertIn("pushup_press_rise_m", self.failed_checks(bad))
        self.assertIn("pushup_phase_order", self.failed_checks(bad))

    def test_slipping_planted_wrist_is_rejected(self):
        bad = copy.deepcopy(self.data)
        plank = _plank_indices(bad["frames"])
        for i in plank[len(plank)//2:]:
            bad["frames"][i]["wrist_l"][0] += .02
        self.assertIn("plank_contact_drift_m", self.failed_checks(bad))

    def test_wrong_variant_is_rejected(self):
        bad = copy.deepcopy(self.data)
        bad["id"] = "burpee"
        self.assertIn("exercise_id", self.failed_checks(bad))

    def test_single_frame_discontinuity_is_rejected(self):
        bad = copy.deepcopy(self.data)
        i = len(bad["frames"]) // 2
        bad["frames"][i]["chest"][0] += .5
        self.assertIn("max_joint_speed_m_s", self.failed_checks(bad))

    def test_nonfinite_or_missing_geometry_fails_explicitly(self):
        bad = copy.deepcopy(self.data)
        bad["frames"][20]["pelvis"][0] = float("nan")
        self.assertIn("finite_geometry", self.failed_checks(bad))
        bad = copy.deepcopy(self.data)
        del bad["frames"][20]["toe_r"]
        self.assertIn("finite_geometry", self.failed_checks(bad))

    def test_all_conditioning_siblings_keep_the_frozen_pose_hashes(self):
        baseline = json.loads((ROOT / "work/video-generation-2026-10-21/baseline/pose-hashes.json").read_text())
        for exercise_id in sorted(set(EXERCISES) - {"burpee-pushup"}):
            self.assertIn(exercise_id, baseline)
            data, reach = poses.build(exercise_id)
            self.assertEqual([], reach, exercise_id)
            self.assertEqual(baseline[exercise_id], digest(data), exercise_id)


if __name__ == "__main__":
    unittest.main()
