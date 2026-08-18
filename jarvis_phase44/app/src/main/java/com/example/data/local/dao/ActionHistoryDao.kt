package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.data.local.entities.ActionHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionHistoryDao {
    @Query("SELECT * FROM action_history ORDER BY timestamp DESC LIMIT 50")
    fun getRecentHistory(): Flow<List<ActionHistoryEntity>>

    @Insert
    suspend fun insertAction(action: ActionHistoryEntity)
}
