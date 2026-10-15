"""T03: the R8 importer must be deterministic and preserve strict catalog contracts."""
import json
import pathlib
import subprocess
import sys
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
CONTRACT_PATH = ROOT / "docs/23-new-exercises.json"
IMPORTER = ROOT / "tools/gen_r8_catalog.py"


class R8CatalogGeneratorTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        result = subprocess.run(
            [sys.executable, str(IMPORTER)], cwd=ROOT, text=True, capture_output=True
        )
        if result.returncode:
            raise AssertionError(f"R8 importer failed:\n{result.stdout}\n{result.stderr}")
        cls.contract = json.loads(CONTRACT_PATH.read_text())
        cls.catalog = json.loads((ROOT / "app/src/main/assets/catalog.json").read_text())
        cls.content_catalog = json.loads((ROOT / "content/starter/catalog.json").read_text())
        cls.clip_meta = json.loads((ROOT / "app/src/main/assets/clip_meta.json").read_text())
        cls.meta = json.loads((ROOT / "app/src/main/assets/catalog_meta.json").read_text())
        cls.review = json.loads((ROOT / "work/r8-import-review.json").read_text())

    def test_content_source_and_runtime_asset_are_identical(self):
        self.assertEqual(self.content_catalog, self.catalog)
        self.assertEqual(len(self.contract["exercises"]), 379)
        self.assertTrue((ROOT / "content/starter/catalog_meta.json").is_file())
        self.assertEqual(self.meta, json.loads((ROOT / "content/starter/catalog_meta.json").read_text()))

    def test_every_strength_exercise_has_a_typed_five_tier_policy(self):
        by_id = {v["id"]: v for v in self.catalog["variations"]}
        policies = {p["variationId"]: p for p in self.catalog["policies"]}
        for exercise in self.contract["exercises"]:
            if exercise["kind"] not in {"REPS", "HOLD"}:
                continue
            variation = by_id[exercise["id"]]
            policy = policies[exercise["id"]]
            self.assertEqual(variation["progressionPolicyId"], policy["id"])
            self.assertEqual([1, 2, 3, 4, 5], [tier["index"] for tier in policy["tiers"]])
            expected = "HOLD_SECONDS" if exercise["kind"] == "HOLD" else "REPS"
            self.assertEqual({expected}, {tier["target"]["type"] for tier in policy["tiers"]})
            self.assertTrue(all(t["workWindowSeconds"] > 0 for t in policy["tiers"]))

    def test_only_the_six_confirmed_explosive_pushups_have_hard_cap_three(self):
        policies = {p["variationId"]: p for p in self.catalog["policies"]}
        expected = {
            "pushup-pop-up-incline", "pushup-kneeling-push-off", "pushup-pop-up",
            "pushup-pop-up-wide-close", "pushup-clap", "pushup-chest-strike",
        }
        actual = {variation_id for variation_id, policy in policies.items()
                  if policy.get("maxRepsPerSet") is not None}
        self.assertEqual(expected, actual)
        for exercise_id in expected:
            self.assertEqual(3, policies[exercise_id]["maxRepsPerSet"])
        for exercise in self.contract["exercises"]:
            if exercise["familyId"] in {"plyo", "plyo-lateral", "plyo-box"}:
                policy = policies[exercise["id"]]
                self.assertNotIn("maxRepsPerSet", policy)
                self.assertEqual(5, len(policy["tiers"]))
                self.assertEqual(sorted(t["target"]["value"] for t in policy["tiers"]),
                                 [t["target"]["value"] for t in policy["tiers"]])
        policies = {p["variationId"]: p for p in self.catalog["policies"]}
        self.assertEqual([1, 2, 3, 3, 3], [t["target"]["value"] for t in policies["depth-jump"]["tiers"]])
        self.assertEqual([3, 4, 5, 6, 8], [t["target"]["value"] for t in policies["pogo-hop"]["tiers"]])

    def test_prerequisites_are_only_catalog_exercises_and_routine_ids_resolve(self):
        catalog_ids = {v["id"] for v in self.catalog["variations"]}
        policies = {p["variationId"]: p for p in self.catalog["policies"]}
        for exercise in self.contract["exercises"]:
            policy = policies.get(exercise["id"])
            if not policy:
                continue
            expected = set(([exercise["insertAfter"]] if exercise.get("insertAfter") else []) + exercise.get("alsoRequires", []))
            actual = {
                pred["variationTierMet"]["variationId"]
                for group in policy.get("prerequisiteRule", {}).get("allOf", [])
                for pred in group if "variationTierMet" in pred
            }
            self.assertEqual(expected, actual, exercise["id"])
            self.assertTrue(actual <= catalog_ids)
            self.assertFalse(any("equipmentAvailable" in pred for group in policy.get("prerequisiteRule", {}).get("allOf", []) for pred in group))
        for program in self.catalog["fixedPrograms"]:
            self.assertTrue(all(item["exerciseId"] in catalog_ids for item in program["items"]))

    def test_reviewed_power_gates_and_stretch_reachability(self):
        by_id = {p["variationId"]: p for p in self.catalog["policies"]}
        plyo_ids = {e["id"] for e in self.contract["exercises"] if e["familyId"].startswith("plyo")}
        for exercise_id in plyo_ids:
            prereqs = {
                pred["variationTierMet"]["variationId"]
                for group in by_id[exercise_id].get("prerequisiteRule", {}).get("allOf", [])
                for pred in group if "variationTierMet" in pred
            }
            self.assertIn("squat-air", prereqs, exercise_id)
        capped = {exercise_id for exercise_id, p in by_id.items() if p.get("maxRepsPerSet") == 3}
        for exercise_id in capped:
            prereqs = {
                pred["variationTierMet"]["variationId"]
                for group in by_id[exercise_id].get("prerequisiteRule", {}).get("allOf", [])
                for pred in group if "variationTierMet" in pred
            }
            self.assertIn("pushup-standard", prereqs, exercise_id)
        reachable = {sid for v in self.catalog["variations"] for sid in v.get("compatibleStretchIds", [])}
        stretch_ids = {v["id"] for v in self.catalog["variations"] if v["kind"] == "STRETCH"}
        self.assertEqual(stretch_ids, reachable & stretch_ids)
        variations = {v["id"]: v for v in self.catalog["variations"]}
        for exercise in self.contract["exercises"]:
            if exercise["kind"] not in {"REPS", "HOLD"}:
                continue
            links = [variations[sid] for sid in variations[exercise["id"]].get("compatibleStretchIds", [])]
            self.assertTrue(any(s.get("stretchUse") != "COOL_DOWN" and not s.get("optIn", False) for s in links), exercise["id"])

    def test_imported_progression_edges_are_present_and_acyclic(self):
        policies = {p["variationId"]: p for p in self.catalog["policies"]}
        children = {}
        for exercise in self.contract["exercises"]:
            predecessor = exercise.get("insertAfter")
            if predecessor and exercise["kind"] in {"REPS", "HOLD"}:
                children.setdefault(predecessor, set()).add(exercise["id"])
        for parent, expected in children.items():
            actual = set(policies[parent].get("nextVariationIds", []))
            self.assertTrue(expected <= actual, f"{parent} missing imported children {expected - actual}")

        graph = {key: policy.get("nextVariationIds", []) for key, policy in policies.items()}
        visiting, visited = set(), set()

        def visit(node):
            if node in visiting:
                self.fail(f"progression cycle includes {node}")
            if node in visited:
                return
            visiting.add(node)
            for child in graph.get(node, []):
                visit(child)
            visiting.remove(node)
            visited.add(node)

        for node in graph:
            visit(node)

    def test_all_catalog_ids_have_clip_metadata_and_new_meta_uses_contract_muscles(self):
        catalog_ids = {v["id"] for v in self.catalog["variations"]}
        self.assertEqual(catalog_ids, set(self.clip_meta))
        for exercise in self.contract["exercises"]:
            self.assertEqual(exercise["clipKind"], self.clip_meta[exercise["id"]]["kind"])
            self.assertEqual(exercise.get("primaryMuscles", []), self.clip_meta[exercise["id"]]["primary"])
            self.assertEqual(exercise.get("secondaryMuscles", []), self.clip_meta[exercise["id"]]["secondary"])
        self.assertEqual(self.contract["equipmentNew"], self.meta["equipment"])
        self.assertEqual(self.contract["familiesNew"], self.meta["families"])
        self.assertEqual(self.contract["sourcesNew"], self.meta["sources"])
        self.assertEqual(len(self.catalog["fixedPrograms"]), 8)

    def test_aliases_options_programs_groups_and_reviewed_edges_are_imported(self):
        variations = {v["id"]: v for v in self.catalog["variations"]}
        self.assertEqual("German Hang", variations["german-hang"]["name"])
        for alias in self.contract["aliases"]:
            self.assertIn(alias["alias"], variations[alias["existingId"]]["aliases"])
        self.assertEqual(self.contract["options"], self.catalog["optionSets"])
        self.assertEqual(self.contract["routines"], self.catalog["fixedPrograms"])
        self.assertEqual("pushup-feet-elevated", self.catalog["exerciseGroups"][0]["mainVariationId"])
        self.assertEqual(["pushup-diamond"], self.catalog["exerciseGroups"][0]["alternativeVariationIds"])
        policies = {p["variationId"]: p for p in self.catalog["policies"]}
        self.assertEqual("pushup-feet-elevated", policies["pushup-standard"]["mainSuccessorId"])
        for entry in self.review["progressionEdges"]:
            policy = policies[entry["parentId"]]
            old_edges = entry["existingNextVariationIds"]
            self.assertEqual(old_edges, policy.get("nextVariationIds", [])[:len(old_edges)])
            if old_edges and entry["selectionReason"] == "preserved-existing-main":
                self.assertEqual(old_edges[0], policy["mainSuccessorId"])
        standard = next(entry for entry in self.review["progressionEdges"] if entry["parentId"] == "pushup-standard")
        self.assertEqual("owner-confirmed-comparable-exercise-group", standard["selectionReason"])
        self.assertTrue(any(entry["reviewRequired"] for entry in self.review["progressionEdges"]))
        self.assertTrue(any(line.startswith("flag-press:") for line in self.review["targetProfileExceptions"]))

    def test_catalog_files_stay_under_preflight_limit_and_import_is_repeatable(self):
        paths = [
            ROOT / "content/starter/catalog.json",
            ROOT / "app/src/main/assets/catalog.json",
            ROOT / "content/starter/catalog_meta.json",
            ROOT / "app/src/main/assets/catalog_meta.json",
            ROOT / "app/src/main/assets/clip_meta.json",
            ROOT / "work/r8-import-review.json",
        ]
        self.assertTrue(all(path.stat().st_size < 1024 * 1024 for path in paths),
                        [(str(path), path.stat().st_size) for path in paths if path.stat().st_size >= 1024 * 1024])
        first = {path: path.read_bytes() for path in paths}
        subprocess.run([sys.executable, str(IMPORTER)], cwd=ROOT, check=True, capture_output=True)
        self.assertEqual(first, {path: path.read_bytes() for path in paths})


if __name__ == "__main__":
    unittest.main()
