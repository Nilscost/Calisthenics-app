// Service worker of the web app (Phase 5). `sw.js` in the build is generated from this template by the Gradle task
// `generateServiceWorker` (web/build.gradle.kts), which fills in the three placeholders below from the finished build.
//
//  * Shell (html, js, wasm, Compose resources, catalog, licences, icons): precached on install in a cache named after
//    the content hash of those files, served cache-first. A new deploy has a new hash, so it installs a new cache,
//    activates immediately and deletes every other cache of this origin, including Compose Multiplatform's own
//    "compose_web_resources_cache", so nothing of an old build survives (Compose revalidates it itself; checked by the two-build test).
//  * Clips and thumbnails (data/demos, data/thumbs): fetched on first use, then cache-first from a media cache named after
//    the hash of the clip files (replaced as a whole only when a clip changes). <video> sends byte-range requests, and
//    Safari refuses anything but a 206 answer to them, so ranges are served as 206 slices of the cached full file.
'use strict';
const BUILD_ID = '__BUILD_ID__';
const MEDIA_ID = '__MEDIA_ID__';
const SHELL = ['__SHELL_FILES__'];
const SHELL_CACHE = 'cal-shell-' + BUILD_ID;
const MEDIA_CACHE = 'cal-media-' + MEDIA_ID;
const MEDIA_RE = /\/data\/(demos|thumbs)\/[^/]+$/;

self.addEventListener('install', event => {
  event.waitUntil((async () => {
    const cache = await caches.open(SHELL_CACHE);
    // reload: bypass the HTTP cache so a half-updated CDN edge cannot put old bytes under the new name
    await Promise.all(SHELL.map(async f => {
      const r = await fetch(new Request(f, { cache: 'reload' }));
      if (!r.ok) throw new Error('precache ' + f + ' ' + r.status);
      await cache.put(new Request(f), r);
    }));
    await self.skipWaiting();
  })());
});

self.addEventListener('activate', event => {
  event.waitUntil((async () => {
    for (const name of await caches.keys()) {
      if (name !== SHELL_CACHE && name !== MEDIA_CACHE) await caches.delete(name);
    }
    await self.clients.claim();
  })());
});

self.addEventListener('message', event => {
  if (event.data && event.data.type === 'version' && event.ports[0]) event.ports[0].postMessage({ buildId: BUILD_ID, mediaId: MEDIA_ID });
});

self.addEventListener('fetch', event => {
  const req = event.request;
  if (req.method !== 'GET') return;
  const url = new URL(req.url);
  if (url.origin !== self.location.origin) return;
  if (MEDIA_RE.test(url.pathname)) { event.respondWith(media(event, req, url)); return; }
  event.respondWith(shell(req));
});

async function shell(req) {
  const cache = await caches.open(SHELL_CACHE);
  let hit = await cache.match(req, { ignoreSearch: true });
  if (!hit && req.mode === 'navigate') hit = await cache.match('index.html');
  if (hit) return hit;
  return fetch(req);
}

async function media(event, req, url) {
  const cache = await caches.open(MEDIA_CACHE);
  const key = new Request(url.origin + url.pathname);
  let full = await cache.match(key);
  if (!full) {
    const net = await fetch(key); // the whole file (a clip is ~18 KB), never a partial range, so the cache holds complete files
    if (!net.ok || net.status !== 200) return net;
    event.waitUntil(cache.put(key, net.clone())); // answer now, store in the background
    full = net;
  }
  const range = req.headers.get('range');
  if (!range) return full;
  const blob = await full.blob();
  const size = blob.size;
  const m = /^bytes=(\d*)-(\d*)$/.exec(range);
  if (!m || (!m[1] && !m[2])) return new Response(null, { status: 416, headers: { 'Content-Range': 'bytes */' + size } });
  const start = m[1] ? Number(m[1]) : Math.max(0, size - Number(m[2]));
  const end = m[1] && m[2] ? Math.min(Number(m[2]), size - 1) : size - 1;
  if (start >= size || start > end) return new Response(null, { status: 416, headers: { 'Content-Range': 'bytes */' + size } });
  return new Response(blob.slice(start, end + 1), {
    status: 206,
    headers: { 'Content-Type': full.headers.get('Content-Type') || 'video/mp4', 'Content-Length': String(end - start + 1),
      'Content-Range': 'bytes ' + start + '-' + end + '/' + size, 'Accept-Ranges': 'bytes' },
  });
}
