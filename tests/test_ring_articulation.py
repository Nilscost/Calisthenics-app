"""Independent dynamic-ring and wrist articulation regressions."""
import copy
import gzip
import json
import math
from pathlib import Path
import sys
import unittest

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools/blender'))
import poses
from ring_geometry import ring_frame
from generation_checks.ring_articulation import audit


def failures(data):
    return {x['check'] for x in audit(data)['failures']}


class RingArticulationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        poses.load_families()
        cls.data = poses.build('ring-muscle-up')[0]
        with gzip.open(ROOT / 'tests/fixtures/motion/ring-articulation-before.json.gz', 'rt') as f:
            cls.before = json.load(f)

    def test_candidate_has_dynamic_orthogonal_rings_and_grip(self):
        result = audit(self.data)
        self.assertEqual([], result['failures'], result['failures'])
        self.assertTrue(all(m['ok'] for m in result['metrics'].values()))

    def test_frozen_legacy_baseline_fails_ring_dynamics_but_keeps_wrist_baseline_honest(self):
        result = audit(self.before)
        bad = {x['check'] for x in result['failures']}
        self.assertIn('ring_l_dynamic_channels', bad)
        self.assertIn('ring_l_center_excursion_m', bad)
        self.assertTrue(result['metrics']['wrist_l_bend_deg']['ok'])
        self.assertTrue(result['metrics']['wrist_r_bend_deg']['ok'])
        self.assertTrue(result['metrics']['wrist_l_bend_step_deg']['ok'])
        self.assertIn('hang_wrist_angle_min_deg', bad)
        self.assertIn('support_wrist_angle_max_deg', bad)
        self.assertTrue(math.isfinite(result['metrics']['wrist_l_grip_contact_error_m']['value']))

    def test_legacy_extreme_wrist_bend_remains_rejected_by_global_limit(self):
        with gzip.open(ROOT / 'tests/fixtures/motion/ring-muscle-up-wrist-before.json.gz', 'rt') as f:
            old = json.load(f)
        result = audit(old)
        self.assertIn('wrist_l_bend_deg', {x['check'] for x in result['failures']})
        self.assertGreater(result['metrics']['wrist_l_bend_deg']['value'], 110)

    def test_freezing_ring_and_hand_motion_fails_independent_range_gates(self):
        bad = copy.deepcopy(self.data)
        first = bad['frames'][0]
        for f in bad['frames']:
            for side in ('l', 'r'):
                for prefix in ('ring_center_', 'ring_plane_', 'ring_radial_'):
                    key = prefix + side + ('_up' if prefix != 'ring_center_' else '')
                    if key in first: f[key] = list(first[key])
                origin = first['wrist_' + side]
                axis = [first['hand_' + side][j] - origin[j] for j in range(3)]
                f['hand_' + side] = [f['wrist_' + side][j] + axis[j] for j in range(3)]
                f['palmn_' + side] = list(first['palmn_' + side])
        bad_checks = failures(bad)
        self.assertIn('ring_l_center_excursion_m', bad_checks)
        self.assertIn('ring_l_plane_rotation_range_deg', bad_checks)
        self.assertIn('hand_l_axis_rotation_range_deg', bad_checks)

    def test_zero_ring_axis_is_rejected_without_crashing(self):
        bad = copy.deepcopy(self.data)
        bad['frames'][0]['ring_plane_l_up'] = [0.0, 0.0, 0.0]
        result = audit(bad)
        self.assertIn('ring_l_frame_data_valid', {x['check'] for x in result['failures']})

    def test_nonorthogonal_hand_and_palm_axes_are_rejected(self):
        bad = copy.deepcopy(self.data)
        f = bad['frames'][40]
        axis = [f['hand_l'][j] - f['wrist_l'][j] for j in range(3)]
        length = math.sqrt(sum(x*x for x in axis))
        f['palmn_l'] = [x/length for x in axis]
        self.assertIn('hand_l_palm_orthogonality_error', failures(bad))

    def test_upturned_false_grip_polarity_is_rejected_independently(self):
        bad = copy.deepcopy(self.data)
        result = audit(bad)
        left = next(p for p in bad['props'] if p.get('joint') == 'wrist_l')
        selected = [i for i, f in enumerate(bad['frames'])
                    if f['chest'][2] - ring_frame(f, left)[3][2] <= -.30]
        self.assertTrue(selected)
        for i in selected:
            for side in ('l', 'r'):
                f = bad['frames'][i]
                f['palmn_' + side][2] = abs(f['palmn_' + side][2])
        result = audit(bad)
        self.assertIn('false_grip_hang_palm_normal_z_max', {x['check'] for x in result['failures']})

    def test_wrist_below_wood_is_rejected_even_with_downward_palm(self):
        bad = copy.deepcopy(self.data)
        left = next(p for p in bad['props'] if p.get('joint') == 'wrist_l')
        target_z = ring_frame(bad['frames'][0], left)[3][2]
        selected = [f for f in bad['frames'] if f['chest'][2] - ring_frame(f, left)[3][2] <= -.30]
        self.assertTrue(selected)
        for f in selected:
            for side in ('l', 'r'):
                # Keep a valid downward palm polarity while placing the wrist
                # below the declared grip height.
                f['palmn_' + side] = [0., 0., -1.]
                f['wrist_' + side][2] = target_z - .02
        result = audit(bad)
        self.assertIn('false_grip_hang_wrist_over_wood_min_m', {x['check'] for x in result['failures']})

    def test_nonorthogonal_ring_frame_is_rejected(self):
        bad = copy.deepcopy(self.data)
        bad['frames'][30]['ring_radial_l_up'] = list(bad['frames'][30]['ring_plane_l_up'])
        self.assertIn('ring_l_orthogonality_error', failures(bad))

    def test_detached_grip_is_rejected(self):
        bad = copy.deepcopy(self.data)
        bad['frames'][40]['wrist_l'][0] += .02
        self.assertIn('wrist_l_grip_contact_error_m', failures(bad))

    def test_single_frame_ring_twist_is_rejected(self):
        bad = copy.deepcopy(self.data)
        f = bad['frames'][40]
        n = f['ring_plane_l_up']
        r = f['ring_radial_l_up']
        # Rotate the radial axis 90 degrees around the unchanged plane normal.
        cross = [r[1]*n[2]-r[2]*n[1], r[2]*n[0]-r[0]*n[2], r[0]*n[1]-r[1]*n[0]]
        f['ring_radial_l_up'] = cross
        bad_checks = failures(bad)
        self.assertIn('ring_l_max_frame_twist_deg', bad_checks)

    def test_lost_turnover_body_motion_is_rejected(self):
        bad = copy.deepcopy(self.data)
        for f in bad['frames']:
            f['chest'][2] = bad['frames'][0]['chest'][2]
        self.assertIn('chest_vertical_excursion_m', failures(bad))

    def test_missing_dynamic_frame_data_returns_finite_failure_metrics(self):
        bad = copy.deepcopy(self.data)
        del bad['frames'][20]['ring_radial_l_up']
        result = audit(bad)
        self.assertTrue(result['failures'])
        for metric in result['metrics'].values():
            self.assertTrue(metric['value'] is None or math.isfinite(metric['value']))


if __name__ == '__main__':
    unittest.main()
