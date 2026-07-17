package com.unmsm.habitflow.voice.whisper

import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class WhisperTranscriber private constructor(
    private val modelManager: WhisperModelProvider,
    private val engine: WhisperInferenceEngine,
    private val recorder: WhisperRecorder
) {
    @Inject constructor(
        modelManager: WhisperModelManager,
        engine: WhisperEngine,
        recorder: WhisperAudioRecorder
    ) : this(modelManager as WhisperModelProvider, engine as WhisperInferenceEngine, recorder as WhisperRecorder)

    internal constructor(
        modelManager: WhisperModelProvider,
        engine: WhisperInferenceEngine,
        recorder: WhisperRecorder,
        testing: Boolean
    ) : this(modelManager, engine, recorder) {
        check(testing)
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val operationMutex = Mutex()
    private val _state = MutableStateFlow<WhisperState>(WhisperState.ModelNotPrepared)
    val state: StateFlow<WhisperState> = _state.asStateFlow()
    private var metricsJob: Job? = null
    private val automaticStopStarted = AtomicBoolean(false)

    suspend fun prepareModel(): Result<Unit> = runCatching {
        if (_state.value == WhisperState.Ready || _state.value is WhisperState.Result) return@runCatching
        _state.value = WhisperState.PreparingModel
        try {
            val model = modelManager.prepareModel()
            engine.initialize(model.absolutePath)
            _state.value = WhisperState.Ready
        } catch (error: Throwable) {
            val typed = error.toWhisperError()
            _state.value = WhisperState.Error(typed)
            throw typed
        }
    }

    suspend fun startRecording(): Result<Unit> = operationMutex.withLock {
        runCatching {
            if (_state.value == WhisperState.PreparingModel || _state.value == WhisperState.Processing) {
                throw WhisperError(WhisperErrorType.NativeFailure, "Whisper todavía está procesando.")
            }
            if (_state.value == WhisperState.ModelNotPrepared || _state.value is WhisperState.Error) {
                prepareModel().getOrThrow()
            }
            recorder.start()
            automaticStopStarted.set(false)
            _state.value = WhisperState.Recording(0, 0f)
            metricsJob?.cancel()
            metricsJob = scope.launch {
                recorder.metrics.collectLatest { metrics ->
                    _state.value = WhisperState.Recording(metrics.durationMillis, metrics.audioLevel)
                    if (metrics.reachedLimit && automaticStopStarted.compareAndSet(false, true)) {
                        launch { stopAndTranscribe() }
                    }
                }
            }
        }.onFailure { _state.value = WhisperState.Error(it.toWhisperError()) }
    }

    suspend fun stopAndTranscribe(): Result<String> = operationMutex.withLock {
        runCatching {
            val current = _state.value
            if (current !is WhisperState.Recording) {
                throw WhisperError(WhisperErrorType.RecordingInterrupted, "La grabación fue interrumpida.")
            }
            metricsJob?.cancel()
            metricsJob = null
            val samples = recorder.stop()
            WhisperPcm.validate(samples)
            _state.value = WhisperState.Processing
            val text = engine.transcribe(samples, "es")
            _state.value = WhisperState.Result(text)
            text
        }.onFailure { error ->
            val typed = error.toWhisperError()
            _state.value = if (typed.type == WhisperErrorType.InferenceCancelled) {
                WhisperState.Cancelled
            } else {
                WhisperState.Error(typed)
            }
        }
    }

    suspend fun cancel() {
        metricsJob?.cancel()
        metricsJob = null
        if (_state.value == WhisperState.Processing) engine.cancel() else recorder.cancel()
        _state.value = WhisperState.Cancelled
    }
}
