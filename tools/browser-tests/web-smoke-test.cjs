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
  '.md': 'text/markdown', '.ttf': 'font/ttf', '.xml': 'application/xml', '.map': 'application/json', '.txt': 'text/plain' };

function serve() {
  const server = http.createServer((req, res) => {
    const rel = decodeURIComponent(new URL(req.url, 'http://x').pathname);
    let file = path.join(dist, rel.endsWith('/') ? rel + 'index.html' : rel);
    if (!file.startsWith(dist) || !fs.existsSync(file) || fs.statSync(file).isDirectory()) { res.writeHead(404); res.end('not found'); return; }
    res.writeHead(200, { 'content-type': MIME[path.extname(file)] || 'application/octet-stream', 'cache-control': 'no-store' });
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
  const context = await browser.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 1, hasTouch: true });
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

    // History is empty before the first workout (no card under the week strip)
    await goTab(243);
    img = await settle(null); await shot('history-empty');
    expect(!near(px(img, 300, 420), WHITE, 3), 'History should be empty before the first workout');
    await goTab(40);
    img = await click(313, 725);                                          // Workout preview
    expect(near(px(img, 175, 800), BLUE), 'Preview: Start workout button missing');
    expect(near(px(img, 30, 213), BLUE), 'Preview: "Circuit" format segment not selected');
    await shot('preview');
    // Start workout (from the preview opened above) -> session screen, timer running, cue audio unlocked and played
    await tap(175, 800);
    img = await waitPx(sessionShowing, 'session screen after Start workout');
    await sleep(1000); await shot('session-work');
    const t1 = await pixels(); await sleep(2200); const t2 = await pixels();
    expect(!same(t1, t2), 'session timer did not advance');
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
