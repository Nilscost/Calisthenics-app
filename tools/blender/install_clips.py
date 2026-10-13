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

DEMOS = os.path.join(ROOT, "app", "src", "main", "assets", "demos")
THUMBS = os.path.join(ROOT, "app", "src", "main", "assets", "thumbs")
CAP = 120 * 1024


def complete(ex):
    src = os.path.join(WORK, ex, "blender", "frames")
    j = os.path.join(WORK, ex, "joints.json")
    if not (os.path.isdir(src) and os.path.exists(j)):
        return False
    n = len(json.load(open(j))["frames"])
    return len([f for f in os.listdir(src) if f.endswith(".png")]) >= n


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
    out = os.path.join(DEMOS, ex + ".mp4") if not dry else os.path.join("/tmp", ex + ".mp4")
    for crf in (30, 32, 34):
        with tempfile.TemporaryDirectory() as tmp:
            for i, n in enumerate(names):
                flat(os.path.join(src, n)).save(os.path.join(tmp, f"{i:04d}.png"))
            subprocess.run([FFMPEG, "-y", "-loglevel", "error", "-framerate", "24", "-i",
                            os.path.join(tmp, "%04d.png"), "-an", "-c:v", "libx264", "-profile:v", "main",
                            "-pix_fmt", "yuv420p", "-crf", str(crf), "-preset", "slow", "-movflags", "+faststart",
                            out], check=True)
        if os.path.getsize(out) < CAP:
            break
    t_out = os.path.join(THUMBS, ex + ".webp") if not dry else os.path.join("/tmp", ex + ".webp")
    thumb(ex, t_out)
    return os.path.getsize(out), crf, os.path.getsize(t_out)


def main(argv):
    dry = "--dry" in argv
    ids = [a for a in argv if not a.startswith("--")]
    catalog_ids = {f[:-4] for f in os.listdir(DEMOS) if f.endswith(".mp4")}
    if not ids:
        ids = sorted(d for d in os.listdir(WORK) if d in catalog_ids)
    problems = []
    for ex in ids:
        if ex not in catalog_ids:
            problems.append(f"{ex}: not a shipped clip id")
            continue
        if not complete(ex):
            problems.append(f"{ex}: frames incomplete or missing (kept the old clip)")
            continue
        size, crf, tsize = install(ex, dry)
        flag = "" if size < CAP else "  OVER CAP"
        print(f"{ex:40s} {size // 1024:4d} KB (crf {crf})  thumb {tsize // 1024} KB{flag}")
        if size >= CAP:
            problems.append(f"{ex}: {size // 1024} KB over the cap")
    missing = sorted(catalog_ids - set(ids))
    print(f"\ninstalled {len(ids) - len(problems)}; problems {len(problems)}; ids without a Blender clip {len(missing)}")
    for p in problems:
        print("  PROBLEM", p)
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
