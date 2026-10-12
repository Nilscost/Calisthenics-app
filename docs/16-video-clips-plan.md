# 16 — Exercise clips: Blender vs AI video bake-off, then the owner chooses

Status: **plan, not started.** Rewritten 2026-10-12 at the owner's request: try both routes on four exercises, then the owner decides. The 2026-10-09 version planned AI video only (see git history).

Background:
- research report `/Users/nils/Documents/hermes/Apps/reports/Exercise demo video options.md` (recommends Blender; AI only as an experiment);
- today's clips: procedural Python + Pillow generator (`tools/gen_demo_clips_v2.py`); weak ones are listed in `docs/evidence/clips-v3-review.md`.

Why both: the report argues that a rigged Blender figure fixes form, loops and licences, but nobody has seen either route on our hardest exercises. Comparing three versions of the same four exercises is cheaper than arguing about it. The worked muscles don't have to be in the clip, because the body figure (v2 task V08) shows them next to it.

## Known conflicts (flagged, not resolved here)
- **Cost per AI clip.** The old plan estimated Seedance 2.0 at ≈ $0.34 per 5 s clip, using its own token maths. The report quotes ≈ $0.30 per *second*, which is ≈ $1.50 per clip. Step 3.4 measures the real price on the first clip before generating the rest.
- **Provider.** The report chose fal.ai (Kling v3 Pro, where end-frame support is confirmed). The old plan chose OpenRouter (one key for Seedance and Kling, with first and last frame listed). This plan uses **OpenRouter**, so there is one key and one script. If a model rejects `last_frame`, record that as a finding rather than working around it.

## 0. Ground rules
- **Nothing ships from this bake-off.** Today's clips stay in the app until the owner picks a route (step 6).
- **Never print or commit API keys.** The script reads `.secrets/openrouter_key` and never logs it.
- **Spend cap: $30 for the whole AI side,** enforced by the script (step 3). Stop and ask the owner before spending more.
- Work files live in `work/clips-bakeoff/` (outside git; add it to `.gitignore`). Only the review file, contact sheets, manifest and comparison videos are committed, under `docs/evidence/clips-bakeoff/`.
- `manifest.csv` records:
  - for each AI clip: model, prompt, seed, attempts and cost;
  - for each Blender clip: Blender version, rig source and licence.
- Check `git status` first, and don't work in the repo while another agent is in the middle of a task.

## 1. Exercises (4, the hardest for today's clips)
| id | Why it's hard |
|---|---|
| `pullup-full` | hands stay fixed on a bar while the body moves under them |
| `pushup-feet-elevated` | the variant's difference (feet on a chair) must be visible |
| `squat-pistol` | one-leg balance, other leg held straight in front |
| `stretch-pigeon` | floor stretch where the legs cross the body (occlusion) |

Each route makes all four clips. For a fair comparison, every route uses the same output spec:
- 4:3, 480 px wide, white background, no audio;
- a seamless loop of 2–4 s;
- H.264 Main, yuv420p, `-movflags +faststart`.

## 2. Route A: Blender (no spend)
1. **Install.**
   - Checked 2026-10-12: blender.org ships Linux builds for x64 only (5.2.2), none for arm64, and the sandbox is arm64. Running x64 Blender under emulation is possible in theory but slow and fragile.
   - If there is none, the owner installs Blender on the Mac (`brew install --cask blender`) and runs one render script that the agent writes: `blender -b -P tools/blender/render.py -- <id>`.
2. **Rig.** One CC0 humanoid rig (e.g. Quaternius; check the licence of each asset), neutral grey, no face detail.
   - Not Mixamo: its terms forbid sharing raw files in a public repo.
   - Record the source and licence in the manifest.
3. **Poses.** 3–6 keyed poses per rep, with cyclic F-curves so the loop is seamless by construction.
   - Use IK for the hands on the bar and the feet on the chair.
   - The bar, chair and floor are simple boxes.
   - Keyframe data goes in `tools/blender/poses/<id>.json` (our own data, GPL like the rest).
4. **Render.** Workbench or Eevee, flat shading, fixed 3/4 camera, then the step 4 encode. Optionally, colour the worked muscles on one exercise (via a vertex-group mask) to show whether it is worth doing.
5. **Log the hours spent per exercise.** For this route the real cost is agent time per clip, and that decides whether ~140 clips are feasible.

## 3. Route B: AI video, two models (≤ $30)
Key (2026-10-12): the Hermes egress proxy rejects OpenRouter at its secrets step (`rejected_by: secrets`, CONNECT 403) even with the key present in `.env`, so the script does NOT use the proxy. The owner made a separate key `calisthenics-clips` (OpenRouter limit $50; delete it after the bake-off), saved via `pbpaste` to `/Users/nils/Documents/hermes/.secrets/openrouter_key` (600). The script reads that file, calls OpenRouter with `--noproxy` / `proxies=None`, and never prints the key.

**Spend cap:** the script reads `usage` from `GET /api/v1/key` before every clip and stops at $30 for the bake-off.

**Live model facts (checked 2026-10-12):**
| Model | Frames | Sizes / ratios | Price | ≈ per 5 s clip, no audio |
|---|---|---|---|---|
| `bytedance/seedance-2.0` | first + last | 480p incl. 640×480 (4:3) | $7 per M video tokens | ≈ $0.25 (640×480×24 fps×5 s/1024 ≈ 36k tokens) |
| `kwaivgi/kling-v3.0-std` | first + last | 720p only, 16:9 / 9:16 / 1:1 (no 4:3) | $0.084/s | $0.42 |

