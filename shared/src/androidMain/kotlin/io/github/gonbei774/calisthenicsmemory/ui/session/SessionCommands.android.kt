// Android session control: the same explicit Intents to the foreground service as before the move.
package io.github.gonbei774.calisthenicsmemory.ui.session

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import io.github.gonbei774.calisthenicsmemory.session.SessionServiceContract as C

/** The service lives in :app; naming its class avoids a dependency from :shared onto :app. */
private fun Context.serviceIntent(action: String) = Intent().setClassName(packageName, C.SERVICE_CLASS).setAction(action)

internal actual fun platformStartWorkout(ctx: Context, planJson: String, sessionId: String, speak: Boolean) {
    val i = ctx.serviceIntent(C.ACTION_START)
        .putExtra(C.EXTRA_PLAN, planJson).putExtra(C.EXTRA_SESSION_ID, sessionId)
        .putExtra(C.EXTRA_SPEAK, speak)
    ContextCompat.startForegroundService(ctx, i)
}

internal actual fun platformRecoverWorkout(ctx: Context, resume: Boolean) {
    val i = ctx.serviceIntent(C.ACTION_RECOVER).putExtra(C.EXTRA_RESUME, resume)
    ContextCompat.startForegroundService(ctx, i)
}

internal actual fun platformSessionCommand(ctx: Context, command: SessionCommand) {
    ctx.startService(ctx.serviceIntent(when (command) {
        SessionCommand.PAUSE -> C.ACTION_PAUSE
        SessionCommand.RESUME -> C.ACTION_RESUME
        SessionCommand.SKIP -> C.ACTION_SKIP
        SessionCommand.FINISH -> C.ACTION_FINISH
        SessionCommand.DONE -> C.ACTION_DONE
    }))
}

internal actual fun platformLogBlock(ctx: Context, blockId: String, reps: Int?, tooHard: Boolean, pain: Boolean, tooEasy: Boolean) {
    ctx.startService(ctx.serviceIntent(C.ACTION_LOG)
        .putExtra(C.EXTRA_BLOCK_ID, blockId).putExtra(C.EXTRA_REPS, reps ?: -1)
        .putExtra(C.EXTRA_TOO_HARD, tooHard).putExtra(C.EXTRA_PAIN, pain).putExtra(C.EXTRA_TOO_EASY, tooEasy))
}
