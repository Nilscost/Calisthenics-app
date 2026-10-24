import copy
import gzip
import json
from pathlib import Path
import sys
import unittest
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools/blender'))
import poses
from generation_checks.next_pilates import audit

class NextPilatesTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        poses.load_families();cls.data,cls.reach=poses.build('pilates-saw')
    def test_saw_keeps_long_planted_legs_and_smooth_bilateral_reaches(self):
        self.assertEqual([],self.reach);self.assertEqual([],poses.checks(self.data))
        self.assertEqual([],audit(self.data)['failures'])
    def test_old_saw_exposes_abrupt_arm_switch_and_moving_heels(self):
        with gzip.open(ROOT/'tests/fixtures/motion/pilates-saw-before.json.gz','rt') as f:old=json.load(f)
        bad={f['check'] for f in audit(old)['failures']}
        self.assertIn('saw_joint_speed_m_s',bad);self.assertIn('saw_l_heel_drift_m',bad)
    def test_freezing_rotation_does_not_pass_by_smoothness_alone(self):
        bad=copy.deepcopy(self.data)
        for f in bad['frames']:f['chest_left']=[0,1,0]
        self.assertIn('saw_left_rotation',{f['check'] for f in audit(bad)['failures']})
