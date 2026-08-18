package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "action_history")
data class ActionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val commandText: String,
    val skillExecuted: String, // "APP_LAUNCH", "PHONE_CALL", "CARD_TRANSFER", "SMS_SEARCH", "TERMUX_COMMAND"
    val resultSummary: String,
    val isSuccess: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
