package com.example.data.models

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Final local-LLM lifecycle controller.
 *
 * Guarantees that only one text model is active at a time, validates the local
 * file before loading, and keeps inference behind LlmRuntimeManager.
 */
class LocalLlmModelController(
    private val runtime: LlmRuntimeManager,
    private val modelDao: com.example.data.local.dao.ModelDao
) {
    private val mutex = Mutex()

    suspend fun activate(
        model: com.example.data.local.entities.DownloadedModelEntity,
        speedMode: String
    ): Result<String> = mutex.withLock {
        if (model.modelType != ModelType.TEXT) {
            return@withLock Result.failure(
                IllegalArgumentException("این کنترلر فقط برای مدل متنی است.")
            )
        }

        val readiness = ModelReadinessChecker.check(model)
        if (!readiness.ready) {
            return@withLock Result.failure(
                IllegalStateException(readiness.message)
            )
        }

        val file = File(model.localFilePath)
        return@withLock try {
            runtime.load(file, speedMode)
            modelDao.unloadAllModelsOfType(ModelType.TEXT)
            modelDao.setModelLoaded(model.id, true)
            Result.success("مدل ${model.name} فعال شد و inference به‌صورت کاملاً محلی آماده است.")
        } catch (t: Throwable) {
            modelDao.setModelLoaded(model.id, false)
            Result.failure(t)
        }
    }

    suspend fun deactivate(modelId: String): Result<Unit> = mutex.withLock {
        return@withLock try {
            runtime.unload()
            modelDao.setModelLoaded(modelId, false)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun generate(prompt: String, speedMode: String): Result<String> =
        mutex.withLock {
            if (prompt.isBlank()) {
                return@withLock Result.failure(
                    IllegalArgumentException("متن ورودی خالی است.")
                )
            }
            if (!runtime.isLoaded()) {
                return@withLock Result.failure(
                    IllegalStateException("هیچ مدل متنی محلی فعال نیست.")
                )
            }

            return@withLock try {
                Result.success(runtime.complete(prompt, speedMode))
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }
}
