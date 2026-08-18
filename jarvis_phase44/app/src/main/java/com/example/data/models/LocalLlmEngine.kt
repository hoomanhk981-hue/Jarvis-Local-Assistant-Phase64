package com.example.data.models

import java.io.File
import kotlin.math.min

/**
 * Real on-device GGUF inference via llama.cpp JNI.
 *
 * Speed modes are runtime profiles rather than UI-only labels. Changing the
 * profile reloads the currently loaded GGUF with a different context size
 * and CPU thread count. Token budget is applied per generation as well.
 */
class LocalLlmEngine {
    companion object {
        init {
            try {
                System.loadLibrary("jarvis_vision")
            } catch (_: UnsatisfiedLinkError) {}
        }
    }

    enum class SpeedProfile(
        val contextSize: Int,
        val maxTokens: Int,
        val requestedThreads: Int
    ) {
        LOW(contextSize = 2048, maxTokens = 160, requestedThreads = 2),
        MEDIUM(contextSize = 4096, maxTokens = 320, requestedThreads = 4),
        HIGH(contextSize = 8192, maxTokens = 768, requestedThreads = 6)
    }

    private external fun nativeLoad(modelPath: String, threads: Int, context: Int): Boolean
    private external fun nativeComplete(prompt: String, systemPrompt: String, maxTokens: Int): String
    private external fun nativeRelease()
    private external fun nativeIsLoaded(): Boolean

    private var loadedPath: String? = null
    private var loadedProfile: SpeedProfile? = null

    suspend fun load(file: File, speedMode: String = "MEDIUM") {
        val profile = profileFor(speedMode)
        unload()
        val ok = try {
            nativeLoad(
                modelPath = file.absolutePath,
                threads = cpuThreads(profile),
                context = profile.contextSize
            )
        } catch (_: UnsatisfiedLinkError) {
            false
        }
        if (!ok) {
            error("خطا در بارگذاری مدل محلی: ${file.name}")
        }
        loadedPath = file.absolutePath
        loadedProfile = profile
    }

    suspend fun complete(prompt: String, speedMode: String): String {
        if (!isLoaded()) error("مدل محلی Load نشده است")
        val requestedProfile = profileFor(speedMode)

        if (loadedProfile != requestedProfile) {
            val path = loadedPath ?: error("مسیر مدل محلی مشخص نیست")
            val file = File(path)
            if (!file.exists() || file.length() == 0L) {
                error("فایل مدل برای تغییر حالت سرعت پیدا نشد")
            }
            load(file, requestedProfile.name)
        }

        return try {
            nativeComplete(
                prompt = prompt,
                systemPrompt = systemPromptFor(requestedProfile),
                maxTokens = requestedProfile.maxTokens
            ).trim()
        } catch (e: UnsatisfiedLinkError) {
            "موتور بومی در این محیط قابل اجرا نیست."
        }
    }

    fun activeProfile(): SpeedProfile? = loadedProfile

    fun isLoaded(): Boolean = try {
        nativeIsLoaded()
    } catch (_: UnsatisfiedLinkError) {
        false
    }

    private fun profileFor(speedMode: String): SpeedProfile = runCatching {
        SpeedProfile.valueOf(speedMode.uppercase())
    }.getOrDefault(SpeedProfile.MEDIUM)

    private fun cpuThreads(profile: SpeedProfile): Int {
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
        try {
            nativeRelease()
        } catch (_: UnsatisfiedLinkError) {}
        loadedPath = null
        loadedProfile = null
    }
}
