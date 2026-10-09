package app.calisthenics.domain

import app.calisthenics.domain.backup.*
import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.feedback.*
import app.calisthenics.domain.history.BlockRecord
import app.calisthenics.domain.history.SessionRecord
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.*
import app.calisthenics.domain.routine.SessionDraft
import app.calisthenics.domain.routine.StarterRoutine
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * V00b: builds the fixture the debug-only SeedReceiver loads (app/src/debug/assets/seed). Deterministic: the committed
 * files must equal what this generates. Regenerate with `WRITE_SEED=1 ./gradlew :domain:test --tests '*SeedFixtureTest*'`.
 */
class SeedFixtureTest {
    private val root = File(System.getProperty("repo.root"))
    private val dir = File(root, "app/src/debug/assets/seed")
    private val catalog = parseCatalog(File(root, "content/starter/catalog.json").readText())
    private val base = 20_000L // epoch day, only relative times matter (the receiver rebases)

    private fun build(): Pair<String, String> {
        val levels = catalog.onboardingFamilies.associate { it.anchorVariationId to 3 }
        val actions = levels.map { (v, t) -> UserAction.SelfAssessment(0, v, t) }
        val engine = ProgressionEngine(catalog)
        val evidence = mutableListOf<SessionEvidence>()
        val plans = mutableListOf<PlanRecord>(); val sessions = mutableListOf<SessionRecord>(); val feedback = mutableListOf<FeedbackRecord>()
        val planJson = Json { encodeDefaults = true }
        for (i in 0 until 8) {
            val day = (base + i * 3).toInt()
            val at = day * 86_400_000L + 9 * 3_600_000L
            val progress = engine.replay(evidence, actions)
            val draft = SessionDraft.from(Preferences(defaultDurationSeconds = 2700), StarterRoutine.routine).copy(rounds = 3)
            val plan = (generate(PlanInput("seed-plan-$i", at, day, catalog, StarterRoutine.routine, draft, SeedProfiles.home, progress = progress)) as PlanResult.Ready).plan
            val work = plan.blocks.filter { it.type == BlockType.WORK && it.variationId != null }
            val firstVid = work.first().variationId!!
            // Session 4: one exercise logged below target (shows a corrected-looking detail); session 6: typed reps on the first exercise.
            val row = mutableMapOf<String, Feedback>()
            if (i == 4) row[firstVid] = Feedback(Rating.BELOW, actualReps = (work.first().target!!.value - 2).coerceAtLeast(1))
            if (i == 6) row[firstVid] = Feedback(Rating.MET, actualReps = work.first().target!!.value + 1)
            val exec = work.associate { it.id to Execution.COMPLETED }
            val resolved = resolveFeedback(plan, exec, emptyMap(), row)
            evidence += deriveEvidence("seed-session-$i", day, plan, resolved, { catalog.variation(it)?.familyId ?: it },
                { v, tier -> catalog.policyForVariation(v)?.tiers?.firstOrNull { it.index == tier }?.minQualifyingBlocks ?: 2 })
            plans += PlanRecord(plan.id, planJson.encodeToString(WorkoutPlan.serializer(), plan))
            val blocks = plan.blocks.filter { it.type == BlockType.WORK || it.type == BlockType.STRETCH }.map { b ->
                val r = resolved.firstOrNull { it.blockId == b.id }
                val value = if (b.type == BlockType.WORK) (r?.actualReps ?: r?.actualHoldSeconds ?: b.target?.value) else null
                BlockRecord(b.id, b.variationId, b.type.name, "MET", b.durationSeconds, value)
            }
            sessions += SessionRecord("seed-session-$i", plan.id, at, at + plan.plannedDurationSeconds * 1000L, "COMPLETED", blocks)
            for (o in resolved.distinctBy { it.variationId }) {
                val f = row[o.variationId]
                feedback += FeedbackRecord("seed-session-$i", o.variationId, 1, (f?.rating ?: Rating.MET).name, false, f == null, at + plan.plannedDurationSeconds * 1000L, f?.actualReps)
            }
        }
        val payload = BackupPayload(Preferences(), listOf(StarterRoutine.routine), plans, sessions, feedback, emptyList(), SeedProfiles.all)
        val backup = exportBackup(payload, "seed", 0L)
        val levelsJson = Json { prettyPrint = true }.encodeToString(MapSerializer(String.serializer(), Int.serializer()), levels.toSortedMap())
        return backup to levelsJson
    }

    @Test fun committedSeedFixtureIsCurrentAndImports() {
        val (backup, levels) = build()
        val bf = File(dir, "backup.json"); val lf = File(dir, "levels.json")
        if (System.getProperty("write.seed") == "1" || !bf.exists() || !lf.exists()) { dir.mkdirs(); bf.writeText(backup); lf.writeText(levels) }
        assertTrue("fixture missing", bf.exists() && lf.exists())
        assertEquals("seed backup is stale; rerun with WRITE_SEED=1 to regenerate", backup, bf.readText())
        assertEquals(levels, lf.readText())
        val ok = importBackup(bf.readText()) as ImportResult.Ok
        assertEquals(8, ok.payload.sessions.size)
        assertEquals(2, ok.payload.profiles.size)
        val snap = ProgressionEngine(catalog).replay(emptyList(), Json.decodeFromString(MapSerializer(String.serializer(), Int.serializer()), lf.readText()).map { (v, t) -> UserAction.SelfAssessment(0, v, t) })
        assertTrue(snap.variations.isNotEmpty())
    }
}
