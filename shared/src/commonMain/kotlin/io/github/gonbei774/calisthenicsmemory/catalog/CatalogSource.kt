package io.github.gonbei774.calisthenicsmemory.catalog

/**
 * How shared code reads the exercise catalog.
 *
 * The catalog (`catalog.json`, thumbnails and clips) still lives in the Android app's assets during
 * Phase 3a; this interface is the seam shared code will depend on. Android implements it over
 * `AssetManager`; the web build implements it with a bundled resource or `fetch`.
 */
interface CatalogSource {
    /** Raw UTF-8 bytes of `catalog.json`. Suspends because the web build loads it over the network. */
    suspend fun readCatalogJson(): ByteArray
}
