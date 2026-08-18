package com.example.speech

import android.content.Context
import android.os.Build
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Central readiness check for the offline voice pipeline.
 * It never treats an online recognizer/voice as an offline capability.
 */
object VoiceReadinessGate {
    data class Result(
        val sttOnDevicePossible: Boolean,
        val persianTtsOffline: Boolean,
        val englishTtsOffline: Boolean,
        val messages: List<String>
    )

    fun check(context: Context): Result {
        val messages = mutableListOf<String>()
        val stt = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isRecognitionAvailable(context)
        if (!stt) messages += "تشخیص گفتار آفلاین روی این دستگاه در دسترس نیست."

        var fa = false
        var en = false
        runCatching {
            val tts = TextToSpeech(context) { }
            tts.voices?.forEach { voice ->
                if (!voice.isNetworkConnectionRequired && voice.locale.language == "fa") fa = true
                if (!voice.isNetworkConnectionRequired && voice.locale.language == Locale.US.language) en = true
            }
            tts.shutdown()
        }
        if (!fa) messages += "صدای فارسی آفلاین نصب نیست."
        if (!en) messages += "صدای انگلیسی آفلاین نصب نیست."
        return Result(stt, fa, en, messages)
    }
}
