"""Movement-identity regressions, using the actual pre-fix poses as negative controls."""
import copy
import gzip
import json
from pathlib import Path
import sys
import unittest

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools/blender'))
import poses
from motion_quality import audit
from motion_constraints import bisect_target,planted_ankle


class MotionQualityTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        poses.load_families()
        cls.data={ex:poses.build(ex)[0] for ex in ('pullup-full','pushup-archer')}

    def test_both_full_repetitions_pass_geometry_and_identity(self):
        for ex,data in self.data.items():
            self.assertEqual([],poses.build(ex)[1]);self.assertEqual([],poses.checks(data))
            self.assertEqual('pass',audit(data)['status'],audit(data)['failures'])

    def test_actual_old_archer_is_rejected(self):
        with gzip.open(ROOT/'tests/fixtures/motion/pushup-archer-before.json.gz') as f:old=json.load(f)
        failures={v['check'] for v in audit(old)['failures']}
        self.assertIn('lateral_range_m',failures)
        self.assertIn('wrist_l_drift_m',failures)

    def test_actual_old_vertical_pullup_is_rejected(self):
        with gzip.open(ROOT/'tests/fixtures/motion/pullup-full-before.json.gz') as f:old=json.load(f)
        failures={v['check'] for v in audit(old)['failures']}
        self.assertIn('top_back_tilt_deg',failures);self.assertIn('shoulder_action_m',failures)

    def test_normal_pushup_cannot_pass_as_an_archer(self):
        wrong=poses.build('pushup-standard')[0];wrong['id']='pushup-archer'
        self.assertEqual('fail',audit(wrong)['status'])

    def test_bending_the_assisting_arm_is_detected_independently_of_shift(self):
        bad=copy.deepcopy(self.data['pushup-archer'])
        for frame in bad['frames']:
            side='l' if frame['chest'][1]<0 else 'r'
            frame['elbow_'+side][2]+=.20
        self.assertTrue(any('support_elbow' in f['check'] for f in audit(bad)['failures']))

    def test_wrong_grip_and_sliding_hands_are_detected(self):
        bad=copy.deepcopy(self.data['pullup-full'])
        bad['frames'][10]['palmn_r']=[-1,0,0]
        bad['frames'][12]['wrist_l'][0]+=.05
        failures={v['check'] for v in audit(bad)['failures']}
        self.assertIn('palm_r_forward',failures);self.assertIn('wrist_l_drift_m',failures)

    def test_teleporting_joint_is_detected(self):
        bad=copy.deepcopy(self.data['pullup-full']);bad['frames'][30]['ankle_l'][0]+=1
        self.assertIn('max_joint_speed_m_s',{v['check'] for v in audit(bad)['failures']})

    def test_unknown_exercises_are_unscoped_not_approved(self):
        self.assertEqual('unscoped',audit({'id':'unreviewed'})['status'])

    def test_unreachable_constraints_are_rejected(self):
        with self.assertRaises(ValueError):bisect_target(lambda x:x*x,2,3,1)
        with self.assertRaises(ValueError):planted_ankle([10,0,.5],[0,0,.02],[0,0,.08])

    def test_no_fake_success_for_nonfinite_geometry(self):
        bad=copy.deepcopy(self.data['pullup-full']);bad['frames'][0]['chest'][0]=float('nan')
        self.assertEqual('fail',audit(bad)['status'])


if __name__=='__main__':unittest.main()
