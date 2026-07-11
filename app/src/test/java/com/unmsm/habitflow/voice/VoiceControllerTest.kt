package com.unmsm.habitflow.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceControllerTest {
    @Test
    fun startListeningUsesOnDeviceRecognitionAndEmitsPartialAndFinalText() {
        val service = FakeSpeechRecognitionService(VoiceRecognitionMode.OnDevice)
        val controller = VoiceController(
            FakeSpeechRecognitionServiceFactory(
                available = true,
                onDeviceAvailable = true,
                service = service
            )
        )
        val partials = mutableListOf<String>()
        val finals = mutableListOf<String>()
        val errors = mutableListOf<VoiceRecognitionError>()

        val mode = controller.startListening(
            onPartial = partials::add,
            onFinal = finals::add,
            onError = errors::add
        ).getOrThrow()

        assertEquals(VoiceRecognitionMode.OnDevice, mode)
        assertEquals("es-PE", service.configs.single().language)
        assertTrue(service.configs.single().preferOffline)

        service.listener.onReadyForSpeech()
        assertEquals(VoiceRecognitionState.Listening, controller.state.value)

        service.listener.onPartialResults(listOf("", "Hoy tomé"))
        assertEquals(listOf("Hoy tomé"), partials)
        assertEquals(VoiceRecognitionState.PartialResult("Hoy tomé"), controller.state.value)

        service.listener.onResults(listOf("Hoy tomé dos litros de agua"))
        assertEquals(listOf("Hoy tomé dos litros de agua"), finals)
        assertTrue(errors.isEmpty())
        assertEquals(VoiceRecognitionState.Result("Hoy tomé dos litros de agua"), controller.state.value)
    }

    @Test
    fun startListeningFallsBackToSystemRecognizerWhenOnDeviceIsUnavailable() {
        val service = FakeSpeechRecognitionService(VoiceRecognitionMode.System)
        val controller = VoiceController(
            FakeSpeechRecognitionServiceFactory(
                available = true,
                onDeviceAvailable = false,
                service = service
            )
        )

        val mode = controller.startListening(
            onPartial = {},
            onFinal = {},
            onError = {}
        ).getOrThrow()

        assertEquals(VoiceRecognitionMode.System, mode)
        assertEquals(false, service.createdWithOnDevicePreference)
        assertEquals("es-PE", service.configs.single().language)
    }

    @Test
    fun languageUnavailableRetriesSpanishFallbackBeforeReportingError() {
        val service = FakeSpeechRecognitionService(VoiceRecognitionMode.OnDevice)
        val controller = VoiceController(
            FakeSpeechRecognitionServiceFactory(
                available = true,
                onDeviceAvailable = true,
                service = service
            )
        )
        val finals = mutableListOf<String>()
        val errors = mutableListOf<VoiceRecognitionError>()

        controller.startListening(
            onPartial = {},
            onFinal = finals::add,
            onError = errors::add
        ).getOrThrow()

        service.listener.onError(12)

        assertEquals(listOf("es-PE", "es-ES"), service.configs.map { it.language })
        assertTrue(errors.isEmpty())

        service.listener.onResults(listOf("Hoy tomé dos litros de agua"))
        assertEquals(listOf("Hoy tomé dos litros de agua"), finals)
    }

    @Test
    fun onErrorPreservesExactRecognizerErrorCode() {
        val service = FakeSpeechRecognitionService(VoiceRecognitionMode.System)
        val controller = VoiceController(
            FakeSpeechRecognitionServiceFactory(
                available = true,
                onDeviceAvailable = false,
                service = service
            )
        )
        val errors = mutableListOf<VoiceRecognitionError>()

        controller.startListening(
            onPartial = {},
            onFinal = {},
            onError = errors::add
        ).getOrThrow()

        service.listener.onError(5)

        assertEquals(VoiceErrorType.Client, errors.single().type)
        assertEquals(5, errors.single().code)
    }

    private class FakeSpeechRecognitionServiceFactory(
        private val available: Boolean,
        private val onDeviceAvailable: Boolean,
        private val service: FakeSpeechRecognitionService
    ) : SpeechRecognitionServiceFactory {
        override fun isRecognitionAvailable(): Boolean = available
        override fun isOnDeviceRecognitionAvailable(): Boolean = onDeviceAvailable
        override fun isCurrentThreadMain(): Boolean = true

        override fun create(preferOnDevice: Boolean): SpeechRecognitionService {
            service.createdWithOnDevicePreference = preferOnDevice
            return service
        }
    }

    private class FakeSpeechRecognitionService(
        override val mode: VoiceRecognitionMode
    ) : SpeechRecognitionService {
        val configs = mutableListOf<SpeechRecognitionConfig>()
        var createdWithOnDevicePreference: Boolean? = null
        lateinit var listener: SpeechRecognitionCallback
            private set

        override fun startListening(config: SpeechRecognitionConfig, callback: SpeechRecognitionCallback) {
            configs += config
            listener = callback
        }

        override fun stopListening() = Unit
        override fun cancel() = Unit
        override fun destroy() = Unit
    }
}
