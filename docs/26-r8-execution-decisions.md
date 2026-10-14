# R8 execution decisions · 2026-10-14

Owner decisions in the Codex chat supersede the open questions in docs 23–25.

- Prioritize reviewed poses and explicit render batches for the owner's Blender run tomorrow morning. Two Luna pose agents work in parallel, one group each; coordinator reviews each pair before the next starts. A third Luna agent starts the app work in parallel.
- Comparable exercises may rotate between workouts. Each keeps independent progress and targets. The main exercise's mastery advances the main path; mastering every alternative is not required. Alternatives remain available after advancement. Comparable groups are explicit, not inferred solely from difficulty rank.
- The 30 plyometric exercises require squat-air and the six capped explosive push-ups require pushup-standard, as specified in handoff T09. These are exercise-achievement gates only.
- Unlock prerequisites use achievements of exercises present in the app only. No box-height measurement or additional self-check gate. Depth jumps require box-jump and squat-pistol achievements; rebound box jumps use the same extra prerequisites. Ring muscle-up negatives and the full movement require chest-to-bar pull-ups and ring dips. These are draft app rules, not source-backed thresholds.
- Hard prescribed rep ceiling of 3 only for pushup-pop-up-incline, pushup-kneeling-push-off, pushup-pop-up, pushup-pop-up-wide-close, pushup-clap and pushup-chest-strike. Individual power jumps default to 1–3 reps without a blanket hard ceiling on other drills. Consecutive hops/bounds and conditioning retain their exercise-specific prescriptions.
- Reconcile depth-jump props to the instructions' 30 cm starting box. Its former 40–50 cm prop text and measurement-based unlock caution were corrected in docs/23-new-exercises.json. Formal alsoRequires entries were added for the app-only gates above.
- Pilates core flow may run about 12 minutes despite its 10-minute title. Other duration estimates are informational; no forced timing change.
- Keep SSD's draft 10 reps, Russian and L-sit dips, shoulder rolls, cautioned kipping pull-ups, Pilates mappings (Hundred as hollow with arm-pump option), and non-flow Army 4C.
- Add proper power/conditioning movement patterns and neck/tibialis muscle labels. Rename the Shoulders tab to Handstand and the German-hang family display to Back lever. Stable IDs remain unchanged.
- Step and box remain separate equipment IDs. Keep the eight contracted programs; no extra Army CD/CL routines in this release.
- Training must include only exercises available with equipment selected in the active profile. The owner need not provide an equipment inventory in chat.
- Check human-flag descriptions against the owner's CC2 book photos before accepting the flag pose group. The supplied "Full text of Convict Conditioning 2" PDF is a text export without those photos; written text was inspected, photo comparison remains open.
- Contract structural fixes: shoulder-roll gains UPPER_BODY/CORE areas and ABS/UPPER_BACK primary muscles; pilates-spine-stretch-forward gains CORE. Plyometric/conditioning families gain their proper movement labels while retaining anatomical patterns; neck exercises use NECK rather than the UPPER_BACK stand-in.

All training content remains DRAFT. No phone testing or owner gate approval is claimed. New clip assets cannot pass completeness checks until the owner renders them and they are encoded and installed.

Written CC2 checks exposed contract errors in five flag entries. Clutch hang now uses the armpit wrap and opposite elbow braced into the waist, not an overhead press grip. Diagonal and horizontal split clutch use one tucked leg and one extended leg. Split Press uses a roughly 90-degree pole-side knee and one extended leg. Support Press uses an overhead bar plus a wall/fixed support, so its equipment and diagram props were corrected. Photos remain unavailable and no flag pose group is yet accepted.
