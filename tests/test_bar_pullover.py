"""Actual bad encode trajectory and independent smoothness/contact regressions."""
import copy
import gzip
import json
import math
from pathlib import Path
import sys
import unittest
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools/blender'))
import poses
from motion_quality import audit
from bar_grip import wrapped_segments, grip_clearance


class BarPulloverTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        poses.load_families()
        cls.data,cls.reach=poses.build('pullup-pullover')

    def failures(self,data):
        return {f['check'] for f in audit(data)['failures']}

    def test_full_pullover_and_slower_reverse_pass_geometry_and_motion_gates(self):
        self.assertEqual([],self.reach)
        self.assertEqual([],poses.checks(self.data))
        report=audit(self.data)
        self.assertEqual('pass',report['status'],report['failures'])
        self.assertGreater(report['metrics']['descent_ascent_time_ratio']['value'],1.05)

    def test_actual_old_abrupt_trajectory_is_rejected(self):
        with gzip.open(ROOT/'tests/fixtures/motion/pullup-pullover-before.json.gz') as f:old=json.load(f)
        self.assertTrue({'angular_speed_deg_s','joint_acceleration_m_s2','body_bar_volume_clearance_m','descent_ascent_time_ratio'}<=self.failures(old))

    def test_stretching_old_duration_alone_does_not_pass(self):
        with gzip.open(ROOT/'tests/fixtures/motion/pullup-pullover-before.json.gz') as f:old=json.load(f)
        old['frames']=[f for f in old['frames'] for _ in range(3)]
        self.assertNotIn('duration_s',self.failures(old))
        self.assertIn('joint_acceleration_m_s2',self.failures(old))

    def test_shortened_loop_is_rejected(self):
        bad=copy.deepcopy(self.data);bad['frames']=bad['frames'][::3]
        self.assertIn('duration_s',self.failures(bad))
        self.assertIn('angular_speed_deg_s',self.failures(bad))

    def test_single_frame_pop_is_rejected(self):
        bad=copy.deepcopy(self.data);bad['frames'][90]['pelvis'][2]+=.08
        self.assertIn('joint_acceleration_m_s2',self.failures(bad))

    def test_mid_turn_pause_is_rejected_even_with_zero_velocity(self):
        bad=copy.deepcopy(self.data)
        for i in range(62,67):bad['frames'][i]=copy.deepcopy(bad['frames'][61])
        self.assertIn('turnover_interior_speed_deg_s',self.failures(bad))

    def test_sliding_wrist_and_missing_grip_are_rejected(self):
        bad=copy.deepcopy(self.data);bad['frames'][90]['wrist_l'][0]+=.02
        self.assertIn('wrist_l_contact_error_m',self.failures(bad))
        bad=copy.deepcopy(self.data);bad['bar_grip']['rotate_with_hand']=False
        self.assertIn('rolling_grip',self.failures(bad))

    def test_rotating_grip_preserves_cylinder_clearance(self):
        self.assertGreater(grip_clearance(self.data),.0005)
        distances=[]
        for frame in self.data['frames']:
            segments=wrapped_segments(frame,'r',self.data['bar_grip'])
            distances.append([math.hypot(p[0],p[2]-2.3) for pair in segments for p in pair])
        for values in zip(*distances):self.assertLess(max(values)-min(values),.00002)

    def test_body_bar_intersection_is_rejected_between_endpoints(self):
        bad=copy.deepcopy(self.data)
        bad['frames'][100]['spine']=[0,0,2.3]
        self.assertIn('body_bar_volume_clearance_m',self.failures(bad))


if __name__=='__main__':unittest.main()
