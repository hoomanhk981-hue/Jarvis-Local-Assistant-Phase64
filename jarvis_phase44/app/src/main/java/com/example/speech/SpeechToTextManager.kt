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
 * Speech recognition manager supporting both on-device recognition and standard offline preferences.
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

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _errorState.value = "سرویس تشخیص گفتار روی دستگاه در دسترس نیست."
            return
        }

        stopListening()

        try {
            val listener = object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                    _errorState.value = null
                }

                override fun onBeginningOfSpeech() {
                    _isListening.value = true
                }

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _isListening.value = false
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "خطای ضبط صدا از میکروفون"
                        SpeechRecognizer.ERROR_CLIENT -> "خطای کلاینت تشخیص گفتار"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "مجوز دسترسی به میکروفون (RECORD_AUDIO) داده نشده است"
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "خطای ارتباط شبکه در تشخیص گفتار"
                        SpeechRecognizer.ERROR_NO_MATCH -> "صدایی با مفهوم مشخص تشخیص داده نشد"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "سرویس تشخیص گفتار مشغول است"
                        SpeechRecognizer.ERROR_SERVER -> "خطای سرور تشخیص گفتار"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "هیچ گفتاری در زمان مجاز شنیده نشد"
                        else -> "خطا در تشخیص گفتار (کد: $error)"
                    }
                    _errorState.value = msg
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull { it.isNotBlank() }
                        ?.let { _speechResult.value = it.trim() }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull { it.isNotBlank() }
                        ?.let { _speechResult.value = it.trim() }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            }

            var recognizer: SpeechRecognizer? = null

            // 1. Try On-Device Recognizer on Android 12+ (API 31+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                try {
                    recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                    _isOnDevice.value = true
                } catch (_: Exception) {
                    recognizer = null
                    _isOnDevice.value = false
                }
            }

            // 2. Fallback to standard system speech recognizer
            if (recognizer == null) {
                recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                _isOnDevice.value = false
            }

            recognizer.setRecognitionListener(listener)
            speechRecognizer = recognizer

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _isOnDevice.value = false
            _errorState.value = "راه‌اندازی تشخیص گفتار با خطا مواجه شد: ${e.message ?: "خطای ناشناخته"}"
            stopListening()
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        _isListening.value = false
    }

    fun destroy() = stopListening()
}
