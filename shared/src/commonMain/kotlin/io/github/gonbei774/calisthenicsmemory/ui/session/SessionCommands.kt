// Commands from the UI to the session owner. The owner (Android: the foreground service) holds the timer; the UI only sends commands.
package io.github.gonbei774.calisthenicsmemory.ui.session

import io.github.gonbei774.calisthenicsmemory.platform.PlatformContext
import io.github.gonbei774.calisthenicsmemory.session.SessionBus

enum class SessionCommand { PAUSE, RESUME, SKIP, FINISH, DONE }

fun startWorkout(ctx: PlatformContext, planJson: String, sessionId: String, speak: Boolean) {
    SessionBus.begin() // before the screen changes: no stale end screen, and "Starting…" is legitimate
    platformStartWorkout(ctx, planJson, sessionId, speak)
}

fun recoverWorkout(ctx: PlatformContext, resume: Boolean) {
    SessionBus.begin()
    platformRecoverWorkout(ctx, resume)
}

fun sessionCommand(ctx: PlatformContext, command: SessionCommand) = platformSessionCommand(ctx, command)

/** [reps] null = not typed (reps for rep targets, seconds for holds). */
fun logBlock(ctx: PlatformContext, blockId: String, reps: Int?, tooHard: Boolean, pain: Boolean, tooEasy: Boolean = false) =
    platformLogBlock(ctx, blockId, reps, tooHard, pain, tooEasy)

internal expect fun platformStartWorkout(ctx: PlatformContext, planJson: String, sessionId: String, speak: Boolean)
internal expect fun platformRecoverWorkout(ctx: PlatformContext, resume: Boolean)
internal expect fun platformSessionCommand(ctx: PlatformContext, command: SessionCommand)
internal expect fun platformLogBlock(ctx: PlatformContext, blockId: String, reps: Int?, tooHard: Boolean, pain: Boolean, tooEasy: Boolean)
