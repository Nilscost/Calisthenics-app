# Two-exercise motion-quality pilot — 2026-10-20

Implemented and installed source demonstrations for `pushup-archer` and
`pullup-full`, preserving the existing R8.3/R8.4 and hand-detail work. No APK was
rebuilt. No video-generation credits or paid generation APIs were used.

## Changes and measured results

| Measure | Before | After |
|---|---:|---:|
| Archer assisting elbow at the low position | 116.5° | 170.4° |
| Archer chest travel | 8 cm toward one side | 18.8 cm toward each side |
| Archer planted-hand displacement | up to 10 cm | under rounding precision |
| Pull-up torso backward inclination at top | 2.2° | 16.8° |
| Pull-up change in torso inclination | 2.4° | 12.2° |
| Pull-up shoulder action relative to torso | effectively zero | 3.5 cm |

The archer shows alternating right/left repetitions, fixed wide palms and a
plank/foot pivot that leaves toe anchors planted. The pull-up shows shoulder
initiation, intermediate pulling and a controlled mildly arched top position.
Both use a continuous torso mesh and clearer body contrast. The pull-up retains
the enlarged grip view. These remain stylized mannequins, not force simulations.

Full measurements, hashes, before/after negative controls and per-source
comparisons: [machine-readable evidence](motion-quality-pilot-2026-10-20.json).
[Before/after image](../media/motion-quality-pilot-2026-10-20.png).

## Verification actually performed

- **64 Python tests pass**, including frozen actual pre-fix poses, a normal
  push-up relabelled as an archer, bent assisting arm, wrong grip, moving hands,
  teleporting joints, invalid geometry, stale camera projections, failed-motion
  installation and passing motion with stale source metadata.
- Real Blender checks cover **96,620 sampled hands across 533 poses**, plus
  **76,800 torso vertices across all 240 pilot frames**: closed torso mesh,
  no torso/floor penetration, app framing and grip-inset clearance.
- Batch gate: **2 pass, 0 fail, 531 unscoped**, measured at 0.057 seconds for the
  scoped builds/checks and unscoped enumeration. This does not approve 533 motions.
- Six actual source-video analyses were processed and replayed as **six cache
  hits**, without repeating landmark inference. Three distinct videos per
  exercise; body and hand landmarks, timestamps, confidence and model/source
  hashes retained in ignored work files.
- Final Cycles render: archer **144 frames / 6 seconds**, pull-up **96 frames /
  4 seconds**. Both decode fully as silent H.264 Main, yuv420p, 480×360, 24 fps.
- Encoded inspection: 10 archer and 9 pull-up key phases, plus the maximum
  decoded frame-step image for each. All frames were decoded and numerically
  checked; there is no claim of manually inspecting every rendered pixel.
- Installed MP4 bytes exactly equal the locally reviewed encodes. Archer is
  **47,669 bytes**, pull-up **22,222 bytes**, both below the 120 KiB cap. Fresh
  exact frame sequences, current source joints, motion checks, renderer hashes
  and both real renderer resume checks pass. Original assets are preserved in
  ignored `work/motion-quality-pilot/baseline/`.
- Content structure/rights validation and diff whitespace checks pass. Android
  source behavior was not changed; no new Gradle/APK/device run was performed.

Logs: [tests](motion-pilot-tests-2026-10-20.log),
[hands](motion-pilot-hand-geometry-2026-10-20.log),
[torso/framing](motion-pilot-torso-geometry-2026-10-20.log),
[reference cache](motion-pilot-reference-cache-replay-2026-10-20.log),
[render](motion-pilot-render-2026-10-20.log),
[cache provenance](motion-pilot-cache-check-2026-10-20.log).

## Reference results and limits

Archer references are FitnessFAQs, Minus The Gym and Calisthenicmovement.
Pull-up references are FitnessFAQs, Calisthenicmovement and Jeff Nippard.
The manifest records source links, selected rep intervals and approximate
comparison cameras. Diagnostic projections are computed without additional
video rendering. Candidate cycles match the source's one or two repetitions.

Aligned mean angular differences remain **9.3–28.8° for archer references** and
**10.3–22.3° for pull-up references**. These are residual screening differences,
not acceptance scores. Source cameras are manually estimated, not calibrated;
individual proportions, joint tracking, occlusion, depth and technique variants
remain confounders. A threshold pass on our authored movement rules is separate
from agreement with reference footage. There is no automatic form certificate.

Owner phone readability and form acceptance remain open. The remaining 531
clips are unchanged by this pass. Expansion requires movement-family rules,
negative controls and appropriate references; copying bounds is insufficient.
Reproduction and script workflow: [document 36](../36-motion-quality-pilot.md).
