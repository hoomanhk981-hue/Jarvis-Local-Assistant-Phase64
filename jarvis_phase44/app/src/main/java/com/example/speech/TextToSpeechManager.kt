package com.example.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * TTS wrapper that prefers an installed voice which does not require a network
 * connection. If no offline voice exists for the requested language, Jarvis
 * refuses to speak rather than silently switching to an online engine.
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
        val hasAnyOfflineVoice = engine.voices?.any { !it.isNetworkConnectionRequired } == true
        if (!hasAnyOfflineVoice) {
            _errorState.value = "هیچ صدای TTS آفلاینی روی دستگاه نصب نیست."
            return
        }

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { _isSpeaking.value = true }
            override fun onDone(utteranceId: String?) { _isSpeaking.value = false }
            override fun onError(utteranceId: String?) { _isSpeaking.value = false }
        })
        _isInitialized.value = true
        _errorState.value = null
    }

    /** Returns true only when an installed voice for this locale is offline-capable. */
    fun hasOfflineVoice(isPersian: Boolean): Boolean {
        val locale = if (isPersian) Locale("fa", "IR") else Locale.US
        return tts?.voices?.any { voice ->
            !voice.isNetworkConnectionRequired &&
                (voice.locale.language == locale.language) &&
                (locale.country.isBlank() || voice.locale.country.isBlank() || voice.locale.country == locale.country)
        } == true
    }

    fun speak(text: String, isPersian: Boolean = true): Boolean {
        if (tts == null || !_isInitialized.value || text.isBlank()) return false

        val locale = if (isPersian) Locale("fa", "IR") else Locale.US
        val offlineVoice = tts?.voices
            ?.filter { !it.isNetworkConnectionRequired }
            ?.filter { it.locale.language == locale.language }
            ?.sortedByDescending { it.locale.country == locale.country }
            ?.firstOrNull()

        if (offlineVoice == null) {
            _errorState.value = if (isPersian) {
                "صدای فارسی آفلاین روی دستگاه نصب نیست."
            } else {
                "صدای انگلیسی آفلاین روی دستگاه نصب نیست."
            }
            return false
        }

        return try {
            tts?.apply {
                setVoice(offlineVoice)
                setSpeechRate(0.95f)
                setPitch(1.0f)
                speak(text, TextToSpeech.QUEUE_FLUSH, null, "JARVIS_${System.currentTimeMillis()}")
            }
            _errorState.value = null
            true
        } catch (e: Exception) {
            _errorState.value = "پخش TTS محلی ناموفق بود: ${e.message ?: "خطای ناشناخته"}"
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
