# 16 — Exercise clips with AI video (Seedance via OpenRouter): steps

Status: **plan, not started.** The owner runs it from Hermes later. Written 2026-10-09 by the reviewer.

Background:
- research report `/Users/nils/Documents/hermes/Apps/reports/Exercise demo video options.md`;
- live OpenRouter model list checked on 2026-10-09.

Why AI video is back on the table:
- Seedance 2.0 / 2.0 Fast / 2.0 Mini, Kling v3 and Veo 3.1 on OpenRouter all accept a **first and a last frame**. The same image for both gives a clean loop.
- The worked muscles no longer need to be in the clip, because the body figure (v2 task V08) shows them next to it.

## 0. Ground rules
- **Do not replace the shipped clips until the bake-off (step 3) has been reviewed by the owner.** Today's clips stay the fallback.
- **Never print or commit API keys.** The key lives in `/Users/nils/Documents/hermes/.secrets/openrouter_key` (mode 600) and in Hermes's `.env`.
- Generated clips are **DRAFT** like all content.
- Record the model, prompt, seed and cost per clip in `docs/evidence/clips-ai/manifest.csv`.
- Rights: before shipping, read OpenRouter's terms and ByteDance's Seedance (or Kling's) output terms. Record in `THIRD_PARTY_NOTICES.md` what applies. The repo is public and GPL-3.0. If the terms don't allow redistribution, stop and ask the owner.
- Coordinate with the executor:
  - Clips live in `app/src/main/assets/demos/<exerciseId>.mp4`. Thumbnails live in `app/src/main/assets/thumbs/<id>.webp` (after v2 task V06a).
  - `ClipAssetsTest` caps a clip at **120 KB**. AI clips need a higher cap (e.g. 300 KB) and a total cap (e.g. 30 MB). Change the test in the same commit and say why.
  - Check `git status` first, and don't work in the repo while the executor is in the middle of a task.

## 1. One-time setup (owner)
1. On https://openrouter.ai, sign in, buy **$20–25 of credits**, and create an API key at https://openrouter.ai/settings/keys. Optionally set a credit limit on the key (e.g. $25).
2. Hermes:
   - run `hermes config env-path` to see which `.env` Hermes uses;
   - add the line `OPENROUTER_API_KEY=<key>` there;
   - run `hermes tools` → **Video Generation** → choose **OpenRouter**;
   - optionally, in the same menu, choose OpenRouter for **Image Generation** too.
3. For scripts, save the same key: `printf %s '<key>' > /Users/nils/Documents/hermes/.secrets/openrouter_key && chmod 600 /Users/nils/Documents/hermes/.secrets/openrouter_key`.
4. Check that it works, without printing the key:
   ```
   curl -s https://openrouter.ai/api/v1/videos/models | python3 -c 'import json,sys; d=json.load(sys.stdin); d=d.get("data",d); print([m["id"] for m in d if "seedance" in m["id"]])'
   ```
   For a credit check: `curl -s -H "Authorization: Bearer $(cat /Users/nils/Documents/hermes/.secrets/openrouter_key)" https://openrouter.ai/api/v1/credits`.

**Hermes limit:** Hermes's `video_generate` tool (OpenRouter plugin) sends only a **first** frame. It is fine for quick looks, but looping clips need the script in step 4, which also sends `last_frame`.

## 2. A consistent character (once)
1. Write one character description and reuse it verbatim in every prompt. Suggestion: *"a neutral athletic adult mannequin-like figure, plain light-grey fitted clothing, no logos, no face detail, plain light studio background, soft even light, full body always in frame, side-on 3/4 camera, fixed camera, no camera motion"*.
2. Make a **reference image** of that character (Hermes image generation, or an OpenRouter image model that accepts a reference image).
3. Per exercise, make a **start-pose image** from the reference: same character, same background and camera, in the exercise's start position (e.g. "top of a feet-elevated push-up, feet on a chair, hands on the floor"). Check it by eye: correct contact points, the right number of limbs, and equipment (chair, bar) in the right place.
4. Save it as `work/clips-ai/<exerciseId>/start.png`. The `work/` folder is outside git; add it to `.gitignore` if needed.

## 3. Bake-off (about $5–10), before anything else
- **Exercises**, the five hardest for today's clips:
  - `pullup` (or the catalog's pull-up id);
  - the feet-elevated push-up;
  - a pigeon or other floor hip stretch;
  - the pistol squat;
  - plank shoulder taps (if not in the catalog yet, use a plank variant).
- **Models**, all at 480p, 5 s, no audio, first frame = last frame = the start pose:

  | Model | Est. cost per 5 s clip |
  |---|---|
  | `bytedance/seedance-2.0` | ≈ $0.34 |
  | `bytedance/seedance-2.0-mini` | ≈ $0.17 |
  | `kwaivgi/kling-v3.0-std` | ≈ $0.42 |

  The Seedance figures are estimates (billing is per video token, assumed to be w×h×fps×s/1024); the Kling figure is the listed $0.084/s × 5 s. Check the real cost in the OpenRouter activity page after the first clip.
- **Review every clip**, frame by frame (extract ~12 frames with ffmpeg and look at them), against the quality bar of plan §5.1:
  - Does the movement read within one loop?
  - Is the anatomy plausible: limb count, contact points, feet on or off the floor as they should be, the bar or chair in the right place?
  - Is the variant's difference visible?
  - Is the loop seamless?
  
  Score each 1–5 next to today's clip in `docs/evidence/clips-ai/bakeoff.md`, with contact sheets.
- **Owner decision:** go or no-go, and which model.

## 4. Generation script (direct API, supports loops)
`tools/gen_clips_ai.py`:
- reads the key from the secrets file;
- never logs the key;
- for each exercise, POSTs to `https://openrouter.ai/api/v1/videos`.

```json
{ "model": "bytedance/seedance-2.0",
  "prompt": "<character description>. <exercise motion: one full rep, start and end in the same pose, steady tempo, correct form cues>",
  "resolution": "480p", "duration": 5, "aspect_ratio": "4:3",
  "frame_images": [
    { "type": "image_url", "image_url": { "url": "data:image/png;base64,<start.png>" }, "frame_type": "first_frame" },
    { "type": "image_url", "image_url": { "url": "data:image/png;base64,<start.png>" }, "frame_type": "last_frame" } ] }
```

Then:
1. Poll `GET /api/v1/videos/{id}` every 10 s until the status is `completed` or `failed` (give up after 15 min).
2. Download `GET /api/v1/videos/{id}/content` with the bearer key.
3. Keep the raw file in `work/clips-ai/<id>/raw-<model>-<n>.mp4`.
4. Write a manifest row.
5. Skip ids that already have an accepted clip, so the script can resume.

The field names come from Hermes's OpenRouter plugin (`~/.hermes/hermes-agent/plugins/video_gen/openrouter/__init__.py`). Check `aspect_ratio` against the model's `supported_aspect_ratios` in `/videos/models`.

## 5. Post-processing (keeps the APK light)
Trim to the cleanest 2–4 s loop, scale to 480 wide, remove audio, and encode H.264 for Android minSdk 26:

```
ffmpeg -i raw.mp4 -ss <start> -t <len> -an -vf "scale=480:-2,fps=24" -c:v libx264 -profile:v main -pix_fmt yuv420p -crf 30 -preset slow -movflags +faststart out.mp4
```

- Target ≤ 250 KB per clip, and ≤ 30 MB for all clips. Raise the CRF to 32–34 if a clip is bigger.
- Make the thumbnail from a key frame: `ffmpeg -ss <t> -i out.mp4 -frames:v 1 -vf scale=240:-2 thumb.webp`.
- Check the loop seam: the last frame should be close to the first. Re-trim if it jumps.

## 6. Full run (after a "go")
1. Generate in batches of about 20.
2. Review each clip, using the same 1–5 scoring as the bake-off.
3. Retry failures up to 3 times with a better start image or prompt, then fall back to today's clip and list it as rejected.
4. Budget: about $50–100 for ~100 clips with retries at Seedance 2.0 / Mini prices. Stop and ask the owner if the spend passes $100.

Ship as one commit:
- the clips and thumbnails;
- `ClipAssetsTest` caps updated;
- the manifest and review file;
- `THIRD_PARTY_NOTICES.md`.

Then:
1. Run the full verify and the ui-screens run.
2. Do a screenshot review of the workout screen with several clips.
3. Build the APK.

## 7. What stays the owner's check
Whether the clips feel right on the phone, and whether the technique shown is correct for the owner. The agent can catch obvious anatomy errors, not coaching subtleties.
