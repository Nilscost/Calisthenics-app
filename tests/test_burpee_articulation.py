"""Independent regressions for burpee-pushup knee articulation."""
import copy
import gzip
import json
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools/blender"))
import poses
from generation_checks.burpee_articulation import (
    audit, _plank_mask, _knee_metrics, _longest_consecutive_run,
)


class BurpeeArticulationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        poses.load_families()
        cls.data, cls.reach = poses.build("burpee-pushup")
        fixture = ROOT / "tests/fixtures/motion/burpee-knees-before.json.gz"
        with gzip.open(fixture, "rt") as stream:
            cls.before = json.load(stream)

    @staticmethod
    def failed(data):
        return {item["check"] for item in audit(data)["failures"]}

    @staticmethod
    def plank_bounds(data):
        mask = _plank_mask(data["frames"])
        ids = [i for i, planted in enumerate(mask) if planted]
        return min(ids), max(ids)

    @classmethod
    def final_flight_bounds(cls, data):
        _, plank_end = cls.plank_bounds(data)
        runs, active = [], None
        for i in range(plank_end + 1, len(data["frames"])):
            frame = data["frames"][i]
            airborne = min(frame["ankle_l"][2], frame["ankle_r"][2]) > .14
            if airborne and active is None:
                active = i
            elif not airborne and active is not None:
                runs.append((active, i - 1))
                active = None
        if active is not None:
            runs.append((active, len(data["frames"]) - 1))
        return max(runs, key=lambda span: span[1])

    def test_candidate_keeps_knees_in_sagittal_plane_and_transition_continuity(self):
        self.assertEqual([], self.reach)
        self.assertEqual([], poses.checks(self.data))
        report = audit(self.data)
        self.assertEqual([], report["failures"])
        self.assertTrue(all(m["ok"] for m in report["metrics"].values()))
        self.assertLessEqual(report["metrics"]["max_knee_lateral_excursion_m"]["value"], .10)

    def test_frozen_actual_old_pose_reproduces_lateral_knee_pop_and_transition_spikes(self):
        report = audit(self.before)
        failed = {item["check"] for item in report["failures"]}
        self.assertTrue({"max_knee_lateral_excursion_m", "forward_sagittal_bend_fraction",
                         "peak_lateral_knee_accel_m_s2", "squat_to_plank_knee_accel_m_s2",
                         "plank_to_squat_knee_accel_m_s2"} <= failed)
        self.assertTrue({"final_jump_flight_duration_s", "final_jump_apex_dwell_s",
                         "final_jump_gravity_fit_m_s2", "final_jump_takeoff_knee_min_deg"} <= failed)
        self.assertAlmostEqual(report["metrics"]["max_knee_lateral_excursion_m"]["value"],
                               .411384, places=5)
        # The old animation retains its stable push-up contacts and meaningful
        # leg flexion; the regression is specifically the knee path/branch.
        self.assertGreater(report["metrics"]["stable_plank_frames"]["value"], 50)
        self.assertGreater(report["metrics"]["flexed_knee_samples"]["value"], 250)

    def test_final_jump_apex_hold_mutation_is_rejected(self):
        bad = copy.deepcopy(self.data)
        takeoff, landing = self.final_flight_bounds(bad)
        peak = max(range(takeoff, landing + 1), key=lambda i: bad["frames"][i]["pelvis"][2])
        peak_z = bad["frames"][peak]["pelvis"][2]
        half = max(2, round(.15 * bad["fps"]))
        for i in range(max(takeoff, peak - half), min(landing + 1, peak + half + 1)):
            bad["frames"][i]["pelvis"][2] = peak_z
        self.assertIn("final_jump_apex_dwell_s", self.failed(bad))

    def test_bent_leg_takeoff_mutation_is_rejected(self):
        bad = copy.deepcopy(self.data)
        first_air, _ = self.final_flight_bounds(bad)
        _, plank_end = self.plank_bounds(bad)
        takeoff = next(i for i in range(first_air - 1, max(plank_end, first_air - round(.4 * bad["fps"])) - 1, -1)
                       if bad["frames"][i]["ankle_l"][2] <= .095
                       and bad["frames"][i]["ankle_r"][2] <= .095)
        frame = bad["frames"][takeoff]
        for side in ("l", "r"):
            hip, ankle = frame[f"hip_{side}"], frame[f"ankle_{side}"]
            chord = [ankle[j] - hip[j] for j in range(3)]
            chord_len = sum(x*x for x in chord) ** .5
            normal = [1 - chord[0]**2/chord_len**2,
                      -chord[0]*chord[1]/chord_len**2,
                      -chord[0]*chord[2]/chord_len**2]
            n_len = sum(x*x for x in normal) ** .5
            if n_len < 1e-6:
                normal = [0, -chord[2], chord[1]]
                n_len = sum(x*x for x in normal) ** .5
            normal = [x / n_len for x in normal]
            frame[f"knee_{side}"] = [(hip[j] + ankle[j]) / 2 + .14 * normal[j]
                                     for j in range(3)]
        self.assertIn("final_jump_takeoff_knee_min_deg", self.failed(bad))

    def test_apex_dwell_counts_only_consecutive_near_peak_frames(self):
        self.assertEqual(3, _longest_consecutive_run([3, 4, 5, 9, 10, 15]))
        self.assertEqual(1, _longest_consecutive_run([2, 4, 6]))

    def test_lateral_knee_excursion_mutation_is_rejected(self):
        bad = copy.deepcopy(self.data)
        i = next(i for i, frame in enumerate(bad["frames"])
                 if _knee_metrics(frame, "l")[0] < 145)
        bad["frames"][i]["knee_l"][1] += .18
        self.assertIn("max_knee_lateral_excursion_m", self.failed(bad))

    def test_one_frame_knee_acceleration_mutation_is_rejected(self):
        bad = copy.deepcopy(self.data)
        start, _ = self.plank_bounds(bad)
        bad["frames"][start]["knee_l"][1] += .045
        failed = self.failed(bad)
        self.assertTrue({"squat_to_plank_knee_accel_m_s2", "peak_lateral_knee_accel_m_s2"} & failed)

    def test_sagittal_branch_flip_mutation_is_rejected_without_lateral_shift(self):
        bad = copy.deepcopy(self.data)
        center = next(i for i, frame in enumerate(bad["frames"])
                      if _knee_metrics(frame, "l")[0] < 140)
        frame = bad["frames"][center]
        hip, knee, ankle = frame["hip_l"], frame["knee_l"], frame["ankle_l"]
        chord_mid_x = (hip[0] + ankle[0]) / 2
        knee[0] = 2 * chord_mid_x - knee[0]
        self.assertIn("flexed_sagittal_branch_flips", self.failed(bad))

    def test_knee_discontinuity_at_plank_to_squat_is_rejected(self):
        bad = copy.deepcopy(self.data)
        _, end = self.plank_bounds(bad)
        bad["frames"][end]["knee_r"][1] -= .045
        failed = self.failed(bad)
        self.assertTrue({"plank_to_squat_knee_accel_m_s2", "peak_lateral_knee_accel_m_s2"} & failed)

    def test_nonfinite_and_missing_knee_geometry_fail_explicitly(self):
        bad = copy.deepcopy(self.data)
        bad["frames"][20]["knee_l"][0] = float("nan")
        self.assertIn("finite_leg_geometry", self.failed(bad))
        bad = copy.deepcopy(self.data)
        del bad["frames"][20]["ankle_r"]
        self.assertIn("finite_leg_geometry", self.failed(bad))


if __name__ == "__main__":
    unittest.main()
