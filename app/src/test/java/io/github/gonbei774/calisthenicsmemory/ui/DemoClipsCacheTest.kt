package io.github.gonbei774.calisthenicsmemory.ui

import io.github.gonbei774.calisthenicsmemory.ui.screens.DemoClips
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File

/** R7-T07 (B10): the cached clip follows the app version and the asset size; a new version clears the old copies. */
class DemoClipsCacheTest {
    private fun dir() = java.nio.file.Files.createTempDirectory("demos").toFile().also { it.deleteOnExit() }
    private fun get(d: File, v: Long, id: String, bytes: ByteArray) = DemoClips.cachedClip(d, v, id, { bytes.size.toLong() }, { ByteArrayInputStream(bytes) })

    @Test fun theCopyIsMadeOnceAndReused() {
        val d = dir(); val a = get(d, 9, "x", byteArrayOf(1, 2, 3))!!
        a.writeBytes(byteArrayOf(9, 9, 9))                              // a marker: a second call must not copy again
        val b = get(d, 9, "x", byteArrayOf(1, 2, 3))!!
        assertEquals(a, b); assertArrayEquals(byteArrayOf(9, 9, 9), b.readBytes())
    }

    @Test fun aNewVersionClearsTheOldFilesAndCopiesAgain() {
        val d = dir(); val old = get(d, 9, "x", byteArrayOf(1, 2, 3))!!
        File(d, "stale.mp4").writeBytes(byteArrayOf(5))
        val new = get(d, 10, "x", byteArrayOf(7, 7, 7))!!
        assertFalse(old.exists()); assertFalse(File(d, "stale.mp4").exists())
        assertArrayEquals(byteArrayOf(7, 7, 7), new.readBytes())
    }

    @Test fun aChangedAssetSizeGetsANewCopyInTheSameVersion() {
        val d = dir(); val old = get(d, 9, "x", byteArrayOf(1, 2, 3))!!
        val new = get(d, 9, "x", byteArrayOf(4, 5, 6, 7))!!
        assertNotEquals(old, new); assertFalse(old.exists()); assertArrayEquals(byteArrayOf(4, 5, 6, 7), new.readBytes())
    }

    @Test fun aMissingAssetGivesNullAndLeavesNoFile() {
        val d = dir()
        val r = DemoClips.cachedClip(d, 9, "y", { -1L }, { throw java.io.FileNotFoundException() })
        assertNull(r); assertEquals(emptyList<String>(), d.list { _, n -> n.endsWith(".mp4") }!!.toList())
    }
}
