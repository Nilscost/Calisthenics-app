package io.github.gonbei774.calisthenicsmemory.session

import app.calisthenics.domain.session.CueKind
import app.calisthenics.domain.session.*
import app.calisthenics.domain.model.BlockType
import app.calisthenics.domain.model.TimelineBlock
import app.calisthenics.domain.model.WorkoutPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CuePlaybackPipelineTest {
    private class FakeSink : CueAudioSink {
        val loaded = mutableListOf<CueSample>()
        val played = mutableListOf<Triple<CueSample, Float, Float>>()
        var playResult = 1
        override fun load(sample: CueSample): Int { loaded += sample; return sample.ordinal + 1 }
        override fun play(sampleId: Int, leftGain: Float, rightGain: Float): Int {
            val sample = CueSample.entries.single { it.ordinal + 1 == sampleId }
            played += Triple(sample, leftGain, rightGain)
            return playResult
        }
    }

    @Test fun countdownIsDeliveredToLoadedWarningSampleAtSavedAppGain() {
        val sink = FakeSink()
        val diagnostics = mutableListOf<CueAudioDiagnostic>()
        val pipeline = CuePlaybackPipeline(sink, diagnostics::add)
        pipeline.loadAll()
        CueSample.entries.forEach { pipeline.onLoadComplete(it.ordinal + 1, 0) }

        assertTrue(pipeline.deliver(CueKind.COUNTDOWN_3, 30))
        assertEquals(listOf(Triple(CueSample.WARNING, .3f, .3f)), sink.played)
        assertEquals(CueAudioDiagnostic.PlayRequested(CueKind.COUNTDOWN_3, CueSample.WARNING, 1, .3f), diagnostics.last())
    }

    @Test fun speechToggleIsNotPartOfBeepDeliveryAndZeroGainDoesMuteIt() {
        val sink = FakeSink()
        val pipeline = CuePlaybackPipeline(sink) { }
        pipeline.loadAll()
        CueSample.entries.forEach { pipeline.onLoadComplete(it.ordinal + 1, 0) }
        assertTrue(pipeline.deliver(CueKind.COUNTDOWN_1, 100))
        assertEquals(1, sink.played.size)
        assertFalse(pipeline.deliver(CueKind.COUNTDOWN_1, 0))
        assertEquals(1, sink.played.size)
        assertFalse(shouldSpeakCue(audioEnabled = false, ttsReady = true, text = "Next", volumePercent = 100))
        assertTrue(pipeline.deliver(CueKind.COUNTDOWN_2, 30))
    }

    @Test fun failedLoadAndPlaybackAreObservableAndDoNotPretendDelivery() {
        val sink = FakeSink()
        sink.playResult = 0
        val diagnostics = mutableListOf<CueAudioDiagnostic>()
        val pipeline = CuePlaybackPipeline(sink, diagnostics::add)
        pipeline.loadAll()
        pipeline.onLoadComplete(CueSample.WARNING.ordinal + 1, -1)
        assertFalse(pipeline.deliver(CueKind.COUNTDOWN_2, 30))
        pipeline.onLoadComplete(CueSample.START.ordinal + 1, 0)
        assertFalse(pipeline.deliver(CueKind.START, 30))
        assertTrue(diagnostics.any { it is CueAudioDiagnostic.LoadFailed && it.sample == CueSample.WARNING })
        assertTrue(diagnostics.any { it is CueAudioDiagnostic.PlayFailed && it.sample == CueSample.START })
    }

    @Test fun configuredGainIsAppliedToStartWarningAndFinish() {
        val sink = FakeSink()
        val pipeline = CuePlaybackPipeline(sink) { }
        pipeline.loadAll()
        CueSample.entries.forEach { pipeline.onLoadComplete(it.ordinal + 1, 0) }
        pipeline.deliver(CueKind.START, 30)
        pipeline.deliver(CueKind.COUNTDOWN_2, 30)
        pipeline.deliver(CueKind.FINISHED, 30)
        assertEquals(listOf(CueSample.START, CueSample.WARNING, CueSample.FINISHED), sink.played.map { it.first })
        assertTrue(sink.played.all { it.second == .3f && it.third == .3f })
    }

    @Test fun reducerEventsReachSinkAcrossPauseResumeAndDelayedBlockTransitionWithoutBackfill() {
        val sink = FakeSink()
        val pipeline = CuePlaybackPipeline(sink) { }
        pipeline.loadAll()
        CueSample.values().forEach { pipeline.onLoadComplete(it.ordinal + 1, 0) }
        val plan = WorkoutPlan(
            id = "p", routineId = "r", routineRevision = 1, catalogVersion = 1, createdAtEpochMs = 0,
            profileId = "home", requestedDurationSeconds = 14, plannedDurationSeconds = 14,
            focus = emptySet(), stretchOn = false, rounds = 1, changesExplained = emptyList(), warnings = emptyList(),
            needsAcceptance = false, usesDraftContent = false,
            blocks = listOf(
                TimelineBlock("rest", BlockType.PASSIVE_RECOVERY, 6),
                TimelineBlock("prep", BlockType.TRANSITION, 8),
            ),
        )
        fun perform(reduced: Reduced) {
            reduced.effects.filterIsInstance<SessionEffect.Cue>().forEach { pipeline.deliver(it.kind, 30) }
        }

        var state = newSession("s", plan)
        var reduced = reduce(state, SessionEvent.Start(0)); state = reduced.state; perform(reduced)
        reduced = reduce(state, SessionEvent.Tick(3_000)); state = reduced.state; perform(reduced)
        reduced = reduce(state, SessionEvent.Pause(3_200)); state = reduced.state; perform(reduced)
        reduced = reduce(state, SessionEvent.Resume(5_000)); state = reduced.state; perform(reduced)
        reduced = reduce(state, SessionEvent.Tick(5_800)); state = reduced.state; perform(reduced)
        reduced = reduce(state, SessionEvent.Tick(7_800)); state = reduced.state; perform(reduced)

        assertEquals(
            listOf(CueSample.START, CueSample.WARNING, CueSample.WARNING, CueSample.START),
            sink.played.map { it.first },
        )

        val recovered = reduce(state, SessionEvent.ProcessRecovered)
        assertTrue(recovered.effects.none { it is SessionEffect.Cue })
        val resumedRecovered = reduce(recovered.state, SessionEvent.RecoveryChoice(true))
        assertTrue(resumedRecovered.effects.none { it is SessionEffect.Cue })
        perform(resumedRecovered)
        assertEquals(4, sink.played.size)
    }
}
