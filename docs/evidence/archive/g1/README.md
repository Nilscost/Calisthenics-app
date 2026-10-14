# G1 phone run, Galaxy S21, 2026-10-05 (owner-run, airplane mode, music playing)
Raw files: smoke15s.csv, full46min.csv, spike_done.txt (summary written by the app).
Agent's reading of the numbers (the owner decides G1):
- Full run: 110 blocks, 219/219 cues fired, max lateness 17 ms (mean 3.1 ms), total 2,740,003 ms vs 2,740,000 planned (+3 ms). Threshold: 1 s per cue, 2 s total.
- Offline TTS: 219 spoken, 0 failed; offline English voice present. SoundPool loaded before t=0.
- Audio focus (MAY_DUCK) granted every time (focusDenied=0).
- Owner by ear: cues audible over music in airplane mode. Music volume did NOT dip (ducking not observed).
- Not recorded by the app: whether the screen was locked the whole time (owner reports the test ran as instructed).
