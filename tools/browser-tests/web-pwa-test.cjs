// Browser test of the PWA layer (Phase 5): manifest + icons + iOS meta tags, service worker offline use of a clip (with
// byte-range answers as Safari needs), and a two-build update (build A visited, build B deployed, reload -> B active, A's
// caches gone, Compose's own resource cache not stale). Served under /Calisthenics-app/ like GitHub Pages.
//
// Env: WEB_DIST (default web/build/dist/wasmJs/productionExecutable), WEB_PWA_ENGINES (default "webkit,chromium"),
//      WEB_PWA_OUT (default $TMPDIR/web-pwa).
const { chromium, webkit } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const { ALLOWED_CONSOLE_ERRORS } = require('./console-policy.cjs');
const http = require('node:http');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const distA = process.env.WEB_DIST || path.join(__dirname, '../../web/build/dist/wasmJs/productionExecutable');
const outDir = process.env.WEB_PWA_OUT || path.join(os.tmpdir(), 'web-pwa');
const engines = (process.env.WEB_PWA_ENGINES || 'webkit,chromium').split(',');
const PREFIX = '/Calisthenics-app/';
const MIME = { '.html': 'text/html', '.js': 'text/javascript', '.wasm': 'application/wasm', '.json': 'application/json', '.webmanifest': 'application/manifest+json',
  '.md': 'text/markdown', '.ttf': 'font/ttf', '.png': 'image/png', '.mp4': 'video/mp4', '.webp': 'image/webp' };
const sleep = ms => new Promise(r => setTimeout(r, ms));

function serve(state) {
  const server = http.createServer((req, res) => {
    if (state.down) { req.socket.destroy(); return; }
    const rel = decodeURIComponent(new URL(req.url, 'http://x').pathname);
    if (!rel.startsWith(PREFIX)) { res.writeHead(404); res.end(); return; }
    const dist = state.root;
    const file = path.join(dist, rel.slice(PREFIX.length) || 'index.html');
    if (!file.startsWith(dist) || !fs.existsSync(file) || fs.statSync(file).isDirectory()) { res.writeHead(404); res.end('not found'); return; }
    const size = fs.statSync(file).size;
    const headers = { 'content-type': MIME[path.extname(file)] || 'application/octet-stream', 'cache-control': 'no-store', 'accept-ranges': 'bytes' };
    const m = /^bytes=(\d*)-(\d*)$/.exec(req.headers.range || '');
    if (m && (m[1] || m[2])) {
      const start = m[1] ? Number(m[1]) : Math.max(0, size - Number(m[2]));
      const end = m[1] && m[2] ? Math.min(Number(m[2]), size - 1) : size - 1;
      res.writeHead(206, { ...headers, 'content-range': `bytes ${start}-${end}/${size}`, 'content-length': end - start + 1 });
      fs.createReadStream(file, { start, end }).pipe(res);
      return;
    }
    res.writeHead(200, { ...headers, 'content-length': size });
    fs.createReadStream(file).pipe(res);
  });
  return new Promise(resolve => server.listen(0, '127.0.0.1', () => resolve(server)));
}

function makeBuildB(tmp) {
  const b = path.join(tmp, 'buildB');
  fs.cpSync(distA, b, { recursive: true });
  // a changed Compose resource under the same name (stale-cache check), a changed data file, and a new build id
  const font = path.join(b, 'composeResources/io.github.gonbei774.calisthenicsmemory.shared.resources/font/barlow_condensed_bold.ttf');
  fs.appendFileSync(font, Buffer.alloc(64));
  fs.appendFileSync(path.join(b, 'data/privacy.md'), '\nBuild B.\n');
  const sw = fs.readFileSync(path.join(b, 'sw.js'), 'utf8').replace(/const BUILD_ID = '[^']*'/, "const BUILD_ID = 'build-b-test'");
  fs.writeFileSync(path.join(b, 'sw.js'), sw);
  return { dir: b, fontSize: fs.statSync(font).size, fontRel: 'composeResources/io.github.gonbei774.calisthenicsmemory.shared.resources/font/barlow_condensed_bold.ttf' };
}

