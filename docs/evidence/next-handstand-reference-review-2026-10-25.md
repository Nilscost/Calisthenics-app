# Handstand reference review

Reviewed decoded local MP4 frames from five sources. The contact sheets are in
`sheets/`; files in `media/` are analysis copies with the adjacent yt-dlp info
JSON. SHA-256 hashes below identify the exact downloaded encodes. I inspected
the listed timestamps as still frames; they are not full-video playback claims.

## Wall walk-up, toes-only

The catalog describes a push-up start with feet at the wall, walking feet up and
hands in until nearly vertical with toes alone touching, then a controlled walk
back down. Three original coaching/demo sources provide useful but different
coverage:

| Source | Reviewed interval and visible evidence | Limits |
|---|---|---|
| [CrossFit, “The Wall Walk”](https://www.youtube.com/watch?v=NK_OcHEm8yM) | 21–45 s, 23.976 fps. On-screen instructions say start prone, extend arms, maintain a neutral spine, climb feet up and hands toward wall, arms extended, continue until thighs/chest touch wall, then return prone. The frames at 21–39 show the approach to the wall; 41–45 show the reverse. | The video’s end position explicitly includes thighs/chest touching, which is deeper than the app’s toes-only endpoint. Instructional overlays obscure fine hand/foot details. |
| [Andrew Alinda, “Wall Walk Tutorial | Handstand Drill”](https://www.youtube.com/watch?v=9TKfShDYax4) | 18–30 s, 25 fps. Frames show feet climbing while the hands advance, a near-vertical wall-supported position, then the hands moving back out to a floor-supported plank. | The selected excerpt begins partway through a cycle; the sequence includes wall-assisted inversion and is not a strict toes-only endpoint demonstration. |
| [GMB Fitness (Praxis), “Handstand Variations to Challenge & Deepen Your Practice (Part 5 of 8)”](https://www.youtube.com/watch?v=Ji99FIQWwA0) | 60–90 s coarse sheet includes a wall-walk/inversion drill; 156–184 s is a separate split-leg kick-up progression (see below). | Segment framing and lesson structure make this less direct evidence for a single standardized wall-walk rep. Do not treat different drills in this compilation as one cycle. |

CrossFit’s demonstration and narration are the clearest source for the
toes-only mechanics, but its coached terminal contact differs from the catalog
endpoint. The other examples support the hand/foot progression and return but
do not settle exact contact or acceptable trunk shape.

## Kick-up and bail

The catalog name is “Kick-Up to Handstand with Roll-Out Bail” and instructions
specify practising a forward roll from a forward-tipping handstand (or stepping
out to the side), then a lunge kick-up with the rear leg leading and front leg
following, a brief vertical hold, and return by the entry path. The review found
one directly observed split-leg kick-up and two useful but non-equivalent bail
demonstrations. This is not enough to validate the complete composite variant.

| Source | Reviewed interval and visible evidence | Limits |
|---|---|---|
| [GMB Fitness (Praxis), “Handstand Variations to Challenge & Deepen Your Practice (Part 5 of 8)”](https://www.youtube.com/watch?v=Ji99FIQWwA0) | 156–184 s, 29.97 fps. On-screen label at 160 s identifies “Practice Split Leg Kick-Up.” The athlete starts with hands grounded and legs split, passes through a split-leg inverted position around 166–182 s, then one leg begins to lower by 184 s. | This is the strongest direct kick-up sample, but the excerpt does not show a complete clear return to the initial lunge. It shows no forward-roll or side-step bail. Perspective is mostly side-on and feet approach the frame edge. |
| [Howcast, “How to Do a Handstand Forward Roll | Gymnastics”](https://www.youtube.com/watch?v=QDDx8CL2NKg) | 50–54 s, 30 fps. Demonstration frames show an inverted handstand on a raised mat followed by the performer on the back/shoulders on the mat, consistent with a forward roll out. 13–18 s separately shows a standing forward roll. | The handstand-roll segment is brief, transitions are fast, and the mat/support setup differs from the catalog’s general “on a mat” wording. Frames do not resolve hand placement or head/neck loading precisely. |
| [Andrew Alinda, “How to Bail Out of a Chest To Wall Handstand”](https://www.youtube.com/watch?v=fSdS5T2kgps) | 151–190 s, 25 fps. From a chest-to-wall handstand the athlete opens into a split, rotates out laterally, and places feet to the side; at 187–190 s the body is descending in a side/cartwheel-like exit. | This is a side/cartwheel-style bail, not a forward roll. It is against a wall and appears in a practice diary; it should inform only the catalog’s alternative side-exit branch, not be conflated with rolling. |

The available footage supports separating the kick-up entry, controlled return,
forward-roll bail, and side-step/cartwheel bail into distinct branches. The
generated pilot currently demonstrates an asymmetric kick-up and brief
vertical hold, then returns by its entry path; it does not encode either named
bail. Consequently it is an engineering pilot only and promotion as the full
catalog variant remains blocked. These clips are illustrative references, not
form approval or calibrated movement measurements.

## Local source files and hashes

| YouTube ID | Creator / title | Duration / encode fps | SHA-256 |
|---|---|---:|---|
| `NK_OcHEm8yM` | CrossFit / The Wall Walk | 65.98 s / 23.976 | `96b16a7966613d9650a705a4a8542bd5d1d6bc5b9b1eb4cc7fc023deb1f06025` |
| `9TKfShDYax4` | Andrew Alinda / Wall Walk Tutorial | Handstand Drill | 173.20 s / 25 | `e7c48a0d520302d22ed188a2a0bb10a19bd71c374eb9dc6df3fcf295ebcea67f` |
| `Ji99FIQWwA0` | GMB Fitness (Praxis) / Handstand Variations… (Part 5 of 8) | 241.37 s / 29.97 | `f7126e2997fa4898fa2381fde0c008ce4b3a90776d4a3b794dc53c2a1f58af8b` |
| `fSdS5T2kgps` | Andrew Alinda / How to Bail Out of a Chest To Wall Handstand | 226.36 s / 25 | `62af831ea42f999a7c69de1135677f2c5a6a3e8aab637c6cde06f996f30920f0` |
| `QDDx8CL2NKg` | Howcast / How to Do a Handstand Forward Roll | Gymnastics | 101.57 s / 30 | `04f6addaacbed4822a3eb5ab1f0f0713aaf9aeb508d0782707c39d094185c107` |

An additional search result, LittleTFitness’ [“Stop complicating your Handstand
kick-up”](https://www.youtube.com/watch?v=kEYsmHpxzL4), could not be downloaded
because the network name lookup failed. It was not frame-reviewed and is not
counted as visual evidence. The Prehab Guys’ wall-walk page was also found, but
its embedded media was not accessible through the available downloader.

## Engineering gate recommendation

`tools/blender/generation_checks/next_handstand.py` defines focused, numeric
bounds for data shape, world-joint speed, wall toe anchor, hand height/travel,
kick-up hand stability, asymmetric leg lead, and vertical hold. Proposed
contract values are in the module metrics: max joint speed 2.5 m/s; wall toe
anchor error at most 6 mm; wall-hand height within 1 mm of the authored floor
height; travel 0.20–1.80 m; kick-up hand drift at most 3 mm; leg stagger at
least 0.30 m; and vertical hold at least 0.70 s. These are engineering screens
for this authored pose set, not sourced human-form thresholds. The kick-up
audit also reports `catalog_bail_branch_present=0` as a promotion blocker.


Original reference clips and sampled sheets are retained only in local work analysis; they are not bundled or redistributed with the app.
