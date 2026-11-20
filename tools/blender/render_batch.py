"""Isolated, sequential render runner for reviewed motion candidates.

Examples:
  python tools/blender/render_batch.py prepare --directory /absolute/review --ids ring-muscle-up
  python tools/blender/render_batch.py run --directory /absolute/review
  python tools/blender/render_batch.py status --directory /absolute/review

This runner writes only beneath the requested directory. It does not install
media into the app. Status is a process/file snapshot, not proof of render work.
"""
import argparse
import fcntl
import hashlib
import json
import math
import os
from pathlib import Path
import re
import shutil
import signal
import subprocess
import sys
import tempfile
import time

ROOT = Path(__file__).resolve().parents[2]
BLENDER = "/Applications/Blender.app/Contents/MacOS/Blender"
RENDER = ROOT / "tools/blender/render.py"
ENCODE = ROOT / "tools/blender/encode.py"
GEOMETRY_CHECK = ROOT / "tools/blender/check_batch_geometry.py"
ID_RE = re.compile(r"^[a-z0-9][a-z0-9-]*$")
BYTE_CAP = 120 * 1024


def _atomic_json(path, value):
    path = Path(path); path.parent.mkdir(parents=True, exist_ok=True)
    fd, tmp = tempfile.mkstemp(prefix="." + path.name + ".", dir=path.parent)
    try:
        with os.fdopen(fd, "w") as fh:
            json.dump(value, fh, indent=2, sort_keys=True, allow_nan=False)
            fh.write("\n"); fh.flush(); os.fsync(fh.fileno())
        os.replace(tmp, path)
    finally:
        if os.path.exists(tmp): os.unlink(tmp)


def _load(path):
    with open(path) as fh: return json.load(fh)


def _digest(path):
    h = hashlib.sha256()
    with open(path, "rb") as fh:
        for block in iter(lambda: fh.read(1024 * 1024), b""): h.update(block)
    return h.hexdigest()


def _canonical_digest(data):
    return hashlib.sha256(json.dumps(data, sort_keys=True, separators=(",", ":"), allow_nan=False).encode()).hexdigest()


def _source_state(ex_id):
    sys.path.insert(0, str(ROOT / "tools/blender"))
    import poses
    from render_contract import render_stamp
    poses.load_families()
    data, reach = poses.build(ex_id)
    from motion_quality import audit
    module = poses.EXERCISES[ex_id].__code__.co_filename
    source_files = sorted({Path(poses.__file__).resolve(), Path(module).resolve()})
    pose_stamp = {str(path.relative_to(ROOT)): _digest(path) for path in source_files}
    pipeline_stamp = _pipeline_source_stamp()
    return data, reach, audit(data), render_stamp(), pose_stamp, pipeline_stamp


def _pipeline_source_stamp():
    """Hash all Python/JSON inputs beneath tools/blender, including cached helpers."""
    root = ROOT / "tools/blender"
    files = sorted(p for p in root.rglob("*") if p.is_file() and p.suffix in (".py", ".json")
                   and "__pycache__" not in p.parts)
    return {str(path.relative_to(ROOT)): _digest(path) for path in files}


def prepare(directory, ids):
    directory = Path(directory)
    if not directory.is_absolute(): raise ValueError("--directory must be absolute")
    if not ids: raise ValueError("at least one exercise ID is required")
    if len(set(ids)) != len(ids) or any(not ID_RE.fullmatch(x) for x in ids):
        raise ValueError("IDs must be unique lowercase exercise IDs")
    directory.mkdir(parents=True, exist_ok=True)
    lock = open(directory / ".runner.lock", "a+")
    try:
        try: fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError: raise RuntimeError("another render_batch operation holds the directory lock")
        return _prepare_locked(directory, ids)
    finally:
        fcntl.flock(lock, fcntl.LOCK_UN); lock.close()


