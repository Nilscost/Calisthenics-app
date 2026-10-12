"""Generate AI video clips for the bake-off via OpenRouter (docs/16-video-clips-plan.md).

    python3 tools/gen_clips_ai.py <model> <id> [--tries N] [--dry] [--text]

--text: no start image; a real person is prompted instead (owner test 2026-10-12).

Start image = the Blender start frame (work/clips-bakeoff/<id>/blender/start.png),
sent as BOTH first and last frame so the clip loops. The key is read from
/Users/nils/Documents/hermes/.secrets/openrouter_key and never printed. Calls
go direct (no egress proxy: it rejects OpenRouter). Stops when the bake-off
has spent BUDGET dollars (OpenRouter /key usage, measured from the first run).
Raw clips: work/clips-bakeoff/<id>/ai/raw-<model>-<n>.mp4; one manifest row
per attempt in docs/evidence/clips-bakeoff/manifest.csv.
"""
import base64
import csv
import io
import json
import os
import sys
import time
import urllib.error
import urllib.request

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WORK = os.path.join(ROOT, "work", "clips-bakeoff")
EVID = os.path.join(ROOT, "docs", "evidence", "clips-bakeoff")
KEY_FILE = "/Users/nils/Documents/hermes/.secrets/openrouter_key"
API = "https://openrouter.ai/api/v1"
BUDGET = 30.0
OPENER = urllib.request.build_opener(urllib.request.ProxyHandler({}))

CHARACTER = ("a smooth grey artist's mannequin figure (wooden-mannequin style, no face, no clothes, "
             "no logos), pure white seamless background, soft even studio light, soft floor shadow, "
             "full body always in frame, fixed camera, no camera motion, no cuts")
MOTION = {
    "pullup-full": "The figure does one slow strict pull-up on the bar: from a dead hang with straight arms it pulls "
                   "until the chin is above the bar, then lowers with control back to a dead hang. Hands stay "
                   "fixed on the bar the whole time, legs together, no swinging.",
    "pushup-feet-elevated": "The figure does one slow push-up with its feet resting on the chair seat and hands on the "
                            "floor: the body stays in one straight line, it lowers the chest close to the floor with "
                            "elbows about 45 degrees from the body, then presses back up to straight arms. Feet stay "
                            "on the chair, hands stay on the floor.",
    "squat-pistol": "The figure does one slow pistol squat on its far leg: the near leg stays straight and rises "
                    "forward off the floor, it sits down to full depth with the standing heel flat, arms reaching "
                    "forward for balance, then stands back up to the start pose.",
    "stretch-pigeon": "The figure holds a pigeon stretch on the floor: front shin across the body, back leg straight "
                      "behind on the floor. It slowly walks the hands forward and folds the chest over the front "
                      "leg, holds, then walks the hands back and returns to the start pose. The legs do not move.",
}
MODELS = {
    "seedance": dict(id="bytedance/seedance-2.0", resolution="480p", aspect="4:3", square=False),
    "kling": dict(id="kwaivgi/kling-v3.0-std", resolution="720p", aspect="1:1", square=True),
}


def key():
    with open(KEY_FILE) as fh:
        return fh.read().strip()


def call(method, path, body=None, raw=False):
    req = urllib.request.Request(API + path, method=method,
                                 data=None if body is None else json.dumps(body).encode(),
                                 headers={"Authorization": "Bearer " + key(), "Content-Type": "application/json"})
    try:
        with OPENER.open(req, timeout=120) as r:
            data = r.read()
    except urllib.error.HTTPError as e:
        raise RuntimeError(f"HTTP {e.code} {path}: {e.read()[:600].decode(errors='replace')}") from None
    return data if raw else json.loads(data)


def usage():
    """Spend on this key (the /key 'usage' field stays 0; budgets carry the spend)."""
    d = call("GET", "/key")["data"]
    eff = d.get("effective_budget") or {}
    return float(eff.get("spend_usd") or d.get("usage") or 0.0)


def account_left():
    c = call("GET", "/credits")["data"]
    return float(c["total_credits"]) - float(c["total_usage"])


