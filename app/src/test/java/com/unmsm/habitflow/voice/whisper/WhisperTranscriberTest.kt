package com.unmsm.habitflow.voice.whisper

import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhisperTranscriberTest {
    @Test fun preparesModelStartsStopsAndReturnsEditableText() = runBlocking {
        val recorder = FakeRecorder()
        val engine = FakeEngine("Hoy tomé dos litros de agua")
        val transcriber = transcriber(recorder, engine)

        assertTrue(transcriber.prepareModel().isSuccess)
        assertEquals(WhisperState.Ready, transcriber.state.value)
        assertTrue(transcriber.startRecording().isSuccess)
        assertTrue(transcriber.state.value is WhisperState.Recording)
        val result = transcriber.stopAndTranscribe()

        assertEquals("Hoy tomé dos litros de agua", result.getOrThrow())
        assertEquals(WhisperState.Result("Hoy tomé dos litros de agua"), transcriber.state.value)
        assertEquals("es", engine.language)
        assertTrue(recorder.stopped)
    }

    @Test fun missingOrCorruptModelBecomesControlledError() = runBlocking {
        val missing = WhisperError(WhisperErrorType.ModelNotFound, "No se encontró el modelo local de voz.")
        val transcriber = WhisperTranscriber(FakeModelProvider(missing), FakeEngine(""), FakeRecorder(), true)
        assertTrue(transcriber.prepareModel().isFailure)
        assertEquals(WhisperErrorType.ModelNotFound, (transcriber.state.value as WhisperState.Error).error.type)
    }

    @Test fun modelIntegrityAcceptsExpectedHashAndRejectsCorruption() {
        val file = File.createTempFile("whisper-model", ".bin")
        try {
            file.writeText("model")
            assertTrue(
                WhisperModelIntegrity.isValid(
                    file,
                    5,
                    "9372c470eeadd5ecd9c3c74c2b3cb633f8e2f2fad799250a0f70d652b6b825e4"
                )
            )
            file.appendText("corrupt")
            assertFalse(WhisperModelIntegrity.isValid(file, 5, "9372c470eeadd5ecd9c3c74c2b3cb633f8e2f2fad799250a0f70d652b6b825e4"))
        } finally {
            file.delete()
        }
    }

    @Test fun cancelReleasesRecorderAndDoesNotTranscribe() = runBlocking {
        val recorder = FakeRecorder()
        val engine = FakeEngine("ignored")
        val transcriber = transcriber(recorder, engine)
        transcriber.prepareModel()
        transcriber.startRecording()
        transcriber.cancel()
        assertTrue(recorder.cancelled)
        assertFalse(engine.transcribed)
        assertEquals(WhisperState.Cancelled, transcriber.state.value)
    }

    @Test fun emptyTranscriptionIsRejected() = runBlocking {
        val transcriber = transcriber(FakeRecorder(), FakeEngine("   "))
        transcriber.prepareModel()
        transcriber.startRecording()
        assertTrue(transcriber.stopAndTranscribe().isFailure)
        assertEquals(WhisperErrorType.EmptyText, (transcriber.state.value as WhisperState.Error).error.type)
    }

    @Test fun preventsSimultaneousInferenceRequests() = runBlocking {
        val transcriber = transcriber(FakeRecorder(), FakeEngine("texto"))
        transcriber.prepareModel()
        transcriber.startRecording()
        val first = async { transcriber.stopAndTranscribe() }
        val second = async { transcriber.stopAndTranscribe() }
        assertEquals(1, listOf(first.await(), second.await()).count { it.isSuccess })
    }

    @Test fun reachingFifteenSecondsStopsAutomatically() = runBlocking {
        val recorder = FakeRecorder()
        val transcriber = transcriber(recorder, FakeEngine("texto automático"))
        transcriber.prepareModel()
        transcriber.startRecording()
        recorder.emitLimit()
        repeat(50) {
            if (transcriber.state.value is WhisperState.Result) return@repeat
            delay(10)
        }
        assertEquals(WhisperState.Result("texto automático"), transcriber.state.value)
        assertTrue(recorder.stopped)
    }

    @Test fun cancelsActiveInference() = runBlocking {
        val recorder = FakeRecorder()
        val engine = BlockingEngine()
        val transcriber = WhisperTranscriber(FakeModelProvider(), engine, recorder, true)
        transcriber.prepareModel()
        transcriber.startRecording()
        val inference = async { transcriber.stopAndTranscribe() }
        repeat(50) {
            if (transcriber.state.value == WhisperState.Processing) return@repeat
            delay(10)
        }
        transcriber.cancel()
        assertTrue(inference.await().isFailure)
        assertTrue(engine.cancelled)
        assertEquals(WhisperState.Cancelled, transcriber.state.value)
    }

    private fun transcriber(recorder: FakeRecorder, engine: FakeEngine) =
        WhisperTranscriber(FakeModelProvider(), engine, recorder, true)
}

private class FakeModelProvider(private val failure: WhisperError? = null) : WhisperModelProvider {
    override suspend fun prepareModel(): File {
        failure?.let { throw it }
        return File("fake-model.bin")
    }
}

private class FakeEngine(private val output: String) : WhisperInferenceEngine {
    var language: String? = null
    var transcribed = false
    override suspend fun initialize(modelPath: String) = Unit
    override suspend fun transcribe(samples: FloatArray, language: String): String {
        transcribed = true
        this.language = language
        return output.cleanWhisperText().ifBlank {
            throw WhisperError(WhisperErrorType.EmptyText, "No se detectó voz con claridad.")
        }
    }
    override fun cancel() = Unit
    override suspend fun release() = Unit
}

private class BlockingEngine : WhisperInferenceEngine {
    private val result = CompletableDeferred<String>()
    var cancelled = false
    override suspend fun initialize(modelPath: String) = Unit
    override suspend fun transcribe(samples: FloatArray, language: String): String = result.await()
    override fun cancel() {
        cancelled = true
        result.completeExceptionally(
            WhisperError(WhisperErrorType.InferenceCancelled, "La transcripción fue cancelada.")
        )
    }
    override suspend fun release() = Unit
}

private class FakeRecorder : WhisperRecorder {
    private val mutableMetrics = MutableStateFlow(RecordingMetrics())
    override val metrics: StateFlow<RecordingMetrics> = mutableMetrics
    var stopped = false
    var cancelled = false
    override suspend fun start() { mutableMetrics.value = RecordingMetrics(500, 0.4f) }
    override suspend fun stop(): FloatArray {
        stopped = true
        return FloatArray(WhisperPcm.MIN_SAMPLES) { 0.2f }
    }
    override suspend fun cancel() { cancelled = true }
    fun emitLimit() {
        mutableMetrics.value = RecordingMetrics(15_000, 0.5f, reachedLimit = true)
    }
}
