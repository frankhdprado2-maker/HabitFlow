package com.unmsm.habitflow.voice.whisper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhisperPcmTest {
    @Test fun convertsSignedPcmToNormalizedFloatsAndHonorsPartialBuffers() {
        val result = WhisperPcm.shortsToFloats(shortArrayOf(Short.MIN_VALUE, 0, Short.MAX_VALUE, 123), 3)
        assertEquals(-1f, result[0], 0f)
        assertEquals(0f, result[1], 0f)
        assertEquals(1f, result[2], 0f)
        assertEquals(3, result.size)
    }

    @Test fun rejectsEmptyAndSilentAudio() {
        val error = runCatching { WhisperPcm.validate(FloatArray(WhisperPcm.MIN_SAMPLES)) }.exceptionOrNull()
        assertEquals(WhisperErrorType.EmptyRecording, (error as WhisperError).type)
    }

    @Test fun rejectsTooShortAudio() {
        val samples = FloatArray(WhisperPcm.MIN_SAMPLES - 1) { 0.2f }
        val error = runCatching { WhisperPcm.validate(samples) }.exceptionOrNull()
        assertEquals(WhisperErrorType.RecordingTooShort, (error as WhisperError).type)
    }

    @Test fun enforcesFifteenSecondLimit() {
        assertEquals(240_000, WhisperPcm.MAX_SAMPLES)
        val error = runCatching { WhisperPcm.validate(FloatArray(WhisperPcm.MAX_SAMPLES + 1) { 0.2f }) }.exceptionOrNull()
        assertEquals(WhisperErrorType.RecordingTooLong, (error as WhisperError).type)
    }

    @Test fun cleansOnlyTechnicalMarkersAndWhitespace() {
        assertEquals("Hoy tomé dos litros de agua", "  [MÚSICA]  Hoy   tomé dos litros de agua <|endoftext|> ".cleanWhisperText())
        assertTrue(WhisperPcm.audioLevel(shortArrayOf(0, Short.MAX_VALUE), 2) > 0f)
    }
}
