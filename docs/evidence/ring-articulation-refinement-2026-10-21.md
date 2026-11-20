# Ring muscle-up moving-hand follow-up — 2026-10-21

Status: **review candidate; hand rig still needs refinement; no app promotion or new APK.**

The owner's frozen/unnatural hand report led to explicit moving ring centres, independent hoop planes, changing grip arcs and forearm-relative wrist/hand frames. Fixed suspension anchors and constant strap lengths constrain the moving apparatus. Full hang/support extension, quiet legs and the existing no-lurch gates remain. A false-grip heel-over-wood/down-palm identity screen rejects the earlier underhook draft. A phase-specific wrist check preserves deliberately flexed hang and straighter support; the old 115° support error remains rejected.

A native check caught a separate renderer mismatch: authored finger endpoints changed, but capsule meshes retained their first-frame lengths. Dynamic-ring finger meshes now follow the actual endpoints without stretching cap radii. Legacy finger rendering is unchanged.

## Evidence and limits

- 205 Python tests pass in the existing pose-analysis environment, including independent old-motion, missing-ring, frozen-frame, detached-contact, wrist-twist, palm-intrusion and moving-capsule controls. See `ring-articulation-tests-2026-10-21.log`.
- Native all-frame check: 192 frames, 1,267,200 actual hand vertices; minimum hand/wood clearance 8.903 mm; conservative arm clearance 3.592 mm; whole-finger capsule clearance 2.804 mm; remaining body vertex clearance 14.665 mm. See `ring-articulation-geometry-2026-10-21.log`. Positive clearance proves separation, not grip contact or anatomical correctness.
- Scripted parameter searches and native meshes rejected several plausible-looking drafts. They did not relax the existing reach, body-motion or collision gates.
- **Unresolved:** the procedural finger wrap changes proximal finger lengths. The explicit proportion report flags this, and the skin/wood gap still needs refinement. A mathematically exact declared grip proxy is not actual skin contact. This candidate must not be installed based on collision scores. See `ring-articulation-proportions-2026-10-21.json`.
- Five original-source reference intervals were inspected in decoded frames. GMB's assisted transition and continuous negative, FitnessFAQs' false-grip close-up, and Rogue's swinging sequence provide different qualitative evidence. No reviewed interval establishes three clear complete unassisted strict cycles with visible hands. No calibrated numerical form comparison or professional approval is claimed. Detailed identities, source hashes and intervals: `work/ring-articulation-refinement-2026-10-21/reference-review/report.md`.

The new movement is intended for review of the hand/body/apparatus relationship. The hand rig issue remains open even if media decoding, framing and desktop playback pass. Phone-size readability, owner acceptance and expert form review remain separate checks.

## Render completion

Both isolated clips completed native rendering, encode and full decode on
2026-10-22 after restarting a failed Depth Jump Blender process. The matching
source/data hashes and media facts are in
[the combined evidence](ring-depth-review-refinement-2026-10-21.json).
Selected actual encoded phases were visually inspected; this is not a claim
of continuous expert form review. Installed app media remains unchanged.

Desktop gallery playback/control verification passes for all eight clips.
Ring, Depth Jump and Burpee played completely at 1×; the five unchanged clips
played completely at 2×. Replay, pause, frame stepping, speed control, saved
selection/feedback restoration and 320/736 px layout checks pass with no page
errors. This is media/UI verification, not continuous visual form approval or
actual phone verification. See `ring-articulation-gallery-playback-2026-10-21.json`.
