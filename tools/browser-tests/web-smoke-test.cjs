// Browser smoke test of the production web build (Phase 3c).
//
// Serves web/build/dist/wasmJs/productionExecutable on a random local port, opens it at iPhone size (390x844) in
// WebKit and Chromium with a fresh context each, and walks: onboarding -> Train -> Round preview -> reload (state kept).
// Compose for web draws everything on one canvas and exposes no DOM/accessibility tree (checked: the body holds only
// empty divs), so the walk clicks fixed CSS-pixel positions of this fixed viewport and verifies each step from
// screenshot pixels (ultramarine buttons, grey disabled button, background colour, screen changed and settled).
//
// Phase 4a: the walk then starts a real workout (shared SessionRunner in the page): session screen, running timer, Web Audio
// start cue, Done, End workout (confirm), end screen, Done, History shows it, survives a reload; and a mid-workout reload
// offers recovery (Continue -> session -> end -> History shows a second workout).
//
// Phase 4b: the walk also checks the demo clip in the session screen (a muted, looping, playing <video> over the canvas),
// a real thumbnail on the Progress tree, browser Back / Escape from Preview to Train (history.back()), and a backup round
// trip: change a setting, Export (download), wipe all browser storage, redo onboarding, Check + Restore the downloaded file,
// and see History and the setting come back.
//
// Env: WEB_DIST (default web/build/dist/wasmJs/productionExecutable), WEB_SMOKE_OUT (screenshot dir, default
// $TMPDIR/web-smoke), WEB_SMOKE_ENGINES (comma list, default "webkit,chromium").
const { chromium, webkit } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const { PNG } = require('pngjs');
const http = require('node:http');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const dist = process.env.WEB_DIST || path.join(__dirname, '../../web/build/dist/wasmJs/productionExecutable');
const outDir = process.env.WEB_SMOKE_OUT || path.join(os.tmpdir(), 'web-smoke');
const engines = (process.env.WEB_SMOKE_ENGINES || 'webkit,chromium').split(',');

// Console policy: every console error and page error fails the run, except exactly these two known upstream messages
// (docs/evidence/kmp-port.md, "Phase 3a" toolchain decision and docs/evidence/kmp-preparation.md). Both are emitted by
// the app bundle itself (calisthenics-web.js), so the source is matched too. Nothing is patched.
const ALLOWED_CONSOLE_ERRORS = [
  {
    // (a) Compose Multiplatform resources 1.12.2 on Kotlin 2.4.21 read wasm memory through the deprecated
    //     `wasmExports.memory`. Fixed upstream in Compose Multiplatform 1.13.0-beta01.
    id: 'cmp-resources-wasm-memory',
    engines: ['webkit', 'chromium'],
    text: s => s.startsWith('Accessing `memory` via `wasmExports` is deprecated. Use `kotlin.wasm.unsafe.wasmMemory`'),
    source: url => url.endsWith('/calisthenics-web.js'),
  },
  {
    // (b) Skiko 0.150.2 queries the unmasked WebGL renderer without enabling WEBGL_debug_renderer_info; WebKit logs
    //     INVALID_ENUM. Not fixed in any Skiko release through 0.156.1.
    id: 'skiko-webkit-invalid-enum',
    engines: ['webkit'],
    text: s => s === 'WebGL: INVALID_ENUM: getParameter: invalid parameter name, WEBGL_debug_renderer_info not enabled',
    source: url => url.endsWith('/calisthenics-web.js'),
  },
];

const BLUE = [48, 58, 242];   // ultramarine accent #303AF2
const PAPER = [247, 245, 240]; // background #F7F5F0
const MIME = { '.html': 'text/html', '.js': 'text/javascript', '.wasm': 'application/wasm', '.json': 'application/json',
  '.md': 'text/markdown', '.ttf': 'font/ttf', '.xml': 'application/xml', '.map': 'application/json', '.txt': 'text/plain',
  '.mp4': 'video/mp4', '.webp': 'image/webp' };

