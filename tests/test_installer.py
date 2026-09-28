import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'tools'))
from install_local import install


class InstallerTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name).resolve()
        self.source = self.root / 'source'
        self.dest = self.root / 'dest'
        self.source.mkdir()
        (self.source / 'README.md').write_text('test-only package\n')
        self.manifest = {'version': 1, 'files': [{'path': 'README.md', 'sha256': hashlib.sha256((self.source / 'README.md').read_bytes()).hexdigest()}]}
        self.save_manifest()

    def save_manifest(self):
        (self.source / 'PACKAGE-MANIFEST.json').write_text(json.dumps(self.manifest))

    def test_copy_and_idempotent_repeat(self):
        self.assertEqual(1, install(self.source, self.dest, initialize_git=False))
        self.assertEqual((self.source / 'README.md').read_bytes(), (self.dest / 'README.md').read_bytes())
        self.assertEqual(1, install(self.source, self.dest, initialize_git=False))

    def test_conflict_preserves_user_edit(self):
        self.dest.mkdir(); (self.dest / 'README.md').write_text('user edit')
        with self.assertRaisesRegex(ValueError, 'refusing overwrite'):
            install(self.source, self.dest, initialize_git=False)
        self.assertEqual('user edit', (self.dest / 'README.md').read_text())
        self.assertFalse((self.dest / 'PACKAGE-MANIFEST.json').exists())

    def test_tampered_source_refused_before_target_creation(self):
        (self.source / 'README.md').write_text('tampered')
        with self.assertRaisesRegex(ValueError, 'Source hash mismatch'):
            install(self.source, self.dest, initialize_git=False)
        self.assertFalse(self.dest.exists())

    def test_traversal_refused(self):
        self.manifest['files'][0]['path'] = '../outside'
        self.save_manifest()
        with self.assertRaisesRegex(ValueError, 'Unsafe'):
            install(self.source, self.dest, initialize_git=False)
        self.assertFalse(self.dest.exists())

    def test_symlink_destination_refused(self):
        real = self.root / 'real'; real.mkdir(); self.dest.symlink_to(real, target_is_directory=True)
        with self.assertRaisesRegex(ValueError, 'symlink'):
            install(self.source, self.dest, initialize_git=False)
        self.assertEqual([], list(real.iterdir()))

    def test_duplicate_manifest_entry_refused(self):
        self.manifest['files'].append(dict(self.manifest['files'][0])); self.save_manifest()
        with self.assertRaisesRegex(ValueError, 'Duplicate'):
            install(self.source, self.dest, initialize_git=False)
        self.assertFalse(self.dest.exists())


if __name__ == '__main__':
    unittest.main()
