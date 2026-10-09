"""U11: the clip generator's own consistency checks (every catalog id has a pose, every muscle has a body region).
Needs Pillow; skipped where it is missing (the Mac system Python has none, the build sandbox does)."""
import pathlib, subprocess, sys, unittest

ROOT = pathlib.Path(__file__).resolve().parent.parent

try:
    import PIL  # noqa: F401
    HAVE_PIL = True
except ImportError:
    HAVE_PIL = False


class ClipsV3(unittest.TestCase):
    @unittest.skipUnless(HAVE_PIL, "Pillow not installed")
    def test_generator_checks_pass(self):
        r = subprocess.run([sys.executable, str(ROOT / "tools" / "gen_demo_clips_v3.py"), "--check"], capture_output=True, text=True)
        self.assertEqual(r.returncode, 0, r.stderr)
        self.assertIn("checks passed", r.stdout)


if __name__ == "__main__":
    unittest.main()
