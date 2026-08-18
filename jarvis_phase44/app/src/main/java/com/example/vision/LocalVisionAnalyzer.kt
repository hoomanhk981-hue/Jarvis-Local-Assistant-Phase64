package com.example.vision

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Real, on-device image analysis fallback. It never pretends OCR is a multimodal LLM. */
class LocalVisionAnalyzer(private val context: Context) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

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

    fun close() = recognizer.close()
}
