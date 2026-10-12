"""Turn Blender mannequin start frames into a photoreal person (image model via OpenRouter).

    python3 tools/gen_start_images.py <id> [--ref <png>] [--model M]

Input: work/clips-bakeoff/<id>/blender/start.png (pose guide, composited on white).
Optional --ref: an earlier output, so the same person is kept across exercises.
Output: work/clips-bakeoff/<id>/human/start-<n>.png (640x480, white background),
plus a manifest row (docs/evidence/clips-bakeoff/manifest.csv). Key handling and
direct (no-proxy) calls as in tools/gen_clips_ai.py.
"""
import base64
import io
import os
import sys
import time

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_clips_ai as g  # noqa: E402

MODEL = "google/gemini-3.1-flash-image"
PERSON = ("a real athletic man in his early thirties, short dark hair, light stubble, fit but not bulky, "
          "wearing a plain dark grey fitted t-shirt, black shorts and black trainers, no logos")
POSE = {
    "pullup-full": "hanging from a black pull-up bar with an overhand grip, arms fully straight (dead hang), "
                   "legs together and relaxed, feet clear of the floor",
    "squat-pistol": "standing on his far leg with the near leg held straight out in front and lifted off the floor, "
                    "arms reaching straight forward at shoulder height, at the top of a pistol squat",
    "pushup-feet-elevated": "in the top of a push-up with his feet on the seat of a plain chair and hands on the "
                            "floor, arms straight, body in one straight line",
    "stretch-pigeon": "in a pigeon stretch on the floor: front shin across in front of the body, back leg straight "
                      "behind, hands on the floor beside the front knee, chest upright",
}


def png_data_url(im):
    buf = io.BytesIO()
    im.save(buf, "PNG")
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()


def on_white(path):
    im = Image.open(path).convert("RGBA")
    bg = Image.new("RGBA", im.size, (255, 255, 255, 255))
    bg.alpha_composite(im)
    return bg.convert("RGB")


def run(ex, ref=None, model=MODEL):
    guide = on_white(os.path.join(g.WORK, ex, "blender", "start.png"))
    text = (f"Image 1 is a pose guide: a grey mannequin {POSE[ex]}. Create a photorealistic photo of {PERSON} "
            f"in EXACTLY the same body pose, limb positions, camera angle and framing as the mannequin, with the "
            f"same equipment (bar, chair) in the same place. Pure white seamless studio background (#FFFFFF), soft "
            f"even light, soft floor shadow, full body in frame, nothing else in the scene, no text. "
            f"Correct anatomy: five fingers per hand, hands gripping or resting naturally.")
    content = [{"type": "text", "text": text},
               {"type": "image_url", "image_url": {"url": png_data_url(guide)}}]
    if ref:
        content[0]["text"] += " Image 2 shows the person: keep the same face, hair, build and clothes exactly."
        content.append({"type": "image_url", "image_url": {"url": png_data_url(Image.open(ref).convert("RGB"))}})
    body = {"model": model, "modalities": ["image", "text"],
            "messages": [{"role": "user", "content": content}],
            "image_config": {"aspect_ratio": "4:3"}}
    t0 = time.time()
    r = g.call("POST", "/chat/completions", body)
    msg = r["choices"][0]["message"]
    imgs = msg.get("images") or []
    if not imgs:
        raise RuntimeError("no image returned: " + str(msg.get("content"))[:400])
    url = imgs[0]["image_url"]["url"]
    raw = base64.b64decode(url.split(",", 1)[1])
    im = Image.open(io.BytesIO(raw)).convert("RGB")
    out_dir = os.path.join(g.WORK, ex, "human")
    os.makedirs(out_dir, exist_ok=True)
    n = 1 + len([f for f in os.listdir(out_dir) if f.startswith("start-")])
    out = os.path.join(out_dir, f"start-{n}.png")
    w, h = im.size
    tw = min(w, int(h * 4 / 3))
    th = int(tw * 3 / 4)
    im = im.crop(((w - tw) // 2, (h - th) // 2, (w - tw) // 2 + tw, (h - th) // 2 + th)).resize((640, 480), Image.LANCZOS)
    im.save(out)
    cost = float((r.get("usage") or {}).get("cost") or 0.0)
    g.manifest(dict(time=time.strftime("%Y-%m-%d %H:%M"), exercise=ex, route="image", model=model, attempt=n,
                    status="completed", seconds=round(time.time() - t0), cost_usd=round(cost, 4),
                    file=os.path.relpath(out, g.ROOT), prompt=text))
    print(f"{ex}: {out} ({w}x{h} raw), ${cost:.4f}, {time.time() - t0:.0f} s")
    return out


if __name__ == "__main__":
    a = sys.argv[1:]
    ref = a[a.index("--ref") + 1] if "--ref" in a else None
    model = a[a.index("--model") + 1] if "--model" in a else MODEL
    run(a[0], ref, model)
