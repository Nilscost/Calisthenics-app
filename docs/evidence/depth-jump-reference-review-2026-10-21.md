# Depth-jump reference review

This is a bounded frame review of the three downloaded public demonstrations. It is not full-motion playback, a quantitative motion-capture comparison, or a form approval. All contact sheets are decoded from the original downloaded MP4s, resized only for layout, and read in row-major order. Detailed sheets sample at 5 fps; Wil Fleming's sheet starts at 8.5 s and advances by 0.2 s per tile, while Third Space's starts at 0.0 s and advances by 0.2 s per tile. A visible event boundary is therefore uncertain by at least one 0.2 s sampling interval, with extra uncertainty from blur and occlusion.

## Sources and files

| Source | Clip | Encoded video | Local MP4 SHA-256 | Reviewed interval |
|---|---|---|---|---|
| [Catalyst Athletics — Depth Jump | Olympic Weightlifting Exercise Library](https://www.youtube.com/watch?v=GeN0S3XCZnM) | 79.014 s, 854×480, 30 fps (2,369 frames) | `references/GeN0S3XCZnM.mp4` | `70541daa6c5667584bad1c4bae00642075a28dcf99e3e1355eb1aa3766e77f57` | Root's selected first-rep stills, 8.0–12.8 s, with detailed motion review concentrated at 9.2–10.8 s; not full playback. See `references/catalyst-first-rep.jpg`. |
| [Wil Fleming — Max Effort Plyometrics: Depth Jumps](https://www.youtube.com/watch?v=AzPJZHOmGEg) | 16.714 s, 854×480, 29.97 fps (500 frames) | `references/AzPJZHOmGEg.mp4` | `bdbd5bbf05d08d378ac207f76d1c041a82d47662959b6a57572051f851405d0f` | Sparse full-clip context at 1 and 2 fps; focused 8.5–16.5 s at 5 fps. |
| [Third Space London — How To Depth Jumps](https://www.youtube.com/watch?v=DxzbXy0lC6Y) | 5.494 s, 854×480, 25 fps (137 frames) | `references/DxzbXy0lC6Y.mp4` | `fa74fb0e94b0f6313aa23fceb5b082510fd593ab980beaba1e272a9b14c065ef` | Whole clip sampled at 5 fps. |

Focused source-frame sheets:

- Wil Fleming: `AzPJZHOmGEg-8.5to16.5-5fps.jpg` (40 cells; tile *n* is approximately 8.5 + 0.2*n* seconds).
- Third Space: `DxzbXy0lC6Y-0to5.4-5fps.jpg` (27 source cells plus one blank tile; tile *n* is approximately 0.2*n* seconds).
- Catalyst's existing sheet labels each source interval directly; it was reviewed by root, not re-reviewed frame by frame here.

The companion `make_sheets.py` regenerates the two focused sheets with FFmpeg. It requires no image or motion-analysis packages.

## Visible sequence and timing notes

**Catalyst Athletics.** In the selected first-rep stills, the demonstrator starts on a raised platform, steps/drops to the floor, visibly flexes at the hips and knees with the arms back/down, then rebounds with a quick arm swing to an overhead reach near the apex. The floor-load interval appears brief, but the sampled stills do not resolve exact touchdown or toe-off. This is root's bounded review of 9.2–10.8 s, not a claim that the full 79-second clip was watched.

**Wil Fleming.** The video has an opening title overlay, a coach/demonstrator in view, and apparent repositioning/repeated attempts; the whole 16.7-second file is not one clean, uninterrupted repetition. In the focused interval, the demonstrator is on the box around 8.5–9.1 s, then appears on the floor in a hip-and-knee-flexed load around 9.3–9.5 s. The next takeoff is visible by about 9.7 s, with arms swinging up through flight; descent and a flexed landing appear around 10.5–10.9 s. That suggests roughly 0.2–0.4 s of floor contact before rebound and about 0.8–1.2 s of rebound flight, with ±0.2 s boundary uncertainty and appreciable blur. Later tiles include recovery/repositioning and more movement; they should not be used to infer a precise repeated-cycle duration. The clip description warns that impact is high and limits contacts per session, but gives no timing prescription.

**Third Space London.** The source description is unusually explicit: start at the edge with arms overhead; step off one foot then the other rather than hopping; land softly with both feet together while bringing arms down roughly parallel to the shins; then jump high with an arm swing; land softly and repeat. It distinguishes depth jumps from drop jumps by describing more knee bend and longer ground contact. In the visible side view, arms remain raised during the step-off, move down/back through the landing load, swing forward/up for takeoff, and sweep back as the jumper descends/lands. The knees/hips sink into a noticeably deep loaded position; the landing is held in a crouch before rising. One representative pass suggests an initial drop flight around 0.3–0.6 s, a floor load/contact around 0.4–0.7 s, and a rebound flight around 0.6–1.0 s. These broad intervals are read from 0.2 s still spacing, not measured trajectories. The clip shows one pass followed by recovery/fade, not enough repetitions to establish a cycle tempo.

## Cross-source implications and limits

The clearest common arm cue is a brief, purposeful swing coupled to the drop, load, rebound and landing: arms can be overhead on the box, travel down/back during the floor load, reach overhead near rebound apex, then lower/back through landing. The selected references do not support holding the arms rigidly overhead throughout a prolonged flight. They also do not imply one universal contact time: the Third Space description expressly contrasts depth-jump contact with a shorter drop-jump contact, while Catalyst and Wil Fleming frame the rebound as fast/maximal. A renderer should keep the box drop and rebound distinct, avoid floating with static limbs, and let deeper loading be a deliberate variant choice rather than silently stretch all contacts.

Approximate timing is not a calibrated metric. The clips use different camera positions, box heights, body sizes, framing and editing. Source descriptions and the visible demonstrations differ in emphasis; no pixel-to-metre scale or view-matched pose score was used. Still sheets cannot establish forces, exact bilateral touchdown order, exact joint angles, an exact frame-level speed curve, or whether every source segment has been retimed. No speed edit is clearly announced in the reviewed local metadata; that absence does not prove the clips play at capture speed. The review does not endorse an exercise as safe for a particular person.
