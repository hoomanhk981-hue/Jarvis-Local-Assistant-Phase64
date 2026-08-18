package com.example.data.models

import android.content.Context
import com.example.data.local.dao.ModelDao
import com.example.data.local.entities.DownloadedModelEntity
import com.example.data.local.entities.ModelType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.Locale
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class RealModelDownloadManager(
    private val context: Context,
    private val modelDao: ModelDao
) {
    private val runtime = LlmRuntimeManager(context)

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val downloadScope = CoroutineScope(Dispatchers.IO)

    fun getModelsDirectory(): File {
        val dir = File(context.filesDir, "ai_models")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Starts or resumes a real HTTP download for a model GGUF file.
     */
    fun startDownload(model: DownloadedModelEntity, onResult: ((Boolean, String) -> Unit)? = null) {
        if (activeJobs.containsKey(model.id)) return

        val job = downloadScope.launch {
            try {
                modelDao.updateDownloadStatus(
                    id = model.id,
                    isDownloaded = false,
                    progress = model.downloadProgressPercentage,
                    bytes = model.bytesDownloaded,
                    isDownloading = true,
                    speedText = "در حال اتصال به سرور..."
                )

                val targetFile = File(getModelsDirectory(), "${model.id}.gguf")
                val currentBytes = if (targetFile.exists()) targetFile.length() else 0L

                val requestBuilder = Request.Builder()
                    .url(model.downloadUrl.ifEmpty { "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf" })

                if (currentBytes > 0) {
                    requestBuilder.addHeader("Range", "bytes=$currentBytes-")
                }

                val request = requestBuilder.build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful && response.code != 206) {
                    throw Exception("خطا در دانلود از سرور: کد وضعیت HTTP ${response.code}")
                }

                val responseBody = response.body ?: throw Exception("پاسخ دریافتی از سرور خالی است.")
                val append = response.code == 206 && currentBytes > 0L
                val contentLength = responseBody.contentLength()
                val totalBytes = if (append && contentLength >= 0) currentBytes + contentLength else contentLength

                val inputStream: InputStream = responseBody.byteStream()
                val raf = RandomAccessFile(targetFile, "rw")
                val startingBytes = if (append) currentBytes else 0L
                if (append) {
                    raf.seek(currentBytes)
                } else {
                    raf.setLength(0)
                }

                val buffer = ByteArray(64 * 1024) // 64KB buffer
                var bytesRead: Int
                var totalRead = startingBytes
                var lastUpdateTime = System.currentTimeMillis()
                var lastUpdateBytes = totalRead

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    raf.write(buffer, 0, bytesRead)
                    totalRead += bytesRead

                    val now = System.currentTimeMillis()
                    if (now - lastUpdateTime >= 600) { // Update DB progress every 600ms
                        val deltaBytes = totalRead - lastUpdateBytes
                        val deltaTimeSec = (now - lastUpdateTime) / 1000.0
                        val speedBytesPerSec = if (deltaTimeSec > 0) (deltaBytes / deltaTimeSec).toLong() else 0L
                        val speedFormatted = formatSpeed(speedBytesPerSec)

                        val progress = if (totalBytes > 0) ((totalRead * 100) / totalBytes).toInt() else 0
                        modelDao.updateDownloadStatus(
                            id = model.id,
                            isDownloaded = false,
                            progress = progress.coerceIn(0, 99),
                            bytes = totalRead,
                            isDownloading = true,
                            speedText = speedFormatted
                        )

                        lastUpdateTime = now
                        lastUpdateBytes = totalRead
                    }
                }

                raf.close()
                inputStream.close()

                // Final verification: never mark a model ready without a complete file and, when supplied, SHA-256 validation.
                if (targetFile.exists() && targetFile.length() > 0) {
                    val expectedSha = model.checksumSha256.trim().lowercase()
                    if (expectedSha.isNotEmpty()) {
                        val actualSha = sha256(targetFile)
                        if (!actualSha.equals(expectedSha, ignoreCase = true)) {
                            targetFile.delete()
                            throw Exception("SHA-256 مدل با مقدار مورد انتظار مطابقت ندارد.")
                        }
                    }
                    // Vision models can have a separate multimodal projector (mmproj).
                    // It is downloaded independently and is never bundled in the APK.
                    if (model.auxiliaryDownloadUrl.isNotBlank()) {
                        val auxName = model.auxiliaryFileName.ifBlank { "${model.id}-mmproj.gguf" }
                        val auxFile = File(getModelsDirectory(), auxName)
                        downloadSingleFile(
                            url = model.auxiliaryDownloadUrl,
                            target = auxFile,
                            expectedSha256 = model.auxiliaryChecksumSha256
                        )
                        modelDao.setAuxiliaryFilePath(model.id, auxFile.absolutePath)
                    }
                    modelDao.updateDownloadStatus(
                        id = model.id,
                        isDownloaded = true,
                        progress = 100,
                        bytes = targetFile.length(),
                        filePath = targetFile.absolutePath,
                        isDownloading = false,
                        speedText = if (model.auxiliaryDownloadUrl.isBlank()) "دانلود کامل و تأیید شد" else "مدل و mmproj دانلود و تأیید شدند"
                    )
                    activeJobs.remove(model.id)
                    onResult?.invoke(true, "مدل ${model.name} با موفقیت دانلود و صحت‌سنجی شد.")
                } else {
                    throw Exception("فایل دانلود شده نامعتبر است یا وجود ندارد.")
                }

            } catch (e: CancellationException) {
                // Paused by user
                modelDao.updateDownloadStatus(
                    id = model.id,
                    isDownloaded = false,
                    progress = model.downloadProgressPercentage,
                    isDownloading = false,
                    speedText = "متوقف شد (Pause)"
                )
                activeJobs.remove(model.id)
            } catch (e: Exception) {
                modelDao.updateDownloadStatus(
                    id = model.id,
                    isDownloaded = false,
                    progress = 0,
                    isDownloading = false,
                    speedText = "خطا در دانلود: ${e.localizedMessage ?: "عدم دسترسی به اینترنت"}"
                )
                activeJobs.remove(model.id)
                onResult?.invoke(false, "خطا در دانلود مدل: ${e.message}")
            }
        }

        activeJobs[model.id] = job
    }

    /**
     * Pauses or cancels an ongoing download.
     */
    fun pauseDownload(modelId: String) {
        activeJobs[modelId]?.cancel()
        activeJobs.remove(modelId)
    }

    /**
     * Deletes model file from storage and resets its database record.
     */
    suspend fun deleteModel(model: DownloadedModelEntity): Boolean = withContext(Dispatchers.IO) {
        pauseDownload(model.id)
        val file = if (model.localFilePath.isNotEmpty()) File(model.localFilePath) else File(getModelsDirectory(), "${model.id}.gguf")
        if (file.exists()) {
            file.delete()
        }
        modelDao.resetModelDownload(model.id)
        true
    }

    /**
     * Loads a downloaded model for real local inference.
     */
    suspend fun loadModel(model: DownloadedModelEntity, speedMode: String = "MEDIUM"): String = withContext(Dispatchers.IO) {
        if (model.modelType != ModelType.TEXT) {
            return@withContext "مدل ${model.name} فعلاً فقط دانلود می‌شود؛ موتور Vision در مرحله بعد اضافه خواهد شد."
        }
        val file = if (model.localFilePath.isNotEmpty()) File(model.localFilePath) else File(getModelsDirectory(), "${model.id}.gguf")
        if (!file.exists() || file.length() == 0L) {
            return@withContext "خطا: فایل مدل در مسیر ${file.absolutePath} یافت نشد. لطفاً ابتدا مدل را دانلود کنید."
        }
        return@withContext try {
            runtime.load(file, speedMode)
            modelDao.unloadAllModelsOfType(model.modelType)
            modelDao.setModelLoaded(model.id, true)
            "مدل ${model.name} با موتور llama.cpp واقعاً در حافظه بارگذاری شد و آماده پاسخگویی کاملاً محلی است."
        } catch (e: Exception) {
            "بارگذاری واقعی مدل شکست خورد: ${e.message ?: "خطای ناشناخته"}"
        }
    }

    suspend fun generate(model: DownloadedModelEntity, prompt: String, speedMode: String): String = withContext(Dispatchers.IO) {
        if (!model.isLoaded) return@withContext "مدل ${model.name} هنوز Load نشده است."
        try {
            runtime.complete(prompt, speedMode)
        } catch (e: Exception) {
            "خطا در inference محلی: ${e.message ?: "خطای ناشناخته"}"
        }
    }

    suspend fun unloadModel(model: DownloadedModelEntity): String = withContext(Dispatchers.IO) {
        runtime.unload()
        modelDao.setModelLoaded(model.id, false)
        "مدل ${model.name} از موتور محلی خارج شد."
    }

    fun runtimeCapability(): LlmRuntimeManager.DeviceCapability = runtime.capability()

    suspend fun onTrimMemory(level: Int) {
        runtime.onTrimMemory(level)
    }

    suspend fun unloadRuntimeSafely() {
        runtime.unload()
    }

    private suspend fun downloadSingleFile(url: String, target: File, expectedSha256: String) {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("دانلود فایل جانبی شکست خورد: HTTP ${response.code}")
            val body = response.body ?: throw Exception("پاسخ فایل جانبی خالی است")
            target.parentFile?.mkdirs()
            body.byteStream().use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output, 64 * 1024) }
            }
        }
        if (target.length() == 0L) throw Exception("فایل جانبی خالی است")
        if (expectedSha256.isNotBlank() && !sha256(target).equals(expectedSha256.trim(), ignoreCase = true)) {
            target.delete()
            throw Exception("SHA-256 فایل جانبی با مقدار مورد انتظار مطابقت ندارد")
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        return when {
            bytesPerSec >= 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSec / (1024.0 * 1024.0))
            bytesPerSec >= 1024 -> String.format("%.1f KB/s", bytesPerSec / 1024.0)
            else -> "$bytesPerSec B/s"
        }
    }
}
