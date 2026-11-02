// Shared console allow-list of the browser tests (unchanged since Phase 3c).
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

module.exports = { ALLOWED_CONSOLE_ERRORS };
