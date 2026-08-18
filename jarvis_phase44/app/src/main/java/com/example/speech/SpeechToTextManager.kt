package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Strict on-device speech recognition.
 *
 * This class intentionally does NOT fall back to the network. On Android 12+
 * it uses SpeechRecognizer.createOnDeviceSpeechRecognizer(). On older Android
 * versions Jarvis reports that a local recognizer is unavailable instead of
 * silently sending audio to an online service.
 */
class SpeechToTextManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _speechResult = MutableStateFlow("")
    val speechResult: StateFlow<String> = _speechResult

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState

    private val _isOnDevice = MutableStateFlow(false)
    val isOnDevice: StateFlow<Boolean> = _isOnDevice

    fun startListening(languageCode: String = "fa-IR") {
        _speechResult.value = ""
        _errorState.value = null
        _isOnDevice.value = false

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            _errorState.value = "تشخیص گفتار کاملاً محلی در Android 12 به بالا در این نسخه فعال است."
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _errorState.value = "سرویس تشخیص گفتار روی دستگاه در دسترس نیست."
            return
        }

        stopListening()

        try {
            speechRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context).apply {
                _isOnDevice.value = true
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        _errorState.value = null
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { _isListening.value = false }
                    override fun onError(error: Int) {
                        _isListening.value = false
                        _errorState.value = "تشخیص گفتار محلی با خطا متوقف شد (کد: $error)."
                    }
                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull { it.isNotBlank() }
                            ?.let { _speechResult.value = it }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull { it.isNotBlank() }
                            ?.let { _speechResult.value = it }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                // Defensive flag: the recognizer itself is already on-device.
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: UnsupportedOperationException) {
            _isOnDevice.value = false
            _errorState.value = "تشخیص گفتار آفلاین روی این دستگاه پشتیبانی نمی‌شود."
            stopListening()
        } catch (e: Exception) {
            _isOnDevice.value = false
            _errorState.value = "راه‌اندازی تشخیص گفتار محلی ناموفق بود: ${e.message ?: "خطای ناشناخته"}"
            stopListening()
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }
        speechRecognizer = null
        _isListening.value = false
    }

    fun destroy() = stopListening()
}
