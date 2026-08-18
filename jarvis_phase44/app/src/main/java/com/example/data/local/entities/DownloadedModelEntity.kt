package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ModelType {
    TEXT,
    VISION
}

enum class SpeedRating {
    LOW,    // Fast speed, lower accuracy
    MEDIUM, // Balanced speed & quality
    HIGH    // Deep reasoning, high precision
}

@Entity(tableName = "downloaded_models")
data class DownloadedModelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val sizeBytes: Long,
    val sizeFormatted: String,
    val ramRequiredMb: Int = 1500,
    val requiredAbi: String = "arm64-v8a / arm64",
    val license: String = "Open Source / Apache 2.0",
    val modelType: ModelType,
    val speedRating: SpeedRating,
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val isLoaded: Boolean = false,
    val downloadProgressPercentage: Int = 0,
    val bytesDownloaded: Long = 0L,
    val downloadSpeedText: String = "",
    val localFilePath: String = "",
    val downloadUrl: String = "",
    val checksumSha256: String = "",
    val auxiliaryDownloadUrl: String = "",
    val auxiliaryFileName: String = "",
    val auxiliaryChecksumSha256: String = "",
    val localAuxiliaryFilePath: String = ""
)
