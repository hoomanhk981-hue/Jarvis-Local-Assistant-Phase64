package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.ActionHistoryDao
import com.example.data.local.dao.ChatDao
import com.example.data.local.dao.ModelDao
import com.example.data.local.dao.SavedPasswordDao
import com.example.data.local.dao.UserMemoryDao
import com.example.data.local.entities.ActionHistoryEntity
import com.example.data.local.entities.ChatMessageEntity
import com.example.data.local.entities.ChatSessionEntity
import com.example.data.local.entities.DownloadedModelEntity
import com.example.data.local.entities.SavedPasswordEntity
import com.example.data.local.entities.UserMemoryEntity

@Database(
    entities = [
        DownloadedModelEntity::class,
        UserMemoryEntity::class,
        SavedPasswordEntity::class,
        ActionHistoryEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun modelDao(): ModelDao
    abstract fun userMemoryDao(): UserMemoryDao
    abstract fun savedPasswordDao(): SavedPasswordDao
    abstract fun actionHistoryDao(): ActionHistoryDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "local_assistant_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
