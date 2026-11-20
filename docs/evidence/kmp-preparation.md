# Port preparation probes

These isolated experiments are preparation, not acceptance of a later phase. The initial probes preceded Phase 2 production changes; later preparation is dated below. Temporary probe files are outside the repository.

## KMP build and test tooling

An isolated AGP 9.1.1/Kotlin 2.4.21/KSP 2.3.12/Room 2.8.5 module compiled for Android and Wasm. A clean `connectedAndroidDeviceTest` passed one schema-create/open/read/write test with the canonical `schemas` directory configured through `KotlinMultiplatformAndroidComponentsExtension`, `variant.deviceTests` and `sources.assets.addStaticSourceDirectory`. No copied schema fixture remained. One shared serialization test also actually ran on Android host and Wasm/Node with zero failures/skips, confirming default host linkage to commonTest. This is not the real schema-24 migration test.

Multiplatform Settings 1.3.0 compiled on both targets with strict repository enforcement: Android direct/factory SharedPreferencesSettings and Wasm StorageSettings default constructor. Its primitive per-key writers do not replace the native batched editor needed for existing Android stores.

The Compose production probe initially failed strict repository enforcement because Kotlin's Binaryen downloader adds a project repository. Provisioned Binaryen 130 on PATH with `BinaryenEnvSpec.download=false` passed the production compilation and optimization tasks without relaxing policy. The probe needed a 3 GiB compiler/Gradle heap. A future web CI job must provision and verify the pinned tool, rather than reference a developer cache path.

## Storage contracts

Keep Android database name `bodyweight_trainer_database`, version 24, all 15 Room entities, migrations 9–24 and conflict/transaction rules. The upgrade comparator checks those 15 tables plus `room_master_table`; it excludes SQLite internal tables and `android_metadata`.

Normal workout save aborts and rolls back on duplicate session, block or feedback keys. Restore differs: plans and events are inserted with IGNORE; children are considered only for newly inserted sessions, and duplicate child keys keep the first accepted record. Existing-session incoming children are ignored. Preserve that distinction in the web repository.

A versioned IndexedDB JSON document with a common transaction engine is the proposed web starting point. Transform synchronously inside the request callback of one readwrite transaction and await actual commit; never suspend inside that active transaction. Validate browser durability, abort, concurrent-tab revisions and representative history size. Android continues to use its existing Room transactions.

Preference extraction must preserve named files, typed values, native StringSets, grouped edits, synchronous commits and the foreground service's planner-prefs listener. Prior upgrade evidence covered only six populated files. The Phase 2 fixture must cover the complete store inventory and semantic reads, with established legacy/default migrations tested separately.

## Original APK instrumentation oracle — 2026-10-30

`/tmp/kmp-upgrade-oracle-probe` is a standalone Java instrumentation APK targeting the installed app. It compiles against a compile-only jar containing only `GoalStore.class`; application/domain classes are absent from its DEX. Its signing certificate matches the owner-selected APK. The installed original APK was byte-verified against SHA-256 `e1619feafcb12cafee85828e1196a2cf48f5666c3d885174d9ee9f855096853d` before execution.

The initial test APK shadowed the target's lifecycle dependency and crashed before tests. Excluding that dependency removed the collision; DEX inspection confirmed no lifecycle definitions remained. The same GoalStore test then passed on the original APK and after installing the Phase 1 APK over it (`OK (1 test)` each). It invokes the target's save/load methods, reads the native preference and restores its exact prior key/absence. This proves feasibility for that one binary API; it does not prove upgrade-spanning data preservation or compatibility of other store APIs/codecs. Extend with independently verified class origins and old/new semantic assertions.

## Compose browser probe

`/tmp/kmp-ui-probe` built Compose 1.12.2 with Kotlin 2.4.21 and rendered at 390×844 in Playwright Chromium and WebKit. Native pointer input at semantic button bounds changed the counter in both engines; test tags appear as DOM IDs. Canvas hit testing means ordinary role clicks can target the semantic proxy behind the canvas, so real-flow tests must verify bounds, visibility, occlusion and resulting state.

Unmodified WebKit emitted two invalid-enum errors when Skiko's Emscripten glue queried unmasked renderer tokens without enabling `WEBGL_debug_renderer_info`. A diagnostic global context wrapper worked when the extension was available but was not adopted. A narrower isolated glue guard requests the real extension before those two queries; when unavailable it preserves the prior zero return and Emscripten `GL_INVALID_ENUM`, avoiding the invalid browser call. It changes neither console handling nor browser getParameter.

