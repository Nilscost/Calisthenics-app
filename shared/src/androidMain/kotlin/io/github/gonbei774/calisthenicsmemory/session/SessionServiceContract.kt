package io.github.gonbei774.calisthenicsmemory.session

/** Intent vocabulary shared by the UI adapter (here) and WorkoutSessionService (in :app). Values are unchanged. */
object SessionServiceContract {
    const val SERVICE_CLASS = "io.github.gonbei774.calisthenicsmemory.session.WorkoutSessionService"
    const val ACTION_START = "wss.START"
    const val ACTION_PAUSE = "wss.PAUSE"
    const val ACTION_RESUME = "wss.RESUME"
    const val ACTION_SKIP = "wss.SKIP"
    const val ACTION_FINISH = "wss.FINISH"
    const val ACTION_DONE = "wss.DONE"
    const val ACTION_LOG = "wss.LOG"
    const val EXTRA_BLOCK_ID = "block_id"
    const val EXTRA_REPS = "reps"      // -1 = not typed
    const val EXTRA_TOO_HARD = "too_hard"
    const val EXTRA_PAIN = "pain"
    const val EXTRA_TOO_EASY = "too_easy"
    const val ACTION_RECOVER = "wss.RECOVER"
    const val EXTRA_RESUME = "resume"
    const val EXTRA_PLAN = "plan_json"
    const val EXTRA_SESSION_ID = "session_id"
    const val EXTRA_SPEAK = "speak"
}
