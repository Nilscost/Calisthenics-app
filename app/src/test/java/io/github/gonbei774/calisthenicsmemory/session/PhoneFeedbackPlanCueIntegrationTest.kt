package io.github.gonbei774.calisthenicsmemory.session

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.calisthenics.domain.content.parseCatalog
import app.calisthenics.domain.equipment.SeedProfiles
import app.calisthenics.domain.model.*
import app.calisthenics.domain.planner.*
import app.calisthenics.domain.progression.ProgressSnapshot
import app.calisthenics.domain.routine.StarterRoutine
import app.calisthenics.domain.session.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Actual generated timelines exercise reducer -> cue sink and visible-window policy together. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PhoneFeedbackPlanCueIntegrationTest {
    @Test fun allModesAndFormatsDeliverCountdownsForRealBlocksAndKeepEveryActivePhaseAwake() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val catalog = ctx.assets.open("catalog.json").bufferedReader().use { parseCatalog(it.readText()) }
        val empty = ProgressSnapshot(emptyMap(), emptyMap(), emptyMap(), emptyList())
        val countdown = setOf(CueKind.COUNTDOWN_3, CueKind.COUNTDOWN_2, CueKind.COUNTDOWN_1)
        for (mode in BetweenSetMode.entries) for (format in WorkoutFormat.entries) {
            val plan = (buildTrainPlan(catalog, StarterRoutine.routine, empty, SeedProfiles.home,
                TrainSettings(rounds = 2, exercisesPerRound = 3, format = format,
                    betweenSetMode = mode, preparationSeconds = 8, warmupOn = true, cooldownOn = true)) as PlanResult.Ready).plan
            val workIndices = plan.blocks.indices.filter { plan.blocks[it].type == BlockType.WORK }
            val between = plan.blocks.subList(workIndices.first(), workIndices.last() + 1)
            when (mode) {
                BetweenSetMode.FLOW -> assertFalse(between.any { it.type in setOf(BlockType.STRETCH, BlockType.PASSIVE_RECOVERY) })
                BetweenSetMode.REST -> assertTrue(between.any { it.type == BlockType.PASSIVE_RECOVERY })
                BetweenSetMode.STRETCH -> assertTrue(between.any { it.type == BlockType.STRETCH })
            }
            assertEquals(plan.blocks.sumOf { it.durationSeconds }, plan.plannedDurationSeconds)
            var played = 0
            val sink = object : CueAudioSink {
                override fun load(sample: CueSample) = sample.ordinal + 1
                override fun play(sampleId: Int, leftGain: Float, rightGain: Float): Int {
                    assertEquals(.3f, leftGain); assertEquals(.3f, rightGain)
                    played++
                    return played
                }
            }
            val pipeline = CuePlaybackPipeline(sink)
            pipeline.loadAll()
            CueSample.entries.forEach { pipeline.onLoadComplete(it.ordinal + 1, 0) }
            val seen = mutableListOf<SessionEffect.Cue>()
            fun effects(r: Reduced) {
                for (cue in r.effects.filterIsInstance<SessionEffect.Cue>()) {
                    seen += cue
                    if (cue.kind != CueKind.PREVIEW_NEXT) assertTrue(pipeline.deliver(cue.kind, 30))
                }
            }
            var r = reduce(newSession("$mode-$format", plan), SessionEvent.Start(0))
            effects(r)
            for (second in 1..plan.plannedDurationSeconds) {
                assertTrue(shouldKeepSessionScreenOn(r.state, true))
                assertFalse(shouldKeepSessionScreenOn(r.state, false))
                r = reduce(r.state, SessionEvent.Tick(second * 1000L))
                effects(r)
            }
            assertTrue(r.state.isTerminal)
            assertFalse(shouldKeepSessionScreenOn(r.state, true))
            val warnings = seen.filter { it.kind in countdown }.groupBy { it.blockId }
            plan.blocks.forEachIndexed { index, block ->
                val expected = if (index < plan.blocks.lastIndex && block.durationSeconds > 3) countdown else emptySet()
                assertEquals("$mode $format ${block.id}", expected, warnings[block.id].orEmpty().map { it.kind }.toSet())
            }
            assertEquals(seen.size, seen.map { it.id }.distinct().size)
            assertEquals(seen.count { it.kind != CueKind.PREVIEW_NEXT }, played)
        }
    }
}
