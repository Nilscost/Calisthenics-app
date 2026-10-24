# Scripted exercise-video generation and review

Updated 2026-10-24. This procedure covers Blender review candidates and deliberate
app-media replacement. Script scores do not certify exercise form.

## 1. Establish the movement and preserve the baseline

Record the exact exercise/variant, intended phases, equipment, contact points,
full return and the owner's observation. Read README, STATUS and repository
instructions. Preserve dirty and untracked work; do not reset or clean it.
Freeze the current trajectory and hashes of all generated trajectories and
installed demos/thumbnails. Existing accepted videos are regression controls.

Use bounded file ownership when delegating: the orchestrator owns shared pose,
render, persistence/resource contracts and verification; executors own named
family functions, isolated validators/tests or reference notes. Agree APIs first.
Require changed-file, executed-test and unresolved-issue reports.

## 2. Inspect three to five useful filmed references

Prefer original coaching demonstrations. Cache extraction and decoded frames so
subsequent passes reuse them. Record creator, URL, source hash, exact interval,
variant, view, editing, assistance, occlusion and sampling limits. Inspect actual
frames/video, not titles or URLs alone. A close grip explanation, assisted
transition and continuous negative offer different evidence; do not count them
as three complete unassisted repetitions.

Use pose comparisons only for compatible views/variants with sufficient
visibility. Missing fingers or a low body-pose error cannot prove palm direction,
contact, force or anatomy. Report inconclusive comparisons explicitly. Keep
third-party source footage in local analysis, outside app media. No paid video
generation is required for this workflow.

## 3. Implement observable physical contracts

Define exercise identity and phases before tuning: lateral travel for an archer
push-up, grip direction for pull-up/chin-up, gravity-shaped airborne motion for
jumps, etc. Preserve the movement's full range and return, rather than fitting a
single attractive still.

For loaded hands check the entire forearm–wrist–palm–MCP–finger chain:

- independent body, palm and apparatus frames;
- fixed phalanx lengths, connected joints and stable MCP attachment to the palm;
- plausible wrist flexion/deviation and continuous orientation through phases;
- actual palm/heel and each digit near the wood, with no penetration;
- constrained suspension and coherent hand/ring motion through pull, turnover,
  support and return.

A declared contact proxy can be mathematically exact while skin floats or cuts
through another hoop arc. Match the actual equipment axis and finite endpoints;
a bar along Y must not be measured as a bar along X. Tapered feet and fingers
need their actual skin radii rather than a single maximum-width capsule. Clearance and contact proximity are separate tests.
Do not shrink mesh radii, stretch fingers or quietly relax bounds to hide a bad
pose. Keep the featureless head/current appearance unless style is deliberately
being changed.

For walking/reset phases check facing relative to travel, planted-sole position
and heading, swing-foot lift, support transfer, solid apparatus clearance and the
loop seam. Correct jump physics alone does not catch a crab-walking return.

## 4. Use cheap scripts before rendering

Run complete-trajectory reach, phase, speed/acceleration, orientation, contact,
rigidity and seam checks. Store the actual old defective source as an independent
negative fixture. Add focused mutations for detached contact, frozen directions,
sliding supports, deformed/disconnected fingers and lost movement identity.

Keep engineering bounds and intentional changes in `motion_contracts.json`.
They describe the authored variant, not universal safe human ranges. A higher
jump must retain gravity and landing checks; a false-grip wrist needs phase-aware
checks rather than one misleading generic bend limit. Report worst frame/time.
Unknown exercises remain unscoped, never automatically approved.

## 5. Verify actual Blender geometry

Check all frames using actual renderer meshes: body/foot floor clearance, solid
apparatus interiors, hand/wood proximity and penetration, physical segment lengths,
and final encoded framing/inset clearance. Conservative whole-capsule bounds
cover segment thickness and gaps between samples; selected joint endpoints do not.
A skeleton pass is insufficient if the renderer keeps an obsolete capsule length.

Use enlarged grip views and selected diagnostic phases to investigate failures,
then inspect the encoded result. Native mesh tests still do not prove loading,
balance, grip pressure or anatomical correctness.

## 6. Freeze and render an isolated batch

Finish shared source/validator changes first. Prepare a small batch with:

```sh
python3 tools/blender/render_batch.py prepare --directory /absolute/review-batch --ids exercise-id
python3 tools/blender/render_batch.py run --directory /absolute/review-batch
python3 tools/blender/render_batch.py status --directory /absolute/review-batch
```

The runner writes staged outputs and a manifest, not app assets. It requires
matching source/data/render/validator stamps, native geometry preflight, complete
fresh frames and final encoding. Do not edit pipeline source during rendering.
A process ID or manifest state alone is not success: check frame/log progress.
If Blender fails, preserve the failure and use the established runner to restart
the failed clip; do not call partial frames a completed render. Preventing idle
system sleep for the render process may avoid interruptions; it does not diagnose
a crash or require preventing manual sleep/locking.

The media contract is silent 480×360, 24 fps H.264 Main/yuv420p, at most 120 KiB,
with the exact expected duration/frame count. Probe and fully decode every encode.

## 7. Review and promote deliberately

Build the eight-to-ten-item review gallery from hash-checked completed encodes.
Preserve the owner's selection and notes. Check full playback, replay, pause,
frame stepping, speed controls and narrow layout. State playback rate: accelerated
control testing is not a normal-speed visual form review. Never label screenshots,
mocks or desktop playback as actual phone verification.

Keep unresolved defects visible and block automatic app promotion. When the
owner explicitly requests replacement/push, include only eligible outputs and
retain DRAFT/form-review status until actual acceptance. Preserve the other
trajectories/media by hash, update evidence and STATUS, run content validation
and the app's established test/lint/build workflow, and package the new APK with
its version, hash, signing continuity and location. Push only the intended source,
media and procedure; do not stage analysis caches, downloaded reference videos,
secrets or unrelated work.

Report three separate outcomes: automated checks, actual visual/device review,
and unresolved form questions. More mannequin detail can improve hand, thumb,
foot and joint readability; it cannot repair an incorrect trajectory.

## 8. Continue in bounded batches

Rank the next eight-to-ten candidates from actual catalog/source screening.
Target a concrete observable concern, not a generic aesthetic score. Reuse
reference extraction and deterministic checks, supervise bounded executor lanes,
and keep accepted controls in each batch. Complete review, provenance and
verification before promoting further assets.