An initial routed-glue test failed to exercise the patch because webpack had bundled it. A corrected hash-checked patch of the actual served bundle passed four cases: Chromium and WebKit, each with normal capability and test-only simulated unavailable extension. Each verified one patched response, two guard calls, no console/page errors and actual counter interaction. These are prototype results. Native caller behavior, context restoration, a reproducible pre-minification source transform and the complete app remain unverified. No production graphics patch or phone test is claimed.


## Shared UI/resource probe — 2026-11-01

An isolated `/tmp/kmp-shared-probe` uses AGP Android KMP 9.1.1, Kotlin 2.4.21 and CMP 1.12.2 under strict repositories. `compileAndroidMain compileKotlinWasmJs testAndroidHostTest` completed successfully, including generated common Compose string/font resources, an Android actual default environment using LocalContext without an app dependency, and common Font variationSettings matching the existing IBM Plex Sans normal/medium/semibold/bold weight instances. One common host test passed. This establishes API/build compatibility, not rendering parity. The earlier command including a Skiko-dependent Wasm Node test failed because its browser glue cannot load Skiko in Node; UI verification must run in a browser. Domain and data retain their actual passing Node suites.

The source survey covers all 32 original app test files, preserving 290 class/method identities. Existing Context facades, SessionBus/Intent contracts, native resource wording, internal-symbol visibility, asset paths and Java locale date APIs require narrow Android compatibility adapters. Shared screens need typed environment/session interfaces; they must not depend on app service classes. An app-provided controller delegates to the existing bus, avoiding a second state source. Application initialization or direct-screen wrappers must be proven through the original test suite. No Phase 3 production files have been moved; the Phase 2 screenshot gate remains a prerequisite.

The 19 actually imported Material vectors (17 filled and two auto-mirrored arrows) were extracted into a separate package in `/tmp/kmp-common-icons`, retaining upstream headers, paths and autoMirror flags. The exact source was AndroidX `material-icons-core-android:1.7.8` ([source artifact](https://dl.google.com/dl/android/maven2/androidx/compose/material/material-icons-core-android/1.7.8/material-icons-core-android-1.7.8-sources.jar)), SHA-256 `14db888d2f698178799a4c18a58bb134b3e98063c6748c443b339044e0145432`. Android and Wasm compilation passed in the isolated shared probe. Rendering and final third-party notices still require review before adoption; no large extended-icon dependency or production icon replacement has been introduced.


The resource rendering probe then exposed a separate Kotlin 2.4 incompatibility in CMP resources 1.12.2: `WasmByteArrayUtils.isWasmExportsAvailable` sees Kotlin's compatibility proxy and selects deprecated memory access. Both development and optimized production builds emitted a console error; neither passed that strict check. Kotlin documents the migration to `kotlin.wasm.unsafe.wasmMemory` in [2.4.20 changes](https://kotlinlang.org/docs/whatsnew2420.html). The [pinned resource source](https://raw.githubusercontent.com/JetBrains/compose-multiplatform/v1.12.2/components/resources/library/src/wasmJsMain/kotlin/org/jetbrains/compose/resources/WasmByteArrayUtils.kt) already contains a slower `Int8Array.toByteArray` fallback.

A temporary pre-minification loader on the exact generated resource-import function selects that existing fallback, requiring single exact detector/copy function matches and emitting a provenance manifest. A separate Skiko loader requires the exact 0.150.2 source hash and one known guard site before minification, also emitting a manifest. Both remain outside the repository. Unlike the earlier routed bundle experiment, browser tests consume the actual built artifacts without response replacement.

Android compilation and optimized Wasm production build passed. Real Chromium/WebKit, each with normal capability and test-only simulated absent debug-renderer extension, passed exact byte verification for nine fixtures (0 through 1,048,577 bytes; 1,053,698 bytes total), real WebP/font/icon rendering and native pointer increment 0→1, with zero console/page errors. Production byte checks took 114–268 ms locally; this does not predict iPhone performance. Root opened both normal-engine captures. Context-loss recovery, full app behavior, final dependency notices and production integration remain unverified. Resource Cache API update behavior also needs the Phase 5 two-build/offline check; versioned resource URLs may be needed, and actual stale behavior has not been established.
