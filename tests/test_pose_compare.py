import math
from pathlib import Path
import sys
import unittest
import argparse
import contextlib
import io
import json
import tempfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'tools/blender'))
import hand_axes
import pose_compare
import poses


class HandDirectionTests(unittest.TestCase):
    def test_supination_reverses_thumb_on_each_anatomical_hand(self):
        for side in ('l','r'):
            _,n1,t1=hand_axes.axes([0,0,0],[0,0,1],[1,0,0],side)
            _,n2,t2=hand_axes.axes([0,0,0],[0,0,1],[-1,0,0],side)
            self.assertEqual(t1,[-x for x in t2])
            self.assertEqual(n1,[-x for x in n2])
        self.assertLess(hand_axes.axes([0,0,0],[0,0,1],[1,0,0],'l')[2][1],0)

    def test_downward_degenerate_normal_still_has_orthogonal_axes(self):
        u,n,t=hand_axes.axes([0,0,0],[0,0,-1],[0,0,-1],'r')
        for a,b in ((u,n),(u,t),(n,t)):
            self.assertAlmostEqual(0,sum(x*y for x,y in zip(a,b)))
        for v in (u,n,t):self.assertAlmostEqual(1,math.sqrt(sum(x*x for x in v)))

    def test_actual_chinup_and_pullup_frames_have_opposite_grip_and_detail(self):
        poses.load_families()
        for exercise,sign in [('pullup-full',1),('chinup-full',-1)]:
            data,errors=poses.build(exercise)
            self.assertEqual([],errors)
            self.assertEqual([],poses.checks(data))
            self.assertIn('grip_detail',data)
            for fr in data['frames']:
                for side in ('l','r'):
                    self.assertEqual(sign,fr['palmn_'+side][0])
                    self.assertLess(sign*fr['wrist_'+side][0],0)


class ComparisonTests(unittest.TestCase):
    def test_stale_reference_camera_projections_are_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);joints=root/'joints.json';joints.write_text('{"current":true}')
            projection=root/'projection.json';projection.write_text(json.dumps({'space':'image_2d','source_joints_sha256':'obsolete','frames':[]}))
            analysis=root/'analysis.json';analysis.write_text(json.dumps({'space':'image_2d','interval':[0,1],'frames':[]}))
            refs=[dict(video_id=str(i),url='https://example.org/'+str(i),exercises=['pullup-full'],
                       analysis={'pullup-full':str(analysis)},candidate_projection={'pullup-full':str(projection)},
                       review={'pullup-full':{'interval':[0,1],'view_comparable':True}}) for i in range(3)]
            manifest=root/'manifest.json';manifest.write_text(json.dumps({'references':refs}));output=root/'report.json'
            args=argparse.Namespace(candidate=str(projection),joints=str(joints),manifest=str(manifest),exercise='pullup-full',output=str(output))
            with contextlib.redirect_stdout(io.StringIO()):pose_compare.compare(args)
            self.assertEqual({'stale_candidate_projection'},{r['status'] for r in json.loads(output.read_text())['references']})

    def trajectory(self,values):
        return [dict(elbow_l=v,elbow_r=v) for v in values]

    def test_phase_alignment_handles_different_rep_timing(self):
        a=self.trajectory([170,170,120,60,120,170])
        b=self.trajectory([170,120,60,60,120,170])
        result=pose_compare.align(a,b)
        self.assertEqual(0,result['mean_deg'])
        self.assertEqual(1,result['path_coverage'])

    def test_bad_range_of_motion_is_measured(self):
        result=pose_compare.align(self.trajectory([170,120,60,120,170]),self.trajectory([170,150,140,150,170]))
        self.assertGreater(result['joints']['elbow_l']['p95_deg'],60)

    def test_missing_observations_never_become_a_pass(self):
        result=pose_compare.align([{}]*6,[{}]*6)
        self.assertEqual(0,result['path_coverage'])
        self.assertEqual(90,result['mean_deg'])

    def test_missing_joints_are_not_fabricated(self):
        self.assertEqual({},pose_compare.features({'wrist_l':[1,2]}))
        self.assertIsNone(pose_compare.angle([0,0],[0,0],[1,0]))

    def test_angles_use_pixels_to_preserve_aspect_ratio(self):
        self.assertAlmostEqual(90,pose_compare.angle([1,0],[0,0],[0,2]))

    def test_three_distinct_sources_required_and_stale_intervals_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp)
            candidate=root/'candidate.json';candidate.write_text(json.dumps({'space':'image_2d','frames':[]}))
            analysis=root/'reference.json';analysis.write_text(json.dumps({'space':'image_2d','interval':[0,1],'frames':[]}))
            refs=[dict(video_id=str(i),url='https://example.org/'+str(i),exercises=['pullup-full'],
                       analysis={'pullup-full':str(analysis)},review={'pullup-full':{'interval':[1,2]}}) for i in range(3)]
            manifest=root/'manifest.json';manifest.write_text(json.dumps({'references':refs}))
            output=root/'report.json'
            args=argparse.Namespace(candidate=str(candidate),manifest=str(manifest),exercise='pullup-full',output=str(output))
            with contextlib.redirect_stdout(io.StringIO()):pose_compare.compare(args)
            report=json.loads(output.read_text())
            self.assertEqual('requires_visual_review',report['status'])
            self.assertEqual({'stale_or_incompatible_analysis'},{r['status'] for r in report['references']})
            refs[2]['video_id']='0';manifest.write_text(json.dumps({'references':refs}))
            with self.assertRaisesRegex(ValueError,'3–5'):pose_compare.compare(args)


if __name__=='__main__':unittest.main()
