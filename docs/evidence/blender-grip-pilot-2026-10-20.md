# Blender hand detail and real-reference screening pilot — 2026-10-20

Scope: improve the Blender route after the owner performed a chin-up with the
wrong grip because the demonstration's hands were unreadable. Pilot exercises:
`chinup-full` and `pullup-full`; no bulk replacement of the library. Existing
R8.3/R8.4 working changes are preserved. No video-generation credits used.

## Implemented

- Anatomical left/right thumb orientation follows palm rotation, including
  supination; a world-midline clamp no longer reverses the thumb incorrectly.
  The index finger is next to the thumb and the little finger on the far side.
- Distinct blue palm pads and light hand backs on articulated finger/thumb
  geometry; face direction is visible on the two pilot mannequins.
- Chin-up wrist contact moves to the underhand side of the bar. The body shifts
  with it, keeping all sampled arm targets reachable.
- Each pilot gets a synchronized enlarged right-hand view, a palm-direction
  caption and blue-palm legend in the encoded 480×360 demonstration.
- Installer requires fresh complete close-up frames and current renderer hashes;
  encoder fails if a required inset is missing. Renderer failures exit nonzero.
- Local MediaPipe body **and hand** extraction; exact Blender camera projection;
  constrained timing alignment; per-joint angular differences and coverage;
  source/model hashes; stale-interval rejection; 3–5 distinct source requirement.
  Body tracking cannot silently substitute for a grip review.

Implementation and reproduction: [FORM_REVIEW.md](../../tools/blender/FORM_REVIEW.md).
Reference manifest: [form_references.json](../../tools/blender/form_references.json).

## Actual source videos used

| Creator | Reference | Exercise comparison |
|---|---|---|
| FitnessFAQs | [Pull-Ups OR Chin-Ups?](https://www.youtube.com/watch?v=GBcUcATb8RQ) | Both |
| Calisthenicmovement | [The Perfect Pull Up](https://www.youtube.com/watch?v=eGo4IYlbE5g) | Pull-up |
| Jeff Nippard | [Pull-up technique](https://www.youtube.com/watch?v=Hdc7Mw6BIEE) | Pull-up |
| Howcast | [How to Do a Chin-Up](https://www.youtube.com/watch?v=_71FpEaq-fQ) | Chin-up |
| ScottHermanFitness | [Weighted Chin-Up](https://www.youtube.com/watch?v=bZ6Ysk9jf6E) | Chin-up grip/upper limb only |

Downloaded public source videos are local analysis inputs in ignored `work/`;
none are included in the app or committed evidence. Overview and closer decoded
contact sheets were inspected to select demonstration intervals and exclude
obvious wrong-form examples. Exact intervals are in the manifest. Weighted
chin-ups do not establish the unweighted clip's leg motion or balance.

## What the automated comparison actually found

Three distinct videos were processed for each exercise. The body/hand models
ran on the actual source frames; these are not mock landmarks. Numerical
comparisons remain **inconclusive** because source camera views differ from the
app's camera, some bodies are cropped/occluded, and hand coverage varies widely.
There is no automatic correct-form approval.

| Exercise / source | Aligned body-measurement coverage | Frames with a detected hand |
|---|---:|---:|
| Pull-up / FitnessFAQs | 100% | 100% |
| Pull-up / Calisthenicmovement | 100% | 2.8% |
| Pull-up / Jeff Nippard | 100% | 100% |
| Chin-up / FitnessFAQs | 93.1% | 59.1% |
| Chin-up / Howcast | 94.4% | 65.5% |
| Chin-up / ScottHermanFitness | 100% | 32% |

Coverage means enough observable joint angles along the alignment path, not all
joints, accurate grip, or correct biomechanics. Full per-joint results and hashes:
[pull-up](pullup-full-reference-screening-2026-10-20.json),
[chin-up](chinup-full-reference-screening-2026-10-20.json).

## Verification and remaining acceptance

The real Blender hand-geometry check passes **96,428 sampled hands across all
533 exercise poses**, including anatomical index/thumb ordering, orthogonal axes
and finite geometry. The real renderer resume function accepts a complete current
cache and rejects missing/extra frames and stale renderer stamps (temporary
cache fixtures, separate from rendered-media and device evidence).

51 Python tests pass, including actual full-loop grip directions/reach/floor,
thumb reversal, degenerate-axis handling, timing alignment, bad range detection,
missing measurements, duplicate sources, stale analysis and incomplete/stale
inset installation. Both final Cycles loops rendered with 72 frames each. All 144 decoded MP4
frames were inspected across eight contact sheets, with additional 480×360
keyframe inspection. The two clips and thumbnails are installed in source
assets; installed MP4 hashes exactly equal the reviewed encodes. Their sizes
are 21,711 and 20,683 bytes. Every projected body joint stays in the unobstructed
left column throughout the rep. Content structure/rights validation and diff
whitespace checks pass. [Media/provenance record](blender-grip-pilot-media-2026-10-20.json) ·
[Test log](blender-grip-pilot-tests-2026-10-20.log).
No new APK was built during this media pilot.

Owner phone readability remains unverified. This is a two-exercise pilot;
remaining bundled demonstrations retain their existing renders. Source reference
angles and successful landmark extraction do not professionally validate form.
The next library pass should prioritize grip-dependent variants and then subtle
scapular/neck/foot movements, with camera-matched source segments and explicit
per-exercise visual criteria. Existing R8.4 APK packaging is a separate artifact.
