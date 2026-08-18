package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.DownloadedModelEntity
import com.example.data.local.entities.ModelType
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelDao {
    @Query("SELECT * FROM downloaded_models ORDER BY name ASC")
    fun getAllModels(): Flow<List<DownloadedModelEntity>>

    @Query("SELECT * FROM downloaded_models WHERE modelType = :type")
    fun getModelsByType(type: ModelType): Flow<List<DownloadedModelEntity>>

    @Query("SELECT * FROM downloaded_models WHERE id = :id LIMIT 1")
    suspend fun getModelById(id: String): DownloadedModelEntity?

    @Query("SELECT * FROM downloaded_models WHERE isLoaded = 1 AND modelType = :type LIMIT 1")
    suspend fun getActiveLoadedModel(type: ModelType): DownloadedModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateModel(model: DownloadedModelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(models: List<DownloadedModelEntity>)

    @Query("UPDATE downloaded_models SET isDownloaded = :isDownloaded, downloadProgressPercentage = :progress, bytesDownloaded = :bytes, localFilePath = :filePath, isDownloading = :isDownloading, downloadSpeedText = :speedText WHERE id = :id")
    suspend fun updateDownloadStatus(
        id: String,
        isDownloaded: Boolean,
        progress: Int,
        bytes: Long = 0L,
        filePath: String = "",
        isDownloading: Boolean = false,
        speedText: String = ""
    )

    @Query("UPDATE downloaded_models SET isLoaded = 0 WHERE modelType = :type")
    suspend fun unloadAllModelsOfType(type: ModelType)

    @Query("UPDATE downloaded_models SET localAuxiliaryFilePath = :path WHERE id = :id")
    suspend fun setAuxiliaryFilePath(id: String, path: String)

    @Query("UPDATE downloaded_models SET isLoaded = :isLoaded WHERE id = :id")
    suspend fun setModelLoaded(id: String, isLoaded: Boolean)

    @Query("UPDATE downloaded_models SET isDownloaded = 0, downloadProgressPercentage = 0, bytesDownloaded = 0, isDownloading = 0, isLoaded = 0, localFilePath = '' WHERE id = :id")
    suspend fun resetModelDownload(id: String)

    @Delete
    suspend fun deleteModel(model: DownloadedModelEntity)
}
