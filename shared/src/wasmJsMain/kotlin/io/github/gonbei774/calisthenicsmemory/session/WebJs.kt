// Browser primitives of the web session host (Phase 4a): Web Audio beeps, speechSynthesis, Screen Wake Lock, page
// lifecycle. Every function swallows browser errors (unsupported API, blocked autoplay): a cue or a wake lock must never
// break a workout. State lives in JS globals so it survives Kotlin object re-creation; `globalThis.__calAudio` also counts
// beeps and records spoken text, which the browser smoke test reads.
@file:Suppress("unused")
package io.github.gonbei774.calisthenicsmemory.session

private const val AUDIO_STATE = "(globalThis.__calAudio ||= { ctx: null, beeps: 0, spoken: [], installed: false, unlocked: false, voiceUnlocked: false })"

/** Creates/resumes the AudioContext and unlocks speechSynthesis on the first user gesture (iOS Safari requires it). Idempotent. */
@JsFun("""() => {
  const A = $AUDIO_STATE;
  if (A.installed) return;
  A.installed = true;
  const unlock = () => {
    try {
      const C = globalThis.AudioContext || globalThis.webkitAudioContext;
      if (C && !A.ctx) A.ctx = new C();
      if (A.ctx && A.ctx.state !== 'running') A.ctx.resume().catch(() => {});
      if (A.ctx && !A.unlocked) {
        const s = A.ctx.createBufferSource();
        s.buffer = A.ctx.createBuffer(1, 1, 22050);
        s.connect(A.ctx.destination);
        s.start(0);
        A.unlocked = true;
      }
      if (globalThis.speechSynthesis && !A.voiceUnlocked) {
        const u = new SpeechSynthesisUtterance('');
        u.volume = 0;
        globalThis.speechSynthesis.speak(u);
        A.voiceUnlocked = true;
      }
    } catch (e) {}
  };
  ['pointerdown', 'pointerup', 'touchend', 'click', 'keydown'].forEach(t => addEventListener(t, unlock, { capture: true, passive: true }));
}""")
external fun webInstallAudioUnlock()

/** Plays the synthesized cue [kind] (0 start, 1 warning, 2 finished) at linear [gain]; false when audio is not unlocked/available. */
@JsFun("""(kind, gain) => {
  try {
    const A = $AUDIO_STATE, c = A.ctx;
    if (!c || c.state !== 'running') return false;
    const t0 = c.currentTime;
    const seq = kind === 0 ? [[1320, 0, 0.25]] : kind === 1 ? [[1000, 0, 0.12]] : [[660, 0, 0.14], [880, 0.16, 0.14], [1100, 0.32, 0.28]];
    for (const [f, dt, d] of seq) {
      const o = c.createOscillator(), g = c.createGain();
      o.type = 'sine'; o.frequency.value = f;
      g.gain.setValueAtTime(0, t0 + dt);
      g.gain.linearRampToValueAtTime(gain, t0 + dt + 0.01);
      g.gain.linearRampToValueAtTime(0, t0 + dt + d);
      o.connect(g); g.connect(c.destination);
      o.start(t0 + dt); o.stop(t0 + dt + d + 0.02);
    }
    A.beeps++;
    return true;
  } catch (e) { return false; }
}""")
internal external fun webBeep(kind: Int, gain: Double): Boolean

@JsFun("() => !!globalThis.speechSynthesis && typeof SpeechSynthesisUtterance !== 'undefined'")
internal external fun webSpeechAvailable(): Boolean

/** Speaks [text] at linear volume [volume], replacing anything still being spoken (Android uses QUEUE_FLUSH too). */
@JsFun("""(text, volume) => {
  try {
    const A = $AUDIO_STATE;
    A.spoken.push(text); if (A.spoken.length > 50) A.spoken.shift();
    const s = globalThis.speechSynthesis;
    s.cancel();
    const u = new SpeechSynthesisUtterance(text);
    u.lang = 'en-US'; u.rate = 0.75; u.volume = volume;
    s.speak(u);
    return true;
  } catch (e) { return false; }
}""")
internal external fun webSpeak(text: String, volume: Double): Boolean

/** Holds a Screen Wake Lock while wanted; re-requests it when the page becomes visible again (the browser drops it when hidden). */
@JsFun("""(want) => {
  const W = (globalThis.__calWake ||= { want: false, sentinel: null, installed: false });
  const request = () => {
    try {
      if (!W.want || W.sentinel || !navigator.wakeLock || document.visibilityState !== 'visible') return;
      navigator.wakeLock.request('screen').then(s => {
        if (!W.want) { s.release().catch(() => {}); return; }
        W.sentinel = s;
        s.addEventListener('release', () => { if (W.sentinel === s) W.sentinel = null; });
      }).catch(() => {});
    } catch (e) {}
  };
  if (!W.installed) {
    W.installed = true;
    document.addEventListener('visibilitychange', request);
  }
  W.want = want;
  if (want) request();
  else if (W.sentinel) { const s = W.sentinel; W.sentinel = null; try { s.release().catch(() => {}); } catch (e) {} }
}""")
internal external fun webWakeLock(want: Boolean)

/** [onVisible] when the tab/app comes back, [onHide] when it is hidden or the page is going away. The latest callbacks win. */
@JsFun("""(onVisible, onHide) => {
  const L = (globalThis.__calLifecycle ||= { installed: false });
  L.onVisible = onVisible; L.onHide = onHide;
  if (L.installed) return;
  L.installed = true;
  document.addEventListener('visibilitychange', () => { if (document.visibilityState === 'visible') L.onVisible(); else L.onHide(); });
  addEventListener('pagehide', () => L.onHide());
}""")
internal external fun webOnLifecycle(onVisible: () -> Unit, onHide: () -> Unit)
