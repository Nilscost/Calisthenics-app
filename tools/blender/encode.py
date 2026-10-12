"""Encode Blender frames (RGBA PNG) into small looping H.264 clips (sandbox).

    python3 tools/blender/encode.py [ids]

Composites each frame on white, scales to 480x360 and encodes with the
plan-16 settings (H.264 Main, yuv420p, CRF 30, faststart, no audio, 24 fps).
Writes work/clips-bakeoff/<id>/blender/<id>.mp4 and prints size and the loop
seam (mean pixel difference between the last and the first frame, 0-255).
"""
import os
import subprocess
import sys
import tempfile

from PIL import Image, ImageChops, ImageStat
import imageio_ffmpeg

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
WORK = os.path.join(ROOT, "work", "clips-bakeoff")
FFMPEG = imageio_ffmpeg.get_ffmpeg_exe()


def flat(path):
    im = Image.open(path).convert("RGBA")
    bg = Image.new("RGBA", im.size, (255, 255, 255, 255))
    bg.alpha_composite(im)
    return bg.convert("RGB").resize((480, 360), Image.LANCZOS)


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
