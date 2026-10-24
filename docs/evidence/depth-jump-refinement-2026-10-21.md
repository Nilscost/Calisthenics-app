# Depth Jump feedback refinement — 2026-10-21

Owner feedback: arms looked unnatural, the jump lasted too long and the person
appeared to fly. The gallery was showing the original eight-second installed
clip, not the separately rendered 6.79-second batch draft. Both old trajectories
are retained as distinct durable gzip fixtures; both fail the new physics/arm
checks. No old result is relabelled as the new candidate.

## References actually inspected

Three original coaching demonstrations were downloaded into ignored work space
for analysis only; none of their footage ships in the app:

- [Catalyst Athletics](https://www.youtube.com/watch?v=GeN0S3XCZnM), selected first arm-swing rep around 9.1–11.1 s; [creator instructions](https://www.catalystathletics.com/exercise/551/Depth-Jump/).
- [Wil Fleming](https://www.youtube.com/watch?v=AzPJZHOmGEg), first attempt around 9.0–10.9 s. Later repeated/slower sequence is excluded from timing interpretation.
- [Third Space London](https://www.youtube.com/watch?v=DxzbXy0lC6Y), active movement around 1.2–3.6 s. Starts with arms overhead and uses deeper loading; it is not an identical contact prescription.

Actual source frames were inspected, including dense phase sheets. This is not a
claim that every full reference file was watched continuously. References differ
in camera, athlete, box height, emphasis and possibly playback speed. They inform
arm sequencing and visual review; their pixel positions are not metres.

The existing local pose pipeline extracted 24, 23 and 29 frames respectively at
12 Hz. Body detection coverage was 100%; hand coverage was 0%, 0% and 6.9%.
Camera-projected comparison used only the active candidate interval, excluding
its walking return. All three numerical comparisons remain **inconclusive**
because views and variations differ. Detection coverage does not approve form.
Reference metadata, hashes, intervals and those limits are retained in
`tools/blender/depth_jump_references.json` and the work reference-review report.

## Authored correction

The intended variant keeps the catalog's 30 cm box, steps into a bilateral drop,
absorbs briefly and rebounds vertically. The arms start relaxed/back, drive
forward/up with the rebound, then lower through landing and recovery. Hand axes
follow the actual solved forearms throughout the loop, including the walking
return. A constant world hand direction is rejected.

The rebound uses a sampled ballistic pelvis/COM-proxy curve under 9.81 m/s²,
without easing to a stop at the apex. The target takeoff-to-touchdown interval is
0.50 s and its rise is about 0.307 m. The longer overall clip includes recovery
and walking back onto the box; it does not stretch the airborne jump.
A pelvis proxy is not a complete mass/force simulation of a human.

## What the review caught during implementation

A gravity fit inside the airborne phase was insufficient: the first candidate
joined a low drop-contact pelvis height to flight and left the knees too bent at
takeoff. Root stopped that isolated render after viewing actual native phases.
The correction requires leg extension and a continuous ground-to-flight path,
not merely a valid parabola between its endpoints. That finding gains independent
regression coverage. The earlier actual mesh preflight was a geometry pass, not
an exercise-form approval.

Whole-cycle guards also retain bilateral drop, no premature single-floor support,
short contact, absorption, planted-foot drift, neutral wrists, proper arm drive,
no held apex, physical return and source/render provenance. Historical depth
checks tied to fixed percentages of the old loop were retired; the new checks
locate actual contact events. Pistol checks and its output are preserved.

## Isolation and verification

The eight-item batch from the earlier task continues from a byte-identical frozen
copy of all its Python/JSON inputs. Every snapshot hash was checked against its
manifest before restart. Completed ring and old depth draft outputs were retained;
new work does not silently alter that batch's inputs. Only a task-owned refinement
runner was stopped when its takeoff issue was found.

Final native render, encoded playback, installation and gallery results are
recorded below when complete. Until then, this document does not claim a new
installed demonstration, device verification or expert form approval.

## Completed verification and delivery

- 169 Python tests pass, including both actual old trajectories and independent
  floating/apex, contact, arm, wrist, leg-extension and launch-boundary faults.
- Native geometry examines all 198 frames and 3,003,264 actual body vertices:
  zero cropping, minimum floor clearance 4.010 mm. These are geometry checks.
- Final takeoff knee extension is 162.7 degrees; planted absorption lasts 0.25 s;
  the takeoff-to-touchdown arc lasts 0.50 s, rises 0.30656 m and fits 9.8103 m/s².
  Source time-event boundaries carry one-frame (1/24 s) sampling uncertainty.
- The final silent 480×360, 24 fps H.264 Main/yuv420p video has 198 frames,
  duration 8.25 s and size 38,401 bytes. Full decode passes.
- Dry-run and actual established installation pass. Only `depth-jump.mp4` and
  its thumbnail changed; the other 1,064 media assets and 532 trajectories are
  unchanged from this feedback task's baseline. All R8.3/R8.4 work is preserved.
- All 533 installed videos decode without a technical error. Generic screening
  retains 110 review prompts; stale default render-cache prompts are not form
  failures or evidence against the fresh isolated depth render.
- The refreshed eight-candidate gallery uses actual completed render hashes.
  Every clip played to its end and replayed in an isolated desktop browser.
  Pause, frame stepping, speed, feedback retention, saved-state event restoration,
  320/736px layouts and absence of runtime errors pass. This is desktop playback
  mechanics, not actual phone or professional form verification.
- The original eight-item batch is complete. Its older depth draft is superseded
  in the gallery by this correction. Other seven new renders are review candidates,
  not bulk-installed app replacements. No new APK was packaged.

[Corrected clip](../media/depth-jump-refined-2026-10-21.mp4),
[encoded phase sheet](../media/depth-jump-encoded-2026-10-21.jpg),
[structured evidence](depth-jump-refinement-2026-10-21.json),
[desktop playback results](depth-jump-gallery-playback-2026-10-21.json).

The owner confirmed the eight-to-ten clip size and existing appearance. Prioritize
owner review of Depth Jump, Ring Muscle-Up, Burpee, Roll-Over and Windshield Wiper.
Pistol is a further balance/contact review candidate; accepted Pullover and
Straddle Press motions remain useful regression controls. Numeric reference
comparison remains inconclusive; phone readability and expert form approval are
still open. No paid generation credits were used.
