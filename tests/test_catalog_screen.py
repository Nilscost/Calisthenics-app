"""Negative controls for catalog screening; screening never approves form."""
import copy
import json
from pathlib import Path
import sys
import tempfile
import unittest
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools/blender'))
import poses
from catalog_screen import trajectory,media,screen
from build_review_gallery import build

class CatalogScreenTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        poses.load_families();cls.data=poses.build('pullup-full')[0]

    def test_bad_coordinate_is_an_error(self):
        data=copy.deepcopy(self.data);data['frames'][5]['pelvis'][0]=float('nan')
        self.assertEqual('invalid_geometry',trajectory(data,[])['flags'][0]['check'])

    def test_joint_jump_and_loop_jump_are_review_flags(self):
        data=copy.deepcopy(self.data);data['frames'][5]['ankle_l'][0]+=1
        data['frames'][-1]['ankle_r'][0]+=.2
        flags=trajectory(data,[])['flags']
        self.assertTrue({'high_joint_speed','large_loop_step'}<={f['check'] for f in flags})
        self.assertTrue(all(f['severity']=='review' for f in flags))

    def test_missing_video_is_an_error(self):
        with tempfile.TemporaryDirectory() as tmp:
            self.assertEqual('missing_video',media(Path(tmp)/'missing.mp4')['flags'][0]['check'])

    def test_valid_installed_video_decodes_and_remains_unapproved(self):
        record=screen({'id':'pullup-full','name':'Pull-up'})
        self.assertEqual(96,record['media']['decoded_frames'])
        self.assertEqual('not_approved',record['form_status'])
        self.assertNotEqual('error',record['technical_status'])

    def test_review_gallery_rejects_stale_video_before_writing(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);source=root/'screen.json';output=root/'gallery.html'
            source.write_text(json.dumps({'records':[{'id':'ring-muscle-up','media':{'sha256':'stale'}}]}))
            with self.assertRaisesRegex(ValueError,'changed after screening'):
                build(source,output)
            self.assertFalse(output.exists())

if __name__=='__main__':unittest.main()
