package com.example.data.models

import com.example.data.local.entities.ModelType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * Local vision-model lifecycle controller.
 *
 * Vision inference is performed through the existing local runtime boundary.
 * Model acquisition is separate from inference and may require internet only
 * when the user chooses to download a model.
 */
class LocalVisionModelController(
    private val runtime: VisionRuntimeManager,
    private val modelDao: com.example.data.local.dao.ModelDao
) {
    private val mutex = Mutex()

    suspend fun activate(
        model: com.example.data.local.entities.DownloadedModelEntity,
        speedMode: String
    ): Result<String> = mutex.withLock {
        if (model.modelType != ModelType.VISION) {
            return@withLock Result.failure(
                IllegalArgumentException("این کنترلر فقط برای مدل Vision است.")
            )
        }

        val readiness = ModelReadinessChecker.check(model)
        if (!readiness.ready) {
            return@withLock Result.failure(
                IllegalStateException(readiness.message)
            )
        }

        return@withLock try {
            runtime.load(model, speedMode)
            modelDao.unloadAllModelsOfType(ModelType.VISION)
            modelDao.setModelLoaded(model.id, true)
            Result.success("مدل Vision ${model.name} فعال شد و آماده پردازش محلی تصویر است.")
        } catch (t: Throwable) {
            modelDao.setModelLoaded(model.id, false)
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

    suspend fun describeImage(imagePath: String, prompt: String): Result<String> =
        mutex.withLock {
            if (imagePath.isBlank()) {
                return@withLock Result.failure(
                    IllegalArgumentException("مسیر تصویر خالی است.")
                )
            }
            if (prompt.isBlank()) {
                return@withLock Result.failure(
                    IllegalArgumentException("پرسش تصویر خالی است.")
                )
            }
            if (!runtime.isLoaded()) {
                return@withLock Result.failure(
                    IllegalStateException("هیچ مدل Vision محلی فعال نیست.")
                )
            }

            return@withLock try {
                Result.success(runtime.describe(File(imagePath), prompt))
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }
}
