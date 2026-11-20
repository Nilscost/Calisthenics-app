# Straddle Press foot correction — 2026-10-21

Owner feedback: the feet change direction and should stay consistent with the
body in `handstand-press-straddle`. Implementation began on October 20 and
finished after the owner's October 21 resume request. The positive Bar Pullover
follow-up (“Much better”) is recorded separately; that clip remains unchanged.

## Correction and scope

Previously both toe axes stayed at world +X while the body inverted. The heel
frame was derived from the shin projected perpendicular to that fixed axis.
The source also contained only the ascent: the loop reset directly from the
handstand to its starting floor pose.

The feet now rotate continuously with press progress, from the initial flat stance
into a pointed finish aligned with the raised legs. The final toe/shin cosine
is 0.93833. A first candidate still looked too bent away from the leg line and
was rejected before installation; the final candidate addresses that observation.
The source keeps the existing torso, leg-spread, planted-hand and camera formulas.
It adds a complete matching descent and smooth endpoint ramps. Eight seconds
contain a 3.2-second ascent and descent, a 0.64-second top hold and 0.96 seconds
of combined bottom holds. There is no sudden reset at the loop boundary.

Only this exercise and its thumbnail are replaced. No Android behavior, APK,
model migration or catalog review-state change is part of this correction.
All 532 other source trajectory hashes and installed video hashes are checked
against a pre-change baseline. No video-generation credits are used.

## Automated checks

105 Python tests pass. Regression controls reject the actual old world-fixed
feet and reset, fixed-world toes on the otherwise corrected body/timeline,
a one-frame toe flip, a degenerate or collinear foot frame and sliding hands.
The matching return is checked against the ascent. Articulation report tests
cover straight, bent, reversed and degenerate wrist/hand axes, and ensure no
form approval can be returned from those measurements.

The scoped foot-direction contract checks initial flat toe direction, total
continuous foot turn, angular speed, heel-frame conditioning and continuity,
pointed top alignment and this authored ankle-angle range. These are engineering
regression bounds for this candidate, not universal anatomical limits.

Source metrics: foot turn 250°, maximum foot angular speed 88.882°/s, minimum
heel-projection norm 0.31506, consecutive heel-normal cosine at least 0.997921,
maximum tracked joint speed 0.92848 m/s and zero stored loop gap.

Actual Blender checks cover 2,912,256 body vertices within the app camera and
151,296 foot vertices over all 192 final frames. Minimum foot/floor clearance
is 9.423 mm. Rendered foot/toe direction agrees with source at cosine 0.999717
or better; the actual heel normals do not flip. These are vertex, framing and
orientation checks, not a full body collision or expert form assessment.

## Wrist realism follow-up

The saved owner note says the Ring Muscle-Up wrist/hand position looks unnatural
and asks that articulation be added to the realism checks. `FORM_REVIEW.md` now
requires reviewing the forearm–wrist–hand chain, palm direction, flexion/deviation,
loaded grip and contact continuity through every phase. A compact local report
records wrist-angle extrema, frame/time and the largest frame-angle change.

The current ring clip reaches a simplified forearm-to-fingertip axis bend of
115.668° at frame 64 (2.667 s), with an 11.504° largest frame change. This is a
review locator, not a physiological diagnosis: the hand axis is a simplified
fingertip direction rather than a precise skeletal palm axis. The report always
requires human review. The ring asset is unchanged and that concern remains open.

## Review limitations

Luna reviewed selected actual Cycles frames of both candidates, rejecting the
first candidate's still-bent finish. The final sampled toes read more pointed
and better aligned with the legs; no material issue was seen in those samples.
That review does not establish continuous playback, phone readability or expert
form approval. No new three-to-five-reference numerical video comparison was
performed. Owner and professional form acceptance remain separate.

## Installed media and desktop playback

The silent 480×360 H.264 Main/yuv420p encode is eight seconds, 192 frames at
24 fps and 32,032 bytes. Every frame decodes; the installed encode exactly
matches the reviewed file. All 192 final render frames postdate the final
render start and provenance matches the current renderer and contracts. The
rejected earlier partial render is not used. Installation dry run, actual
installation and structural content validation pass.

Desktop playback reaches the end and replays the complete eight-second clip
at 2×. The unchanged eleven-second Pullover also passes full playback and replay.
All eight review clips decode; play/pause, frame stepping, playback rates,
feedback retention, 320/736-pixel layouts and runtime checks pass. No feedback
message was sent by these tests. The encoded first/last mean pixel difference
is 0.298 on a 0–255 scale; the stored source endpoints match exactly.
These are desktop/media checks, separate from phone or expert form verification.

The fresh whole-catalog screen reports 108 clips with generic review flags and
425 without those flags; five scoped contracts pass and 528 remain unscoped.
No review category approves exercise form.

[Summary, measurements and bounded Luna review](straddle-foot-refinement-2026-10-21.json) ·
[Tests](straddle-foot-tests-2026-10-21.log) ·
[Actual Blender geometry](straddle-foot-geometry-2026-10-21.log) ·
[Final full render](straddle-foot-render-2026-10-21.log) ·
[Desktop review checks](straddle-foot-gallery-qa-2026-10-21.log) ·
[Wrist articulation locators](articulation-review-2026-10-21.json) ·
[Revised video](../media/straddle-press-feet-2026-10-21.mp4).
