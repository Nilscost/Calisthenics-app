# R8 repair final acceptance, 2026-10-17

Decision: all 25 repaired demonstrations are accepted for installation. No phone testing was done; all training content stays DRAFT.

## Method
- Each clip was rendered (Cycles, 640x480, 24 samples) from the repaired rig and pose sources, then encoded (H.264 Main, 480x360, 24 fps).
- The Opus coordinator decoded every staged MP4 and reviewed contact sheets of all frames (`work/r8-video-repair-2026-10-16/decoded-review/page-01..07.jpg`).
- Install preflight (`/tmp/r8-promote-repairs.py`) checked, per clip: staged joints equal the source builder output, pose validation, frame count, encode input hash, encoded media hash, stream contract and a full ffmpeg decode. The installed asset hash equals the reviewed encode hash for all 25 (`repair-install-2026-10-16.json`).

## Accepted clips (25)
- `stretch-thoracic`
- `short-foot`
- `rower-situp`
- `scapular-pushup-wall`
- `scapular-pushup-knee`
- `scapular-pushup`
- `support-scapular-shrug`
- `wrist-palm-pulses`
- `chin-nod-supine`
- `chin-tuck-seated`
- `chin-tuck-head-lift`
- `neck-head-raise-manual`
- `turn-and-lunge`
- `stretch-neck-side`
- `stretch-twist-straight-leg`
- `stretch-twist-easy`
- `stretch-twist-half`
- `stretch-twist-three-quarter`
- `stretch-twist-full`
- `pilates-roll-up`
- `pilates-saw`
- `pilates-roll-over`
- `pilates-seal`
- `pilates-spine-twist`
- `pilates-double-leg-kick`

## Known and accepted limits (schematic mannequin)
- The three scapular push-up clips and `chin-nod-supine` show a real but subtle action.
- `short-foot`: the arch change is subtle.
- `neck-head-raise-manual`: shows hand contact rather than visible resistance.
- `turn-and-lunge`: the blank cells in contact-sheet page 04 are a sheet artifact; the figure is in every video frame.
