package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entities.SavedPasswordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPasswordDao {
    @Query("SELECT * FROM saved_passwords ORDER BY appName ASC")
    fun getAllPasswords(): Flow<List<SavedPasswordEntity>>

    @Query("SELECT * FROM saved_passwords WHERE appName LIKE '%' || :appName || '%' LIMIT 1")
    suspend fun getPasswordForApp(appName: String): SavedPasswordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassword(password: SavedPasswordEntity)

    @Query("DELETE FROM saved_passwords WHERE id = :id")
    suspend fun deletePassword(id: Long)
}
