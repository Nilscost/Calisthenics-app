import copy
import fcntl
import json
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "tools/blender"))
import render_batch


class FakeProcess:
    pid = 987654
    def __init__(self, code): self.code = code; self.terminated = False
    def wait(self, *args, **kwargs): return self.code
    def terminate(self): self.terminated = True
    def kill(self): self.terminated = True


class RenderBatchTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.data, cls.reach, cls.report, cls.stamp, cls.pose_stamp, cls.pipeline_stamp = render_batch._source_state("ring-muscle-up")

    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.directory = Path(self.tmp.name) / "isolated-review"
        self.directory.mkdir()
        self.source = (copy.deepcopy(self.data), [], copy.deepcopy(self.report), copy.deepcopy(self.stamp),
                       copy.deepcopy(self.pose_stamp), copy.deepcopy(self.pipeline_stamp))

    def tearDown(self): self.tmp.cleanup()

    def patch_source(self):
        return patch.object(render_batch, "_source_state", return_value=copy.deepcopy(self.source))

    def fake_popen(self, *, preflight_status="pass", preflight_stamp=True, preflight_code=0, render_code=7):
        calls = []
        def launch(command, **kwargs):
            calls.append(command)
            if str(render_batch.GEOMETRY_CHECK) in command:
                output = Path(command[command.index("--output") + 1])
                manifest = json.loads((self.directory / "manifest.json").read_text())
                first = manifest["items"][manifest["order"][0]]
                report = {"status": preflight_status,
                          "pipeline_source_stamp": first["pipeline_source_stamp"] if preflight_stamp else {"different": "stamp"},
                          "items": {key: {"data_sha256": value["data_sha256"]} for key, value in manifest["items"].items()}}
                output.write_text(json.dumps(report))
                return FakeProcess(preflight_code)
            return FakeProcess(render_code)
        return calls, launch

    def test_prepare_stages_only_after_all_checks_pass(self):
        with self.patch_source():
            manifest = render_batch.prepare(self.directory, ["ring-muscle-up"])
        joints = self.directory / "staged/ring-muscle-up/joints.json"
        self.assertTrue(joints.is_file())
        self.assertEqual(manifest["items"]["ring-muscle-up"]["state"], "prepared")
        self.assertTrue(manifest["items"]["ring-muscle-up"]["requires_render_detail_stamp"])

    def test_stale_source_is_rejected_before_spawning_renderer(self):
        original = copy.deepcopy(self.source)
        with patch.object(render_batch, "_source_state", return_value=original):
            render_batch.prepare(self.directory, ["ring-muscle-up"])
        changed = copy.deepcopy(original)
        changed[0]["frames"][1]["chest"][0] += 0.0001
        with patch.object(render_batch, "_source_state", return_value=changed), \
             patch.object(render_batch.subprocess, "Popen") as popen:
            result = render_batch.run(self.directory, blender="fake-blender")
        self.assertEqual(result, 1); popen.assert_not_called()
        item = json.loads((self.directory / "manifest.json").read_text())["items"]["ring-muscle-up"]
        self.assertEqual(item["state"], "stale")

    def test_each_id_keeps_its_own_source_stamp_and_requested_order(self):
        def per_id(ex_id):
            values = copy.deepcopy(self.source)
            values = list(values); values[4] = {"family": ex_id}; return tuple(values)
        with patch.object(render_batch, "_source_state", side_effect=per_id):
            manifest = render_batch.prepare(self.directory, ["pullup-pullover", "ring-muscle-up"])
        self.assertEqual(manifest["order"], ["pullup-pullover", "ring-muscle-up"])
        self.assertEqual(manifest["items"]["pullup-pullover"]["pose_source_stamp"], {"family": "pullup-pullover"})
        self.assertEqual(manifest["items"]["ring-muscle-up"]["pose_source_stamp"], {"family": "ring-muscle-up"})
        calls, launch = self.fake_popen()
        with patch.object(render_batch, "_source_state", side_effect=per_id), \
             patch.object(render_batch.subprocess, "Popen", side_effect=launch) as popen:
            self.assertEqual(render_batch.run(self.directory, blender="fake-blender"), 1)
        self.assertEqual(len(calls), 3)
        first_command = popen.call_args_list[1].args[0]
        self.assertEqual(first_command[-1], "pullup-pullover")
        self.assertIn("--python-exit-code", first_command)
        self.assertEqual(popen.call_args_list[2].args[0][-1], "ring-muscle-up")
        saved = json.loads((self.directory / "manifest.json").read_text())
        self.assertEqual(saved["order"], ["pullup-pullover", "ring-muscle-up"])

    def test_changed_delegated_pipeline_source_stops_batch(self):
        def per_id(ex_id):
            values = copy.deepcopy(self.source)
            return values
        with patch.object(render_batch, "_source_state", side_effect=per_id):
            render_batch.prepare(self.directory, ["ring-muscle-up", "pullup-pullover"])
        changed = copy.deepcopy(self.source)
        changed = list(changed); changed[5] = {"tools/blender/ring_geometry.py": "changed"}; changed = tuple(changed)
        with patch.object(render_batch, "_source_state", return_value=changed), \
             patch.object(render_batch.subprocess, "Popen") as popen:
            self.assertEqual(render_batch.run(self.directory, blender="fake-blender"), 1)
        popen.assert_not_called()
        saved = json.loads((self.directory / "manifest.json").read_text())
        self.assertEqual(saved["items"]["ring-muscle-up"]["state"], "stale")
        self.assertEqual(saved["items"]["pullup-pullover"]["state"], "prepared")

    def test_nonzero_blender_exit_is_recorded_failed(self):
        with self.patch_source(): render_batch.prepare(self.directory, ["ring-muscle-up"])
        calls, launch = self.fake_popen()
        with self.patch_source(), patch.object(render_batch.subprocess, "Popen", side_effect=launch) as popen:
            result = render_batch.run(self.directory, blender="fake-blender")
        self.assertEqual(result, 1); self.assertEqual(popen.call_count, 2)
        item = json.loads((self.directory / "manifest.json").read_text())["items"]["ring-muscle-up"]
        self.assertEqual(item["state"], "failed")
        self.assertIn("Blender exited 7", item["error"])

    def test_duplicate_runner_lock_is_rejected(self):
        lock_path = self.directory / ".runner.lock"
        with open(lock_path, "a+") as held:
            fcntl.flock(held, fcntl.LOCK_EX | fcntl.LOCK_NB)
            with self.assertRaisesRegex(RuntimeError, "another render_batch runner"):
                render_batch.run(self.directory)
            fcntl.flock(held, fcntl.LOCK_UN)

    def test_prepare_cannot_race_an_active_batch_writer(self):
        lock_path = self.directory / ".runner.lock"
        with open(lock_path, "a+") as held:
            fcntl.flock(held, fcntl.LOCK_EX | fcntl.LOCK_NB)
            with self.assertRaisesRegex(RuntimeError, "another render_batch operation"):
                with self.patch_source(): render_batch.prepare(self.directory, ["ring-muscle-up"])
            self.assertFalse((self.directory / "manifest.json").exists())
            fcntl.flock(held, fcntl.LOCK_UN)

    def test_failed_preflight_writes_no_staged_data(self):
        bad = copy.deepcopy(self.source); bad = (bad[0], [], {"status": "fail", "failures": [{"check": "fixture"}]}, bad[3], bad[4], bad[5])
        with patch.object(render_batch, "_source_state", return_value=bad):
            with self.assertRaisesRegex(ValueError, "failed pre-render checks"):
                render_batch.prepare(self.directory, ["ring-muscle-up"])
        self.assertFalse((self.directory / "staged").exists())

    def test_native_geometry_guard_failure_prevents_every_render(self):
        with self.patch_source(): render_batch.prepare(self.directory, ["ring-muscle-up"])
        calls, launch = self.fake_popen(preflight_status="fail")
        with self.patch_source(), patch.object(render_batch.subprocess, "Popen", side_effect=launch):
            self.assertEqual(render_batch.run(self.directory, blender="fake-blender"), 1)
        self.assertEqual(len(calls), 1)
        self.assertTrue(any("check_batch_geometry.py" in part for part in calls[0]))
        self.assertTrue((self.directory / "logs/preflight.log").is_file())
        manifest = json.loads((self.directory / "manifest.json").read_text())
        self.assertEqual(manifest["state"], "failed")
        self.assertEqual(manifest["items"]["ring-muscle-up"]["state"], "prepared")

    def test_unstamped_geometry_report_prevents_every_render(self):
        with self.patch_source(): render_batch.prepare(self.directory, ["ring-muscle-up"])
        calls, launch = self.fake_popen(preflight_stamp=False)
        with self.patch_source(), patch.object(render_batch.subprocess, "Popen", side_effect=launch):
            self.assertEqual(render_batch.run(self.directory, blender="fake-blender"), 1)
        self.assertEqual(len(calls), 1)
        self.assertIn("pipeline source stamp mismatch", json.loads((self.directory / "manifest.json").read_text())["preflight_error"])

    def test_geometry_digest_mismatch_prevents_every_render(self):
        with self.patch_source(): render_batch.prepare(self.directory, ["ring-muscle-up"])
        calls, launch = self.fake_popen()
        original = launch
        def bad_digest(command, **kwargs):
            proc = original(command, **kwargs)
            if str(render_batch.GEOMETRY_CHECK) in command:
                path = Path(command[command.index("--output") + 1]); report = json.loads(path.read_text())
                report["items"]["ring-muscle-up"]["data_sha256"] = "wrong"
                path.write_text(json.dumps(report))
            return proc
        with self.patch_source(), patch.object(render_batch.subprocess, "Popen", side_effect=bad_digest):
            self.assertEqual(render_batch.run(self.directory, blender="fake-blender"), 1)
        self.assertEqual(len(calls), 1)

    def test_interrupt_during_preflight_terminates_child_and_never_renders(self):
        with self.patch_source(): render_batch.prepare(self.directory, ["ring-muscle-up"])
        interrupted = FakeProcess(0)
        def wait(*args, **kwargs): raise KeyboardInterrupt()
        interrupted.wait = wait
        calls = []
        def launch(command, **kwargs): calls.append(command); return interrupted
        with self.patch_source(), patch.object(render_batch.subprocess, "Popen", side_effect=launch):
            with self.assertRaises(KeyboardInterrupt): render_batch.run(self.directory, blender="fake-blender")
        self.assertEqual(len(calls), 1); self.assertTrue(interrupted.terminated)
        manifest = json.loads((self.directory / "manifest.json").read_text())
        self.assertEqual(manifest["state"], "interrupted")
        self.assertIsNone(manifest["child_pid"])

    def test_pid_permission_error_is_unknown_not_dead(self):
        with patch.object(render_batch.os, "kill", side_effect=PermissionError()):
            self.assertIsNone(render_batch._pid_alive(123))


if __name__ == "__main__": unittest.main()
