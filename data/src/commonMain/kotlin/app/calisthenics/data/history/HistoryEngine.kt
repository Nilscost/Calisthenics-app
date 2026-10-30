package app.calisthenics.data.history

/** Pure transaction transformations for the browser store. An exception aborts the transaction. */
object HistoryEngine {
    fun save(state: HistoryState, plan: PlanRecord, session: SessionRow, blocks: List<BlockRow>, feedback: List<FeedbackRow>): HistoryState {
        val plans = if (state.plans.any { it.planId == plan.planId }) state.plans else state.plans + plan
        require(state.sessions.none { it.sessionId == session.sessionId }) { "duplicate session" }
        val sessions = state.sessions + session
        require(sessions.any { it.planId == session.planId } && plans.any { it.planId == session.planId }) { "missing plan" }
        val allBlocks = state.blocks.toMutableList()
        for (row in blocks) {
            require(sessions.any { it.sessionId == row.sessionId }) { "missing session" }
            require(allBlocks.none { it.sessionId == row.sessionId && it.blockId == row.blockId }) { "duplicate block" }
            allBlocks += row
        }
        val allFeedback = state.feedback.toMutableList()
        for (row in feedback) {
            require(sessions.any { it.sessionId == row.sessionId }) { "missing session" }
            require(allFeedback.none { it.sessionId == row.sessionId && it.variationId == row.variationId && it.revision == row.revision }) { "duplicate feedback" }
            allFeedback += row
        }
        return state.copy(plans = plans, sessions = sessions, blocks = allBlocks, feedback = allFeedback)
    }

    fun restore(state: HistoryState, plans: List<PlanRecord>, sessions: List<SessionRow>, blocks: List<BlockRow>, feedback: List<FeedbackRow>, events: List<EventRow>): Pair<HistoryState, Int> {
        val outPlans = state.plans.toMutableList()
        plans.forEach { p -> if (outPlans.none { it.planId == p.planId }) outPlans += p }
        val outSessions = state.sessions.toMutableList()
        val fresh = mutableSetOf<String>()
        sessions.forEach { s -> if (outSessions.none { it.sessionId == s.sessionId }) {
            require(outPlans.any { it.planId == s.planId }) { "missing plan" }
            outSessions += s; fresh += s.sessionId
        } }
        val outBlocks = state.blocks.toMutableList()
        blocks.filter { it.sessionId in fresh }.forEach { b -> if (outBlocks.none { it.sessionId == b.sessionId && it.blockId == b.blockId }) outBlocks += b }
        val outFeedback = state.feedback.toMutableList()
        feedback.filter { it.sessionId in fresh }.forEach { f -> if (outFeedback.none { it.sessionId == f.sessionId && it.variationId == f.variationId && it.revision == f.revision }) outFeedback += f }
        val outEvents = state.events.toMutableList()
        events.forEach { e -> if (outEvents.none { it.eventId == e.eventId }) outEvents += e }
        return state.copy(plans = outPlans, sessions = outSessions, blocks = outBlocks, feedback = outFeedback, events = outEvents) to fresh.size
    }

    fun revise(state: HistoryState, sessionId: String, variationId: String, rows: List<RoundRevision>, now: Long): Pair<HistoryState, Int> {
        var revision = state.feedback.filter { it.sessionId == sessionId && it.variationId == variationId }.maxOfOrNull { it.revision } ?: 0
        val out = state.feedback.toMutableList()
        for (row in rows) {
            require(state.sessions.any { it.sessionId == sessionId }) { "missing session" }
            revision += 1
            require(out.none { it.sessionId == sessionId && it.variationId == variationId && it.revision == revision }) { "duplicate feedback" }
            out += FeedbackRow(sessionId, variationId, revision, row.rating, row.discomfort, false, now, row.reps.takeUnless { row.isHold }, row.blockId, row.reps.takeIf { row.isHold })
        }
        return state.copy(feedback = out) to revision
    }
}

/** Repository shared by in-memory tests and the transactional browser persistence adapter. */
abstract class StateHistoryRepository : HistoryRepository {
    protected abstract suspend fun readState(): HistoryState
    protected abstract suspend fun <T> updateState(transform: (HistoryState) -> Pair<HistoryState, T>): T
    override suspend fun sessions() = readState().sessions.sortedByDescending { it.startedAtEpochMs }
    override suspend fun plan(id: String) = readState().plans.firstOrNull { it.planId == id }
    override suspend fun blockResults(id: String) = readState().blocks.filter { it.sessionId == id }
    override suspend fun feedbackHistory(id: String, variationId: String) = readState().feedback.filter { it.sessionId == id && it.variationId == variationId }.sortedBy { it.revision }
    override suspend fun feedbackForSession(id: String) = readState().feedback.filter { it.sessionId == id }.sortedWith(compareBy({ it.variationId }, { it.revision }))
    override suspend fun latestRevision(id: String, variationId: String) = feedbackHistory(id, variationId).maxOfOrNull { it.revision } ?: 0
    override suspend fun events(variationId: String) = readState().events.filter { it.variationId == variationId }.sortedBy { it.atEpochMs }
    override suspend fun allFeedback() = readState().feedback.sortedWith(compareBy({ it.sessionId }, { it.variationId }, { it.revision }))
    override suspend fun allEvents() = readState().events.sortedBy { it.atEpochMs }
    override suspend fun saveFinishedSession(plan: PlanRecord, session: SessionRow, blocks: List<BlockRow>, feedback: List<FeedbackRow>) = updateState { HistoryEngine.save(it, plan, session, blocks, feedback) to Unit }
    override suspend fun restoreMerge(plans: List<PlanRecord>, sessions: List<SessionRow>, blocks: List<BlockRow>, feedback: List<FeedbackRow>, events: List<EventRow>) = updateState { HistoryEngine.restore(it, plans, sessions, blocks, feedback, events) }
    override suspend fun reviseRounds(sessionId: String, variationId: String, rounds: List<RoundRevision>, now: Long) = updateState { HistoryEngine.revise(it, sessionId, variationId, rounds, now) }
    override suspend fun reviseFeedback(sessionId: String, variationId: String, rating: String, discomfort: Boolean, assumedMet: Boolean, now: Long, actualReps: Int?, blockId: String?, actualHoldSeconds: Int?) = updateState { state ->
        require(state.sessions.any { it.sessionId == sessionId }) { "missing session" }
        val next = (state.feedback.filter { it.sessionId == sessionId && it.variationId == variationId }.maxOfOrNull { it.revision } ?: 0) + 1
        require(state.feedback.none { it.sessionId == sessionId && it.variationId == variationId && it.revision == next }) { "duplicate feedback" }
        state.copy(feedback = state.feedback + FeedbackRow(sessionId, variationId, next, rating, discomfort, assumedMet, now, actualReps, blockId, actualHoldSeconds)) to next
    }
}

class InMemoryHistoryRepository(initial: HistoryState = HistoryState()) : StateHistoryRepository() {
    private var state = initial
    private val lock = kotlinx.coroutines.sync.Mutex()
    override suspend fun readState(): HistoryState { lock.lock(); try { return state } finally { lock.unlock() } }
    override suspend fun <T> updateState(transform: (HistoryState) -> Pair<HistoryState, T>): T {
        lock.lock()
        try { val (next, result) = transform(state); state = next; return result } finally { lock.unlock() }
    }
}
