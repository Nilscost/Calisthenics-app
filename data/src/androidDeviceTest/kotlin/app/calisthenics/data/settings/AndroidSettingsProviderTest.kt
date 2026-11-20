package app.calisthenics.data.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidSettingsProviderTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "kmp_settings_provider_test"
    private val native get() = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    private val settings get() = AndroidSettingsProvider(context).open(name)

    @After fun cleanup() { native.edit().clear().commit() }

    @Test fun existingNativeTypesAndStringSetsRoundTripWithoutConversion() {
        assertTrue(native.edit().putString("text", "hello").putInt("int", 7).putLong("long", 9L)
            .putBoolean("bool", true).putStringSet("set", setOf("a", "b")).commit())
        assertEquals("hello", settings.stringOrNull("text")); assertEquals(7, settings.intOrNull("int"))
        assertEquals(9L, settings.longOrNull("long")); assertEquals(true, settings.booleanOrNull("bool"))
        assertEquals(setOf("a", "b"), settings.stringSetOrNull("set"))
        settings.edit(commit = true) { putStringSet("set", setOf("b", "c")); putInt("int", 8); remove("text") }
        assertEquals(setOf("b", "c"), native.getStringSet("set", null)); assertEquals(8, native.getInt("int", 0))
        assertFalse(native.contains("text")); assertTrue(native.all["set"] is Set<*>)
    }

    @Test fun failedGroupedEditDoesNotCommitPartialChanges() {
        assertTrue(native.edit().putString("first", "old").putInt("second", 1).commit())
        try {
            settings.edit(commit = true) { putString("first", "new"); putInt("second", 2); error("abort") }
            fail("expected exception")
        } catch (expected: IllegalStateException) { assertEquals("abort", expected.message) }
        assertEquals("old", native.getString("first", null)); assertEquals(1, native.getInt("second", 0))
    }
}
