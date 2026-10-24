"""Regressions for the reported intermediate stops and finger/bar intersections."""
import copy
import gzip
import json
from pathlib import Path
import sys
import unittest
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools/blender'))
import poses
from bar_grip import wrapped_segments,segment_clearance,grip_clearance
from motion_quality import audit


class PullupSmoothGripTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        poses.load_families();cls.data=poses.build('pullup-full')[0]

    def test_no_stops_in_middle_of_pull_or_descent(self):
        self.assertTrue(audit(self.data)['metrics']['interior_speed_min_m_s']['ok'])
        # Reproduce the actual previous interpolation while retaining all poses.
        spec=poses.pullup();old=copy.deepcopy(self.data)
        old['frames']=[poses.solve(poses.sample(spec['keys'],i/96),{}) for i in range(96)]
        self.assertFalse(audit(old)['metrics']['interior_speed_min_m_s']['ok'])

    def test_internal_keys_have_continuous_velocity_and_no_overshoot(self):
        keys=poses.pullup()['keys'];eps=1e-6
        for t,_ in keys[1:-1]:
            a=poses.sample_continuous(keys,t-eps)['pelvis'][2]
            b=poses.sample_continuous(keys,t)['pelvis'][2]
            c=poses.sample_continuous(keys,t+eps)['pelvis'][2]
            self.assertAlmostEqual((b-a)/eps,(c-b)/eps,delta=.001)
        for i in range(1001):
            z=poses.sample_continuous(keys,i/1000)['pelvis'][2]
            self.assertTrue(1.201-1e-9<=z<=1.70+1e-9)

    def test_actual_previous_surging_clip_is_rejected(self):
        with gzip.open(ROOT/'tests/fixtures/motion/pullup-surging-before.json.gz') as f:
            previous=json.load(f)
        metrics=audit(previous)['metrics']
        self.assertFalse(metrics['rise_vertical_speed_ratio']['ok'])
        self.assertFalse(metrics['descent_vertical_speed_ratio']['ok'])

    def test_steady_main_travel_in_both_directions(self):
        metrics=audit(self.data)['metrics']
        for direction in ('rise','descent'):
            self.assertLess(metrics[direction+'_vertical_speed_ratio']['value'],1.003)
            self.assertLess(metrics[direction+'_torso_speed_ratio']['value'],1.08)

    def test_pacing_eases_only_at_ends_without_position_or_velocity_jumps(self):
        eps=1e-6
        for t in (.12,.88):
            a,b,c=[poses.steady_progress(t+d) for d in (-eps,0,eps)]
            self.assertAlmostEqual((b-a)/eps,(c-b)/eps,delta=.0001)
        for t in (.2,.4,.6,.8):
            speed=(poses.steady_progress(t+eps)-poses.steady_progress(t-eps))/(2*eps)
            self.assertAlmostEqual(speed,1/.88,places=6)
        self.assertAlmostEqual(poses.steady_progress(eps)/eps,0,delta=.0001)
        self.assertAlmostEqual((1-poses.steady_progress(1-eps))/eps,0,delta=.0001)

    def test_every_finger_and_thumb_capsule_clears_bar_in_all_frames(self):
        self.assertGreater(grip_clearance(self.data),.0005)
        for f in self.data['frames']:
            for side in ('l','r'):
                self.assertEqual(14,len(wrapped_segments(f,side,self.data['bar_grip'])))

    def test_capsule_chord_collision_is_detected_with_clear_endpoints(self):
        bar={'z':0,'r':.017}
        self.assertLess(segment_clearance((-.04,0,0),(.04,0,0),bar,.0088),0)
        bad=copy.deepcopy(self.data)
        bad['bar_grip']['r']=.07
        self.assertFalse(audit(bad)['metrics']['finger_bar_clearance_m']['ok'])

    def test_special_grip_is_scoped_to_pullup(self):
        self.assertNotIn('bar_grip',poses.build('pushup-archer')[0])
        self.assertNotIn('bar_grip',poses.build('chinup-full')[0])

if __name__=='__main__':unittest.main()
