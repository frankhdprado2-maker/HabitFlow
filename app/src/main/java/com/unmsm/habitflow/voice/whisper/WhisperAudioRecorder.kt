package com.unmsm.habitflow.voice.whisper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface WhisperRecorder {
    val metrics: StateFlow<RecordingMetrics>
    suspend fun start()
    suspend fun stop(): FloatArray
    suspend fun cancel()
}

@Singleton
class WhisperAudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) : WhisperRecorder {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _metrics = MutableStateFlow(RecordingMetrics())
    override val metrics: StateFlow<RecordingMetrics> = _metrics.asStateFlow()
    @Volatile private var recording = false
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var captured = ShortArray(WhisperPcm.MAX_SAMPLES)
    @Volatile private var capturedCount = 0
    @Volatile private var readFailure: WhisperError? = null

    override suspend fun start() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            throw WhisperError(WhisperErrorType.PermissionDenied, "Necesito permiso para usar el micrófono.")
        }
        if (recording) throw WhisperError(WhisperErrorType.MicrophoneBusy, "El micrófono está ocupado.")
        val minimum = AudioRecord.getMinBufferSize(
            WhisperPcm.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minimum <= 0) {
            throw WhisperError(WhisperErrorType.MicrophoneUnavailable, "No se pudo inicializar el micrófono.")
        }
        val recorder = try {
            AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(WhisperPcm.SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build()
                )
                .setBufferSizeInBytes((minimum * 2).coerceAtLeast(WhisperPcm.SAMPLE_RATE))
                .build()
        } catch (error: SecurityException) {
            throw WhisperError(WhisperErrorType.PermissionDenied, "Necesito permiso para usar el micrófono.", error)
        } catch (error: Exception) {
            throw WhisperError(WhisperErrorType.MicrophoneUnavailable, "No se pudo inicializar el micrófono.", error)
        }
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw WhisperError(WhisperErrorType.MicrophoneBusy, "El micrófono está ocupado o no está disponible.")
        }
        captured = ShortArray(WhisperPcm.MAX_SAMPLES)
        capturedCount = 0
        readFailure = null
        _metrics.value = RecordingMetrics()
        audioRecord = recorder
        try {
            recorder.startRecording()
        } catch (error: Exception) {
            recorder.release()
            audioRecord = null
            throw WhisperError(WhisperErrorType.MicrophoneBusy, "El micrófono está ocupado o no está disponible.", error)
        }
        recording = true
        recordingJob = scope.launch { capture(recorder, minimum.coerceAtLeast(1024) / 2) }
    }

    override suspend fun stop(): FloatArray {
        recording = false
        recordingJob?.join()
        recordingJob = null
        readFailure?.let { throw it }
        return WhisperPcm.shortsToFloats(captured, capturedCount)
    }

    override suspend fun cancel() {
        recording = false
        recordingJob?.join()
        recordingJob = null
        captured.fill(0)
        capturedCount = 0
        _metrics.value = RecordingMetrics()
    }

    private fun capture(recorder: AudioRecord, bufferSize: Int) {
        val buffer = ShortArray(bufferSize.coerceAtMost(WhisperPcm.SAMPLE_RATE))
        try {
            while (recording && capturedCount < WhisperPcm.MAX_SAMPLES) {
                val remaining = WhisperPcm.MAX_SAMPLES - capturedCount
                val count = recorder.read(buffer, 0, buffer.size.coerceAtMost(remaining), AudioRecord.READ_BLOCKING)
                if (count > 0) {
                    buffer.copyInto(captured, capturedCount, 0, count)
                    capturedCount += count
                    _metrics.value = RecordingMetrics(
                        durationMillis = capturedCount * 1_000L / WhisperPcm.SAMPLE_RATE,
                        audioLevel = WhisperPcm.audioLevel(buffer, count),
                        reachedLimit = capturedCount >= WhisperPcm.MAX_SAMPLES
                    )
                } else if (count < 0) {
                    readFailure = WhisperError(WhisperErrorType.RecordingInterrupted, "La grabación fue interrumpida.")
                    break
                }
            }
        } finally {
            recording = false
            runCatching { recorder.stop() }
            recorder.release()
            if (audioRecord === recorder) audioRecord = null
        }
    }
}
