package com.unmsm.habitflow.voice.whisper

import android.content.Context
import com.unmsm.habitflow.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal interface WhisperModelProvider {
    suspend fun prepareModel(): File
}

@Singleton
class WhisperModelManager @Inject constructor(
    @ApplicationContext private val context: Context
) : WhisperModelProvider {
    override suspend fun prepareModel(): File = withContext(Dispatchers.IO) {
        val directory = File(context.filesDir, "whisper-models").apply { mkdirs() }
        val destination = File(directory, BuildConfig.WHISPER_MODEL_FILE)
        if (WhisperModelIntegrity.isValid(destination, BuildConfig.WHISPER_MODEL_SIZE, BuildConfig.WHISPER_MODEL_SHA256)) {
            return@withContext destination
        }
        destination.delete()
        val temporary = File(directory, "${BuildConfig.WHISPER_MODEL_FILE}.part").also { it.delete() }
        try {
            context.assets.open("whisper/${BuildConfig.WHISPER_MODEL_FILE}").use { input ->
                temporary.outputStream().buffered().use(input::copyTo)
            }
        } catch (error: Exception) {
            temporary.delete()
            throw WhisperError(WhisperErrorType.ModelNotFound, "No se encontró el modelo local de voz.", error)
        }
        if (!WhisperModelIntegrity.isValid(temporary, BuildConfig.WHISPER_MODEL_SIZE, BuildConfig.WHISPER_MODEL_SHA256)) {
            temporary.delete()
            throw WhisperError(WhisperErrorType.ModelCorrupt, "El modelo local está dañado. Vuelve a prepararlo.")
        }
        if (!temporary.renameTo(destination)) {
            temporary.delete()
            throw WhisperError(WhisperErrorType.ModelCorrupt, "El modelo local está dañado. Vuelve a prepararlo.")
        }
        destination
    }

}

internal object WhisperModelIntegrity {
    fun isValid(file: File, expectedSize: Long, expectedSha256: String): Boolean =
        file.isFile && file.length() == expectedSize && file.sha256() == expectedSha256

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
