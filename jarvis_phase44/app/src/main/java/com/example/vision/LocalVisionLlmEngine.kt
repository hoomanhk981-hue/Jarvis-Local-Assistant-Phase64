package com.example.vision

/**
 * JNI bridge to llama.cpp + libmtmd.
 * No model weights are packaged here: callers provide downloaded GGUF/mmproj paths.
 */
class LocalVisionLlmEngine {
    companion object {
        init {
            try {
                System.loadLibrary("jarvis_vision")
            } catch (_: UnsatisfiedLinkError) {}
        }
    }

    private external fun nativeLoad(modelPath: String, mmprojPath: String, threads: Int, context: Int): Boolean
    private external fun nativeAnalyze(imageBytes: ByteArray, question: String, maxTokens: Int): String
    private external fun nativeRelease()
    private external fun nativeIsLoaded(): Boolean

    fun load(modelPath: String, mmprojPath: String, threads: Int = 2, context: Int = 4096): Boolean = try {
        nativeLoad(modelPath, mmprojPath, threads, context)
    } catch (_: UnsatisfiedLinkError) {
        false
    }

    fun analyze(imageBytes: ByteArray, question: String, maxTokens: Int = 256): String = try {
        nativeAnalyze(imageBytes, question, maxTokens)
    } catch (e: UnsatisfiedLinkError) {
        "Vision Runtime در این محیط در دسترس نیست."
    }

    fun isLoaded(): Boolean = try {
        nativeIsLoaded()
    } catch (_: UnsatisfiedLinkError) {
        false
    }

    fun release() {
        try {
            nativeRelease()
        } catch (_: UnsatisfiedLinkError) {}
    }
}
