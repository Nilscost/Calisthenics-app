package io.github.gonbei774.calisthenicsmemory

import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

/**
 * T03 Compose launch smoke test (instrumented; runs on an authorized device
 * per docs/04-verification.md). Only proves the app process starts and the
 * launcher activity composes — NOT evidence of product functionality.
 */
@RunWith(AndroidJUnit4::class)
class LaunchSmokeTest {

    @Test
    fun appLaunchesWithoutCrash() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        Assert.assertEquals(ctx.packageName, "app.calisthenics.personal")
        // Launch the main activity and ensure the UI thread composes.
        val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
        Assert.assertNotNull("launcher intent present", intent)
        ctx.startActivity(intent)
        // Give the first frame a moment, then pump the main thread.
        Thread.sleep(2000)
        Espresso.onIdle { }
    }
}
