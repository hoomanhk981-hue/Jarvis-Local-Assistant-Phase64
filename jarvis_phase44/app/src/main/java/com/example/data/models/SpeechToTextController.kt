package com.example.data.models

import com.example.data.local.entities.ModelType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Offline-first Speech-to-Text controller.
 *
 * Model acquisition is separate from recognition. Recognition itself never
 * calls a remote speech API.
 */
class SpeechToTextController(
    private val runtime: SpeechRuntimeManager,
    private val modelDao: com.example.data.local.dao.ModelDao
) {
    private val mutex = Mutex()

    suspend fun activate(
        model: com.example.data.local.entities.DownloadedModelEntity,
        speedMode: String
    ): Result<String> = mutex.withLock {
        if (model.modelType != ModelType.SPEECH_TO_TEXT) {
            return@withLock Result.failure(
                IllegalArgumentException("این کنترلر فقط برای Speech-to-Text است.")
            )
        }

        val readiness = ModelReadinessChecker.check(model)
        if (!readiness.ready) {
            return@withLock Result.failure(
                IllegalStateException(readiness.message)
            )
        }

        return@withLock try {
            runtime.load(File(model.localFilePath), speedMode)
            modelDao.unloadAllModelsOfType(ModelType.SPEECH_TO_TEXT)
            modelDao.setModelLoaded(model.id, true)
            Result.success("مدل Speech-to-Text فعال شد.")
        } catch (t: Throwable) {
            modelDao.setModelLoaded(model.id, false)
            Result.failure(t)
        }
    }

    suspend fun transcribe(
        pcm16Mono16k: ShortArray,
        language: String = "fa"
    ): Result<String> = mutex.withLock {
        if (pcm16Mono16k.isEmpty()) {
            return@withLock Result.failure(
                IllegalArgumentException("صدای ضبط‌شده خالی است.")
            )
        }
        if (!runtime.isLoaded()) {
            return@withLock Result.failure(
                IllegalStateException("مدل Speech-to-Text فعال نیست.")
            )
        }

        return@withLock try {
            Result.success(runtime.transcribe(pcm16Mono16k, language))
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun deactivate(modelId: String): Result<Unit> = mutex.withLock {
        try {
            runtime.unload()
            modelDao.setModelLoaded(modelId, false)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
