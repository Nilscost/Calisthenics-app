"""Doc 43 lane L6 tooling: reference fetch/sheet, side-by-side sheet, movement-class checks."""
import copy
import json
import math
from pathlib import Path
import sys
import tempfile
import unittest
from types import SimpleNamespace

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools/blender'))
import motion_quality
import movement_class
import poses
import reference_fetch
import reference_sheet
import side_by_side
from poses import base_pose, both, rep, sample_continuous

ENTRY = dict(video_id='AAAAAAAAAAA', url='https://www.youtube.com/watch?v=AAAAAAAAAAA', creator='Coach',
             interval=[12.5, 15.0], variant='long-lever plank', view='side')


# ---- a tiny synthetic exercise (bodyweight squat) the class checker can pass -------------
def _squat_key(down):
    z = .955 - .42 * down
    return base_pose(pelvis=[-.12 * down, 0, z], p1=12 * down, p2=22 * down, hp=-8 * down,
                     **both(ankle=[0.0, .10, .08 + .0], knee=[1, 0, 0], foot=[1, .08, 0],
                            wrist_rel=[.30 * down + .02, .04, -.40 - .1 * (1 - down)]))


def _squat_spec(pauses=True):
    top, bottom = _squat_key(0), _squat_key(1)
    if pauses:
        keys = rep(top, bottom, hold_top=.12, down_end=.45, hold_bottom=.15)
        keys = keys + [(1.0, top)]
    else:
        keys = [(0.0, top), (.5, bottom), (1.0, top)]
    return dict(seconds=4.0, keys=keys, cam_az=30, cam_el=10, props=[], interpolation='continuous')


CONTRACT = {'schema': 1, 'exercises': {'test-squat': {
    'movement_class': 'squat', 'style': 'synthetic bodyweight squat',
    'params': {'main_range': {'metric': 'knee_angle', 'sides': ['l', 'r'], 'min': 40}, 'dwell_s': .4,
               'contacts': ['toe_l', 'toe_r']},
    'identity': [{'metric': 'ankle_gap_m', 'stat': 'max', 'hi': .3}]}}}


class ClassCheckTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        poses.EXERCISES['test-squat'] = lambda: _squat_spec(True)
        poses.EXERCISES['test-squat-nopause'] = lambda: _squat_spec(False)
        cls.good, cls.msgs = poses.build('test-squat')
        cls.nopause, _ = poses.build('test-squat-nopause')
        cls.nopause['id'] = 'test-squat'

    @classmethod
    def tearDownClass(cls):
        poses.EXERCISES.pop('test-squat', None)
        poses.EXERCISES.pop('test-squat-nopause', None)

    def failed(self, data):
        report = motion_quality.audit(data, CONTRACT)
        return {f['check'] for f in report['failures']}

    def mutate(self, fn):
        data = copy.deepcopy(self.good)
        fn(data['frames'])
        return data

    def test_synthetic_squat_passes_every_shared_check(self):
        report = motion_quality.audit(self.good, CONTRACT)
        self.assertEqual('pass', report['status'], report['failures'])
        self.assertEqual('squat', report['movement_class'])
        for name in ('toe_l_slide_m', 'thigh_l_length_error_m', 'main_range_knee_angle', 'floor_clearance_m',
                     'loop_gap_m', 'max_joint_speed_m_s', 'dwell_low_s', 'ankle_gap_m_max'):
            self.assertIn(name, report['metrics'])

    def test_sliding_planted_foot_fails(self):
        def slide(frames):
            for i, f in enumerate(frames):
                f['toe_l'][0] += .0015 * i  # creeps forward 0.17 m over the clip while on the floor
        self.assertIn('toe_l_slide_m', self.failed(self.mutate(slide)))

    def test_stretched_segment_fails(self):
        def stretch(frames):
            for f in frames:
                f['knee_l'][2] += .03
        self.assertIn('thigh_l_length_error_m', self.failed(self.mutate(stretch)))

    def test_missing_range_of_main_joint_fails(self):
        def freeze(frames):
            for i in range(len(frames)):
                frames[i] = copy.deepcopy(frames[0])
        self.assertIn('main_range_knee_angle', self.failed(self.mutate(freeze)))

    def test_floor_penetration_fails(self):
        def sink(frames):
            for f in frames:
                for name in ('toe_l', 'ankle_l', 'knee_l'):
                    f[name][2] -= .2
        self.assertIn('floor_clearance_m', self.failed(self.mutate(sink)))

    def test_body_inside_a_box_prop_fails(self):
        data = copy.deepcopy(self.good)
        data['props'] = [dict(kind='box', center=data['frames'][0]['pelvis'], size=[.5, .5, .5])]
        self.assertIn('prop_box_interior_joints', self.failed(data))

    def test_body_through_a_bar_fails(self):
        data = copy.deepcopy(self.good)
        p = data['frames'][0]['knee_l']
        data['props'] = [dict(kind='rod', a=[p[0], -.5, p[2]], b=[p[0], .5, p[2]], r=.02)]
        self.assertIn('prop_rod_clearance_m', self.failed(data))

    def test_loop_seam_gap_and_jump_fail(self):
        def seam(frames):
            for f in frames[-3:]:
                f['pelvis'][0] += .15
        failed = self.failed(self.mutate(seam))
        self.assertIn('loop_gap_m', failed)

    def test_teleporting_joint_fails_speed_and_acceleration(self):
        def pop(frames):
            frames[30]['wrist_l'][0] += .4
        failed = self.failed(self.mutate(pop))
        self.assertIn('max_joint_speed_m_s', failed)
        self.assertIn('max_joint_accel_m_s2', failed)

    def test_motion_without_pauses_at_the_ends_fails_dwell(self):
        self.assertTrue(self.failed(self.nopause) & {'dwell_low_s', 'dwell_high_s'})

    def test_identity_check_rejects_the_wrong_variant(self):
        bad = copy.deepcopy(CONTRACT)
        bad['exercises']['test-squat']['identity'] = [{'metric': 'ankle_gap_m', 'stat': 'max', 'hi': .05}]
        report = motion_quality.audit(self.good, bad)
        self.assertEqual('fail', report['status'])
        self.assertIn('ankle_gap_m_max', {f['check'] for f in report['failures']})

    def test_unknown_class_is_a_configuration_error_not_a_pass(self):
        bad = copy.deepcopy(CONTRACT)
        bad['exercises']['test-squat']['movement_class'] = 'levitation'
        with self.assertRaises(KeyError):
            motion_quality.audit(self.good, bad)

    def test_repo_contract_classes_exist(self):
        contract = json.loads((ROOT / 'tools/blender/motion_contracts.json').read_text())
        for ex, d in contract['exercises'].items():
            if d.get('movement_class'):
                self.assertIn(d['movement_class'], movement_class.CLASS_DEFAULTS, ex)
                for chk in d.get('identity', []):
                    self.assertIn(chk['metric'], movement_class.FRAME_METRICS, ex)
                mr = d.get('params', {}).get('main_range')
                if mr:
                    self.assertIn(mr['metric'], movement_class.FRAME_METRICS, ex)


