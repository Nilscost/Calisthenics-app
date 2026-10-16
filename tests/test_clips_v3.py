"""U11: the clip generator's own consistency checks (every catalog id has a pose, every muscle has a body region).
Needs Pillow; skipped where it is missing (the Mac system Python has none, the build sandbox does)."""
import pathlib, subprocess, sys, unittest
import importlib.util

ROOT = pathlib.Path(__file__).resolve().parent.parent

try:
    import PIL  # noqa: F401
    HAVE_PIL = True
except ImportError:
    HAVE_PIL = False


class ClipsV3(unittest.TestCase):
    @unittest.skipUnless(HAVE_PIL, "Pillow not installed")
    def test_generator_checks_pass(self):
        r = subprocess.run([sys.executable, "-B", str(ROOT / "tools" / "gen_demo_clips_v3.py"), "--check"], capture_output=True, text=True)
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertIn("checks passed", r.stdout)

    @unittest.skipUnless(HAVE_PIL, "Pillow not installed")
    def test_missing_r8_blender_pose_is_rejected(self):
        script = ROOT / "tools" / "gen_demo_clips_v3.py"
        spec = importlib.util.spec_from_file_location("gen_demo_clips_v3_test", script)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        errors = module.pose_coverage_errors(
            {"legacy", "new-r8"}, {"new-r8"}, {"legacy"}, set(),
        )
        self.assertIn("no canonical Blender pose for R8 catalog id: new-r8", errors)

    @unittest.skipUnless(HAVE_PIL, "Pillow not installed")
    def test_catalog_pose_coverage_accepts_each_pipeline_and_blender_only_exception(self):
        script = ROOT / "tools" / "gen_demo_clips_v3.py"
        spec = importlib.util.spec_from_file_location("gen_demo_clips_v3_coverage_test", script)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        errors = module.pose_coverage_errors(
            {"legacy", "new-r8", "jefferson-curl"}, {"new-r8"},
            {"legacy"}, {"new-r8", "jefferson-curl", "german-hang"},
        )
        self.assertEqual([], errors)

    @unittest.skipUnless(HAVE_PIL, "Pillow not installed")
    def test_legacy_render_selection_excludes_canonical_ids_and_rejects_explicit_r8(self):
        script = ROOT / "tools" / "gen_demo_clips_v3.py"
        spec = importlib.util.spec_from_file_location("gen_demo_clips_v3_selection_test", script)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        self.assertEqual(
            ["legacy"],
            module.legacy_render_selection(
                ["legacy", "new-r8", "jefferson-curl"], {"new-r8"}, {"legacy", "new-r8"},
            ),
        )
        with self.assertRaisesRegex(ValueError, "canonical Blender pipeline"):
            module.legacy_render_selection(
                ["legacy", "new-r8"], {"new-r8"}, {"legacy", "new-r8"}, ["new-r8"],
            )


if __name__ == "__main__":
    unittest.main()
