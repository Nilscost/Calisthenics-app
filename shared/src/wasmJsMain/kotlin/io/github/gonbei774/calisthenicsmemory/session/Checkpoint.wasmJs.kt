package io.github.gonbei774.calisthenicsmemory.session

import app.calisthenics.data.settings.NamedSettings
import io.github.gonbei774.calisthenicsmemory.platform.PlatformContext
import io.github.gonbei774.calisthenicsmemory.platform.settingsProvider

/** Same logical file and key as Android, over the web settings store. */
actual object CheckpointStore {
    private fun file(ctx: PlatformContext): NamedSettings = ctx.settingsProvider().open("workout_checkpoint")
    actual fun write(ctx: PlatformContext, text: String) = file(ctx).edit(commit = true) { putString("json", text) }
    actual fun read(ctx: PlatformContext): Checkpoint? = file(ctx).stringOrNull("json")?.let { CheckpointCodec.decode(it) }
    actual fun clear(ctx: PlatformContext) = file(ctx).edit(commit = true) { remove("json") }
}
