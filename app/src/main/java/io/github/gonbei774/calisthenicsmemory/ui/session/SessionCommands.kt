// Commands from the UI to the foreground service. The service owns the timer; the UI only sends intents.
package io.github.gonbei774.calisthenicsmemory.ui.session

import android.content.Context
import android.content.Intent
import io.github.gonbei774.calisthenicsmemory.session.WorkoutSessionService

fun startWorkout(ctx: Context, planJson: String, sessionId: String, speak: Boolean) {
    val i = Intent(ctx, WorkoutSessionService::class.java).setAction(WorkoutSessionService.ACTION_START)
        .putExtra(WorkoutSessionService.EXTRA_PLAN, planJson).putExtra(WorkoutSessionService.EXTRA_SESSION_ID, sessionId)
        .putExtra(WorkoutSessionService.EXTRA_SPEAK, speak)
    androidx.core.content.ContextCompat.startForegroundService(ctx, i)
}

fun recoverWorkout(ctx: Context, resume: Boolean) {
    val i = Intent(ctx, WorkoutSessionService::class.java).setAction(WorkoutSessionService.ACTION_RECOVER)
        .putExtra(WorkoutSessionService.EXTRA_RESUME, resume)
    androidx.core.content.ContextCompat.startForegroundService(ctx, i)
}

fun sessionCommand(ctx: Context, action: String) {
    ctx.startService(Intent(ctx, WorkoutSessionService::class.java).setAction(action))
}

/** [reps] null = not typed (reps for rep targets, seconds for holds). */
fun logBlock(ctx: Context, blockId: String, reps: Int?, tooHard: Boolean, pain: Boolean, tooEasy: Boolean = false) {
    ctx.startService(Intent(ctx, WorkoutSessionService::class.java).setAction(WorkoutSessionService.ACTION_LOG)
        .putExtra(WorkoutSessionService.EXTRA_BLOCK_ID, blockId).putExtra(WorkoutSessionService.EXTRA_REPS, reps ?: -1)
        .putExtra(WorkoutSessionService.EXTRA_TOO_HARD, tooHard).putExtra(WorkoutSessionService.EXTRA_PAIN, pain).putExtra(WorkoutSessionService.EXTRA_TOO_EASY, tooEasy))
}
