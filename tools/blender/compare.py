"""Side-by-side comparison videos for the clip bake-off (sandbox).

    python3 tools/blender/compare.py

Per exercise: work/clips-bakeoff/<id>/<id>-compare.mp4, a 2x2 grid
(today | Blender / Seedance | Kling), every panel 480x360 on white, each
looping for 10 s, labelled. Missing clips get a labelled white panel.
Also encodes each AI raw clip with the shipping settings to report its size.
"""
import os
import subprocess

from PIL import Image, ImageDraw, ImageFont
import imageio_ffmpeg

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
WORK = os.path.join(ROOT, "work", "clips-bakeoff")
FONT = os.path.join(ROOT, "app", "src", "main", "res", "font")
FF = imageio_ffmpeg.get_ffmpeg_exe()
IDS = ["pullup-full", "pushup-feet-elevated", "squat-pistol", "stretch-pigeon"]
SECONDS = 10


def font(size):
    for n in sorted(os.listdir(FONT)):
        if n.startswith("barlow") and n.endswith(".ttf"):
            return ImageFont.truetype(os.path.join(FONT, n), size)
    return ImageFont.load_default()


def label_png(path, labels):
    im = Image.new("RGBA", (960, 720), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    f = font(26)
    for i, text in enumerate(labels):
        x, y = (i % 2) * 480, (i // 2) * 360
        w = d.textlength(text, font=f)
        d.rounded_rectangle([x + 8, y + 8, x + 22 + w, y + 44], radius=8, fill=(20, 20, 20, 200))
        d.text((x + 15, y + 11), text, font=f, fill=(255, 255, 255, 255))
    d.line([(480, 0), (480, 720)], fill=(200, 200, 200, 255), width=2)
    d.line([(0, 360), (960, 360)], fill=(200, 200, 200, 255), width=2)
    im.save(path)


def placeholder(path, text):
    im = Image.new("RGB", (480, 360), "white")
    d = ImageDraw.Draw(im)
    f = font(24)
    lines = text.split("\n")
    for i, ln in enumerate(lines):
        w = d.textlength(ln, font=f)
        d.text(((480 - w) / 2, 150 + i * 32), ln, font=f, fill=(150, 40, 40))
    im.save(path)


def panel_filter(i, square):
    crop = "crop=iw:iw*3/4," if square else ""
    return (f"[{i}:v]{crop}scale=480:360:force_original_aspect_ratio=decrease,"
            f"pad=480:360:(ow-iw)/2:(oh-ih)/2:white,fps=24,setsar=1,format=yuv420p[p{i}]")


def ship_size(raw, out):
    subprocess.run([FF, "-y", "-loglevel", "error", "-i", raw, "-an", "-vf", "scale=480:-2,fps=24",
                    "-c:v", "libx264", "-profile:v", "main", "-pix_fmt", "yuv420p", "-crf", "30",
                    "-preset", "slow", "-movflags", "+faststart", out], check=True)
    return os.path.getsize(out) // 1024


def main():
    for ex in IDS:
        d = os.path.join(WORK, ex)
        srcs = [
            ("Today (current app)", os.path.join(ROOT, "app", "src", "main", "assets", "demos", ex + ".mp4"), False),
            ("Blender", os.path.join(d, "blender", ex + ".mp4"), False),
            ("Seedance 2.0", os.path.join(d, "ai", "raw-seedance-1.mp4"), False),
            ("Kling v3 std", os.path.join(d, "ai", "raw-kling-1.mp4"), True),
        ]
        args, filters, labels = [], [], []
        for i, (name, path, square) in enumerate(srcs):
            labels.append(name)
            if os.path.exists(path):
                args += ["-stream_loop", "-1", "-i", path]
            else:
                ph = os.path.join(d, f"missing-{i}.png")
                placeholder(ph, f"{name}\nno clip: refused by the\nmodel's content filter")
                args += ["-loop", "1", "-framerate", "24", "-i", ph]
                square = False
            filters.append(panel_filter(i, square))
        lab = os.path.join(d, "labels.png")
        label_png(lab, labels)
        args += ["-loop", "1", "-i", lab]
        fc = ";".join(filters) + ";[p0][p1][p2][p3]xstack=inputs=4:layout=0_0|w0_0|0_h0|w0_h0[g];[g][4:v]overlay=0:0[v]"
        out = os.path.join(d, f"{ex}-compare.mp4")
        subprocess.run([FF, "-y", "-loglevel", "error", *args, "-filter_complex", fc, "-map", "[v]", "-t", str(SECONDS),
                        "-an", "-c:v", "libx264", "-pix_fmt", "yuv420p", "-crf", "26", "-movflags", "+faststart", out],
                       check=True)
        sizes = []
        for name, path, _ in srcs:
            if os.path.exists(path):
                if "ai" + os.sep in path:
                    sizes.append(f"{name} {ship_size(path, path.replace('raw-', 'ship-'))} KB")
                else:
                    sizes.append(f"{name} {os.path.getsize(path) // 1024} KB")
        print(f"{ex}: {out} ({os.path.getsize(out) // 1024} KB) | shipping sizes: " + ", ".join(sizes))


if __name__ == "__main__":
    main()