class FetchTests(unittest.TestCase):
    def test_cache_key_depends_on_url_and_interval(self):
        a = reference_fetch.cache_key(ENTRY['url'], [12.5, 15])
        self.assertEqual(a, reference_fetch.cache_key(ENTRY['url'], [12.5, 15]))
        self.assertNotEqual(a, reference_fetch.cache_key(ENTRY['url'], [12.5, 16]))
        self.assertNotEqual(a, reference_fetch.cache_key(ENTRY['url'].replace('AAAAAAAAAAA', 'BBBBBBBBBBB'), [12.5, 15]))

    def test_command_downloads_only_the_interval_at_480p_or_less_without_audio(self):
        cmd = reference_fetch.build_command(ENTRY, '/tmp/x.mp4')
        self.assertIn('--download-sections', cmd)
        self.assertEqual('*12.5-15', cmd[cmd.index('--download-sections') + 1])
        self.assertIn('480', cmd[cmd.index('-f') + 1])
        self.assertNotIn('ba', cmd[cmd.index('-f') + 1].split('/')[0].replace('bv*', ''))

    def test_bad_entries_are_rejected(self):
        for bad in (dict(ENTRY, url='https://evil.example/x'), dict(ENTRY, interval=[5, 5]), dict(ENTRY, interval=[0, 60]),
                    dict(ENTRY, creator='')):
            with self.assertRaises(ValueError):
                reference_fetch.validate_entry(bad)

    def test_dry_run_downloads_nothing(self):
        called = []
        with tempfile.TemporaryDirectory() as tmp:
            r = reference_fetch.fetch(ENTRY, tmp, dry_run=True, run=lambda *a, **k: called.append(a))
            self.assertEqual([], called)
            self.assertFalse(Path(r['path']).exists())
            self.assertIsNone(r['sha256'])

    def test_fetch_caches_and_hashes_then_reuses(self):
        calls = []
        def fake_run(cmd, **kw):
            calls.append(cmd)
            Path(cmd[cmd.index('-o') + 1]).write_bytes(b'video-bytes')
            return SimpleNamespace(returncode=0, stderr='')
        with tempfile.TemporaryDirectory() as tmp:
            r1 = reference_fetch.fetch(ENTRY, tmp, run=fake_run)
            r2 = reference_fetch.fetch(ENTRY, tmp, run=fake_run)
            self.assertEqual(1, len(calls))
            self.assertTrue(r2['cached'])
            self.assertEqual(r1['sha256'], r2['sha256'])
            self.assertEqual(64, len(r1['sha256']))

    def test_hash_is_recorded_into_the_references_file(self):
        data = {'batch_references': {'x': [dict(ENTRY)]}}
        reference_fetch.record_hash(data, 'x', 0, 'ab' * 32)
        self.assertEqual('ab' * 32, data['batch_references']['x'][0]['sha256'])

    def test_failed_download_raises(self):
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaises(RuntimeError):
                reference_fetch.fetch(ENTRY, tmp, run=lambda *a, **k: SimpleNamespace(returncode=1, stderr='boom'))

    def test_repo_references_are_well_formed_and_hold_no_media(self):
        data = reference_fetch.load()
        for ex, entries in data.get('batch_references', {}).items():
            self.assertIn(len(entries), (2, 3), ex)
            for e in entries:
                reference_fetch.validate_entry(e)
                for key in ('variant', 'view', 'video_id'):
                    self.assertTrue(e.get(key), (ex, key))


