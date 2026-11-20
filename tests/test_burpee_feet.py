import copy,gzip,json,sys,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools/blender'))
import poses
from generation_checks.burpee_feet import audit
from generation_checks.burpee_articulation import _plank_mask

class BurpeeFootTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        poses.load_families();cls.data,cls.reach=poses.build('burpee-pushup')
        with gzip.open(ROOT/'tests/fixtures/motion/burpee-feet-flat-before.json.gz','rt') as f:cls.before=json.load(f)
    def failed(self,d):return {x['check'] for x in audit(d)['failures']}
    def test_new_candidate_has_toe_support_and_smooth_return(self):
        self.assertEqual([],self.reach);self.assertEqual([],poses.checks(self.data))
        self.assertEqual([],audit(self.data)['failures'])
    def test_actual_previous_encode_source_fails_flat_feet_and_fast_landing(self):
        self.assertTrue({'burpee_plank_toe_drop_min_m','burpee_feet_in_peak_speed_m_s','burpee_feet_in_peak_accel_m_s2'} <= self.failed(self.before))
    def test_one_frame_return_foot_pop_is_rejected(self):
        d=copy.deepcopy(self.data);end=max(i for i,x in enumerate(_plank_mask(d['frames'])) if x)
        d['frames'][end+8]['ankle_l'][0]+=.06
        self.assertIn('burpee_feet_in_peak_accel_m_s2',self.failed(d))
    def test_floating_planted_skin_is_rejected(self):
        d=copy.deepcopy(self.data)
        for i,planted in enumerate(_plank_mask(d['frames'])):
            if planted:
                for side in ('l','r'):d['frames'][i]['toe_'+side][2]+=.012
        self.assertIn('burpee_plank_skin_floor_max_m',self.failed(d))
if __name__=='__main__':unittest.main()
