"""Regression controls for feet fixed to world space and missing press return."""
import copy,gzip,json,math,sys,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools/blender'))
import poses
from motion_quality import audit
from articulation_review import review,bend


class StraddleFeetTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        poses.load_families();cls.data,cls.reach=poses.build('handstand-press-straddle')

    def failures(self,data):return {v['check'] for v in audit(data)['failures']}

    def test_feet_and_closed_press_cycle_pass_geometry_and_foot_contract(self):
        self.assertEqual([],self.reach);self.assertEqual([],poses.checks(self.data))
        self.assertEqual('pass',audit(self.data)['status'],audit(self.data)['failures'])

    def test_actual_old_fixed_world_feet_and_reset_are_rejected(self):
        with gzip.open(ROOT/'tests/fixtures/motion/handstand-press-straddle-before.json.gz') as f:old=json.load(f)
        failures=self.failures(old)
        self.assertTrue({'r_foot_turn_deg','r_top_toe_shin_dot','loop_gap_m'}<=failures)

    def test_fixed_world_toes_fail_independently_of_body_and_timing(self):
        bad=copy.deepcopy(self.data)
        for f in bad['frames']:
            for side in ('l','r'):f['toe_'+side]=[f['ankle_'+side][0]+.2,*f['ankle_'+side][1:]]
        failures=self.failures(bad)
        self.assertIn('l_top_toe_shin_dot',failures);self.assertIn('r_foot_turn_deg',failures)
        self.assertNotIn('loop_gap_m',failures)

    def test_single_frame_toe_flip_is_rejected(self):
        bad=copy.deepcopy(self.data);f=bad['frames'][65]
        f['toe_r']=[2*a-t for a,t in zip(f['ankle_r'],f['toe_r'])]
        self.assertIn('r_foot_angular_speed_deg_s',self.failures(bad))

    def test_collinear_heel_frame_is_rejected(self):
        bad=copy.deepcopy(self.data);f=bad['frames'][65]
        v=poses.unit(poses.sub(f['ankle_r'],f['knee_r']))
        f['toe_r']=[a+.2*b for a,b in zip(f['ankle_r'],v)]
        self.assertIn('r_heel_frame_norm',self.failures(bad))

    def test_degenerate_toe_axis_is_reported_without_crashing(self):
        bad=copy.deepcopy(self.data);bad['frames'][65]['toe_r']=bad['frames'][65]['ankle_r'][:]
        self.assertIn('r_foot_axis_valid',self.failures(bad))

    def test_hand_slide_is_still_rejected(self):
        bad=copy.deepcopy(self.data);bad['frames'][65]['wrist_l'][0]+=.05
        self.assertIn('wrist_l_drift_m',self.failures(bad))

    def test_return_retraces_foot_direction_and_pose(self):
        fs=self.data['frames']
        for i in range(1,len(fs)):
            for name in ('pelvis','ankle_l','ankle_r','toe_l','toe_r'):
                self.assertLess(math.dist(fs[i][name],fs[-i][name]),.0001)

    def test_wrist_measurement_distinguishes_straight_bent_and_backwards(self):
        self.assertEqual(0,bend([0,0,0],[0,0,1],[0,0,2]))
        self.assertEqual(90,bend([0,0,0],[0,0,1],[1,0,1]))
        self.assertEqual(180,bend([0,0,0],[0,0,1],[0,0,0]))

    def test_articulation_report_cannot_approve_form(self):
        data=copy.deepcopy(self.data);f=data['frames'][65]
        f['hand_r']=f['elbow_r'][:]
        result=review(data)
        self.assertFalse(result['form_approval']);self.assertEqual('needs_human_review',result['status'])
        self.assertGreater(result['wrists']['r']['max_bend_deg'],100)
        self.assertEqual(65,result['wrists']['r']['max_bend_frame'])

    def test_degenerate_hand_axis_remains_explicit(self):
        bad=copy.deepcopy(self.data);bad['frames'][0]['hand_r']=bad['frames'][0]['wrist_r'][:]
        self.assertIn('error',review(bad)['wrists']['r'])


if __name__=='__main__':unittest.main()
