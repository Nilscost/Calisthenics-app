"""Encode Blender frames (RGBA PNG) into small looping H.264 clips (sandbox).

    python3 tools/blender/encode.py [ids]

Composites each frame on white, scales to 480x360 and encodes with the
plan-16 settings (H.264 Main, yuv420p, CRF 30, faststart, no audio, 24 fps).
Writes work/clips-bakeoff/<id>/blender/<id>.mp4 and prints size and the loop
seam (mean pixel difference between the last and the first frame, 0-255).
"""
import os
import json
import subprocess
import sys
import tempfile

from PIL import Image, ImageChops, ImageStat, ImageDraw, ImageFont
import imageio_ffmpeg

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
WORK = os.environ.get("CALISTHENICS_RENDER_WORK", os.path.join(ROOT, "work", "clips-bakeoff"))
FFMPEG = imageio_ffmpeg.get_ffmpeg_exe()


def flat(path):
    im = Image.open(path).convert("RGBA")
    bg = Image.new("RGBA", im.size, (255, 255, 255, 255))
    bg.alpha_composite(im)
    result = bg.convert("RGB").resize((480, 360), Image.LANCZOS)
    blender_dir = os.path.dirname(os.path.dirname(path))
    joints_path = os.path.join(os.path.dirname(blender_dir), "joints.json")
    if os.path.isfile(joints_path):
        with open(joints_path) as fh:
            detail = json.load(fh).get("grip_detail")
        if detail:
            # These reviewed pilots fit in the left column. Reserve the right
            # column so the inset cannot hide the moving head, elbow or hand.
            shifted = Image.new("RGB", result.size, "white")
            shifted.paste(result, (-65, 0))
            result = shifted
            inset_path = os.path.join(blender_dir, "grip-frames", os.path.basename(path))
            # Never silently publish a grip-critical clip without its detail view.
            inset = Image.open(inset_path).convert("RGBA")
            white = Image.new("RGBA", inset.size, "white")
            white.alpha_composite(inset)
            result.paste(white.convert("RGB").resize((192, 144), Image.LANCZOS), (284, 4))
            draw = ImageDraw.Draw(result)
            draw.rectangle((283, 3, 477, 187), outline=(28, 45, 58), width=1)
            draw.rectangle((284, 148, 476, 186), fill="white")
            font = ImageFont.load_default(size=15)
            draw.text((289, 150), detail, fill=(20, 35, 45), font=font)
            draw.text((289, 169), "BLUE = PALM", fill=(20, 80, 110), font=ImageFont.load_default(size=12))
    return result


def encode(ex, crf=30):
    src = os.path.join(WORK, ex, "blender", "frames")
    names = sorted(n for n in os.listdir(src) if n.endswith(".png"))
    with tempfile.TemporaryDirectory() as tmp:
        frames = []
        for i, n in enumerate(names):
            im = flat(os.path.join(src, n))
            im.save(os.path.join(tmp, f"{i:04d}.png"))
            frames.append(im)
        out = os.path.join(WORK, ex, "blender", f"{ex}.mp4")
        cmd = [FFMPEG, "-y", "-loglevel", "error", "-framerate", "24", "-i", os.path.join(tmp, "%04d.png"),
               "-an", "-c:v", "libx264", "-profile:v", "main", "-pix_fmt", "yuv420p", "-crf", str(crf),
               "-preset", "slow", "-movflags", "+faststart", out]
        subprocess.run(cmd, check=True)
    seam = sum(ImageStat.Stat(ImageChops.difference(frames[-1], frames[0])).mean) / 3
    step = sum(ImageStat.Stat(ImageChops.difference(frames[1], frames[0])).mean) / 3
    size = os.path.getsize(out)
    print(f"{ex}: {len(frames)} frames, {size / 1024:.0f} KB, seam {seam:.2f} (typical step {step:.2f}) -> {out}")
    return size


def main(argv):
    ids = argv or sorted(d for d in os.listdir(WORK) if os.path.isdir(os.path.join(WORK, d, "blender", "frames")))
    for ex in ids:
        encode(ex)


if __name__ == "__main__":
    main(sys.argv[1:])