def _prepare_locked(directory, ids):
    manifest_path = directory / "manifest.json"
    if manifest_path.exists(): raise FileExistsError(f"manifest already exists: {manifest_path}")
    # Validate all candidates before writing any staged joints or starting render.
    built = []
    for ex_id in ids:
        data, reach, report, stamp, pose_stamp, pipeline_stamp = _source_state(ex_id)
        import poses
        floor = poses.checks(data)
        problems = []
        if reach: problems.append({"check": "reach", "detail": reach})
        if floor: problems.append({"check": "floor", "detail": floor})
        if report.get("status") != "pass": problems.append({"check": "motion_quality", "status": report.get("status"), "failures": report.get("failures", [])})
        if problems: raise ValueError(f"{ex_id} failed pre-render checks: {json.dumps(problems, separators=(',', ':'))}")
        built.append((ex_id, data, stamp, report, pose_stamp, pipeline_stamp))
    items = {}
    for ex_id, data, stamp, report, pose_stamp, pipeline_stamp in built:
        staged = directory / "staged" / ex_id
        staged.mkdir(parents=True, exist_ok=True)
        joints = staged / "joints.json"
        _atomic_json(joints, data)
        items[ex_id] = {
            "state": "prepared", "frames_expected": len(data["frames"]),
            "fps": data["fps"], "duration_s": len(data["frames"]) / data["fps"],
            "data_sha256": _canonical_digest(data), "joints_file_sha256": _digest(joints),
            "render_stamp": stamp, "pose_source_stamp": pose_stamp,
            "pipeline_source_stamp": pipeline_stamp,
            "requires_render_detail_stamp": True,
            "grip_detail": bool(data.get("grip_detail")),
            "motion_quality": report, "log": f"logs/{ex_id}.log"
        }
    manifest = {"schema": 1, "directory": str(directory), "created_at": time.time(),
                "state": "prepared", "own_pid": None, "child_pid": None,
                "order": list(ids), "items": items}
    _atomic_json(manifest_path, manifest)
    print(f"Prepared {len(items)} IDs in {directory}; no render started.")
    return manifest


def _signature_matches(ex_id, item):
    data, reach, report, stamp, pose_stamp, pipeline_stamp = _source_state(ex_id)
    import poses
    if reach or poses.checks(data) or report.get("status") != "pass": return False, "current source checks fail"
    if _canonical_digest(data) != item["data_sha256"]: return False, "source joints changed since prepare"
    if stamp != item["render_stamp"]: return False, "render source stamp changed since prepare"
    if pose_stamp != item["pose_source_stamp"]: return False, "pose source file changed since prepare"
    if pipeline_stamp != item["pipeline_source_stamp"]: return False, "Blender pipeline source changed since prepare"
    return True, ""


def _native_frames_ok(base, item):
    joints = base / "joints.json"; blender = base / "blender"
    frames = blender / "frames"
    expected = {f"{i:04d}.png" for i in range(item["frames_expected"])}
    if not joints.is_file() or _digest(joints) != item["joints_file_sha256"]: return False, "staged joints changed"
    try: names = {p.name for p in frames.iterdir() if p.is_file() and p.suffix == ".png"}
    except OSError: return False, "render frame directory missing"
    if names != expected: return False, f"frame set mismatch ({len(names)}/{len(expected)})"
    fresh = joints.stat().st_mtime
    if any((frames / name).stat().st_mtime <= fresh for name in expected): return False, "frames are not newer than staged joints"
    if item["requires_render_detail_stamp"]:
        try: stamp = _load(blender / "render-detail.json")
        except (OSError, ValueError): return False, "render-detail stamp missing or invalid"
        if stamp != item["render_stamp"]: return False, "render-detail stamp mismatch"
        if item.get("grip_detail"):
            detail = blender / "grip-frames"
            try: detail_names = {p.name for p in detail.iterdir() if p.is_file() and p.suffix == ".png"}
            except OSError: return False, "grip closeup frames missing"
            if detail_names != expected or any((detail / name).stat().st_mtime <= fresh for name in expected): return False, "grip closeup frame set is incomplete or stale"
    return True, ""


