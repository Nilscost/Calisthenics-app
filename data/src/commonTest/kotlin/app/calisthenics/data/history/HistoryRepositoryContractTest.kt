package app.calisthenics.data.history

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Shared contract usable with in-memory and platform repositories. */
suspend fun exerciseHistoryContract(repo: HistoryRepository) {
    val plan = PlanRecord("p", 1, "r", 1, 1, "home", "{}")
    val older = SessionRow("old", "p", 10, 20, "COMPLETED")
    val newer = SessionRow("new", "p", 30, 40, "COMPLETED")
    val block = BlockRow("old", "b", "MET", 30, 8)
    repo.saveFinishedSession(plan, older, listOf(block), emptyList())
    repo.saveFinishedSession(plan, newer, emptyList(), emptyList())
    assertEquals(listOf(newer, older), repo.sessions())
    assertEquals(plan, repo.plan("p")); assertEquals(listOf(block), repo.blockResults("old"))
    assertFailsWith<Exception> { repo.saveFinishedSession(plan, older, listOf(block), emptyList()) }
    assertEquals(listOf(newer, older), repo.sessions())
    val invalidPlan = plan.copy(planId = "atomic-plan")
    val invalidSession = SessionRow("atomic", "atomic-plan", 50, 60, "COMPLETED")
    assertFailsWith<Exception> {
        repo.saveFinishedSession(invalidPlan, invalidSession, listOf(BlockRow("missing-session", "b", "MET", 5, 2)), emptyList())
    }
    assertEquals(null, repo.plan("atomic-plan")); assertEquals(listOf(newer, older), repo.sessions())
    assertEquals(1, repo.reviseFeedback("old", "v", "MET", false, true, 50, 8, null, null))
    assertEquals(3, repo.reviseRounds("old", "v", listOf(RoundRevision("b1", 6, "BELOW", false), RoundRevision("b2", 35, "MET", true, true)), 60))
    val history = repo.feedbackHistory("old", "v")
    assertEquals(listOf(1, 2, 3), history.map { it.revision })
    assertEquals(true, history[0].assumedMet); assertEquals(8, history[0].actualReps)
    assertEquals(6, history[1].actualReps); assertEquals(null, history[1].actualHoldSeconds)
    assertEquals(null, history[2].actualReps); assertEquals(35, history[2].actualHoldSeconds)
    assertEquals(3, repo.latestRevision("old", "v"))
    val existing = repo.feedbackForSession("old")
    assertEquals(0, repo.restoreMerge(listOf(plan), listOf(older), listOf(block.copy(outcome = "SKIPPED")), existing, emptyList()))
    assertEquals(listOf(block), repo.blockResults("old")); assertEquals(existing, repo.feedbackForSession("old"))
}

class HistoryRepositoryContractTest {
    @Test fun inMemoryImplementsHistoryContract() = runTest { exerciseHistoryContract(InMemoryHistoryRepository()) }
}
