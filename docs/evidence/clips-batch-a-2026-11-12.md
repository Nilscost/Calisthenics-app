# Batch A clips: evidence (2026-11-12, lane L6)

## Installed media before this lane (sha256, app/src/main/assets)

| id | role | demo mp4 | thumb webp |
| --- | --- | --- | --- |
| plank-long-lever | A1 | 4b891eed7d21c95764ba4555a1abce23beff1f9c7c00cc8a3bc6ba1f861faec7 | 4f473f88eac53da9d29c12a055f8178228811641ff0d68e60baaa5852985dd90 |
| bridge-single-leg | A2 | 776a095c1c99a7c12bb9f801fbad2430f3c709ebe80c180326c18129c0508756 | f3299a4ac0a37d505dfa9a06bbb8410e82d966c033e68d1a7ce134ab9e1a12df |
| walking-lunge | A3 | 2c01b0e8b5091c2713a6bc53847affae0cd06ebc80fcde264ab69dbaf9dbb1eb | 28e601e5c12023062cff84d93d2f94e4e789c6ab1fddf375a7e400391c81e602 |
| russian-twist | A4 | 12c93d572bbc03a9134865df42a6bf25e04e7e9bc2f74f2ce7e4e52692d1b597 | 39d55f1a8805c227bde45e0e197364e3e28031940bd51d953d7a27399d3ee71c |
| side-plank | A5 | eb6a24a659be2fa442eba9533008aab9c1fa899b23296e0a3e3b8ef3545bcf2e | 20a9d5044d97ab3ba17e86c9c45a4b1ae52ba4ee4fd4cfd4ed8c36ceda786ae0 |
| dip-parallel | A6 | 7f4e2e160245de6e8fe69bf215bec586248bb8d5adbb227d02f6689a2b1588b9 | 133d9ba4e49b1972b469a02b14f6fcf9239e8fc37075fdec544e288bdb24cb94 |
| inverted-row | A7 | d79eb1751862e634c7c2ffa8777a963ce8b31a36ef3c98ac66822dff0e433a42 | cab8150d4259c274e9474e6d39bc64731e33710266838d3b08168559e0316d82 |
| glute-bridge | A8 | a6ff9be96d93db5b61b97c25d52121b6a12d7e93600bdd556f07bb3d69793146 | 3e8e880b1bf1a5e614c607d5ace8e95f93689eba7f926f2ce7d2178fd5f68559 |
| split-squat | A9 | e2249e65c848baa45e97996bc59150e5b44458241b9dc14ddac3c842d690096a | d18c45dfe73fabf55ba6476771d8e78ea22612b88ffc3950ebf5547d8e21092c |
| plank-shoulder-tap | A10 | ca9d43c6693a93effd8262b50803afaa04e01e5b7bc4d2cf70d3934f9ab7f969 | 3e75d1400e1c4ae9ce28fc0e09adbc4cc7fe0af6d7cb19b97e4fe6cfef6bc9cf |
| pushup-archer | control | 9183227a1f7f727510f6cd7ff64122e2a917cea90d4330a6a7b26c3bca0530d4 | 8cf06297e4dac806547d9c0de7d5e9624ec6dacdf03053bf4b2aa5065428d985 |
| pullup-full | control | 1ce1557f1ec537e7eb6d5155b708749e01ad215c1181062454407b709d8ce741 | aed2423983ee35c32330426c5c5fe39c6f44eb6e082824cd9a60200be2fb0fcb |

## Catalog ids

plank-long-lever, bridge-single-leg, walking-lunge, russian-twist, side-plank, dip-parallel, inverted-row, glute-bridge, split-squat, plank-shoulder-tap. Controls: pushup-archer, pullup-full (source and media untouched).

## Phase 1 status

Rebuilt from references, class checks and native geometry preflight pass: plank-long-lever, bridge-single-leg, glute-bridge. Not yet rebuilt: walking-lunge, russian-twist, side-plank, dip-parallel, inverted-row, split-squat, plank-shoulder-tap. Reference intervals for all ten are in motion_references.json (batch_references). Note: glute-bridge ref2 interval crosses a camera cut.

## Phase 2 status

All ten rebuilt (families/batch_a.py, batch_a_more.py), class checks and native geometry PASS for all ten (work/render-batches/batch-a-2026-11-12/_preflight-phase2/geometry.json). Known reference problems: glute-bridge ref2 and dip-parallel ref2 intervals cross camera cuts; split-squat ref2 interval is a different pace/segment. walking-lunge is an in-place alternating forward lunge (step out, lower, return), not a travelling walk.

## Phase 3: render and install (2026-11-13)

Rendered with render_batch.py (prepare, run) in work/render-batches/batch-a-2026-11-12/final: 8 ids, geometry preflight pass, all complete. Every encode decodes cleanly and is 480x360, 24 fps, H.264 Main, yuv420p, well under 120 KiB. Installed with install_clips.py (explicit ids): plank-long-lever, bridge-single-leg, glute-bridge, plank-shoulder-tap, walking-lunge, split-squat, dip-parallel, inverted-row. Sent back and not rendered: side-plank, russian-twist.

Asset check: sha256 of every file under app/src/main/assets before and after. Exactly the 16 files below differ (8 demos, 8 thumbs); all other files, including the pushup-archer and pullup-full clips and thumbnails, are byte-identical.

| id | new demo sha256 | new thumb sha256 |
| --- | --- | --- |
| plank-long-lever | b32d24a48797c025ab03998b1edd4623749753390a2b7b7c80bcb17840ad4940 | 10a3a2b1c0a69ea040fdf12a68bd1f43e8084aef180345ca1eb62891f9e58cac |
| bridge-single-leg | 4099fb79b41ca6ed71b49d2b4c6417adafd7d9d62fdd07b00bfb4ed7e764ee84 | d3802065c6a1c43c73c085dd7697a374cdc6d11fcd1c2cd90158f03656939ba0 |
| glute-bridge | 49a594e804fa3dc6966ea0422d3fee43ea5fb4abd7842ce5c682ad2ad0070bde | 4de52b3b6f5743d8cfffadba9030c65307860cd9d6913dba19aaa156700d9110 |
| plank-shoulder-tap | 4a9fddb30debf0d5065385200965db5a11c874f887e8523e43bfb69eb8b76121 | ab85b96125e0eb83787aca3384bb93a510ec1a201248b395434c732442a0f370 |
| walking-lunge | 3ccb22d40c0eae30c4400a44bd6d0758fb841d412fbfad429e39ae4ccdaebc55 | b94b7728d6d29c5d5332f33380d6b41bd686319f5587d593ba47a280d64bc4f2 |
| split-squat | 27c0c472c3fca455cc6f3eeeb8bb9a953033f7d6fbe4c48381ed765d3e94de11 | bc834465b605a213e5edcbefb0289524f5a541b902d7d08007626e33d3770770 |
| dip-parallel | 7644f61400332e9badd1a6a882e783149c56181a41b7fd26d083438e8dec5884 | 76bf393a66cb303a3b5a337ee6e007b734a7810ef88da84599833b3ba6f45751 |
| inverted-row | 4dfd2565e19fa547ce53e465e4324705ba7d406089a2433f4856cab50027a06c | 4730076bf1c40ad9212f26a835d4f4020b5f252f5835471574c085ba0e403cc8 |

Controls unchanged (before equals after); their full hashes are in the first table.

Checks: tools/validate_content.py content/starter OK; ClipAssetsTest 3 tests, 0 failures.
