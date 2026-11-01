package io.github.gonbei774.calisthenicsmemory.ui.session

import app.calisthenics.domain.session.BlockLog
import io.github.gonbei774.calisthenicsmemory.platform.PlatformContext
import io.github.gonbei774.calisthenicsmemory.session.WebSession

// Phase 4a: the web session owner is the shared SessionRunner running in the page (no service, no Intents).
internal actual fun platformStartWorkout(ctx: PlatformContext, planJson: String, sessionId: String, speak: Boolean) =
    WebSession.start(ctx, planJson, sessionId)

internal actual fun platformRecoverWorkout(ctx: PlatformContext, resume: Boolean) = WebSession.recover(ctx, resume)

internal actual fun platformSessionCommand(ctx: PlatformContext, command: SessionCommand) {
    val r = WebSession.runner() ?: return
    when (command) {
        SessionCommand.PAUSE -> r.pause()
        SessionCommand.RESUME -> r.resume()
        SessionCommand.SKIP -> r.skip()
        SessionCommand.FINISH -> r.finishEarly()
        SessionCommand.DONE -> r.earlyDone()
    }
}

internal actual fun platformLogBlock(ctx: PlatformContext, blockId: String, reps: Int?, tooHard: Boolean, pain: Boolean, tooEasy: Boolean) {
    WebSession.runner()?.log(blockId, BlockLog(reps, tooHard, pain, tooEasy))
}
