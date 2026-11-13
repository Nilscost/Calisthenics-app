"""Regressions for missing rings and the reported forward lurch."""
import copy,gzip,json,math
from pathlib import Path
import sys,unittest
ROOT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(ROOT/'tools/blender'))
import poses
from motion_quality import audit
from ring_geometry import ring_frame,hand_segments,torus_clearance,capsule_ring_clearance,strap_attachment

class RingMuscleupTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        poses.load_families();cls.data=poses.build('ring-muscle-up')[0]

    def test_full_cycle_has_visible_rings_and_passes_motion_checks(self):
        self.assertEqual([],poses.build('ring-muscle-up')[1]);self.assertEqual([],poses.checks(self.data))
        self.assertEqual('pass',audit(self.data)['status'],audit(self.data)['failures'])
        self.assertEqual(2,sum(p['kind']=='ring' for p in self.data['props']))

    def test_actual_old_missing_rings_and_lurch_are_rejected(self):
        with gzip.open(ROOT/'tests/fixtures/motion/ring-muscle-up-before.json.gz') as f:old=json.load(f)
        failures={v['check'] for v in audit(old)['failures']}
        self.assertTrue({'visible_rings_count','chest_fore_aft_m','chest_forward_speed_m_s','leg_tuck_change_l_m'}<=failures)

    def test_removing_ring_geometry_is_detected(self):
        bad=copy.deepcopy(self.data);bad['props']=[]
        self.assertIn('visible_rings_count',{v['check'] for v in audit(bad)['failures']})

    def test_forward_lurch_is_detected_independently_of_ring_visibility(self):
        bad=copy.deepcopy(self.data);bad['frames'][40]['chest'][0]+=.35
        checks={v['check'] for v in audit(bad)['failures']}
        self.assertTrue({'chest_fore_aft_m','chest_forward_speed_m_s'}<=checks)

    def test_rings_remain_at_grip_and_fingers_clear_the_actual_torus(self):
        for spec in self.data['props']:
            side=spec['joint'][-1]
            length=math.dist(spec['anchor'],strap_attachment(self.data['frames'][0],spec))
            for frame in self.data['frames']:
                self.assertLess(abs(length-math.dist(spec['anchor'],strap_attachment(frame,spec))),.003)
                for i,(a,b) in enumerate(hand_segments(frame,side,spec)):
                    for t in range(17):
                        p=[x+(y-x)*t/16 for x,y in zip(a,b)]
                        self.assertGreater(torus_clearance(p,frame,spec)-(.0105 if i>=12 else .0088),.0005)

    def test_torus_distance_detects_contact_not_just_its_bounding_box(self):
        spec=self.data['props'][0];frame=self.data['frames'][0];centre,n,tangent,_=ring_frame(frame,spec)
        self.assertGreater(torus_clearance(centre,frame,spec),0) # The hole is empty.
        p=[x+y*spec['radius'] for x,y in zip(centre,tangent)]
        self.assertLess(torus_clearance(p,frame,spec),0) # The wood is solid.

    def test_full_arm_volumes_clear_rings_between_mesh_vertices(self):
        for frame in self.data['frames']:
            for spec in self.data['props']:
                side=spec['joint'][-1]
                for a,b,r1,r2 in [('shoulder','elbow',.052,.042),('elbow','wrist',.04,.03)]:
                    self.assertGreater(capsule_ring_clearance(frame[a+'_'+side],frame[b+'_'+side],r1,r2,frame,spec),.0005)

    def test_old_bottom_contact_intersects_arm_despite_sparse_vertices(self):
        with gzip.open(ROOT/'tests/fixtures/motion/ring-muscle-up-wrist-before.json.gz') as source:old=json.load(source)
        frame=max(old['frames'],key=lambda f:f['chest'][2]);bad_spec=copy.deepcopy(old['props'][0])
        bad_spec['grip_angle_deg']=0
        self.assertLess(capsule_ring_clearance(frame['shoulder_l'],frame['elbow_l'],.052,.042,frame,bad_spec),0)
        bad=copy.deepcopy(old)
        for spec in bad['props']:spec['grip_angle_deg']=0
        self.assertFalse(audit(bad)['metrics']['arm_ring_volume_clearance_m']['ok'])

    def test_actual_old_frozen_hand_axis_fails_wrist_realism_gate(self):
        with gzip.open(ROOT/'tests/fixtures/motion/ring-muscle-up-wrist-before.json.gz') as source:old=json.load(source)
        failures={v['check'] for v in audit(old)['failures']}
        self.assertIn('wrist_l_bend_deg',failures)
        self.assertIn('wrist_r_bend_deg',failures)

    def test_ring_plane_stays_independent_of_the_hand_pitch(self):
        data=copy.deepcopy(self.data);original=ring_frame(data['frames'][0],data['props'][0])
        data['frames'][0]['palmn_l']=[1,0,0]
        actual=ring_frame(data['frames'][0],data['props'][0])
        self.assertEqual(original,actual)

    def test_sliding_hand_contact_is_rejected_even_when_wrist_drift_is_allowed(self):
        bad=copy.deepcopy(self.data);bad['frames'][40]['wrist_l'][0]+=.025
        self.assertIn('wrist_l_contact_error_m',{v['check'] for v in audit(bad)['failures']})


if __name__=='__main__':unittest.main()
