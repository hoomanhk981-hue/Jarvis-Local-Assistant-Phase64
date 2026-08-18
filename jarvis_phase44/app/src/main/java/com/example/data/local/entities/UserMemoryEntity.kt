package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MemoryCategory {
    MODEL_KNOWLEDGE, // Learned fact from web/google
    APP_PASSWORD,    // App/Service password
    USER_PREFERENCE, // User preference or custom note
    SKILL_DATA       // Banking, ticket, or phone shortcut
}

@Entity(tableName = "user_memory")
data class UserMemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,
    val value: String,
    val category: MemoryCategory,
    val sourceFolder: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