def start_image(ex, square):
    im = Image.open(os.path.join(WORK, ex, "blender", "start.png")).convert("RGBA")
    bg = Image.new("RGBA", im.size, (255, 255, 255, 255))
    bg.alpha_composite(im)
    im = bg.convert("RGB")
    if square:
        side = max(im.size)
        sq = Image.new("RGB", (side, side), "white")
        sq.paste(im, ((side - im.width) // 2, (side - im.height) // 2))
        im = sq.resize((720, 720), Image.LANCZOS)
    else:
        im = im.resize((640, 480), Image.LANCZOS)
    buf = io.BytesIO()
    im.save(buf, "PNG")
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()


def manifest(row):
    os.makedirs(EVID, exist_ok=True)
    path = os.path.join(EVID, "manifest.csv")
    new = not os.path.exists(path)
    with open(path, "a", newline="") as fh:
        w = csv.DictWriter(fh, fieldnames=list(row))
        if new:
            w.writeheader()
        w.writerow(row)


HUMAN = ("A real athletic adult in plain dark grey sports clothes (fitted t-shirt, shorts, trainers, no logos) "
         "on a pure white seamless studio background with a soft floor shadow, soft even light, side-on 3/4 view, "
         "full body always in frame, fixed camera, no camera motion, no cuts, no text")
HUMAN_MOTION = {
    "pullup-full": "The person hangs from a simple black pull-up bar with an overhand grip, arms straight, then does "
                   "one slow strict pull-up until the chin is clearly above the bar and lowers with control back to a "
                   "dead hang. Hands stay fixed on the bar, legs together, no swinging, no kipping.",
}


def generate(model, ex, n, dry=False, text_only=False):
    m = MODELS[model]
    if text_only:
        prompt = f"{HUMAN}. {HUMAN_MOTION[ex]}"
        body = {"model": m["id"], "prompt": prompt, "resolution": m["resolution"], "duration": 5,
                "aspect_ratio": m["aspect"], "generate_audio": False}
        model = model + "-text"
    else:
        img = start_image(ex, m["square"])
        prompt = f"{CHARACTER}. {MOTION[ex]} The clip starts and ends in exactly the same pose."
        body = {"model": m["id"], "prompt": prompt, "resolution": m["resolution"], "duration": 5,
                "aspect_ratio": m["aspect"], "generate_audio": False,
                "frame_images": [
                    {"type": "image_url", "image_url": {"url": img}, "frame_type": "first_frame"},
                    {"type": "image_url", "image_url": {"url": img}, "frame_type": "last_frame"}]}
    if dry:
        print(json.dumps({k: v for k, v in body.items() if k != "frame_images"}, indent=1))
        return
    before = usage()
    t0 = time.time()
    job = call("POST", "/videos", body)
    vid = job.get("id") or job.get("data", {}).get("id")
    print(f"[{model} {ex} #{n}] job {vid}: {json.dumps(job)[:300]}")
    status, st = None, {}
    while time.time() - t0 < 900:
        time.sleep(10)
        st = call("GET", f"/videos/{vid}")
        status = st.get("status") or st.get("data", {}).get("status")
        if status in ("completed", "failed", "error", "cancelled"):
            break
    out = None
    if status == "completed":
        out = os.path.join(WORK, ex, "ai", f"raw-{model}-{n}.mp4")
        os.makedirs(os.path.dirname(out), exist_ok=True)
        with open(out, "wb") as fh:
            fh.write(call("GET", f"/videos/{vid}/content", raw=True))
    else:
        print("   final status:", json.dumps(st)[:600])
    cost = float((st.get("usage") or {}).get("cost") or 0.0) or (usage() - before)
    manifest(dict(time=time.strftime("%Y-%m-%d %H:%M"), exercise=ex, route=model, model=m["id"], attempt=n,
                  status=status, seconds=round(time.time() - t0), cost_usd=round(cost, 4),
                  file=os.path.relpath(out, ROOT) if out else "", prompt=prompt))
    print(f"   {status}, {time.time() - t0:.0f} s, cost ${cost:.3f} -> {out}")
    return status, cost


def main(argv):
    model, ex = argv[0], argv[1]
    tries = int(argv[argv.index("--tries") + 1]) if "--tries" in argv else 1
    dry = "--dry" in argv
    text_only = "--text" in argv
    base_file = os.path.join(WORK, "ai-budget.json")
    if not os.path.exists(base_file) and not dry:
        json.dump({"usage_at_start": usage()}, open(base_file, "w"))
    for _ in range(tries):
        if not dry:
            spent = usage() - json.load(open(base_file))["usage_at_start"]
            if spent >= BUDGET:
                print(f"budget reached (${spent:.2f}); stopping")
                return 2
            if account_left() < 0.6:
                print(f"OpenRouter account has only ${account_left():.2f} left; stopping")
                return 3
        done = [f for f in os.listdir(os.path.join(WORK, ex, "ai"))] if os.path.isdir(os.path.join(WORK, ex, "ai")) else []
        tag = model + ("-text" if text_only else "")
        n = 1 + len([f for f in done if f.startswith(f"raw-{tag}-")])
        generate(model, ex, n, dry, text_only)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
