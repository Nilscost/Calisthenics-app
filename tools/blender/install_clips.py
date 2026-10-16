"""Install rendered Blender clips into the app (sandbox, after the overnight render).

    python3 tools/blender/install_clips.py [ids] [--dry]

For every id with complete frames in work/clips-bakeoff/<id>/blender/frames:
- app/src/main/assets/demos/<id>.mp4: 480x360, 24 fps, H.264 Main, CRF 30, faststart, no audio
  (frames composited on white); must stay under the ClipAssetsTest cap (120 KB);
- app/src/main/assets/thumbs/<id>.webp: 128x128 RGBA, the start frame cropped to the
  figure and its props on a transparent background (same format as the v3 thumbnails).
clip_meta.json (muscles, kind) is left as is. Prints a table and a list of problems.
"""
import json
import os
import subprocess
import sys
import tempfile

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from encode import FFMPEG, WORK, ROOT, flat  # noqa: E402
from camera_views import camera_stamp  # noqa: E402

DEMOS = os.path.join(ROOT, "app", "src", "main", "assets", "demos")
THUMBS = os.path.join(ROOT, "app", "src", "main", "assets", "thumbs")
CATALOG = os.path.join(ROOT, "app", "src", "main", "assets", "catalog.json")
CAP = 120 * 1024


def catalog_ids():
    with open(CATALOG) as f:
        catalog = json.load(f)
    return {item["id"] for item in catalog["variations"]}


def frame_problem(ex):
    src = os.path.join(WORK, ex, "blender", "frames")
    j = os.path.join(WORK, ex, "joints.json")
    if not os.path.isdir(src) or not os.path.isfile(j):
        return "missing joints.json or Blender frames directory"
    try:
        with open(j) as f:
            frames = json.load(f)["frames"]
    except (OSError, ValueError, KeyError, TypeError):
        return "invalid joints.json"
    if not isinstance(frames, list) or not frames:
        return "joints.json has no expected frames"

    expected_camera = camera_stamp(ex)
    if expected_camera:
        try:
            with open(os.path.join(WORK, ex, "blender", "render-camera.json")) as f:
                if json.load(f) != expected_camera:
                    return "camera view is outdated; render this exercise again"
        except (OSError, ValueError):
            return "missing reviewed camera view stamp; render this exercise again"

    expected = {f"{i:04d}.png" for i in range(len(frames))}
    actual = {name for name in os.listdir(src) if name.endswith(".png")}
    if actual != expected:
        missing = sorted(expected - actual)
        extra = sorted(actual - expected)
        details = []
        if missing:
            details.append(f"missing {missing[0]}")
        if extra:
            details.append(f"unexpected {extra[0]}")
        return "frame names do not exactly match the expected contiguous sequence" + (
            f" ({', '.join(details)})" if details else ""
        )

    joints_mtime = os.stat(j).st_mtime_ns
    for name in sorted(expected):
        path = os.path.join(src, name)
        if not os.path.isfile(path):
            return f"{name} is not a regular file"
        if os.stat(path).st_mtime_ns <= joints_mtime:
            return f"{name} is not newer than joints.json"
    return None


def complete(ex):
    return frame_problem(ex) is None


def thumb(ex, out):
    im = Image.open(os.path.join(WORK, ex, "blender", "frames", "0000.png")).convert("RGBA")
    a = im.split()[3].point(lambda v: 255 if v > 60 else 0)  # ignore faint floor shadow
    box = a.getbbox() or (0, 0, im.width, im.height)
    pad = 6
    box = (max(box[0] - pad, 0), max(box[1] - pad, 0), min(box[2] + pad, im.width), min(box[3] + pad, im.height))
    im = im.crop(box)
    side = max(im.size)
    sq = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    sq.paste(im, ((side - im.width) // 2, (side - im.height) // 2))
    sq.resize((128, 128), Image.LANCZOS).save(out, "WEBP", quality=80, method=6)


def install(ex, dry=False):
    src = os.path.join(WORK, ex, "blender", "frames")
    names = sorted(f for f in os.listdir(src) if f.endswith(".png"))
    problem = frame_problem(ex)
    if problem:
        raise ValueError(f"{ex}: {problem}")

    # Stage each output on the destination filesystem so a successful os.replace
    # is atomic. Dry runs stage under the system temp directory and never touch
    # shipped files.
    mp4_stage_parent = None if dry else DEMOS
    thumb_stage_parent = None if dry else THUMBS
    with tempfile.TemporaryDirectory(dir=mp4_stage_parent) as mp4_tmp, \
            tempfile.TemporaryDirectory(dir=thumb_stage_parent) as thumb_tmp:
        mp4_stage = os.path.join(mp4_tmp, ex + ".mp4")
        thumb_stage = os.path.join(thumb_tmp, ex + ".webp")
        frames_tmp = tempfile.TemporaryDirectory()
        try:
            for i, n in enumerate(names):
                flat(os.path.join(src, n)).save(os.path.join(frames_tmp.name, f"{i:04d}.png"))
            for crf in (30, 32, 34):
                subprocess.run([FFMPEG, "-y", "-loglevel", "error", "-framerate", "24", "-i",
                                os.path.join(frames_tmp.name, "%04d.png"), "-an", "-c:v", "libx264",
                                "-profile:v", "main", "-pix_fmt", "yuv420p", "-crf", str(crf),
                                "-preset", "slow", "-movflags", "+faststart", mp4_stage], check=True)
                if os.path.getsize(mp4_stage) < CAP:
                    break
        finally:
            frames_tmp.cleanup()

        size = os.path.getsize(mp4_stage)
        thumb(ex, thumb_stage)
        thumb_size = os.path.getsize(thumb_stage)
        if size < CAP and not dry:
            os.replace(mp4_stage, os.path.join(DEMOS, ex + ".mp4"))
            os.replace(thumb_stage, os.path.join(THUMBS, ex + ".webp"))
        return size, crf, thumb_size


def main(argv):
    dry = "--dry" in argv
    ids = [a for a in argv if not a.startswith("--")]
    ids_in_catalog = catalog_ids()
    if not ids:
        ids = sorted(d for d in os.listdir(WORK) if d in ids_in_catalog)
    problems = []
    completed = 0
    for ex in ids:
        if ex not in ids_in_catalog:
            problems.append(f"{ex}: not a catalog exercise id")
            continue
        problem = frame_problem(ex)
        if problem:
            problems.append(f"{ex}: {problem} (kept the old clip)")
            continue
        size, crf, tsize = install(ex, dry)
        flag = "" if size < CAP else "  OVER CAP"
        print(f"{ex:40s} {size // 1024:4d} KB (crf {crf})  thumb {tsize // 1024} KB{flag}")
        if size >= CAP:
            problems.append(f"{ex}: {size // 1024} KB over the cap")
        else:
            completed += 1
    missing = sorted(ids_in_catalog - set(ids))
    action = "prepared" if dry else "installed"
    print(f"\n{action} {completed}; problems {len(problems)}; ids without a Blender clip {len(missing)}")
    for p in problems:
        print("  PROBLEM", p)
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
