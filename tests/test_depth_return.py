"""Independent fault controls for the depth-jump reset feedback."""
import copy,gzip,json,sys,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools/blender'))
import poses
from generation_checks.depth_return import audit
from generation_checks.depth_jump import audit as physics
class DepthReturnTests(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  poses.load_families();cls.data,cls.reach=poses.build('depth-jump')
 def failures(self,d):return {f['check'] for f in audit(d)['failures']}
 def test_candidate_faces_travel_and_keeps_planted_soles_fixed(self):
  self.assertFalse(self.reach);self.assertEqual([],audit(self.data)['failures'])
 def test_actual_old_shuffle_and_lower_rebound_are_rejected(self):
  old=json.load(gzip.open(ROOT/'tests/fixtures/motion/depth-jump-walk-before.json.gz','rt'))
  self.assertIn('return_travel_alignment_min',self.failures(old))
  self.assertIn('rebound_pelvis_rise_m',{f['check'] for f in physics(old)['failures']})
 def test_backward_facing_travel_is_rejected(self):
  d=copy.deepcopy(self.data)
  for f in d['frames'][52:70]:f['pelvis_left']=[-v for v in f['pelvis_left']]
  self.assertIn('return_travel_alignment_min',self.failures(d))
 def test_sliding_and_twisting_support_foot_are_rejected(self):
  d=copy.deepcopy(self.data);d['frames'][55]['ankle_l'][0]+=.025
  self.assertIn('return_planted_sole_slip_m',self.failures(d))
  d=copy.deepcopy(self.data);f=d['frames'][55];f['toe_l']=[f['ankle_l'][0],f['ankle_l'][1]+.2,.08]
  self.assertIn('return_planted_sole_twist_deg',self.failures(d))
 def test_missing_reverse_facing_is_rejected(self):
  d=copy.deepcopy(self.data)
  for f in d['frames']:f['pelvis_left']=[0,1,0];f['pelvis_up']=[0,0,1]
  self.assertIn('return_reverse_facing_min_x',self.failures(d))
if __name__=='__main__':unittest.main()
