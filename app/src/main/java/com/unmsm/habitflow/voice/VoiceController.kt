package com.unmsm.habitflow.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null

    fun startRecording(): Result<File> =
        runCatching {
            stopSilently()
            val directory = File(context.cacheDir, "voice").apply { mkdirs() }
            val file = File.createTempFile("habitflow-voice-", ".m4a", directory)
            val mediaRecorder = createRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16_000)
                setAudioEncodingBitRate(64_000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            currentFile = file
            file
        }

    fun stopRecording(): Result<File> =
        runCatching {
            val file = currentFile ?: error("No hay una grabacion activa.")
            val mediaRecorder = recorder ?: error("No hay una grabacion activa.")
            try {
                mediaRecorder.stop()
            } finally {
                mediaRecorder.release()
                recorder = null
                currentFile = null
            }
            if (!file.exists() || file.length() < 1_000L) {
                file.delete()
                error("No pude capturar audio suficiente.")
            }
            file
        }

    fun cancelRecording() {
        val file = currentFile
        stopSilently()
        file?.delete()
    }

    private fun stopSilently() {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        currentFile = null
    }

    private fun createRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
}
