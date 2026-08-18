package com.example.vision

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream

/**
 * JNI bridge to llama.cpp + libmtmd.
 * No model weights are packaged here: callers provide downloaded GGUF/mmproj paths.
 */
class LocalVisionLlmEngine {
    companion object {
        init { System.loadLibrary("jarvis_vision") }
    }

    private external fun nativeLoad(modelPath: String, mmprojPath: String, threads: Int, context: Int): Boolean
    private external fun nativeAnalyze(imageBytes: ByteArray, question: String, maxTokens: Int): String
    private external fun nativeRelease()
    private external fun nativeIsLoaded(): Boolean

    fun load(modelPath: String, mmprojPath: String, threads: Int = 2, context: Int = 4096): Boolean =
        nativeLoad(modelPath, mmprojPath, threads, context)

    fun analyze(imageBytes: ByteArray, question: String, maxTokens: Int = 256): String =
        nativeAnalyze(imageBytes, question, maxTokens)

    fun isLoaded(): Boolean = nativeIsLoaded()
    fun release() = nativeRelease()
}

class LocalVisionAnalyzer(private val context: Context) {
    private val runtime = LocalVisionLlmEngine()

    suspend fun analyze(uri: Uri, question: String, speedMode: String = "MEDIUM"): String {
        val modelDir = java.io.File(context.filesDir, "ai_models")
        val model = java.io.File(modelDir, "qwen2_vl_2b.gguf")
        val mmproj = java.io.File(modelDir, "qwen2_vl_2b-mmproj-f16.gguf")
        if (!model.isFile || model.length() == 0L) {
            return "مدل Vision دانلود نشده است. از Model Manager مدل Vision و فایل mmproj آن را دانلود کنید."
        }
        if (!mmproj.isFile || mmproj.length() == 0L) {
            return "فایل mmproj مدل Vision دانلود نشده است؛ بدون آن Vision Runtime فعال نمی‌شود."
        }

        val loaded = runtime.load(model.absolutePath, mmproj.absolutePath, threadsFor(speedMode), contextFor(speedMode))
        if (!loaded) return "بارگذاری واقعی مدل Vision شکست خورد؛ سازگاری مدل و mmproj را بررسی کنید."

        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            ByteArrayOutputStream().use { out ->
                input.copyTo(out)
                out.toByteArray()
            }
        } ?: return "خواندن تصویر شکست خورد."

        return runtime.analyze(bytes, question.ifBlank { "این تصویر را با جزئیات توصیف کن." }, maxTokensFor(speedMode))
    }

    fun close() = runtime.release()

    private fun threadsFor(mode: String): Int = when (mode.uppercase()) {
        "LOW" -> 2
        "HIGH" -> 6
        else -> 4
    }

    private fun contextFor(mode: String): Int = when (mode.uppercase()) {
        "LOW" -> 2048
        "HIGH" -> 4096
        else -> 3072
    }

    private fun maxTokensFor(mode: String): Int = when (mode.uppercase()) {
        "LOW" -> 128
        "HIGH" -> 384
        else -> 256
    }
}
