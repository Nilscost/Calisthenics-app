"""R8 imported flexibility drills are manual picks, never automatic recovery."""
import importlib.util
import json
import pathlib
import sys
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
SPEC = importlib.util.spec_from_file_location("gen_r8_catalog", ROOT / "tools/gen_r8_catalog.py")
GEN = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(GEN)


class R8StretchOptInTest(unittest.TestCase):
    def test_every_new_stretch_and_mobility_contract_row_is_opt_in(self):
        contract = json.loads((ROOT / "docs/23-new-exercises.json").read_text())
        entries = [row for row in contract["exercises"] if row["kind"] in {"STRETCH", "MOBILITY"}]
        self.assertTrue(entries)
        for exercise in entries:
            with self.subTest(exercise=exercise["id"]):
                self.assertTrue(GEN._exercise(exercise)["optIn"])

    def test_strength_entries_do_not_get_stretch_opt_in_by_default(self):
        contract = json.loads((ROOT / "docs/23-new-exercises.json").read_text())
        strength = next(row for row in contract["exercises"] if row["kind"] in {"REPS", "HOLD"})
        self.assertNotIn("optIn", GEN._exercise(strength))


if __name__ == "__main__":
    unittest.main()
