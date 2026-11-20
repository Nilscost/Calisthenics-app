# Push-Up Burpee: foot contact and fluidity refinement

2026-10-25. Owner feedback: “It's better but the position of the feet is a bit
weird and it's not very fluid.” Revised review candidate; not installed in the
app and no new APK is packaged by this follow-up.

The prior source laid both feet flat throughout the press and compressed a
340 mm forward / 260 mm down feet-in transfer into about 0.21 seconds. The new
source raises the heels over planted toes, gradually leaves that support after
the press, adds an intermediate feet-in descent pose and grounded settling crouch, and rises
directly into the jump preload. Repeated holds in the hop-back, bottom push-up
and elevated feet-in path are removed. The 10.5-second overall teaching pace,
short final jump, mannequin, camera and anatomical segment lengths are retained.

A read-only Luna review independently identified the abrupt .63→.65 return and
occluded feet in the previous render. Reference sheets from the previously
reviewed Peak Physio, CrossFit and Rogue videos were revisited; Peak Physio is
the closest push-up-and-jump variant. The chest/thigh-to-floor variants are not
silently copied into the strict push-up. Existing sampled-reference limits remain;
there is no new universal form score or expert approval.

## Verification

- Full Python suite: 242 tests pass. Four new regressions reject the actual
  previous flat-foot/fast-return source, a one-frame ankle pop and floating soles.
- Feet-in peak ankle speed falls from 2.739 to 1.306 m/s; peak acceleration falls
  from 34.213 to 7.494 m/s². These local engineering comparisons are not human
  biomechanical standards. Existing knee-transition gates remain at 30 m/s²;
  no floor, speed, reach or anatomy gate is loosened.
- Native all-frame Blender preflight: 252 frames, 3,822,336 vertices, no framing
  exclusions; minimum body height +3.000 mm and minimum foot +4.013 mm.
- Independent native planted-foot check: 112 foot/frame samples, actual lowest
  skin +4.929 to +5.910 mm, within the −2/+6 mm review contact range. It runs
  from `tools/check_burpee_toe_contact.py` against frozen joints, not screenshots.
- Only the Burpee trajectory changes; the other 532 trajectories are unchanged.
  Installed app media still matches the immutable R8.5 build-input manifest.
- Repository verification passes: app/domain unit tests, lint, build and
  structural/rights content validation. This is not phone verification.

The render and full decode are complete (252 frames, 480×360, 24 fps). Full
desktop playback at normal speed, replay, pause, frame-step, speed, feedback
and 736/320 px layout checks pass for all eight clips. Owner naturalness/form
and phone readability remain open. No paid generation credits were used.

A second read-only Luna pass noted that the intermediate .645 pose is still
above the floor: it is an approach pose, not a verified contact. The return
metric ends at the subsequent grounded crouch and covers that approach. The
planted push-up’s actual mesh contact is independently measured. Root inspected
rendered return frames 159/166; this remains qualitative, not owner approval.

## Review output and reproducibility

[Revised Burpee review video](../media/burpee-fluidity-review-2026-10-25.mp4).
Only Burpee is replaced in the existing eight-clip review set; the other seven
encoded clips and feedback wiring are preserved. No installed demo, thumbnail,
APK or pending publication commit is replaced.

- [Motion and render evidence](burpee-fluidity-2026-10-25.json)
- [Native rendered foot contact](burpee-fluidity-native-contact-2026-10-25.json)
- [Preservation evidence](burpee-fluidity-preservation-2026-10-25.json)
- [Python regression log](burpee-fluidity-python-2026-10-25.log)
- [Repository verification](burpee-fluidity-app-verify-2026-10-25.log)
- [Desktop playback and controls](burpee-fluidity-gallery-playback-2026-10-25.json)

The native foot-contact gate can be rerun with Blender using
`tools/check_burpee_toe_contact.py -- --input /absolute/joints.json --output /absolute/report.json`
after the usual `--background --python` launcher arguments. The input is the
frozen staged pose file, whose canonical hash and pipeline hashes are preserved
in the structured report. Geometry/contact checks do not establish a realistic
load-bearing movement or qualified exercise-form approval.
