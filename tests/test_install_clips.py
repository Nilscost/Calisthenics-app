"""Focused safety checks for the Blender asset installer."""
import contextlib
import io
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest import mock

from tools.blender import install_clips


class InstallClipsTests(unittest.TestCase):
    def make_frames(self, root, ex="test-exercise", count=2):
        work = Path(root)
        source = work / ex / "blender" / "frames"
        source.mkdir(parents=True)
        joints = work / ex / "joints.json"
        joints.write_text(json.dumps({"frames": [{} for _ in range(count)]}))
        joints_ns = joints.stat().st_mtime_ns
        for i in range(count):
            path = source / f"{i:04d}.png"
            path.write_bytes(b"frame")
            os.utime(path, ns=(joints_ns + 2_000_000_000, joints_ns + 2_000_000_000))
        return work, source, joints

    def test_catalog_variations_include_new_exercises(self):
        ids = install_clips.catalog_ids()
        self.assertIn("jefferson-curl", ids)
        self.assertIn("german-hang", ids)

        with tempfile.TemporaryDirectory() as work:
            output = io.StringIO()
            with mock.patch.object(install_clips, "WORK", work), contextlib.redirect_stdout(output):
                result = install_clips.main(["jefferson-curl", "--dry"])
            self.assertEqual(result, 1)
            self.assertIn("jefferson-curl: missing joints.json", output.getvalue())
            self.assertNotIn("not a catalog exercise id", output.getvalue())

    def test_rejects_missing_or_noncontiguous_frames(self):
        with tempfile.TemporaryDirectory() as root:
            work, source, _ = self.make_frames(root)
            with mock.patch.object(install_clips, "WORK", str(work)):
                self.assertIsNone(install_clips.frame_problem("test-exercise"))
                (source / "0001.png").unlink()
                self.assertIn("frame names", install_clips.frame_problem("test-exercise"))
                (source / "0001.png").write_bytes(b"frame")
                (source / "0002.png").write_bytes(b"extra")
                self.assertIn("frame names", install_clips.frame_problem("test-exercise"))

    def test_rejects_frames_older_than_joints(self):
        with tempfile.TemporaryDirectory() as root:
            work, source, joints = self.make_frames(root, count=1)
            frame = source / "0000.png"
            joints_ns = joints.stat().st_mtime_ns
            os.utime(frame, ns=(joints_ns - 2_000_000_000, joints_ns - 2_000_000_000))
            with mock.patch.object(install_clips, "WORK", str(work)):
                self.assertIn("not newer than joints.json", install_clips.frame_problem("test-exercise"))

    def run_mocked_install(self, root, output_size, dry=False, encode_error=False):
        work, _, _ = self.make_frames(root, count=1)
        demos = Path(root) / "assets" / "demos"
        thumbs = Path(root) / "assets" / "thumbs"
        demos.mkdir(parents=True)
        thumbs.mkdir(parents=True)
        mp4 = demos / "test-exercise.mp4"
        webp = thumbs / "test-exercise.webp"
        mp4.write_bytes(b"old clip")
        webp.write_bytes(b"old thumb")

        class Frame:
            def save(self, path):
                Path(path).write_bytes(b"encoded input frame")

        def fake_encode(command, check):
            if encode_error:
                raise subprocess.CalledProcessError(1, command)
            Path(command[-1]).write_bytes(b"x" * output_size)

        def fake_thumb(ex, path):
            Path(path).write_bytes(b"new thumb")

        patches = [
            mock.patch.object(install_clips, "WORK", str(work)),
            mock.patch.object(install_clips, "DEMOS", str(demos)),
            mock.patch.object(install_clips, "THUMBS", str(thumbs)),
            mock.patch.object(install_clips, "flat", return_value=Frame()),
            mock.patch.object(install_clips, "thumb", side_effect=fake_thumb),
            mock.patch.object(install_clips.subprocess, "run", side_effect=fake_encode),
        ]
        with contextlib.ExitStack() as stack:
            for p in patches:
                stack.enter_context(p)
            result = install_clips.install("test-exercise", dry=dry)
        return result, mp4.read_bytes(), webp.read_bytes(), sorted(p.name for p in demos.iterdir()), sorted(p.name for p in thumbs.iterdir())

    def test_successful_encode_replaces_both_files_from_staging(self):
        with tempfile.TemporaryDirectory() as root:
            _, mp4, webp, demo_names, thumb_names = self.run_mocked_install(root, 32)
            self.assertEqual(mp4, b"x" * 32)
            self.assertEqual(webp, b"new thumb")
            self.assertEqual(demo_names, ["test-exercise.mp4"])
            self.assertEqual(thumb_names, ["test-exercise.webp"])

    def test_over_cap_keeps_both_shipped_files(self):
        with tempfile.TemporaryDirectory() as root:
            size, mp4, webp, _, _ = self.run_mocked_install(root, install_clips.CAP)
            self.assertEqual(size[0], install_clips.CAP)
            self.assertEqual(mp4, b"old clip")
            self.assertEqual(webp, b"old thumb")

    def test_encoder_failure_keeps_both_shipped_files(self):
        with tempfile.TemporaryDirectory() as root:
            with self.assertRaises(subprocess.CalledProcessError):
                self.run_mocked_install(root, 32, encode_error=True)
            mp4 = Path(root) / "assets" / "demos" / "test-exercise.mp4"
            webp = Path(root) / "assets" / "thumbs" / "test-exercise.webp"
            self.assertEqual(mp4.read_bytes(), b"old clip")
            self.assertEqual(webp.read_bytes(), b"old thumb")

    def test_dry_run_does_not_replace_shipped_files(self):
        with tempfile.TemporaryDirectory() as root:
            _, mp4, webp, _, _ = self.run_mocked_install(root, 32, dry=True)
            self.assertEqual(mp4, b"old clip")
            self.assertEqual(webp, b"old thumb")


if __name__ == "__main__":
    unittest.main()
