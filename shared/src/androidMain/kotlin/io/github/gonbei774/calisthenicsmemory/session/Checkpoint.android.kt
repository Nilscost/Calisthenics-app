package io.github.gonbei774.calisthenicsmemory.session

import android.content.Context

actual object CheckpointStore {
    private const val PREFS = "workout_checkpoint"; private const val KEY = "json"
    actual fun write(ctx: Context, text: String) { ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, text).commit() }
    actual fun read(ctx: Context): Checkpoint? = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)?.let { CheckpointCodec.decode(it) }
    actual fun clear(ctx: Context) { ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).commit() }
}
