# Upper-body repair media review — 2026-10-16

This is a read-only review of nine staged replacement MP4s. I decoded the encoded videos at 480×360, 24 fps and inspected a start, midpoint, and near-end sample from each clip. The contact sheets preserve those samples in ID order by row; the columns are start, midpoint, and near-end. Ratings describe what the encoded clip communicates at its delivery size. They are not a final release signoff.

- [Group 1: thoracic rotation, wall scapular push-up, knee scapular push-up](repair-upperbody-review-2026-10-16-group-1.png)
- [Group 2: full scapular push-up, support shrug, supine chin nod](repair-upperbody-review-2026-10-16-group-2.png)
- [Group 3: chin tuck with head lift, seated chin tuck, manual-resistance head raise](repair-upperbody-review-2026-10-16-group-3.png)

## Findings

| Clip | Score | Encoded-video finding |
|---|---:|---|
| `stretch-thoracic` | 4/5 | The torso center and long axis stay planted while the rib-cage left axis rotates from `[0, .906, .423]` to `[0, .866, -.500]`; the shoulder line rolls with it as the free arm opens. This is the intended axial rotation without shifting the hips. At this small output size, the moving arm is more conspicuous than the rolling shoulder girdle, but the clip does show the rotation. Confidence: medium-high. |
| `scapular-pushup-wall` | 3/5 | Hands stay fixed on the wall and arms remain straight. The chest shifts about 3.5 cm over the cycle, but the shoulder line and upper-back contour barely change in this side view. The setup is clear; the targeted protraction/retraction is only moderately legible. Confidence: medium-high. |
| `scapular-pushup-knee` | 3/5 | The kneeling plank and straight arms read correctly. Chest travel is about 2.5 cm while wrists, shoulders, knees, and toes remain fixed; the shoulder contour changes little, so the motion can read as a small whole-torso shift rather than scapular glide. Confidence: medium. |
| `scapular-pushup` | 3/5 | The straight-arm plank is clear and the chest sinks/rises by about 3.4 cm with fixed hand and toe contacts. In the three-quarter view, the shoulders and back silhouette barely separate between samples; it is real motion but not a clear demonstration of “push the floor away.” Confidence: medium. |
| `support-scapular-shrug` | 4/5 | The straight-arm support on dip bars is recognizable. The torso/head lower by about 5 cm and rise again while the hands remain fixed, clearly showing the shrug pattern. The bars cross in front of the torso but do not obscure the shoulder movement. Confidence: medium-high. |
| `chin-nod-supine` | 3/5 | The occiput stays grounded as cued. The near-fixed head center is intentional: the head shifts relative to the neck by about 8.6 mm at peak (roughly a 5° craniocervical nod), while nose/chin details show the direction. The nod is subtle at 480×360 but appears mechanically correct; no head lift is visible. Confidence: medium. |
| `chin-tuck-head-lift` | 4/5 | The head lifts about 2.5 cm while the torso stays down, then returns. The nose/chin profile is visible and helps the tuck direction read in the close side view; the short range matches the cue. Confidence: medium-high. |
| `chin-tuck-seated` | 3/5 | The seated figure stays tall while the head moves back about 3.5 cm relative to the chest. Nose/chin detail makes the backward glide direction legible; there are no eyes to verify the “eyes level” cue directly. Confidence: medium. |
| `neck-head-raise-manual` | 4/5 | The head rises toward the chest while a hand stays at the forehead, then lowers. The lift is visible. The hand follows the head rather than visibly resisting it, so the “manual resistance” part remains implied by contact instead of demonstrated. Confidence: medium. |

No source or staged media was modified during this review. The earlier UI screenshot run is still in progress and is tracked separately; this document does not sign off that run.

## Scapular clarity follow-up

The three scapular push-up clips retain planted hand/foot or knee contacts and straight elbows, so increasing torso travel substantially would undermine the intended small isolated movement. A camera-only improvement worth testing is a tighter, lower three-quarter side crop centered on shoulders, upper back, hands, and (for the floor versions) toes/knees; the current floor view leaves the whole body prominent and the shoulder contour small. The wall clip already reads in profile, so a closer crop around shoulders and wall contact is more promising than a new angle. Even with that framing, the rig has no scapula joint: if the upper-back contour still does not visibly distinguish the action, keep the limitation explicit in the teaching copy rather than enlarging the motion into a push-up.

## Staged-media identity

I decoded the nine MP4s under `work/r8-video-repair-2026-10-16/staged/<id>/blender/<id>.mp4`, not the canonical clip cache. The source-joints hashes match `inputSha256` and the encoded MP4 hashes match `sha256` in `work/r8-video-repair-2026-10-16/encoded-repairs.json` for all nine IDs:

| Clip | Source joints SHA-256 (`inputSha256`) | Staged MP4 SHA-256 (`sha256`) |
|---|---|---|
| `stretch-thoracic` | `ebaea865088b0552d30cfd24a3626942ccd2be285028b0b2f8c2c881151f4c97` | `236f4e381f1eabd3954b059b776e2913cb7e36d0ad9bd702a9487ee0ebf22eab` |
| `scapular-pushup-wall` | `06abc548f877bac09fbf2e06e979b187ecc722dd08ea07f29cd05cbd495bfb8f` | `b760224cabf7c48aa42447c0c8ee9f33f5f8477408d27d7b38da5e77e90fe7a3` |
| `scapular-pushup-knee` | `be199408938253dc2efa44e2e4de11b8fc1d540e9e188e20800e6368ec9f884d` | `7eefa9e39fda970eca175eb17ddde1906ca8a40cb6dce2d2d13cd356608966cb` |
| `scapular-pushup` | `aae662c9728865d4c50574474720170efef5da0d6056c8943a30b4f3a180fcb5` | `15331a51794a7cc8ad693ba644b7350c9e897a241e9059203854386c7a9a4066` |
| `support-scapular-shrug` | `4aa4545d48b824b46d153d37b5ce7604236b09ce0d11f31bbc76cd40947fcde3` | `34543537a5fc9a5adc88c62fb5b35c46c60cd65d7bf566243a352ed3ced202b5` |
| `chin-nod-supine` | `c42d88880f615fdfe870c60af05ac56389d98bc032d2d9ed4e276b7334c33dc3` | `e08c196a43847f92824384399071724eacf8489a4d3a15474a4e4b9c0949e4f7` |
| `chin-tuck-head-lift` | `47f99b9a81abdaafb8d172a1a428919910bbe59151ddd036b596574b4f5b3d90` | `0fb81a3240118a853bf83353701aed27b98a5b88e9d4ae338b1412e97d15a1db` |
| `chin-tuck-seated` | `2688cb19b2b412150a9899be3a8860c6d80774bd0e56e23c5597c514fb35c0d3` | `f7618035e9cb35d63a9ec5b3a74e1c1cc625e4dc26f9446ac1db42382e0c67c9` |
| `neck-head-raise-manual` | `d4d22059c932fe31bcb7e68a96f90209fb07f0a97cbef65588f66037687bf8b9` | `a6fba30df6869d925a5ad04f3b160a38bde7493da5b38a8b32da07cd9e8b4d5c` |