def _probe_video(path, expected_frames, duration):
    ffprobe = shutil.which("ffprobe")
    ffmpeg = shutil.which("ffmpeg")
    if not ffprobe or not ffmpeg: return False, "ffprobe and ffmpeg must be available on PATH", {}
    try:
        raw = subprocess.check_output([ffprobe, "-v", "error", "-count_frames", "-show_streams", "-show_format", "-of", "json", str(path)], text=True)
        info = json.loads(raw)
        videos = [s for s in info.get("streams", []) if s.get("codec_type") == "video"]
        audios = [s for s in info.get("streams", []) if s.get("codec_type") == "audio"]
        if len(videos) != 1: return False, "expected exactly one video stream", {}
        v = videos[0]
        facts = {"codec": v.get("codec_name"), "profile": v.get("profile"), "pix_fmt": v.get("pix_fmt"),
                 "width": v.get("width"), "height": v.get("height"), "frame_rate": v.get("r_frame_rate"),
                 "frames": int(v.get("nb_read_frames") or v.get("nb_frames") or 0),
                 "duration_s": float(info.get("format", {}).get("duration", 0)), "bytes": path.stat().st_size}
        required = (facts["codec"], facts["profile"], facts["pix_fmt"], facts["width"], facts["height"], facts["frame_rate"])
        if required != ("h264", "Main", "yuv420p", 480, 360, "24/1"): return False, "unexpected encoded format", facts
        if audios: return False, "audio stream present", facts
        if facts["frames"] != expected_frames: return False, "decoded frame count metadata mismatch", facts
        if abs(facts["duration_s"] - duration) > 0.05: return False, "duration mismatch", facts
        if facts["bytes"] > BYTE_CAP: return False, "video exceeds 120 KiB cap", facts
        subprocess.run([ffmpeg, "-v", "error", "-i", str(path), "-f", "null", "-"], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
        return True, "", facts
    except (OSError, ValueError, KeyError, subprocess.CalledProcessError) as exc:
        return False, f"video probe/decode failed: {exc}", {}


def _pid_alive(pid):
    if not isinstance(pid, int) or pid <= 0: return False
    try: os.kill(pid, 0); return True
    except ProcessLookupError: return False
    except PermissionError: return None
    except OSError: return None


def _batch_signatures_match(manifest):
    for ex_id in manifest.get("order", manifest["items"]):
        matches, why = _signature_matches(ex_id, manifest["items"][ex_id])
        if not matches: return False, f"{ex_id}: {why}"
    return True, ""


def _geometry_report_ok(path, manifest):
    try: report = _load(path)
    except (OSError, ValueError) as exc: return False, f"geometry report missing or invalid: {exc}"
    first_id = manifest.get("order", manifest["items"])[0]
    expected_pipeline = manifest["items"][first_id]["pipeline_source_stamp"]
    if report.get("status") != "pass": return False, f"geometry preflight status is {report.get('status')!r}"
    if report.get("pipeline_source_stamp") != expected_pipeline:
        return False, "geometry preflight pipeline source stamp mismatch"
    geometry_items = report.get("items")
    if not isinstance(geometry_items, dict): return False, "geometry preflight items missing"
    for ex_id in manifest.get("order", manifest["items"]):
        item = geometry_items.get(ex_id)
        if not isinstance(item, dict) or item.get("data_sha256") != manifest["items"][ex_id]["data_sha256"]:
            return False, f"geometry preflight data digest mismatch for {ex_id}"
    return True, ""


def status(directory):
    directory = Path(directory); manifest = _load(directory / "manifest.json")
    active = _pid_alive(manifest.get("own_pid")); child = _pid_alive(manifest.get("child_pid"))
    rows = []
    for ex_id in manifest.get("order", manifest["items"]):
        item = manifest["items"][ex_id]
        base = directory / "staged" / ex_id; frames = base / "blender" / "frames"
        try: count = sum(1 for p in frames.iterdir() if p.is_file() and p.suffix == ".png")
        except OSError: count = 0
        log = directory / item["log"]
        rows.append({"id": ex_id, "state": item["state"], "frames": f"{count}/{item['frames_expected']}",
                     "log_bytes": log.stat().st_size if log.exists() else 0, "error": item.get("error")})
    result = {"state": manifest["state"], "runner_pid": manifest.get("own_pid"), "runner_alive": active,
              "child_pid": manifest.get("child_pid"), "child_alive": child, "items": rows,
              "note": "Snapshot only: PID and files do not prove useful render progress."}
    print(json.dumps(result, indent=2)); return result


def run(directory, blender=BLENDER):
    directory = Path(directory)
    if not directory.is_absolute(): raise ValueError("--directory must be absolute")
    lock_path = directory / ".runner.lock"; lock_path.parent.mkdir(parents=True, exist_ok=True)
    lock = open(lock_path, "a+")
    try:
        try: fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError: raise RuntimeError("another render_batch runner holds the directory lock")
        old_handlers = {}
        active_child = [None]
        stop_requested = [False]
        def stop_handler(signum, _frame):
            stop_requested[0] = True
            if active_child[0] is not None:
                try: active_child[0].terminate()
                except Exception: pass
        for sig in (signal.SIGINT, signal.SIGTERM):
            old_handlers[sig] = signal.signal(sig, stop_handler)
        manifest_path = directory / "manifest.json"; manifest = _load(manifest_path)
        manifest.update(state="preflighting", own_pid=os.getpid(), child_pid=None, updated_at=time.time())
        manifest.pop("geometry_preflight", None)
        _atomic_json(manifest_path, manifest)
        env = os.environ.copy(); env["CALISTHENICS_RENDER_WORK"] = str(directory / "staged")
        ok_all = True
        try:
            # Freeze the batch before invoking Blender, then prove the checker
            # examined the same pipeline and staged joint definitions.
            same, why = _batch_signatures_match(manifest)
            if not same:
                stale_id = why.split(":", 1)[0]
                if stale_id in manifest["items"]: manifest["items"][stale_id].update(state="stale", error=why)
                manifest.update(state="failed", preflight_error="stale before geometry preflight: " + why,
                                own_pid=None, child_pid=None, updated_at=time.time())
                _atomic_json(manifest_path, manifest)
                return 1
            geometry_path = directory / "geometry.json"
            try: geometry_path.unlink()
            except FileNotFoundError: pass
            preflight_log = directory / "logs" / "preflight.log"
            preflight_log.parent.mkdir(parents=True, exist_ok=True)
            command = [blender, "--background", "--python-exit-code", "1", "--python", str(GEOMETRY_CHECK),
                       "--", "--directory", str(directory), "--output", str(geometry_path)]
            manifest["current_command"] = command
            manifest["preflight_log"] = "logs/preflight.log"
            _atomic_json(manifest_path, manifest)
            try:
                with open(preflight_log, "w") as log:
                    proc = subprocess.Popen(command, cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT)
                    active_child[0] = proc
                    manifest["child_pid"] = proc.pid; _atomic_json(manifest_path, manifest)
                    if stop_requested[0]: proc.terminate()
                    try: preflight_code = _wait_child(proc, stop_requested)
                    finally: active_child[0] = None
                manifest["child_pid"] = None
                manifest["geometry_preflight_exit_code"] = preflight_code
                try: manifest["geometry_preflight"] = _load(geometry_path)
                except (OSError, ValueError): manifest["geometry_preflight"] = None
                _atomic_json(manifest_path, manifest)
                if preflight_code != 0: raise RuntimeError(f"geometry preflight Blender exited {preflight_code}")
                report_ok, report_why = _geometry_report_ok(geometry_path, manifest)
                if not report_ok: raise RuntimeError(report_why)
                same, why = _batch_signatures_match(manifest)
                if not same:
                    stale_id = why.split(":", 1)[0]
                    if stale_id in manifest["items"]: manifest["items"][stale_id].update(state="stale", error=why)
                    raise StaleSourceError("source changed during geometry preflight: " + why)
                manifest["geometry_preflight"] = _load(geometry_path)
                manifest["geometry_preflight_status"] = "pass"
            except StaleSourceError as exc:
                manifest.update(state="stale", preflight_error=str(exc), child_pid=None,
                                own_pid=None, updated_at=time.time())
                _atomic_json(manifest_path, manifest)
                return 1
            except Exception as exc:
                manifest.update(state="failed", preflight_error=str(exc), child_pid=None,
                                own_pid=None, updated_at=time.time())
                _atomic_json(manifest_path, manifest)
                return 1
            manifest.update(state="running", current_command=None, updated_at=time.time())
            _atomic_json(manifest_path, manifest)
            for ex_id in manifest.get("order", manifest["items"]):
                item = manifest["items"][ex_id]
                base = directory / "staged" / ex_id; video = base / "blender" / f"{ex_id}.mp4"
                # Reuse only verified complete outputs, bound to the frozen source/stamp.
                same, why = _signature_matches(ex_id, item)
                if not same:
                    item.update(state="stale", error=why); ok_all = False; _atomic_json(manifest_path, manifest)
                    break
                valid, _, _ = _probe_video(video, item["frames_expected"], item["duration_s"]) if video.is_file() else (False, "missing", {})
                frames_ok, _ = _native_frames_ok(base, item)
                if item.get("state") == "complete" and valid and frames_ok and _digest(video) == item.get("video_sha256"):
                    item["state"] = "complete"; continue
                item.update(state="rendering", error=None, video_sha256=None); manifest["child_pid"] = None; _atomic_json(manifest_path, manifest)
                out = base / "blender"; shutil.rmtree(out / "frames", ignore_errors=True); shutil.rmtree(out / "grip-frames", ignore_errors=True)
                for name in ("render-detail.json", "render-camera.json", "start.png", f"{ex_id}.mp4"):
                    try: (out / name).unlink()
                    except FileNotFoundError: pass
                out.mkdir(parents=True, exist_ok=True)
                log_path = directory / item["log"]; log_path.parent.mkdir(parents=True, exist_ok=True)
                stop_after_item = False
                try:
                    with open(log_path, "w") as log:
                        proc = subprocess.Popen([blender, "--background", "--python-exit-code", "1", "--python", str(RENDER), "--", ex_id],
                                                cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT)
                        active_child[0] = proc
                        manifest["child_pid"] = proc.pid; _atomic_json(manifest_path, manifest)
                        if stop_requested[0]: proc.terminate()
                        try: code = _wait_child(proc, stop_requested)
                        finally: active_child[0] = None
                    item["render_exit_code"] = code; manifest["child_pid"] = None; _atomic_json(manifest_path, manifest)
                    if code != 0: raise RuntimeError(f"Blender exited {code}")
                    same, why = _signature_matches(ex_id, item)
                    if not same: raise StaleSourceError("stale after render: " + why)
                    native_ok, why = _native_frames_ok(base, item)
                    if not native_ok: raise RuntimeError("native render validation failed: " + why)
                    item["state"] = "encoding"; _atomic_json(manifest_path, manifest)
                    with open(log_path, "a") as log:
                        proc = subprocess.Popen([sys.executable, str(ENCODE), ex_id], cwd=ROOT, env=env,
                                                stdout=log, stderr=subprocess.STDOUT)
                        active_child[0] = proc
                        manifest["child_pid"] = proc.pid; _atomic_json(manifest_path, manifest)
                        if stop_requested[0]: proc.terminate()
                        try: code = _wait_child(proc, stop_requested)
                        finally: active_child[0] = None
                    manifest["child_pid"] = None; _atomic_json(manifest_path, manifest)
                    if code != 0: raise RuntimeError(f"encoder exited {code}")
                    same, why = _signature_matches(ex_id, item)
                    if not same: raise StaleSourceError("stale after encode: " + why)
                    valid, why, facts = _probe_video(video, item["frames_expected"], item["duration_s"])
                    if not valid: raise RuntimeError("encoded video validation failed: " + why)
                    item.update(state="complete", video_sha256=_digest(video), video=facts, error=None)
                except StaleSourceError as exc:
                    item.update(state="stale", error=str(exc)); ok_all = False
                    stop_after_item = True
                except Exception as exc:
                    item.update(state="failed", error=str(exc)); ok_all = False
                    stop_after_item = False
                finally:
                    manifest["child_pid"] = None; manifest["updated_at"] = time.time(); _atomic_json(manifest_path, manifest)
                if stop_after_item: break
            manifest.update(state="complete" if ok_all else "failed", own_pid=None, child_pid=None,
                            current_command=None, updated_at=time.time())
            _atomic_json(manifest_path, manifest)
            return 0 if ok_all else 1
        except BaseException:
            manifest.update(state="interrupted", own_pid=None, child_pid=None, updated_at=time.time()); _atomic_json(manifest_path, manifest)
            raise
    finally:
        for sig, handler in locals().get("old_handlers", {}).items(): signal.signal(sig, handler)
        fcntl.flock(lock, fcntl.LOCK_UN); lock.close()


class StaleSourceError(RuntimeError): pass


def _wait_child(proc, stop_requested=None):
    try:
        code = proc.wait()
        if stop_requested and stop_requested[0]: raise KeyboardInterrupt("runner interrupted")
        return code
    except BaseException:
        try: proc.terminate()
        except Exception: pass
        try: proc.wait(timeout=10)
        except Exception:
            try: proc.kill(); proc.wait()
            except Exception: pass
        raise


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__); sub = p.add_subparsers(dest="command", required=True)
    prep = sub.add_parser("prepare"); prep.add_argument("--directory", required=True); prep.add_argument("--ids", nargs="+", required=True)
    runner = sub.add_parser("run"); runner.add_argument("--directory", required=True); runner.add_argument("--blender", default=BLENDER)
    stat = sub.add_parser("status"); stat.add_argument("--directory", required=True)
    args = p.parse_args(argv)
    if args.command == "prepare": prepare(args.directory, args.ids); return 0
    if args.command == "run": return run(args.directory, args.blender)
    status(args.directory); return 0


if __name__ == "__main__": raise SystemExit(main())
