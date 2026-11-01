// Browser smoke test of the production web build (Phase 3c).
//
// Serves web/build/dist/wasmJs/productionExecutable on a random local port, opens it at iPhone size (390x844) in
// WebKit and Chromium with a fresh context each, and walks: onboarding -> Train -> Round preview -> reload (state kept).
// Compose for web draws everything on one canvas and exposes no DOM/accessibility tree (checked: the body holds only
// empty divs), so the walk clicks fixed CSS-pixel positions of this fixed viewport and verifies each step from
// screenshot pixels (ultramarine buttons, grey disabled button, background colour, screen changed and settled).
//
// Starting a real workout needs Phase 4's web session runner (the shared UI sends the commands to a stub), so the walk
// stops at the preview; Phase 4 extends it.
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

    img = await click(313, 725);                                          // Workout preview
    expect(near(px(img, 175, 800), BLUE), 'Preview: Start workout button missing');
    expect(near(px(img, 30, 213), BLUE), 'Preview: "Circuit" format segment not selected');
    await shot('preview');
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
