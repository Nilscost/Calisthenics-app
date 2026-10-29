# KMP Phase 0 latest screenshot review

Run: [ui-screens 40812205504](https://github.com/Nilscost/Calisthenics-app/actions/runs/40812205504), commit `28861398487364259986afb459060519a326ddc3`. Dark and font13 jobs passed; light failed in flow b. This is a visual review, not approval of the phase gate.

The artifacts contained 125 light, 133 dark and 136 font13 PNGs, including diagnostic step and failure images. Contact sheets and per-variant manifests are in [kmp-phase0-latest-review](kmp-phase0-latest-review/). All 125 available light images were opened; all available b, c and o images were opened in each variant. The light sheets cover all flows; dark and font13 sheets preserved here cover b, c and o. Their manifests list the complete downloaded variants, including sheets not copied here. Contact-sheet inspection cannot establish touch target dimensions or behavior. Screenshots capture states, not animation or timing.

| Screen / flow | Variant | Score (1–5) | Observations |
|---|---|---:|---|
| Train and Preview (b) | light | 4 | Available screens readable. The run failed before the add sheet and later b screenshots; those states need replacement screenshots from a passing light run. Failure and diagnostic screenshots are included separately. |
| Train and Preview (b) | dark | 4 | Secondary explanatory text has relatively low contrast. |
| Train (b) | font13 | 3 | “Conditioning” wraps awkwardly with the final letter on a second line. Attribution to the toolchain upgrade is unverified. |
| Preview (b) | font13 | 4 | Available controls and sheets remain legible. |
| Session and end (c) | light | 4 | No obvious overlap in captured states. |
| Session and end (c) | dark | 4 | Video and header area is bright white against dark controls. End screenshot 08 was captured before cards appeared; 09 shows the cards, so 08 alone does not establish missing content. |
| Session and end (c) | font13 | 4 | No obvious overlap in captured states. |
| Progress routes (o) | light / dark | 4 | No obvious overlap in captured states. |
| Progress routes (o) | font13 | 3 | Some long node names break mid-word, reducing readability. Attribution to the toolchain upgrade is unverified. |
| Other flows (a, d–n) | light | 4 | All available light screenshots opened. Onboarding cards show partially visible neighboring horizontal cards; comparison with the original baseline confirms this is existing layout, not clipping within a fully visible card. |

No visual defects were fixed in this review. The font13 wrapping and dark contrast observations remain open for baseline comparison and appropriate follow-up. The failed light attempt does not satisfy the required all-pass screenshot gate; a passing replacement run and review of its previously missing b states remain pending. The original representative review and its evidence remain intact.
