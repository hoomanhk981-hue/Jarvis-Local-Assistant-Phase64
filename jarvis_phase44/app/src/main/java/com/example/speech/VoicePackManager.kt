package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Device voice-pack manager.
 *
 * Important: Android does not expose a universal public API for Jarvis to
 * silently download an arbitrary SpeechRecognizer language pack. Therefore
 * this manager uses the official system flows: TTS data installation and
 * voice-input settings. Jarvis never downloads a proprietary speech pack
 * from an undocumented endpoint and never turns online recognition on as a
 * hidden fallback.
 */
class VoicePackManager(private val context: Context) {
    data class VoiceStatus(
        val speechRecognizerAvailable: Boolean,
        val offlineSpeechLikelyAvailable: Boolean,
        val offlinePersianTts: Boolean,
        val offlineEnglishTts: Boolean
    )

    fun status(): VoiceStatus {
        val recognizerAvailable = SpeechRecognizer.isRecognitionAvailable(context)
        val offlineTts = runCatching {
            var faTts = false
            var enTts = false
            val tts = TextToSpeech(context) { /* init listener */ }
            faTts = tts.voices?.any { !it.isNetworkConnectionRequired && it.locale.language == "fa" } == true
            enTts = tts.voices?.any { !it.isNetworkConnectionRequired && it.locale.language == "en" } == true
            tts.shutdown()
            VoiceStatus(
                speechRecognizerAvailable = recognizerAvailable,
                offlineSpeechLikelyAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && recognizerAvailable,
                offlinePersianTts = faTts,
                offlineEnglishTts = enTts
            )
        }.getOrNull()
        return offlineTts ?: VoiceStatus(
            speechRecognizerAvailable = recognizerAvailable,
            offlineSpeechLikelyAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && recognizerAvailable,
            offlinePersianTts = false,
            offlineEnglishTts = false
        )
    }

    /** Opens Android's official TTS-data installation flow. */
    fun openTtsInstall(language: String) {
        val locale = if (language == "fa") Locale("fa", "IR") else Locale.US
        val intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).apply {
            putExtra("locale", locale.toLanguageTag())
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startSafely(intent, "com.android.settings.TTS_SETTINGS")
    }

    /**
     * Opens the official voice-input settings. The selected recognizer owns
     * its language-pack download UI; Android has no universal install intent
     * for third-party speech-recognition language models.
     */
    fun openSpeechRecognitionSettings() {
        val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startSafely(intent, Settings.ACTION_SETTINGS)
    }

    private fun startSafely(primary: Intent, fallbackAction: String) {
        try {
            context.startActivity(primary)
        } catch (_: Exception) {
            runCatching { context.startActivity(Intent(fallbackAction).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }
}