function serve() {
  const server = http.createServer((req, res) => {
    const rel = decodeURIComponent(new URL(req.url, 'http://x').pathname);
    let file = path.join(dist, rel.endsWith('/') ? rel + 'index.html' : rel);
    if (!file.startsWith(dist) || !fs.existsSync(file) || fs.statSync(file).isDirectory()) { res.writeHead(404); res.end('not found'); return; }
    const size = fs.statSync(file).size;
    const headers = { 'content-type': MIME[path.extname(file)] || 'application/octet-stream', 'cache-control': 'no-store', 'accept-ranges': 'bytes' };
    // WebKit only plays video from a server that answers byte-range requests (as GitHub Pages does)
    const m = /^bytes=(\d*)-(\d*)$/.exec(req.headers.range || '');
    if (m && (m[1] || m[2])) {
      const start = m[1] ? Number(m[1]) : Math.max(0, size - Number(m[2]));
      const end = m[1] && m[2] ? Math.min(Number(m[2]), size - 1) : size - 1;
      if (start >= size || start > end) { res.writeHead(416, { 'content-range': `bytes */${size}` }); res.end(); return; }
      res.writeHead(206, { ...headers, 'content-range': `bytes ${start}-${end}/${size}`, 'content-length': end - start + 1 });
      fs.createReadStream(file, { start, end }).pipe(res);
      return;
    }
    res.writeHead(200, { ...headers, 'content-length': size });
    fs.createReadStream(file).pipe(res);
  });
  return new Promise(resolve => server.listen(0, '127.0.0.1', () => resolve(server)));
}

const sleep = ms => new Promise(r => setTimeout(r, ms));
const near = (px, rgb, tol = 40) => px.every((v, i) => Math.abs(v - rgb[i]) <= tol);

