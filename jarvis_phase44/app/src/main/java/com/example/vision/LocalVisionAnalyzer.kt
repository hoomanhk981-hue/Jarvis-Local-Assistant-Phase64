package com.example.vision

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.resume

/** Real, on-device image analysis and OCR. */
class LocalVisionAnalyzer(private val context: Context) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val runtime = LocalVisionLlmEngine()

    suspend fun extractText(uri: Uri): String = suspendCancellableCoroutine { cont ->
        try {
            val image = InputImage.fromFilePath(context, uri)
            recognizer.process(image)
                .addOnSuccessListener { result -> if (cont.isActive) cont.resume(result.text.trim()) }
                .addOnFailureListener { if (cont.isActive) cont.resume("") }
        } catch (_: Exception) {
            if (cont.isActive) cont.resume("")
        }
    }

    suspend fun analyze(uri: Uri, question: String, speedMode: String = "MEDIUM"): String {
        val modelDir = File(context.filesDir, "ai_models")
        val model = File(modelDir, "qwen2_vl_2b.gguf")
        val mmproj = File(modelDir, "qwen2_vl_2b-mmproj-f16.gguf")

        if (model.isFile && model.length() > 0L && mmproj.isFile && mmproj.length() > 0L) {
            val loaded = runtime.load(model.absolutePath, mmproj.absolutePath, threadsFor(speedMode), contextFor(speedMode))
            if (loaded) {
                val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
                    ByteArrayOutputStream().use { out ->
                        input.copyTo(out)
                        out.toByteArray()
                    }
                }
                if (bytes != null) {
                    return runtime.analyze(bytes, question.ifBlank { "این تصویر را با جزئیات توصیف کن." }, maxTokensFor(speedMode))
                }
            }
        }

        // Fallback to OCR if multimodal LLM weights are not downloaded
        val ocrText = extractText(uri)
        if (ocrText.isNotBlank()) {
            return "متن استخراج‌شده از تصویر (OCR محلی):\n$ocrText\n\n(برای تحلیل تصویر و توصیف بصری، مدل Vision را از بخش مدیریت مدل‌ها دانلود کنید.)"
        }

        return "مدل Vision دانلود نشده است. برای تحلیل کامل یا استخراج متن، مدل Vision را دانلود کنید."
    }

    fun close() {
        recognizer.close()
        runtime.release()
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

    private fun maxTokensFor(mode: String): Int = when (mode.uppercase()) {
        "LOW" -> 128
        "HIGH" -> 384
        else -> 256
    }
}
