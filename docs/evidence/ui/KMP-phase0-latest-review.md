# KMP Phase 0 latest screenshot review

Run: [ui-screens 40812205504](https://github.com/Nilscost/Calisthenics-app/actions/runs/40812205504), commit `28861398487364259986afb459060519a326ddc3`. Dark and font13 jobs passed; light failed in flow b. This is a visual review, not approval of the phase gate.

The artifacts contained 125 light, 133 dark and 136 font13 PNGs, including diagnostic step and failure images. Contact sheets and per-variant manifests are in [kmp-phase0-latest-review](kmp-phase0-latest-review/). All 125 available light images were opened; all available b, c and o images were opened in each variant. The light sheets cover all flows; dark and font13 sheets preserved here cover b, c and o. Their manifests list the complete downloaded variants, including sheets not copied here. Contact-sheet inspection cannot establish touch target dimensions or behavior. Screenshots capture states, not animation or timing.

| Screen / flow | Variant | Score (1–5) | Observations |
|---|---|---:|---|
| Train and Preview (b) | light | 4 | Available screens readable. The run failed before the add sheet and later b screenshots; those states need replacement screenshots from a passing light run. Failure and diagnostic screenshots are included separately. |
| Train and Preview (b) | dark | 4 | Secondary explanatory text has relatively low contrast. |
| Train (b) | font13 | 3 | “Conditioning” wraps awkwardly with the final letter on a second line in both the original and upgraded APK captures. |
| Preview (b) | font13 | 4 | Available controls and sheets remain legible. |
| Session and end (c) | light | 4 | No obvious overlap in captured states. |
| Session and end (c) | dark | 4 | Video and header area is bright white against dark controls. End screenshot 08 was captured before cards appeared; 09 shows the cards, so 08 alone does not establish missing content. |
| Session and end (c) | font13 | 4 | No obvious overlap in captured states. |
| Progress routes (o) | light / dark | 4 | No obvious overlap in captured states. |
| Progress routes (o) | font13 | 3 | Some long node names break mid-word, reducing readability. The original and upgraded captures show different scroll positions, so attribution for the affected node remains unverified. |
| Other flows (a, d–n) | light | 4 | All available light screenshots opened. Onboarding cards show partially visible neighboring horizontal cards; comparison with the original baseline confirms this is existing layout, not clipping within a fully visible card. |

No visual defects were fixed in this review. The Progress node wrapping and dark contrast observations remain open for appropriate follow-up. The failed light attempt does not satisfy the required all-pass screenshot gate; a passing replacement run and review of its previously missing b states remain pending. The original representative review and its evidence remain intact.

## Original APK comparison at font 1.3

The original owner-selected APK ran flows b and o on disposable `emulator-5556` at font scale 1.3; both passed. Original images are under `/tmp/kmp-phase0-baseline-font13/{b_train_preview,o_r8_progress_routes}/`; upgraded images are under `/tmp/kmp-phase0-run40812205504-font13/ui-shots-font13-28861398487364259986afb459060519a326ddc3/work/_temp/shots/` in the corresponding flow directories. [Paired images](kmp-phase0-baseline-comparison/font13/) show original on the left and upgraded on the right. These runs used different emulator instances and persisted states, so the scroll positions, duration and system bar differ.

In `01d-train-include-font13.png`, “Conditioning” breaks before its final “g” in both captures. In the Progress pairs, shared visible names including “Box Squat (sit and stand)” and “Uneven Push-Up (per” have the same line breaks. The upgraded `02-push-up-main-spine-font13.png` shows “Hands-Elevated One-Arm” breaking within “Elevated”; that node is outside the original capture's viewport, so this comparison does not establish whether its break existed in the original. The matching portions support existing wrapping but do not prove equivalence for every node.

## Focused light replacement

A corrected local b flow ran on `emulator-5556` against `app/build/outputs/apk/debug/app-debug.apk` with:

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17 PATH=/opt/homebrew/share/android-commandlinetools/platform-tools:$HOME/.maestro/bin:$PATH ANDROID_SERIAL=emulator-5556 FLOWS=b_train_preview VARIANTS=light bash scripts/ui_shots.sh app/build/outputs/apk/debug/app-debug.apk /tmp/kmp-phase0-b-fix-java > /tmp/kmp-phase0-b-fix-java.log 2>&1
```

The command exited 0 and the log reports `PASS b_train_preview light`. Its output contained 27 PNGs, including three diagnostic scroll screenshots. All were opened in the [focused light contact sheets](kmp-phase0-latest-review/focused-light/manifest.txt). The previously missing add sheet, rule, cool-down, timeline and swap states score **4/5**: no obvious overlap or clipped content was observed at sheet scale. This local run fills the visual gap from the failed CI attempt; it does not replace the required complete passing CI run. A full new CI run is pending.

## Final CI run

[ui-screens 40862677497](https://github.com/Nilscost/Calisthenics-app/actions/runs/40862677497), commit `fc6885d05fdfbf116704ef69391f08776e65bc28`, completed with 51 PASS results (17 flows in each of light, dark and font13), no FAIL or HOOKFAIL. The harness retried b and j once in each variant (six retries); all passed on retry. The connected job also passed. Results are preserved in [kmp-phase0-final](kmp-phase0-final/).

The final artifacts contained 133 light, 133 dark and 135 font13 PNGs, including diagnostic screenshots. All light images and all b, c and o images in dark and font13 were opened via the final contact sheets and manifests in that directory. Final scores remain: b Train font13 and o Progress font13 **3/5** for the previously recorded word wrapping; b Preview, c Session and o in the other variants **4/5**; other light flows **4/5**. Dark Preview's small secondary text remains relatively low contrast and the session video/header area remains white. No new blocking visual defect was observed. In light `d_tabs/03-history`, the screenshot caught `Loading...`; seeded history screenshots show populated history, but that particular capture does not show the loaded state.

The visual review supports passing the screenshot gate at the documented minimum score of 3, subject to coordinator verification of the results and other phase gates. Font13 wrapping remains open; its attribution to the upgrade is unverified. These screenshots do not establish phone behavior or touch target measurements.
