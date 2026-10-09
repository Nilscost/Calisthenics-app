# V08 — body figure: screenshot review

Run: https://github.com/Nilscost/Calisthenics-app/actions/runs/37943497117 (commit `6ba74e9`), 15/15 flow x variant PASS. The figure shows in the Progress sheet (`e_seeded/07` kettlebell deadlift, `10` thoracic rotation).

| Screen | Variant | Score | Defects |
|---|---|---|---|
| Kettlebell deadlift sheet (`07`) | dark (opened) | 3 | gold primary (glutes, forearms) and dim secondary (hamstrings, lower back) read at 48 dp, but the figure is small and the silhouette very dark on the dark sheet; the chips beside it still use the old orange |
| Thoracic rotation sheet (`10`) | light (opened) | 3 | upper back and obliques are visible; light silhouette is faint; primary vs secondary differ little on white (the owner's tokens differ by saturation) |
| same sheets | font13 | not opened one by one | passes; the figure does not scale with the font |

Fixed in V08b: figure width 60 dp, colours from the theme tokens (gold / accentDim), chips follow the same tokens. Open: the secondary colour on white (token decision).
