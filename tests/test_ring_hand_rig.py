"""Independent rigidity, scale, connectivity and wood-proximity regressions."""
import gzip
import json
import math
from pathlib import Path
import sys
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools/blender"))
import poses
import ring_geometry
from hand_axes import axes
from generation_checks.ring_hand_rig import audit


class RingHandRigTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        poses.load_families()
        from experimental.ring_rigid_candidate import specification
        previous = poses.EXERCISES["ring-muscle-up"]
        try:
            poses.EXERCISES["ring-muscle-up"] = specification
            cls.data = poses.build("ring-muscle-up")[0]
        finally:
            poses.EXERCISES["ring-muscle-up"] = previous
        with gzip.open(ROOT / "tests/fixtures/motion/ring-variable-fingers-before.json.gz", "rt") as f:
            cls.before = json.load(f)

    def test_candidate_rig_is_fixed_connected_anatomical_and_near_wood(self):
        result = audit(self.data)
        self.assertEqual([], result["failures"], result["failures"])
        self.assertTrue(all(metric["ok"] for metric in result["metrics"].values()))

    def test_old_variable_length_hovering_hand_is_rejected(self):
        result = audit(self.before)
        bad = {item["check"] for item in result["failures"]}
        self.assertIn("ring_l_segment_length_variation_max_m", bad)
        self.assertIn("ring_r_segment_length_variation_max_m", bad)
        self.assertIn("ring_l_hang_palm_wood_clearance_max_m", bad)
        self.assertIn("ring_l_support_palm_wood_clearance_max_m", bad)
        self.assertGreater(result["metrics"]["ring_l_segment_length_variation_max_m"]["value"], .02)

    def test_one_phalanx_length_mutation_fails_rigidity(self):
        original = ring_geometry.hand_segments

        def stretched(frame, side, spec):
            segments = [([*a], [*b]) for a, b in original(frame, side, spec)]
            if frame is self.data["frames"][24] and side == "l":
                a, b = segments[0]
                delta = [(b[i]-a[i])*.2 for i in range(3)]
                segments[0] = (a, [b[i]+delta[i] for i in range(3)])
                for j in (1, 2):
                    segments[j] = ([p+delta[i] for i, p in enumerate(segments[j][0])],
                                   [p+delta[i] for i, p in enumerate(segments[j][1])])
            return segments

        with patch.object(ring_geometry, "hand_segments", side_effect=stretched):
            bad = {x["check"] for x in audit(self.data)["failures"]}
        self.assertIn("ring_l_segment_length_variation_max_m", bad)

    def test_disconnected_joint_is_rejected(self):
        original = ring_geometry.hand_segments

        def disconnected(frame, side, spec):
            segments = [([*a], [*b]) for a, b in original(frame, side, spec)]
            if frame is self.data["frames"][24] and side == "r":
                segments[1] = ([segments[1][0][0]+.002, *segments[1][0][1:]], segments[1][1])
            return segments

        with patch.object(ring_geometry, "hand_segments", side_effect=disconnected):
            bad = {x["check"] for x in audit(self.data)["failures"]}
        self.assertIn("ring_r_joint_gap_max_m", bad)

    def test_connected_fixed_length_digit_sliding_on_palm_is_rejected(self):
        original = ring_geometry.hand_segments

        def sliding(frame, side, spec):
            segments = [([*a], [*b]) for a, b in original(frame, side, spec)]
            if frame is self.data["frames"][24] and side == "l":
                _, _, across = axes(frame["wrist_l"], frame["hand_l"], frame["palmn_l"], "l")
                delta = [.01*x for x in across]
                for i in range(3):
                    segments[i] = ([segments[i][0][j]+delta[j] for j in range(3)],
                                   [segments[i][1][j]+delta[j] for j in range(3)])
            return segments

        with patch.object(ring_geometry, "hand_segments", side_effect=sliding):
            result = audit(self.data)
        bad = {x["check"] for x in result["failures"]}
        self.assertIn("ring_l_index_mcp_local_variation_max_m", bad)
        self.assertTrue(result["metrics"]["ring_l_segment_length_variation_max_m"]["ok"])
        self.assertTrue(result["metrics"]["ring_l_joint_gap_max_m"]["ok"])

    def test_hovering_capsules_are_rejected_even_when_rigidity_is_unchanged(self):
        original = ring_geometry.hand_segments

        def hovering(frame, side, spec):
            centre, normal, _, _ = ring_geometry.ring_frame(frame, spec)
            shifted = []
            for a, b in original(frame, side, spec):
                mid = [(a[i]+b[i])*.5 for i in range(3)]
                radial = [mid[i]-centre[i] for i in range(3)]
                dot = sum(radial[i]*normal[i] for i in range(3))
                radial = [radial[i]-dot*normal[i] for i in range(3)]
                mag = math.sqrt(sum(v*v for v in radial))
                radial = [v/mag for v in radial]
                shifted.append(([a[i]+.03*radial[i] for i in range(3)],
                                [b[i]+.03*radial[i] for i in range(3)]))
            return shifted

        with patch.object(ring_geometry, "hand_segments", side_effect=hovering):
            result = audit(self.data)
        bad = {x["check"] for x in result["failures"]}
        self.assertIn("ring_l_actual_finger_to_wood_gap_max_m", bad)
        self.assertTrue(result["metrics"]["ring_l_segment_length_variation_max_m"]["ok"])

    def test_malformed_or_nonfinite_segments_fail_without_crashing(self):
        original = ring_geometry.hand_segments

        def malformed(frame, side, spec):
            segments = original(frame, side, spec)
            if frame is self.data["frames"][8] and side == "l":
                return segments[:-1]
            if frame is self.data["frames"][9] and side == "r":
                segments = list(segments)
                segments[0] = (segments[0][0], [math.nan, 0., 0.])
            return segments

        with patch.object(ring_geometry, "hand_segments", side_effect=malformed):
            result = audit(self.data)
        bad = {x["check"] for x in result["failures"]}
        self.assertIn("ring_l_hand_data_valid", bad)
        self.assertIn("ring_r_hand_data_valid", bad)
        self.assertTrue(all(m["value"] is None or math.isfinite(m["value"])
                            for m in result["metrics"].values()))


if __name__ == "__main__":
    unittest.main()
