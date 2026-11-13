# Clip fixes and installation (2026-10-26)

## Scope

Eleven demonstrations were fixed and installed in one batch:
- the seven Oct-25 review candidates of doc 40;
- the five installed clips that the 2026-10-25 stills review rated CONCERNS.

Burpee was already installed in PR #1. Russian dip is unchanged, because its candidate was practically identical to the installed clip.

## Changes by clip

| Clip | Source change | Camera |
|---|---|---|
| flag-kick-press | None. The doc-40 candidate source is installed as rendered. | default |
| toes-to-bar | Legs stay together: constant ankle lateral offset in `strict_toes.py`. The cycle is 4.6 s. Knees stay ≥170°, with bilateral toe/bar contact. | default |
| heel-hook | Rebuilt hook-up: hips forward, trunk back, feet clear the bar before dropping onto it, then crossed ankles. Skin gap is about 0–2 mm. | (25, −6) |
| dragon-flag-negative | The knee pole is now the belly direction, so the recovery is a tuck instead of the splayed "diamond". | default |
| hanging-windshield-wiper | Top elevation is 65° and the sweep ±50°. Everything stays inside the existing contract bounds. | (82, −8) |
| handstand-press-straddle | Straddle half-spread goes from 0.30 to 0.45 m. `check_straddle_feet` passes with 15.3 mm minimum clearance. | (75, 16) |
| handstand-wall-walk-up | Rewritten as a 9 s cycle: plank, feet climb, 3 alternating planted steps per hand, chest-to-wall handstand, then the walk down. Native geometry passes, with minimum foot height +6 mm (the 10-25 prototype had a 22.82 mm intersection). | default |
| pushup-archer | Reframed. Planted feet are raised 3.1 cm, because the toe tips sank about 3 cm below the floor. | (50, 20) |
| pilates-saw | Arms stay level through the twist, the front hand lifts before travelling (no slide), and the cycle is 5.6 s instead of 8.5 s. | (90, 26) |
| squat-pistol | Free leg is held off the floor in every phase (0.46 m at the top, 0.28 m at the bottom). The support knee reaches about 179° at the top. | default |
| ring-muscle-up | Camera only. The straps now hang beside the torso instead of crossing it in support. The ring/hand geometry and gates are untouched. | (12, 8) |

## Gate change

The wall-walk gate `left_wrist_floor_height_error_m` required the left hand to stay down the whole time, which rules out stepping. It is replaced by four checks:
- planted-wrist floor contact;
- planted-wrist slide of 1 mm/frame or less;
- zero frames with both hands lifted;
- at least 6 steps per hand.

No other tolerance or skin radius was changed.

`tests/test_install_clips.py`: the sample exercise `pushup-archer` now has a reviewed camera view, so the helper writes the camera stamp a real render writes.

## Verification

**Render**
- One isolated batch from the merged source: `work/render-batches/2026-10-26-clip-fixes` (local, not committed).
- Native geometry preflight passed, and all 11 clips are complete.

**Media and install**
- Every encode is 480×360, 24 fps, H.264 Main, 18–40 KB.
- `install_clips.py` installed 11 with 0 problems: stamps, camera, motion rules and size cap all passed.
- Exactly 11 demos and 11 thumbnails changed.

**Tests**
- Python pipeline: 243 tests. The only errors come from local `work/` baselines and the catalog-screen environment, and they also error on the unchanged base. The baseline-dependent conditioning and floor tests pass when the baseline is present, so the other trajectories are preserved.
- App: `:domain:test` 594, `:app:testDebugUnitTest` 265, 0 failures; `assembleDebug` builds.

**Review**
- Each clip was reviewed from contact sheets and full-size key frames: the lanes reviewed theirs, and the coordinator cross-checked every clip.
- No real-speed owner playback or phone check is claimed.

**Still open**
- Owner form acceptance at real speed for all eleven.
- Ring muscle-up: the camera follows the body, so the rise reads mainly through the shadow (shared renderer behaviour).
- Heel hook: the head sits near bar level.
- Wall walk: the feet glide up the wall rather than step.

## Installed hashes

| Clip | Demo SHA-256 | Size |
|---|---|---|
| flag-kick-press | `3844525b541d4335…` | 21854 B |
| toes-to-bar | `ab408cbec9efc23f…` | 18205 B |
| heel-hook | `eaa9083243b94248…` | 18561 B |
| dragon-flag-negative | `375229cf4eb3736b…` | 23164 B |
| hanging-windshield-wiper | `aae604596a98d85d…` | 23535 B |
| handstand-press-straddle | `90f3fced8d2ce2a8…` | 27974 B |
| handstand-wall-walk-up | `2909f01903ae5f06…` | 35879 B |
| pushup-archer | `9183227a1f7f7275…` | 39195 B |
| pilates-saw | `38eedc76bcbc4693…` | 35983 B |
| squat-pistol | `ceec33dbcdefc69c…` | 24358 B |
| ring-muscle-up | `04fa7b6b5a81e71c…` | 40657 B |
