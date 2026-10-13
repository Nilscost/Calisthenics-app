# 20 — Handoff: state on 2026-10-13 and next steps

Repo: `/Users/nils/Documents/hermes/Apps/calisthenics` (github.com/Nilscost/Calisthenics-app, branch `main`). Owner: Nils.

Two tracks ran in parallel today. A Claude Opus coordinator reviewed and committed; a Sonnet executor did the R7 batch, and Haiku agents wrote the Blender poses. **Read this file, then the docs it names. Don't redo finished work.**

House rules (unchanged):
- Never claim phone testing. Never approve owner gates. All training content stays DRAFT.
- The Mac has no Java or Android SDK. Builds and verifies run in the Hermes Docker container (`docker ps`, currently `hermes-f5da05b7`; `source /root/toolchain/env.sh`; build tree `/workspace/review`). See doc 14 §2 and doc 19 §2.
- Push command with the proxy unset and the token file: see `docs/19-r7-executor-handoff.md` §2. Never print the token.
- Stage only your own paths (`git add <file>`, `git commit -- <file>`), never `git add -A`. Both tracks share one working tree.

---

## Track 1: R7 app batch (`docs/18-r7-plan.md`, `docs/19-r7-executor-handoff.md`)

### Done (committed and pushed, `origin/main` = `1f9dc5b` at time of writing)
| Task | Commit | Verify log |
|---|---|---|
| T01 hold targets, logger defaults (B2, B3) | f2206bb | r7-t01 |
| T02 DONE in breaks, captions, headers (B4–B6) | 58db2a9 | r7-t02 |
| T03 rep logging on the exercise screen (D3) | 6a2efa5 | r7-t03 |
| T04 questionnaire at/above top level (B1) | 08df05c | r7-t04 |
| T05 Too easy → next exercise (B9) | ed7c6d2 | r7-t05 |
| T06 Progressions chart, corrections (B7, B8) | 00603aa | r7-t06 |
| T07 clip cache (B10) | fe09913 | r7-t07 |
| T08 exercises per round (D1) | 9330ba3 | r7-t08 |
| T09 skill sessions (D2, B11) | e8b151c | r7-t09 |
| T10 Preview editor (D4, D11, D12, O1) | 83d1416 | r7-t10 |
| T11 Progress edit mode, tabs (D5–D7, O2, O3) | e1453c3 | r7-t11 |
| T12 stretches between sets, Jefferson curl, German hang (D8, D9) | 081772a | r7-t12 |
| T13 cool-down switch (D10) | 56c9d03 | r7-t13 |
| T14 release 0.10.0-r7, versionCode 10, STATUS/README, `docs/evidence/owner-check-r7.md` | 71b053e, 7b234b0 | r7-t14 (fresh clone) |
| Maestro flow fixes after T14 | 95299b5, 1f9dc5b | — |

All verify logs are under `docs/evidence/r7-tNN-verify-2026-10-13.log`. Progress notes have `### R7-T01` … `### R7-T13` in `docs/evidence/progress-notes.md`.

APK: `/Users/nils/Documents/hermes/Apps/builds/app-debug-R7.apk` (built 13:44, **before** the two flow-fix commits; those only touch Maestro flows, so the APK is still current unless app code changed after it).

### Open on Track 1 (check each, then do it)
1. **Is the executor still running?** It was fixing Maestro flows after T14. Check `git log` and `git status` for uncommitted work before touching anything.
2. **ui-screens must end all PASS.** Check the latest `ui-screens` run on GitHub (`gh run list --workflow ui-screens.yml`). Fix failing flows until it's all PASS.
3. **Screenshot reviews are missing.** There is no `docs/evidence/ui/R7-TNN-review.md` and no `docs/evidence/ui/R7-review.md`. Doc 19 §3 step 6 and T14 require them: fetch the shots from the `ui-shots` branch, open every changed screen (light, dark, font13) plus the light shot of each other flow, score 1–5, and fix anything below 4. At minimum write `R7-review.md` covering every flow.
4. **Progress note `### R7-T14` is missing.** Add it: what changed, tests, NOT done, choices not in the plan.
5. **Final owner report** (doc 19 T14): a table of what changed, what's verified, what's NOT done, and choices not in the plan. Known NOT done: Jefferson curl and German hang use placeholder clips until the clip track delivers them (T12).
6. If app code changed after 13:44, rebuild the APK and copy it to `builds/app-debug-R7.apk`.

---

## Track 2: Blender clips (`docs/16-video-clips-plan.md`, `tools/blender/POSES.md`, `tools/blender/HAIKU-TASK.md`)

### Done
Poses for all 8 groups in `work/clips-bakeoff/pose-groups.json`, one module each in `tools/blender/families/`. Every id prints OK in `poses.py --describe`, and the coordinator re-checked each group (angles, floor contacts, chest facing) and sent fix rounds:

| Group | Ids | Commit |
|---|---|---|
| squat_lowerlegs | 15 | b894236 |
| pull_row | 21 | 7e21900 |
| push_shoulders | 20 | 8a214d6 |
| hips_back | 24 | 6eedc09 |
| core | 23 | 033d124 |
| stretches_a | 13 | 7738567 |
| stretches_b | 12 | 28536f4 |
| warmup_weights | 20 | d02f385 |

`python3 tools/blender/poses.py` (no args) registers 152 ids, all OK. That is the 148 from the groups plus 4 extras that already existed.

Known limits (accepted; look at these renders first):
- `stretch-sleeper`: the pelvis floats about 8 cm above the floor, because the model's shoulder offset puts the shoulder through the floor any lower.
- `weighted-pushup`, `weighted-pullup`, `weighted-dip`, `weighted-squat`: no vest prop, so a weight sits on the chest.
- `stretch-thoracic`: the whole trunk rolls, not only the upper back.
- `stretch-wrist-flexor`: palms down, fingers toward the knees (the catalog says "palms up"), so it differs from the extensor stretch.
- `scapular-pull`: the body rises only about 1 cm; the elbows bend slightly.
- `planche-adv-tuck`: a static hold with no breath (a breath tripped the reach check).

