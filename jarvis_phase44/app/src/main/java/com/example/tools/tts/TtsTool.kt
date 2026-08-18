package com.example.tools.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class TtsTool(
    context: Context,
    private val preferredLocale: Locale = Locale("fa", "IR")
) {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var initialized = false

    suspend fun initialize(): Result<Unit> =
        suspendCancellableCoroutine { continuation ->
            lateinit var engine: TextToSpeech
            engine = TextToSpeech(appContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts = engine
                    val languageStatus = engine.setLanguage(preferredLocale)
                    initialized =
                        languageStatus != TextToSpeech.LANG_MISSING_DATA &&
                        languageStatus != TextToSpeech.LANG_NOT_SUPPORTED

                    if (initialized) {
                        continuation.resume(Result.success(Unit))
                    } else {
                        continuation.resume(
                            Result.failure(
                                IllegalStateException(
                                    "Persian TTS voice/data is not installed or supported."
                                )
                            )
                        )
                    }
                } else {
                    continuation.resume(
                        Result.failure(
                            IllegalStateException("Android TTS engine initialization failed.")
                        )
                    )
                }
            }
        }

    fun isReady(): Boolean = initialized && tts != null

    fun availablePersianVoices(): List<String> =
        tts?.voices
            ?.filter { it.locale.language == preferredLocale.language }
            ?.map { it.name }
            ?.distinct()
            ?.sorted()
            ?: emptyList()

    fun speak(text: String): Result<Unit> {
        if (!isReady()) return Result.failure(
            IllegalStateException("TTS is not initialized.")
        )
        if (text.isBlank()) return Result.failure(
            IllegalArgumentException("TTS text is empty.")
        )

        val result = tts!!.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "jarvis-${UUID.randomUUID()}"
        )

        return if (result == TextToSpeech.SUCCESS) Result.success(Unit)
        else Result.failure(IllegalStateException("TTS synthesis failed."))
    }

    fun stop(): Result<Unit> = try {
        tts?.stop()
        Result.success(Unit)
    } catch (t: Throwable) {
        Result.failure(t)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        initialized = false
    }
}
