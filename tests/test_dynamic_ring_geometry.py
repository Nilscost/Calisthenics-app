"""Independent geometry regressions for moving, independently turning rings."""
import copy
import gzip
import json
import math
from pathlib import Path
import sys
import unittest

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools/blender'))
from ring_geometry import ring_frame,ring_outline,hand_segments,strap_attachment,torus_clearance,palm_surface_clearance,flexed_hand_axes


class DynamicRingGeometryTests(unittest.TestCase):
    def setUp(self):
        with gzip.open(ROOT/'tests/fixtures/motion/ring-articulation-before.json.gz','rt') as f:
            data=json.load(f)
        self.frame=copy.deepcopy(data['frames'][0]);self.spec=copy.deepcopy(data['props'][0])
        centre,normal,_,_=ring_frame(self.frame,self.spec)
        self.frame.update(ring_center_l=centre,ring_plane_l_up=normal,ring_radial_l_up=[0,0,1])
        self.spec.update(frame_center='ring_center_l',frame_normal='ring_plane_l_up',frame_radial='ring_radial_l_up')

    def test_capsule_endpoints_follow_motion_without_scaling_cap_thickness(self):
        from ring_geometry import capsule_vertex_z
        for length in (.02,.04,.08):
            self.assertEqual(-.0088,capsule_vertex_z(-.0088,.05,length))
            self.assertAlmostEqual(length,capsule_vertex_z(.05,.05,length))
            self.assertAlmostEqual(length+.008,capsule_vertex_z(.058,.05,length))
            self.assertAlmostEqual(length*.5,capsule_vertex_z(.025,.05,length))
        with self.assertRaises(ValueError):capsule_vertex_z(.01,0,.04)

    def test_dynamic_center_takes_precedence_over_legacy_static_center(self):
        self.frame['ring_center_l']=[.2,.3,2.4]
        self.assertEqual([.2,.3,2.4],ring_frame(self.frame,self.spec)[0])
        self.assertAlmostEqual(2.4-self.spec['radius'],ring_frame(self.frame,self.spec)[3][2])

    def test_outline_uses_actual_independent_ring_plane(self):
        n=[math.sin(.4),-math.cos(.4),0]
        self.frame['ring_plane_l_up']=n
        centre=ring_frame(self.frame,self.spec)[0]
        for p in ring_outline(self.frame,self.spec):
            delta=[a-b for a,b in zip(p,centre)]
            self.assertAlmostEqual(0,sum(a*b for a,b in zip(delta,n)),places=10)
            self.assertAlmostEqual(self.spec['radius']+self.spec['tube'],math.dist(p,centre),places=10)
        before=ring_frame(self.frame,self.spec)
        self.frame['palmn_l']=[1,0,0]
        self.assertEqual(before,ring_frame(self.frame,self.spec))

    def test_whole_fingers_translate_with_ring_and_hand(self):
        before=hand_segments(self.frame,'l',self.spec);delta=[.06,-.02,.01]
        for k in ('wrist_l','hand_l','ring_center_l'):
            self.frame[k]=[a+b for a,b in zip(self.frame[k],delta)]
        after=hand_segments(self.frame,'l',self.spec)
        for pair_a,pair_b in zip(before,after):
            for a,b in zip(pair_a,pair_b):
                self.assertLess(math.dist([x+y for x,y in zip(a,delta)],b),1e-10)

    def test_turning_palm_routes_every_finger_around_actual_wood(self):
        fixture=json.loads((ROOT/'tests/fixtures/motion/ring-turned-palm.json').read_text())
        frame,spec=fixture['frame'],fixture['spec']
        for i,(a,b) in enumerate(hand_segments(frame,'r',spec)):
            radius=.0105 if i>=12 else .0088
            minimum=min(torus_clearance([x+(y-x)*t/64 for x,y in zip(a,b)],frame,spec)-radius
                        for t in range(65))
            # Torus distance is 1-Lipschitz; protect the unsampled half-steps.
            self.assertGreater(minimum-math.dist(a,b)/128,.0005)

    def test_palm_surface_catches_intrusion_despite_exact_grip_point(self):
        fixture=json.loads((ROOT/'tests/fixtures/motion/ring-palm-penetration-before.json').read_text())
        frame,spec=fixture['frame'],fixture['spec']
        self.assertLess(palm_surface_clearance(frame,spec),-.01)
        side=spec['joint'][-1];normal=ring_frame(frame,spec)[1]
        for name in ('wrist_'+side,'hand_'+side):
            frame[name]=[x+.20*y for x,y in zip(frame[name],normal)]
        self.assertGreater(palm_surface_clearance(frame,spec),.10)

    def test_forearm_relative_false_grip_stays_continuous_through_horizontal(self):
        previous=None
        for degrees in range(0,181,2):
            t=math.radians(degrees);fore=[math.sin(t),0,math.cos(t)]
            hand,palm=flexed_hand_axes(fore,70)
            self.assertAlmostEqual(0,sum(a*b for a,b in zip(hand,palm)))
            self.assertAlmostEqual(math.cos(math.radians(70)),sum(a*b for a,b in zip(hand,fore)))
            self.assertAlmostEqual(-math.sin(math.radians(70)),sum(a*b for a,b in zip(palm,fore)))
            if previous:self.assertLess(math.dist(previous,palm),.04)
            previous=palm
        _,palm=flexed_hand_axes([0,0,1],70)
        self.assertLess(palm[2],-.9)
        with self.assertRaises(ValueError):flexed_hand_axes([0,1,0],70)

    def test_strap_attaches_to_highest_hoop_point_even_when_tilted(self):
        normal=[.6,0,.8];self.frame['ring_plane_l_up']=normal
        self.frame['ring_radial_l_up']=[.8,0,-.6]
        p=strap_attachment(self.frame,self.spec);centre=ring_frame(self.frame,self.spec)[0]
        self.assertAlmostEqual(self.spec['radius'],math.dist(p,centre))
        self.assertAlmostEqual(0,sum((a-b)*n for a,b,n in zip(p,centre,normal)))
        self.assertAlmostEqual(centre[2]+.6*self.spec['radius'],p[2])
        self.assertAlmostEqual(-self.spec['tube'],torus_clearance(p,self.frame,self.spec))


if __name__=='__main__':unittest.main()
