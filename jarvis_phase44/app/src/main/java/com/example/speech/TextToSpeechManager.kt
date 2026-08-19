package com.example.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * Robust Text-to-Speech manager supporting Persian (fa-IR) and English (en-US).
 * Uses installed offline voices when available, with automatic fallback to standard language packs.
 */
class TextToSpeechManager(context: Context) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = TextToSpeech(appContext, this)

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            _errorState.value = "موتور TTS اندروید در دسترس نیست."
            return
        }

        val engine = tts ?: return
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { _isSpeaking.value = true }
            override fun onDone(utteranceId: String?) { _isSpeaking.value = false }
            override fun onError(utteranceId: String?) { _isSpeaking.value = false }
        })
        _isInitialized.value = true
        _errorState.value = null
    }

    /** Returns true when a voice or language data for this locale is available. */
    fun hasOfflineVoice(isPersian: Boolean): Boolean {
        val locale = if (isPersian) Locale("fa", "IR") else Locale.US
        val engine = tts ?: return false

        val hasVoice = engine.voices?.any { voice ->
            !voice.isNetworkConnectionRequired &&
                (voice.locale.language == locale.language) &&
                (locale.country.isBlank() || voice.locale.country.isBlank() || voice.locale.country == locale.country)
        } == true

        if (hasVoice) return true

        val res = engine.isLanguageAvailable(locale)
        return res >= TextToSpeech.LANG_AVAILABLE
    }

    fun speak(text: String, isPersian: Boolean = true): Boolean {
        if (tts == null || !_isInitialized.value || text.isBlank()) return false

        val locale = if (isPersian) Locale("fa", "IR") else Locale.US
        val engine = tts ?: return false

        // 1. Try finding dedicated offline voice for this language
        val offlineVoice = engine.voices
            ?.filter { !it.isNetworkConnectionRequired }
            ?.filter { it.locale.language == locale.language }
            ?.sortedByDescending { it.locale.country == locale.country }
            ?.firstOrNull()

        return try {
            if (offlineVoice != null) {
                engine.voice = offlineVoice
            } else {
                val langResult = engine.setLanguage(locale)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    _errorState.value = if (isPersian) {
                        "داده‌های صوتی زبان فارسی روی موتور TTS دستگاه نصب نیست."
                    } else {
                        "داده‌های صوتی زبان انگلیسی روی موتور TTS دستگاه نصب نیست."
                    }
                    return false
                }
            }

            engine.setSpeechRate(0.95f)
            engine.setPitch(1.0f)
            val res = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "JARVIS_${System.currentTimeMillis()}")
            if (res == TextToSpeech.SUCCESS) {
                _errorState.value = null
                true
            } else {
                _errorState.value = "پخش صدا با خطا مواجه شد."
                false
            }
        } catch (e: Exception) {
            _errorState.value = "پخش TTS ناموفق بود: ${e.message ?: "خطای ناشناخته"}"
            false
        }
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _isInitialized.value = false
        _isSpeaking.value = false
    }
}
