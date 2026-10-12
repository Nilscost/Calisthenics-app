"""Keyframed AI clip: split one rep into segments between key poses (owner idea 2026-10-12).

Seedance on OpenRouter takes only a first and a last frame, so a middle pose (e.g. the
bottom of a pistol squat) is forced by generating two clips: top -> bottom and
bottom -> top, then joining them. Each key pose = a Blender frame turned into the same
photoreal person with Seedream (ByteDance's image model, accepted by Seedance's input check).

    python3 tools/gen_keyframe_clip.py squat-pistol --bottom-frame 0046 --duration 4
"""
import base64
import io
import os
import subprocess
import sys
import time

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_clips_ai as g  # noqa: E402
import gen_start_images as si  # noqa: E402
import imageio_ffmpeg  # noqa: E402

FF = imageio_ffmpeg.get_ffmpeg_exe()
BOTTOM_POSE = {
    "squat-pistol": "at the BOTTOM of a full-depth pistol squat on his standing leg: hips below the knee, hamstring "
                    "resting on the calf, standing heel flat on the floor, the other leg held straight out in front "
                    "and off the floor, torso leaning forward, arms reaching straight forward",
}
SEG_MOTION = {
    ("squat-pistol", "down"): "The man slowly lowers into a full-depth pistol squat on his standing leg, the other leg "
                              "straight in front and off the floor, standing heel flat, arms forward, ending exactly in "
                              "the pose of the last frame.",
    ("squat-pistol", "up"): "The man slowly stands up from the bottom of the pistol squat on his standing leg, the other "
                            "leg straight in front and off the floor, arms forward, ending exactly in the pose of the "
                            "last frame.",
}


def url(path):
    im = Image.open(path).convert("RGB").resize((640, 480), Image.LANCZOS)
    buf = io.BytesIO()
    im.save(buf, "PNG")
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()


def bottom_image(ex, frame, ref):
    """Seedream image of the bottom pose, guided by a Blender frame, same person as ref."""
    guide_src = os.path.join(g.WORK, ex, "blender", "frames", f"{frame}.png")
    tmp = os.path.join(g.WORK, ex, "blender", "start.png")
    keep = tmp + ".keep"
    os.replace(tmp, keep)
    try:
        Image.open(guide_src).save(tmp)
        si.POSE[ex] = BOTTOM_POSE[ex]
        out = si.run(ex, ref, "bytedance-seed/seedream-5-0-lite")
    finally:
        os.replace(keep, tmp)
    final = os.path.join(g.WORK, ex, "human", "bottom.png")
    os.replace(out, final)
    return final


def segment(ex, first, last, motion, duration, tag):
    prompt = ("The man in the image, same person and clothes, pure white seamless studio background, fixed camera, "
              "no camera motion, no cuts, no text. " + motion)
    body = {"model": "bytedance/seedance-2.0", "prompt": prompt, "resolution": "480p", "duration": duration,
            "aspect_ratio": "4:3", "generate_audio": False,
            "frame_images": [
                {"type": "image_url", "image_url": {"url": url(first)}, "frame_type": "first_frame"},
                {"type": "image_url", "image_url": {"url": url(last)}, "frame_type": "last_frame"}]}
    t0 = time.time()
    job = g.call("POST", "/videos", body)
    vid = job["id"]
    st = {}
    while time.time() - t0 < 900:
        time.sleep(10)
        st = g.call("GET", f"/videos/{vid}")
        if st.get("status") in ("completed", "failed", "error", "cancelled"):
            break
    if st.get("status") != "completed":
        raise RuntimeError(f"{tag}: {st}")
    out = os.path.join(g.WORK, ex, "ai", f"seg-{tag}.mp4")
    with open(out, "wb") as fh:
        fh.write(g.call("GET", f"/videos/{vid}/content", raw=True))
    cost = float((st.get("usage") or {}).get("cost") or 0.0)
    g.manifest(dict(time=time.strftime("%Y-%m-%d %H:%M"), exercise=ex, route="seedance-keyframe", model=body["model"],
                    attempt=tag, status="completed", seconds=round(time.time() - t0), cost_usd=round(cost, 4),
                    file=os.path.relpath(out, g.ROOT), prompt=prompt))
    print(f"{tag}: {out} ${cost:.3f} {time.time() - t0:.0f} s")
    return out


def main(argv):
    ex = argv[0]
    frame = argv[argv.index("--bottom-frame") + 1]
    duration = int(argv[argv.index("--duration") + 1]) if "--duration" in argv else 4
    top = os.path.join(g.WORK, ex, "human", "start-2.png")
    bottom = os.path.join(g.WORK, ex, "human", "bottom.png")
    if not os.path.exists(bottom):
        bottom = bottom_image(ex, frame, top)
        print("bottom image:", bottom)
    if "--image-only" in argv:
        return
    down = segment(ex, top, bottom, SEG_MOTION[(ex, "down")], duration, "down")
    up = segment(ex, bottom, top, SEG_MOTION[(ex, "up")], duration, "up")
    lst = os.path.join(g.WORK, ex, "ai", "segs.txt")
    with open(lst, "w") as fh:
        fh.write(f"file '{down}'\nfile '{up}'\n")
    out = os.path.join(g.ROOT, "docs", "evidence", "clips-bakeoff", f"{ex}-seedance-keyframed.mp4")
    subprocess.run([FF, "-y", "-loglevel", "error", "-f", "concat", "-safe", "0", "-i", lst, "-an",
                    "-vf", "scale=480:-2,fps=24", "-c:v", "libx264", "-profile:v", "main", "-pix_fmt", "yuv420p",
                    "-crf", "30", "-preset", "slow", "-movflags", "+faststart", out], check=True)
    print("joined:", out, os.path.getsize(out) // 1024, "KB")


if __name__ == "__main__":
    main(sys.argv[1:])
