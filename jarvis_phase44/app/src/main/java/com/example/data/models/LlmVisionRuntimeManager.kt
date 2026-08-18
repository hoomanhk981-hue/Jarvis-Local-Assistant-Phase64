package com.example.data.models

import com.example.data.local.entities.DownloadedModelEntity
import com.example.data.local.entities.ModelType
import com.example.vision.LocalVisionLlmEngine
import java.io.File

/**
 * Real llama.cpp + libmtmd vision runtime.
 *
 * The model (GGUF) and multimodal projector (mmproj) paths are resolved from
 * the downloaded model record, so inference always uses the exact files the
 * user downloaded. No model weights are bundled in the APK.
 */
class LlmVisionRuntimeManager : VisionRuntimeManager {

    private val engine = LocalVisionLlmEngine()

    override fun load(model: DownloadedModelEntity, speedMode: String) {
        if (model.modelType != ModelType.VISION) {
            throw IllegalArgumentException("این runtime فقط برای مدل‌های Vision است.")
        }
        val gguf = File(model.localFilePath)
        val mmproj = File(model.localAuxiliaryFilePath)
        if (!gguf.isFile || gguf.length() == 0L) {
            throw IllegalStateException("فایل مدل Vision (GGUF) موجود نیست.")
        }
        if (!mmproj.isFile || mmproj.length() == 0L) {
            throw IllegalStateException("فایل mmproj مدل Vision دانلود نشده است.")
        }
        val ok = engine.load(
            modelPath = gguf.absolutePath,
            mmprojPath = mmproj.absolutePath,
            threads = threadsFor(speedMode),
            context = contextFor(speedMode)
        )
        if (!ok) {
            throw IllegalStateException("بارگذاری مدل Vision در موتور محلی (llama.cpp/libmtmd) ناموفق بود.")
        }
    }

    override fun unload() {
        engine.release()
    }

    override fun isLoaded(): Boolean = engine.isLoaded()

    override fun describe(imageFile: File, prompt: String): String {
        if (!imageFile.isFile || imageFile.length() == 0L) {
            throw IllegalArgumentException("فایل تصویر موجود نیست یا خالی است.")
        }
        val bytes = imageFile.readBytes()
        return engine.analyze(
            imageBytes = bytes,
            question = prompt.ifBlank { "این تصویر را با جزئیات توصیف کن." },
            maxTokens = 256
        )
    }

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
}
