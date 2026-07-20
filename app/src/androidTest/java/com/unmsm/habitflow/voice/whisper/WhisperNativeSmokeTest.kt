package com.unmsm.habitflow.voice.whisper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WhisperNativeSmokeTest {
    @Test
    fun loadsNativeLibraryCreatesAndReleasesContext() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val model = WhisperModelManager(context).prepareModel()
        val engine = WhisperEngine()
        engine.initialize(model.absolutePath)
        engine.release()
        engine.release()
        assertTrue(model.isFile && model.length() > 0)
    }
}
