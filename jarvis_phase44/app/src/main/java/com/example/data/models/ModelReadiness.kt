package com.example.data.models

import com.example.data.local.entities.DownloadedModelEntity
import com.example.data.local.entities.ModelType
import java.io.File

data class ModelReadiness(
    val ready: Boolean,
    val message: String
)

object ModelReadinessChecker {
    fun check(model: DownloadedModelEntity): ModelReadiness {
        if (!model.isDownloaded) return ModelReadiness(false, "مدل هنوز دانلود نشده است")
        val main = File(model.localFilePath)
        if (!main.isFile || main.length() == 0L) return ModelReadiness(false, "فایل اصلی مدل موجود نیست")
        if (model.modelType == ModelType.VISION) {
            if (model.localAuxiliaryFilePath.isBlank()) return ModelReadiness(false, "فایل mmproj مدل Vision دانلود نشده است")
            val mmproj = File(model.localAuxiliaryFilePath)
            if (!mmproj.isFile || mmproj.length() == 0L) return ModelReadiness(false, "فایل mmproj موجود نیست")
        }
        return ModelReadiness(true, "آماده اجرا به‌صورت محلی")
    }
}
