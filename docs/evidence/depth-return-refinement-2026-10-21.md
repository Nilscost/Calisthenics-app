# Depth Jump height and return follow-up — 2026-10-21

Status: **review candidate; no app media replacement or new APK.**

The owner reported a weak rebound and unnatural walking. The rebound now lasts 0.625 s and follows gravity, with a theoretical pelvis rise of 0.479 m instead of 0.307 m. The authored stronger-variant height bound changes explicitly to 0.40–0.55 m; all other ballistic, contact, takeoff-extension, arm-swing and wrist safeguards remain. There is no held apex.

The sideways/backward walk around the box is replaced with two small forward steps, alternating planted steps to turn toward the box, a sequential step-up, a second planted turn on the platform and a short alignment to the loop's opening pose. Feet lift before advancing across the solid box edge. Body, knees, hands and foot headings turn together. The platform remains 30 cm high; its footprint is slightly wider to accommodate the return turn.

## Verification

- 205 Python tests pass. New independent controls reject the actual old crab walk, lower rebound, backward-facing travel, sliding or twisting support feet and a missing return turn. The held-contact negative control still independently fails the contact rule even when its inserted hold also exceeds loop duration.
- Reset travel alignment is 1.0, planted-sole slip is zero within authored rounding, and planted heading change is about 0.000001°. These are trajectory checks, not a proof of natural gait or balance.
- Dedicated all-frame actual mesh screen covers 210 frames: no body vertices inside the solid box beyond the 2 mm margin, and minimum floor height is 4.013 mm. Batch preflight also checks final app framing.
- All three existing filmed references were re-inspected through their decoded sheets: Catalyst Athletics, Wil Fleming and Third Space London. Catalyst's 11.0–12.8 s return supports turning toward the box and stepping up directly. Different cameras, variants and sparse sampling prevent calibrated height/joint comparisons. The new metre value comes from the gravity model, not measurement of the athletes. Details: `work/depth-return-refinement-2026-10-21/reference-notes.md`.
- Structural content validation passes. It does not approve exercise form.

This follow-up changes only the Ring Muscle-Up and Depth Jump source trajectories. The other 531 trajectories and all 1,066 installed demonstration/thumbnail files are preserved against the start-of-follow-up hashes. The previous installed Depth Jump remains in place until review. Actual phone readability, owner acceptance and expert form review remain open.

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
