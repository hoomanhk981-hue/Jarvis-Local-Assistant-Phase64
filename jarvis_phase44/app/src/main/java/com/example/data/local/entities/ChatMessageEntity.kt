package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val sender: String, // "USER" or "ASSISTANT"
    val text: String,
    val imageUriString: String? = null,
    val fileUriString: String? = null,
    val fileName: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
