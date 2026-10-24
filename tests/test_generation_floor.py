"""Negative controls and authored-variant checks for floor/core pilots."""
import copy
import gzip
import hashlib
import json
import math
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools/blender"))
import poses
from generation_checks.floor import audit


SIBLING_HASHES = {
    "hanging-windshield-wiper-half": "6b6182a50234738a938a48d3c141713104aa08117c1f1c2f424544251d0e224c",
    "hanging-leg-raise": "13112eb63581fe2e269bfaa16effd23b411ffca395f3551eab17f952ffb607bf",
    "pilates-seal": "a8d4dac3f037128d5b25ba0b426ec5776860cdacfdd26174aa300a37bbc8b621",
    "pilates-open-leg-rocker": "47c0a9fd7a85d64668ea20a1e7c4101f4cae0b6efc5df4a8916af169817d4af0",
}


class GenerationFloorTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        poses.load_families()
        cls.roll, cls.roll_reach = poses.build("pilates-roll-over")
        cls.wiper, cls.wiper_reach = poses.build("hanging-windshield-wiper")

    @staticmethod
    def failures(data):
        return {failure["check"] for failure in audit(data)["failures"]}

    def test_authored_variants_pass_reach_floor_and_scoped_checks(self):
        self.assertEqual([], self.roll_reach)
        self.assertEqual([], self.wiper_reach)
        self.assertEqual([], poses.checks(self.roll))
        self.assertEqual([], poses.checks(self.wiper))
        self.assertEqual([], audit(self.roll)["failures"])
        self.assertEqual([], audit(self.wiper)["failures"])
        for data in (self.roll, self.wiper):
            metrics = audit(data)["metrics"]
            self.assertGreater(metrics["heel_frame_condition_min"]["value"], .15)
            self.assertLess(metrics["heel_frame_normal_step_max_deg"]["value"], 45)
        self.assertGreaterEqual(audit(self.roll)["metrics"]["chest_surface_min_m"]["value"], .001)

    def test_rollover_chest_support_keeps_fixed_wrist_anchors_and_range(self):
        for frame in self.roll["frames"]:
            self.assertEqual([.03, -.18, .04], frame["wrist_r"])
            self.assertEqual([.03, .18, .04], frame["wrist_l"])
        metrics = audit(self.roll)["metrics"]
        self.assertGreaterEqual(metrics["trunk_curl_max_deg"]["value"], 45)
        self.assertGreaterEqual(metrics["pelvis_lift_range_m"]["value"], .18)
        self.assertGreaterEqual(metrics["leg_opening_max_m"]["value"], .30)

    def test_rollover_chest_surface_penetration_is_rejected(self):
        bad = copy.deepcopy(self.roll)
        for frame in bad["frames"]:
            frame["chest"][2] -= .01
        self.assertIn("chest_surface_min_m", self.failures(bad))

    def test_frozen_old_encodes_are_exact_baseline_copies_and_rejected(self):
        for exercise_id, checks in {
            "pilates-roll-over": {"trunk_curl_max_deg", "hand_drift_max_m", "chest_surface_min_m"},
            "hanging-windshield-wiper": {"torso_counterroll_max_deg"},
        }.items():
            fixture = ROOT / "tests/fixtures/motion" / f"generation-floor-{exercise_id}-before.json.gz"
            baseline = ROOT / "work/video-generation-2026-10-21/baseline" / f"{exercise_id}.json"
            with gzip.open(fixture, "rb") as stream:
                frozen = stream.read()
            self.assertEqual(frozen, baseline.read_bytes())
            self.assertTrue(checks <= self.failures(json.loads(frozen)))

    def test_missing_rollover_curl_phase_is_rejected(self):
        bad = copy.deepcopy(self.roll)
        for frame in bad["frames"]:
            frame["chest_up"] = list(frame["pelvis_up"])
        self.assertIn("trunk_curl_max_deg", self.failures(bad))

    def test_missing_wiper_counterroll_is_rejected(self):
        bad = copy.deepcopy(self.wiper)
        for frame in bad["frames"]:
            frame["pelvis_left"] = list(bad["frames"][0]["pelvis_left"])
        self.assertIn("torso_counterroll_max_deg", self.failures(bad))

    def test_slipping_floor_hands_are_rejected(self):
        bad = copy.deepcopy(self.roll)
        for i, frame in enumerate(bad["frames"]):
            frame["wrist_l"][0] += .02 * i/(len(bad["frames"])-1)
        self.assertIn("hand_drift_max_m", self.failures(bad))

    def test_wrong_variant_and_missing_phase_duration_are_rejected(self):
        bad = copy.deepcopy(self.roll)
        bad["id"] = "pilates-single-leg-circle"
        self.assertIn("wrong_variant", self.failures(bad))
        bad = copy.deepcopy(self.wiper)
        bad["frames"] = bad["frames"][:len(bad["frames"])//2]
        self.assertIn("duration_s", self.failures(bad))

    def test_discontinuity_and_floor_penetration_are_rejected(self):
        bad = copy.deepcopy(self.wiper)
        bad["frames"][60]["pelvis"][0] += .25
        self.assertIn("max_joint_speed_m_s", self.failures(bad))
        bad = copy.deepcopy(self.roll)
        bad["frames"][20]["spine"][2] = -.3
        self.assertIn("floor_surface_min_m", self.failures(bad))

    def test_invalid_geometry_fails_explicitly(self):
        bad = copy.deepcopy(self.roll)
        bad["frames"][2]["pelvis"][0] = math.nan
        self.assertIn("valid_geometry", self.failures(bad))

    def test_zero_orientation_axis_fails_and_metric_is_strict_json(self):
        bad = copy.deepcopy(self.roll)
        for frame in bad["frames"]:
            frame["chest_up"] = [0., 0., 0.]
        result = audit(bad)
        self.assertIn("trunk_curl_max_deg", {x["check"] for x in result["failures"]})
        self.assertIsNone(result["metrics"]["trunk_curl_max_deg"]["value"])
        json.dumps(result, allow_nan=False)

    def test_speed_and_seam_ignore_orientation_and_grip_metadata(self):
        altered = copy.deepcopy(self.wiper)
        for i, frame in enumerate(altered["frames"]):
            frame["pelvis_up"] = [1e6 if i % 2 else -1e6, 0., 0.]
            frame["grip_l"] = [1e6 if i % 2 else -1e6, 0., 0.]
        baseline = audit(self.wiper)
        result = audit(altered)
        self.assertAlmostEqual(result["metrics"]["max_joint_speed_m_s"]["value"],
                               baseline["metrics"]["max_joint_speed_m_s"]["value"])
        self.assertAlmostEqual(result["metrics"]["loop_seam_max_m"]["value"],
                               baseline["metrics"]["loop_seam_max_m"]["value"])
        self.assertEqual([], result["failures"])
        json.dumps(result, allow_nan=False)

    def test_world_up_wiper_feet_are_rejected_by_heel_frame_condition(self):
        bad = copy.deepcopy(self.wiper)
        for frame in bad["frames"]:
            for side in ("l", "r"):
                frame["toe_"+side] = [frame["ankle_"+side][0],
                                      frame["ankle_"+side][1],
                                      frame["ankle_"+side][2]+.20]
        self.assertIn("heel_frame_condition_min", self.failures(bad))

    def test_rollover_feet_follow_shins_without_collinearity(self):
        bad = copy.deepcopy(self.roll)
        for frame in bad["frames"]:
            for side in ("l", "r"):
                shin = [frame["ankle_"+side][i]-frame["knee_"+side][i] for i in range(3)]
                length = math.sqrt(sum(x*x for x in shin))
                frame["toe_"+side] = [frame["ankle_"+side][i]+.20*shin[i]/length for i in range(3)]
        self.assertIn("heel_frame_condition_min", self.failures(bad))

    def test_heel_frame_normal_flip_is_rejected(self):
        bad = copy.deepcopy(self.wiper)
        frame = bad["frames"][70]
        shin = [frame["ankle_l"][i]-frame["knee_l"][i] for i in range(3)]
        length = math.sqrt(sum(x*x for x in shin))
        shin = [x/length for x in shin]
        offset = [-shin[2], 0., shin[0]]
        offset_length = math.sqrt(sum(x*x for x in offset))
        offset = [x/offset_length for x in offset]
        pointed = [shin[i]-.28*offset[i] for i in range(3)]
        pointed_length = math.sqrt(sum(x*x for x in pointed))
        frame["toe_l"] = [frame["ankle_l"][i]+.20*pointed[i]/pointed_length for i in range(3)]
        self.assertIn("heel_frame_normal_step_max_deg", self.failures(bad))

    def test_untouched_sibling_outputs_keep_their_hashes(self):
        for exercise_id, expected in SIBLING_HASHES.items():
            data, reach = poses.build(exercise_id)
            self.assertEqual([], reach, exercise_id)
            raw = json.dumps(data, sort_keys=True, separators=(",", ":")).encode()
            self.assertEqual(expected, hashlib.sha256(raw).hexdigest(), exercise_id)


if __name__ == "__main__":
    unittest.main()