async function run(engine, browserType, base) {
  const dir = path.join(outDir, engine);
  fs.mkdirSync(dir, { recursive: true });
  const browser = await browserType.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 1, hasTouch: true, acceptDownloads: true });
  const page = await context.newPage();
  const errors = [], allowed = [];
  page.on('console', m => {
    if (m.type() !== 'error') return;
    const text = m.text(), url = m.location().url || '';
    const hit = ALLOWED_CONSOLE_ERRORS.find(a => a.engines.includes(engine) && a.text(text) && a.source(url));
    (hit ? allowed : errors).push(hit ? { id: hit.id } : `${text} (${url})`);
  });
  page.on('pageerror', e => errors.push(`pageerror: ${e.message}`));
  page.on('requestfailed', r => errors.push(`request failed: ${r.url()}`));
  page.on('response', r => { if (r.status() >= 400) errors.push(`HTTP ${r.status()}: ${r.url()}`); });

  let step = 0;
  const shot = async name => {
    const buf = await page.screenshot({ path: path.join(dir, `${String(++step).padStart(2, '0')}-${name}.png`) });
    return buf;
  };
  const pixels = async () => PNG.sync.read(await page.screenshot());
  const px = (img, x, y) => { const i = (img.width * y + x) * 4; return [img.data[i], img.data[i + 1], img.data[i + 2]]; };
  const same = (a, b) => a.data.equals(b.data);
  const settle = async before => {
    // wait until the screen differs from `before` (when given), then until two shots 250 ms apart are identical
    const deadline = Date.now() + 15000;
    let prev = await pixels();
    for (;;) {
      await sleep(250);
      const cur = await pixels();
      if ((!before || !same(cur, before)) && same(cur, prev)) return cur;
      prev = cur;
      if (Date.now() > deadline) throw new Error(`${engine}: screen did not change/settle`);
    }
  };
  const click = async (x, y) => { const before = await pixels(); await page.mouse.click(x, y); return settle(before); };
  const expect = (cond, what) => { if (!cond) throw new Error(`${engine}: ${what}`); };
  const dbg = (img, x, y) => `pixel(${x},${y})=${px(img, x, y)}`;

  try {
    await page.goto(base);
    await page.waitForSelector('canvas', { timeout: 60000 });
    let img = await settle(null);
    // a blank/background-only first frame would settle too; wait for the Next button to appear
    const appear = Date.now() + 30000;
    while (!near(px(img, 305, 800), BLUE)) {
      expect(Date.now() < appear, 'onboarding Next button did not appear');
      await sleep(300); img = await pixels();
    }
    expect(near(px(img, 5, 400), PAPER, 3), 'background is not #F7F5F0');
    await shot('onboarding-welcome');

    img = await click(334, 800); await shot('onboarding-goal');           // Welcome -> Goal
    img = await click(334, 800); await shot('onboarding-equipment');      // Goal -> Equipment
    img = await click(334, 800); await shot('onboarding-level-1');        // Equipment -> Level (first movement pattern)
    const patterns = 6;
    for (let i = 1; i <= patterns; i++) {
      expect(!near(px(img, 305, 800), BLUE), `level step ${i}: Next should be disabled before choosing`);
      img = await click(285, 182);                                        // "I don't know"
      expect(near(px(img, 210, 182), BLUE), `level step ${i}: "I don't know" not selected (${dbg(img, 210, 182)})`);
      img = await click(190, 420);                                        // "Normal" suggestion
      expect(near(px(img, 305, 800), BLUE), `level step ${i}: Next not enabled after choosing`);
      if (i === 1) await shot('onboarding-level-suggestions');
      img = await click(334, 800);                                        // Next
    }
    await shot('onboarding-training-mode');
    img = await click(334, 800);                                          // Next -> "You are ready"
    await shot('onboarding-ready');
    img = await click(307, 800);                                          // Start training -> Train
    await sleep(500); img = await settle(null);
    expect(near(px(img, 40, 725), BLUE), 'Train: Start workout button missing');
    await shot('train');

    await page.reload();
    await page.waitForSelector('canvas', { timeout: 60000 });
    const back = Date.now() + 30000;
    for (;;) {
      img = await pixels();
      // after the reload the Train screen is shown directly (onboarding is remembered); onboarding would show Next, not this
      if (near(px(img, 40, 725), BLUE)) break;
      expect(Date.now() < back, 'after reload the Train screen did not appear (onboarding not persisted?)');
      await sleep(300);
    }
    img = await settle(null);
    await shot('train-after-reload');

    // ---- Phase 4a: a real workout in the page --------------------------------------------------------------------------
    const WHITE = [255, 255, 255];
    const waitPx = async (pred, what, ms = 20000) => {
      const end = Date.now() + ms;
      for (;;) {
        const im = await pixels();
        if (pred(im)) return im;
        expect(Date.now() < end, `timeout waiting for ${what}`);
        await sleep(250);
      }
    };
    const tap = async (x, y) => { await page.mouse.click(x, y); await sleep(350); };
    const goTab = async x => { await tap(x, 806); await sleep(1200); return pixels(); };  // 40 Train, 243 History
    const audio = () => page.evaluate(() => { const a = globalThis.__calAudio; return a && { state: a.ctx && a.ctx.state, beeps: a.beeps, spoken: a.spoken.length }; });
    const sessionShowing = im => near(px(im, 150, 803), BLUE, 30) && !near(px(im, 40, 725), BLUE, 10); // DONE button, no Train button
    const endWorkout = async () => {
      await tap(350, 32); await shot('session-menu');                      // overflow menu -> End workout
      await tap(315, 88); await sleep(500); await shot('end-confirm');     // confirm dialog
      await tap(278, 478);                                                 // End workout
      const im = await waitPx(i => near(px(i, 80, 410), BLUE, 30) && near(px(i, 200, 650), PAPER, 6), 'end screen');
      await sleep(1500);                                                   // "Saved to your history."
      return im;
    };
    const openHistory = async () => { await goTab(243); return settle(null); };

    // ---- Phase 4b: thumbnails (Progress tree), browser Back and Escape -------------------------------------------------
    const nonWhite = (im, x0, y0, x1, y1) => { let n = 0; for (let y = y0; y < y1; y++) for (let x = x0; x < x1; x++) if (!near(px(im, x, y), WHITE, 30)) n++; return n; };
    await goTab(140);                                                      // Progress: first tree card shows a picture, not a letter
    img = await waitPx(i => nonWhite(i, 46, 345, 112, 388) > 300, 'a real thumbnail on the first Progress card (not the letter fallback)', 15000);
    await shot('progress-thumbnails');
    await goTab(40);

    // History is empty before the first workout (no card under the week strip)
    await goTab(243);
    img = await settle(null); await shot('history-empty');
    expect(!near(px(img, 300, 420), WHITE, 3), 'History should be empty before the first workout');
    await goTab(40);
    img = await click(313, 725);                                          // Workout preview
    expect(near(px(img, 175, 800), BLUE), 'Preview: Start workout button missing');
    expect(near(px(img, 30, 213), BLUE), 'Preview: "Circuit" format segment not selected');
    await shot('preview');
    // browser Back (history.back(), what the iOS/Android swipe does) and Escape go back inside the app, not out of it
    const trainShowing = i => near(px(i, 40, 725), BLUE);
    const historyLen = () => page.evaluate(() => history.length);
    const len0 = await historyLen();
    await page.evaluate(() => history.back());
    await waitPx(trainShowing, 'Train after history.back() from Preview'); await shot('train-after-browser-back');
    expect(page.url().startsWith(base), 'history.back() left the app');
    await click(313, 725);                                                 // Preview again
    await page.keyboard.press('Escape');
    await waitPx(trainShowing, 'Train after Escape on Preview'); await shot('train-after-escape');
    for (let k = 0; k < 3; k++) { await click(313, 725); await sleep(500); await page.evaluate(() => history.back()); await waitPx(trainShowing, 'Train after repeated Back'); }
    await sleep(400);
    expect(await historyLen() <= len0 + 1, `history grew: ${len0} -> ${await historyLen()}`);
    img = await click(313, 725);                                           // Preview for the workout
    // Start workout (from the preview opened above) -> session screen, timer running, cue audio unlocked and played
    await tap(175, 800);
    img = await waitPx(sessionShowing, 'session screen after Start workout');
    await sleep(1000); await shot('session-work');
    const t1 = await pixels(); await sleep(2200); const t2 = await pixels();
    expect(!same(t1, t2), 'session timer did not advance');
    // the demo clip: a muted, looping, autoplaying <video> over the canvas, actually playing (time advances)
    const clipInfo = () => page.evaluate(() => {
      const v = document.querySelector('video[data-cal-clip]');
      if (!v) return null;
      const r = v.getBoundingClientRect();
      return { t: v.currentTime, ready: v.readyState, paused: v.paused, muted: v.muted, loop: v.loop, inline: v.playsInline, w: v.videoWidth,
        rect: [r.left, r.top, r.width, r.height], pe: getComputedStyle(v).pointerEvents, src: v.currentSrc.split('/').slice(-2).join('/') };
    });
    let c1 = null;
    const clipDeadline = Date.now() + 20000;
    while (!(c1 && c1.ready >= 2 && !c1.paused)) { c1 = await clipInfo(); expect(Date.now() < clipDeadline, `demo clip not playing: ${JSON.stringify(c1)}`); await sleep(300); }
    // the clips are short loops, so currentTime wraps: sample several times and require movement between samples
    const samples = [c1.t];
    for (let k = 0; k < 6; k++) { await sleep(250); const c = await clipInfo(); samples.push(c.t); }
    const c2 = await clipInfo();
    expect(new Set(samples.map(t => t.toFixed(2))).size >= 4, `demo clip time does not advance: ${JSON.stringify(samples)}`);
    expect(c2.muted && c2.loop && c2.inline && c2.pe === 'none', `clip flags: ${JSON.stringify(c2)}`);
    expect(c2.w === 480 && c2.rect[2] > 200 && c2.rect[1] > 100 && c2.rect[1] + c2.rect[3] < 520, `clip overlay position: ${JSON.stringify(c2)}`);
    await shot('session-clip');
    const au = await audio();
    expect(au && au.state === 'running' && au.beeps >= 1, `Web Audio start cue not played: ${JSON.stringify(au)}`);

    // Done on the first block: the screen moves on to the next block (stretch)
    const before = await pixels();
    await tap(194, 803); await sleep(1200);
    const after = await pixels(); await shot('session-after-done');
    const headerChanged = (() => { for (let y = 60; y < 160; y++) for (let x = 10; x < 380; x++) if (px(before, x, y).some((v, i) => Math.abs(v - px(after, x, y)[i]) > 30)) return true; return false; })();
    expect(headerChanged, 'Done did not advance to the next block');

    // End workout (confirm) -> end screen -> Done -> Train -> History shows the workout; it survives a reload
    img = await endWorkout(); await shot('end-screen');
    await tap(195, 410); await sleep(1200);
    img = await settle(null);
    expect(near(px(img, 40, 725), BLUE), 'Done on the end screen did not return to Train');
    img = await openHistory(); await shot('history-after-workout');
    expect(near(px(img, 300, 420), WHITE, 3), 'History does not show the finished workout');
    await page.reload();
    await page.waitForSelector('canvas', { timeout: 60000 });
    await waitPx(i => near(px(i, 40, 725), BLUE), 'Train after reload');
    img = await openHistory(); await shot('history-after-reload');
    expect(near(px(img, 300, 420), WHITE, 3), 'History lost the workout after reload');

    // Mid-workout reload -> recovery offered -> Continue -> session screen; ending it saves a second workout
    await goTab(40);
    await tap(313, 725); await sleep(800);
    await tap(175, 800);
    await waitPx(sessionShowing, 'second session screen');
    await sleep(1000); await tap(194, 803); await sleep(1500);               // finish one block so the checkpoint is not at the start
    await page.reload();
    await page.waitForSelector('canvas', { timeout: 60000 });
    img = await waitPx(i => near(px(i, 117, 172), BLUE, 30), 'recovery card with "Continue it"');
    await settle(null); await shot('recovery-offered');
    await tap(117, 172);
    await waitPx(i => !near(px(i, 40, 725), BLUE, 10) && !near(px(i, 117, 172), BLUE, 30), 'session screen after Continue');
    await sleep(1000); await shot('recovered-session');
    img = await endWorkout(); await shot('end-screen-recovered');
    await tap(195, 410); await sleep(1200);
    img = await openHistory(); await shot('history-two-workouts');
    expect(near(px(img, 300, 490), WHITE, 3), 'History does not show the second (recovered) workout');

    // ---- Phase 4b: backup round trip -----------------------------------------------------------------------------------
    const region = (im, x0, y0, x1, y1) => { const out = []; for (let y = y0; y < y1; y++) for (let x = x0; x < x1; x++) out.push(px(im, x, y).join(',')); return out.join(';'); };
    const ROUNDS = [272, 266, 300, 292];      // the "Rounds per workout" number on Settings
    const openSettings = async () => { await goTab(345); return settle(null); };
    const scrollSettingsDown = async () => { await page.mouse.move(200, 400); await page.mouse.wheel(0, 1000); await sleep(800); return settle(null); };
    const blueRowY = (im, x, y0, y1) => { for (let y = y0; y < y1; y++) if (near(px(im, x, y), BLUE, 30)) return y; return -1; };
    img = await openSettings();
    img = await click(334, 278);                                           // Rounds per workout 4 -> 5 (a setting that must survive)
    const roundsBefore = region(img, ...ROUNDS);
    await shot('settings-rounds-5');
    await scrollSettingsDown();
    img = await click(195, 688);                                           // Open-source licences: the generated list, then browser Back
    let dark = 0; for (let y = 80; y < 200; y++) for (let x = 10; x < 380; x++) if (px(img, x, y).every(v => v < 90)) dark++;
    expect(dark > 300, `licences list did not render (${dark} dark pixels)`);
    await shot('licences');
    await page.evaluate(() => history.back()); await sleep(1500); await settle(null);
    await scrollSettingsDown();
    await click(195, 510);                                                 // Backup and restore
    await shot('backup-screen');
    const [download] = await Promise.all([page.waitForEvent('download', { timeout: 15000 }), page.mouse.click(86, 186)]);   // Export backup
    const backupFile = path.join(dir, 'calisthenics-backup.json');
    await download.saveAs(backupFile);
    expect(download.suggestedFilename() === 'calisthenics-backup.json', `download name ${download.suggestedFilename()}`);
    const backup = JSON.parse(fs.readFileSync(backupFile, 'utf8'));
    const payload = JSON.parse(backup.payload);
    expect(backup.formatVersion >= 1 && payload.sessions.length === 2, `backup content: ${backup.formatVersion}, ${payload.sessions && payload.sessions.length} sessions`);
    await sleep(800); await shot('backup-exported');

    // wipe ALL browser storage (localStorage + IndexedDB), reload: the app starts empty (onboarding) like a new device
    await page.evaluate(async () => {
      localStorage.clear(); sessionStorage.clear();
      const dbs = await indexedDB.databases();
      await Promise.all(dbs.map(d => new Promise(r => { const q = indexedDB.deleteDatabase(d.name); q.onsuccess = q.onerror = q.onblocked = () => r(); })));
    });
    await page.reload();
    await page.waitForSelector('canvas', { timeout: 60000 });
    img = await waitPx(i => near(px(i, 305, 800), BLUE) && near(px(i, 5, 400), PAPER, 3), 'onboarding after the wipe');
    await shot('wiped-onboarding');
    await click(334, 800); await click(334, 800); await click(334, 800);
    for (let i = 1; i <= 6; i++) { await click(285, 182); await click(190, 420); await click(334, 800); }
    await click(334, 800); await click(307, 800);
    await waitPx(trainShowing, 'Train after the second onboarding');
    img = await openHistory(); await shot('history-wiped');
    expect(!near(px(img, 300, 420), WHITE, 3), 'History is not empty after wiping storage');
    img = await openSettings();
    expect(region(img, ...ROUNDS) !== roundsBefore, 'the rounds setting was not reset by the wipe');
    await scrollSettingsDown();
    await click(195, 510);                                                 // Backup and restore
    const chooser = page.waitForEvent('filechooser', { timeout: 15000 });
    await page.mouse.click(103, 244);                                      // Check a backup file
    await (await chooser).setFiles(backupFile);
    await sleep(1500); img = await settle(null); await shot('backup-checked');
    const restoreY = blueRowY(img, 40, 280, 700);
    expect(restoreY > 0, 'no Restore button after checking the backup file');
    await page.mouse.click(40, restoreY + 4); await sleep(1500); img = await settle(null); await shot('backup-restored');
    await tap(33, 32); await sleep(1000);                                  // Back to Settings (the nav bar is hidden on this screen)
    img = await openHistory(); await shot('history-restored');
    expect(near(px(img, 300, 420), WHITE, 3) && near(px(img, 300, 490), WHITE, 3), 'History was not restored (two workouts expected)');
    img = await openSettings();
    expect(region(img, ...ROUNDS) === roundsBefore, 'the rounds setting was not restored');
    await shot('settings-restored');
  } catch (e) {
    await shot('failure').catch(() => {});
    if (errors.length) e.message += `\nconsole/page errors so far:\n${errors.join('\n')}`;
    throw e;
  } finally {
    await context.close(); await browser.close();
  }
  if (errors.length) throw new Error(`${engine} console/page errors:\n${errors.join('\n')}`);
  return { engine, steps: step, allowedConsoleErrors: allowed.map(a => a.id) };
}

(async () => {
  expectDist();
  const server = await serve();
  const base = `http://127.0.0.1:${server.address().port}/`;
  const results = [];
  let failed = false;
  for (const name of engines) {
    try { results.push(await run(name, name === 'webkit' ? webkit : chromium, base)); }
    catch (e) { failed = true; console.error(e.message || e); results.push({ engine: name, error: String(e.message || e) }); }
  }
  server.close();
  fs.mkdirSync(outDir, { recursive: true });
  fs.writeFileSync(path.join(outDir, 'results.json'), JSON.stringify(results, null, 2) + '\n');
  console.log(JSON.stringify(results, null, 2));
  process.exitCode = failed ? 1 : 0;
})();

function expectDist() {
  if (!fs.existsSync(path.join(dist, 'index.html'))) throw new Error(`No web build at ${dist}; run ./gradlew :web:wasmJsBrowserDistribution first`);
}
