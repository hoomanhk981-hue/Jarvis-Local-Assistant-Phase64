package com.example.data.models

/**
 * Native/offline speech runtime boundary.
 *
 * Audio is PCM 16-bit, mono, 16 kHz. No network endpoint is required.
 */
interface SpeechRuntimeManager {
    fun load(modelFile: java.io.File, speedMode: String)
    fun unload()
    fun isLoaded(): Boolean
    fun transcribe(pcm16Mono16k: ShortArray, language: String): String
}