class SheetTests(unittest.TestCase):
    def test_six_to_eight_evenly_spaced_frames_excluding_the_repeat_of_the_start(self):
        t = reference_sheet.frame_times(2.4, 8)
        self.assertEqual(8, len(t))
        self.assertAlmostEqual(0.0, t[0])
        self.assertAlmostEqual(2.1, t[-1])
        self.assertTrue(all(abs((b - a) - .3) < 1e-9 for a, b in zip(t, t[1:])))
        for bad in (5, 9):
            with self.assertRaises(ValueError):
                reference_sheet.frame_times(2, bad)

    def test_angle_table_is_na_when_joints_are_not_visible(self):
        pts = {n: (0, 0) for n in ('shoulder_l', 'elbow_l', 'wrist_l')}
        row = reference_sheet.angle_table(pts)
        self.assertEqual('n/a', row['knee'])
        self.assertEqual('n/a', row['trunk'])
        low = reference_sheet.angle_table({'shoulder_l': (0, 0), 'elbow_l': (1, 0), 'wrist_l': (1, 1)}, {'shoulder_l': .1, 'elbow_l': 1, 'wrist_l': 1})
        self.assertEqual('n/a', low['elbow'])

    def test_angle_table_values(self):
        pts = {'shoulder_l': (0, 0), 'elbow_l': (1, 0), 'wrist_l': (1, 1),
               'hip_l': (0, 2), 'knee_l': (1, 2), 'ankle_l': (1, 3),
               'shoulder_r': (0, 0), 'hip_r': (0, 2)}
        row = reference_sheet.angle_table(pts)
        self.assertEqual('90', row['elbow'])
        self.assertEqual('90', row['knee'])
        self.assertEqual('0', row['trunk'])

    def test_compose_writes_a_labelled_png_with_na_table(self):
        from PIL import Image
        with tempfile.TemporaryDirectory() as tmp:
            frames = []
            for i in range(6):
                p = Path(tmp) / ('f%d.png' % i)
                Image.new('RGB', (320, 240), (i * 30, 90, 200)).save(p)
                frames.append(p)
            out = reference_sheet.compose(frames, ['%d' % i for i in range(6)], 'title', Path(tmp) / 's.png')
            im = Image.open(out)
            self.assertGreater(im.width, 400)
            self.assertGreater(im.height, 300)


class SideBySideTests(unittest.TestCase):
    def test_rows_must_have_matching_column_counts(self):
        with self.assertRaises(ValueError):
            side_by_side.compose([('a', [None] * 3), ('b', [None] * 4)], 4, '/tmp/never.png')

    def test_candidate_and_reference_rows_share_the_phase_columns(self):
        from PIL import Image
        with tempfile.TemporaryDirectory() as tmp:
            tmp = Path(tmp)
            cand, ref = tmp / 'cand', tmp / 'ex-ref1-frames'
            cand.mkdir()
            ref.mkdir()
            for i in range(8):
                Image.new('RGB', (640, 480), (200, 20, 20)).save(cand / ('c%02d.png' % i))
                Image.new('RGB', (480, 270), (20, 20, 200)).save(ref / ('f%02d.png' % i))
            out = side_by_side.build('ex', cand, [ref], tmp / 'out.png')
            im = Image.open(out)
            self.assertEqual(150 + 8 * side_by_side.CELL[0], im.width)
            self.assertEqual(34 + 2 * (side_by_side.CELL[1] + 4), im.height)
            with self.assertRaises(FileNotFoundError):
                side_by_side.build('ex', tmp / 'nope', [ref], tmp / 'o2.png')


if __name__ == '__main__':
    unittest.main()
