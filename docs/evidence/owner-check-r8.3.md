# R8.3 owner walkthrough — 2026-10-19

Release candidate: `0.11.3-r8`, versionCode 17. Implementation follows `docs/34-user-feedback-executor-handoff.md`. Content remains DRAFT. Owner acceptance and all physical-phone checks below remain open.

## One end-to-end phone check

Record phone model, Android version, local music player/version, and Bluetooth device before starting. Automated tests do not establish audio coexistence on a phone.

1. Start a downloaded song in a local music player, using speakers. In Settings, confirm Light is the new-install default, select a quiet app volume, and retain the desired voice toggle. Repeat later with Bluetooth headphones.
2. On Train select **Between sets → Flow**. Open Preview, set **Preparation time → 8 s**. Confirm no Flow switch remains in Preview, and the estimate follows preparation duration.
3. Use a push-up's app-level stepper to reach level 5, then continue to 40 reps. Save the routine. Leave/reopen Preview; restart the app, reload the routine, and verify 40 reps and 8 seconds persist.
4. Start the routine. Music must continue during start cues, countdowns, speech, preparation, work, pause/resume, and finish. The slider changes app cues only; zero silences both voice and beeps without changing system media volume.
5. Between every two Flow work sets, including repeated exercises and sides, confirm one preparation interval with the correct upcoming silent looping video/name/target/side. Confirm no duplicate preparation, no final preparation after the last work, and no reps or achievements credited for preparation. Try an exercise with unavailable media and check the thumbnail/instructions fallback.
6. Pause during preparation and resume. Separately recover after process interruption; confirm remaining time, app volume, and voice toggle survive. Never force-stop/clear app storage while checking recovery.
7. Log actual 40-rep sets. Confirm History keeps 40, and after three qualifying sessions within 14 days the result is a level-5 accomplishment, never level 6. Check a hold above level 5 similarly; skip or partially finish a set and verify it cannot earn credit. End-screen and History corrections must remain effective.
8. In Progress → exercise detail → Edit, save personal targets for all five levels. Confirm another exercise sharing the original catalog policy is unchanged. Cancel an unsaved edit and Reset to app defaults. Manual star adjustment remains independent. Raising targets must affect future Train/Preview/logging and prevent old lower thresholds or a pre-edit pending streak from earning new progress. Old History and earned achievements remain.
9. In Train switch between Rest, Stretch and Flow in Straight sets, Pairs and Circuit. Rest intervals and stretch selections return when relevant; compatible automatic stretches occur at most twice, then plain rest. Warm-up, optional conditioning finisher, and cool-down retain their own behavior.
10. Use an editable program, including a program saved as a routine. Check preparation and explicit app-level extensions survive save/reload and actual logged results count; untouched authored timer-only items must not invent strength results.
11. Export a backup, restore it, and repeat the saved-routine, app-volume, mode, preparation, and personal-target persistence checks.
12. Review Train, Preview, workout, Progress, History, and Settings in Light, Dark and font scale 1.3. Actions should be ultramarine, Light background blanc cassé with white cards and ink text, and achievement stars yellow. Pilates and Conditioning should show compact static thumbnail cards, including correct search/detail navigation.

## Evidence and remaining gates

Automated test/build results and migration compatibility are recorded in `r8.3-implementation-2026-10-20.md` and the linked verification logs. No screenshot from an older release represents this candidate. The local build environment has no emulator or Maestro, and its available Robolectric native renderer lacks Linux ARM64 binaries; screenshot execution/review remains pending. Run the updated Maestro flows in all three variants on an authorized emulator/device before treating the visual gate as complete.

Owner results: not yet recorded. No owner gate is approved by this checklist.
