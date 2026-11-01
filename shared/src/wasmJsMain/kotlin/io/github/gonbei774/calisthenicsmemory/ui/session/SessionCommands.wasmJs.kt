package io.github.gonbei774.calisthenicsmemory.ui.session

import io.github.gonbei774.calisthenicsmemory.platform.PlatformContext

// Compile-only for Phase 3b: the web session owner (timer in common code + wake lock) arrives in Phase 4.
internal actual fun platformStartWorkout(ctx: PlatformContext, planJson: String, sessionId: String, speak: Boolean) = Unit
internal actual fun platformRecoverWorkout(ctx: PlatformContext, resume: Boolean) = Unit
internal actual fun platformSessionCommand(ctx: PlatformContext, command: SessionCommand) = Unit
internal actual fun platformLogBlock(ctx: PlatformContext, blockId: String, reps: Int?, tooHard: Boolean, pain: Boolean, tooEasy: Boolean) = Unit
