package com.unmsm.habitflow.voice.whisper

object WhisperPcm {
    const val SAMPLE_RATE = 16_000
    const val MAX_DURATION_SECONDS = 15
    const val MAX_SAMPLES = SAMPLE_RATE * MAX_DURATION_SECONDS
    const val MIN_SAMPLES = SAMPLE_RATE / 3

    fun shortsToFloats(samples: ShortArray, count: Int = samples.size): FloatArray {
        val safeCount = count.coerceIn(0, samples.size)
        return FloatArray(safeCount) { index ->
            val sample = samples[index]
            if (sample == Short.MIN_VALUE) -1f else sample.toFloat() / Short.MAX_VALUE.toFloat()
        }
    }

    fun audioLevel(samples: ShortArray, count: Int): Float {
        if (count <= 0) return 0f
        var sum = 0.0
        repeat(count.coerceAtMost(samples.size)) { index ->
            val normalized = if (samples[index] == Short.MIN_VALUE) -1.0 else samples[index] / 32767.0
            sum += normalized * normalized
        }
        return kotlin.math.sqrt(sum / count.coerceAtMost(samples.size)).toFloat().coerceIn(0f, 1f)
    }

    fun validate(samples: FloatArray) {
        if (samples.isEmpty() || samples.all { kotlin.math.abs(it) < 0.0005f }) {
            throw WhisperError(WhisperErrorType.EmptyRecording, "No se detectó voz con claridad.")
        }
        if (samples.size < MIN_SAMPLES) {
            throw WhisperError(WhisperErrorType.RecordingTooShort, "La grabación fue demasiado corta.")
        }
        if (samples.size > MAX_SAMPLES) {
            throw WhisperError(WhisperErrorType.RecordingTooLong, "La grabación superó el límite de 15 segundos.")
        }
    }
}
