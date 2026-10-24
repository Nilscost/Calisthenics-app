"""Depth-jump variant screening and independent fault controls."""
import copy
import gzip
import json
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools/blender"))
import poses
from generation_checks.depth_jump import audit, _contact_flags, _phase_indices


class DepthJumpPhysicsTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        poses.load_families()
        cls.data, cls.reach = poses.build("depth-jump")

    def failed(self, data): return {f["check"] for f in audit(data)["failures"]}

    def phases(self, data=None):
        data = data or self.data
        floor, box = _contact_flags(data)
        return _phase_indices(data["frames"], floor, box)

    def test_candidate_has_short_ballistic_drop_and_rebound_with_arm_swing(self):
        self.assertEqual([], self.reach)
        report = audit(self.data)
        self.assertEqual([], report["failures"])
        self.assertTrue(all(m["ok"] for m in report["metrics"].values()))
        self.assertGreaterEqual(report["metrics"]["rebound_gravity_m_s2"]["value"], 6.0)
        self.assertLessEqual(report["metrics"]["rebound_gravity_m_s2"]["value"], 13.5)
        self.assertGreater(report["metrics"]["arm_takeoff_elbow_min_deg"]["value"], 145)

    def test_frozen_old_8s_media_source_and_superseded_68s_pose_fail_physics(self):
        gallery_before = json.load(gzip.open(ROOT / "tests/fixtures/motion/depth-jump-gallery-before.json.gz", "rt"))
        float_before = json.load(gzip.open(ROOT / "tests/fixtures/motion/depth-jump-float-before.json.gz", "rt"))
        for old in (gallery_before, float_before):
            failures = self.failed(old)
            self.assertIn("rebound_gravity_m_s2", failures)
            self.assertIn("arm_takeoff_elbow_min_deg", failures)
            self.assertIn("arm_swing_relative_height_range_m", failures)
            self.assertIn("max_wrist_axis_bend_deg", failures)
            self.assertIn("takeoff_knee_min_deg", failures)
        # The original 8 s gallery source is specifically rejected for a long,
        # nonballistic rebound; the later 6.8 s draft remains separate evidence.
        self.assertIn("rebound_flight_duration_s", self.failed(gallery_before))

    def test_floor_contact_while_rear_foot_is_still_box_supported_is_rejected(self):
        bad = copy.deepcopy(self.data); floor, box = _contact_flags(bad)
        i = next(i for i in range(1, len(bad["frames"]))
                 if box(bad["frames"][i], "l") != box(bad["frames"][i], "r"))
        side = "l" if not box(bad["frames"][i], "l") else "r"
        bad["frames"][i]["ankle_"+side][2] = .08
        self.assertIn("early_floor_while_box_supported_frames", self.failed(bad))

    def test_missing_bilateral_preflight_is_rejected(self):
        bad = copy.deepcopy(self.data); phase = self.phases(bad)
        for i in phase["both_air"]: bad["frames"][i]["ankle_l"][2] = .08
        self.assertIn("box_drop_bilateral_air_frames", self.failed(bad))

    def test_frozen_apex_and_linear_floating_rebound_are_rejected(self):
        phase = self.phases(); takeoff, landing = phase["first_takeoff"], phase["second_touch"]
        self.assertIsNotNone(takeoff); self.assertIsNotNone(landing)
        apex = max(range(takeoff, landing+1), key=lambda i: self.data["frames"][i]["pelvis"][2])
        held = copy.deepcopy(self.data); z = held["frames"][apex]["pelvis"][2]
        for i in range(max(takeoff, apex-2), min(landing+1, apex+3)):
            held["frames"][i]["pelvis"][2] = z
        self.assertIn("rebound_apex_dwell_frames", self.failed(held))

        floaty = copy.deepcopy(self.data); z0 = floaty["frames"][takeoff]["pelvis"][2]
        z1 = floaty["frames"][landing]["pelvis"][2]
        for i in range(takeoff, landing+1):
            t = (i-takeoff)/(landing-takeoff)
            floaty["frames"][i]["pelvis"][2] = z0+(z1-z0)*t
        self.assertIn("rebound_gravity_m_s2", self.failed(floaty))

    def test_missing_first_landing_absorption_is_rejected(self):
        bad = copy.deepcopy(self.data); phase = self.phases(bad)
        lo, hi = phase["first_bilateral"], phase["first_takeoff"]
        z = bad["frames"][lo]["pelvis"][2]
        for i in range(lo, hi): bad["frames"][i]["pelvis"][2] = z
        self.assertIn("first_landing_compression_m", self.failed(bad))

    def test_held_first_landing_contact_is_rejected(self):
        bad = copy.deepcopy(self.data); phase = self.phases(bad)
        launch = phase["first_takeoff"]-1
        held = [copy.deepcopy(bad["frames"][launch]) for _ in range(8)]
        bad["frames"][launch+1:launch+1] = held
        # Added hold can also exceed the overall loop limit; the contact
        # guard must independently reject the altered landing.
        self.assertIn("first_landing_contact_duration_s", self.failed(bad))

    def test_first_landing_slip_is_rejected(self):
        bad = copy.deepcopy(self.data); phase = self.phases(bad)
        lo, hi = phase["first_bilateral"], phase["first_takeoff"]
        planted = [i for i in range(lo, hi)
                   if all(_contact_flags(bad)[0](bad["frames"][i], s) for s in ("l", "r"))]
        for i in planted[1:4]: bad["frames"][i]["ankle_l"][0] += .01
        self.assertIn("first_landing_planted_slip_m", self.failed(bad))

    def test_slipping_landed_foot_is_rejected(self):
        bad = copy.deepcopy(self.data); phase = self.phases(bad); landing = phase["second_touch"]
        self.assertIsNotNone(landing)
        floor, _ = _contact_flags(bad)
        stable = [i for i in range(landing, len(bad["frames"]))
                  if floor(bad["frames"][i], "l") and floor(bad["frames"][i], "r")]
        for i in stable[1:5]: bad["frames"][i]["ankle_l"][0] += .12
        self.assertIn("second_landing_planted_slip_m", self.failed(bad))

    def test_fixed_bent_forearms_are_rejected(self):
        bad = copy.deepcopy(self.data); i = self.phases(bad)["first_takeoff"]-1
        for side in ("l", "r"):
            sh, wr = bad["frames"][i]["shoulder_"+side], bad["frames"][i]["wrist_"+side]
            delta = [wr[k]-sh[k] for k in range(3)]
            length = sum(x*x for x in delta)**.5
            perp = [-delta[2]/length, 0, delta[0]/length]
            bad["frames"][i]["elbow_"+side] = [
                (sh[k]+wr[k])*.5 + perp[k]*length*.5 for k in range(3)]
        self.assertIn("arm_takeoff_elbow_min_deg", self.failed(bad))

    def test_frozen_world_hand_axes_are_rejected_across_the_cycle(self):
        bad = copy.deepcopy(self.data)
        for frame in bad["frames"]:
            frame["hand_l"] = [frame["wrist_l"][0]+1, frame["wrist_l"][1], frame["wrist_l"][2]]
            frame["hand_r"] = [frame["wrist_r"][0]+1, frame["wrist_r"][1], frame["wrist_r"][2]]
        report = audit(bad)
        metric = report["metrics"]["max_wrist_axis_bend_deg"]
        self.assertFalse(metric["ok"])
        self.assertGreater(metric["value"], 45)

    def test_step_off_not_jump_off_and_nonfinite_geometry_are_rejected(self):
        bad = copy.deepcopy(self.data); phase = self.phases(bad)
        for i in range(phase["box_clear"]+1, phase["first_touch"]):
            bad["frames"][i]["pelvis"][2] += .12
        self.assertIn("step_departure_pelvis_rise_m", self.failed(bad))

        malformed = copy.deepcopy(self.data)
        malformed["frames"][0]["head"][0] = float("nan")
        report = audit(malformed)
        self.assertFalse(report["metrics"]["finite_geometry"]["ok"])
        self.assertIn("finite_geometry", {f["check"] for f in report["failures"]})

    def test_unextended_knees_and_boundary_launch_dip_are_rejected(self):
        phase = self.phases(); launch = phase["first_takeoff"]-1
        bad_knee = copy.deepcopy(self.data)
        for side in ("l", "r"):
            frame = bad_knee["frames"][launch]
            hip, ankle = frame["hip_"+side], frame["ankle_"+side]
            axis = [ankle[k]-hip[k] for k in range(3)]
            perp = [axis[2], 0., -axis[0]]
            length = sum(x*x for x in axis)**.5
            norm = sum(x*x for x in perp)**.5
            frame["knee_"+side] = [
                (hip[k]+ankle[k])*.5 + perp[k]/norm*length*.35 for k in range(3)]
        failures = self.failed(bad_knee)
        self.assertIn("takeoff_knee_min_deg", failures)
        self.assertIn("takeoff_knee_extension_gain_deg", failures)

        dip = copy.deepcopy(self.data)
        dip["frames"][launch]["pelvis"][2] -= .15
        failures = self.failed(dip)
        self.assertIn("takeoff_upward_boundary_min_velocity_m_s", failures)
        self.assertIn("takeoff_boundary_velocity_delta_m_s", failures)

    def test_wrong_nonstep_variant_and_lost_return_are_rejected(self):
        bad = copy.deepcopy(self.data); bad["id"] = "box-jump"
        self.assertIn("exercise_id", self.failed(bad))
        bad = copy.deepcopy(self.data)
        for f in bad["frames"][-14:]: f["ankle_l"][2] = .08; f["ankle_r"][2] = .08
        self.assertIn("return_both_feet_to_box_frames", self.failed(bad))


if __name__ == "__main__": unittest.main()