So the old plan's Seedance estimate was about right and the report's $0.30/s was wrong for 480p. Kling clips are generated 1:1 at 720×720 and cropped to 4:3 in post. Worst case for the bake-off (24 clips): ≈ $8. Also listed and available if a third contender is wanted: `bytedance/seedance-2.0-mini` (half the Seedance price), `kwaivgi/kling-v3.0-pro` ($0.112/s), `bytedance/seedance-2.5`.

Agent:
1. **Re-check the models live** with `GET https://openrouter.ai/api/v1/videos/models` before the run (table above).
2. **Character.** One fixed description, reused word for word in every prompt: *"a neutral athletic adult mannequin-like figure, plain light-grey fitted clothing, no logos, no face detail, pure white seamless background, soft even light, full body always in frame, side-on 3/4 camera, fixed camera, no camera motion"*.
   - **Preferred start image:** render the start pose with the route A rig and use it here. Both routes then start from the same frame, and no image model is needed.
   - **Fallback:** make one reference image with an image model, then a start-pose image per exercise. Check each by eye: contact points, limb count, bar and chair in place.
3. **Generate** with `tools/gen_clips_ai.py`. It reads the key from the secrets file and never logs it. Field names come from Hermes's plugin `~/.hermes/hermes-agent/plugins/video_gen/openrouter/__init__.py`. The request:
   ```json
   { "model": "bytedance/seedance-2.0",
     "prompt": "<character description>. <one full rep, start and end in the same pose, steady tempo, form cues>",
     "resolution": "480p", "duration": 5, "aspect_ratio": "4:3",
     "frame_images": [
       { "type": "image_url", "image_url": { "url": "data:image/png;base64,<start.png>" }, "frame_type": "first_frame" },
       { "type": "image_url", "image_url": { "url": "data:image/png;base64,<start.png>" }, "frame_type": "last_frame" } ] }
   ```
   Then the script:
   - polls `GET /api/v1/videos/{id}` every 10 s and gives up after 15 min;
   - downloads `GET /api/v1/videos/{id}/content`;
   - keeps the raw file as `work/clips-bakeoff/<id>/raw-<model>-<n>.mp4`;
   - writes a manifest row;
   - skips ids that already have an accepted clip, so it can resume.
4. **Check the price after the first clip** on the OpenRouter activity page. The budget is 4 exercises × 2 models × up to 3 tries = 24 clips. If the real price would take 24 clips past $30, cut to 2 tries and tell the owner.
5. **Rights.** Read OpenRouter's terms and the output terms of the model owners (ByteDance, Kuaishou). Note in the review whether their output may be shipped in a public GPL-3.0 repo. A clip with unclear rights can still be compared, but not shipped.

## 4. Post-processing (both routes)
```
ffmpeg -i raw.mp4 -ss START -t LEN -an -vf "scale=480:-2,fps=24" -c:v libx264 -profile:v main -pix_fmt yuv420p -crf 30 -preset slow -movflags +faststart out.mp4
```
- Target ≤ 250 KB per clip; if a clip is bigger, raise the CRF to 32–34.
- Check the loop seam: the last frame should be close to the first. Re-trim if it jumps.
- Note that `ClipAssetsTest` caps clips at 120 KB today. Record the measured sizes; the cap is only changed in the full run.

## 5. Review (agent) and what the owner gets
For each exercise, in `docs/evidence/clips-bakeoff/`:
- `<id>-compare.mp4`: one looping video with four labelled panels side by side: **today · Blender · Seedance · Kling**;
- a 12-frame contact sheet per clip, judged with vision;
- a 1–5 score per clip on each of these:
  - the movement reads within one loop;
  - anatomy: limb count, contact points, feet on or off the floor, bar and chair in place;
  - the variant's difference is visible;
  - the loop is seamless;
  - the character stays consistent.

`bakeoff.md` ends with a decision table:

| Route | Avg score | Worst failure | KB per clip | Cost for ~140 clips | Agent time for ~140 clips | Rights | How a wrong clip gets fixed |
|---|---|---|---|---|---|---|---|
| Today | | | | $0 | — | ours | edit poses in Python |
| Blender | | | | $0 | (measured) | ours if the rig is CC0 | edit keys, re-render |
| Seedance 2.0 | | | | (measured price × tries) | | (step 3.5) | re-roll |
| Kling v3 | | | | | | | re-roll |

Plus a test APK, `Apps/builds/app-debug-bakeoff.apk`. It is a debug build, not a release. A hidden debug setting swaps the four exercises to the best clip from each route, so the owner can see them on the S21 at workout size. Skip the APK if it would take more than half a day; the comparison videos then carry the decision.

## 6. Owner decision
The options:
- **Blender for all**;
- **one AI model for all**;
- **a mix**: Blender by default, AI for clips Blender can't pose well;
- **keep today's clips**.

After the decision, a separate plan covers the full run:
- ~140 clips in batches of 20;
- `ClipAssetsTest` caps raised in the same commit, with the reason;
- `THIRD_PARTY_NOTICES.md` updated;
- full verify, ui-screens run, screenshot review, APK.

## 7. What stays the owner's check
Whether the clips feel right on the phone, and whether the technique is correct for the owner. The agent can catch obvious anatomy errors, not coaching detail.
