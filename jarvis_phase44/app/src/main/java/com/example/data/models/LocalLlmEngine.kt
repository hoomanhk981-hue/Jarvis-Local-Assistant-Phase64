package com.example.data.models

import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import dev.ffmpegkit.llama.LlamaModel
import java.io.File
import kotlin.math.min

/**
 * Real on-device GGUF inference.
 *
 * Speed modes are runtime profiles rather than UI-only labels.  Changing the
 * profile may reload the currently loaded GGUF with a different context size
 * and CPU thread count.  Token budget is applied per generation as well.
 */
class LocalLlmEngine {
    enum class SpeedProfile(
        val contextSize: Int,
        val maxTokens: Int,
        val requestedThreads: Int
    ) {
        LOW(contextSize = 2048, maxTokens = 160, requestedThreads = 2),
        MEDIUM(contextSize = 4096, maxTokens = 320, requestedThreads = 4),
        HIGH(contextSize = 8192, maxTokens = 768, requestedThreads = 6)
    }

    private var modelHandle: LlamaModel? = null
    private var loadedPath: String? = null
    private var loadedProfile: SpeedProfile? = null

    suspend fun load(file: File, speedMode: String = "MEDIUM") {
        val profile = profileFor(speedMode)
        unload()
        modelHandle = Llama.loadModel(
            modelPath = file.absolutePath,
            config = LlamaConfig(
                contextSize = profile.contextSize,
                threads = cpuThreads(profile)
            )
        )
        loadedPath = file.absolutePath
        loadedProfile = profile
    }

    suspend fun complete(prompt: String, speedMode: String): String {
        var handle = modelHandle ?: error("مدل محلی Load نشده است")
        val requestedProfile = profileFor(speedMode)

        // Context/thread settings are load-time settings in this llama Android
        // binding. Reload only when the selected profile actually changes.
        if (loadedProfile != requestedProfile) {
            val path = loadedPath ?: error("مسیر مدل محلی مشخص نیست")
            val file = File(path)
            if (!file.exists() || file.length() == 0L) {
                error("فایل مدل برای تغییر حالت سرعت پیدا نشد")
            }
            load(file, requestedProfile.name)
            handle = modelHandle ?: error("مدل محلی پس از reload آماده نشد")
        }

        val result = Llama.complete(
            handle,
            prompt = prompt,
            systemPrompt = systemPromptFor(requestedProfile),
            maxTokens = requestedProfile.maxTokens
        )
        return result.text.trim()
    }

    fun activeProfile(): SpeedProfile? = loadedProfile

    fun isLoaded(): Boolean = modelHandle != null

    private fun profileFor(speedMode: String): SpeedProfile = runCatching {
        SpeedProfile.valueOf(speedMode.uppercase())
    }.getOrDefault(SpeedProfile.MEDIUM)

    private fun cpuThreads(profile: SpeedProfile): Int {
        // Avoid asking a small phone for more threads than it physically has.
        val available = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        return min(profile.requestedThreads, available)
    }

    private fun systemPromptFor(profile: SpeedProfile): String = when (profile) {
        SpeedProfile.LOW ->
            "You are Jarvis, a private Android assistant. Answer in the user's language. " +
                "Prioritize fast, concise answers. Never claim an action was performed unless a real Android tool reported success."
        SpeedProfile.MEDIUM ->
            "You are Jarvis, a private Android assistant. Answer in the user's language. " +
                "Balance speed and accuracy. Never claim an action was performed unless a real Android tool reported success."
        SpeedProfile.HIGH ->
            "You are Jarvis, a private Android assistant. Answer in the user's language. " +
                "Use careful reasoning and verify assumptions before tool actions. Never claim an action was performed unless a real Android tool reported success."
    }

    suspend fun unload() {
        val handle = modelHandle ?: return
        Llama.releaseModel(handle)
        modelHandle = null
        loadedPath = null
        loadedProfile = null
    }
}