async function run(engine, browserType, state, base, A, B) {
  const dir = path.join(outDir, engine); fs.mkdirSync(dir, { recursive: true });
  const browser = await browserType.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 390, height: 844 }, deviceScaleFactor: 1, hasTouch: true });
  const page = await context.newPage();
  const errors = [];
  page.on('console', m => {
    if (m.type() !== 'error') return;
    const text = m.text(), url = m.location().url || '';
    if (!ALLOWED_CONSOLE_ERRORS.some(a => a.engines.includes(engine) && a.text(text) && a.source(url))) errors.push(`${text} (${url})`);
  });
  page.on('pageerror', e => errors.push(`pageerror: ${e.message}`));
  page.on('response', r => { if (r.status() >= 400) errors.push(`HTTP ${r.status()}: ${r.url()}`); });
  const expect = (cond, what) => { if (!cond) throw new Error(`${engine}: ${what}`); };
  const steps = [];
  const version = () => page.evaluate(() => new Promise((resolve, reject) => {
    const c = navigator.serviceWorker.controller;
    if (!c) return resolve(null);
    const ch = new MessageChannel(); ch.port1.onmessage = e => resolve(e.data);
    c.postMessage({ type: 'version' }, [ch.port2]);
    setTimeout(() => reject(new Error('no version reply')), 5000);
  }));
  const cacheNames = () => page.evaluate(async () => (await caches.keys()).sort());
  const waitFor = async (fn, what, ms = 30000) => { const end = Date.now() + ms; for (;;) { const v = await fn(); if (v) return v; expect(Date.now() < end, `timeout: ${what}`); await sleep(250); } };
  try {
    // ---- manifest, icons, meta tags --------------------------------------------------------------------------------
    await page.goto(base);
    await page.waitForSelector('canvas', { timeout: 60000 });
    const head = await page.evaluate(() => ({
      manifest: document.querySelector('link[rel=manifest]')?.href,
      touch: document.querySelector('link[rel=apple-touch-icon]')?.href,
      capable: document.querySelector('meta[name=apple-mobile-web-app-capable]')?.content,
      status: document.querySelector('meta[name=apple-mobile-web-app-status-bar-style]')?.content,
      title: document.querySelector('meta[name=apple-mobile-web-app-title]')?.content,
      viewport: document.querySelector('meta[name=viewport]')?.content,
      theme: document.querySelector('meta[name=theme-color]')?.content,
    }));
    expect(head.manifest === base + 'manifest.webmanifest', `manifest link ${head.manifest}`);
    expect(head.touch === base + 'icons/apple-touch-icon.png', `apple-touch-icon link ${head.touch}`);
    expect(head.capable === 'yes' && head.status && head.title === 'Calisthenics', `iOS meta tags ${JSON.stringify(head)}`);
    expect(/viewport-fit=cover/.test(head.viewport) && head.theme === '#303AF2', `viewport/theme ${JSON.stringify(head)}`);
    const manifest = await (await context.request.get(head.manifest)).json();
    expect(manifest.name === 'Calisthenics' && manifest.short_name && manifest.display === 'standalone', 'manifest name/display');
    expect(manifest.theme_color === '#303AF2' && manifest.background_color === '#F7F5F0', 'manifest colours');
    const start = new URL(manifest.start_url, head.manifest).href, scope = new URL(manifest.scope, head.manifest).href;
    expect(start === base && scope === base, `start_url/scope not under the deploy path: ${start} ${scope}`);
    const purposes = new Set(manifest.icons.map(i => `${i.sizes}/${i.purpose}`));
    expect(purposes.has('192x192/any') && purposes.has('512x512/any') && purposes.has('512x512/maskable'), `manifest icons ${[...purposes]}`);
    const iconUrls = [...manifest.icons.map(i => [new URL(i.src, head.manifest).href, i.sizes]), [head.touch, '180x180']];
    for (const [url, sizes] of iconUrls) {
      const dims = await page.evaluate(u => new Promise(res => { const i = new Image(); i.onload = () => res(`${i.naturalWidth}x${i.naturalHeight}`); i.onerror = () => res('error'); i.src = u; }), url);
      expect(dims === sizes, `icon ${url} is ${dims}, expected ${sizes}`);
    }
    steps.push('manifest, meta tags, 4 icons');

    // ---- service worker, clip offline ----------------------------------------------------------------------------------
    await waitFor(() => page.evaluate(() => !!navigator.serviceWorker.controller), 'service worker controls the page');
    const vA = await version();
    expect(vA && vA.buildId && vA.mediaId, 'service worker version');
    const catalogUrl = base + 'data/catalog.json';
    const clipName = fs.readdirSync(path.join(A, 'data/demos'))[0];
    const clipUrl = base + 'data/demos/' + clipName;
    const playClip = () => page.evaluate(async u => {
      const v = document.createElement('video'); v.muted = true; v.loop = true; v.playsInline = true; v.src = u;
      v.style.cssText = 'position:fixed;left:0;top:0;width:10px;height:10px;z-index:-1'; document.body.appendChild(v);
      await v.play();
      const t0 = v.currentTime; await new Promise(r => setTimeout(r, 1200));
      const out = { readyState: v.readyState, advanced: v.currentTime > t0 || v.currentTime > 0, w: v.videoWidth };
      v.remove(); return out;
    }, clipUrl);
    const online = await playClip();
    expect(online.readyState >= 2 && online.w > 0, `clip did not play online ${JSON.stringify(online)}`);
    expect((await cacheNames()).some(n => n.startsWith('cal-media-')), 'media cache missing after playing a clip');
    const rng = await page.evaluate(async u => { const r = await fetch(u, { headers: { Range: 'bytes=0-1' } }); return { status: r.status, cr: r.headers.get('content-range'), len: (await r.arrayBuffer()).byteLength }; }, clipUrl);
    const size = fs.statSync(path.join(A, 'data/demos', clipName)).size;
    expect(rng.status === 206 && rng.cr === `bytes 0-1/${size}` && rng.len === 2, `range answer ${JSON.stringify(rng)}`);
    const tail = await page.evaluate(async u => { const r = await fetch(u, { headers: { Range: 'bytes=-10' } }); return { status: r.status, cr: r.headers.get('content-range') }; }, clipUrl);
    expect(tail.status === 206 && tail.cr === `bytes ${size - 10}-${size - 1}/${size}`, `suffix range answer ${JSON.stringify(tail)}`);
    state.down = true;
    await page.reload();
    await page.waitForSelector('canvas', { timeout: 60000 });
    const offline = await playClip();
    expect(offline.readyState >= 2 && offline.w > 0, `clip did not play offline ${JSON.stringify(offline)}`);
    const rngOff = await page.evaluate(async u => { const r = await fetch(u, { headers: { Range: 'bytes=0-1' } }); return r.status; }, clipUrl);
    expect(rngOff === 206, `offline range status ${rngOff}`);
    state.down = false;
    steps.push('clip plays offline; range requests answered with 206 from cache');

    // ---- two-build update ------------------------------------------------------------------------------------------------
    // make sure Compose's own cache is populated by build A (the app has loaded its font/strings)
    const composeCacheA = await page.evaluate(async rel => {
      const out = {};
      for (const name of await caches.keys()) if (!name.startsWith('cal-')) { const c = await caches.open(name); out[name] = (await c.keys()).length; }
      return out;
    });
    const namesA = await cacheNames();
    expect(namesA.includes(`cal-shell-${vA.buildId}`), 'shell cache of build A');
    state.root = B.dir;
    await page.reload();
    await page.waitForSelector('canvas', { timeout: 60000 });
    await waitFor(async () => (await version().catch(() => null))?.buildId === 'build-b-test', 'build B active');
    await waitFor(async () => { const n = await cacheNames(); return !n.includes(`cal-shell-${vA.buildId}`); }, 'old shell cache deleted');
    const namesB = await cacheNames();
    expect(namesB.includes('cal-shell-build-b-test'), `shell cache of B missing ${namesB}`);
    expect(!namesB.includes(`cal-shell-${vA.buildId}`), 'old shell cache still there');
    // the page (reloaded after the switch) shows build B's bytes for a Compose resource and the data file
    await page.waitForSelector('canvas', { timeout: 60000 });
    await sleep(1500);
    const seen = await page.evaluate(async rel => {
      const url = new URL(rel, document.baseURI).href;
      const direct = (await (await fetch(url)).arrayBuffer()).byteLength;
      let composeCached = null;
      for (const name of await caches.keys()) if (!name.startsWith('cal-')) { const c = await caches.open(name); const hit = await c.match(url); if (hit) composeCached = (await hit.arrayBuffer()).byteLength; }
      const priv = await (await fetch('data/privacy.md')).text();
      return { direct, composeCached, priv: priv.includes('Build B.') };
    }, B.fontRel);
    expect(seen.direct === B.fontSize, `font served is not build B's (${seen.direct} vs ${B.fontSize})`);
    expect(seen.composeCached === null || seen.composeCached === B.fontSize, `Compose resource cache is stale (${seen.composeCached} vs ${B.fontSize})`);
    expect(seen.priv, 'privacy.md is not build B\'s');
    await page.screenshot({ path: path.join(dir, 'build-b.png') });
    steps.push(`update A(${vA.buildId}) -> B: B active, old shell cache deleted, Compose cache entries before: ${JSON.stringify(composeCacheA)}, after: ${JSON.stringify(await page.evaluate(async () => { const o = {}; for (const n of await caches.keys()) if (!n.startsWith('cal-')) o[n] = (await (await caches.open(n)).keys()).length; return o; }))}`);
  } catch (e) {
    await page.screenshot({ path: path.join(dir, 'failure.png') }).catch(() => {});
    if (errors.length) e.message += `\nconsole/page errors so far:\n${errors.join('\n')}`;
    throw e;
  } finally { state.down = false; state.root = A; await context.close(); await browser.close(); }
  if (errors.length) throw new Error(`${engine} console/page errors:\n${errors.join('\n')}`);
  return { engine, steps };
}

(async () => {
  if (!fs.existsSync(path.join(distA, 'sw.js'))) throw new Error(`No web build with sw.js at ${distA}; run ./gradlew :web:wasmJsBrowserDistribution first`);
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'web-pwa-b-'));
  const B = makeBuildB(tmp);
  const state = { root: distA, down: false };
  const server = await serve(state);
  const base = `http://127.0.0.1:${server.address().port}${PREFIX}`;
  const results = []; let failed = false;
  for (const name of engines) {
    state.root = distA; state.down = false;
    try { results.push(await run(name, name === 'webkit' ? webkit : chromium, state, base, distA, B)); }
    catch (e) { failed = true; console.error(e.message || e); results.push({ engine: name, error: String(e.message || e) }); }
  }
  server.closeAllConnections(); server.close();
  fs.rmSync(tmp, { recursive: true, force: true });
  fs.mkdirSync(outDir, { recursive: true });
  fs.writeFileSync(path.join(outDir, 'results.json'), JSON.stringify(results, null, 2) + '\n');
  console.log(JSON.stringify(results, null, 2));
  process.exitCode = failed ? 1 : 0;
})();