### In progress (owner's Mac, started about 15:00)
Nils is running the overnight render in his own terminal:
```
cd /Users/nils/Documents/hermes/Apps/calisthenics && /Applications/Blender.app/Contents/MacOS/Blender -b -P tools/blender/render.py -- --resume
```
- About 91 of 152 to render, about 2 min each. `--resume` skips ids whose frames are newer than their `joints.json`.
- The `n/152` counter counts skipped ids silently, so it jumps.
- Output: `work/clips-bakeoff/<id>/blender/frames/*.png` plus `start.png`.
- **Don't run `poses.py` without ids while the render runs or before it ends.** It rewrites every `joints.json`, which makes `--resume` re-render everything. Use `poses.py <id> ...` for the ids you change, and only those re-render.

### Next steps (after the render prints `[bakeoff] done …`)
1. Read the summary line. Re-run the same command for any FAILED ids.
2. **Visual check (the first real look; so far only text checks were done):** in the sandbox, run `python3 tools/blender/preview_sheet.py`, or open `start.png` per id. Check the six known limits above first, then every id for wrong facing, limbs through props, and missing props. Write `docs/evidence/clips-bakeoff/blender-review.md` with one line per id: OK / fix.
3. Fix poses the same way: edit the group module, run `poses.py <id> --describe` until OK, commit `Blender clips: <group> fixes`, and ask Nils to re-run the `--resume` command.
4. Encode and install (sandbox): `python3 tools/blender/encode.py` (check the loop seam and size), then `python3 tools/blender/install_clips.py --dry`, then without `--dry`. This writes `app/src/main/assets/demos/<id>.mp4` (cap 120 KB, `ClipAssetsTest`) and `assets/thumbs/<id>.webp`.
5. Replace the T12 placeholder clips: Jefferson curl and German hang have no poses yet. Add them to a family module (`hips_back` and `pull_row` fit), render, install, and remove their `SHARED_POSES` placeholder entries.
6. Full verify in the sandbox (command in doc 19 §2), then commit and push the assets. Bump the version only if a release is planned, and leave that decision to Nils.

### How the pose work was run (to repeat it)
- One Haiku subagent per group, at most 2 at a time.
- The prompt was `tools/blender/HAIKU-TASK.md` plus the group's ids, plus "you run on the Mac: skip `source /root/toolchain/env.sh`". It also listed lessons: face up is `p ≈ -90` and face down is `p ≈ +90`; contacts appear under "on floor" in every moment; planted feet keep one world target; "straight" means ≥ 165.
- Never trust a subagent's pasted `--describe` output (one had a typo). Re-run it yourself, and check chest facing from `joints.json` (forward = `chest_left × chest_up`).

---

## Continuation by Codex with Luna subagents (2026-10-13)

Do not repeat the completed R7 implementation or rewrite all joint files. The original owner render is still running during this continuation.

Committed continuation code:
- `a372a85`: portable clip tooling paths fix the verify CI preflight failure. Full offline Android verification exit 0 (`docs/evidence/r7-resume-verify-2026-10-13.log`), starter content structural/rights validation passed, GitHub verify run `38404466903` passed.
- `5f6a3b2`: Jefferson curl and foot-supported German hang poses; six wall/door camera fixes; corrected wall HSPU body line; stick-sheet bar x-offset matches the renderer. Coordinator independently regenerated only the eight changed/new IDs and checked geometry/previews.
- `ab77d74`: Maestro centers the cool-down controls before tapping and targets the Dip Support Hold by name instead of the first 30 s hold.
- `89ade24`: installer accepts new catalog IDs without needing pre-existing demo files, rejects stale/noncontiguous frames, stages files before replacement, and keeps shipped files when encoding fails or exceeds the cap. Seven focused installer tests passed independently in Hermes.

Evidence and owner delivery:
- `docs/evidence/ui/R7-review.md`: screenshot review and precise CI status; use its latest status rather than assuming every screen passed. Earlier `1f9dc5b` shots were partial and b/j failed.
- `docs/evidence/progress-notes.md`: previously missing R7-T14 entry is now added.
- `docs/evidence/r7-owner-report.md`: changes, verification, remaining implementation limits and owner gates.
- `docs/evidence/owner-check-r7.md`: corrected to state that level/swap/removal affect all sets, while number/stretch are per set; deep stretch duration is max(rest window, default), per side.

Blender snapshot review (`docs/evidence/clips-bakeoff/blender-review.md`): 154 joint directories now exist (152 original plus the two new IDs). At the review snapshot 100 outputs had no major visual defect, six needed rerendering, and 48 were pending. This is a snapshot, not the final render count. Some previously stale outputs have since been rerendered by the owner and still need visual inspection.

After the owner's process ends, the eight changed/new IDs need another render and rendered review:
`calf-raise-single-loaded german-hang hspu-wall hspu-wall-negative jefferson-curl pushup-wall towel-door-row wall-handstand-hold`.
The owner's running process enumerated its IDs before the two new IDs existed, so it cannot render those on this pass. A new `--resume` run will pick them up and rerender the six stale camera/body corrections.

**Check the timestamp of `work/clips-bakeoff/render-summary.txt`.** During this continuation it still contains the old 13:18 run summary (84 rendered); that is not the completion of the currently running owner's batch. No new shipped assets, aliases or version changes were made. Placeholder removal, remaining rendered review, complete encode/seam/size validation, install, full verification and updated APK remain pending on the clip track.
